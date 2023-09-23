package gr.uom.java.xmi.decomposition;

import java.util.Comparator;

public class ScopedLeafMappingComparatorForInline implements Comparator<LeafMapping> {
	private AbstractCodeMapping parentMapping;
	
	public ScopedLeafMappingComparatorForInline(AbstractCodeMapping parentMapping) {
		this.parentMapping = parentMapping;
	}

	@Override
	public int compare(LeafMapping o1, LeafMapping o2) {
		if(parentMapping != null) {
			if(parentMapping.getFragment2().getLocationInfo().subsumes(o1.getFragment2().getLocationInfo()) &&
					!parentMapping.getFragment2().getLocationInfo().subsumes(o2.getFragment2().getLocationInfo())) {
				return -1;
			}
			else if(!parentMapping.getFragment2().getLocationInfo().subsumes(o1.getFragment2().getLocationInfo()) &&
					parentMapping.getFragment2().getLocationInfo().subsumes(o2.getFragment2().getLocationInfo())) {
				return 1;
			}
		}
		return o1.compareTo(o2);
	}
}
