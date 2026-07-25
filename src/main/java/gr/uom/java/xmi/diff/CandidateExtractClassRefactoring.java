package gr.uom.java.xmi.diff;

import java.util.List;

import gr.uom.java.xmi.UMLType;

public class CandidateExtractClassRefactoring implements Comparable<CandidateExtractClassRefactoring> {
	private UMLClassBaseDiff classDiff;
	private ExtractClassRefactoring refactoring;
	private List<UMLClassMoveDiff> classMoveDiffList;
	
	public CandidateExtractClassRefactoring(UMLClassBaseDiff classDiff, ExtractClassRefactoring refactoring, List<UMLClassMoveDiff> classMoveDiffList) {
		this.classDiff = classDiff;
		this.refactoring = refactoring;
		this.classMoveDiffList = classMoveDiffList;
	}
	
	public boolean innerClassExtract() {
		String movedClassName = null;
		for(UMLClassMoveDiff classMoveDiff : classMoveDiffList) {
			if(classMoveDiff.getOriginalClass().getName().equals(refactoring.getOriginalClass().getName())) {
				movedClassName = classMoveDiff.getMovedClass().getName();
				break;
			}
		}
		if(refactoring.getExtractedClass().getName().startsWith(refactoring.getOriginalClass().getName() + ".")) {
			return true;
		}
		else if(movedClassName != null && refactoring.getExtractedClass().getName().startsWith(movedClassName + ".")) {
			return true;
		}
		return false;
	}

	public boolean sameFileExtract() {
		return this.refactoring.getExtractedClass().getSourceFile().equals(this.classDiff.getNextClass().getSourceFile());
	}

	public boolean subclassExtract() {
		UMLType thisSuperType = this.refactoring.getExtractedClass().getSuperclass();
		return thisSuperType != null && this.classDiff.getNextClassName().endsWith("." + thisSuperType.getClassType());
	}

	public UMLClassBaseDiff getClassDiff() {
		return classDiff;
	}

	public ExtractClassRefactoring getRefactoring() {
		return refactoring;
	}

	@Override
	public int compareTo(CandidateExtractClassRefactoring o) {
		if(this.sameFileExtract() && !o.sameFileExtract())
			return -1;
		else if(!this.sameFileExtract() && o.sameFileExtract())
			return 1;
		double sourceFolderDistance1 = this.refactoring.getExtractedClass().normalizedSourceFolderDistance(this.classDiff.getNextClass());
		double sourceFolderDistance2 = o.refactoring.getExtractedClass().normalizedSourceFolderDistance(o.classDiff.getNextClass());
		if(sourceFolderDistance1 != sourceFolderDistance2) {
			return Double.compare(sourceFolderDistance1, sourceFolderDistance2);
		}
		else {
			if(this.innerClassExtract() && !o.innerClassExtract()) {
				return -1;
			}
			else if(o.innerClassExtract() && !this.innerClassExtract()) {
				return 1;
			}
			if(this.subclassExtract() && !o.subclassExtract()) {
				return -1;
			}
			else if(o.subclassExtract() && !this.subclassExtract()) {
				return 1;
			}
			return this.classDiff.compareTo(o.classDiff);
		}
	}
}
