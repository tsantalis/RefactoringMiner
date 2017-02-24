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
//   private List<UMLClass> commonUnchangedClasses;
   private List<UMLClassMoveDiff> classMoveDiffList;
   private List<UMLClassMoveDiff> innerClassMoveDiffList;
   private List<UMLClassRenameDiff> classRenameDiffList;
   
//   private List<UMLAnonymousClass> addedAnonymousClasses;
   private List<UMLAnonymousClass> removedAnonymousClasses;
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
//      this.commonUnchangedClasses = new ArrayList<UMLClass>();
      this.classMoveDiffList = new ArrayList<UMLClassMoveDiff>();
      this.innerClassMoveDiffList = new ArrayList<UMLClassMoveDiff>();
      this.classRenameDiffList = new ArrayList<UMLClassRenameDiff>();
      
//      this.addedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
      this.removedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
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

//   public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
//      this.addedAnonymousClasses.add(umlClass);
//   }

   public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
      this.removedAnonymousClasses.add(umlClass);
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

//   public void addUnchangedClass(UMLClass umlClass) {
//      this.commonUnchangedClasses.add(umlClass);
//   }

   private UMLClassDiff getUMLClassDiff(String className) {
      for(UMLClassDiff classDiff : commonClassDiffList) {
         if(classDiff.getClassName().equals(className))
            return classDiff;
      }
      return null;
   }

// private UMLClass getAddedClass(String className) {
//    for(UMLClass umlClass : addedClasses) {
//       if(umlClass.getName().equals(className))
//          return umlClass;
//    }
//    return null;
// }

