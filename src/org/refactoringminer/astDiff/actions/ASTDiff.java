package org.refactoringminer.astDiff.actions;

import java.util.Map;
import java.util.Set;

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
	private EditScript editScript; //field hiding to make it non-final
	private ExtendedMultiMappingStore multiMappings;

	public ASTDiff(TreeContext src, TreeContext dst, ExtendedMultiMappingStore mappings) {
		super(src, dst, mappings.getMonoMappingStore(), null);
		this.multiMappings = mappings;
	}

	public ExtendedMultiMappingStore getMultiMappings() {
		return multiMappings;
	}

	public EditScript getEditScript() {
		return editScript;
	}

	public void computeEditScript(Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		this.editScript = new SimplifiedChawatheScriptGenerator().computeActions(multiMappings,parentContextMap,childContextMap);
		processMultiMappings(multiMappings, editScript);
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
		return new ExtendedOnlyRootsClassifier(this);
	}
}
