package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.util.PathFileUtils;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;

public class ModuleContainer implements VariableDeclarationContainer {
	private List<AbstractStatement> statementList;
	private LocationInfo locationInfo;
	private String name;
	private String className;
	private List<UMLComment> comments = new ArrayList<>();
	//all comments of the module, including the ones distributed to the code elements of the module
	private List<UMLComment> allComments = new ArrayList<>();
	private List<UMLClass> nestedClasses = new ArrayList<>();
	//a list, as the classes are equal by name, i.e., interface Variables declared in several tests
	private List<NestedClassDeclaredInCallback> nestedClassesDeclaredInCallbacks = new ArrayList<>();
	private record NestedClassDeclaredInCallback(UMLClass nestedClass, LambdaExpressionObject callback) {}
	private List<UMLOperation> nestedOperations = new ArrayList<>();
	private List<UMLAttribute> nestedAttributes = new ArrayList<>();
	private List<UMLImport> nestedImports = new ArrayList<>();
	private List<UMLNamedExport> namedExports = new ArrayList<>();
	private List<UMLAnonymousClass> anonymousClassList = new ArrayList<>();
	private Map<String, AbstractStatement> describeMap = new LinkedHashMap<>();
	private final Constants LANG;

	public ModuleContainer(LocationInfo locationInfo, String name) {
		this.statementList = new ArrayList<>();
		this.locationInfo = locationInfo;
		this.name = name;
		this.className = name;
		this.LANG = PathFileUtils.getLang(locationInfo.getFilePath());
	}

	public Constants getLANG() {
		return LANG;
	}

	public void addStatements(List<AbstractStatement> statements) {
		for(AbstractStatement s : statements) {
			AbstractCall call = s.invocationCoveringEntireFragment();
			if(isTestCall(call)) {
				describeMap.put(call.arguments().get(0), s);
			}
			else if(call != null && call.arguments().size() > 0 && s.getLambdas().size() > 0 && s.getLambdas().get(0).getBody() != null &&
					containsDescribe(s.getLambdas().get(0).getBody().getCompositeStatement().getStatements())) {
				describeMap.put(call.arguments().get(0), s);
			}
			else {
				statementList.add(s);
			}
		}
	}

	private static boolean isTestCall(AbstractCall call) {
		return call != null && (isTestSuiteName(call.getName()) || (call.getName().equals("add") && call.getExpression() != null && call.getExpression().startsWith("storiesOf("))) && call.arguments().size() > 0;
	}

	//context(...) is the alias of describe(...) in Mocha, i.e., context('when providing a namespace', function () {...}) in store.spec.ts (compass 00209272)
	private static boolean isTestSuiteName(String name) {
		return name.startsWith("describe") || name.equals("context");
	}

