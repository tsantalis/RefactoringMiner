package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLGeneralization;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLRealization;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.refactoringminer.api.Refactoring;

public class UMLModelDiff {
   private List<UMLClass> addedClasses;
   private List<UMLClass> removedClasses;
   
   private List<UMLGeneralization> addedGeneralizations;
   private List<UMLGeneralization> removedGeneralizations;
   private List<UMLGeneralizationDiff> generalizationDiffList;
   private List<UMLRealization> addedRealizations;
   private List<UMLRealization> removedRealizations;
   private List<UMLRealizationDiff> realizationDiffList;
   
   private List<UMLClassDiff> commonClassDiffList;
   private List<UMLClassMoveDiff> classMoveDiffList;
   private List<UMLClassMoveDiff> innerClassMoveDiffList;
   private List<UMLClassRenameDiff> classRenameDiffList;
   private List<Refactoring> refactorings;
   
   public UMLModelDiff() {
      this.addedClasses = new ArrayList<UMLClass>();
      this.removedClasses = new ArrayList<UMLClass>();
      this.addedGeneralizations = new ArrayList<UMLGeneralization>();
      this.removedGeneralizations = new ArrayList<UMLGeneralization>();
      this.generalizationDiffList = new ArrayList<UMLGeneralizationDiff>();
      this.realizationDiffList = new ArrayList<UMLRealizationDiff>();
      this.addedRealizations = new ArrayList<UMLRealization>();
      this.removedRealizations = new ArrayList<UMLRealization>();
      this.commonClassDiffList = new ArrayList<UMLClassDiff>();
      this.classMoveDiffList = new ArrayList<UMLClassMoveDiff>();
      this.innerClassMoveDiffList = new ArrayList<UMLClassMoveDiff>();
      this.classRenameDiffList = new ArrayList<UMLClassRenameDiff>();
      this.refactorings = new ArrayList<Refactoring>();
   }

   public void reportAddedClass(UMLClass umlClass) {
	   if(!addedClasses.contains(umlClass))
		   this.addedClasses.add(umlClass);
   }

   public void reportRemovedClass(UMLClass umlClass) {
	   if(!removedClasses.contains(umlClass))
		   this.removedClasses.add(umlClass);
   }

   public void reportAddedGeneralization(UMLGeneralization umlGeneralization) {
      this.addedGeneralizations.add(umlGeneralization);
   }

   public void reportRemovedGeneralization(UMLGeneralization umlGeneralization) {
      this.removedGeneralizations.add(umlGeneralization);
   }

   public void reportAddedRealization(UMLRealization umlRealization) {
      this.addedRealizations.add(umlRealization);
   }

   public void reportRemovedRealization(UMLRealization umlRealization) {
      this.removedRealizations.add(umlRealization);
   }

   public void addUMLClassDiff(UMLClassDiff classDiff) {
      this.commonClassDiffList.add(classDiff);
   }

   private UMLClassDiff getUMLClassDiff(String className) {
      for(UMLClassDiff classDiff : commonClassDiffList) {
         if(classDiff.getClassName().equals(className))
            return classDiff;
      }
      return null;
   }

   private UMLClassDiff getUMLClassDiff(UMLType type) {
      for(UMLClassDiff classDiff : commonClassDiffList) {
         if(classDiff.getClassName().endsWith("." + type.getClassType()))
            return classDiff;
      }
      return null;
   }

   private boolean isSubclassOf(String subclass, String finalSuperclass) {
      UMLClassDiff subclassDiff = getUMLClassDiff(subclass);
      if(subclassDiff == null) {
    	  subclassDiff = getUMLClassDiff(UMLType.extractTypeObject(subclass));
      }
      if(subclassDiff != null) {
         UMLType superclass = subclassDiff.getSuperclass();
         if(superclass != null) {
            if(looksLikeSameType(superclass.getClassType(), finalSuperclass))
               return true;
            else
               return isSubclassOf(superclass.getClassType(), finalSuperclass);
         }
         else if(subclassDiff.getOldSuperclass() != null && subclassDiff.getNewSuperclass() != null &&
        		 !subclassDiff.getOldSuperclass().equals(subclassDiff.getNewSuperclass()) && looksLikeAddedClass(subclassDiff.getNewSuperclass()) != null) {
        	 UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
        	 if(addedClass.getSuperclass() != null)
        		 return looksLikeSameType(addedClass.getSuperclass().getClassType(), finalSuperclass);
        	 else
        		 return false;
         }
         else
            return false;
      }
      return false;
   }

