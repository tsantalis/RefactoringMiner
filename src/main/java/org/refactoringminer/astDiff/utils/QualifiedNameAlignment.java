package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.tree.Tree;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.wrappers.JavaToKotlinMigration;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DeferredFlattenings;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.util.PathFileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//the Java qualified names are nested during matching, as the model refers to their prefixes (a.b in a.b.c), and flat in the diff,
//so that a name inserted in the middle is a single inserted name, i.e., com.zaxxer.hikari.util.IConcurrentBagEntry -> com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry
public class QualifiedNameAlignment {

    //aligns the names of the mapped Java qualified names in label order, and registers the flattening of the nested qualified names, applied after all diffs are matched
    public static void align(ASTDiff diff, DeferredFlattenings deferredFlattenings) {
        if(isCOrCppFile(diff.getSrcPath()) && isCOrCppFile(diff.getDstPath())) {
            alignCppQualifiedNames(diff);
            return;
        }
        if(!PathFileUtils.isJavaFile(diff.getSrcPath()))
            return;
        //the Java qualified names migrated to Kotlin are mapped with the Kotlin navigation expressions of their names
        if(Constants.isCrossLanguage(diff.LANG1, diff.LANG2)) {
            JavaToKotlinMigration.alignQualifiedNames(diff, deferredFlattenings);
            return;
        }
        if(!PathFileUtils.isJavaFile(diff.getDstPath()))
            return;
        ExtendedMultiMappingStore mappingStore = diff.getAllMappings();
        //the names of unrelated qualified names are not counterparts, as when the identical prefixes of a deleted and an added import are mapped in their nested form,
        //i.e., uk.co.real_logic.aeron.command in two imports of different classes
        for(Tree qualified1 : diff.src.getRoot().preOrder()) {
            if(!isOutermostQualifiedName(qualified1, diff.LANG1))
                continue;
            Set<Tree> qualifiedDsts = mappingStore.isSrcMapped(qualified1) ? mappingStore.getDsts(qualified1) : Set.of();
            for(Tree name1 : names(qualified1, diff.LANG1)) {
                Set<Tree> dsts = mappingStore.getDsts(name1);
                if(dsts == null)
                    continue;
                for(Tree dst : new ArrayList<>(dsts)) {
                    Tree qualified2 = outermostQualifiedName(dst, diff.LANG2);
                    if(qualified2 != null && !qualifiedDsts.contains(qualified2))
                        mappingStore.removeMapping(name1, dst);
                }
            }
        }
        for(Tree qualified1 : diff.src.getRoot().preOrder()) {
            if(!isOutermostQualifiedName(qualified1, diff.LANG1) || !mappingStore.isSrcMapped(qualified1))
                continue;
            for(Tree qualified2 : new ArrayList<>(mappingStore.getDsts(qualified1))) {
                if(isOutermostQualifiedName(qualified2, diff.LANG2))
                    alignNames(mappingStore, qualified1, qualified2, diff.LANG1, diff.LANG2);
                else if(qualified2.getType().name.equals(diff.LANG2.SIMPLE_NAME))
                    alignNameWithQualifiedName(mappingStore, qualified1, qualified2, names(qualified1, diff.LANG1), true);
            }
        }
        for(Tree qualified2 : diff.dst.getRoot().preOrder()) {
            if(!isOutermostQualifiedName(qualified2, diff.LANG2) || !mappingStore.isDstMapped(qualified2))
                continue;
            for(Tree name1 : new ArrayList<>(mappingStore.getSrcs(qualified2))) {
                if(name1.getType().name.equals(diff.LANG1.SIMPLE_NAME))
                    alignNameWithQualifiedName(mappingStore, qualified2, name1, names(qualified2, diff.LANG2), false);
            }
        }
        //the nested qualified names are flattened innermost first, i.e., QualifiedName -> [QualifiedName -> [a, b], c] becomes QualifiedName -> [a, b, c]
        for(Tree qualified1 : diff.src.getRoot().postOrder()) {
            if(isNestedQualifiedName(qualified1, diff.LANG1))
                deferredFlattenings.flattenRemovingAllMappings(qualified1.getParent(), qualified1);
        }
        for(Tree qualified2 : diff.dst.getRoot().postOrder()) {
            if(isNestedQualifiedName(qualified2, diff.LANG2))
                deferredFlattenings.flattenTargetRemovingAllMappings(qualified2.getParent(), qualified2);
        }
    }

