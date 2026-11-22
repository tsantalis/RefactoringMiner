package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

/**
 * The declarations in the first argument of the calls of unknown macros that CDT parses as problem statements, since an argument of a call cannot be a declaration,
 * i.e., auto x in ASSIGN_OR_RETURN(auto x, f(a)); of xla/tsl/platform/status_macros.h, which declares x for the statements after the call.
 * The model keeps the call as one statement, like the AST diff tree, and adds these declarations to it.
 * The file is parsed again with each such call written as the statements of its arguments, i.e., auto x ; f(a) ;, which keeps the offsets of the file
 * and the block of the call, so that the declarations have the scope they have in the expansion of the macro.
 */
public class CppMacroArguments {
	//the declarations by the offset of the problem statement of the call, for the translation units of the files, which are kept while the translation units are used
	private static final Map<IASTTranslationUnit, Map<Integer, IASTDeclarationStatement>> DECLARATIONS = Collections.synchronizedMap(new WeakHashMap<>());

	//the declaration in the first argument of the call of a macro parsed as the problem statement
	public static IASTDeclarationStatement declaration(IASTProblemStatement problem) {
		IASTFileLocation location = problem.getFileLocation();
		Map<Integer, IASTDeclarationStatement> declarations = location != null ? DECLARATIONS.get(problem.getTranslationUnit()) : null;
		return declarations != null ? declarations.get(location.getNodeOffset()) : null;
	}

