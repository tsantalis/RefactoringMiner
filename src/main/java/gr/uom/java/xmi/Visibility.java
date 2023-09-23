package gr.uom.java.xmi;

public enum Visibility {
	PUBLIC, PRIVATE, PROTECTED, PACKAGE;

	public String toString() {
		return this.name().toLowerCase();
	}
}
