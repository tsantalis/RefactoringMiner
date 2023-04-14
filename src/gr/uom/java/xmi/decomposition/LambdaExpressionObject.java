package gr.uom.java.xmi.decomposition;

import java.util.*;

import com.intellij.psi.*;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class LambdaExpressionObject implements VariableDeclarationContainer, LocationInfoProvider {
	private LocationInfo locationInfo;
	private OperationBody body;
	private AbstractExpression expression;
	private List<VariableDeclaration> parameters = new ArrayList<VariableDeclaration>();
	private List<UMLParameter> umlParameters = new ArrayList<UMLParameter>();
	private boolean hasParentheses = false;
	private VariableDeclarationContainer owner;
	
	public LambdaExpressionObject(PsiFile cu, String filePath, PsiLambdaExpression lambda, VariableDeclarationContainer owner) {
		this.owner = owner;
		this.locationInfo = new LocationInfo(cu, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
		this.hasParentheses = lambda.hasFormalParameterTypes();
		PsiParameterList params = lambda.getParameterList();
		for(PsiParameter param : params.getParameters()) {
			VariableDeclaration parameter = new VariableDeclaration(cu, filePath, param, CodeElementType.LAMBDA_EXPRESSION_PARAMETER, this);
			this.parameters.add(parameter);
			if(param.getTypeElement() != null) {
				String parameterName = param.getName();
				UMLType type = UMLTypePsiParser.extractTypeObject(cu, filePath, param.getTypeElement(), param.getType());
				UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", param.isVarArgs());
				umlParameter.setVariableDeclaration(parameter);
				this.umlParameters.add(umlParameter);
			}
		}
		if(lambda.getBody() instanceof PsiCodeBlock) {
			this.body = new OperationBody(cu, filePath, (PsiCodeBlock)lambda.getBody(), this, new ArrayList<>());
		}
		else if(lambda.getBody() instanceof PsiExpression) {
			this.expression = new AbstractExpression(cu, filePath, (PsiExpression)lambda.getBody(), CodeElementType.LAMBDA_EXPRESSION_BODY, this);
			this.expression.setLambdaOwner(this);
			for(VariableDeclaration parameter : parameters) {
				parameter.addStatementInScope(expression);
			}
		}
	}

	public LambdaExpressionObject(PsiFile cu, String filePath, PsiMethodReferenceExpression reference, VariableDeclarationContainer owner) {
		this.owner = owner;
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION);
		this.expression = new AbstractExpression(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION_BODY, this);
	}
	
	public VariableDeclarationContainer getOwner() {
		return owner;
	}

	public OperationBody getBody() {
		return body;
	}

	public AbstractExpression getExpression() {
		return expression;
	}

	public List<VariableDeclaration> getParameters() {
		return parameters;
	}

	public List<UMLParameter> getUmlParameters() {
		return umlParameters;
	}

	public List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		for(VariableDeclaration parameter : parameters) {
			parameterNameList.add(parameter.getVariableName());
		}
		return parameterNameList;
	}

	public List<UMLType> getParameterTypeList() {
		List<UMLType> parameterTypeList = new ArrayList<UMLType>();
		for(UMLParameter parameter : umlParameters) {
			parameterTypeList.add(parameter.getType());
		}
		return parameterTypeList;
	}

	public int getNumberOfNonVarargsParameters() {
		int counter = 0;
		for(UMLParameter parameter : umlParameters) {
			if(!parameter.isVarargs()) {
				counter++;
			}
		}
		return counter;
	}

	public boolean hasVarargsParameter() {
		for(UMLParameter parameter : umlParameters) {
			if(parameter.isVarargs()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((locationInfo == null) ? 0 : locationInfo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LambdaExpressionObject other = (LambdaExpressionObject) obj;
		if (locationInfo == null) {
			if (other.locationInfo != null)
				return false;
		} else if (!locationInfo.equals(other.locationInfo))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(hasParentheses) {
			sb.append("(");
		}
		for(int i=0; i<parameters.size(); i++) {
			sb.append(parameters.get(i).getVariableName());
			if(i < parameters.size()-1)
				sb.append(", ");
		}
		if(hasParentheses) {
			sb.append(")");
		}
		if(parameters.size() > 0 || hasParentheses) {
			sb.append(" -> ");
		}
		if(expression != null) {
			sb.append(expression.getString());
		}
		else if(body != null) {
			List<String> statements = body.stringRepresentation();
			for(String statement : statements) {
				sb.append(statement);
			}
		}
		return sb.toString();
	}

	@Override
	public List<VariableDeclaration> getParameterDeclarationList() {
		return getParameters();
	}

	@Override
	public List<UMLParameter> getParametersWithoutReturnType() {
		return getUmlParameters();
	}

	@Override
	public List<UMLAnonymousClass> getAnonymousClassList() {
		if(owner != null) {
			List<UMLAnonymousClass> anonymousClassList = new ArrayList<>();
			for(UMLAnonymousClass anonymousClass : owner.getAnonymousClassList()) {
				if(this.locationInfo.subsumes(anonymousClass.getLocationInfo())) {
					anonymousClassList.add(anonymousClass);
				}
			}
			return anonymousClassList;
		}
		return Collections.emptyList();
	}

	@Override
	public List<LambdaExpressionObject> getAllLambdas() {
		if(expression != null) {
			return expression.getLambdas();
		}
		if(body != null) {
			return body.getAllLambdas();
		}
		return Collections.emptyList();
	}

	@Override
	public List<AbstractCall> getAllOperationInvocations() {
		if(expression != null) {
			return new ArrayList<>(expression.getMethodInvocations());
		}
		if(body != null) {
			return body.getAllOperationInvocations();
		}
		return Collections.emptyList();
	}

	@Override
	public List<AbstractCall> getAllCreations() {
		if(expression != null) {
			return new ArrayList<>(expression.getCreations());
		}
		if(body != null) {
			return body.getAllCreations();
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getAllVariables() {
		if(expression != null) {
			List<String> variables = new ArrayList<>();
			for(LeafExpression variable : expression.getVariables()) {
				variables.add(variable.getString());
			}
			return variables;
		}
		if(body != null) {
			return body.getAllVariables();
		}
		return Collections.emptyList();
	}

	@Override
	public List<UMLComment> getComments() {
		if(owner != null) {
			List<UMLComment> comments = new ArrayList<>();
			for(UMLComment comment : owner.getComments()) {
				if(this.locationInfo.subsumes(comment.getLocationInfo())) {
					comments.add(comment);
				}
			}
			return comments;
		}
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getElementType() {
		return "lambda";
	}

	@Override
	public String getClassName() {
		if(owner != null) {
			return owner.getClassName();
		}
		return null;
	}

	@Override
	public String toQualifiedString() {
		if(owner != null) {
			return owner.toQualifiedString() + " -> @" + locationInfo;
		}
		return null;
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

	@Override
	public UMLAnonymousClass findAnonymousClass(AnonymousClassDeclarationObject anonymousClassDeclaration) {
		for(UMLAnonymousClass anonymousClass : this.getAnonymousClassList()) {
			if(anonymousClass.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo())) {
				return anonymousClass;
			}
		}
		return null;
	}

	@Override
	public boolean hasTestAnnotation() {
		return false;
	}

	@Override
	public boolean hasSetUpAnnotation() {
		return false;
	}

	@Override
	public boolean hasTearDownAnnotation() {
		return false;
	}

	@Override
	public boolean isDeclaredInAnonymousClass() {
		return false;
	}

	@Override
	public Optional<UMLAnonymousClass> getAnonymousClassContainer() {
		return Optional.empty();
	}

	@Override
	public boolean isGetter() {
		return false;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public AbstractCall isDelegate() {
		return null;
	}
}
