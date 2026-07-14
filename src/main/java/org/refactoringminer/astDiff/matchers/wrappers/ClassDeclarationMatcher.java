package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import gr.uom.java.xmi.ListCompositeType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLNamedExport;
import gr.uom.java.xmi.UMLPreprocessorStatement;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.UMLTypeAlias;
import gr.uom.java.xmi.UMLTypeParameter;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLNamedExportDiff;
import gr.uom.java.xmi.diff.UMLNamedExportListDiff;
import gr.uom.java.xmi.diff.UMLTypeAliasListDiff;

import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.IgnoringCommentsLeafMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.refactoringminer.astDiff.utils.Helpers.findPairOfType;

/* Created by pourya on 2024-05-22*/
public class ClassDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLClassBaseDiff baseClassDiff;
    private final String IMPLEMENTS_KEYWORD_LABEL = "implements";
    private final String EXTENDS_KEYWORD_LABEL = "extends";
    private final String PERMITS_KEYWORD_LABEL = "permits";
    private final Constants LANG1;
    private final Constants LANG2;

    public ClassDeclarationMatcher(UMLClassBaseDiff baseClassDiff, Constants LANG1, Constants LANG2) {
        this.baseClassDiff = baseClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public ClassDeclarationMatcher(OptimizationData optimizationData, UMLClassBaseDiff baseClassDiff, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.baseClassDiff = baseClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processClassDeclarationMapping(srcTree,dstTree,baseClassDiff,mappingStore);
    }

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        String AST_type1 = astType(classDiff.getOriginalClass(), LANG1);
        String AST_type2 = astType(classDiff.getNextClass(), LANG2);
        Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,AST_type1);
        Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,AST_type2);
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isTypeAlias() && classDiff.getNextClass().isTypeAlias()) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.TYPE_ALIAS_DECLARATION);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.TYPE_ALIAS_DECLARATION);
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isInterface() && classDiff.getNextClass().isInterface()) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.INTERFACE_DECLARATION);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.INTERFACE_DECLARATION);
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isObject() && classDiff.getNextClass().isObject()) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT_DECLARATION);
            if(srcTypeDeclaration == null)
                srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT_DECLARATION);
            if(dstTypeDeclaration == null)
                dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT);
            if(srcTypeDeclaration == null) {
                Tree t1 = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.PAIR);
                Tree t2 = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.LEXICAL_DECLARATION);
                if(t1 != null && t2 == null) {
                    srcTypeDeclaration = t1;
                }
                else if(t1 == null && t2 != null) {
                    srcTypeDeclaration = t2;
                }
                else if(t1 != null && t2 != null) {
                    if(t1.getLength() > t2.getLength())
                        srcTypeDeclaration = t1;
                    else if(t2.getLength() > t1.getLength())
                        srcTypeDeclaration = t2;
                }
            }
            if(dstTypeDeclaration == null) {
                Tree t1 = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.PAIR);
                Tree t2 = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.LEXICAL_DECLARATION);
                if(t1 != null && t2 == null) {
                    dstTypeDeclaration = t1;
                }
                else if(t1 == null && t2 != null) {
                    dstTypeDeclaration = t2;
                }
                else if(t1 != null && t2 != null) {
                    if(t1.getLength() > t2.getLength())
                        dstTypeDeclaration = t1;
                    else if(t2.getLength() > t1.getLength())
                        dstTypeDeclaration = t2;
                }
            }
        }
        if(classDiff.getOriginalClass().isObject() && !classDiff.getNextClass().isObject()) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT_DECLARATION);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.TYPE_DECLARATION);
        }
        else if(!classDiff.getOriginalClass().isObject() && classDiff.getNextClass().isObject()) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.TYPE_DECLARATION);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT_DECLARATION);
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isModule() && classDiff.getNextClass().isModule()) {
            srcTypeDeclaration = srcTree;
            dstTypeDeclaration = dstTree;
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null) {
            srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.ERROR);
            dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.ERROR);
        }
        if(classDiff.getTypeAliasListDiff().isPresent()) {
            processTypeAliasList(srcTree, dstTree, classDiff.getTypeAliasListDiff().get(), mappingStore);
        }
        if(classDiff.getNamedExportListDiff().isPresent()) {
            processNamedExportList(srcTree, dstTree, classDiff.getNamedExportListDiff().get(), mappingStore);
        }
        if (srcTypeDeclaration == null || dstTypeDeclaration == null) return;
        if (srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null) {
            if (srcTypeDeclaration.getParent().getType().name.equals(LANG1.TYPE_DECLARATION_STATEMENT)
                    && dstTypeDeclaration.getParent().getType().name.equals(LANG2.TYPE_DECLARATION_STATEMENT)) {
                mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
            }
            if(srcTypeDeclaration.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT) && dstTypeDeclaration.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT)) {
                mappingStore.addMapping(srcTypeDeclaration.getParent(), dstTypeDeclaration.getParent());
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent(),LANG1.EXPORT_KEYWORD,LANG2.EXPORT_KEYWORD);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent(), LANG1.DEFAULT_KEYWORD, LANG2.DEFAULT_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
            }
        }

        if(classDiff.getOriginalClass().isFunctionalInterface() && classDiff.getNextClass().isFunctionalInterface()) {
            new MethodMatcher(optimizationData, classDiff.getOperationBodyMapperList().get(0), LANG1, LANG2).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
            return;
        }
        mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);
        if(srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null &&
                srcTypeDeclaration.getParent().getType().name.equals(LANG1.DECORATED_METHOD) && dstTypeDeclaration.getParent().getType().name.equals(LANG1.DECORATED_METHOD)) {
            mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
        }

        String v1 = classDiff.getOriginalClass().getVisibility().toString();
        String v2 = classDiff.getNextClass().getVisibility().toString();
        Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, LANG1.MODIFIER, v1, LANG1);
        Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, LANG2.MODIFIER, v2, LANG2);
        if (tree1 != null && tree2 != null)
            mappingStore.addMappingRecursively(tree1,tree2);

        Pair<Tree,Tree> matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.TYPE_IDENTIFIER,LANG2.TYPE_IDENTIFIER);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.TYPE_DECLARATION_KIND,LANG2.TYPE_DECLARATION_KIND);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.STRUCT_KEYWORD,LANG2.STRUCT_KEYWORD);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.MODIFIERS,LANG2.MODIFIERS);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);

        if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
            new SameModifierMatcher(LANG1, LANG2, LANG1.STATIC).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
            new SameModifierMatcher(LANG1, LANG2, LANG1.FINAL).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
            new SameModifierMatcher(LANG1, LANG2, LANG1.ABSTRACT).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isSealed() && classDiff.getNextClass().isSealed())
            new SameModifierMatcher(LANG1, LANG2, LANG1.SEALED).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isStrictfp() && classDiff.getNextClass().isStrictfp())
            new SameModifierMatcher(LANG1, LANG2, LANG1.STRICTFP).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.ANNOTATION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.ENUM).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.OPEN).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.DATA).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.COMPANION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.INNER).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.VALUE).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);

        Tree parent1 = srcTypeDeclaration.getParent();
        Tree parent2 = dstTypeDeclaration.getParent();
        if(parent1 != null && parent1.getType().name.equals(LANG1.TEMPLATE_DECLARATION) &&
                parent2 != null && parent2.getType().name.equals(LANG2.TEMPLATE_DECLARATION)) {
            mappingStore.addMapping(parent1, parent2);
            com.github.gumtreediff.utils.Pair<Tree, Tree> templates = Helpers.findPairOfType(parent1, parent2,LANG1.TEMPLATE_KEYWORD,LANG2.TEMPLATE_KEYWORD);
            if (templates != null) {
                mappingStore.addMapping(templates.first,templates.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> templateParameterLists = Helpers.findPairOfType(parent1, parent2,LANG1.TEMPLATE_PARAMETER_LIST,LANG2.TEMPLATE_PARAMETER_LIST);
            if (templateParameterLists != null) {
                mappingStore.addMappingRecursively(templateParameterLists.first,templateParameterLists.second);
            }
        }
        if (classDiff.getTypeParameterDiffList() != null)
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo(), LANG1);
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo(), LANG2);
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
            if (srcTypeParam.getParent().getType().name.equals(LANG1.TYPE_PARAMETERS) && dstTypeParam.getParent().getType().name.equals(LANG2.TYPE_PARAMETERS)) {
                mappingStore.addMapping(srcTypeParam.getParent(), dstTypeParam.getParent());
                matched = Helpers.findPairOfType(srcTypeParam.getParent(),dstTypeParam.getParent(),LANG1.OPENING_TAG,LANG2.OPENING_TAG);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcTypeParam.getParent(),dstTypeParam.getParent(),LANG1.CLOSING_TAG,LANG2.CLOSING_TAG);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                int index1 = srcTypeParam.getParent().getChildPosition(srcTypeParam);
                int index2 = dstTypeParam.getParent().getChildPosition(dstTypeParam);
                if(srcTypeParam.getParent().getChildren().size() > index1+1 && srcTypeParam.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                        dstTypeParam.getParent().getChildren().size() > index2+1 && dstTypeParam.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                    Tree t1 = srcTypeParam.getParent().getChild(index1+1);
                    Tree t2 = dstTypeParam.getParent().getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
        }
        
        if (classDiff.getPrimaryConstructorParameterListDiff().isPresent()) {
            Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> pairs = classDiff.getPrimaryConstructorParameterListDiff().get().getCommonParameters();
            for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> pair : pairs) {
                Tree srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, pair.getLeft().getLocationInfo(), LANG1);
                Tree dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, pair.getRight().getLocationInfo(), LANG2);
                if (srcFieldDeclaration == null || srcFieldDeclaration.getType().name.endsWith("_comment")) {
                    srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, pair.getLeft().getLocationInfo(), LANG1, LANG1.CLASS_PARAMETER);
                }
                if (dstFieldDeclaration == null || dstFieldDeclaration.getType().name.endsWith("_comment")) {
                    dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, pair.getRight().getLocationInfo(), LANG2, LANG2.CLASS_PARAMETER);
                }
                if (srcFieldDeclaration != null && dstFieldDeclaration != null && srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
                    mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
            }
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, classDiff.getOriginalClass().getPrimaryConstructor().get().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, classDiff.getNextClass().getPrimaryConstructor().get().getLocationInfo(), LANG2);
            if (srcSubTree != null && dstSubTree != null) {
                mappingStore.addMapping(srcSubTree, dstSubTree);
                new SameModifierMatcher(LANG1, LANG2, LANG1.INTERNAL).match(srcSubTree,dstSubTree,mappingStore);
                new SameModifierMatcher(LANG1, LANG2, LANG1.PRIVATE).match(srcSubTree,dstSubTree,mappingStore);
                matched = findPairOfType(srcSubTree,dstSubTree,LANG1.MODIFIERS,LANG2.MODIFIERS);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
                matched = findPairOfType(srcSubTree,dstSubTree,LANG1.CONSTRUCTOR_KEYWORD,LANG2.CONSTRUCTOR_KEYWORD);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
            }
        }
        if (classDiff.getCommonFunctionType().isPresent()) {
            org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> pair = classDiff.getCommonFunctionType().get();
            Tree srcFunctionType = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, pair.getLeft().getLocationInfo(), LANG1);
            Tree dstFunctionType = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, pair.getRight().getLocationInfo(), LANG2);
            if (srcFunctionType != null && dstFunctionType != null) {
                if(!srcFunctionType.isIsomorphicTo(dstFunctionType) && pair.getLeft() instanceof ListCompositeType listType1 && pair.getRight() instanceof ListCompositeType listType2) {
                    List<UMLType> types1 = listType1.getTypes();
                    List<UMLType> types2 = listType2.getTypes();
                    boolean matchFound = false;
                    if(types1.size() <= types2.size()) {
                        for(UMLType type1 : types1) {
                            int index = types2.indexOf(type1);
                            if(index != -1) {
                                matchFound = true;
                                UMLType type2 = types2.get(index);
                                Tree t1 = TreeUtilFunctions.findByLocationInfo(srcFunctionType, type1.getLocationInfo(), LANG1);
                                Tree t2 = TreeUtilFunctions.findByLocationInfo(dstFunctionType, type2.getLocationInfo(), LANG2);
                                mappingStore.addMappingRecursively(t1, t2);
                                int index1 = t1.getParent().getChildPosition(t1);
                                int index2 = t2.getParent().getChildPosition(t2);
                                if(index1 > 0 && t1.getParent().getChild(index1-1).getType().name.equals(LANG1.UNION) &&
                                        index2 > 0 && t2.getParent().getChild(index2-1).getType().name.equals(LANG2.UNION)) {
                                    Tree tt1 = t1.getParent().getChild(index1-1);
                                    Tree tt2 = t2.getParent().getChild(index2-1);
                                    mappingStore.addMapping(tt1,tt2);
                                    //parent is a union type
                                    mappingStore.addMapping(t1.getParent(),t2.getParent());
                                }
                            }
                        }
                    }
                    else {
                        for(UMLType type2 : types2) {
                            int index = types1.indexOf(type2);
                            if(index != -1) {
                                matchFound = true;
                                UMLType type1 = types1.get(index);
                                Tree t1 = TreeUtilFunctions.findByLocationInfo(srcFunctionType, type1.getLocationInfo(), LANG1);
                                Tree t2 = TreeUtilFunctions.findByLocationInfo(dstFunctionType, type2.getLocationInfo(), LANG2);
                                mappingStore.addMappingRecursively(t1, t2);
                                int index1 = t1.getParent().getChildPosition(t1);
                                int index2 = t2.getParent().getChildPosition(t2);
                                if(index1 > 0 && t1.getParent().getChild(index1-1).getType().name.equals(LANG1.UNION) &&
                                        index2 > 0 && t2.getParent().getChild(index2-1).getType().name.equals(LANG2.UNION)) {
                                    Tree tt1 = t1.getParent().getChild(index1-1);
                                    Tree tt2 = t2.getParent().getChild(index2-1);
                                    mappingStore.addMapping(tt1,tt2);
                                    //parent is a union type
                                    mappingStore.addMapping(t1.getParent(),t2.getParent());
                                }
                            }
                        }
                    }
                    if(matchFound) {
                        mappingStore.addMapping(srcFunctionType, dstFunctionType);
                    }
                }
                else {
                    mappingStore.addMappingRecursively(srcFunctionType, dstFunctionType);
                }
            }
        }
        if(classDiff.getPreprocessorStatementListDiff().isPresent()) {
            for (org.apache.commons.lang3.tuple.Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> statementPair : classDiff.getPreprocessorStatementListDiff().get().getCommonStatements()) {
                Tree srcStatement = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, statementPair.getLeft().getLocationInfo(), LANG1);
                if(!srcStatement.getLabel().isEmpty())
                    srcStatement = srcStatement.getParent();
                Tree dstStatement = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, statementPair.getRight().getLocationInfo(), LANG2);
                if(!dstStatement.getLabel().isEmpty())
                    dstStatement = dstStatement.getParent();
                mappingStore.addMappingRecursively(srcStatement, dstStatement);
            }
            for (org.apache.commons.lang3.tuple.Pair<UMLPreprocessorStatement, UMLPreprocessorStatement> statementPair : classDiff.getPreprocessorStatementListDiff().get().getChangedStatements()) {
                Tree srcStatement = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, statementPair.getLeft().getLocationInfo(), LANG1);
                if(!srcStatement.getLabel().isEmpty())
                    srcStatement = srcStatement.getParent();
                Tree dstStatement = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, statementPair.getRight().getLocationInfo(), LANG2);
                if(!dstStatement.getLabel().isEmpty())
                    dstStatement = dstStatement.getParent();
                if(!mappingStore.isSrcMapped(srcStatement) && !mappingStore.isDstMapped(dstStatement))
                    mappingStore.addMappingRecursively(srcStatement, dstStatement);
            }
        }
        processSuperClasses(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassPermittedTypes(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processInterfaceToSuperclassOrOpposite(classDiff, mappingStore, srcTypeDeclaration, dstTypeDeclaration);
        new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getJavadoc(), classDiff.getNextClass().getJavadoc(), classDiff.getJavadocDiff(), LANG1, LANG2)
                .match(srcTree, dstTree, mappingStore);
        new CommentMatcher(optimizationData, classDiff.getPackageDeclarationCommentListDiff(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        if (classDiff.getPackageDeclarationJavadocDiff().isPresent()) {
            new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getPackageDeclarationJavadoc(), classDiff.getNextClass().getPackageDeclarationJavadoc(), classDiff.getPackageDeclarationJavadocDiff(), LANG1, LANG2)
            .match(srcTree, dstTree, mappingStore);
        }
        processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);
        processClassBlock(srcTypeDeclaration, dstTypeDeclaration, mappingStore);
    }

    private String astType(UMLClass umlClass, Constants LANG) {
        String type = LANG.TYPE_DECLARATION;
        if (umlClass.isEnum())
            type = LANG.ENUM_DECLARATION;
        else if (umlClass.isAnnotation())
            type = LANG.ANNOTATION_TYPE_DECLARATION;
        else if (umlClass.isRecord())
            type = LANG.RECORD_DECLARATION;
        else if (umlClass.isModule())
            type = LANG.MODULE;
        else if (umlClass.isObject())
            type = LANG.COMPANION_OBJECT;
        else if (umlClass.isFunctionalInterface())
            type = LANG.METHOD_DECLARATION;
        else if (umlClass.isStruct())
            type = LANG.STRUCT_SPECIFIER;
        return type;
    }

    private void processClassBlock(Tree srcTypeDeclaration, Tree dstTypeDeclaration, ExtendedMultiMappingStore mappingStore) {
        Tree srcBlock = TreeUtilFunctions.findFirstByType(srcTypeDeclaration, LANG1.CLASS_BLOCK);
        Tree dstBlock = TreeUtilFunctions.findFirstByType(dstTypeDeclaration, LANG2.CLASS_BLOCK);
        if (srcTypeDeclaration.getType().name.equals(LANG1.TYPE_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG2.CLASS_DECLARATION) && dstBlock != null && srcBlock == null) {
            //handle Java to Kotlin migration
            dstTypeDeclaration.getChildren().addAll(dstBlock.getChildren());
            dstTypeDeclaration.getChildren().remove(dstBlock);
            Tree name1 = TreeUtilFunctions.findChildByType(srcTypeDeclaration, LANG1.SIMPLE_NAME);
            Tree name2 = TreeUtilFunctions.findChildByType(dstTypeDeclaration, LANG2.TYPE_IDENTIFIER);
            if(name1 != null && name2 != null) {
                mappingStore.addMapping(name1, name2);
            }
        }
        if (srcTypeDeclaration.getType().name.equals(LANG1.TYPE_ALIAS_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG2.TYPE_ALIAS_DECLARATION)) {
            Pair<Tree, Tree> object_types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.OBJECT_TYPE, LANG2.OBJECT_TYPE);
            if (object_types != null) {
                mappingStore.addMapping(object_types.first, object_types.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
            else {
                Pair<Tree, Tree> intersection_types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.INTERSECTION_TYPE, LANG2.INTERSECTION_TYPE);
                if(intersection_types != null) {
                    if(intersection_types.first.isIsoStructuralTo(intersection_types.second)) {
                        mappingStore.addMappingRecursively(intersection_types.first, intersection_types.second);
                    }
                    else {
                        mappingStore.addMapping(intersection_types.first, intersection_types.second);
                        object_types = Helpers.findPairOfType(intersection_types.first, intersection_types.second, LANG1.OBJECT_TYPE, LANG2.OBJECT_TYPE);
                        if (object_types != null) {
                            mappingStore.addMapping(object_types.first, object_types.second);
                            Pair<Tree,Tree> opening = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                            if (opening != null) {
                                mappingStore.addMapping(opening.first,opening.second);
                            }
                            Pair<Tree,Tree> closing = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                            if (closing != null) {
                                mappingStore.addMapping(closing.first,closing.second);
                            }
                        }
                    }
                }
                Pair<Tree, Tree> union_types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.UNION_TYPE, LANG2.UNION_TYPE);
                if(union_types != null) {
                    if(union_types.first.isIsoStructuralTo(union_types.second)) {
                        mappingStore.addMappingRecursively(union_types.first, union_types.second);
                    }
                    else {
                        mappingStore.addMapping(union_types.first, union_types.second);
                        object_types = Helpers.findPairOfType(union_types.first, union_types.second, LANG1.OBJECT_TYPE, LANG2.OBJECT_TYPE);
                        if (object_types != null) {
                            mappingStore.addMapping(object_types.first, object_types.second);
                            Pair<Tree,Tree> opening = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                            if (opening != null) {
                                mappingStore.addMapping(opening.first,opening.second);
                            }
                            Pair<Tree,Tree> closing = Helpers.findPairOfType(object_types.first,object_types.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                            if (closing != null) {
                                mappingStore.addMapping(closing.first,closing.second);
                            }
                        }
                    }
                }
                Pair<Tree, Tree> generic_types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.GENERIC_TYPE, LANG2.GENERIC_TYPE);
                if(generic_types != null) {
                    if(generic_types.first.isIsoStructuralTo(generic_types.second)) {
                        mappingStore.addMappingRecursively(generic_types.first, generic_types.second);
                    }
                    else {
                        mappingStore.addMapping(generic_types.first, generic_types.second);
                    }
                }
            }
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.TYPE_KEYWORD, LANG2.TYPE_KEYWORD);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> equalOperators = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.EQUAL_OPERATOR, LANG2.EQUAL_OPERATOR);
            if(equalOperators != null) {
                mappingStore.addMapping(equalOperators.first, equalOperators.second);
                //check if the aliased type is just a type_identifier
                int index1 = srcTypeDeclaration.getChildPosition(equalOperators.first);
                int index2 = dstTypeDeclaration.getChildPosition(equalOperators.second);
                if(srcTypeDeclaration.getChildren().size() > index1+1 && srcTypeDeclaration.getChild(index1+1).getType().name.equals(LANG1.TYPE_IDENTIFIER) &&
                        dstTypeDeclaration.getChildren().size() > index2+1 && dstTypeDeclaration.getChild(index2+1).getType().name.equals(LANG2.TYPE_IDENTIFIER)) {
                    Tree t1 = srcTypeDeclaration.getChild(index1+1);
                    Tree t2 = dstTypeDeclaration.getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
            Pair<Tree, Tree> semicolons = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.SEMICOLON, LANG2.SEMICOLON);
            if(semicolons != null) {
                mappingStore.addMapping(semicolons.first, semicolons.second);
            }
        }
        if (srcTypeDeclaration.getType().name.equals(LANG1.INTERFACE_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG2.INTERFACE_DECLARATION)) {
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.INTERFACE_KEYWORD, LANG2.INTERFACE_KEYWORD);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.INTERFACE_BODY, LANG2.INTERFACE_BODY);
            if(blocks != null) {
                mappingStore.addMapping(blocks.first, blocks.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
        }
        if (srcTypeDeclaration.getType().name.equals(LANG1.CLASS_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG2.CLASS_DECLARATION)) {
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.CLASS_KEYWORD, LANG2.CLASS_KEYWORD);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.CLASS_BODY, LANG2.CLASS_BODY);
            if(blocks != null) {
                mappingStore.addMapping(blocks.first, blocks.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
            //find last delegation specifier
            if(srcTypeDeclaration.getChildren().size() > 0 && srcTypeDeclaration.getChildren().get(srcTypeDeclaration.getChildren().size()-1).getType().name.equals(LANG1.DELEGATION_SPECIFIER) &&
                    dstTypeDeclaration.getChildren().size() > 0 && dstTypeDeclaration.getChildren().get(dstTypeDeclaration.getChildren().size()-1).getType().name.equals(LANG2.DELEGATION_SPECIFIER)) {
                Tree delegationSpecifier1 = srcTypeDeclaration.getChildren().get(srcTypeDeclaration.getChildren().size()-1);
                Tree delegationSpecifier2 = dstTypeDeclaration.getChildren().get(dstTypeDeclaration.getChildren().size()-1);
                mappingStore.addMapping(delegationSpecifier1, delegationSpecifier2);
                Pair<Tree,Tree> explicitDelegations = Helpers.findPairOfType(delegationSpecifier1,delegationSpecifier2, LANG1.EXPLICIT_DELEGATION, LANG2.EXPLICIT_DELEGATION);
                if (explicitDelegations != null) {
                    mappingStore.addMapping(explicitDelegations.first,explicitDelegations.second);
                    Pair<Tree, Tree> delegateKeywords = Helpers.findPairOfType(explicitDelegations.first,explicitDelegations.second, LANG1.DELEGATE_KEYWORD, LANG2.DELEGATE_KEYWORD);
                    if (delegateKeywords != null) {
                        mappingStore.addMapping(delegateKeywords.first,delegateKeywords.second);
                    }
                    Pair<Tree, Tree> byIdentifiers = Helpers.findPairOfType(explicitDelegations.first,explicitDelegations.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                    if (byIdentifiers != null) {
                        mappingStore.addMapping(byIdentifiers.first,byIdentifiers.second);
                    }
                    Pair<Tree, Tree> callExpressions = Helpers.findPairOfType(explicitDelegations.first,explicitDelegations.second, LANG1.METHOD_INVOCATION, LANG2.METHOD_INVOCATION);
                    if (callExpressions != null) {
                        processCallExpressionsInDelegationSpecifiers(mappingStore, callExpressions, LANG1, LANG2);
                    }
                    Pair<Tree, Tree> navigationExpressions = Helpers.findPairOfType(explicitDelegations.first,explicitDelegations.second, LANG1.NAVIGATION_EXPRESSION, LANG2.NAVIGATION_EXPRESSION);
                    if (navigationExpressions != null) {
                        mappingStore.addMapping(navigationExpressions.first,navigationExpressions.second);
                        callExpressions = Helpers.findPairOfType(navigationExpressions.first,navigationExpressions.second, LANG1.METHOD_INVOCATION, LANG2.METHOD_INVOCATION);
                        if (callExpressions != null) {
                            processCallExpressionsInDelegationSpecifiers(mappingStore, callExpressions, LANG1, LANG2);
                        }
                    }
                }
            }
        }
        if (srcTypeDeclaration.getType().name.equals(LANG1.ENUM_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG2.ENUM_DECLARATION)) {
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.ENUM_KEYWORD, LANG2.ENUM_KEYWORD);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.ENUM_BODY, LANG2.ENUM_BODY);
            if(blocks != null) {
                mappingStore.addMapping(blocks.first, blocks.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
        }
        if(srcTypeDeclaration.getType().name.equals(LANG1.PAIR) && dstTypeDeclaration.getType().name.equals(LANG1.PAIR)) {
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.PROPERTY_IDENTIFIER, LANG2.PROPERTY_IDENTIFIER);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> colons = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.COLON, LANG2.COLON);
            if(colons != null) {
                mappingStore.addMapping(colons.first, colons.second);
            }
            Pair<Tree,Tree> objects = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.OBJECT,LANG2.OBJECT);
            if (objects != null) {
                mappingStore.addMapping(objects.first, objects.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(objects.first,objects.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(objects.first,objects.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
            int index1 = srcTypeDeclaration.getParent().getChildPosition(srcTypeDeclaration);
            int index2 = dstTypeDeclaration.getParent().getChildPosition(dstTypeDeclaration);
            if(srcTypeDeclaration.getParent().getChildren().size() > index1+1 && srcTypeDeclaration.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                    dstTypeDeclaration.getParent().getChildren().size() > index2+1 && dstTypeDeclaration.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                Tree t1 = srcTypeDeclaration.getParent().getChild(index1+1);
                Tree t2 = dstTypeDeclaration.getParent().getChild(index2+1);
                mappingStore.addMapping(t1,t2);
            }
        }
        if(srcTypeDeclaration.getType().name.equals(LANG1.PAIR) && dstTypeDeclaration.getType().name.equals(LANG1.LEXICAL_DECLARATION)) {
            Tree t1 = TreeUtilFunctions.findFirstByType(srcTypeDeclaration, LANG1.PROPERTY_IDENTIFIER);
            Tree variableDeclarator2 = TreeUtilFunctions.findFirstByType(dstTypeDeclaration, LANG2.VARIABLE_DECLARATOR);
            Tree t2 = TreeUtilFunctions.findFirstByType(variableDeclarator2, LANG2.SIMPLE_NAME);
            if(t1 != null && t2 != null) {
                mappingStore.addMapping(t1, t2);
            }
            Pair<Tree,Tree> objects = Helpers.findPairOfType(srcTypeDeclaration,variableDeclarator2,LANG1.OBJECT,LANG2.OBJECT);
            if (objects != null) {
                mappingStore.addMapping(objects.first, objects.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(objects.first,objects.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(objects.first,objects.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
        }
        if(srcTypeDeclaration.getType().name.equals(LANG1.LEXICAL_DECLARATION) && dstTypeDeclaration.getType().name.equals(LANG1.LEXICAL_DECLARATION)) {
            Pair<Tree,Tree> matched = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.VARIABLE_DECLARATOR,LANG2.VARIABLE_DECLARATOR);
            if(matched != null) {
                processVariableDeclaratorPair(matched.first, matched.second, mappingStore);
            }
        }
        if (srcTypeDeclaration.getType().name.equals(LANG1.MODULE) && dstTypeDeclaration.getType().name.equals(LANG2.MODULE)) {
            Tree ambient1 = srcTypeDeclaration.getParent();
            Tree ambient2 = dstTypeDeclaration.getParent();
            if(ambient1 != null && ambient2 != null && ambient1.getType().name.equals(LANG1.AMBIENT_DECLARATION) && ambient2.getType().name.equals(LANG2.AMBIENT_DECLARATION)) {
                mappingStore.addMapping(ambient1, ambient2);
                Pair<Tree, Tree> declares = Helpers.findPairOfType(ambient1, ambient2, LANG1.DECLARE_KEYWORD, LANG2.DECLARE_KEYWORD);
                if(declares != null) {
                    mappingStore.addMapping(declares.first, declares.second);
                }
                if(ambient1.getParent().getType().name.equals(LANG1.PROGRAM) && ambient2.getParent().getType().name.equals(LANG2.PROGRAM)) {
                    mappingStore.addMapping(ambient1.getParent(), ambient2.getParent());
                }
            }
            Pair<Tree, Tree> types = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.MODULE, LANG2.MODULE);
            if(types != null) {
                mappingStore.addMapping(types.first, types.second);
            }
            Pair<Tree, Tree> names = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.STRING, LANG2.STRING);
            if(names != null) {
                mappingStore.addMappingRecursively(names.first, names.second);
            }
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcTypeDeclaration,dstTypeDeclaration, LANG1.STATEMENT_BLOCK, LANG2.STATEMENT_BLOCK);
            if(blocks != null) {
                mappingStore.addMapping(blocks.first, blocks.second);
                com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
        }
        if(srcTypeDeclaration.getType().name.equals(LANG1.OBJECT) && dstTypeDeclaration.getType().name.equals(LANG2.OBJECT)) {
            if(srcTypeDeclaration.getParent().getType().name.equals(LANG1.VARIABLE_DECLARATOR) && dstTypeDeclaration.getParent().getType().name.equals(LANG2.VARIABLE_DECLARATOR)) {
                processVariableDeclaratorPair(srcTypeDeclaration.getParent(), dstTypeDeclaration.getParent(), mappingStore);
            }
        }
        if (srcBlock == null || dstBlock == null) return;
        mappingStore.addMapping(srcBlock, dstBlock);
        Pair<Tree,Tree> opening = Helpers.findPairOfType(srcBlock,dstBlock, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
        if (opening != null) {
            mappingStore.addMapping(opening.first,opening.second);
        }
        Pair<Tree,Tree> closing = Helpers.findPairOfType(srcBlock,dstBlock, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
        if (closing != null) {
            mappingStore.addMapping(closing.first,closing.second);
        }
        int index1 = srcTypeDeclaration.getParent().getChildPosition(srcTypeDeclaration);
        int index2 = dstTypeDeclaration.getParent().getChildPosition(dstTypeDeclaration);
        if(srcTypeDeclaration.getParent().getChildren().size() > index1+1 && srcTypeDeclaration.getParent().getChild(index1+1).getType().name.equals(LANG1.SEMICOLON) &&
                dstTypeDeclaration.getParent().getChildren().size() > index2+1 && dstTypeDeclaration.getParent().getChild(index2+1).getType().name.equals(LANG2.SEMICOLON)) {
            Tree t1 = srcTypeDeclaration.getParent().getChild(index1+1);
            Tree t2 = dstTypeDeclaration.getParent().getChild(index2+1);
            mappingStore.addMapping(t1,t2);
        }
        List<Tree> accessSpecifiers1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcBlock, LANG1.ACCESS_SPECIFIER);
        List<Tree> accessSpecifiers2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstBlock, LANG2.ACCESS_SPECIFIER);
        List<Tree> matched1 = new ArrayList<Tree>();
        List<Tree> matched2 = new ArrayList<Tree>();
        if(accessSpecifiers1.size() <= accessSpecifiers2.size()) {
            for(int i=0; i<accessSpecifiers1.size(); i++) {
                Tree accessSpecifier1 = accessSpecifiers1.get(i);
                for(int j=0; j<accessSpecifiers2.size(); j++) {
                    Tree accessSpecifier2 = accessSpecifiers2.get(j);
                    if(accessSpecifier1.getChild(0).getLabel().equals(accessSpecifier2.getChild(0).getLabel()) &&
                            !matched1.contains(accessSpecifier1) && !matched2.contains(accessSpecifier2)) {
                        mappingStore.addMappingRecursively(accessSpecifier1, accessSpecifier2);
                        index1 = accessSpecifier1.getParent().getChildPosition(accessSpecifier1);
                        index2 = accessSpecifier2.getParent().getChildPosition(accessSpecifier2);
                        if(accessSpecifier1.getParent().getChildren().size() > index1+1 && accessSpecifier1.getParent().getChild(index1+1).getType().name.equals(LANG1.COLON) &&
                                accessSpecifier2.getParent().getChildren().size() > index2+1 && accessSpecifier2.getParent().getChild(index2+1).getType().name.equals(LANG2.COLON)) {
                            Tree t1 = accessSpecifier1.getParent().getChild(index1+1);
                            Tree t2 = accessSpecifier2.getParent().getChild(index2+1);
                            mappingStore.addMapping(t1,t2);
                        }
                        matched1.add(accessSpecifier1);
                        matched2.add(accessSpecifier2);
                        break;
                    }
                }
            }
        }
        else {
            for(int j=0; j<accessSpecifiers2.size(); j++) {
                Tree accessSpecifier2 = accessSpecifiers2.get(j);
                for(int i=0; i<accessSpecifiers1.size(); i++) {
                    Tree accessSpecifier1 = accessSpecifiers1.get(i);
                    if(accessSpecifier1.getChild(0).getLabel().equals(accessSpecifier2.getChild(0).getLabel()) &&
                            !matched1.contains(accessSpecifier1) && !matched2.contains(accessSpecifier2)) {
                        mappingStore.addMappingRecursively(accessSpecifier1, accessSpecifier2);
                        index1 = accessSpecifier1.getParent().getChildPosition(accessSpecifier1);
                        index2 = accessSpecifier2.getParent().getChildPosition(accessSpecifier2);
                        if(accessSpecifier1.getParent().getChildren().size() > index1+1 && accessSpecifier1.getParent().getChild(index1+1).getType().name.equals(LANG1.COLON) &&
                                accessSpecifier2.getParent().getChildren().size() > index2+1 && accessSpecifier2.getParent().getChild(index2+1).getType().name.equals(LANG2.COLON)) {
                            Tree t1 = accessSpecifier1.getParent().getChild(index1+1);
                            Tree t2 = accessSpecifier2.getParent().getChild(index2+1);
                            mappingStore.addMapping(t1,t2);
                        }
                        matched1.add(accessSpecifier1);
                        matched2.add(accessSpecifier2);
                        break;
                    }
                }
            }
        }
        Tree parent1 = srcTypeDeclaration.getParent();
        Tree parent2 = dstTypeDeclaration.getParent();
        while(parent1 != null && parent2 != null) {
            if(parent1.getType().equals(parent2.getType())) {
                mappingStore.addMapping(parent1, parent2);
                opening = Helpers.findPairOfType(parent1,parent2, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                closing = Helpers.findPairOfType(parent1,parent2, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
                if(parent1.getType().name.equals(LANG1.PACKAGE_DECLARATION) && parent2.getType().name.equals(LANG2.PACKAGE_DECLARATION)) {
                    Pair<Tree,Tree> namespaces = Helpers.findPairOfType(parent1,parent2, LANG1.NAMESPACE, LANG2.NAMESPACE);
                    if (namespaces != null) {
                        mappingStore.addMapping(namespaces.first,namespaces.second);
                    }
                    Pair<Tree,Tree> namespace_identifiers = Helpers.findPairOfType(parent1,parent2, LANG1.NAMESPACE_IDENTIFIER, LANG2.NAMESPACE_IDENTIFIER);
                    if (namespace_identifiers != null) {
                        mappingStore.addMapping(namespace_identifiers.first,namespace_identifiers.second);
                    }
                    Pair<Tree,Tree> nested_namespace_specifiers = Helpers.findPairOfType(parent1,parent2, LANG1.NESTED_NAMESPACE_SPECIFIER, LANG2.NESTED_NAMESPACE_SPECIFIER);
                    if (nested_namespace_specifiers != null) {
                        mappingStore.addMappingRecursively(nested_namespace_specifiers.first,nested_namespace_specifiers.second);
                    }
                    Pair<Tree,Tree> declaration_lists = Helpers.findPairOfType(parent1,parent2, LANG1.DECLARATION_LIST, LANG2.DECLARATION_LIST);
                    if (declaration_lists != null) {
                        mappingStore.addMapping(declaration_lists.first,declaration_lists.second);
                        opening = Helpers.findPairOfType(declaration_lists.first,declaration_lists.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        closing = Helpers.findPairOfType(declaration_lists.first,declaration_lists.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                    }
                }
            }
            else {
                break;
            }
            parent1 = parent1.getParent();
            parent2 = parent2.getParent();
        }
    }

    public static void processCallExpressionsInDelegationSpecifiers(ExtendedMultiMappingStore mappingStore, Pair<Tree, Tree> callExpressions, Constants LANG1, Constants LANG2) {
        mappingStore.addMapping(callExpressions.first,callExpressions.second);
        Pair<Tree, Tree> byIdentifiers = Helpers.findPairOfType(callExpressions.first,callExpressions.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
        if(byIdentifiers != null) {
            mappingStore.addMapping(byIdentifiers.first,byIdentifiers.second);
        }
        Pair<Tree, Tree> callSuffixes = Helpers.findPairOfType(callExpressions.first,callExpressions.second, LANG1.CALL_SUFFIX, LANG2.CALL_SUFFIX);
        if (callSuffixes != null) {
            mappingStore.addMapping(callSuffixes.first,callSuffixes.second);
            Pair<Tree, Tree> annotatedLambdas = Helpers.findPairOfType(callSuffixes.first,callSuffixes.second, LANG1.ANNOTATED_LAMBDA, LANG2.ANNOTATED_LAMBDA);
            if (annotatedLambdas != null) {
                mappingStore.addMapping(annotatedLambdas.first,annotatedLambdas.second);
                Pair<Tree, Tree> lambdaLiterals = Helpers.findPairOfType(annotatedLambdas.first,annotatedLambdas.second, LANG1.LAMBDA_LITERAL, LANG2.LAMBDA_LITERAL);
                if (lambdaLiterals != null) {
                    mappingStore.addMapping(lambdaLiterals.first,lambdaLiterals.second);
                    Pair<Tree, Tree> statements = Helpers.findPairOfType(lambdaLiterals.first,lambdaLiterals.second, LANG1.STATEMENTS, LANG2.STATEMENTS);
                    if (statements != null) {
                        mappingStore.addMapping(statements.first,statements.second);
                    }
                }
            }
        }
    }

    private void processVariableDeclaratorPair(Tree tree1, Tree tree2, ExtendedMultiMappingStore mappingStore) {
        mappingStore.addMapping(tree1, tree2);
        Pair<Tree,Tree> identifiers = Helpers.findPairOfType(tree1,tree2,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
        if (identifiers != null) {
            mappingStore.addMapping(identifiers.first, identifiers.second);
        }
        Pair<Tree,Tree> equals = Helpers.findPairOfType(tree1,tree2,LANG1.EQUAL_OPERATOR,LANG2.EQUAL_OPERATOR);
        if (equals != null) {
            mappingStore.addMapping(equals.first, equals.second);
        }
        Pair<Tree,Tree> arrowFunctions = Helpers.findPairOfType(tree1,tree2,LANG1.ARROW_FUNCTION,LANG2.ARROW_FUNCTION);
        if(arrowFunctions != null) {
            mappingStore.addMapping(arrowFunctions.first, arrowFunctions.second);
            BodyMapperMatcher.processArrowFunction(arrowFunctions.first, arrowFunctions.second, mappingStore, LANG1, LANG2);
        }
        Pair<Tree,Tree> typeAnnotations = Helpers.findPairOfType(tree1,tree2,LANG1.TYPE_ANNOTATION,LANG2.TYPE_ANNOTATION);
        if(typeAnnotations != null) {
            mappingStore.addMappingRecursively(typeAnnotations.first, typeAnnotations.second);
        }
        Pair<Tree,Tree> objects = Helpers.findPairOfType(tree1,tree2,LANG1.OBJECT,LANG2.OBJECT);
        if (objects != null) {
            mappingStore.addMapping(objects.first, objects.second);
            Pair<Tree,Tree> opening = Helpers.findPairOfType(objects.first,objects.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
            if (opening != null) {
                mappingStore.addMapping(opening.first,opening.second);
            }
            Pair<Tree,Tree> closing = Helpers.findPairOfType(objects.first,objects.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
            if (closing != null) {
                mappingStore.addMapping(closing.first,closing.second);
            }
        }
        if(tree1.getParent().getType().name.equals(LANG1.LEXICAL_DECLARATION) && tree2.getParent().getType().name.equals(LANG2.LEXICAL_DECLARATION)) {
            mappingStore.addMapping(tree1.getParent(), tree2.getParent());
            Pair<Tree,Tree> const_keywords = Helpers.findPairOfType(tree1.getParent(),tree2.getParent(),LANG1.CONST_KEYWORD,LANG2.CONST_KEYWORD);
            if(const_keywords != null) {
                mappingStore.addMapping(const_keywords.first, const_keywords.second);
            }
            Pair<Tree,Tree> semicolons = Helpers.findPairOfType(tree1.getParent(),tree2.getParent(),LANG1.SEMICOLON,LANG2.SEMICOLON);
            if(semicolons != null) {
                mappingStore.addMapping(semicolons.first, semicolons.second);
            }
        }
        if(tree1.getParent().getType().name.equals(LANG1.VARIABLE_DECLARATION) && tree2.getParent().getType().name.equals(LANG2.VARIABLE_DECLARATION)) {
            mappingStore.addMapping(tree1.getParent(), tree2.getParent());
            Pair<Tree,Tree> var_keywords = Helpers.findPairOfType(tree1.getParent(),tree2.getParent(),LANG1.VAR_KEYWORD,LANG2.VAR_KEYWORD);
            if(var_keywords != null) {
                mappingStore.addMapping(var_keywords.first, var_keywords.second);
            }
            Pair<Tree,Tree> semicolons = Helpers.findPairOfType(tree1.getParent(),tree2.getParent(),LANG1.SEMICOLON,LANG2.SEMICOLON);
            if(semicolons != null) {
                mappingStore.addMapping(semicolons.first, semicolons.second);
            }
        }
    }

    private void processInterfaceToSuperclassOrOpposite(UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore, Tree srcTypeDeclaration, Tree dstTypeDeclaration) {
        if (classDiff.getImplementedInterfaceBecomesSuperclass().isPresent()) {
            org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> umlTypeUMLTypePair = classDiff.getImplementedInterfaceBecomesSuperclass().get();
            processLocationInfoProvidersRecursively(srcTypeDeclaration, dstTypeDeclaration, mappingStore,umlTypeUMLTypePair.getLeft(),umlTypeUMLTypePair.getRight());
        }
        if (classDiff.getSuperclassBecomesImplementedInterface().isPresent()) {
            org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> umlTypeUMLTypePair = classDiff.getSuperclassBecomesImplementedInterface().get();
            processLocationInfoProvidersRecursively(srcTypeDeclaration, dstTypeDeclaration, mappingStore,umlTypeUMLTypePair.getLeft(),umlTypeUMLTypePair.getRight());
        }
    }

    private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getInterfaceListDiff().getCommonTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getInterfaceListDiff().getChangedTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
        String keyword = IMPLEMENTS_KEYWORD_LABEL;
        if (classDiff.getOriginalClass().isInterface())
            keyword = EXTENDS_KEYWORD_LABEL;
        new KeywordMatcher(LANG1, LANG2, LANG1.TYPE_INHERITANCE_KEYWORD, LANG2.TYPE_INHERITANCE_KEYWORD, keyword).match(srcTree, dstTree, mappingStore);
        Pair<Tree, Tree> extendTypeClause = Helpers.findPairOfType(srcTree,dstTree, LANG1.EXTENDS_TYPE_CLAUSE, LANG2.EXTENDS_TYPE_CLAUSE);
        if(extendTypeClause != null) {
            mappingStore.addMapping(extendTypeClause.first,extendTypeClause.second);
            Pair<Tree, Tree> extendKeywords = Helpers.findPairOfType(extendTypeClause.first,extendTypeClause.second, LANG1.EXTENDS_KEYWORD, LANG2.EXTENDS_KEYWORD);
            if(extendKeywords != null) {
                mappingStore.addMapping(extendKeywords.first,extendKeywords.second);
            }
        }
    }


    private void processClassPermittedTypes(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getPermittedTypeListDiff().getCommonTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getPermittedTypeListDiff().getChangedTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
        new KeywordMatcher(LANG1, LANG2, LANG1.PERMITS_KEYWORD, LANG2.PERMITS_KEYWORD, PERMITS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
    }

    private void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo(), LANG1);
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo(), LANG2);
        if (srcSubTree == null || dstSubTree == null) return;
        if (srcSubTree.isIsoStructuralTo(dstSubTree))
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        Tree parent1 = srcSubTree.getParent();
        Tree parent2 = dstSubTree.getParent();
        if(parent1.getType().name.equals(LANG1.BASE_CLASS_CLAUSE) && parent2.getType().name.equals(LANG2.BASE_CLASS_CLAUSE)) {
            mappingStore.addMappingRecursively(parent1,parent2);
        }
        if(parent1.getType().name.equals(LANG1.CONSTRUCTOR_INVOCATION) && parent2.getType().name.equals(LANG2.CONSTRUCTOR_INVOCATION)) {
            mappingStore.addMappingRecursively(parent1,parent2);
            if(parent1.getParent().getType().name.equals(LANG1.DELEGATION_SPECIFIER) && parent2.getParent().getType().name.equals(LANG2.DELEGATION_SPECIFIER)) {
                mappingStore.addMapping(parent1.getParent(), parent2.getParent());
            }
        }
        if (parent1.getType().name.equals(LANG1.GENERIC_TYPE) && parent2.getType().name.equals(LANG2.GENERIC_TYPE)) {
            if (parent1.isIsoStructuralTo(parent2))
                mappingStore.addMappingRecursively(parent1,parent2);
        }
        if (parent1.getType().name.equals(LANG1.IMPLEMENTS_CLAUSE) && parent2.getType().name.equals(LANG2.IMPLEMENTS_CLAUSE)) {
            mappingStore.addMapping(parent1, parent2);
            Pair<Tree, Tree> implement = Helpers.findPairOfType(parent1, parent2, LANG1.IMPLEMENTS_KEYWORD, LANG2.IMPLEMENTS_KEYWORD);
            if (implement != null) {
                mappingStore.addMapping(implement.first,implement.second);
            }
            if(parent1.getParent().getType().name.equals(LANG1.CLASS_HERITAGE) && parent2.getParent().getType().name.equals(LANG2.CLASS_HERITAGE)) {
                mappingStore.addMapping(parent1.getParent(), parent2.getParent());
            }
        }
    }
    private void processSuperClasses(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        UMLType srcParentUML = classDiff.getOldSuperclass();
        UMLType dstParentUML = classDiff.getNewSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, srcParentUML, dstParentUML);
            processArgumentList(srcTree, dstTree, mappingStore, srcParentUML, dstParentUML);

        }
        new KeywordMatcher(LANG1, LANG2, LANG1.TYPE_INHERITANCE_KEYWORD, LANG2.TYPE_INHERITANCE_KEYWORD, EXTENDS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
        Pair<Tree, Tree> classHeritage = Helpers.findPairOfType(srcTree,dstTree, LANG1.CLASS_HERITAGE, LANG2.CLASS_HERITAGE);
        if(classHeritage != null) {
            mappingStore.addMapping(classHeritage.first,classHeritage.second);
            Pair<Tree, Tree> extendClauses = Helpers.findPairOfType(classHeritage.first,classHeritage.second, LANG1.EXTENDS_CLAUSE, LANG2.EXTENDS_CLAUSE);
            if(extendClauses != null) {
                mappingStore.addMapping(extendClauses.first,extendClauses.second);
                Pair<Tree, Tree> extendKeywords = Helpers.findPairOfType(extendClauses.first,extendClauses.second, LANG1.EXTENDS_KEYWORD, LANG2.EXTENDS_KEYWORD);
                if(extendKeywords != null) {
                    mappingStore.addMapping(extendKeywords.first,extendKeywords.second);
                }
            }
        }
    }

    private void processArgumentList(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, UMLType left, UMLType right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo(), LANG1);
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo(), LANG2);
        if (srcSubTree == null || dstSubTree == null) return;
        Tree src_argumentList = srcSubTree.getParent();
        Tree dst_argumentList = dstSubTree.getParent();
        if (src_argumentList != null && dst_argumentList != null &&
                (src_argumentList.getType().name.equals(LANG1.ARGUMENT_LIST) || src_argumentList.getType().name.equals(LANG1.CONSTRUCTOR_INVOCATION)) &&
                (dst_argumentList.getType().name.equals(LANG2.ARGUMENT_LIST) || dst_argumentList.getType().name.equals(LANG2.CONSTRUCTOR_INVOCATION))) {
            if(src_argumentList.isIsomorphicTo(dst_argumentList))
                mappingStore.addMappingRecursively(src_argumentList,dst_argumentList);
            else
                mappingStore.addMapping(src_argumentList,dst_argumentList);
        }
        if (src_argumentList.getParent().getType().name.equals(LANG1.DELEGATION_SPECIFIER) && dst_argumentList.getParent().getType().name.equals(LANG2.DELEGATION_SPECIFIER)) {
            mappingStore.addMapping(src_argumentList.getParent(),dst_argumentList.getParent());
        }
    }

    private void processTypeAliasList(Tree srcTree, Tree dstTree, UMLTypeAliasListDiff typeAliasListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeAlias, UMLTypeAlias> typeAliasPair : typeAliasListDiff.getCommonTypeAliases()) {
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, typeAliasPair.getLeft().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, typeAliasPair.getRight().getLocationInfo(), LANG2);
            if (srcSubTree == null || dstSubTree == null) return;
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        }
    }

    private void processNamedExportList(Tree srcTree, Tree dstTree, UMLNamedExportListDiff namedExportListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLNamedExport, UMLNamedExport> namedExportPair : namedExportListDiff.getCommonExports()) {
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, namedExportPair.getLeft().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, namedExportPair.getRight().getLocationInfo(), LANG2);
            if (srcSubTree == null || dstSubTree == null) return;
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        }
        for (UMLNamedExportDiff diff : namedExportListDiff.getChangedExports()) {
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, diff.getOriginalNamedExport().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, diff.getNextNamedExport().getLocationInfo(), LANG2);
            if (srcSubTree == null || dstSubTree == null) return;
            mappingStore.addMapping(srcSubTree,dstSubTree);
            Pair<Tree, Tree> exports = Helpers.findPairOfType(srcSubTree,dstSubTree, LANG1.EXPORT_KEYWORD, LANG2.EXPORT_KEYWORD);
            if(exports != null) {
                mappingStore.addMapping(exports.first, exports.second);
            }
            Pair<Tree, Tree> export_clauses = Helpers.findPairOfType(srcSubTree,dstSubTree, LANG1.EXPORT_CLAUSE, LANG2.EXPORT_CLAUSE);
            if(export_clauses != null) {
                mappingStore.addMapping(export_clauses.first, export_clauses.second);
                Pair<Tree,Tree> opening = Helpers.findPairOfType(export_clauses.first,export_clauses.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree,Tree> closing = Helpers.findPairOfType(export_clauses.first,export_clauses.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
            }
            Pair<Tree, Tree> froms = Helpers.findPairOfType(srcSubTree,dstSubTree, LANG1.FROM_KEYWORD, LANG2.FROM_KEYWORD);
            if(froms != null) {
                mappingStore.addMapping(froms.first, froms.second);
            }
            Pair<Tree, Tree> strings = Helpers.findPairOfType(srcSubTree,dstSubTree, LANG1.STRING, LANG2.STRING);
            if(strings != null) {
                mappingStore.addMappingRecursively(strings.first, strings.second);
            }
            Pair<Tree, Tree> semicolons = Helpers.findPairOfType(srcSubTree,dstSubTree, LANG1.SEMICOLON, LANG2.SEMICOLON);
            if(semicolons != null) {
                mappingStore.addMapping(semicolons.first, semicolons.second);
            }
            for(org.apache.commons.lang3.tuple.Pair<LeafExpression, LeafExpression> pair : diff.getCommonSpecifiers()) {
                Tree srcSpecifier = TreeUtilFunctions.findByLocationInfo(srcSubTree, pair.getLeft().getLocationInfo(), LANG1);
                Tree dstSpecifier = TreeUtilFunctions.findByLocationInfo(dstSubTree, pair.getRight().getLocationInfo(), LANG2);
                if (srcSpecifier != null && dstSpecifier != null) {
                    mappingStore.addMappingRecursively(srcSpecifier,dstSpecifier);
                    Tree srcParent = srcSpecifier.getParent();
                    Tree dstParent = dstSpecifier.getParent();
                    if(srcParent.getType().name.equals(LANG1.EXPORT_SPECIFIER) && dstParent.getType().name.equals(LANG2.EXPORT_SPECIFIER)) {
                        mappingStore.addMappingRecursively(srcParent, dstParent);
                        int index1 = srcParent.getParent().getChildPosition(srcParent);
                        int index2 = dstParent.getParent().getChildPosition(dstParent);
                        if(srcParent.getParent().getChildren().size() > index1+1 && srcParent.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                                dstParent.getParent().getChildren().size() > index2+1 && dstParent.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                            Tree t1 = srcParent.getParent().getChild(index1+1);
                            Tree t2 = dstParent.getParent().getChild(index2+1);
                            mappingStore.addMapping(t1,t2);
                        }
                    }
                    else if(srcSpecifier.getType().name.equals(LANG1.EXPORT_SPECIFIER) && dstSpecifier.getType().name.equals(LANG2.EXPORT_SPECIFIER)) {
                        int index1 = srcSpecifier.getParent().getChildPosition(srcSpecifier);
                        int index2 = dstSpecifier.getParent().getChildPosition(dstSpecifier);
                        if(srcSpecifier.getParent().getChildren().size() > index1+1 && srcSpecifier.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                                dstSpecifier.getParent().getChildren().size() > index2+1 && dstSpecifier.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                            Tree t1 = srcSpecifier.getParent().getChild(index1+1);
                            Tree t2 = dstSpecifier.getParent().getChild(index2+1);
                            mappingStore.addMapping(t1,t2);
                        }
                    }
                }
            }
        }
    }

    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo(), LANG1);
            if(srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(LANG1.DECORATOR))
                srcClassAnnotationTree = srcClassAnnotationTree.getParent();
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo(), LANG2);
            if(dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(LANG1.DECORATOR))
                dstClassAnnotationTree = dstClassAnnotationTree.getParent();
            if (srcClassAnnotationTree == null || dstClassAnnotationTree == null) continue;
            if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
                mappingStore.addMappingRecursively(srcClassAnnotationTree, dstClassAnnotationTree);
            else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                JavaToKotlinMigration.handleAnnotationMapping(mappingStore, srcClassAnnotationTree, dstClassAnnotationTree, LANG1, LANG2);
            }
            else {
                new IgnoringCommentsLeafMatcher(LANG1, LANG2).match(srcClassAnnotationTree, dstClassAnnotationTree, mappingStore);
            }
            //processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
            if(annotationListDiff.getCommonAnnotations().size() > 1 &&
                    srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(LANG1.MODIFIERS) &&
                    dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(LANG2.MODIFIERS)) {
                mappingStore.addMapping(srcClassAnnotationTree.getParent(), dstClassAnnotationTree.getParent());
            }
        }
    }
}
