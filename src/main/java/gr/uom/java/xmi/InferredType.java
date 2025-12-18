package gr.uom.java.xmi;

/**
 * Represents missing type from variable/property/methodReturn that is inferred by Kotlin
 * fun add(a: Int, b: Int) = a + b
 * val PI = 3.14159
 * var counter = 0
 */
public class InferredType extends UMLType {
	@Override
	public boolean equals(Object o) {
		return o instanceof InferredType;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		return "Inferred";
	}

	@Override
	public String toQualifiedString() {
		return "Inferred";
	}

	@Override
	public String getClassType() {
		return "Inferred";
	}
}
