
package org.refactoringminer.astDiff.actions;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;

import java.util.Map;
import java.util.Set;

public class ASTDiff {

//    public String srcPath;
//    public String dstPath;

    private TreeContext srcTC;

    private TreeContext dstTC;

    private ExtendedMultiMappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    private EditScript editScript = new EditScript();

    public TreeContext getDstTC() {
        return dstTC;
    }

    public TreeContext getSrcTC() {
        return srcTC;
    }

    public ExtendedMultiMappingStore getMappings() {
        return mappings;
    }

    public EditScript getEditScript() {
        return editScript;
    }

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public ASTDiff(TreeContext src, TreeContext dst, ExtendedMultiMappingStore mappings) {
//        this.srcPath = srcPath;
//        this.dstPath = dstPath;
        this.srcTC = src;
        this.dstTC = dst;
        this.mappings = mappings;
    }
    public void computeEditScript(Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap)
    {
        this.editScript = new SimplifiedChawatheScriptGenerator().computeActions(this.mappings,parentContextMap,childContextMap);
        processMultiMaps(this.editScript);
    }

    private void processMultiMaps(EditScript editScript) {
//        ArrayList<Action> multiMoves = new ArrayList<>();
        Map<Tree, Set<Tree>> dstToSrcMultis = mappings.dstToSrcMultis();
        MultiMoveActionGenerator multiMoveActionGenerator = new MultiMoveActionGenerator();

        for(Map.Entry<Tree, Set<Tree>> entry : dstToSrcMultis.entrySet())
        {
            Set<Tree> srcTrees = entry.getValue();
            Set<Tree> dstTrees = mappings.getDstForSrc(srcTrees.iterator().next());
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
        return new OnlyRootsClassifier(this);
    }
}
