package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.diff.UMLJavadocDiff;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.BasicTreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-05-22*/
public class JavaDocMatcher implements TreeMatcher {

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
                UMLJavadocDiff diff = new UMLJavadocDiff(srcUMLJavaDoc, dstUMLJavaDoc);
                for(Pair<UMLDocElement, UMLDocElement> pair : diff.getCommonDocElements()) {
            		Tree src = TreeUtilFunctions.findByLocationInfo(srcTree,pair.getLeft().getLocationInfo());
                    Tree dst = TreeUtilFunctions.findByLocationInfo(dstTree,pair.getRight().getLocationInfo());
                    if(!mappingStore.isSrcMapped(src) || !mappingStore.isDstMapped(dst)) {
                    	mappingStore.addMapping(src,dst);
                    }
            	}
                mappingStore.addMapping(srcJavaDocNode,dstJavaDocNode);
            }
        }
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processJavaDocs(srcTree,dstTree,srcUMLJavaDoc,dstUMLJavaDoc,mappingStore);
    }
}
