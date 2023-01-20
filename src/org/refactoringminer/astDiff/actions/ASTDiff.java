package org.refactoringminer.astDiff.actions;

import java.util.*;

import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ASTDiff extends Diff {
	private String srcPath;
	private String dstPath;
	private String srcContents;
	private String dstContents;
	private ExtendedMultiMappingStore multiMappings;

	public ASTDiff(String srcPath, String dstPath, TreeContext src, TreeContext dst, ExtendedMultiMappingStore mappings) {
		super(src, dst, null, new EditScript());
		this.srcPath = srcPath;
		this.dstPath = dstPath;
		this.multiMappings = mappings;
	}

	/**
	 * @return A Set of Subtrees from the src AST which are not mapped
	 * This is not a getter. It calculates the entire set per each method call
	 * This method returns all the subtrees (not only the roots), the user might need to group them if all the children of one tree is included
	 */
	public Set<Tree> calculateNonMappedSrcSubTrees(){
		Set<Tree> treeSet = new LinkedHashSet<>();
		this.src.getRoot().preOrder().forEach(tree -> {if (!getMultiMappings().isSrcMapped(tree)) treeSet.add(tree);});
		return treeSet;
	}


	/**
	 * @return A Set of Subtrees from the dst AST which are not mapped
	 * This is not a getter. It calculates the entire set per each method call
	 * This method returns all the subtrees (not only the roots), the user might need to group them if all the children of one tree is included
	 *
	 */
	public Set<Tree> calculateNonMappedDstSubTrees(){
		Set<Tree> treeSet = new LinkedHashSet<>();
		this.dst.getRoot().preOrder().forEach(tree -> {if (!getMultiMappings().isDstMapped(tree)) treeSet.add(tree);});
		return treeSet;
	}

	public ExtendedMultiMappingStore getMultiMappings() {
		return multiMappings;
	}

	public String getSrcContents() {
		return srcContents;
	}

	public void setSrcContents(String srcContents) {
		this.srcContents = srcContents;
	}

	public String getDstContents() {
		return dstContents;
	}

	public void setDstContents(String dstContents) {
		this.dstContents = dstContents;
	}

	public String getSrcPath() {
		return srcPath;
	}

	public String getDstPath() {
		return dstPath;
	}

	public void computeEditScript(Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		EditScript newEditScript = new SimplifiedChawatheScriptGenerator().computeActions(multiMappings,parentContextMap,childContextMap);
		processMultiMappings(multiMappings, newEditScript);
		for(Action action : newEditScript) {
			editScript.add(action);
		}
	}

	private static void processMultiMappings(ExtendedMultiMappingStore mappings, EditScript editScript) {
		//ArrayList<Action> multiMoves = new ArrayList<>();
		Map<Tree, Set<Tree>> dstToSrcMultis = mappings.dstToSrcMultis();
		MultiMoveActionGenerator multiMoveActionGenerator = new MultiMoveActionGenerator();

		for(Map.Entry<Tree, Set<Tree>> entry : dstToSrcMultis.entrySet())
		{
			Set<Tree> srcTrees = entry.getValue();
			Set<Tree> dstTrees = mappings.getDsts(srcTrees.iterator().next());
			multiMoveActionGenerator.addMapping(srcTrees,dstTrees);
		}
		for (Action action : multiMoveActionGenerator.generate()) {
			editScript.add(action);
		}
	}

	/**
	 * Compute and return a root node classifier that indicates which node have
	 * been added/deleted/updated/moved. Only the root note is marked when a whole
	 * subtree has been subject to a same operation.
	 */
	public TreeClassifier createRootNodesClassifier() {
		ExtendedOnlyRootsClassifier classifier = new ExtendedOnlyRootsClassifier(this);
		classifier.classify();
		return classifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dstPath, srcPath);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ASTDiff other = (ASTDiff) obj;
		return Objects.equals(dstPath, other.dstPath) && Objects.equals(srcPath, other.srcPath);
	}
}
