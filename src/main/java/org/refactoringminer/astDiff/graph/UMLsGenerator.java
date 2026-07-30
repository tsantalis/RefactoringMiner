package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class UMLsGenerator {
  private UMLModelDiff modelDiff;

  UMLsGenerator(UMLModelDiff modelDiff) {
    this.modelDiff = modelDiff;
  }

  public UMLs getUMLs(Tree tree, SrcDst srcDst, String path, boolean isContext) {
    UMLs umls = new UMLs(srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel(),
            new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

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
      for (UMLClass umlClass : umls.umlModel.getClassList()) {
        getUMLs(path, pos, endPos, umlClass, umls);
      }
    }

    return umls;
  }

  private void getUMLs(String path, int pos, int endPos, UMLClass umlClass, UMLs umls) {
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
//        umlClass.getComments()
//        umlClass.getJavadoc()
    LocationInfo classLocation = umlClass.getLocationInfo();
    if (!path.equals(classLocation.getFilePath())) {
      return;
    }

    if (classLocation.getStartOffset() == pos && endPos == classLocation.getEndOffset()) {
      umls.umlClasses.add(umlClass);
    }

    getUMLs(path, pos, endPos, (UMLAbstractClass) umlClass, umls);
  }

  private void getUMLs(String path, int pos, int endPos, UMLAnonymousClass anonymousClass, UMLs umls) {
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

    getUMLs(path, pos, endPos, (UMLAbstractClass) anonymousClass, umls);
  }

  private void getUMLs(String path, int pos, int endPos, UMLAbstractClass umlClass, UMLs umls) {
    for (UMLAnonymousClass umlAnonymousClass : umlClass.getAnonymousClassList()) {
      getUMLs(path, pos, endPos, umlAnonymousClass, umls);
    }

    for (UMLOperation operation : umlClass.getOperations()) {
      getUMLs(path, pos, endPos, operation, umls);
    }

    for (UMLAttribute attribute : umlClass.getAttributes()) {
//          attribute.getAllLambdas()
//          attribute.getAllOperationInvocations()
//          attribute.getAllStringLiterals()
//          attribute.getAllVariables()
//          attribute.getComments()
//          attribute.getParameterDeclarationList()
//          attribute.getAllVariableDeclarations()
//          attribute.getJavadoc()
      LocationInfo attributeLocation = attribute.getFieldDeclarationLocationInfo();
      if (attributeLocation.getStartOffset() == pos && endPos == attributeLocation.getEndOffset()) {
        umls.umlAttributes.add(attribute);
      }

      for (UMLAnonymousClass umlAnonymousClass : attribute.getAnonymousClassList()) {
        getUMLs(path, pos, endPos, umlAnonymousClass, umls);
      }
    }
  }

  private void getUMLs(String path, int pos, int endPos, UMLOperation umlOperation, UMLs umls) {
//          operation.getAllLambdas()
//          operation.getAllOperationInvocations()
//          operation.getAllStringLiterals()
//          operation.getAllVariables()
//          operation.getNestedImports()
//          operation.getParameters()
//          operation.getComments()
//          operation.getJavadoc()
    LocationInfo operationLocation = umlOperation.getLocationInfo();
    if (!path.equals(operationLocation.getFilePath())) {
      return;
    }

    if (operationLocation.getStartOffset() == pos && endPos == operationLocation.getEndOffset()) {
      umls.umlOperations.add(umlOperation);
    }

    for (VariableDeclaration variableDeclaration : umlOperation.getAllVariableDeclarations()) {
      LocationInfo variableLocation = variableDeclaration.getLocationInfo();
      if (variableLocation.getStartOffset() == pos && endPos == variableLocation.getEndOffset()) {
        umls.variableDeclarations.add(variableDeclaration);
      }
    }

    for (VariableDeclaration parameterDeclaration : umlOperation.getParameterDeclarationList()) {
      LocationInfo parameterLocation = parameterDeclaration.getLocationInfo();
      if (parameterLocation.getStartOffset() == pos && endPos == parameterLocation.getEndOffset()) {
        umls.operationParameters.putIfAbsent(umlOperation, new HashSet<>());
        umls.operationParameters.get(umlOperation).add(parameterDeclaration);
      }
    }

    for (UMLOperation nestedOperation : umlOperation.getNestedOperations()) {
      getUMLs(path, pos, endPos, nestedOperation, umls);
    }

    for (UMLClass nestedClass : umlOperation.getNestedClasses()) {
      getUMLs(path, pos, endPos, nestedClass, umls);
    }

    for (UMLAnonymousClass umlAnonymousClass : umlOperation.getAnonymousClassList()) {
      getUMLs(path, pos, endPos, umlAnonymousClass, umls);
    }
  }
}
