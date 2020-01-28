package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.refactorings.CommitInfoOuterClass.CommitInfo.JarInfo;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.PrimitiveAnalysis;

import java.nio.file.Path;
import java.util.Set;

import static gr.uom.java.xmi.TypeFactMiner.CompositionUtil.getCompositionRelation;
import static gr.uom.java.xmi.TypeFactMiner.ExtractSyntacticTypeChange.prettyEqual;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.R_SUPER_T;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.T_SUPER_R;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.getHierarchyRelation;
import static com.t2r.common.models.ast.TypeGraphOuterClass.*;

import static com.t2r.common.utilities.PrettyPrinter.pretty;

public class ExtractHierarchyPrimitiveCompositionInfo extends AbstractTypeChangeVisitor<GlobalContext, TypeChangeAnalysis> {

    private Set<JarInfo> jars;
    private Path pathToJars;

    public ExtractHierarchyPrimitiveCompositionInfo(GlobalContext input, Set<JarInfo> jars, Path pathToJars) {
        super((t1,t2) -> TypeChangeAnalysis.newBuilder().build(), input);
        this.jars = jars;
        this.pathToJars = pathToJars;
    }

    private ExtractHierarchyPrimitiveCompositionInfo(GlobalContext input) {
        super((t1,t2) -> TypeChangeAnalysis.newBuilder().build(), input);
    }