   private UMLClass looksLikeAddedClass(UMLType type) {
	   for(UMLClass umlClass : addedClasses) {
	         if(umlClass.getName().endsWith("." + type.getClassType())) {
	        	 return umlClass;
	         }
	   }
	   return null;
   }

   public boolean isAddedClass(String className) {
      for(UMLClass umlClass : addedClasses) {
         if(umlClass.getName().equals(className))
            return true;
      }
      return false;
   }

   public boolean isRemovedClass(String className) {
      for(UMLClass umlClass : removedClasses) {
         if(umlClass.getName().equals(className))
            return true;
      }
      return false;
   }

   public String isRenamedClass(UMLClass umlClass) {
      for(UMLClassRenameDiff renameDiff : classRenameDiffList) {
         if(renameDiff.getOriginalClass().equals(umlClass))
            return renameDiff.getRenamedClass().getName();
      }
      return null;
   }

   public String isMovedClass(UMLClass umlClass) {
      for(UMLClassMoveDiff moveDiff : classMoveDiffList) {
         if(moveDiff.getOriginalClass().equals(umlClass))
            return moveDiff.getMovedClass().getName();
      }
      return null;
   }

   public void checkForGeneralizationChanges() {
      for(Iterator<UMLGeneralization> removedGeneralizationIterator = removedGeneralizations.iterator(); removedGeneralizationIterator.hasNext();) {
         UMLGeneralization removedGeneralization = removedGeneralizationIterator.next();
         for(Iterator<UMLGeneralization> addedGeneralizationIterator = addedGeneralizations.iterator(); addedGeneralizationIterator.hasNext();) {
            UMLGeneralization addedGeneralization = addedGeneralizationIterator.next();
            String renamedChild = isRenamedClass(removedGeneralization.getChild());
            String movedChild = isMovedClass(removedGeneralization.getChild());
            if(removedGeneralization.getChild().equals(addedGeneralization.getChild())) {
               UMLGeneralizationDiff generalizationDiff = new UMLGeneralizationDiff(removedGeneralization, addedGeneralization);
               addedGeneralizationIterator.remove();
               removedGeneralizationIterator.remove();
               generalizationDiffList.add(generalizationDiff);
               break;
            }
            if( (renamedChild != null && renamedChild.equals(addedGeneralization.getChild().getName())) ||
                  (movedChild != null && movedChild.equals(addedGeneralization.getChild().getName()))) {
               UMLGeneralizationDiff generalizationDiff = new UMLGeneralizationDiff(removedGeneralization, addedGeneralization);
               addedGeneralizationIterator.remove();
               removedGeneralizationIterator.remove();
               generalizationDiffList.add(generalizationDiff);
               break;
            }
         }
      }
   }

   public void checkForRealizationChanges() {
      for(Iterator<UMLRealization> removedRealizationIterator = removedRealizations.iterator(); removedRealizationIterator.hasNext();) {
         UMLRealization removedRealization = removedRealizationIterator.next();
         for(Iterator<UMLRealization> addedRealizationIterator = addedRealizations.iterator(); addedRealizationIterator.hasNext();) {
            UMLRealization addedRealization = addedRealizationIterator.next();
            String renamedChild = isRenamedClass(removedRealization.getClient());
            String movedChild = isMovedClass(removedRealization.getClient());
            //String renamedParent = isRenamedClass(removedRealization.getSupplier());
            //String movedParent = isMovedClass(removedRealization.getSupplier());
            if( (renamedChild != null && renamedChild.equals(addedRealization.getClient().getName())) ||
                  (movedChild != null && movedChild.equals(addedRealization.getClient().getName()))) {
               UMLRealizationDiff realizationDiff = new UMLRealizationDiff(removedRealization, addedRealization);
               addedRealizationIterator.remove();
               removedRealizationIterator.remove();
               realizationDiffList.add(realizationDiff);
               break;
            }
         }
      }
   }

