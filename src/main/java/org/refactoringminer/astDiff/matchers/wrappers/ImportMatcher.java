package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.diff.UMLImportListDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
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
        if (!commonImports.isEmpty()) {
            for (org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : commonImports) {
                Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, pair.getLeft().getLocationInfo());
                Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, pair.getRight().getLocationInfo());
                if (srcImportStatement != null && dstImportStatement != null) {
                    mappingStore.addMappingRecursively(srcImportStatement, dstImportStatement);
                    handleParent(mappingStore, srcImportStatement, dstImportStatement);
                }
            }
        }
        //Grouped Imports
        for (Map.Entry<Set<UMLImport>, UMLImport> setUMLImportEntry : importDiffList.getGroupedImports().entrySet()) {
            Set<UMLImport> srcImportSet = setUMLImportEntry.getKey();
            UMLImport dstUMLImport = setUMLImportEntry.getValue();
            Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLImport.getLocationInfo());
            for (UMLImport srcUMLImport : srcImportSet) {
                Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLImport.getLocationInfo());
                if (srcImportStatement != null && dstImportStatement != null)
                    mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //UnGrouped Imports
        for (Map.Entry<UMLImport, Set<UMLImport>> umlImportSetEntry : importDiffList.getUnGroupedImports().entrySet()) {
            UMLImport srcUMLImport = umlImportSetEntry.getKey();
            Set<UMLImport> dstImportSet = umlImportSetEntry.getValue();
            Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLImport.getLocationInfo());
            for (UMLImport dstUMLImport : dstImportSet) {
                Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLImport.getLocationInfo());
                if (srcImportStatement != null && dstImportStatement != null)
                    mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //Changed Imports
        for(org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : importDiffList.getChangedImports()) {
            Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, pair.getLeft().getLocationInfo());
            Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, pair.getRight().getLocationInfo());
            if (srcImportStatement != null && dstImportStatement != null) {
                mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
                handleParent(mappingStore, srcImportStatement, dstImportStatement);
            }
        }
    }

    private void handleParent(ExtendedMultiMappingStore mappingStore, Tree srcImportStatement, Tree dstImportStatement) {
        //python handling
        if (srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(Constants.get().IMPORT_FROM_STATEMENT) &&
                dstImportStatement.getParent().getType().name.equals(Constants.get().IMPORT_FROM_STATEMENT)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
            com.github.gumtreediff.utils.Pair<Tree, Tree> froms = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().FROM_KEYWORD);
            if (froms != null) {
                mappingStore.addMapping(froms.first,froms.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().RELATIVE_IMPORT);
            if (relatives != null) {
                mappingStore.addMappingRecursively(relatives.first,relatives.second);
            }
            else {
                relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().RELATIVE_IMPORT_DOTTED_NAME);
                if (relatives != null) {
                    mappingStore.addMappingRecursively(relatives.first,relatives.second);
                }
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> lineContinuations = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().LINE_CONTINUATION);
            if (lineContinuations != null) {
                mappingStore.addMappingRecursively(lineContinuations.first,lineContinuations.second);
            }
        }
        else if (srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(Constants.get().IMPORT_DECLARATION) &&
                dstImportStatement.getParent().getType().name.equals(Constants.get().IMPORT_DECLARATION)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
        }
        else if(srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(Constants.get().FUTURE_IMPORT_STATEMENT) &&
                dstImportStatement.getParent().getType().name.equals(Constants.get().FUTURE_IMPORT_STATEMENT)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
            com.github.gumtreediff.utils.Pair<Tree, Tree> froms = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().FROM_KEYWORD);
            if (froms != null) {
                mappingStore.addMapping(froms.first,froms.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), Constants.get().FUTURE);
            if (relatives != null) {
                mappingStore.addMappingRecursively(relatives.first,relatives.second);
            }
        }
    }
}
