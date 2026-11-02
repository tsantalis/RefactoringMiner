package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLPreprocessorStatement;

public class UMLPreprocessorStatementListDiff {
	private List<UMLPreprocessorStatement> removedStatements;
	private List<UMLPreprocessorStatement> addedStatements;
	private List<Pair<UMLPreprocessorStatement, UMLPreprocessorStatement>> commonStatements;
	private List<Pair<UMLPreprocessorStatement, UMLPreprocessorStatement>> changedStatements;

	public UMLPreprocessorStatementListDiff(List<UMLPreprocessorStatement> oldStatements, List<UMLPreprocessorStatement> newStatements) {
		this.changedStatements = new ArrayList<>();
		this.commonStatements = new ArrayList<>();
		Map<UMLPreprocessorStatement, UMLPreprocessorStatement> pairedOldToNew = new IdentityHashMap<>();
		Map<UMLPreprocessorStatement, UMLPreprocessorStatement> pairedNewToOld = new IdentityHashMap<>();
		List<UMLPreprocessorStatement> oldStatementsCopy = new ArrayList<>(oldStatements);
		List<UMLPreprocessorStatement> newStatementsCopy = new ArrayList<>(newStatements);
		if(oldStatements.size() <= newStatements.size()) {
			Iterator<UMLPreprocessorStatement> it = oldStatementsCopy.iterator();
			while(it.hasNext()) {
				UMLPreprocessorStatement statement = it.next();
				int oldIndexOf = identityIndexOf(statement, oldStatementsCopy);
				int newIndexOf = indexOfCorrespondingStatement(statement, newStatementsCopy, pairedOldToNew);
				if(oldIndexOf != -1 && newIndexOf != -1) {
					UMLPreprocessorStatement oldStatement = oldStatementsCopy.get(oldIndexOf);
					UMLPreprocessorStatement newStatement = newStatementsCopy.get(newIndexOf);
					Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair = Pair.of(oldStatement, newStatement);
					if(oldStatement.getValue().isPresent() && newStatement.getValue().isPresent()) {
						if(oldStatement.getValue().get().equals(newStatement.getValue().get())) {
							UMLPreprocessorStatement anotherNewStatement = anotherEqualStatementExistsWithSameNamespace(oldStatement, newStatement, newStatementsCopy);
							if(anotherNewStatement != null) {
								pair = Pair.of(oldStatement, anotherNewStatement);
							}
							commonStatements.add(pair);
						}
						else {
							changedStatements.add(pair);
						}
					}
					else {
						UMLPreprocessorStatement anotherNewStatement = anotherEqualStatementExistsWithSameNamespace(oldStatement, newStatement, newStatementsCopy);
						if(anotherNewStatement != null) {
							pair = Pair.of(oldStatement, anotherNewStatement);
						}
						commonStatements.add(pair);
					}
					it.remove();
					pairedOldToNew.put(oldStatement, newStatementsCopy.get(newIndexOf));
					newStatementsCopy.remove(newIndexOf);
				}
			}
		}
		else if(newStatements.size() < oldStatements.size()) {
			Iterator<UMLPreprocessorStatement> it = newStatementsCopy.iterator();
			while(it.hasNext()) {
				UMLPreprocessorStatement statement = it.next();
				int oldIndexOf = indexOfCorrespondingStatement(statement, oldStatementsCopy, pairedNewToOld);
				int newIndexOf = identityIndexOf(statement, newStatementsCopy);
				if(oldIndexOf != -1 && newIndexOf != -1) {
					UMLPreprocessorStatement oldStatement = oldStatementsCopy.get(oldIndexOf);
					UMLPreprocessorStatement newStatement = newStatementsCopy.get(newIndexOf);
					Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair = Pair.of(oldStatement, newStatement);
					if(oldStatement.getValue().isPresent() && newStatement.getValue().isPresent()) {
						if(oldStatement.getValue().get().equals(newStatement.getValue().get())) {
							commonStatements.add(pair);
						}
						else {
							changedStatements.add(pair);
						}
					}
					else {
						commonStatements.add(pair);
					}
					it.remove();
					pairedNewToOld.put(newStatement, oldStatementsCopy.get(oldIndexOf));
					oldStatementsCopy.remove(oldIndexOf);
				}
			}
		}
		List<UMLPreprocessorStatement> oldToBeRemoved = new ArrayList<UMLPreprocessorStatement>();
		List<UMLPreprocessorStatement> newToBeRemoved = new ArrayList<UMLPreprocessorStatement>();
		//the remaining statements of the same type and name have a changed value, i.e., #define FEATURE_DIAGNOSTICS 1 -> #define FEATURE_DIAGNOSTICS 0
		for(UMLPreprocessorStatement oldStatement : oldStatementsCopy) {
			if(oldStatement.getName().isPresent()) {
				for(UMLPreprocessorStatement newStatement : newStatementsCopy) {
					if(newStatement.getType() == oldStatement.getType() && newStatement.getName().equals(oldStatement.getName()) && newToBeRemoved.stream().noneMatch(statement -> statement == newStatement)) {
						changedStatements.add(Pair.of(oldStatement, newStatement));
						oldToBeRemoved.add(oldStatement);
						newToBeRemoved.add(newStatement);
						break;
					}
				}
			}
		}
		removeByIdentity(oldStatementsCopy, oldToBeRemoved);
		removeByIdentity(newStatementsCopy, newToBeRemoved);
		oldToBeRemoved.clear();
		newToBeRemoved.clear();
		//check if some of the remaining statements have equal values
		for(UMLPreprocessorStatement oldStatement : oldStatementsCopy) {
			if(oldStatement.getValue().isPresent()) {
				for(UMLPreprocessorStatement newStatement : newStatementsCopy) {
					if(newStatement.getValue().isPresent()) {
						if(oldStatement.getValue().get().equals(newStatement.getValue().get())) {
							Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair = Pair.of(oldStatement, newStatement);
							changedStatements.add(pair);
							oldToBeRemoved.add(oldStatement);
							newToBeRemoved.add(newStatement);
							break;
						}
					}
				}
			}
		}
		removeByIdentity(oldStatementsCopy, oldToBeRemoved);
		removeByIdentity(newStatementsCopy, newToBeRemoved);
		//handles modified include statements, because the parent container is moved
		for(UMLPreprocessorStatement oldStatement : oldStatementsCopy) {
			if(oldStatement.getName().isPresent()) {
				String rawName1 = oldStatement.getName().get();
				if(rawName1.startsWith("../"))
					rawName1 = rawName1.replace("../", "");
				for(UMLPreprocessorStatement newStatement : newStatementsCopy) {
					if(newStatement.getName().isPresent() && !alreadyMatchedInChangedStatements(newStatement)) {
						String rawName2 = newStatement.getName().get();
						if(rawName2.startsWith("../"))
							rawName2 = rawName2.replace("../", "");
						if(rawName1.equals(rawName2) || rawName2.endsWith(rawName1) || rawName1.endsWith(rawName2)) {
							Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair = Pair.of(oldStatement, newStatement);
							changedStatements.add(pair);
							oldToBeRemoved.add(oldStatement);
							newToBeRemoved.add(newStatement);
							break;
						}
					}
				}
			}
		}
		removeByIdentity(oldStatementsCopy, oldToBeRemoved);
		removeByIdentity(newStatementsCopy, newToBeRemoved);
		this.removedStatements = oldStatementsCopy;
		this.addedStatements = newStatementsCopy;
	}

