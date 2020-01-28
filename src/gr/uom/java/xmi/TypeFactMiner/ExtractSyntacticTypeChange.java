package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.SyntacticTransformation;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import io.vavr.Tuple3;
import org.refactoringminer.api.TypeRelatedRefactoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind.*;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.Map.*;
import static java.util.stream.Collectors.toList;

public class ExtractSyntacticTypeChange extends AbstractTypeChangeVisitor<TypeRelatedRefactoring, SyntacticTransformation> {

    public ExtractSyntacticTypeChange() {
        super((t1, t2) -> SyntacticTransformation.newBuilder()
                .setB4(t1).setAftr(t2).addTransformation(replaceDescription(t1.getRoot().getKind(), t2.getRoot().getKind())).build());
    }

    protected ExtractSyntacticTypeChange(BiFunction<TypeGraph, TypeGraph, SyntacticTransformation> defaultCase) {
        super(defaultCase);
    }


    private Supplier<ArrayList<Tuple3<TypeGraph, TypeGraph, TypeChangeInstance>>> emptyList = ArrayList::new;

    public static String replaceDescription(TypeKind b4, TypeKind aftr) {
        if (b4.equals(aftr))
            return "Update " + b4.toString();
        return "Replace " + b4.toString() + " with " + aftr.toString();
    }

    public static boolean prettyEqual(TypeGraph t1, TypeGraph t2) {
        return pretty(t1).equals(pretty(t2));
    }


