package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.List;

//the expressions of the extracted or inlined code mapped to the arguments of the call, i.e., event.repository.owner.login passed to find_app_comment
public class ArgumentMappingsMatcher extends OptimizationAwareMatcher {

    private final List<AbstractCodeMapping> argumentMappings;

    public ArgumentMappingsMatcher(List<AbstractCodeMapping> argumentMappings) {
        this.argumentMappings = argumentMappings;
    }

    public ArgumentMappingsMatcher(OptimizationData optimizationData, List<AbstractCodeMapping> argumentMappings) {
        super(optimizationData);
        this.argumentMappings = argumentMappings;
    }

    @Override
    void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        for(AbstractCodeMapping expressionMapping : argumentMappings) {
            Constants LANG1 = new Constants(expressionMapping.getFragment1().getLocationInfo().getFilePath());
            Constants LANG2 = new Constants(expressionMapping.getFragment2().getLocationInfo().getFilePath());
            Tree t1 = TreeUtilFunctions.findByLocationInfo(srcTree,expressionMapping.getFragment1().getLocationInfo(),LANG1);
            Tree t2 = TreeUtilFunctions.findByLocationInfo(dstTree,expressionMapping.getFragment2().getLocationInfo(),LANG2);
            new LeafMatcher(LANG1, LANG2).match(t1,t2,optimizationData.getSubtreeMappings());
        }
    }
}