	static void parse(IASTTranslationUnit translationUnit, String filePath, String fileContent, boolean astDiff, String preamble) {
		//the range of each call, and the offsets of the commas separating its arguments
		Map<Integer, int[]> calls = new TreeMap<>();
		Map<Integer, List<Integer>> commas = new HashMap<>();
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				IASTFileLocation location = statement.getFileLocation();
				if(statement instanceof IASTProblemStatement && location != null && CppFileProcessor.isPartOfFile(statement)) {
					List<Integer> separators = new ArrayList<>();
					int[] call = macroCall(fileContent, location.getNodeOffset(), separators);
					if(call != null && !separators.isEmpty()) {
						calls.put(location.getNodeOffset(), call);
						commas.put(location.getNodeOffset(), separators);
					}
				}
				return PROCESS_CONTINUE;
			}
		});
		if(calls.isEmpty()) {
			return;
		}
		char[] code = fileContent.toCharArray();
		for(var entry : calls.entrySet()) {
			int[] call = entry.getValue();
			//NAME( and ) are blanked, the commas between the arguments end their statements, and the semicolon of the call ends the last one
			for(int k = call[0]; k <= call[1]; k++) {
				code[k] = ' ';
			}
			code[call[2]] = ' ';
			for(int comma : commas.get(entry.getKey())) {
				code[comma] = ';';
			}
		}
		IASTTranslationUnit reparsed;
		try {
			reparsed = CppFileProcessor.parse(filePath, new String(code), astDiff, preamble);
		}
		catch(CoreException e) {
			return;
		}
		Map<Integer, IASTDeclarationStatement> declarations = new HashMap<>();
		reparsed.accept(new ASTVisitor() {
			{
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				IASTFileLocation location = statement.getFileLocation();
				if(statement instanceof IASTDeclarationStatement declarationStatement && location != null && CppFileProcessor.isPartOfFile(statement)) {
					//the first argument starts after NAME(, so the declaration belongs to the call starting before it at the closest offset
					Map.Entry<Integer, int[]> call = ((TreeMap<Integer, int[]>)calls).floorEntry(location.getNodeOffset());
					if(call != null && location.getNodeOffset() == call.getValue()[1] + 1 + leadingBlanks(fileContent, call.getValue()[1] + 1)
							&& location.getNodeOffset() + location.getNodeLength() == commas.get(call.getKey()).get(0) + 1 && isDeclaration(declarationStatement)) {
						declarations.put(call.getKey(), declarationStatement);
					}
				}
				return PROCESS_CONTINUE;
			}
		});
		if(!declarations.isEmpty()) {
			DECLARATIONS.put(translationUnit, declarations);
		}
	}

	private static int leadingBlanks(String fileContent, int offset) {
		int k = offset;
		while(k < fileContent.length() && Character.isWhitespace(fileContent.charAt(k))) {
			k++;
		}
		return k - offset;
	}

	//a declaration of named variables without problems and initializers, and not a call parsed as a declaration, i.e., not x in f(x); declaring x with type f
	private static boolean isDeclaration(IASTDeclarationStatement statement) {
		if(!(statement.getDeclaration() instanceof IASTSimpleDeclaration declaration) || declaration.getDeclarators().length == 0 || containsProblem(statement)) {
			return false;
		}
		for(IASTDeclarator declarator : declaration.getDeclarators()) {
			if(declarator.getNestedDeclarator() != null || declarator.getName() == null || declarator.getName().toString().isEmpty() || declarator.getInitializer() != null) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsProblem(IASTNode node) {
		boolean[] found = {false};
		node.accept(new ASTVisitor(true) {
			@Override
			public int visit(IASTProblem problem) {
				found[0] = true;
				return PROCESS_ABORT;
			}

			@Override
			public int visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration declaration) {
				if(declaration instanceof IASTProblemDeclaration) {
					found[0] = true;
					return PROCESS_ABORT;
				}
				return PROCESS_CONTINUE;
			}
		});
		return found[0];
	}

	//the offsets of the start of the name, of the opening parenthesis and of the closing parenthesis of the call of a macro starting at the offset,
	//which ends with a semicolon, i.e., ASSIGN_OR_RETURN(auto x, f(a));, with the offsets of the commas between its arguments,
	//outside parentheses, brackets, braces, template arguments, and string and character literals and comments
	static int[] macroCall(String code, int start, List<Integer> commas) {
		int k = start;
		if(k >= code.length() || !Character.isJavaIdentifierStart(code.charAt(k))) {
			return null;
		}
		while(k < code.length() && Character.isJavaIdentifierPart(code.charAt(k))) {
			k++;
		}
		String name = code.substring(start, k);
		if(!name.equals(name.toUpperCase())) {
			return null;
		}
		k += leadingBlanks(code, k);
		if(k >= code.length() || code.charAt(k) != '(') {
			return null;
		}
		int open = k;
		int depth = 0;
		int angles = 0;
		for(; k < code.length(); k++) {
			char c = code.charAt(k);
			if(c == '"' || c == '\'') {
				for(k++; k < code.length() && code.charAt(k) != c; k++) {
					if(code.charAt(k) == '\\') {
						k++;
					}
				}
			}
			else if(c == '/' && k + 1 < code.length() && code.charAt(k + 1) == '/') {
				int end = code.indexOf('\n', k);
				k = end < 0 ? code.length() : end;
			}
			else if(c == '/' && k + 1 < code.length() && code.charAt(k + 1) == '*') {
				int end = code.indexOf("*/", k + 2);
				k = end < 0 ? code.length() : end + 1;
			}
			else if(c == '(' || c == '[' || c == '{') {
				depth++;
			}
			else if(c == ')' || c == ']' || c == '}') {
				depth--;
				if(depth == 0) {
					break;
				}
			}
			//the template arguments of a name, i.e., std::pair<A, B> x, where < follows the name, unlike a comparison written with spaces
			else if(c == '<' && depth == 1 && k > 0 && (Character.isJavaIdentifierPart(code.charAt(k - 1)) || code.charAt(k - 1) == '>') ) {
				angles++;
			}
			else if(c == '>' && depth == 1 && angles > 0 && code.charAt(k - 1) != '-') {
				angles--;
			}
			else if(c == ',' && depth == 1 && angles == 0) {
				commas.add(k);
			}
		}
		if(k >= code.length() || code.charAt(k) != ')') {
			return null;
		}
		int close = k;
		k = close + 1 + leadingBlanks(code, close + 1);
		if(k >= code.length() || code.charAt(k) != ';') {
			return null;
		}
		return new int[] {start, open, close};
	}
}
