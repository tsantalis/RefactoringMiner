package org.refactoringminer.api;

import gr.uom.java.xmi.TypeFactMiner.Models.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass.TypeGraph;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import io.vavr.Tuple3;

import java.util.List;
import java.util.Map;

public interface TypeRelatedRefactoring {
    boolean isResolved();
    TypFct getTypeB4();
    TypFct getTypeAfter();
    Map<String, TypFct> getFieldTypeMapB4();
    Map<String, TypFct> getFieldTypeMapAfter();
    Map<String, TypFct> getVariableTypeMapB4();
    Map<String, TypFct> getVariableTypeMapAfter();
    void updateTypeB4(GlobalContext gc);
    void updateTypeAfter(GlobalContext gc);
    void updateTypeNameSpaceBefore(GlobalContext gc);
    void updateTypeNameSpaceAfter(GlobalContext gc);
    void extractRealTypeChange(GlobalContext gc);
    List<Tuple3<TypeGraph, TypeGraph, List<String>>> getRealTypeChanges();
}
