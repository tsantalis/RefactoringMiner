package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLModuleDiff;
import gr.uom.java.xmi.diff.UMLPackageInfoDiff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import com.github.gumtreediff.tree.TreeContext;

public class UMLModel {
	private Set<String> repositoryDirectories;
	private List<UMLModule> moduleList;
	private List<UMLPackageInfo> packageInfoList;
    private List<UMLClass> classList;
    private List<UMLGeneralization> generalizationList;
    private List<UMLRealization> realizationList;
    private boolean partial;
    private Map<String, TreeContext> treeContextMap = new LinkedHashMap<>();
    private Map<String, List<UMLComment>> commentMap = new LinkedHashMap<>();

    public UMLModel(Set<String> repositoryDirectories) {
    	this.repositoryDirectories = repositoryDirectories;
        classList = new ArrayList<UMLClass>();
        moduleList = new ArrayList<UMLModule>();
        packageInfoList = new ArrayList<UMLPackageInfo>();
        generalizationList = new ArrayList<UMLGeneralization>();
        realizationList = new ArrayList<UMLRealization>();
    }

	public Map<String, TreeContext> getTreeContextMap() {
		return treeContextMap;
	}

	public Map<String, List<UMLComment>> getCommentMap() {
		return commentMap;
	}

	public boolean isPartial() {
		return partial;
	}

	public void setPartial(boolean partial) {
		this.partial = partial;
	}

    public void addClass(UMLClass umlClass) {
        classList.add(umlClass);
    }

    public void addModule(UMLModule umlModule) {
        moduleList.add(umlModule);
    }

    public void addPackageInfo(UMLPackageInfo umlPackageInfo) {
        packageInfoList.add(umlPackageInfo);
    }

    public void addGeneralization(UMLGeneralization umlGeneralization) {
        generalizationList.add(umlGeneralization);
    }

    public void addRealization(UMLRealization umlRealization) {
    	realizationList.add(umlRealization);
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

    public UMLPackageInfo getPackageInfo(UMLPackageInfo packageInfoFromOtherModel) {
        for(UMLPackageInfo info : packageInfoList) {
            if(info.equals(packageInfoFromOtherModel)) {
                return info;
            }
        }
        return null;
    }

    public UMLModule getModuleInfo(UMLModule moduleInfoFromOtherModel) {
        for(UMLModule info : moduleList) {
            if(info.equals(moduleInfoFromOtherModel)) {
                return info;
            }
        }
        return null;
    }

    public List<UMLClass> getClassList() {
        return this.classList;
    }

    public List<UMLModule> getModuleList() {
        return moduleList;
    }

    public List<UMLPackageInfo> getPackageInfoList() {
        return packageInfoList;
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

	public UMLModelDiff diff(UMLModel umlModel) throws RefactoringMinerTimedOutException {
    	UMLModelDiff modelDiff = new UMLModelDiff(this, umlModel);
    	for(UMLClass umlClass : classList) {
    		if(!umlModel.classList.contains(umlClass))
    			modelDiff.reportRemovedClass(umlClass);
    	}
    	for(UMLClass umlClass : umlModel.classList) {
    		if(!this.classList.contains(umlClass))
    			modelDiff.reportAddedClass(umlClass);
    	}
    	modelDiff.checkForMovedClasses(umlModel.repositoryDirectories, new UMLClassMatcher.Move());
    	modelDiff.checkForMovedClasses(umlModel.repositoryDirectories, new UMLClassMatcher.RelaxedMove());
    	modelDiff.checkForRenamedClasses(new UMLClassMatcher.Rename());
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
    	for(UMLPackageInfo packageInfo : packageInfoList) {
    		if(umlModel.packageInfoList.contains(packageInfo)) {
    			UMLPackageInfoDiff diff = new UMLPackageInfoDiff(packageInfo, umlModel.getPackageInfo(packageInfo));
    			modelDiff.addUMLPackageInfoDiff(diff);
    		}
    	}
    	for(UMLModule moduleInfo : moduleList) {
    		if(umlModel.moduleList.contains(moduleInfo)) {
    			UMLModuleDiff diff = new UMLModuleDiff(moduleInfo, umlModel.getModuleInfo(moduleInfo));
    			modelDiff.addUMLModuleDiff(diff);
    		}
    	}
    	List<UMLClass> list = new ArrayList<>(classList);
    	for(int i=0; i<classList.size(); i++) {
    		UMLClass classI = classList.get(i);
    		for(int j=i+1; j<classList.size(); j++) {
    			UMLClass classJ = classList.get(j);
    			if(classJ.explicitStaticImportOrCall(classI.getName())) {
    				int indexOfI = list.indexOf(classList.get(i));
    				int indexOfJ = list.indexOf(classList.get(j));
    				if(indexOfI < indexOfJ)
    					Collections.swap(list, indexOfI, indexOfJ);
    			}
    		}
    	}
    	for(UMLClass umlClass : list) {
    		if(umlModel.classList.contains(umlClass)) {
    			UMLClassDiff classDiff = new UMLClassDiff(umlClass, umlModel.getClass(umlClass), modelDiff);
    			classDiff.process();
    			modelDiff.addUMLClassDiff(classDiff);
    		}
    	}
    	//modelDiff.checkForMovedClasses(umlModel.repositoryDirectories, new UMLClassMatcher.RelaxedMove());
    	modelDiff.checkForRenamedClasses(new UMLClassMatcher.RelaxedRename());
    	modelDiff.inferClassRenameBasedOnFilePaths(new UMLClassMatcher.RelaxedRename());
    	modelDiff.inferClassRenameBasedOnReferencesInStringLiterals();
    	return modelDiff;
    }
}
