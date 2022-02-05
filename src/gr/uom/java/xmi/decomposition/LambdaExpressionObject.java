package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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
	
	public LambdaExpressionObject(CompilationUnit cu, String filePath, LambdaExpression lambda) {
		this.locationInfo = new LocationInfo(cu, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
		if(lambda.getBody() instanceof Block) {
			this.body = new OperationBody(cu, filePath, (Block)lambda.getBody());
		}
		else if(lambda.getBody() instanceof Expression) {
			this.expression = new AbstractExpression(cu, filePath, (Expression)lambda.getBody(), CodeElementType.LAMBDA_EXPRESSION_BODY);
		}
		this.hasParentheses = lambda.hasParentheses();
		List<org.eclipse.jdt.core.dom.VariableDeclaration> params = lambda.parameters();
		for(org.eclipse.jdt.core.dom.VariableDeclaration param : params) {
			VariableDeclaration parameter = null;
			if(param instanceof VariableDeclarationFragment) {
				parameter = new VariableDeclaration(cu, filePath, (VariableDeclarationFragment)param);
			}
			else if(param instanceof SingleVariableDeclaration) {
				parameter = new VariableDeclaration(cu, filePath, (SingleVariableDeclaration)param);
			}
			this.parameters.add(parameter);
		}
	}

	public LambdaExpressionObject(CompilationUnit cu, String filePath, ExpressionMethodReference reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION);
		this.expression = new AbstractExpression(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION_BODY);
	}

	public LambdaExpressionObject(CompilationUnit cu, String filePath, SuperMethodReference reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION);
		this.expression = new AbstractExpression(cu, filePath, reference, CodeElementType.LAMBDA_EXPRESSION_BODY);
	}

	public LambdaExpressionObject(CompilationUnit cu, String filePath, TypeMethodReference reference) {
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

	public List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		for(VariableDeclaration parameter : parameters) {
			parameterNameList.add(parameter.getVariableName());
		}
		return parameterNameList;
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
