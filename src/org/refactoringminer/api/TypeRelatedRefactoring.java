package org.refactoringminer.api;

import gr.uom.java.xmi.TypeFactMiner.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.SyntacticTransformation;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface TypeRelatedRefactoring {

    boolean isResolved();
    TypFct getTypeB4();
    TypFct getTypeAfter();
    void updateTypeB4(GlobalContext gc);
    void updateTypeAfter(GlobalContext gc);

    void extractRealTypeChange();

    List<Tuple2<TypeGraph, TypeGraph>> getRealTypeChanges();

    TypeChangeInstance getTypeChangeInstance();

    default List<Tuple2<TypeGraph,TypeGraph>> extractRealTypeChange(SyntacticTransformation s){
        if(s.getSubTransformationsCount() == 0){
            return Arrays.asList(Tuple.of(s.getB4(),s.getAftr()));
        }else{
            return s.getSubTransformationsList().stream()
                    .flatMap(x->extractRealTypeChange(x).stream()).collect(Collectors.toList());
        }
    }

    default CodeMapping getCodeMapping(AbstractCodeMapping c){
//        System.out.println(c.isExact());
//        System.out.println(c.getReplacements());
//        System.out.println(c.getReplacements().stream().map(x -> x.getType().name()).collect(Collectors.joining(",")));
        return CodeMapping.newBuilder()
                .setB4(c.getFragment1().getString())
                .setAfter(c.getFragment2().getString())
                .setIsSame(c.isExact() )
                .build();
    }


}
