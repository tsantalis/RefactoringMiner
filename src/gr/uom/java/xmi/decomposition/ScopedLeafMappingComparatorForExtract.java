package gr.uom.java.xmi.decomposition;

import java.util.Comparator;

public class ScopedLeafMappingComparatorForExtract implements Comparator<LeafMapping> {
	private AbstractCodeMapping parentMapping;
	
	public ScopedLeafMappingComparatorForExtract(AbstractCodeMapping parentMapping) {
		this.parentMapping = parentMapping;
	}

	@Override
	public int compare(LeafMapping o1, LeafMapping o2) {
		if(parentMapping != null) {
			if(parentMapping.getFragment1().getLocationInfo().subsumes(o1.getFragment1().getLocationInfo()) &&
					!parentMapping.getFragment1().getLocationInfo().subsumes(o2.getFragment1().getLocationInfo())) {
				return -1;
			}
			else if(!parentMapping.getFragment1().getLocationInfo().subsumes(o1.getFragment1().getLocationInfo()) &&
					parentMapping.getFragment1().getLocationInfo().subsumes(o2.getFragment1().getLocationInfo())) {
				return 1;
			}
		}
		return o1.compareTo(o2);
	}
}
