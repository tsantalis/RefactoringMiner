package org.refactoringminer.astDiff;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.actions.SimplifiedExtendedChawatheScriptGenerator;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public abstract class MovedASTDiffGenerator {
    protected final ProjectASTDiff projectASTDiff;
    protected final UMLModelDiff modelDiff;
    protected final Map<Pair<String, String>, List<Mapping>> filePairMappings = new LinkedHashMap<>();
    public MovedASTDiffGenerator(UMLModelDiff modelDiff, ProjectASTDiff projectASTDiff) {
        this.modelDiff = modelDiff;
        this.projectASTDiff = projectASTDiff;
    }
    protected abstract void populateFilePairMappings();
    public Set<ASTDiff> make() {
        populateFilePairMappings();
        Set<ASTDiff> movedDiffs = new LinkedHashSet<>();
        for(Pair<String, String> pair : filePairMappings.keySet()) {
            Pair<TreeContext, TreeContext> treeContextPairs = findTreeContexts(pair.first, pair.second);
            List<Mapping> mappings = filePairMappings.get(pair);
            if(!mappings.isEmpty()) {
                Tree leftRoot = TreeUtilFunctions.getFinalRoot(mappings.get(0).first);
                Tree rightRoot = TreeUtilFunctions.getFinalRoot(mappings.get(0).second);
                ExtendedMultiMappingStore store = new ExtendedMultiMappingStore(leftRoot, rightRoot);
                for(Mapping m : mappings) {
                    store.addMappingRecursively(m.first, m.second);
                }
                ASTDiff diff = new ASTDiff(pair.first, pair.second, treeContextPairs.first, treeContextPairs.second, store, new SimplifiedExtendedChawatheScriptGenerator().computeActions(store, null, null));
                movedDiffs.add(diff);
            }
        }
        return movedDiffs;
    }

    protected List<Mapping> getMappingForLeft(ASTDiff diff, Tree left) {
        List<Mapping> matchingMappings = new ArrayList<Mapping>();
        for(Mapping mapping : diff.getAllMappings()) {
            if(mapping.first.equals(left)) {
                matchingMappings.add(mapping);
            }
        }
        return matchingMappings;
    }
    private Pair<TreeContext, TreeContext> findTreeContexts(String srcPath, String dstPath) {
        return new Pair<>(modelDiff.getParentModel().getTreeContextMap().get(srcPath),
                modelDiff.getChildModel().getTreeContextMap().get(dstPath));
    }
}
