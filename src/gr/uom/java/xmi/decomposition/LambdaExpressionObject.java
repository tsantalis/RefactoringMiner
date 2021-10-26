package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.LocationInfoProvider;

public class LambdaExpressionObject implements LocationInfoProvider {
	private LocationInfo locationInfo;
	private OperationBody body;
	private AbstractExpression expression;
	
	public LambdaExpressionObject(CompilationUnit cu, String filePath, LambdaExpression lambda) {
		this.locationInfo = new LocationInfo(cu, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
		if(lambda.getBody() instanceof Block) {
			this.body = new OperationBody(cu, filePath, (Block)lambda.getBody());
		}
		else if(lambda.getBody() instanceof Expression) {
			this.expression = new AbstractExpression(cu, filePath, (Expression)lambda.getBody(), CodeElementType.LAMBDA_EXPRESSION_BODY);
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

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
