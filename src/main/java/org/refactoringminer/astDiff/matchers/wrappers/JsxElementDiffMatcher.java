package org.refactoringminer.astDiff.matchers.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.JsxElement.JsxAttribute;
import gr.uom.java.xmi.decomposition.JsxElement.JsxChild;
import gr.uom.java.xmi.diff.JsxElementDiff;

/**
 * Matches the nodes of two JSX elements as their diff pairs them, i.e., the attributes by name and the children in order,
 * so that a changed attribute value is an update of the attribute, and a removed attribute is deleted,
 * i.e., {@code <Draggable onStop={handleStop} zIndex={999}>} -> {@code <Draggable onStop={(e, data) => handleStop(e, data)}>}.
 */
public class JsxElementDiffMatcher {
	private static final String JSX_ELEMENT = "jsx_element";
	private static final String JSX_SELF_CLOSING_ELEMENT = "jsx_self_closing_element";
	private static final String JSX_OPENING_ELEMENT = "jsx_opening_element";
	private static final String JSX_CLOSING_ELEMENT = "jsx_closing_element";
	private static final String JSX_ATTRIBUTE = "jsx_attribute";
	private static final String JSX_EXPRESSION = "jsx_expression";

	private final Tree srcTree;
	private final Tree dstTree;
	private final ExtendedMultiMappingStore mappingStore;
	private final Constants LANG1;
	private final Constants LANG2;

	public JsxElementDiffMatcher(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
		this.srcTree = srcTree;
		this.dstTree = dstTree;
		this.mappingStore = mappingStore;
		this.LANG1 = LANG1;
		this.LANG2 = LANG2;
	}

	public void match(JsxElementDiff diff) {
		Tree element1 = TreeUtilFunctions.findByLocationInfo(srcTree, diff.getElement1().getLocationInfo(), LANG1, JSX_ELEMENT, JSX_SELF_CLOSING_ELEMENT);
		Tree element2 = TreeUtilFunctions.findByLocationInfo(dstTree, diff.getElement2().getLocationInfo(), LANG2, JSX_ELEMENT, JSX_SELF_CLOSING_ELEMENT);
		if(element1 == null || element2 == null)
			return;
		mappingStore.addMapping(element1, element2);
		//the opening tag holds the attributes, which are the element itself when it is self closing
		Tree opening1 = element1.getType().name.equals(JSX_ELEMENT) ? child(element1, JSX_OPENING_ELEMENT) : element1;
		Tree opening2 = element2.getType().name.equals(JSX_ELEMENT) ? child(element2, JSX_OPENING_ELEMENT) : element2;
		if(opening1 != null && opening2 != null) {
			if(opening1 != element1 || opening2 != element2)
				mappingStore.addMapping(opening1, opening2);
			matchTag(opening1, opening2);
			matchTokens(opening1, opening2);
		}
		Tree closing1 = child(element1, JSX_CLOSING_ELEMENT);
		Tree closing2 = child(element2, JSX_CLOSING_ELEMENT);
		if(closing1 != null && closing2 != null) {
			mappingStore.addMapping(closing1, closing2);
			matchTag(closing1, closing2);
			matchTokens(closing1, closing2);
		}
		for(JsxAttribute attribute : diff.getCommonAttributes()) {
			JsxAttribute attribute2 = counterpart(attribute, diff.getElement2().getAttributes());
			Tree attribute1Tree = find(srcTree, attribute.location(), LANG1);
			Tree attribute2Tree = attribute2 != null ? find(dstTree, attribute2.location(), LANG2) : null;
			if(attribute1Tree != null && attribute2Tree != null && attribute1Tree.isIsomorphicTo(attribute2Tree))
				mappingStore.addMappingRecursively(attribute1Tree, attribute2Tree);
		}
		for(Pair<JsxAttribute, JsxAttribute> pair : diff.getChangedAttributes()) {
			Tree attribute1 = find(srcTree, pair.getLeft().location(), LANG1);
			Tree attribute2 = find(dstTree, pair.getRight().location(), LANG2);
			if(attribute1 != null && attribute2 != null)
				matchChangedAttribute(attribute1, attribute2);
		}
		for(JsxAttribute attribute : diff.getRemovedAttributes()) {
			Tree attribute1 = find(srcTree, attribute.location(), LANG1);
			if(attribute1 != null)
				removeMappingsWithin(attribute1, element2, true);
		}
		for(JsxAttribute attribute : diff.getAddedAttributes()) {
			Tree attribute2 = find(dstTree, attribute.location(), LANG2);
			if(attribute2 != null)
				removeMappingsWithin(attribute2, element1, false);
		}
		for(Pair<JsxChild, JsxChild> pair : diff.getCommonChildren()) {
			Tree child1 = find(srcTree, pair.getLeft().location(), LANG1);
			Tree child2 = find(dstTree, pair.getRight().location(), LANG2);
			if(child1 != null && child2 != null && child1.isIsomorphicTo(child2))
				mappingStore.addMappingRecursively(child1, child2);
		}
		for(Pair<JsxChild, JsxChild> pair : diff.getChangedChildren()) {
			if(pair.getLeft().isElement() && pair.getRight().isElement()) {
				for(JsxElementDiff childDiff : diff.getChildElementDiffs()) {
					if(childDiff.getElement1() == pair.getLeft().element() && childDiff.getElement2() == pair.getRight().element()) {
						match(childDiff);
						break;
					}
				}
			}
			else {
				Tree child1 = find(srcTree, pair.getLeft().location(), LANG1);
				Tree child2 = find(dstTree, pair.getRight().location(), LANG2);
				if(child1 != null && child2 != null && child1.getType().name.equals(child2.getType().name))
					matchChangedValue(child1, child2);
			}
		}
	}