	//the statements themselves, as the directives with the same condition are equal
	private static void removeByIdentity(List<UMLPreprocessorStatement> statements, List<UMLPreprocessorStatement> toBeRemoved) {
		statements.removeIf(statement -> toBeRemoved.stream().anyMatch(removed -> removed == statement));
	}

	//the index of the statement itself, as the directives with the same condition are equal
	private static int identityIndexOf(UMLPreprocessorStatement statement, List<UMLPreprocessorStatement> statements) {
		for(int i=0; i<statements.size(); i++) {
			if(statements.get(i) == statement) {
				return i;
			}
		}
		return -1;
	}

	//the equal statement corresponding to the given one, i.e., among several #if LANG_CXX11, the one controlling the same declarations,
	//and for an #else, #elif or #endif, the one following the counterpart of its #if
	private static int indexOfCorrespondingStatement(UMLPreprocessorStatement statement, List<UMLPreprocessorStatement> candidates,
			Map<UMLPreprocessorStatement, UMLPreprocessorStatement> paired) {
		List<Integer> equalIndices = new ArrayList<>();
		for(int i=0; i<candidates.size(); i++) {
			if(candidates.get(i).equals(statement)) {
				equalIndices.add(i);
			}
		}
		if(equalIndices.size() <= 1) {
			return equalIndices.isEmpty() ? -1 : equalIndices.get(0);
		}
		if(statement.getPrevious().isPresent()) {
			UMLPreprocessorStatement counterpart = paired.get(statement.getPrevious().get());
			if(counterpart != null) {
				for(int index : equalIndices) {
					if(candidates.get(index).getPrevious().isPresent() && candidates.get(index).getPrevious().get() == counterpart) {
						return index;
					}
				}
			}
		}
		if(statement.getBlockText().isPresent()) {
			String blockText = statement.getBlockText().get();
			int best = equalIndices.get(0);
			double bestSimilarity = -1;
			for(int index : equalIndices) {
				Optional<String> candidateText = candidates.get(index).getBlockText();
				if(candidateText.isPresent()) {
					if(candidateText.get().equals(blockText)) {
						return index;
					}
					double similarity = tokenSimilarity(blockText, candidateText.get());
					if(similarity > bestSimilarity) {
						bestSimilarity = similarity;
						best = index;
					}
				}
			}
			return best;
		}
		return equalIndices.get(0);
	}

