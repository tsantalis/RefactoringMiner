package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.Mapping;
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
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 12:04 a.m.
 */
public class TreeUtilFunctions {

	private static FakeTree _instance;

	public static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo){
		if (locationInfo == null) return null;
		int start_offset = locationInfo.getStartOffset();
		int end_offset = locationInfo.getEndOffset();
		if (tree.getPos() > start_offset || tree.getEndPos() < end_offset)  return (tree.getParent() != null) ? findByLocationInfo(tree.getParent(),locationInfo) : null;
		return findByLocationInfoNoLookAhead(tree, locationInfo);
	}

	public static Tree findByLocationInfoNoLookAhead(Tree tree, LocationInfo locationInfo) {
		int start_offset = locationInfo.getStartOffset();
		int end_offset = locationInfo.getEndOffset();
		Tree treeBetweenPositions = getTreeBetweenPositions(tree, start_offset, end_offset);
		if (treeBetweenPositions == null) return null;
		if (isFromType(treeBetweenPositions, Constants.METHOD_INVOCATION_ARGUMENTS))
		{
			if (treeBetweenPositions.getChildren().size() > 0 )
			{
				if (treeBetweenPositions.getChild(0).getPos() == start_offset
						&& treeBetweenPositions.getChild(0).getEndPos() == end_offset)
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

	public static Tree findByLocationInfo(Tree tree, LocationInfo locationInfo, String... type){
        if (locationInfo == null) return null;
		int startoffset = locationInfo.getStartOffset();
		int endoffset = locationInfo.getEndOffset();
        if (tree.getPos() > startoffset || tree.getEndPos() < endoffset)  return (tree.getParent() != null) ? findByLocationInfo(tree.getParent(),locationInfo) : null;
		return getTreeBetweenPositions(tree, startoffset, endoffset,type);
	}

	public static Tree getTreeBetweenPositions(Tree tree, int position, int endPosition) {
		for (Tree t: tree.preOrder()) {
			if (t.getPos() >= position && t.getEndPos() <= endPosition)
				return t;
		}
		return null;
	}

	public static Tree getTreeBetweenPositions(Tree tree, int position, int endPosition,String... type) {
		for (Tree t: tree.preOrder()) {
			if (t.getPos() >= position && t.getEndPos() <= endPosition)
				if (isFromType(t, type))
					return t;
		}
		return null;
	}
	public static Tree getTreeBetweenPositionsSecure(Tree tree, int position, int endPosition,String type, String parentType, String label) {
		for (Tree t: tree.preOrder()) {
			if (t.getPos() == position && t.getEndPos() == endPosition)
				if (isFromType(t, type) && t.getLabel().equals(label)) {
					String t_parentType = (t.getParent() != null) ? t.getParent().getType().name : "";
					if (t_parentType.equals(parentType))
						return t;
				}
		}
		return null;
	}

	public static Tree findChildByType(Tree tree, String type) {
		if (!tree.getChildren().isEmpty())
		{
			for (Tree child: tree.getChildren()) {
				if (isFromType(child, type))
					return child;
			}
		}
		return null;
	}

	public static Tree findChildByTypeAndLabel(Tree tree, String type,String label) {
		if (!tree.getChildren().isEmpty())
		{
			for (Tree child: tree.getChildren()) {
				if (isFromType(child, type) && child.getLabel().equals(label))
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
		if (isFromType(tree, Constants.BLOCK))
			if (tree.getChildren().size() != 0) return null;
		Tree copy = makeDefaultTree(tree);
		cpyMap.put(copy,tree);
		if (isFromType(tree, Constants.ANONYMOUS_CLASS_DECLARATION)) return copy;
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

	public static org.apache.commons.lang3.tuple.Pair<Tree, Tree> populateLeftAndRightBasedOnTheFirstChildOfType(Tree srcTree, Tree dstTree, String[] types)
	{
		Tree srcRes = null;
		Tree dstRes = null;
		for (String type : types) {
			srcRes = TreeUtilFunctions.findFirstByType(srcTree, type);
			dstRes = TreeUtilFunctions.findFirstByType(dstTree, type);
			if (srcRes != null && dstRes != null)
				break;
			else {
				//In case, only one of them is null. We want to avoid this scenario (unless we get more examples)
				srcRes = null;
				dstRes = null;
			}
		}
		return org.apache.commons.lang3.tuple.Pair.of(srcRes,dstRes);
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
		if (isFromType(tree, matchingType))
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
	public static boolean isPartOf(Tree srcSubTree, String type) {
		if (srcSubTree.getType().name.equals(type))
			return true;
		if (srcSubTree.getParent() == null) return false;
		return isPartOf(srcSubTree.getParent(), type);
	}

	public static boolean isStatement(String type){
		switch (type){
			case Constants.ASSERT_STATEMENT: //Leaf
			case Constants.BLOCK: // Composite
			case Constants.BREAK_STATEMENT: //Leaf
			case Constants.CONSTRUCTOR_INVOCATION: //leaf
			case Constants.CONTINUE_STATEMENT: //leaf
			case Constants.DO_STATEMENT: //composite
			case Constants.EMPTY_STATEMENT: //leaf
			case Constants.ENHANCED_FOR_STATEMENT: //composite
			case Constants.EXPRESSION_STATEMENT: //leaf
			case Constants.FOR_STATEMENT: //composite
			case Constants.IF_STATEMENT: //composite
			case Constants.LABELED_STATEMENT: //composite
			case Constants.RETURN_STATEMENT: //leaf
			case Constants.SUPER_CONSTRUCTOR_INVOCATION: //leaf
			case Constants.SWITCH_CASE: //leaf
			case Constants.SWITCH_STATEMENT: //composite
			case Constants.SYNCHRONIZED_STATEMENT: //composite
			case Constants.THROW_STATEMENT://leaf
			case Constants.TRY_STATEMENT: //composite
			case Constants.TYPE_DECLARATION_STATEMENT: //composite!!!!!!
			case Constants.VARIABLE_DECLARATION_STATEMENT: //leaf
			case Constants.WHILE_STATEMENT: //composite
				return true;
			default:
				return false;
		}   //Update the jdt version (website)
	}

	public static boolean isPartOfJavadoc(Tree srcSubTree) {
		if (isFromType(srcSubTree, Constants.JAVA_DOC))
			return true;
		if (srcSubTree.getParent() == null) return false;
		return isPartOfJavadoc(srcSubTree.getParent());
	}

	public static List<Tree> findVariable(Tree inputTree, String variableName) {
		if (inputTree == null) return null;
		boolean _seen = false;
		List<Tree> refs = new ArrayList<>();
		for (Tree tree : inputTree.preOrder()) {
			if (isFromType(tree, Constants.SIMPLE_NAME) && tree.getLabel().equals(variableName))
			{
				refs.add(tree);
				_seen = true;
			}
		}
		return refs;
	}
	public static boolean hasSameTypeAndLabel(Tree t1, Tree t2) {
		return hasSameType(t1,t2) && t1.getLabel().equals(t2.getLabel());
	}
	public static boolean hasSameType(Tree t1, Tree t2){
		return isFromType(t1, t2.getType().name);
	}
	public static boolean isIsomorphicTo(Tree t1, Tree t2) {
		if (!hasSameTypeAndLabel(t1,t2))
			return false;

		if (t1.getChildren().size() != t2.getChildren().size())
			return false;

		for (int i = 0; i < t1.getChildren().size(); i++)  {
			boolean isChildrenIsomophic = isIsomorphicTo(t1.getChild(i),(t2.getChild(i)));
			if (!isChildrenIsomophic)
				return false;
		}

		return true;
	}

    public static boolean areBothFromThisType(Mapping mapping, String simpleName) {
        return areBothFromThisType(mapping.first,mapping.second, simpleName);
    }
	public static boolean areBothFromThisType(Tree t1, Tree t2, String type) {
		return isFromType(t1, type)
				&& isFromType(t2, type);
	}

	public static boolean isFromType(Tree t1, String... type) {
        if (t1 == null) return false;
        for (String t: type)
		    if (t1.getType().name.equals(t))
                return true;
        return false;
	}

	public static Tree findFirstByType(Tree inputTree, String typeName) {
		if (inputTree == null) return null;
		Tree fieldAnnotation = null;
		for (Tree child : inputTree.getChildren()) {
			if (isFromType(child, typeName)) {
				fieldAnnotation = child;
				break;
			}
		}
		return fieldAnnotation;
	}
}
