package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtBreakExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtContinueExpression;
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFinallySection;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLabeledExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtThrowExpression;
import org.jetbrains.kotlin.psi.KtTryExpression;
import org.jetbrains.kotlin.psi.KtVariableDeclaration;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.psi.KtWhileExpression;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.statement.LangAssertStatement;
import extension.ast.node.statement.LangAsyncStatement;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangBreakStatement;
import extension.ast.node.statement.LangCaseStatement;
import extension.ast.node.statement.LangCatchClause;
import extension.ast.node.statement.LangContinueStatement;
import extension.ast.node.statement.LangDelStatement;
import extension.ast.node.statement.LangExpressionStatement;
import extension.ast.node.statement.LangForStatement;
import extension.ast.node.statement.LangGlobalStatement;
import extension.ast.node.statement.LangIfStatement;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.statement.LangNonLocalStatement;
import extension.ast.node.statement.LangPassStatement;
import extension.ast.node.statement.LangReturnStatement;
import extension.ast.node.statement.LangSwitchStatement;
import extension.ast.node.statement.LangThrowStatement;
import extension.ast.node.statement.LangTryStatement;
import extension.ast.node.statement.LangWhileStatement;
import extension.ast.node.statement.LangWithStatement;
import extension.ast.node.statement.LangYieldStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.ast.visitor.LangVisitor;
import extension.base.LangSupportedEnum;
import static extension.umladapter.UMLModelAdapter.createUMLOperation;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class OperationBody {

	private CompositeStatementObject compositeStatement;
	private List<String> stringRepresentation;
	private boolean containsAssertion;
	private Map<String, Set<VariableDeclaration>> activeVariableDeclarations;
	private VariableDeclarationContainer container;
	private int bodyHashCode;
	private List<UMLComment> comments;

	// constructor for module class
	public OperationBody(LangCompilationUnit cu, String sourceFolder, String filePath, List<LangASTNode> statements, ModuleContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, cu, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		for(LangASTNode statement : statements) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		this.activeVariableDeclarations = null;
	}

	// constructor for regular functions and lambdas
	public OperationBody(LangCompilationUnit cu, String sourceFolder, String filePath, LangBlock methodBody, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = LangVisitor.stringify(methodBody).hashCode();
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		for(LangASTNode statement : methodBody.getStatements()) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public OperationBody(CompilationUnit cu, String sourceFolder, String filePath, Block methodBody, VariableDeclarationContainer container, List<UMLAttribute> attributes, String javaFileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, javaFileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = stringify(methodBody).hashCode();
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		for(UMLAttribute attribute : attributes) {
			addInActiveVariableDeclarations(attribute.getVariableDeclaration());
		}
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		List<Statement> statements = methodBody.statements();
		for(Statement statement : statements) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, javaFileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public OperationBody(CompilationUnit cu, String sourceFolder, String filePath, Block methodBody, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, javaFileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = stringify(methodBody).hashCode();
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		List<Statement> statements = methodBody.statements();
		for(Statement statement : statements) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, javaFileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	private void addInActiveVariableDeclarations(VariableDeclaration v) {
		if(activeVariableDeclarations.containsKey(v.getVariableName())) {
			activeVariableDeclarations.get(v.getVariableName()).add(v);
		}
		else {
			Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
			set.add(v);
			activeVariableDeclarations.put(v.getVariableName(), set);
		}
	}

	private void removeFromActiveVariableDeclarations(VariableDeclaration v) {
		if(activeVariableDeclarations.containsKey(v.getVariableName())) {
			activeVariableDeclarations.get(v.getVariableName()).remove(v);
		}
	}

	private void addAllInActiveVariableDeclarations(List<VariableDeclaration> variables) {
		for(VariableDeclaration v : variables) {
			addInActiveVariableDeclarations(v);
		}
	}

	private void removeAllFromActiveVariableDeclarations(List<VariableDeclaration> variables) {
		for(VariableDeclaration v : variables) {
			removeFromActiveVariableDeclarations(v);
		}
	}

	public double builderStatementRatio() {
		List<AbstractStatement> fragments = compositeStatement.getStatements();
		int builderCount = 0;
		for(AbstractCodeFragment fragment : fragments) {
			AbstractCall invocation = fragment.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = fragment.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation instanceof OperationInvocation) {
				OperationInvocation inv = (OperationInvocation)invocation;
				if(inv.numberOfSubExpressions() > 3) {
					builderCount++;
				}
			}
		}
		//fragments.size() == 1 corresponds to a single-statement method
		if(fragments.size() > 1)
			return (double)builderCount/(double)fragments.size();
		return 0;
	}

	public int statementCount() {
		return compositeStatement.statementCount();
	}

	public int statementCountIncludingBlocks() {
		return compositeStatement.statementCountIncludingBlocks();
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public boolean containsAssertion() {
		return containsAssertion;
	}

	public List<CompositeStatementObject> getSynchronizedStatements() {
		List<CompositeStatementObject> synchronizedStatements = new ArrayList<CompositeStatementObject>();
		for(CompositeStatementObject innerNode : compositeStatement.getInnerNodes()) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
				synchronizedStatements.add(innerNode);
			}
		}
		return synchronizedStatements;
	}

	public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
		return new ArrayList<AnonymousClassDeclarationObject>(compositeStatement.getAllAnonymousClassDeclarations());
	}

	public List<AbstractCall> getAllOperationInvocations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllMethodInvocations());
	}

	public List<AbstractCall> getAllCreations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllCreations());
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		return new ArrayList<LambdaExpressionObject>(compositeStatement.getAllLambdas());
	}

	public List<String> getAllVariables() {
		return new ArrayList<String>(compositeStatement.getAllVariables());
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		return new ArrayList<VariableDeclaration>(compositeStatement.getAllVariableDeclarations());
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		return new ArrayList<VariableDeclaration>(compositeStatement.getVariableDeclarationsInScope(location));
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		return compositeStatement.getVariableDeclaration(variableName);
	}

	private void processStatement(LangCompilationUnit cu, String sourceFolder, String filePath, CompositeStatementObject parent, LangASTNode statement, String fileContent) {
		for(UMLComment comment : comments) {
			if(comment.getParent() != null && comment.getParent().equals(parent))
				continue;
			if(parent.getLocationInfo().subsumes(comment.getLocationInfo())) {
				if(comment.getParent() != null) {
					if(!parent.getLocationInfo().subsumes(comment.getParent().getLocationInfo())) {
						comment.setParent(parent);
					}
				}
				else {
					comment.setParent(parent);
				}
			}
		}
		if(statement instanceof LangBlock) {
			LangBlock block = (LangBlock)statement;
			List<LangASTNode> blockStatements = block.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(LangASTNode blockStatement : blockStatements) {
				processStatement(cu, sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof LangIfStatement) {
			LangIfStatement ifStatement = (LangIfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, ifStatement.getBody(), fileContent);
			if(ifStatement.getElseBody() != null) {
				processStatement(cu, sourceFolder, filePath, child, ifStatement.getElseBody(), fileContent);
			}
		}
		else if(statement instanceof LangForStatement) {
			LangForStatement forStatement = (LangForStatement)statement;
			CompositeStatementObject child = null;
			if(forStatement.getUpdates().isEmpty() && forStatement.getInitializers().size() >= 1) {
				child = new CompositeStatementObject(cu, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, fileContent);
				parent.addStatement(child);
				List<LangSingleVariableDeclaration> initializers = forStatement.getInitializers();
				for(LangSingleVariableDeclaration variableDeclaration : initializers) {
					VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, fileContent);
					child.addVariableDeclaration(vd);
					AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getLangSimpleName(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, fileContent);
					child.addExpression(variableDeclarationName);
				}
				if(forStatement.getCondition() != null) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, forStatement.getCondition(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
			}
			else {
				child = new CompositeStatementObject(cu, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT, fileContent);
				parent.addStatement(child);
				List<LangSingleVariableDeclaration> initializers = forStatement.getInitializers();
				for(LangSingleVariableDeclaration initializer : initializers) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
				LangASTNode expression = forStatement.getCondition();
				if(expression != null) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
				List<LangASTNode> updaters = forStatement.getUpdates();
				for(LangASTNode updater : updaters) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(cu, sourceFolder, filePath, child, forStatement.getBody(), fileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		/*else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, enhancedForStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, javaFileContent);
			parent.addStatement(child);
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, javaFileContent);
			child.addVariableDeclaration(vd);
			AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getName(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, enhancedForStatement.getExpression(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(cu, sourceFolder, filePath, child, enhancedForStatement.getBody(), javaFileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}*/
		else if(statement instanceof LangWhileStatement) {
			LangWhileStatement whileStatement = (LangWhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
		}
		/*else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, doStatement.getExpression(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, doStatement.getBody(), javaFileContent);
		}*/
		else if(statement instanceof LangExpressionStatement) {
			LangExpressionStatement expressionStatement = (LangExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, expressionStatement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangSwitchStatement) {
			LangSwitchStatement switchStatement = (LangSwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<LangCaseStatement> switchStatements = switchStatement.getCases();
			for(LangCaseStatement switchStatement2 : switchStatements)
				processStatement(cu, sourceFolder, filePath, child, switchStatement2, fileContent);
		}
		else if(statement instanceof LangCaseStatement) {
			LangCaseStatement switchCase = (LangCaseStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, switchCase, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangYieldStatement) {
			LangYieldStatement yieldStatement = (LangYieldStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, yieldStatement, parent.getDepth()+1, CodeElementType.YIELD_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangAssertStatement) {
			LangAssertStatement assertStatement = (LangAssertStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, assertStatement, parent.getDepth()+1, CodeElementType.ASSERT_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		/*else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			SimpleName label = labeledStatement.getLabel();
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(label.getIdentifier()), javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, labeledStatement.getBody(), javaFileContent);
		}*/
		else if(statement instanceof LangReturnStatement) {
			LangReturnStatement returnStatement = (LangReturnStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		/*else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, synchronizedStatement, parent.getDepth()+1, CodeElementType.SYNCHRONIZED_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, synchronizedStatement.getExpression(), CodeElementType.SYNCHRONIZED_STATEMENT_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, synchronizedStatement.getBody(), javaFileContent);
		}*/
		else if(statement instanceof LangThrowStatement) {
			LangThrowStatement throwStatement = (LangThrowStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangTryStatement) {
			LangTryStatement tryStatement = (LangTryStatement)statement;
			TryStatementObject child = new TryStatementObject(cu, sourceFolder, filePath, tryStatement, parent.getDepth()+1, fileContent);
			parent.addStatement(child);
			/*List<Expression> resources = tryStatement.resources();
			for(Expression resource : resources) {
				AbstractExpression expression = new AbstractExpression(cu, sourceFolder, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(expression);
			}*/
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(tryStatement.getBody() instanceof LangBlock tryBody) {
				for(LangASTNode blockStatement : tryBody.getStatements()) {
					processStatement(cu, sourceFolder, filePath, child, blockStatement, fileContent);
				}
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
			List<LangCatchClause> catchClauses = tryStatement.getCatchClauses();
			for(LangCatchClause catchClause : catchClauses) {
				LangASTNode catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				LangSimpleName variableDeclaration = catchClause.getExceptionVariable();
				if(variableDeclaration != null) {
					// TODO fix catch exception variable declaration
					//VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, javaFileContent);
					//catchClauseStatementObject.addVariableDeclaration(vd);
					AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration, CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, fileContent);
					catchClauseStatementObject.addExpression(variableDeclarationName);
				}
				//if(variableDeclaration.getInitializer() != null) {
				//	AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
				//	catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				//}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
				if(catchClauseBody instanceof LangBlock catchBody) {
					for(LangASTNode blockStatement : catchBody.getStatements()) {
						processStatement(cu, sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
					}
				}
				removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
			}
			LangASTNode finallyBlock = tryStatement.getFinallyBlock();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, fileContent);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				finallyClauseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(finallyClauseStatementObject);
				if(finallyBlock instanceof LangBlock finallyBody) {
					for(LangASTNode blockStatement : finallyBody.getStatements()) {
						processStatement(cu, sourceFolder, filePath, finallyClauseStatementObject, blockStatement, fileContent);
					}
				}
			}
		}
		/*else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, variableDeclarationStatement, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, constructorInvocation, parent.getDepth()+1, CodeElementType.CONSTRUCTOR_INVOCATION, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, superConstructorInvocation, parent.getDepth()+1, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}*/
		else if(statement instanceof LangBreakStatement) {
			LangBreakStatement breakStatement = (LangBreakStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangContinueStatement) {
			LangContinueStatement continueStatement = (LangContinueStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangPassStatement) {
			LangPassStatement emptyStatement = (LangPassStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, emptyStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangWithStatement) {
			LangWithStatement withStatement = (LangWithStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, withStatement, parent.getDepth()+1, CodeElementType.WITH_STATEMENT, fileContent);
			parent.addStatement(child);
			List<LangASTNode> contextItems = withStatement.getContextItems();
			for (LangASTNode contextItem : contextItems) {
				// Create expressions for context managers
				AbstractExpression contextExpr = new AbstractExpression(cu, sourceFolder, filePath, contextItem, CodeElementType.VARIABLE_DECLARATION_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(contextExpr);
				// if parent is try statement, then add as resource
				if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
					parent.addExpression(contextExpr);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			LangBlock body = withStatement.getBody();
			processStatement(cu, sourceFolder, filePath, child, body, fileContent);
		}
		else if(statement instanceof LangImportStatement) {	
			LangImportStatement importStatement = (LangImportStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, importStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangGlobalStatement) {
			// The global statement in Python is a keyword used within a function to declare that a variable being referenced or assigned to within that function refers to a global variable, rather than a local one.
		}
		else if(statement instanceof LangNonLocalStatement) {
			// The nonlocal statement in Python is used within nested functions to declare that a variable refers to a previously bound variable in the nearest enclosing, non-global scope. 
			// It allows an inner function to modify variables defined in an outer (but not global) function's scope. 
		}
		else if(statement instanceof LangDelStatement) {
			// The del statement in Python is used to delete objects, including variables, items within data structures (like lists and dictionaries), and slices of arrays. 
			// It effectively removes references to objects from a given scope or container.
		}
		else if(statement instanceof LangAsyncStatement) {
			// In Python, the async statement (keyword) is used to define asynchronous functions, also known as coroutines. 
			// These functions are designed to work with Python's asyncio library for concurrent programming, allowing for efficient handling of I/O-bound operations like network requests or file operations without blocking the main program execution.
		}
		else if(statement instanceof LangMethodDeclaration) {
			LangMethodDeclaration methodDecl = (LangMethodDeclaration)statement;
			String className = container.getClassName() + "." + container.getName();
			UMLOperation nested = createUMLOperation(methodDecl, className, sourceFolder, filePath, fileContent, activeVariableDeclarations, LangSupportedEnum.fromFileName(filePath));
			if(container instanceof UMLOperation) {
				((UMLOperation)container).addNestedOperation(nested);
			}
		}
	}

	private void processStatement(CompilationUnit cu, String sourceFolder, String filePath, CompositeStatementObject parent, Statement statement, String javaFileContent) {
		for(UMLComment comment : comments) {
			if(comment.getParent() != null && comment.getParent().equals(parent))
				continue;
			if(parent.getLocationInfo().subsumes(comment.getLocationInfo())) {
				if(comment.getParent() != null) {
					if(!parent.getLocationInfo().subsumes(comment.getParent().getLocationInfo())) {
						comment.setParent(parent);
					}
				}
				else {
					comment.setParent(parent);
				}
			}
		}
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(Statement blockStatement : blockStatements) {
				processStatement(cu, sourceFolder, filePath, child, blockStatement, javaFileContent);
			}
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, ifStatement.getExpression(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, ifStatement.getThenStatement(), javaFileContent);
			if(ifStatement.getElseStatement() != null) {
				processStatement(cu, sourceFolder, filePath, child, ifStatement.getElseStatement(), javaFileContent);
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT, javaFileContent);
			parent.addStatement(child);
			List<Expression> initializers = forStatement.initializers();
			for(Expression initializer : initializers) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(abstractExpression);
			}
			Expression expression = forStatement.getExpression();
			if(expression != null) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(abstractExpression);
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression updater : updaters) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(cu, sourceFolder, filePath, child, forStatement.getBody(), javaFileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, enhancedForStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, javaFileContent);
			parent.addStatement(child);
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, javaFileContent);
			child.addVariableDeclaration(vd);
			AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getName(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, enhancedForStatement.getExpression(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(cu, sourceFolder, filePath, child, enhancedForStatement.getBody(), javaFileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, whileStatement.getExpression(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, whileStatement.getBody(), javaFileContent);
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, doStatement.getExpression(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, doStatement.getBody(), javaFileContent);
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, expressionStatement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<Statement> switchStatements = switchStatement.statements();
			for(Statement switchStatement2 : switchStatements)
				processStatement(cu, sourceFolder, filePath, child, switchStatement2, javaFileContent);
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, switchCase, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof YieldStatement) {
			YieldStatement yieldStatement = (YieldStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, yieldStatement, parent.getDepth()+1, CodeElementType.YIELD_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, assertStatement, parent.getDepth()+1, CodeElementType.ASSERT_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			SimpleName label = labeledStatement.getLabel();
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(label.getIdentifier()), javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, labeledStatement.getBody(), javaFileContent);
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, synchronizedStatement, parent.getDepth()+1, CodeElementType.SYNCHRONIZED_STATEMENT, javaFileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, synchronizedStatement.getExpression(), CodeElementType.SYNCHRONIZED_STATEMENT_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, synchronizedStatement.getBody(), javaFileContent);
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			TryStatementObject child = new TryStatementObject(cu, sourceFolder, filePath, tryStatement, parent.getDepth()+1, javaFileContent);
			parent.addStatement(child);
			List<Expression> resources = tryStatement.resources();
			for(Expression resource : resources) {
				AbstractExpression expression = new AbstractExpression(cu, sourceFolder, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(expression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			List<Statement> tryStatements = tryStatement.getBody().statements();
			for(Statement blockStatement : tryStatements) {
				processStatement(cu, sourceFolder, filePath, child, blockStatement, javaFileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				Block catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, javaFileContent);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				SingleVariableDeclaration variableDeclaration = catchClause.getException();
				VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, javaFileContent);
				catchClauseStatementObject.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getName(), CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, javaFileContent);
				catchClauseStatementObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getInitializer() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
					catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
				List<Statement> blockStatements = catchClauseBody.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, sourceFolder, filePath, catchClauseStatementObject, blockStatement, javaFileContent);
				}
				removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, javaFileContent);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				finallyClauseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(finallyClauseStatementObject);
				List<Statement> blockStatements = finallyBlock.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, sourceFolder, filePath, finallyClauseStatementObject, blockStatement, javaFileContent);
				}
			}
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, variableDeclarationStatement, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, constructorInvocation, parent.getDepth()+1, CodeElementType.CONSTRUCTOR_INVOCATION, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, superConstructorInvocation, parent.getDepth()+1, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof EmptyStatement) {
			EmptyStatement emptyStatement = (EmptyStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, emptyStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, javaFileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}

	private void addStatementInVariableScopes(AbstractStatement statement) {
		for(String variableName : activeVariableDeclarations.keySet()) {
			Set<VariableDeclaration> variableDeclarations = activeVariableDeclarations.get(variableName);
			for(VariableDeclaration variableDeclaration : variableDeclarations) {
				boolean localVariableWithSameName = false;
				if(variableDeclaration.isAttribute() && variableDeclarations.size() > 1) {
					localVariableWithSameName = true;
				}
				variableDeclaration.addStatementInScope(statement, localVariableWithSameName);
				if(container != null) {
					for(AnonymousClassDeclarationObject anonymous : statement.getAnonymousClassDeclarations()) {
						UMLAnonymousClass anonymousClass = container.findAnonymousClass(anonymous);
						for(UMLOperation operation : anonymousClass.getOperations()) {
							if(operation.getBody() != null) {
								CompositeStatementObject composite = operation.getBody().getCompositeStatement();
								for(AbstractStatement anonymousStatement : composite.getInnerNodes()) {
									variableDeclaration.addStatementInScope(anonymousStatement, localVariableWithSameName);
								}
								for(AbstractCodeFragment anonymousStatement : composite.getLeaves()) {
									variableDeclaration.addStatementInScope(anonymousStatement, localVariableWithSameName);
								}
							}
						}
					}
					for(LambdaExpressionObject lambda : statement.getLambdas()) {
						OperationBody lambdaBody = lambda.getBody();
						if(lambdaBody != null) {
							CompositeStatementObject composite = lambdaBody.getCompositeStatement();
							for(AbstractStatement lambdaStatement : composite.getInnerNodes()) {
								variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
							}
							for(AbstractCodeFragment lambdaStatement : composite.getLeaves()) {
								variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
							}
						}
						AbstractExpression lambdaExpression = lambda.getExpression();
						if(lambdaExpression != null) {
							variableDeclaration.addStatementInScope(lambdaExpression, localVariableWithSameName);
						}
					}
				}
			}
		}
		for(UMLComment comment : comments) {
			if(comment.getLocationInfo().nextLine(statement.getLocationInfo())) {
				comment.addPreviousLocation(statement.getLocationInfo());
			}
		}
	}

	public Map<String, Set<String>> aliasedVariables() {
		return compositeStatement.aliasedVariables();
	}

	public Map<String, Set<String>> aliasedAttributes() {
		return compositeStatement.aliasedAttributes();
	}

	public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		return compositeStatement.loopWithVariables(currentElementName, collectionName);
	}

	public int getBodyHashCode() {
		return bodyHashCode;
	}

	public List<String> stringRepresentation() {
		if(stringRepresentation == null) {
			stringRepresentation = compositeStatement.stringRepresentation();
		}
		return stringRepresentation;
	}

	public OperationBody(KtFile ktFile, String sourceFolder, String filePath, KtBlockExpression methodBody, VariableDeclarationContainer container, List<UMLAttribute> attributes, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(ktFile, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = methodBody.getText().hashCode();
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		for(UMLAttribute attribute : attributes) {
			addInActiveVariableDeclarations(attribute.getVariableDeclaration());
		}
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		List<KtExpression> statements = methodBody.getStatements();
		for(KtExpression statement : statements) {
			processStatement(ktFile, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public OperationBody(KtFile ktFile, String sourceFolder, String filePath, KtBlockExpression methodBody, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(ktFile, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = methodBody.getText().hashCode();
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		List<KtExpression> statements = methodBody.getStatements();
		for(KtExpression statement : statements) {
			processStatement(ktFile, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	private void processStatement(KtFile ktFile, String sourceFolder, String filePath, CompositeStatementObject parent, KtExpression statement, String fileContent) {
		if(statement instanceof KtBlockExpression block) {
			List<KtExpression> blockStatements = block.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(KtExpression blockStatement : blockStatements) {
				processStatement(ktFile, sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof KtIfExpression ifStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(ktFile, sourceFolder, filePath, child, ifStatement.getThen(), fileContent);
			if(ifStatement.getElse() != null) {
				processStatement(ktFile, sourceFolder, filePath, child, ifStatement.getElse(), fileContent);
			}
		}
		else if(statement instanceof KtWhileExpression whileStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(ktFile, sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
		}
		else if(statement instanceof KtDoWhileExpression doStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(ktFile, sourceFolder, filePath, child, doStatement.getBody(), fileContent);
		}
		else if(statement instanceof KtForExpression forStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, fileContent);
			parent.addStatement(child);
			KtParameter loopParameter = forStatement.getLoopParameter();
			KtDestructuringDeclaration destructuringDeclaration = forStatement.getDestructuringDeclaration();
			if (destructuringDeclaration != null) {
				AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, destructuringDeclaration, CodeElementType.ENHANCED_FOR_STATEMENT_DESTRUCTURING_DECLARATION, container, activeVariableDeclarations, fileContent);
				child.addExpression(variableDeclarationName);
				List<KtDestructuringDeclarationEntry> entries = destructuringDeclaration.getEntries();
				for (KtDestructuringDeclarationEntry entry : entries) {
					VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, entry, container, activeVariableDeclarations, fileContent, child.getLocationInfo());
					child.addVariableDeclaration(vd);
				}
			}
			else if (loopParameter != null) {
				VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, loopParameter, container, activeVariableDeclarations, fileContent, child.getLocationInfo());
				child.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, loopParameter, CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, fileContent);
				child.addExpression(variableDeclarationName);
				if(loopParameter.getDefaultValue() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(ktFile, sourceFolder, filePath, loopParameter.getDefaultValue(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, fileContent);
					child.addExpression(variableDeclarationInitializer);
				}
			}
			if (forStatement.getLoopRange() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, forStatement.getLoopRange(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(ktFile, sourceFolder, filePath, child, forStatement.getBody(), fileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof KtTryExpression tryStatement) {
			TryStatementObject child = new TryStatementObject(ktFile, sourceFolder, filePath, tryStatement, parent.getDepth()+1, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			KtBlockExpression tryBlock = tryStatement.getTryBlock();
			for(KtExpression blockStatement : tryBlock.getStatements()) {
				processStatement(ktFile, sourceFolder, filePath, child, blockStatement, fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
			List<KtCatchClause> catchClauses = tryStatement.getCatchClauses();
			for(KtCatchClause catchClause : catchClauses) {
				KtExpression catchClauseBody = catchClause.getCatchBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(ktFile, sourceFolder, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				KtParameter variableDeclaration = catchClause.getCatchParameter();
				VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, fileContent, catchClauseStatementObject.getLocationInfo());
				catchClauseStatementObject.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, variableDeclaration, CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, fileContent);
				catchClauseStatementObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getDefaultValue() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(ktFile, sourceFolder, filePath, variableDeclaration.getDefaultValue(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, fileContent);
					catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
				if(catchClauseBody instanceof KtBlockExpression catchBlock) {
					for(KtExpression blockStatement : catchBlock.getStatements()) {
						processStatement(ktFile, sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
					}
				}
				removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
			}
			KtFinallySection finallyBlock = tryStatement.getFinallyBlock();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(ktFile, sourceFolder, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, fileContent);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				finallyClauseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(finallyClauseStatementObject);
				KtBlockExpression finalExpression = finallyBlock.getFinalExpression();
				for(KtExpression blockStatement : finalExpression.getStatements()) {
					processStatement(ktFile, sourceFolder, filePath, finallyClauseStatementObject, blockStatement, fileContent);
				}
			}
		}
		else if(statement instanceof KtWhenExpression whenStatement) {
			
		}
		else if(statement instanceof KtLabeledExpression labeledStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(labeledStatement.getLabelName()), fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			if(labeledStatement.getBaseExpression() != null) {
				processStatement(ktFile, sourceFolder, filePath, child, labeledStatement.getBaseExpression(), fileContent);
			}
		}
		else if(statement instanceof KtReturnExpression returnStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtThrowExpression throwStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtBreakExpression breakStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtContinueExpression continueStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtVariableDeclaration variableDeclaration) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, variableDeclaration, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			VariableDeclaration vd = variableDeclaration instanceof KtProperty ?
					new VariableDeclaration(ktFile, sourceFolder, filePath, (KtProperty)variableDeclaration, container, activeVariableDeclarations, fileContent, parent.getLocationInfo()) :
					new VariableDeclaration(ktFile, sourceFolder, filePath, (KtDestructuringDeclarationEntry)variableDeclaration, container, activeVariableDeclarations, fileContent, parent.getLocationInfo());
			child.getVariableDeclarations().add(vd);
			addStatementInVariableScopes(child);
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
		}
		else if(!(statement instanceof KtNameReferenceExpression)) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, statement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}
}
