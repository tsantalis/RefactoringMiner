package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.*;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.HashMap;
import java.util.Map;

/** Use this matcher when both code fragments are {@link gr.uom.java.xmi.decomposition.CompositeStatementObject}. <br>
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class CompositeMatcher extends BasicTreeMatcher implements TreeMatcher {

	private final CompositeStatementObject fragment1;
	private final CompositeStatementObject fragment2;

	public CompositeMatcher(AbstractCodeMapping abstractCodeMapping) {
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
		fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();
	}

	public CompositeMatcher(CompositeStatementObject fragment1, CompositeStatementObject fragment2) {
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
	}

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		String labeled = Constants.LABELED_STATEMENT;
		if (src.getType().name.equals(labeled) && dst.getType().name.equals(labeled))
			mappingStore.addMapping(src.getChild(0),dst.getChild(0));
		else
			process(src,dst,mappingStore);
	}
	private void process(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		Map<Tree, Tree> cpyToSrc = new HashMap<>();
		Map<Tree, Tree> cpyToDst = new HashMap<>();
		Tree srcFakeTree = makeFakeTree(src,fragment1, cpyToSrc);
		Tree dstFakeTree = makeFakeTree(dst,fragment2, cpyToDst);
		ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null);
		super.match(srcFakeTree,dstFakeTree,tempMapping);
		for(Mapping mapping : tempMapping) {
			if (mapping.first == srcFakeTree) continue;
			mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
		}
	}

	private static Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap) {
		Tree cpy = TreeUtilFunctions.makeDefaultTree(tree);
		cpyMap.put(cpy,tree);
		for (AbstractExpression abstractExpression : fragment.getExpressions()) {
			Tree expTree = TreeUtilFunctions.findByLocationInfo(tree,abstractExpression.getLocationInfo());
			Tree expCopy =  TreeUtilFunctions.deepCopyWithMap(expTree,cpyMap);
			cpy.addChild(expCopy);
		}
		for (VariableDeclaration variableDeclaration : fragment.getVariableDeclarations()) {
			Tree varTree = TreeUtilFunctions.findByLocationInfo(tree, variableDeclaration.getLocationInfo());
			Tree varCopy = TreeUtilFunctions.deepCopyWithMap(varTree, cpyMap);
			cpy.addChild(varCopy);
		}
		return cpy;
	}
}
