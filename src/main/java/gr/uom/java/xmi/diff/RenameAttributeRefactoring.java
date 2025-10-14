package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RenameAttributeRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private UMLAttribute originalAttribute;
	private UMLAttribute renamedAttribute;
	private Set<CandidateAttributeRefactoring> attributeRenames;
	private Set<AbstractCodeMapping> attributeReferences;
	private String classNameBefore;
	private String classNameAfter;

	public RenameAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute renamedAttribute,
			Set<CandidateAttributeRefactoring> attributeRenames, Set<AbstractCodeMapping> attributeReferences) {
		this.originalAttribute = originalAttribute;
		this.renamedAttribute = renamedAttribute;
		this.classNameBefore = originalAttribute.getClassName();
		this.classNameAfter = renamedAttribute.getClassName();
		this.attributeRenames = attributeRenames;
		this.attributeReferences = attributeReferences;
		for(CandidateAttributeRefactoring candidate : attributeRenames) {
			for(AbstractCodeMapping mapping : candidate.getReferences()) {
				List<LeafExpression> leafExpressions1 = mapping.getFragment1().findExpression(originalAttribute.getName());
				List<LeafExpression> leafExpressions2 = mapping.getFragment2().findExpression(renamedAttribute.getName());
				if(leafExpressions1.size() == leafExpressions2.size()) {
					for(int i=0; i<leafExpressions1.size(); i++) {
						LeafExpression leafExpression1 = leafExpressions1.get(i);
						LeafExpression leafExpression2 = leafExpressions2.get(i);
						LeafMapping leafMapping = new LeafMapping(leafExpression1, leafExpression2, candidate.getOperationBefore(), candidate.getOperationAfter());
						mapping.addSubExpressionMapping(leafMapping);
					}
				}
			}
		}
		for(AbstractCodeMapping mapping : attributeReferences) {
			List<LeafExpression> leafExpressions1 = mapping.getFragment1().findExpression(originalAttribute.getName());
			List<LeafExpression> leafExpressions2 = mapping.getFragment2().findExpression(renamedAttribute.getName());
			if(leafExpressions1.size() == leafExpressions2.size()) {
				for(int i=0; i<leafExpressions1.size(); i++) {
					LeafExpression leafExpression1 = leafExpressions1.get(i);
					LeafExpression leafExpression2 = leafExpressions2.get(i);
					LeafMapping leafMapping = new LeafMapping(leafExpression1, leafExpression2, mapping.getOperation1(), mapping.getOperation2());
					mapping.addSubExpressionMapping(leafMapping);
				}
			}
		}
	}

	public UMLAttribute getOriginalAttribute() {
		return originalAttribute;
	}

	public UMLAttribute getRenamedAttribute() {
		return renamedAttribute;
	}

	public Set<AbstractCodeMapping> getReferences() {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		for(CandidateAttributeRefactoring candidate : attributeRenames) {
			references.addAll(candidate.getReferences());
		}
		references.addAll(attributeReferences);
		return references;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		VariableDeclaration originalVariableDeclaration = originalAttribute.getVariableDeclaration();
		VariableDeclaration renamedVariableDeclaration = renamedAttribute.getVariableDeclaration();
		boolean qualified = originalVariableDeclaration.equalType(renamedVariableDeclaration) && !originalVariableDeclaration.equalQualifiedType(renamedVariableDeclaration);
		if(originalVariableDeclaration.equalType(renamedVariableDeclaration) && originalVariableDeclaration.equalQualifiedType(renamedVariableDeclaration) &&
				!originalAttribute.toQualifiedString().equals(originalAttribute.toString())) {
			qualified = true;
		}
		sb.append(qualified ? originalVariableDeclaration.toQualifiedString() : originalVariableDeclaration.toString());
		sb.append(" to ");
		sb.append(qualified ? renamedVariableDeclaration.toQualifiedString() : renamedVariableDeclaration.toString());
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((originalAttribute == null || originalAttribute.getVariableDeclaration() == null) ? 0 : originalAttribute.getVariableDeclaration().hashCode());
		result = prime * result + ((renamedAttribute == null || renamedAttribute.getVariableDeclaration() == null) ? 0 : renamedAttribute.getVariableDeclaration().hashCode());
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
		RenameAttributeRefactoring other = (RenameAttributeRefactoring) obj;
		if (classNameAfter == null) {
			if (other.classNameAfter != null)
				return false;
		} else if (!classNameAfter.equals(other.classNameAfter))
			return false;
		if (classNameBefore == null) {
			if (other.classNameBefore != null)
				return false;
		} else if (!classNameBefore.equals(other.classNameBefore))
			return false;
		if (originalAttribute == null) {
			if (other.originalAttribute != null)
				return false;
		} else if(originalAttribute.getVariableDeclaration() == null) {
			if(other.originalAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!originalAttribute.getVariableDeclaration().equals(other.originalAttribute.getVariableDeclaration()))
			return false;
		if (renamedAttribute == null) {
			if (other.renamedAttribute != null)
				return false;
		} else if(renamedAttribute.getVariableDeclaration() == null) {
			if(other.renamedAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!renamedAttribute.getVariableDeclaration().equals(other.renamedAttribute.getVariableDeclaration()))
			return false;
		return true;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalAttribute().getLocationInfo().getFilePath(), getClassNameBefore()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getRenamedAttribute().getLocationInfo().getFilePath(), getClassNameAfter()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalAttribute.getVariableDeclaration().codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(originalAttribute.getVariableDeclaration().toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedAttribute.getVariableDeclaration().codeRange()
				.setDescription("renamed attribute declaration")
				.setCodeElement(renamedAttribute.getVariableDeclaration().toString()));
		return ranges;
	}
}
