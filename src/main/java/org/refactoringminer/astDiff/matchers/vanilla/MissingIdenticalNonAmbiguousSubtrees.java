package org.refactoringminer.astDiff.matchers.vanilla;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.DefaultPriorityTreeQueue;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.PriorityTreeQueue;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/* Created by pourya on 2023-06-14 2:10 p.m. */
public class MissingIdenticalNonAmbiguousSubtrees extends GreedySubtreeMatcher implements TreeMatcher {

    private final static boolean ONLY_JAVA_DOCS = false;
    private static final int DEFAULT_MIN_PRIORITY = 1;
    protected int minPriority = DEFAULT_MIN_PRIORITY;
    protected final Predicate<Mapping> acceptance;
    public final int tooAmbiguousThreshold = 5;

    public MissingIdenticalNonAmbiguousSubtrees(Predicate<Mapping> acceptance) {
        this.acceptance = acceptance;
    }

    public MissingIdenticalNonAmbiguousSubtrees() {
        this.acceptance = (m) -> isAcceptable(m.first, m.second);
    }

    private static final String DEFAULT_PRIORITY_CALCULATOR = "height";
    protected Function<Tree, Integer> priorityCalculator = PriorityTreeQueue
            .getPriorityCalculator(DEFAULT_PRIORITY_CALCULATOR);

    protected Tree src;
    protected Tree dst;
    protected ExtendedMultiMappingStore mappings;

    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappingStore;

