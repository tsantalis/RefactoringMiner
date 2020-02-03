package org.refactoringminer.api;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.SyntacticTransformation;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.TypeFactMiner.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl.generateUrl;

public interface TypeRelatedRefactoring {

    boolean isResolved();
    TypFct getTypeB4();
    TypFct getTypeAfter();
    void updateTypeB4(GlobalContext gc);
    void updateTypeAfter(GlobalContext gc);

    void extractRealTypeChange();

    List<Tuple2<TypeGraph, TypeGraph>> getRealTypeChanges();

    TypeChangeInstance getTypeChangeInstance();

    Set<AbstractCodeMapping> getReferences();

    void updateTypeChangeInstance(Function<TypeChangeInstance, TypeChangeInstance> fn);


    default List<Tuple2<TypeGraph,TypeGraph>> extractRealTypeChange(SyntacticTransformation s){
        if(s.getSubTransformationsCount() == 0){
            return Arrays.asList(Tuple.of(s.getB4(),s.getAftr()));
        }else{
            return s.getSubTransformationsList().stream()
                    .flatMap(x->extractRealTypeChange(x).stream()).collect(toList());
        }
    }

    default List<CodeMapping> getCodeMapping( String url){

        return getReferences().stream().map(c -> CodeMapping.newBuilder()
                .setB4(c.getFragment1().getString())
                .setAfter(c.getFragment2().getString())
                .setIsSame(c.isExact())
                .setUrlbB4(generateUrl(c.getFragment1().getLocationInfo(), url, "L"))
                .setUrlAftr(generateUrl(c.getFragment2().getLocationInfo(), url, "R"))
                .addAllReplcementInferred(replacementInferred(c))
                .build()).collect(toList());


    }


    default List<ReplacementInferred> replacementInferred(AbstractCodeMapping c){
        return c.getReplacements().stream()
                .map(x -> ReplacementInferred.newBuilder().setB4(x.getBefore())
                        .setAftr(x.getAfter()).setReplacementType(x.getType().name()).build())
                .collect(toList());
    }


    Tuple2<LocationInfo, LocationInfo> getLocationOfType();


}
