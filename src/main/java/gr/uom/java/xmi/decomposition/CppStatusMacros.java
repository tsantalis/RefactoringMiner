package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

/**
 * The C++ status macros that stand for statements returning an error, which the model keeps as one call statement,
 * like the AST diff tree, and matches with the statements they replace.
 */
public class CppStatusMacros {
	//the macros returning an error when their condition is false, with the message streamed after them,
	//i.e., #define TF_RET_CHECK(condition) while (!(condition)) return MakeErrorStream(..., kInternal) of xla/status_macros.h
	private static final Set<String> CHECK_MACROS = Set.of("TF_RET_CHECK");

	//a check macro replacing an if statement returning an error with the same message, when the condition is false,
	//i.e., if (executable_candidates.empty()) { ...; return absl::InternalError(absl::StrCat("Autotuning failed for HLO: ", instr->ToString(), ". No configs could be compiled.")); }
	//-> TF_RET_CHECK(!candidates.empty()) << "Autotuning failed for HLO: " << instr->ToString() << ". No configs could be compiled.";
	public static Replacement conditionalReturnReplacedWithCheckMacro(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		if(conditionalReturnOfCheckMacro(statement1, statement2)) {
			return new Replacement(statement1.getString(), statement2.getString(), ReplacementType.CONDITIONAL_RETURN_REPLACED_WITH_CHECK_MACRO);
		}
		if(conditionalReturnOfCheckMacro(statement2, statement1)) {
			return new Replacement(statement1.getString(), statement2.getString(), ReplacementType.CONDITIONAL_RETURN_REPLACED_WITH_CHECK_MACRO);
		}
		return null;
	}

