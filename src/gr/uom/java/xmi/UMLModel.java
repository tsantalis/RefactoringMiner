package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class UMLModel {
    private List<UMLClass> classList;
    private List<UMLGeneralization> generalizationList;
    private List<UMLRealization> realizationList;
    private List<UMLAnonymousClass> anonymousClassList;

    public UMLModel() {
        classList = new ArrayList<UMLClass>();
        generalizationList = new ArrayList<UMLGeneralization>();
        realizationList = new ArrayList<UMLRealization>();
        anonymousClassList = new ArrayList<UMLAnonymousClass>();
    }

	public void addClass(UMLClass umlClass) {
        classList.add(umlClass);
    }

    public void addGeneralization(UMLGeneralization umlGeneralization) {
        generalizationList.add(umlGeneralization);
    }

    public void addRealization(UMLRealization umlRealization) {
    	realizationList.add(umlRealization);
    }

    public void addAnonymousClass(UMLAnonymousClass anonymousClass) {
    	anonymousClassList.add(anonymousClass);
    }

    public UMLClass getClass(UMLClass umlClassFromOtherModel) {
    	ListIterator<UMLClass> it = classList.listIterator();
        while(it.hasNext()) {
            UMLClass umlClass = it.next();
            if(umlClass.equals(umlClassFromOtherModel))
                return umlClass;
        }
        return null;
    }

    public List<UMLClass> getClassList() {
        return this.classList;
    }

    public List<UMLGeneralization> getGeneralizationList() {
        return this.generalizationList;
    }

    public List<UMLRealization> getRealizationList() {
		return realizationList;
	}

	public UMLGeneralization matchGeneralization(UMLGeneralization otherGeneralization) {
    	ListIterator<UMLGeneralization> generalizationIt = generalizationList.listIterator();
    	while(generalizationIt.hasNext()) {
    		UMLGeneralization generalization = generalizationIt.next();
    		if(generalization.getChild().equals(otherGeneralization.getChild())) {
    			String thisParent = generalization.getParent();
    			String otherParent = otherGeneralization.getParent();
    			String thisParentComparedString = null;
    			if(thisParent.contains("."))
    				thisParentComparedString = thisParent.substring(thisParent.lastIndexOf(".")+1);
    			else
    				thisParentComparedString = thisParent;
    			String otherParentComparedString = null;
    			if(otherParent.contains("."))
    				otherParentComparedString = otherParent.substring(otherParent.lastIndexOf(".")+1);
    			else
    				otherParentComparedString = otherParent;
    			if(thisParentComparedString.equals(otherParentComparedString))
    				return generalization;
    		}
    	}
    	return null;
    }

    public UMLRealization matchRealization(UMLRealization otherRealization) {
    	ListIterator<UMLRealization> realizationIt = realizationList.listIterator();
    	while(realizationIt.hasNext()) {
    		UMLRealization realization = realizationIt.next();
    		if(realization.getClient().equals(otherRealization.getClient())) {
    			String thisSupplier = realization.getSupplier();
    			String otherSupplier = otherRealization.getSupplier();
    			String thisSupplierComparedString = null;
    			if(thisSupplier.contains("."))
    				thisSupplierComparedString = thisSupplier.substring(thisSupplier.lastIndexOf(".")+1);
    			else
    				thisSupplierComparedString = thisSupplier;
    			String otherSupplierComparedString = null;
    			if(otherSupplier.contains("."))
    				otherSupplierComparedString = otherSupplier.substring(otherSupplier.lastIndexOf(".")+1);
    			else
    				otherSupplierComparedString = otherSupplier;
    			if(thisSupplierComparedString.equals(otherSupplierComparedString))
    				return realization;
    		}
    	}
    	return null;
    }

    public UMLModelDiff diff(UMLModel umlModel) {
    	return this.diff(umlModel, Collections.<String, String>emptyMap());
    }

	public UMLModelDiff diff(UMLModel umlModel, Map<String, String> renamedFileHints) {
    	UMLModelDiff modelDiff = new UMLModelDiff();
    	for(UMLClass umlClass : classList) {
    		if(!umlModel.classList.contains(umlClass))
    			modelDiff.reportRemovedClass(umlClass);
    	}
    	for(UMLClass umlClass : umlModel.classList) {
    		if(!this.classList.contains(umlClass))
    			modelDiff.reportAddedClass(umlClass);
    	}
    	modelDiff.checkForMovedClasses(renamedFileHints);
    	modelDiff.checkForRenamedClasses(renamedFileHints);
    	for(UMLGeneralization umlGeneralization : generalizationList) {
    		if(!umlModel.generalizationList.contains(umlGeneralization))
    			modelDiff.reportRemovedGeneralization(umlGeneralization);
    	}
    	for(UMLGeneralization umlGeneralization : umlModel.generalizationList) {
    		if(!this.generalizationList.contains(umlGeneralization))
    			modelDiff.reportAddedGeneralization(umlGeneralization);
    	}
    	modelDiff.checkForGeneralizationChanges();
    	for(UMLRealization umlRealization : realizationList) {
    		if(!umlModel.realizationList.contains(umlRealization))
    			modelDiff.reportRemovedRealization(umlRealization);
    	}
    	for(UMLRealization umlRealization : umlModel.realizationList) {
    		if(!this.realizationList.contains(umlRealization))
    			modelDiff.reportAddedRealization(umlRealization);
    	}
    	modelDiff.checkForRealizationChanges();
    	for(UMLClass umlClass : classList) {
    		if(umlModel.classList.contains(umlClass)) {
    			UMLClassDiff classDiff = umlClass.diff(umlModel.getClass(umlClass));
    			if(!classDiff.isEmpty())
    				modelDiff.addUMLClassDiff(classDiff);
    			else {
//    				modelDiff.addUnchangedClass(umlClass);
    			}
    		}
    	}
    	
    	for(UMLAnonymousClass umlClass : anonymousClassList) {
    		if(!umlModel.anonymousClassList.contains(umlClass))
    			modelDiff.reportRemovedAnonymousClass(umlClass);
    	}
//    	for(UMLAnonymousClass umlClass : umlModel.anonymousClassList) {
//    		if(!this.anonymousClassList.contains(umlClass))
//    			modelDiff.reportAddedAnonymousClass(umlClass);
//    	}
    	modelDiff.checkForOperationMoves();
    	modelDiff.checkForExtractedAndMovedOperations();
    	return modelDiff;
    }
}