    //a qualified name mapped with a simple name is decomposed, so that the simple name is mapped with the name of the same label, or else the last name,
    //and the other names are deleted (or added), i.e., Constants.BAR -> BAR with an imported field, or package com.mongodb -> package util
    private static void alignNameWithQualifiedName(ExtendedMultiMappingStore mappingStore, Tree qualified, Tree simpleName, List<Tree> names, boolean qualifiedIsSrc) {
        Tree counterpart = names.get(names.size() - 1);
        for(Tree name : names) {
            if(name.getLabel().equals(simpleName.getLabel()))
                counterpart = name;
        }
        boolean free = qualifiedIsSrc ? !mappingStore.isSrcMapped(counterpart) && mappingStore.getSrcs(simpleName).size() == 1 :
                !mappingStore.isDstMapped(counterpart) && mappingStore.getDsts(simpleName).size() == 1;
        if(!free)
            return;
        if(qualifiedIsSrc) {
            mappingStore.removeMapping(qualified, simpleName);
            mappingStore.addMapping(counterpart, simpleName);
        }
        else {
            mappingStore.removeMapping(simpleName, qualified);
            mappingStore.addMapping(simpleName, counterpart);
        }
    }

    //aligns the names of the two qualified names in label order, removing only the mappings within them that disagree with the alignment,
    //so that the names mapped by the matchers to many qualified names, i.e., System in duplicated System.err calls, keep their mappings,
    //while the names mapped outside qualified names are left as they are
    private static void alignNames(ExtendedMultiMappingStore mappingStore, Tree qualified1, Tree qualified2, Constants LANG1, Constants LANG2) {
        List<Tree> names1 = names(qualified1, LANG1);
        List<Tree> names2 = names(qualified2, LANG2);
        List<Pair<Tree, Tree>> aligned = namesInLabelOrder(names1, names2);
        //the mappings between the names and the nested qualified names, i.e., com -> com.zaxxer, when the matchers pair the nested levels by position
        List<Tree> descendants2 = qualified2.getDescendants();
        for(Tree descendant1 : qualified1.getDescendants()) {
            Set<Tree> dsts = mappingStore.getDsts(descendant1);
            if(dsts != null) {
                for(Tree dst : new ArrayList<>(dsts)) {
                    if(descendants2.contains(dst) && (names1.contains(descendant1) || names2.contains(dst)) && !aligned.contains(Pair.of(descendant1, dst)))
                        mappingStore.removeMapping(descendant1, dst);
                }
            }
        }
        //the names follow the mapping of their qualified names, which may map many qualified names to one, i.e., grouped static imports a.B.x and a.B.y -> a.B.*
        for(Pair<Tree, Tree> pair : aligned) {
            Tree name1 = pair.getLeft();
            Tree name2 = pair.getRight();
            Set<Tree> dsts = mappingStore.getDsts(name1);
            Set<Tree> srcs = mappingStore.getSrcs(name2);
            boolean mapped1 = dsts != null && dsts.stream().anyMatch(dst -> outermostQualifiedName(dst, LANG2) == null);
            boolean mapped2 = srcs != null && srcs.stream().anyMatch(src -> outermostQualifiedName(src, LANG1) == null);
            if(!mapped1 && !mapped2 && (dsts == null || !dsts.contains(name2)))
                mappingStore.addMapping(name1, name2);
        }
    }

    private static boolean isCOrCppFile(String path) {
        return PathFileUtils.isCppFile(path) || PathFileUtils.isCFile(path);
    }

    //aligns the names of the mapped C++ qualified identifiers in label order, i.e., std::string(str) -> internal::StringType<T>(str) maps std to internal and string to StringType,
    //where the name of a template is the name before its template arguments
    private static void alignCppQualifiedNames(ASTDiff diff) {
        ExtendedMultiMappingStore mappingStore = diff.getAllMappings();
        for(Tree qualified1 : diff.src.getRoot().preOrder()) {
            if(!isOutermostQualifiedIdentifier(qualified1, diff.LANG1) || !mappingStore.isSrcMapped(qualified1))
                continue;
            for(Tree qualified2 : new ArrayList<>(mappingStore.getDsts(qualified1))) {
                if(isOutermostQualifiedIdentifier(qualified2, diff.LANG2))
                    mapNamesInLabelOrder(mappingStore, cppNames(qualified1, diff.LANG1), cppNames(qualified2, diff.LANG2));
            }
        }
    }

    private static boolean isOutermostQualifiedIdentifier(Tree tree, Constants LANG) {
        return tree.getType().name.equals(LANG.QUALIFIED_IDENTIFIER) && (tree.getParent() == null || !tree.getParent().getType().name.equals(LANG.QUALIFIED_IDENTIFIER));
    }

