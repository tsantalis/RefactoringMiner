package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.matchers.statement.CompositeMatcher;
import org.refactoringminer.astDiff.matchers.statement.IgnoringCommentsLeafMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.refactoringminer.util.PathFileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class BodyMapperMatcher extends OptimizationAwareMatcher {


    protected final UMLOperationBodyMapper bodyMapper;
    protected final boolean isPartOfExtractedMethod;
    protected final Constants LANG1;
    protected final Constants LANG2;

    public BodyMapperMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod, Constants LANG1, Constants LANG2) {
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }
    public BodyMapperMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore,isPartOfExtractedMethod);
    }
    private void processBodyMapper(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        if (bodyMapper.getAnonymousClassDiffs() != null) {
            for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
                new ClassAttrMatcher(optimizationData, anonymousClassDiff, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                for (UMLOperationBodyMapper umlOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                    new MethodMatcher(optimizationData, umlOperationBodyMapper, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
                for (UMLClassDiff nestedClassDiff : anonymousClassDiff.getNestedClassDiffList()) {
                    new ClassDiffMatcher(optimizationData, nestedClassDiff, true, Collections.emptyList(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
                }
                //the comments inside the anonymous class, which are not inside its operations
                new CommentMatcher(optimizationData, anonymousClassDiff.getCommentListDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                //the object literals of JavaScript and TypeScript and the dictionaries of Python, which the anonymous classes of the other languages are not
                if(!hasObjectLiterals(anonymousClassDiff.getOriginalClass().getLocationInfo().getFilePath()) || !hasObjectLiterals(anonymousClassDiff.getNextClass().getLocationInfo().getFilePath())) {
                    continue;
                }
                Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,anonymousClassDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT,LANG1.DICTIONARY);
                Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,anonymousClassDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT,LANG2.DICTIONARY);
                if(srcTypeDeclaration != null && dstTypeDeclaration != null) {
                    Tree object1 = srcTypeDeclaration;
                    Tree object2 = dstTypeDeclaration;
                    mappingStore.addMapping(object1,object2);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(object1,object2, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (opening != null) {
                        mappingStore.addMapping(opening.first,opening.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(object1,object2, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (closing != null) {
                        mappingStore.addMapping(closing.first,closing.second);
                    }
                    MethodMatcher.processObjectLiteralWithinMethodCall(object1, object2, mappingStore, LANG1, LANG2);
                }
            }
        }
        Set<AbstractCodeMapping> mappingSet = bodyMapper.getMappings();
        ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
        for (AbstractCodeMapping abstractCodeMapping : mappings) {
            if (abstractCodeMapping instanceof LeafMapping) {
                if (optimizationData.getProcessedLeafMappings().add(abstractCodeMapping)) {
                    processLeafMapping(srcTree,dstTree,abstractCodeMapping,mappingStore, isPartOfExtractedMethod);
                    if (containsReplacementOfType(abstractCodeMapping, ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION)) {
                        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo(), LANG1);
                        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo(), LANG2);
                        if (srcStatementNode != null && dstStatementNode != null)
                            alignAssignmentReplacedWithSetterCall(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                    }
                }
            }
            else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
                processCompositeMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
        }
        if (Constants.isCrossLanguage(LANG1, LANG2) && bodyMapper.getOperationInvocation() != null)
            processAnonymousArgumentReplacedWithLambda(srcTree, dstTree, bodyMapper, mappingStore);
        if (isPartOfExtractedMethod)
            new JavaDocMatcher(optimizationData, bodyMapper.getContainer1().getJavadoc(), bodyMapper.getContainer2().getJavadoc(), bodyMapper.getJavadocDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);
        new CommentMatcher(optimizationData, bodyMapper.getCommentListDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);

    }

    private static boolean containsReplacementOfType(AbstractCodeMapping mapping, ReplacementType type) {
        for (Replacement replacement : mapping.getReplacements()) {
            if (replacement.getType().equals(type))
                return true;
        }
        return false;
    }

    //object.name = value replaced with object._set('name', value), where the values are mapped
    protected static void alignAssignmentWithSetterCall(Tree value1, Tree value2, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Tree assignment1 = value1.getParent();
        Tree arguments2 = value2.getParent();
        Tree call2 = arguments2.getParent();
        //remove the arguments node, so that the values have mapped parents and are not reported as moved
        Set<Tree> srcs = mappingStore.getSrcs(arguments2);
        if(srcs != null) {
            for(Tree src : new ArrayList<>(srcs)) {
                mappingStore.removeMapping(src, arguments2);
            }
        }
        int index = call2.getChildPosition(arguments2);
        call2.getChildren().remove(index);
        for(Tree argumentChild : new ArrayList<>(arguments2.getChildren())) {
            call2.insertChild(argumentChild, index++);
        }
        mappingStore.addMapping(assignment1, call2);
        if(assignment1.getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT) && call2.getParent().getType().name.equals(LANG2.EXPRESSION_STATEMENT)) {
            Tree statement1 = assignment1.getParent();
            Tree statement2 = call2.getParent();
            mappingStore.addMapping(statement1, statement2);
            com.github.gumtreediff.utils.Pair<Tree,Tree> semicolons = Helpers.findPairOfType(statement1, statement2,LANG1.SEMICOLON,LANG2.SEMICOLON);
            if (semicolons != null) {
                mappingStore.addMapping(semicolons.first, semicolons.second);
            }
        }
    }

    //aligns the statements of a leaf mapping where object.name = value; is replaced with object._set('name', value);
    private static void alignAssignmentReplacedWithSetterCall(Tree statement1, Tree statement2, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        if(!statement1.getType().name.equals(LANG1.EXPRESSION_STATEMENT) || !statement2.getType().name.equals(LANG2.EXPRESSION_STATEMENT) ||
                statement1.getChildren().isEmpty() || statement2.getChildren().isEmpty()) {
            return;
        }
        Tree assignment1 = statement1.getChild(0);
        Tree call2 = statement2.getChild(0);
        if(!assignment1.getType().name.equals(LANG1.ASSIGNMENT_EXPRESSION) || !call2.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            return;
        }
        Tree arguments2 = TreeUtilFunctions.findChildByType(call2, LANG2.METHOD_INVOCATION_ARGUMENTS);
        if(arguments2 == null || !propertyNamePassedAsArgument(assignment1, arguments2, LANG1, LANG2)) {
            return;
        }
        Tree value1 = assignment1.getChild(assignment1.getChildren().size()-1);
        for(Tree argument2 : arguments2.getChildren()) {
            if(value1.isIsomorphicTo(argument2)) {
                mappingStore.addMappingRecursively(value1, argument2);
                alignAssignmentWithSetterCall(value1, argument2, mappingStore, LANG1, LANG2);
                return;
            }
        }
    }

    private static boolean stringArgument(Tree arguments, String value, Constants LANG) {
        for(Tree argument : arguments.getChildren()) {
            if(argument.getType().name.equals(LANG.STRING)) {
                for(Tree stringChild : argument.getChildren()) {
                    if(stringChild.getLabel().equals(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //the name of the assigned property is passed as a string argument, i.e., object.name = value replaced with object._set('name', value)
    protected static boolean propertyNamePassedAsArgument(Tree assignment1, Tree arguments2, Constants LANG1, Constants LANG2) {
        Tree leftHandSide1 = assignment1.getChild(0);
        if(!leftHandSide1.getType().name.equals(LANG1.MEMBER_EXPRESSION) || leftHandSide1.getChildren().isEmpty()) {
            return false;
        }
        Tree property1 = leftHandSide1.getChild(leftHandSide1.getChildren().size()-1);
        if(!property1.getType().name.equals(LANG1.PROPERTY_IDENTIFIER)) {
            return false;
        }
        return stringArgument(arguments2, property1.getLabel(), LANG2);
    }

    //post-processing of the call site of the inlined method, whose argument is an anonymous class replaced with a lambda,
    //i.e., pushExecutorExecute(new NamedRunnable("OkHttp %s", connectionName) {...}) inlined as pushExecutor.execute("OkHttp $connectionName") {...}
    private void processAnonymousArgumentReplacedWithLambda(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore) {
        //the extracted method mapper shares container1 with its parent mapper, while the call site is in container2 of the parent mapper
        if (bodyMapper.getParentMapper() != null && bodyMapper.getContainer1().equals(bodyMapper.getParentMapper().getContainer1())) {
            processAnonymousArgumentReplacedWithLambdaInExtractedMethod(srcTree, dstTree, bodyMapper, mappingStore);
            return;
        }
        Tree call1 = TreeUtilFunctions.findByLocationInfo(srcTree, bodyMapper.getOperationInvocation().getLocationInfo(), LANG1, LANG1.METHOD_INVOCATION);
        if (call1 == null)
            return;
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping instanceof LeafMapping && mapping.getReplacements().stream().anyMatch(r -> r.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_LAMBDA))) {
                Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree, mapping.getFragment2().getLocationInfo(), LANG2);
                if (dstStatementNode != null) {
                    JavaToKotlinMigration.handleAnonymousArgumentReplacedWithLambda(mappingStore, call1, dstStatementNode, LANG1, LANG2, optimizationData.getDeferredFlattenings());
                }
            }
        }
    }

    //post-processing of the call site of the extracted method, whose argument is a lambda replacing an anonymous class passed as argument in the extracted statement,
    //i.e., pushExecutor.execute(new NamedRunnable("OkHttp %s", connectionName) {...}) extracted as pushExecutorExecute("OkHttp $connectionName") {...}
    private void processAnonymousArgumentReplacedWithLambdaInExtractedMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore) {
        Tree callSite2 = TreeUtilFunctions.findByLocationInfo(dstTree, bodyMapper.getOperationInvocation().getLocationInfo(), LANG2, LANG2.METHOD_INVOCATION);
        if (callSite2 == null)
            return;
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping instanceof LeafMapping && mapping.getReplacements().stream().anyMatch(r -> r.getType().equals(Replacement.ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA))) {
                AbstractCall invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
                if (invocation1 == null)
                    continue;
                Tree call1 = TreeUtilFunctions.findByLocationInfo(srcTree, invocation1.getLocationInfo(), LANG1, LANG1.METHOD_INVOCATION);
                Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree, mapping.getFragment2().getLocationInfo(), LANG2);
                if (call1 != null && dstStatementNode != null) {
                    JavaToKotlinMigration.handleAnonymousArgumentReplacedWithLambdaInExtractedMethod(mappingStore, call1, dstStatementNode, callSite2, LANG1, LANG2, optimizationData.getDeferredFlattenings());
                }
            }
        }
    }

    private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        LocationInfo srcLocationInfo = compositeStatementObjectMapping.getFragment1().getLocationInfo();
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcLocationInfo,LANG1);
        LocationInfo dstLocationInfo = compositeStatementObjectMapping.getFragment2().getLocationInfo();
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstLocationInfo,LANG2);
        //the Kotlin else branch has the same range as the nested if of an else if, i.e., if (a) {...} else if (b) {...}
        //in a cross-language diff, the else branch is flattened by JavaToKotlinMigration.handleElseIfMapping, as Java has no counterpart
        if (!Constants.isCrossLanguage(LANG1, LANG2) && compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
                srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.CONTROL_STRUCTURE_BODY) &&
                srcStatementNode.getChildren().size() == 1 && srcStatementNode.getChild(0).getType().name.equals(LANG1.IF_STATEMENT)) {
            srcStatementNode = srcStatementNode.getChild(0);
        }
        if (!Constants.isCrossLanguage(LANG1, LANG2) && compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
                dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.CONTROL_STRUCTURE_BODY) &&
                dstStatementNode.getChildren().size() == 1 && dstStatementNode.getChild(0).getType().name.equals(LANG2.IF_STATEMENT)) {
            dstStatementNode = dstStatementNode.getChild(0);
        }
        if(Constants.isCrossLanguage(LANG1, LANG2) && dstStatementNode.getType().name.equals(LANG2.STATEMENTS) && abstractCodeMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
            Tree statements2 = dstStatementNode;
            dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstLocationInfo, LANG2, LANG2.TRY_STATEMENT);
            //ignoreIoExceptions {} is modeled as a try statement, and it is the only statement of the enclosing statements with the same location
            if((dstStatementNode == null || !dstStatementNode.getType().name.equals(LANG2.TRY_STATEMENT)) && statements2.getChildren().size() == 1 &&
                    statements2.getChild(0).getType().name.equals(LANG2.METHOD_INVOCATION)) {
                dstStatementNode = statements2.getChild(0);
            }
        }
        //the statements of a lambda aligned with a Java Block are moved to the lambda_literal including the braces of the block, i.e., synchronized(this) {...}
        if(Constants.isCrossLanguage(LANG1, LANG2) && dstStatementNode != null && dstLocationInfo.getCodeElementType().equals(CodeElementType.BLOCK) &&
                !dstStatementNode.getType().name.equals(LANG2.STATEMENTS) &&
                dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LAMBDA_LITERAL)) {
            Tree lambdaLiteral2 = dstStatementNode.getParent();
            Tree first2 = lambdaLiteral2.getChild(0);
            Tree last2 = lambdaLiteral2.getChild(lambdaLiteral2.getChildren().size() - 1);
            if(first2.getPos() == dstLocationInfo.getStartOffset() && last2.getEndPos() == dstLocationInfo.getEndOffset()) {
                //the lambda_literal matching the body of an anonymous class method is mapped to the anonymous class, and the method body is deleted
                Set<Tree> lambdaSrcs = mappingStore.getSrcs(lambdaLiteral2);
                if(lambdaSrcs != null && srcStatementNode != null && !lambdaSrcs.contains(srcStatementNode))
                    return;
                dstStatementNode = lambdaLiteral2;
            }
        }
        if(Constants.isCrossLanguage(LANG1, LANG2) && srcStatementNode != null && dstStatementNode != null &&
                srcStatementNode.getType().name.equals(LANG1.IF_STATEMENT) && dstLocationInfo.getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
            //the Kotlin else-if is nested in a control_structure_body with the same range, which is located instead of the if_expression
            dstStatementNode = JavaToKotlinMigration.handleElseIfMapping(mappingStore, dstStatementNode, LANG2, optimizationData.getDeferredFlattenings());
        }
        {
            if (srcStatementNode == null || dstStatementNode == null)
                return;
            if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name)) {
                mappingStore.addMapping(srcStatementNode,dstStatementNode);
                if(srcStatementNode.getType().name.equals(LANG1.STATEMENT_BLOCK) && dstStatementNode.getType().name.equals(LANG2.STATEMENT_BLOCK)) {
                    Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.CLASS_BLOCK) && dstStatementNode.getType().name.equals(LANG2.CLASS_BLOCK) &&
                        srcStatementNode.getParent().getType().name.equals(LANG1.ELSE) && dstStatementNode.getParent().getType().name.equals(LANG2.ELSE)) {
                    //the else blocks are mapped by the model, even when they belong to different clauses, i.e., an elif is added before the else
                    Tree else1 = srcStatementNode.getParent();
                    Tree else2 = dstStatementNode.getParent();
                    mappingStore.addMapping(else1, else2);
                    Pair<Tree, Tree> elses = Helpers.findPairOfType(else1, else2, LANG1.ELSE_KEYWORD, LANG2.ELSE_KEYWORD);
                    if (elses != null) {
                        mappingStore.addMapping(elses.first,elses.second);
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.COMPOUND_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.COMPOUND_STATEMENT)) {
                    Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                //the composite is the only statement of the statements node, while a block is the statements node itself
                if(srcStatementNode.getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getType().name.equals(LANG2.STATEMENTS) && !srcLocationInfo.getCodeElementType().equals(CodeElementType.BLOCK)) {
                    Tree srcFirstChild = srcStatementNode.getChild(0);
                    Tree dstFirstChild = dstStatementNode.getChild(0);
                    mappingStore.addMapping(srcFirstChild, dstFirstChild);
                    if (srcFirstChild.getType().name.equals(LANG1.WHEN_EXPRESSION) && dstFirstChild.getType().name.equals(LANG1.WHEN_EXPRESSION)) {
                        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcFirstChild,dstFirstChild, LANG1.WHEN_SUBJECT, LANG2.WHEN_SUBJECT);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                    if (srcFirstChild.getType().name.equals(LANG1.FOR_STATEMENT) && dstFirstChild.getType().name.equals(LANG2.FOR_STATEMENT)) {
                        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcFirstChild,dstFirstChild, LANG1.COLLECTION_ITERATED, LANG2.COLLECTION_ITERATED);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                }
                if(compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
                        compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
                        srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) &&
                        srcStatementNode.getChildren().size() > 1 && dstStatementNode.getChildren().size() > 1) {
                    //handle ignoreIoException {} in Kotlin to Kotlin
                    processCallExpressionsModeledAsCompositeStatements(mappingStore, srcStatementNode, dstStatementNode);
                }
                if(compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
                        compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
                        srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) &&
                        srcStatementNode.getChildren().size() > 1 && dstStatementNode.getChildren().size() > 1 &&
                        JavaToKotlinMigration.isRequireOrCheckCall(srcStatementNode, LANG1) && JavaToKotlinMigration.isRequireOrCheckCall(dstStatementNode, LANG2)) {
                    //handle require/check(condition) {} in Kotlin to Kotlin
                    processCallExpressionsModeledAsCompositeStatements(mappingStore, srcStatementNode, dstStatementNode);
                }
                if(compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
                        compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
                        srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) &&
                        srcStatementNode.getChildren().size() > 1 && dstStatementNode.getChildren().size() > 1) {
                    //handle synchronized(lock) {} in Kotlin to Kotlin
                    processCallExpressionsModeledAsCompositeStatements(mappingStore, srcStatementNode, dstStatementNode);
                }
                if(srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
                    Tree parent1 = srcStatementNode.getParent();
                    Tree parent2 = dstStatementNode.getParent();
                    mappingStore.addMapping(parent1, parent2);
                }
            }
            else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                boolean skip = srcStatementNode.getType().name.equals(LANG1.BLOCK) && dstStatementNode.getType().name.equals(LANG2.STATEMENTS) && !dstStatementNode.getParent().getType().name.equals(LANG2.LAMBDA_LITERAL);
                if(!skip) {
                    //the Java finally block has no node for the finally keyword, so it is the counterpart of the statements of the Kotlin finally block,
                    //which are flattened into the try expression like the Java finally block in the try statement
                    Tree finallyStatements2 = srcStatementNode.getType().name.equals(LANG1.BLOCK) && dstStatementNode.getType().name.equals(LANG2.FINALLY_BLOCK) ?
                            TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.STATEMENTS) : null;
                    if(finallyStatements2 != null) {
                        mappingStore.addMapping(srcStatementNode, finallyStatements2);
                        optimizationData.getDeferredFlattenings().flattenKeepingMappings(dstStatementNode.getParent(), dstStatementNode);
                        return;
                    }
                    mappingStore.addMapping(srcStatementNode, dstStatementNode);
                    JavaToKotlinMigration.handleCompositeMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, optimizationData.getDeferredFlattenings());
                    return;
                }
            }
            //a Python elif clause is an if nested in the clause before it, like in the model, where each elif is mapped on its own
            if ((srcStatementNode.getType().name.equals(LANG1.IF_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.IF_STATEMENT)) ||
                    (srcStatementNode.getType().name.equals(LANG1.ELSE_IF) && dstStatementNode.getType().name.equals(LANG2.ELSE_IF))) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //if expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE, LANG2.ELSE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    Pair<Tree, Tree> elses = Helpers.findPairOfType(matched.first,matched.second, LANG1.ELSE_KEYWORD, LANG2.ELSE_KEYWORD);
                    if (elses != null) {
                        mappingStore.addMapping(elses.first,elses.second);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.IF_KEYWORD, LANG2.IF_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELIF_KEYWORD, LANG2.ELIF_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONSTEXPR, LANG2.CONSTEXPR);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matchConditionClauses(srcStatementNode, dstStatementNode, mappingStore);
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHEN_ENTRY) && dstStatementNode.getType().name.equals(LANG2.WHEN_ENTRY)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARROW, LANG2.ARROW);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHEN_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.WHEN_EXPRESSION)) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //when expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHEN_SUBJECT, LANG2.WHEN_SUBJECT);
                if (matched != null) {
                    //when subject can be a property declaration, thus we need to match it recursively
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.FOR_STATEMENT)) {
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLLECTION_ITERATED, LANG2.COLLECTION_ITERATED);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SUBSCRIPT, LANG2.SUBSCRIPT);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CALL, LANG2.CALL);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DECLARATION, LANG2.DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.VARIABLE_DECLARATION, LANG2.VARIABLE_DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TUPLE_PATTERN, LANG2.TUPLE_PATTERN);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PATTERN_LIST, LANG2.PATTERN_LIST);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.EMPTY_STATEMENT, LANG2.EMPTY_STATEMENT);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SEMICOLON, LANG2.SEMICOLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.UPDATE_EXPRESSION, LANG2.UPDATE_EXPRESSION);
                if (matched != null) {
                    Tree t1 = matched.first;
                    Tree t2 = matched.second;
                    int index1 = t1.getParent().getChildPosition(t1);
                    int index2 = t2.getParent().getChildPosition(t2);
                    if(index1 > 0 && t1.getParent().getChild(index1-1).getType().name.equals(LANG1.SEMICOLON) &&
                            index2 > 0 && t2.getParent().getChild(index2-1).getType().name.equals(LANG2.SEMICOLON)) {
                        Tree tt1 = t1.getParent().getChild(index1-1);
                        Tree tt2 = t2.getParent().getChild(index2-1);
                        mappingStore.addMapping(tt1,tt2);
                    }
                }
                List<Pair<Tree, Tree>> matchedPairs = Helpers.findPairsOfType(srcStatementNode,dstStatementNode, LANG1.SEMICOLON, LANG2.SEMICOLON);
                for(int i=0; i<matchedPairs.size(); i++) {
                    mappingStore.addMapping(matchedPairs.get(i).first,matchedPairs.get(i).second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.LEXICAL_DECLARATION, LANG2.LEXICAL_DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_RANGE_LOOP) && dstStatementNode.getType().name.equals(LANG2.FOR_RANGE_LOOP)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    int index1 = srcStatementNode.getChildPosition(matched.first);
                    int index2 = dstStatementNode.getChildPosition(matched.second);
                    if(srcStatementNode.getChildren().size() > index1+1 && srcStatementNode.getChild(index1+1).getType().name.equals(LANG1.SIMPLE_NAME) &&
                            dstStatementNode.getChildren().size() > index2+1 && dstStatementNode.getChild(index2+1).getType().name.equals(LANG2.SIMPLE_NAME)) {
                        Tree t1 = srcStatementNode.getChild(index1+1);
                        Tree t2 = dstStatementNode.getChild(index2+1);
                        mappingStore.addMapping(t1,t2);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PLACEHOLDER_TYPE_SPECIFIER, LANG2.PLACEHOLDER_TYPE_SPECIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.STRUCTURED_BINDING_DECLARATOR, LANG2.STRUCTURED_BINDING_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TEMPLATE_TYPE, LANG2.TEMPLATE_TYPE);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_QUALIFIER, LANG2.TYPE_QUALIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.INITIALIZER_LIST, LANG2.INITIALIZER_LIST);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_IDENTIFIER, LANG2.TYPE_IDENTIFIER);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.QUALIFIED_IDENTIFIER, LANG2.QUALIFIED_IDENTIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.REFERENCE_DECLARATOR, LANG2.REFERENCE_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.POINTER_DECLARATOR, LANG2.POINTER_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.BINARY_EXPRESSION, LANG2.BINARY_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.POINTER_EXPRESSION, LANG2.POINTER_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SUBSCRIPT_EXPRESSION, LANG2.SUBSCRIPT_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITIONAL_EXPRESSION, LANG2.CONDITIONAL_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.METHOD_INVOCATION, LANG2.METHOD_INVOCATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FIELD_EXPRESSION, LANG2.FIELD_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COMPOUND_LITERAL_EXPRESSION, LANG2.COMPOUND_LITERAL_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PRIMITIVE_TYPE, LANG2.PRIMITIVE_TYPE);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_IN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.FOR_IN_STATEMENT)) {
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OF_KEYWORD, LANG2.OF_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.IN_KEYWORD, LANG2.IN_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> objectPattern = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OBJECT_PATTERN, LANG2.OBJECT_PATTERN);
                if (objectPattern != null) {
                    mappingStore.addMappingRecursively(objectPattern.first,objectPattern.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONST_KEYWORD, LANG2.CONST_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.VAR_KEYWORD, LANG2.VAR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.LET_KEYWORD, LANG2.LET_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARRAY_PATTERN, LANG2.ARRAY_PATTERN);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.SWITCH_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SWITCH_KEYWORD, LANG2.SWITCH_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matchConditionClauses(srcStatementNode, dstStatementNode, mappingStore);
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SWITCH_BODY, LANG2.SWITCH_BODY);
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
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_CASE) && dstStatementNode.getType().name.equals(LANG2.SWITCH_CASE)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CASE_KEYWORD, LANG2.CASE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_DEFAULT) && dstStatementNode.getType().name.equals(LANG2.SWITCH_DEFAULT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DEFAULT_KEYWORD, LANG2.DEFAULT_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHILE_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.WHILE_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHILE_KEYWORD, LANG2.WHILE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matchConditionClauses(srcStatementNode, dstStatementNode, mappingStore);
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.DO_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.DO_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHILE_KEYWORD, LANG2.WHILE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DO_KEYWORD, LANG2.DO_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SEMICOLON, LANG2.SEMICOLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if ((srcStatementNode.getType().name.equals(LANG1.TRY_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.TRY_STATEMENT)) ||
                    (srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE))) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //try expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                matchBlocks(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                //the empty Kotlin try, catch and finally blocks, which have no mapped statements
                matchEmptyStatements(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                Pair<Tree, Tree> finallyBlocks = Helpers.findPairOfType(srcStatementNode, dstStatementNode, LANG1.FINALLY_BLOCK, LANG2.FINALLY_BLOCK);
                if (finallyBlocks != null)
                    matchEmptyStatements(finallyBlocks.first, finallyBlocks.second, mappingStore, LANG1, LANG2);
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            } else if (!srcStatementNode.getType().name.equals(LANG1.BLOCK) && !dstStatementNode.getType().name.equals(LANG2.BLOCK)) {
                //the conditions of composites of different kinds, i.e., if (f(x)) -> switch (g(x))
                matchConditionClauses(srcStatementNode, dstStatementNode, mappingStore);
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode, dstStatementNode, mappingStore);
            }
            matchChildrenWithMappedContent(srcStatementNode, dstStatementNode, LANG1.CONTROL_STRUCTURE_BODY, LANG2.CONTROL_STRUCTURE_BODY, mappingStore);
            matchChildrenWithMappedContent(srcStatementNode, dstStatementNode, LANG1.WHEN_CONDITION, LANG2.WHEN_CONDITION, mappingStore);
        }
        if (!abstractCodeMapping.getRefactorings().isEmpty()) {
            leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
        }
    }

    //the condition clauses of the mapped composites with their parentheses, i.e., (x) in if (x) and switch (x)
    private void matchConditionClauses(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
        Pair<Tree, Tree> conditionClauses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITION_CLAUSE, LANG2.CONDITION_CLAUSE);
        if (conditionClauses != null) {
            mappingStore.addMapping(conditionClauses.first,conditionClauses.second);
            Pair<Tree, Tree> matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
            if (matched != null) {
                mappingStore.addMapping(matched.first,matched.second);
            }
            matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
            if (matched != null) {
                mappingStore.addMapping(matched.first,matched.second);
            }
        }
    }

    //the Kotlin children of the same type of a mapped composite, with mapped content, i.e., the branches of an if, a loop or a when entry, and the conditions of a when entry,
    //i.e., if (x) throw e -> if (x) { throw e } and A.B.X, A.B.Y -> ... -> X, Y -> ...
    //the content distinguishes the corresponding children from the children at the same position of reordered if clauses in an else if chain
    private void matchChildrenWithMappedContent(Tree srcStatementNode, Tree dstStatementNode, String type1, String type2, ExtendedMultiMappingStore mappingStore) {
        Set<Tree> dsts = mappingStore.getDsts(srcStatementNode);
        if (dsts == null || !dsts.contains(dstStatementNode))
            return;
        List<Tree> children1 = TreeUtilFunctions.findChildrenByType(srcStatementNode, type1);
        List<Tree> children2 = TreeUtilFunctions.findChildrenByType(dstStatementNode, type2);
        if (children1.size() != children2.size())
            return;
        for (int i = 0; i < children1.size(); i++) {
            Tree child1 = children1.get(i);
            Tree child2 = children2.get(i);
            if (!mappingStore.isSrcMapped(child1) && !mappingStore.isDstMapped(child2) && contentMappedWithin(child1, child2, mappingStore))
                mappingStore.addMapping(child1, child2);
        }
    }

    //a node within the first tree is mapped to a node within the second tree
    private static boolean contentMappedWithin(Tree tree1, Tree tree2, ExtendedMultiMappingStore mappingStore) {
        for (Tree t1 : tree1.getDescendants()) {
            Set<Tree> dsts = mappingStore.getDsts(t1);
            if (dsts == null)
                continue;
            for (Tree t2 : dsts) {
                for (Tree p = t2.getParent(); p != null; p = p.getParent()) {
                    if (p == tree2)
                        return true;
                }
            }
        }
        return false;
    }

    private void processCallExpressionsModeledAsCompositeStatements(ExtendedMultiMappingStore mappingStore,
            Tree srcStatementNode, Tree dstStatementNode) {
        if(srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
            mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
        }
        Tree name1 = srcStatementNode.getChild(0);
        Tree name2 = dstStatementNode.getChild(0);
        if(name1.getType().name.equals(LANG1.NAVIGATION_EXPRESSION) && name2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                name1.getChildren().size() == 2 && name2.getChildren().size() == 2 &&
                name1.getChild(1).getType().name.equals(LANG1.NAVIGATION_SUFFIX) && name2.getChild(1).getType().name.equals(LANG2.NAVIGATION_SUFFIX)) {
            //resource.use {}, the resource receiver is matched by the CompositeMatcher, as it is the expression of the try statement
            mappingStore.addMapping(name1, name2);
            mappingStore.addMappingRecursively(name1.getChild(1), name2.getChild(1));
        }
        else {
            mappingStore.addMappingRecursively(name1, name2);
        }
        Tree suffix1 = srcStatementNode.getChild(1);
        Tree suffix2 = dstStatementNode.getChild(1);
        if(suffix1.getType().name.equals(LANG1.CALL_SUFFIX) && suffix2.getType().name.equals(LANG2.CALL_SUFFIX)) {
            mappingStore.addMapping(suffix1, suffix2);
            if(suffix1.getChildren().size() > 0 && suffix2.getChildren().size() > 0 &&
                    suffix1.getChild(0).getType().name.equals(LANG1.ANNOTATED_LAMBDA) && suffix2.getChild(0).getType().name.equals(LANG2.ANNOTATED_LAMBDA)) {
                mappingStore.addMapping(suffix1.getChild(0), suffix2.getChild(0));
                if(suffix1.getChild(0).getChildren().size() > 0 && suffix2.getChild(0).getChildren().size() > 0 &&
                        suffix1.getChild(0).getChild(0).getType().name.equals(LANG1.LAMBDA_LITERAL) &&
                        suffix2.getChild(0).getChild(0).getType().name.equals(LANG2.LAMBDA_LITERAL)) {
                    Tree lambdaLiteral1 = suffix1.getChild(0).getChild(0);
                    Tree lambdaLiteral2 = suffix2.getChild(0).getChild(0);
                    mappingStore.addMapping(lambdaLiteral1, lambdaLiteral2);
                    //the statements follow the lambda parameters and the arrow, i.e., resource.use { r -> ...}
                    Tree statements1 = TreeUtilFunctions.findChildByType(lambdaLiteral1, LANG1.STATEMENTS);
                    Tree statements2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.STATEMENTS);
                    if(statements1 != null && statements2 != null) {
                        mappingStore.addMapping(statements1, statements2);
                    }
                    Tree parameters1 = TreeUtilFunctions.findChildByType(lambdaLiteral1, LANG1.LAMBDA_PARAMETERS);
                    Tree parameters2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.LAMBDA_PARAMETERS);
                    if(parameters1 != null && parameters2 != null && parameters1.getChildren().size() == parameters2.getChildren().size()) {
                        mappingStore.addMapping(parameters1, parameters2);
                        for(int i=0; i<parameters1.getChildren().size(); i++) {
                            mappingStore.addMappingRecursively(parameters1.getChild(i), parameters2.getChild(i));
                            //the parameter name, when the type is added or removed, i.e., use { graph -> ...} -> use { graph: HeapGraph -> ...}
                            Tree parameterName1 = TreeUtilFunctions.findChildByType(parameters1.getChild(i), LANG1.SIMPLE_NAME);
                            Tree parameterName2 = TreeUtilFunctions.findChildByType(parameters2.getChild(i), LANG2.SIMPLE_NAME);
                            if(parameterName1 != null && parameterName2 != null) {
                                mappingStore.addMapping(parameterName1, parameterName2);
                            }
                        }
                        Pair<Tree, Tree> arrows = Helpers.findPairOfType(lambdaLiteral1, lambdaLiteral2, "arrow", "arrow");
                        if(arrows != null) {
                            mappingStore.addMapping(arrows.first, arrows.second);
                        }
                    }
                }
            }
        }
    }

    //an empty block is a statements node spanning its braces, possibly with comments, which is not mapped as the parent of mapped statements
    private static void matchEmptyStatements(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Tree statements1 = findEmptyStatements(srcStatementNode, LANG1);
        Tree statements2 = findEmptyStatements(dstStatementNode, LANG2);
        if (statements1 != null && statements2 != null && !mappingStore.isSrcMapped(statements1) && !mappingStore.isDstMapped(statements2))
            mappingStore.addMapping(statements1, statements2);
    }

    private static Tree findEmptyStatements(Tree tree, Constants LANG) {
        for (Tree child : tree.getChildren()) {
            if (child.getType().name.equals(LANG.STATEMENTS) && child.getChildren().stream().allMatch(c -> c.getType().name.equals(LANG.LINE_COMMENT) || c.getType().name.equals(LANG.BLOCK_COMMENT)))
                return child;
        }
        return null;
    }

    public static void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.BLOCK, LANG2.BLOCK);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        if (matched == null) {
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
            if (blocks != null) {
                mappingStore.addMapping(blocks.first,blocks.second);
            }
            blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COMPOUND_STATEMENT, LANG2.COMPOUND_STATEMENT);
            if (blocks != null) {
                mappingStore.addMapping(blocks.first,blocks.second);
                matched = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
            }
            Pair<Tree, Tree> trys = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TRY_KEYWORD, LANG2.TRY_KEYWORD);
            if (trys != null) {
                mappingStore.addMapping(trys.first,trys.second);
            }
            Pair<Tree, Tree> catches = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CATCH_CLAUSE, LANG2.CATCH_CLAUSE);
            List<Tree> children1 = new ArrayList<>();
            for(Tree t : srcStatementNode.getChildren()) {
                if(t.getType().name.equals(LANG1.CATCH_CLAUSE))
                    children1.add(t);
            }
            List<Tree> children2 = new ArrayList<>();
            for(Tree t : dstStatementNode.getChildren()) {
                if(t.getType().name.equals(LANG2.CATCH_CLAUSE))
                    children2.add(t);
            }
            if (catches != null) {
                Pair<Tree, Tree> exceptions = Helpers.findPairOfType(catches.first,catches.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                boolean differentExceptionType = exceptions != null && !exceptions.first.getLabel().equals(exceptions.second.getLabel()) && (children1.size() > 1 || children2.size() > 1);
                if(!differentExceptionType) {
                    mappingStore.addMapping(catches.first,catches.second);
                    if (exceptions != null) {
                        mappingStore.addMapping(exceptions.first,exceptions.second);
                    }
                }
                Pair<Tree, Tree> attributeExceptions = Helpers.findPairOfType(catches.first,catches.second, LANG1.ATTRIBUTE_EXCEPTION, LANG2.ATTRIBUTE_EXCEPTION);
                if (attributeExceptions != null) {
                    mappingStore.addMappingRecursively(attributeExceptions.first,attributeExceptions.second);
                }
                Pair<Tree, Tree> catch_blocks = Helpers.findPairOfType(catches.first,catches.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (catch_blocks != null) {
                    mappingStore.addMapping(catch_blocks.first,catch_blocks.second);
                }
            }
            Pair<Tree, Tree> finallys = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FINALLY_CLAUSE, LANG2.FINALLY_CLAUSE);
            if (finallys != null) {
                mappingStore.addMapping(finallys.first,finallys.second);
                Pair<Tree, Tree> finally_keywords = Helpers.findPairOfType(finallys.first,finallys.second, LANG1.FINALLY_KEYWORD, LANG2.FINALLY_KEYWORD);
                if (finally_keywords != null) {
                    mappingStore.addMapping(finally_keywords.first,finally_keywords.second);
                }
                Pair<Tree, Tree> finally_blocks = Helpers.findPairOfType(finallys.first,finallys.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (finally_blocks != null) {
                    mappingStore.addMapping(finally_blocks.first,finally_blocks.second);
                }
            }
            Pair<Tree, Tree> elses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE, LANG2.ELSE);
            if (elses != null) {
                mappingStore.addMapping(elses.first,elses.second);
                Pair<Tree, Tree> finally_keywords = Helpers.findPairOfType(elses.first,elses.second, LANG1.ELSE_KEYWORD, LANG2.ELSE_KEYWORD);
                if (finally_keywords != null) {
                    mappingStore.addMapping(finally_keywords.first,finally_keywords.second);
                }
                Pair<Tree, Tree> finally_blocks = Helpers.findPairOfType(elses.first,elses.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (finally_blocks != null) {
                    mappingStore.addMapping(finally_blocks.first,finally_blocks.second);
                }
            }
            if(srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE)) {
                Pair<Tree, Tree> excepts = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.EXCEPT_KEYWORD, LANG2.EXCEPT_KEYWORD);
                if (excepts != null) {
                    mappingStore.addMapping(excepts.first,excepts.second);
                }
                Pair<Tree, Tree> catchKeywords = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CATCH_KEYWORD, LANG2.CATCH_KEYWORD);
                if (catchKeywords != null) {
                    mappingStore.addMapping(catchKeywords.first,catchKeywords.second);
                }
                Pair<Tree, Tree> asPatterns = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.AS_PATTERN, LANG2.AS_PATTERN);
                if (asPatterns != null) {
                    mappingStore.addMappingRecursively(asPatterns.first,asPatterns.second);
                }
                Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first,identifiers.second);
                }
                Pair<Tree, Tree> opening = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree, Tree> closing = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
                Pair<Tree, Tree> typeAnnotation = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_ANNOTATION, LANG2.TYPE_ANNOTATION);
                if (typeAnnotation != null) {
                    mappingStore.addMappingRecursively(typeAnnotation.first,typeAnnotation.second);
                }
                Pair<Tree,Tree> parameter_lists = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.PARAMETER_LIST,LANG2.PARAMETER_LIST);
                if (parameter_lists != null) {
                    mappingStore.addMapping(parameter_lists.first,parameter_lists.second);
                    opening = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (opening != null) {
                        mappingStore.addMapping(opening.first,opening.second);
                    }
                    closing = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (closing != null) {
                        mappingStore.addMapping(closing.first,closing.second);
                    }
                    Pair<Tree, Tree> ellipsis = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.ELLIPSIS, LANG2.ELLIPSIS);
                    if (ellipsis != null) {
                        mappingStore.addMapping(ellipsis.first,ellipsis.second);
                    }
                    Pair<Tree, Tree> parameterDeclarations = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.PARAMETER_DECLARATION, LANG2.PARAMETER_DECLARATION);
                    if (parameterDeclarations != null) {
                        mappingStore.addMapping(parameterDeclarations.first,parameterDeclarations.second);
                        identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                        if (identifiers != null) {
                            mappingStore.addMapping(identifiers.first,identifiers.second);
                        }
                        Pair<Tree, Tree> types = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.PRIMITIVE_TYPE, LANG2.PRIMITIVE_TYPE);
                        if (types != null) {
                            mappingStore.addMappingRecursively(types.first,types.second);
                        }
                        Pair<Tree, Tree> reference_declarators = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.REFERENCE_DECLARATOR, LANG2.REFERENCE_DECLARATOR);
                        if (reference_declarators != null) {
                            mappingStore.addMappingRecursively(reference_declarators.first,reference_declarators.second);
                        }
                        reference_declarators = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.ABSTRACT_REFERENCE_DECLARATOR, LANG2.ABSTRACT_REFERENCE_DECLARATOR);
                        if (reference_declarators != null) {
                            mappingStore.addMappingRecursively(reference_declarators.first,reference_declarators.second);
                        }
                        Pair<Tree, Tree> qualified_identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.QUALIFIED_IDENTIFIER, LANG2.QUALIFIED_IDENTIFIER);
                        if (qualified_identifiers != null) {
                            mappingStore.addMappingRecursively(qualified_identifiers.first,qualified_identifiers.second);
                        }
                        Pair<Tree, Tree> type_identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.TYPE_IDENTIFIER, LANG2.TYPE_IDENTIFIER);
                        if (type_identifiers != null) {
                            mappingStore.addMappingRecursively(type_identifiers.first,type_identifiers.second);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasObjectLiterals(String filePath) {
        return PathFileUtils.isJavaScriptFile(filePath) || PathFileUtils.isTypeScriptFile(filePath) || PathFileUtils.isPythonFile(filePath);
    }

    private static boolean unmapped(Set<Tree> mapped) {
        return mapped == null || mapped.isEmpty();
    }

    //the anonymous classes without operations matched through their attributes within the fragments of the mapping
    private List<UMLAnonymousClassDiff> matchedAnonymousClassDiffs(AbstractCodeMapping mapping, boolean initializer) {
        List<UMLAnonymousClassDiff> diffs = new ArrayList<>();
        if (initializer && (!mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_INITIALIZER) ||
                !mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_INITIALIZER)))
            return diffs;
        //in an initializer, the anonymous classes matched through their operations too, i.e., prompt: { start: message => ..., retry: ... }
        for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
            if ((initializer || bodyMapper.matchedThroughAttributes(anonymousClassDiff)) &&
                    mapping.getFragment1().getLocationInfo().subsumes(anonymousClassDiff.getOriginalClass().getLocationInfo()) &&
                    mapping.getFragment2().getLocationInfo().subsumes(anonymousClassDiff.getNextClass().getLocationInfo()))
                diffs.add(anonymousClassDiff);
        }
        return diffs;
    }

    //the leaves of the anonymous classes matched by the model are mapped through their attributes and operations, so within them
    //the leaves of the statement only complete the unmapped ones, i.e., the commas between the attributes of StyleSheet.create({...}) are not mapped again
    private void matchLeavesAroundAnonymousClasses(Tree srcTree, Tree dstTree, Tree srcStatementNode, Tree dstStatementNode,
            List<UMLAnonymousClassDiff> matchedAnonymousClassDiffs, ExtendedMultiMappingStore mappingStore) {
        ExtendedMultiMappingStore statementMappings = new ExtendedMultiMappingStore(srcTree, dstTree, LANG1, LANG2);
        new IgnoringCommentsLeafMatcher(LANG1, LANG2).match(srcStatementNode,dstStatementNode,statementMappings);
        for (Mapping mapping : statementMappings) {
            if (!withinAnonymousClass(mapping, matchedAnonymousClassDiffs) ||
                    (unmapped(mappingStore.getDsts(mapping.first)) && unmapped(mappingStore.getSrcs(mapping.second))))
                mappingStore.addMapping(mapping.first, mapping.second);
        }
    }

    private static boolean withinAnonymousClass(Mapping mapping, List<UMLAnonymousClassDiff> anonymousClassDiffs) {
        for (UMLAnonymousClassDiff anonymousClassDiff : anonymousClassDiffs) {
            LocationInfo location1 = anonymousClassDiff.getOriginalClass().getLocationInfo();
            LocationInfo location2 = anonymousClassDiff.getNextClass().getLocationInfo();
            if ((mapping.first.getPos() >= location1.getStartOffset() && mapping.first.getEndPos() <= location1.getEndOffset()) ||
                    (mapping.second.getPos() >= location2.getStartOffset() && mapping.second.getEndPos() <= location2.getEndOffset()))
                return true;
        }
        return false;
    }
    private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
        if(leafMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.PROBLEM_STATEMENT) && leafMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.PROBLEM_STATEMENT)) {
            Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo(),LANG1);
            Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo(),LANG2);
            if (srcStatementNode == null || dstStatementNode == null) {
                System.err.println("Tree not found for " + abstractCodeMapping);
                return;
            }
            if(srcStatementNode.getType().name.endsWith("_statement") && dstStatementNode.getType().name.endsWith("_statement")) {
                
            }
            else {
                while(srcStatementNode.getParent() != null && !srcStatementNode.getType().name.endsWith("_statement") && (!srcStatementNode.getType().name.equals(LANG1.FUNCTION_DECLARATOR) || srcStatementNode.getParent().getType().name.equals(LANG1.FUNCTION_DECLARATOR))) {
                    srcStatementNode = srcStatementNode.getParent();
                }
                while(dstStatementNode.getParent() != null && !dstStatementNode.getType().name.endsWith("_statement") && (!dstStatementNode.getType().name.equals(LANG2.FUNCTION_DECLARATOR) || dstStatementNode.getParent().getType().name.equals(LANG2.FUNCTION_DECLARATOR))) {
                    dstStatementNode = dstStatementNode.getParent();
                }
                if(srcStatementNode.getType().name.equals(dstStatementNode.getType().name)) {
                    mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
                    if(srcStatementNode.getParent().getType().name.equals(LANG1.COMPOUND_STATEMENT) && dstStatementNode.getParent().getType().name.equals(LANG2.COMPOUND_STATEMENT)) {
                        mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                    }
                    if(!isPartOfExtractedMethod && srcStatementNode.getParent().getType().name.equals(LANG1.METHOD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.METHOD_DECLARATION)) {
                        mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> compounds = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.COMPOUND_STATEMENT, LANG2.COMPOUND_STATEMENT);
                        if(compounds != null && compounds.first.isIsomorphicTo(compounds.second)) {
                            mappingStore.addMappingRecursively(compounds.first, compounds.second);
                        }
                    }
                }
            }
        }
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo(),LANG1);
        if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.ERROR)) {
            Tree tmpSrcStatement = TreeUtilFunctions.getParentUntilType(srcStatementNode, LANG1.EXPRESSION_STATEMENT);
            if(tmpSrcStatement != null)
                srcStatementNode = tmpSrcStatement;
        }
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo(),LANG2);
        if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.ERROR)) {
            Tree tmpDstStatement = TreeUtilFunctions.getParentUntilType(dstStatementNode, LANG2.EXPRESSION_STATEMENT);
            if(tmpDstStatement != null)
                dstStatementNode = tmpDstStatement;
        }
        if (srcStatementNode == null || dstStatementNode == null) {
            System.err.println("Tree not found for " + abstractCodeMapping);
            return;
        }
        //the blocks with a single statement correspond when their statements are mapped, i.e., if x: raise ValueError(...) -> if y: raise ValueError(...)
        if(srcStatementNode.getParent() != null && dstStatementNode.getParent() != null &&
                srcStatementNode.getParent().getType().name.equals(LANG1.CLASS_BLOCK) && dstStatementNode.getParent().getType().name.equals(LANG2.CLASS_BLOCK) &&
                srcStatementNode.getParent().getChildren().size() == 1 && dstStatementNode.getParent().getChildren().size() == 1) {
            mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
        }
        if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
            if(srcStatementNode.getType().name.equals(LANG1.ARROW_FUNCTION) && dstStatementNode.getType().name.equals(LANG2.ARROW_FUNCTION)) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                processArrowFunction(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                return;
            }
            else if(containsLambdaMapperWithNonMappedLeaves(abstractCodeMapping, srcStatementNode, dstStatementNode)) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                Pair<Tree,Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.SEMICOLON,LANG2.SEMICOLON);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                if(srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
                    //the statements are method invocations themselves, i.e., Kotlin calls with trailing lambda
                    matched = new Pair<>(srcStatementNode, dstStatementNode);
                    processCallWithTrailingLambda(srcStatementNode, dstStatementNode, mappingStore);
                }
                else {
                    matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.METHOD_INVOCATION,LANG2.METHOD_INVOCATION);
                }
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                    Pair<Tree,Tree> argument_lists = Helpers.findPairOfType(matched.first, matched.second,LANG1.ARGUMENT_LIST,LANG2.ARGUMENT_LIST);
                    if(argument_lists != null) {
                        mappingStore.addMapping(argument_lists.first, argument_lists.second);
                        Pair<Tree, Tree> opening = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        Pair<Tree, Tree> closing = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                        Pair<Tree, Tree> lambdas = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.LAMBDA_EXPRESSION, LANG2.LAMBDA_EXPRESSION);
                        if (lambdas != null) {
                            mappingStore.addMapping(lambdas.first,lambdas.second);
                            Pair<Tree, Tree> lambda_specifiers = Helpers.findPairOfType(lambdas.first,lambdas.second, LANG1.LAMBDA_CAPTURE_SPECIFIER, LANG2.LAMBDA_CAPTURE_SPECIFIER);
                            if(lambda_specifiers != null) {
                                mappingStore.addMappingRecursively(lambda_specifiers.first,lambda_specifiers.second);
                            }
                            Pair<Tree, Tree> function_declarators = Helpers.findPairOfType(lambdas.first,lambdas.second, LANG1.ABSTRACT_FUNCTION_DECLARATOR, LANG2.ABSTRACT_FUNCTION_DECLARATOR);
                            if(function_declarators != null) {
                                mappingStore.addMappingRecursively(function_declarators.first,function_declarators.second);
                            }
                        }
                    }
                    else {
                        processArgumentsWithArrowFunction(matched.first, matched.second, mappingStore);
                    }
                    Pair<Tree,Tree> field_expressions = Helpers.findPairOfType(matched.first, matched.second,LANG1.FIELD_EXPRESSION,LANG2.FIELD_EXPRESSION);
                    if(field_expressions != null) {
                        mappingStore.addMappingRecursively(field_expressions.first, field_expressions.second);
                    }
                }
                for(UMLOperationBodyMapper lambdaMapper : abstractCodeMapping.getLambdaMappers()) {
                    processBodyMapper(srcTree, dstTree, lambdaMapper, mappingStore, isPartOfExtractedMethod);
                }
                matchParentStatements(srcStatementNode, dstStatementNode, mappingStore);
                return;
            }
            else {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                //handle following error node
                int index1 = srcStatementNode.getParent().getChildren().indexOf(srcStatementNode);
                int index2 = dstStatementNode.getParent().getChildren().indexOf(dstStatementNode);
                if(srcStatementNode.getParent().getChildren().size() > index1+1 && srcStatementNode.getParent().getChild(index1+1).getType().name.equals(LANG1.ERROR) &&
                        dstStatementNode.getParent().getChildren().size() > index2+1 && dstStatementNode.getParent().getChild(index2+1).getType().name.equals(LANG2.ERROR)) {
                    Tree t1 = srcStatementNode.getParent().getChild(index1+1);
                    Tree t2 = dstStatementNode.getParent().getChild(index2+1);
                    if(t1.isIsomorphicTo(t2)) {
                        mappingStore.addMappingRecursively(t1, t2);
                    }
                }
                //handle parent labeled statement
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                boolean testFunctionCall = (abstractCodeMapping.getFragment1().getString().startsWith("describe(") && abstractCodeMapping.getFragment2().getString().startsWith("describe(")) ||
                        (abstractCodeMapping.getFragment1().getString().startsWith("it(") && abstractCodeMapping.getFragment2().getString().startsWith("it("));
                if(testFunctionCall &&
                        srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENT_BLOCK) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENT_BLOCK) &&
                        srcStatementNode.getParent().getParent() != null && srcStatementNode.getParent().getParent().getType().name.equals(LANG1.FUNCTION_EXPRESSION) &&
                        dstStatementNode.getParent().getParent() != null && dstStatementNode.getParent().getParent().getType().name.equals(LANG2.FUNCTION_EXPRESSION)) {
                    Tree grandParent1 = srcStatementNode.getParent().getParent();
                    Tree grandParent2 = dstStatementNode.getParent().getParent();
                    mappingStore.addMapping(grandParent1, grandParent2);
                    processArrowFunction(grandParent1, grandParent2, mappingStore, LANG1, LANG2);
                    Pair<Tree, Tree> blocks = Helpers.findPairOfType(grandParent1, grandParent2, LANG1.STATEMENT_BLOCK, LANG2.STATEMENT_BLOCK);
                    if(blocks != null) {
                        List<Tree> children1 = blocks.first.getChildren();
                        List<Tree> children2 = blocks.second.getChildren();
                        for(int i=1; i<children1.size(); i++) {
                            Tree child1 = children1.get(i);
                            Tree child2 = children2.get(i);
                            if(child1.isIsomorphicTo(child2)) {
                                mappingStore.addMappingRecursively(child1, child2);
                            }
                            else {
                                break;
                            }
                        }
                    }
                    if(grandParent1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION_ARGUMENTS) && grandParent2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION_ARGUMENTS)) {
                        MethodMatcher.processObjectLiteralWithinMethodCall(grandParent1, grandParent2, mappingStore, LANG1, LANG2);
                    }
                }
                //special handling for multiplication expression following return
                if(srcStatementNode.getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    Tree parent1 = srcStatementNode.getParent();
                    Tree parent2 = dstStatementNode.getParent();
                    index1 = parent1.getChildPosition(srcStatementNode);
                    index2 = parent2.getChildPosition(dstStatementNode);
                    if(index1 < parent1.getChildren().size()-1 && index2 < parent2.getChildren().size()-1) {
                        Tree t1 = parent1.getChild(index1+1);
                        Tree t2 = parent2.getChild(index2+1);
                        if(t1.getType().name.equals(LANG1.MULT_EXPRESSION) && t2.getType().name.equals(LANG2.MULT_EXPRESSION) && t1.isIsomorphicTo(t2)) {
                            mappingStore.addMappingRecursively(t1, t2);
                        }
                    }
                }
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT) && dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.EXPORT_STATEMENT)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(),LANG1.EXPORT_KEYWORD,LANG2.EXPORT_KEYWORD);
                    if(matched != null) {
                        mappingStore.addMapping(matched.first, matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.DEFAULT_KEYWORD, LANG2.DEFAULT_KEYWORD);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    ClassDeclarationMatcher.processParentInternalModule(srcStatementNode.getParent(), dstStatementNode.getParent(), mappingStore, LANG1, LANG2);
                }
                ClassDeclarationMatcher.processParentInternalModule(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
            }
        else if(Constants.isCrossLanguage(LANG1, LANG2)) {
            //an assignment moved to the initializer of the assigned field, i.e., the statement client = builder.client; in the constructor
            boolean assignmentToInitializer = leafMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_INITIALIZER) &&
                    JavaToKotlinMigration.handleAssignmentToInitializerMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, optimizationData.getDeferredFlattenings());
            //the constant of the if condition replaced with the condition of the when entry
            boolean equalityOperandToWhenCondition = !assignmentToInitializer && leafMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.WHEN_ENTRY_CONDITION) &&
                    JavaToKotlinMigration.handleEqualityOperandToWhenCondition(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, optimizationData.getDeferredFlattenings());
            //the name negated in the inverted condition, i.e., if (started) throw ... -> check(!started) {...}
            boolean nameReplacedWithNegatedName = !assignmentToInitializer && !equalityOperandToWhenCondition &&
                    JavaToKotlinMigration.handleNameReplacedWithNegatedName(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2);
            //the throw statement replaced with its message, i.e., if (x) throw new IllegalStateException("message"); -> check(!x) { "message" }
            boolean throwReplacedWithMessage = !assignmentToInitializer && !equalityOperandToWhenCondition && !nameReplacedWithNegatedName &&
                    JavaToKotlinMigration.handleThrowReplacedWithMessage(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, optimizationData.getDeferredFlattenings());
            if(!assignmentToInitializer && !equalityOperandToWhenCondition && !nameReplacedWithNegatedName && !throwReplacedWithMessage) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                JavaToKotlinMigration.handleLeafMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, abstractCodeMapping.getRefactorings(), optimizationData.getDeferredFlattenings());
            }
        }
        matchParentStatements(srcStatementNode, dstStatementNode, mappingStore);
        matchEqualOperatorsOfInitializers(srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
        boolean _abstractExp = abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression;
        boolean _leafExp = abstractCodeMapping.getFragment1() instanceof LeafExpression || abstractCodeMapping.getFragment2() instanceof LeafExpression;
        boolean _abstractExpWithNonCompositeOwner = _abstractExp;
        if (_abstractExp){
            if (abstractCodeMapping.getFragment1() instanceof AbstractExpression)
                if (((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner() != null
                        && ((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.FOR_STATEMENT))
                    _abstractExpWithNonCompositeOwner = false;
            if (abstractCodeMapping.getFragment2() instanceof AbstractExpression)
                if (((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner() != null
                        && ((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.FOR_STATEMENT))
                    _abstractExpWithNonCompositeOwner = false;
        }
        if (_abstractExpWithNonCompositeOwner || _leafExp) {
            //the initializers with anonymous classes without operations matched through their attributes are not matched again as leaves in the last step,
            //which overrides the mappings of their attributes, i.e., the object literal of StyleSheet.create({...})
            List<UMLAnonymousClassDiff> matchedAnonymousClassDiffs = matchedAnonymousClassDiffs(abstractCodeMapping, true);
            if (matchedAnonymousClassDiffs.isEmpty())
                optimizationData.getLastStepMappings().add(abstractCodeMapping);
            else
                matchLeavesAroundAnonymousClasses(srcTree, dstTree, srcStatementNode, dstStatementNode, matchedAnonymousClassDiffs, mappingStore);
        } else {
            List<UMLAnonymousClassDiff> matchedAnonymousClassDiffs = matchedAnonymousClassDiffs(abstractCodeMapping, false);
            if (!matchedAnonymousClassDiffs.isEmpty())
                matchLeavesAroundAnonymousClasses(srcTree, dstTree, srcStatementNode, dstStatementNode, matchedAnonymousClassDiffs, mappingStore);
            else
                new IgnoringCommentsLeafMatcher(LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            additionallyMatchedStatements(srcTree, dstTree, srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
        }
        optimizeVariableDeclarations(abstractCodeMapping);
        if (!isPartOfExtractedMethod && srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.RETURN_STATEMENT)) {
            optimizationData.getSubtreeMappings().addMapping(srcStatementNode,dstStatementNode);
        }
        if (!abstractCodeMapping.getRefactorings().isEmpty()) {
            leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
        }
    }

    //the = before the mapped initializers, i.e., idx_t file_index = DConstants::INVALID_INDEX; -> idx_t scan_chunk_file_index = DConstants::INVALID_INDEX;
    private void matchEqualOperatorsOfInitializers(Tree srcStatementNode, Tree dstStatementNode, AbstractCodeMapping mapping, ExtendedMultiMappingStore mappingStore) {
        if(!mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_INITIALIZER) ||
                !mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_INITIALIZER))
            return;
        int srcPosition = srcStatementNode.getParent() != null ? srcStatementNode.positionInParent() : 0;
        int dstPosition = dstStatementNode.getParent() != null ? dstStatementNode.positionInParent() : 0;
        if(srcPosition > 0 && dstPosition > 0) {
            Tree srcPrevious = srcStatementNode.getParent().getChild(srcPosition-1);
            Tree dstPrevious = dstStatementNode.getParent().getChild(dstPosition-1);
            if(srcPrevious.getType().name.equals(LANG1.EQUAL_OPERATOR) && dstPrevious.getType().name.equals(LANG2.EQUAL_OPERATOR)) {
                mappingStore.addMapping(srcPrevious, dstPrevious);
            }
        }
    }

    private void matchParentStatements(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
        if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
            mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
        }
    }

    //Kotlin call_expression -> [callee, call_suffix -> [value_arguments?, annotated_lambda -> lambda_literal -> [lambda_parameters?, arrow?, statements]]],
    //match everything except the body of the lambda, which is matched by the lambda mappers, i.e., list.forEachIndexed { index, item -> ... }
    private void processCallWithTrailingLambda(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
        Tree suffix1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.CALL_SUFFIX);
        Tree suffix2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.CALL_SUFFIX);
        if(suffix1 == null || suffix2 == null)
            return;
        //the callee, i.e., list.forEachIndexed
        Tree callee1 = srcStatementNode.getChild(0);
        Tree callee2 = dstStatementNode.getChild(0);
        if(callee1 != suffix1 && callee2 != suffix2) {
            new LeafMatcher(LANG1, LANG2).match(callee1, callee2, mappingStore);
        }
        mappingStore.addMapping(suffix1, suffix2);
        Pair<Tree,Tree> arguments = Helpers.findPairOfType(suffix1, suffix2, LANG1.METHOD_INVOCATION_ARGUMENTS, LANG2.METHOD_INVOCATION_ARGUMENTS);
        if(arguments != null) {
            new LeafMatcher(LANG1, LANG2).match(arguments.first, arguments.second, mappingStore);
        }
        Pair<Tree,Tree> annotatedLambdas = Helpers.findPairOfType(suffix1, suffix2, LANG1.ANNOTATED_LAMBDA, LANG2.ANNOTATED_LAMBDA);
        if(annotatedLambdas == null)
            return;
        mappingStore.addMapping(annotatedLambdas.first, annotatedLambdas.second);
        Pair<Tree,Tree> lambdaLiterals = Helpers.findPairOfType(annotatedLambdas.first, annotatedLambdas.second, LANG1.LAMBDA_LITERAL, LANG2.LAMBDA_LITERAL);
        if(lambdaLiterals == null)
            return;
        mappingStore.addMapping(lambdaLiterals.first, lambdaLiterals.second);
        Pair<Tree,Tree> parameters = Helpers.findPairOfType(lambdaLiterals.first, lambdaLiterals.second, LANG1.LAMBDA_PARAMETERS, LANG2.LAMBDA_PARAMETERS);
        if(parameters != null) {
            new LeafMatcher(LANG1, LANG2).match(parameters.first, parameters.second, mappingStore);
        }
        Pair<Tree,Tree> arrows = Helpers.findPairOfType(lambdaLiterals.first, lambdaLiterals.second, LANG1.ARROW, LANG2.ARROW);
        if(arrows != null) {
            mappingStore.addMapping(arrows.first, arrows.second);
        }
    }

    //JS/TS calls with arrow function arguments, i.e., useEffect(() => {...}, [dependencies])
    //the arrow function bodies are matched by the lambda mappers, the remaining arguments are matched if isomorphic
    private void processArgumentsWithArrowFunction(Tree call1, Tree call2, ExtendedMultiMappingStore mappingStore) {
        Pair<Tree,Tree> arguments = Helpers.findPairOfType(call1, call2, LANG1.METHOD_INVOCATION_ARGUMENTS, LANG2.METHOD_INVOCATION_ARGUMENTS);
        if(arguments == null || arguments.first.getChildren().size() != arguments.second.getChildren().size() ||
                arguments.first.getChildren().stream().noneMatch(child -> child.getType().name.equals(LANG1.ARROW_FUNCTION))) {
            return;
        }
        Tree callee1 = call1.getChild(0);
        Tree callee2 = call2.getChild(0);
        if(callee1.isIsomorphicTo(callee2)) {
            mappingStore.addMappingRecursively(callee1, callee2);
        }
        mappingStore.addMapping(arguments.first, arguments.second);
        for(int i=0; i<arguments.first.getChildren().size(); i++) {
            Tree argument1 = arguments.first.getChild(i);
            Tree argument2 = arguments.second.getChild(i);
            if(argument1.getType().name.equals(LANG1.ARROW_FUNCTION) && argument2.getType().name.equals(LANG2.ARROW_FUNCTION)) {
                mappingStore.addMapping(argument1, argument2);
                processArrowFunction(argument1, argument2, mappingStore, LANG1, LANG2);
            }
            else if(argument1.isIsomorphicTo(argument2)) {
                mappingStore.addMappingRecursively(argument1, argument2);
            }
        }
    }

    private boolean containsLambdaMapperWithNonMappedLeaves(AbstractCodeMapping abstractCodeMapping, Tree srcStatementNode, Tree dstStatementNode) {
        if(abstractCodeMapping.getLambdaMappers().size() > 0 && !srcStatementNode.isIsoStructuralTo(dstStatementNode)) {
            for(UMLOperationBodyMapper lambdaMapper : abstractCodeMapping.getLambdaMappers()) {
                if(lambdaMapper.getParentMapper() !=  null && lambdaMapper.getParentMapper().getParentMapper() != null)
                    continue;
                int nonMappedT2 = lambdaMapper.getNonMappedLeavesT2().size();
                for(AbstractCodeFragment fragment2 : lambdaMapper.getNonMappedLeavesT2()) {
                    if(lambdaMapper.getParentMapper() != null) {
                        for(UMLOperationBodyMapper childMapper : lambdaMapper.getParentMapper().getChildMappers()) {
                            if(fragment2.getLocationInfo().subsumes(childMapper.getOperationInvocation().getLocationInfo())) {
                                nonMappedT2--;
                                break;
                            }
                        }
                        if(fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
                            nonMappedT2--;
                        }
                    }
                }
                if(lambdaMapper.getNonMappedLeavesT1().size() > 0 && nonMappedT2 > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void processArrowFunction(Tree srcStatementNode, Tree dstStatementNode,
            ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Pair<Tree, Tree> formal_parameters = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FORMAL_PARAMETERS, LANG2.FORMAL_PARAMETERS);
        if(formal_parameters != null && formal_parameters.first.isIsoStructuralTo(formal_parameters.second)) {
            mappingStore.addMappingRecursively(formal_parameters.first, formal_parameters.second);
        }
        else if(formal_parameters != null) {
            mappingStore.addMapping(formal_parameters.first, formal_parameters.second);
            Pair<Tree, Tree> open_parenthesis = Helpers.findPairOfType(formal_parameters.first, formal_parameters.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
            if(open_parenthesis != null) {
                mappingStore.addMapping(open_parenthesis.first, open_parenthesis.second);
            }
            Pair<Tree, Tree> close_parenthesis = Helpers.findPairOfType(formal_parameters.first, formal_parameters.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
            if(close_parenthesis != null) {
                mappingStore.addMapping(close_parenthesis.first, close_parenthesis.second);
            }
        }
        Pair<Tree, Tree> arrows = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARROW_TOKEN, LANG2.ARROW_TOKEN);
        if(arrows != null) {
            mappingStore.addMapping(arrows.first, arrows.second);
        }
        Pair<Tree, Tree> functions = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FUNCTION, LANG2.FUNCTION);
        if(functions != null) {
            mappingStore.addMapping(functions.first, functions.second);
        }
        Pair<Tree, Tree> async = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ASYNC_KEYWORD, LANG2.ASYNC_KEYWORD);
        if(async != null) {
            mappingStore.addMapping(async.first, async.second);
        }
        Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.STATEMENT_BLOCK, LANG2.STATEMENT_BLOCK);
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

    private void additionallyMatchedStatements(Tree srcTree, Tree dstTree, Tree srcStatementNode, Tree dstStatementNode, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        if (abstractCodeMapping != null) {
            for (Replacement replacement : abstractCodeMapping.getReplacements()) {
                if (replacement instanceof CompositeReplacement) {
                    CompositeReplacement compositeReplacement = (CompositeReplacement) replacement;
                    if (!compositeReplacement.getAdditionallyMatchedStatements1().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements1()) {
                            Tree srcAdditionalTree = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeFragment.getLocationInfo(), LANG1);
                            new LeafMatcher(LANG1, LANG2).match(srcAdditionalTree, dstStatementNode, mappingStore);
                        }
                    } else if (!compositeReplacement.getAdditionallyMatchedStatements2().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements2()) {
                            Tree dstAdditionalTree = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeFragment.getLocationInfo(), LANG2);
                            new LeafMatcher(LANG1, LANG2).match(srcStatementNode, dstAdditionalTree, mappingStore);
                        }
                    }
                }
            }
        }
    }

    private void leafMappingRefactoringAwareness(Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        for (Refactoring refactoring : abstractCodeMapping.getRefactorings()) {
            if (refactoring instanceof ExtractVariableRefactoring)
            {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                for (AbstractCodeMapping reference : extractVariableRefactoring.getReferences()) {
                    //the references in other files than the new container
                    if (!reference.getFragment2().getLocationInfo().getFilePath().equals(bodyMapper.getContainer2().getLocationInfo().getFilePath()))
                        continue;
                    for (LeafExpression variable : reference.getFragment2().getVariables()) {
                        if (variable.getString().equals(extractVariableRefactoring.getVariableDeclaration().getVariableName())) {
                            Tree referenceNode = TreeUtilFunctions.findByLocationInfo(dstTree, variable.getLocationInfo(), LANG2);
                            if (referenceNode != null)
                            {
                                if (!referenceNode.getChildren().isEmpty()){
                                    referenceNode = referenceNode.getChild(0);
                                }
                                if (mappingStore.isDstMapped(referenceNode) && !mappingStore.isDstMultiMapped(referenceNode)) {
                                    Tree tempSrc = mappingStore.getSrcs(referenceNode).iterator().next();
                                    mappingStore.removeMapping(tempSrc, referenceNode);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void optimizeVariableDeclarations(AbstractCodeMapping abstractCodeMapping) {
        List<VariableDeclaration> variableDeclarations1 = abstractCodeMapping.getFragment1().getVariableDeclarations();
        List<VariableDeclaration> variableDeclarations2 = abstractCodeMapping.getFragment2().getVariableDeclarations();
        if (variableDeclarations1.size() == 1 && variableDeclarations2.isEmpty()){
            if (variableDeclarations1.get(0).getInitializer() != null)
                if (abstractCodeMapping.getFragment2().toString().contains(variableDeclarations1.get(0).getInitializer().toString()))
                    optimizationData.getLastStepMappings().add(new LeafMapping(variableDeclarations1.get(0).getInitializer(), abstractCodeMapping.getFragment2(),null,null));
        }
        if (variableDeclarations1.isEmpty() && variableDeclarations2.size() == 1){
            if (variableDeclarations2.get(0).getInitializer() != null)
                if (abstractCodeMapping.getFragment1().toString().contains(variableDeclarations2.get(0).getInitializer().toString()))
                    optimizationData.getLastStepMappings().add(new LeafMapping(abstractCodeMapping.getFragment1(),variableDeclarations2.get(0).getInitializer(),null,null));
        }
    }
}