//   private UMLClass getUnchangedClass(String className) {
//      for(UMLClass umlClass : commonUnchangedClasses) {
//         if(umlClass.getName().equals(className)) {
//            System.out.println("XXXX getUnchangedClass");
//            return umlClass;
//         }
//      }
//      return null;
//   }

   private boolean isSubclassOf(String subclass, String finalSuperclass) {
      UMLClassDiff subclassDiff = getUMLClassDiff(subclass);
      if(subclassDiff != null) {
         UMLType superclass = subclassDiff.getSuperclass();
         if(superclass != null) {
            if(looksLikeSameType(superclass.getClassType(), finalSuperclass))
               return true;
            else
               return isSubclassOf(superclass.getClassType(), finalSuperclass);
         }
         else
            return false;
      }
//      else {
//         UMLClass umlClass = getUnchangedClass(subclass);
//         if(umlClass != null) {
//            System.out.println("XXXX isSubclassOf");
//            UMLType superclass = umlClass.getSuperclass();
//            if(superclass != null) {
//               if(this.looksLikeSameType(superclass.getClassType(), finalSuperclass))
//                  return true;
//               else
//                  return isSubclassOf(superclass.getClassType(), finalSuperclass);
//            }
//            else
//               return false;
//         }
//      }
      return false;
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
// public List<UMLClass> getAddedClasses() {
//    return addedClasses;
// }
//
// public List<UMLClass> getRemovedClasses() {
//    return removedClasses;
// }

   public List<UMLGeneralization> getAddedGeneralizations() {
      return addedGeneralizations;
   }

// public List<UMLGeneralization> getRemovedGeneralizations() {
//    return removedGeneralizations;
// }

   public List<UMLRealization> getAddedRealizations() {
      return addedRealizations;
   }

// public List<UMLRealization> getRemovedRealizations() {
//    return removedRealizations;
// }
/*
   private List<ExtractOperationRefactoring> identifyExtractOperationRefactoringsWithoutSequenceDiagramInformation() {
      List<ExtractOperationRefactoring> refactorings = new ArrayList<ExtractOperationRefactoring>();
      for(UMLClassDiff classDiff : commonClassDiffList) {
         for(UMLOperationBodyDiff operationBodyDiff : classDiff.getOperationBodyDiffList()) {
            Set<MethodCall> addedMethodCalls = operationBodyDiff.getAddedMethodCalls();
            for(UMLOperation operation : classDiff.getAddedOperations()) {
               if(!operation.hasEmptyBody()) {
               for(MethodCall methodCall : addedMethodCalls) {
                  if(methodCall.matchesOperation(operation)) {
                     Set<AccessedMember> removedAccessedMembers = operationBodyDiff.getRemovedAccessedMembers();
                     //remove recursive method call
                     //remove this code
                     Set<AccessedMember> removedAccessedMembers = new LinkedHashSet<AccessedMember>(operationBodyDiff.getRemovedAccessedMembers());
                     MethodCall recursiveMethodCall = null;
                     for(AccessedMember member : removedAccessedMembers) {
                        if(member instanceof MethodCall) {
                           MethodCall call = (MethodCall)member;
                           if(call.matchesOperation(operationBodyDiff.getOriginalOperation())) {
                              recursiveMethodCall = call;
                              break;
                           }
                        }
                     }
                     if(recursiveMethodCall != null)
                        removedAccessedMembers.remove(recursiveMethodCall);
                     //END remove this code
                     if( (operation.getAccessedMembers().containsAll(removedAccessedMembers) && !removedAccessedMembers.isEmpty()) ||
                        (operationBodyDiff.getOriginalOperation().getAccessedMembers().containsAll(operation.getAccessedMembers())) ) {
                        ExtractOperationRefactoring extractOperationRefactoring =
                              new ExtractOperationRefactoring(operation, operationBodyDiff.getOriginalOperation(), methodCall.getOriginClassName());
                        refactorings.add(extractOperationRefactoring);
                     }
                  }
               }
               }
            }
         }
      }
      return refactorings;
   }
*/
   private List<MoveAttributeRefactoring> identifyMoveAttributeRefactoringsWithoutSequenceDiagramInformation() {
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

// private List<MoveOperationRefactoring> identifyMoveOperationRefactoringsWithoutSequenceDiagramInformation() {
//    List<MoveOperationRefactoring> refactorings = new ArrayList<MoveOperationRefactoring>();
//    List<UMLOperation> addedOperations = getAddedOperationsInCommonClasses();
//    List<UMLOperation> removedOperations = getRemovedOperationsInCommonClasses();
//    
//    for(UMLOperation addedOperation : addedOperations) {
//       for(UMLOperation removedOperation : removedOperations) {
//          if(addedOperation.getName().equals(removedOperation.getName()) &&
//                addedOperation.equalReturnParameter(removedOperation) &&
//                addedOperation.isAbstract() == removedOperation.isAbstract()) {
//             boolean matchingParameterTypes = false;
//             if(addedOperation.getParameters().equals(removedOperation.getParameters())) {
//                matchingParameterTypes = true;
//             }
//             else {
//                //a parameter corresponding to the source class may have been removed from the added operation
//                if(removedOperation.getParameters().size() == addedOperation.getParameters().size() + 1) {
//                   int numberOfMappedParameters = 0;
//                   boolean originClassFound = false;
//                   for(UMLParameter oldParameter : removedOperation.getParameters()) {
//                      boolean found = false;
//                      for(UMLParameter newParameter : addedOperation.getParameters()) {
//                         if(newParameter.equalsIncludingName(oldParameter)) {
//                            numberOfMappedParameters++;
//                            found = true;
//                         }
//                      }
//                      if(!found) {
//                         if(oldParameter.getType().getClassType().equals(addedOperation.getClassName()))
//                            originClassFound = true;
//                      }
//                   }
//                   if(numberOfMappedParameters == addedOperation.getParameters().size() && originClassFound) {
//                      matchingParameterTypes = true;
//                   }
//                }
//             }
//             if(matchingParameterTypes) {
//                if(isSubclassOf(removedOperation.getClassName(), addedOperation.getClassName())) {
//                   PullUpOperationRefactoring pullUpOperation = new PullUpOperationRefactoring(removedOperation, addedOperation);
//                   refactorings.add(pullUpOperation);
//                }
//                else if(isSubclassOf(addedOperation.getClassName(), removedOperation.getClassName())) {
//                   PushDownOperationRefactoring pushDownOperation = new PushDownOperationRefactoring(removedOperation, addedOperation);
//                   refactorings.add(pushDownOperation);
//                }
//                else {
//                   MoveOperationRefactoring moveOperation = new MoveOperationRefactoring(removedOperation, addedOperation);
//                   refactorings.add(moveOperation);
//                }
//             }
//          }
//       }
//    }
//    return refactorings;
// }

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
/*
   private List<IntroducePolymorphismRefactoring> identifyIntroducePolymorphismRefactoringsWithoutSequenceDiagramInformation() {
      List<IntroducePolymorphismRefactoring> refactorings = new ArrayList<IntroducePolymorphismRefactoring>();
      InheritanceDetection inheritanceDetection = new InheritanceDetection(this);
      for(String superclassName : inheritanceDetection.getRoots()) {
         for(UMLClassDiff classDiff : commonClassDiffList) {
            for(UMLOperationBodyDiff operationBodyDiff : classDiff.getOperationBodyDiffList()) {
               Set<MethodCall> addedMethodCalls = operationBodyDiff.getAddedMethodCalls();
               for(MethodCall methodCall : addedMethodCalls) {
                  if(methodCall.getOriginClassName().equals(superclassName)) {
                     UMLClass superclass = getAddedClass(superclassName);
                     UMLOperation invokedOperation = null;
                     for(UMLOperation superclassOperation : superclass.getOperations()) {
                        if(methodCall.matchesOperation(superclassOperation)) {
                           invokedOperation = superclassOperation;
                           break;
                        }
                     }
                     if(invokedOperation != null && (invokedOperation.isAbstract() || superclass.isInterface()) ) {
                        IntroducePolymorphismRefactoring introducePolymorphism = new IntroducePolymorphismRefactoring(
                              classDiff.getClassName(), methodCall.getOriginClassName(), methodCall, operationBodyDiff.getOriginalOperation());
                        refactorings.add(introducePolymorphism);
                     }
                  }
               }
            }
         }
      }
      return refactorings;
   }
*/
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
      return parent.equals(addedClassName);
   }

   private List<ConvertAnonymousClassToTypeRefactoring> identifyConvertAnonymousClassToTypeRefactorings() {
      List<ConvertAnonymousClassToTypeRefactoring> refactorings = new ArrayList<ConvertAnonymousClassToTypeRefactoring>();
      for(UMLAnonymousClass anonymousClass : removedAnonymousClasses) {
         for(UMLClass addedClass : addedClasses) {
            if(addedClass.getAttributes().containsAll(anonymousClass.getAttributes()) &&
                  addedClass.getOperations().containsAll(anonymousClass.getOperations())) {
               ConvertAnonymousClassToTypeRefactoring refactoring = new ConvertAnonymousClassToTypeRefactoring(anonymousClass, addedClass);
               refactorings.add(refactoring);
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
      
      //refactorings.addAll(identifyExtractOperationRefactoringsWithoutSequenceDiagramInformation());
      //refactorings.addAll(identifyMoveOperationRefactoringsWithoutSequenceDiagramInformation());
      refactorings.addAll(identifyMoveAttributeRefactoringsWithoutSequenceDiagramInformation());
      //refactorings.addAll(identifyIntroducePolymorphismRefactoringsWithoutSequenceDiagramInformation());
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
    		  if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
                 !mapper.getVariableReplacementsWithMethodInvocation().isEmpty() || !mapper.getMethodInvocationReplacements().isEmpty()) {
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
	               else if(movedMethodSignature(removedOperation, addedOperation)) {
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
	               else if(movedMethodSignature(removedOperation, addedOperation)) {
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

   private void deleteRemovedOperation(UMLOperation operation) {
      UMLClassDiff classDiff = getUMLClassDiff(operation.getClassName());
      classDiff.getRemovedOperations().remove(operation);
   }
   
   private void deleteAddedOperation(UMLOperation operation) {
      UMLClassDiff classDiff = getUMLClassDiff(operation.getClassName());
      classDiff.getAddedOperations().remove(operation);
   }
   
// public String toString() {
//    StringBuilder sb = new StringBuilder();
//    if(!addedClasses.isEmpty() || !removedClasses.isEmpty() || !classMoveDiffList.isEmpty() || !classRenameDiffList.isEmpty())
//       sb.append("--class level edits--").append("\n");
//    Collections.sort(removedClasses);
//    for(UMLClass umlClass : removedClasses) {
//       sb.append("class " + umlClass + " removed").append("\n");
//    }
//    Collections.sort(addedClasses);
//    for(UMLClass umlClass : addedClasses) {
//       sb.append("class " + umlClass + " added").append("\n");
//    }
//    Collections.sort(classMoveDiffList);
//    for(UMLClassMoveDiff classMoveDiff : classMoveDiffList) {
//       sb.append(classMoveDiff);
//    }
//    Collections.sort(classRenameDiffList);
//    for(UMLClassRenameDiff classRenameDiff : classRenameDiffList) {
//       sb.append(classRenameDiff);
//    }
//    if(!addedGeneralizations.isEmpty() || !removedGeneralizations.isEmpty() || !generalizationDiffList.isEmpty())
//       sb.append("--generalization edits--").append("\n");
//    Collections.sort(removedGeneralizations);
//    for(UMLGeneralization umlGeneralization : removedGeneralizations) {
//       sb.append("generalization " + umlGeneralization + " removed").append("\n");
//    }
//    Collections.sort(addedGeneralizations);
//    for(UMLGeneralization umlGeneralization : addedGeneralizations) {
//       sb.append("generalization " + umlGeneralization + " added").append("\n");
//    }
//    Collections.sort(generalizationDiffList);
//    for(UMLGeneralizationDiff generalizationDiff : generalizationDiffList) {
//       sb.append(generalizationDiff);
//    }
////     if(!addedAssociations.isEmpty() || !removedAssociations.isEmpty())
////        sb.append("--association edits--").append("\n");
////     Collections.sort(removedAssociations);
////     for(UMLAssociation umlAssociation : removedAssociations) {
////        sb.append("association " + umlAssociation + " removed").append("\n");
////     }
////     Collections.sort(addedAssociations);
////     for(UMLAssociation umlAssociation : addedAssociations) {
////        sb.append("association " + umlAssociation + " added").append("\n");
////     }
////     if(!addedDependencies.isEmpty() || !removedDependencies.isEmpty())
////        sb.append("--dependency edits--").append("\n");
////     Collections.sort(removedDependencies);
////     for(UMLDependency umlDependency : removedDependencies) {
////        sb.append("dependency " + umlDependency + " removed").append("\n");
////     }
////     Collections.sort(addedDependencies);
////     for(UMLDependency umlDependency : addedDependencies) {
////        sb.append("dependency " + umlDependency + " added").append("\n");
////     }
//    if(!addedRealizations.isEmpty() || !removedRealizations.isEmpty() || !realizationDiffList.isEmpty())
//       sb.append("--realization edits--").append("\n");
//    Collections.sort(removedRealizations);
//    for(UMLRealization umlRealization : removedRealizations) {
//       sb.append("realization " + umlRealization + " removed").append("\n");
//    }
//    Collections.sort(addedRealizations);
//    for(UMLRealization umlRealization : addedRealizations) {
//       sb.append("realization " + umlRealization + " added").append("\n");
//    }
//    Collections.sort(realizationDiffList);
//    for(UMLRealizationDiff realizationDiff : realizationDiffList) {
//       sb.append(realizationDiff);
//    }
//    if(!commonClassDiffList.isEmpty())
//       sb.append("--edits between common classes--").append("\n");
//    Collections.sort(commonClassDiffList);
//    for(UMLClassDiff classDiff : commonClassDiffList) {
//       sb.append(classDiff);
//    }
////     if(!collaborationDiffList.isEmpty())
////        sb.append("--edits between collaborations--").append("\n");
////     for(UMLCollaborationDiff collaborationDiff : collaborationDiffList) {
////        sb.append(collaborationDiff);
////     }
//    /*List<ExtractOperationRefactoring> extractOperationList = identifyExtractOperationRefactoringsWithoutSequenceDiagramInformation();
//    List<MoveOperationRefactoring> moveOperationList = identifyMoveOperationRefactoringsWithoutSequenceDiagramInformation();
//    List<MoveAttributeRefactoring> moveAttributeList = identifyMoveAttributeRefactoringsWithoutSequenceDiagramInformation();
//    List<IntroducePolymorphismRefactoring> introducePolymorphismList = identifyIntroducePolymorphismRefactoringsWithoutSequenceDiagramInformation();
//    List<ExtractSuperclassRefactoring> extractSuperclassList = identifyExtractSuperclassRefactorings();
//    List<RenameClassRefactoring> renameClassList = getRenameClassRefactorings();
//    if(!extractOperationList.isEmpty() || !moveOperationList.isEmpty() || !moveAttributeList.isEmpty() ||
//          !introducePolymorphismList.isEmpty() || !extractSuperclassList.isEmpty() || !renameClassList.isEmpty())
//       sb.append("--identified refactorings--").append("\n");
//    for(ExtractOperationRefactoring extractOperation : extractOperationList) {
//       sb.append(extractOperation).append("\n");
//    }
//    for(MoveOperationRefactoring moveOperation : moveOperationList) {
//       sb.append(moveOperation).append("\n");
//    }
//    for(MoveAttributeRefactoring moveAttribute : moveAttributeList) {
//       sb.append(moveAttribute).append("\n");
//    }
//    for(IntroducePolymorphismRefactoring introducePolymorphism : introducePolymorphismList) {
//       sb.append(introducePolymorphism).append("\n");
//    }
//    for(ExtractSuperclassRefactoring extractSuperclass : extractSuperclassList) {
//       sb.append(extractSuperclass).append("\n");
//    }
//    for(RenameClassRefactoring renameClass : renameClassList) {
//       sb.append(renameClass).append("\n");
//    }*/
//    return sb.toString();
// }
}
