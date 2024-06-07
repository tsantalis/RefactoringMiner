package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.Map;
import java.util.Set;

/* Created by pourya on 2024-06-06*/
public class ASTDiffMappingOptimizer extends OptimizationAwareMatcher{
    final ASTDiff astDiff;
    final Map<String, TreeContext> parentContextMap;
    final Map<String, TreeContext> childContextMap;

    public ASTDiffMappingOptimizer(ASTDiff astDiff, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
        this.astDiff = astDiff;
        this.parentContextMap = parentContextMap;
        this.childContextMap = childContextMap;
    }

    public ASTDiffMappingOptimizer(OptimizationData optimizationData, ASTDiff astDiff, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
        super(optimizationData);
        this.astDiff = astDiff;
        this.parentContextMap = parentContextMap;
        this.childContextMap = childContextMap;
    }

    @Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        processOptimization(astDiff, optimizationData, parentContextMap, childContextMap);

    }
    private static void processOptimization(ASTDiff input, OptimizationData optimizationData, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
        Tree srcTree = input.src.getRoot();
        Tree dstTree = input.dst.getRoot();
        ExtendedMultiMappingStore lastStepMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
        for (AbstractCodeMapping lastStepMapping : optimizationData.getLastStepMappings()) {
            if (lastStepMapping.getFragment1().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.STRING_LITERAL)
                    && lastStepMapping.getFragment2().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.STRING_LITERAL))
            {
                //Handles all string-literal cases (intra and inter file)
                Tree srcTotal = parentContextMap.get(lastStepMapping.getFragment1().getLocationInfo().getFilePath()).getRoot();
                Tree dstTotal = childContextMap.get(lastStepMapping.getFragment2().getLocationInfo().getFilePath()).getRoot();
                Tree srcStringLiteral = TreeUtilFunctions.findByLocationInfo(srcTotal, lastStepMapping.getFragment1().getLocationInfo(), Constants.STRING_LITERAL);
                Tree dstStringLiteral = TreeUtilFunctions.findByLocationInfo(dstTotal, lastStepMapping.getFragment2().getLocationInfo(), Constants.STRING_LITERAL);
                if (srcStringLiteral != null && dstStringLiteral != null) {
                    input.getAllMappings().addMapping(srcStringLiteral, dstStringLiteral);
                }
                continue;
            }
            if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(input.getSrcPath()) && lastStepMapping.getFragment2().getLocationInfo().getFilePath().equals(input.getDstPath())) {
                Tree srcExp = TreeUtilFunctions.findByLocationInfo(srcTree, lastStepMapping.getFragment1().getLocationInfo());
                Tree dstExp = TreeUtilFunctions.findByLocationInfo(dstTree, lastStepMapping.getFragment2().getLocationInfo());
                if (srcExp == null || dstExp == null) continue;
                if (needToOverride(input, srcExp, dstExp))
                    new LeafMatcher().match(srcExp, dstExp, lastStepMappingStore);
                else
                    new LeafMatcher().match(srcExp,dstExp,input.getAllMappings());
            }
            else {
                //Inter-file optimizations
            }
        }
        ExtendedMultiMappingStore allMappings = input.getAllMappings();
        allMappings.replaceWithOptimizedMappings(lastStepMappingStore);
        allMappings.replaceWithOptimizedMappings(optimizationData.getSubtreeMappings());
    }

    private static boolean needToOverride(ASTDiff input, Tree srcExp, Tree dstExp) {
        if (!srcExp.isIsomorphicTo(dstExp)) return true;
        ExtendedMultiMappingStore allMappings = input.getAllMappings();
        Set<Tree> dsts = allMappings.getDsts(srcExp);
        if (dsts != null)
        {
            for (Tree dst : dsts)
                if (!srcExp.isIsomorphicTo(dst))
                    return true;
            return false;
        }
        Set<Tree> srcs = allMappings.getSrcs(dstExp);
        if (srcs != null) {
            for (Tree src : srcs) {
                if (!src.isIsomorphicTo(dstExp)) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }
}
