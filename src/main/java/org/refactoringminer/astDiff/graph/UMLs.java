package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import java.util.*;

public class UMLs {
    public Set<UMLClass> umlClasses = new HashSet<>();
    public Set<UMLOperation> umlOperations = new HashSet<>();
    public Set<UMLAttribute> umlAttributes = new HashSet<>();
    public Set<VariableDeclaration> variableDeclarations = new HashSet<>();
    public Map<UMLOperation, Set<VariableDeclaration>> operationParameters = new IdentityHashMap<>();

    UMLs() {}
  }