	private static boolean containsDescribe(List<AbstractStatement> statements) {
		for(AbstractStatement statement : statements) {
			AbstractCall nestedCall = statement.invocationCoveringEntireFragment();
			if(isTestCall(nestedCall)) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getTestKeysRecursively() {
		Set<String> keys = new LinkedHashSet<>();
		for(String key : describeMap.keySet()) {
			keys.add(key);
			AbstractStatement statement = describeMap.get(key);
			processStatement(statement, keys);
		}
		return keys;
	}

	private static void processStatement(AbstractStatement statement, Set<String> keys) {
		Map<String, AbstractStatement> describeMap = nestedDescribeMap(statement);
		for(String key : describeMap.keySet()) {
			keys.add(key);
			AbstractStatement nestedStatement = describeMap.get(key);
			processStatement(nestedStatement, keys);
		}
		Map<String, AbstractStatement> itMap = nestedItMap(statement);
		for(String key : itMap.keySet()) {
			keys.add(key);
			AbstractStatement nestedStatement = itMap.get(key);
			processStatement(nestedStatement, keys);
		}
	}

	public static Map<String, AbstractStatement> nestedDescribeMap(AbstractStatement statement) {
		Map<String, AbstractStatement> nestedDescribeMap = new LinkedHashMap<>();
		if(statement.getLambdas().size()  > 0) {
			LambdaExpressionObject lambda = statement.getLambdas().get(0);
			if(lambda.getBody() != null) {
				List<AbstractStatement> statements = lambda.getBody().getCompositeStatement().getStatements();
				for(AbstractStatement s : statements) {
					AbstractCall call = s.invocationCoveringEntireFragment();
					if(call != null && isTestSuiteName(call.getName()) && call.arguments().size() > 0) {
						nestedDescribeMap.put(call.arguments().get(0), s);
					}
				}
			}
		}
		return nestedDescribeMap;
	}

	public static Map<String, AbstractStatement> nestedItMap(AbstractStatement statement) {
		Map<String, AbstractStatement> nestedDescribeMap = new LinkedHashMap<>();
		if(statement.getLambdas().size()  > 0) {
			LambdaExpressionObject lambda = statement.getLambdas().get(0);
			if(lambda.getBody() != null) {
				List<AbstractStatement> statements = lambda.getBody().getCompositeStatement().getStatements();
				for(AbstractStatement s : statements) {
					AbstractCall call = s.invocationCoveringEntireFragment();
					if(call != null && call.getName().startsWith("it") && call.arguments().size() > 0) {
						nestedDescribeMap.put(call.arguments().get(0), s);
					}
				}
			}
		}
		return nestedDescribeMap;
	}

	public Map<String, AbstractStatement> getDescribeMap() {
		return describeMap;
	}

	public void addComments(List<UMLComment> comments) {
		this.comments.addAll(comments);
		this.allComments.addAll(comments);
	}

	public List<UMLComment> getAllComments() {
		return allComments;
	}

	public void addNestedClass(UMLClass nestedClass) {
		this.nestedClasses.add(nestedClass);
	}

	//the types declared in the callbacks of the module statements, i.e., in describe('...', () => {...}): the ones with unique names in the file are classes of the module,
	//as the types with the same name declared in different callbacks (or functions) cannot be told apart by the diff of the classes, so the others are kept
	//in the callbacks declaring them, and are matched by their names when the callbacks are matched (i.e., interface Variables declared in several tests)
	public void addNestedClassDeclaredInCallback(UMLClass nestedClass, LambdaExpressionObject callback) {
		this.nestedClassesDeclaredInCallbacks.add(new NestedClassDeclaredInCallback(nestedClass, callback));
	}

	public boolean hasNestedClassesDeclaredInCallbacks() {
		return !nestedClassesDeclaredInCallbacks.isEmpty();
	}

	//typeDeclarations: the number of the type declarations of the file with each name, at any depth
	public void addNestedClassesDeclaredInCallbacks(Map<String, Integer> typeDeclarations) {
		for(NestedClassDeclaredInCallback declared : nestedClassesDeclaredInCallbacks) {
			if(typeDeclarations.getOrDefault(declared.nestedClass().getNonQualifiedName(), 0) == 1) {
				nestedClasses.add(declared.nestedClass());
			}
			else {
				declared.callback().addNestedClass(declared.nestedClass());
			}
		}
		nestedClassesDeclaredInCallbacks.clear();
	}

	public List<UMLClass> getNestedClasses() {
		return nestedClasses;
	}

	public void addNestedOperation(UMLOperation nested) {
		this.nestedOperations.add(nested);
	}

	public List<UMLOperation> getNestedOperations() {
		return nestedOperations;
	}

	public void addNestedAttribute(UMLAttribute nested) {
		this.nestedAttributes.add(nested);
	}

	public List<UMLAttribute> getNestedAttributes() {
		return nestedAttributes;
	}

	public void addNestedImport(UMLImport nested) {
		this.nestedImports.add(nested);
	}

	public List<UMLImport> getNestedImports() {
		return nestedImports;
	}

	public void addNamedExport(UMLNamedExport nested) {
		this.namedExports.add(nested);
	}

	public List<UMLNamedExport> getNamedExports() {
		return namedExports;
	}

	public List<AbstractStatement> getStatementList() {
		return statementList;
	}

	public List<AbstractStatement> getAllStatements() {
		List<AbstractStatement> allStatements = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			allStatements.add(statement);
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				allStatements.addAll(composite.getAllStatements());
			}
		}
		return allStatements;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				innerNodes.addAll(composite.getInnerNodes());
			}
		}
		return innerNodes;
	}

	public List<AbstractCodeFragment> getLeaves() {
		List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
		for(AbstractStatement statement : statementList) {
			leaves.addAll(statement.getLeaves());
		}
		return leaves;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public OperationBody getBody() {
		return null;
	}

	public AbstractExpression getDefaultExpression() {
		return null;
	}

	@Override
	public List<UMLAnonymousClass> getAnonymousClassList() {
		return anonymousClassList;
	}

	//the anonymous classes of a module (i.e., the object literals of a test file) are many, so they are looked up by their location
	private Map<LocationInfo, UMLAnonymousClass> anonymousClassIndex;
	private int indexedAnonymousClasses = -1;

	@Override
	public UMLAnonymousClass findAnonymousClass(AnonymousClassDeclarationObject anonymousClassDeclaration) {
		List<UMLAnonymousClass> anonymousClasses = getAnonymousClassList();
		if(anonymousClassIndex == null || indexedAnonymousClasses != anonymousClasses.size()) {
			anonymousClassIndex = new LinkedHashMap<>();
			for(UMLAnonymousClass anonymousClass : anonymousClasses) {
				anonymousClassIndex.putIfAbsent(anonymousClass.getLocationInfo(), anonymousClass);
			}
			indexedAnonymousClasses = anonymousClasses.size();
		}
		return anonymousClassIndex.get(anonymousClassDeclaration.getLocationInfo());
	}

	public List<UMLAnonymousClass> getAnonymousClassListIncludingNested() {
		List<UMLAnonymousClass> list = new ArrayList<UMLAnonymousClass>(anonymousClassList);
		for(UMLOperation nested : nestedOperations) {
			for(UMLAnonymousClass anonymous : nested.getAnonymousClassList()) {
				if(!list.contains(anonymous)) {
					list.add(anonymous);
				}
			}
		}
		return list;
	}

	@Override
	public List<LambdaExpressionObject> getAllLambdas() {
		List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				lambdas.addAll(composite.getAllLambdas());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				lambdas.addAll(statementObject.getLambdas());
			}
		}
		return lambdas;
	}

	//the statements of the module and the test suites describe('...', () => {...}), which are kept apart for matching them by their name
	private List<AbstractStatement> allStatements() {
		if(describeMap.isEmpty()) {
			return statementList;
		}
		List<AbstractStatement> statements = new ArrayList<>(statementList);
		statements.addAll(describeMap.values());
		return statements;
	}

	//the statements of the module, including the statements of the test suites describe('...', () => {...}) and the callbacks in them
	@Override
	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<>();
		for(AbstractStatement statement : allStatements()) {
			addStringRepresentation(statement, stringRepresentation);
		}
		return stringRepresentation;
	}

	private static void addStringRepresentation(AbstractStatement statement, List<String> stringRepresentation) {
		stringRepresentation.addAll(statement.stringRepresentation());
		List<LambdaExpressionObject> lambdas = statement instanceof CompositeStatementObject composite ? composite.getAllLambdas() : statement.getLambdas();
		for(LambdaExpressionObject lambda : lambdas) {
			if(lambda.getBody() != null) {
				for(AbstractStatement lambdaStatement : lambda.getBody().getCompositeStatement().getStatements()) {
					addStringRepresentation(lambdaStatement, stringRepresentation);
				}
			}
		}
	}

	@Override
	public List<AbstractCall> getAllOperationInvocations() {
		List<AbstractCall> list = new ArrayList<>();
		for(AbstractStatement statement : allStatements()) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				list.addAll(composite.getAllMethodInvocations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				list.addAll(statementObject.getMethodInvocations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					if(lambda.hasCode()) {
						list.addAll(lambda.getAllOperationInvocations());
					}
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					list.addAll(anonymous.getMethodInvocations());
				}
			}
		}
		return list;
	}

	@Override
	public List<AbstractCall> getAllCreations() {
		List<AbstractCall> list = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				list.addAll(composite.getAllCreations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				list.addAll(statementObject.getCreations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					list.addAll(lambda.getAllCreations());
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					list.addAll(anonymous.getCreations());
				}
			}
		}
		return list;
	}

	@Override
	public List<LeafExpression> getAllStringLiterals() {
		List<LeafExpression> list = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				list.addAll(composite.getAllStringLiterals());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				list.addAll(statementObject.getStringLiterals());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					list.addAll(lambda.getAllStringLiterals());
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					list.addAll(anonymous.getStringLiterals());
				}
			}
		}
		return list;
	}

	@Override
	public List<String> getAllVariables() {
		List<String> variables = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variables.addAll(composite.getAllVariables());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				for(LeafExpression variable : statementObject.getVariables()) {
					variables.add(variable.getString());
				}
			}
		}
		return variables;
	}

	@Override
	public List<UMLComment> getComments() {
		return comments;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getElementType() {
		return "module";
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getNonQualifiedClassName() {
		return className.contains(".") ? className.substring(className.lastIndexOf(".")+1, className.length()) : className;
	}

	public String toString() {
		return name;
	}

	@Override
	public String toQualifiedString() {
		return name;
	}

	@Override
	public Map<String, Set<VariableDeclaration>> variableDeclarationMap() {
		Map<String, Set<VariableDeclaration>> variableDeclarationMap = new LinkedHashMap<String, Set<VariableDeclaration>>();
		for(VariableDeclaration declaration : getAllVariableDeclarations()) {
			if(variableDeclarationMap.containsKey(declaration.getVariableName())) {
				variableDeclarationMap.get(declaration.getVariableName()).add(declaration);
			}
			else {
				Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
				variableDeclarations.add(declaration);
				variableDeclarationMap.put(declaration.getVariableName(), variableDeclarations);
			}
		}
		return variableDeclarationMap;
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(VariableDeclaration variableDeclaration : getAllVariableDeclarations()) {
			if(variableDeclaration.getScope().subsumes(location)) {
				variableDeclarations.add(variableDeclaration);
			}
		}
		return variableDeclarations;
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variableDeclarations.addAll(composite.getAllVariableDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				variableDeclarations.addAll(statementObject.getVariableDeclarations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					variableDeclarations.addAll(lambda.getParameters());
					if(lambda.getBody() != null) {
						variableDeclarations.addAll(lambda.getBody().getAllVariableDeclarations());
					}
				}
			}
		}
		return variableDeclarations;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations();
		List<VariableDeclaration> matchingDeclarations = new ArrayList<>();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				matchingDeclarations.add(declaration);
			}
		}
		if(matchingDeclarations.size() > 0) {
			return matchingDeclarations.get(matchingDeclarations.size()-1);
		}
		return null;
	}

}
