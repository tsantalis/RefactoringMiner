package org.refactoringminer.api;

import com.t2r.common.models.ast.GlobalContext;
import com.t2r.common.models.ast.TypFct;
import com.t2r.common.models.ast.TypeGraphOuterClass;
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
    List<Tuple3<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph, List<String>>> getRealTypeChanges();
}
