package gr.uom.java.xmi.diff;

import java.util.Comparator;

public class ClassRenameComparator implements Comparator<UMLClassRenameDiff> {

	@Override
	public int compare(UMLClassRenameDiff o1, UMLClassRenameDiff o2) {
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
