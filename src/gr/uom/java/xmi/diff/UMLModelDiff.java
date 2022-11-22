package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;
import gr.uom.java.xmi.UMLClassMatcher.Rename;
import gr.uom.java.xmi.UMLGeneralization;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLRealization;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.UMLTypeParameter;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObjectMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapperComparator;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class UMLModelDiff {
	private static final Pattern RETURN_NUMBER_LITERAL = Pattern.compile("return \\d+;\n");
	private final int MAXIMUM_NUMBER_OF_COMPARED_METHODS;
	private UMLModel parentModel;
	private UMLModel childModel;
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
	private List<UMLClassMergeDiff> classMergeDiffList;
	private List<UMLClassSplitDiff> classSplitDiffList;
	private List<Refactoring> refactorings;
	private Set<Refactoring> moveRenameClassRefactorings;
	private Set<String> deletedFolderPaths;
	private Set<Pair<VariableDeclarationContainer, VariableDeclarationContainer>> processedOperationPairs = new HashSet<Pair<VariableDeclarationContainer, VariableDeclarationContainer>>();
	private Set<Pair<UMLClass, UMLClass>> processedClassPairs = new HashSet<Pair<UMLClass, UMLClass>>();

	public UMLModelDiff(UMLModel parentModel, UMLModel childModel) {
		this.parentModel = parentModel;
		this.childModel = childModel;
		if(partialModel()) {
			MAXIMUM_NUMBER_OF_COMPARED_METHODS = 500;
		}
		else {
			MAXIMUM_NUMBER_OF_COMPARED_METHODS = 200;
		}
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
		this.classMergeDiffList = new ArrayList<UMLClassMergeDiff>();
		this.classSplitDiffList = new ArrayList<UMLClassSplitDiff>();
		this.refactorings = new ArrayList<Refactoring>();
		this.deletedFolderPaths = new LinkedHashSet<String>();
	}

	public UMLAbstractClass findClassInParentModel(String className) {
		for(UMLClass umlClass : parentModel.getClassList()) {
			if(umlClass.getName().equals(className)) {
				return umlClass;
			}
		}
		for(UMLClass umlClass : parentModel.getClassList()) {
			if(umlClass.getName().endsWith("." + className)) {
				return umlClass;
			}
		}
		return null;
	}

	public UMLAbstractClass findClassInChildModel(String className) {
		for(UMLClass umlClass : childModel.getClassList()) {
			if(umlClass.getName().equals(className)) {
				return umlClass;
			}
		}
		for(UMLClass umlClass : childModel.getClassList()) {
			if(umlClass.getName().endsWith("." + className)) {
				return umlClass;
			}
		}
		return null;
	}

	public void reportAddedClass(UMLClass umlClass) {
		if(!addedClasses.contains(umlClass))
			this.addedClasses.add(umlClass);
	}

	public List<UMLClass> getAddedClasses() {
		return addedClasses;
	}

	public void reportRemovedClass(UMLClass umlClass) {
		if(!removedClasses.contains(umlClass))
			this.removedClasses.add(umlClass);
	}

	public List<UMLClass> getRemovedClasses() {
		return removedClasses;
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

	public List<UMLClassDiff> getCommonClassDiffList() {
		return commonClassDiffList;
	}

	public List<UMLClassMoveDiff> getClassMoveDiffList() {
		return classMoveDiffList;
	}

	public List<UMLClassMoveDiff> getInnerClassMoveDiffList() {
		return innerClassMoveDiffList;
	}

	public List<UMLClassRenameDiff> getClassRenameDiffList() {
		return classRenameDiffList;
	}

	public boolean commonlyImplementedOperations(UMLOperation operation1, UMLOperation operation2, UMLClassBaseDiff classDiff2) {
		UMLClassBaseDiff classDiff1 = getUMLClassDiff(operation1.getClassName());
		if(classDiff1 != null) {
			Set<UMLType> commonInterfaces = classDiff1.nextClassCommonInterfaces(classDiff2);
			for(UMLType commonInterface : commonInterfaces) {
				UMLClassBaseDiff interfaceDiff = getUMLClassDiff(commonInterface);
				if(interfaceDiff != null &&
						interfaceDiff.containsOperationWithTheSameSignatureInOriginalClass(operation1) &&
						interfaceDiff.containsOperationWithTheSameSignatureInNextClass(operation2)) {
					return true;
				}
			}
		}
		return false;
	}

	public UMLClassBaseDiff getUMLClassDiff(String className) {
		for(UMLClassDiff classDiff : commonClassDiffList) {
			if(classDiff.matches(className))
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			if(classDiff.matches(className))
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			if(classDiff.matches(className))
				return classDiff;
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			if(classDiff.matches(className))
				return classDiff;
		}
		return null;
	}

	public UMLClassBaseDiff getUMLClassDiff(UMLType type) {
		for(UMLClassDiff classDiff : commonClassDiffList) {
			if(classDiff.matches(type))
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			if(classDiff.matches(type))
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			if(classDiff.matches(type))
				return classDiff;
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			if(classDiff.matches(type))
				return classDiff;
		}
		return null;
	}

	private UMLClassBaseDiff getUMLClassDiffWithAttribute(Replacement pattern) {
		for(UMLClassDiff classDiff : commonClassDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				return classDiff;
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				return classDiff;
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getBefore()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				return classDiff;
		}
		return null;
	}

	private List<UMLClassBaseDiff> getUMLClassDiffWithExistingAttributeAfter(Replacement pattern) {
		List<UMLClassBaseDiff> classDiffs = new ArrayList<UMLClassBaseDiff>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) != null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		return classDiffs;
	}

	private List<UMLClassBaseDiff> getUMLClassDiffWithNewAttributeAfter(Replacement pattern) {
		List<UMLClassBaseDiff> classDiffs = new ArrayList<UMLClassBaseDiff>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			if(classDiff.findAttributeInOriginalClass(pattern.getAfter()) == null &&
					classDiff.findAttributeInNextClass(pattern.getAfter()) != null)
				classDiffs.add(classDiff);
		}
		return classDiffs;
	}

	public boolean isSubclassOf(String subclass, String finalSuperclass) {
		return isSubclassOf(subclass, finalSuperclass, new LinkedHashSet<String>());
	}

	private boolean isSubclassOf(String subclass, String finalSuperclass, Set<String> visitedClasses) {
		if(visitedClasses.contains(subclass)) {
			return false;
		}
		else {
			visitedClasses.add(subclass);
		}
		UMLClassBaseDiff subclassDiff = getUMLClassDiff(subclass);
		if(subclassDiff == null) {
			subclassDiff = getUMLClassDiff(UMLType.extractTypeObject(subclass));
		}
		if(subclassDiff != null) {
			UMLType superclass = subclassDiff.getSuperclass();
			if(superclass != null) {
				if(checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses)) {
					return true;
				}
			}
			else if(subclassDiff.getOldSuperclass() != null && subclassDiff.getNewSuperclass() != null &&
					!subclassDiff.getOldSuperclass().equals(subclassDiff.getNewSuperclass()) && looksLikeAddedClass(subclassDiff.getNewSuperclass()) != null) {
				UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
				if(addedClass.getSuperclass() != null) {
					return checkInheritanceRelationship(addedClass.getSuperclass(), finalSuperclass, visitedClasses);
				}
			}
			else if(subclassDiff.getOldSuperclass() == null && subclassDiff.getNewSuperclass() != null && looksLikeAddedClass(subclassDiff.getNewSuperclass()) != null) {
				UMLClass addedClass = looksLikeAddedClass(subclassDiff.getNewSuperclass());
				return checkInheritanceRelationship(UMLType.extractTypeObject(addedClass.getName()), finalSuperclass, visitedClasses);
			}
			for(UMLType implementedInterface : subclassDiff.getAddedImplementedInterfaces()) {
				if(checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
					return true;
				}
			}
			for(UMLType implementedInterface : subclassDiff.getNextClass().getImplementedInterfaces()) {
				if(checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
					return true;
				}
			}
		}
		UMLClass addedClass = getAddedClass(subclass);
		if(addedClass == null) {
			addedClass = looksLikeAddedClass(UMLType.extractTypeObject(subclass));
		}
		if(addedClass != null) {
			UMLType superclass = addedClass.getSuperclass();
			if(superclass != null) {
				return checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses);
			}
			for(UMLType implementedInterface : addedClass.getImplementedInterfaces()) {
				if(checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
					return true;
				}
			}
		}
		UMLClass removedClass = getRemovedClass(subclass);
		if(removedClass == null) {
			removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(subclass));
		}
		if(removedClass != null) {
			UMLType superclass = removedClass.getSuperclass();
			if(superclass != null) {
				return checkInheritanceRelationship(superclass, finalSuperclass, visitedClasses);
			}
			for(UMLType implementedInterface : removedClass.getImplementedInterfaces()) {
				if(checkInheritanceRelationship(implementedInterface, finalSuperclass, visitedClasses)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkInheritanceRelationship(UMLType superclass, String finalSuperclass, Set<String> visitedClasses) {
		if(looksLikeSameType(superclass.getClassType(), finalSuperclass))
			return true;
		else
			return isSubclassOf(superclass.getClassType(), finalSuperclass, visitedClasses);
	}

	private UMLClass looksLikeAddedClass(UMLType type) {
		for(UMLClass umlClass : addedClasses) {
			if(umlClass.getName().endsWith("." + type.getClassType())) {
				return umlClass;
			}
		}
		return null;
	}

	private UMLClass looksLikeRemovedClass(UMLType type) {
		for(UMLClass umlClass : removedClasses) {
			if(umlClass.getName().endsWith("." + type.getClassType())) {
				return umlClass;
			}
		}
		return null;
	}

	public UMLOperation findOperationInAddedClasses(AbstractCall operationInvocation, VariableDeclarationContainer callerOperation) {
		for(UMLClass umlClass : addedClasses) {
			String expression = operationInvocation.getExpression();
			if(expression != null && umlClass.getNonQualifiedName().equalsIgnoreCase(expression)) {
				for(UMLOperation operation : umlClass.getOperations()) {
					if(operationInvocation.matchesOperation(operation, callerOperation, this)) {
						return operation;
					}
				}
			}
		}
		return null;
	}

	public UMLClass getAddedClass(String className) {
		for(UMLClass umlClass : addedClasses) {
			if(umlClass.getName().equals(className))
				return umlClass;
		}
		return null;
	}

	public UMLClass getRemovedClass(String className) {
		for(UMLClass umlClass : removedClasses) {
			if(umlClass.getName().equals(className))
				return umlClass;
		}
		return null;
	}

	private String isRenamedClass(UMLClass umlClass) {
		for(UMLClassRenameDiff renameDiff : classRenameDiffList) {
			if(renameDiff.getOriginalClass().equals(umlClass))
				return renameDiff.getRenamedClass().getName();
		}
		return null;
	}

	private String isMovedClass(UMLClass umlClass) {
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

	public void checkForMovedClasses(Set<String> repositoryDirectories, UMLClassMatcher matcher) throws RefactoringMinerTimedOutException {
		if(removedClasses.size() <= addedClasses.size()) {
			for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
				UMLClass removedClass = removedClassIterator.next();
				TreeSet<UMLClassMoveDiff> diffSet = new TreeSet<UMLClassMoveDiff>(new ClassMoveComparator());
				for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
					UMLClass addedClass = addedClassIterator.next();
					String removedClassSourceFile = removedClass.getSourceFile();
					String removedClassSourceFolder = "";
					if(removedClassSourceFile.contains("/")) {
						removedClassSourceFolder = removedClassSourceFile.substring(0, removedClassSourceFile.lastIndexOf("/"));
					}
					if(!repositoryDirectories.contains(removedClassSourceFolder)) {
						deletedFolderPaths.add(removedClassSourceFolder);
						//add deleted sub-directories
						String subDirectory = new String(removedClassSourceFolder);
						while(subDirectory.contains("/")) {
							subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
							if(!repositoryDirectories.contains(subDirectory)) {
								deletedFolderPaths.add(subDirectory);
							}
						}
					}
					MatchResult matchResult = matcher.match(removedClass, addedClass);
					if(matchResult.isMatch()) {
						if(!conflictingMoveOfTopLevelClass(removedClass, addedClass)) {
							UMLClassMoveDiff classMoveDiff = new UMLClassMoveDiff(removedClass, addedClass, this, matchResult);
							diffSet.add(classMoveDiff);
						}
					}
				}
				if(!diffSet.isEmpty()) {
					UMLClassMoveDiff minClassMoveDiff = diffSet.first();
					TreeSet<UMLClassRenameDiff> renameDiffSet = new TreeSet<>();
					if(matcher instanceof UMLClassMatcher.RelaxedMove) {
						renameDiffSet = findRenameMatchesForRemovedClass(removedClass, new UMLClassMatcher.RelaxedRename());
					}
					if(!renameDiffSet.isEmpty() && !(renameDiffSet.first().getOriginalClass().equals(minClassMoveDiff.getOriginalClass()) &&
							renameDiffSet.first().getRenamedClass().equals(minClassMoveDiff.getMovedClass()))) {
						UMLClassRenameDiff minClassRenameDiff = renameDiffSet.first();
						int matchedMembers1 = minClassMoveDiff.getMatchResult().getMatchedOperations() + minClassMoveDiff.getMatchResult().getMatchedAttributes();
						int matchedMembers2 = minClassRenameDiff.getMatchResult().getMatchedOperations() + minClassRenameDiff.getMatchResult().getMatchedAttributes();
						if(matchedMembers2 > matchedMembers1) {
							minClassRenameDiff.process();
							classRenameDiffList.add(minClassRenameDiff);
							addedClasses.remove(minClassRenameDiff.getRenamedClass());
							removedClassIterator.remove();
						}
						else {
							minClassMoveDiff.process();
							classMoveDiffList.add(minClassMoveDiff);
							addedClasses.remove(minClassMoveDiff.getMovedClass());
							removedClassIterator.remove();
						}
					}
					else {
						minClassMoveDiff.process();
						classMoveDiffList.add(minClassMoveDiff);
						addedClasses.remove(minClassMoveDiff.getMovedClass());
						removedClassIterator.remove();
					}
				}
			}
		}
		else {
			for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
				UMLClass addedClass = addedClassIterator.next();
				TreeSet<UMLClassMoveDiff> diffSet = new TreeSet<UMLClassMoveDiff>(new ClassMoveComparator());
				for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
					UMLClass removedClass = removedClassIterator.next();
					String removedClassSourceFile = removedClass.getSourceFile();
					String removedClassSourceFolder = "";
					if(removedClassSourceFile.contains("/")) {
						removedClassSourceFolder = removedClassSourceFile.substring(0, removedClassSourceFile.lastIndexOf("/"));
					}
					if(!repositoryDirectories.contains(removedClassSourceFolder)) {
						deletedFolderPaths.add(removedClassSourceFolder);
						//add deleted sub-directories
						String subDirectory = new String(removedClassSourceFolder);
						while(subDirectory.contains("/")) {
							subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
							if(!repositoryDirectories.contains(subDirectory)) {
								deletedFolderPaths.add(subDirectory);
							}
						}
					}
					MatchResult matchResult = matcher.match(removedClass, addedClass);
					if(matchResult.isMatch()) {
						if(!conflictingMoveOfTopLevelClass(removedClass, addedClass)) {
							UMLClassMoveDiff classMoveDiff = new UMLClassMoveDiff(removedClass, addedClass, this, matchResult);
							diffSet.add(classMoveDiff);
						}
					}
				}
				if(!diffSet.isEmpty()) {
					UMLClassMoveDiff minClassMoveDiff = diffSet.first();
					TreeSet<UMLClassRenameDiff> renameDiffSet = new TreeSet<>();
					if(matcher instanceof UMLClassMatcher.RelaxedMove) {
						renameDiffSet = findRenameMatchesForAddedClass(addedClass, new UMLClassMatcher.RelaxedRename());
					}
					if(!renameDiffSet.isEmpty() && !(renameDiffSet.first().getOriginalClass().equals(minClassMoveDiff.getOriginalClass()) &&
							renameDiffSet.first().getRenamedClass().equals(minClassMoveDiff.getMovedClass()))) {
						UMLClassRenameDiff minClassRenameDiff = renameDiffSet.first();
						int matchedMembers1 = minClassMoveDiff.getMatchResult().getMatchedOperations() + minClassMoveDiff.getMatchResult().getMatchedAttributes();
						int matchedMembers2 = minClassRenameDiff.getMatchResult().getMatchedOperations() + minClassRenameDiff.getMatchResult().getMatchedAttributes();
						if(matchedMembers2 > matchedMembers1) {
							minClassRenameDiff.process();
							classRenameDiffList.add(minClassRenameDiff);
							removedClasses.remove(minClassRenameDiff.getOriginalClass());
							addedClassIterator.remove();
						}
						else {
							minClassMoveDiff.process();
							classMoveDiffList.add(minClassMoveDiff);
							removedClasses.remove(minClassMoveDiff.getOriginalClass());
							addedClassIterator.remove();
						}
					}
					else {
						minClassMoveDiff.process();
						classMoveDiffList.add(minClassMoveDiff);
						removedClasses.remove(minClassMoveDiff.getOriginalClass());
						addedClassIterator.remove();
					}
				}
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
		if(!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
			//check if classMoveDiffList contains already a move for the outer class to a different target
			for(UMLClassMoveDiff diff : classMoveDiffList) {
				if((diff.getOriginalClass().getName().startsWith(removedClass.getPackageName()) &&
						!diff.getMovedClass().getName().startsWith(addedClass.getPackageName())) ||
						(!diff.getOriginalClass().getName().startsWith(removedClass.getPackageName()) &&
								diff.getMovedClass().getName().startsWith(addedClass.getPackageName()))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean sameRenamedClass(Set<UMLClassRenameDiff> union) {
		if(union.size() > 1) {
			UMLClass renamedClass = null;
			for(UMLClassRenameDiff renameDiff : union) {
				if(renamedClass == null) {
					renamedClass = renameDiff.getRenamedClass();
				}
				else if(!renamedClass.equals(renameDiff.getRenamedClass())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean sameOriginalClass(Set<UMLClassRenameDiff> union) {
		if(union.size() > 1) {
			UMLClass originalClass = null;
			for(UMLClassRenameDiff renameDiff : union) {
				if(originalClass == null) {
					originalClass = renameDiff.getOriginalClass();
				}
				else if(!originalClass.equals(renameDiff.getOriginalClass())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean inheritanceRelationshipBetweenMergedClasses(Set<UMLClassRenameDiff> union) {
		if(union.size() > 1) {
			UMLClass originalClass = null;
			for(UMLClassRenameDiff renameDiff : union) {
				if(originalClass == null) {
					originalClass = renameDiff.getOriginalClass();
				}
				else if(isSubclassOf(originalClass.getName(), renameDiff.getOriginalClass().getName()) ||
						isSubclassOf(renameDiff.getOriginalClass().getName(), originalClass.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean inheritanceRelationshipBetweenSplitClasses(Set<UMLClassRenameDiff> union) {
		if(union.size() > 1) {
			UMLClass renamedClass = null;
			for(UMLClassRenameDiff renameDiff : union) {
				if(renamedClass == null) {
					renamedClass = renameDiff.getRenamedClass();
				}
				else if(isSubclassOf(renamedClass.getName(), renameDiff.getRenamedClass().getName()) ||
						isSubclassOf(renameDiff.getRenamedClass().getName(), renamedClass.getName()) ||
						renamedClass.hasCommonOperationWithTheSameSignature(renameDiff.getRenamedClass()).isMatch()) {
					return true;
				}
			}
		}
		return false;
	}

	public void checkForRenamedClasses(UMLClassMatcher matcher) throws RefactoringMinerTimedOutException {
		if(removedClasses.size() <= addedClasses.size()) {
			Set<UMLClass> mergedClassesToBeRemoved = new HashSet<UMLClass>();
			for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
				UMLClass removedClass = removedClassIterator.next();
				TreeSet<UMLClassRenameDiff> diffSet = findRenameMatchesForRemovedClass(removedClass, matcher);
				if(!diffSet.isEmpty()) {
					UMLClassRenameDiff minClassRenameDiff = diffSet.first();
					boolean mergeFound = false;
					boolean splitFound = false;
					boolean conflictFound = false;
					TreeSet<UMLClassRenameDiff> renameDiffSet = findRenameMatchesForAddedClass(minClassRenameDiff.getRenamedClass(), matcher);
					TreeSet<UMLClassRenameDiff> union = new TreeSet<>();
					union.addAll(diffSet);
					union.addAll(renameDiffSet);
					if(matcher instanceof UMLClassMatcher.RelaxedRename) {
						if(sameRenamedClass(union) && !inheritanceRelationshipBetweenMergedClasses(union) && !partialModel()) {
							UMLClassMergeDiff mergeDiff = new UMLClassMergeDiff(union);
							classMergeDiffList.add(mergeDiff);
							for(UMLClassRenameDiff renameDiff : union) {
								addedClasses.remove(renameDiff.getRenamedClass());
								mergedClassesToBeRemoved.add(renameDiff.getOriginalClass());
							}
							removedClassIterator.remove();
							mergeFound = true;
						}
						else if(sameOriginalClass(union) && !inheritanceRelationshipBetweenSplitClasses(union) && !partialModel()) {
							UMLClassSplitDiff splitDiff = new UMLClassSplitDiff(union);
							classSplitDiffList.add(splitDiff);
							for(UMLClassRenameDiff renameDiff : union) {
								addedClasses.remove(renameDiff.getRenamedClass());
								mergedClassesToBeRemoved.add(renameDiff.getOriginalClass());
							}
							removedClassIterator.remove();
							splitFound = true;
						}
					}
					else if(matcher instanceof UMLClassMatcher.Rename) {
						if((union.size() > 2 && sameRenamedClass(union)) || (renameDiffSet.size() > 2 && sameRenamedClass(renameDiffSet))) {
							for(UMLClassRenameDiff renameDiff : union) {
								addedClasses.remove(renameDiff.getRenamedClass());
								mergedClassesToBeRemoved.add(renameDiff.getOriginalClass());
							}
							removedClassIterator.remove();
							conflictFound = true;
						}
					}
					if(!mergeFound && !splitFound && !conflictFound) {
						if(!renameDiffSet.isEmpty() && !(renameDiffSet.first().getOriginalClass().equals(minClassRenameDiff.getOriginalClass()) &&
								renameDiffSet.first().getRenamedClass().equals(minClassRenameDiff.getRenamedClass()))) {
							UMLClassRenameDiff minClassRenameDiff2 = renameDiffSet.first();
							int matchedMembers1 = minClassRenameDiff.getMatchResult().getMatchedOperations() + minClassRenameDiff.getMatchResult().getMatchedAttributes();
							int matchedMembers2 = minClassRenameDiff2.getMatchResult().getMatchedOperations() + minClassRenameDiff2.getMatchResult().getMatchedAttributes();
							if(matchedMembers2 > matchedMembers1) {
								minClassRenameDiff2.process();
								classRenameDiffList.add(minClassRenameDiff2);
								addedClasses.remove(minClassRenameDiff2.getRenamedClass());
								removedClassIterator.remove();
							}
							else {
								minClassRenameDiff.process();
								classRenameDiffList.add(minClassRenameDiff);
								addedClasses.remove(minClassRenameDiff.getRenamedClass());
								removedClassIterator.remove();
							}
						}
						else {
							minClassRenameDiff.process();
							classRenameDiffList.add(minClassRenameDiff);
							addedClasses.remove(minClassRenameDiff.getRenamedClass());
							removedClassIterator.remove();
						}
					}
				}
			}
			removedClasses.removeAll(mergedClassesToBeRemoved);
		}
		else {
			for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
				UMLClass addedClass = addedClassIterator.next();
				TreeSet<UMLClassRenameDiff> diffSet = findRenameMatchesForAddedClass(addedClass, matcher);
				if(!diffSet.isEmpty()) {
					UMLClassRenameDiff minClassRenameDiff = diffSet.first();
					boolean mergeFound = false;
					boolean splitFound = false;
					boolean conflictFound = false;
					TreeSet<UMLClassRenameDiff> renameDiffSet = findRenameMatchesForRemovedClass(minClassRenameDiff.getOriginalClass(), matcher);
					TreeSet<UMLClassRenameDiff> union = new TreeSet<>();
					union.addAll(diffSet);
					union.addAll(renameDiffSet);
					if(matcher instanceof UMLClassMatcher.RelaxedRename) {
						if(sameRenamedClass(union) && !inheritanceRelationshipBetweenMergedClasses(union) && !partialModel()) {
							UMLClassMergeDiff mergeDiff = new UMLClassMergeDiff(union);
							classMergeDiffList.add(mergeDiff);
							for(UMLClassRenameDiff renameDiff : union) {
								removedClasses.remove(renameDiff.getOriginalClass());
							}
							addedClassIterator.remove();
							mergeFound = true;
						}
						else if(sameOriginalClass(union) && !inheritanceRelationshipBetweenSplitClasses(union) && !partialModel()) {
							UMLClassSplitDiff splitDiff = new UMLClassSplitDiff(union);
							classSplitDiffList.add(splitDiff);
							for(UMLClassRenameDiff renameDiff : union) {
								removedClasses.remove(renameDiff.getOriginalClass());
							}
							addedClassIterator.remove();
							splitFound = true;
						}
					}
					else if(matcher instanceof UMLClassMatcher.Rename) {
						if((union.size() > 2 && sameRenamedClass(union)) || (renameDiffSet.size() > 2 && sameRenamedClass(renameDiffSet))) {
							for(UMLClassRenameDiff renameDiff : union) {
								removedClasses.remove(renameDiff.getOriginalClass());
							}
							addedClassIterator.remove();
							conflictFound = true;
						}
					}
					if(!mergeFound && !splitFound && !conflictFound) {
						if(!renameDiffSet.isEmpty() && !(renameDiffSet.first().getOriginalClass().equals(minClassRenameDiff.getOriginalClass()) &&
								renameDiffSet.first().getRenamedClass().equals(minClassRenameDiff.getRenamedClass()))) {
							UMLClassRenameDiff minClassRenameDiff2 = renameDiffSet.first();
							int matchedMembers1 = minClassRenameDiff.getMatchResult().getMatchedOperations() + minClassRenameDiff.getMatchResult().getMatchedAttributes();
							int matchedMembers2 = minClassRenameDiff2.getMatchResult().getMatchedOperations() + minClassRenameDiff2.getMatchResult().getMatchedAttributes();
							if(matchedMembers2 > matchedMembers1) {
								minClassRenameDiff2.process();
								classRenameDiffList.add(minClassRenameDiff2);
								removedClasses.remove(minClassRenameDiff2.getOriginalClass());
								addedClassIterator.remove();
							}
							else {
								minClassRenameDiff.process();
								classRenameDiffList.add(minClassRenameDiff);
								removedClasses.remove(minClassRenameDiff.getOriginalClass());
								addedClassIterator.remove();
							}
						}
						else {
							minClassRenameDiff.process();
							classRenameDiffList.add(minClassRenameDiff);
							removedClasses.remove(minClassRenameDiff.getOriginalClass());
							addedClassIterator.remove();
						}
					}
				}
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

	private TreeSet<UMLClassRenameDiff> findRenameMatchesForRemovedClass(UMLClass removedClass, UMLClassMatcher matcher) {
		TreeSet<UMLClassRenameDiff> diffSet = new TreeSet<UMLClassRenameDiff>(new ClassRenameComparator());
		for(Iterator<UMLClass> addedClassIterator = addedClasses.iterator(); addedClassIterator.hasNext();) {
			UMLClass addedClass = addedClassIterator.next();
			if(matcher instanceof UMLClassMatcher.RelaxedRename) {
				Pair<UMLClass, UMLClass> pair = Pair.of(removedClass, addedClass);
				if(processedClassPairs.contains(pair)) {
					continue;
				}
				else {
					processedClassPairs.add(pair);
				}
			}
			MatchResult matchResult = matcher.match(removedClass, addedClass);
			if(matchResult.isMatch()) {
				if(!conflictingMoveOfTopLevelClass(removedClass, addedClass) && !innerClassWithTheSameName(removedClass, addedClass)) {
					UMLClassRenameDiff classRenameDiff = new UMLClassRenameDiff(removedClass, addedClass, this, matchResult);
					diffSet.add(classRenameDiff);
				}
			}
		}
		return diffSet;
	}

	private TreeSet<UMLClassRenameDiff> findRenameMatchesForAddedClass(UMLClass addedClass, UMLClassMatcher matcher) {
		TreeSet<UMLClassRenameDiff> diffSet = new TreeSet<UMLClassRenameDiff>(new ClassRenameComparator());
		for(Iterator<UMLClass> removedClassIterator = removedClasses.iterator(); removedClassIterator.hasNext();) {
			UMLClass removedClass = removedClassIterator.next();
			if(matcher instanceof UMLClassMatcher.RelaxedRename) {
				Pair<UMLClass, UMLClass> pair = Pair.of(removedClass, addedClass);
				if(processedClassPairs.contains(pair)) {
					continue;
				}
				else {
					processedClassPairs.add(pair);
				}
			}
			MatchResult matchResult = matcher.match(removedClass, addedClass);
			if(matchResult.isMatch()) {
				if(!conflictingMoveOfTopLevelClass(removedClass, addedClass) && !innerClassWithTheSameName(removedClass, addedClass)) {
					UMLClassRenameDiff classRenameDiff = new UMLClassRenameDiff(removedClass, addedClass, this, matchResult);
					diffSet.add(classRenameDiff);
				}
			}
		}
		return diffSet;
	}

	private boolean innerClassWithTheSameName(UMLClass removedClass, UMLClass addedClass) {
		if(!removedClass.isTopLevel() && !addedClass.isTopLevel()) {
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

	private List<MoveAttributeRefactoring> checkForAttributeMovesIncludingRemovedClasses(Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> refactorings) {
		List<UMLAttribute> addedAttributes = getAddedAttributesInCommonClasses();
		/*for(UMLClass addedClass : addedClasses) {
    	  addedAttributes.addAll(addedClass.getAttributes());
      }*/
		List<UMLAttribute> removedAttributes = getRemovedAttributesInCommonClasses();
		for(UMLClass removedClass : removedClasses) {
			removedAttributes.addAll(removedClass.getAttributes());
		}
		return checkForAttributeMoves(addedAttributes, removedAttributes, renameMap, refactorings);
	}

	private List<MoveAttributeRefactoring> checkForAttributeMovesIncludingAddedClasses(Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> refactorings) {
		List<UMLAttribute> addedAttributes = getAddedAttributesInCommonClasses();
		for(UMLClass addedClass : addedClasses) {
			addedAttributes.addAll(addedClass.getAttributes());
		}
		List<UMLAttribute> removedAttributes = getRemovedAttributesInCommonClasses();
		/*for(UMLClass removedClass : removedClasses) {
    	  removedAttributes.addAll(removedClass.getAttributes());
      }*/
		return checkForAttributeMoves(addedAttributes, removedAttributes, renameMap, refactorings);
	}

	private List<MoveAttributeRefactoring> checkForAttributeMovesBetweenCommonClasses(Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> refactorings) {
		List<UMLAttribute> addedAttributes = getAddedAttributesInCommonClasses();
		List<UMLAttribute> removedAttributes = getRemovedAttributesInCommonClasses();
		return checkForAttributeMoves(addedAttributes, removedAttributes, renameMap, refactorings);
	}

	private List<MoveAttributeRefactoring> checkForAttributeMovesBetweenRemovedAndAddedClasses(Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> refactorings) {
		List<UMLAttribute> addedAttributes = new ArrayList<UMLAttribute>();
		for(UMLClass addedClass : addedClasses) {
			addedAttributes.addAll(addedClass.getAttributes());
		}
		List<UMLAttribute> removedAttributes = new ArrayList<UMLAttribute>();
		for(UMLClass removedClass : removedClasses) {
			removedAttributes.addAll(removedClass.getAttributes());
		}
		return checkForAttributeMoves(addedAttributes, removedAttributes, renameMap, refactorings);
	}

	private List<MoveAttributeRefactoring> checkForAttributeMoves(List<UMLAttribute> addedAttributes, List<UMLAttribute> removedAttributes,
			Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> pastRefactorings) {
		List<MoveAttributeRefactoring> refactorings = new ArrayList<MoveAttributeRefactoring>();
		if(addedAttributes.size() <= removedAttributes.size()) {
			for(UMLAttribute addedAttribute : addedAttributes) {
				List<MoveAttributeRefactoring> candidates = new ArrayList<MoveAttributeRefactoring>();
				for(UMLAttribute removedAttribute : removedAttributes) {
					MoveAttributeRefactoring candidate = processPairOfAttributes(addedAttribute, removedAttribute, renameMap, pastRefactorings);
					if(candidate != null) {
						candidates.add(candidate);
					}
				}
				processCandidates(candidates, refactorings);
			}
		}
		else {
			for(UMLAttribute removedAttribute : removedAttributes) {
				List<MoveAttributeRefactoring> candidates = new ArrayList<MoveAttributeRefactoring>();
				for(UMLAttribute addedAttribute : addedAttributes) {
					MoveAttributeRefactoring candidate = processPairOfAttributes(addedAttribute, removedAttribute, renameMap, pastRefactorings);
					if(candidate != null) {
						candidates.add(candidate);
					}
				}
				processCandidates(candidates, refactorings);
			}
		}
		return refactorings;
	}

	private List<Refactoring> filterOutDuplicateRefactorings(Set<Refactoring> refactorings) {
		List<Refactoring> filtered = new ArrayList<Refactoring>();
		Map<String, List<Refactoring>> map = new LinkedHashMap<String, List<Refactoring>>();
		for(Refactoring ref : refactorings) {
			if(map.containsKey(ref.toString())) {
				map.get(ref.toString()).add(ref);
			}
			else {
				List<Refactoring> refs = new ArrayList<Refactoring>();
				refs.add(ref);
				map.put(ref.toString(), refs);
			}
		}
		for(String key : map.keySet()) {
			List<Refactoring> refs = map.get(key);
			if(refs.size() == 1) {
				filtered.addAll(refs);
			}
			else {
				filtered.addAll(filterOutBasedOnFilePath(refs));
			}
		}
		return filtered;
	}

	private List<Refactoring> filterOutBasedOnFilePath(List<Refactoring> refs) {
		List<Refactoring> filtered = new ArrayList<Refactoring>();
		Map<String, List<Refactoring>> groupBySourceFilePath = new LinkedHashMap<String, List<Refactoring>>();
		for(Refactoring ref : refs) {
			String sourceFilePath = ref.getInvolvedClassesBeforeRefactoring().iterator().next().getLeft();
			if(groupBySourceFilePath.containsKey(sourceFilePath)) {
				groupBySourceFilePath.get(sourceFilePath).add(ref);
			}
			else {
				List<Refactoring> refs2 = new ArrayList<Refactoring>();
				refs2.add(ref);
				groupBySourceFilePath.put(sourceFilePath, refs2);
			}
		}
		for(String sourceFilePath : groupBySourceFilePath.keySet()) {
			List<Refactoring> sourceFilePathGroup = groupBySourceFilePath.get(sourceFilePath);
			TreeMap<Integer, List<Refactoring>> groupByLongestCommonSourceFilePath = new TreeMap<Integer, List<Refactoring>>();
			for(Refactoring ref : sourceFilePathGroup) {
				String longestCommonFilePathPrefix = PrefixSuffixUtils.longestCommonPrefix(ref.getInvolvedClassesBeforeRefactoring().iterator().next().getLeft(),
						ref.getInvolvedClassesAfterRefactoring().iterator().next().getLeft());
				int length = longestCommonFilePathPrefix.length();
				if(groupByLongestCommonSourceFilePath.containsKey(length)) {
					groupByLongestCommonSourceFilePath.get(length).add(ref);
				}
				else {
					List<Refactoring> refs2 = new ArrayList<Refactoring>();
					refs2.add(ref);
					groupByLongestCommonSourceFilePath.put(length, refs2);
				}
			}
			filtered.addAll(groupByLongestCommonSourceFilePath.lastEntry().getValue());
		}
		return filtered;
	}

	private void processCandidates(List<MoveAttributeRefactoring> candidates, List<MoveAttributeRefactoring> refactorings) {
		if(candidates.size() > 1) {
			TreeMap<Integer, List<MoveAttributeRefactoring>> map = new TreeMap<Integer, List<MoveAttributeRefactoring>>();
			for(MoveAttributeRefactoring candidate : candidates) {
				int compatibility = computeCompatibility(candidate);
				if(map.containsKey(compatibility)) {
					map.get(compatibility).add(candidate);
				}
				else {
					List<MoveAttributeRefactoring> refs = new ArrayList<MoveAttributeRefactoring>();
					refs.add(candidate);
					map.put(compatibility, refs);
				}
			}
			int maxCompatibility = map.lastKey();
			refactorings.addAll(map.get(maxCompatibility));
		}
		else if(candidates.size() == 1) {
			refactorings.addAll(candidates);
		}
	}

	private MoveAttributeRefactoring processPairOfAttributes(UMLAttribute addedAttribute, UMLAttribute removedAttribute, Map<Replacement,
			Set<CandidateAttributeRefactoring>> renameMap, Set<Refactoring> pastRefactorings) {
		if(!removedAttribute.getName().equals(addedAttribute.getName()) && movedAttributeRenamed(removedAttribute.getVariableDeclaration(), addedAttribute.getVariableDeclaration(), pastRefactorings).size() > 0) {
			return null;
		}
		if(addedAttribute.getName().equals(removedAttribute.getName()) &&
				addedAttribute.getType().equals(removedAttribute.getType())) {
			if(isSubclassOf(removedAttribute.getClassName(), addedAttribute.getClassName())) {
				PullUpAttributeRefactoring pullUpAttribute = new PullUpAttributeRefactoring(removedAttribute, addedAttribute);
				return pullUpAttribute;
			}
			else if(isSubclassOf(addedAttribute.getClassName(), removedAttribute.getClassName())) {
				PushDownAttributeRefactoring pushDownAttribute = new PushDownAttributeRefactoring(removedAttribute, addedAttribute);
				return pushDownAttribute;
			}
			else if(sourceClassImportsTargetClass(removedAttribute.getClassName(), addedAttribute.getClassName()) ||
					targetClassImportsSourceClass(removedAttribute.getClassName(), addedAttribute.getClassName())) {
				if(!initializerContainsTypeLiteral(addedAttribute, removedAttribute)) {
					MoveAttributeRefactoring moveAttribute = new MoveAttributeRefactoring(removedAttribute, addedAttribute);
					return moveAttribute;
				}
			}
		}
		if(!removedAttribute.getClassName().equals(addedAttribute.getClassName()) &&
				addedAttribute.getType().equals(removedAttribute.getType())) {
			Replacement rename = new Replacement(removedAttribute.getName(), addedAttribute.getName(), ReplacementType.VARIABLE_NAME);
			if(renameMap.containsKey(rename)) {
				Set<CandidateAttributeRefactoring> candidates = renameMap.get(rename);
				MoveAndRenameAttributeRefactoring moveAttribute = new MoveAndRenameAttributeRefactoring(removedAttribute, addedAttribute, candidates);
				return moveAttribute;
			}
		}
		return null;
	}

	private boolean initializerContainsTypeLiteral(UMLAttribute addedAttribute, UMLAttribute removedAttribute) {
		VariableDeclaration v1 = addedAttribute.getVariableDeclaration();
		VariableDeclaration v2 = removedAttribute.getVariableDeclaration();
		if(v1.getInitializer() != null && v2.getInitializer() != null) {
			List<String> typeLiterals1 = v1.getInitializer().getTypeLiterals();
			List<String> typeLiterals2 = v2.getInitializer().getTypeLiterals();
			String className1 = addedAttribute.getNonQualifiedClassName();
			String className2 = removedAttribute.getNonQualifiedClassName();
			if(typeLiterals1.contains(className1 + ".class") && typeLiterals2.contains(className2 + ".class") &&
					addedAttribute.getType().getClassType().endsWith("Logger") && removedAttribute.getType().getClassType().endsWith("Logger")) {
				return true;
			}
		}
		return false;
	}

	private int computeCompatibility(MoveAttributeRefactoring candidate) {
		int count = 0;
		for(Refactoring ref : refactorings) {
			if(ref instanceof MoveOperationRefactoring) {
				MoveOperationRefactoring moveRef = (MoveOperationRefactoring)ref;
				if(moveRef.compatibleWith(candidate)) {
					count++;
				}
			}
		}
		UMLClassBaseDiff sourceClassDiff = getUMLClassDiff(candidate.getSourceClassName());
		UMLClassBaseDiff targetClassDiff = getUMLClassDiff(candidate.getTargetClassName());
		if(sourceClassDiff != null) {
			UMLType targetSuperclass = null;
			if(targetClassDiff != null) {
				targetSuperclass = targetClassDiff.getSuperclass();
			}
			List<UMLAttribute> addedAttributes = sourceClassDiff.getAddedAttributes();
			for(UMLAttribute addedAttribute : addedAttributes) {
				if(looksLikeSameType(addedAttribute.getType().getClassType(), candidate.getTargetClassName())) {
					count++;
				}
				if(targetSuperclass != null && looksLikeSameType(addedAttribute.getType().getClassType(), targetSuperclass.getClassType())) {
					count++;
				}
			}
			List<UMLAttribute> originalAttributes = sourceClassDiff.originalClassAttributesOfType(candidate.getTargetClassName());
			List<UMLAttribute> nextAttributes = sourceClassDiff.nextClassAttributesOfType(candidate.getTargetClassName());
			if(targetSuperclass != null) {
				originalAttributes.addAll(sourceClassDiff.originalClassAttributesOfType(targetSuperclass.getClassType()));
				nextAttributes.addAll(sourceClassDiff.nextClassAttributesOfType(targetSuperclass.getClassType()));
			}
			Set<UMLAttribute> intersection = new LinkedHashSet<UMLAttribute>(originalAttributes);
			intersection.retainAll(nextAttributes);
			if(!intersection.isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private boolean sourceClassImportsSuperclassOfTargetClass(String sourceClassName, String targetClassName) {
		UMLClassBaseDiff targetClassDiff = getUMLClassDiff(targetClassName);
		if(targetClassDiff != null && targetClassDiff.getSuperclass() != null) {
			UMLClassBaseDiff superclassOfTargetClassDiff = getUMLClassDiff(targetClassDiff.getSuperclass());
			if(superclassOfTargetClassDiff != null) {
				return sourceClassImportsTargetClass(sourceClassName, superclassOfTargetClassDiff.getNextClassName());
			}
		}
		return false;
	}

	private boolean sourceClassImportsTargetClass(String sourceClassName, String targetClassName) {
		UMLClassBaseDiff classDiff = getUMLClassDiff(sourceClassName);
		if(classDiff == null) {
			classDiff = getUMLClassDiff(UMLType.extractTypeObject(sourceClassName));
		}
		if(classDiff != null) {
			return classDiff.nextClassImportsType(targetClassName) || classDiff.originalClassImportsType(targetClassName);
		}
		UMLClass removedClass = getRemovedClass(sourceClassName);
		if(removedClass == null) {
			removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(sourceClassName));
		}
		if(removedClass != null) {
			return removedClass.importsType(targetClassName);
		}
		return false;
	}

	private boolean targetClassImportsSourceClass(String sourceClassName, String targetClassName) {
		UMLClassBaseDiff classDiff = getUMLClassDiff(targetClassName);
		if(classDiff == null) {
			classDiff = getUMLClassDiff(UMLType.extractTypeObject(targetClassName));
		}
		if(classDiff != null) {
			return classDiff.originalClassImportsType(sourceClassName) || classDiff.nextClassImportsType(sourceClassName);
		}
		UMLClass addedClass = getAddedClass(targetClassName);
		if(addedClass == null) {
			addedClass = looksLikeAddedClass(UMLType.extractTypeObject(targetClassName));
		}
		if(addedClass != null) {
			return addedClass.importsType(sourceClassName);
		}
		return false;
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

	private List<UMLOperation> getAddedAndExtractedOperationsInCommonClasses() throws RefactoringMinerTimedOutException {
		List<UMLOperation> addedOperations = new ArrayList<UMLOperation>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			addedOperations.addAll(classDiff.getAddedOperations());
			for(Refactoring ref : classDiff.getRefactoringsBeforePostProcessing()) {
				if(ref instanceof ExtractOperationRefactoring) {
					ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
					addedOperations.add(extractRef.getExtractedOperation());
				}
			}
		}
		return addedOperations;
	}

	private List<UMLOperation> getAddedOperationsInMovedAndRenamedClasses() {
		List<UMLOperation> addedOperations = new ArrayList<UMLOperation>();
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			addedOperations.addAll(classDiff.getAddedOperations());
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			addedOperations.addAll(classDiff.getAddedOperations());
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
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

	private List<UMLOperation> getRemovedOperationsInCommonMovedRenamedClasses() {
		List<UMLOperation> removedOperations = new ArrayList<UMLOperation>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			removedOperations.addAll(classDiff.getRemovedOperations());
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			removedOperations.addAll(classDiff.getRemovedOperations());
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			removedOperations.addAll(classDiff.getRemovedOperations());
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			removedOperations.addAll(classDiff.getRemovedOperations());
		}
		return removedOperations;
	}

	private List<UMLOperation> getRemovedAndInlinedOperationsInCommonClasses() throws RefactoringMinerTimedOutException {
		List<UMLOperation> removedOperations = new ArrayList<UMLOperation>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			removedOperations.addAll(classDiff.getRemovedOperations());
			for(Refactoring ref : classDiff.getRefactoringsBeforePostProcessing()) {
				if(ref instanceof InlineOperationRefactoring) {
					InlineOperationRefactoring extractRef = (InlineOperationRefactoring)ref;
					removedOperations.add(extractRef.getInlinedOperation());
				}
			}
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

	private List<UMLOperationBodyMapper> getOperationBodyMappersInMovedAndRenamedClasses() {
		List<UMLOperationBodyMapper> mappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			mappers.addAll(classDiff.getOperationBodyMapperList());
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			mappers.addAll(classDiff.getOperationBodyMapperList());
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			mappers.addAll(classDiff.getOperationBodyMapperList());
		}
		return mappers;
	}

	private List<ExtractClassRefactoring> identifyExtractClassRefactorings(List<? extends UMLClassBaseDiff> classDiffs) throws RefactoringMinerTimedOutException {
		List<ExtractClassRefactoring> refactorings = new ArrayList<ExtractClassRefactoring>();
		for(UMLClass addedClass : addedClasses) {
			TreeSet<CandidateExtractClassRefactoring> candidates = new TreeSet<CandidateExtractClassRefactoring>();
			UMLType addedClassSuperType = addedClass.getSuperclass();
			if(!addedClass.isInterface()) {
				for(UMLClassBaseDiff classDiff : classDiffs) {
					UMLType classDiffSuperType = classDiff.getNewSuperclass();
					boolean commonSuperType = addedClassSuperType != null && classDiffSuperType != null &&
							addedClassSuperType.getClassType().equals(classDiffSuperType.getClassType());
					boolean commonInterface = false;
					for(UMLType addedClassInterface : addedClass.getImplementedInterfaces()) {
						for(UMLType classDiffInterface : classDiff.getNextClass().getImplementedInterfaces()) {
							if(addedClassInterface.getClassType().equals(classDiffInterface.getClassType())) {
								commonInterface = true;
								break;
							}
						}
						if(commonInterface)
							break;
					}
					boolean extendsAddedClass = classDiff.getNewSuperclass() != null &&
							addedClass.getName().endsWith("." + classDiff.getNewSuperclass().getClassType());
					UMLAttribute attributeOfExtractedClassType = attributeOfExtractedClassType(addedClass, classDiff);
					boolean isTestClass =  addedClass.isTestClass() && classDiff.getOriginalClass().isTestClass();
					if((!commonSuperType && !commonInterface && !extendsAddedClass) || attributeOfExtractedClassType != null || isTestClass) {
						ExtractClassRefactoring refactoring = atLeastOneCommonAttributeOrOperation(addedClass, classDiff, attributeOfExtractedClassType);
						if(refactoring != null) {
							CandidateExtractClassRefactoring candidate = new CandidateExtractClassRefactoring(classDiff, refactoring);
							candidates.add(candidate);
						}
					}
				}
			}
			if(!candidates.isEmpty()) {
				CandidateExtractClassRefactoring firstCandidate = candidates.first();
				if(firstCandidate.innerClassExtract() || firstCandidate.subclassExtract()) {
					detectSubRefactorings(firstCandidate.getClassDiff(),
							firstCandidate.getRefactoring().getExtractedClass(),
							firstCandidate.getRefactoring().getRefactoringType());
					refactorings.add(firstCandidate.getRefactoring());
				}
				else {
					for(CandidateExtractClassRefactoring candidate : candidates) {
						detectSubRefactorings(candidate.getClassDiff(),
								candidate.getRefactoring().getExtractedClass(),
								candidate.getRefactoring().getRefactoringType());
						refactorings.add(candidate.getRefactoring());
					}
				}
			}
		}
		return refactorings;
	}

	private UMLAttribute attributeOfExtractedClassType(UMLClass umlClass, UMLClassBaseDiff classDiff) {
		List<UMLAttribute> addedAttributes = classDiff.getAddedAttributes();
		for(UMLAttribute addedAttribute : addedAttributes) {
			if(umlClass.getName().endsWith("." + addedAttribute.getType().getClassType())) {
				return addedAttribute;
			}
		}
		return null;
	}

	private ExtractClassRefactoring atLeastOneCommonAttributeOrOperation(UMLClass umlClass, UMLClassBaseDiff classDiff, UMLAttribute attributeOfExtractedClassType) {
		Map<UMLOperation, UMLOperation> commonOperations = new LinkedHashMap<>();
		for(UMLOperation operation : classDiff.getRemovedOperations()) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				UMLOperation matchedOperation = umlClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
				if(matchedOperation != null) {
					commonOperations.put(operation, matchedOperation);
				}
			}
		}
		Map<UMLAttribute, UMLAttribute> commonAttributes = new LinkedHashMap<>();
		for(UMLAttribute attribute : classDiff.getRemovedAttributes()) {
			UMLAttribute matchedAttribute = umlClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(matchedAttribute != null) {
				commonAttributes.put(attribute, matchedAttribute);
			}
		}
		int threshold = 1;
		if(attributeOfExtractedClassType != null || classDiff.getNextClass().isInnerClass(umlClass))
			threshold = 0;
		if(commonOperations.size() > threshold || commonAttributes.size() > threshold) {
			return new ExtractClassRefactoring(umlClass, classDiff, commonOperations, commonAttributes, attributeOfExtractedClassType);
		}
		return null;
	}

	private List<CollapseHierarchyRefactoring> identifyCollapseHierarchyRefactorings() throws RefactoringMinerTimedOutException {
		List<CollapseHierarchyRefactoring> refactorings = new ArrayList<CollapseHierarchyRefactoring>();
		for(UMLClass removedClass : removedClasses) {
			for(UMLRealization removedRealization : removedRealizations) {
				UMLClass client = removedRealization.getClient();
				UMLClassBaseDiff supplierClassDiff = getUMLClassDiff(UMLType.extractTypeObject(removedRealization.getSupplier()));
				if(removedClass.equals(client) && supplierClassDiff != null) {
					int commonOperations = 0;
					for(UMLOperation operation : removedClass.getOperations()) {
						if(supplierClassDiff.containsConcreteOperationWithTheSameSignatureInNextClass(operation)) {
							commonOperations++;
						}
					}
					if(commonOperations > 0) {
						CollapseHierarchyRefactoring refactoring = new CollapseHierarchyRefactoring(removedClass, supplierClassDiff.getNextClass());
						refactorings.add(refactoring);
						for(UMLOperation removedOperation : removedClass.getOperations()) {
							UMLOperation addedOperation = supplierClassDiff.containsAddedOperationWithTheSameSignature(removedOperation);
							if(addedOperation != null) {
								supplierClassDiff.getAddedOperations().remove(addedOperation);
								Refactoring ref = new PullUpOperationRefactoring(removedOperation, addedOperation);
								this.refactorings.add(ref);
								UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(removedOperation, addedOperation, supplierClassDiff);
								this.refactorings.addAll(mapper.getRefactorings());
								checkForExtractedOperationsWithinMovedMethod(mapper, supplierClassDiff.getRemovedOperations(), supplierClassDiff.getNextClass());
							}
						}
						for(UMLAttribute removedAttribute : removedClass.getAttributes()) {
							UMLAttribute addedAttribute = supplierClassDiff.containsAddedAttributeWithTheSameSignature(removedAttribute);
							if(addedAttribute != null) {
								supplierClassDiff.getAddedAttributes().remove(addedAttribute);
								Refactoring ref = new PullUpAttributeRefactoring(removedAttribute, addedAttribute);
								this.refactorings.add(ref);
							}
						}
					}
				}
			}
		}
		return refactorings;
	}

	private List<ExtractSuperclassRefactoring> identifyExtractSuperclassRefactorings() throws RefactoringMinerTimedOutException {
		List<ExtractSuperclassRefactoring> refactorings = new ArrayList<ExtractSuperclassRefactoring>();
		for(UMLClass addedClass : addedClasses) {
			Set<UMLClass> subclassSetBefore = new LinkedHashSet<UMLClass>();
			Set<UMLClass> subclassSetAfter = new LinkedHashSet<UMLClass>();
			String addedClassName = addedClass.getName();
			for(UMLGeneralization addedGeneralization : addedGeneralizations) {
				processAddedGeneralization(addedClass, subclassSetBefore, subclassSetAfter, addedGeneralization);
			}
			for(UMLGeneralizationDiff generalizationDiff : generalizationDiffList) {
				UMLGeneralization addedGeneralization = generalizationDiff.getAddedGeneralization();
				UMLGeneralization removedGeneralization = generalizationDiff.getRemovedGeneralization();
				if(!addedGeneralization.getParent().equals(removedGeneralization.getParent())) {
					processAddedGeneralization(addedClass, subclassSetBefore, subclassSetAfter, addedGeneralization);
				}
			}
			for(UMLRealization addedRealization : addedRealizations) {
				String supplier = addedRealization.getSupplier();
				if(looksLikeSameType(supplier, addedClassName) && topLevelOrSameOuterClass(addedClass, addedRealization.getClient()) && getAddedClass(addedRealization.getClient().getName()) == null) {
					UMLClassBaseDiff clientClassDiff = getUMLClassDiff(addedRealization.getClient().getName());
					int implementedInterfaceOperations = 0;
					boolean clientImplementsSupplier = false;
					if(clientClassDiff != null) {
						for(UMLOperation interfaceOperation : addedClass.getOperations()) {
							if(clientClassDiff.containsOperationWithTheSameSignatureInOriginalClass(interfaceOperation)) {
								implementedInterfaceOperations++;
							}
						}
						clientImplementsSupplier = clientClassDiff.getOriginalClass().getImplementedInterfaces().contains(UMLType.extractTypeObject(supplier));
					}
					if((implementedInterfaceOperations > 0 || addedClass.getOperations().size() == 0) && !clientImplementsSupplier && clientClassDiff != null) {
						subclassSetBefore.add(clientClassDiff.getOriginalClass());
						subclassSetAfter.add(clientClassDiff.getNextClass());
					}
				}
			}
			if(subclassSetBefore.size() > 0) {
				ExtractSuperclassRefactoring extractSuperclassRefactoring = new ExtractSuperclassRefactoring(addedClass, subclassSetBefore, subclassSetAfter);
				refactorings.add(extractSuperclassRefactoring);
			}
		}
		return refactorings;
	}

	private void processAddedGeneralization(UMLClass addedClass, Set<UMLClass> subclassSetBefore, Set<UMLClass> subclassSetAfter, UMLGeneralization addedGeneralization) throws RefactoringMinerTimedOutException {
		String parent = addedGeneralization.getParent();
		UMLClass subclass = addedGeneralization.getChild();
		if(looksLikeSameType(parent, addedClass.getName()) && topLevelOrSameOuterClass(addedClass, subclass) && getAddedClass(subclass.getName()) == null) {
			UMLClassBaseDiff subclassDiff = getUMLClassDiff(subclass.getName());
			if(subclassDiff != null) {
				detectSubRefactorings(subclassDiff, addedClass, RefactoringType.EXTRACT_SUPERCLASS);
				subclassSetBefore.add(subclassDiff.getOriginalClass());
				subclassSetAfter.add(subclassDiff.getNextClass());
			}
		}
	}

	private void detectSubRefactorings(UMLClassBaseDiff classDiff, UMLClass addedClass, RefactoringType parentType) throws RefactoringMinerTimedOutException {
		for(UMLOperation addedOperation : addedClass.getOperations()) {
			UMLOperation removedOperation = classDiff.containsRemovedOperationWithTheSameSignature(addedOperation);
			if(parentType.equals(RefactoringType.MERGE_CLASS) && removedOperation == null) {
				removedOperation = classDiff.getOriginalClass().operationWithTheSameSignature(addedOperation);
				if(addedOperation.isConstructor() && removedOperation == null) {
					RenamePattern renamePattern = new RenamePattern(classDiff.getOriginalClass().getNonQualifiedName(), classDiff.getNextClass().getNonQualifiedName());
					removedOperation = classDiff.getOriginalClass().operationWithTheSameRenamePattern(addedOperation, renamePattern);
				}
			}
			if(removedOperation != null) {
				classDiff.getRemovedOperations().remove(removedOperation);
				MoveOperationRefactoring ref = null;
				if(parentType.equals(RefactoringType.EXTRACT_SUPERCLASS)) {
					ref = new PullUpOperationRefactoring(removedOperation, addedOperation);
				}
				else if(parentType.equals(RefactoringType.EXTRACT_CLASS) || parentType.equals(RefactoringType.MERGE_CLASS)) {
					ref = new MoveOperationRefactoring(removedOperation, addedOperation);
				}
				else if(parentType.equals(RefactoringType.EXTRACT_SUBCLASS)) {
					ref = new PushDownOperationRefactoring(removedOperation, addedOperation);
				}
				this.refactorings.add(ref);
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(removedOperation, addedOperation, classDiff);
				refactorings.addAll(mapper.getRefactorings());
				ref.setBodyMapper(mapper);
				checkForExtractedOperationsWithinMovedMethod(mapper, classDiff.getRemovedOperations(), addedClass);
			}
		}
		for(UMLAttribute addedAttribute : addedClass.getAttributes()) {
			UMLAttribute removedAttribute = classDiff.containsRemovedAttributeWithTheSameSignature(addedAttribute);
			if(parentType.equals(RefactoringType.MERGE_CLASS) && removedAttribute == null) {
				removedAttribute = classDiff.getOriginalClass().attributeWithTheSameSignature(addedAttribute);
			}
			if(removedAttribute != null) {
				classDiff.getRemovedAttributes().remove(removedAttribute);
				Refactoring ref = null;
				if(parentType.equals(RefactoringType.EXTRACT_SUPERCLASS)) {
					ref = new PullUpAttributeRefactoring(removedAttribute, addedAttribute);
				}
				else if(parentType.equals(RefactoringType.EXTRACT_CLASS) || parentType.equals(RefactoringType.MERGE_CLASS)) {
					ref = new MoveAttributeRefactoring(removedAttribute, addedAttribute);
				}
				else if(parentType.equals(RefactoringType.EXTRACT_SUBCLASS)) {
					ref = new PushDownAttributeRefactoring(removedAttribute, addedAttribute);
				}
				this.refactorings.add(ref);
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, Collections.emptyList()); 
				refactorings.addAll(attributeDiff.getRefactorings()); 
			}
		}
	}

	private void checkForExtractedOperationsWithinMovedMethod(UMLOperationBodyMapper movedMethodMapper, List<UMLOperation> potentiallyMovedOperations, UMLClass addedClass) throws RefactoringMinerTimedOutException {
		VariableDeclarationContainer removedOperation = movedMethodMapper.getContainer1();
		VariableDeclarationContainer addedOperation = movedMethodMapper.getContainer2();
		List<AbstractCall> removedInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedInvocations);
		intersection.retainAll(addedInvocations);
		Set<AbstractCall> newInvocations = new LinkedHashSet<AbstractCall>(addedInvocations);
		newInvocations.removeAll(intersection);
		for(AbstractCall newInvocation : newInvocations) {
			for(UMLOperation operation : addedClass.getOperations()) {
				if(!operation.isAbstract() && !operation.hasEmptyBody() &&
						newInvocation.matchesOperation(operation, addedOperation, this)) {
					ExtractOperationDetection detection = new ExtractOperationDetection(movedMethodMapper, potentiallyMovedOperations, addedClass.getOperations(), getUMLClassDiff(operation.getClassName()), this);
					List<ExtractOperationRefactoring> refs = detection.check(operation);
					this.refactorings.addAll(refs);
				}
			}
		}
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
			return UMLType.extractTypeObject(parent).equalClassType(UMLType.extractTypeObject(addedClassName));
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
		List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = new ArrayList<MoveSourceFolderRefactoring>();
		for(UMLClassMoveDiff classMoveDiff : classMoveDiffList) {
			UMLClass originalClass = classMoveDiff.getOriginalClass();
			String originalName = originalClass.getName();
			UMLClass movedClass = classMoveDiff.getMovedClass();
			String movedName = movedClass.getName();

			String originalPath = originalClass.getSourceFile();
			String movedPath = movedClass.getSourceFile();
			String originalPathPrefix = "";
			if(originalPath.contains("/")) {
				originalPathPrefix = originalPath.substring(0, originalPath.lastIndexOf('/'));
			}
			String movedPathPrefix = "";
			if(movedPath.contains("/")) {
				movedPathPrefix = movedPath.substring(0, movedPath.lastIndexOf('/'));
			}

			if (!originalName.equals(movedName)) {
				MoveClassRefactoring refactoring = new MoveClassRefactoring(originalClass, movedClass);
				RenamePattern renamePattern = refactoring.getRenamePattern();
				//check if the the original path is a substring of the moved path and vice versa
				if(renamePattern.getBefore().contains(renamePattern.getAfter()) ||
						renamePattern.getAfter().contains(renamePattern.getBefore()) ||
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
			} else if(!originalPathPrefix.equals(movedPathPrefix)) {
				MovedClassToAnotherSourceFolder refactoring = new MovedClassToAnotherSourceFolder(originalClass, movedClass, originalPathPrefix, movedPathPrefix);
				RenamePattern renamePattern = refactoring.getRenamePattern();
				boolean foundInMatchingMoveSourceFolderRefactoring = false;
				for(MoveSourceFolderRefactoring moveSourceFolderRefactoring : moveSourceFolderRefactorings) {
					if(moveSourceFolderRefactoring.getPattern().equals(renamePattern)) {
						moveSourceFolderRefactoring.addMovedClassToAnotherSourceFolder(refactoring);
						foundInMatchingMoveSourceFolderRefactoring = true;
						break;
					}
				}
				if(!foundInMatchingMoveSourceFolderRefactoring) {
					moveSourceFolderRefactorings.add(new MoveSourceFolderRefactoring(refactoring));
				}
			}
		}
		for(RenamePackageRefactoring renamePackageRefactoring : renamePackageRefactorings) {
			List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
			if(moveClassRefactorings.size() >= 1 && isSourcePackageDeleted(renamePackageRefactoring)) {
				refactorings.add(renamePackageRefactoring);
			}
			refactorings.addAll(moveClassRefactorings);
		}
		refactorings.addAll(moveSourceFolderRefactorings);
		return refactorings;
	}

	private boolean isSourcePackageDeleted(RenamePackageRefactoring renamePackageRefactoring) {
		for(String deletedFolderPath : deletedFolderPaths) {
			String originalPath = renamePackageRefactoring.getPattern().getBefore();
			//remove last .
			String trimmedOriginalPath = originalPath.endsWith(".") ? originalPath.substring(0, originalPath.length()-1) : originalPath;
			String convertedPackageToFilePath = trimmedOriginalPath.replaceAll("\\.", "/");
			if(deletedFolderPath.endsWith(convertedPackageToFilePath)) {
				return true;
			}
		}
		return false;
	}

	private List<Refactoring> getRenameClassRefactorings(List<RenamePackageRefactoring> previousRenamePackageRefactorings) {
		List<Refactoring> refactorings = new ArrayList<Refactoring>();
		List<RenamePackageRefactoring> newRenamePackageRefactorings = new ArrayList<RenamePackageRefactoring>();
		for(UMLClassRenameDiff classRenameDiff : classRenameDiffList) {
			if(classRenameDiff.samePackage()) {
				RenameClassRefactoring refactoring = new RenameClassRefactoring(classRenameDiff.getOriginalClass(), classRenameDiff.getRenamedClass());
				refactorings.add(refactoring);
			}
			else {
				MoveAndRenameClassRefactoring refactoring = new MoveAndRenameClassRefactoring(classRenameDiff.getOriginalClass(), classRenameDiff.getRenamedClass());
				RenamePattern renamePattern = refactoring.getRenamePattern();
				boolean foundInMatchingRenamePackageRefactoring = false;
				//search first in RenamePackage refactorings established from Move Class refactorings
				for(RenamePackageRefactoring renamePackageRefactoring : previousRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				if(!foundInMatchingRenamePackageRefactoring) {
					newRenamePackageRefactorings.add(new RenamePackageRefactoring(refactoring));
				}
				refactorings.add(refactoring);
			}
		}
		for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
			List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
			if(moveClassRefactorings.size() >= 1 && isSourcePackageDeleted(renamePackageRefactoring)) {
				refactorings.add(renamePackageRefactoring);
				previousRenamePackageRefactorings.add(renamePackageRefactoring);
			}
		}
		return refactorings;
	}

	private List<Refactoring> getMergeClassRefactorings(List<RenamePackageRefactoring> previousRenamePackageRefactorings) throws RefactoringMinerTimedOutException {
		List<Refactoring> refactorings = new ArrayList<Refactoring>();
		List<RenamePackageRefactoring> newRenamePackageRefactorings = new ArrayList<RenamePackageRefactoring>();
		for(UMLClassMergeDiff classMergeDiff : classMergeDiffList) {
			MergeClassRefactoring refactoring = new MergeClassRefactoring(classMergeDiff);
			for(UMLClassRenameDiff renameDiff : classMergeDiff.getClassRenameDiffs()) {
				detectSubRefactorings(renameDiff, renameDiff.getRenamedClass(), refactoring.getRefactoringType());
			}
			if(!classMergeDiff.samePackage()) {
				RenamePattern renamePattern = refactoring.getRenamePattern();
				boolean foundInMatchingRenamePackageRefactoring = false;
				//search first in RenamePackage refactorings established from Move Class refactorings
				for(RenamePackageRefactoring renamePackageRefactoring : previousRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				if(!foundInMatchingRenamePackageRefactoring) {
					newRenamePackageRefactorings.add(new RenamePackageRefactoring(refactoring));
				}
			}
			refactorings.add(refactoring);
		}
		for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
			List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
			if(moveClassRefactorings.size() >= 1 && isSourcePackageDeleted(renamePackageRefactoring)) {
				refactorings.add(renamePackageRefactoring);
				previousRenamePackageRefactorings.add(renamePackageRefactoring);
			}
		}
		return refactorings;
	}

	private List<Refactoring> getSplitClassRefactorings(List<RenamePackageRefactoring> previousRenamePackageRefactorings) throws RefactoringMinerTimedOutException {
		List<Refactoring> refactorings = new ArrayList<Refactoring>();
		List<RenamePackageRefactoring> newRenamePackageRefactorings = new ArrayList<RenamePackageRefactoring>();
		for(UMLClassSplitDiff classSplitDiff : classSplitDiffList) {
			SplitClassRefactoring refactoring = new SplitClassRefactoring(classSplitDiff);
			for(UMLClassRenameDiff renameDiff : classSplitDiff.getClassRenameDiffs()) {
				detectSubRefactorings(renameDiff, renameDiff.getRenamedClass(), refactoring.getRefactoringType());
			}
			if(!classSplitDiff.samePackage()) {
				RenamePattern renamePattern = refactoring.getRenamePattern();
				boolean foundInMatchingRenamePackageRefactoring = false;
				//search first in RenamePackage refactorings established from Move Class refactorings
				for(RenamePackageRefactoring renamePackageRefactoring : previousRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
					if(renamePackageRefactoring.getPattern().equals(renamePattern)) {
						renamePackageRefactoring.addMoveClassRefactoring(refactoring);
						foundInMatchingRenamePackageRefactoring = true;
						break;
					}
				}
				if(!foundInMatchingRenamePackageRefactoring) {
					newRenamePackageRefactorings.add(new RenamePackageRefactoring(refactoring));
				}
			}
			refactorings.add(refactoring);
		}
		for(RenamePackageRefactoring renamePackageRefactoring : newRenamePackageRefactorings) {
			List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
			if(moveClassRefactorings.size() >= 1 && isSourcePackageDeleted(renamePackageRefactoring)) {
				refactorings.add(renamePackageRefactoring);
				previousRenamePackageRefactorings.add(renamePackageRefactoring);
			}
		}
		return refactorings;
	}

	private void postProcessRenamedPackages(List<RenamePackageRefactoring> renamePackageRefactorings, Set<Refactoring> allRefactorings) {
		Map<String, Set<RenamePackageRefactoring>> groupBasedOnOriginalPackage = new LinkedHashMap<String, Set<RenamePackageRefactoring>>();
		Map<String, Set<RenamePackageRefactoring>> groupBasedOnNewPackage = new LinkedHashMap<String, Set<RenamePackageRefactoring>>();
		for(RenamePackageRefactoring refactoring : renamePackageRefactorings) {
			RenamePattern pattern = refactoring.getPattern();
			String before = pattern.getBefore();
			String after = pattern.getAfter();
			if(groupBasedOnOriginalPackage.containsKey(before)) {
				groupBasedOnOriginalPackage.get(before).add(refactoring);
			}
			else {
				Set<RenamePackageRefactoring> initialRenamePackageRefactorings = new LinkedHashSet<>();
				initialRenamePackageRefactorings.add(refactoring);
				groupBasedOnOriginalPackage.put(before, initialRenamePackageRefactorings);
			}
			if(groupBasedOnNewPackage.containsKey(after)) {
				groupBasedOnNewPackage.get(after).add(refactoring);
			}
			else {
				Set<RenamePackageRefactoring> initialRenamePackageRefactorings = new LinkedHashSet<>();
				initialRenamePackageRefactorings.add(refactoring);
				groupBasedOnNewPackage.put(after, initialRenamePackageRefactorings);
			}
		}
		for(String key : groupBasedOnOriginalPackage.keySet()) {
			Set<RenamePackageRefactoring> value = groupBasedOnOriginalPackage.get(key);
			if(value.size() > 1) {
				SplitPackageRefactoring refactoring = new SplitPackageRefactoring(value);
				allRefactorings.add(refactoring);
				allRefactorings.removeAll(value);
			}
		}
		for(String key : groupBasedOnNewPackage.keySet()) {
			Set<RenamePackageRefactoring> value = groupBasedOnNewPackage.get(key);
			if(value.size() > 1) {
				MergePackageRefactoring refactoring = new MergePackageRefactoring(value);
				allRefactorings.add(refactoring);
				allRefactorings.removeAll(value);
			}
		}
	}

	public List<Refactoring> getRefactorings() throws RefactoringMinerTimedOutException {
		Set<Refactoring> refactorings = getMoveRenameClassRefactorings();
		refactorings.addAll(identifyConvertAnonymousClassToTypeRefactorings());
		Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap = new LinkedHashMap<Replacement, Set<CandidateAttributeRefactoring>>();
		Map<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>> mergeMap = new LinkedHashMap<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			refactorings.addAll(classDiff.getRefactorings());
			extractMergePatterns(classDiff, mergeMap);
			extractRenamePatterns(classDiff, renameMap);
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			refactorings.addAll(classDiff.getRefactorings());
			extractMergePatterns(classDiff, mergeMap);
			extractRenamePatterns(classDiff, renameMap);
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			refactorings.addAll(classDiff.getRefactorings());
			extractMergePatterns(classDiff, mergeMap);
			extractRenamePatterns(classDiff, renameMap);
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			refactorings.addAll(classDiff.getRefactorings());
			extractMergePatterns(classDiff, mergeMap);
			extractRenamePatterns(classDiff, renameMap);
		}
		Map<RenamePattern, Integer> typeRenamePatternMap = typeRenamePatternMap(refactorings);
		for(RenamePattern pattern : typeRenamePatternMap.keySet()) {
			if(typeRenamePatternMap.get(pattern) > 1) {
				UMLClass removedClass = looksLikeRemovedClass(UMLType.extractTypeObject(pattern.getBefore()));
				UMLClass addedClass = looksLikeAddedClass(UMLType.extractTypeObject(pattern.getAfter()));
				if(removedClass != null && addedClass != null) {
					UMLClassRenameDiff renameDiff = new UMLClassRenameDiff(removedClass, addedClass, this, new Rename().match(removedClass, addedClass));
					renameDiff.process();
					refactorings.addAll(renameDiff.getRefactorings());
					extractMergePatterns(renameDiff, mergeMap);
					extractRenamePatterns(renameDiff, renameMap);
					classRenameDiffList.add(renameDiff);
					Refactoring refactoring = null;
					if(renameDiff.samePackage())
						refactoring = new RenameClassRefactoring(renameDiff.getOriginalClass(), renameDiff.getRenamedClass());
					else
						refactoring = new MoveAndRenameClassRefactoring(renameDiff.getOriginalClass(), renameDiff.getRenamedClass());
					refactorings.add(refactoring);
					removedClasses.remove(removedClass);
					addedClasses.remove(addedClass);
				}
			}
		}
		for(MergeVariableReplacement merge : mergeMap.keySet()) {
			UMLClassBaseDiff diff = null;
			for(String mergedVariable : merge.getMergedVariables()) {
				Replacement replacement = new Replacement(mergedVariable, merge.getAfter(), ReplacementType.VARIABLE_NAME);
				diff = getUMLClassDiffWithAttribute(replacement);
			}
			if(diff != null) {
				Set<UMLAttribute> mergedAttributes = new LinkedHashSet<UMLAttribute>();
				Set<VariableDeclaration> mergedVariables = new LinkedHashSet<VariableDeclaration>();
				for(String mergedVariable : merge.getMergedVariables()) {
					UMLAttribute a1 = diff.findAttributeInOriginalClass(mergedVariable);
					if(a1 != null) {
						mergedAttributes.add(a1);
						mergedVariables.add(a1.getVariableDeclaration());
					}
				}
				UMLAttribute a2 = diff.findAttributeInNextClass(merge.getAfter());
				Set<CandidateMergeVariableRefactoring> set = mergeMap.get(merge);
				if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && a2 != null) {
					MergeAttributeRefactoring ref = new MergeAttributeRefactoring(mergedAttributes, a2, diff.getOriginalClassName(), diff.getNextClassName(), set);
					if(!refactorings.contains(ref)) {
						refactorings.add(ref);
						Set<Refactoring> conflictingRefactorings = attributeRenamed(mergedVariables, a2.getVariableDeclaration(), refactorings);
						if(!conflictingRefactorings.isEmpty()) {
							refactorings.removeAll(conflictingRefactorings);
						}
					}
				}
			}
		}
		for(Replacement pattern : renameMap.keySet()) {
			UMLClassBaseDiff diff = getUMLClassDiffWithAttribute(pattern);
			Set<CandidateAttributeRefactoring> set = renameMap.get(pattern);
			for(CandidateAttributeRefactoring candidate : set) {
				if(candidate.getOriginalVariableDeclaration() == null && candidate.getRenamedVariableDeclaration() == null) {
					if(diff != null) {
						UMLAttribute a1 = diff.findAttributeInOriginalClass(pattern.getBefore());
						UMLAttribute a2 = diff.findAttributeInNextClass(pattern.getAfter());
						if(!diff.getOriginalClass().containsAttributeWithName(pattern.getAfter()) &&
								!diff.getNextClass().containsAttributeWithName(pattern.getBefore()) &&
								!attributeMerged(a1, a2, refactorings)) {
							UMLAttributeDiff attributeDiff = new UMLAttributeDiff(a1, a2, diff, this);
							Set<Refactoring> attributeDiffRefactorings = attributeDiff.getRefactorings(set);
							if(!refactorings.containsAll(attributeDiffRefactorings)) {
								refactorings.addAll(attributeDiffRefactorings);
								break;//it's not necessary to repeat the same process for all candidates in the set
							}
						}
					}
				}
				else if(candidate.getOriginalVariableDeclaration() != null) {
					List<UMLClassBaseDiff> diffs1 = getUMLClassDiffWithExistingAttributeAfter(pattern);
					List<UMLClassBaseDiff> diffs2 = getUMLClassDiffWithNewAttributeAfter(pattern);
					if(!diffs1.isEmpty()) {
						UMLClassBaseDiff diff1 = diffs1.get(0);
						UMLClassBaseDiff originalClassDiff = null;
						if(candidate.getOriginalAttribute() != null) {
							originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName()); 
						}
						else {
							originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
						}
						if(diffs1.size() > 1) {
							for(UMLClassBaseDiff classDiff : diffs1) {
								if(isSubclassOf(originalClassDiff.nextClass.getName(), classDiff.nextClass.getName())) {
									diff1 = classDiff;
									break;
								}
							}
						}
						UMLAttribute a2 = diff1.findAttributeInNextClass(pattern.getAfter());
						if(a2 != null) {
							if(candidate.getOriginalVariableDeclaration().isAttribute()) {
								if(originalClassDiff != null && originalClassDiff.removedAttributes.contains(candidate.getOriginalAttribute())) {
									ReplaceAttributeRefactoring ref = new ReplaceAttributeRefactoring(candidate.getOriginalAttribute(), a2, set);
									if(!refactorings.contains(ref)) {
										refactorings.add(ref);
										break;//it's not necessary to repeat the same process for all candidates in the set
									}
								}
							}
							else {
								RenameVariableRefactoring ref = new RenameVariableRefactoring(candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(), candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
								if(!refactorings.contains(ref)) {
									refactorings.add(ref);
									break;//it's not necessary to repeat the same process for all candidates in the set
								}
							}
						}
					}
					else if(!diffs2.isEmpty()) {
						UMLClassBaseDiff diff2 = diffs2.get(0);
						UMLClassBaseDiff originalClassDiff = null;
						if(candidate.getOriginalAttribute() != null) {
							originalClassDiff = getUMLClassDiff(candidate.getOriginalAttribute().getClassName()); 
						}
						else {
							originalClassDiff = getUMLClassDiff(candidate.getOperationBefore().getClassName());
						}
						if(diffs2.size() > 1) {
							for(UMLClassBaseDiff classDiff : diffs2) {
								if(isSubclassOf(originalClassDiff.nextClass.getName(), classDiff.nextClass.getName())) {
									diff2 = classDiff;
									break;
								}
							}
						}
						UMLAttribute a2 = diff2.findAttributeInNextClass(pattern.getAfter());
						if(a2 != null) {
							if(candidate.getOriginalVariableDeclaration().isAttribute()) {
								if(originalClassDiff != null && originalClassDiff.removedAttributes.contains(candidate.getOriginalAttribute())) {
									MoveAndRenameAttributeRefactoring ref = new MoveAndRenameAttributeRefactoring(candidate.getOriginalAttribute(), a2, set);
									if(!refactorings.contains(ref)) {
										refactorings.add(ref);
										break;//it's not necessary to repeat the same process for all candidates in the set
									}
								}
							}
							else {
								RenameVariableRefactoring ref = new RenameVariableRefactoring(candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(), candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
								if(!refactorings.contains(ref)) {
									refactorings.add(ref);
									break;//it's not necessary to repeat the same process for all candidates in the set
								}
							}
						}
					}
				}
			}
		}
		refactorings.addAll(identifyExtractSuperclassRefactorings());
		refactorings.addAll(identifyCollapseHierarchyRefactorings());
		refactorings.addAll(identifyExtractClassRefactorings(commonClassDiffList));
		refactorings.addAll(identifyExtractClassRefactorings(classMoveDiffList));
		refactorings.addAll(identifyExtractClassRefactorings(innerClassMoveDiffList));
		refactorings.addAll(identifyExtractClassRefactorings(classRenameDiffList));
		checkForOperationMovesBetweenCommonClasses();
		checkForOperationMovesIncludingRemovedAndAddedClasses();
		List<UMLOperation> addedAndExtractedOperationsInCommonClasses = getAddedAndExtractedOperationsInCommonClasses();
		List<UMLOperation> addedOperationsInMovedAndRenamedClasses = getAddedOperationsInMovedAndRenamedClasses();
		List<UMLOperation> allAddedOperations = new ArrayList<UMLOperation>(addedAndExtractedOperationsInCommonClasses);
		allAddedOperations.addAll(addedOperationsInMovedAndRenamedClasses);
		if(addedAndExtractedOperationsInCommonClasses.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
			checkForExtractedAndMovedOperations(getOperationBodyMappersInCommonClasses(), allAddedOperations);
		}
		if(addedOperationsInMovedAndRenamedClasses.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
			checkForExtractedAndMovedOperations(getOperationBodyMappersInMovedAndRenamedClasses(), allAddedOperations);
		}
		List<UMLOperation> removedAndInlinedOperationsInCommonClasses = getRemovedAndInlinedOperationsInCommonClasses();
		if(removedAndInlinedOperationsInCommonClasses.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) {
			checkForMovedAndInlinedOperations(getOperationBodyMappersInCommonClasses(), removedAndInlinedOperationsInCommonClasses);
		}
		List<MoveAttributeRefactoring> moveAttributeRefactorings = new ArrayList<MoveAttributeRefactoring>();
		moveAttributeRefactorings.addAll(checkForAttributeMovesBetweenCommonClasses(renameMap, refactorings));
		moveAttributeRefactorings.addAll(checkForAttributeMovesIncludingAddedClasses(renameMap, refactorings));
		moveAttributeRefactorings.addAll(checkForAttributeMovesIncludingRemovedClasses(renameMap, refactorings));
		refactorings.addAll(moveAttributeRefactorings);
		for(MoveAttributeRefactoring moveAttributeRefactoring : moveAttributeRefactorings) {
			UMLAttribute originalAttribute = moveAttributeRefactoring.getOriginalAttribute();
			UMLAttribute movedAttribute = moveAttributeRefactoring.getMovedAttribute();
			Set<Refactoring> conflictingRefactorings = movedAttributeRenamed(originalAttribute.getVariableDeclaration(), movedAttribute.getVariableDeclaration(), refactorings);
			if(!conflictingRefactorings.isEmpty()) {
				refactorings.removeAll(conflictingRefactorings);
			}
			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(originalAttribute, movedAttribute, Collections.emptyList());
			refactorings.addAll(attributeDiff.getRefactorings());
		}
		refactorings.addAll(this.refactorings);
		Set<Refactoring> packageRefactorings = filterPackageRefactorings(refactorings);
		for(UMLClassDiff classDiff : commonClassDiffList) {
			inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
			detectImportDeclarationChanges(classDiff, packageRefactorings);
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
			detectImportDeclarationChanges(classDiff, packageRefactorings);
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
			detectImportDeclarationChanges(classDiff, packageRefactorings);
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			inferMethodSignatureRelatedRefactorings(classDiff, refactorings);
			detectImportDeclarationChanges(classDiff, packageRefactorings);
		}
		return filterOutDuplicateRefactorings(refactorings);
	}

	public Set<Refactoring> getMoveRenameClassRefactorings() throws RefactoringMinerTimedOutException {
		if(moveRenameClassRefactorings != null) {
			return this.moveRenameClassRefactorings;
		}
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		refactorings.addAll(getMoveClassRefactorings());
		List<RenamePackageRefactoring> renamePackageRefactorings = new ArrayList<RenamePackageRefactoring>();
		for(Refactoring r : refactorings) {
			if(r instanceof RenamePackageRefactoring) {
				renamePackageRefactorings.add((RenamePackageRefactoring)r);
			}
		}
		refactorings.addAll(getRenameClassRefactorings(renamePackageRefactorings));
		refactorings.addAll(getMergeClassRefactorings(renamePackageRefactorings));
		refactorings.addAll(getSplitClassRefactorings(renamePackageRefactorings));
		postProcessRenamedPackages(renamePackageRefactorings, refactorings);
		this.moveRenameClassRefactorings = refactorings;
		return refactorings;
	}

	private Map<RenamePattern, Integer> typeRenamePatternMap(Set<Refactoring> refactorings) {
		Map<RenamePattern, Integer> typeRenamePatternMap = new LinkedHashMap<RenamePattern, Integer>();
		for(Refactoring ref : refactorings) {
			if(ref instanceof ChangeVariableTypeRefactoring) {
				ChangeVariableTypeRefactoring refactoring = (ChangeVariableTypeRefactoring)ref;
				RenamePattern pattern = new RenamePattern(refactoring.getOriginalVariable().getType().toString(), refactoring.getChangedTypeVariable().getType().toString());
				if(typeRenamePatternMap.containsKey(pattern)) {
					typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
				}
				else {
					typeRenamePatternMap.put(pattern, 1);
				}
			}
			else if(ref instanceof ChangeAttributeTypeRefactoring) {
				ChangeAttributeTypeRefactoring refactoring = (ChangeAttributeTypeRefactoring)ref;
				RenamePattern pattern = new RenamePattern(refactoring.getOriginalAttribute().getType().toString(), refactoring.getChangedTypeAttribute().getType().toString());
				if(typeRenamePatternMap.containsKey(pattern)) {
					typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
				}
				else {
					typeRenamePatternMap.put(pattern, 1);
				}
			}
			else if(ref instanceof ChangeReturnTypeRefactoring) {
				ChangeReturnTypeRefactoring refactoring = (ChangeReturnTypeRefactoring)ref;
				RenamePattern pattern = new RenamePattern(refactoring.getOriginalType().toString(), refactoring.getChangedType().toString());
				if(typeRenamePatternMap.containsKey(pattern)) {
					typeRenamePatternMap.put(pattern, typeRenamePatternMap.get(pattern) + 1);
				}
				else {
					typeRenamePatternMap.put(pattern, 1);
				}
			}
		}
		return typeRenamePatternMap;
	}

	private Set<Refactoring> filterPackageRefactorings(Set<Refactoring> refactoringsAtRevision) {
		Set<RefactoringType> refactoringTypesToConsider = new HashSet<>();
		refactoringTypesToConsider.add(RefactoringType.MOVE_CLASS);
		refactoringTypesToConsider.add(RefactoringType.RENAME_CLASS);
		refactoringTypesToConsider.add(RefactoringType.MOVE_RENAME_CLASS);
		refactoringTypesToConsider.add(RefactoringType.RENAME_PACKAGE);
		refactoringTypesToConsider.add(RefactoringType.MOVE_PACKAGE);
		refactoringTypesToConsider.add(RefactoringType.SPLIT_PACKAGE);
		refactoringTypesToConsider.add(RefactoringType.MERGE_PACKAGE);
		Set<Refactoring> filtered = new LinkedHashSet<Refactoring>();
		for (Refactoring ref : refactoringsAtRevision) {
			if (refactoringTypesToConsider.contains(ref.getRefactoringType())) {
				filtered.add(ref);
			}
		}
		return filtered;
	}

	private void detectImportDeclarationChanges(UMLClassBaseDiff classDiff, Set<Refactoring> refactorings) {
		if(classDiff.hasBothAddedAndRemovedImports()) {
			for(Refactoring ref : refactorings) {
				if(ref instanceof RenameClassRefactoring) {
					RenameClassRefactoring rename = (RenameClassRefactoring)ref;
					classDiff.findImportChanges(rename.getOriginalClassName(), rename.getRenamedClassName());
				}
				else if(ref instanceof MoveClassRefactoring) {
					MoveClassRefactoring move = (MoveClassRefactoring)ref;
					classDiff.findImportChanges(move.getOriginalClassName(), move.getMovedClassName());
				}
				else if(ref instanceof MoveAndRenameClassRefactoring) {
					MoveAndRenameClassRefactoring move = (MoveAndRenameClassRefactoring)ref;
					classDiff.findImportChanges(move.getOriginalClassName(), move.getMovedClassName());
				}
			}
		}
	}

	private void inferMethodSignatureRelatedRefactorings(UMLClassBaseDiff classDiff, Set<Refactoring> refactorings) {
		for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
			for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
				if(allowInference(classDiff, removedOperation, addedOperation)) {
					List<UMLOperationBodyMapper> mappers = findMappersWithMatchingSignatures(removedOperation, addedOperation);
					if(!mappers.isEmpty()) {
						UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(removedOperation, addedOperation);
						if(operationSignatureDiff.isOperationRenamed()) {
							RenameOperationRefactoring refactoring = new RenameOperationRefactoring(removedOperation, addedOperation);
							refactorings.add(refactoring);
						}
						Set<Refactoring> signatureRefactorings = operationSignatureDiff.getRefactorings();
						refactorings.addAll(signatureRefactorings);
						if(signatureRefactorings.isEmpty()) {
							inferRefactoringsFromMatchingMappers(mappers, operationSignatureDiff, refactorings);
						}
					}
				}
			}
		}
	}

	private boolean allowInference(UMLClassBaseDiff classDiff, UMLOperation removedOperation, UMLOperation addedOperation) {
		return removedOperation.isAbstract() == addedOperation.isAbstract();
	}

	private void inferRefactoringsFromMatchingMappers(List<UMLOperationBodyMapper> mappers, UMLOperationDiff operationSignatureDiff, Set<Refactoring> refactorings) {
		Set<Refactoring> refactoringsToBeAdded = new HashSet<Refactoring>();
		for(UMLOperationBodyMapper mapper : mappers) {
			for(Refactoring refactoring : refactorings) {
				if(refactoring instanceof RenameVariableRefactoring) {
					RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
					if(mapper.getContainer1().equals(rename.getOperationBefore()) && mapper.getContainer2().equals(rename.getOperationAfter())) {
						UMLParameter matchingRemovedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getRemovedParameters()) {
							if(parameter.getName().equals(rename.getOriginalVariable().getVariableName()) &&
									parameter.getVariableDeclaration().equalType(rename.getOriginalVariable())) {
								matchingRemovedParameter = parameter;
								break;
							}
						}
						UMLParameter matchingAddedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getAddedParameters()) {
							if(parameter.getName().equals(rename.getRenamedVariable().getVariableName()) &&
									parameter.getVariableDeclaration().equalType(rename.getRenamedVariable())) {
								matchingAddedParameter = parameter;
								break;
							}
						}
						if(matchingRemovedParameter != null && matchingAddedParameter != null) {
							RenameVariableRefactoring newRename = new RenameVariableRefactoring(matchingRemovedParameter.getVariableDeclaration(), matchingAddedParameter.getVariableDeclaration(),
									operationSignatureDiff.getRemovedOperation(), operationSignatureDiff.getAddedOperation(), new LinkedHashSet<AbstractCodeMapping>(), false);
							refactoringsToBeAdded.add(newRename);
						}
					}
				}
				else if(refactoring instanceof ChangeVariableTypeRefactoring) {
					ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
					if(mapper.getContainer1().equals(changeType.getOperationBefore()) && mapper.getContainer2().equals(changeType.getOperationAfter())) {
						UMLParameter matchingRemovedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getRemovedParameters()) {
							if(parameter.getName().equals(changeType.getOriginalVariable().getVariableName()) &&
									parameter.getVariableDeclaration().equalType(changeType.getOriginalVariable())) {
								matchingRemovedParameter = parameter;
								break;
							}
						}
						UMLParameter matchingAddedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getAddedParameters()) {
							if(parameter.getName().equals(changeType.getChangedTypeVariable().getVariableName()) &&
									parameter.getVariableDeclaration().equalType(changeType.getChangedTypeVariable())) {
								matchingAddedParameter = parameter;
								break;
							}
						}
						if(matchingRemovedParameter != null && matchingAddedParameter != null) {
							ChangeVariableTypeRefactoring newChangeType = new ChangeVariableTypeRefactoring(matchingRemovedParameter.getVariableDeclaration(), matchingAddedParameter.getVariableDeclaration(),
									operationSignatureDiff.getRemovedOperation(), operationSignatureDiff.getAddedOperation(), new LinkedHashSet<AbstractCodeMapping>(), false);
							refactoringsToBeAdded.add(newChangeType);
						}
					}
				}
				else if(refactoring instanceof AddParameterRefactoring) {
					AddParameterRefactoring addParameter = (AddParameterRefactoring)refactoring;
					if(mapper.getContainer1().equals(addParameter.getOperationBefore()) && mapper.getContainer2().equals(addParameter.getOperationAfter())) {
						UMLParameter matchingAddedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getAddedParameters()) {
							if(parameter.getName().equals(addParameter.getParameter().getName()) &&
									parameter.getType().equals(addParameter.getParameter().getType())) {
								matchingAddedParameter = parameter;
								break;
							}
						}
						if(matchingAddedParameter != null) {
							AddParameterRefactoring newAddParameter = new AddParameterRefactoring(matchingAddedParameter,
									operationSignatureDiff.getRemovedOperation(), operationSignatureDiff.getAddedOperation());
							refactoringsToBeAdded.add(newAddParameter);
						}
					}
				}
				else if(refactoring instanceof RemoveParameterRefactoring) {
					RemoveParameterRefactoring removeParameter = (RemoveParameterRefactoring)refactoring;
					if(mapper.getContainer1().equals(removeParameter.getOperationBefore()) && mapper.getContainer2().equals(removeParameter.getOperationAfter())) {
						UMLParameter matchingRemovedParameter = null;
						for(UMLParameter parameter : operationSignatureDiff.getRemovedParameters()) {
							if(parameter.getName().equals(removeParameter.getParameter().getName()) &&
									parameter.getType().equals(removeParameter.getParameter().getType())) {
								matchingRemovedParameter = parameter;
								break;
							}
						}
						if(matchingRemovedParameter != null) {
							RemoveParameterRefactoring newRemovedParameter = new RemoveParameterRefactoring(matchingRemovedParameter,
									operationSignatureDiff.getRemovedOperation(), operationSignatureDiff.getAddedOperation());
							refactoringsToBeAdded.add(newRemovedParameter);
						}
					}
				}
			}
		}
		refactorings.addAll(refactoringsToBeAdded);
	}

	private List<UMLOperationBodyMapper> findMappersWithMatchingSignatures(UMLOperation operation1, UMLOperation operation2) {
		List<UMLOperationBodyMapper> mappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignatures(operation1, operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		return mappers;
	}

	public List<UMLOperationBodyMapper> findMappersWithMatchingSignature2(UMLOperation operation2) {
		List<UMLOperationBodyMapper> mappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLClassDiff classDiff : commonClassDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassMoveDiff classDiff : classMoveDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassMoveDiff classDiff : innerClassMoveDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		for(UMLClassRenameDiff classDiff : classRenameDiffList) {
			UMLOperationBodyMapper mapper = classDiff.findMapperWithMatchingSignature2(operation2);
			if(mapper != null) {
				mappers.add(mapper);
			}
		}
		return mappers;
	}

	private void extractMergePatterns(UMLClassBaseDiff classDiff, Map<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>> mergeMap) {
		for(CandidateMergeVariableRefactoring candidate : classDiff.getCandidateAttributeMerges()) {
			Set<String> before = new LinkedHashSet<String>();
			for(String mergedVariable : candidate.getMergedVariables()) {
				before.add(PrefixSuffixUtils.normalize(mergedVariable));
			}
			String after = PrefixSuffixUtils.normalize(candidate.getNewVariable());
			MergeVariableReplacement merge = new MergeVariableReplacement(before, after);
			if(mergeMap.containsKey(merge)) {
				mergeMap.get(merge).add(candidate);
			}
			else {
				Set<CandidateMergeVariableRefactoring> set = new LinkedHashSet<CandidateMergeVariableRefactoring>();
				set.add(candidate);
				mergeMap.put(merge, set);
			}
		}
	}

	private void extractRenamePatterns(UMLClassBaseDiff classDiff, Map<Replacement, Set<CandidateAttributeRefactoring>> map) {
		for(CandidateAttributeRefactoring candidate : classDiff.getCandidateAttributeRenames()) {
			String before = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName());
			String after = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName());
			if(before.contains(".") && after.contains(".")) {
				String prefix1 = before.substring(0, before.lastIndexOf(".") + 1);
				String prefix2 = after.substring(0, after.lastIndexOf(".") + 1);
				if(prefix1.equals(prefix2)) {
					before = before.substring(prefix1.length(), before.length());
					after = after.substring(prefix2.length(), after.length());
				}
			}
			Replacement renamePattern = new Replacement(before, after, ReplacementType.VARIABLE_NAME);
			if(map.containsKey(renamePattern)) {
				map.get(renamePattern).add(candidate);
			}
			else {
				Set<CandidateAttributeRefactoring> set = new LinkedHashSet<CandidateAttributeRefactoring>();
				set.add(candidate);
				map.put(renamePattern, set);
			}
		}
	}

	private void checkForMovedAndInlinedOperations(List<UMLOperationBodyMapper> mappers, List<UMLOperation> removedOperations) throws RefactoringMinerTimedOutException {
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : mappers) {
				if((!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() || !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) && !mapper.containsInlineOperationRefactoring(removedOperation)) {
					List<AbstractCall> operationInvocations = mapper.getContainer1().getAllOperationInvocations();
					List<AbstractCall> removedOperationInvocations = new ArrayList<AbstractCall>();
					for(AbstractCall invocation : operationInvocations) {
						if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), this)) {
							removedOperationInvocations.add(invocation);
						}
					}
					if(removedOperationInvocations.size() > 0) {
						for(AbstractCall removedOperationInvocation : removedOperationInvocations) {
							if(!invocationMatchesWithAddedOperation(removedOperationInvocation, mapper.getContainer1(), mapper.getContainer2().getAllOperationInvocations())) {
								List<String> arguments = removedOperationInvocation.getArguments();
								List<String> parameters = removedOperation.getParameterNameList();
								Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<String, String>();
								//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
								int size = Math.min(arguments.size(), parameters.size());
								for(int i=0; i<size; i++) {
									parameterToArgumentMap1.put(parameters.get(i), arguments.get(i));
								}
								Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<String, String>();
								String expression = removedOperationInvocation.getExpression();
								if(expression != null && !removedOperation.getClassName().endsWith("." + expression)) {
									parameterToArgumentMap2.put(expression + ".", "");
									parameterToArgumentMap1.put("this.", "");
								}
								UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap1, parameterToArgumentMap2, getUMLClassDiff(removedOperation.getClassName()), false);
								if(moveAndInlineMatchCondition(operationBodyMapper, mapper)) {
									InlineOperationRefactoring inlineOperationRefactoring =	new InlineOperationRefactoring(operationBodyMapper, mapper.getContainer1(), removedOperationInvocations);
									refactorings.add(inlineOperationRefactoring);
									deleteRemovedOperation(removedOperation);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean moveAndInlineMatchCondition(UMLOperationBodyMapper operationBodyMapper, UMLOperationBodyMapper parentMapper) {
		List<AbstractCodeMapping> mappingList = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		if((operationBodyMapper.getContainer1().isGetter() || operationBodyMapper.getContainer1().isDelegate() != null) && mappingList.size() == 1) {
			List<AbstractCodeMapping> parentMappingList = new ArrayList<AbstractCodeMapping>(parentMapper.getMappings());
			for(AbstractCodeMapping mapping : parentMappingList) {
				if(mapping.getFragment2().equals(mappingList.get(0).getFragment2())) {
					return false;
				}
				if(mapping instanceof CompositeStatementObjectMapping) {
					CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping)mapping;
					CompositeStatementObject fragment2 = (CompositeStatementObject)compositeMapping.getFragment2();
					for(AbstractExpression expression : fragment2.getExpressions()) {
						if(expression.equals(mappingList.get(0).getFragment2())) {
							return false;
						}
					}
				}
			}
		}
		int delegateStatements = 0;
		for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null && invocation.matchesOperation(operationBodyMapper.getContainer1(), parentMapper.getContainer1(), this)) {
				delegateStatements++;
			}
		}
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1()-delegateStatements;
		List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
		List<AbstractCodeMapping> exactMatchListWithoutMatchesInNestedContainers = operationBodyMapper.getExactMatchesWithoutMatchesInNestedContainers();
		int exactMatches = exactMatchList.size();
		int exactMatchesWithoutMatchesInNestedContainers = exactMatchListWithoutMatchesInNestedContainers.size();
		return mappings > 0 && (mappings > nonMappedElementsT1 ||
				(exactMatchesWithoutMatchesInNestedContainers == 1 && !exactMatchListWithoutMatchesInNestedContainers.get(0).getFragment1().throwsNewException() && nonMappedElementsT1-exactMatchesWithoutMatchesInNestedContainers < 10) ||
				(exactMatches > 1 && nonMappedElementsT1-exactMatches < 20));
	}

	private boolean invocationMatchesWithAddedOperation(AbstractCall removedOperationInvocation, VariableDeclarationContainer callerOperation, List<AbstractCall> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for(UMLOperation addedOperation : getAddedOperationsInCommonClasses()) {
				if(removedOperationInvocation.matchesOperation(addedOperation, callerOperation, this)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean getterOrSetterCorrespondingToRenamedAttribute(UMLOperation addedOperation) {
		UMLClassBaseDiff classDiff = getUMLClassDiff(addedOperation.getClassName());
		if(classDiff != null) {
			if(classDiff.matchesCandidateAttributeRename(addedOperation)) {
				return true;
			}
		}
		return false;
	}

	private void checkForExtractedAndMovedOperations(List<UMLOperationBodyMapper> mappers, List<UMLOperation> addedOperations) throws RefactoringMinerTimedOutException {
		for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			if(!getterOrSetterCorrespondingToRenamedAttribute(addedOperation)) {
				for(UMLOperationBodyMapper mapper : mappers) {
					Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(mapper.getContainer1(), addedOperation);
					if((mapper.nonMappedElementsT1() > 0 || !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) && !mapper.containsExtractOperationRefactoring(addedOperation) && !processedOperationPairs.contains(pair)) {
						processedOperationPairs.add(pair);
						List<AbstractCall> operationInvocations = ExtractOperationDetection.getInvocationsInSourceOperationAfterExtraction(mapper);
						List<AbstractCall> addedOperationInvocations = new ArrayList<AbstractCall>();
						for(AbstractCall invocation : operationInvocations) {
							if(invocation.matchesOperation(addedOperation, mapper.getContainer2(), this)) {
								addedOperationInvocations.add(invocation);
							}
						}
						if(addedOperationInvocations.size() > 0) {
							AbstractCall addedOperationInvocation = addedOperationInvocations.get(0);
							List<String> arguments = addedOperationInvocation.getArguments();
							List<String> parameters = addedOperation.getParameterNameList();
							Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<String, String>();
							//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
							int size = Math.min(arguments.size(), parameters.size());
							for(int i=0; i<size; i++) {
								parameterToArgumentMap2.put(parameters.get(i), arguments.get(i));
							}
							String className = mapper.getContainer2().getClassName();
							List<UMLAttribute> attributes = new ArrayList<UMLAttribute>();
							if(className.contains(".") && isAnonymousClassName(className)) {
								//add enclosing class fields + anonymous class fields
								String[] tokens = className.split("\\.");
								String anonymousID = "";
								for(int i=tokens.length-1; i>=0; i--) {
									String token = tokens[i];
									if(StringDistance.isNumeric(token) || Character.isLowerCase(token.charAt(0))) {
										anonymousID = "." + token + anonymousID;
									}
									else {
										break;
									}
								}
								UMLClassBaseDiff umlClassDiff = getUMLClassDiff(className);
								if(umlClassDiff == null) {
									String enclosingClassName = className.substring(0, className.indexOf(anonymousID));
									umlClassDiff = getUMLClassDiff(enclosingClassName);
								}
								attributes.addAll(umlClassDiff.originalClassAttributesOfType(addedOperation.getClassName()));
								for(UMLAnonymousClass anonymousClass : umlClassDiff.getOriginalClass().getAnonymousClassList()) {
									if(className.equals(anonymousClass.getCodePath())) {
										attributes.addAll(anonymousClass.attributesOfType(addedOperation.getClassName()));
										break;
									}
								}
							}
							else {
								UMLClassBaseDiff umlClassDiff = getUMLClassDiff(className);
								if(umlClassDiff == null) {
									for(UMLClassDiff classDiff : commonClassDiffList) {
										for(UMLAnonymousClass anonymousClass : classDiff.getAddedAnonymousClasses()) {
											if(className.equals(anonymousClass.getCodePath())) {
												umlClassDiff = classDiff;
												attributes.addAll(anonymousClass.attributesOfType(addedOperation.getClassName()));
												break;
											}
										}
										if(umlClassDiff != null) {
											break;
										}
									}
								}
								attributes.addAll(umlClassDiff.originalClassAttributesOfType(addedOperation.getClassName()));
							}
							Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<String, String>();
							for(UMLAttribute attribute : attributes) {
								parameterToArgumentMap1.put(attribute.getName() + ".", "");
								parameterToArgumentMap2.put("this.", "");
							}
							if(addedOperationInvocation.getExpression() != null) {
								parameterToArgumentMap1.put(addedOperationInvocation.getExpression() + ".", "");
								parameterToArgumentMap2.put("this.", "");
							}
							UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(mapper, addedOperation, parameterToArgumentMap1, parameterToArgumentMap2, getUMLClassDiff(addedOperation.getClassName()), addedOperationInvocation, false);
							if(!anotherAddedMethodExistsWithBetterMatchingInvocationExpression(addedOperationInvocation, addedOperation, addedOperations) &&
									!conflictingExpression(addedOperationInvocation, addedOperation, mapper.getContainer2().variableDeclarationMap()) &&
									extractAndMoveMatchCondition(operationBodyMapper, mapper)) {
								if(className.equals(addedOperation.getClassName())) {
									//extract inside moved or renamed class
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
								else if(isSubclassOf(className, addedOperation.getClassName())) {
									//extract and pull up method
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
								else if(isSubclassOf(addedOperation.getClassName(), className)) {
									//extract and push down method
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
								else if(addedOperation.getClassName().startsWith(className + ".")) {
									//extract and move to inner class
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
								else if(className.startsWith(addedOperation.getClassName() + ".")) {
									//extract and move to outer class
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
								else if(sourceClassImportsTargetClass(className, addedOperation.getClassName()) ||
										sourceClassImportsSuperclassOfTargetClass(className, addedOperation.getClassName()) ||
										targetClassImportsSourceClass(className, addedOperation.getClassName())) {
									//extract and move
									ExtractOperationRefactoring extractOperationRefactoring =
											new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
									refactorings.add(extractOperationRefactoring);
									deleteAddedOperation(addedOperation);
								}
							}
							else {
								for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
									AbstractCodeFragment fragment1 = mapping.getFragment1();
									if(mapping instanceof LeafMapping && !mapper.getNonMappedLeavesT1().contains(fragment1) && fragment1 instanceof StatementObject) {
										mapper.getNonMappedLeavesT1().add((StatementObject) fragment1);
									}
									else if(mapping instanceof CompositeStatementObjectMapping && !mapper.getNonMappedInnerNodesT1().contains(fragment1) && fragment1 instanceof CompositeStatementObject) {
										mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject) fragment1);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean isAnonymousClassName(String className) {
		String anonymousID = className.substring(className.lastIndexOf(".")+1, className.length());
		return StringDistance.isNumeric(anonymousID) || Character.isLowerCase(anonymousID.charAt(0));
	}

	private boolean conflictingExpression(AbstractCall invocation, UMLOperation addedOperation, Map<String, Set<VariableDeclaration>> variableDeclarationMap) {
		String expression = invocation.getExpression();
		if(expression != null && variableDeclarationMap.containsKey(expression)) {
			Set<VariableDeclaration> variableDeclarations = variableDeclarationMap.get(expression);
			UMLClassBaseDiff classDiff = getUMLClassDiff(addedOperation.getClassName());
			boolean superclassRelationship = false;
			for(VariableDeclaration variableDeclaration : variableDeclarations) {
				UMLType type = variableDeclaration.getType();
				if(classDiff != null && type != null && classDiff.getNewSuperclass() != null &&
						classDiff.getNewSuperclass().equals(type)) {
					superclassRelationship = true;
				}
				if(type != null && !addedOperation.getNonQualifiedClassName().equals(type.getClassType()) && !superclassRelationship) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean anotherAddedMethodExistsWithBetterMatchingInvocationExpression(AbstractCall invocation, UMLOperation addedOperation, List<UMLOperation> addedOperations) {
		String expression = invocation.getExpression();
		if(expression != null) {
			for(UMLOperation operation : addedOperations) {
				UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
				boolean isInterface = classDiff != null ? classDiff.nextClass.isInterface() : false;
				if(!operation.equals(addedOperation) && addedOperation.equalSignature(operation) && !operation.isAbstract() && !isInterface) {
					if(expression.equals(operation.getNonQualifiedClassName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean extractAndMoveMatchCondition(UMLOperationBodyMapper operationBodyMapper, UMLOperationBodyMapper parentMapper) {
		List<AbstractCodeMapping> mappingList = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		if(operationBodyMapper.getContainer2().isGetter() && mappingList.size() == 1) {
			List<AbstractCodeMapping> parentMappingList = new ArrayList<AbstractCodeMapping>(parentMapper.getMappings());
			for(AbstractCodeMapping mapping : parentMappingList) {
				if(mapping.getFragment1().equals(mappingList.get(0).getFragment1())) {
					return false;
				}
				if(mapping instanceof CompositeStatementObjectMapping) {
					CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping)mapping;
					CompositeStatementObject fragment1 = (CompositeStatementObject)compositeMapping.getFragment1();
					for(AbstractExpression expression : fragment1.getExpressions()) {
						if(expression.equals(mappingList.get(0).getFragment1())) {
							return false;
						}
					}
				}
			}
			if(operationBodyMapper.getContainer1().isGetter()) {
				if(mappingList.get(0).getFragment1().getString().startsWith("return this;") ||
						mappingList.get(0).getFragment2().getString().startsWith("return this;")) {
					return false;
				}
			}
		}
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
		List<AbstractCodeMapping> exactMatchListWithoutMatchesInNestedContainers = operationBodyMapper.getExactMatchesWithoutMatchesInNestedContainers();
		int exactMatches = exactMatchList.size();
		int exactMatchesWithoutMatchesInNestedContainers = exactMatchListWithoutMatchesInNestedContainers.size();
		return mappings > 0 && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
				(exactMatches == mappings && nonMappedElementsT1 == 0) ||
				(exactMatchesWithoutMatchesInNestedContainers == 1 && !exactMatchListWithoutMatchesInNestedContainers.get(0).getFragment1().throwsNewException() && nonMappedElementsT2-exactMatchesWithoutMatchesInNestedContainers <= 10) ||
				(exactMatches > 1 && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2()));
	}

	private void checkForOperationMovesIncludingRemovedAndAddedClasses() throws RefactoringMinerTimedOutException {
		Set<UMLType> interfacesImplementedByAddedClasses = new LinkedHashSet<UMLType>();
		for(UMLClass addedClass : addedClasses) {
			interfacesImplementedByAddedClasses.addAll(addedClass.getImplementedInterfaces());
			UMLType superclass = addedClass.getSuperclass();
			if(superclass != null && superclass.getClassType().startsWith("Abstract")) {
				interfacesImplementedByAddedClasses.add(superclass);
			}
		}
		Set<UMLType> interfacesImplementedByRemovedClasses = new LinkedHashSet<UMLType>();
		for(UMLClass removedClass : removedClasses) {
			interfacesImplementedByRemovedClasses.addAll(removedClass.getImplementedInterfaces());
			UMLType superclass = removedClass.getSuperclass();
			if(superclass != null && superclass.getClassType().startsWith("Abstract")) {
				interfacesImplementedByRemovedClasses.add(superclass);
			}
		}
		Set<UMLType> interfaceIntersection = new LinkedHashSet<UMLType>(interfacesImplementedByAddedClasses);
		interfaceIntersection.retainAll(interfacesImplementedByRemovedClasses);
		List<UMLOperation> addedOperations = getAddedAndExtractedOperationsInCommonClasses();
		addedOperations.addAll(getAddedOperationsInMovedAndRenamedClasses());
		for(UMLClass addedClass : addedClasses) {
			if(!addedClass.implementsInterface(interfaceIntersection) && !addedClass.extendsSuperclass(interfaceIntersection) && !outerClassMovedOrRenamed(addedClass)) {
				addedOperations.addAll(addedClass.getOperations());
			}
			else if(addedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS &&
					addedClass.getOperationsWithOverrideAnnotation().size() > 0) {
				addedOperations.addAll(addedClass.getOperationsWithoutOverrideAnnotation());
			}
		}
		List<UMLOperation> removedOperations = new ArrayList<UMLOperation>();
		for(UMLClass removedClass : removedClasses) {
			if(!removedClass.implementsInterface(interfaceIntersection) && !removedClass.extendsSuperclass(interfaceIntersection) && !outerClassMovedOrRenamed(removedClass)) {
				removedOperations.addAll(removedClass.getOperations());
			}
			else if(addedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS && removedOperations.size() <= MAXIMUM_NUMBER_OF_COMPARED_METHODS &&
					removedClass.getOperationsWithOverrideAnnotation().size() > 0) {
				removedOperations.addAll(removedClass.getOperationsWithoutOverrideAnnotation());
			}
		}
		List<UMLOperation> removedOperations1 = getRemovedOperationsInCommonClasses();
		List<UMLOperation> removedOperations2 = getRemovedOperationsInCommonMovedRenamedClasses();
		if(condition(addedOperations.size(), removedOperations.size() + removedOperations2.size())) {
			for(UMLOperation operation : removedOperations2) {
				if(removedOperations1.contains(operation)) {
					removedOperations.add(operation);
				}
				else if(!operation.isGetter() && !operation.isSetter() && !operation.isMultiSetter()) {
					removedOperations.add(operation);
				}
			}
		}
		else {
			removedOperations.addAll(removedOperations1);
		}
		if(condition(addedOperations.size(), removedOperations.size())) {
			checkForOperationMoves(addedOperations, removedOperations);
		}
	}

	private void checkForOperationMovesBetweenCommonClasses() throws RefactoringMinerTimedOutException {
		List<UMLOperation> addedOperations = getAddedAndExtractedOperationsInCommonClasses();
		addedOperations.addAll(getAddedOperationsInMovedAndRenamedClasses());
		List<UMLOperation> removedOperations = getRemovedOperationsInCommonMovedRenamedClasses();
		if(condition(addedOperations.size(), removedOperations.size())) {
			checkForOperationMoves(addedOperations, removedOperations);
		}
	}

	private boolean condition(int size1, int size2) {
		if(partialModel()) {
			return size1 <= MAXIMUM_NUMBER_OF_COMPARED_METHODS && size2 <= MAXIMUM_NUMBER_OF_COMPARED_METHODS;
		}
		else {
			return (size1 <= MAXIMUM_NUMBER_OF_COMPARED_METHODS || size2 <= MAXIMUM_NUMBER_OF_COMPARED_METHODS) &&
					size1*size2 <= MAXIMUM_NUMBER_OF_COMPARED_METHODS*MAXIMUM_NUMBER_OF_COMPARED_METHODS;
		}
	}

	private boolean partialModel() {
		return childModel.isPartial() || parentModel.isPartial();
	}

	private boolean outerClassMovedOrRenamed(UMLClass umlClass) {
		if(!umlClass.isTopLevel()) {
			for(UMLClassMoveDiff diff : classMoveDiffList) {
				if(diff.getOriginalClass().getName().equals(umlClass.getPackageName())) {
					String nestedClassExpectedName = diff.getMovedClass().getName() + 
							umlClass.getName().substring(diff.getOriginalClass().getName().length(), umlClass.getName().length());
					for(UMLClass addedClass : addedClasses) {
						if(addedClass.getName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
					for(UMLClassMoveDiff innerClassDiff : innerClassMoveDiffList) {
						if(innerClassDiff.getNextClassName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
				}
				else if(diff.getMovedClass().getName().equals(umlClass.getPackageName())) {
					String nestedClassExpectedName = diff.getOriginalClass().getName() +
							umlClass.getName().substring(diff.getMovedClass().getName().length(), umlClass.getName().length());
					for(UMLClass removedClass : removedClasses) {
						if(removedClass.getName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
					for(UMLClassMoveDiff innerClassDiff : innerClassMoveDiffList) {
						if(innerClassDiff.getOriginalClassName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
				}
			}
			for(UMLClassRenameDiff diff : classRenameDiffList) {
				if(diff.getOriginalClass().getName().equals(umlClass.getPackageName())) {
					String nestedClassExpectedName = diff.getRenamedClass().getName() + 
							umlClass.getName().substring(diff.getOriginalClass().getName().length(), umlClass.getName().length());
					for(UMLClass addedClass : addedClasses) {
						if(addedClass.getName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
					for(UMLClassMoveDiff innerClassDiff : innerClassMoveDiffList) {
						if(innerClassDiff.getNextClassName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
				}
				else if(diff.getRenamedClass().getName().equals(umlClass.getPackageName())) {
					String nestedClassExpectedName = diff.getOriginalClass().getName() +
							umlClass.getName().substring(diff.getRenamedClass().getName().length(), umlClass.getName().length());
					for(UMLClass removedClass : removedClasses) {
						if(removedClass.getName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
					for(UMLClassMoveDiff innerClassDiff : innerClassMoveDiffList) {
						if(innerClassDiff.getOriginalClassName().equals(nestedClassExpectedName)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void checkForOperationMoves(List<UMLOperation> addedOperations, List<UMLOperation> removedOperations) throws RefactoringMinerTimedOutException {
		if(addedOperations.size() <= removedOperations.size()) {
			for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap = new TreeMap<Integer, List<UMLOperationBodyMapper>>();
				for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();

					Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(removedOperation, addedOperation);
					if(!processedOperationPairs.contains(pair) && removedOperation.testMethodCheck(addedOperation)) {
						UMLClassBaseDiff umlClassDiff = getUMLClassDiff(removedOperation.getClassName());
						if(umlClassDiff == null) {
							umlClassDiff = getUMLClassDiff(addedOperation.getClassName());
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, umlClassDiff);
						processedOperationPairs.add(pair);
						int mappings = operationBodyMapper.mappingsWithoutBlocks();
						if((mappings > 0 && mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)) || addedOperation.equalSignatureForAbstractMethods(removedOperation) ||
								(mappings > 0 && isPartOfMethodExtracted(removedOperation, addedOperation, addedOperations))) {
							int exactMatches = operationBodyMapper.exactMatches();
							List<AbstractCodeMapping> exactMappings = operationBodyMapper.getExactMatches();
							for(AbstractCodeMapping mapping : exactMappings) {
								String fragment1 = mapping.getFragment1().getString();
								if(RETURN_NUMBER_LITERAL.matcher(fragment1).matches()) {
									exactMatches--;
								}
							}
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
				}
				if(!operationBodyMapperMap.isEmpty()) {
					List<UMLOperationBodyMapper> firstMappers = firstMappers(operationBodyMapperMap);
					Collections.sort(firstMappers, new UMLOperationBodyMapperComparator());
					addedOperationIterator.remove();
					boolean sameSourceAndTargetClass = sameSourceAndTargetClass(firstMappers);
					if(sameSourceAndTargetClass) {
						TreeSet<UMLOperationBodyMapper> set = null;
						if(allRenamedOperations(firstMappers)) {
							set = new TreeSet<UMLOperationBodyMapper>();
						}
						else {
							set = new TreeSet<UMLOperationBodyMapper>(new UMLOperationBodyMapperComparator());
						}
						set.addAll(firstMappers);
						UMLOperationBodyMapper bestMapper = set.first();
						firstMappers.clear();
						firstMappers.add(bestMapper);
					}
					for(UMLOperationBodyMapper firstMapper : firstMappers) {
						UMLOperation removedOperation = firstMapper.getOperation1();
						if(sameSourceAndTargetClass) {
							removedOperations.remove(removedOperation);
						}

						Refactoring refactoring = null;
						if(removedOperation.getClassName().equals(addedOperation.getClassName())) {
							if (addedOperation.equalParameters(removedOperation)) {
								//refactoring = new RenameOperationRefactoring(removedOperation, addedOperation);
							} else {
								// Methods in the same class with similar body but different signature
							}
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								isSubclassOf(removedOperation.getClassName(), addedOperation.getClassName()) && addedOperation.compatibleSignature(removedOperation, typeParameterToTypeArgumentMap(removedOperation.getClassName(), addedOperation.getClassName())) &&
								!refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new PullUpOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								isSubclassOf(addedOperation.getClassName(), removedOperation.getClassName()) && addedOperation.compatibleSignature(removedOperation, typeParameterToTypeArgumentMap(addedOperation.getClassName(), removedOperation.getClassName()))) {
							refactoring = new PushDownOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								movedMethodSignature(removedOperation, addedOperation, firstMapper, firstMappers.size() > 1) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new MoveOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								movedAndRenamedMethodSignature(removedOperation, addedOperation, firstMapper, firstMappers.size() > 1) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new MoveOperationRefactoring(firstMapper);
						}
						if(refactoring != null) {
							deleteRemovedOperation(removedOperation);
							deleteAddedOperation(addedOperation);
							Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
							for(Refactoring r : this.refactorings) {
								if(r instanceof ExtractOperationRefactoring) {
									ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
									if(extract.getExtractedOperation().equals(addedOperation)) {
										if(firstMapper.getMappings().size() > extract.getBodyMapper().getMappings().size()) {
											refactoringsToBeRemoved.add(r);
										}
									}
								}
							}
							refactorings.removeAll(refactoringsToBeRemoved);
							refactorings.addAll(firstMapper.getRefactorings());
							refactorings.add(refactoring);
							UMLClass addedClass = getAddedClass(addedOperation.getClassName());
							if(addedClass != null) {
								List<UMLOperation> potentiallyMovedOperations = new ArrayList<>();
								UMLClassBaseDiff removedClassDiff = getUMLClassDiff(removedOperation.getClassName());
								if(removedClassDiff != null) {
									potentiallyMovedOperations.addAll(removedClassDiff.getRemovedOperations());
								}
								else {
									UMLClass removedClass = getRemovedClass(removedOperation.getClassName());
									if(removedClass != null) {
										potentiallyMovedOperations.addAll(removedClass.getOperations());
									}
								}
								checkForExtractedOperationsWithinMovedMethod(firstMapper, potentiallyMovedOperations, addedClass);
							}
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

					Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(removedOperation, addedOperation);
					if(!processedOperationPairs.contains(pair) && removedOperation.testMethodCheck(addedOperation)) {
						UMLClassBaseDiff umlClassDiff = getUMLClassDiff(removedOperation.getClassName());
						if(umlClassDiff == null) {
							umlClassDiff = getUMLClassDiff(addedOperation.getClassName());
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, umlClassDiff);
						processedOperationPairs.add(pair);
						int mappings = operationBodyMapper.mappingsWithoutBlocks();
						if((mappings > 0 && mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)) || removedOperation.equalSignatureForAbstractMethods(addedOperation) ||
								(mappings > 0 && isPartOfMethodExtracted(removedOperation, addedOperation, addedOperations))) {
							int exactMatches = operationBodyMapper.exactMatches();
							List<AbstractCodeMapping> exactMappings = operationBodyMapper.getExactMatches();
							for(AbstractCodeMapping mapping : exactMappings) {
								String fragment1 = mapping.getFragment1().getString();
								if(RETURN_NUMBER_LITERAL.matcher(fragment1).matches()) {
									exactMatches--;
								}
							}
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
				}
				if(!operationBodyMapperMap.isEmpty()) {
					List<UMLOperationBodyMapper> firstMappers = firstMappers(operationBodyMapperMap);
					Collections.sort(firstMappers, new UMLOperationBodyMapperComparator());
					removedOperationIterator.remove();
					boolean sameSourceAndTargetClass = sameSourceAndTargetClass(firstMappers);
					if(sameSourceAndTargetClass) {
						TreeSet<UMLOperationBodyMapper> set = null;
						if(allRenamedOperations(firstMappers)) {
							set = new TreeSet<UMLOperationBodyMapper>();
						}
						else {
							set = new TreeSet<UMLOperationBodyMapper>(new UMLOperationBodyMapperComparator());
						}
						set.addAll(firstMappers);
						UMLOperationBodyMapper bestMapper = set.first();
						firstMappers.clear();
						firstMappers.add(bestMapper);
					}
					for(UMLOperationBodyMapper firstMapper : firstMappers) {
						UMLOperation addedOperation = firstMapper.getOperation2();
						if(sameSourceAndTargetClass) {
							addedOperations.remove(addedOperation);
						}

						Refactoring refactoring = null;
						if(removedOperation.getClassName().equals(addedOperation.getClassName())) {
							if (addedOperation.equalParameters(removedOperation)) {
								//refactoring = new RenameOperationRefactoring(removedOperation, addedOperation);
							} else {
								// Methods in the same class with similar body but different signature
							}
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								isSubclassOf(removedOperation.getClassName(), addedOperation.getClassName()) && addedOperation.compatibleSignature(removedOperation, typeParameterToTypeArgumentMap(removedOperation.getClassName(), addedOperation.getClassName())) &&
								!refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new PullUpOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								isSubclassOf(addedOperation.getClassName(), removedOperation.getClassName()) && addedOperation.compatibleSignature(removedOperation, typeParameterToTypeArgumentMap(addedOperation.getClassName(), removedOperation.getClassName()))) {
							refactoring = new PushDownOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								movedMethodSignature(removedOperation, addedOperation, firstMapper, firstMappers.size() > 1) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new MoveOperationRefactoring(firstMapper);
						}
						else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
								movedAndRenamedMethodSignature(removedOperation, addedOperation, firstMapper, firstMappers.size() > 1) && !refactoringListContainsAnotherMoveRefactoringWithTheSameOperations(removedOperation, addedOperation)) {
							refactoring = new MoveOperationRefactoring(firstMapper);
						}
						if(refactoring != null) {
							deleteRemovedOperation(removedOperation);
							deleteAddedOperation(addedOperation);
							Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
							for(Refactoring r : this.refactorings) {
								if(r instanceof ExtractOperationRefactoring) {
									ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
									if(extract.getExtractedOperation().equals(addedOperation)) {
										if(firstMapper.getMappings().size() > extract.getBodyMapper().getMappings().size()) {
											refactoringsToBeRemoved.add(r);
										}
									}
								}
							}
							refactorings.removeAll(refactoringsToBeRemoved);
							refactorings.addAll(firstMapper.getRefactorings());
							refactorings.add(refactoring);
							UMLClass addedClass = getAddedClass(addedOperation.getClassName());
							if(addedClass != null) {
								List<UMLOperation> potentiallyMovedOperations = new ArrayList<>();
								UMLClassBaseDiff removedClassDiff = getUMLClassDiff(removedOperation.getClassName());
								if(removedClassDiff != null) {
									potentiallyMovedOperations.addAll(removedClassDiff.getRemovedOperations());
								}
								else {
									UMLClass removedClass = getRemovedClass(removedOperation.getClassName());
									if(removedClass != null) {
										potentiallyMovedOperations.addAll(removedClass.getOperations());
									}
								}
								checkForExtractedOperationsWithinMovedMethod(firstMapper, potentiallyMovedOperations, addedClass);
							}
						}
					}
				}
			}
		}
	}

	private Map<UMLTypeParameter, UMLType> typeParameterToTypeArgumentMap(String subclass, String superclass) {
		Map<UMLTypeParameter, UMLType> typeParameterToTypeArgumentMap = new LinkedHashMap<UMLTypeParameter, UMLType>();
		UMLClassBaseDiff subclassDiff = getUMLClassDiff(subclass);
		if(subclassDiff != null) {
			UMLType superclassType = subclassDiff.getNewSuperclass();
			if(superclassType != null) {
				UMLClassBaseDiff superclassDiff = getUMLClassDiff(superclass);
				if(superclassDiff != null) {
					List<UMLTypeParameter> typeParameters = superclassDiff.getNextClass().getTypeParameters();
					List<UMLType> typeArguments = superclassType.getTypeArguments();
					if(typeParameters.size() == typeArguments.size()) {
						for(int i=0; i<typeParameters.size(); i++) {
							typeParameterToTypeArgumentMap.put(typeParameters.get(i), typeArguments.get(i));
						}
					}
				}
			}
		}
		return typeParameterToTypeArgumentMap;
	}

	private List<UMLOperationBodyMapper> firstMappers(TreeMap<Integer, List<UMLOperationBodyMapper>> operationBodyMapperMap) {
		List<UMLOperationBodyMapper> firstMappers = new ArrayList<UMLOperationBodyMapper>(operationBodyMapperMap.get(operationBodyMapperMap.lastKey()));
		List<UMLOperationBodyMapper> extraMappers = operationBodyMapperMap.get(0);
		if(extraMappers != null && operationBodyMapperMap.lastKey() != 0) {
			for(UMLOperationBodyMapper extraMapper : extraMappers) {
				UMLOperation operation1 = extraMapper.getOperation1();
				UMLOperation operation2 = extraMapper.getOperation2();
				if(operation1.equalSignature(operation2)) {
					List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>(extraMapper.getMappings());
					if(mappings.size() == 1) {
						Set<Replacement> replacements = mappings.get(0).getReplacements();
						if(replacements.size() == 1) {
							Replacement replacement = replacements.iterator().next();
							List<String> parameterNames1 = operation1.getParameterNameList();
							List<String> parameterNames2 = operation2.getParameterNameList();
							for(int i=0; i<parameterNames1.size(); i++) {
								String parameterName1 = parameterNames1.get(i);
								String parameterName2 = parameterNames2.get(i);
								if(replacement.getBefore().equals(parameterName1) &&
										replacement.getAfter().equals(parameterName2)) {
									firstMappers.add(extraMapper);
									break;
								}
							}
						}
					}
				}
			}
		}
		return firstMappers;
	}

	private boolean allRenamedOperations(List<UMLOperationBodyMapper> mappers) {
		for (UMLOperationBodyMapper mapper : mappers) {
			if(mapper.getContainer1().getName().equals(mapper.getContainer2().getName())) {
				return false;
			}
		}
		return true;
	}

	private boolean sameSourceAndTargetClass(List<UMLOperationBodyMapper> mappers) {
		if(mappers.size() == 1) {
			return false;
		}
		String sourceClassName = null;
		String targetClassName = null;
		for (UMLOperationBodyMapper mapper : mappers) {
			String mapperSourceClassName = mapper.getContainer1().getClassName();
			if(sourceClassName == null) {
				sourceClassName = mapperSourceClassName;
			}
			else if(!mapperSourceClassName.equals(sourceClassName)) {
				return false;
			}
			String mapperTargetClassName = mapper.getContainer2().getClassName();
			if(targetClassName == null) {
				targetClassName = mapperTargetClassName;
			}
			else if(!mapperTargetClassName.equals(targetClassName)) {
				return false;
			}
		}
		return true;
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		UMLClass addedClass = getAddedClass(operationBodyMapper.getContainer2().getClassName());
		int nonMappedStatementsDeclaringSameVariable = 0;
		for(ListIterator<AbstractCodeFragment> leafIterator1 = operationBodyMapper.getNonMappedLeavesT1().listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment s1 = leafIterator1.next();
			for(AbstractCodeFragment s2 : operationBodyMapper.getNonMappedLeavesT2()) {
				if(s1.getVariableDeclarations().size() == 1 && s2.getVariableDeclarations().size() == 1) {
					VariableDeclaration v1 = s1.getVariableDeclarations().get(0);
					VariableDeclaration v2 = s2.getVariableDeclarations().get(0);
					if(v1.getVariableName().equals(v2.getVariableName()) && v1.equalType(v2)) {
						nonMappedStatementsDeclaringSameVariable++;
					}
				}
			}
			if(addedClass != null && s1.getVariableDeclarations().size() == 1) {
				VariableDeclaration v1 = s1.getVariableDeclarations().get(0);
				for(UMLAttribute attribute : addedClass.getAttributes()) {
					VariableDeclaration attributeDeclaration = attribute.getVariableDeclaration();
					if(attributeDeclaration.getInitializer() != null && v1.getInitializer() != null) {
						String attributeInitializer = attributeDeclaration.getInitializer().getString();
						String variableInitializer = v1.getInitializer().getString();
						if(attributeInitializer.equals(variableInitializer) && attributeDeclaration.equalType(v1) &&
								(attribute.getName().equals(v1.getVariableName()) ||
										attribute.getName().toLowerCase().contains(v1.getVariableName().toLowerCase()) ||
										v1.getVariableName().toLowerCase().contains(attribute.getName().toLowerCase()))) {
							nonMappedStatementsDeclaringSameVariable++;
							leafIterator1.remove();
							LeafMapping mapping = new LeafMapping(v1.getInitializer(), attributeDeclaration.getInitializer(),
									operationBodyMapper.getContainer1(),
									operationBodyMapper.getContainer2());
							operationBodyMapper.addMapping(mapping);
							break;
						}
					}
				}
			}
		}
		int nonMappedLoopsIteratingOverSameVariable = 0;
		for(CompositeStatementObject c1 : operationBodyMapper.getNonMappedInnerNodesT1()) {
			if(c1.isLoop()) {
				for(CompositeStatementObject c2 : operationBodyMapper.getNonMappedInnerNodesT2()) {
					if(c2.isLoop()) {
						Set<String> intersection = new LinkedHashSet<String>(c1.getVariables());
						intersection.retainAll(c2.getVariables());
						if(!intersection.isEmpty()) {
							nonMappedLoopsIteratingOverSameVariable++;
						}
					}
				}
			}
		}
		return (mappings > nonMappedElementsT1-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable &&
				mappings > nonMappedElementsT2-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable) ||
				(mappings > 10 && mappings >= nonMappedElementsT1-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable &&
						mappings >= nonMappedElementsT2-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable) ||
				(nonMappedElementsT1-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable == 0 && mappings > Math.floor(nonMappedElementsT2/2.0)) ||
				(nonMappedElementsT2-nonMappedStatementsDeclaringSameVariable-nonMappedLoopsIteratingOverSameVariable == 0 && mappings > Math.floor(nonMappedElementsT1/2.0));
	}

	private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation, List<UMLOperation> addedOperations) {
		//filter addedOperations that belong to the same class as the addedOperation
		List<UMLOperation> filteredAddedOperations = new ArrayList<>();
		for(UMLOperation operation : addedOperations) {
			if(operation.getClassName().equals(addedOperation.getClassName())) {
				filteredAddedOperations.add(operation);
			}
		}
		List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = new LinkedHashSet<AbstractCall>(removedOperationInvocations).size() - intersection.size();

		Set<AbstractCall> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<AbstractCall>();
		for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : filteredAddedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation, addedOperation, this)) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<AbstractCall> newIntersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);

		Set<AbstractCall> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<AbstractCall> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			AbstractCall invocation = operationInvocationIterator.next();
			if(invocation.getName().startsWith("get")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}

	private boolean movedAndRenamedMethodSignature(UMLOperation removedOperation, UMLOperation addedOperation, UMLOperationBodyMapper mapper, boolean multipleMappers) {
		UMLClassBaseDiff addedOperationClassDiff = getUMLClassDiff(addedOperation.getClassName());
		if(addedOperationClassDiff != null) {
			for(Refactoring r : addedOperationClassDiff.getRefactoringsBeforePostProcessing()) {
				if(r instanceof ExtractOperationRefactoring) {
					ExtractOperationRefactoring extractRefactoring = (ExtractOperationRefactoring)r;
					if(extractRefactoring.getExtractedOperation().equals(addedOperation)) {
						return false;
					}
				}
			}
		}
		UMLClassBaseDiff removedOperationClassDiff = getUMLClassDiff(removedOperation.getClassName());
		if(removedOperationClassDiff != null && removedOperationClassDiff.containsOperationWithTheSameSignatureInNextClass(removedOperation)) {
			return false;
		}
		if((removedOperation.isGetter() || removedOperation.isSetter() || addedOperation.isGetter() || addedOperation.isSetter()) &&
				mapper.mappingsWithoutBlocks() == 1 && mapper.getMappings().size() == 1) {
			if(!mapper.getMappings().iterator().next().isExact() || multipleMappers) {
				return false;
			}
		}
		if((removedOperation.isConstructor() || addedOperation.isConstructor()) && mapper.mappingsWithoutBlocks() > 0) {
			if(!(mapper.allMappingsAreExactMatches() && mapper.nonMappedElementsT1() == 0 && mapper.nonMappedElementsT2() == 0)) {
				return false;
			}
			if(mapper.mappingsWithoutBlocks() == 1 && mapper.getMappings().size() == 1 && mapper.exactMatches() == 1 && multipleMappers) {
				return false;
			}
		}
		int exactLeafMappings = 0;
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping instanceof LeafMapping && mapping.isExact() && !mapping.getFragment1().getString().startsWith("return ")) {
				exactLeafMappings++;
			}
		}
		double normalizedEditDistance = mapper.normalizedEditDistance();
		if(exactLeafMappings == 0 && normalizedEditDistance > 0.24) {
			return false;
		}
		if(exactLeafMappings == 1 && normalizedEditDistance > 0.51 && (mapper.nonMappedElementsT1() > 0 || mapper.nonMappedElementsT2() > 0)) {
			return false;
		}
		if(mapper.mappingsWithoutBlocks() == 1) {
			for(AbstractCodeMapping mapping : mapper.getMappings()) {
				String fragment1 = mapping.getFragment1().getString();
				String fragment2 = mapping.getFragment2().getString();
				if(fragment1.startsWith("return true;") || fragment1.startsWith("return false;") || fragment1.startsWith("return this;") || fragment1.startsWith("return null;") || fragment1.startsWith("return;") ||
						fragment2.startsWith("return true;") || fragment2.startsWith("return false;") || fragment2.startsWith("return this;") || fragment2.startsWith("return null;") || fragment2.startsWith("return;")) {
					return false;
				}
			}
		}
		if(addedOperation.isAbstract() == removedOperation.isAbstract() &&
				addedOperation.getTypeParameters().equals(removedOperation.getTypeParameters())) {
			List<UMLType> addedOperationParameterTypeList = addedOperation.getParameterTypeList();
			List<UMLType> removedOperationParameterTypeList = removedOperation.getParameterTypeList();
			if(addedOperationParameterTypeList.equals(removedOperationParameterTypeList) && addedOperationParameterTypeList.size() > 0) {
				return true;
			}
			else {
				// ignore parameters of types sourceClass and targetClass
				List<UMLParameter> oldParameters = new ArrayList<UMLParameter>();
				Set<String> oldParameterNames = new LinkedHashSet<String>();
				for (UMLParameter oldParameter : removedOperation.getParameters()) {
					if (!oldParameter.getKind().equals("return")
							&& !looksLikeSameType(oldParameter.getType().getClassType(), addedOperation.getClassName())
							&& !looksLikeSameType(oldParameter.getType().getClassType(), removedOperation.getClassName())) {
						oldParameters.add(oldParameter);
						oldParameterNames.add(oldParameter.getName());
					}
				}
				List<UMLParameter> newParameters = new ArrayList<UMLParameter>();
				Set<String> newParameterNames = new LinkedHashSet<String>();
				for (UMLParameter newParameter : addedOperation.getParameters()) {
					if (!newParameter.getKind().equals("return") &&
							!looksLikeSameType(newParameter.getType().getClassType(), addedOperation.getClassName()) &&
							!looksLikeSameType(newParameter.getType().getClassType(), removedOperation.getClassName())) {
						newParameters.add(newParameter);
						newParameterNames.add(newParameter.getName());
					}
				}
				Set<String> intersection = new LinkedHashSet<String>(oldParameterNames);
				intersection.retainAll(newParameterNames);
				boolean parameterMatch = oldParameters.equals(newParameters) || oldParameters.containsAll(newParameters) || newParameters.containsAll(oldParameters) || intersection.size() > 0 ||
						removedOperation.isStatic() || addedOperation.isStatic();
				return (parameterMatch && oldParameters.size() > 0 && newParameters.size() > 0) ||
						(parameterMatch && addedOperation.equalReturnParameter(removedOperation) && (oldParameters.size() == 0 || newParameters.size() == 0));
			}
		}
		return false;
	}

	private boolean movedMethodSignature(UMLOperation removedOperation, UMLOperation addedOperation, UMLOperationBodyMapper mapper, boolean multipleMappers) {
		if((removedOperation.isGetter() || removedOperation.isSetter() || addedOperation.isGetter() || addedOperation.isSetter()) &&
				mapper.mappingsWithoutBlocks() == 1 && mapper.getMappings().size() == 1) {
			if(!mapper.getMappings().iterator().next().isExact() || multipleMappers) {
				return false;
			}
		}
		if(addedOperation.getName().equals(removedOperation.getName()) &&
				addedOperation.equalReturnParameter(removedOperation) &&
				addedOperation.isAbstract() == removedOperation.isAbstract() &&
				addedOperation.getTypeParameters().equals(removedOperation.getTypeParameters())) {
			if(addedOperation.getParameters().equals(removedOperation.getParameters())) {
				return true;
			}
			else {
				// ignore parameters of types sourceClass and targetClass
				List<UMLParameter> oldParameters = new ArrayList<UMLParameter>();
				Set<String> oldParameterNames = new LinkedHashSet<String>();
				for (UMLParameter oldParameter : removedOperation.getParameters()) {
					if (!oldParameter.getKind().equals("return")
							&& !looksLikeSameType(oldParameter.getType().getClassType(), addedOperation.getClassName())
							&& !looksLikeSameType(oldParameter.getType().getClassType(), removedOperation.getClassName())) {
						oldParameters.add(oldParameter);
						oldParameterNames.add(oldParameter.getName());
					}
				}
				List<UMLParameter> newParameters = new ArrayList<UMLParameter>();
				Set<String> newParameterNames = new LinkedHashSet<String>();
				for (UMLParameter newParameter : addedOperation.getParameters()) {
					if (!newParameter.getKind().equals("return") &&
							!looksLikeSameType(newParameter.getType().getClassType(), addedOperation.getClassName()) &&
							!looksLikeSameType(newParameter.getType().getClassType(), removedOperation.getClassName())) {
						newParameters.add(newParameter);
						newParameterNames.add(newParameter.getName());
					}
				}
				Set<String> intersection = new LinkedHashSet<String>(oldParameterNames);
				intersection.retainAll(newParameterNames);
				return oldParameters.equals(newParameters) || oldParameters.containsAll(newParameters) || newParameters.containsAll(oldParameters) || intersection.size() > 0 ||
						removedOperation.isStatic() || addedOperation.isStatic();
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

	public boolean refactoringListContainsAnotherMoveRefactoringWithTheSameAddedOperation(UMLOperation addedOperation) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof MoveOperationRefactoring) {
				MoveOperationRefactoring moveRefactoring = (MoveOperationRefactoring)refactoring;
				if(moveRefactoring.getMovedOperation().equals(addedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean attributeMerged(UMLAttribute a1, UMLAttribute a2, Set<Refactoring> refactorings) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof MergeAttributeRefactoring) {
				MergeAttributeRefactoring merge = (MergeAttributeRefactoring)refactoring;
				if(merge.getMergedVariables().contains(a1.getVariableDeclaration()) && merge.getNewAttribute().getVariableDeclaration().equals(a2.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<Refactoring> movedAttributeRenamed(VariableDeclaration a1, VariableDeclaration a2, Set<Refactoring> refactorings) {
		Set<Refactoring> matchingRefactorings = new HashSet<Refactoring>();
		boolean renameFound = false;
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof RenameAttributeRefactoring) {
				RenameAttributeRefactoring rename = (RenameAttributeRefactoring)refactoring;
				if(a1.equals(rename.getOriginalAttribute().getVariableDeclaration()) || a2.equals(rename.getRenamedAttribute().getVariableDeclaration())) {
					matchingRefactorings.add(rename);
					renameFound = true;
				}
			}
			else if(refactoring instanceof ChangeAttributeTypeRefactoring) {
				ChangeAttributeTypeRefactoring changeType = (ChangeAttributeTypeRefactoring)refactoring;
				if(a1.equals(changeType.getOriginalAttribute().getVariableDeclaration()) || a2.equals(changeType.getChangedTypeAttribute().getVariableDeclaration())) {
					matchingRefactorings.add(changeType);
				}
			}
			else if(refactoring instanceof ChangeAttributeAccessModifierRefactoring) {
				ChangeAttributeAccessModifierRefactoring changeAccessModifer = (ChangeAttributeAccessModifierRefactoring)refactoring;
				if(a1.equals(changeAccessModifer.getAttributeBefore().getVariableDeclaration()) || a2.equals(changeAccessModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(changeAccessModifer);
				}
			}
			else if(refactoring instanceof AddAttributeModifierRefactoring) {
				AddAttributeModifierRefactoring addModifer = (AddAttributeModifierRefactoring)refactoring;
				if(a1.equals(addModifer.getAttributeBefore().getVariableDeclaration()) || a2.equals(addModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(addModifer);
				}
			}
			else if(refactoring instanceof RemoveAttributeModifierRefactoring) {
				RemoveAttributeModifierRefactoring removeModifer = (RemoveAttributeModifierRefactoring)refactoring;
				if(a1.equals(removeModifer.getAttributeBefore().getVariableDeclaration()) || a2.equals(removeModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(removeModifer);
				}
			}
			else if(refactoring instanceof AddAttributeAnnotationRefactoring) {
				AddAttributeAnnotationRefactoring addAnnotation = (AddAttributeAnnotationRefactoring)refactoring;
				if(a1.equals(addAnnotation.getAttributeBefore().getVariableDeclaration()) || a2.equals(addAnnotation.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(addAnnotation);
				}
			}
			else if(refactoring instanceof RemoveAttributeAnnotationRefactoring) {
				RemoveAttributeAnnotationRefactoring removeAnnotation = (RemoveAttributeAnnotationRefactoring)refactoring;
				if(a1.equals(removeAnnotation.getAttributeBefore().getVariableDeclaration()) || a2.equals(removeAnnotation.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(removeAnnotation);
				}
			}
		}
		if(renameFound)
			return matchingRefactorings;
		else
			return Collections.emptySet();
	}

	private Set<Refactoring> attributeRenamed(Set<VariableDeclaration> mergedAttributes, VariableDeclaration a2, Set<Refactoring> refactorings) {
		Set<Refactoring> matchingRefactorings = new HashSet<Refactoring>();
		boolean renameFound = false;
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof RenameAttributeRefactoring) {
				RenameAttributeRefactoring rename = (RenameAttributeRefactoring)refactoring;
				if(mergedAttributes.contains(rename.getOriginalAttribute().getVariableDeclaration()) && a2.equals(rename.getRenamedAttribute().getVariableDeclaration())) {
					matchingRefactorings.add(rename);
					renameFound = true;
				}
			}
			else if(refactoring instanceof ChangeAttributeTypeRefactoring) {
				ChangeAttributeTypeRefactoring changeType = (ChangeAttributeTypeRefactoring)refactoring;
				if(mergedAttributes.contains(changeType.getOriginalAttribute().getVariableDeclaration()) && a2.equals(changeType.getChangedTypeAttribute().getVariableDeclaration())) {
					matchingRefactorings.add(changeType);
				}
			}
			else if(refactoring instanceof ChangeAttributeAccessModifierRefactoring) {
				ChangeAttributeAccessModifierRefactoring changeAccessModifer = (ChangeAttributeAccessModifierRefactoring)refactoring;
				if(mergedAttributes.contains(changeAccessModifer.getAttributeBefore().getVariableDeclaration()) && a2.equals(changeAccessModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(changeAccessModifer);
				}
			}
			else if(refactoring instanceof AddAttributeModifierRefactoring) {
				AddAttributeModifierRefactoring addModifer = (AddAttributeModifierRefactoring)refactoring;
				if(mergedAttributes.contains(addModifer.getAttributeBefore().getVariableDeclaration()) && a2.equals(addModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(addModifer);
				}
			}
			else if(refactoring instanceof RemoveAttributeModifierRefactoring) {
				RemoveAttributeModifierRefactoring removeModifer = (RemoveAttributeModifierRefactoring)refactoring;
				if(mergedAttributes.contains(removeModifer.getAttributeBefore().getVariableDeclaration()) && a2.equals(removeModifer.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(removeModifer);
				}
			}
			else if(refactoring instanceof AddAttributeAnnotationRefactoring) {
				AddAttributeAnnotationRefactoring addAnnotation = (AddAttributeAnnotationRefactoring)refactoring;
				if(mergedAttributes.contains(addAnnotation.getAttributeBefore().getVariableDeclaration()) && a2.equals(addAnnotation.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(addAnnotation);
				}
			}
			else if(refactoring instanceof RemoveAttributeAnnotationRefactoring) {
				RemoveAttributeAnnotationRefactoring removeAnnotation = (RemoveAttributeAnnotationRefactoring)refactoring;
				if(mergedAttributes.contains(removeAnnotation.getAttributeBefore().getVariableDeclaration()) && a2.equals(removeAnnotation.getAttributeAfter().getVariableDeclaration())) {
					matchingRefactorings.add(removeAnnotation);
				}
			}
		}
		if(renameFound)
			return matchingRefactorings;
		else
			return Collections.emptySet();
	}

	private void deleteRemovedOperation(UMLOperation operation) {
		UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
		if(classDiff != null)
			classDiff.getRemovedOperations().remove(operation);
	}

	private void deleteAddedOperation(UMLOperation operation) {
		UMLClassBaseDiff classDiff = getUMLClassDiff(operation.getClassName());
		if(classDiff != null)
			classDiff.getAddedOperations().remove(operation);
	}
}
