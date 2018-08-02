package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.CodeRange;

public class VariableDeclaration implements LocationInfoProvider {
	private String variableName;
	private String initializer;
	private UMLType type;
	private LocationInfo locationInfo;
	
	public VariableDeclaration(CompilationUnit cu, String filePath, VariableDeclarationFragment fragment) {
		this.locationInfo = new LocationInfo(cu, filePath, fragment);
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? fragment.getInitializer().toString() : null;
		this.type = UMLType.extractTypeObject(UMLType.getTypeName(extractType(fragment), fragment.getExtraDimensions()));
	}

	public VariableDeclaration(CompilationUnit cu, String filePath, SingleVariableDeclaration fragment) {
		this.locationInfo = new LocationInfo(cu, filePath, fragment);
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? fragment.getInitializer().toString() : null;
		this.type = UMLType.extractTypeObject(UMLType.getTypeName(extractType(fragment), fragment.getExtraDimensions()));
	}

	public String getVariableName() {
		return variableName;
	}

	public String getInitializer() {
		return initializer;
	}

	public UMLType getType() {
		return type;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableDeclaration other = (VariableDeclaration) obj;
		if (initializer == null) {
			if (other.initializer != null)
				return false;
		} else if (!initializer.equals(other.initializer))
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
        sb.append(variableName);
        if(initializer != null) {
        	sb.append("=").append(initializer);
        }
        return sb.toString();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
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
}
