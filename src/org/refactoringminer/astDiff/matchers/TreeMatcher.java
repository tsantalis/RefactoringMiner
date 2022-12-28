package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
public interface TreeMatcher {
    void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore);

    default void match(Tree src, Tree dst, AbstractStatement st1, AbstractStatement st2, ExtendedMultiMappingStore mappingStore) {}
}
