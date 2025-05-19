package gr.uom.java.xmi;

import java.util.Comparator;

public class ClassComparator implements Comparator<UMLClass> {

	@Override
	public int compare(UMLClass o1, UMLClass o2) {
		if(o1.isTestClass() && !o2.isTestClass()) {
			return -1;
		}
		else if(!o1.isTestClass() && o2.isTestClass()) {
			return 1;
		}
		return o1.toString().compareTo(o2.toString());
	}

}
