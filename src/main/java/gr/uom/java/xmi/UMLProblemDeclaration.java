package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLProblemDeclaration implements Serializable, LocationInfoProvider, CommentProvider {
	private String packageName;
	private String sourceFolder;
	private LocationInfo location;
	private String signature;
	private String fullText;
	private boolean functionDefinition;
	private boolean functionDeclarator;
	private boolean classDeclaration;
	private List<UMLComment> comments;

	public UMLProblemDeclaration(String packageName, String sourceFolder, LocationInfo location, String signature, String fullText, boolean functionDefinition, boolean functionDeclarator, boolean classDeclaration) {
		this.packageName = packageName;
		this.sourceFolder = sourceFolder;
		this.location = location;
		this.signature = signature;
		this.fullText = fullText;
		this.functionDefinition = functionDefinition;
		this.functionDeclarator = functionDeclarator;
		this.classDeclaration = classDeclaration;
		this.comments = new ArrayList<UMLComment>();
	}

	public boolean isClassDeclaration() {
		return classDeclaration;
	}

	public boolean isFunctionDeclarator() {
		return functionDeclarator;
	}

	public boolean isFunctionDefinition() {
		return functionDefinition;
	}

	public String getFullText() {
		return fullText;
	}

	public String toString() {
		return packageName + "." + signature;
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, signature, sourceFolder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLProblemDeclaration other = (UMLProblemDeclaration) obj;
		return Objects.equals(packageName, other.packageName) && Objects.equals(signature, other.signature)
				&& Objects.equals(sourceFolder, other.sourceFolder);
	}

	@Override
	public List<UMLComment> getComments() {
		return comments;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return location;
	}

	@Override
	public CodeRange codeRange() {
		return location.codeRange();
	}
}
