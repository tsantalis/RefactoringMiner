package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class ExtractAttributeRefactoring implements Refactoring, ReferenceBasedRefactoring, LeafMappingProvider {
	private UMLAttribute attributeDeclaration;
	private UMLAbstractClass originalClass;
	private UMLAbstractClass nextClass;
	private Set<AbstractCodeMapping> references;
	private List<LeafMapping> subExpressionMappings;
	private boolean insideExtractedOrInlinedMethod;
	private List<UMLAnonymousClassDiff> anonymousClassDiffList;

	public ExtractAttributeRefactoring(UMLAttribute variableDeclaration, UMLAbstractClass originalClass, UMLAbstractClass nextClass,
			boolean insideExtractedOrInlinedMethod) {
		this.attributeDeclaration = variableDeclaration;
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
		this.subExpressionMappings = new ArrayList<LeafMapping>();
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
		this.anonymousClassDiffList = new ArrayList<>();
	}

	public List<Refactoring> addReference(AbstractCodeMapping mapping, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		references.add(mapping);
		List<Refactoring> allRefactorings = new ArrayList<>();
		List<UMLAnonymousClass> attributeAnonymousClassList = attributeDeclaration.getAnonymousClassList();
		List<AnonymousClassDeclarationObject> fragmentAnonymousClassDeclarations = new ArrayList<>();
		recursivelyCollectAnonymousClassDeclarations(fragmentAnonymousClassDeclarations, mapping.getFragment1().getAnonymousClassDeclarations());
		if(attributeAnonymousClassList.size() > 0 && fragmentAnonymousClassDeclarations.size() > 0 &&
				attributeAnonymousClassList.size() == fragmentAnonymousClassDeclarations.size()) {
			for(int i=0; i<attributeAnonymousClassList.size(); i++) {
				UMLAnonymousClass after = attributeAnonymousClassList.get(i);
				UMLAnonymousClass before = mapping.getOperation1().findAnonymousClass(fragmentAnonymousClassDeclarations.get(i));
				if(before != null && after != null) {
					UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(before, after, classDiff, modelDiff);
					anonymousClassDiff.process();
					List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
					if(matchedOperationMappers.size() > 0) {
						anonymousClassDiffList.add(anonymousClassDiff);
						List<Refactoring> anonymousClassDiffRefactorings = anonymousClassDiff.getRefactorings();
						allRefactorings.addAll(anonymousClassDiffRefactorings);
						if(classDiff != null && classDiff.getRemovedAnonymousClasses().contains(before)) {
							classDiff.getRemovedAnonymousClasses().remove(before);
						}
						if(classDiff != null && classDiff.getAddedAnonymousClasses().contains(after)) {
							classDiff.getAddedAnonymousClasses().remove(after);
						}
					}
				}
			}
		}
		return allRefactorings;
	}

	public void addReferences(Set<AbstractCodeMapping> mappings) {
		references.addAll(mappings);
	}

	private static void recursivelyCollectAnonymousClassDeclarations(List<AnonymousClassDeclarationObject> all, List<AnonymousClassDeclarationObject> current) {
		for(AnonymousClassDeclarationObject anonymous : current) {
			recursivelyCollectAnonymousClassDeclarations(all, anonymous.getAnonymousClassDeclarations());
			all.add(anonymous);
		}
	}

	public void addSubExpressionMapping(LeafMapping newLeafMapping) {
		boolean alreadyPresent = false; 
		for(LeafMapping oldLeafMapping : subExpressionMappings) { 
			if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) && 
					oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) { 
				alreadyPresent = true; 
				break; 
			} 
		} 
		if(!alreadyPresent) { 
			subExpressionMappings.add(newLeafMapping); 
		}
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLAttribute getVariableDeclaration() {
		return attributeDeclaration;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public List<LeafMapping> getSubExpressionMappings() {
		return subExpressionMappings;
	}

	public List<UMLAnonymousClassDiff> getAnonymousClassDiffList() {
		return anonymousClassDiffList;
	}

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(attributeDeclaration);
		sb.append(" in class ");
		sb.append(attributeDeclaration.getClassName());
		return sb.toString();
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return attributeDeclaration.codeRange();
	}

	public UMLAbstractClass getOriginalClass() {
		return originalClass;
	}

	public UMLAbstractClass getNextClass() {
		return nextClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeDeclaration.getVariableDeclaration() == null) ? 0 : attributeDeclaration.getVariableDeclaration().hashCode());
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
		ExtractAttributeRefactoring other = (ExtractAttributeRefactoring) obj;
		if (attributeDeclaration == null) {
			if (other.attributeDeclaration != null)
				return false;
		} else if (!attributeDeclaration.getVariableDeclaration().equals(other.attributeDeclaration.getVariableDeclaration()))
			return false;
		return true;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getNextClass().getLocationInfo().getFilePath(), getNextClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment1().codeRange().setDescription("statement with the initializer of the extracted attribute"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeDeclaration.codeRange()
				.setDescription("extracted attribute declaration")
				.setCodeElement(attributeDeclaration.toString()));
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment2().codeRange().setDescription("statement with the name of the extracted attribute"));
		}
		return ranges;
	}
}