	//the return statement of the if, or the condition of the if, i.e., the condition executable_candidates.empty() matches the macro with the return of its if,
	//so that the condition is not matched with another statement containing an inverted condition
	private static boolean conditionalReturnOfCheckMacro(AbstractCodeFragment statement, AbstractCodeFragment macroStatement) {
		if(!macroStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.EXPRESSION_STATEMENT)) {
			return false;
		}
		AbstractCodeFragment returnStatement = statement;
		if(statement instanceof AbstractExpression condition && condition.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT_CONDITION) && condition.getOwner() != null) {
			returnStatement = lastReturnOfThenBranch(condition.getOwner());
		}
		if(returnStatement == null || !returnStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
			return false;
		}
		AbstractCall check = checkMacro(macroStatement);
		CompositeStatementObject ifStatement = enclosingIfWithoutElse(returnStatement);
		if(check == null || ifStatement == null || ifStatement.getExpressions().size() != 1 || statement != returnStatement && ifStatement != ((AbstractExpression)statement).getOwner()) {
			return false;
		}
		//the if returns when the condition of the macro is false
		String condition = ifStatement.getExpressions().get(0).getString();
		if(!shape(condition).equals(negatedShape(check.arguments().get(0)))) {
			return false;
		}
		//the message streamed after the macro is the message of the returned error
		List<String> message = macroStatement.getStringLiterals().stream().map(LeafExpression::getString).collect(Collectors.toList());
		Set<String> returned = returnStatement.getStringLiterals().stream().map(LeafExpression::getString).collect(Collectors.toSet());
		return !message.isEmpty() && returned.containsAll(message);
	}

	//the check macro called first in the statement, i.e., TF_RET_CHECK(!candidates.empty()) in TF_RET_CHECK(!candidates.empty()) << "..." << x;
	private static AbstractCall checkMacro(AbstractCodeFragment statement) {
		for(AbstractCall call : statement.getMethodInvocations()) {
			if(CHECK_MACROS.contains(call.getName()) && call.arguments().size() == 1 && statement.getString().startsWith(call.getName() + "(")) {
				return call;
			}
		}
		return null;
	}

	//the condition of the if whose return the check macro replaced, i.e., executable_candidates.empty() for return absl::InternalError(...)
	public static AbstractExpression conditionOfReplacedIf(AbstractCodeFragment returnStatement) {
		//the macro statement replaced with the if statement has no replaced if, i.e., TF_RET_CHECK(!x.empty()) << "..." -> if (x.empty()) { return absl::InternalError("..."); }
		if(checkMacro(returnStatement) != null) {
			return null;
		}
		CompositeStatementObject ifStatement = enclosingIfWithoutElse(returnStatement);
		return ifStatement != null && ifStatement.getExpressions().size() == 1 ? ifStatement.getExpressions().get(0) : null;
	}

	//the statement ending the then branch of the if statement, i.e., return absl::InternalError(...) in if (x.empty()) { log(); return absl::InternalError(...); }
	private static AbstractCodeFragment lastReturnOfThenBranch(CompositeStatementObject ifStatement) {
		if(!ifStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) || ifStatement.getStatements().size() != 1) {
			return null;
		}
		AbstractStatement then = ifStatement.getStatements().get(0);
		if(then instanceof CompositeStatementObject block && block.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			List<AbstractStatement> statements = block.getStatements();
			return statements.isEmpty() ? null : statements.get(statements.size() - 1);
		}
		return then;
	}

	//the if statement whose then branch ends with the statement, without an else branch
	private static CompositeStatementObject enclosingIfWithoutElse(AbstractCodeFragment statement) {
		CompositeStatementObject parent = statement.getParent();
		if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			List<AbstractStatement> statements = parent.getStatements();
			if(statements.isEmpty() || statements.get(statements.size() - 1) != statement) {
				return null;
			}
			parent = parent.getParent();
		}
		if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent.getStatements().size() == 1) {
			return parent;
		}
		return null;
	}

	//the macros declaring a value of a StatusOr or returning its error,
	//i.e., #define ASSIGN_OR_RETURN(lhs, rexpr) auto statusor = (rexpr); if (!statusor.ok()) { return statusor.status(); } lhs = std::move(statusor).value() of xla/tsl/platform/status_macros.h
	private static final Set<String> ASSIGN_OR_RETURN_MACROS = Set.of("ASSIGN_OR_RETURN");
	//the expressions unwrapping the value of a StatusOr, where x is the StatusOr
	private static final Set<String> UNWRAPPED_VALUES = Set.of("std::move(x).value()", "x.value()", "*x", "std::move(*x)", "*std::move(x)", "std::move(x.value())");

	//an assign-or-return macro replacing the declaration of a StatusOr checked by an if statement returning its error,
	//i.e., auto profile_results_or = ProfileAll(std::move(executable_candidates), instr); if (!profile_results_or.ok()) { return ...; } std::vector<ConfigResult> profile_results = std::move(profile_results_or).value();
	//-> ASSIGN_OR_RETURN(std::vector<Tuner::ConfigProfile> profiles, tuner_->ProfileAll(std::move(candidates), instr));
	public static Replacement statusOrDeclarationReplacedWithAssignOrReturn(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		if(statusOrDeclarationOfAssignOrReturn(statement1, statement2) || statusOrDeclarationOfAssignOrReturn(statement2, statement1)) {
			return new Replacement(statement1.getString(), statement2.getString(), ReplacementType.STATUS_OR_DECLARATION_REPLACED_WITH_ASSIGN_OR_RETURN);
		}
		return null;
	}

	private static boolean statusOrDeclarationOfAssignOrReturn(AbstractCodeFragment declaration, AbstractCodeFragment macroStatement) {
		AbstractCall macro = assignOrReturnMacro(macroStatement);
		if(macro == null || !declaration.getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) ||
				declaration.getVariableDeclarations().size() != 1 || declaration.getVariableDeclarations().get(0).getInitializer() == null) {
			return false;
		}
		//the StatusOr is the value of the same call, i.e., ProfileAll(std::move(executable_candidates), instr) and tuner_->ProfileAll(std::move(candidates), instr)
		AbstractCall initializer = callOf(declaration, initializer(declaration.getVariableDeclarations().get(0)));
		AbstractCall value = callOf(macroStatement, macro.arguments().get(1));
		if(initializer == null || value == null || !initializer.getName().equals(value.getName()) || initializer.arguments().size() != value.arguments().size()) {
			return false;
		}
		return statusCheck(declaration) != null;
	}

	//the assign-or-return macro covering the statement
	private static AbstractCall assignOrReturnMacro(AbstractCodeFragment statement) {
		for(AbstractCall call : statement.getMethodInvocations()) {
			if(ASSIGN_OR_RETURN_MACROS.contains(call.getName()) && call.arguments().size() == 2 && statement.getString().startsWith(call.getName() + "(")) {
				return call;
			}
		}
		return null;
	}

	//the call of the statement written as the expression, i.e., tuner_.ProfileAll(std::move(candidates),instr) for tuner_->ProfileAll(std::move(candidates), instr)
	private static AbstractCall callOf(AbstractCodeFragment statement, String expression) {
		String normalized = normalized(expression);
		for(AbstractCall call : statement.getMethodInvocations()) {
			if(normalized(call.actualString()).equals(normalized)) {
				return call;
			}
		}
		return null;
	}

	//the initializer of the declaration, without the = that the C++ initializer includes, i.e., ProfileAll(x) for = ProfileAll(x)
	private static String initializer(VariableDeclaration declaration) {
		String initializer = declaration.getInitializer().getString().strip();
		return initializer.startsWith("=") ? initializer.substring(1).strip() : initializer;
	}

	private static String normalized(String expression) {
		return expression.replaceAll("\\s+", "").replace("->", ".");
	}

	//the if statement following the declaration of the StatusOr in its block, returning when it is not ok, i.e., if (!profile_results_or.ok())
	private static CompositeStatementObject statusCheck(AbstractCodeFragment declaration) {
		String name = declaration.getVariableDeclarations().get(0).getVariableName();
		for(AbstractStatement statement : followingStatements(declaration)) {
			if(statement instanceof CompositeStatementObject ifStatement && ifStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
					ifStatement.getStatements().size() == 1 && ifStatement.getExpressions().size() == 1 &&
					normalized(ifStatement.getExpressions().get(0).getString()).equals("!" + name + ".ok()") && lastReturnOfThenBranch(ifStatement) instanceof StatementObject) {
				return ifStatement;
			}
		}
		return null;
	}

	//the declaration of the value of the StatusOr after its check, i.e., std::vector<ConfigResult> profile_results = std::move(profile_results_or).value();
	private static AbstractStatement unwrappedValue(AbstractCodeFragment declaration) {
		String name = declaration.getVariableDeclarations().get(0).getVariableName();
		for(AbstractStatement statement : followingStatements(declaration)) {
			if(statement.getVariableDeclarations().size() == 1 && statement.getVariableDeclarations().get(0).getInitializer() != null) {
				String initializer = normalized(initializer(statement.getVariableDeclarations().get(0)));
				for(String unwrapped : UNWRAPPED_VALUES) {
					if(initializer.equals(normalized(unwrapped.replace("x", name)))) {
						return statement;
					}
				}
			}
		}
		return null;
	}

	private static List<AbstractStatement> followingStatements(AbstractCodeFragment statement) {
		CompositeStatementObject parent = statement.getParent();
		if(parent == null) {
			return List.of();
		}
		List<AbstractStatement> statements = parent.getStatements();
		int index = statements.indexOf(statement);
		return index == -1 ? List.of() : statements.subList(index + 1, statements.size());
	}

	//the statements of the declaration of the StatusOr that the assign-or-return macro also replaces: the condition and the return of its check, and the declaration of its value
	public static List<AbstractCodeFragment> replacedByAssignOrReturn(AbstractCodeFragment declaration) {
		List<AbstractCodeFragment> replaced = new java.util.ArrayList<>();
		//the macro statement replaced with the declaration of the StatusOr replaces no statements of its own code, i.e., ASSIGN_OR_RETURN(T v, f()) -> auto v_or = f(); if (!v_or.ok()) {...}
		if(assignOrReturnMacro(declaration) != null || declaration.getVariableDeclarations().size() != 1) {
			return replaced;
		}
		CompositeStatementObject check = statusCheck(declaration);
		if(check != null) {
			replaced.add(check.getExpressions().get(0));
			replaced.add(lastReturnOfThenBranch(check));
		}
		AbstractStatement value = unwrappedValue(declaration);
		if(value instanceof StatementObject) {
			replaced.add(value);
		}
		return replaced;
	}

	//the expression with the names of its variables replaced, so that the conditions of renamed variables are compared, i.e., x.empty() for executable_candidates.empty()
	private static String shape(String expression) {
		String shape = expression.replaceAll("\\s+", "");
		while(shape.startsWith("(") && shape.endsWith(")")) {
			shape = shape.substring(1, shape.length() - 1);
		}
		return shape.replaceAll("\\b[A-Za-z_]\\w*\\b(?!\\s*\\()", "x");
	}

	private static String negatedShape(String expression) {
		String shape = shape(expression);
		if(shape.startsWith("!")) {
			return shape(shape.substring(1));
		}
		return "!" + shape;
	}
}
