package gr.uom.java.xmi.TypeFactMiner;

import gr.uom.java.xmi.TypeFactMiner.Models.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;

import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.*;
import static gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode.TypeKind.Simple;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.pretty;
import static java.util.stream.Collectors.toList;

public class HierarchyUtil {

    public static HierarchyRelation getHierarchyRelation(TypeGraphOuterClass.TypeGraph tg1, TypeGraphOuterClass.TypeGraph tg2, GlobalContext gc){
        if(tg1.getRoot().getKind().equals(Simple) && tg2.getRoot().getKind().equals(Simple)){
            Map<String, List<String>> classHierarchyRelation = gc.getInternalClassHierarchy();
            Map<String, List<String>> classHierarchyJdk = gc.getSuperTypes();
            if(pretty(tg1).contains("CharSequence"))
                System.out.println();
            return getHierarchyRelation(pretty(tg1), pretty(tg2),classHierarchyRelation, classHierarchyJdk);
        }
        return NO_RELATION;
    }

    public static HierarchyRelation getHierarchyRelation(String from, String to, Map<String, List<String>> internalHieararchyTree, Map<String, List<String>> classHierarchyJdk) {
        // MyList < List
        // MyLinkedList < MyList
        // MyArrayList < MyList
        // ArrayList < List
        // LinkedList < List

        List<String> allSuperTypes_From_Internal = dfsHierarchyTree(from, internalHieararchyTree);
        // MyLinkedList -> MyList
        if(allSuperTypes_From_Internal.contains(to))
            return R_SUPER_T;
        // ArrayList -> List
        List<String> allSuperTypes_From_Jdk = dfsHierarchyTree(from, classHierarchyJdk);
        if(allSuperTypes_From_Internal.contains(to))
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
