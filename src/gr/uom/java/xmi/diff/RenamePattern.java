package gr.uom.java.xmi.diff;

public class RenamePattern {
	private String originalPath;
	private String movedPath;
	
	public RenamePattern(String originalPath, String movedPath) {
		this.originalPath = originalPath;
		this.movedPath = movedPath;
	}

	public String getOriginalPath() {
		return originalPath;
	}

	public String getMovedPath() {
		return movedPath;
	}

	public String toString() {
		return originalPath + "\t->\t" + movedPath;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	if(o instanceof RenamePattern) {
    		RenamePattern pattern = (RenamePattern)o;
    		return this.originalPath.equals(pattern.originalPath) && this.movedPath.equals(pattern.movedPath);
    	}
    	return false;
	}
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((movedPath == null) ? 0 : movedPath.hashCode());
		result = prime * result + ((originalPath == null) ? 0 : originalPath.hashCode());
		return result;
	}
}
