package org.refactoringminer.astDiff.models;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.actions.classifier.ExtendedOnlyRootsClassifier;
import org.refactoringminer.astDiff.actions.editscript.ExtendedEditScriptGenerator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
		finalizeEditScript(new SimplifiedChawatheScriptGenerator().computeActions(this.getAllMappings().getMonoMappingStore()));
	}
	public void computeEditScript(Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap, ExtendedEditScriptGenerator extendedEditScriptGenerator) {
		finalizeEditScript(extendedEditScriptGenerator.computeActions(mappings,parentContextMap,childContextMap));
	}

	private void finalizeEditScript(EditScript newEditScript) {
		for(Action action : newEditScript)
			editScript.add(action);
		this.classifier = new ExtendedOnlyRootsClassifier(this);
		this.classifier.classify();
	}


	public TreeClassifier createAllNodeClassifier() {
		//TODO: Have to write our own version of AllNodesClassifier
		return new AllNodesClassifier(this);
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