	//the Jaccard similarity of the identifier and literal tokens of two code blocks
	private static double tokenSimilarity(String text1, String text2) {
		Set<String> tokens1 = new HashSet<>(Arrays.asList(text1.split("\\W+")));
		Set<String> tokens2 = new HashSet<>(Arrays.asList(text2.split("\\W+")));
		Set<String> union = new HashSet<>(tokens1);
		union.addAll(tokens2);
		tokens1.retainAll(tokens2);
		return union.isEmpty() ? 0 : (double)tokens1.size()/union.size();
	}

	private UMLPreprocessorStatement anotherEqualStatementExistsWithSameNamespace(UMLPreprocessorStatement oldStatement, UMLPreprocessorStatement newStatement, List<UMLPreprocessorStatement> newStatements) {
		if(!newStatement.getParentNamespaces().equals(oldStatement.getParentNamespaces())) {
			for(UMLPreprocessorStatement statement : newStatements) {
				if(newStatement != statement && statement.equals(oldStatement) && statement.getParentNamespaces().equals(oldStatement.getParentNamespaces())) {
					return statement;
				}
			}
		}
		return null;
	}

	private boolean alreadyMatchedInChangedStatements(UMLPreprocessorStatement newStatement) {
		for(Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair : changedStatements) {
			if(pair.getRight() == newStatement) {
				return true;
			}
		}
		return false;
	}

	private UMLPreprocessorStatement findMatchingStatement(List<UMLPreprocessorStatement> statements, String name) {
		for(UMLPreprocessorStatement statement : statements) {
			if(statement.getName().isPresent()) {
				String rawName = statement.getName().get();
				if(rawName.startsWith("../"))
					rawName = rawName.replace("../", "");
				if(name.endsWith(rawName))
					return statement;
			}
		}
		return null;
	}

	public void findPreprocessorStatementChanges(String nameBefore, String nameAfter) {
		UMLPreprocessorStatement removedStatement = findMatchingStatement(removedStatements, nameBefore);
		UMLPreprocessorStatement addedStatement = findMatchingStatement(addedStatements, nameAfter);
		if(removedStatement != null && addedStatement != null) {
			Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> pair = Pair.of(removedStatement, addedStatement);
			changedStatements.add(pair);
			removedStatements.removeIf(statement -> statement == removedStatement);
			addedStatements.removeIf(statement -> statement == addedStatement);
		}
	}

	public List<UMLPreprocessorStatement> getRemovedStatements() {
		return removedStatements;
	}

	public List<UMLPreprocessorStatement> getAddedStatements() {
		return addedStatements;
	}

	public List<Pair<UMLPreprocessorStatement, UMLPreprocessorStatement>> getCommonStatements() {
		return commonStatements;
	}

	public List<Pair<UMLPreprocessorStatement, UMLPreprocessorStatement>> getChangedStatements() {
		return changedStatements;
	}
}
