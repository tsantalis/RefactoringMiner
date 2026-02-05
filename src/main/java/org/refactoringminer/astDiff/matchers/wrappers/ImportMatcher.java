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
    private final Constants LANG1;
    private final Constants LANG2;

    public ImportMatcher(UMLImportListDiff importDiffList, Constants LANG1, Constants LANG2) {
        this.importDiffList = importDiffList;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processImports(srcTree, dstTree, importDiffList, mappingStore);
    }

    private void processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList, ExtendedMultiMappingStore mappingStore) {
        if (importDiffList == null) return;
        Set<Pair<UMLImport, UMLImport>> commonImports = importDiffList.getCommonImports();
        if (!commonImports.isEmpty()) {
        	int counter = 0;
            for (org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : commonImports) {
                Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, pair.getLeft().getLocationInfo(), LANG1);
                Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, pair.getRight().getLocationInfo(), LANG2);
                boolean isLast = counter == commonImports.size()-1;
                if(isLast && srcImportStatement.getType().name.equals(LANG1.IMPORT_IDENTIFIER) && !dstImportStatement.getType().name.equals(LANG2.IMPORT_IDENTIFIER)) {
                	srcImportStatement = srcImportStatement.getParent();
                }
                if(isLast && !srcImportStatement.getType().name.equals(LANG1.IMPORT_IDENTIFIER) && dstImportStatement.getType().name.equals(LANG2.IMPORT_IDENTIFIER)) {
                	dstImportStatement = dstImportStatement.getParent();
                }
                if (srcImportStatement != null && dstImportStatement != null) {
                    mappingStore.addMappingRecursively(srcImportStatement, dstImportStatement);
                    handleParent(mappingStore, srcImportStatement, dstImportStatement);
                }
                counter++;
            }
        }
        //Grouped Imports
        for (Map.Entry<Set<UMLImport>, UMLImport> setUMLImportEntry : importDiffList.getGroupedImports().entrySet()) {
            Set<UMLImport> srcImportSet = setUMLImportEntry.getKey();
            UMLImport dstUMLImport = setUMLImportEntry.getValue();
            Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLImport.getLocationInfo(), LANG2);
            for (UMLImport srcUMLImport : srcImportSet) {
                Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLImport.getLocationInfo(), LANG1);
                if (srcImportStatement != null && dstImportStatement != null)
                    mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //UnGrouped Imports
        for (Map.Entry<UMLImport, Set<UMLImport>> umlImportSetEntry : importDiffList.getUnGroupedImports().entrySet()) {
            UMLImport srcUMLImport = umlImportSetEntry.getKey();
            Set<UMLImport> dstImportSet = umlImportSetEntry.getValue();
            Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLImport.getLocationInfo(), LANG1);
            for (UMLImport dstUMLImport : dstImportSet) {
                Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLImport.getLocationInfo(), LANG2);
                if (srcImportStatement != null && dstImportStatement != null)
                    mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
            }
        }
        //Changed Imports
        for(org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : importDiffList.getChangedImports()) {
            Tree srcImportStatement = TreeUtilFunctions.findByLocationInfo(srcTree, pair.getLeft().getLocationInfo(), LANG1);
            Tree dstImportStatement = TreeUtilFunctions.findByLocationInfo(dstTree, pair.getRight().getLocationInfo(), LANG2);
            if (srcImportStatement != null && dstImportStatement != null) {
                mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
                handleParent(mappingStore, srcImportStatement, dstImportStatement);
            }
        }
    }

    private void handleParent(ExtendedMultiMappingStore mappingStore, Tree srcImportStatement, Tree dstImportStatement) {
        //python handling
        if (srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(LANG1.IMPORT_FROM_STATEMENT) &&
                dstImportStatement.getParent().getType().name.equals(LANG2.IMPORT_FROM_STATEMENT)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
            com.github.gumtreediff.utils.Pair<Tree, Tree> froms = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.FROM_KEYWORD, LANG2.FROM_KEYWORD);
            if (froms != null) {
                mappingStore.addMapping(froms.first,froms.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.RELATIVE_IMPORT, LANG2.RELATIVE_IMPORT);
            if (relatives != null) {
                mappingStore.addMappingRecursively(relatives.first,relatives.second);
            }
            else {
                relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.RELATIVE_IMPORT_DOTTED_NAME, LANG2.RELATIVE_IMPORT_DOTTED_NAME);
                if (relatives != null) {
                    mappingStore.addMappingRecursively(relatives.first,relatives.second);
                }
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> lineContinuations = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.LINE_CONTINUATION, LANG2.LINE_CONTINUATION);
            if (lineContinuations != null) {
                mappingStore.addMappingRecursively(lineContinuations.first,lineContinuations.second);
            }
        }
        else if (srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(LANG1.IMPORT_DECLARATION) &&
                dstImportStatement.getParent().getType().name.equals(LANG2.IMPORT_DECLARATION)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
        }
        else if(srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(LANG1.FUTURE_IMPORT_STATEMENT) &&
                dstImportStatement.getParent().getType().name.equals(LANG2.FUTURE_IMPORT_STATEMENT)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
            com.github.gumtreediff.utils.Pair<Tree, Tree> froms = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.FROM_KEYWORD, LANG2.FROM_KEYWORD);
            if (froms != null) {
                mappingStore.addMapping(froms.first,froms.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> relatives = Helpers.findPairOfType(srcImportStatement.getParent(),dstImportStatement.getParent(), LANG1.FUTURE, LANG2.FUTURE);
            if (relatives != null) {
                mappingStore.addMappingRecursively(relatives.first,relatives.second);
            }
        }
        else if (srcImportStatement.getParent() != null && dstImportStatement.getParent() != null &&
                srcImportStatement.getParent().getType().name.equals(LANG1.IMPORT_LIST) &&
                dstImportStatement.getParent().getType().name.equals(LANG2.IMPORT_LIST)) {
            mappingStore.addMapping(srcImportStatement.getParent(), dstImportStatement.getParent());
        }
    }
}
