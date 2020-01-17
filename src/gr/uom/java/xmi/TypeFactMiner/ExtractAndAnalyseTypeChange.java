package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypFct;
import com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind;
import com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeSem;
import com.t2r.common.models.ast.GlobalContext;
import io.vavr.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.NO_HIERARCHY_SUPPORT;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.R_SUPER_T;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.T_SUPER_R;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.getHierarchyRelation;
import static com.t2r.common.models.ast.TypeGraphOuterClass.*;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.NameSpace.TypeVariable;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind.*;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind.Primitive;
import static com.t2r.common.utilities.TypeGraphUtil.pretty;
import static java.util.stream.Collectors.toList;

public class ExtractAndAnalyseTypeChange {

    public static String replaceDescription(TypeKind b4, TypeKind aftr) {
        if (b4.equals(aftr))
            return "Update " + b4.toString();
        return "Replace " + b4.toString() + " with " + aftr.toString();
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> getReplacement(TypeGraph fromType, TypeGraph toType) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        typeChangeInfo.add(Tuple.of(fromType, toType, Arrays.asList(replaceDescription(fromType.getRoot().getKind(), toType.getRoot().getKind()))));
        return typeChangeInfo;
    }

    public static boolean prettyEqual(TypeGraph t1, TypeGraph t2) {
        return pretty(t1).equals(pretty(t2));
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> realTypeChange(TypFct from, TypFct to, GlobalContext gc) {
        TypeGraph fromType = from.getType();
        TypeGraph toType = to.getType();
        if (from.isResolved() && to.isResolved())
            return realTypeChange(fromType, toType, gc);
        return getReplacement(fromType, toType);

    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> realTypeChange(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {

        switch (fromType.getRoot().getKind()) {
            case Primitive:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzePrimitiveToPrimitive(fromType, toType);
                    case Array: return analyzePrimitiveToArray(fromType, toType, gc);
                    case Simple: return analyzePrimitiveToSimple(fromType, toType);
                    case Parameterized: return analyzePrimitiveToParameterized(fromType, toType);
                    case WildCard: return analyzePrimitiveToWildCard(fromType, toType, gc);
                    default: return getReplacement(fromType, toType);
                }
            case Simple:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeSimpleToPrimitive(fromType, toType);
                    case Array: return analyzeSimpleToArray(fromType, toType, gc);
                    case Simple: return analyzeSimpleToSimple(fromType, toType, gc);
                    case Parameterized: return analyzeSimpleToParameterized(fromType, toType, gc);
                    case WildCard: return analyzeSimpleToWildCard(fromType, toType, gc);
                    default: return getReplacement(fromType, toType);
                }
            case Parameterized:
                switch (toType.getRoot().getKind()) {
                    case Array: return analyzeParameterizedToArray(fromType, toType, gc);
                    case Simple: return analyzeParameterizedToSimple(fromType, toType, gc);
                    case Parameterized: return analyzeParameterizedToParameterized(fromType, toType, gc);
                    case WildCard: return analyzeParameterizedToWildCard(fromType, toType, gc);
                    case Primitive: return analyzeParameterizedToPrimitive(fromType, toType);
                    default: return getReplacement(fromType, toType);
                }
            case Array: {
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeArrayToPrimitive(fromType, toType, gc);
                    case Array: return analyzeArrayToArray(fromType, toType, gc);
                    case Simple: return analyzeArrayToSimple(fromType, toType, gc);
                    case Parameterized: return analyzeArrayToParameterized(fromType, toType, gc);
                    case WildCard: return analyzeArrayToWildCard(fromType, toType, gc);
                    default: return getReplacement(fromType, toType);
                }
            }
            case WildCard:
                switch (toType.getRoot().getKind()) {
                    case Primitive: return analyzeWildCardToPrimitive(fromType, toType, gc);
                    case Array:return analyzeWildCardToArray(fromType, toType, gc);
                    case Simple: return analyzeWildCardToSimple(fromType, toType, gc);
                    case Parameterized: return analyzeWildCardToParameterized(fromType, toType, gc);
                    case WildCard: return analyzeWildCardToWildCard(fromType, toType, gc);
                    default: return getReplacement(fromType, toType);
                }
            default: return getReplacement(fromType, toType);

        }
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzePrimitiveToPrimitive(TypeGraph fromType, TypeGraph toType) {

        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Primitive, Primitive));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (isWidening(fromType, toType))
            replacementInfo.add("Primitive Widening");
        if (isNarrowing(fromType, toType))
            replacementInfo.add("Primitive Narrowing");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzePrimitiveToParameterized(TypeGraph fromType, TypeGraph toType) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Primitive, Parameterized));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (toType.getEdgesMap().values().stream().anyMatch(t -> isBoxing(fromType, t)))
            replacementInfo.add("Box");
        if (anyEdgeMatch(toType, x -> isEnum(x)))
            replacementInfo.add("Introduce Enum");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzePrimitiveToSimple(TypeGraph fromType, TypeGraph toType) {

        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Primitive, Simple));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (isBoxing(fromType, toType))
            replacementInfo.add("Box");
        if (isTypeVariable(toType))
            replacementInfo.add("Introduce Type Variable");
        if (isEnum(toType))
            replacementInfo.add("Introduce Enum");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzePrimitiveToArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Primitive, Array));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (pretty(toType.getEdgesMap().get("of")).equals(pretty(fromType)))
            replacementInfo.add("Convert To Array");
        else {
            typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("of"), gc));
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzePrimitiveToWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Primitive, WildCard));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (toType.getEdgesMap().get("of") != null)
            typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("of"), gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeSimpleToPrimitive(TypeGraph fromType, TypeGraph toType) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Simple, Primitive));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (isUnBoxing(fromType, toType))
            replacementInfo.add("Unbox");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeSimpleToSimple(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Simple, Simple));
        if (isTypeVariable(fromType) && isTypeVariable(toType)) {
            replacementInfo.add("Change Type Variable");
        } else if (isTypeVariable(fromType) && !isTypeVariable(toType)) {
            replacementInfo.add(T_SUPER_R.toString());
            replacementInfo.add("Introduce Type Variable");
        } else if (!isTypeVariable(fromType) && isTypeVariable(toType)) {
            replacementInfo.add("Remove Type Variable");
            replacementInfo.add(T_SUPER_R.toString());
        } else {

            if (isEnum(toType))
                replacementInfo.add("Introduce Enum");

            replacementInfo.add(getHierarchyRelation(fromType, toType, gc).toString());
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeSimpleToArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Simple, Array));
        if (pretty(toType.getEdgesMap().get("of")).equals(pretty(fromType)))
            replacementInfo.add("Convert To Array");
        else
            typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("of"), gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeSimpleToWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return toWildCard(fromType, toType, gc);
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeSimpleToParameterized(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Simple, Parameterized));
        if (prettyEqual(toType.getEdgesMap().get("of"), fromType))
            replacementInfo.add("Add Type Parameters");
        else if (toType.getEdgesMap().values().stream().anyMatch(t -> prettyEqual(t, fromType)))
            replacementInfo.add("Wrap With Parameterized Type");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeParameterizedToSimple(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Parameterized, Simple));
        if (prettyEqual(fromType.getEdgesMap().get("of"), toType))
            replacementInfo.add("Remove Type Parameters");
        else if (fromType.getEdgesMap().values().stream().anyMatch(t -> prettyEqual(t, toType)))
            replacementInfo.add("UnWrap Parameterized Type");
        if (isEnum(toType))
            replacementInfo.add("Introduce Enum");
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeParameterizedToParameterized(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        if (prettyEqual(fromType.getEdgesMap().get("of"), toType.getEdgesMap().get("of"))) {
            replacementInfo.add("Update Type Parameters");
            List<TypeGraph> b4Params = fromType.getEdgesMap().entrySet().stream()
                    .filter(x -> x.getKey().startsWith("Param:")).map(x -> x.getValue()).collect(toList());
            List<TypeGraph> afterParams = toType.getEdgesMap().entrySet().stream()
                    .filter(x -> x.getKey().startsWith("Param:")).map(x -> x.getValue()).collect(toList());
            if (b4Params.size() == afterParams.size()) {
                for (int i = 0; i < b4Params.size(); i++)
                    if (!prettyEqual(b4Params.get(i), afterParams.get(i)))
                        typeChangeInfo.addAll(realTypeChange(b4Params.get(i), afterParams.get(i), gc));
            }
        } else {
            replacementInfo.add("Update Parameterized Type");
            typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("of"), toType.getEdgesMap().get("of"), gc));
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeParameterizedToArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<String> replacementInfo = new ArrayList<>();
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Parameterized, Array));
        if (pretty(toType.getEdgesMap().get("of")).equals(pretty(fromType)))
            replacementInfo.add("Convert To Array");
        else
            typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("of"), gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeParameterizedToWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return toWildCard(fromType, toType, gc);
    }

    private static List<Tuple3<TypeGraph, TypeGraph, List<String>>> toWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(fromType.getRoot().getKind(), WildCard));
        if (toType.getEdgesCount() == 0) {
            replacementInfo.add(R_SUPER_T.toString());
        } else if (toType.getEdgesMap().containsKey("extends")) {
            replacementInfo.add("Add upper bound");
            if (prettyEqual(fromType, toType.getEdgesMap().get("extends"))) {
                replacementInfo.add("Convert to WildCard");
                replacementInfo.add(R_SUPER_T.toString());
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("extends"), gc));
            }
        } else if (toType.getEdgesMap().containsKey("super")) {
            replacementInfo.add("Add lower bound");
            if (prettyEqual(fromType, toType.getEdgesMap().get("super"))) {
                replacementInfo.add("Convert to WildCard");
                replacementInfo.add(T_SUPER_R.toString());
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("super"), gc));
            }
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeParameterizedToPrimitive(TypeGraph fromType, TypeGraph toType) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Parameterized, Primitive));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeArrayToPrimitive(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Array, Primitive));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (pretty(fromType.getEdgesMap().get("of")).equals(pretty(fromType)))
            replacementInfo.add("Convert From Array");
        else
            typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("of"), toType, gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeArrayToArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Array, Array));
        typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("of"), toType.getEdgesMap().get("of"), gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeArrayToSimple(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return fromArray(fromType, toType, gc);
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> fromArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Array, toType.getRoot().getKind()));
        if (pretty(fromType.getEdgesMap().get("of")).equals(pretty(toType)))
            replacementInfo.add("Convert From Array");
        else
            typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("of"), toType, gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    public static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeArrayToParameterized(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return fromArray(fromType, toType, gc);
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeArrayToWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(Array, WildCard));
        if (toType.getEdgesMap().containsKey("extends")) {
            replacementInfo.add("Add upper bound");
            if (prettyEqual(fromType, toType.getEdgesMap().get("extends"))) {
                replacementInfo.add("Convert to WildCard");
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("extends"), gc));
            }
        } else if (toType.getEdgesMap().containsKey("super")) {
            replacementInfo.add("Add lower bound");
            if (prettyEqual(fromType, toType.getEdgesMap().get("super"))) {
                replacementInfo.add("Convert to WildCard");
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType, toType.getEdgesMap().get("super"), gc));
            }
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeWildCardToPrimitive(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(WildCard, Primitive));
        replacementInfo.add(NO_HIERARCHY_SUPPORT.toString());
        if (fromType.getEdgesMap().get("of") != null)
            typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("of"), toType, gc));
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeWildCardToSimple(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return fromWildCard(fromType, toType, gc, Simple);
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeWildCardToParameterized(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return fromWildCard(fromType, toType, gc, Parameterized);
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeWildCardToArray(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        return fromWildCard(fromType, toType, gc, Array);
    }

    static List<Tuple3<TypeGraph, TypeGraph, List<String>>> analyzeWildCardToWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc) {
        List<String> replacementInfo = new ArrayList<>();
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(WildCard, WildCard));
        if (fromType.getEdgesMap().containsKey("extends")) {
            if (toType.getEdgesMap().containsKey("extends")) {
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("extends"), toType.getEdgesMap().get("extends"), gc));
            } else if (toType.getEdgesMap().containsKey("super")) {
                replacementInfo.add("Change upper bound to lower bound");
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("extends"), toType.getEdgesMap().get("super"), gc));
            } else {
                replacementInfo.add("Remove upper bound");
                replacementInfo.add(R_SUPER_T.toString());
            }
        } else if (fromType.getEdgesMap().containsKey("super")) {
            if (toType.getEdgesMap().containsKey("extends")) {
                replacementInfo.add("Change lower bound to upper bound");
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("super"), toType.getEdgesMap().get("extends"), gc));
            } else if (toType.getEdgesMap().containsKey("super")) {
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("super"), toType.getEdgesMap().get("super"), gc));
            } else {
                replacementInfo.add("Remove lower bound");
                replacementInfo.add(R_SUPER_T.toString());
            }
        } else if (toType.getEdgesMap().containsKey("extends")) {
            replacementInfo.add("Introduce upper bound");
            replacementInfo.add(T_SUPER_R.toString());
        } else if (toType.getEdgesMap().containsKey("super")) {
            replacementInfo.add("Remove lower bound");
            replacementInfo.add(T_SUPER_R.toString());
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }

    private static List<Tuple3<TypeGraph, TypeGraph, List<String>>> fromWildCard(TypeGraph fromType, TypeGraph toType, GlobalContext gc, TypeKind typeKind) {
        List<Tuple3<TypeGraph, TypeGraph, List<String>>> typeChangeInfo = new ArrayList<>();
        List<String> replacementInfo = new ArrayList<>();
        replacementInfo.add(replaceDescription(WildCard, typeKind));
        if (fromType.getEdgesCount() == 0) {
            replacementInfo.add(T_SUPER_R.toString());
        } else if (fromType.getEdgesMap().containsKey("extends")) {
            if (prettyEqual(fromType.getEdgesMap().get("extends"), toType)) {
                replacementInfo.add("Convert from WildCard");
                replacementInfo.add(T_SUPER_R.toString());
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("extends"), toType, gc));
            }
        } else if (fromType.getEdgesMap().containsKey("super")) {
            if (prettyEqual(fromType.getEdgesMap().get("super"), toType)) {
                replacementInfo.add("Convert from WildCard");
                replacementInfo.add(R_SUPER_T.toString());
            } else {
                typeChangeInfo.addAll(realTypeChange(fromType.getEdgesMap().get("super"), toType, gc));
            }
        }
        typeChangeInfo.add(0, Tuple.of(fromType, toType, replacementInfo));
        return typeChangeInfo;
    }


    private static boolean isTypeVariable(TypeGraph fromType) {
        return fromType.getRoot().getNamespacee().equals(TypeVariable);
    }


    static boolean isAutoBoxing(TypeGraph b4, TypeGraph after) {
        return isBoxing(b4, after) || isUnBoxing(b4, after);
    }

    static boolean anyEdgeMatch(TypeGraph tg, Predicate<TypeGraph> predicate) {
        return tg.getEdgesCount() > 0 && tg.getEdgesMap().values().stream().anyMatch(predicate);
    }

    static boolean isEnum(TypeGraph t) {
        return t.getRoot().getKind().equals(Simple) && t.getRoot().getTypeSem().equals(TypeSem.Enum);
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

    ;

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

    ;
}
