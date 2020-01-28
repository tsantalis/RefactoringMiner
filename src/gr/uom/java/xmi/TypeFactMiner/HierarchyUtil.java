package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass;
import com.t2r.common.models.refactorings.CommitInfoOuterClass.CommitInfo.JarInfo;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static gr.uom.java.xmi.TypeFactMiner.TypFct.getExternalHierarchyMap;
import static com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind.Simple;
import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.*;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class HierarchyUtil {

    public static HierarchyRelation getHierarchyRelation(TypeGraphOuterClass.TypeGraph tg1, TypeGraphOuterClass.TypeGraph tg2, GlobalContext gc, Set<JarInfo> jars , Path pathToJars){
        if(tg1.getRoot().getKind().equals(Simple) && tg2.getRoot().getKind().equals(Simple)){
            Map<String, List<String>> classHierarchyRelation = gc.getInternalClassHierarchy();
            Map<String, List<String>> classHierarchyJdk = gc.getSuperTypes().collect(toMap(x->x._1(), x->x._2(), (a,b)->a));
            if(pretty(tg1).contains("CharSequence"))
                System.out.println();
            return getHierarchyRelation(pretty(tg1), pretty(tg2),classHierarchyRelation, classHierarchyJdk, jars, pathToJars);
        }
        return NO_RELATION;
    }

    public static HierarchyRelation getHierarchyRelation(String from, String to, Map<String, List<String>> internalHieararchyTree, Map<String, List<String>> classHierarchyJdk, Set<JarInfo> jars , Path pathToJars) {
        // MyList < List
        // MyLinkedList < MyList
        // MyArrayList < MyList
        // ArrayList < List
        // LinkedList < List

        if(from.equals(to))
            return NO_RELATION;

        List<String> allSuperTypes_From_Internal = dfsHierarchyTree(from, internalHieararchyTree);
        // MyLinkedList -> MyList
        if(allSuperTypes_From_Internal.contains(to))
            return R_SUPER_T;
        // ArrayList -> List
        List<String> allSuperTypes_From_Jdk = dfsHierarchyTree(from, classHierarchyJdk);
        if(allSuperTypes_From_Jdk.contains(to))
            return R_SUPER_T;

        List<String> allSuperTypes_To_Internal = dfsHierarchyTree(to, internalHieararchyTree);
        // MyList -> MyLinkedList
        if(allSuperTypes_To_Internal.contains(from))
            return T_SUPER_R;

        List<String> allSuperTypes_To_Jdk = dfsHierarchyTree(to, classHierarchyJdk);
        // List -> ArrayList
        if(allSuperTypes_To_Jdk.contains(from))
            return T_SUPER_R;

        // LinkedList -> ArrayList
        // MyLinkedList -> MyArrayList
        if (allSuperTypes_From_Jdk.stream().anyMatch(x -> allSuperTypes_To_Jdk.stream().anyMatch(z -> z.equals(x)))
                || allSuperTypes_From_Internal.stream().anyMatch(x -> allSuperTypes_To_Internal.stream().anyMatch(z -> z.equals(x))))
            return SIBLING;

        // LinkedList -> MyLinkedList
        // MyArrayList -> ArrayList
        if (allSuperTypes_From_Jdk.stream().anyMatch(x -> allSuperTypes_To_Internal.stream().anyMatch(z -> z.equals(x)))
                || allSuperTypes_From_Internal.stream().anyMatch(x -> allSuperTypes_To_Jdk.stream().anyMatch(z -> z.equals(x))))
            return SIBLING;

        Map<String, List<String>> externalHierarchyTree = jars.stream().map(x -> getExternalHierarchyMap(pathToJars, x))
                .flatMap(x -> x.entrySet().stream())
                .collect(toMap(x -> x.getKey(), x -> x.getValue(), (a, b) -> a));



        List<String> allSuperTypes_From_External = dfsHierarchyTree(from, externalHierarchyTree);
        // MyLinkedList -> MyList
        if(allSuperTypes_From_External.contains(to))
            return R_SUPER_T;

        List<String> allSuperTypes_To_External = dfsHierarchyTree(to, externalHierarchyTree);
        // MyList -> MyLinkedList
        if(allSuperTypes_To_External.contains(from))
            return T_SUPER_R;


        if (allSuperTypes_From_Jdk.stream().anyMatch(x -> allSuperTypes_To_Jdk.stream().anyMatch(z -> z.equals(x)))
                || allSuperTypes_From_External.stream().anyMatch(x -> allSuperTypes_To_External.stream().anyMatch(z -> z.equals(x))))
            return SIBLING;

        // LinkedList -> MyLinkedList
        // MyArrayList -> ArrayList
        if (allSuperTypes_From_Jdk.stream().anyMatch(x -> allSuperTypes_To_External.stream().anyMatch(z -> z.equals(x)))
                || allSuperTypes_From_External.stream().anyMatch(x -> allSuperTypes_To_Jdk.stream().anyMatch(z -> z.equals(x))))
            return SIBLING;





        return NO_RELATION;

    }

    public static List<String> dfsHierarchyTree(String node,Map<String, List<String>> hierarchyAdjList ){
        if(hierarchyAdjList.containsKey(node)){
            return hierarchyAdjList.get(node).stream()
                    .flatMap(x-> Stream.concat(Stream.of(x),dfsHierarchyTree(x, hierarchyAdjList).stream()))
                    .collect(toList());
        }
        return new ArrayList<>();
    }

    public enum HierarchyRelation{
        NO_RELATION,
        T_SUPER_R,
        R_SUPER_T,
        SIBLING,
        NO_HIERARCHY_SUPPORT
    }



}
