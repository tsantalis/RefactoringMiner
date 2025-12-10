package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class IgnoringCommentsLeafMatcher extends LeafMatcher {
    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        ExtendedMultiMappingStore newMappings = new ExtendedMultiMappingStore(src, dst);
        super.match(src, dst, newMappings);
        // Remove comment mappings from the mapping store
        for (Mapping newMapping : newMappings) {
            if (isPartOfComments(newMapping.first) || isPartOfComments(newMapping.second)) {
                newMappings.removeMapping(newMapping.first, newMapping.second);
            }
        }
        for (Mapping newMapping : newMappings) {
            mappingStore.addMapping(newMapping.first, newMapping.second);
        }
    }
    public static boolean isPartOfComments(Tree tree) {
        return TreeUtilFunctions.getParentUntilType(tree, Constants.get().LINE_COMMENT) != null ||
               TreeUtilFunctions.getParentUntilType(tree, Constants.get().JAVA_DOC) != null ||
               TreeUtilFunctions.getParentUntilType(tree, Constants.get().BLOCK_COMMENT) != null;
    }
}
