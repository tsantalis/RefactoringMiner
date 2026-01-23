package gr.uom.java.xmi;

import java.util.Objects;

public interface UMLClassMatcher {

	public class MatchResult {
		private int identicalBodyOperations;
		private int matchedOperations;
		private int matchedAttributes;
		private int matchedCompanions;
		private int totalOperations;
		private int totalAttributes;
		private int totalCompanions;
		private boolean match;

		public MatchResult(int matchedOperations, int matchedAttributes,
				int identicalBodyOperations,
				int totalOperations, int totalAttributes,
				int matchedCompanions, int totalCompanions,
				boolean match) {
			this.matchedOperations = matchedOperations;
			this.matchedAttributes = matchedAttributes;
			this.identicalBodyOperations = identicalBodyOperations;
			this.totalOperations = totalOperations;
			this.totalAttributes = totalAttributes;
			this.matchedCompanions = matchedCompanions;
			this.totalCompanions = totalCompanions;
			this.match = match;
		}

		public int getIdenticalBodyOperations() {
			return identicalBodyOperations;
		}

		public int getMatchedOperations() {
			return matchedOperations;
		}

		public int getMatchedAttributes() {
			return matchedAttributes;
		}

		public int getTotalOperations() {
			return totalOperations;
		}

		public int getTotalAttributes() {
			return totalAttributes;
		}

		public int getMatchedCompanions() {
			return matchedCompanions;
		}

		public int getTotalCompanions() {
			return totalCompanions;
		}

		public boolean isMatch() {
			return match;
		}

		private void setMatch(boolean match) {
			this.match = match;
		}

		@Override
		public int hashCode() {
			return Objects.hash(identicalBodyOperations, match, matchedAttributes, matchedOperations, totalAttributes,
					totalOperations);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MatchResult other = (MatchResult) obj;
			return identicalBodyOperations == other.identicalBodyOperations && match == other.match
					&& matchedAttributes == other.matchedAttributes && matchedOperations == other.matchedOperations
					&& totalAttributes == other.totalAttributes && totalOperations == other.totalOperations;
		}
	}

	public MatchResult match(UMLClass removedClass, UMLClass addedClass);

	public static class Move implements UMLClassMatcher {
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasSameAttributesAndOperations(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) && matchResult.isMatch()) {
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
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasCommonAttributesAndOperations(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) && matchResult.isMatch() && !removedClass.isModule() && !addedClass.isModule()) {
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
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasAttributesAndOperationsWithCommonNames(addedClass);
			if(removedClass.hasSameNameAndKind(addedClass) && matchResult.isMatch()) {
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
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasSameAttributesAndOperations(addedClass);
			if(removedClass.hasSameKind(addedClass) && matchResult.isMatch()) {
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
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasCommonAttributesAndOperations(addedClass);
			if(removedClass.hasSameKind(addedClass) && matchResult.isMatch()) {
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
		public MatchResult match(UMLClass removedClass, UMLClass addedClass) {
			MatchResult matchResult = removedClass.hasAttributesAndOperationsWithCommonNames(addedClass);
			if(removedClass.hasSameKind(addedClass) && matchResult.isMatch()) {
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
