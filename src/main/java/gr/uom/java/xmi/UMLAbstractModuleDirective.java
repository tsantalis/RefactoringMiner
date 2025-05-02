package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;

import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLAbstractModuleDirective implements Serializable, LocationInfoProvider {
	private LeafExpression name;
	protected LocationInfo locationInfo;

	public UMLAbstractModuleDirective(LeafExpression name) {
		this.name = name;
	}

	public LeafExpression getName() {
		return name;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name.getString(), getClass());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLAbstractModuleDirective other = (UMLAbstractModuleDirective) obj;
		return Objects.equals(name.getString(), other.name.getString()) && Objects.equals(getClass(), other.getClass());
	}

	public boolean equalProperies(UMLAbstractModuleDirective other) {
		if(this instanceof UMLRequiresModuleDirective && other instanceof UMLRequiresModuleDirective) {
			return ((UMLRequiresModuleDirective)this).equalModifiers((UMLRequiresModuleDirective)other);
		}
		else if(this instanceof UMLProvidesModuleDirective && other instanceof UMLProvidesModuleDirective) {
			return ((UMLProvidesModuleDirective)this).equalImplementations((UMLProvidesModuleDirective)other);
		}
		else if(this instanceof UMLPackageAccessModuleDirective && other instanceof UMLPackageAccessModuleDirective) {
			return ((UMLPackageAccessModuleDirective)this).equalModules((UMLPackageAccessModuleDirective)other) &&
					((UMLPackageAccessModuleDirective)this).equalType((UMLPackageAccessModuleDirective)other);
		}
		else if(this instanceof UMLUsesModuleDirective && other instanceof UMLUsesModuleDirective) {
			return Objects.equals(name.getString(), other.name.getString());
		}
		return false;
	}
}
