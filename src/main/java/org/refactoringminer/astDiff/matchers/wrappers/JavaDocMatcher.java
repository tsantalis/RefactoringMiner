package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLTagElement;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import static org.refactoringminer.astDiff.utils.TreeUtilFunctions.areBothFromThisType;
import static org.refactoringminer.astDiff.utils.TreeUtilFunctions.isFromType;

/* Created by pourya on 2024-05-22*/
public class JavaDocMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLJavadoc srcUMLJavaDoc;
    private final UMLJavadoc dstUMLJavaDoc;

    public JavaDocMatcher(UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc) {
        this.srcUMLJavaDoc = srcUMLJavaDoc;
        this.dstUMLJavaDoc = dstUMLJavaDoc;
    }

    public JavaDocMatcher(OptimizationData optimizationData, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc) {
        super(optimizationData);
        this.srcUMLJavaDoc = srcUMLJavaDoc;
        this.dstUMLJavaDoc = dstUMLJavaDoc;
    }

    private void processJavaDocs(Tree srcTree, Tree dstTree, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, ExtendedMultiMappingStore mappingStore) {
        if (srcUMLJavaDoc != null && dstUMLJavaDoc != null) {
            Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLJavaDoc.getLocationInfo());
            Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLJavaDoc.getLocationInfo());
            if (srcJavaDocNode == null || dstJavaDocNode == null) return;
            UMLJavadocDiff diff = new UMLJavadocDiff(srcUMLJavaDoc, dstUMLJavaDoc);
            mappingStore.addMapping(srcJavaDocNode,dstJavaDocNode); // Match the entire javadoc subtree node (parent)
            if (srcJavaDocNode.isIsoStructuralTo(dstJavaDocNode) & !diff.isManyToManyReformat()) {
                mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
            }
            else if(diff.getCommonTags().size() > 0 || diff.getCommonDocElements().size() > 0 || srcUMLJavaDoc.isEmpty() || dstUMLJavaDoc.isEmpty()) {
                new LeafMatcher().match(srcJavaDocNode,dstJavaDocNode,mappingStore);
                for (Pair<UMLTagElement, UMLTagElement> pair : diff.getCommonNestedTags()) {
                    Tree srcTag = TreeUtilFunctions.findByLocationInfo(srcTree,pair.getLeft().getLocationInfo());
                    Tree dstTag = TreeUtilFunctions.findByLocationInfo(dstTree,pair.getRight().getLocationInfo());
                    if (srcTag != null && dstTag != null) {
                        if (!mappingStore.isSrcMapped(srcTag) || !mappingStore.isDstMapped(dstTag) || diff.isManyToManyReformat()) {
                            optimizationData.getSubtreeMappings().addMappingRecursively(srcTag,dstTag);
                        }
                    }
                }
                for(Pair<UMLDocElement, UMLDocElement> pair : diff.getCommonDocElements()) {
            		Tree src = TreeUtilFunctions.findByLocationInfo(srcTree,pair.getLeft().getLocationInfo());
                    Tree dst = TreeUtilFunctions.findByLocationInfo(dstTree,pair.getRight().getLocationInfo());
                    if (src != null && dst != null) {
                        if (!mappingStore.isSrcMapped(src) || !mappingStore.isDstMapped(dst) || diff.isManyToManyReformat())
                        {
                            Tree srcTxt = null;
                            Tree dstTxt = null;
                            boolean matchParents = false;
                            if (isFromType(src, Constants.TAG_ELEMENT) && isFromType(dst, Constants.TEXT_ELEMENT)) {
                                srcTxt = src.getChild(0);
                                dstTxt = dst;
                            } else if (isFromType(src, Constants.TEXT_ELEMENT) && isFromType(dst, Constants.TAG_ELEMENT)) {
                                srcTxt = src;
                                dstTxt = dst.getChild(0);
                            }
                            else if (areBothFromThisType(src, dst, Constants.TEXT_ELEMENT)){
                                srcTxt = src;
                                dstTxt = dst;
                                matchParents = true;
                            }
                            if (srcTxt != null && dstTxt != null) {
                                optimizationData.getSubtreeMappings().addMapping(srcTxt, dstTxt);
                                if (matchParents)
                                {
                                    Tree srcTxtParent = srcTxt.getParent();
                                    Tree dstTxtParent = dstTxt.getParent();
                                    if (areBothFromThisType(srcTxtParent, dstTxtParent, Constants.TAG_ELEMENT))
                                        if (!mappingStore.isSrcMapped(srcTxtParent) && !mappingStore.isDstMapped(dstTxtParent))
                                            optimizationData.getSubtreeMappings().addMapping(srcTxtParent, dstTxtParent);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processJavaDocs(srcTree,dstTree,srcUMLJavaDoc,dstUMLJavaDoc,mappingStore);
    }
}
