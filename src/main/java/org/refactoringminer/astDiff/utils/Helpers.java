package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLModuleDiff;
import gr.uom.java.xmi.diff.UMLPackageInfoDiff;

import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-05-22*/
public class Helpers {
    //the qualifiers of a C++ type are siblings of the type in the AST and part of the type in the model, i.e., const in static const int x and const struct S * s
    public static void matchTypeQualifiers(Tree srcType, Tree dstType, String srcTypeText, String dstTypeText, Constants LANG1, Constants LANG2, ExtendedMultiMappingStore mappingStore) {
        if (srcType.getParent() == null || dstType.getParent() == null)
            return;
        List<Tree> qualifiers2 = TreeUtilFunctions.findChildrenByType(dstType.getParent(), LANG2.TYPE_QUALIFIER);
        for (Tree qualifier1 : TreeUtilFunctions.findChildrenByType(srcType.getParent(), LANG1.TYPE_QUALIFIER)) {
            if (qualifier1.getChildren().size() != 1 || !srcTypeText.contains(qualifier1.getChild(0).getLabel()))
                continue;
            for (Tree qualifier2 : qualifiers2) {
                if (!mappingStore.isDstMapped(qualifier2) && qualifier1.isIsoStructuralTo(qualifier2) && dstTypeText.contains(qualifier2.getChild(0).getLabel())) {
                    mappingStore.addMappingRecursively(qualifier1, qualifier2);
                    break;
                }
            }
        }
    }
    public static String findNameByTree(Map<String, TreeContext> contextMap, Tree t) {
        if (contextMap == null) return null;
        for (Map.Entry<String, TreeContext> stringTreeContextEntry : contextMap.entrySet()) {
            if (stringTreeContextEntry.getValue().getRoot().equals(TreeUtilFunctions.getFinalRoot(t)))
            {
                return stringTreeContextEntry.getKey();
            }
        }
        return null;
    }
    public static Pair<TreeContext, TreeContext> findTreeContexts(UMLModelDiff modelDiff, String srcPath, String dstPath) {
        return new Pair<>(modelDiff.getParentModel().getTreeContextMap().get(srcPath),
                modelDiff.getChildModel().getTreeContextMap().get(dstPath));
    }
    public static Pair<TreeContext, TreeContext> findTreeContexts(UMLModelDiff modelDiff, UMLAbstractClassDiff classDiff) {
        return findTreeContexts(
                modelDiff,
                classDiff.getOriginalClass().getSourceFile(),
                classDiff.getNextClass().getSourceFile()
        );
    }
    public static Pair<TreeContext, TreeContext> findTreeContexts(UMLModelDiff modelDiff, UMLPackageInfoDiff packageInfoDiff) {
        return findTreeContexts(
                modelDiff,
                packageInfoDiff.getOriginalPackageInfo().getLocationInfo().getFilePath(),
                packageInfoDiff.getNextPackageInfo().getLocationInfo().getFilePath()
        );
    }
    public static Pair<TreeContext, TreeContext> findTreeContexts(UMLModelDiff modelDiff, UMLModuleDiff moduleDiff) {
        return findTreeContexts(
                modelDiff,
                moduleDiff.getOriginalModule().getLocationInfo().getFilePath(),
                moduleDiff.getNextModule().getLocationInfo().getFilePath()
        );
    }

    public static Pair<Tree, Tree> findPairOfType(Tree srcOperationNode, Tree dstOperationNode, String searchingType1, String searchingType2) {
        if (srcOperationNode == null || dstOperationNode == null) return null;
        Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode,searchingType1);
        Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode,searchingType2);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }

    public static List<Pair<Tree, Tree>> findPairsOfType(Tree srcOperationNode, Tree dstOperationNode, String searchingType1, String searchingType2) {
        if (srcOperationNode == null || dstOperationNode == null) return Collections.emptyList();
        List<Pair<Tree, Tree>> list = new ArrayList<Pair<Tree,Tree>>();
        List<Tree> srcModifiers = TreeUtilFunctions.findChildrenByType(srcOperationNode,searchingType1);
        List<Tree> dstModifiers = TreeUtilFunctions.findChildrenByType(dstOperationNode,searchingType2);
        if (srcModifiers != null && dstModifiers != null && srcModifiers.size() == dstModifiers.size()) {
            for(int i=0; i<srcModifiers.size(); i++)
                list.add(new Pair<>(srcModifiers.get(i), dstModifiers.get(i)));
        }
        return list;
    }

    public static Collection<ASTDiff> findAppends(Collection<ASTDiff> diffs, String originalSourceFile, String nextSourceFile, boolean both) {
        Collection<ASTDiff> appends = new LinkedHashSet<>();
        for (ASTDiff existing : diffs) {
            if (both) {
                if (existing.getSrcPath().equals(originalSourceFile) && existing.getDstPath().equals(nextSourceFile)) {
                    appends.add(existing);
                    break;
                }
            }
            else {
                if (existing.getSrcPath().equals(originalSourceFile))
                    appends.add(existing);
                else if (existing.getDstPath().equals(nextSourceFile))
                    appends.add(existing);
            }
        }
        return appends;
    }
}
