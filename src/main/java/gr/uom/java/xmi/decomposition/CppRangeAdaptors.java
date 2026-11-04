package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

//the declaration of a variable initialized with a call, whose call becomes the argument of another call with the arguments replaced with their values,
//i.e., auto tiled = std::make_unique<T>(hlo, tile); -> tiled_operands.push_back(std::make_unique<T>(&operand.instruction(), operands_tiles[i]));
//when const HloInstruction* hlo = &operand.instruction(); is inlined, and tile is the element of operands_tiles that llvm::enumerate(llvm::zip(operands_tiles, ...)) binds at index i
public class CppRangeAdaptors {
	//llvm::enumerate(range) binds the index and the element of range, and llvm::zip(a, b, ...) binds the elements of a, b, ... at the same position
	private static final String ENUMERATE = "llvm::enumerate";
	private static final String ZIP = "llvm::zip";
	private static final Pattern STRUCTURED_BINDING = Pattern.compile("^(?:const\\s+)?auto\\s*&{0,2}\\s*\\[([^\\]]*)\\]\\s*=\\s*([A-Za-z_]\\w*)\\s*;\\s*$");
	private static final Pattern DESTRUCTURING = Pattern.compile("^(?:const\\s+)?auto\\s*&{0,2}\\s*\\[([^\\]]*)\\]$");
	private static final Pattern ARRAY_ACCESS = Pattern.compile("^([A-Za-z_][\\w.>-]*)\\[([A-Za-z_]\\w*)\\]$");
	private static final Pattern VARIABLE = Pattern.compile("^[A-Za-z_]\\w*$");

