package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;

public abstract class AbstractCodeFragment {
	private int depth;
	private int index;

    public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public abstract String getString();
	public abstract List<String> getVariables();
	public abstract Map<String, OperationInvocation> getMethodInvocationMap();
}
