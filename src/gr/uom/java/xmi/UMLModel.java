package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLCollaborationDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;

public class UMLModel {
    private List<UMLClass> classList;
    private List<UMLDataType> dataTypeList;
    private List<UMLAssociation> associationList;
    private List<UMLDependency> dependencyList;
    private List<UMLGeneralization> generalizationList;
    private List<UMLRealization> realizationList;
    private List<UMLCollaboration> collaborationList;
    private List<UMLActor> actorList;
    private List<UMLUseCase> useCaseList;
    private List<UMLInclude> includeList;
    private List<UMLExtend> extendList;
    private Map<String, String> xmiIdTypeMap;
    private List<UMLAnonymousClass> anonymousClassList;

    public UMLModel() {
        classList = new ArrayList<UMLClass>();
        dataTypeList = new ArrayList<UMLDataType>();
        associationList = new ArrayList<UMLAssociation>();
        dependencyList = new ArrayList<UMLDependency>();
        generalizationList = new ArrayList<UMLGeneralization>();
        realizationList = new ArrayList<UMLRealization>();
        collaborationList = new ArrayList<UMLCollaboration>();
        actorList = new ArrayList<UMLActor>();
        useCaseList = new ArrayList<UMLUseCase>();
        includeList = new ArrayList<UMLInclude>();
        extendList = new ArrayList<UMLExtend>();
        xmiIdTypeMap = new LinkedHashMap<String, String>();
        anonymousClassList = new ArrayList<UMLAnonymousClass>();
    }

    public void addClass(UMLClass umlClass) {
    	if(umlClass.getXmiID() != null)
    		xmiIdTypeMap.put(umlClass.getXmiID(), umlClass.getName());
        classList.add(umlClass);
    }

    public void addDataType(UMLDataType umlDataType) {
    	if(umlDataType.getXmiID() != null)
    		xmiIdTypeMap.put(umlDataType.getXmiID(), umlDataType.getName());
    	dataTypeList.add(umlDataType);
    }

    public void addActor(UMLActor umlActor) {
    	if(umlActor.getXmiID() != null)
    		xmiIdTypeMap.put(umlActor.getXmiID(), umlActor.getName());
        actorList.add(umlActor);
    }

    public void addUseCase(UMLUseCase umlUseCase) {
    	if(umlUseCase.getXmiID() != null)
    		xmiIdTypeMap.put(umlUseCase.getXmiID(), umlUseCase.getName());
    	useCaseList.add(umlUseCase);
    }

    public void addAssociation(UMLAssociation umlAssociation) {
        associationList.add(umlAssociation);
    }

    public void addDependency(UMLDependency umlDependency) {
        dependencyList.add(umlDependency);
    }

    public void addGeneralization(UMLGeneralization umlGeneralization) {
        generalizationList.add(umlGeneralization);
    }

    public void addRealization(UMLRealization umlRealization) {
    	realizationList.add(umlRealization);
    }

    public void addCollaboration(UMLCollaboration umlCollaboration) {
    	collaborationList.add(umlCollaboration);
    }

    public void addInclude(UMLInclude umlInclude) {
    	includeList.add(umlInclude);
    }

    public void addExtend(UMLExtend umlExtend) {
    	extendList.add(umlExtend);
    }

    public void addAnonymousClass(UMLAnonymousClass anonymousClass) {
    	anonymousClassList.add(anonymousClass);
    }

    public String getElementName(String xmiID) {
        return xmiIdTypeMap.get(xmiID);
    }

    public UMLGeneralization getGeneralization(String xmiID) {
    	ListIterator<UMLGeneralization> it = generalizationList.listIterator();
    	while(it.hasNext()) {
    		UMLGeneralization generalization = it.next();
    		if(generalization.getXmiID() != null && generalization.getXmiID().equals(xmiID))
    			return generalization;
    	}
    	return null;
    }

    public UMLInclude getInclude(String xmiID) {
    	ListIterator<UMLInclude> it = includeList.listIterator();
    	while(it.hasNext()) {
    		UMLInclude include = it.next();
    		if(include.getXmiID() != null && include.getXmiID().equals(xmiID))
    			return include;
    	}
    	return null;
    }

    public UMLExtend getExtend(String xmiID) {
    	ListIterator<UMLExtend> it = extendList.listIterator();
    	while(it.hasNext()) {
    		UMLExtend extend = it.next();
    		if(extend.getXmiID() != null && extend.getXmiID().equals(xmiID))
    			return extend;
    	}
    	return null;
    }

    public UMLDependency getDependency(String xmiID) {
    	ListIterator<UMLDependency> it = dependencyList.listIterator();
    	while(it.hasNext()) {
    		UMLDependency dependency = it.next();
    		if(dependency.getXmiID() != null && dependency.getXmiID().equals(xmiID))
    			return dependency;
    	}
    	return null;
    }

