package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.diff.UMLImportListDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

import java.util.Map;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class ImportMatcher implements TreeMatcher {

    private final UMLImportListDiff importDiffList;

    public ImportMatcher(UMLImportListDiff importDiffList) {
        this.importDiffList = importDiffList;
    }

    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processImports(srcTree, dstTree, importDiffList, mappingStore);
    }

    private void processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList, ExtendedMultiMappingStore mappingStore) {
        if (importDiffList == null) return;
        Set<Pair<UMLImport, UMLImport>> commonImports = importDiffList.getCommonImports();
        String searchingType = Constants.IMPORT_DECLARATION;
        if (!commonImports.isEmpty()) {
            for (org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : commonImports) {
                Tree srcImportStatement = findImportByTypeAndLabel(srcTree, searchingType, pair.getLeft());
                Tree dstImportStatement = findImportByTypeAndLabel(dstTree, searchingType, pair.getRight());
                if (srcImportStatement != null && dstImportStatement != null)
                    mappingStore.addMappingRecursively(srcImportStatement, dstImportStatement);
            }
        }
        //Grouped Imports
        for (Map.Entry<Set<UMLImport>, UMLImport> setUMLImportEntry : importDiffList.getGroupedImports().entrySet()) {
            Set<UMLImport> srcImportSet = setUMLImportEntry.getKey();
            UMLImport dstImport = setUMLImportEntry.getValue();
            Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,dstImport);
            for (UMLImport srcUMLImport : srcImportSet) {
                Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,srcUMLImport);
                mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //UnGrouped Imports
        for (Map.Entry<UMLImport, Set<UMLImport>> umlImportSetEntry : importDiffList.getUnGroupedImports().entrySet()) {
            UMLImport srcImport = umlImportSetEntry.getKey();
            Set<UMLImport> dstImportSet = umlImportSetEntry.getValue();
            Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,srcImport);
            for (UMLImport dstUMLImport : dstImportSet) {
                Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,dstUMLImport);
                mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //Changed Imports
        for(org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : importDiffList.getChangedImports()) {
            Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,pair.getLeft());
            Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,pair.getRight());
            mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
        }
    }

    private Tree findImportByTypeAndLabel(Tree inputTree, String searchingType, UMLImport label) {
        for (Tree treeNode: inputTree.getChildren()) {
            if (treeNode.getType().name.equals(searchingType)) {
                if (treeNode.getChild(0).getLabel().equals(label.getName()) && treeNode.getPos() == label.getLocationInfo().getStartOffset()) //getChild 0 might be problematic
                    if (label.isOnDemand()) {
                        if (treeNode.getChild(0).getEndPos() + 3 == treeNode.getEndPos()) {
                            return treeNode;
                        }
                    } else {
                        return treeNode;
                    }
            }
        }
        return null;
    }

}
