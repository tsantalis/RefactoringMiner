package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import gr.uom.java.xmi.diff.UMLModelDiff;

public class UMLJavadoc extends UMLAbstractDocumentation {
	private List<UMLTagElement> tags;

	public UMLJavadoc(String text, LocationInfo locationInfo) {
		super(text, locationInfo);
		this.tags = new ArrayList<UMLTagElement>();
	}

	public boolean isEmpty() {
		return tags.size() == 0;
	}

	public void addTag(UMLTagElement tag) {
		tags.add(tag);
	}

	public List<UMLTagElement> getTags() {
		return tags;
	}

	public boolean contains(String s) {
		for(UMLTagElement tag : tags) {
			if(tag.contains(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean equalText(UMLJavadoc other) {
		return this.tags.equals(other.tags);
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(this.text);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if(line.startsWith("/*")) {
				line = line.substring(2);
			}
			if(line.endsWith("*/")) {
				line = line.substring(0, line.length()-2);
			}
			if(line.startsWith("//")) {
				line = line.substring(2);
			}
			if(line.startsWith("*")) {
				line = line.substring(1);
			}
			line = line.trim();
			sb.append(line).append("\n");
		}
		scanner.close();
		return sb.toString();
	}

	public boolean refersToModifiedClass(UMLModelDiff diff) {
		if(diff != null) {
			for(UMLTagElement tag : tags) {
				for(UMLClass childModelClass : diff.getChildModel().getClassList()) {
					if(tag.contains(childModelClass.getNonQualifiedName())) {
						return true;
					}
					if(childModelClass.getSuperclass() != null && tag.contains(childModelClass.getSuperclass().getClassType())) {
						return true;
					}
					for(UMLType type : childModelClass.getImplementedInterfaces()) {
						if(tag.contains(type.getClassType())) {
							return true;
						}
					}
				}
				for(UMLClass parentModelClass : diff.getParentModel().getClassList()) {
					if(tag.contains(parentModelClass.getNonQualifiedName())) {
						return true;
					}
					if(parentModelClass.getSuperclass() != null && tag.contains(parentModelClass.getSuperclass().getClassType())) {
						return true;
					}
					for(UMLType type : parentModelClass.getImplementedInterfaces()) {
						if(tag.contains(type.getClassType())) {
							return true;
						}
					}
				}
				if(diff.getChildModel().isPartial()) {
					for(UMLClass childModelClass : diff.getChildModel().getClassList()) {
						for(UMLImport imp : childModelClass.getImportedTypes()) {
							String qualifiedName = imp.getName();
							if(qualifiedName.contains(".")) {
								String nonQualifiedName = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1, qualifiedName.length());
								if(tag.contains(nonQualifiedName)) {
									return true;
								}
							}
						}
					}
				}
				if(diff.getParentModel().isPartial()) {
					for(UMLClass parentModelClass : diff.getParentModel().getClassList()) {
						for(UMLImport imp : parentModelClass.getImportedTypes()) {
							String qualifiedName = imp.getName();
							if(qualifiedName.contains(".")) {
								String nonQualifiedName = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1, qualifiedName.length());
								if(tag.contains(nonQualifiedName)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
}
