package org.refactoringminer.astDiff.matchers.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.refactoringminer.astDiff.models.DeferredFlattenings;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.utils.Pair;

public class JavaToKotlinMigration {

    public static void handleCompositeMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        handleCompositeMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, null);
    }

    //deferredFlattenings: the flattenings of the Java tree are deferred until all diffs are matched, so that the Java tree has the same structure
    //for all the Kotlin trees it is matched with (i.e., a statement of a method inlined to multiple call sites), or applied immediately if null
    public static void handleCompositeMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE)) {
            Tree singleVariableDeclaration1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.SINGLE_VARIABLE_DECLARATION);
            if(singleVariableDeclaration1 != null) {
                handleParameterMapping(mappingStore, singleVariableDeclaration1, dstStatementNode, LANG1, LANG2, deferredFlattenings);
                if(deferredFlattenings != null)
                    deferredFlattenings.flattenKeepingMappings(srcStatementNode, singleVariableDeclaration1);
                else
                    flattenChild(srcStatementNode, singleVariableDeclaration1);
            }
            Tree block1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.BLOCK);
            Tree block2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.STATEMENTS);
            if(block1 != null && block2 != null) {
                mappingStore.addMapping(block1, block2);
            }
        }
        else if(srcStatementNode.getType().name.equals(LANG1.TRY_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.TRY_STATEMENT)) {
            Tree block1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.BLOCK);
            Tree block2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.STATEMENTS);
            if(block1 != null && block2 != null) {
                mappingStore.addMapping(block1, block2);
            }
        }
        else if(srcStatementNode.getType().name.equals(LANG1.TRY_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            handleTryToLambdaCallMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, deferredFlattenings);
        }
        else if(srcStatementNode.getType().name.equals(LANG1.IF_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.IF_STATEMENT) &&
                srcStatementNode.getChildren().size() > 0 && dstStatementNode.getChildren().size() > 0) {
            Tree expression1 = srcStatementNode.getChild(0);
            Tree expression2 = dstStatementNode.getChild(0);
            handleLeafMapping(mappingStore, expression1, expression2, LANG1, LANG2, deferredFlattenings);
        }
        else if(srcStatementNode.getType().name.equals(LANG1.WHILE_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.WHILE_STATEMENT) &&
                srcStatementNode.getChildren().size() > 0 && dstStatementNode.getChildren().size() > 0) {
            Tree expression1 = srcStatementNode.getChild(0);
            Tree expression2 = dstStatementNode.getChild(0);
            handleLeafMapping(mappingStore, expression1, expression2, LANG1, LANG2, deferredFlattenings);
        }
        else if(srcStatementNode.getType().name.equals(LANG1.SYNCHRONIZED_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) && dstStatementNode.getChildren().size() > 1) {
            //the Kotlin nodes flattened into the call, to compute its children when the flattenings are deferred
            Set<Tree> flattenedSynchronized2 = Collections.newSetFromMap(new IdentityHashMap<>());
            Tree suffix2 = dstStatementNode.getChild(1);
            if(suffix2.getType().name.equals(LANG2.CALL_SUFFIX) && suffix2.getChildren().size() > 0 && suffix2.getChild(0).getType().name.equals(LANG2.ANNOTATED_LAMBDA) &&
                    suffix2.getChild(0).getChildren().size() > 0 && suffix2.getChild(0).getChild(0).getType().name.equals(LANG2.LAMBDA_LITERAL)) {
                Tree annotatedLambda2 = suffix2.getChild(0);
                Tree lambdaLiteral2 = annotatedLambda2.getChild(0);
                Tree statements2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.STATEMENTS);
                Tree block1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.BLOCK);
                if(block1 != null && statements2 != null) {
                    //align call_expression -> call_suffix -> annotated_lambda -> lambda_literal with Java SynchronizedStatement -> Block
                    flattenDstChild(mappingStore, deferredFlattenings, dstStatementNode, suffix2);
                    flattenDstChild(mappingStore, deferredFlattenings, dstStatementNode, annotatedLambda2);
                    flattenedSynchronized2.add(suffix2);
                    flattenedSynchronized2.add(annotatedLambda2);
                    //align lambda_literal -> statements -> stmt* with Java Block -> stmt*, as in function_body and control_structure_body including the braces of the block
                    flattenDstChild(mappingStore, deferredFlattenings, lambdaLiteral2, statements2);
                    removeDstMappingsOfCounterpart(mappingStore, deferredFlattenings, lambdaLiteral2, srcStatementNode);
                    mappingStore.addMapping(block1, lambdaLiteral2);
                }
                //align call_expression -> call_expression -> call_suffix -> value_arguments -> value_argument -> expression with Java SynchronizedStatement -> expression
                Tree call2 = dstStatementNode.getChild(0);
                if(call2.getType().name.equals(LANG2.METHOD_INVOCATION) && call2.getChildren().size() > 1) {
                    Tree callSuffix2 = call2.getChild(1);
                    Tree valueArguments2 = TreeUtilFunctions.findChildByType(callSuffix2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                    if(callSuffix2.getType().name.equals(LANG2.CALL_SUFFIX) && callSuffix2.getChildren().size() == 1 &&
                            valueArguments2 != null && valueArguments2.getChildren().size() == 1 && valueArguments2.getChild(0).getChildren().size() == 1) {
                        Tree valueArgument2 = valueArguments2.getChild(0);
                        Tree expression2 = valueArgument2.getChild(0);
                        Tree keyword2 = call2.getChild(0);
                        flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, call2);
                        flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, callSuffix2);
                        flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, valueArguments2);
                        flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, valueArgument2);
                        flattenedSynchronized2.addAll(List.of(call2, callSuffix2, valueArguments2, valueArgument2));
                        //align Kotlin call_expression -> [synchronized, expression, statements] with Java SynchronizedStatement -> [expression, Block], as Java SynchronizedStatement includes the synchronized keyword
                        if(keyword2.getType().name.equals(LANG2.SIMPLE_NAME) && keyword2.getLabel().equals("synchronized") &&
                                sizeAfterFlattening(dstStatementNode, flattenedSynchronized2) == srcStatementNode.getChildren().size() + 1) {
                            removeDstChild(mappingStore, deferredFlattenings, dstStatementNode, keyword2);
                        }
                        if(srcStatementNode.getChildren().size() > 0) {
                            Tree expression1 = srcStatementNode.getChild(0);
                            mappingStore.addMapping(expression1, expression2);
                            handleLeafMapping(mappingStore, expression1, expression2, LANG1, LANG2, deferredFlattenings);
                        }
                    }
                }
            }
        }
    }

    public static void handleLeafMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        handleLeafMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, null);
    }

    //deferredFlattenings: the flattenings of the Java tree are deferred until all diffs are matched, so that the Java tree has the same structure
    //for all the Kotlin trees it is matched with (i.e., a statement of a method inlined to multiple call sites), or applied immediately if null
    public static void handleLeafMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        /*
        Map<Tree, Tree> cpyToSrc = new HashMap<>();
        Tree srcFakeTree = TreeUtilFunctions.deepCopyWithMap(srcStatementNode, cpyToSrc);
        Map<Tree, Tree> cpyToDst = new HashMap<>();
        Tree dstFakeTree = TreeUtilFunctions.deepCopyWithMapAndTypeTranslation(dstStatementNode, cpyToDst, LANG1, LANG2);
        ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null,LANG1,LANG2);
        //both trees are now in LANG1
        new LeafMatcher(LANG1, LANG1).match(srcFakeTree, dstFakeTree, tempMapping);
        */
        if(dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION) && dstStatementNode.getChildren().size() == 1 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_KEYWORD)) {
            if(dstStatementNode.getChild(0).getLabel().equals("break") || dstStatementNode.getChild(0).getLabel().equals("continue")) {
                dstStatementNode.getChildren().clear();
            }
        }
        if(srcStatementNode.getType().name.equals(LANG1.SIMPLE_NAME) && dstStatementNode.getType().name.equals(LANG2.SIMPLE_NAME)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode);
            return;
        }
        if(srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.SIMPLE_NAME) &&
                dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.FUNCTION_BODY) &&
                TreeUtilFunctions.findChildByType(dstStatementNode.getParent(), LANG2.AFFECTATION_OPERATOR) != null) {
            if(srcStatementNode.getChildren().size() == 1 && srcStatementNode.getChild(0).getType().name.equals(LANG1.SIMPLE_NAME)) {
                //align Java ReturnStatement -> SimpleName with Kotlin function_body -> [=, simple_identifier], as Kotlin expression body has no return statement wrapper
                //the ReturnStatement takes the place of the SimpleName, as the ReturnStatement takes the place of a returned non-leaf expression (see below)
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                absorbSrcLeafChild(mappingStore, deferredFlattenings, srcStatementNode, srcStatementNode.getChild(0));
                return;
            }
            if(srcStatementNode.isLeaf()) {
                //the SimpleName is already absorbed by a previous mapping of the same statement
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                return;
            }
        }
        if(srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) || srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION)) {
            flattenTrailingLambdaCall(mappingStore, dstStatementNode, LANG2);
        }
        List<Tree> children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_NAME);
        Tree firstChild1 = children1.size() > 0 ? children1.get(0) : null;
        boolean firstChildIsType1 = firstChild1 != null && firstChild1.getParent().getType().name.equals(LANG1.SIMPLE_TYPE) &&
                !firstChild1.getParent().getParent().getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) &&
                !firstChild1.getParent().getParent().getType().name.equals(LANG1.CAST_EXPRESSION);
        List<Tree> mathSimpleNames1 = children1.stream().filter(t -> t.getLabel().equals("Math")).collect(Collectors.toList());
        List<Tree> children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.SIMPLE_NAME);
        List<Tree> mathSimpleNames2 = children2.stream().filter(t -> t.getLabel().equals("Math")).collect(Collectors.toList());
        List<Tree> thisExpressionsWithSimpleName1 = children1.stream().filter(t -> t.getParent().getType().name.equals(LANG1.THIS_EXPRESSION)).collect(Collectors.toList());
        children1.removeAll(thisExpressionsWithSimpleName1);
        if(mathSimpleNames1.size() > 0 && mathSimpleNames2.isEmpty()) {
            children1.removeAll(mathSimpleNames1);
        }
        List<Tree> interpolatedIdentifiers2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.INTERPOLATED_IDENTIFIER);
        List<Tree> interpolatedExpressions2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.INTERPOLATED_EXPRESSION);
        if(children2.size() > 0 && children2.get(children2.size()-1).getLabel().equals("code")) {
            //remove .code on character literals to convert to int
            children2.remove(children2.size()-1);
        }
        Iterator<Tree> iter2 = children2.iterator();
        boolean letFound = false;
        while(iter2.hasNext()) {
            Tree t2 = iter2.next();
            String name = t2.getLabel();
            //remove let
            if(name.equals("let")) {
                iter2.remove();
                letFound = true;
            }
        }
        Tree assignment1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.ASSIGNMENT);
        if(assignment1 != null && !dstStatementNode.getType().name.equals(LANG2.ASSIGNMENT)) {
            children1.remove(0);
        }
        //the Java receiver is the implicit receiver of the Kotlin lambda, i.e., peerSettings.set(...); -> val peerSettings = Settings().apply { set(...) }
        Tree implicitReceiver1 = findImplicitReceiver(srcStatementNode, dstStatementNode, LANG1, LANG2);
        if(implicitReceiver1 != null) {
            children1.remove(implicitReceiver1);
        }
        //the Java variable is the implicit receiver of the Kotlin lambda in a field assignment, i.e., call.transmitter = new Transmitter(client, call); -> RealCall(...).apply { transmitter = Transmitter(client, this) }
        String implicitReceiverName1 = alignImplicitReceiverFieldAssignment(mappingStore, srcStatementNode, dstStatementNode, children2, LANG1, LANG2);
        if(implicitReceiverName1 != null) {
            List<Tree> receiverNames1 = children1.stream().filter(t -> t.getLabel().equals(implicitReceiverName1)).collect(Collectors.toList());
            List<Tree> thisExpressions2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.THIS_EXPRESSION).stream()
                    .filter(t -> t.getChildren().isEmpty() ? t.getLabel().equals("this") : t.getChildren().size() == 1 && t.getChild(0).getLabel().equals("this"))
                    .collect(Collectors.toList());
            if(receiverNames1.size() == thisExpressions2.size()) {
                //the Java variable becomes this inside the Kotlin lambda
                for(int i=0; i<receiverNames1.size(); i++) {
                    Tree this2 = thisExpressions2.get(i);
                    if(this2.getChildren().size() == 1) {
                        this2.setLabel(this2.getChild(0).getLabel());
                        this2.getChildren().clear();
                    }
                    mappingStore.addMapping(receiverNames1.get(i), this2);
                }
                children1.removeAll(receiverNames1);
            }
        }
        List<Tree> anonymous1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.ANONYMOUS_CLASS_DECLARATION);
        List<Tree> lambdas1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.LAMBDA_EXPRESSION);
        //remove the simpleName children of anonymous/lambdas from the parent children, before matching them with interpolated identifiers
        removeFromParent(children1, anonymous1, LANG1.SIMPLE_NAME);
        removeFromParent(children1, lambdas1, LANG1.SIMPLE_NAME);
        //the Java builder chains replaced with Kotlin constructor calls with named arguments, i.e., new X.Builder().a(1).build() -> X(a = 1)
        List<Tree> builderNodes1 = new ArrayList<>();
        List<Tree> builderNodes2 = new ArrayList<>();
        alignBuilderWithNamedArguments(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, builderNodes1, builderNodes2);
        children1.removeAll(builderNodes1);
        children2.removeAll(builderNodes2);
        //remove from children1 simple names corresponding to interpolated identifiers
        if(interpolatedIdentifiers2.size() > 0 || interpolatedExpressions2.size() > 0) {
            Iterator<Tree> iter1 = children1.iterator();
            while(iter1.hasNext()) {
                Tree t1 = iter1.next();
                String name = t1.getLabel();
                for(Tree t2 : interpolatedIdentifiers2) {
                    if(name.equals(t2.getLabel())) {
                        mappingStore.addMapping(t1, t2);
                        iter1.remove();
                        break;
                    }
                }
                for(Tree t2 : interpolatedExpressions2) {
                    List<Tree> simpleNames2 = TreeUtilFunctions.findChildrenByTypeRecursively(t2, LANG2.SIMPLE_NAME);
                    boolean found = false;
                    for(Tree simpleName2 : simpleNames2) {
                        if(name.equals(simpleName2.getLabel())) {
                            mappingStore.addMapping(t1, simpleName2);
                            iter1.remove();
                            found = true;
                            break;
                        }
                        else if(name.toLowerCase().endsWith(simpleName2.getLabel())) {
                            mappingStore.addMapping(t1, simpleName2);
                            iter1.remove();
                            found = true;
                            break;
                        }
                    }
                    if(found)
                        break;
                }
            }
        }
        removeFromParent(children2, interpolatedExpressions2, LANG2.SIMPLE_NAME);
        List<Tree> types1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_TYPE);
        List<Tree> castExpressions1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.CAST_EXPRESSION);
        List<Tree> qualifiedNames1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.QUALIFIED_NAME);
        //the statement itself is a qualified name, i.e., a field initializer
        if(srcStatementNode.getType().name.equals(LANG1.QUALIFIED_NAME)) {
            qualifiedNames1.add(0, srcStatementNode);
        }
        List<Tree> anonymous2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.OBJECT_LITERAL);
        List<Tree> lambdas2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.ANNOTATED_LAMBDA);
        //remove the simpleName children of anonymous/lambdas from the parent children
        removeFromParent(qualifiedNames1, anonymous1, LANG1.QUALIFIED_NAME);
        removeFromParent(children2, anonymous2, LANG2.SIMPLE_NAME);
        boolean letWithLambda = letFound && lambdas2.size() > lambdas1.size();
        if(!letWithLambda) {
            removeFromParent(children2, lambdas2, LANG2.SIMPLE_NAME);
        }
        //the last argument of the Java call is replaced with the trailing lambda of the Kotlin call, i.e., pushExecutor.execute(namedRunnable) -> pushExecutor.execute(...) {...}
        if(children1.size() == children2.size() + 1 && hasTrailingLambda(dstStatementNode, LANG2)) {
            Tree call1 = srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && srcStatementNode.getChildren().size() == 1 ? srcStatementNode.getChild(0) : srcStatementNode;
            Tree arguments1 = call1.getType().name.equals(LANG1.METHOD_INVOCATION) ? TreeUtilFunctions.findChildByType(call1, LANG1.METHOD_INVOCATION_ARGUMENTS) : null;
            if(arguments1 != null && arguments1.getChildren().size() > 0) {
                Tree lastArgument1 = arguments1.getChild(arguments1.getChildren().size() - 1);
                if(lastArgument1.getType().name.equals(LANG1.SIMPLE_NAME) && children1.get(children1.size() - 1) == lastArgument1) {
                    children1.remove(children1.size() - 1);
                }
            }
        }
        Map<Tree, Tree> qualifiedNameToNavigationExpression = new LinkedHashMap<>();
        if(types1.size() > 0 && children1.size() != children2.size()) {
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree type1 : types1) {
                if(type1.getChildren().size() > 0 && type1.getChild(0).getType().name.equals(LANG1.QUALIFIED_NAME)) {
                    String qualifiedType = type1.getChild(0).getLabel();
                    for(Tree child2 : children2) {
                        if(isQualifiedNameSegment(qualifiedType, child2.getLabel())) {
                            toBeRemoved2.add(child2);
                            if(child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                    //important: skip qualified types whose parent is a variable declaration statement, because these are replaced with var in Kotlin
                                    !type1.getParent().getType().name.equals(LANG1.VARIABLE_DECLARATION_STATEMENT) &&
                                    !qualifiedNameToNavigationExpression.containsKey(type1.getChild(0)) &&
                                    !qualifiedNameToNavigationExpression.containsValue(child2.getParent())) {
                                Tree lastChild = child2.getParent().getChild(child2.getParent().getChildren().size() - 1);
                                if(lastChild.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && lastChild.getChildren().size() > 0 &&
                                        isQualifiedNameSegment(qualifiedType, lastChild.getChild(0).getLabel())) {
                                    qualifiedNameToNavigationExpression.put(type1.getChild(0), child2.getParent());
                                }
                            }
                        }
                    }
                }
            }
            children2.removeAll(toBeRemoved2);
        }
        if(qualifiedNames1.size() > 0 && children1.size() != children2.size()) {
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree qualified1 : qualifiedNames1) {
                String qualifiedType = qualified1.getLabel();
                for(Tree child2 : children2) {
                    if(isQualifiedNameSegment(qualifiedType, child2.getLabel())) {
                        boolean skip = child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                child2.getParent().getParent().getType().name.equals(LANG2.METHOD_INVOCATION);
                        //keep the simple names outside navigation expressions matching a Java simple name, i.e., x in x == 0 || s.x == 0
                        if(!isInsideNavigationExpression(child2, dstStatementNode, LANG2) &&
                                children1.stream().anyMatch(child1 -> child1.getLabel().equals(child2.getLabel()))) {
                            continue;
                        }
                        if(!skip) {
                            toBeRemoved2.add(child2);
                        }
                        if(child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                !qualifiedNameToNavigationExpression.containsKey(qualified1) &&
                                !qualifiedNameToNavigationExpression.containsValue(child2.getParent())) {
                            Tree lastChild = child2.getParent().getChild(child2.getParent().getChildren().size() - 1);
                            if(lastChild.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && lastChild.getChildren().size() > 0 &&
                                    isQualifiedNameSegment(qualifiedType, lastChild.getChild(0).getLabel())) {
                                qualifiedNameToNavigationExpression.put(qualified1, child2.getParent());
                            }
                        }
                    }
                }
            }
            children2.removeAll(toBeRemoved2);
        }
        //the navigation expressions already converted to qualified names by a previous mapping of the same statement
        List<Tree> navigationExpressions2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.NAVIGATION_EXPRESSION);
        if(dstStatementNode.getType().name.equals(LANG2.NAVIGATION_EXPRESSION)) {
            navigationExpressions2.add(0, dstStatementNode);
        }
        for(Tree qualified1 : qualifiedNames1) {
            if(qualifiedNameToNavigationExpression.containsKey(qualified1))
                continue;
            for(Tree navigation2 : navigationExpressions2) {
                if(navigation2.isLeaf() && navigation2.getLabel().equals(qualified1.getLabel()) && !qualifiedNameToNavigationExpression.containsValue(navigation2)) {
                    qualifiedNameToNavigationExpression.put(qualified1, navigation2);
                    break;
                }
            }
        }
        for(Tree key : qualifiedNameToNavigationExpression.keySet()) {
            Tree value = qualifiedNameToNavigationExpression.get(key);
            value.setLabel(key.getLabel());
            value.getChildren().clear();
            mappingStore.addMapping(key, value);
        }
        if(castExpressions1.size() > 0 && children1.size() != children2.size()) {
            //remove from children1
            List<Tree> toBeRemoved1 = new ArrayList<>();
            if(castExpressions1.get(0).getChildren().size() > 0) {
                Tree simpleType = castExpressions1.get(0).getChild(0);
                for(Tree child1 : children1) {
                    if(simpleType.getChildren().contains(child1)) {
                        toBeRemoved1.add(child1);
                    }
                }
            }
            children1.removeAll(toBeRemoved1);
        }
        List<Tree> inv1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION, LANG1.CLASS_INSTANCE_CREATION);
        if(srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) || srcStatementNode.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
            inv1.add(0, srcStatementNode);
        }
        List<Tree> inv2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION);
        if(dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) && !isFlattenedReturnExpression(srcStatementNode, dstStatementNode, LANG1, LANG2) &&
                !isAssertCall(srcStatementNode, dstStatementNode, LANG1, LANG2)) {
            inv2.add(0, dstStatementNode);
        }
        inv1.removeAll(builderNodes1);
        inv2.removeAll(builderNodes2);
        //the Java variable initializer is the receiver of the Kotlin apply call, i.e., RealCall call = new RealCall(x); -> return RealCall(x).apply {...}
        Tree applyCall2 = findApplyCallWithInitializerReceiver(srcStatementNode, dstStatementNode, LANG1, LANG2);
        if(applyCall2 != null) {
            inv2.remove(applyCall2);
        }
        removeFromParent(inv1, anonymous1, LANG1.METHOD_INVOCATION);
        removeFromParent(inv1, anonymous1, LANG1.CLASS_INSTANCE_CREATION);
        removeFromParent(inv2, anonymous2, LANG2.METHOD_INVOCATION);
        removeFromParent(inv1, lambdas1, LANG1.METHOD_INVOCATION);
        removeFromParent(inv1, lambdas1, LANG1.CLASS_INSTANCE_CREATION);
        if(!letWithLambda) {
            removeFromParent(inv2, lambdas2, LANG2.METHOD_INVOCATION);
        }
        //check if class instance creation has an anonymous class and remove it
        if(anonymous1.size() > anonymous2.size()) {
            List<Tree> invocationToBeRemoved = new ArrayList<Tree>();
            for(Tree inv : inv1) {
                if(inv.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
                    Tree anonymous = TreeUtilFunctions.findChildByType(inv, LANG1.ANONYMOUS_CLASS_DECLARATION);
                    if(anonymous != null && anonymous1.contains(anonymous)) {
                        Tree simpleType = inv.getChild(0);
                        if(simpleType.getChildren().size() > 0 && children1.contains(simpleType.getChild(0))) {
                            children1.remove(simpleType.getChild(0));
                        }
                        invocationToBeRemoved.add(inv);
                    }
                }
            }
            inv1.removeAll(invocationToBeRemoved);
        }
        //check if toArray() is replaced with toTypedArray() and remove nested simpleNames and invocations from the toArray() argument
        boolean containsToArray1 = children1.stream().anyMatch(t -> t.getLabel().equals("toArray"));
        boolean containsToArray2 = children2.stream().anyMatch(t -> t.getLabel().equals("toArray"));
        boolean containsToTypedArray2 = children2.stream().anyMatch(t -> t.getLabel().equals("toTypedArray"));
        if(containsToArray1 && !containsToArray2 && containsToTypedArray2) {
            Tree arguments = null;
            for(Tree inv : inv1) {
                Tree name = TreeUtilFunctions.findChildByType(inv, LANG1.SIMPLE_NAME);
                if(name != null && name.getLabel().equals("toArray")) {
                    arguments = TreeUtilFunctions.findChildByType(inv, LANG1.METHOD_INVOCATION_ARGUMENTS);
                    break;
                }
            }
            if(arguments != null) {
                List<Tree> list = List.of(arguments);
                removeFromParent(inv1, list, LANG1.METHOD_INVOCATION);
                removeFromParent(inv1, list, LANG1.CLASS_INSTANCE_CREATION);
                removeFromParent(children1, list, LANG1.SIMPLE_NAME);
            }
        }
        if(applyCall2 != null) {
            //the Java variable type and name are replaced with the Kotlin apply call
            Tree fragment1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.VARIABLE_DECLARATION_FRAGMENT);
            if(fragment1 != null && fragment1.getChildren().size() > 0) {
                children1.remove(fragment1.getChild(0));
            }
            if(srcStatementNode.getChildren().size() > 0 && srcStatementNode.getChild(0) != fragment1) {
                children1.removeAll(TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode.getChild(0), LANG1.SIMPLE_NAME));
            }
            Tree applyName2 = findApplyName(applyCall2, LANG2);
            if(applyName2 != null) {
                children2.remove(applyName2);
            }
            firstChildIsType1 = false;
        }
        if(children1.size() > children2.size()) {
            //remove from children1 the simple names of the Java receivers dropped in Kotlin, i.e., RealCall.newRealCall(client) -> newRealCall(client)
            for(Tree receiver1 : droppedReceivers(inv1, inv2, LANG1, LANG2)) {
                children1.removeAll(TreeUtilFunctions.findChildrenByTypeRecursively(receiver1, LANG1.SIMPLE_NAME));
            }
        }
        boolean equalsMismatch = children1.stream().anyMatch(node -> node.getLabel().equals("equals")) &&
                !children2.stream().anyMatch(node -> node.getLabel().equals("equals"));
        if(children1.size() != children2.size() || equalsMismatch) {
            List<Tree> toBeRemoved1 = new ArrayList<>();
            for(Tree child1 : children1) {
                if(child1.getLabel().equals("get") || child1.getLabel().equals("put") || child1.getLabel().equals("equals")) {
                    toBeRemoved1.add(child1);
                }
            }
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree child2 : children2) {
                if(child2.getLabel().equals("get") || child2.getLabel().equals("put") || child2.getLabel().equals("equals")) {
                    toBeRemoved2.add(child2);
                }
            }
            if(toBeRemoved1.size() > 0 && toBeRemoved2.size() == 0) {
                children1.removeAll(toBeRemoved1);
            }
        }
        if(children1.size() == children2.size() && (!firstChildIsType1 || children1.size() == 1)) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0)
                    mappingStore.addMapping(children1.get(i), children2.get(i).getChild(0));
                else
                    mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        if(children1.size() == children2.size() && firstChildIsType1) {
            //this happens when Java side has a type, but Kotlin side has var/val
            Tree t2 = children2.get(0);
            int start1 = -1;
            for(int i=0; i<children1.size(); i++) {
                if(children1.get(i).getLabel().equals(t2.getLabel())) {
                    start1 = i;
                    break;
                }
            }
            if(start1 >= 1) {
                for(int i=start1; i<children1.size() && i-1<children2.size(); i++) {
                    if(children1.get(i).getLabel().equals(children2.get(i-1).getLabel())) {
                        mappingStore.addMapping(children1.get(i), children2.get(i-1));
                    }
                }
                //handle last child
                Tree lastChild1 = children1.get(children1.size()-1);
                Tree lastChild2 = children2.get(children2.size()-1);
                if(lastChild1.getLabel().equals(lastChild2.getLabel())) {
                    mappingStore.addMapping(lastChild1, lastChild2);
                }
            }
        }
        else if(children1.size() > children2.size() && children2.size() > 0 && firstChildIsType1) {
            //this happens when Java side has a type, but Kotlin side has var/val
            Tree t2 = children2.get(0);
            int start1 = -1;
            for(int i=0; i<children1.size(); i++) {
                if(children1.get(i).getLabel().equals(t2.getLabel())) {
                    start1 = i;
                    break;
                }
            }
            //confirm simple name correspondence
            if(start1 == -1) {
                int matchingNames = 0;
                for(int i=1; i<children1.size() && i-1<children2.size(); i++) {
                    if(children1.get(i).getLabel().equals(children2.get(i-1).getLabel())) {
                        matchingNames++;
                    }
                }
                if(matchingNames > 0) {
                    start1 = 1;
                }
            }
            if(children2.size() == children1.size() - start1) {
                for(int i=0; i<children2.size(); i++) {
                    mappingStore.addMapping(children1.get(i+start1), children2.get(i));
                }
            }
        }
        else if(children2.size() > children1.size()) {
            //match only the children with identical labels on a first match basis
            for(int i=0; i<children2.size(); i++) {
                for(int j=0; j<children1.size(); j++) {
                    if(children2.get(i).getLabel().equals(children1.get(j).getLabel())) {
                        mappingStore.addMapping(children1.get(j), children2.get(i));
                        break;
                    }
                }
            }
        }
        List<Tree> invocationsToBeRemoved = new ArrayList<>();
        if(nameCompliance(inv1, inv2, LANG1, LANG2)) {
            if(inv1.size() <= inv2.size()) {
                for(int i=0; i<inv1.size(); i++) {
                    Tree child1 = inv1.get(i);
                    Tree child2 = inv2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2, invocationsToBeRemoved, deferredFlattenings);
                }
            }
            else if(inv2.size() < inv1.size()) {
                for(int i=0; i<inv2.size(); i++) {
                    Tree child1 = inv1.get(i);
                    Tree child2 = inv2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2, invocationsToBeRemoved, deferredFlattenings);
                }
            }
        }
        inv1.removeAll(invocationsToBeRemoved);
        List<Tree> casts1 = new ArrayList<>(castExpressions1);
        if(srcStatementNode.getType().name.equals(LANG1.CAST_EXPRESSION)) {
            casts1.add(0, srcStatementNode);
        }
        List<Tree> asExpressions2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.AS_EXPRESSION);
        if(dstStatementNode.getType().name.equals(LANG2.AS_EXPRESSION)) {
            asExpressions2.add(0, dstStatementNode);
        }
        if(casts1.size() > 0 && casts1.size() == asExpressions2.size()) {
            //align Kotlin [parenthesized_expression ->] as_expression -> [expression, as, type] with Java [ParenthesizedExpression ->] CastExpression -> [type, expression]
            for(int i=0; i<casts1.size(); i++) {
                Tree cast1 = casts1.get(i);
                Tree as2 = asExpressions2.get(i);
                mappingStore.addMapping(cast1, as2);
                if(cast1.getChildren().size() > 0) {
                    handleTypeMapping(mappingStore, cast1.getChild(0), as2, LANG1, LANG2, deferredFlattenings);
                }
                Tree parent1 = cast1.getParent();
                Tree parent2 = as2.getParent();
                if(parent1 != null && parent2 != null && parent1.getType().name.equals(LANG1.PARENTHESIZED_EXPRESSION) && parent2.getType().name.equals(LANG2.PARENTHESIZED_EXPRESSION)) {
                    mappingStore.addMapping(parent1, parent2);
                }
            }
        }
        else if(castExpressions1.size() > 0) {
            Tree simpleType = castExpressions1.get(0).getChild(0);
            Tree as2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.AS_EXPRESSION);
            if(as2 != null) {
                Tree userType2 = TreeUtilFunctions.findChildByType(as2, LANG2.USER_TYPE);
                if(userType2 != null) {
                    handleTypeMapping(mappingStore, simpleType, userType2, LANG1, LANG2, deferredFlattenings);
                }
            }
        }
        if(lambdas1.size() == lambdas2.size()) {
            for(int i=0; i<lambdas1.size(); i++) {
                Tree lambda1 = lambdas1.get(i);
                Tree lambda2 = lambdas2.get(i);
                List<Tree> block1 = TreeUtilFunctions.findChildrenByTypeRecursively(lambda1, LANG1.BLOCK);
                List<Tree> block2 = TreeUtilFunctions.findChildrenByTypeRecursively(lambda2, LANG2.STATEMENTS);
                if(block1.size() > 0 && block2.size() > 0) {
                    mappingStore.addMapping(block1.get(0), block2.get(0));
                    mappingStore.addMapping(block1.get(0).getParent(), block2.get(0).getParent());
                }
                Tree fragment1 = TreeUtilFunctions.findChildByType(lambda1, LANG1.VARIABLE_DECLARATION_FRAGMENT);
                List<Tree> lambdaParameters2 = TreeUtilFunctions.findChildrenByTypeRecursively(lambda2, LANG2.LAMBDA_PARAMETERS);
                if(fragment1 != null && lambdaParameters2.size() > 0) {
                    Tree lambdaParameters = lambdaParameters2.get(0);
                    List<Tree> variableDeclarations = TreeUtilFunctions.findChildrenByTypeRecursively(lambdaParameters, LANG2.VARIABLE_DECLARATION);
                    if(variableDeclarations.size() > 0) {
                        Tree simpleName1 = TreeUtilFunctions.findChildByType(fragment1, LANG1.SIMPLE_NAME);
                        Tree simpleName2 = TreeUtilFunctions.findChildByType(variableDeclarations.get(0), LANG2.SIMPLE_NAME);
                        if(simpleName1 != null && simpleName2 != null) {
                            mappingStore.addMapping(simpleName1, simpleName2);
                        }
                    }
                }
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.THIS_EXPRESSION);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.THIS_EXPRESSION);
        //the statement itself is a this expression
        if(srcStatementNode.getType().name.equals(LANG1.THIS_EXPRESSION)) {
            children1.add(0, srcStatementNode);
        }
        if(dstStatementNode.getType().name.equals(LANG2.THIS_EXPRESSION)) {
            children2.add(0, dstStatementNode);
        }
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0) {
                    children2.get(i).setLabel(children1.get(i).getLabel());
                    children2.get(i).getChildren().remove(0);
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                    //align Java ThisExpression -> SimpleName with Kotlin this_expression -> [this@, type_identifier]
                    if(children1.get(i).getChildren().size() == 1 && children2.get(i).getChildren().size() == 1) {
                        Tree qualifier1 = children1.get(i).getChild(0);
                        Tree qualifier2 = children2.get(i).getChild(0);
                        if(qualifier1.getType().name.equals(LANG1.SIMPLE_NAME) && qualifier2.getType().name.equals(LANG2.TYPE_IDENTIFIER) &&
                                qualifier1.getLabel().equals(qualifier2.getLabel())) {
                            mappingStore.addMapping(qualifier1, qualifier2);
                        }
                    }
                }
                //align Java X.this.wait() with Kotlin (this@X as Object).wait(), by flattening the Kotlin parenthesized cast into the receiver
                Tree this1 = children1.get(i);
                Tree this2 = children2.get(i);
                if(this1.getParent() != null && this1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION_RECEIVER)) {
                    Tree as2 = this2.getParent();
                    if(as2 != null && as2.getType().name.equals(LANG2.AS_EXPRESSION) && as2.getChild(0) == this2) {
                        Tree parenthesized2 = as2.getParent();
                        if(parenthesized2 != null && parenthesized2.getType().name.equals(LANG2.PARENTHESIZED_EXPRESSION) && parenthesized2.getChildren().size() == 1 &&
                                parenthesized2.getParent() != null && parenthesized2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION)) {
                            Tree navigation2 = parenthesized2.getParent();
                            flattenDstChildKeepingMappings(deferredFlattenings, navigation2, parenthesized2);
                            flattenDstChildKeepingMappings(deferredFlattenings, navigation2, as2);
                        }
                    }
                }
                else {
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                }
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.CHARACTER_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.CHARACTER_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0) {
                    children2.get(i).setLabel(children1.get(i).getLabel());
                    children2.get(i).getChildren().clear();
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                }
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.STRING_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.STRING_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0) {
                    children2.get(i).setLabel(children1.get(i).getLabel());
                    children2.get(i).getChildren().remove(0);
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                }
                else {
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                }
            }
        }
        else if(children1.size() > children2.size() && (interpolatedExpressions2.size() > 0 || interpolatedIdentifiers2.size() > 0)) {
            List<Tree> stringLiteralsInInterpolatedExpression = new ArrayList<>();
            for(Tree interpolated : interpolatedExpressions2) {
                stringLiteralsInInterpolatedExpression.addAll(TreeUtilFunctions.findChildrenByTypeRecursively(interpolated, LANG2.STRING_LITERAL));
            }
            List<Tree> children1ToBeRemoved = new ArrayList<>();
            List<Tree> children2ToBeRemoved = new ArrayList<>();
            for(int j=0; j<stringLiteralsInInterpolatedExpression.size(); j++) {
                Tree child2 = stringLiteralsInInterpolatedExpression.get(j);
                for(int i=0; i<children1.size(); i++) {
                    Tree child1 = children1.get(i);
                    if(child2.getChildren().size() > 0 && child1.getLabel().equals("\"" + child2.getChild(0).getLabel() + "\"")) {
                        child2.setLabel(child1.getLabel());
                        child2.getChildren().remove(0);
                        mappingStore.addMapping(child1, child2);
                        children1ToBeRemoved.add(child1);
                        children2ToBeRemoved.add(child2);
                        break;
                    }
                }
            }
            children1.removeAll(children1ToBeRemoved);
            children2.removeAll(children2ToBeRemoved);
            for(int j=0; j<children2.size(); j++) {
                Tree child2 = children2.get(j);
                List<Tree> stringContents = TreeUtilFunctions.findChildrenByTypeRecursively(child2,LANG2.STRING_CONTENT);
                for(int i=0; i<children1.size(); i++) {
                    Tree child1 = children1.get(i);
                    for(Tree stringContent : stringContents) {
                        if(child1.getLabel().equals("\"" + stringContent.getLabel() + "\"")) {
                            stringContent.setLabel(child1.getLabel());
                            mappingStore.addMapping(child1, stringContent);
                            break;
                        }
                    }
                }
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.NUMBER_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.INTEGER_LITERAL, LANG2.FLOAT_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                //handle -literal
                Tree parent1 = children1.get(i).getParent();
                Tree parent2 = children2.get(i).getParent();
                if(!parent2.getType().name.equals(LANG2.LONG_LITERAL)) {
                    mappingStore.addMapping(children1.get(i), children2.get(i));
                }
                if(parent1.getType().name.equals(LANG1.PREFIX_EXPRESSION) && parent2.getType().name.equals(LANG2.PREFIX_EXPRESSION)) {
                    if(parent2.getChild(0).getType().name.equals(LANG2.ARITHMETIC_OPERATOR)) {
                        Tree t1 = TreeUtilFunctions.findChildByType(parent1, LANG1.PREFIX_EXPRESSION_OPERATOR);
                        Tree t2 = TreeUtilFunctions.findChildByType(parent2, LANG1.ARITHMETIC_OPERATOR);
                        mappingStore.addMapping(t1, t2);
                        mappingStore.addMapping(parent1, parent2);
                    }
                }
                //handle long literal
                if(parent2.getType().name.equals(LANG2.LONG_LITERAL)) {
                    parent2.setLabel(children1.get(i).getLabel());
                    parent2.getChildren().clear();
                    mappingStore.addMapping(children1.get(i), parent2);
                }
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.BOOLEAN_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.BOOLEAN_LITERAL);
        //the statement itself is a boolean literal, i.e., a field initializer
        if(srcStatementNode.getType().name.equals(LANG1.BOOLEAN_LITERAL)) {
            children1.add(0, srcStatementNode);
        }
        if(dstStatementNode.getType().name.equals(LANG2.BOOLEAN_LITERAL)) {
            children2.add(0, dstStatementNode);
        }
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                //Java BooleanLiteral is a leaf, while Kotlin boolean_literal has a true/false leaf child
                Tree literal2 = children2.get(i);
                if(literal2.getChildren().size() == 1 && literal2.getChild(0).isLeaf()) {
                    Tree value2 = literal2.getChild(0);
                    if(mappingStore.isDstMapped(value2)) {
                        for(Tree src : new ArrayList<>(mappingStore.getSrcs(value2))) {
                            mappingStore.removeMapping(src, value2);
                        }
                    }
                    literal2.setLabel(value2.getLabel());
                    literal2.getChildren().clear();
                }
                mappingStore.addMapping(children1.get(i), literal2);
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.NULL_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.NULL_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                children1.get(i).setLabel("null");
                mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.PREFIX_EXPRESSION_OPERATOR);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.NOT_PREFIX_OPERATOR);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
                //align Kotlin prefix_expression -> ! with Java PrefixExpression -> PREFIX_EXPRESSION_OPERATOR
                Tree parent1 = children1.get(i).getParent();
                Tree parent2 = children2.get(i).getParent();
                if(parent1 != null && parent2 != null && parent1.getType().name.equals(LANG1.PREFIX_EXPRESSION) && parent2.getType().name.equals(LANG2.PREFIX_EXPRESSION)) {
                    mappingStore.addMapping(parent1, parent2);
                }
            }
        }
        nestExtendedOperands(srcStatementNode, LANG1);
        List<Tree> nestedInfix1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.INFIX_EXPRESSION);
        if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION)) {
            nestedInfix1.add(0, srcStatementNode);
        }
        List<Tree> nestedInfix2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.DISJUNCTION_EXPRESSION, LANG2.CONJUNCTION_EXPRESSION, LANG2.EQUALITY_EXPRESSION, LANG2.ADDITIVE_EXPRESSION, LANG2.COMPARISON_EXPRESSION, LANG2.MULTIPLICATIVE_EXPRESSION);
        if(dstStatementNode.getType().name.equals(LANG2.DISJUNCTION_EXPRESSION) ||
                dstStatementNode.getType().name.equals(LANG2.CONJUNCTION_EXPRESSION) ||
                dstStatementNode.getType().name.equals(LANG2.EQUALITY_EXPRESSION) ||
                dstStatementNode.getType().name.equals(LANG2.ADDITIVE_EXPRESSION) ||
                dstStatementNode.getType().name.equals(LANG2.COMPARISON_EXPRESSION) ||
                dstStatementNode.getType().name.equals(LANG2.MULTIPLICATIVE_EXPRESSION)) {
            nestedInfix2.add(0, dstStatementNode);
        }
        alignAndMatchInfixExpressions(nestedInfix1, nestedInfix2, LANG1, LANG2, mappingStore);
        Tree variableDeclarationFragment = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.VARIABLE_DECLARATION_FRAGMENT);
        Tree variableDeclaration = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.VARIABLE_DECLARATION);
        Tree affectationOperator = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.AFFECTATION_OPERATOR);
        if(variableDeclarationFragment != null && variableDeclaration != null && affectationOperator != null) {
            variableDeclarationFragment.setLabel("=");
            mappingStore.addMapping(variableDeclarationFragment, affectationOperator);
        }
        Tree assignment = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.ASSIGNMENT);
        Tree assignableExpression = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.DIRECTLY_ASSIGNABLE_EXPRESSION);
        if(assignment != null && assignableExpression != null && affectationOperator != null) {
            Tree assignmentOperator = TreeUtilFunctions.findChildByType(assignment, LANG1.ASSIGNMENT_OPERATOR);
            mappingStore.addMapping(assignmentOperator, affectationOperator);
            mappingStore.addMapping(assignment, assignableExpression);
        }
        //handle case of method invocation converted to navigation expression
        //children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.NAVIGATION_EXPRESSION);
        if(children2.size() > 0) {
            Tree lastChild = children2.get(children2.size()-1);
            if(lastChild.getChildren().size() > 0) {
                Tree lastGrandChild = lastChild.getChildren().get(lastChild.getChildren().size()-1);
                if(lastGrandChild.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && lastGrandChild.getChildren().size() > 0 &&
                        lastGrandChild.getChildren().get(0).getLabel().equals("code")) {
                    //remove .code on character literals to convert to int
                    children2.remove(children2.size()-1);
                }
            }
        }
        if(dstStatementNode.getType().name.equals(LANG2.NAVIGATION_EXPRESSION)) {
            children2.add(0, dstStatementNode);
        }
        if(equalsMismatch) {
            Iterator<Tree> iterator1 = inv1.iterator();
            while(iterator1.hasNext()) {
                Tree t1 = iterator1.next();
                Tree simpleName = TreeUtilFunctions.findChildByType(t1, LANG1.SIMPLE_NAME);
                if(simpleName != null && simpleName.getLabel().equals("equals")) {
                    iterator1.remove();
                }
            }
        }
        Iterator<Tree> iterator2 = children2.iterator();
        while(iterator2.hasNext()) {
            Tree t2 = iterator2.next();
            //skip the navigation expressions being the callee of a call, but not the call arguments, i.e., connection.inputStream in InputStreamReader(connection.inputStream, UTF_8)
            boolean callee = t2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION) && t2.getParent().getChild(0) == t2;
            if(callee || qualifiedNameToNavigationExpression.containsValue(t2)) {
                iterator2.remove();
            }
        }
        if(inv1.size() == children2.size()) {
            for(int i=0; i<inv1.size(); i++) {
                if(alignMethodInvocationWithPropertyAccess(mappingStore, inv1.get(i), children2.get(i), LANG1, LANG2, deferredFlattenings))
                    continue;
                Tree navigationSuffix = TreeUtilFunctions.findChildByType(children2.get(i), LANG2.NAVIGATION_SUFFIX);
                if(navigationSuffix != null)
                    mappingStore.addMapping(inv1.get(i), navigationSuffix);
                boolean skip = inv1.get(i).getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT) && children2.get(i).getParent().getType().name.equals(LANG2.VALUE_ARGUMENT);
                if(!skip)
                    mappingStore.addMapping(inv1.get(i), children2.get(i));
            }
        }
        if(inv1.size() == 1 && assignableExpression != null) {
            Tree navigationSuffix = TreeUtilFunctions.findChildByType(assignableExpression, LANG2.NAVIGATION_SUFFIX);
            if(navigationSuffix != null)
                mappingStore.addMapping(inv1.get(0), navigationSuffix);
        }
        boolean returnStatement1 = srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT);
        boolean throwStatement1 = srcStatementNode.getType().name.equals(LANG1.THROW_STATEMENT);
        if((returnStatement1 || throwStatement1) && dstStatementNode.getType().name.equals(LANG2.CONTROL_STRUCTURE_BODY) &&
                dstStatementNode.getChildren().size() > 0 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_EXPRESSION)) {
            Tree jumpExpression = dstStatementNode.getChild(0);
            Tree firstChild = jumpExpression.getChild(0);
            if(firstChild.getLabel().equals("return@")) {
                firstChild.setLabel("return");
            }
            if(dstStatementNode.getChildren().size() == 1 && isJumpKeywordWithAlignedChildren(srcStatementNode, jumpExpression, LANG1, LANG2)) {
                //align Kotlin control_structure_body -> jump_expression -> [return|throw, expression?] with Java ReturnStatement|ThrowStatement -> expression?
                flattenDstChild(mappingStore, deferredFlattenings, dstStatementNode, jumpExpression);
                removeDstChild(mappingStore, deferredFlattenings, dstStatementNode, firstChild);
            }
            else if(returnStatement1) {
                mappingStore.addMapping(srcStatementNode, firstChild);
            }
        }
        if((returnStatement1 || throwStatement1) && dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION) &&
                dstStatementNode.getChildren().size() > 0 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_KEYWORD)) {
            Tree jumpKeyword = dstStatementNode.getChild(0);
            if(isJumpKeywordWithAlignedChildren(srcStatementNode, dstStatementNode, LANG1, LANG2)) {
                //align Kotlin jump_expression -> [return|throw, expression?] with Java ReturnStatement|ThrowStatement -> expression?, as the Java statement includes the keyword
                removeDstChild(mappingStore, deferredFlattenings, dstStatementNode, jumpKeyword);
            }
            else if(returnStatement1) {
                mappingStore.addMapping(srcStatementNode, jumpKeyword);
            }
        }
        if(srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && srcStatementNode.getChildren().size() == 1 && !srcStatementNode.getChild(0).isLeaf() &&
                dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.FUNCTION_BODY) &&
                TreeUtilFunctions.findChildByType(dstStatementNode.getParent(), LANG2.AFFECTATION_OPERATOR) != null) {
            //align Java ReturnStatement -> expression with Kotlin function_body -> [=, expression], as Kotlin expression body has no return statement wrapper
            Tree expression1 = srcStatementNode.getChild(0);
            flattenSrcChild(mappingStore, deferredFlattenings, srcStatementNode, expression1);
        }
        if(isAssertCall(srcStatementNode, dstStatementNode, LANG1, LANG2)) {
            alignAssertStatement(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2, deferredFlattenings);
        }
        if(srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && srcStatementNode.getChildren().size() == 1 &&
                srcStatementNode.getChild(0).getType().name.equals(LANG1.METHOD_INVOCATION) && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            //align Java ExpressionStatement -> MethodInvocation with Kotlin call_expression, as Kotlin has no expression statement wrapper
            Tree invocation1 = srcStatementNode.getChild(0);
            flattenSrcChild(mappingStore, deferredFlattenings, srcStatementNode, invocation1);
        }
        if(srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && srcStatementNode.getChildren().size() == 1 &&
                srcStatementNode.getChild(0).getType().name.equals(LANG1.ASSIGNMENT) && dstStatementNode.getType().name.equals(LANG2.ASSIGNMENT)) {
            //align Java ExpressionStatement -> Assignment -> [target, operator, value] with Kotlin assignment -> [target, operator, value]
            Tree statementAssignment1 = srcStatementNode.getChild(0);
            //the node with the [target, operator, value] children: the Assignment, or the statement if the Assignment is flattened immediately
            Tree assignmentParent1 = statementAssignment1;
            flattenSrcChild(mappingStore, deferredFlattenings, srcStatementNode, statementAssignment1);
            if(deferredFlattenings == null) {
                assignmentParent1 = srcStatementNode;
            }
            Tree target1 = assignmentParent1.getChild(0);
            Tree assignableExpression2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.DIRECTLY_ASSIGNABLE_EXPRESSION);
            if(assignableExpression2 != null) {
                if(assignableExpression2.getChildren().size() == 1) {
                    //align directly_assignable_expression -> name with Java name
                    flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, assignableExpression2);
                }
                else if(assignableExpression2.getChildren().size() > 1) {
                    alignFieldAccess(mappingStore, target1, assignableExpression2, LANG1, LANG2, deferredFlattenings);
                }
            }
            Tree value1 = assignmentParent1.getChild(assignmentParent1.getChildren().size() - 1);
            Tree value2 = dstStatementNode.getChild(dstStatementNode.getChildren().size() - 1);
            if(!mappingStore.isDstMapped(value2) && value2.getChildren().size() == 1 && mappingStore.getSrcs(value2.getChild(0)) != null &&
                    mappingStore.getSrcs(value2.getChild(0)).contains(value1)) {
                //align Kotlin value wrapper (e.g., boolean_literal -> true) with Java value
                flattenDstChildKeepingMappings(deferredFlattenings, dstStatementNode, value2);
            }
            else if(!mappingStore.isDstMapped(value2) && value2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) && value1.getChildren().size() == 2 &&
                    value2.getChildren().size() == 2 && value2.getChild(1).getType().name.equals(LANG2.NAVIGATION_SUFFIX) &&
                    value2.getChild(1).getChildren().size() == 1 && mappingStore.getSrcs(value2.getChild(1).getChild(0)) != null &&
                    mappingStore.getSrcs(value2.getChild(1).getChild(0)).contains(value1.getChild(1))) {
                alignFieldAccess(mappingStore, value1, value2, LANG1, LANG2, deferredFlattenings);
            }
        }
        if(srcStatementNode.getType().name.equals(LANG1.VARIABLE_DECLARATION_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.FIELD_DECLARATION)) {
            alignVariableDeclaration(mappingStore, srcStatementNode, dstStatementNode, false, LANG1, LANG2, deferredFlattenings);
            //the variable type is explicit in Kotlin, i.e., var errorException: IOException? = null
            alignTypes(mappingStore, findJavaType(srcStatementNode, LANG1), findKotlinType(dstStatementNode, LANG2), LANG1, LANG2, deferredFlattenings);
        }
    }

    //restructures the Java and Kotlin field declarations, so that the name, = and initializer have the same parent (see alignVariableDeclaration)
    //must be executed after matching the attribute initializers, because they are located through the VariableDeclarationFragment, which becomes a leaf
    public static void alignFieldDeclaration(ExtendedMultiMappingStore mappingStore, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2) {
        alignFieldDeclaration(mappingStore, srcFieldDeclaration, dstFieldDeclaration, LANG1, LANG2, null);
    }

    public static void alignFieldDeclaration(ExtendedMultiMappingStore mappingStore, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(srcFieldDeclaration == null || dstFieldDeclaration == null)
            return;
        if(!srcFieldDeclaration.getType().name.equals(LANG1.FIELD_DECLARATION))
            return;
        Set<Tree> dsts = mappingStore.getDsts(srcFieldDeclaration);
        if(dsts == null || !dsts.contains(dstFieldDeclaration))
            return;
        if(dstFieldDeclaration.getType().name.equals(LANG2.FIELD_DECLARATION)) {
            alignVariableDeclaration(mappingStore, srcFieldDeclaration, dstFieldDeclaration, true, LANG1, LANG2, deferredFlattenings);
        }
        else if(dstFieldDeclaration.getType().name.equals(LANG2.CLASS_PARAMETER)) {
            //align Kotlin class_parameter -> [name, type] with Java FieldDeclaration -> [type, VariableDeclarationFragment -> name]
            List<Tree> fragments1 = TreeUtilFunctions.findChildrenByType(srcFieldDeclaration, LANG1.VARIABLE_DECLARATION_FRAGMENT);
            if(fragments1.size() == 1 && fragments1.get(0).getChildren().size() == 1) {
                Tree fragment1 = fragments1.get(0);
                flattenSrcChild(mappingStore, deferredFlattenings, srcFieldDeclaration, fragment1);
            }
        }
    }

    //align Kotlin property_declaration -> [variable_declaration -> [name, type], =, initializer] with Java declaration -> [type, VariableDeclarationFragment -> [name, initializer]]
    private static void alignVariableDeclaration(ExtendedMultiMappingStore mappingStore, Tree srcDeclaration, Tree dstDeclaration, boolean mapFragmentToAffectationOperator, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        List<Tree> fragments1 = TreeUtilFunctions.findChildrenByType(srcDeclaration, LANG1.VARIABLE_DECLARATION_FRAGMENT);
        Tree variableDeclaration2 = TreeUtilFunctions.findChildByType(dstDeclaration, LANG2.VARIABLE_DECLARATION);
        if(fragments1.size() == 1 && variableDeclaration2 != null) {
            flattenDstChildKeepingMappings(deferredFlattenings, dstDeclaration, variableDeclaration2);
            Tree fragment1 = fragments1.get(0);
            Tree affectationOperator2 = TreeUtilFunctions.findChildByType(dstDeclaration, LANG2.AFFECTATION_OPERATOR);
            if(mapFragmentToAffectationOperator && affectationOperator2 != null && fragment1.getChildren().size() > 1) {
                fragment1.setLabel(affectationOperator2.getLabel());
                mappingStore.addMapping(fragment1, affectationOperator2);
            }
            Set<Tree> fragmentDsts = mappingStore.getDsts(fragment1);
            if(deferredFlattenings != null) {
                //the fragment is kept as the leaf matching the = operator, or flattened, after all diffs are matched
                deferredFlattenings.alignVariableDeclarationFragment(srcDeclaration, fragment1, LANG2.AFFECTATION_OPERATOR);
            }
            else if(affectationOperator2 != null && fragmentDsts != null && fragmentDsts.contains(affectationOperator2) && fragment1.getChildren().size() > 1) {
                //keep the fragment as the leaf matching the = operator, placed between the name and the initializer
                int index = srcDeclaration.getChildPosition(fragment1);
                Tree name1 = fragment1.getChild(0);
                List<Tree> rest1 = new ArrayList<>(fragment1.getChildren().subList(1, fragment1.getChildren().size()));
                fragment1.getChildren().clear();
                srcDeclaration.getChildren().add(index, name1);
                name1.setParent(srcDeclaration);
                srcDeclaration.getChildren().addAll(index + 2, rest1);
                for(Tree t : rest1)
                    t.setParent(srcDeclaration);
            }
            else {
                if(fragmentDsts != null) {
                    for(Tree dst : new ArrayList<>(fragmentDsts)) {
                        mappingStore.removeMapping(fragment1, dst);
                    }
                }
                flattenChild(srcDeclaration, fragment1);
            }
        }
    }

    //align Kotlin navigation_expression -> [receiver, navigation_suffix -> name] with Java MethodInvocation -> [METHOD_INVOCATION_RECEIVER -> receiver, name]
    //for method invocations without arguments converted to property accesses, i.e., debugData.size() -> debugData.size
    private static boolean alignMethodInvocationWithPropertyAccess(ExtendedMultiMappingStore mappingStore, Tree invocation1, Tree navigation2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(!invocation1.getType().name.equals(LANG1.METHOD_INVOCATION) || invocation1.getChildren().size() != 2)
            return false;
        Tree receiver1 = invocation1.getChild(0);
        Tree name1 = invocation1.getChild(1);
        if(!receiver1.getType().name.equals(LANG1.METHOD_INVOCATION_RECEIVER) || receiver1.getChildren().size() != 1 || !name1.getType().name.equals(LANG1.SIMPLE_NAME))
            return false;
        if(!navigation2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) || navigation2.getChildren().size() != 2)
            return false;
        Tree suffix2 = navigation2.getChild(1);
        if(!suffix2.getType().name.equals(LANG2.NAVIGATION_SUFFIX) || suffix2.getChildren().size() != 1 || !suffix2.getChild(0).isLeaf())
            return false;
        Tree name2 = suffix2.getChild(0);
        flattenDstChild(mappingStore, deferredFlattenings, navigation2, suffix2);
        flattenSrcChild(mappingStore, deferredFlattenings, invocation1, receiver1);
        mappingStore.addMapping(invocation1, navigation2);
        mappingStore.addMapping(name1, name2);
        //align Kotlin navigation_expression -> [postfix_expression -> [receiver, !!], name] as navigation_expression -> [receiver, !!, name], i.e., requestUrl.scheme() -> requestUrl!!.scheme
        Tree receiverPostfix2 = navigation2.getChild(0);
        if(isNonNullAssertion(receiverPostfix2, LANG2)) {
            Tree receiver2 = receiverPostfix2.getChild(0);
            Tree assertion2 = receiverPostfix2.getChild(1);
            removeDstMappings(mappingStore, receiverPostfix2);
            navigation2.getChildren().set(0, receiver2);
            receiver2.setParent(navigation2);
            navigation2.insertChild(assertion2, 1);
            assertion2.setParent(navigation2);
        }
        //the receiver expression, whether or not the METHOD_INVOCATION_RECEIVER is already flattened
        Tree receiverExpression1 = receiver1.getChild(0);
        Tree receiverExpression2 = navigation2.getChild(0);
        if(receiverExpression1.isLeaf() && receiverExpression2.isLeaf()) {
            mappingStore.addMapping(receiverExpression1, receiverExpression2);
        }
        //align Kotlin postfix_expression -> [navigation_expression -> [receiver, name], !!] as navigation_expression -> [receiver, name, !!], i.e., response.getBody() -> response.body!!
        Tree postfix2 = navigation2.getParent();
        if(postfix2 != null && isNonNullAssertion(postfix2, LANG2) && postfix2.getChild(0) == navigation2 && postfix2.getParent() != null) {
            Tree assertion2 = postfix2.getChild(1);
            removeDstMappings(mappingStore, postfix2);
            Tree parent2 = postfix2.getParent();
            parent2.getChildren().set(parent2.getChildPosition(postfix2), navigation2);
            navigation2.setParent(parent2);
            navigation2.addChild(assertion2);
            assertion2.setParent(navigation2);
            updateRangeToChildren(navigation2);
        }
        //align Kotlin navigation_expression statement with Java ExpressionStatement -> MethodInvocation, i.e., conn.getResponseCode(); -> conn.responseCode
        Tree statement1 = invocation1.getParent();
        Set<Tree> statementDsts = statement1 != null ? mappingStore.getDsts(statement1) : null;
        if(statement1 != null && statement1.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && statement1.getChildren().size() == 1 &&
                statementDsts != null && statementDsts.contains(navigation2)) {
            if(deferredFlattenings != null) {
                deferredFlattenings.flattenRemovingMappingTo(statement1, invocation1, navigation2);
            }
            else {
                mappingStore.removeMapping(invocation1, navigation2);
                flattenChild(statement1, invocation1);
            }
        }
        return true;
    }

    //align Kotlin call_expression -> [call_expression -> [receiver, name, value_arguments], call_suffix -> annotated_lambda] with Java invocation -> [receiver, name, arguments],
    //as the call with a trailing lambda is nested in another call_expression, i.e., writerExecutor.tryExecute("OkHttp $connectionName") {...}
    private static void flattenTrailingLambdaCall(ExtendedMultiMappingStore mappingStore, Tree call2, Constants LANG2) {
        if(!call2.getType().name.equals(LANG2.METHOD_INVOCATION) || call2.getChildren().size() != 2)
            return;
        Tree innerCall2 = call2.getChild(0);
        Tree suffix2 = call2.getChild(1);
        if(!innerCall2.getType().name.equals(LANG2.METHOD_INVOCATION) || !suffix2.getType().name.equals(LANG2.CALL_SUFFIX) ||
                suffix2.getChildren().size() != 1 || !suffix2.getChild(0).getType().name.equals(LANG2.ANNOTATED_LAMBDA))
            return;
        removeDstMappings(mappingStore, innerCall2);
        removeDstMappings(mappingStore, suffix2);
        flattenChild(call2, innerCall2);
        flattenChild(call2, suffix2);
    }

    //returns the Kotlin call_expression -> [receiver, name, value_arguments, annotated_lambda -> lambda_literal], or the same with the value_arguments nested in a call_expression
    //and the annotated_lambda nested in a call_suffix, if the Java ClassInstanceCreation is the only argument of the call mapped to the value_arguments
    private static Tree findTrailingLambdaOfMappedCall(ExtendedMultiMappingStore mappingStore, Tree classInstanceCreation1, Constants LANG1, Constants LANG2) {
        if(classInstanceCreation1 == null || !classInstanceCreation1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION))
            return null;
        Tree arguments1 = classInstanceCreation1.getParent();
        if(arguments1 == null || !arguments1.getType().name.equals(LANG1.METHOD_INVOCATION_ARGUMENTS) || arguments1.getChildren().size() != 1 || !mappingStore.isSrcMapped(arguments1))
            return null;
        for(Tree arguments2 : mappingStore.getDsts(arguments1)) {
            Tree call2 = arguments2.getParent();
            //the value_arguments are nested in the call_suffix, if its flattening is deferred (see alignCallSuffix)
            Tree callSuffix2 = null;
            if(call2 != null && call2.getType().name.equals(LANG2.CALL_SUFFIX)) {
                callSuffix2 = call2;
                call2 = call2.getParent();
            }
            if(!arguments2.getType().name.equals(LANG2.METHOD_INVOCATION_ARGUMENTS) || call2 == null || !call2.getType().name.equals(LANG2.METHOD_INVOCATION))
                continue;
            //the trailing lambda is a child of the call, or of the call_suffix of the enclosing call, if the calls are not flattened yet
            Tree annotatedLambda2 = TreeUtilFunctions.findChildByType(call2, LANG2.ANNOTATED_LAMBDA);
            if(annotatedLambda2 == null && callSuffix2 != null)
                annotatedLambda2 = TreeUtilFunctions.findChildByType(callSuffix2, LANG2.ANNOTATED_LAMBDA);
            Tree outerCall2 = call2.getParent();
            if(annotatedLambda2 == null && outerCall2 != null && outerCall2.getType().name.equals(LANG2.METHOD_INVOCATION) && outerCall2.getChild(0) == call2) {
                Tree suffix2 = TreeUtilFunctions.findChildByType(outerCall2, LANG2.CALL_SUFFIX);
                annotatedLambda2 = suffix2 != null ? TreeUtilFunctions.findChildByType(suffix2, LANG2.ANNOTATED_LAMBDA) : null;
            }
            Tree lambdaLiteral2 = annotatedLambda2 != null ? TreeUtilFunctions.findChildByType(annotatedLambda2, LANG2.LAMBDA_LITERAL) : null;
            if(lambdaLiteral2 != null)
                return lambdaLiteral2;
        }
        return null;
    }

    //Kotlin postfix_expression -> [expression, !!]
    private static boolean isNonNullAssertion(Tree t, Constants LANG2) {
        return t.getType().name.equals(LANG2.KOTLIN_POSTFIX_EXPRESSION) && t.getChildren().size() == 2 &&
                t.getChild(1).getType().name.equals(LANG2.NON_NULL_ASSERTION_OPERATOR);
    }

    private static void alignFieldAccess(ExtendedMultiMappingStore mappingStore, Tree fieldAccess1, Tree navigation2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        //align [receiver, navigation_suffix -> name] with Java FieldAccess -> [receiver, name]
        Tree suffix2 = navigation2.getChild(navigation2.getChildren().size() - 1);
        if(suffix2.getType().name.equals(LANG2.NAVIGATION_SUFFIX)) {
            flattenDstChildKeepingMappings(deferredFlattenings, navigation2, suffix2);
        }
        mappingStore.addMapping(fieldAccess1, navigation2);
        if(fieldAccess1.getChildren().size() > 0 && fieldAccess1.getChild(0).getType().name.equals(LANG1.THIS_EXPRESSION) &&
                !mappingStore.isSrcMapped(fieldAccess1.getChild(0)) && !mappingStore.isDstMapped(navigation2.getChild(0))) {
            mappingStore.addMapping(fieldAccess1.getChild(0), navigation2.getChild(0));
        }
    }

    //align Kotlin left-nested binary expressions, i.e., multiplicative_expression -> [multiplicative_expression -> [16, *, 1024], *, 1024],
    //with Java InfixExpression -> [16, *, 1024, 1024], where the extended operands share the operator of the first two operands
    private static void nestExtendedOperands(Tree srcStatementNode, Constants LANG1) {
        List<Tree> infixExpressions1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.INFIX_EXPRESSION);
        if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION)) {
            infixExpressions1.add(0, srcStatementNode);
        }
        for(Tree infix1 : infixExpressions1) {
            List<Tree> children1 = new ArrayList<>(infix1.getChildren());
            if(children1.size() <= 3 || !children1.get(1).getType().name.equals(LANG1.INFIX_EXPRESSION_OPERATOR))
                continue;
            Tree operator1 = children1.get(1);
            //the extended operators are not in the tree, so they are placed at the same distance from their left operand as the first operator
            int distance = operator1.getPos() - children1.get(0).getEndPos();
            Tree left = createInfixExpression(infix1.getType(), children1.get(0), operator1, children1.get(2));
            for(int i=3; i<children1.size(); i++) {
                Tree operand = children1.get(i);
                DefaultTree operator = new DefaultTree(operator1.getType(), operator1.getLabel());
                int pos = left.getEndPos() + distance;
                if(pos < left.getEndPos() || pos + operator1.getLength() > operand.getPos())
                    pos = Math.max(left.getEndPos(), operand.getPos() - operator1.getLength());
                operator.setPos(pos);
                operator.setLength(operator1.getLength());
                if(i < children1.size() - 1) {
                    left = createInfixExpression(infix1.getType(), left, operator, operand);
                }
                else {
                    //the original node remains the outermost expression, keeping its mappings
                    infix1.setChildren(new ArrayList<>(List.of(left, operator, operand)));
                    for(Tree child : infix1.getChildren())
                        child.setParent(infix1);
                }
            }
        }
    }

    private static Tree createInfixExpression(com.github.gumtreediff.tree.Type type, Tree left, Tree operator, Tree right) {
        DefaultTree infix = new DefaultTree(type, "");
        infix.setChildren(new ArrayList<>(List.of(left, operator, right)));
        for(Tree child : infix.getChildren())
            child.setParent(infix);
        infix.setPos(left.getPos());
        infix.setLength(right.getEndPos() - left.getPos());
        return infix;
    }

    private static void alignAndMatchInfixExpressions(List<Tree> children1, List<Tree> children2, Constants LANG1, Constants LANG2, ExtendedMultiMappingStore mappingStore) {
        if(children1.size() == children2.size()) {
            List<Tree> matched2 = new ArrayList<>();
            for(Tree child1 : children1) {
                Tree operator1 = TreeUtilFunctions.findChildByType(child1, LANG1.INFIX_EXPRESSION_OPERATOR);
                for(Tree child2 : children2) {
                    Tree operator2 = TreeUtilFunctions.findChildByType(child2, LANG2.LOGICAL_OPERATOR, LANG2.COMPARISON_OPERATOR, LANG2.ARITHMETIC_OPERATOR, "<=", ">=", "%");
                    boolean invertOperator = children1.size() == 1 &&
                            ((operator1.getLabel().equals("==") && operator2.getLabel().equals("!=")) ||
                             (operator1.getLabel().equals("!=") && operator2.getLabel().equals("==")));
                    if(!matched2.contains(child2) && (operator1.getLabel().equals(operator2.getLabel()) || invertOperator)) {
                        mappingStore.addMapping(child1, child2);
                        mappingStore.addMapping(operator1, operator2);
                        matched2.add(child2);
                        break;
                    }
                }
            }
        }
    }

    private static boolean nameCompliance(List<Tree> children1, List<Tree> children2, Constants LANG1, Constants LANG2) {
        List<String> callNames1 = new ArrayList<>();
        for(Tree child1 : children1) {
            if(child1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
                Tree type = child1.getChildren().size() > 0 ? child1.getChild(0) : null;
                if(type != null && type.getType().name.equals(LANG1.PARAMETERIZED_TYPE) && type.getChildren().size() > 0) {
                    type = type.getChild(0);
                }
                if(type != null && type.getType().name.equals(LANG1.SIMPLE_TYPE) && type.getChildren().size() > 0) {
                    callNames1.add(type.getChild(0).getLabel());
                }
            }
            else {
                Tree simpleName = TreeUtilFunctions.findChildByType(child1, LANG1.SIMPLE_NAME);
                if(simpleName != null) {
                    callNames1.add(simpleName.getLabel());
                }
            }
        }
        List<String> callNames2 = new ArrayList<>();
        List<Tree> toBeRemoved2 = new ArrayList<>();
        for(Tree child2 : children2) {
            Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
            if(receiver2 != null) {
                Tree navigationSuffix2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                if(navigationSuffix2 != null) {
                    Tree simpleName = TreeUtilFunctions.findChildByType(navigationSuffix2, LANG2.SIMPLE_NAME);
                    if(simpleName.getChildren().size() > 0)
                        callNames2.add(simpleName.getChild(0).getLabel());
                    else
                        callNames2.add(simpleName.getLabel());
                }
                else if(receiver2.getLabel() != null){
                    callNames2.add(receiver2.getLabel());
                }
            }
            else {
                Tree simpleName = TreeUtilFunctions.findChildByType(child2, LANG2.SIMPLE_NAME);
                if(simpleName != null) {
                    callNames2.add(simpleName.getLabel());
                }
                else {
                    toBeRemoved2.add(child2);
                }
            }
        }
        //import okhttp3.internal.tryExecute
        //tryExecute in an internal okhttp Kotlin function
        Map<String, String> synonyms = Map.of("url", "toUrl", "getBytes", "toByteArray", "asList", "listOf", "get", "toHttpUrl", "min", "minOf", "execute", "tryExecute", "toArray", "toTypedArray");
        if(callNames1.size() <= callNames2.size()) {
            int matches = 0;
            for(int i=0; i<callNames1.size(); i++) {
                String s1 = callNames1.get(i);
                String s2 = callNames2.get(i);
                if(s1.equals(s2) || s1.contains("." + s2)) {
                    matches++;
                }
                else if(s1.equals("isEmpty") && s2.equals("isNotEmpty")) {
                    matches++;
                }
                else if(synonyms.containsKey(s1) && synonyms.get(s1).equals(s2)) {
                    matches++;
                }
                else if(s1.startsWith(s2) || s2.startsWith(s1)) {
                    matches++;
                }
            }
            if(matches == callNames1.size()) {
                children2.removeAll(toBeRemoved2);
                return true;
            }
        }
        else if(callNames2.size() < callNames1.size()) {
            int matches = 0;
            for(int i=0; i<callNames2.size(); i++) {
                String s1 = callNames1.get(i);
                String s2 = callNames2.get(i);
                if(s1.equals(s2) || s1.contains("." + s2)) {
                    matches++;
                }
                else if(s1.equals("isEmpty") && s2.equals("isNotEmpty")) {
                    matches++;
                }
                else if(synonyms.containsKey(s1) && synonyms.get(s1).equals(s2)) {
                    matches++;
                }
                else if(s1.startsWith(s2) || s2.startsWith(s1)) {
                    matches++;
                }
            }
            if(matches == callNames2.size()) {
                return true;
            }
        }
        List<String> callNamesReplacedWithSynonyms2 = new ArrayList<>();
        for(String callName2 : callNames2) {
            if(synonyms.containsValue(callName2)) {
                Optional<String> key = synonyms.entrySet().stream()
                        .filter(entry -> callName2.equals(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .findFirst();
                if(key.isPresent()) {
                    callNamesReplacedWithSynonyms2.add(key.get());
                }
            }
            else {
                callNamesReplacedWithSynonyms2.add(callName2);
            }
        }
        if(callNames1.size() > callNames2.size() && callNames1.containsAll(callNamesReplacedWithSynonyms2)) {
            //sort callNames1 based on callNames2
            List<Tree> newChildren1 = new ArrayList<>();
            for(String s : callNamesReplacedWithSynonyms2) {
                int index = callNames1.indexOf(s);
                newChildren1.add(children1.get(index));
            }
            for(String s : callNames1) {
                if(!callNamesReplacedWithSynonyms2.contains(s)) {
                    int index = callNames1.indexOf(s);
                    newChildren1.add(children1.get(index));
                }
            }
            children1.clear();
            children1.addAll(newChildren1);
            return true;
        }
        else if(callNames2.size() > callNames1.size() && (callNames2.containsAll(callNames1) || callNamesReplacedWithSynonyms2.containsAll(callNames1))) {
            //sort callNames2 based on callNames1
            List<Tree> newChildren2 = new ArrayList<>();
            for(String s : callNames1) {
                int index = callNames2.indexOf(s);
                if(index == -1)
                    index = callNamesReplacedWithSynonyms2.indexOf(s);
                newChildren2.add(children2.get(index));
            }
            for(String s : callNames2) {
                if(!callNames1.contains(s)) {
                    int index = callNames2.indexOf(s);
                    if(!newChildren2.contains(children2.get(index)))
                        newChildren2.add(children2.get(index));
                }
            }
            children2.clear();
            children2.addAll(newChildren2);
            return true;
        }
        return false;
    }

    //align Java builder chain MethodInvocation -> [METHOD_INVOCATION_RECEIVER -> MethodInvocation -> [... -> ClassInstanceCreation -> SimpleType -> QualifiedName X.Builder], a, METHOD_INVOCATION_ARGUMENTS -> arg], build]
    //with Kotlin constructor call call_expression -> [X, call_suffix -> value_arguments -> value_argument -> [a, =, arg]], i.e., new X.Builder().a(1).build() -> X(a = 1)
    //the mapped Java and Kotlin nodes are added to builderNodes1 and builderNodes2, so that they are excluded from the matching of simple names and invocations by position
    private static void alignBuilderWithNamedArguments(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2,
            List<Tree> builderNodes1, List<Tree> builderNodes2) {
        for(Tree build1 : srcStatementNode.preOrder()) {
            if(!build1.getType().name.equals(LANG1.METHOD_INVOCATION) || builderNodes1.contains(build1))
                continue;
            Tree buildName1 = TreeUtilFunctions.findChildByType(build1, LANG1.SIMPLE_NAME);
            if(buildName1 == null || !buildName1.getLabel().equals("build") || TreeUtilFunctions.findChildByType(build1, LANG1.METHOD_INVOCATION_ARGUMENTS) != null)
                continue;
            //the chained calls between the builder creation and build()
            List<Tree> chain1 = new ArrayList<>();
            Tree creation1 = null;
            Tree current1 = build1;
            while(true) {
                Tree receiver1 = TreeUtilFunctions.findChildByType(current1, LANG1.METHOD_INVOCATION_RECEIVER);
                if(receiver1 == null || receiver1.getChildren().size() != 1)
                    break;
                Tree inner1 = receiver1.getChild(0);
                if(inner1.getType().name.equals(LANG1.METHOD_INVOCATION)) {
                    chain1.add(inner1);
                    current1 = inner1;
                }
                else {
                    if(inner1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION))
                        creation1 = inner1;
                    break;
                }
            }
            if(creation1 == null || chain1.isEmpty() || creation1.getChildren().isEmpty())
                continue;
            Tree type1 = creation1.getChild(0);
            if(!type1.getType().name.equals(LANG1.SIMPLE_TYPE) || type1.getChildren().size() != 1 || !type1.getChild(0).getType().name.equals(LANG1.QUALIFIED_NAME))
                continue;
            Tree qualifiedName1 = type1.getChild(0);
            String qualifiedType = qualifiedName1.getLabel();
            if(!qualifiedType.endsWith(".Builder"))
                continue;
            String builtType = qualifiedType.substring(0, qualifiedType.length() - ".Builder".length());
            String className = builtType.substring(builtType.lastIndexOf('.') + 1);
            Map<String, Tree> chainByName1 = new LinkedHashMap<>();
            for(Tree call1 : chain1) {
                Tree name1 = TreeUtilFunctions.findChildByType(call1, LANG1.SIMPLE_NAME);
                Tree arguments1 = TreeUtilFunctions.findChildByType(call1, LANG1.METHOD_INVOCATION_ARGUMENTS);
                if(name1 != null && arguments1 != null && arguments1.getChildren().size() == 1)
                    chainByName1.putIfAbsent(name1.getLabel(), call1);
            }
            for(Tree call2 : dstStatementNode.preOrder()) {
                if(!call2.getType().name.equals(LANG2.METHOD_INVOCATION) || call2.getChildren().isEmpty() || builderNodes2.contains(call2))
                    continue;
                Tree name2 = call2.getChild(0);
                if(!name2.getType().name.equals(LANG2.SIMPLE_NAME) || !name2.getLabel().equals(className))
                    continue;
                Tree callSuffix2 = TreeUtilFunctions.findChildByType(call2, LANG2.CALL_SUFFIX);
                Tree valueArguments2 = TreeUtilFunctions.findChildByType(callSuffix2 != null ? callSuffix2 : call2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                if(valueArguments2 == null || valueArguments2.getChildren().isEmpty())
                    continue;
                //all arguments are named, and each name corresponds to a call of the builder chain
                boolean allNamed = true;
                for(Tree valueArgument2 : valueArguments2.getChildren()) {
                    if(!isNamedArgument(valueArgument2, LANG2) || !chainByName1.containsKey(valueArgument2.getChild(0).getLabel())) {
                        allNamed = false;
                        break;
                    }
                }
                if(!allNamed)
                    continue;
                mappingStore.addMapping(build1, call2);
                mappingStore.addMapping(qualifiedName1, name2);
                for(Tree valueArgument2 : valueArguments2.getChildren()) {
                    Tree call1 = chainByName1.get(valueArgument2.getChild(0).getLabel());
                    Tree argument1 = TreeUtilFunctions.findChildByType(call1, LANG1.METHOD_INVOCATION_ARGUMENTS).getChild(0);
                    Tree argument2 = valueArgument2.getChild(2);
                    mappingStore.addMapping(call1, valueArgument2);
                    mappingStore.addMapping(TreeUtilFunctions.findChildByType(call1, LANG1.SIMPLE_NAME), valueArgument2.getChild(0));
                    mappingStore.addMapping(argument1, argument2);
                    //the simple names of the argument with identical labels
                    List<Tree> argumentNames2 = TreeUtilFunctions.findChildrenByTypeRecursively(argument2, LANG2.SIMPLE_NAME);
                    for(Tree argumentName1 : TreeUtilFunctions.findChildrenByTypeRecursively(argument1, LANG1.SIMPLE_NAME)) {
                        for(Tree argumentName2 : argumentNames2) {
                            if(argumentName1.getLabel().equals(argumentName2.getLabel()) && !mappingStore.isDstMapped(argumentName2)) {
                                mappingStore.addMapping(argumentName1, argumentName2);
                                break;
                            }
                        }
                    }
                }
                builderNodes1.add(build1);
                builderNodes1.addAll(chain1);
                builderNodes1.add(creation1);
                builderNodes1.addAll(TreeUtilFunctions.findChildrenByTypeRecursively(build1, LANG1.SIMPLE_NAME, LANG1.METHOD_INVOCATION, LANG1.CLASS_INSTANCE_CREATION));
                builderNodes2.add(call2);
                builderNodes2.addAll(TreeUtilFunctions.findChildrenByTypeRecursively(call2, LANG2.SIMPLE_NAME, LANG2.METHOD_INVOCATION));
                break;
            }
        }
    }

    //Kotlin value_argument -> [simple_identifier, =, expression]
    private static boolean isNamedArgument(Tree valueArgument2, Constants LANG2) {
        return valueArgument2.getType().name.equals(LANG2.VALUE_ARGUMENT) && valueArgument2.getChildren().size() == 3 &&
                valueArgument2.getChild(0).getType().name.equals(LANG2.SIMPLE_NAME) &&
                valueArgument2.getChild(1).getType().name.equals(LANG2.AFFECTATION_OPERATOR);
    }

    private static void removeFromParent(List<Tree> children, List<Tree> anonymousList, String astType) {
        for(Tree anonymous : anonymousList) {
            List<Tree> anonymousChildren = TreeUtilFunctions.findChildrenByTypeRecursively(anonymous, astType);
            children.removeAll(anonymousChildren);
        }
    }

    //the Java ReturnStatement -> expression is already flattened, when aligned with the Kotlin function_body -> [=, expression] of a previous mapping,
    //so the ReturnStatement corresponds to the Kotlin expression, i.e., return originalRequest.url().host(); -> = originalRequest.url().host()
    private static boolean isFlattenedReturnExpression(Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        if(!srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) || srcStatementNode.getChildren().isEmpty())
            return false;
        if(dstStatementNode.getParent() == null || !dstStatementNode.getParent().getType().name.equals(LANG2.FUNCTION_BODY) ||
                TreeUtilFunctions.findChildByType(dstStatementNode.getParent(), LANG2.AFFECTATION_OPERATOR) == null)
            return false;
        Tree expression1 = srcStatementNode.getChild(0);
        boolean invocation1 = expression1.getType().name.equals(LANG1.METHOD_INVOCATION) || expression1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION);
        return srcStatementNode.getChildren().size() > 1 || !invocation1;
    }

    //the receivers of the Java invocations, which are called without receiver in Kotlin, i.e., the class name of a static method moved to a companion object
    private static List<Tree> droppedReceivers(List<Tree> inv1, List<Tree> inv2, Constants LANG1, Constants LANG2) {
        List<Tree> receivers1 = new ArrayList<>();
        for(Tree invocation1 : inv1) {
            Tree receiver1 = TreeUtilFunctions.findChildByType(invocation1, LANG1.METHOD_INVOCATION_RECEIVER);
            Tree name1 = TreeUtilFunctions.findChildByType(invocation1, LANG1.SIMPLE_NAME);
            if(receiver1 == null || name1 == null)
                continue;
            boolean callWithoutReceiver2 = false;
            boolean callWithReceiver2 = false;
            for(Tree invocation2 : inv2) {
                Tree name2 = TreeUtilFunctions.findChildByType(invocation2, LANG2.SIMPLE_NAME);
                Tree navigation2 = TreeUtilFunctions.findChildByType(invocation2, LANG2.NAVIGATION_EXPRESSION);
                if(navigation2 == null && name2 != null && name2.getLabel().equals(name1.getLabel())) {
                    callWithoutReceiver2 = true;
                }
                else if(navigation2 != null && navigation2.getChildren().size() > 0) {
                    Tree last2 = navigation2.getChild(navigation2.getChildren().size() - 1);
                    if(last2.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && last2.getChildren().size() > 0 && last2.getChild(0).getLabel().equals(name1.getLabel())) {
                        callWithReceiver2 = true;
                    }
                }
            }
            //skip if the Kotlin statement has also a call with receiver to the same method
            if(callWithoutReceiver2 && !callWithReceiver2) {
                receivers1.add(receiver1);
            }
        }
        return receivers1;
    }

    //align Kotlin if_expression -> [condition, body, control_structure_body -> if_expression] with Java IfStatement -> [condition, Block, IfStatement] of an else-if
    //returns the nested if_expression, after flattening the control_structure_body, which has no Java counterpart (or deferring the flattening)
    public static Tree handleElseIfMapping(ExtendedMultiMappingStore mappingStore, Tree dstStatementNode, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(dstStatementNode.getType().name.equals(LANG2.CONTROL_STRUCTURE_BODY) && dstStatementNode.getChildren().size() == 1 &&
                dstStatementNode.getChild(0).getType().name.equals(LANG2.IF_STATEMENT) &&
                dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.IF_STATEMENT)) {
            Tree elseIf2 = dstStatementNode.getChild(0);
            flattenDstChild(mappingStore, deferredFlattenings, dstStatementNode.getParent(), dstStatementNode);
            return elseIf2;
        }
        return dstStatementNode;
    }

    //the Java assert statement is replaced with a call to the Kotlin assert function, i.e., assert (x); -> assert(x)
    private static boolean isAssertCall(Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        if(!srcStatementNode.getType().name.equals(LANG1.ASSERT_STATEMENT) || !dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION))
            return false;
        Tree name2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.SIMPLE_NAME);
        if(name2 != null)
            return name2.getLabel().equals("assert");
        //the assert name is already removed by a previous mapping of the same statement, so the call has no callee
        return TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.NAVIGATION_EXPRESSION) == null;
    }

    //align Kotlin call_expression -> [assert, call_suffix -> value_arguments -> value_argument -> expression] with Java AssertStatement -> [ParenthesizedExpression ->] expression
    private static void alignAssertStatement(ExtendedMultiMappingStore mappingStore, Tree assert1, Tree call2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        Tree name2 = TreeUtilFunctions.findChildByType(call2, LANG2.SIMPLE_NAME);
        if(name2 != null && name2.getLabel().equals("assert")) {
            //the Java statement includes the assert keyword
            removeDstChild(mappingStore, deferredFlattenings, call2, name2);
        }
        Tree valueArguments2 = alignCallSuffix(mappingStore, assert1, call2, true, LANG2, deferredFlattenings);
        if(valueArguments2 == null) {
            //the call_suffix is already flattened by a previous mapping of the same statement
            valueArguments2 = TreeUtilFunctions.findChildByType(call2, LANG2.METHOD_INVOCATION_ARGUMENTS);
        }
        if(valueArguments2 == null || assert1.getChildren().isEmpty())
            return;
        Tree expression1 = assert1.getChild(0);
        if(expression1.getType().name.equals(LANG1.PARENTHESIZED_EXPRESSION)) {
            //the Java parentheses correspond to the Kotlin argument list parentheses
            mappingStore.addMapping(expression1, valueArguments2);
        }
        else {
            //Java side has no node for the argument list
            flattenDstChildKeepingMappings(deferredFlattenings, call2, valueArguments2);
        }
    }

    private static void processPair(ExtendedMultiMappingStore mappingStore, Tree child1, Tree child2, Constants LANG1, Constants LANG2, List<Tree> invocationsToBeRemoved, DeferredFlattenings deferredFlattenings) {
        mappingStore.addMapping(child1, child2);
        boolean isFirstChildType = child1.getChildren().size() > 0 && child1.getChild(0).getType().name.equals(LANG1.SIMPLE_TYPE);
        Tree name1 = TreeUtilFunctions.findChildByType(child1, LANG1.SIMPLE_NAME);
        Tree name2 = TreeUtilFunctions.findChildByType(child2, LANG2.SIMPLE_NAME);
        //soft keywords, like set, are wrapped in the simple_identifier, as in the matching of simple names by position
        if(name2 != null && name2.getLabel().isEmpty() && name2.getChildren().size() == 1 && name2.getChild(0).isLeaf()) {
            name2 = name2.getChild(0);
        }
        if(!isFirstChildType && name1 != null && name2 != null) {
            mappingStore.addMapping(name1, name2);
        }
        Tree args1 = TreeUtilFunctions.findChildByType(child1, LANG1.METHOD_INVOCATION_ARGUMENTS);
        if(args1 != null) {
            Tree args2 = TreeUtilFunctions.findChildByType(child2, LANG2.CALL_SUFFIX);
            if(args2 != null) {
                args2 = TreeUtilFunctions.findChildByType(args2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                mappingStore.addMapping(args1, args2);
                invocationsToBeRemoved.add(child1);
            }
        }
        else {
            //Java side has a method invocation without arguments
            Tree args2 = TreeUtilFunctions.findChildByType(child2, LANG2.CALL_SUFFIX);
            if(args2 != null && args2.getChildren().size() > 0) {
                Tree valueArguments = TreeUtilFunctions.findChildByType(args2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                if(valueArguments != null) {
                    mappingStore.addMapping(child1, valueArguments);
                    invocationsToBeRemoved.add(child1);
                }
            }
        }
        Tree receiver1 = TreeUtilFunctions.findChildByType(child1, LANG1.METHOD_INVOCATION_RECEIVER);
        if(receiver1 != null) {
            Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
            if(receiver2 != null) {
                receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                mappingStore.addMapping(receiver1, receiver2);
                invocationsToBeRemoved.add(child1);
            }
        }
        if(child1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
            Tree type = TreeUtilFunctions.findChildByType(child1, LANG1.SIMPLE_TYPE);
            if(type != null && type.getChildren().size() > 0) {
                if(type.getChild(0).getType().name.equals(LANG1.QUALIFIED_NAME)) {
                    Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
                    if(receiver2 != null) {
                        receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                        mappingStore.addMapping(type.getChild(0), receiver2);
                        invocationsToBeRemoved.add(child1);
                    }
                }
                else if(type.getChild(0).getType().name.equals(LANG1.SIMPLE_NAME)) {
                    Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
                    if(receiver2 != null) {
                        receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                        mappingStore.addMapping(type.getChild(0), receiver2);
                        invocationsToBeRemoved.add(child1);
                    }
                }
            }
        }
        if(child1.getType().name.equals(LANG1.METHOD_INVOCATION) && child2.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            alignMethodInvocation(mappingStore, child1, child2, LANG1, LANG2, deferredFlattenings);
        }
        else if(child1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) && child2.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            alignClassInstanceCreation(mappingStore, child1, child2, LANG1, LANG2, deferredFlattenings);
        }
    }

    private static void alignMethodInvocation(ExtendedMultiMappingStore mappingStore, Tree invocation1, Tree invocation2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        Tree args1 = TreeUtilFunctions.findChildByType(invocation1, LANG1.METHOD_INVOCATION_ARGUMENTS);
        //align call_expression -> call_suffix -> value_arguments -> value_argument -> expression with Java MethodInvocation -> METHOD_INVOCATION_ARGUMENTS -> expression
        alignCallSuffix(mappingStore, invocation1, invocation2, args1 != null, LANG2, deferredFlattenings);
        //align call_expression -> navigation_expression -> [receiver, navigation_suffix -> name] with Java MethodInvocation -> [METHOD_INVOCATION_RECEIVER -> receiver, name]
        Tree receiver1 = TreeUtilFunctions.findChildByType(invocation1, LANG1.METHOD_INVOCATION_RECEIVER);
        Tree navigation2 = invocation2.getChildren().size() > 0 ? invocation2.getChild(0) : null;
        if(receiver1 != null && navigation2 != null && navigation2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) && navigation2.getChildren().size() > 1) {
            Tree suffix2 = navigation2.getChild(navigation2.getChildren().size() - 1);
            if(suffix2.getType().name.equals(LANG2.NAVIGATION_SUFFIX)) {
                mappingStore.removeMapping(receiver1, suffix2);
                navigation2.getChildren().remove(suffix2);
                invocation2.getChildren().addAll(1, suffix2.getChildren());
                for(Tree t : suffix2.getChildren())
                    t.setParent(invocation2);
                mappingStore.addMapping(receiver1, navigation2);
            }
        }
        //the navigation_suffix is already moved to call_expression by another Java statement mapped to the same Kotlin statement
        else if(receiver1 != null && navigation2 != null && navigation2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) && navigation2.getChildren().size() == 1) {
            mappingStore.addMapping(receiver1, navigation2);
        }
        //names that are Kotlin soft keywords (e.g., set, data) are nested in an empty simple_identifier
        for(Tree child2 : new ArrayList<>(invocation2.getChildren())) {
            if(child2.getType().name.equals(LANG2.SIMPLE_NAME) && child2.getLabel().isEmpty() && child2.getChildren().size() == 1 && child2.getChild(0).isLeaf() &&
                    !mappingStore.isDstMapped(child2)) {
                flattenChild(invocation2, child2);
            }
        }
    }

    private static void alignClassInstanceCreation(ExtendedMultiMappingStore mappingStore, Tree creation1, Tree creation2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(TreeUtilFunctions.findChildByType(creation1, LANG1.ANONYMOUS_CLASS_DECLARATION) != null)
            return;
        Tree type1 = creation1.getChildren().size() > 0 ? creation1.getChild(0) : null;
        boolean arguments1 = creation1.getChildren().size() > 1;
        //align call_expression -> call_suffix -> value_arguments -> value_argument -> expression with Java ClassInstanceCreation -> expression
        Tree valueArguments2 = alignCallSuffix(mappingStore, creation1, creation2, arguments1, LANG2, deferredFlattenings);
        if(valueArguments2 != null) {
            //Java side has no node for the argument list
            mappingStore.removeMapping(creation1, valueArguments2);
            flattenDstChildKeepingMappings(deferredFlattenings, creation2, valueArguments2);
        }
        //align call_expression -> name with Java ClassInstanceCreation -> [ParameterizedType ->] SimpleType -> SimpleName
        Tree parameterizedType1 = null;
        if(type1 != null && type1.getType().name.equals(LANG1.PARAMETERIZED_TYPE) && type1.getChildren().size() > 0) {
            parameterizedType1 = type1;
            type1 = type1.getChild(0);
        }
        if(type1 != null && type1.getType().name.equals(LANG1.SIMPLE_TYPE) && type1.getChildren().size() == 1) {
            Tree name1 = type1.getChild(0);
            Tree name2 = creation2.getChildren().size() > 0 ? creation2.getChild(0) : null;
            Set<Tree> nameDsts = mappingStore.getDsts(name1);
            if(name2 != null && nameDsts != null && nameDsts.contains(name2) && !mappingStore.isSrcMapped(type1) &&
                    (parameterizedType1 == null || !mappingStore.isSrcMapped(parameterizedType1))) {
                if(parameterizedType1 != null) {
                    flattenSrcChildKeepingMappings(deferredFlattenings, creation1, parameterizedType1);
                }
                flattenSrcChildKeepingMappings(deferredFlattenings, creation1, type1);
            }
        }
    }

    private static Tree alignCallSuffix(ExtendedMultiMappingStore mappingStore, Tree call1, Tree call2, boolean arguments1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        //returns the value_arguments of call2, after flattening call_suffix and value_argument nodes (or deferring their flattening)
        Tree callSuffix2 = TreeUtilFunctions.findChildByType(call2, LANG2.CALL_SUFFIX);
        if(callSuffix2 == null)
            return null;
        Tree valueArguments2 = TreeUtilFunctions.findChildByType(callSuffix2, LANG2.METHOD_INVOCATION_ARGUMENTS);
        if(valueArguments2 != null) {
            if(!arguments1 && valueArguments2.getChildren().isEmpty()) {
                //Java side has no node for an empty argument list
                mappingStore.removeMapping(call1, valueArguments2);
                removeDstChild(mappingStore, deferredFlattenings, callSuffix2, valueArguments2);
                valueArguments2 = null;
            }
            else {
                for(Tree valueArgument2 : new ArrayList<>(valueArguments2.getChildren())) {
                    //named arguments have more than one child
                    if(valueArgument2.getType().name.equals(LANG2.VALUE_ARGUMENT) && valueArgument2.getChildren().size() == 1) {
                        flattenDstChildKeepingMappings(deferredFlattenings, valueArguments2, valueArgument2);
                    }
                }
            }
        }
        flattenDstChildKeepingMappings(deferredFlattenings, call2, callSuffix2);
        return valueArguments2;
    }

    //maps the name, type and annotations of the Java and Kotlin field declarations, without restructuring the trees (see alignFieldDeclaration)
    public static void handleFieldDeclarationMapping(ExtendedMultiMappingStore mappingStore,
            Tree srcAttr, Tree dstAttr, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2) {
        handleFieldDeclarationMapping(mappingStore, srcAttr, dstAttr, srcFieldDeclaration, dstFieldDeclaration, LANG1, LANG2, null);
    }

    public static void handleFieldDeclarationMapping(ExtendedMultiMappingStore mappingStore,
            Tree srcAttr, Tree dstAttr, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        Tree variableDeclaration2 = TreeUtilFunctions.findChildByType(dstAttr, LANG2.VARIABLE_DECLARATION);
        if(variableDeclaration2 == null && dstFieldDeclaration != null)
            variableDeclaration2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.VARIABLE_DECLARATION);
        if(variableDeclaration2 != null) {
            Tree name1 = TreeUtilFunctions.findChildByType(srcAttr, LANG1.SIMPLE_NAME);
            Tree name2 = TreeUtilFunctions.findChildByType(variableDeclaration2, LANG2.SIMPLE_NAME);
            if(name1 != null && name2 != null) {
                mappingStore.addMapping(name1, name2);
            }
            alignTypes(mappingStore, findJavaType(srcFieldDeclaration, LANG1), findKotlinType(variableDeclaration2, LANG2), LANG1, LANG2, deferredFlattenings);
            Tree annotation1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.MARKER_ANNOTATION);
            Tree modifiers2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.MODIFIERS);
            handleAnnotationMapping(mappingStore, annotation1, modifiers2, LANG1, LANG2);
        }
        //the location of a documented class parameter starts at its KDoc, so the class parameter is found only as the field declaration
        Tree classParameter2 = dstAttr.getType().name.equals(LANG2.CLASS_PARAMETER) ? dstAttr :
                dstFieldDeclaration != null && dstFieldDeclaration.getType().name.equals(LANG2.CLASS_PARAMETER) ? dstFieldDeclaration : null;
        if(classParameter2 != null) {
            Set<Tree> fieldDeclarationDsts = mappingStore.getDsts(srcFieldDeclaration);
            if(fieldDeclarationDsts == null || !fieldDeclarationDsts.contains(classParameter2))
                mappingStore.addMapping(srcAttr, classParameter2);
            Tree name1 = TreeUtilFunctions.findChildByType(srcAttr, LANG1.SIMPLE_NAME);
            Tree name2 = TreeUtilFunctions.findChildByType(classParameter2, LANG2.SIMPLE_NAME);
            if(name1 != null && name2 != null) {
                mappingStore.addMapping(name1, name2);
            }
            alignTypes(mappingStore, findJavaType(srcFieldDeclaration, LANG1), findKotlinType(classParameter2, LANG2), LANG1, LANG2, deferredFlattenings);
            Tree annotation1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.MARKER_ANNOTATION);
            Tree modifiers2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.MODIFIERS);
            handleAnnotationMapping(mappingStore, annotation1, modifiers2, LANG1, LANG2);
        }
    }

    public static void handleAnnotationMapping(ExtendedMultiMappingStore mappingStore, Tree srcClassAnnotationTree, Tree dstClassAnnotationTree, Constants LANG1, Constants LANG2) {
        if(srcClassAnnotationTree != null && dstClassAnnotationTree != null) {
            Tree typeName1 = TreeUtilFunctions.findChildByType(srcClassAnnotationTree, LANG1.SIMPLE_NAME);
            Tree classModifiers2 = findKotlinAnnotation(dstClassAnnotationTree, typeName1, LANG2);
            if(classModifiers2 == null) {
                if(!dstClassAnnotationTree.getType().name.equals(LANG2.MODIFIERS))
                    mappingStore.addMapping(srcClassAnnotationTree, dstClassAnnotationTree);
                return;
            }
            //map the Java annotation only to the Kotlin annotation node to avoid multi-mappings reported as moves
            mappingStore.addMapping(srcClassAnnotationTree, classModifiers2);
            List<Tree> stringLiteral1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcClassAnnotationTree, LANG1.STRING_LITERAL);
            List<Tree> numberLiteral1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcClassAnnotationTree, LANG1.NUMBER_LITERAL);
            List<Tree> stringLiteral2 = null;
            List<Tree> numberLiteral2 = null;
            Tree userType2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.USER_TYPE);
            Tree typeIdentifier2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.TYPE_IDENTIFIER);
            Tree at = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.AT);
            //align Kotlin class_modifier -> at, user_type -> type_identifier with Java MarkerAnnotation -> SimpleName
            if(at != null) {
                removeDstMappings(mappingStore, at);
                classModifiers2.getChildren().remove(at);
            }
            if(userType2 == null && typeIdentifier2 == null) {
                Tree constuctorInvocation2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.CONSTRUCTOR_INVOCATION);
                if(constuctorInvocation2 != null) {
                    stringLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.STRING_LITERAL);
                    numberLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.INTEGER_LITERAL);
                    userType2 = TreeUtilFunctions.findChildByType(constuctorInvocation2, LANG2.USER_TYPE);
                    Tree valueArguments2 = TreeUtilFunctions.findChildByType(constuctorInvocation2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                    //align Kotlin class_modifier -> constructor_invocation -> user_type, value_arguments -> value_argument -> expression
                    //with Java SingleMemberAnnotation -> SimpleName, Expression
                    if(srcClassAnnotationTree.getType().name.equals(LANG1.SINGLE_MEMBER_ANNOTATION) && userType2 != null && valueArguments2 != null &&
                            constuctorInvocation2.getChildren().size() == 2 && valueArguments2.getChildren().size() == 1) {
                        Tree valueArgument2 = valueArguments2.getChild(0);
                        if(valueArgument2.getType().name.equals(LANG2.VALUE_ARGUMENT) && valueArgument2.getChildren().size() == 1) {
                            Tree expression2 = valueArgument2.getChild(0);
                            removeDstMappings(mappingStore, constuctorInvocation2);
                            removeDstMappings(mappingStore, valueArguments2);
                            removeDstMappings(mappingStore, valueArgument2);
                            int index = classModifiers2.getChildPosition(constuctorInvocation2);
                            classModifiers2.getChildren().set(index, userType2);
                            userType2.setParent(classModifiers2);
                            classModifiers2.insertChild(expression2, index + 1);
                        }
                    }
                }
            }
            if(userType2 != null && userType2.getParent() == classModifiers2 && userType2.getChildren().size() == 1 && userType2.getChild(0).isLeaf()) {
                typeIdentifier2 = userType2.getChild(0);
                removeDstMappings(mappingStore, userType2);
                int index = classModifiers2.getChildPosition(userType2);
                classModifiers2.getChildren().set(index, typeIdentifier2);
                typeIdentifier2.setParent(classModifiers2);
            }
            else if(userType2 != null && userType2.getChildren().size() > 0) {
                typeIdentifier2 = userType2.getChild(0);
            }
            if(typeName1 != null && typeIdentifier2 != null) {
                mappingStore.addMapping(typeName1, typeIdentifier2);
            }
            if(stringLiteral2 != null && stringLiteral1.size() == stringLiteral2.size()) {
                for(int i=0; i<stringLiteral1.size(); i++) {
                    if(stringLiteral2.get(i).getChildren().size() > 0) {
                        stringLiteral2.get(i).setLabel(stringLiteral1.get(i).getLabel());
                        stringLiteral2.get(i).getChildren().remove(0);
                        mappingStore.addMapping(stringLiteral1.get(i), stringLiteral2.get(i));
                    }
                    else {
                        mappingStore.addMapping(stringLiteral1.get(i), stringLiteral2.get(i));
                    }
                }
            }
            if(numberLiteral2 != null && numberLiteral1.size() == numberLiteral2.size()) {
                for(int i=0; i<numberLiteral1.size(); i++) {
                    mappingStore.addMapping(numberLiteral1.get(i), numberLiteral2.get(i));
                }
            }
            //align Kotlin declaration -> modifiers -> class_modifier with Java declaration -> Annotation
            Tree modifiers2 = classModifiers2.getParent();
            Tree parent1 = srcClassAnnotationTree.getParent();
            if(modifiers2 != null && modifiers2.getType().name.equals(LANG2.MODIFIERS) && modifiers2.getParent() != null &&
                    parent1 != null && !parent1.getType().name.equals(LANG1.MODIFIERS)) {
                Tree declaration2 = modifiers2.getParent();
                modifiers2.getChildren().remove(classModifiers2);
                if(modifiers2.getChildren().isEmpty()) {
                    removeDstMappings(mappingStore, modifiers2);
                    declaration2.getChildren().remove(modifiers2);
                }
                else {
                    updateRangeToChildren(modifiers2);
                }
                //keep the source code order of the annotations and modifiers
                int insertionIndex = 0;
                while(insertionIndex < declaration2.getChildren().size() && declaration2.getChild(insertionIndex).getPos() < classModifiers2.getPos())
                    insertionIndex++;
                declaration2.insertChild(classModifiers2, insertionIndex);
                classModifiers2.setParent(declaration2);
            }
        }
    }

    private static Tree findKotlinAnnotation(Tree dstClassAnnotationTree, Tree typeName1, Constants LANG2) {
        if(dstClassAnnotationTree.getType().name.equals(LANG2.CLASS_MODIFIER))
            return dstClassAnnotationTree;
        if(dstClassAnnotationTree.getType().name.equals(LANG2.MODIFIERS)) {
            List<Tree> candidates = new ArrayList<>(TreeUtilFunctions.findChildrenByType(dstClassAnnotationTree, LANG2.CLASS_MODIFIER));
            //the annotation might have been already moved from the modifiers to the declaration
            if(dstClassAnnotationTree.getParent() != null)
                candidates.addAll(TreeUtilFunctions.findChildrenByType(dstClassAnnotationTree.getParent(), LANG2.CLASS_MODIFIER));
            for(Tree candidate : candidates) {
                List<Tree> typeIdentifiers = TreeUtilFunctions.findChildrenByTypeRecursively(candidate, LANG2.TYPE_IDENTIFIER);
                if(typeName1 != null && typeIdentifiers.size() > 0 && typeIdentifiers.get(0).getLabel().equals(typeName1.getLabel()))
                    return candidate;
            }
            if(candidates.size() > 0 && TreeUtilFunctions.findChildByType(candidates.get(0), LANG2.AT) != null)
                return candidates.get(0);
        }
        return null;
    }

    //align Java Javadoc -> TagElement -> TextElement with Kotlin multiline_comment by mirroring the Java structure inside the textually identical Kotlin comment
    public static boolean handleJavadocMapping(ExtendedMultiMappingStore mappingStore, Tree srcJavadoc, Tree dstComment, Constants LANG1, Constants LANG2) {
        if(!srcJavadoc.getType().name.equals(LANG1.JAVA_DOC) || !dstComment.getType().name.equals(LANG2.BLOCK_COMMENT))
            return false;
        List<Pair<Tree, Tree>> pairs = new ArrayList<>();
        if(dstComment.isLeaf()) {
            //the label is the source code of the comment, as restored by KotlinTreeSitterTreeFixer, so the offsets are computed from the start of the comment
            String text = dstComment.getLabel();
            int offset = dstComment.getPos();
            if(!text.startsWith("/*") || !text.endsWith("*/"))
                return false;
            int[] cursor = {text.startsWith("/**") ? 3 : 2};
            List<Tree> children = new ArrayList<>();
            for(Tree srcChild : srcJavadoc.getChildren()) {
                int[] position = {cursor[0]};
                Tree dstChild = mirrorJavadocElement(srcChild, text, offset, position, false, LANG1);
                if(dstChild == null) {
                    //the Kotlin comment might contain added documentation before the element, so the element is searched in the following lines
                    int lineStart = findJavadocElementLineStart(srcChild, text, offset, cursor[0], LANG1);
                    if(lineStart < 0)
                        return false;
                    Tree added = createAddedJavadocElement(text, offset, cursor[0], lineStart, LANG1);
                    if(added != null)
                        children.add(added);
                    position[0] = lineStart;
                    dstChild = mirrorJavadocElement(srcChild, text, offset, position, false, LANG1);
                }
                cursor[0] = position[0];
                pairs.add(new Pair<>(srcChild, dstChild));
                children.add(dstChild);
            }
            //the Kotlin comment might contain added documentation after the last element
            if(!isJavadocFormatting(text, cursor[0], text.length() - 2, false)) {
                Tree added = createAddedJavadocElement(text, offset, cursor[0], text.length() - 2, LANG1);
                if(added == null)
                    return false;
                children.add(added);
            }
            dstComment.setLabel(srcJavadoc.getLabel());
            for(Tree child : children)
                dstComment.addChild(child);
        }
        else if(mappingStore.getDsts(srcJavadoc) != null && mappingStore.getDsts(srcJavadoc).contains(dstComment)) {
            return true;
        }
        else {
            //the cached tree metrics of the Kotlin comment are outdated, so isIsoStructuralTo() cannot be used
            int j = 0;
            for(Tree srcChild : srcJavadoc.getChildren()) {
                while(j < dstComment.getChildren().size() && !haveSameStructure(srcChild, dstComment.getChild(j)))
                    j++;
                if(j == dstComment.getChildren().size())
                    return false;
                pairs.add(new Pair<>(srcChild, dstComment.getChild(j++)));
            }
        }
        if(!srcJavadoc.getLabel().equals(dstComment.getLabel()))
            return false;
        mappingStore.addMapping(srcJavadoc, dstComment);
        for(Pair<Tree, Tree> pair : pairs)
            mapSameStructure(mappingStore, pair.first, pair.second);
        //align Kotlin declaration's parent -> multiline_comment, declaration with Java declaration -> Javadoc
        Tree parent1 = srcJavadoc.getParent();
        Tree parent2 = dstComment.getParent();
        if(parent1 != null && parent2 != null) {
            Tree declaration2 = null;
            Set<Tree> dsts = mappingStore.getDsts(parent1);
            if(dsts != null) {
                for(Tree dst : dsts) {
                    if(dst.getPos() >= dstComment.getEndPos() && dst != parent2) {
                        declaration2 = dst;
                        break;
                    }
                }
            }
            else {
                int index = parent2.getChildPosition(dstComment);
                if(index + 1 < parent2.getChildren().size()) {
                    Tree nextSibling = parent2.getChild(index + 1);
                    if(!nextSibling.getType().name.equals(LANG2.BLOCK_COMMENT) && !nextSibling.getType().name.equals(LANG2.LINE_COMMENT))
                        declaration2 = nextSibling;
                }
            }
            if(declaration2 != null) {
                parent2.getChildren().remove(dstComment);
                declaration2.insertChild(dstComment, 0);
                dstComment.setParent(declaration2);
            }
        }
        return true;
    }

    //returns the start of the line after the start position, from which the Java element can be mirrored in the Kotlin comment
    private static int findJavadocElementLineStart(Tree srcElement, String text, int offset, int start, Constants LANG1) {
        Tree firstLeaf = null;
        for(Tree t : srcElement.preOrder()) {
            if(t.isLeaf() && !t.getLabel().isBlank()) {
                firstLeaf = t;
                break;
            }
        }
        if(firstLeaf == null)
            return -1;
        Pattern pattern = firstLeaf.getType().name.equals(LANG1.TEXT_ELEMENT) ? javadocTextPattern(firstLeaf.getLabel()) : Pattern.compile(Pattern.quote(firstLeaf.getLabel()));
        Matcher matcher = pattern.matcher(text);
        int from = start;
        while(from < text.length() && matcher.find(from)) {
            int lineStart = text.lastIndexOf('\n', matcher.start()) + 1;
            if(lineStart > start) {
                int[] position = {lineStart};
                if(mirrorJavadocElement(srcElement, text, offset, position, false, LANG1) != null)
                    return lineStart;
            }
            from = matcher.start() + 1;
        }
        return -1;
    }

    //creates a TagElement with a TextElement for each line of the documentation added in the Kotlin comment between start and end
    private static Tree createAddedJavadocElement(String text, int offset, int start, int end, Constants LANG1) {
        DefaultTree tagElement = new DefaultTree(TypeSet.type(LANG1.TAG_ELEMENT), "");
        int lineStart = start;
        while(lineStart < end) {
            int lineEnd = text.indexOf('\n', lineStart);
            if(lineEnd < 0 || lineEnd > end)
                lineEnd = end;
            int textStart = lineStart;
            while(textStart < lineEnd && isJavadocFormatting(text, textStart, textStart + 1, false))
                textStart++;
            int textEnd = lineEnd;
            while(textEnd > textStart && Character.isWhitespace(text.charAt(textEnd - 1)))
                textEnd--;
            if(textStart < textEnd) {
                DefaultTree textElement = new DefaultTree(TypeSet.type(LANG1.TEXT_ELEMENT), text.substring(textStart, textEnd));
                textElement.setPos(offset + textStart);
                textElement.setLength(textEnd - textStart);
                tagElement.addChild(textElement);
            }
            lineStart = lineEnd + 1;
        }
        if(tagElement.getChildren().isEmpty())
            return null;
        setRangeToChildren(tagElement);
        return tagElement;
    }

    private static boolean haveSameStructure(Tree tree1, Tree tree2) {
        //labels are not compared, because Java texts with HTML markup are replaced with Kotlin texts with Markdown
        if(!tree1.getType().name.equals(tree2.getType().name) || tree1.getChildren().size() != tree2.getChildren().size())
            return false;
        for(int i=0; i<tree1.getChildren().size(); i++) {
            if(!haveSameStructure(tree1.getChild(i), tree2.getChild(i)))
                return false;
        }
        return true;
    }

    private static void mapSameStructure(ExtendedMultiMappingStore mappingStore, Tree tree1, Tree tree2) {
        mappingStore.addMapping(tree1, tree2);
        for(int i=0; i<tree1.getChildren().size(); i++) {
            mapSameStructure(mappingStore, tree1.getChild(i), tree2.getChild(i));
        }
    }

    private static Tree mirrorJavadocElement(Tree srcElement, String text, int offset, int[] cursor, boolean reference, Constants LANG1) {
        DefaultTree dstElement = new DefaultTree(srcElement.getType(), srcElement.getLabel());
        String type = srcElement.getType().name;
        if(srcElement.isLeaf()) {
            String label = srcElement.getLabel();
            int start = cursor[0];
            int end = cursor[0];
            if(!label.isBlank()) {
                //the text might differ in whitespace, line breaks, and HTML markup, i.e., "<p>Writes are" and "Writes are"
                Matcher matcher = javadocTextPattern(label).matcher(text);
                if(!matcher.find(cursor[0]))
                    return null;
                start = matcher.start();
                end = matcher.end();
            }
            if(!isJavadocFormatting(text, cursor[0], start, reference))
                return null;
            cursor[0] = end;
            dstElement.setPos(offset + start);
            dstElement.setLength(end - start);
            String kotlinText = normalizeJavadocText(text.substring(start, end));
            if(!kotlinText.equals(normalizeJavadocText(label)))
                dstElement.setLabel(kotlinText);
        }
        else if(type.equals(LANG1.TAG_ELEMENT) && srcElement.getParent() != null && srcElement.getParent().getType().name.equals(LANG1.TAG_ELEMENT) &&
                srcElement.getChild(0).getType().name.equals(LANG1.TAG_NAME)) {
            return mirrorJavadocInlineTag(srcElement, text, offset, cursor, LANG1);
        }
        else {
            boolean isReference = reference || type.equals(LANG1.METHOD_REF) || type.equals(LANG1.MEMBER_REF) || type.equals(LANG1.QUALIFIED_NAME);
            for(Tree srcChild : srcElement.getChildren()) {
                Tree dstChild = mirrorJavadocElement(srcChild, text, offset, cursor, isReference, LANG1);
                if(dstChild == null)
                    return null;
                dstElement.addChild(dstChild);
            }
            setRangeToChildren(dstElement);
        }
        return dstElement;
    }

    //align Java inline tags {@code text}, {@link ref}, {@link ref label} with Kotlin markup `text`, [ref], [label][ref], or the unchanged Java inline tag
    private static Tree mirrorJavadocInlineTag(Tree srcElement, String text, int offset, int[] cursor, Constants LANG1) {
        int start = cursor[0];
        while(start < text.length() && isJavadocFormatting(text, start, start + 1, false))
            start++;
        if(start >= text.length())
            return null;
        char opening = text.charAt(start);
        if(opening != '{' && opening != '`' && opening != '[')
            return null;
        DefaultTree dstElement = new DefaultTree(srcElement.getType(), srcElement.getLabel());
        List<Tree> srcChildren = srcElement.getChildren();
        List<Tree> dstChildren = new ArrayList<>();
        int[] position = {start + 1};
        if(opening == '{') {
            for(Tree srcChild : srcChildren) {
                Tree dstChild = mirrorJavadocElement(srcChild, text, offset, position, false, LANG1);
                if(dstChild == null)
                    return null;
                dstChildren.add(dstChild);
            }
            if(!consume(text, position, "}"))
                return null;
        }
        else {
            //the markup replaces the Java tag name, so it gets the same label to avoid an update, i.e., @code for `
            Tree srcTagName = srcChildren.get(0);
            DefaultTree dstTagName = new DefaultTree(srcTagName.getType(), srcTagName.getLabel());
            dstTagName.setPos(offset + start);
            dstTagName.setLength(1);
            dstChildren.add(dstTagName);
            List<Tree> references = new ArrayList<>();
            List<Tree> labels = new ArrayList<>();
            for(Tree srcChild : srcChildren.subList(1, srcChildren.size())) {
                if(labels.isEmpty() && !srcChild.getType().name.equals(LANG1.TEXT_ELEMENT))
                    references.add(srcChild);
                else
                    labels.add(srcChild);
            }
            List<Tree> mirrored = null;
            if(opening == '[' && !references.isEmpty() && !labels.isEmpty()) {
                //[label][ref]
                int[] labelPosition = {position[0]};
                List<Tree> dstLabels = mirrorJavadocElements(labels, text, offset, labelPosition, false, LANG1);
                if(dstLabels != null && consume(text, labelPosition, "][")) {
                    List<Tree> dstReferences = mirrorJavadocElements(references, text, offset, labelPosition, true, LANG1);
                    if(dstReferences != null && consume(text, labelPosition, "]")) {
                        //keep the order of the Java tree
                        mirrored = new ArrayList<>(dstReferences);
                        mirrored.addAll(dstLabels);
                        position[0] = labelPosition[0];
                    }
                }
            }
            if(mirrored == null) {
                //`text` or [ref]
                mirrored = mirrorJavadocElements(srcChildren.subList(1, srcChildren.size()), text, offset, position, true, LANG1);
                if(mirrored == null || !consume(text, position, opening == '`' ? "`" : "]"))
                    return null;
            }
            dstChildren.addAll(mirrored);
        }
        for(Tree dstChild : dstChildren)
            dstElement.addChild(dstChild);
        cursor[0] = position[0];
        dstElement.setPos(offset + start);
        dstElement.setLength(position[0] - start);
        return dstElement;
    }

    private static List<Tree> mirrorJavadocElements(List<Tree> srcElements, String text, int offset, int[] cursor, boolean reference, Constants LANG1) {
        List<Tree> dstElements = new ArrayList<>();
        for(Tree srcElement : srcElements) {
            Tree dstElement = mirrorJavadocElement(srcElement, text, offset, cursor, reference, LANG1);
            if(dstElement == null)
                return null;
            dstElements.add(dstElement);
        }
        return dstElements;
    }

    //consumes the expected text after optional whitespace
    private static boolean consume(String text, int[] cursor, String expected) {
        int index = cursor[0];
        while(index < text.length() && Character.isWhitespace(text.charAt(index)))
            index++;
        if(!text.startsWith(expected, index))
            return false;
        cursor[0] = index + expected.length();
        return true;
    }

    //matches the words of a Javadoc text separated by any whitespace, including line breaks followed by a leading *,
    //allowing HTML markup to be converted to Markdown, and words to be converted to links, i.e., IOException to [IOException]
    private static Pattern javadocTextPattern(String label) {
        String[] words = label.trim().split("\\s+");
        StringBuilder regex = new StringBuilder();
        for(String word : words) {
            String markdown = word.replaceAll("</?p>", "").replaceAll("</?(strong|b)>", "**").replaceAll("</?(em|i)>", "*").replaceAll("</?code>", "`");
            if(markdown.isEmpty())
                continue;
            if(regex.length() > 0)
                regex.append("(?:\\s*\\n\\s*\\*(?!/))?\\s+");
            String quoted = Pattern.quote(markdown);
            regex.append("(?:\\[").append(quoted).append("\\]|").append(quoted).append(")");
        }
        return Pattern.compile(regex.toString());
    }

    private static String normalizeJavadocText(String text) {
        return text.replaceAll("\\s*\\n\\s*\\*", " ").replaceAll("\\s+", " ").trim();
    }

    //checks if the text between start and end contains only Javadoc formatting characters, or reference separators, i.e., Http2Stream#isOpen() and Http2Stream.isOpen
    private static boolean isJavadocFormatting(String text, int start, int end, boolean reference) {
        for(int i=start; i<end; i++) {
            char c = text.charAt(i);
            if(!Character.isWhitespace(c) && c != '*' && !(reference && (c == '.' || c == '#' || c == '(' || c == ')')))
                return false;
        }
        return true;
    }

    private static void setRangeToChildren(Tree tree) {
        if(!tree.getChildren().isEmpty()) {
            int start = tree.getChild(0).getPos();
            int end = tree.getChild(0).getEndPos();
            for(Tree child : tree.getChildren()) {
                start = Math.min(start, child.getPos());
                end = Math.max(end, child.getEndPos());
            }
            tree.setPos(start);
            tree.setLength(end - start);
        }
    }

    public static void handleImportMapping(ExtendedMultiMappingStore mappingStore, Tree srcImportStatement, Tree dstImportStatement, Constants LANG1, Constants LANG2) {
        mappingStore.addMapping(srcImportStatement, dstImportStatement);
        Tree qualifiedName = TreeUtilFunctions.findChildByType(srcImportStatement, LANG1.QUALIFIED_NAME);
        Tree identifier = TreeUtilFunctions.findChildByType(dstImportStatement, LANG1.IMPORT_IDENTIFIER);
        if(qualifiedName != null && identifier != null) {
            String qualified = "";
            int i = 0;
            for(Tree t : identifier.getChildren()) {
                qualified = qualified + t.getLabel();
                if(i<identifier.getChildren().size()-1) {
                    qualified = qualified + ".";
                }
                i++;
            }
            identifier.setLabel(qualified);
            mappingStore.addMapping(qualifiedName, identifier);
            identifier.getChildren().clear();
        }
        alignImportList(mappingStore, dstImportStatement, LANG2);
    }

    //align Kotlin source_file -> [package_header, import_list -> import_header*, declaration*] with Java CompilationUnit -> [PackageDeclaration, ImportDeclaration*, declaration*]
    private static void alignImportList(ExtendedMultiMappingStore mappingStore, Tree dstImportStatement, Constants LANG2) {
        Tree importList2 = dstImportStatement.getParent();
        if(importList2 == null || !importList2.getType().name.equals(LANG2.IMPORT_LIST) || importList2.getParent() == null)
            return;
        removeDstMappings(mappingStore, importList2);
        flattenChild(importList2.getParent(), importList2);
    }

    public static void handlePackageDeclarationMapping(ExtendedMultiMappingStore mappingStore, Tree srcPackageDeclaration, Tree dstPackageDeclaration, Constants LANG1, Constants LANG2) {
        mappingStore.addMapping(srcPackageDeclaration, dstPackageDeclaration);
        Tree packageName = TreeUtilFunctions.findChildByType(srcPackageDeclaration, LANG1.QUALIFIED_NAME);
        if(packageName == null)
            packageName = TreeUtilFunctions.findChildByType(srcPackageDeclaration, LANG1.SIMPLE_NAME);
        Tree identifier = TreeUtilFunctions.findChildByType(dstPackageDeclaration, LANG2.IMPORT_IDENTIFIER);
        if(packageName != null && identifier != null) {
            String qualified = "";
            int i = 0;
            for(Tree t : identifier.getChildren()) {
                qualified = qualified + t.getLabel();
                if(i<identifier.getChildren().size()-1) {
                    qualified = qualified + ".";
                }
                i++;
            }
            identifier.setLabel(qualified);
            mappingStore.addMapping(packageName, identifier);
            identifier.getChildren().clear();
            Tree packageKeyword = TreeUtilFunctions.findChildByType(dstPackageDeclaration, LANG2.PACKAGE);
            dstPackageDeclaration.getChildren().remove(packageKeyword);
        }
    }

    public static void handleParameterMapping(ExtendedMultiMappingStore mappingStore, Tree leftTree, Tree rightTree, Constants LANG1, Constants LANG2) {
        handleParameterMapping(mappingStore, leftTree, rightTree, LANG1, LANG2, null);
    }

    public static void handleParameterMapping(ExtendedMultiMappingStore mappingStore, Tree leftTree, Tree rightTree, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(leftTree.getType().name.equals(LANG1.SINGLE_VARIABLE_DECLARATION) && rightTree.getType().name.equals(LANG2.PARAMETER))
            mappingStore.addMapping(leftTree, rightTree);
        alignTypes(mappingStore, findJavaType(leftTree, LANG1), findKotlinType(rightTree, LANG2), LANG1, LANG2, deferredFlattenings);
        Tree name1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.SIMPLE_NAME);
        Tree name2 = TreeUtilFunctions.findChildByType(rightTree, LANG2.SIMPLE_NAME);
        if(name1 != null && name2 != null) {
            mappingStore.addMapping(name1, name2);
        }
    }

    public static void handleTypeMapping(ExtendedMultiMappingStore mappingStore, Tree srcNode, Tree dstNode, Constants LANG1, Constants LANG2) {
        handleTypeMapping(mappingStore, srcNode, dstNode, LANG1, LANG2, null);
    }

    public static void handleTypeMapping(ExtendedMultiMappingStore mappingStore, Tree srcNode, Tree dstNode, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        Tree type2 = dstNode.getType().name.equals(LANG2.USER_TYPE) || dstNode.getType().name.equals(LANG2.NULLABLE_TYPE) ? dstNode : TreeUtilFunctions.findChildByType(dstNode, LANG2.USER_TYPE);
        alignTypes(mappingStore, srcNode, type2, LANG1, LANG2, deferredFlattenings);
    }

    private static Tree findJavaType(Tree declaration1, Constants LANG1) {
        Tree type1 = TreeUtilFunctions.findChildByType(declaration1, LANG1.SIMPLE_TYPE);
        if(type1 == null)
            type1 = TreeUtilFunctions.findChildByType(declaration1, LANG1.PRIMITIVE_TYPE);
        if(type1 == null)
            type1 = TreeUtilFunctions.findChildByType(declaration1, LANG1.PARAMETERIZED_TYPE);
        return type1;
    }

    private static Tree findKotlinType(Tree declaration2, Constants LANG2) {
        Tree type2 = TreeUtilFunctions.findChildByType(declaration2, LANG2.USER_TYPE);
        if(type2 == null)
            type2 = TreeUtilFunctions.findChildByType(declaration2, LANG2.NULLABLE_TYPE);
        //the type is nested in the variable_declaration, if its flattening is deferred (see alignVariableDeclaration)
        Tree variableDeclaration2 = TreeUtilFunctions.findChildByType(declaration2, LANG2.VARIABLE_DECLARATION);
        if(type2 == null && variableDeclaration2 != null && declaration2.getType().name.equals(LANG2.FIELD_DECLARATION))
            type2 = findKotlinType(variableDeclaration2, LANG2);
        return type2;
    }

    //align Kotlin user_type with Java SimpleType, PrimitiveType, and ParameterizedType, i.e., int -> Int, Integer -> Int, List<Header> -> List<Header>
    private static void alignTypes(ExtendedMultiMappingStore mappingStore, Tree type1, Tree type2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(type1 == null || type2 == null)
            return;
        //the nullability of the Kotlin type is an addition, i.e., Http2Stream -> Http2Stream?
        if(type2.getType().name.equals(LANG2.NULLABLE_TYPE)) {
            type2 = TreeUtilFunctions.findChildByType(type2, LANG2.USER_TYPE);
            if(type2 == null)
                return;
        }
        if(!type2.getType().name.equals(LANG2.USER_TYPE))
            return;
        if(type1.getType().name.equals(LANG1.PRIMITIVE_TYPE)) {
            if(type2.getChildren().size() == 1 && type2.getChild(0).isLeaf() && type2.getParent() != null) {
                //align Kotlin user_type -> type_identifier 'Int' with Java PrimitiveType 'int'
                Tree typeIdentifier2 = type2.getChild(0);
                removeDstMappings(mappingStore, type2);
                Tree parent2 = type2.getParent();
                parent2.getChildren().set(parent2.getChildPosition(type2), typeIdentifier2);
                typeIdentifier2.setParent(parent2);
                mappingStore.addMapping(type1, typeIdentifier2);
            }
        }
        else if(type1.getType().name.equals(LANG1.SIMPLE_TYPE)) {
            mappingStore.addMapping(type1, type2);
            if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                collapseQualifiedTypeIdentifiers(mappingStore, type1.getChild(0), type2, LANG1, LANG2);
                mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
            }
        }
        else if(type1.getType().name.equals(LANG1.PARAMETERIZED_TYPE) && type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
            mappingStore.addMapping(type1, type2);
            //align Kotlin user_type -> [type_identifier, type_arguments -> type_projection* -> type] with Java ParameterizedType -> [SimpleType -> SimpleName, type*]
            Tree baseType1 = type1.getChild(0);
            //the leaf with the name of the Java type, whether or not the SimpleType is already flattened
            Tree baseName1 = baseType1;
            if(baseType1.getType().name.equals(LANG1.SIMPLE_TYPE) && baseType1.getChildren().size() == 1 && baseType1.getChild(0).isLeaf()) {
                baseName1 = baseType1.getChild(0);
                flattenSrcChildKeepingMappings(deferredFlattenings, type1, baseType1);
            }
            collapseQualifiedTypeIdentifiers(mappingStore, baseName1, type2, LANG1, LANG2);
            Tree typeArguments2 = TreeUtilFunctions.findChildByType(type2, LANG2.TYPE_ARGUMENTS);
            if(typeArguments2 != null) {
                flattenChild(type2, typeArguments2);
                for(Tree typeProjection2 : TreeUtilFunctions.findChildrenByType(type2, LANG2.TYPE_PROJECTION)) {
                    if(typeProjection2.getChildren().size() == 1) {
                        flattenChild(type2, typeProjection2);
                    }
                }
            }
            if(baseName1.isLeaf() && type2.getChild(0).isLeaf()) {
                mappingStore.addMapping(baseName1, type2.getChild(0));
            }
            if(type1.getChildren().size() == type2.getChildren().size()) {
                for(int i=1; i<type1.getChildren().size(); i++) {
                    alignTypes(mappingStore, type1.getChild(i), type2.getChild(i), LANG1, LANG2, deferredFlattenings);
                }
            }
        }
    }

    //align Kotlin user_type -> [type_identifier 'MockResponse', type_identifier 'Builder', ...] with Java SimpleType -> QualifiedName 'MockResponse.Builder',
    //the first type_identifier gets the label of the Java qualified name and spans all type_identifiers, which are removed, as with the qualified names in handleLeafMapping
    private static void collapseQualifiedTypeIdentifiers(ExtendedMultiMappingStore mappingStore, Tree qualifiedName1, Tree type2, Constants LANG1, Constants LANG2) {
        if(!qualifiedName1.getType().name.equals(LANG1.QUALIFIED_NAME) || !qualifiedName1.isLeaf())
            return;
        List<Tree> typeIdentifiers2 = new ArrayList<>();
        for(Tree child2 : type2.getChildren()) {
            if(!child2.getType().name.equals(LANG2.TYPE_IDENTIFIER) || !child2.isLeaf())
                break;
            typeIdentifiers2.add(child2);
        }
        //the type_identifiers are already collapsed by a previous mapping of the same Kotlin type
        if(typeIdentifiers2.size() < 2)
            return;
        String qualifiedName2 = typeIdentifiers2.stream().map(Tree::getLabel).collect(Collectors.joining("."));
        if(!qualifiedName2.equals(qualifiedName1.getLabel()))
            return;
        Tree first2 = typeIdentifiers2.get(0);
        Tree last2 = typeIdentifiers2.get(typeIdentifiers2.size() - 1);
        for(int i=1; i<typeIdentifiers2.size(); i++) {
            Tree typeIdentifier2 = typeIdentifiers2.get(i);
            removeDstMappings(mappingStore, typeIdentifier2);
            type2.getChildren().remove(typeIdentifier2);
        }
        first2.setLabel(qualifiedName1.getLabel());
        first2.setLength(last2.getEndPos() - first2.getPos());
    }

    public static void handleModifierMapping(ExtendedMultiMappingStore mappingStore, Tree srcModifierTree, Tree dstModifierTree, Constants LANG1, Constants LANG2) {
        if(dstModifierTree.isLeaf()) {
            mappingStore.addMapping(srcModifierTree, dstModifierTree);
            return;
        }
        Tree modifier2 = TreeUtilFunctions.findChildByType(dstModifierTree, LANG2.MODIFIER);
        //other Kotlin modifiers are wrapped in the same way, e.g., inheritance_modifier -> inherit_modifier 'abstract'
        if(modifier2 == null && dstModifierTree.getChildren().size() == 1 && dstModifierTree.getChild(0).isLeaf()) {
            modifier2 = dstModifierTree.getChild(0);
        }
        if(modifier2 != null) {
            mappingStore.addMapping(srcModifierTree, modifier2);
            //align Kotlin declaration -> modifiers -> visibility_modifier -> visibility_modifier 'private' with Java declaration -> Modifier 'private'
            Tree modifiers2 = dstModifierTree.getParent();
            if(modifier2.isLeaf() && dstModifierTree.getChildren().size() == 1 && srcModifierTree.getParent() != null &&
                    modifiers2 != null && modifiers2.getType().name.equals(LANG2.MODIFIERS) && modifiers2.getParent() != null) {
                Tree declaration2 = modifiers2.getParent();
                boolean first = modifiers2.getChildPosition(dstModifierTree) == 0;
                removeDstMappings(mappingStore, dstModifierTree);
                modifiers2.getChildren().remove(dstModifierTree);
                int index = declaration2.getChildPosition(modifiers2);
                if(modifiers2.getChildren().isEmpty()) {
                    removeDstMappings(mappingStore, modifiers2);
                    declaration2.getChildren().set(index, modifier2);
                    modifier2.setParent(declaration2);
                }
                else {
                    updateRangeToChildren(modifiers2);
                    //keep the source code order of the modifiers
                    declaration2.insertChild(modifier2, first ? index : index + 1);
                }
            }
        }
    }

    //align Kotlin function_declaration -> modifiers -> class_modifier -> [@, user_type -> Synchronized] with Java MethodDeclaration -> Modifier 'synchronized'
    public static void handleSynchronizedMapping(ExtendedMultiMappingStore mappingStore, Tree srcOperationNode, Tree dstOperationNode, Constants LANG1, Constants LANG2) {
        handleModifierAnnotationMapping(mappingStore, srcOperationNode, dstOperationNode, LANG1.SYNCHRONIZED, "Synchronized", LANG1, LANG2);
    }

    //align Kotlin property_declaration -> modifiers -> class_modifier -> [@, user_type -> Volatile] with Java FieldDeclaration -> Modifier 'volatile'
    public static void handleVolatileMapping(ExtendedMultiMappingStore mappingStore, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2) {
        handleModifierAnnotationMapping(mappingStore, srcFieldDeclaration, dstFieldDeclaration, LANG1.VOLATILE, "Volatile", LANG1, LANG2);
    }

    //the Kotlin annotation is the equivalent of the Java modifier, so it is labeled as the Java modifier
    private static void handleModifierAnnotationMapping(ExtendedMultiMappingStore mappingStore, Tree srcDeclaration, Tree dstDeclaration, String modifier, String annotationName, Constants LANG1, Constants LANG2) {
        Tree modifier1 = TreeUtilFunctions.findChildByTypeAndLabel(srcDeclaration, LANG1.MODIFIER, modifier, LANG1);
        Tree modifiers2 = TreeUtilFunctions.findChildByType(dstDeclaration, LANG2.MODIFIERS);
        if(modifier1 == null || modifiers2 == null)
            return;
        Tree annotation2 = null;
        for(Tree classModifier2 : TreeUtilFunctions.findChildrenByType(modifiers2, LANG2.CLASS_MODIFIER)) {
            Tree userType2 = TreeUtilFunctions.findChildByType(classModifier2, LANG2.USER_TYPE);
            if(TreeUtilFunctions.findChildByType(classModifier2, LANG2.AT) != null && userType2 != null && userType2.getChildren().size() == 1 &&
                    userType2.getChild(0).getLabel().equals(annotationName)) {
                annotation2 = classModifier2;
                break;
            }
        }
        if(annotation2 == null)
            return;
        for(Tree t : annotation2.getDescendants()) {
            removeDstMappings(mappingStore, t);
        }
        annotation2.getChildren().clear();
        annotation2.setLabel(modifier1.getLabel());
        liftFromModifiers(mappingStore, modifiers2, annotation2);
        mappingStore.addMapping(modifier1, annotation2);
    }

    //align Kotlin function_declaration -> modifiers -> member_modifier -> member_modifier 'override' with Java MethodDeclaration -> MarkerAnnotation -> SimpleName 'Override'
    public static void handleOverrideMapping(ExtendedMultiMappingStore mappingStore, Tree srcOperationNode, Tree dstOperationNode, Constants LANG1, Constants LANG2) {
        Tree annotation1 = null;
        for(Tree child1 : TreeUtilFunctions.findChildrenByType(srcOperationNode, LANG1.MARKER_ANNOTATION)) {
            if(child1.getChildren().size() == 1 && child1.getChild(0).getType().name.equals(LANG1.SIMPLE_NAME) && child1.getChild(0).getLabel().equals("Override")) {
                annotation1 = child1;
                break;
            }
        }
        Tree modifiers2 = TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.MODIFIERS);
        if(annotation1 == null || modifiers2 == null)
            return;
        Tree override2 = null;
        for(Tree child2 : modifiers2.getChildren()) {
            if(child2.getChildren().size() == 1 && child2.getChild(0).isLeaf() && child2.getChild(0).getLabel().equals(LANG2.OVERRIDE)) {
                override2 = child2;
                break;
            }
        }
        if(override2 == null)
            return;
        //the override modifier is the Kotlin equivalent of the @Override annotation, so it is labeled as the Java annotation name
        Tree keyword2 = override2.getChild(0);
        removeDstMappings(mappingStore, override2);
        removeDstMappings(mappingStore, keyword2);
        keyword2.setLabel(annotation1.getChild(0).getLabel());
        liftFromModifiers(mappingStore, modifiers2, override2);
        mappingStore.addMapping(annotation1, override2);
        mappingStore.addMapping(annotation1.getChild(0), keyword2);
    }

    //moves the modifier from the Kotlin modifiers to the declaration, keeping the source code order of the annotations and modifiers
    private static void liftFromModifiers(ExtendedMultiMappingStore mappingStore, Tree modifiers2, Tree modifier2) {
        Tree declaration2 = modifiers2.getParent();
        boolean first = modifiers2.getChildPosition(modifier2) == 0;
        modifiers2.getChildren().remove(modifier2);
        int index = declaration2.getChildPosition(modifiers2);
        if(modifiers2.getChildren().isEmpty()) {
            removeDstMappings(mappingStore, modifiers2);
            declaration2.getChildren().set(index, modifier2);
        }
        else {
            updateRangeToChildren(modifiers2);
            declaration2.insertChild(modifier2, first ? index : index + 1);
        }
        modifier2.setParent(declaration2);
    }

    //align Kotlin property_declaration -> [name, =, initializer] with Java ExpressionStatement -> Assignment -> [name, =, expression],
    //when the assignment is moved to the initializer of the assigned property, i.e., client = builder.client; -> val client: Boolean = builder.client
    public static boolean handleAssignmentToInitializerMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstInitializer, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(!srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) || srcStatementNode.getChildren().size() != 1)
            return false;
        Tree assignment1 = srcStatementNode.getChild(0);
        if(!assignment1.getType().name.equals(LANG1.ASSIGNMENT) || assignment1.getChildren().size() != 3 ||
                !assignment1.getChild(1).getType().name.equals(LANG1.ASSIGNMENT_OPERATOR))
            return false;
        Tree declaration2 = dstInitializer.getParent();
        if(declaration2 == null || !declaration2.getType().name.equals(LANG2.FIELD_DECLARATION))
            return false;
        int index2 = declaration2.getChildPosition(dstInitializer);
        if(index2 < 1 || !declaration2.getChild(index2 - 1).getType().name.equals(LANG2.AFFECTATION_OPERATOR))
            return false;
        Tree operator1 = assignment1.getChild(1);
        Tree operator2 = declaration2.getChild(index2 - 1);
        if(!mappingStore.isDstMapped(operator2))
            mappingStore.addMapping(operator1, operator2);
        Tree expression1 = assignment1.getChild(2);
        mappingStore.addMapping(expression1, dstInitializer);
        handleLeafMapping(mappingStore, expression1, dstInitializer, LANG1, LANG2, deferredFlattenings);
        return true;
    }

    //align Kotlin call_expression -> [simple_identifier, call_suffix -> annotated_lambda -> lambda_literal -> statements -> stmt*] with Java TryStatement -> [try, Block -> stmt*, CatchClause*],
    //when the try statement is replaced with a call to a function taking the try block as lambda, i.e., try {...} catch (IOException ignored) {} -> ignoreIoExceptions {...}
    private static void handleTryToLambdaCallMapping(ExtendedMultiMappingStore mappingStore, Tree try1, Tree call2, Constants LANG1, Constants LANG2, DeferredFlattenings deferredFlattenings) {
        if(call2.getChildren().size() != 2)
            return;
        Tree name2 = call2.getChild(0);
        Tree suffix2 = call2.getChild(1);
        if(!name2.getType().name.equals(LANG2.SIMPLE_NAME) || !suffix2.getType().name.equals(LANG2.CALL_SUFFIX) || suffix2.getChildren().size() != 1)
            return;
        Tree annotatedLambda2 = suffix2.getChild(0);
        if(!annotatedLambda2.getType().name.equals(LANG2.ANNOTATED_LAMBDA) || annotatedLambda2.getChildren().size() != 1 ||
                !annotatedLambda2.getChild(0).getType().name.equals(LANG2.LAMBDA_LITERAL))
            return;
        Tree lambdaLiteral2 = annotatedLambda2.getChild(0);
        Tree statements2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.STATEMENTS);
        Tree block1 = TreeUtilFunctions.findChildByType(try1, LANG1.BLOCK);
        if(block1 == null || statements2 == null)
            return;
        //align call_expression -> call_suffix -> annotated_lambda -> lambda_literal with Java TryStatement -> Block
        flattenDstChild(mappingStore, deferredFlattenings, call2, suffix2);
        flattenDstChild(mappingStore, deferredFlattenings, call2, annotatedLambda2);
        //align lambda_literal -> statements -> stmt* with Java Block -> stmt*, as in function_body and control_structure_body including the braces of the block
        flattenDstChild(mappingStore, deferredFlattenings, lambdaLiteral2, statements2);
        removeDstMappingsOfCounterpart(mappingStore, deferredFlattenings, lambdaLiteral2, try1);
        mappingStore.addMapping(block1, lambdaLiteral2);
        //the try keyword is not in the Java tree, so a leaf is added for it, which is updated to the name of the Kotlin function
        Tree keyword1 = TreeUtilFunctions.findChildByType(try1, LANG1.TRY_KEYWORD);
        if(keyword1 == null) {
            keyword1 = new DefaultTree(TypeSet.type(LANG1.TRY_KEYWORD), LANG1.TRY_KEYWORD);
            keyword1.setPos(try1.getPos());
            keyword1.setLength(LANG1.TRY_KEYWORD.length());
            try1.insertChild(keyword1, 0);
            keyword1.setParent(try1);
        }
        if(!mappingStore.isDstMapped(name2)) {
            mappingStore.addMapping(keyword1, name2);
        }
    }

    //align Kotlin annotated_lambda -> lambda_literal -> statements -> stmt* with
    //Java ClassInstanceCreation -> AnonymousClassDeclaration -> MethodDeclaration -> Block -> stmt*, when the anonymous class is replaced with a lambda,
    //i.e., new NamedRunnable("OkHttp %s", connectionName) { public void execute() {...} } -> tryExecute("OkHttp $connectionName") {...}
    //the body of the anonymous class becomes the lambda, while the method, including the braces of its body, is deleted
    public static void handleAnonymousToLambdaMapping(ExtendedMultiMappingStore mappingStore, Tree anonymousClass1, Tree lambda2, Constants LANG1, Constants LANG2) {
        if(anonymousClass1 == null || !anonymousClass1.getType().name.equals(LANG1.ANONYMOUS_CLASS_DECLARATION) || lambda2 == null)
            return;
        //the anonymous class implements a single method, i.e., execute() of NamedRunnable
        List<Tree> methods1 = TreeUtilFunctions.findChildrenByType(anonymousClass1, LANG1.METHOD_DECLARATION);
        if(methods1.size() != 1)
            return;
        Tree anonymousMethod1 = methods1.get(0);
        Tree lambdaLiteral2 = lambda2;
        if(lambdaLiteral2.getType().name.equals(LANG2.ANNOTATED_LAMBDA))
            lambdaLiteral2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.LAMBDA_LITERAL);
        else if(lambdaLiteral2.getType().name.equals(LANG2.STATEMENTS))
            lambdaLiteral2 = lambdaLiteral2.getParent();
        if(lambdaLiteral2 == null || !lambdaLiteral2.getType().name.equals(LANG2.LAMBDA_LITERAL))
            return;
        Tree block1 = TreeUtilFunctions.findChildByType(anonymousMethod1, LANG1.BLOCK);
        Tree trailingLambdaLiteral2 = findTrailingLambdaOfMappedCall(mappingStore, anonymousClass1.getParent(), LANG1, LANG2);
        if(trailingLambdaLiteral2 != null) {
            //the lambda passed to the call having the anonymous class as argument, i.e., pushExecutorExecute(new NamedRunnable(...) {...}) -> pushExecutor.execute(...) {...}
            lambdaLiteral2 = trailingLambdaLiteral2;
        }
        else if(block1 != null && mappingStore.isSrcMapped(block1)) {
            //the lambda, whose body is mapped to the body of the method, replaces the anonymous class
            Tree mappedLambdaLiteral2 = null;
            for(Tree dst : mappingStore.getDsts(block1)) {
                if(dst.getType().name.equals(LANG2.STATEMENTS) && dst.getParent() != null && dst.getParent().getType().name.equals(LANG2.LAMBDA_LITERAL)) {
                    mappedLambdaLiteral2 = dst.getParent();
                    break;
                }
            }
            if(mappedLambdaLiteral2 == null)
                return;
            lambdaLiteral2 = mappedLambdaLiteral2;
        }
        //the method, including its body, has no counterpart in the lambda
        removeSrcMappings(mappingStore, anonymousMethod1);
        if(block1 != null)
            removeSrcMappings(mappingStore, block1);
        //align lambda_literal -> statements -> stmt* with the statements of the method body
        Tree statements2 = TreeUtilFunctions.findChildByType(lambdaLiteral2, LANG2.STATEMENTS);
        if(statements2 != null) {
            removeDstMappings(mappingStore, statements2);
            flattenChild(lambdaLiteral2, statements2);
        }
        removeDstMappings(mappingStore, lambdaLiteral2);
        mappingStore.addMapping(anonymousClass1, lambdaLiteral2);
        Tree classInstanceCreation1 = anonymousClass1.getParent();
        Tree annotatedLambda2 = lambdaLiteral2.getParent();
        if(classInstanceCreation1 != null && annotatedLambda2 != null && classInstanceCreation1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) &&
                annotatedLambda2.getType().name.equals(LANG2.ANNOTATED_LAMBDA)) {
            mappingStore.addMapping(classInstanceCreation1, annotatedLambda2);
        }
    }

    public static void handleFunctionBodyMapping(ExtendedMultiMappingStore mappingStore, Tree srcOperationNode, Tree dstOperationNode, Constants LANG1, Constants LANG2) {
        Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.BLOCK,LANG2.FUNCTION_BODY);
        if (matched != null) {
            Tree functionBody = matched.second;
            //align function_body -> statements -> stmt* with Java Block -> stmt*
            flattenChild(functionBody, TreeUtilFunctions.findChildByType(functionBody, LANG2.STATEMENTS));
            //align control_structure_body -> statements -> stmt* with Java Block -> stmt*
            for (Tree controlStructureBody : TreeUtilFunctions.findChildrenByTypeRecursively(functionBody, LANG2.CONTROL_STRUCTURE_BODY)) {
                flattenChild(controlStructureBody, TreeUtilFunctions.findChildByType(controlStructureBody, LANG2.STATEMENTS));
            }
            mappingStore.addMapping(matched.first, functionBody);
        }
        //align function_declaration -> function_value_parameters -> parameter* with Java MethodDeclaration -> SingleVariableDeclaration*
        flattenChild(dstOperationNode, TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.FUNCTION_PARAMETERS));
        //align function names that are soft keywords, i.e., simple_identifier -> class_modifier 'data', with Java SimpleName 'data'
        Tree name1 = TreeUtilFunctions.findChildByType(srcOperationNode, LANG1.SIMPLE_NAME);
        Tree name2 = TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.SIMPLE_NAME);
        if(name1 != null && name2 != null && name2.getLabel().isEmpty() && name2.getChildren().size() == 1 && name2.getChild(0).isLeaf() &&
                name2.getChild(0).getLabel().equals(name1.getLabel())) {
            removeDstMappings(mappingStore, name2.getChild(0));
            name2.setLabel(name2.getChild(0).getLabel());
            name2.getChildren().clear();
            mappingStore.addMapping(name1, name2);
        }
    }

    private static boolean isJumpKeywordWithAlignedChildren(Tree statement1, Tree jumpExpression2, Constants LANG1, Constants LANG2) {
        String keyword = statement1.getType().name.equals(LANG1.THROW_STATEMENT) ? "throw" : "return";
        return statement1.getChildren().size() <= 1 && jumpExpression2.getChildren().size() == statement1.getChildren().size() + 1 &&
                jumpExpression2.getChild(0).getType().name.equals(LANG2.JUMP_KEYWORD) && jumpExpression2.getChild(0).getLabel().equals(keyword);
    }

    //shrinks the source code range of a node to cover only its remaining children
    private static void updateRangeToChildren(Tree tree) {
        if(!tree.getChildren().isEmpty()) {
            int start = tree.getChild(0).getPos();
            int end = tree.getChild(tree.getChildren().size() - 1).getEndPos();
            tree.setPos(start);
            tree.setLength(end - start);
        }
    }

    private static void removeSrcMappings(ExtendedMultiMappingStore mappingStore, Tree src) {
        Set<Tree> dsts = mappingStore.getDsts(src);
        if(dsts != null) {
            for(Tree dst : new ArrayList<>(dsts)) {
                mappingStore.removeMapping(src, dst);
            }
        }
    }

    private static void removeDstMappings(ExtendedMultiMappingStore mappingStore, Tree dst) {
        Set<Tree> srcs = mappingStore.getSrcs(dst);
        if(srcs != null) {
            for(Tree src : new ArrayList<>(srcs)) {
                mappingStore.removeMapping(src, dst);
            }
        }
    }

    //returns the receiver of Java [ExpressionStatement|MethodInvocation] -> [METHOD_INVOCATION_RECEIVER -> SimpleName, SimpleName, ...],
    //if the matching Kotlin call_expression -> [simple_identifier, ...] has no receiver and is inside a lambda, i.e., the lambda of a scope function like apply
    private static Tree findImplicitReceiver(Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        Tree invocation1 = srcStatementNode;
        if(invocation1.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && invocation1.getChildren().size() == 1 &&
                invocation1.getChild(0).getType().name.equals(LANG1.METHOD_INVOCATION)) {
            invocation1 = invocation1.getChild(0);
        }
        if(!invocation1.getType().name.equals(LANG1.EXPRESSION_STATEMENT) && !invocation1.getType().name.equals(LANG1.METHOD_INVOCATION))
            return null;
        if(invocation1.getChildren().size() < 2 || !dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION) || dstStatementNode.getChildren().isEmpty())
            return null;
        Tree receiver1 = invocation1.getChild(0);
        Tree name1 = invocation1.getChild(1);
        if(!receiver1.getType().name.equals(LANG1.METHOD_INVOCATION_RECEIVER) || receiver1.getChildren().size() != 1 ||
                !receiver1.getChild(0).getType().name.equals(LANG1.SIMPLE_NAME) || !name1.getType().name.equals(LANG1.SIMPLE_NAME))
            return null;
        Tree name2 = dstStatementNode.getChild(0);
        if(!name2.getType().name.equals(LANG2.SIMPLE_NAME))
            return null;
        //soft keywords, like set, are wrapped in the simple_identifier
        String label2 = name2.getLabel().isEmpty() && name2.getChildren().size() == 1 ? name2.getChild(0).getLabel() : name2.getLabel();
        if(!name1.getLabel().equals(label2) || TreeUtilFunctions.getParentUntilType(dstStatementNode, LANG2.LAMBDA_LITERAL) == null)
            return null;
        return receiver1.getChild(0);
    }

    //the Kotlin call apply, whose receiver corresponds to the initializer of the Java variable declaration, i.e., RealCall call = new RealCall(x); -> return RealCall(x).apply {...}
    private static Tree findApplyCallWithInitializerReceiver(Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        if(!srcStatementNode.getType().name.equals(LANG1.VARIABLE_DECLARATION_STATEMENT))
            return null;
        Tree fragment1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.VARIABLE_DECLARATION_FRAGMENT);
        if(fragment1 == null || fragment1.getChildren().size() < 2)
            return null;
        Tree initializer1 = fragment1.getChild(fragment1.getChildren().size() - 1);
        if(!initializer1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) && !initializer1.getType().name.equals(LANG1.METHOD_INVOCATION))
            return null;
        List<Tree> calls2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION);
        if(dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            calls2.add(0, dstStatementNode);
        }
        for(Tree call2 : calls2) {
            Tree navigation2 = call2.getChildren().size() > 0 ? call2.getChild(0) : null;
            if(navigation2 != null && navigation2.getType().name.equals(LANG2.NAVIGATION_EXPRESSION) && navigation2.getChildren().size() > 0 &&
                    navigation2.getChild(0).getType().name.equals(LANG2.METHOD_INVOCATION) && findApplyName(call2, LANG2) != null) {
                return call2;
            }
        }
        return null;
    }

    //the apply name in call_expression -> [navigation_expression -> [receiver, navigation_suffix -> apply], call_suffix],
    //or call_expression -> [navigation_expression -> receiver, apply, call_suffix], if the navigation_suffix is already moved by a previous mapping of the same statement
    private static Tree findApplyName(Tree call2, Constants LANG2) {
        Tree navigation2 = call2.getChild(0);
        Tree last2 = navigation2.getChild(navigation2.getChildren().size() - 1);
        if(last2.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && last2.getChildren().size() == 1 && last2.getChild(0).getLabel().equals("apply")) {
            return last2.getChild(0);
        }
        if(call2.getChildren().size() > 1 && call2.getChild(1).getType().name.equals(LANG2.SIMPLE_NAME) && call2.getChild(1).getLabel().equals("apply")) {
            return call2.getChild(1);
        }
        return null;
    }

    //maps the Java assigned field qualified with a variable to the Kotlin assigned field inside a lambda, whose receiver is implicit, i.e., call.transmitter -> transmitter
    //returns the name of the variable, after removing the Kotlin field name from children2
    private static String alignImplicitReceiverFieldAssignment(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, List<Tree> children2, Constants LANG1, Constants LANG2) {
        if(!srcStatementNode.getType().name.equals(LANG1.EXPRESSION_STATEMENT) || !dstStatementNode.getType().name.equals(LANG2.ASSIGNMENT) ||
                srcStatementNode.getChildren().isEmpty() || dstStatementNode.getChildren().isEmpty() ||
                TreeUtilFunctions.getParentUntilType(dstStatementNode, LANG2.LAMBDA_LITERAL) == null)
            return null;
        //the Assignment, or the statement if the Assignment is already flattened by a previous mapping of the same statement
        Tree assignment1 = srcStatementNode.getChild(0).getType().name.equals(LANG1.ASSIGNMENT) ? srcStatementNode.getChild(0) : srcStatementNode;
        Tree target1 = assignment1.getChild(0);
        if(!target1.getType().name.equals(LANG1.QUALIFIED_NAME) || assignment1.getChildren().size() < 2 ||
                !assignment1.getChild(1).getType().name.equals(LANG1.ASSIGNMENT_OPERATOR))
            return null;
        String qualifiedName1 = target1.getLabel();
        int indexOfDot = qualifiedName1.indexOf(".");
        if(indexOfDot == -1 || indexOfDot != qualifiedName1.lastIndexOf("."))
            return null;
        String receiverName1 = qualifiedName1.substring(0, indexOfDot);
        String fieldName1 = qualifiedName1.substring(indexOfDot + 1);
        Tree target2 = dstStatementNode.getChild(0);
        if(target2.getType().name.equals(LANG2.DIRECTLY_ASSIGNABLE_EXPRESSION) && target2.getChildren().size() == 1) {
            target2 = target2.getChild(0);
        }
        if(!target2.getType().name.equals(LANG2.SIMPLE_NAME) || !target2.getLabel().equals(fieldName1))
            return null;
        mappingStore.addMapping(target1, target2);
        children2.remove(target2);
        return receiverName1;
    }

    private static boolean isInsideNavigationExpression(Tree t, Tree statement, Constants LANG2) {
        for(Tree parent = t.getParent(); parent != null && parent != statement.getParent(); parent = parent.getParent()) {
            if(parent.getType().name.equals(LANG2.NAVIGATION_EXPRESSION))
                return true;
        }
        return false;
    }

    //checks if name is one of the dot-separated segments of qualifiedName, i.e., e is not a segment of ErrorCode.PROTOCOL_ERROR
    private static boolean isQualifiedNameSegment(String qualifiedName, String name) {
        return !name.isEmpty() && Arrays.asList(qualifiedName.split("\\.")).contains(name);
    }

    //post-processing of the Java anonymous class creation passed as argument at the call site of an inlined method, replaced with the lambda of the Kotlin call,
    //i.e., call(new NamedRunnable("OkHttp %s", connectionName) {...}) -> call("OkHttp $connectionName") {...}, match the string literal and variables with the interpolated string
    public static void handleAnonymousArgumentReplacedWithLambda(ExtendedMultiMappingStore mappingStore, Tree call1, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        Tree arguments1 = TreeUtilFunctions.findChildByType(call1, LANG1.METHOD_INVOCATION_ARGUMENTS);
        if(arguments1 == null)
            return;
        Tree creation1 = null;
        for(Tree argument1 : arguments1.getChildren()) {
            if(argument1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) && TreeUtilFunctions.findChildByType(argument1, LANG1.ANONYMOUS_CLASS_DECLARATION) != null) {
                creation1 = argument1;
                break;
            }
        }
        if(creation1 == null)
            return;
        //the string literals and interpolated identifiers of the Kotlin call outside the lambda
        List<Tree> stringLiterals2 = new ArrayList<>();
        List<Tree> interpolatedIdentifiers2 = new ArrayList<>();
        for(Tree t2 : dstStatementNode.preOrder()) {
            if(isWithinLambda(t2, dstStatementNode, LANG2))
                continue;
            if(t2.getType().name.equals(LANG2.STRING_LITERAL))
                stringLiterals2.add(t2);
            else if(t2.getType().name.equals(LANG2.INTERPOLATED_IDENTIFIER))
                interpolatedIdentifiers2.add(t2);
        }
        //the arguments of the anonymous class creation, excluding the body of the anonymous class
        List<Tree> stringLiterals1 = TreeUtilFunctions.findChildrenByType(creation1, LANG1.STRING_LITERAL);
        if(stringLiterals1.size() == 1 && stringLiterals2.size() == 1) {
            Tree stringLiteral1 = stringLiterals1.get(0);
            Tree stringLiteral2 = stringLiterals2.get(0);
            //the Kotlin string literal gets the label of the Java string literal, as in the other string literal mappings (see handleLeafMapping)
            if(stringLiteral2.getChildren().size() > 0 && !stringLiteral2.getLabel().equals(stringLiteral1.getLabel())) {
                stringLiteral2.setLabel(stringLiteral1.getLabel());
                stringLiteral2.getChildren().remove(0);
            }
            mappingStore.addMapping(stringLiteral1, stringLiteral2);
        }
        for(Tree simpleName1 : TreeUtilFunctions.findChildrenByType(creation1, LANG1.SIMPLE_NAME)) {
            for(Tree interpolatedIdentifier2 : interpolatedIdentifiers2) {
                if(simpleName1.getLabel().equals(interpolatedIdentifier2.getLabel()) && !mappingStore.isDstMapped(interpolatedIdentifier2)) {
                    mappingStore.addMapping(simpleName1, interpolatedIdentifier2);
                    break;
                }
            }
        }
    }

    //post-processing of the Java anonymous class creation passed as argument in the statement of an extracted method, whose parameter is passed as argument to the call,
    //i.e., call(new NamedRunnable("OkHttp %s", connectionName) {...}) -> extracted("OkHttp $connectionName") {...}, where extracted(name, block) calls call(name, block),
    //match the string literal and variables with the interpolated string at the call site of the extracted method, instead of the parameters of the extracted method
    public static void handleAnonymousArgumentReplacedWithLambdaInExtractedMethod(ExtendedMultiMappingStore mappingStore, Tree call1, Tree dstStatementNode, Tree callSite2, Constants LANG1, Constants LANG2) {
        Tree arguments1 = TreeUtilFunctions.findChildByType(call1, LANG1.METHOD_INVOCATION_ARGUMENTS);
        if(arguments1 == null)
            return;
        for(Tree argument1 : arguments1.getChildren()) {
            if(argument1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION) && TreeUtilFunctions.findChildByType(argument1, LANG1.ANONYMOUS_CLASS_DECLARATION) != null) {
                for(Tree child1 : argument1.getChildren()) {
                    if(child1.getType().name.equals(LANG1.ANONYMOUS_CLASS_DECLARATION))
                        continue;
                    for(Tree t1 : child1.preOrder()) {
                        Set<Tree> dsts = mappingStore.getDsts(t1);
                        if(dsts == null)
                            continue;
                        for(Tree dst : new ArrayList<>(dsts)) {
                            if(isDescendantOf(dst, dstStatementNode))
                                mappingStore.removeMapping(t1, dst);
                        }
                    }
                }
            }
        }
        handleAnonymousArgumentReplacedWithLambda(mappingStore, call1, callSite2, LANG1, LANG2);
    }

    private static boolean isDescendantOf(Tree t, Tree ancestor) {
        for(Tree parent = t; parent != null; parent = parent.getParent()) {
            if(parent == ancestor)
                return true;
        }
        return false;
    }

    //the node is within a lambda of the statement
    private static boolean isWithinLambda(Tree t, Tree statement, Constants LANG) {
        for(Tree parent = t; parent != null && parent != statement; parent = parent.getParent()) {
            if(parent.getType().name.equals(LANG.ANNOTATED_LAMBDA) || parent.getType().name.equals(LANG.LAMBDA_LITERAL))
                return true;
        }
        return false;
    }

    //Kotlin call_expression -> [..., annotated_lambda] or call_expression -> [..., call_suffix -> [..., annotated_lambda]]
    private static boolean hasTrailingLambda(Tree call2, Constants LANG2) {
        if(!call2.getType().name.equals(LANG2.METHOD_INVOCATION))
            return false;
        if(TreeUtilFunctions.findChildByType(call2, LANG2.ANNOTATED_LAMBDA) != null)
            return true;
        Tree suffix2 = TreeUtilFunctions.findChildByType(call2, LANG2.CALL_SUFFIX);
        return suffix2 != null && TreeUtilFunctions.findChildByType(suffix2, LANG2.ANNOTATED_LAMBDA) != null;
    }

    //flattens the child of the Kotlin tree removing all mappings to it, or defers the flattening until all diffs are matched
    private static void flattenDstChild(ExtendedMultiMappingStore mappingStore, DeferredFlattenings deferredFlattenings, Tree parent2, Tree child2) {
        if(deferredFlattenings != null) {
            deferredFlattenings.flattenTargetRemovingAllMappings(parent2, child2);
            return;
        }
        removeDstMappings(mappingStore, child2);
        flattenChild(parent2, child2);
    }

    //flattens the child of the Kotlin tree keeping the mappings to it, or defers the flattening until all diffs are matched
    private static void flattenDstChildKeepingMappings(DeferredFlattenings deferredFlattenings, Tree parent2, Tree child2) {
        if(deferredFlattenings != null)
            deferredFlattenings.flattenKeepingMappings(parent2, child2);
        else
            flattenChild(parent2, child2);
    }

    //removes the child of the Kotlin tree and all mappings to it, or defers the removal until all diffs are matched
    private static void removeDstChild(ExtendedMultiMappingStore mappingStore, DeferredFlattenings deferredFlattenings, Tree parent2, Tree child2) {
        if(deferredFlattenings != null) {
            deferredFlattenings.removeTargetChild(parent2, child2);
            return;
        }
        removeDstMappings(mappingStore, child2);
        parent2.getChildren().remove(child2);
    }

    //removes the mappings to the Kotlin node, before mapping it to the Java node, keeping the mappings from other Java trees if the flattenings are deferred,
    //i.e., multiple Java statements mapped to the same Kotlin statement
    private static void removeDstMappingsOfCounterpart(ExtendedMultiMappingStore mappingStore, DeferredFlattenings deferredFlattenings, Tree dst, Tree src1) {
        if(deferredFlattenings == null) {
            removeDstMappings(mappingStore, dst);
            return;
        }
        Set<Tree> srcs = mappingStore.getSrcs(dst);
        if(srcs != null) {
            for(Tree src : new ArrayList<>(srcs)) {
                for(Tree parent = src; parent != null; parent = parent.getParent()) {
                    if(parent == src1) {
                        mappingStore.removeMapping(src, dst);
                        break;
                    }
                }
            }
        }
    }

    //the number of children of the node after flattening the given children (which are already flattened, if the flattenings are not deferred)
    private static int sizeAfterFlattening(Tree node, Set<Tree> flattened) {
        int size = 0;
        for(Tree child : node.getChildren())
            size += flattened.contains(child) ? sizeAfterFlattening(child, flattened) : 1;
        return size;
    }

    //flattens the child of the Java tree keeping its mappings, or defers the flattening until all diffs are matched
    private static void flattenSrcChildKeepingMappings(DeferredFlattenings deferredFlattenings, Tree parent1, Tree child1) {
        if(deferredFlattenings != null)
            deferredFlattenings.flattenKeepingMappings(parent1, child1);
        else
            flattenChild(parent1, child1);
    }

    //flattens the child of the Java tree removing all its mappings, or defers the flattening until all diffs are matched
    private static void flattenSrcChild(ExtendedMultiMappingStore mappingStore, DeferredFlattenings deferredFlattenings, Tree parent1, Tree child1) {
        if(deferredFlattenings != null) {
            deferredFlattenings.flattenRemovingAllMappings(parent1, child1);
            return;
        }
        Set<Tree> dsts = mappingStore.getDsts(child1);
        if(dsts != null) {
            for(Tree dst : new ArrayList<>(dsts)) {
                mappingStore.removeMapping(child1, dst);
            }
        }
        flattenChild(parent1, child1);
    }

    //removes the leaf child of the Java tree with all its mappings, so that the parent gets its label, or defers the removal until all diffs are matched
    private static void absorbSrcLeafChild(ExtendedMultiMappingStore mappingStore, DeferredFlattenings deferredFlattenings, Tree parent1, Tree child1) {
        if(deferredFlattenings != null) {
            deferredFlattenings.absorbLeafChild(parent1, child1);
            return;
        }
        Set<Tree> dsts = mappingStore.getDsts(child1);
        if(dsts != null) {
            for(Tree dst : new ArrayList<>(dsts)) {
                mappingStore.removeMapping(child1, dst);
            }
        }
        DeferredFlattenings.applyLeafChildAbsorption(parent1, child1);
    }

    private static void flattenChild(Tree parent, Tree child) {
        if (child == null)
            return;
        int index = parent.getChildPosition(child);
        parent.getChildren().remove(index);
        parent.getChildren().addAll(index, child.getChildren());
        for (Tree t : child.getChildren())
            t.setParent(parent);
    }
}
