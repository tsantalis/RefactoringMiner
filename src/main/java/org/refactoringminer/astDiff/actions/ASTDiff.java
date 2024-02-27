package org.refactoringminer.astDiff.actions;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.gumtreediff.matchers.MappingStore;
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
	private ExtendedMultiMappingStore mappings;
	private ExtendedOnlyRootsClassifier classifier;

	public ASTDiff(String srcPath, String dstPath, TreeContext src, TreeContext dst, ExtendedMultiMappingStore mappings) {
		super(src, dst, new MappingStore(src.getRoot(),dst.getRoot()), new EditScript());
		this.srcPath = srcPath;
		this.dstPath = dstPath;
		this.mappings = mappings;
	}

	public ASTDiff(String srcPath, String dstPath, TreeContext src, TreeContext dst, ExtendedMultiMappingStore mappings, EditScript ed) {
		super(src, dst, new MappingStore(src.getRoot(),dst.getRoot()), new EditScript());
		this.srcPath = srcPath;
		this.dstPath = dstPath;
		this.mappings = mappings;
		finalizeEditScript(ed);
	}

	/**
	 * @deprecated
	 * This method is going to be replaced due to the misleading name
	 * <p> Use {@link ASTDiff#getAllMappings()} instead.
	 * @return {@link ExtendedMultiMappingStore} for the current diff which contains all the mappings
	 */
	@Deprecated(forRemoval = true)
	public ExtendedMultiMappingStore getMappings() {
		return getAllMappings();
	}

	public ExtendedMultiMappingStore getAllMappings() {
		return mappings;
	}

	public String getSrcPath() {
		return srcPath;
	}

	public String getDstPath() {
		return dstPath;
	}

	public Set<Tree> getDeletedSrcTrees() {
		return classifier.getDeletedSrcs();
	}

	public Set<Tree> getAddedDstTrees() {
		return classifier.getInsertedDsts();
	}

	public void computeVanillaEditScript() {
		EditScript newEditScript = new com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator().computeActions(this.getAllMappings().getMonoMappingStore());
		finalizeEditScript(newEditScript);
	}
	public void computeEditScript(Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		EditScript newEditScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings,parentContextMap,childContextMap);
		processMultiMappings(mappings, newEditScript);
		finalizeEditScript(newEditScript);
	}

	private void finalizeEditScript(EditScript newEditScript) {
		for(Action action : newEditScript)
			editScript.add(action);
		this.classifier = new ExtendedOnlyRootsClassifier(this);
		this.classifier.classify();
	}

	private static void processMultiMappings(ExtendedMultiMappingStore mappings, EditScript editScript) {
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
