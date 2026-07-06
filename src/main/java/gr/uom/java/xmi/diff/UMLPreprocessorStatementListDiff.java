package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
		List<UMLPreprocessorStatement> oldStatementsCopy = new ArrayList<>(oldStatements);
		List<UMLPreprocessorStatement> newStatementsCopy = new ArrayList<>(newStatements);
		if(oldStatements.size() <= newStatements.size()) {
			Iterator<UMLPreprocessorStatement> it = oldStatementsCopy.iterator();
			while(it.hasNext()) {
				UMLPreprocessorStatement statement = it.next();
				int oldIndexOf = oldStatementsCopy.indexOf(statement);
				int newIndexOf = newStatementsCopy.indexOf(statement);
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
					newStatementsCopy.remove(newIndexOf);
				}
			}
		}
		else if(newStatements.size() < oldStatements.size()) {
			Iterator<UMLPreprocessorStatement> it = newStatementsCopy.iterator();
			while(it.hasNext()) {
				UMLPreprocessorStatement statement = it.next();
				int oldIndexOf = oldStatementsCopy.indexOf(statement);
				int newIndexOf = newStatementsCopy.indexOf(statement);
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
					oldStatementsCopy.remove(oldIndexOf);
				}
			}
		}
		//check if some of the remaining statements have equal values
		List<UMLPreprocessorStatement> oldToBeRemoved = new ArrayList<UMLPreprocessorStatement>();
		List<UMLPreprocessorStatement> newToBeRemoved = new ArrayList<UMLPreprocessorStatement>();
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
		oldStatementsCopy.removeAll(oldToBeRemoved);
		newStatementsCopy.removeAll(newToBeRemoved);
		this.removedStatements = oldStatementsCopy;
		this.addedStatements = newStatementsCopy;
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
