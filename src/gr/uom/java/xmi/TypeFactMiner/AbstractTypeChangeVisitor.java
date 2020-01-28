package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;

import java.util.function.BiFunction;

public abstract class AbstractTypeChangeVisitor<X,Y> {

    protected X input;
    protected BiFunction<TypeGraph, TypeGraph, Y> defaultCase;

    protected AbstractTypeChangeVisitor(BiFunction<TypeGraph,TypeGraph, Y> defaultCase){
        this.defaultCase = defaultCase;
    }

    protected AbstractTypeChangeVisitor(BiFunction<TypeGraph,TypeGraph, Y> defaultCase, X input){
        this.input = input;
        this.defaultCase = defaultCase;
    }


   public Y extract(TypFct from, TypFct to){
        TypeGraph fromType = from.getType();
        TypeGraph toType = to.getType();
        if (from.isResolved() && to.isResolved())
            return extract(fromType, toType);
        return defaultCase.apply(fromType, toType);
    }

    public Y extract(TypeGraph fromType, TypeGraph toType){
        switch (fromType.getRoot().getKind()) {
            case Primitive:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzePrimitiveToPrimitive(fromType, toType);
                    case Array: return analyzePrimitiveToArray(fromType, toType);
                    case Simple: return analyzePrimitiveToSimple(fromType, toType);
                    case Parameterized: return analyzePrimitiveToParameterized(fromType, toType);
                    case WildCard: return analyzePrimitiveToWildCard(fromType, toType);
                    default: return defaultCase.apply(fromType, toType);
                }
            case Simple:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeSimpleToPrimitive(fromType, toType);
                    case Array: return analyzeSimpleToArray(fromType, toType);
                    case Simple: return analyzeSimpleToSimple(fromType, toType);
                    case Parameterized: return analyzeSimpleToParameterized(fromType, toType);
                    case WildCard: return analyzeSimpleToWildCard(fromType, toType);
                    default: return defaultCase.apply(fromType, toType);
                }
            case Parameterized:
                switch (toType.getRoot().getKind()) {
                    case Array: return analyzeParameterizedToArray(fromType, toType);
                    case Simple: return analyzeParameterizedToSimple(fromType, toType);
                    case Parameterized: return analyzeParameterizedToParameterized(fromType, toType);
                    case WildCard: return analyzeParameterizedToWildCard(fromType, toType);
                    case Primitive: return analyzeParameterizedToPrimitive(fromType, toType);
                    default: return defaultCase.apply(fromType, toType);
                }
            case Array: {
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeArrayToPrimitive(fromType, toType);
                    case Array: return analyzeArrayToArray(fromType, toType);
                    case Simple: return analyzeArrayToSimple(fromType, toType);
                    case Parameterized: return analyzeArrayToParameterized(fromType, toType);
                    case WildCard: return analyzeArrayToWildCard(fromType, toType);
                    default: return defaultCase.apply(fromType, toType);
                }
            }
            case WildCard:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeWildCardToPrimitive(fromType, toType);
                    case Array:return analyzeWildCardToArray(fromType, toType);
                    case Simple: return analyzeWildCardToSimple(fromType, toType);
                    case Parameterized: return analyzeWildCardToParameterized(fromType, toType);
                    case WildCard: return analyzeWildCardToWildCard(fromType, toType);
                    default: return defaultCase.apply(fromType, toType);
                }
            default: return defaultCase.apply(fromType, toType);

        }
    }

    abstract Y analyzePrimitiveToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzePrimitiveToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzePrimitiveToSimple(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzePrimitiveToArray(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzePrimitiveToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeSimpleToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeSimpleToSimple(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeSimpleToArray(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeSimpleToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeSimpleToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeParameterizedToSimple(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeParameterizedToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeParameterizedToArray(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeParameterizedToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeParameterizedToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeArrayToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeArrayToArray(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeArrayToSimple(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeArrayToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeArrayToWildCard(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeWildCardToPrimitive(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeWildCardToSimple(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeWildCardToParameterized(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeWildCardToArray(TypeGraph fromType, TypeGraph toType);

    abstract Y analyzeWildCardToWildCard(TypeGraph fromType, TypeGraph toType);
}
