package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import gr.uom.java.xmi.UMLOperation;

public class CallTree {
	private CallTreeNode root;
	
	public CallTree(CallTreeNode root) {
		this.root = root;
	}
	
	public List<CallTreeNode> getNodesInBreadthFirstOrder() {
		List<CallTreeNode> nodes = new ArrayList<CallTreeNode>();
		List<CallTreeNode> queue = new LinkedList<CallTreeNode>();
		nodes.add(root);
		queue.add(root);
		while(!queue.isEmpty()) {
			CallTreeNode node = queue.remove(0);
			nodes.addAll(node.getChildren());
			queue.addAll(node.getChildren());
		}
		return nodes;
	}
	
	public boolean contains(UMLOperation invokedOperation) {
		for(CallTreeNode node : getNodesInBreadthFirstOrder()) {
			if(node.getInvokedOperation().equals(invokedOperation)) {
				return true;
			}
		}
		return false;
	}
}
