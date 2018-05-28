package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.Set;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.StringDistance;

public class LeafMapping extends AbstractCodeMapping implements Comparable<LeafMapping> {

	public LeafMapping(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			UMLOperation operation1, UMLOperation operation2) {
		super(statement1, statement2, operation1, operation2);
	}

	@Override
	public int compareTo(LeafMapping o) {
		double distance1;
		double distance2;
		if(this.getFragment1().getString().equals(this.getFragment2().getString())) {
			distance1 = 0;
		}
		else {
			String s1 = this.getFragment1().getString().toLowerCase();
			String s2 = this.getFragment2().getString().toLowerCase();
			int distance = StringDistance.editDistance(s1, s2);
			distance1 = (double)distance/(double)Math.max(s1.length(), s2.length());
		}
		
		if(o.getFragment1().getString().equals(o.getFragment2().getString())) {
			distance2 = 0;
		}
		else {
			String s1 = o.getFragment1().getString().toLowerCase();
			String s2 = o.getFragment2().getString().toLowerCase();
			int distance = StringDistance.editDistance(s1, s2);
			distance2 = (double)distance/(double)Math.max(s1.length(), s2.length());
		}
		
		if(distance1 != distance2) {
			return Double.compare(distance1, distance2);
		}
		else {
			int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
			int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());

			if(depthDiff1 != depthDiff2) {
				return Integer.valueOf(depthDiff1).compareTo(Integer.valueOf(depthDiff2));
			}
			else {
				int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
				int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
				return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
			}
		}
	}
	
	public Set<String> callChainIntersection() {
		OperationInvocation invocation1 = this.getFragment1().invocationCoveringEntireFragment();
		OperationInvocation invocation2 = this.getFragment2().invocationCoveringEntireFragment();
		if(invocation1 != null && invocation2 != null) {
			return invocation1.callChainIntersection(invocation2);
		}
		return new LinkedHashSet<String>();
	}
}
