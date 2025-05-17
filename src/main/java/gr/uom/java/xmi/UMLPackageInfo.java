package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLPackageInfo implements LocationInfoProvider, Serializable {
	private String packageName;
	private UMLJavadoc packageDoc;
	private UMLPackage umlPackage;
	private List<UMLComment> comments;
	private List<UMLImport> importedTypes;
	private List<UMLAnnotation> annotations;

	public UMLPackageInfo(String packageName, UMLJavadoc packageDoc, UMLPackage umlPackage,
			List<UMLComment> comments, List<UMLImport> importedTypes) {
		this.packageName = packageName;
		this.packageDoc = packageDoc;
		this.umlPackage = umlPackage;
		this.comments = comments;
		this.importedTypes = importedTypes;
		this.annotations = new ArrayList<UMLAnnotation>();
	}

	public void addAnnotation(UMLAnnotation annotation) {
		this.annotations.add(annotation);
	}

	public String getPackageName() {
		return packageName;
	}

	public UMLJavadoc getPackageDoc() {
		return packageDoc;
	}

	public UMLPackage getUmlPackage() {
		return umlPackage;
	}

	public List<UMLComment> getComments() {
		return comments;
	}

	public List<UMLImport> getImportedTypes() {
		return importedTypes;
	}

	public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return umlPackage.getLocationInfo();
	}

	@Override
	public CodeRange codeRange() {
		return umlPackage.getLocationInfo().codeRange();
	}

	@Override
	public int hashCode() {
		return Objects.hash(umlPackage);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLPackageInfo other = (UMLPackageInfo) obj;
		return Objects.equals(umlPackage, other.umlPackage);
	}
}
