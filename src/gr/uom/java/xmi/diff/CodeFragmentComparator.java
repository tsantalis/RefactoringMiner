package gr.uom.java.xmi.diff;

import java.util.Comparator;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;

public class CodeFragmentComparator implements Comparator<AbstractCodeFragment> {

	public int compare(AbstractCodeFragment o1, AbstractCodeFragment o2) {
		if(o1 instanceof CompositeStatementObject && !(o2 instanceof CompositeStatementObject)) {
			return -1;
		}
		else if(o2 instanceof CompositeStatementObject && !(o1 instanceof CompositeStatementObject)) {
			return 1;
		}
		else if(o1 instanceof CompositeStatementObject && o2 instanceof CompositeStatementObject){
			CompositeStatementObject comp1 = (CompositeStatementObject)o1;
			CompositeStatementObject comp2 = (CompositeStatementObject)o2;
			if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
					!comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				return 1;
			}
			else if(!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
					comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				return -1;
			}
			return 1;
		}
		return 1;
	}
}
