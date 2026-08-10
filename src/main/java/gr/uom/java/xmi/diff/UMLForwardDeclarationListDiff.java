package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLForwardDeclaration;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class UMLForwardDeclarationListDiff {
	private Set<UMLForwardDeclaration> removedDeclarations;
	private Set<UMLForwardDeclaration> addedDeclarations;
	private Set<Pair<UMLForwardDeclaration, UMLForwardDeclaration>> commonDeclarations;
	private List<UMLOperationBodyMapper> operationBodyMapperList;

	public UMLForwardDeclarationListDiff(List<UMLForwardDeclaration> oldDeclarations, List<UMLForwardDeclaration> newDeclarations, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.commonDeclarations = new LinkedHashSet<>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		Set<UMLForwardDeclaration> oldDeclarationSet = new LinkedHashSet<>(oldDeclarations);
		Set<UMLForwardDeclaration> newDeclarationSet = new LinkedHashSet<>(newDeclarations);
		Set<UMLForwardDeclaration> intersection = new LinkedHashSet<>();
		intersection.addAll(oldDeclarationSet);
		intersection.retainAll(newDeclarationSet);
		for(UMLForwardDeclaration declaration : intersection) {
			UMLForwardDeclaration oldDeclaration = oldDeclarations.get(oldDeclarations.indexOf(declaration));
			UMLForwardDeclaration newDeclaration = newDeclarations.get(newDeclarations.indexOf(declaration));
			if(oldDeclaration.getFunction().isPresent() && newDeclaration.getFunction().isPresent()) {
				UMLOperation oldOperation = oldDeclaration.getFunction().get();
				UMLOperation newOperation = newDeclaration.getFunction().get();
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(oldOperation, newOperation, classDiff);
				operationBodyMapperList.add(mapper);
			}
			Pair<UMLForwardDeclaration, UMLForwardDeclaration> pair = Pair.of(oldDeclaration, newDeclaration);
			commonDeclarations.add(pair);
		}
		oldDeclarationSet.removeAll(intersection);
		this.removedDeclarations = oldDeclarationSet;
		newDeclarationSet.removeAll(intersection);
		this.addedDeclarations = newDeclarationSet;
	}

	public Set<UMLForwardDeclaration> getRemovedDeclarations() {
		return removedDeclarations;
	}

	public Set<UMLForwardDeclaration> getAddedDeclarations() {
		return addedDeclarations;
	}

	public Set<Pair<UMLForwardDeclaration, UMLForwardDeclaration>> getCommonDeclarations() {
		return commonDeclarations;
	}

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}
}
