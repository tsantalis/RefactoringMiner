package gr.uom.java.xmi.diff;

import java.util.Comparator;

public class ClassMoveComparator implements Comparator<UMLClassMoveDiff> {

	@Override
	public int compare(UMLClassMoveDiff o1, UMLClassMoveDiff o2) {
		int matchedMembers1 = o1.getMatchResult().getMatchedOperations() + o1.getMatchResult().getMatchedAttributes();
		int matchedMembers2 = o2.getMatchResult().getMatchedOperations() + o2.getMatchResult().getMatchedAttributes();
		if(matchedMembers1 != matchedMembers2) {
			return -Integer.compare(matchedMembers1, matchedMembers2);
		}
		else {
			double sourceFolderDistance1 = o1.getMovedClass().normalizedSourceFolderDistance(o1.getOriginalClass());
			double sourceFolderDistance2 = o2.getMovedClass().normalizedSourceFolderDistance(o2.getOriginalClass());
			if(sourceFolderDistance1 != sourceFolderDistance2) {
				return Double.compare(sourceFolderDistance1, sourceFolderDistance2);
			}
			else {
				double packageDistance1 = o1.getMovedClass().normalizedPackageNameDistance(o1.getOriginalClass());
				double packageDistance2 = o2.getMovedClass().normalizedPackageNameDistance(o2.getOriginalClass());
				return Double.compare(packageDistance1, packageDistance2);
			}
		}
	}
}
