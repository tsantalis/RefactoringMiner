package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;

public class UMLAttributeDiff {
	private UMLAttribute removedAttribute;
	private UMLAttribute addedAttribute;
	private boolean visibilityChanged;
	private boolean typeChanged;
	private boolean qualifiedTypeChanged;
	private boolean renamed;
	private boolean staticChanged;
	private boolean finalChanged;

	public UMLAttributeDiff(UMLAttribute removedAttribute, UMLAttribute addedAttribute) {
		this.removedAttribute = removedAttribute;
		this.addedAttribute = addedAttribute;
		this.visibilityChanged = false;
		this.typeChanged = false;
		this.renamed = false;
		this.staticChanged = false;
		this.finalChanged = false;
		if(!removedAttribute.getName().equals(addedAttribute.getName()))
			renamed = true;
		if(!removedAttribute.getVisibility().equals(addedAttribute.getVisibility()))
			visibilityChanged = true;
		if(!removedAttribute.getType().equals(addedAttribute.getType()))
			typeChanged = true;
		else if(!removedAttribute.getType().equalsQualified(addedAttribute.getType()))
			qualifiedTypeChanged = true;
		if(removedAttribute.isStatic() != addedAttribute.isStatic())
			staticChanged = true;
		if(removedAttribute.isFinal() != addedAttribute.isFinal())
			finalChanged = true;
	}

	public UMLAttribute getRemovedAttribute() {
		return removedAttribute;
	}

	public UMLAttribute getAddedAttribute() {
		return addedAttribute;
	}

	public boolean isRenamed() {
		return renamed;
	}

	public boolean isVisibilityChanged() {
		return visibilityChanged;
	}

	public boolean isTypeChanged() {
		return typeChanged;
	}

	public boolean isQualifiedTypeChanged() {
		return qualifiedTypeChanged;
	}

	public boolean isEmpty() {
		return !visibilityChanged && !typeChanged && !renamed;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedAttribute).append("\n");
		if(renamed)
			sb.append("\t").append("renamed from " + removedAttribute.getName() + " to " + addedAttribute.getName()).append("\n");
		if(visibilityChanged)
			sb.append("\t").append("visibility changed from " + removedAttribute.getVisibility() + " to " + addedAttribute.getVisibility()).append("\n");
		if(typeChanged || qualifiedTypeChanged)
			sb.append("\t").append("type changed from " + removedAttribute.getType() + " to " + addedAttribute.getType()).append("\n");
		if(staticChanged)
			sb.append("\t").append("modifier changed from " + (removedAttribute.isStatic() ? "static" : "non-static") + " to " +
					(addedAttribute.isStatic() ? "static" : "non-static")).append("\n");
		if(finalChanged)
			sb.append("\t").append("modifier changed from " + (removedAttribute.isFinal() ? "final" : "non-final") + " to " +
					(addedAttribute.isFinal() ? "final" : "non-final")).append("\n");
		return sb.toString();
	}
}
