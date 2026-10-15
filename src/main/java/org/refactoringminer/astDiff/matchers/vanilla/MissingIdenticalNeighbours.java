package org.refactoringminer.astDiff.matchers.vanilla;

import java.util.List;
import java.util.Set;

import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;

/**
 * Matches the unmapped subtrees that are identical to the unmapped subtrees at the same place in the children of a mapped node,
 * i.e., after or before the same mapped sibling, or at the same index when the mapped nodes have the same number of children.
 * These are the tokens that the other matchers do not match, because they are not part of the mapped code elements and they are too small or ambiguous,
 * i.e., the macro LLAMA_API before a mapped declaration, or the macro name, the punctuation and the string literals of a mapped macro call in C and C++.
 * Only the leaves and the literals are matched, because the code elements, i.e., the statements and the calls, are matched by the other matchers.
 */
public class MissingIdenticalNeighbours implements TreeMatcher {

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Tree srcParent : src.preOrder()) {
				if (srcParent.isLeaf() || !mappingStore.isSrcMapped(srcParent)) {
					continue;
				}
				Set<Tree> dstParents = mappingStore.getDsts(srcParent);
				for (Tree dstParent : List.copyOf(dstParents)) {
					if (dstParent.isLeaf() || !dstParent.getType().equals(srcParent.getType())) {
						continue;
					}
					changed |= matchChildren(srcParent, dstParent, mappingStore);
				}
			}
		}
	}

	private static boolean matchChildren(Tree srcParent, Tree dstParent, ExtendedMultiMappingStore mappingStore) {
		boolean changed = false;
		List<Tree> srcChildren = srcParent.getChildren();
		List<Tree> dstChildren = dstParent.getChildren();
		for (int i = 0; i < srcChildren.size(); i++) {
			Tree srcChild = srcChildren.get(i);
			if (mappingStore.isSrcMappedConsideringSubTrees(srcChild)) {
				continue;
			}
			Tree dstChild = neighbour(srcChildren, i, dstParent, mappingStore);
			if (dstChild == null && srcChildren.size() == dstChildren.size()) {
				dstChild = dstChildren.get(i);
			}
			if (dstChild != null && isToken(srcChild) && !mappingStore.isDstMappedConsideringSubTrees(dstChild) && TreeUtilFunctions.isIsomorphicTo(srcChild, dstChild)) {
				mappingStore.addMappingRecursively(srcChild, dstChild);
				changed = true;
			}
		}
		return changed;
	}

	private static final Set<String> LITERALS = Set.of("string_literal", "char_literal", "raw_string_literal", "concatenated_string");

	//a leaf or a literal
	private static boolean isToken(Tree tree) {
		return tree.isLeaf() || LITERALS.contains(tree.getType().name);
	}

	//the child of the destination parent after the child mapped to the previous sibling, or before the child mapped to the next sibling
	private static Tree neighbour(List<Tree> srcChildren, int index, Tree dstParent, ExtendedMultiMappingStore mappingStore) {
		List<Tree> dstChildren = dstParent.getChildren();
		if (index > 0) {
			Tree dstPrevious = mappedChild(srcChildren.get(index - 1), dstParent, mappingStore);
			if (dstPrevious != null) {
				int dstIndex = dstChildren.indexOf(dstPrevious) + 1;
				return dstIndex < dstChildren.size() ? dstChildren.get(dstIndex) : null;
			}
		}
		if (index + 1 < srcChildren.size()) {
			Tree dstNext = mappedChild(srcChildren.get(index + 1), dstParent, mappingStore);
			if (dstNext != null) {
				int dstIndex = dstChildren.indexOf(dstNext) - 1;
				return dstIndex >= 0 ? dstChildren.get(dstIndex) : null;
			}
		}
		return null;
	}

	//the child of the destination parent mapped to the source child
	private static Tree mappedChild(Tree srcChild, Tree dstParent, ExtendedMultiMappingStore mappingStore) {
		if (!mappingStore.isSrcMapped(srcChild)) {
			return null;
		}
		for (Tree dst : mappingStore.getDsts(srcChild)) {
			if (dst.getParent() == dstParent) {
				return dst;
			}
		}
		return null;
	}
}