	public static List<Replacement> rightHandSideWithReplacedArguments(AbstractCodeFragment statement1, AbstractCodeFragment statement2, AbstractCall invocation2,
			VariableDeclarationContainer container1) {
		if(invocation2 == null || !statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) ||
				statement1.getVariableDeclarations().size() != 1 || statement1.getVariableDeclarations().get(0).getInitializer() == null) {
			return null;
		}
		String initializer = initializer(statement1.getVariableDeclarations().get(0));
		String callee1 = callee(initializer);
		if(callee1 == null) {
			return null;
		}
		List<String> arguments1 = arguments(initializer);
		for(String argument2 : invocation2.arguments()) {
			String trimmed2 = argument2.strip();
			if(!callee1.equals(callee(trimmed2))) {
				continue;
			}
			List<String> arguments2 = arguments(trimmed2);
			if(arguments1 == null || arguments2 == null || arguments1.size() != arguments2.size()) {
				continue;
			}
			List<Replacement> replacements = new ArrayList<>();
			boolean justified = true;
			for(int i=0; i<arguments1.size() && justified; i++) {
				String a1 = arguments1.get(i), a2 = arguments2.get(i);
				if(a1.equals(a2)) {
					continue;
				}
				if(inlinedVariable(a1, a2, statement1, container1) || rangeElement(a1, a2, statement1, statement2, container1)) {
					replacements.add(new Replacement(a1, a2, replacementType(a2)));
				}
				else {
					justified = false;
				}
			}
			//the arguments are not all identical, as then the right hand side is the argument
			if(justified && !replacements.isEmpty()) {
				replacements.add(0, new Replacement(initializer, trimmed2, ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION));
				return replacements;
			}
		}
		return null;
	}

	//the variable is declared before the statement with the argument of the other call as its initializer, i.e., const HloInstruction* hlo = &operand.instruction();
	private static boolean inlinedVariable(String argument1, String argument2, AbstractCodeFragment statement1, VariableDeclarationContainer container1) {
		VariableDeclaration declaration = declarationInScope(argument1, statement1, container1);
		return declaration != null && declaration.getInitializer() != null && initializer(declaration).equals(argument2);
	}

	//the variable is the element of a range at the index that llvm::enumerate binds, and the other argument is that range at the index that llvm::enumerate binds for the other statement,
	//i.e., tile in for (const auto& [id, tile_and_operand] : llvm::enumerate(llvm::zip(tiles, operands))) { auto& [tile, operand] = tile_and_operand; ... } -> tiles[i] in for (const auto& [i, operand] : llvm::enumerate(operands))
	private static boolean rangeElement(String argument1, String argument2, AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclarationContainer container1) {
		Matcher access = ARRAY_ACCESS.matcher(argument2);
		if(!access.matches() || !VARIABLE.matcher(argument1).matches()) {
			return false;
		}
		String[] element = elementOf(argument1, statement1, container1);
		if(element == null || !element[0].equals(access.group(1))) {
			return false;
		}
		//the index is the one that llvm::enumerate binds in a loop enclosing the other statement
		for(CompositeStatementObject loop = enclosingLoop(statement2); loop != null; loop = enclosingLoop(loop)) {
			String[] binding = enumerateBinding(loop);
			if(binding != null && binding[0].equals(access.group(2))) {
				return true;
			}
		}
		return false;
	}

	//the range and the index of the element that the variable is, as {range, index}
	private static String[] elementOf(String variable, AbstractCodeFragment statement, VariableDeclarationContainer container) {
		VariableDeclaration declaration = declarationInScope(variable, statement, container);
		if(declaration == null) {
			return null;
		}
		//bound by a loop over llvm::enumerate(range), i.e., for (auto& [i, x] : llvm::enumerate(range))
		for(CompositeStatementObject loop = enclosingLoop(statement); loop != null; loop = enclosingLoop(loop)) {
			if(loop.getVariableDeclarations().contains(declaration)) {
				String[] binding = enumerateBinding(loop);
				if(binding != null && binding[1].equals(variable)) {
					String range = enumeratedRange(loop);
					return range != null && !range.startsWith(ZIP + "(") ? new String[] {range, binding[0]} : null;
				}
				return null;
			}
		}
		//bound by a structured binding of the element of a loop over llvm::enumerate(llvm::zip(a, b, ...)), i.e., auto& [x, y] = element;
		AbstractStatement bindingStatement = bindingStatement(declaration, statement);
		if(bindingStatement == null) {
			return null;
		}
		Matcher matcher = STRUCTURED_BINDING.matcher(bindingStatement.getString());
		if(!matcher.matches()) {
			return null;
		}
		List<String> names = names(matcher.group(1));
		String element = matcher.group(2);
		int position = names.indexOf(variable);
		for(CompositeStatementObject loop = enclosingLoop(statement); loop != null && position >= 0; loop = enclosingLoop(loop)) {
			String[] binding = enumerateBinding(loop);
			String range = enumeratedRange(loop);
			if(binding != null && binding[1].equals(element) && range != null && range.startsWith(ZIP + "(") && range.endsWith(")")) {
				List<String> zipped = arguments(range);
				return zipped != null && zipped.size() == names.size() ? new String[] {zipped.get(position), binding[0]} : null;
			}
		}
		return null;
	}

	//the names that a loop over llvm::enumerate binds, i.e., {i, x} for for (const auto& [i, x] : llvm::enumerate(range))
	private static String[] enumerateBinding(CompositeStatementObject loop) {
		if(enumeratedRange(loop) == null) {
			return null;
		}
		for(AbstractExpression expression : loop.getExpressions()) {
			if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_DESTRUCTURING_DECLARATION)) {
				Matcher matcher = DESTRUCTURING.matcher(expression.getString().strip());
				if(matcher.matches()) {
					List<String> names = names(matcher.group(1));
					return names.size() == 2 ? names.toArray(new String[0]) : null;
				}
			}
		}
		return null;
	}

	//the range of a loop over llvm::enumerate(range)
	private static String enumeratedRange(CompositeStatementObject loop) {
		List<AbstractExpression> expressions = loop.getExpressions();
		if(!loop.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) || expressions.isEmpty()) {
			return null;
		}
		String range = expressions.get(expressions.size() - 1).getString().strip();
		if(!range.startsWith(ENUMERATE + "(") || !range.endsWith(")")) {
			return null;
		}
		List<String> arguments = arguments(range);
		return arguments != null && arguments.size() == 1 ? arguments.get(0) : null;
	}

	//the statement before the given one in its block, or in an enclosing block, that declares the variable
	private static AbstractStatement bindingStatement(VariableDeclaration declaration, AbstractCodeFragment statement) {
		AbstractCodeFragment current = statement;
		while(current != null && current.getParent() != null) {
			for(AbstractStatement sibling : current.getParent().getStatements()) {
				if(sibling == current) {
					break;
				}
				if(sibling.getVariableDeclarations().contains(declaration)) {
					return sibling;
				}
			}
			current = current.getParent();
		}
		return null;
	}

	private static VariableDeclaration declarationInScope(String variable, AbstractCodeFragment statement, VariableDeclarationContainer container) {
		if(container == null) {
			return null;
		}
		VariableDeclaration found = null;
		for(VariableDeclaration declaration : container.getVariableDeclarationsInScope(statement.getLocationInfo())) {
			//the closest declaration in scope, i.e., the one whose scope starts last
			if(declaration.getVariableName().equals(variable) &&
					(found == null || declaration.getScope().getStartOffset() > found.getScope().getStartOffset())) {
				found = declaration;
			}
		}
		return found;
	}

	private static CompositeStatementObject enclosingLoop(AbstractCodeFragment statement) {
		CompositeStatementObject current = statement == null ? null : statement.getParent();
		while(current != null) {
			if(current.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
				return current;
			}
			current = current.getParent();
		}
		return null;
	}

	private static String initializer(VariableDeclaration declaration) {
		String initializer = declaration.getInitializer().getString().strip();
		return initializer.startsWith("=") ? initializer.substring(1).strip() : initializer;
	}

	//the called function of a call, with its template arguments, i.e., std::make_unique<T> for std::make_unique<T>(a, b)
	private static String callee(String call) {
		int open = call.indexOf('(');
		if(open <= 0 || !call.endsWith(")") || closing(call, open) != call.length() - 1) {
			return null;
		}
		String callee = call.substring(0, open).strip();
		return callee.matches("[A-Za-z_][\\w:<>,. *&-]*") ? callee : null;
	}

	//the top-level arguments of a call, i.e., [a, f(b, c)] for g(a, f(b, c))
	private static List<String> arguments(String call) {
		int open = call.indexOf('(');
		if(open < 0 || closing(call, open) != call.length() - 1) {
			return null;
		}
		List<String> arguments = new ArrayList<>();
		int depth = 0, start = open + 1;
		for(int i = open + 1; i < call.length() - 1; i++) {
			char c = call.charAt(i);
			if(c == '(' || c == '[' || c == '{' || c == '<') depth++;
			else if(c == ')' || c == ']' || c == '}' || (c == '>' && call.charAt(i - 1) != '-')) depth--;
			else if(c == ',' && depth == 0) {
				arguments.add(call.substring(start, i).strip());
				start = i + 1;
			}
		}
		String last = call.substring(start, call.length() - 1).strip();
		if(!last.isEmpty() || !arguments.isEmpty()) {
			arguments.add(last);
		}
		return arguments;
	}

	private static int closing(String s, int open) {
		int depth = 0;
		for(int i = open; i < s.length(); i++) {
			if(s.charAt(i) == '(') depth++;
			else if(s.charAt(i) == ')' && --depth == 0) return i;
		}
		return -1;
	}

	private static List<String> names(String list) {
		List<String> names = new ArrayList<>();
		for(String name : list.split(",")) {
			names.add(name.strip());
		}
		return names;
	}

	private static ReplacementType replacementType(String argument2) {
		if(ARRAY_ACCESS.matcher(argument2).matches()) {
			return ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS;
		}
		if(argument2.startsWith("&") || argument2.startsWith("*") || argument2.startsWith("!") || argument2.startsWith("-")) {
			return ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION;
		}
		if(argument2.endsWith(")")) {
			return ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION;
		}
		return ReplacementType.VARIABLE_NAME;
	}
}
