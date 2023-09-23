package gr.uom.java.xmi.decomposition;

import java.util.Comparator;

public class UMLOperationBodyMapperComparator implements Comparator<UMLOperationBodyMapper> {

	@Override
	public int compare(UMLOperationBodyMapper o1, UMLOperationBodyMapper o2) {
		if(o1.involvesTestMethods() && o2.involvesTestMethods())
			return o1.compareTo(o2);
		int thisOperationNameEditDistance = o1.operationNameEditDistance();
		int otherOperationNameEditDistance = o2.operationNameEditDistance();
		if(thisOperationNameEditDistance != otherOperationNameEditDistance)
			return Integer.compare(thisOperationNameEditDistance, otherOperationNameEditDistance);
		else
			return o1.compareTo(o2);
	}

}
