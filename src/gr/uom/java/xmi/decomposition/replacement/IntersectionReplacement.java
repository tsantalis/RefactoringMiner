package gr.uom.java.xmi.decomposition.replacement;

import java.util.ArrayList;
import java.util.List;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.diff.LeafMappingProvider;

public class IntersectionReplacement extends Replacement implements LeafMappingProvider {
	private List<LeafMapping> subExpressionMappings;

	public IntersectionReplacement(String before, String after, ReplacementType type) {
		super(before, after, type);
		this.subExpressionMappings = new ArrayList<LeafMapping>();
	}

	public List<LeafMapping> getSubExpressionMappings() {
		return subExpressionMappings;
	}

	public void addSubExpressionMapping(LeafMapping newLeafMapping) {
		boolean alreadyPresent = false; 
		for(LeafMapping oldLeafMapping : subExpressionMappings) { 
			if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) && 
					oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) { 
				alreadyPresent = true; 
				break; 
			} 
		} 
		if(!alreadyPresent) { 
			subExpressionMappings.add(newLeafMapping); 
		}
	}
}
