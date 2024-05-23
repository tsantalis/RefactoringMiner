package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.Map;

/* Created by pourya on 2024-05-22*/
public class Helpers {
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

    public static Pair<Tree, Tree> findPairOfType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
        if (srcOperationNode == null || dstOperationNode == null) return null;
        Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode,searchingType);
        Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode,searchingType);
        if (srcModifier != null && dstModifier != null)
            return new Pair<>(srcModifier, dstModifier);
        return null;
    }
}
