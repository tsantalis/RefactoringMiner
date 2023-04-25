package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.*;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class CompositeMatcher implements TreeMatcher {

	@Override
	public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		String labeled = Constants.LABELED_STATEMENT;
		if (src.getType().name.equals(labeled) && dst.getType().name.equals(labeled))
			mappingStore.addMapping(src.getChild(0),dst.getChild(0));
		else
			compositeMatcher(src,dst,abstractCodeMapping,mappingStore);
	}

	@Override
	public void match(Tree src, Tree dst, AbstractCodeFragment st1, AbstractCodeFragment st2, ExtendedMultiMappingStore mappingStore) {
		if ((st1 instanceof CompositeStatementObject) &&  (st2 instanceof CompositeStatementObject)) {
			compositeMatcher(src, dst, (CompositeStatementObject) st1, (CompositeStatementObject) st2, mappingStore);
			return;
		}
		//Corner cases;
		if (!(st1 instanceof CompositeStatementObject) &&
				(st2 instanceof CompositeStatementObject)) {
			CompositeStatementObject fragment2 = (CompositeStatementObject) st2;
			for (AbstractExpression expression : fragment2.getExpressions()) {
				Tree dstExpTree = TreeUtilFunctions.findByLocationInfo(dst, expression.getLocationInfo());
				new LeafMatcher(false).match(src,dstExpTree,st1,expression,mappingStore);
			}
		} else if ((st1 instanceof CompositeStatementObject) &&
				!(st2 instanceof CompositeStatementObject)) {
			CompositeStatementObject fragment1 = (CompositeStatementObject) st1;
			for (AbstractExpression expression : fragment1.getExpressions()) {
				Tree srcExpTree = TreeUtilFunctions.findByLocationInfo(src, expression.getLocationInfo());
				new LeafMatcher(false).match(srcExpTree,dst,expression,st2,mappingStore);
			}
		}

	}

	private void compositeMatcher(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		CompositeStatementObject fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
		CompositeStatementObject fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();
		process(src, dst, mappingStore, fragment1, fragment2);
	}

	private void process(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore, CompositeStatementObject fragment1, CompositeStatementObject fragment2) {
		Map<Tree, Tree> cpyToSrc = new HashMap<>();
		Map<Tree, Tree> cpyToDst = new HashMap<>();
		Tree srcFakeTree = makeFakeTree(src,fragment1, cpyToSrc);
		Tree dstFakeTree = makeFakeTree(dst,fragment2, cpyToDst);
		ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null);
		new BasicTreeMatcher().match(srcFakeTree,dstFakeTree,null,tempMapping);
		for(Mapping mapping : tempMapping) {
			if (mapping.first == srcFakeTree) continue;
			mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
		}
	}

	private void compositeMatcher(Tree src, Tree dst, CompositeStatementObject fragment1, CompositeStatementObject fragment2, ExtendedMultiMappingStore mappingStore) {
		process(src, dst, mappingStore, fragment1, fragment2);
	}

	private Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap) {
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
