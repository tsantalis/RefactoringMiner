package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class IgnoringCommentsLeafMatcher extends LeafMatcher {
    public IgnoringCommentsLeafMatcher(Constants LANG1, Constants LANG2) {
        super(LANG1, LANG2);
    }
    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        ExtendedMultiMappingStore newMappings = new ExtendedMultiMappingStore(src, dst, LANG1, LANG2);
        super.match(src, dst, newMappings);
        // Remove comment mappings from the mapping store
        for (Mapping newMapping : newMappings) {
            if (isPartOfComments(newMapping.first, LANG1) || isPartOfComments(newMapping.second, LANG2)) {
                newMappings.removeMapping(newMapping.first, newMapping.second);
            }
        }
        for (Mapping newMapping : newMappings) {
            mappingStore.addMapping(newMapping.first, newMapping.second);
        }
    }
    public static boolean isPartOfComments(Tree tree, Constants LANG) {
        return TreeUtilFunctions.getParentUntilType(tree, LANG.LINE_COMMENT) != null ||
               TreeUtilFunctions.getParentUntilType(tree, LANG.JAVA_DOC) != null ||
               TreeUtilFunctions.getParentUntilType(tree, LANG.BLOCK_COMMENT) != null;
    }
}
