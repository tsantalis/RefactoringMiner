package gr.uom.java.xmi.decomposition;

public class ScopedMappingData {
	private AbstractCodeMapping parentMapping;
	private AbstractCodeMapping previousMapping;
	private AbstractCodeMapping nextMapping;

	public AbstractCodeMapping getParentMapping() {
		return parentMapping;
	}
	public void setParentMapping(AbstractCodeMapping parentMapping) {
		this.parentMapping = parentMapping;
	}
	public AbstractCodeMapping getPreviousMapping() {
		return previousMapping;
	}
	public void setPreviousMapping(AbstractCodeMapping previousMapping) {
		this.previousMapping = previousMapping;
	}
	public AbstractCodeMapping getNextMapping() {
		return nextMapping;
	}
	public void setNextMapping(AbstractCodeMapping nextMapping) {
		this.nextMapping = nextMapping;
	}
	public boolean isEmpty() {
		return parentMapping == null && (previousMapping == null || nextMapping == null);
	}
}
