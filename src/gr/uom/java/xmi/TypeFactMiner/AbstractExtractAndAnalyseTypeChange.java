package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import io.vavr.Tuple;
import io.vavr.Tuple3;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractExtractAndAnalyseTypeChange<X,Y> {

    protected X input;
    protected BiFunction<TypeGraph, TypeGraph, Y> defaultCase;

    protected AbstractExtractAndAnalyseTypeChange(X input, BiFunction<TypeGraph,TypeGraph, Y> defaultCase){
        this.input = input;
        this.defaultCase = defaultCase;
    }


    List<Tuple3<TypeGraph, TypeGraph, Y>> realTypeChange(TypFct from, TypFct to){
        TypeGraph fromType = from.getType();
        TypeGraph toType = to.getType();
        if (from.isResolved() && to.isResolved())
            return realTypeChange(fromType, toType);
        return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
    }

    List<Tuple3<TypeGraph, TypeGraph, Y>> realTypeChange(TypeGraph fromType, TypeGraph toType){
        switch (fromType.getRoot().getKind()) {
            case Primitive:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzePrimitiveToPrimitive(fromType, toType);
                    case Array: return analyzePrimitiveToArray(fromType, toType);
                    case Simple: return analyzePrimitiveToSimple(fromType, toType);
                    case Parameterized: return analyzePrimitiveToParameterized(fromType, toType);
                    case WildCard: return analyzePrimitiveToWildCard(fromType, toType);
                    default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
                }
            case Simple:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeSimpleToPrimitive(fromType, toType);
                    case Array: return analyzeSimpleToArray(fromType, toType);
                    case Simple: return analyzeSimpleToSimple(fromType, toType);
                    case Parameterized: return analyzeSimpleToParameterized(fromType, toType);
                    case WildCard: return analyzeSimpleToWildCard(fromType, toType);
                    default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
                }
            case Parameterized:
                switch (toType.getRoot().getKind()) {
                    case Array: return analyzeParameterizedToArray(fromType, toType);
                    case Simple: return analyzeParameterizedToSimple(fromType, toType);
                    case Parameterized: return analyzeParameterizedToParameterized(fromType, toType);
                    case WildCard: return analyzeParameterizedToWildCard(fromType, toType);
                    case Primitive: return analyzeParameterizedToPrimitive(fromType, toType);
                    default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
                }
            case Array: {
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeArrayToPrimitive(fromType, toType);
                    case Array: return analyzeArrayToArray(fromType, toType);
                    case Simple: return analyzeArrayToSimple(fromType, toType);
                    case Parameterized: return analyzeArrayToParameterized(fromType, toType);
                    case WildCard: return analyzeArrayToWildCard(fromType, toType);
                    default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
                }
            }
            case WildCard:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeWildCardToPrimitive(fromType, toType);
                    case Array:return analyzeWildCardToArray(fromType, toType);
                    case Simple: return analyzeWildCardToSimple(fromType, toType);
                    case Parameterized: return analyzeWildCardToParameterized(fromType, toType);
                    case WildCard: return analyzeWildCardToWildCard(fromType, toType);
                    default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());
                }
            default: return Stream.of(defaultCase.apply(fromType, toType)).map(x -> Tuple.of(fromType, toType, x)).collect(toList());

        }
    }

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzePrimitiveToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzePrimitiveToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzePrimitiveToSimple(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzePrimitiveToArray(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzePrimitiveToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeSimpleToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeSimpleToSimple(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeSimpleToArray(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeSimpleToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeSimpleToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeParameterizedToSimple(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeParameterizedToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeParameterizedToArray(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeParameterizedToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeParameterizedToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeArrayToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeArrayToArray(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeArrayToSimple(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeArrayToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeArrayToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeWildCardToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeWildCardToSimple(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeWildCardToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeWildCardToArray(TypeGraph fromType, TypeGraph toType);

    abstract List<Tuple3<TypeGraph, TypeGraph, Y>> analyzeWildCardToWildCard(TypeGraph fromType, TypeGraph toType);
}
