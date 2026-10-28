package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.diff.StringDistance;
import org.refactoringminer.astDiff.matchers.statement.IgnoringCommentsLeafMatcher;
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
    //adjacent string literals are a single string literal in the model, so a plain string is paired with the most similar part, i.e., 'a={}' -> 'a={}, ' 'b={}'
    private static Tree closestPart(Tree concatenated, Tree string) {
        Tree closest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Tree part : concatenated.getChildren()) {
            int distance = StringDistance.editDistance(part.getLabel(), string.getLabel());
            if (distance < minDistance) {
                minDistance = distance;
                closest = part;
            }
        }
        return closest;
    }

    //the model maps a keyword argument by its value, i.e., vocab_size=vocab_size moved into the initializer of an extracted variable,
    //so the keyword arguments of mapped values with the same keyword are mapped too, unless the arguments are already mapped
    private void matchKeywordArguments(Tree srcValue, Tree dstValue, ExtendedMultiMappingStore store, ExtendedMultiMappingStore allMappings) {
        Tree srcArgument = keywordArgumentOfValue(srcValue, astDiff.LANG1);
        Tree dstArgument = keywordArgumentOfValue(dstValue, astDiff.LANG2);
        if (srcArgument != null && dstArgument != null && srcArgument.getChild(0).getLabel().equals(dstArgument.getChild(0).getLabel()) &&
                !allMappings.isSrcMapped(srcArgument) && !allMappings.isDstMapped(dstArgument)) {
            store.addMapping(srcArgument, dstArgument);
            store.addMapping(srcArgument.getChild(0), dstArgument.getChild(0));
            store.addMapping(srcArgument.getChild(1), dstArgument.getChild(1));
        }
    }

    private static Tree keywordArgumentOfValue(Tree value, Constants lang) {
        Tree argument = value.getParent();
        if (argument != null && argument.getType().name.equals(lang.KEYWORD_ARGUMENT) && argument.getChildren().size() == 3 &&
                argument.getChild(2) == value && argument.getChild(0).getType().name.equals(lang.KEYWORD_IDENTIFIER))
            return argument;
        return null;
    }

    private void processOptimization(ASTDiff input, OptimizationData optimizationData, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
        Tree srcTree = input.src.getRoot();
        Tree dstTree = input.dst.getRoot();
        ExtendedMultiMappingStore lastStepMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree,astDiff.LANG1,astDiff.LANG2);
        for (AbstractCodeMapping lastStepMapping : optimizationData.getLastStepMappings()) {
            if (lastStepMapping.getFragment1().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.STRING_LITERAL)
                    && lastStepMapping.getFragment2().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.STRING_LITERAL))
            {
                //Handles all string-literal cases (intra and inter file)
                Tree srcTotal = parentContextMap.get(lastStepMapping.getFragment1().getLocationInfo().getFilePath()).getRoot();
                Tree dstTotal = childContextMap.get(lastStepMapping.getFragment2().getLocationInfo().getFilePath()).getRoot();
                Tree srcStringLiteral = TreeUtilFunctions.findByLocationInfo(srcTotal, lastStepMapping.getFragment1().getLocationInfo(), astDiff.LANG1, astDiff.LANG1.STRING_LITERAL, astDiff.LANG1.CONCATENATED_STRING);
                Tree dstStringLiteral = TreeUtilFunctions.findByLocationInfo(dstTotal, lastStepMapping.getFragment2().getLocationInfo(), astDiff.LANG2, astDiff.LANG2.STRING_LITERAL, astDiff.LANG2.CONCATENATED_STRING);
                if (srcStringLiteral != null && dstStringLiteral != null) {
                    boolean concatenated1 = srcStringLiteral.getType().name.equals(astDiff.LANG1.CONCATENATED_STRING);
                    boolean concatenated2 = dstStringLiteral.getType().name.equals(astDiff.LANG2.CONCATENATED_STRING);
                    if (concatenated1 && concatenated2) {
                        if (TreeUtilFunctions.isIsomorphicTo(srcStringLiteral, dstStringLiteral))
                            input.getAllMappings().addMappingRecursively(srcStringLiteral, dstStringLiteral);
                        else {
                            new LeafMatcher(astDiff.LANG1, astDiff.LANG2).match(srcStringLiteral, dstStringLiteral, input.getAllMappings());
                            input.getAllMappings().addMapping(srcStringLiteral, dstStringLiteral);
                        }
                    }
                    else if (concatenated1)
                        input.getAllMappings().addMapping(closestPart(srcStringLiteral, dstStringLiteral), dstStringLiteral);
                    else if (concatenated2)
                        input.getAllMappings().addMapping(srcStringLiteral, closestPart(dstStringLiteral, srcStringLiteral));
                    else
                        input.getAllMappings().addMapping(srcStringLiteral, dstStringLiteral);
                }
                continue;
            }
            if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(input.getSrcPath()) && lastStepMapping.getFragment2().getLocationInfo().getFilePath().equals(input.getDstPath())) {
                Tree srcExp = TreeUtilFunctions.findByLocationInfo(srcTree, lastStepMapping.getFragment1().getLocationInfo(), astDiff.LANG1);
                Tree dstExp = TreeUtilFunctions.findByLocationInfo(dstTree, lastStepMapping.getFragment2().getLocationInfo(), astDiff.LANG2);
                if (srcExp == null || dstExp == null) continue;
                if(Constants.isCrossLanguage(astDiff.LANG1, astDiff.LANG2)) {
                    JavaToKotlinMigration.handleLeafMapping(lastStepMappingStore, srcExp, dstExp, astDiff.LANG1, astDiff.LANG2, lastStepMapping.getRefactorings(), optimizationData.getDeferredFlattenings());
                }
                ExtendedMultiMappingStore store = needToOverride(input, srcExp, dstExp) ? lastStepMappingStore : input.getAllMappings();
                if (store == lastStepMappingStore)
                    new IgnoringCommentsLeafMatcher(astDiff.LANG1, astDiff.LANG2).match(srcExp, dstExp, lastStepMappingStore);
                else
                    new LeafMatcher(astDiff.LANG1, astDiff.LANG2).match(srcExp,dstExp,input.getAllMappings());
                //the value is the model's fragment on the source side for an extracted variable and on the destination side for an inlined one
                if (store.isSrcMapped(srcExp))
                    for (Tree dst : Set.copyOf(store.getDsts(srcExp)))
                        matchKeywordArguments(srcExp, dst, store, input.getAllMappings());
                if (store.isDstMapped(dstExp))
                    for (Tree src : Set.copyOf(store.getSrcs(dstExp)))
                        matchKeywordArguments(src, dstExp, store, input.getAllMappings());
            }
            else {
                //Inter-file optimizations
                TreeContext tc1 = parentContextMap.get(lastStepMapping.getFragment1().getLocationInfo().getFilePath());
                Tree t1 = TreeUtilFunctions.findByLocationInfo(tc1.getRoot(), lastStepMapping.getFragment1().getLocationInfo(), astDiff.LANG1);
                TreeContext tc2 = childContextMap.get(lastStepMapping.getFragment2().getLocationInfo().getFilePath());
                Tree t2 = TreeUtilFunctions.findByLocationInfo(tc2.getRoot(), lastStepMapping.getFragment2().getLocationInfo(), astDiff.LANG2);
                if (t1 == null || t2 == null) continue;
                new LeafMatcher(astDiff.LANG1, astDiff.LANG2).match(t1, t2, input.getAllMappings());
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
