package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationProvider;
import gr.uom.java.xmi.diff.CodeRange;

public class VariableDeclaration implements LocationInfoProvider, VariableDeclarationProvider {
	private String variableName;
	private AbstractExpression initializer;
	private UMLType type;
	private boolean varargsParameter;
	private LocationInfo locationInfo;
	private boolean isParameter;
	private boolean isAttribute;
	private VariableScope scope;
	
	public VariableDeclaration(CompilationUnit cu, String filePath, VariableDeclarationFragment fragment) {
		this.locationInfo = new LocationInfo(cu, filePath, fragment, extractVariableDeclarationType(fragment));
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? new AbstractExpression(cu, filePath, fragment.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER) : null;
		Type astType = extractType(fragment);
		this.type = UMLType.extractTypeObject(astType, fragment.getExtraDimensions(), new LocationInfo(cu, filePath, astType, CodeElementType.TYPE));
		ASTNode scopeNode = getScopeNode(fragment);
		int startOffset = 0;
		if(locationInfo.getCodeElementType().equals(CodeElementType.FIELD_DECLARATION)) {
			//field declarations have the entire type declaration as scope, regardless of the location they are declared
			startOffset = scopeNode.getStartPosition();
		}
		else {
			startOffset = fragment.getStartPosition();
		}
		int endOffset = scopeNode.getStartPosition() + scopeNode.getLength();
		this.scope = new VariableScope(cu, filePath, startOffset, endOffset);
	}

	public VariableDeclaration(CompilationUnit cu, String filePath, SingleVariableDeclaration fragment) {
		this.locationInfo = new LocationInfo(cu, filePath, fragment, extractVariableDeclarationType(fragment));
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? new AbstractExpression(cu, filePath, fragment.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER) : null;
		Type astType = extractType(fragment);
		this.type = UMLType.extractTypeObject(astType, fragment.getExtraDimensions(), new LocationInfo(cu, filePath, astType, CodeElementType.TYPE));
		int startOffset = fragment.getStartPosition();
		ASTNode scopeNode = getScopeNode(fragment);
		int endOffset = scopeNode.getStartPosition() + scopeNode.getLength();
		this.scope = new VariableScope(cu, filePath, startOffset, endOffset);
	}

	public VariableDeclaration(CompilationUnit cu, String filePath, SingleVariableDeclaration fragment, boolean varargs) {
		this(cu, filePath, fragment);
		this.varargsParameter = varargs;
	}

	public String getVariableName() {
		return variableName;
	}

	public AbstractExpression getInitializer() {
		return initializer;
	}

	public UMLType getType() {
		return type;
	}

	public VariableScope getScope() {
		return scope;
	}

	public boolean isParameter() {
		return isParameter;
	}

	public void setParameter(boolean isParameter) {
		this.isParameter = isParameter;
	}

	public boolean isAttribute() {
		return isAttribute;
	}

	public void setAttribute(boolean isAttribute) {
		this.isAttribute = isAttribute;
	}

	public boolean isVarargsParameter() {
		return varargsParameter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
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
		VariableDeclaration other = (VariableDeclaration) obj;
		if (scope == null) {
			if (other.scope != null)
				return false;
		} else if (!scope.equals(other.scope))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type);
        if(varargsParameter) {
        	sb.append("...");
        }
        return sb.toString();
	}

	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type.toQualifiedString());
        if(varargsParameter) {
        	sb.append("...");
        }
        return sb.toString();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	private static ASTNode getScopeNode(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			return variableDeclaration.getParent();
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			return variableDeclaration.getParent().getParent();
		}
		return null;
	}

	private static CodeElementType extractVariableDeclarationType(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			return CodeElementType.SINGLE_VARIABLE_DECLARATION;
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				return CodeElementType.VARIABLE_DECLARATION_STATEMENT;
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				return CodeElementType.VARIABLE_DECLARATION_EXPRESSION;
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				return CodeElementType.FIELD_DECLARATION;
			}
		}
		return null;
	}

	private static Type extractType(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
		Type returnedVariableType = null;
		if(variableDeclaration instanceof SingleVariableDeclaration) {
			SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)variableDeclaration;
			returnedVariableType = singleVariableDeclaration.getType();
		}
		else if(variableDeclaration instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment)variableDeclaration;
			if(fragment.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)fragment.getParent();
				returnedVariableType = variableDeclarationStatement.getType();
			}
			else if(fragment.getParent() instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)fragment.getParent();
				returnedVariableType = variableDeclarationExpression.getType();
			}
			else if(fragment.getParent() instanceof FieldDeclaration) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration)fragment.getParent();
				returnedVariableType = fieldDeclaration.getType();
			}
		}
		return returnedVariableType;
	}

	public boolean equalVariableDeclarationType(VariableDeclaration other) {
		return this.locationInfo.getCodeElementType().equals(other.locationInfo.getCodeElementType());
	}

	public VariableDeclaration getVariableDeclaration() {
		return this;
	}
}
