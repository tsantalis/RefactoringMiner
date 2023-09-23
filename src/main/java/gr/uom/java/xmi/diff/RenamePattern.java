package gr.uom.java.xmi.diff;

public class RenamePattern {
	private String before;
	private String after;
	
	public RenamePattern(String originalPath, String movedPath) {
		this.before = originalPath;
		this.after = movedPath;
	}

	public String getBefore() {
		return before;
	}

	public String getAfter() {
		return after;
	}

	public String toString() {
		return before + "\t->\t" + after;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	if(o instanceof RenamePattern) {
    		RenamePattern pattern = (RenamePattern)o;
    		return this.before.equals(pattern.before) && this.after.equals(pattern.after);
    	}
    	return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((after == null) ? 0 : after.hashCode());
		result = prime * result + ((before == null) ? 0 : before.hashCode());
		return result;
	}
	
	public RenamePattern reverse() {
		return new RenamePattern(after, before);
	}
}
