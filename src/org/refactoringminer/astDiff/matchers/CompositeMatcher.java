package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.*;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.HashMap;
import java.util.Map;

public class CompositeMatcher extends BasicTreeMatcher implements TreeMatcher {
    Map<Tree,Tree> cpyToSrc;
    Map<Tree,Tree> cpyToDst;
    @Override
    public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
//        if (true) return;
        String labeled = "LabeledStatement";
        if (src.getType().name.equals(labeled) && dst.getType().name.equals(labeled))
            mappingStore.addMapping(src.getChild(0),dst.getChild(0));
        else
            compositeMatcher(src,dst,abstractCodeMapping,mappingStore);
    }

    @Override
    public void match(Tree src, Tree dst, AbstractStatement st1, AbstractStatement st2, ExtendedMultiMappingStore mappingStore) {
//        if (true) return;
        if (!(st1 instanceof CompositeStatementObject) || !(st2 instanceof CompositeStatementObject))
        {
            // FIXME: 9/27/2022 MUST FIX!!
            return;
        }
        compositeMatcher(src,dst, (CompositeStatementObject) st1, (CompositeStatementObject) st2,mappingStore);
    }

    private void compositeMatcher(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
//        if (true) return;
//        basicMatcher(src,dst,mappingStore);
//        step1(src,dst,mappingStore);
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        CompositeStatementObject fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
        CompositeStatementObject fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();

        cpyToSrc = new HashMap<>();
        cpyToDst = new HashMap<>();
        Tree srcFakeTree = makeFakeTree(src,fragment1,cpyToSrc);
        Tree dstFakeTree = makeFakeTree(dst,fragment2,cpyToDst);
        ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null);
        basicMatcher(srcFakeTree,dstFakeTree,tempMapping);
        for(Mapping mapping : tempMapping) {
            if (mapping.first == srcFakeTree) continue;
            mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
        }
    }

    private void compositeMatcher(Tree src, Tree dst, CompositeStatementObject fragment1, CompositeStatementObject fragment2, ExtendedMultiMappingStore mappingStore) {
        cpyToSrc = new HashMap<>();
        cpyToDst = new HashMap<>();
        Tree srcFakeTree = makeFakeTree(src,fragment1,cpyToSrc);
        Tree dstFakeTree = makeFakeTree(dst,fragment2,cpyToDst);
        ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null);
        basicMatcher(srcFakeTree,dstFakeTree,tempMapping);
        for(Mapping mapping : tempMapping) {
            if (mapping.first == srcFakeTree) continue;
            mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
        }
    }

    private Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap) {
        Tree cpy = TreeUtilFunctions.makeDefaultTree(tree);
        cpyMap.put(cpy,tree);
//        List<Tree> seen = new ArrayList<>();
        for (AbstractExpression abstractExpression : fragment.getExpressions())
        {
            Tree expTree = TreeUtilFunctions.findByLocationInfo(tree,abstractExpression.getLocationInfo());
//            seen.add(expTree);
            Tree expCopy =  TreeUtilFunctions.deepCopyWithMap(expTree,cpyMap);
            cpy.addChild(expCopy);
        }
        for (VariableDeclaration variableDeclaration : fragment.getVariableDeclarations()) {
            Tree varTree = TreeUtilFunctions.findByLocationInfo(tree, variableDeclaration.getLocationInfo());
//            if (!seen.contains(varTree))
//            {
            Tree varCopy = TreeUtilFunctions.deepCopyWithMap(varTree, cpyMap);
            cpy.addChild(varCopy);
//            }
        }
        return cpy;
    }
}
