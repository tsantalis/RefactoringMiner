package gr.uom.java.xmi.diff;

import java.util.Comparator;

public class ClassRenameComparator implements Comparator<UMLClassRenameDiff> {

	@Override
	public int compare(UMLClassRenameDiff o1, UMLClassRenameDiff o2) {
		int matchedMembers1 = o1.getMatchResult().getMatchedOperations() + o1.getMatchResult().getMatchedAttributes();
		int matchedMembers2 = o2.getMatchResult().getMatchedOperations() + o2.getMatchResult().getMatchedAttributes();
		if(matchedMembers1 != matchedMembers2) {
			return -Integer.compare(matchedMembers1, matchedMembers2);
		}
		else {
			double nameDistance1 = o1.getRenamedClass().normalizedNameDistance(o1.getOriginalClass());
			double nameDistance2 = o2.getRenamedClass().normalizedNameDistance(o2.getOriginalClass());
			if(nameDistance1 != nameDistance2) {
				return Double.compare(nameDistance1, nameDistance2);
			}
			else {
				double packageDistance1 = o1.getRenamedClass().normalizedPackageNameDistance(o1.getOriginalClass());
				double packageDistance2 = o2.getRenamedClass().normalizedPackageNameDistance(o2.getOriginalClass());
				return Double.compare(packageDistance1, packageDistance2);
			}
		}
	}
}
