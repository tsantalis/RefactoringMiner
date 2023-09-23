package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class MoveAndRenameClassRefactoring implements PackageLevelRefactoring {

	private UMLClass originalClass;
	private UMLClass renamedClass;
	
	public MoveAndRenameClassRefactoring(UMLClass originalClass,  UMLClass renamedClass) {
		this.originalClass = originalClass;
		this.renamedClass = renamedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass.getName());
		sb.append(" moved and renamed to ");
		sb.append(renamedClass.getName());
		return sb.toString();
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getPackageName(), renamedClass.getPackageName());
		if (separatorPos == -1) {
			return new RenamePattern(originalClass.getPackageName(), renamedClass.getPackageName());
		}
		String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
		String movedPath = renamedClass.getName().substring(0, renamedClass.getName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath);
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getRenamedClassName() {
		return renamedClass.getName();
	}

	public String getMovedClassName() {
		return getRenamedClassName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getRenamedClass() {
		return renamedClass;
	}

	public UMLClass getMovedClass() {
		return getRenamedClass();
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getRenamedClass().getLocationInfo().getFilePath(), getRenamedClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalClass.codeRange()
				.setDescription("original type declaration")
				.setCodeElement(originalClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedClass.codeRange()
				.setDescription("moved and renamed type declaration")
				.setCodeElement(renamedClass.getName()));
		return ranges;
	}
}
