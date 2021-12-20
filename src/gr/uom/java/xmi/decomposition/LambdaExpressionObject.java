package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.*;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.LocationInfoProvider;

public class LambdaExpressionObject implements LocationInfoProvider {
	private LocationInfo locationInfo;
	private OperationBody body;
	private AbstractExpression expression;
	private List<VariableDeclaration> parameters = new ArrayList<VariableDeclaration>();
	private boolean hasParentheses = false;
	
	public LambdaExpressionObject(PsiFile cu, String filePath, PsiLambdaExpression lambda) {
		this.locationInfo = new LocationInfo(cu, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
		if(lambda.getBody() instanceof PsiCodeBlock) {
			this.body = new OperationBody(cu, filePath, (PsiCodeBlock)lambda.getBody());
		}
		else if(lambda.getBody() instanceof PsiExpression) {
			this.expression = new AbstractExpression(cu, filePath, (PsiExpression)lambda.getBody(), CodeElementType.LAMBDA_EXPRESSION_BODY);
		}
		this.hasParentheses = lambda.hasFormalParameterTypes();
		PsiParameterList params = lambda.getParameterList();
		for(PsiParameter param : params.getParameters()) {
			VariableDeclaration parameter = new VariableDeclaration(cu, filePath, param, CodeElementType.LAMBDA_EXPRESSION_PARAMETER);
			this.parameters.add(parameter);
		}
	}

	public LambdaExpressionObject(PsiFile cu, String filePath, PsiMethodReferenceExpression reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION);
		this.expression = new AbstractExpression(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION_BODY);
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
}
