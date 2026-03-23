package org.refactoringminer.astDiff.moved;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findTreeContexts;

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
            Pair<TreeContext, TreeContext> treeContextPairs = findTreeContexts(modelDiff, pair.first, pair.second);
            List<Mapping> mappings = filePairMappings.get(pair);
            if(!mappings.isEmpty()) {
                Tree leftRoot = treeContextPairs.first.getRoot();
                Tree rightRoot = treeContextPairs.second.getRoot();
                Constants LANG1 = new Constants(pair.first);
                Constants LANG2 = new Constants(pair.second);
                ExtendedMultiMappingStore store = new ExtendedMultiMappingStore(leftRoot, rightRoot, LANG1, LANG2);
                for(Mapping m : mappings) {
                    store.addMapping(m.first, m.second); //NOTE: SubClasses must provide all the mappings that are needed for the ASTDiff
                }
                ASTDiff diff = new ASTDiff(
                        pair.first,
                        pair.second,
                        treeContextPairs.first,
                        treeContextPairs.second,
                        store
                );
                movedDiffs.add(diff);
            }
        }
        return movedDiffs;
    }
}
