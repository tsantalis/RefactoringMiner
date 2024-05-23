package org.refactoringminer.astDiff.moved;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.actions.classifier.ExtendedOnlyRootsClassifier;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-05-16*/
public class AllSubTreesMovedASTDiffGenerator extends MovedASTDiffGenerator {

    public AllSubTreesMovedASTDiffGenerator(UMLModelDiff modelDiff, ProjectASTDiff projectASTDiff) {
        super(modelDiff, projectASTDiff);
    }

    @Override
    public Map<Pair<String, String>, List<Mapping>> makeFilePairMappings() {
        Map<Pair<String, String>, List<Mapping>> filePairMappings = new LinkedHashMap<>();
        for(ASTDiff diff : projectASTDiff.getDiffSet()) {
            ExtendedOnlyRootsClassifier classifier = (ExtendedOnlyRootsClassifier) diff.createRootNodesClassifier(); // TODO: Switch to AllNodesClassifier
            for (Map.Entry<Tree, Action> treeActionEntry : classifier.getSrcMoveOutTreeMap().entrySet()) {
                MoveOut moveOut = (MoveOut) treeActionEntry.getValue();
                String srcPath = diff.getSrcPath();
                String dstPath = moveOut.getDstFile();
                add(filePairMappings, srcPath, dstPath, moveOut.getNode(), moveOut.getParent());
            }
            for (Map.Entry<Tree, Action> treeActionEntry : classifier.getDstMoveInTreeMap().entrySet()) {
                MoveIn moveIn = (MoveIn) treeActionEntry.getValue();
                String srcPath = moveIn.getSrcFile();
                String dstPath = diff.getDstPath();
                add(filePairMappings, srcPath, dstPath, moveIn.getNode(), moveIn.getParent());
            }

        }
        return filePairMappings;
    }

    private void add(Map<Pair<String, String>, List<Mapping>> filePairMappings, String srcPath, String dstPath, Tree node, Tree parent) {
        Pair<String, String> pair = new Pair<>(srcPath, dstPath);
        if (!filePairMappings.containsKey(pair)) {
            filePairMappings.put(pair, new ArrayList<>());
        }
        List<Mapping> target = filePairMappings.get(pair);
        target.add(new Mapping(node, parent));
    }
}
