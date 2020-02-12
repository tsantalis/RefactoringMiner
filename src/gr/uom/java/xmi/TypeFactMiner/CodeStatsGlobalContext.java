package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.JarInfoOuterClass;
import com.t2r.common.models.refactorings.TheWorldOuterClass;
import com.t2r.common.utilities.Counter;
import io.vavr.Tuple2;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;

import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind.Primitive;
import static com.t2r.common.models.refactorings.NameSpaceOuterClass.NameSpace;
import static com.t2r.common.models.refactorings.NameSpaceOuterClass.NameSpace.*;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;


public class CodeStatsGlobalContext extends GlobalContext {

    public static <K,V> Map<K,V> lruCache(final int maxSize) {
        return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > maxSize;
            }
        };
    }

    private Counter<String> visibilityMap = new Counter<>();
    private Counter<String> typeKindMap = new Counter<>();

    public Counter<String> getElemKindMap() {
        return elemKindMap;
    }

    private Counter<String> elemKindMap = new Counter<>();
    private Counter<String> nameSpaceMap = new Counter<>();
    private Map<String, List<NameSpace>> typeGraphNameSpaceMap;
    private Map<TypeGraph, TypeGraph> resolvedTypes = new HashMap<>();
    private Map<NameSpace, Set<String>> groupedNameSpaces;

    public Counter<String> getVisibilityMap() {
        return visibilityMap;
    }

    public Counter<String> getTypeKindMap() {
        return typeKindMap;
    }

    public Counter<String> getNameSpaceMap() {
        return nameSpaceMap;
    }

