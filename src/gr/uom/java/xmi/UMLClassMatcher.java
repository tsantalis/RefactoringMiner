package gr.uom.java.xmi;

public interface UMLClassMatcher {

	public class MatchResult {
		private int matchedOperations;
		private int matchedAttributes;
		private boolean match;

		public MatchResult(int matchedOperations, int matchedAttributes, boolean match) {
			this.matchedOperations = matchedOperations;
			this.matchedAttributes = matchedAttributes;
			this.match = match;
		}

		public int getMatchedOperations() {
			return matchedOperations;
		}

		public int getMatchedAttributes() {
			return matchedAttributes;
		}

		public boolean isMatch() {
			return match;
		}

		private void setMatch(boolean match) {
			this.match = match;
		}
	}

	public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile);

	public static class Move implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasSameAttributesAndOperations(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}

	public static class RelaxedMove implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasCommonAttributesAndOperations(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}

	public static class ExtremelyRelaxedMove implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasAttributesAndOperationsWithCommonNames(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}

	public static class Rename implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasSameAttributesAndOperations(addedClass);
			if(removedClass.hasSameKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}

	public static class RelaxedRename implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasCommonAttributesAndOperations(addedClass);
			if(removedClass.hasSameKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}

	public static class ExtremelyRelaxedRename implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass, String renamedFile) {
			MatchResult matchResult = removedClass.hasAttributesAndOperationsWithCommonNames(addedClass);
			if(removedClass.hasSameKind(addedClass) 
					&& (matchResult.isMatch() || addedClass.getSourceFile().equals(renamedFile))) {
				matchResult.setMatch(true);
				return matchResult;
			}
			else {
				matchResult.setMatch(false);
				return matchResult;
			}
		}
	}
}