	//the tag of an opening or closing element, i.e., Draggable in <Draggable ...>
	private void matchTag(Tree tag1Parent, Tree tag2Parent) {
		Tree tag1 = tag(tag1Parent);
		Tree tag2 = tag(tag2Parent);
		if(tag1 == null || tag2 == null)
			return;
		if(tag1.isIsomorphicTo(tag2))
			mappingStore.addMappingRecursively(tag1, tag2);
		else if(tag1.getType().name.equals(tag2.getType().name))
			new LeafMatcher(LANG1, LANG2).match(tag1, tag2, mappingStore);
	}

	private static Tree tag(Tree parent) {
		for(Tree child : parent.getChildren()) {
			if(!child.isLeaf() || !child.getLabel().equals(child.getType().name))
				return child.getType().name.equals(JSX_ATTRIBUTE) ? null : child;
		}
		return null;
	}

	//an attribute with a changed value keeps its name, i.e., onStop={handleStop} -> onStop={(e, data) => handleStop(e, data)}
	private void matchChangedAttribute(Tree attribute1, Tree attribute2) {
		mappingStore.addMapping(attribute1, attribute2);
		List<Tree> children1 = attribute1.getChildren();
		List<Tree> children2 = attribute2.getChildren();
		if(children1.isEmpty() || children2.isEmpty())
			return;
		Tree name1 = children1.get(0);
		Tree name2 = children2.get(0);
		if(name1.isIsomorphicTo(name2))
			mappingStore.addMappingRecursively(name1, name2);
		matchTokens(attribute1, attribute2);
		Tree value1 = children1.get(children1.size() - 1);
		Tree value2 = children2.get(children2.size() - 1);
		if(value1 != name1 && value2 != name2 && value1.getType().name.equals(value2.getType().name))
			matchChangedValue(value1, value2);
	}

	//a value in braces keeps its braces, and the expressions inside are matched as a leaf
	private void matchChangedValue(Tree value1, Tree value2) {
		if(value1.isIsomorphicTo(value2)) {
			mappingStore.addMappingRecursively(value1, value2);
			return;
		}
		mappingStore.addMapping(value1, value2);
		if(value1.getType().name.equals(JSX_EXPRESSION)) {
			matchTokens(value1, value2);
			List<Tree> inner1 = nonTokens(value1);
			List<Tree> inner2 = nonTokens(value2);
			if(inner1.size() == 1 && inner2.size() == 1)
				new LeafMatcher(LANG1, LANG2).match(inner1.get(0), inner2.get(0), mappingStore);
		}
		else {
			new LeafMatcher(LANG1, LANG2).match(value1, value2, mappingStore);
		}
	}

	//the punctuation of two nodes in order, i.e., < > = { } /> </
	private void matchTokens(Tree tree1, Tree tree2) {
		List<Tree> tokens2 = new ArrayList<>();
		for(Tree child2 : tree2.getChildren()) {
			if(isToken(child2))
				tokens2.add(child2);
		}
		for(Tree child1 : tree1.getChildren()) {
			if(!isToken(child1))
				continue;
			for(Tree child2 : tokens2) {
				if(child2.getType().name.equals(child1.getType().name)) {
					mappingStore.addMapping(child1, child2);
					tokens2.remove(child2);
					break;
				}
			}
		}
	}

	private static boolean isToken(Tree tree) {
		return tree.isLeaf() && tree.getLabel().equals(tree.getType().name);
	}

	private static List<Tree> nonTokens(Tree tree) {
		List<Tree> list = new ArrayList<>();
		for(Tree child : tree.getChildren()) {
			if(!isToken(child))
				list.add(child);
		}
		return list;
	}

	//the nodes of a removed (added) attribute are not mapped with nodes of the other element, as the leaf matcher may pair them by similarity
	private void removeMappingsWithin(Tree removed, Tree otherElement, boolean removedIsSrc) {
		List<Tree> otherDescendants = otherElement.getDescendants();
		for(Tree node : removed.preOrder()) {
			if(removedIsSrc && mappingStore.isSrcMapped(node)) {
				for(Tree dst : new ArrayList<>(mappingStore.getDsts(node))) {
					if(otherDescendants.contains(dst))
						mappingStore.removeMapping(node, dst);
				}
			}
			else if(!removedIsSrc && mappingStore.isDstMapped(node)) {
				for(Tree src : new ArrayList<>(mappingStore.getSrcs(node))) {
					if(otherDescendants.contains(src))
						mappingStore.removeMapping(src, node);
				}
			}
		}
	}

	private static JsxAttribute counterpart(JsxAttribute attribute, List<JsxAttribute> attributes) {
		for(JsxAttribute other : attributes) {
			if(other.equals(attribute))
				return other;
		}
		return null;
	}

	private static Tree child(Tree parent, String type) {
		for(Tree child : parent.getChildren()) {
			if(child.getType().name.equals(type))
				return child;
		}
		return null;
	}

	private static Tree find(Tree tree, LocationInfo location, Constants LANG) {
		return location != null ? TreeUtilFunctions.findByLocationInfo(tree, location, LANG) : null;
	}
}