//    public CodeStatsGlobalContext(GlobalContext csi, RevCommit c, Git g, Set<JarInfoOuterClass.JarInfo> jars
//            , BiFunction<Tuple2<String, String>, FileStatus, Information> fn) throws Exception {
//
//        super(csi,c,g,fn);
//
//        visibilityMap = ((CodeStatsGlobalContext)csi).getVisibilityMap();
//        nameSpaceMap = ((CodeStatsGlobalContext)csi).getNameSpaceMap();
//        elemKindMap = ((CodeStatsGlobalContext)csi).getElemKindMap();
//        typeKindMap = ((CodeStatsGlobalContext)csi).getTypeKindMap();
//
//
//        groupedNameSpaces = getNamespaceForImports(this);
//
//        classInformation.entrySet()
//                .stream().flatMap(x -> x.getValue().stream().map(i -> (CodeStatsInformation) i))
//                .filter(x -> x.b4After.equals(Before))
//                .forEach(csii -> {
//                    visibilityMap.unMerge(csii.getVisibilityMap());
//                    typeKindMap.unMerge(csii.getTypeKindMap());
//                    elemKindMap.unMerge(csii.getElemKindMap());
//                    nameSpaceMap.unMerge(csii.getNameSpaceMap());
//                });
//
//
//        classInformation.entrySet()
//                .stream().flatMap(x -> x.getValue().stream().map(i -> (CodeStatsInformation) i))
//                .filter(x->x.b4After.equals(After))
////                .parallel()
//                .forEach(csii -> {
//                    visibilityMap.merge(csii.getVisibilityMap());
//                    typeKindMap.merge(csii.getTypeKindMap());
//                    elemKindMap.merge(csii.getElemKindMap());
//                    for (var t : csii.getTypeFctsFound().items()) {
//                        List<NameSpace> ns = approximateNameSpace(t.getKey());
//                        ns.stream().forEach(n -> {
//                            nameSpaceMap.add(n.name(), t.getValue());
//                        });
//                    }
//                });
//
//    }

    public CodeStatsGlobalContext(Git g, RevCommit commit, GraphTraversalSource gr, Set<JarInfoOuterClass.JarInfo> jars, Path pathToDependencies
            , BiFunction<Tuple2<String,String>, FileStatus, Information> fn, Set<String> classes) throws Exception {

        super(g, commit, gr, jars, pathToDependencies, fn);
        groupedNameSpaces = getNamespaceForImports(this, classes);

        classInformation.entrySet()
                .stream().flatMap(x -> x.getValue().stream().map(i -> (CodeStatsInformation) i))
                .filter(x -> x.typeDecls.stream().anyMatch(classes::contains))
                .forEach(csi -> {
                    visibilityMap.merge(csi.getVisibilityMap());
                    typeKindMap.merge(csi.getTypeKindMap());
                    elemKindMap.merge(csi.getElemKindMap());
                    for (var t : csi.getTypeFctsFound().items()) {
                        List<NameSpace> ns = approximateNameSpace(t.getKey());
                        ns.stream().forEach(n -> {
                            nameSpaceMap.add(n.name(), t.getValue());
                        });
                    }
                });
    }


    public TheWorldOuterClass.TheWorld prettyPrintStats() {
        System.out.println(visibilityMap);
        System.out.println(elemKindMap);
        System.out.println(nameSpaceMap);
        System.out.println(typeKindMap);
        return TheWorldOuterClass.TheWorld.newBuilder()
                .setSha(baseCommit.getId().getName())
                .putAllVisibilityMap(visibilityMap.getCounts())
                .putAllElementKindMap(elemKindMap.getCounts())
                .putAllNameSpaceMap(nameSpaceMap.getCounts())
                .putAllTypeKindMap(typeKindMap.getCounts())
                .build();
    }


    private List<NameSpace> approximateNameSpace(TypFct tf) {
        if(resolvedTypes == null)
            resolvedTypes = lruCache(200);
        if (!tf.isResolved()) {
            if(resolvedTypes.containsKey(tf.getType())){
                return approximateNameSpaceFor(resolvedTypes.get(tf.getType()));
            }
            TypeGraph b4 = tf.getType();
            tf.qualify(this);
            resolvedTypes.put(b4, tf.getType());
        }
        return approximateNameSpaceFor(tf.getType());
    }


    private List<NameSpace> approximateNameSpaceFor(TypeGraph resolvedTypeGraph) {
        switch (resolvedTypeGraph.getRoot().getKind()) {
            case Primitive:
            case Simple:
                return approximateNameSpace(resolvedTypeGraph.getRoot());

            case WildCard: {
                return (resolvedTypeGraph.getEdgesMap().get("extends") != null)
                        ? approximateNameSpaceFor(resolvedTypeGraph.getEdgesMap().get("extends"))
                        : ((resolvedTypeGraph.getEdgesMap().get("super") != null)
                        ? approximateNameSpaceFor(resolvedTypeGraph.getEdgesMap().get("super"))
                        : asList(TypeVariable));
            }
            case Array:
            case Parameterized:
            case Intersection:
            case Union:
                return resolvedTypeGraph.getEdgesMap().entrySet().stream()
                        .flatMap(x -> approximateNameSpaceFor(x.getValue()).stream()).collect(toList());

            default:
                return asList(DontKnow);
        }
    }


    private List<NameSpace> approximateNameSpace(TypeNode tn) {
        if (typeGraphNameSpaceMap == null)
            typeGraphNameSpaceMap = lruCache(500);
        String ptn = pretty(tn);
        if (!ptn.isEmpty() && typeGraphNameSpaceMap.containsKey(ptn))
            return typeGraphNameSpaceMap.get(ptn);
        NameSpace srch = searchApproxNameSpace(tn, groupedNameSpaces);

        typeGraphNameSpaceMap.put(ptn, asList(srch));
        return asList(srch);
    }

    private NameSpace searchApproxNameSpace(TypeNode tn, Map<NameSpace, Set<String>> groupedNameSpaces) {
        if (tn.getIsTypeVariable())
            return (TypeVariable);
        else if (tn.getKind().equals(Primitive))
            return (Jdk);
        else if (groupedNameSpaces.containsKey(Internal) && groupedNameSpaces.get(Internal).stream().anyMatch(x -> tn.getName().contains(x)))
            return (Internal);
        else if(getAllInternalPackages().anyMatch(p -> tn.getName().contains(p)))
            return Internal;
        else if (tn.getName().startsWith("java.") || tn.getName().startsWith("javax."))
            return (Jdk);
        else if (groupedNameSpaces.containsKey(Jdk) && groupedNameSpaces.get(Jdk).stream().anyMatch(x -> tn.getName().contains(x)))
            return (Jdk);
        else if (groupedNameSpaces.containsKey(External) && groupedNameSpaces.get(External).stream().anyMatch(x -> tn.getName().contains(x)))
            return (External);
        return DontKnow;
    }


    public Map<NameSpace, Set<String>> getNamespaceForImports(GlobalContext gc, Set<String> classes) {
        return getAllImports(classes).values().stream().flatMap(Collection::stream).distinct()
                .collect(groupingBy(x -> getNameSpaceForImportStmt(x, gc), toSet()));
    }

    private NameSpace getNameSpaceForImportStmt(String importStmt, GlobalContext gc) {
        return gc.getAllInternalPackages()
                .filter(importStmt::contains).findFirst().map(x -> Internal)
                .or(() -> gc.qNameContainsJavaPackage(importStmt).map(x -> NameSpace.Jdk))
                .orElse(NameSpace.External);
    }


}
