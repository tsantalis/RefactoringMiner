package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import java.util.Map;
import java.util.Set;

public class UMLs {
    public UMLModel umlModel;
    public Set<UMLClass> umlClasses;
    public Set<UMLOperation> umlOperations;
    public Set<UMLAttribute> umlAttributes;
    public Set<VariableDeclaration> variableDeclarations;
    public Map<UMLOperation, Set<VariableDeclaration>> operationParameters;

    UMLs(UMLModel umlModel, Set<UMLClass> umlClasses, Set<UMLOperation> umlOperations, Set<UMLAttribute> umlAttributes,
                      Set<VariableDeclaration> variableDeclarations, Map<UMLOperation, Set<VariableDeclaration>> operationParameters) {
      this.umlModel = umlModel;
      this.umlClasses = umlClasses;
      this.umlOperations = umlOperations;
      this.umlAttributes = umlAttributes;
      this.variableDeclarations = variableDeclarations;
      this.operationParameters = operationParameters;
    }
  }