package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class CompositeStatementObject extends AbstractStatement {

	private List<AbstractStatement> statementList;
	private List<AbstractExpression> expressionList;
	private List<VariableDeclaration> variableDeclarations;
	private Optional<TryStatementObject> tryContainer;
	private LocationInfo locationInfo;

	public CompositeStatementObject(PsiFile cu, String filePath, PsiElement statement, int depth, CodeElementType codeElementType) {
		super();
		this.setDepth(depth);
		this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
		this.statementList = new ArrayList<AbstractStatement>();
		this.expressionList = new ArrayList<AbstractExpression>();
		this.variableDeclarations = new ArrayList<VariableDeclaration>();
	}

	public void addStatement(AbstractStatement statement) {
		statement.setIndex(statementList.size());
		statementList.add(statement);
		statement.setParent(this);
		statement.getVariableDeclarations().stream().forEach(variableDeclaration -> {
			variableDeclaration.getScope().setParentSignature(this.getSignature());
		});
	}

	public List<AbstractStatement> getStatements() {
		return statementList;
	}

	public Optional<TryStatementObject> getTryContainer() {
		return tryContainer;
	}

	public void setTryContainer(TryStatementObject tryContainer) {
		this.tryContainer = Optional.ofNullable(tryContainer);
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

	public void addExpression(AbstractExpression expression) {
		//an expression has the same index and depth as the composite statement it belong to
		expression.setDepth(this.getDepth());
		expression.setIndex(this.getIndex());
		expressionList.add(expression);
		expression.setOwner(this);
		expression.getVariableDeclarations().stream().forEach(variableDeclaration -> {
			variableDeclaration.getScope().setParentSignature(this.getSignature());
		});
	}

	public List<AbstractExpression> getExpressions() {
		return expressionList;
	}

	public void addVariableDeclaration(VariableDeclaration declaration) {
		this.variableDeclarations.add(declaration);
		declaration.getScope().setParentSignature(this.getSignature());
	}

	@Override
	public List<AbstractCodeFragment> getLeaves() {
		List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
		for(AbstractStatement statement : statementList) {
			leaves.addAll(statement.getLeaves());
		}
		return leaves;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				innerNodes.addAll(composite.getInnerNodes());
			}
		}
		innerNodes.add(this);
		return innerNodes;
	}

	public boolean contains(AbstractCodeFragment fragment) {
		if(fragment instanceof StatementObject) {
			return getLeaves().contains(fragment);
		}
		else if(fragment instanceof CompositeStatementObject) {
			return getInnerNodes().contains(fragment);
		}
		else if(fragment instanceof AbstractExpression) {
			return getExpressions().contains(fragment);
		}
		return false;
	}

	public void resetArgumentization() {
		super.resetArgumentization();
		for(AbstractExpression expression : expressionList) {
			expression.resetArgumentization();
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(locationInfo.getCodeElementType().getName());
		if(expressionList.size() > 0) {
			sb.append("(");
			for(int i=0; i<expressionList.size()-1; i++) {
				AbstractExpression expression = expressionList.get(i);
				//special handling for the string representation of enhanced-for parameter declaration
				if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME)) {
					VariableDeclaration parameterDeclaration = this.getVariableDeclaration(expression.toString());
					if(parameterDeclaration != null) {
						if(parameterDeclaration.isFinal()) {
							sb.append("final").append(" ");
						}
						sb.append(parameterDeclaration.getVariableName()).append(": ");
					}
				}
				else {
					sb.append(expression.toString()).append("; ");
				}
			}
			AbstractExpression lastExpression = expressionList.get(expressionList.size()-1);
			if(lastExpression.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME)) {
				VariableDeclaration exceptionDeclaration = this.getVariableDeclaration(lastExpression.toString());
				if(exceptionDeclaration != null) {
					if(exceptionDeclaration.isFinal()) {
						sb.append("final").append(" ");
					}
					sb.append(exceptionDeclaration.getVariableName());
				}
			}
			else {
				sb.append(lastExpression.toString());
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String toStringForStringRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(locationInfo.getCodeElementType().getName());
		if(expressionList.size() > 0) {
			sb.append("(");
			for(int i=0; i<expressionList.size()-1; i++) {
				AbstractExpression expression = expressionList.get(i);
				//special handling for the string representation of enhanced-for parameter declaration
				if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME)) {
					VariableDeclaration parameterDeclaration = this.getVariableDeclaration(expression.toString());
					if(parameterDeclaration != null) {
						if(parameterDeclaration.isFinal()) {
							sb.append("final").append(" ");
						}
						sb.append(parameterDeclaration.getType()).append(" ");
						sb.append(parameterDeclaration.getVariableName()).append(": ");
					}
				}
				else {
					sb.append(expression.toString()).append("; ");
				}
			}
			AbstractExpression lastExpression = expressionList.get(expressionList.size()-1);
			if(lastExpression.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME)) {
				VariableDeclaration exceptionDeclaration = this.getVariableDeclaration(lastExpression.toString());
				if(exceptionDeclaration != null) {
					if(exceptionDeclaration.isFinal()) {
						sb.append("final").append(" ");
					}
					sb.append(exceptionDeclaration.getType()).append(" ");
					sb.append(exceptionDeclaration.getVariableName());
				}
			}
			else {
				sb.append(lastExpression.toString());
			}
			sb.append(")");
		}
		return sb.toString();
	}

	@Override
	public List<String> getVariables() {
		List<String> variables = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			variables.addAll(expression.getVariables());
		}
		return variables;
	}

	@Override
	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			types.addAll(expression.getTypes());
		}
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		//special handling for enhanced-for formal parameter
		variableDeclarations.addAll(this.variableDeclarations);
		for(AbstractExpression expression : expressionList) {
			variableDeclarations.addAll(expression.getVariableDeclarations());
		}
		return variableDeclarations;
	}

	@Override
	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
		Map<String, List<AbstractCall>> map = new LinkedHashMap<String, List<AbstractCall>>();
		for(AbstractExpression expression : expressionList) {
			Map<String, List<AbstractCall>> expressionMap = expression.getMethodInvocationMap();
			for(String key : expressionMap.keySet()) {
				if(map.containsKey(key)) {
					map.get(key).addAll(expressionMap.get(key));
				}
				else {
					List<AbstractCall> list = new ArrayList<AbstractCall>();
					list.addAll(expressionMap.get(key));
					map.put(key, list);
				}
			}
		}
		return map;
	}

	@Override
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
		for(AbstractExpression expression : expressionList) {
			anonymousClassDeclarations.addAll(expression.getAnonymousClassDeclarations());
		}
		return anonymousClassDeclarations;
	}

	@Override
	public List<String> getStringLiterals() {
		List<String> stringLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			stringLiterals.addAll(expression.getStringLiterals());
		}
		return stringLiterals;
	}

	@Override
	public List<String> getNumberLiterals() {
		List<String> numberLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			numberLiterals.addAll(expression.getNumberLiterals());
		}
		return numberLiterals;
	}

	@Override
	public List<String> getNullLiterals() {
		List<String> nullLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			nullLiterals.addAll(expression.getNullLiterals());
		}
		return nullLiterals;
	}

	@Override
	public List<String> getBooleanLiterals() {
		List<String> booleanLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			booleanLiterals.addAll(expression.getBooleanLiterals());
		}
		return booleanLiterals;
	}

	@Override
	public List<String> getTypeLiterals() {
		List<String> typeLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			typeLiterals.addAll(expression.getTypeLiterals());
		}
		return typeLiterals;
	}

	@Override
	public List<String> getInfixExpressions() {
		List<String> infixExpressions = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			infixExpressions.addAll(expression.getInfixExpressions());
		}
		return infixExpressions;
	}

	@Override
	public List<String> getInfixOperators() {
		List<String> infixOperators = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			infixOperators.addAll(expression.getInfixOperators());
		}
		return infixOperators;
	}

	@Override
	public List<String> getArrayAccesses() {
		List<String> arrayAccesses = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			arrayAccesses.addAll(expression.getArrayAccesses());
		}
		return arrayAccesses;
	}

	@Override
	public List<String> getPrefixExpressions() {
		List<String> prefixExpressions = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			prefixExpressions.addAll(expression.getPrefixExpressions());
		}
		return prefixExpressions;
	}

	@Override
	public List<String> getPostfixExpressions() {
		List<String> postfixExpressions = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			postfixExpressions.addAll(expression.getPostfixExpressions());
		}
		return postfixExpressions;
	}

	@Override
	public List<String> getThisExpressions() {
		List<String> thisExpressions = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			thisExpressions.addAll(expression.getThisExpressions());
		}
		return thisExpressions;
	}

	@Override
	public List<String> getArguments() {
		List<String> arguments = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			arguments.addAll(expression.getArguments());
		}
		return arguments;
	}

	@Override
	public List<String> getParenthesizedExpressions() {
		List<String> parenthesizedExpressions = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			parenthesizedExpressions.addAll(expression.getParenthesizedExpressions());
		}
		return parenthesizedExpressions;
	}

	@Override
	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
		for(AbstractExpression expression : expressionList) {
			ternaryOperatorExpressions.addAll(expression.getTernaryOperatorExpressions());
		}
		return ternaryOperatorExpressions;
	}

	@Override
	public List<LambdaExpressionObject> getLambdas() {
		List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
		for(AbstractExpression expression : expressionList) {
			lambdas.addAll(expression.getLambdas());
		}
		return lambdas;
	}

	@Override
	public Map<String, List<ObjectCreation>> getCreationMap() {
		Map<String, List<ObjectCreation>> map = new LinkedHashMap<String, List<ObjectCreation>>();
		for(AbstractExpression expression : expressionList) {
			Map<String, List<ObjectCreation>> expressionMap = expression.getCreationMap();
			for(String key : expressionMap.keySet()) {
				if(map.containsKey(key)) {
					map.get(key).addAll(expressionMap.get(key));
				}
				else {
					List<ObjectCreation> list = new ArrayList<ObjectCreation>();
					list.addAll(expressionMap.get(key));
					map.put(key, list);
				}
			}
		}
		return map;
	}

	public Map<String, List<AbstractCall>> getAllMethodInvocations() {
		Map<String, List<AbstractCall>> map = new LinkedHashMap<String, List<AbstractCall>>();
		map.putAll(getMethodInvocationMap());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				Map<String, List<AbstractCall>> compositeMap = composite.getAllMethodInvocations();
				for(String key : compositeMap.keySet()) {
					if(map.containsKey(key)) {
						map.get(key).addAll(compositeMap.get(key));
					}
					else {
						List<AbstractCall> list = new ArrayList<AbstractCall>();
						list.addAll(compositeMap.get(key));
						map.put(key, list);
					}
				}
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				Map<String, List<AbstractCall>> statementMap = statementObject.getMethodInvocationMap();
				for(String key : statementMap.keySet()) {
					if(map.containsKey(key)) {
						map.get(key).addAll(statementMap.get(key));
					}
					else {
						List<AbstractCall> list = new ArrayList<AbstractCall>();
						list.addAll(statementMap.get(key));
						map.put(key, list);
					}
				}
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					Map<String, List<AbstractCall>> lambdaMap = null;
					if(lambda.getBody() != null) {
						lambdaMap = lambda.getBody().getCompositeStatement().getAllMethodInvocations();
					}
					else if(lambda.getExpression() != null) {
						lambdaMap = new LinkedHashMap<String, List<AbstractCall>>();
						lambdaMap.putAll(lambda.getExpression().getMethodInvocationMap());
					}
					for(String key : lambdaMap.keySet()) {
						if(map.containsKey(key)) {
							map.get(key).addAll(lambdaMap.get(key));
						}
						else {
							List<AbstractCall> list = new ArrayList<AbstractCall>();
							list.addAll(lambdaMap.get(key));
							map.put(key, list);
						}
					}
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					Map<String, List<AbstractCall>> anonymousMap = anonymous.getMethodInvocationMap();
					for(String key : anonymousMap.keySet()) {
						if(map.containsKey(key)) {
							map.get(key).addAll(anonymousMap.get(key));
						}
						else {
							List<AbstractCall> list = new ArrayList<AbstractCall>();
							list.addAll(anonymousMap.get(key));
							map.put(key, list);
						}
					}
				}
			}
		}
		return map;
	}

	public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
		anonymousClassDeclarations.addAll(getAnonymousClassDeclarations());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				anonymousClassDeclarations.addAll(composite.getAllAnonymousClassDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				anonymousClassDeclarations.addAll(statementObject.getAnonymousClassDeclarations());
			}
		}
		return anonymousClassDeclarations;
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
		lambdas.addAll(getLambdas());
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

	public List<String> getAllVariables() {
		List<String> variables = new ArrayList<String>();
		variables.addAll(getVariables());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variables.addAll(composite.getAllVariables());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				variables.addAll(statementObject.getVariables());
			}
		}
		return variables;
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		variableDeclarations.addAll(getVariableDeclarations());
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

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(VariableDeclaration variableDeclaration : getAllVariableDeclarations()) {
			if(variableDeclaration.getScope().subsumes(location)) {
				variableDeclarations.add(variableDeclaration);
			}
		}
		return variableDeclarations;
	}

	@Override
	public int statementCount() {
		int count = 0;
		if(!this.getString().equals("{"))
			count++;
		for(AbstractStatement statement : statementList) {
			count += statement.statementCount();
		}
		return count;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		return null;
	}

	public Map<String, Set<String>> aliasedVariables() {
		Map<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
		for(AbstractCodeFragment statement : getLeaves()) {
			String s = statement.getString();
			if(!s.startsWith("this.") && s.endsWith(";\n")) {
				String firstLine = s.substring(0, s.indexOf("\n"));
				if(firstLine.contains("=")) {
					String variable = s.substring(0, s.indexOf("="));
					String value = s.substring(s.indexOf("=")+1, s.indexOf(";\n"));
					if(map.containsKey(value)) {
						map.get(value).add(variable);
					}
					else {
						Set<String> set = new LinkedHashSet<String>();
						set.add(variable);
						map.put(value, set);
					}
				}
			}
		}
		return map;
	}

	public Map<String, Set<String>> aliasedAttributes() {
		Map<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
		for(AbstractCodeFragment statement : getLeaves()) {
			String s = statement.getString();
			if(s.startsWith("this.") && s.endsWith(";\n")) {
				String firstLine = s.substring(0, s.indexOf("\n"));
				if(firstLine.contains("=")) {
					String attribute = s.substring(5, s.indexOf("="));
					String value = s.substring(s.indexOf("=")+1, s.indexOf(";\n"));
					if(map.containsKey(value)) {
						map.get(value).add(attribute);
					}
					else {
						Set<String> set = new LinkedHashSet<String>();
						set.add(attribute);
						map.put(value, set);
					}
				}
			}
		}
		Set<String> keysToBeRemoved = new LinkedHashSet<String>();
		for(String key : map.keySet()) {
			if(map.get(key).size() <= 1) {
				keysToBeRemoved.add(key);
			}
		}
		for(String key : keysToBeRemoved) {
			map.remove(key);
		}
		return map;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public boolean isBlock() {
		return this.locationInfo.getCodeElementType().equals(CodeElementType.BLOCK) ||
				this.locationInfo.getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) ||
				this.locationInfo.getCodeElementType().equals(CodeElementType.CATCH_CLAUSE);
	}

	public boolean isLoop() {
		return this.locationInfo.getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
				this.locationInfo.getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
				this.locationInfo.getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
				this.locationInfo.getCodeElementType().equals(CodeElementType.DO_STATEMENT);
	}

	public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		for(CompositeStatementObject innerNode : getInnerNodes()) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
				boolean currentElementNameMatched = false;
				for(VariableDeclaration declaration : innerNode.getVariableDeclarations()) {
					if(declaration.getVariableName().equals(currentElementName)) {
						currentElementNameMatched = true;
						break;
					}
				}
				boolean collectionNameMatched = false;
				for(AbstractExpression expression : innerNode.getExpressions()) {
					if(expression.getVariables().contains(collectionName)) {
						collectionNameMatched = true;
						break;
					}
				}
				if(currentElementNameMatched && collectionNameMatched) {
					return innerNode;
				}
			}
			else if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
					innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT)) {
				boolean collectionNameMatched = false;
				for(AbstractExpression expression : innerNode.getExpressions()) {
					if(expression.getVariables().contains(collectionName)) {
						collectionNameMatched = true;
						break;
					}
				}
				boolean currentElementNameMatched = false;
				for(AbstractCodeFragment statement : innerNode.getLeaves()) {
					VariableDeclaration variableDeclaration = statement.getVariableDeclaration(currentElementName);
					if(variableDeclaration != null && statement.getVariables().contains(collectionName)) {
						currentElementNameMatched = true;
						break;
					}
				}
				if(currentElementNameMatched && collectionNameMatched) {
					return innerNode;
				}
			}
		}
		return null;
	}

	@Override
	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toStringForStringRepresentation());
		for(AbstractStatement statement : statementList) {
			stringRepresentation.addAll(statement.stringRepresentation());
		}
		if(getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			stringRepresentation.add("}");
		}
		return stringRepresentation;
	}

	public List<String> bodyStringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		for(AbstractStatement statement : statementList) {
			stringRepresentation.addAll(statement.stringRepresentation());
		}
		if(getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			stringRepresentation.add("}");
		}
		return stringRepresentation;
	}

	public String getSignature() {
		String statementType = getLocationInfo().getCodeElementType().getName() != null ? getLocationInfo().getCodeElementType().getName() : toString();
		CompositeStatementObject parent = getParent();
		if (parent == null) {
			return statementType;
		}
		List<AbstractStatement> sameTypeSibling = parent.getStatements().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().getName())).collect(Collectors.toList());
		int typeIndex = 1;
		for (AbstractStatement abstractStatement : sameTypeSibling) {
			if (abstractStatement.getIndex() == getIndex()) {
				break;
			}
			typeIndex++;
		}
		return String.format("%s:%s%d", parent.getSignature(), statementType, typeIndex);
	}
}
