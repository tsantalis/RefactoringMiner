package gr.uom.java.xmi;

public enum Visibility {
	PUBLIC, PRIVATE, PROTECTED, PACKAGE, INTERNAL;

	public String toString() {
		return this.name().toLowerCase();
	}
}
