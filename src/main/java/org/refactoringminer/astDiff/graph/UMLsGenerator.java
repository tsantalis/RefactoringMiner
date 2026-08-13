package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.*;

public class UMLsGenerator {
  private UMLModelDiff modelDiff;
  private final Map<String, String> srcContents;
  private final Map<String, String> dstContents;

  UMLsGenerator(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents) {
    this.modelDiff = modelDiff;
    this.srcContents = srcContents;
    this.dstContents = dstContents;
  }

  public UMLs getUMLs(Tree tree, SrcDst srcDst, String path, boolean isContext) {
    UMLs umls = new UMLs();

    Map<String, String> contents = srcDst.equals(SrcDst.SRC) ? srcContents : dstContents;

    List<Tree> subTrees = new ArrayList<>();
    subTrees.add(tree);
    if (!isContext) {
      subTrees.addAll(tree.getDescendants());
    }
    for (Tree subTree : subTrees) {
      int pos = subTree.getPos();
      int endPos = subTree.getEndPos();

//      umls.umlModel.getModuleList()
//      umls.umlModel.getPackageInfoList()
      UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
      for (UMLClass umlClass : umlModel.getClassList()) {
        getUMLs(path, pos, endPos, umlClass, contents, umls);
      }
    }

    return umls;
  }

  private void getUMLs(String path, int pos, int endPos, UMLClass umlClass, Map<String, String> contents, UMLs umls) {
//        umlClass.getPreprocessorStatements()
//        umlClass.getTypeAliasList()
//        umlClass.getTypeParameters()
//        umlClass.getCompanionObjects()
//        umlClass.getEnumConstants()
//        umlClass.getSuperTypeCallEntries()
//        umlClass.getImplementedInterfaces()
//        umlClass.getImportedTypes()
//        umlClass.getInitializers()
//        umlClass.getPermittedTypes()
//        umlClass.getJavadoc()
    LocationInfo classLocation = umlClass.getLocationInfo();
    if (!path.equals(classLocation.getFilePath())) {
      return;
    }

    if (checkExactOffsets(classLocation, pos, endPos, contents)) {
      umls.umlClasses.add(umlClass);
    }

    getUMLs(path, pos, endPos, (UMLAbstractClass) umlClass, contents, umls);
  }

  private void getUMLs(String path, int pos, int endPos, UMLAnonymousClass anonymousClass, Map<String, String> contents, UMLs umls) {
//    anonymousClass.getParentContainers()
//    anonymousClass.getAnonymousClassList()
//    anonymousClass.getComments()
//    anonymousClass.getCompanionObjects()
//    anonymousClass.getEnumConstants()
//    anonymousClass.getImplementedInterfaces()
//    anonymousClass.getImportedTypes()
//    anonymousClass.getInitializers()
//    anonymousClass.getPermittedTypes()
    LocationInfo anonymousClassLocation = anonymousClass.getLocationInfo();
    if (!path.equals(anonymousClassLocation.getFilePath())) {
      return;
    }

    getUMLs(path, pos, endPos, (UMLAbstractClass) anonymousClass, contents, umls);
  }

  private void getUMLs(String path, int pos, int endPos, UMLAbstractClass umlClass, Map<String, String> contents, UMLs umls) {
    for (UMLAnonymousClass umlAnonymousClass : umlClass.getAnonymousClassList()) {
      getUMLs(path, pos, endPos, umlAnonymousClass, contents, umls);
    }

    for (UMLOperation operation : umlClass.getOperations()) {
      getUMLs(path, pos, endPos, operation, contents, umls);
    }

    for (UMLAttribute attribute : umlClass.getAttributes()) {
//          attribute.getAllLambdas()
//          attribute.getAllOperationInvocations()
//          attribute.getAllStringLiterals()
//          attribute.getAllVariables()
//          attribute.getParameterDeclarationList()
//          attribute.getAllVariableDeclarations()
//          attribute.getJavadoc()
      Set<LocationInfo> attributeLocations = new HashSet<>();
      attributeLocations.add(attribute.getLocationInfo());
      attributeLocations.add(attribute.getVariableDeclaration().getLocationInfo());
      if (attribute.getFieldDeclarationLocationInfo() != null) {
        attributeLocations.add(attribute.getFieldDeclarationLocationInfo());
      }

      if (attributeLocations.stream().anyMatch(al -> checkExactOffsets(al, pos, endPos, contents))) {
        umls.umlAttributes.add(attribute);
      }

      for (UMLAnonymousClass umlAnonymousClass : attribute.getAnonymousClassList()) {
        getUMLs(path, pos, endPos, umlAnonymousClass, contents, umls);
      }
    }
  }

  private void getUMLs(String path, int pos, int endPos, UMLOperation umlOperation, Map<String, String> contents, UMLs umls) {
//          operation.getAllLambdas()
//          operation.getAllOperationInvocations()
//          operation.getAllStringLiterals()
//          operation.getAllVariables()
//          operation.getNestedImports()
//          operation.getParameters()
//          operation.getJavadoc()
    LocationInfo operationLocation = umlOperation.getLocationInfo();
    if (!path.equals(operationLocation.getFilePath())) {
      return;
    }

    if (checkExactOffsets(operationLocation, pos, endPos, contents)) {
      umls.umlOperations.add(umlOperation);
    }

    for (VariableDeclaration variableDeclaration : umlOperation.getAllVariableDeclarations()) {
      LocationInfo variableLocation = variableDeclaration.getLocationInfo();
      if (checkExactOffsets(variableLocation, pos, endPos, contents)) {
        umls.variableDeclarations.add(variableDeclaration);
      }
    }

    for (VariableDeclaration parameterDeclaration : umlOperation.getParameterDeclarationList()) {
      LocationInfo parameterLocation = parameterDeclaration.getLocationInfo();
      if (checkExactOffsets(parameterLocation, pos, endPos, contents)) {
        umls.operationParameters.putIfAbsent(umlOperation, new HashSet<>());
        umls.operationParameters.get(umlOperation).add(parameterDeclaration);
      }
    }

    for (UMLOperation nestedOperation : umlOperation.getNestedOperations()) {
      getUMLs(path, pos, endPos, nestedOperation, contents, umls);
    }

    for (UMLClass nestedClass : umlOperation.getNestedClasses()) {
      getUMLs(path, pos, endPos, nestedClass, contents, umls);
    }

    for (UMLAnonymousClass umlAnonymousClass : umlOperation.getAnonymousClassList()) {
      getUMLs(path, pos, endPos, umlAnonymousClass, contents, umls);
    }
  }

  private boolean checkExactOffsets(LocationInfo locationInfo, int pos, int endPos, Map<String, String> contents) {
    String fileContent = contents.get(locationInfo.getFilePath());

    int originalStartOffset = locationInfo.getStartOffset();
    int originalEndOffset = locationInfo.getEndOffset();
    String trimmedContent = fileContent.substring(originalStartOffset, originalEndOffset).trim();

    int trimmedStartOffset = fileContent.indexOf(trimmedContent, originalStartOffset);
    int trimmedEndOffset = trimmedStartOffset + trimmedContent.length();

    return (originalStartOffset == pos && originalEndOffset == endPos) || (trimmedStartOffset == pos && trimmedEndOffset == endPos);
  }
}
