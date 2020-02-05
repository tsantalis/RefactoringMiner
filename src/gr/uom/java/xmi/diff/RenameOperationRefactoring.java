package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class RenameOperationRefactoring implements Refactoring {
	private UMLOperation originalOperation;
	private UMLOperation renamedOperation;
	private Set<Replacement> replacements;
	private UMLOperationBodyMapper bodyMapper;
	
	public RenameOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.bodyMapper = bodyMapper;
		this.originalOperation = bodyMapper.getOperation1();
		this.renamedOperation = bodyMapper.getOperation2();
		this.replacements = bodyMapper.getReplacements();
	}

	public RenameOperationRefactoring(UMLOperation originalOperation, UMLOperation renamedOperation) {
		this.originalOperation = originalOperation;
		this.renamedOperation = renamedOperation;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalOperation);
		sb.append(" renamed to ");
		sb.append(renamedOperation);
		sb.append(" in class ").append(getClassName());
		return sb.toString();
	}

	private String getClassName() {
		String sourceClassName = originalOperation.getClassName();
		String targetClassName = renamedOperation.getClassName();
		boolean targetIsAnonymousInsideSource = false;
		if(targetClassName.startsWith(sourceClassName + ".")) {
			String targetClassNameSuffix = targetClassName.substring(sourceClassName.length() + 1, targetClassName.length());
			targetIsAnonymousInsideSource = isNumeric(targetClassNameSuffix);
		}
		return sourceClassName.equals(targetClassName) || targetIsAnonymousInsideSource ? sourceClassName : targetClassName;
	}

	private static boolean isNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) return false;
		}
		return true;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_METHOD;
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public UMLOperation getOriginalOperation() {
		return originalOperation;
	}

	public UMLOperation getRenamedOperation() {
		return renamedOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	/**
	 * @return the code range of the source method in the <b>parent</b> commit
	 */
	public CodeRange getSourceOperationCodeRangeBeforeRename() {
		return originalOperation.codeRange();
	}

	/**
	 * @return the code range of the target method in the <b>child</b> commit
	 */
	public CodeRange getTargetOperationCodeRangeAfterRename() {
		return renamedOperation.codeRange();
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalOperation().getLocationInfo().getFilePath(), getOriginalOperation().getClassName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getRenamedOperation().getLocationInfo().getFilePath(), getRenamedOperation().getClassName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalOperation.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(originalOperation.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedOperation.codeRange()
				.setDescription("renamed method declaration")
				.setCodeElement(renamedOperation.toString()));
		return ranges;
	}
}
