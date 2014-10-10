package gr.uom.java.xmi.decomposition;

public abstract class Replacement {
	private String before;
	private String after;
	
	public Replacement(String before, String after) {
		this.before = before;
		this.after = after;
	}

	public String getBefore() {
		return before;
	}

	public String getAfter() {
		return after;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((after == null) ? 0 : after.hashCode());
		result = prime * result + ((before == null) ? 0 : before.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(obj instanceof Replacement) {
			Replacement other = (Replacement)obj;
			return this.before.equals(other.before) && this.after.equals(other.after);
		}
		return false;
	}
	
	public String toString() {
		return before + " -> " + after;
	}
}
