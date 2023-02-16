package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.refactoringminer.astDiff.matchers.Constants;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 12:04 a.m.
 */
public class TreeUtilFunctions {

	private static FakeTree _instance;

	public static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo){
		int startoffset = locationInfo.getStartOffset();
		int endoffset = locationInfo.getEndOffset();

		Tree treeBetweenPositions = getTreeBetweenPositions(tree, startoffset, endoffset);
		if (treeBetweenPositions == null) return null;
		if (treeBetweenPositions.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS))
		{
			if (treeBetweenPositions.getChildren().size() > 0 )
			{
				if (treeBetweenPositions.getChild(0).getPos() == startoffset
						&& treeBetweenPositions.getChild(0).getEndPos() == endoffset)
					return treeBetweenPositions.getChild(0);
				else {
					return treeBetweenPositions;
				}
			}
			else {
				return treeBetweenPositions;
			}
		}
		return treeBetweenPositions;
	}

	public static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo, String type){
		int startoffset = locationInfo.getStartOffset();
		int endoffset = locationInfo.getEndOffset();
		return getTreeBetweenPositions(tree, startoffset, endoffset,type);
	}

	public static Tree getTreeBetweenPositions(Tree tree, int position, int endPosition) {
		for (Tree t: tree.preOrder()) {
			if (t.getPos() >= position && t.getEndPos() <= endPosition)
				return t;
		}
		return null;
	}

	public static Tree getTreeBetweenPositions(Tree tree, int position, int endPosition,String type) {
		for (Tree t: tree.preOrder()) {
			if (t.getPos() >= position && t.getEndPos() <= endPosition)
				if (t.getType().name.equals(type))
					return t;
		}
		return null;
	}

	public static Tree findChildByType(Tree tree, String type) {
		if (!tree.getChildren().isEmpty())
		{
			for (Tree child: tree.getChildren()) {
				if (child.getType().name.equals(type))
					return child;
			}
		}
		return null;
	}

	public static Tree findChildByTypeAndLabel(Tree tree, String type,String label) {
		if (!tree.getChildren().isEmpty())
		{
			for (Tree child: tree.getChildren()) {
				if (child.getType().name.equals(type) && child.getLabel().equals(label))
					return child;
			}
		}
		return null;
	}

	public static Pair<Tree,Tree> pruneTrees(Tree src, Tree dst, Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy)
	{
		Tree prunedSrc = deepCopyWithMapPruning(src,srcCopy);
		Tree prunedDst = deepCopyWithMapPruning(dst,dstCopy);
		return new Pair<>(prunedSrc,prunedDst);
	}

	public static Tree deepCopyWithMapPruning(Tree tree, Map<Tree,Tree> cpyMap) {
		if (tree.getType().name.equals(Constants.BLOCK)) return null;
		Tree copy = makeDefaultTree(tree);
		cpyMap.put(copy,tree);
		if (tree.getType().name.equals(Constants.ANONYMOUS_CLASS_DECLARATION)) return copy;
		for (Tree child : tree.getChildren()) {
			Tree childCopy = deepCopyWithMapPruning(child,cpyMap);
			if (childCopy != null)
				copy.addChild(childCopy);
		}
		if (copy.getChildren().size() == 0) copy.setParent(tree.getParent());
		return copy;
	}

	public static DefaultTree makeDefaultTree (Tree other) {
		DefaultTree defaultTree = new DefaultTree(null);
		defaultTree.setType(other.getType());
		defaultTree.setLabel(other.getLabel());
		defaultTree.setPos(other.getPos());
		defaultTree.setLength(other.getLength());
		defaultTree.setChildren(new ArrayList<>());
		defaultTree.setMetrics(other.getMetrics());
		return defaultTree;
	}

	public static Tree deepCopyWithMap(Tree tree,Map<Tree,Tree> cpyMap) {
		Tree copy = makeDefaultTree(tree);
		cpyMap.put(copy,tree);
		for (Tree child : tree.getChildren())
			copy.addChild(deepCopyWithMap(child,cpyMap));
		return copy;
	}

	public static FakeTree getFakeTreeInstance()
	{
		if (_instance == null)
			_instance = new FakeTree();
		return _instance;
	}

	public static Tree getFinalRoot(Tree tree)
	{
		if (tree.isRoot())
			return tree;
		if (tree.getParent() instanceof FakeTree)
			return tree;
		return getFinalRoot(tree.getParent());
	}

	public static Tree getParentUntilType(Tree tree, String matchingType) {
		if (tree.getType().name.equals(matchingType))
			return tree;
		if (tree.getParent() != null)
			return getParentUntilType(tree.getParent(),matchingType);
		else
			return null;
	}

	public static Tree loadTree(String name) {
		try {
			InputStream resourceAsStream = FileUtils.openInputStream(new File(name));
			return TreeIoUtils.fromXml().generateFrom().stream(resourceAsStream).getRoot();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Unable to load test resource: %s", name), e);
		}
	}

	public static void writeTree(Tree tree, String filePath) {
		TreeContext srcTC = new TreeContext();
		srcTC.setRoot(tree);
		try {
			TreeIoUtils.toXml(srcTC).writeTo(filePath);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
