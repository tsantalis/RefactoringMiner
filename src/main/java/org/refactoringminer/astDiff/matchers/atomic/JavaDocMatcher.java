package org.refactoringminer.astDiff.matchers.atomic;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLJavadoc;
import org.refactoringminer.astDiff.matchers.BasicTreeMatcher;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-05-22*/
public class JavaDocMatcher implements IExtendedMatcher {

    private final UMLJavadoc srcUMLJavaDoc;
    private final UMLJavadoc dstUMLJavaDoc;

    public JavaDocMatcher(UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc) {
        this.srcUMLJavaDoc = srcUMLJavaDoc;
        this.dstUMLJavaDoc = dstUMLJavaDoc;
    }

    private void processJavaDocs(Tree srcTree, Tree dstTree, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, ExtendedMultiMappingStore mappingStore) {
        if (srcUMLJavaDoc != null && dstUMLJavaDoc != null) {
            Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLJavaDoc.getLocationInfo());
            Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLJavaDoc.getLocationInfo());
            if (srcJavaDocNode == null || dstJavaDocNode == null) return;
            if (srcUMLJavaDoc.equalText(dstUMLJavaDoc) && srcJavaDocNode.isIsoStructuralTo(dstJavaDocNode)) {
                mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
            } else {
                new BasicTreeMatcher().match(srcJavaDocNode,dstJavaDocNode,mappingStore);
                mappingStore.addMapping(srcJavaDocNode,dstJavaDocNode);
            }
        }
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processJavaDocs(srcTree,dstTree,srcUMLJavaDoc,dstUMLJavaDoc,mappingStore);
    }
}
