package gr.uom.java.xmi.decomposition;

import java.util.List;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import gr.uom.java.xmi.Formatter;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class AbstractExpression extends AbstractCodeFragment {
	
	private String expression;
	private LocationInfo locationInfo;
	private CompositeStatementObject owner;
	private LambdaExpressionObject lambdaOwner;
	private List<LeafExpression> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private List<AbstractCall> methodInvocations;
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	private List<LeafExpression> stringLiterals;
	private List<LeafExpression> numberLiterals;
	private List<LeafExpression> nullLiterals;
	private List<LeafExpression> booleanLiterals;
	private List<LeafExpression> typeLiterals;
	private List<AbstractCall> creations;
	private List<LeafExpression> infixExpressions;
	private List<String> infixOperators;
	private List<LeafExpression> arrayAccesses;
	private List<LeafExpression> prefixExpressions;
	private List<LeafExpression> postfixExpressions;
	private List<LeafExpression> thisExpressions;
	private List<LeafExpression> arguments;
	private List<LeafExpression> parenthesizedExpressions;
	private List<LeafExpression> castExpressions;
	private List<TernaryOperatorExpression> ternaryOperatorExpressions;
	private List<LambdaExpressionObject> lambdas;
    
    public AbstractExpression(PsiFile cu, String filePath, PsiElement expression, CodeElementType codeElementType, VariableDeclarationContainer container) {
    	this.locationInfo = new LocationInfo(cu, filePath, expression, codeElementType);
    	Visitor visitor = new Visitor(cu, filePath, container);
    	expression.accept(visitor);
		this.variables = visitor.getVariables();
		this.types = visitor.getTypes();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocations = visitor.getMethodInvocations();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.numberLiterals = visitor.getNumberLiterals();
		this.nullLiterals = visitor.getNullLiterals();
		this.booleanLiterals = visitor.getBooleanLiterals();
		this.typeLiterals = visitor.getTypeLiterals();
		this.creations = visitor.getCreations();
		this.infixExpressions = visitor.getInfixExpressions();
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.thisExpressions = visitor.getThisExpressions();
		this.arguments = visitor.getArguments();
		this.parenthesizedExpressions = visitor.getParenthesizedExpressions();
		this.castExpressions = visitor.getCastExpressions();
		this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
		this.lambdas = visitor.getLambdas();
		if(expression instanceof PsiLocalVariable) {
			String tmp = Formatter.format(expression);
			if(tmp.endsWith(";\n")) {
				this.expression = tmp.substring(0, tmp.length()-2);
			}
			else {
				this.expression = tmp;
			}
		}
		else {
			this.expression = Formatter.format(expression);
		}
    	this.owner = null;
    	this.lambdaOwner = null;
    }

    public void setOwner(CompositeStatementObject owner) {
    	this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
    	return this.owner;
    }

	public LambdaExpressionObject getLambdaOwner() {
		return lambdaOwner;
	}

	public void setLambdaOwner(LambdaExpressionObject lambdaOwner) {
		this.lambdaOwner = lambdaOwner;
	}

	@Override
	public CompositeStatementObject getParent() {
		return getOwner();
	}

    public String getExpression() {
    	return expression;
    }

	public String getString() {
    	return toString();
    }
  
	public String toString() {
		return getExpression().toString();
	}

	@Override
	public List<LeafExpression> getVariables() {
		return variables;
	}

	@Override
	public List<String> getTypes() {
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	@Override
	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
	}

	@Override
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	@Override
	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	@Override
	public List<LeafExpression> getNumberLiterals() {
		return numberLiterals;
	}

	@Override
	public List<LeafExpression> getNullLiterals() {
		return nullLiterals;
	}

	@Override
	public List<LeafExpression> getBooleanLiterals() {
		return booleanLiterals;
	}

	@Override
	public List<LeafExpression> getTypeLiterals() {
		return typeLiterals;
	}

	@Override
	public List<AbstractCall> getCreations() {
		return creations;
	}

	@Override
	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
	}

	@Override
	public List<String> getInfixOperators() {
		return infixOperators;
	}

	@Override
	public List<LeafExpression> getArrayAccesses() {
		return arrayAccesses;
	}

	@Override
	public List<LeafExpression> getPrefixExpressions() {
		return prefixExpressions;
	}

	@Override
	public List<LeafExpression> getPostfixExpressions() {
		return postfixExpressions;
	}

	@Override
	public List<LeafExpression> getThisExpressions() {
		return thisExpressions;
	}

	@Override
	public List<LeafExpression> getArguments() {
		return arguments;
	}

	@Override
	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	@Override
	public List<LeafExpression> getCastExpressions() {
		return castExpressions;
	}

	@Override
	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	@Override
	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public VariableDeclaration searchVariableDeclaration(String variableName) {
		VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
		if(variableDeclaration != null) {
			return variableDeclaration;
		}
		else if(owner != null) {
			return owner.searchVariableDeclaration(variableName);
		}
		else if(lambdaOwner != null) {
			for(VariableDeclaration declaration : lambdaOwner.getParameters()) {
				if(declaration.getVariableName().equals(variableName)) {
					return declaration;
				}
			}
		}
		return null;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		return null;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
