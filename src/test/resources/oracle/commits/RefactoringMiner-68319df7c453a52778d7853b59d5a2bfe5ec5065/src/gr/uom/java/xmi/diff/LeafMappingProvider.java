package gr.uom.java.xmi.diff;

import java.util.List;

import gr.uom.java.xmi.decomposition.LeafMapping;

public interface LeafMappingProvider {
	List<LeafMapping> getSubExpressionMappings();
	void addSubExpressionMapping(LeafMapping newLeafMapping);
}