    @Override
    public TypeChangeAnalysis analyzePrimitiveToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return TypeChangeAnalysis.newBuilder()
                .setPrimitiveInfo(PrimitiveAnalysis.newBuilder()
                        .setNarrowing(isNarrowing(fromType, toType))
                        .setWidening(isWidening(fromType, toType))
                        .build()).build();
    }

    @Override
    public TypeChangeAnalysis analyzePrimitiveToParameterized(TypeGraph fromType, TypeGraph toType) {
        return TypeChangeAnalysis.newBuilder().build();
    }

    @Override
    public TypeChangeAnalysis analyzePrimitiveToSimple(TypeGraph fromType, TypeGraph toType) {
        return TypeChangeAnalysis.newBuilder()
                .setPrimitiveInfo(PrimitiveAnalysis.newBuilder()
                        .setBoxing(isBoxing(fromType, toType))
                        .build()).build();
    }

    @Override
    public TypeChangeAnalysis analyzePrimitiveToArray(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType, toType);
    }

    private TypeChangeAnalysis extractFromAndOfTo(TypeGraph fromType, TypeGraph toType) {
        return extract(fromType, toType.getEdgesMap().get("of"));
    }

    @Override
    public TypeChangeAnalysis analyzePrimitiveToWildCard(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeSimpleToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return TypeChangeAnalysis.newBuilder()
                .setPrimitiveInfo(PrimitiveAnalysis.newBuilder()
                        .setUnboxing(isUnBoxing(fromType, toType))
                        .build()).build();
    }

    @Override
    public TypeChangeAnalysis analyzeSimpleToSimple(TypeGraph fromType, TypeGraph toType) {

        return TypeChangeAnalysis.newBuilder()
                .setHierarchyRelation(getHierarchyRelation(fromType, toType, input, jars, pathToJars).toString())
                .setB4ComposesAfter(getCompositionRelation(fromType, toType, input, jars, pathToJars))
                .build();
    }

    @Override
    public TypeChangeAnalysis analyzeSimpleToArray(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeSimpleToWildCard(TypeGraph fromType, TypeGraph toType) {
        return toWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeSimpleToParameterized(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeParameterizedToSimple(TypeGraph fromType, TypeGraph toType) {
        return extract(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeParameterizedToParameterized(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeParameterizedToArray(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeParameterizedToWildCard(TypeGraph fromType, TypeGraph toType) {
        return toWildCard(fromType, toType);
    }

    private TypeChangeAnalysis toWildCard(TypeGraph fromType, TypeGraph toType) {
        if (toType.getEdgesCount() > 0) {
            if (toType.getEdgesMap().containsKey("extends")) {
                if (prettyEqual(fromType, toType.getEdgesMap().get("extends"))) {
                    return TypeChangeAnalysis.newBuilder()
                            .setHierarchyRelation(R_SUPER_T.name())
                            .build();
                }
            } else if (toType.getEdgesMap().containsKey("super")) {
                if (prettyEqual(fromType, toType.getEdgesMap().get("super"))) {
                    return TypeChangeAnalysis.newBuilder()
                            .setHierarchyRelation(T_SUPER_R.name())
                            .build();
                }
            }
        }
        return TypeChangeAnalysis.newBuilder().build();
    }

    @Override
    public TypeChangeAnalysis analyzeParameterizedToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return TypeChangeAnalysis.newBuilder().build();
    }

    @Override
    public TypeChangeAnalysis analyzeArrayToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return extract(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeArrayToArray(TypeGraph fromType, TypeGraph toType) {
        return extractFromAndOfTo(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeArrayToSimple(TypeGraph fromType, TypeGraph toType) {
        return extract(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeArrayToParameterized(TypeGraph fromType, TypeGraph toType) {
        return extract(fromType.getEdgesMap().get("of"), toType);
    }

    @Override
    public TypeChangeAnalysis analyzeArrayToWildCard(TypeGraph fromType, TypeGraph toType) {
        return toWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeWildCardToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeWildCardToSimple(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeWildCardToParameterized(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeWildCardToArray(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType);
    }

    @Override
    public TypeChangeAnalysis analyzeWildCardToWildCard(TypeGraph fromType, TypeGraph toType) {
        if (fromType.getEdgesMap().containsKey("extends")) {
            if (toType.getEdgesMap().containsKey("extends")) {
                return extractFromAndOfTo(fromType.getEdgesMap().get("extends"), toType);
            } else {
                return TypeChangeAnalysis.newBuilder()
                        .setHierarchyRelation(T_SUPER_R.name())
                        .build();
            }
        } else if (fromType.getEdgesMap().containsKey("super")) {
            if (toType.getEdgesMap().containsKey("super"))
                return extractFromAndOfTo(fromType.getEdgesMap().get("super"), toType);
            else
                return TypeChangeAnalysis.newBuilder()
                        .setHierarchyRelation(R_SUPER_T.name())
                        .build();

        } else if (toType.getEdgesMap().containsKey("extends")) {
            return TypeChangeAnalysis.newBuilder()
                    .setHierarchyRelation(T_SUPER_R.name())
                    .build();
        } else if (toType.getEdgesMap().containsKey("super")) {
            return TypeChangeAnalysis.newBuilder()
                    .setHierarchyRelation(R_SUPER_T.name())
                    .build();
        }
        return TypeChangeAnalysis.newBuilder().build();
    }

    private TypeChangeAnalysis fromWildCard(TypeGraph fromType, TypeGraph toType) {
        if (fromType.getEdgesCount() > 0) {
            if (fromType.getEdgesMap().containsKey("extends")) {
                if (prettyEqual(fromType.getEdgesMap().get("extends"), toType)) {
                    return TypeChangeAnalysis.newBuilder()
                            .setHierarchyRelation(T_SUPER_R.name())
                            .build();
                }
            } else if (fromType.getEdgesMap().containsKey("super")) {
                if (prettyEqual(fromType.getEdgesMap().get("super"), toType)) {
                    return TypeChangeAnalysis.newBuilder()
                            .setHierarchyRelation(R_SUPER_T.name())
                            .build();
                }
            }
        }
        return TypeChangeAnalysis.newBuilder().build();
    }

    private static boolean isUnBoxing(TypeGraph b4, TypeGraph aftr) {
        return checkForUnBoxing(pretty(b4), pretty(aftr));
    }

    private static boolean checkForUnBoxing(String t1, String t2) {
        return (t2.equals("short") && t1.equals("java.lang.Short"))
                || (t2.equals("int") && t1.equals("java.lang.Integer"))
                || (t2.equals("void") && t1.equals("java.lang.Void"))
                || (t2.equals("double") && t1.equals("java.lang.Double"))
                || (t2.equals("float") && t1.equals("java.lang.Float"))
                || (t2.equals("long") && t1.equals("java.lang.Long"))
                || (t2.equals("char") && t1.equals("java.lang.Character"))
                || (t2.equals("byte") && t1.equals("java.lang.Byte"))
                || (t2.equals("boolean") && t1.equals("java.lang.Boolean"));
    }

    private static boolean isBoxing(TypeGraph b4, TypeGraph aftr) {
        return checkForUnBoxing(pretty(aftr), pretty(b4));
    }

    static boolean isNarrowing(TypeGraph t1, TypeGraph t2) {
        String b4 = pretty(t1);
        String aftr = pretty(t2);
        return (b4.equals("short") && (aftr.equals("byte") || aftr.equals("char")))
                || (b4.equals("char") && (aftr.equals("byte") || aftr.equals("short")))
                || (b4.equals("int") && (aftr.equals("byte") || aftr.equals("char") || aftr.equals("short")))
                || (b4.equals("long") && (aftr.equals("byte") || aftr.equals("char") || aftr.equals("int") || aftr.equals("short")))
                || (b4.equals("float") && (aftr.equals("byte") || aftr.equals("char") || aftr.equals("int") || aftr.equals("short") || aftr.equals("long")))
                || (b4.equals("double") && (aftr.equals("byte") || aftr.equals("char") || aftr.equals("int") || aftr.equals("short") || aftr.equals("long") || aftr.equals("float")));
    }

    static boolean isWidening(TypeGraph t1, TypeGraph t2) {
        String b4 = pretty(t1);
        String aftr = pretty(t2);
        return (b4.equals("float") && (aftr.equals("double")))
                || (b4.equals("long") && (aftr.equals("double") || aftr.equals("float")))
                || (b4.equals("int") && (aftr.equals("long") || aftr.equals("float") || aftr.equals("double")))
                || (b4.equals("char") && (aftr.equals("long") || aftr.equals("int") || aftr.equals("short") || aftr.equals("double")))
                || (b4.equals("short") && (aftr.equals("double") || aftr.equals("int") || aftr.equals("short") || aftr.equals("long")))
                || (b4.equals("byte") && (aftr.equals("double") || aftr.equals("int") || aftr.equals("short") || aftr.equals("long") || aftr.equals("float")));
    }
}