    //the names of a C++ qualified identifier, i.e., [std, string] for std::string and [internal, StringType] for internal::StringType<T>
    private static List<Tree> cppNames(Tree qualified, Constants LANG) {
        List<Tree> names = new ArrayList<>();
        for(Tree child : qualified.getChildren()) {
            String type = child.getType().name;
            if(type.equals(LANG.QUALIFIED_IDENTIFIER))
                names.addAll(cppNames(child, LANG));
            else if(type.equals(LANG.TEMPLATE_FUNCTION) || type.equals(LANG.TEMPLATE_TYPE)) {
                if(!child.getChildren().isEmpty() && child.getChild(0).isLeaf())
                    names.add(child.getChild(0));
            }
            else if(child.isLeaf() && !child.getLabel().isEmpty())
                names.add(child);
        }
        return names;
    }

    //maps the names with identical labels in the longest common subsequence, and the names between them in equally long gaps by position,
    //i.e., the Java and Kotlin names of a qualified name, an import, or a package
    public static void mapNamesInLabelOrder(ExtendedMultiMappingStore mappingStore, List<Tree> names1, List<Tree> names2) {
        for(Pair<Tree, Tree> pair : namesInLabelOrder(names1, names2)) {
            mapName(mappingStore, pair.getLeft(), pair.getRight());
        }
    }

    //the names with identical labels in the longest common subsequence, and the names between them in equally long gaps by position, i.e., a renamed package
    private static List<Pair<Tree, Tree>> namesInLabelOrder(List<Tree> names1, List<Tree> names2) {
        List<Pair<Tree, Tree>> aligned = new ArrayList<>();
        int[][] lcs = new int[names1.size() + 1][names2.size() + 1];
        for(int i = names1.size() - 1; i >= 0; i--) {
            for(int j = names2.size() - 1; j >= 0; j--) {
                lcs[i][j] = names1.get(i).getLabel().equals(names2.get(j).getLabel()) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        int i = 0, j = 0, gapStart1 = 0, gapStart2 = 0;
        while(i < names1.size() && j < names2.size()) {
            if(names1.get(i).getLabel().equals(names2.get(j).getLabel())) {
                addGap(aligned, names1.subList(gapStart1, i), names2.subList(gapStart2, j));
                aligned.add(Pair.of(names1.get(i), names2.get(j)));
                i++;
                j++;
                gapStart1 = i;
                gapStart2 = j;
            }
            else if(lcs[i + 1][j] >= lcs[i][j + 1]) {
                i++;
            }
            else {
                j++;
            }
        }
        addGap(aligned, names1.subList(gapStart1, names1.size()), names2.subList(gapStart2, names2.size()));
        return aligned;
    }

    //the names between two identical names are counterparts, if they are equally many, i.e., a.b.c -> a.x.c
    private static void addGap(List<Pair<Tree, Tree>> aligned, List<Tree> gap1, List<Tree> gap2) {
        if(gap1.size() != gap2.size())
            return;
        for(int k = 0; k < gap1.size(); k++) {
            aligned.add(Pair.of(gap1.get(k), gap2.get(k)));
        }
    }

    //the names mapped to each other are kept, and the names mapped elsewhere keep their mappings
    private static void mapName(ExtendedMultiMappingStore mappingStore, Tree name1, Tree name2) {
        if(!mappingStore.isSrcMapped(name1) && !mappingStore.isDstMapped(name2))
            mappingStore.addMapping(name1, name2);
    }

    private static List<Tree> names(Tree qualified, Constants LANG) {
        List<Tree> names = new ArrayList<>();
        for(Tree child : qualified.getChildren()) {
            if(child.getType().name.equals(LANG.QUALIFIED_NAME))
                names.addAll(names(child, LANG));
            else
                names.add(child);
        }
        return names;
    }

    //the outermost qualified name of a name being part of it, or null
    private static Tree outermostQualifiedName(Tree name, Constants LANG) {
        Tree qualified = null;
        for(Tree parent = name.getParent(); parent != null && parent.getType().name.equals(LANG.QUALIFIED_NAME); parent = parent.getParent())
            qualified = parent;
        return qualified;
    }

    private static boolean isOutermostQualifiedName(Tree tree, Constants LANG) {
        return tree.getType().name.equals(LANG.QUALIFIED_NAME) && (tree.getParent() == null || !tree.getParent().getType().name.equals(LANG.QUALIFIED_NAME));
    }

    private static boolean isNestedQualifiedName(Tree tree, Constants LANG) {
        return tree.getType().name.equals(LANG.QUALIFIED_NAME) && tree.getParent() != null && tree.getParent().getType().name.equals(LANG.QUALIFIED_NAME);
    }
}
