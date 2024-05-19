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

import java.util.*;

public abstract class MovedASTDiffGenerator {
    protected final ProjectASTDiff projectASTDiff;
    protected final UMLModelDiff modelDiff;
    public MovedASTDiffGenerator(UMLModelDiff modelDiff, ProjectASTDiff projectASTDiff) {
        this.modelDiff = modelDiff;
        this.projectASTDiff = projectASTDiff;
    }
    protected abstract Map<Pair<String, String>, List<Mapping>> makeFilePairMappings();
    public Set<ASTDiff> make() {
        Map<Pair<String, String>, List<Mapping>> filePairMappings = makeFilePairMappings();
        Set<ASTDiff> movedDiffs = new LinkedHashSet<>();
        for(Pair<String, String> pair : filePairMappings.keySet()) {
            Pair<TreeContext, TreeContext> treeContextPairs = findTreeContexts(pair.first, pair.second);
            List<Mapping> mappings = filePairMappings.get(pair);
            if(!mappings.isEmpty()) {
                Tree leftRoot = treeContextPairs.first.getRoot();
                Tree rightRoot = treeContextPairs.second.getRoot();
                ExtendedMultiMappingStore store = new ExtendedMultiMappingStore(leftRoot, rightRoot);
                for(Mapping m : mappings) {
                    store.addMapping(m.first, m.second); //NOTE: SubClasses must provide all the mappings that are needed for the ASTDiff
                }
                store.addMapping(leftRoot, rightRoot); //This helps Chawathe to generate the editscript properly, however the mapping is actually incorrect
                ASTDiff diff = new ASTDiff(
                        pair.first,
                        pair.second,
                        treeContextPairs.first,
                        treeContextPairs.second,
                        store,
                        new SimplifiedExtendedChawatheScriptGenerator().computeActions(store)
                );
                store.removeMapping(leftRoot, rightRoot); //Removes the mapping that was added to help Chawathe
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
