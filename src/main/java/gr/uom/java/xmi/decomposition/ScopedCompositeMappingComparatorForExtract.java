package gr.uom.java.xmi.decomposition;

import java.util.Comparator;

public class ScopedCompositeMappingComparatorForExtract implements Comparator<CompositeStatementObjectMapping> {
	private AbstractCodeMapping parentMapping;
	
	public ScopedCompositeMappingComparatorForExtract(AbstractCodeMapping parentMapping) {
		this.parentMapping = parentMapping;
	}

	@Override
	public int compare(CompositeStatementObjectMapping o1, CompositeStatementObjectMapping o2) {
		if(parentMapping != null) {
			if(parentMapping.getFragment1().getLocationInfo().subsumes(o1.getFragment1().getLocationInfo()) &&
					!parentMapping.getFragment1().equals(o1.getFragment1()) &&
					!parentMapping.getFragment1().getLocationInfo().subsumes(o2.getFragment1().getLocationInfo())) {
				return -1;
			}
			else if(!parentMapping.getFragment1().getLocationInfo().subsumes(o1.getFragment1().getLocationInfo()) &&
					parentMapping.getFragment1().getLocationInfo().subsumes(o2.getFragment1().getLocationInfo()) &&
					!parentMapping.getFragment1().equals(o2.getFragment1())) {
				return 1;
			}
		}
		return o1.compareTo(o2);
	}
}