   public void checkForMovedClasses(Map<String, String> renamedFileHints) {
	   for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
		   UMLClass removedClass = removedClassIterator.next();
		   TreeSet<UMLClassMoveDiff> diffSet = new TreeSet<UMLClassMoveDiff>(new ClassMoveComparator());
		   for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
			   UMLClass addedClass = addedClassIterator.next();
			   String renamedFile =  renamedFileHints.get(removedClass.getSourceFile());
			   if(removedClass.hasSameNameAndKind(addedClass) 
					   && (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile) )) {
				   if(!conflictingMoveOfTopLevelClass(removedClass, addedClass)) {
					   UMLClassMoveDiff classMoveDiff = new UMLClassMoveDiff(removedClass, addedClass);
					   diffSet.add(classMoveDiff);
				   }
			   }
		   }
		   if(!diffSet.isEmpty()) {
			   UMLClassMoveDiff minClassMoveDiff = diffSet.first();
			   classMoveDiffList.add(minClassMoveDiff);
			   addedClasses.remove(minClassMoveDiff.getMovedClass());
			   removedClassIterator.remove();
		   }
	   }

	   List<UMLClassMoveDiff> allClassMoves = new ArrayList<UMLClassMoveDiff>(this.classMoveDiffList);
	   Collections.sort(allClassMoves);

	   for(int i=0; i<allClassMoves.size(); i++) {
		   UMLClassMoveDiff classMoveI = allClassMoves.get(i);
		   for(int j=i+1; j<allClassMoves.size(); j++) {
			   UMLClassMoveDiff classMoveJ = allClassMoves.get(j);
			   if(classMoveI.isInnerClassMove(classMoveJ)) {
				   innerClassMoveDiffList.add(classMoveJ);
			   }
		   }
	   }
	   this.classMoveDiffList.removeAll(innerClassMoveDiffList);
   }

   private boolean conflictingMoveOfTopLevelClass(UMLClass removedClass, UMLClass addedClass) {
	   if(!removedClass.isTopLevel() || !addedClass.isTopLevel()) {
		   //check if classMoveDiffList contains already a move for the outer class to a different target
		   for(UMLClassMoveDiff diff : classMoveDiffList) {
			   if((diff.getOriginalClass().getName().equals(removedClass.getPackageName()) &&
					   !diff.getMovedClass().getName().equals(addedClass.getPackageName())) ||
					   (!diff.getOriginalClass().getName().equals(removedClass.getPackageName()) &&
						diff.getMovedClass().getName().equals(addedClass.getPackageName()))) {
				   return true;
			   }
		   }
	   }
	   return false;
   }

   public void checkForRenamedClasses(Map<String, String> renamedFileHints) {
      for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
         UMLClass removedClass = removedClassIterator.next();
         TreeSet<UMLClassRenameDiff> diffSet = new TreeSet<UMLClassRenameDiff>(new ClassRenameComparator());
         for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
            UMLClass addedClass = addedClassIterator.next();
            String renamedFile =  renamedFileHints.get(removedClass.getSourceFile());
            if(removedClass.hasSameKind(addedClass) 
            		&& (removedClass.hasSameAttributesAndOperations(addedClass) || addedClass.getSourceFile().equals(renamedFile))) {
               if(!innerClassWithTheSameName(removedClass, addedClass)) {
            	   UMLClassRenameDiff classRenameDiff = new UMLClassRenameDiff(removedClass, addedClass);
            	   diffSet.add(classRenameDiff);
               }
            }
         }
         if(!diffSet.isEmpty()) {
            UMLClassRenameDiff minClassRenameDiff = diffSet.first();
            classRenameDiffList.add(minClassRenameDiff);
            addedClasses.remove(minClassRenameDiff.getRenamedClass());
            removedClassIterator.remove();
         }
      }
      
      List<UMLClassMoveDiff> allClassMoves = new ArrayList<UMLClassMoveDiff>(this.classMoveDiffList);
      Collections.sort(allClassMoves);
      
      for(UMLClassRenameDiff classRename : classRenameDiffList) {
         for(UMLClassMoveDiff classMove : allClassMoves) {
            if(classRename.isInnerClassMove(classMove)) {
               innerClassMoveDiffList.add(classMove);
            }
         }
      }
      this.classMoveDiffList.removeAll(innerClassMoveDiffList);
   }

   private boolean innerClassWithTheSameName(UMLClass removedClass, UMLClass addedClass) {
	   if(!removedClass.isTopLevel() || !addedClass.isTopLevel()) {
		   String removedClassName = removedClass.getName();
		   String removedName = removedClassName.substring(removedClassName.lastIndexOf(".")+1, removedClassName.length());
		   String addedClassName = addedClass.getName();
		   String addedName = addedClassName.substring(addedClassName.lastIndexOf(".")+1, addedClassName.length());
		   if(removedName.equals(addedName)) {
			   return true;
		   }
	   }
	   return false;
   }

   public List<UMLGeneralization> getAddedGeneralizations() {
      return addedGeneralizations;
   }

   public List<UMLRealization> getAddedRealizations() {
      return addedRealizations;
   }

   private List<MoveAttributeRefactoring> identifyMoveAttributeRefactorings() {
      List<MoveAttributeRefactoring> refactorings = new ArrayList<MoveAttributeRefactoring>();
      List<UMLAttribute> addedAttributes = getAddedAttributesInCommonClasses();
      List<UMLAttribute> removedAttributes = getRemovedAttributesInCommonClasses();
      
      for(UMLAttribute addedAttribute : addedAttributes) {
         for(UMLAttribute removedAttribute : removedAttributes) {
            if(addedAttribute.getName().equals(removedAttribute.getName()) &&
                  addedAttribute.getType().equals(removedAttribute.getType())) {
               if(isSubclassOf(removedAttribute.getClassName(), addedAttribute.getClassName())) {
                  PullUpAttributeRefactoring pullUpAttribute = new PullUpAttributeRefactoring(addedAttribute,
                        removedAttribute.getClassName(), addedAttribute.getClassName());
                  refactorings.add(pullUpAttribute);
               }
               else if(isSubclassOf(addedAttribute.getClassName(), removedAttribute.getClassName())) {
                  PushDownAttributeRefactoring pushDownAttribute = new PushDownAttributeRefactoring(addedAttribute,
                        removedAttribute.getClassName(), addedAttribute.getClassName());
                  refactorings.add(pushDownAttribute);
               }
               else {
                  MoveAttributeRefactoring moveAttribute = new MoveAttributeRefactoring(addedAttribute,
                        removedAttribute.getClassName(), addedAttribute.getClassName());
                  refactorings.add(moveAttribute);
               }
            }
         }
      }
      return refactorings;
   }

   private List<UMLAttribute> getAddedAttributesInCommonClasses() {
      List<UMLAttribute> addedAttributes = new ArrayList<UMLAttribute>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         addedAttributes.addAll(classDiff.getAddedAttributes());
      }
      return addedAttributes;
   }

   private List<UMLAttribute> getRemovedAttributesInCommonClasses() {
      List<UMLAttribute> removedAttributes = new ArrayList<UMLAttribute>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         removedAttributes.addAll(classDiff.getRemovedAttributes());
      }
      return removedAttributes;
   }

   private List<UMLOperation> getAddedOperationsInCommonClasses() {
      List<UMLOperation> addedOperations = new ArrayList<UMLOperation>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         addedOperations.addAll(classDiff.getAddedOperations());
      }
      return addedOperations;
   }

   private List<UMLOperation> getRemovedOperationsInCommonClasses() {
      List<UMLOperation> removedOperations = new ArrayList<UMLOperation>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         removedOperations.addAll(classDiff.getRemovedOperations());
      }
      return removedOperations;
   }
   
   private List<UMLOperationBodyMapper> getOperationBodyMappersInCommonClasses() {
      List<UMLOperationBodyMapper> mappers = new ArrayList<UMLOperationBodyMapper>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         mappers.addAll(classDiff.getOperationBodyMapperList());
      }
      return mappers;
   }

   private List<ExtractSuperclassRefactoring> identifyExtractSuperclassRefactorings() {
      List<ExtractSuperclassRefactoring> refactorings = new ArrayList<ExtractSuperclassRefactoring>();
      for(UMLClass addedClass : addedClasses) {
         Set<UMLClass> subclassSet = new LinkedHashSet<UMLClass>();
         String addedClassName = addedClass.getName();
         for(UMLGeneralization addedGeneralization : addedGeneralizations) {
            String parent = addedGeneralization.getParent();
            if(looksLikeSameType(parent, addedClassName) && topLevelOrSameOuterClass(addedClass, addedGeneralization.getChild()) && !isAddedClass(addedGeneralization.getChild().getName())) {
               subclassSet.add(addedGeneralization.getChild());
            }
         }
         for(UMLRealization addedRealization : addedRealizations) {
            String supplier = addedRealization.getSupplier();
			if(looksLikeSameType(supplier, addedClassName) && topLevelOrSameOuterClass(addedClass, addedRealization.getClient()) && !isAddedClass(addedRealization.getClient().getName())) {
               UMLClassDiff clientClassDiff = getUMLClassDiff(addedRealization.getClient().getName());
               boolean implementedInterfaceOperations = true;
               if(clientClassDiff != null) {
                  for(UMLOperation interfaceOperation : addedClass.getOperations()) {
                     if(!clientClassDiff.containsOperationWithTheSameSignature(interfaceOperation)) {
                        implementedInterfaceOperations = false;
                        break;
                     }
                  }
               }
               else {
//                  UMLClass clientClass = getUnchangedClass(addedRealization.getClient());
//                  if (clientClass == null) {
//                     // FIXME [danilofes]: This may happen with moves / renames but I'm not sure what to do
//                     implementedInterfaceOperations = false;
//                  } else {
//                     for(UMLOperation interfaceOperation : addedClass.getOperations()) {
//                        if(!clientClass.containsOperationWithTheSameSignature(interfaceOperation)) {
//                           implementedInterfaceOperations = false;
//                           break;
//                        }
//                     }
//                  }
               }
               if(implementedInterfaceOperations)
                  subclassSet.add(addedRealization.getClient());
            }
         }
         if(subclassSet.size() > 0) {
            ExtractSuperclassRefactoring extractSuperclassRefactoring = new ExtractSuperclassRefactoring(addedClass, subclassSet);
            refactorings.add(extractSuperclassRefactoring);
         }
      }
      return refactorings;
   }

   private boolean topLevelOrSameOuterClass(UMLClass class1, UMLClass class2) {
	   if(!class1.isTopLevel() && !class2.isTopLevel()) {
		   return class1.getPackageName().equals(class2.getPackageName());
	   }
	   return true;
   }

   public static boolean looksLikeSameType(String parent, String addedClassName) {
      if (addedClassName.contains(".") && !parent.contains(".")) {
         return parent.equals(addedClassName.substring(addedClassName.lastIndexOf(".") + 1));
      }
      if (parent.contains(".") && !addedClassName.contains(".")) {
         return addedClassName.equals(parent.substring(parent.lastIndexOf(".") + 1));
      }
      if (parent.contains(".") && addedClassName.contains(".")) {
    	  String s1 = parent.substring(parent.lastIndexOf(".") + 1);
    	  String s2 = addedClassName.substring(addedClassName.lastIndexOf(".") + 1);
    	  return s1.equals(s2);
      }
      return parent.equals(addedClassName);
   }

   private List<ConvertAnonymousClassToTypeRefactoring> identifyConvertAnonymousClassToTypeRefactorings() {
      List<ConvertAnonymousClassToTypeRefactoring> refactorings = new ArrayList<ConvertAnonymousClassToTypeRefactoring>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
	      for(UMLAnonymousClass anonymousClass : classDiff.getRemovedAnonymousClasses()) {
	         for(UMLClass addedClass : addedClasses) {
	            if(addedClass.getAttributes().containsAll(anonymousClass.getAttributes()) &&
	                  addedClass.getOperations().containsAll(anonymousClass.getOperations())) {
	               ConvertAnonymousClassToTypeRefactoring refactoring = new ConvertAnonymousClassToTypeRefactoring(anonymousClass, addedClass);
	               refactorings.add(refactoring);
	            }
	         }
	      }
      }
      return refactorings;
   }

   private List<Refactoring> getMoveClassRefactorings() {
	   List<Refactoring> refactorings = new ArrayList<Refactoring>();
	   List<RenamePackageRefactoring> renamePackageRefactorings = new ArrayList<RenamePackageRefactoring>();
	   for(UMLClassMoveDiff classMoveDiff : classMoveDiffList) {
		   UMLClass originalClass = classMoveDiff.getOriginalClass();
		   String originalName = originalClass.getName();
		   UMLClass movedClass = classMoveDiff.getMovedClass();
		   String movedName = movedClass.getName();
		   
		   String originalPath = originalClass.getSourceFile();
		   String movedPath = movedClass.getSourceFile();
		   boolean pathIsTheSame = originalPath.equals(movedPath);
		   
		   if (!originalName.equals(movedName)) {
			   if (!pathIsTheSame) {
				   MoveClassRefactoring refactoring = new MoveClassRefactoring(originalName, movedName);
				   RenamePattern renamePattern = refactoring.getRenamePattern();
				   //check if the the original path is a substring of the moved path and vice versa
				   if(renamePattern.getOriginalPath().contains(renamePattern.getMovedPath()) ||
						   renamePattern.getMovedPath().contains(renamePattern.getOriginalPath()) ||
						   !originalClass.isTopLevel() || !movedClass.isTopLevel()) {
					   refactorings.add(refactoring);
				   }
				   else {
					   boolean foundInMatchingRenamePackageRefactoring = false;
					   for(RenamePackageRefactoring renamePackageRefactoring : renamePackageRefactorings) {
						   if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
							   renamePackageRefactoring.addMoveClassRefactoring(refactoring);
							   foundInMatchingRenamePackageRefactoring = true;
							   break;
						   }
					   }
					   if(!foundInMatchingRenamePackageRefactoring) {
						   renamePackageRefactorings.add(new RenamePackageRefactoring(refactoring));
					   }
				   }
			   }
		   } else {
			   if (!pathIsTheSame) {
				   MoveClassFolderRefactoring refactoring = new MoveClassFolderRefactoring(originalName, originalPath, movedPath);
				   refactorings.add(refactoring);
			   }
		   }
	   }
	   for(RenamePackageRefactoring renamePackageRefactoring : renamePackageRefactorings) {
		   List<MoveClassRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
		   if(moveClassRefactorings.size() > 1) {
			   refactorings.add(renamePackageRefactoring);
		   }
		   else if(moveClassRefactorings.size() == 1) {
			   refactorings.add(moveClassRefactorings.get(0));
		   }
	   }
	   return refactorings;
   }

   private List<RenameClassRefactoring> getRenameClassRefactorings() {
      List<RenameClassRefactoring> refactorings = new ArrayList<RenameClassRefactoring>();
      for(UMLClassRenameDiff classRenameDiff : classRenameDiffList) {
         RenameClassRefactoring refactoring = new RenameClassRefactoring(classRenameDiff.getOriginalClass().getName(), classRenameDiff.getRenamedClass().getName());
         refactorings.add(refactoring);
      }
      return refactorings;
   }

   public List<Refactoring> getRefactorings() {
      List<Refactoring> refactorings = new ArrayList<Refactoring>();
      refactorings.addAll(identifyMoveAttributeRefactorings());
      refactorings.addAll(identifyExtractSuperclassRefactorings());
      refactorings.addAll(getMoveClassRefactorings());
      refactorings.addAll(getRenameClassRefactorings());
      refactorings.addAll(identifyConvertAnonymousClassToTypeRefactorings());
      
      for(UMLClassDiff classDiff : commonClassDiffList) {
         refactorings.addAll(classDiff.getRefactorings());
      }
      refactorings.addAll(this.refactorings);
      return refactorings;
   }

   public void checkForExtractedAndMovedOperations() {
      List<UMLOperation> addedOperations = getAddedOperationsInCommonClasses();
      for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
    	  UMLOperation addedOperation = addedOperationIterator.next();
    	  for(UMLOperationBodyMapper mapper : getOperationBodyMappersInCommonClasses()) {
    		  if(mapper.nonMappedElementsT1() > 0) {
               Set<OperationInvocation> operationInvocations = mapper.getOperation2().getBody().getAllOperationInvocations();
               OperationInvocation addedOperationInvocation = null;
               for(OperationInvocation invocation : operationInvocations) {
                  if(invocation.matchesOperation(addedOperation)) {
                     addedOperationInvocation = invocation;
                     break;
                  }
               }
               if(addedOperationInvocation != null) {
            	  List<String> arguments = addedOperationInvocation.getArguments();
            	  List<String> parameters = addedOperation.getParameterNameList();
            	  Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
            	  for(int i=0; i<parameters.size(); i++) {
            		  parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
            	  }
                  UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(mapper, addedOperation, parameterToArgumentMap);
                  operationBodyMapper.getMappings();
                  int mappings = operationBodyMapper.mappingsWithoutBlocks();
                  if(mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT2() || operationBodyMapper.exactMatches() > 0) &&
                        mapper.getOperation1().isDelegate() == null && mapper.getOperation2().isDelegate() != null) {
                     ExtractAndMoveOperationRefactoring extractOperationRefactoring =
                           new ExtractAndMoveOperationRefactoring(addedOperation, operationBodyMapper.getOperation1());
                     refactorings.add(extractOperationRefactoring);
                     deleteAddedOperation(addedOperation);
                  }
                  else if(addedOperation.getBody() != null && addedOperation.getBody().getCompositeStatement().getLeaves().size() == 1 &&
                          !addedOperation.getClassName().equals(operationBodyMapper.getOperation1().getClassName())) {
                	  UMLClassDiff classDiff = getUMLClassDiff(operationBodyMapper.getOperation1().getClassName());
                	  if(classDiff != null && !classDiff.getExtractedDelegateOperations().isEmpty()) {
                		  for(OperationInvocation operationInvocation : classDiff.getExtractedDelegateOperations().values()) {
                			  if(operationInvocation.matchesOperation(addedOperation)) {
                    			  ExtractAndMoveOperationRefactoring extractOperationRefactoring =
                                        new ExtractAndMoveOperationRefactoring(addedOperation, operationBodyMapper.getOperation1());
                                  refactorings.add(extractOperationRefactoring);
                                  deleteAddedOperation(addedOperation);
                    			  break;
                    		  }
                		  }
                	  }
                  }
               }
            }
         }
      }
   }

   public void checkForOperationMoves() {
      List<UMLOperation> addedOperations = getAddedOperationsInCommonClasses();
      List<UMLOperation> removedOperations = getRemovedOperationsInCommonClasses();
      
      if(addedOperations.size() <= removedOperations.size()) {
	      for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
	         UMLOperation addedOperation = addedOperationIterator.next();
	         TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap = new TreeMap<Integer, List<UMLOperationBodyMapper>>();
	         for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
	            UMLOperation removedOperation = removedOperationIterator.next();
	            
	            UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation);
	            operationBodyMapper.getMappings();
	            int mappings = operationBodyMapper.mappingsWithoutBlocks();
	            if(mappings > 0 && mappings > operationBodyMapper.nonMappedElementsT1() && mappings > operationBodyMapper.nonMappedElementsT2()) {
	               int exactMatches = operationBodyMapper.exactMatches();
	               if(operationBodyMapperMap.containsKey(exactMatches)) {
	                  List<UMLOperationBodyMapper> mapperList = operationBodyMapperMap.get(exactMatches);
	                  mapperList.add(operationBodyMapper);
	               }
	               else {
	                  List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>();
	                  mapperList.add(operationBodyMapper);
	                  operationBodyMapperMap.put(exactMatches, mapperList);
	               }
	            }
	         }
	         if(!operationBodyMapperMap.isEmpty()) {
	            List<UMLOperationBodyMapper> firstMappers = operationBodyMapperMap.get(operationBodyMapperMap.lastKey());
	            addedOperationIterator.remove();
	            for(UMLOperationBodyMapper firstMapper : firstMappers) {
	               UMLOperation removedOperation = firstMapper.getOperation1();
	               //removedOperations.remove(removedOperation);
	
	               Refactoring refactoring = null;
	               if(removedOperation.getClassName().equals(addedOperation.getClassName())) {
	            	  if (addedOperation.equalParameters(removedOperation)) {
	            		  //refactoring = new RenameOperationRefactoring(removedOperation, addedOperation);
	            	  } else {
	            		  // Methods in the same class with similar body but different signature
	            	  }
	               }
	               else if(isSubclassOf(removedOperation.getClassName(), addedOperation.getClassName()) && (addedOperation.equalParameters(removedOperation) || addedOperation.overloadedParameters(removedOperation))) {
	                  refactoring = new PullUpOperationRefactoring(removedOperation, addedOperation);
	               }
	               else if(isSubclassOf(addedOperation.getClassName(), removedOperation.getClassName()) && (addedOperation.equalParameters(removedOperation) || addedOperation.overloadedParameters(removedOperation))) {
	                  refactoring = new PushDownOperationRefactoring(removedOperation, addedOperation);
	               }
	               else if(movedMethodSignature(removedOperation, addedOperation) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
	                  refactoring = new MoveOperationRefactoring(removedOperation, addedOperation);
	               }
	               if(refactoring != null) {
	                  deleteRemovedOperation(removedOperation);
	                  deleteAddedOperation(addedOperation);
	                  refactorings.add(refactoring);
	               }
	            }
	         }
	      }
      }
      else {
    	  for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
	         UMLOperation removedOperation = removedOperationIterator.next();
	         TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap = new TreeMap<Integer, List<UMLOperationBodyMapper>>();
	         for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
	            UMLOperation addedOperation = addedOperationIterator.next();
	            
	            UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation);
	            operationBodyMapper.getMappings();
	            int mappings = operationBodyMapper.mappingsWithoutBlocks();
	            if(mappings > 0 && mappings > operationBodyMapper.nonMappedElementsT1() && mappings > operationBodyMapper.nonMappedElementsT2()) {
	               int exactMatches = operationBodyMapper.exactMatches();
	               if(operationBodyMapperMap.containsKey(exactMatches)) {
	                  List<UMLOperationBodyMapper> mapperList = operationBodyMapperMap.get(exactMatches);
	                  mapperList.add(operationBodyMapper);
	               }
	               else {
	                  List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>();
	                  mapperList.add(operationBodyMapper);
	                  operationBodyMapperMap.put(exactMatches, mapperList);
	               }
	            }
	         }
	         if(!operationBodyMapperMap.isEmpty()) {
	            List<UMLOperationBodyMapper> firstMappers = operationBodyMapperMap.get(operationBodyMapperMap.lastKey());
	            removedOperationIterator.remove();
	            for(UMLOperationBodyMapper firstMapper : firstMappers) {
	               UMLOperation addedOperation = firstMapper.getOperation2();
	               //addedOperations.remove(addedOperation);

	               Refactoring refactoring = null;
	               if(removedOperation.getClassName().equals(addedOperation.getClassName())) {
	            	  if (addedOperation.equalParameters(removedOperation)) {
	            		  //refactoring = new RenameOperationRefactoring(removedOperation, addedOperation);
	            	  } else {
	            		  // Methods in the same class with similar body but different signature
	            	  }
	               }
	               else if(isSubclassOf(removedOperation.getClassName(), addedOperation.getClassName()) && (addedOperation.equalParameters(removedOperation) || addedOperation.overloadedParameters(removedOperation))) {
	                  refactoring = new PullUpOperationRefactoring(removedOperation, addedOperation);
	               }
	               else if(isSubclassOf(addedOperation.getClassName(), removedOperation.getClassName()) && (addedOperation.equalParameters(removedOperation) || addedOperation.overloadedParameters(removedOperation))) {
	                  refactoring = new PushDownOperationRefactoring(removedOperation, addedOperation);
	               }
	               else if(movedMethodSignature(removedOperation, addedOperation) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
	                  refactoring = new MoveOperationRefactoring(removedOperation, addedOperation);
	               }
	               if(refactoring != null) {
	                  deleteRemovedOperation(removedOperation);
	                  deleteAddedOperation(addedOperation);
	                  refactorings.add(refactoring);
	               }
	            }
	         }
	      }
      }
   }
   
   private boolean movedMethodSignature(UMLOperation removedOperation, UMLOperation addedOperation) {
	   if(!addedOperation.isConstructor() &&
	      !removedOperation.isConstructor() &&
			   addedOperation.getName().equals(removedOperation.getName()) &&
			   addedOperation.equalReturnParameter(removedOperation) &&
			   addedOperation.isAbstract() == removedOperation.isAbstract()) {
		   if(addedOperation.getParameters().equals(removedOperation.getParameters())) {
			   return true;
		   }
		   else {
			   // ignore parameters of types sourceClass and targetClass
			   List<UMLParameter> oldParameters = new ArrayList<UMLParameter>();
			   for (UMLParameter oldParameter : removedOperation.getParameters()) {
				   if (!oldParameter.getKind().equals("return")
						   && !looksLikeSameType(oldParameter.getType().getClassType(), addedOperation.getClassName())
						   && !looksLikeSameType(oldParameter.getType().getClassType(), removedOperation.getClassName())) {
					   oldParameters.add(oldParameter);
				   }
			   }
			   List<UMLParameter> newParameters = new ArrayList<UMLParameter>();
			   for (UMLParameter newParameter : addedOperation.getParameters()) {
				   if (!newParameter.getKind().equals("return") &&
						   !looksLikeSameType(newParameter.getType().getClassType(), addedOperation.getClassName()) &&
						   !looksLikeSameType(newParameter.getType().getClassType(), removedOperation.getClassName())) {
					   newParameters.add(newParameter);
				   }
			   }
			   return oldParameters.equals(newParameters) || oldParameters.containsAll(newParameters) || newParameters.containsAll(oldParameters);
		   }
	   }
	   return false;
   }

   private boolean refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(UMLOperation removedOperation, UMLOperation addedOperation) {
	   for(Refactoring refactoring : refactorings) {
		   if(refactoring instanceof MoveOperationRefactoring) {
			   MoveOperationRefactoring moveRefactoring = (MoveOperationRefactoring)refactoring;
			   if(moveRefactoring.getOriginalOperation().equals(removedOperation)) {
				   return true;
			   }
		   }
	   }
	   return false;
   }

   private void deleteRemovedOperation(UMLOperation operation) {
      UMLClassDiff classDiff = getUMLClassDiff(operation.getClassName());
      classDiff.getRemovedOperations().remove(operation);
   }
   
   private void deleteAddedOperation(UMLOperation operation) {
      UMLClassDiff classDiff = getUMLClassDiff(operation.getClassName());
      classDiff.getAddedOperations().remove(operation);
   }
}
