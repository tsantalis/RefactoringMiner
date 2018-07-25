package gr.uom.java.xmi;

public interface UMLClassMatcher {
	public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile);

	public static class Move implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameNameAndKind(addedClass) 
					&& (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}

	public static class RelaxedMove implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameNameAndKind(addedClass) 
					&& (removedClass.hasCommonAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}

	public static class ExtremelyRelaxedMove implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameNameAndKind(addedClass) 
					&& (removedClass.hasAttributesAndOperationsWithCommonNames(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}

	public static class Rename implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameKind(addedClass) 
					&& (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}

	public static class RelaxedRename implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameKind(addedClass) 
					&& (removedClass.hasCommonAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}

	public static class ExtremelyRelaxedRename implements UMLClassMatcher {
		public boolean match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			return removedClass.hasSameKind(addedClass) 
					&& (removedClass.hasAttributesAndOperationsWithCommonNames(addedClass) || addedClass.getSourceFile().equals(renamedFile));
		}
	}
}