    @Override
    public SyntacticTransformation analyzePrimitiveToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Primitive, Primitive)).build();
    }


    @Override
    public SyntacticTransformation analyzePrimitiveToParameterized(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Primitive, Parameterized)).build();
    }

    @Override
    public SyntacticTransformation analyzePrimitiveToSimple(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Primitive, Simple)).build();
    }

    @Override
    public SyntacticTransformation analyzePrimitiveToArray(TypeGraph fromType, TypeGraph toType) {

        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Primitive, Array));

        if (prettyEqual(getOf(toType, "of"), fromType))
            builder.addTransformation("Convert to Array");
        else
            builder.addSubTransformations(extract(fromType, getOf(toType, "of")));

        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzePrimitiveToWildCard(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Primitive, WildCard));

        if (getOf(toType, "extends") != null)
            builder.addSubTransformations(extract(fromType, getOf(toType, "extends")));
        else if (getOf(toType, "super") != null)
            builder.addSubTransformations(extract(fromType, getOf(toType, "super")));

        return builder.build();
    }


    @Override
    public SyntacticTransformation analyzeSimpleToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Simple, Primitive)).build();
    }


    @Override
    public SyntacticTransformation analyzeSimpleToSimple(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Simple, Simple)).build();
    }

    @Override
    public SyntacticTransformation analyzeSimpleToArray(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Simple, Array));

        if (prettyEqual(getOf(toType, "of"), fromType))
            builder.addTransformation("Convert to Array");
        else
            builder.addSubTransformations(extract(fromType, getOf(toType, "of")));

        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeSimpleToWildCard(TypeGraph fromType, TypeGraph toType) {
        return toWildCard(fromType, Simple ,toType);
    }

    @Override
    public SyntacticTransformation analyzeSimpleToParameterized(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Simple, Parameterized));

        if (prettyEqual(getOf(toType, "of"), fromType))
            builder.addTransformation("Add Type Parameters");
        else if (toType.getEdgesMap().values().stream().anyMatch(t -> prettyEqual(t, fromType)))
            builder.addTransformation("Wrap With Parameterized Type");

        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeParameterizedToSimple(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Parameterized, Simple));
        if (prettyEqual(getOf(fromType, "of"), toType))
            builder.addTransformation("Remove Type Parameters");
        else if (fromType.getEdgesMap().values().stream().anyMatch(t -> prettyEqual(t, toType)))
            builder.addTransformation("UnWrap Parameterized Type");
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeParameterizedToParameterized(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Parameterized, Parameterized));
        if (prettyEqual(getOf(fromType, "of"), getOf(toType, "of"))) {

            List<TypeGraph> b4Params = fromType.getEdgesMap().entrySet().stream()
                    .filter(x -> x.getKey().startsWith("Param:")).map(Entry::getValue).collect(toList());
            List<TypeGraph> afterParams = toType.getEdgesMap().entrySet().stream()
                    .filter(x -> x.getKey().startsWith("Param:")).map(Entry::getValue).collect(toList());
            if (b4Params.size() == afterParams.size()) {
                builder.addTransformation("Update Type Parameters");
                if(new HashSet<>(b4Params).equals(new HashSet<>(afterParams))) {
                    builder.addTransformation("Reorder Type Parameters");
                } else {
                    for (int i = 0; i < b4Params.size(); i++) {
                        if (!prettyEqual(b4Params.get(i), afterParams.get(i))) {
                            builder.addSubTransformations(extract(b4Params.get(i), afterParams.get(i)));
                        }
                    }
                }
            }else if(b4Params.size() < afterParams.size()) {
                builder.addTransformation("Add Type Parameters");
            } else
                builder.addTransformation("Remove Type Parameters");

        } else {
            builder.addTransformation("Update Container");
            builder.addSubTransformations(extract(getOf(fromType, "of"), getOf(toType, "of")));
        }
        return builder.build();
    }

    private TypeGraph getOf(TypeGraph fromType, String of) {
        return fromType.getEdgesMap().get(of);
    }

    @Override
    public SyntacticTransformation analyzeParameterizedToArray(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Parameterized, Array));
        if (prettyEqual(getOf(toType, "of"), fromType))
            builder.addTransformation("Convert To Array");
        else
            builder.addSubTransformations(extract(fromType, getOf(toType, "of")));
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeParameterizedToWildCard(TypeGraph fromType, TypeGraph toType) {
        return toWildCard(fromType, Parameterized, toType);
    }

    private SyntacticTransformation toWildCard(TypeGraph fromType,TypeKind k, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(k, WildCard));
        if (toType.getEdgesMap().containsKey("extends")) {
            if (prettyEqual(fromType, getOf(toType, "extends"))) {
                builder.addTransformation("Convert to WildCard");
            } else {
                builder.addSubTransformations(extract(fromType, getOf(toType, "extends")));
            }
        } else if (toType.getEdgesMap().containsKey("super")) {
            if (prettyEqual(fromType, getOf(toType, "super"))) {
                builder.addTransformation("Convert to WildCard");
            } else {
                builder.addSubTransformations(extract(fromType, getOf(toType, "super")));
            }
        }
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeParameterizedToPrimitive(TypeGraph fromType, TypeGraph toType) {
        return SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Parameterized, Primitive)).build();
    }


    @Override
    public SyntacticTransformation analyzeArrayToPrimitive(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Array, Primitive));
        if (pretty(getOf(fromType, "of")).equals(pretty(fromType)))
            builder.addTransformation("Convert From Array");
        else
            builder.addSubTransformations(extract(getOf(fromType, "of"), toType));
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeArrayToArray(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Array, Array));
        builder.addSubTransformations(extract(getOf(fromType, "of"), getOf(toType, "of")));
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeArrayToSimple(TypeGraph fromType, TypeGraph toType) {
        return fromArray(fromType, toType);
    }

    public SyntacticTransformation fromArray(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Array, toType.getRoot().getKind()));
        if (pretty(getOf(fromType, "of")).equals(pretty(toType)))
            builder.addTransformation("Convert From Array");
        else
            builder.addSubTransformations(extract(getOf(fromType, "of"), toType));
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeArrayToParameterized(TypeGraph fromType, TypeGraph toType) {
        return fromArray(fromType, toType);
    }

    @Override
    public SyntacticTransformation analyzeArrayToWildCard(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(Array, WildCard));
        if (toType.getEdgesMap().containsKey("extends")) {
            builder.addTransformation("Add upper bound");
            if (prettyEqual(fromType, getOf(toType, "extends"))) {
                builder.addTransformation("Convert to WildCard");
            } else {
                builder.addSubTransformations(extract(fromType, getOf(toType, "extends")));
            }
        } else if (toType.getEdgesMap().containsKey("super")) {
            builder.addTransformation("Add lower bound");
            if (prettyEqual(fromType, getOf(toType, "super"))) {
                builder.addTransformation("Convert to WildCard");
            } else {
                builder.addSubTransformations(extract(fromType, getOf(toType, "super")));
            }
        }
        return builder.build();
    }


    @Override
    public SyntacticTransformation analyzeWildCardToPrimitive(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(WildCard, Primitive));
        if (getOf(fromType, "extends") != null)
            builder.addSubTransformations(extract(getOf(fromType, "extends"), toType));
        if (getOf(fromType, "super") != null)
            builder.addSubTransformations(extract(getOf(fromType, "super"), toType));
        return builder.build();
    }

    @Override
    public SyntacticTransformation analyzeWildCardToSimple(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType, Simple);
    }

    @Override
    public SyntacticTransformation analyzeWildCardToParameterized(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType, Parameterized);
    }

    @Override
    public SyntacticTransformation analyzeWildCardToArray(TypeGraph fromType, TypeGraph toType) {
        return fromWildCard(fromType, toType, Array);
    }

    @Override
    public SyntacticTransformation analyzeWildCardToWildCard(TypeGraph fromType, TypeGraph toType) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(WildCard, WildCard));
        if (fromType.getEdgesMap().containsKey("extends")) {
            if (toType.getEdgesMap().containsKey("extends")) {
                builder.addSubTransformations(extract(getOf(fromType, "extends"), getOf(toType, "extends")));
            } else if (toType.getEdgesMap().containsKey("super")) {
                builder.addTransformation("Change upper bound to lower bound");
                builder.addSubTransformations(extract(getOf(fromType, "extends"), getOf(toType, "super")));
            } else {
                builder.addTransformation("Remove upper bound");
            }
        } else if (fromType.getEdgesMap().containsKey("super")) {
            if (toType.getEdgesMap().containsKey("extends")) {
                builder.addTransformation("Change lower bound to upper bound");
                builder.addSubTransformations(extract(getOf(fromType, "super"), getOf(toType, "extends")));
            } else if (toType.getEdgesMap().containsKey("super")) {
                builder.addSubTransformations(extract(getOf(fromType, "super"), getOf(toType, "super")));
            } else {
                builder.addTransformation("Remove lower bound");
            }
        } else if (toType.getEdgesMap().containsKey("extends")) {
            builder.addTransformation("Introduce upper bound");
        } else if (toType.getEdgesMap().containsKey("super")) {
            builder.addTransformation("Remove lower bound");
        }
        return builder.build();
    }

    private SyntacticTransformation fromWildCard(TypeGraph fromType, TypeGraph toType, TypeKind typeKind) {
        SyntacticTransformation.Builder builder = SyntacticTransformation.newBuilder()
                .setB4(fromType).setAftr(toType).addTransformation(replaceDescription(WildCard, typeKind));
        if (fromType.getEdgesCount() > 0 && fromType.getEdgesMap().containsKey("extends")) {
            if (prettyEqual(getOf(fromType, "extends"), toType)) {
                builder.addTransformation("Convert from WildCard");
            } else {
                builder.addSubTransformations(extract(getOf(fromType, "extends"), toType));
            }
        } else if (fromType.getEdgesCount() > 0 && fromType.getEdgesMap().containsKey("super")) {
            if (prettyEqual(getOf(fromType, "super"), toType)) {
                builder.addTransformation("Convert from WildCard");
            } else {
                builder.addSubTransformations(extract(getOf(fromType, "super"), toType));
            }
        }
        return builder.build();
    }
}