        var multiMappings = new MultiMappingStore();
        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, this.minPriority, this.priorityCalculator);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, this.minPriority, this.priorityCalculator);

        while (!(srcTrees.isEmpty() || dstTrees.isEmpty())) {
            PriorityTreeQueue.synchronize(srcTrees, dstTrees);
            if (srcTrees.isEmpty() || dstTrees.isEmpty())
                break;

            var currentPrioritySrcTrees = srcTrees.pop();
            var currentPriorityDstTrees = dstTrees.pop();

            for (var currentSrc : currentPrioritySrcTrees)
                for (var currentDst : currentPriorityDstTrees)
                    if (currentSrc.getMetrics().hash == currentDst.getMetrics().hash)
                        if (TreeUtilFunctions.isIsomorphicTo(currentSrc, currentDst)) {
                            if (!mappingStore.isSrcMappedConsideringSubTrees(currentSrc) && !mappingStore.isDstMappedConsideringSubTrees(currentDst))
                                multiMappings.addMapping(currentSrc, currentDst);
                        }

            for (var t : currentPrioritySrcTrees)
                if (!multiMappings.hasSrc(t))
                    srcTrees.open(t);
            for (var t : currentPriorityDstTrees)
                if (!multiMappings.hasDst(t))
                    dstTrees.open(t);
        }

        filterMappings(multiMappings);
    }
    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        List<Mapping> ambiguousList = new ArrayList<>();
        Set<Tree> ignored = new HashSet<>();
        Set<Tree> trees = new TreeSet<>(Comparator.comparingInt(Tree::getPos));
        trees.addAll(multiMappings.allMappedSrcs());
        for (var src : trees) {
            var isMappingUnique = false;
            if (tinyTrees(src,multiMappings,minPriority))
                continue;
            if (multiMappings.isSrcUnique(src)) {
                var dst = multiMappings.getDsts(src).stream().findAny().get();
                if (multiMappings.isDstUnique(dst)) {
                    if (acceptance.test(new Mapping(src,dst)))
                        mappings.addMappingRecursively(src,dst);
                    isMappingUnique = true;
                }
            }
            if (!isMappingUnique){
                Set<Tree> dsts = multiMappings.getDsts(src);
                boolean tooAmbiguous = false;
                if (dsts.size() > 5) {
                    Tree anyDst = dsts.stream().findAny().get();
                    Set<Tree> srcs = multiMappings.getSrcs(anyDst);
                    if (srcs.size() > tooAmbiguousThreshold) {
                        tooAmbiguous = true;
                    }
                }
                if (tooAmbiguous) continue;
            }
            if (!tinyTrees(src,multiMappings,minPriority) && !(ignored.contains(src) || isMappingUnique))
            {
                var adsts = multiMappings.getDsts(src);
                var asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
                for (Tree asrc : asrcs)
                    for (Tree adst : adsts) {
                        ambiguousList.add(new Mapping(asrc, adst));
                    }
                ignored.addAll(asrcs);
            }
            Set<Tree> srcIgnored = new HashSet<>();
            Set<Tree> dstIgnored = new HashSet<>();
            Collections.sort(ambiguousList, new CustomTopDownMatcher.ExtendedFullMappingComparator(mappings.getMonoMappingStore()));
            // Select the best ambiguous mappings
            retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
        }
    }

    private boolean isAcceptable(Tree src, Tree dst) {
        if (ONLY_JAVA_DOCS)
        {
            if (src.getType().name.equals(Constants.get().JAVA_DOC))
                return true;
            return false;
        }
        boolean ret;
        if (src.getType().name.equals(Constants.get().JAVA_DOC))
            return true;
        else {
            if (TreeUtilFunctions.isStatement(src.getType().name) && !src.getType().name.equals(Constants.get().BLOCK))
                if (src.getType().name.equals(Constants.get().RETURN_STATEMENT) && src.getMetrics().height <= 2)
                    ret =  false;
                else
                    ret =  true;
            else if (src.getType().name.equals(Constants.get().METHOD_INVOCATION)) {
                if (!src.getParent().getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER) &&
                        dst.getParent().getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER)) ret = false;
                else if (src.getParent().getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER) &&
                        !dst.getParent().getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER)) ret = true;
                else{
                    ret = true;
                }
            } else if (src.getType().name.equals(Constants.get().METHOD_INVOCATION_ARGUMENTS))
                ret = true;
            else if (src.getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER))
                ret =  true;
            else if (src.getType().name.equals(Constants.get().INFIX_EXPRESSION))
                ret =  true;
            else if (src.getType().name.equals(Constants.get().CLASS_INSTANCE_CREATION))
                ret =  true;
            else if (src.getType().name.equals(Constants.get().IMPORT_DECLARATION))
                ret = true;
            else if (TreeUtilFunctions.isPartOf(src, Constants.get().JAVA_DOC))
                ret = true;
            else if(src.getType().name.equals(Constants.get().LINE_COMMENT) || src.getType().name.equals(Constants.get().BLOCK_COMMENT))
                ret = true;
            else {
                ret = false;
            }
        }
        if (!ret) return false;
        if (notBelongingToMethodWithTestAnnotation(src) && notBelongingToMethodWithTestAnnotation(dst))
            return ret;
        else return false;

    }

    private boolean notBelongingToMethodWithTestAnnotation(Tree src) {
        Tree methodDecl = TreeUtilFunctions.getParentUntilType(src, Constants.get().METHOD_DECLARATION);
        if (methodDecl == null) return true;
        for (Tree child : methodDecl.getChildren()) {
            if (child.getType().name.equals(Constants.get().MARKER_ANNOTATION))
            {
                if (!child.getChildren().isEmpty() &&
                        child.getChild(0).getType().name.equals(Constants.get().SIMPLE_NAME) &&
                        child.getChild(0).getLabel().equals("Test"))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean tinyTrees(Tree src, MultiMappingStore multiMappings, int minP) {
        if (src.getMetrics().height <= minP){
            if (src.getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER))
                return true;
            if (src.getType().name.equals(Constants.get().METHOD_INVOCATION_ARGUMENTS))
                return true;
            if (src.getType().name.equals(Constants.get().SIMPLE_TYPE ))
                return true;
        }
        if (src.getType().name.equals(Constants.get().METHOD_INVOCATION_RECEIVER)) {
            return true;
        }
        return false;
    }

    @Override
    protected void retainBestMapping(List<Mapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
        List<Mapping> verifiedList = new ArrayList<>();
        for (Mapping mapping : mappingList) {
            if (isAcceptable(mapping.first, mapping.second))
                verifiedList.add(mapping);
        }
        while (!verifiedList.isEmpty()) {
            var mapping = verifiedList.remove(0);
            if (!(srcIgnored.contains(mapping.first) || dstIgnored.contains(mapping.second)))
            {
                mappings.addMappingRecursively(mapping.first, mapping.second);
                srcIgnored.add(mapping.first);
                srcIgnored.addAll(mapping.first.getDescendants());
                dstIgnored.add(mapping.second);
                dstIgnored.addAll(mapping.second.getDescendants());
            }
        }
    }
}
