package gr.uom.java.xmi.TypeFactMiner;


import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass.TypeGraph;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass.TypeGraph.Builder;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode.TypeKind;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

public class TypeGraphUtil {

    public static TypeGraph getTypeGraph(Type t) {
        if(t.isQualifiedType())
            return getTypeGraph((QualifiedType) t);
        else if(t.isNameQualifiedType())
            return getTypeGraph((NameQualifiedType) t);
        else if(t.isSimpleType())
            return getTypeGraph((SimpleType) t);
        else if(t.isParameterizedType())
            return getTypeGraph((ParameterizedType) t);
        else if(t.isWildcardType())
            return getTypeGraph( (WildcardType) t);
        else if(t.isPrimitiveType())
            return getTypeGraph((PrimitiveType) t);
        else if(t.isArrayType())
            return getTypeGraph((ArrayType) t);
        else if(t.isIntersectionType())
            return getTypeGraph((IntersectionType) t);
        else if(t.isUnionType())
            return getTypeGraph((UnionType) t);
        else
            throw new RuntimeException("Could not figure out type");

    }

    public static String pretty(TypeGraph tg){
        if(tg.getRoot().getKind().equals(TypeKind.Simple) || tg.getRoot().getKind().equals(TypeKind.Primitive)){
            return tg.getRoot().getName() + String.join(" ", tg.getRoot().getAnnotationsList());
        }
        else if(tg.getRoot().getKind().equals(TypeKind.Parameterized)){
            return pretty(tg.getEdgesMap().get("of")) + "<"
                    + tg.getEdgesMap().entrySet().stream().filter(x->x.getKey().contains("Param"))
                    .map(x->pretty(x.getValue())).collect(joining(", ")) + ">";
        }
        else if(tg.getRoot().getKind().equals(TypeKind.Array)){
            return pretty(tg.getEdgesMap().get("of")) + "[]";
        }
        else if(tg.getRoot().getKind().equals(TypeKind.WildCard)){
            if(tg.getEdgesMap().containsKey("of"))
                return "?" + tg.getRoot().getName() + pretty(tg.getEdgesMap().get("of"));
            else return "?";
        }
        else if(tg.getRoot().getKind().equals(TypeKind.Union))
            return tg.getEdgesMap().entrySet().stream().filter(x->x.getKey().contains("Union") || x.getKey().contains("Intersection"))
                    .map(x->pretty(x.getValue()))
                    .collect(joining(" & "));
        return "";
    }

    private static TypeNode getTypeNode(String name, List<String> annotations, TypeKind tk){
        return TypeNode.newBuilder().setKind(tk).setName(name).addAllAnnotations(annotations).build();
    }

    private static TypeNode getTypeNode(TypeKind tk){
        return TypeNode.newBuilder().setKind(tk).build();
    }

    private static TypeNode getTypeNode(TypeKind tk, List<String> annotataions){
        return TypeNode.newBuilder().setKind(tk).addAllAnnotations(annotataions).build();
    }


    private static TypeGraph getTypeGraph(SimpleType st){
        final List<Annotation> ann = st.annotations();
        final List<String> annotation = ann.stream().map(a -> "@" + a.getTypeName().getFullyQualifiedName())
                    .collect(toList());
        return of(getTypeNode(st.getName().getFullyQualifiedName(), annotation, TypeKind.Simple));

    }

    private static TypeGraph getTypeGraph(ParameterizedType pt){
        final List<Type> ps = pt.typeArguments();
        final List<TypeGraph> params = ps.stream().map(TypeGraphUtil::getTypeGraph).collect(toList());
        final TypeNode root = getTypeNode(TypeKind.Parameterized);
        return of(root).toBuilder()
                .putEdges("of", getTypeGraph(pt.getType()))
                .putAllEdges(IntStream.range(0,params.size()).mapToObj(x->x)
                        .collect(toMap(x-> ("Param:"+x) , x-> params.get(x))))
                .build();
    }

    private static TypeGraph getTypeGraph(WildcardType wt) {
        final List<Annotation> ann = wt.annotations();
        final List<String> annotation = ann.stream().map(a -> "@" + a.getTypeName().getFullyQualifiedName())
                .collect(toList());
        final TypeNode root = getTypeNode(wt.isUpperBound() ? "super" : "extends", annotation, TypeKind.WildCard);
        Builder bldr = of(root).toBuilder();
        if(wt.getBound()!=null){
            bldr.putEdges("of", getTypeGraph(wt.getBound()));
        }
        return bldr.build();

    }

    private static TypeGraph getTypeGraph(PrimitiveType pt){
        final List<Annotation> ann = pt.annotations();
        final List<String> annotation = ann.stream().map(a -> "@" + a.getTypeName().getFullyQualifiedName())
                .collect(toList());
        return of(getTypeNode(pt.getPrimitiveTypeCode().toString(), annotation, TypeKind.Primitive));
    }

    private static TypeGraph getTypeGraph(ArrayType at){
        final List<Dimension> ds = at.dimensions();
        final List<String> annotation = new ArrayList<>();
        for(Dimension d : ds){
            List<Annotation> aa = d.annotations();
            for(Annotation a: aa){
                annotation.add("@" + a.getTypeName().getFullyQualifiedName());
            }
        }
        final TypeNode root = getTypeNode(TypeKind.Array, annotation);
        return of(root).toBuilder()
                .putEdges("of", getTypeGraph(at.getElementType())).build();
    }

    private static TypeGraph getTypeGraph(IntersectionType it){
        final List<Type> ts = it.types();
        final TypeNode root = getTypeNode(TypeKind.Intersection);
        return of(root).toBuilder()
                .putAllEdges(IntStream.range(0,ts.size()).mapToObj(x->x)
                        .collect(toMap(x-> ("Intersection:"+x) , x-> getTypeGraph(ts.get(x))))).build();
    }

    private static TypeGraph getTypeGraph(UnionType it){
        final List<Type> ts = it.types();
        final TypeNode root = getTypeNode(TypeKind.Union);
        return of(root).toBuilder()
                .putAllEdges(IntStream.range(0,ts.size()).mapToObj(x->x)
                        .collect(toMap(x-> ("Union:"+x) , x-> getTypeGraph(ts.get(x)))))
                .build();
    }

    private static TypeGraph getTypeGraph(QualifiedType q){
        final List<Annotation> ann = q.annotations();
        final List<String> annotation = ann.stream().map(a -> "@" + a.getTypeName().getFullyQualifiedName())
                .collect(toList());
        final String name = getTypeGraph(q.getQualifier()).getRoot().getName() +"." + q.getName().getIdentifier();
        return of(getTypeNode(name, annotation, TypeKind.Simple));
    }

    private static TypeGraph getTypeGraph(NameQualifiedType nq){
        final List<Annotation> ann = nq.annotations();
        final List<String> annotation = ann.stream().map(a -> "@" + a.getTypeName().getFullyQualifiedName())
                .collect(toList());
        final String name = nq.getQualifier().getFullyQualifiedName() + "." + nq.getName().getIdentifier();
        return of(getTypeNode(name, annotation, TypeKind.Simple));
    }

    private static TypeGraph of(TypeNode root){
        return TypeGraph.newBuilder().setRoot(root).build();
    }
}