    public UMLOperation getOperation(String xmiID) {
    	ListIterator<UMLClass> it = classList.listIterator();
        while(it.hasNext()) {
            UMLClass umlClass = it.next();
            UMLOperation umlOperation = umlClass.getOperation(xmiID);
            if(umlOperation != null)
            	return umlOperation;
        }
        return null;
    }

    public UMLClass getClass(String name) {
    	ListIterator<UMLClass> it = classList.listIterator();
        while(it.hasNext()) {
            UMLClass umlClass = it.next();
            if(umlClass.getName().equals(name))
                return umlClass;
        }
        return null;
    }

    public UMLCollaboration getCollaboration(String name) {
    	ListIterator<UMLCollaboration> it = collaborationList.listIterator();
    	while(it.hasNext()) {
    		UMLCollaboration umlCollaboration = it.next();
    		if(umlCollaboration.getName().equals(name))
    			return umlCollaboration;
    	}
    	return null;
    }

    public List<UMLClass> getClassList() {
        return this.classList;
    }

    public List<UMLDataType> getDataTypeList() {
    	return this.dataTypeList;
    }

    public List<UMLAssociation> getAssociationList() {
        return this.associationList;
    }

    public List<UMLDependency> getDependencyList() {
        return this.dependencyList;
    }

    public List<UMLGeneralization> getGeneralizationList() {
        return this.generalizationList;
    }

    public List<UMLRealization> getRealizationList() {
		return realizationList;
	}

    public List<UMLActor> getActorList() {
		return actorList;
	}

	public List<UMLUseCase> getUseCaseList() {
		return useCaseList;
	}

	public List<UMLInclude> getIncludeList() {
		return includeList;
	}

	public List<UMLExtend> getExtendList() {
		return extendList;
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
    	UMLModelDiff modelDiff = new UMLModelDiff();
    	for(UMLClass umlClass : classList) {
    		if(!umlModel.classList.contains(umlClass))
    			modelDiff.reportRemovedClass(umlClass);
    	}
    	for(UMLClass umlClass : umlModel.classList) {
    		if(!this.classList.contains(umlClass))
    			modelDiff.reportAddedClass(umlClass);
    	}
    	modelDiff.checkForMovedClasses();
    	modelDiff.checkForRenamedClasses();
    	for(UMLGeneralization umlGeneralization : generalizationList) {
    		if(!umlModel.generalizationList.contains(umlGeneralization))
    			modelDiff.reportRemovedGeneralization(umlGeneralization);
    	}
    	for(UMLGeneralization umlGeneralization : umlModel.generalizationList) {
    		if(!this.generalizationList.contains(umlGeneralization))
    			modelDiff.reportAddedGeneralization(umlGeneralization);
    	}
    	modelDiff.checkForGeneralizationChanges();
    	for(UMLAssociation umlAssociation : associationList) {
    		if(!umlModel.associationList.contains(umlAssociation))
    			modelDiff.reportRemovedAssociation(umlAssociation);
    	}
    	for(UMLAssociation umlAssociation : umlModel.associationList) {
    		if(!this.associationList.contains(umlAssociation))
    			modelDiff.reportAddedAssociation(umlAssociation);
    	}
    	for(UMLDependency umlDependency : dependencyList) {
    		if(!umlModel.dependencyList.contains(umlDependency))
    			modelDiff.reportRemovedDependency(umlDependency);
    	}
    	for(UMLDependency umlDependency : umlModel.dependencyList) {
    		if(!this.dependencyList.contains(umlDependency))
    			modelDiff.reportAddedDependency(umlDependency);
    	}
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
    			UMLClassDiff classDiff = umlClass.diff(umlModel.getClass(umlClass.getName()));
    			if(!classDiff.isEmpty())
    				modelDiff.addUMLClassDiff(classDiff);
    			else {
    				modelDiff.addUnchangedClass(umlClass);
    			}
    		}
    	}
    	for(UMLCollaboration umlCollaboration : collaborationList) {
    		if(umlModel.collaborationList.contains(umlCollaboration)) {
    			UMLCollaborationDiff collaborationDiff = umlCollaboration.diff(umlModel.getCollaboration(umlCollaboration.getName()));
    			if(!collaborationDiff.isEmpty())
    				modelDiff.addUMLCollaborationDiff(collaborationDiff);
    		}
    	}
    	
    	for(UMLAnonymousClass umlClass : anonymousClassList) {
    		if(!umlModel.anonymousClassList.contains(umlClass))
    			modelDiff.reportRemovedAnonymousClass(umlClass);
    	}
    	for(UMLAnonymousClass umlClass : umlModel.anonymousClassList) {
    		if(!this.anonymousClassList.contains(umlClass))
    			modelDiff.reportAddedAnonymousClass(umlClass);
    	}
    	modelDiff.checkForOperationMoves();
    	modelDiff.checkForExtractedAndMovedOperations();
    	return modelDiff;
    }
}
