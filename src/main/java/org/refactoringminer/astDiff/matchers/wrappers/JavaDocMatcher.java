package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLTagElement;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.Optional;

import static org.refactoringminer.astDiff.utils.TreeUtilFunctions.areBothFromThisType;
import static org.refactoringminer.astDiff.utils.TreeUtilFunctions.isFromType;

/* Created by pourya on 2024-05-22*/
public class JavaDocMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLJavadoc srcUMLJavaDoc;
    private final UMLJavadoc dstUMLJavaDoc;
    private final Optional<UMLJavadocDiff> umlJavadocDiff;

    public JavaDocMatcher(UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, Optional<UMLJavadocDiff> umlJavadocDiff) {
    	if(srcUMLJavaDoc == null && umlJavadocDiff.isPresent()) {
        	this.srcUMLJavaDoc = umlJavadocDiff.get().getJavadocBefore();
        }
        else {
        	this.srcUMLJavaDoc = srcUMLJavaDoc;
        }
        if(dstUMLJavaDoc == null && umlJavadocDiff.isPresent()) {
        	this.dstUMLJavaDoc = umlJavadocDiff.get().getJavadocAfter();
        }
        else {
        	this.dstUMLJavaDoc = dstUMLJavaDoc;
        }
        this.umlJavadocDiff = umlJavadocDiff;
    }

    public JavaDocMatcher(OptimizationData optimizationData, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, Optional<UMLJavadocDiff> umlJavadocDiff) {
        super(optimizationData);
        if(srcUMLJavaDoc == null && umlJavadocDiff.isPresent()) {
        	this.srcUMLJavaDoc = umlJavadocDiff.get().getJavadocBefore();
        }
        else {
        	this.srcUMLJavaDoc = srcUMLJavaDoc;
        }
        if(dstUMLJavaDoc == null && umlJavadocDiff.isPresent()) {
        	this.dstUMLJavaDoc = umlJavadocDiff.get().getJavadocAfter();
        }
        else {
        	this.dstUMLJavaDoc = dstUMLJavaDoc;
        }
        this.umlJavadocDiff = umlJavadocDiff;
    }

    private void processJavaDocs(Tree srcTree, Tree dstTree, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, ExtendedMultiMappingStore mappingStore) {
        if (srcUMLJavaDoc != null && dstUMLJavaDoc != null) {
            Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,
                    umlJavadocDiff.isPresent() ?
                    umlJavadocDiff.get().getJavadocBefore().getLocationInfo() :
                    srcUMLJavaDoc.getLocationInfo());
            Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,
                    umlJavadocDiff.isPresent() ?
                    umlJavadocDiff.get().getJavadocAfter().getLocationInfo() :
                    dstUMLJavaDoc.getLocationInfo());
            if (srcJavaDocNode == null || dstJavaDocNode == null)
                return;
            if (srcJavaDocNode.isIsoStructuralTo(dstJavaDocNode)) {
                if (umlJavadocDiff.isPresent() && umlJavadocDiff.get().isManyToManyReformat()) {
                    mappingStore.addMappingRecursively(srcJavaDocNode, dstJavaDocNode);
                    return;
                }
            }
            if (umlJavadocDiff.isEmpty()) return;
            mappingStore.addMapping(srcJavaDocNode,dstJavaDocNode); // Match the entire javadoc subtree node (parent)
            UMLJavadocDiff diff = umlJavadocDiff.get();
            if(!diff.getCommonTags().isEmpty() || !diff.getCommonDocElements().isEmpty() || srcUMLJavaDoc.isEmpty() || dstUMLJavaDoc.isEmpty()) {
                MappingStore gtSimpleMappings = new CompositeMatchers.SimpleGumtree().match(srcJavaDocNode, dstJavaDocNode);
                mappingStore.add(gtSimpleMappings);
            	for (Pair<UMLTagElement, UMLTagElement> pair : diff.getCommonTags()) {
                    Tree srcTag = TreeUtilFunctions.findByLocationInfo(srcTree,pair.getLeft().getLocationInfo());
                    Tree dstTag = TreeUtilFunctions.findByLocationInfo(dstTree,pair.getRight().getLocationInfo());
                    if (srcTag != null && dstTag != null) {
//                        if (!mappingStore.isSrcMapped(srcTag) || !mappingStore.isDstMapped(dstTag) || diff.isManyToManyReformat())
                        {
                            if (srcTag.isIsoStructuralTo(dstTag))
                                optimizationData.getSubtreeMappings().addMappingRecursively(srcTag,dstTag);
                            else
                                optimizationData.getSubtreeMappings().addMapping(srcTag,dstTag);
                        }
                    }
                }
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
                            else if (areBothFromThisType(src, dst, Constants.SIMPLE_NAME))
                            {
                                optimizationData.getSubtreeMappings().addMapping(src, dst);
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
                for (UMLDocElement deletedDocElement : diff.getDeletedDocElements()) {
                    Tree srcDocElement = TreeUtilFunctions.findByLocationInfo(srcTree, deletedDocElement.getLocationInfo());
                    for (Mapping gtSimpleMapping : gtSimpleMappings) {
                        if (gtSimpleMapping.first.equals(srcDocElement)) {
                            mappingStore.removeMapping(gtSimpleMapping.first, gtSimpleMapping.second);
                        }
                    }
                }
                for (UMLDocElement addedDocElements : diff.getAddedDocElements()) {
                    Tree dstDocElement = TreeUtilFunctions.findByLocationInfo(dstTree, addedDocElements.getLocationInfo());
                    for (Mapping gtSimpleMapping : gtSimpleMappings) {
                        if (gtSimpleMapping.second.equals(dstDocElement)) {
                            mappingStore.removeMapping(gtSimpleMapping.first, gtSimpleMapping.second);
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
