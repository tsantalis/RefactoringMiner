package org.refactoringminer.astDiff.matchers.atomic;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;

/* Created by pourya on 2024-05-22*/
public interface IExtendedMatcher {
    void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore);
    default void match(Tree srcTree, Tree dstTree) {
        this.match(srcTree, dstTree, new ExtendedMultiMappingStore(srcTree, dstTree));
    }
}
