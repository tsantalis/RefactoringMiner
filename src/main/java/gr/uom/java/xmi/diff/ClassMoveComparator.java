package gr.uom.java.xmi.diff;

import java.util.Comparator;
import java.util.Set;

public class ClassMoveComparator implements Comparator<UMLClassMoveDiff> {

	@Override
	public int compare(UMLClassMoveDiff o1, UMLClassMoveDiff o2) {
		int matchedMembers1 = o1.getMatchResult().getMatchedOperations() + o1.getMatchResult().getMatchedAttributes();
		int matchedMembers2 = o2.getMatchResult().getMatchedOperations() + o2.getMatchResult().getMatchedAttributes();
		if(matchedMembers1 != matchedMembers2) {
			return -Integer.compare(matchedMembers1, matchedMembers2);
		}
		else {
			Set<String> set1 = o1.commonPackagesInQualifiedName();
			Set<String> set2 = o2.commonPackagesInQualifiedName();
			boolean sameNumberOfLines1 = o1.getOriginalClass().getLocationInfo().getCompilationUnitLength() == o1.getNextClass().getLocationInfo().getCompilationUnitLength();
			boolean sameNumberOfLines2 = o2.getOriginalClass().getLocationInfo().getCompilationUnitLength() == o2.getNextClass().getLocationInfo().getCompilationUnitLength();
			int lineNumberDifference1 = Math.abs(o1.getOriginalClass().getLocationInfo().getCompilationUnitLength() - o1.getNextClass().getLocationInfo().getCompilationUnitLength());
			int lineNumberDifference2 = Math.abs(o2.getOriginalClass().getLocationInfo().getCompilationUnitLength() - o2.getNextClass().getLocationInfo().getCompilationUnitLength());
			boolean isEmpty1 = o1.getOriginalClass().isEmpty() && o1.getNextClass().isEmpty();
			boolean isEmpty2 = o2.getOriginalClass().isEmpty() && o2.getNextClass().isEmpty();
			boolean topLevelClass1 = o1.getOriginalClass().isTopLevel() && o1.getNextClass().isTopLevel();
			boolean topLevelClass2 = o2.getOriginalClass().isTopLevel() && o2.getNextClass().isTopLevel();
			if(set1.size() != set2.size() && !isEmpty1 && !isEmpty2) {
				if(sameNumberOfLines1 == sameNumberOfLines2 && !topLevelClass1 && !topLevelClass2) {
					return -Integer.compare(set1.size(), set2.size());
				}
				if(lineNumberDifference1 == lineNumberDifference2)
					return -Integer.compare(set1.size(), set2.size());
				else {
					if(sameNumberOfLines1 && !sameNumberOfLines2) {
						return -1;
					}
					else if(!sameNumberOfLines1 && sameNumberOfLines2) {
						return 1;
					}
					else {
						return Integer.compare(lineNumberDifference1, lineNumberDifference2);
					}
				}
			}
			double sourceFolderDistance1 = !o1.getMovedClass().getSourceFolder().isEmpty() && !o1.getOriginalClass().getSourceFolder().isEmpty() ?
					o1.getMovedClass().normalizedSourceFolderDistance(o1.getOriginalClass()) : 0;
			double sourceFolderDistance2 = !o2.getMovedClass().getSourceFolder().isEmpty() && !o2.getOriginalClass().getSourceFolder().isEmpty() ?
					o2.getMovedClass().normalizedSourceFolderDistance(o2.getOriginalClass()) : 0;
			if(sourceFolderDistance1 != sourceFolderDistance2) {
				return Double.compare(sourceFolderDistance1, sourceFolderDistance2);
			}
			else {
				boolean sameNestingLevel1 = o1.getOriginalClass().isTopLevel() == o1.getMovedClass().isTopLevel();
				boolean sameNestingLevel2 = o2.getOriginalClass().isTopLevel() == o2.getMovedClass().isTopLevel();
				if(sameNestingLevel1 && !sameNestingLevel2) {
					return -1;
				}
				else if(!sameNestingLevel1 && sameNestingLevel2) {
					return 1;
				}
				boolean identicalAnnotations1 = o1.getOriginalClass().getAnnotations().equals(o1.getNextClass().getAnnotations()) &&  o1.getOriginalClass().getAnnotations().size() > 0;
				boolean identicalAnnotations2 = o2.getOriginalClass().getAnnotations().equals(o2.getNextClass().getAnnotations()) &&  o2.getOriginalClass().getAnnotations().size() > 0;
				if(identicalAnnotations1 && !identicalAnnotations2) {
					return -1;
				}
				else if(!identicalAnnotations1 && identicalAnnotations2) {
					return 1;
				}
				double packageDistance1 = o1.getMovedClass().normalizedPackageNameDistance(o1.getOriginalClass());
				double packageDistance2 = o2.getMovedClass().normalizedPackageNameDistance(o2.getOriginalClass());
				return Double.compare(packageDistance1, packageDistance2);
			}
		}
	}
}
