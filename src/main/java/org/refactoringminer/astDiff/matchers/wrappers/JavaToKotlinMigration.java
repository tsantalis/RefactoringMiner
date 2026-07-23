package org.refactoringminer.astDiff.matchers.wrappers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

public class JavaToKotlinMigration {

    public static void handleCompositeMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        if(srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE)) {
            Tree singleVariableDeclaration1 = TreeUtilFunctions.findChildByType(srcStatementNode, LANG1.SINGLE_VARIABLE_DECLARATION);
            if(singleVariableDeclaration1 != null) {
                handleParameterMapping(mappingStore, singleVariableDeclaration1, dstStatementNode, LANG1, LANG2);
                srcStatementNode.getChildren().remove(singleVariableDeclaration1);
                srcStatementNode.getChildren().addAll(singleVariableDeclaration1.getChildren());
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
        else if(srcStatementNode.getType().name.equals(LANG1.IF_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.IF_STATEMENT) &&
                srcStatementNode.getChildren().size() > 0 && dstStatementNode.getChildren().size() > 0) {
            Tree expression1 = srcStatementNode.getChild(0);
            Tree expression2 = dstStatementNode.getChild(0);
            handleLeafMapping(mappingStore, expression1, expression2, LANG1, LANG2);
        }
    }

    public static void handleLeafMapping(ExtendedMultiMappingStore mappingStore, Tree srcStatementNode, Tree dstStatementNode, Constants LANG1, Constants LANG2) {
        /*
        Map<Tree, Tree> cpyToSrc = new HashMap<>();
        Tree srcFakeTree = TreeUtilFunctions.deepCopyWithMap(srcStatementNode, cpyToSrc);
        Map<Tree, Tree> cpyToDst = new HashMap<>();
        Tree dstFakeTree = TreeUtilFunctions.deepCopyWithMapAndTypeTranslation(dstStatementNode, cpyToDst, LANG1, LANG2);
        ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null,LANG1,LANG2);
        //both trees are now in LANG1
        new LeafMatcher(LANG1, LANG1).match(srcFakeTree, dstFakeTree, tempMapping);
        */
        if(dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION) && dstStatementNode.getChildren().size() > 0 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_KEYWORD)) {
            if(dstStatementNode.getChild(0).getLabel().equals("break") || dstStatementNode.getChild(0).getLabel().equals("continue")) {
                dstStatementNode.getChildren().clear();
            }
        }
        List<Tree> children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_NAME);
        Tree firstChild1 = children1.size() > 0 ? children1.get(0) : null;
        boolean firstChildIsType1 = firstChild1 != null && firstChild1.getParent().getType().name.equals(LANG1.SIMPLE_TYPE) &&
                !firstChild1.getParent().getParent().getType().name.equals(LANG1.CLASS_INSTANCE_CREATION);
        List<Tree> children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.SIMPLE_NAME);
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
                    for(Tree simpleName2 : simpleNames2) {
                        if(name.equals(simpleName2.getLabel())) {
                            mappingStore.addMapping(t1, simpleName2);
                            iter1.remove();
                            break;
                        }
                        else if(name.toLowerCase().endsWith(simpleName2.getLabel())) {
                            mappingStore.addMapping(t1, simpleName2);
                            iter1.remove();
                            break;
                        }
                    }
                }
            }
        }
        removeFromParent(children2, interpolatedExpressions2, LANG2.SIMPLE_NAME);
        List<Tree> types1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_TYPE);
        List<Tree> castExpressions1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.CAST_EXPRESSION);
        List<Tree> qualifiedNames1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.QUALIFIED_NAME);
        List<Tree> anonymous1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.ANONYMOUS_CLASS_DECLARATION);
        List<Tree> anonymous2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.OBJECT_LITERAL);
        List<Tree> lambdas1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.LAMBDA_EXPRESSION);
        List<Tree> lambdas2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.ANNOTATED_LAMBDA);
        //remove the simpleName children of anonymous/lambdas from the parent children
        removeFromParent(children1, anonymous1, LANG1.SIMPLE_NAME);
        removeFromParent(children2, anonymous2, LANG2.SIMPLE_NAME);
        removeFromParent(children1, lambdas1, LANG1.SIMPLE_NAME);
        boolean letWithLambda = letFound && lambdas2.size() > lambdas1.size();
        if(!letWithLambda) {
            removeFromParent(children2, lambdas2, LANG2.SIMPLE_NAME);
        }
        Map<Tree, Tree> qualifiedNameToNavigationExpression = new LinkedHashMap<>();
        if(types1.size() > 0 && children1.size() != children2.size()) {
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree type1 : types1) {
                if(type1.getChildren().size() > 0 && type1.getChild(0).getType().name.equals(LANG1.QUALIFIED_NAME)) {
                    String qualifiedType = type1.getChild(0).getLabel();
                    for(Tree child2 : children2) {
                        if(qualifiedType.contains(child2.getLabel() + ".") || qualifiedType.contains("." + child2.getLabel())) {
                            toBeRemoved2.add(child2);
                            if(child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                    //important: skip qualified types whose parent is a variable declaration statement, because these are replaced with var in Kotlin
                                    !type1.getParent().getType().name.equals(LANG1.VARIABLE_DECLARATION_STATEMENT) &&
                                    !qualifiedNameToNavigationExpression.containsKey(type1.getChild(0)) &&
                                    !qualifiedNameToNavigationExpression.containsValue(child2.getParent())) {
                                Tree lastChild = child2.getParent().getChild(child2.getParent().getChildren().size() - 1);
                                if(lastChild.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && lastChild.getChildren().size() > 0 &&
                                        qualifiedType.contains("." + lastChild.getChild(0).getLabel())) {
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
                    if(qualifiedType.contains(child2.getLabel() + ".") || qualifiedType.contains("." + child2.getLabel())) {
                        boolean skip = child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                child2.getParent().getParent().getType().name.equals(LANG2.METHOD_INVOCATION);
                        if(!skip) {
                            toBeRemoved2.add(child2);
                        }
                        if(child2.getParent().getType().name.equals(LANG2.NAVIGATION_EXPRESSION) &&
                                !qualifiedNameToNavigationExpression.containsKey(qualified1) &&
                                !qualifiedNameToNavigationExpression.containsValue(child2.getParent())) {
                            Tree lastChild = child2.getParent().getChild(child2.getParent().getChildren().size() - 1);
                            if(lastChild.getType().name.equals(LANG2.NAVIGATION_SUFFIX) && lastChild.getChildren().size() > 0 &&
                                    qualifiedType.contains("." + lastChild.getChild(0).getLabel())) {
                                qualifiedNameToNavigationExpression.put(qualified1, child2.getParent());
                            }
                        }
                    }
                }
            }
            children2.removeAll(toBeRemoved2);
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
        boolean equalsMismatch = children1.stream().anyMatch(node -> node.getLabel().equals("equals")) &&
                !children2.stream().anyMatch(node -> node.getLabel().equals("equals"));
        if(children1.size() != children2.size() || equalsMismatch) {
            List<Tree> toBeRemoved1 = new ArrayList<>();
            for(Tree child1 : children1) {
                if(child1.getLabel().equals("get") || child1.getLabel().equals("equals")) {
                    toBeRemoved1.add(child1);
                }
            }
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree child2 : children2) {
                if(child2.getLabel().equals("get") || child2.getLabel().equals("equals")) {
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
        else if(children1.size() > children2.size() && firstChildIsType1) {
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
        List<Tree> inv1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION, LANG1.CLASS_INSTANCE_CREATION);
        if(srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) || srcStatementNode.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
            inv1.add(0, srcStatementNode);
        }
        List<Tree> inv2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION);
        if(dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            inv2.add(0, dstStatementNode);
        }
        removeFromParent(inv1, anonymous1, LANG1.METHOD_INVOCATION);
        removeFromParent(inv1, anonymous1, LANG1.CLASS_INSTANCE_CREATION);
        removeFromParent(inv2, anonymous2, LANG2.METHOD_INVOCATION);
        removeFromParent(inv1, lambdas1, LANG1.METHOD_INVOCATION);
        removeFromParent(inv1, lambdas1, LANG1.CLASS_INSTANCE_CREATION);
        if(!letWithLambda) {
            removeFromParent(inv2, lambdas2, LANG2.METHOD_INVOCATION);
        }
        List<Tree> invocationsToBeRemoved = new ArrayList<>();
        if(nameCompliance(inv1, inv2, LANG1, LANG2)) {
            if(inv1.size() <= inv2.size()) {
                for(int i=0; i<inv1.size(); i++) {
                    Tree child1 = inv1.get(i);
                    Tree child2 = inv2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2, invocationsToBeRemoved);
                }
            }
            else if(inv2.size() < inv1.size()) {
                for(int i=0; i<inv2.size(); i++) {
                    Tree child1 = inv1.get(i);
                    Tree child2 = inv2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2, invocationsToBeRemoved);
                }
            }
        }
        inv1.removeAll(invocationsToBeRemoved);
        if(castExpressions1.size() > 0) {
            Tree simpleType = castExpressions1.get(0).getChild(0);
            Tree as2 = TreeUtilFunctions.findChildByType(dstStatementNode, LANG2.AS_EXPRESSION);
            if(as2 != null) {
                Tree userType2 = TreeUtilFunctions.findChildByType(as2, LANG2.USER_TYPE);
                if(userType2 != null) {
                    handleTypeMapping(mappingStore, simpleType, userType2, LANG1, LANG2);
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
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0)
                    mappingStore.addMapping(children1.get(i), children2.get(i).getChild(0));
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
        if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.DISJUNCTION_EXPRESSION)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode);
        }
        else if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.EQUALITY_EXPRESSION)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode);
        }
        else if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.ADDITIVE_EXPRESSION)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode);
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.INFIX_EXPRESSION_OPERATOR);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.LOGICAL_OPERATOR, LANG2.COMPARISON_OPERATOR, LANG2.ARITHMETIC_OPERATOR);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.PREFIX_EXPRESSION_OPERATOR);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.NOT_PREFIX_OPERATOR);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        List<Tree> nestedInfix1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.INFIX_EXPRESSION);
        if(srcStatementNode.getType().name.equals(LANG1.INFIX_EXPRESSION)) {
            nestedInfix1.add(srcStatementNode);
        }
        List<Tree> nestedInfix2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.DISJUNCTION_EXPRESSION, LANG2.EQUALITY_EXPRESSION, LANG2.ADDITIVE_EXPRESSION, LANG2.MULTIPLICATIVE_EXPRESSION);
        if(nestedInfix1.size() == nestedInfix2.size()) {
            for(int i=0; i<nestedInfix1.size(); i++) {
                mappingStore.addMapping(nestedInfix1.get(i), nestedInfix2.get(i));
            }
        }
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
            if(t2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION) || qualifiedNameToNavigationExpression.containsValue(t2)) {
                iterator2.remove();
            }
        }
        if(inv1.size() == children2.size()) {
            for(int i=0; i<inv1.size(); i++) {
                Tree navigationSuffix = TreeUtilFunctions.findChildByType(children2.get(i), LANG2.NAVIGATION_SUFFIX);
                if(navigationSuffix != null)
                    mappingStore.addMapping(inv1.get(i), navigationSuffix);
                mappingStore.addMapping(inv1.get(i), children2.get(i));
            }
        }
        if(inv1.size() == 1 && assignableExpression != null) {
            Tree navigationSuffix = TreeUtilFunctions.findChildByType(assignableExpression, LANG2.NAVIGATION_SUFFIX);
            if(navigationSuffix != null)
                mappingStore.addMapping(inv1.get(0), navigationSuffix);
        }
        if(srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.CONTROL_STRUCTURE_BODY) &&
                dstStatementNode.getChildren().size() > 0 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_EXPRESSION)) {
            Tree jumpExpression = dstStatementNode.getChild(0);
            Tree firstChild = jumpExpression.getChild(0);
            if(firstChild.getLabel().equals("return@")) {
                firstChild.setLabel("return");
            }
            mappingStore.addMapping(srcStatementNode, firstChild);
        }
        if(srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION) &&
                dstStatementNode.getChildren().size() > 0 && dstStatementNode.getChild(0).getType().name.equals(LANG2.JUMP_KEYWORD)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode.getChild(0));
        }
    }

    private static boolean nameCompliance(List<Tree> children1, List<Tree> children2, Constants LANG1, Constants LANG2) {
        List<String> callNames1 = new ArrayList<>();
        for(Tree child1 : children1) {
            if(child1.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
                Tree simpleType = TreeUtilFunctions.findChildByType(child1, LANG1.SIMPLE_TYPE);
                if(simpleType != null && simpleType.getChildren().size() > 0) {
                    callNames1.add(simpleType.getChild(0).getLabel());
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
        Map<String, String> synonyms = Map.of("url", "toUrl", "getBytes", "toByteArray", "asList", "listOf", "get", "toHttpUrl");
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
        else if(callNames2.size() > callNames1.size() && callNames2.containsAll(callNames1)) {
            //sort callNames2 based on callNames1
            List<Tree> newChildren2 = new ArrayList<>();
            for(String s : callNames1) {
                int index = callNames2.indexOf(s);
                newChildren2.add(children2.get(index));
            }
            for(String s : callNames2) {
                if(!callNames1.contains(s)) {
                    int index = callNames2.indexOf(s);
                    newChildren2.add(children2.get(index));
                }
            }
            children2.clear();
            children2.addAll(newChildren2);
            return true;
        }
        return false;
    }

    private static void removeFromParent(List<Tree> children, List<Tree> anonymousList, String astType) {
        for(Tree anonymous : anonymousList) {
            List<Tree> anonymousChildren = TreeUtilFunctions.findChildrenByTypeRecursively(anonymous, astType);
            children.removeAll(anonymousChildren);
        }
    }

    private static void processPair(ExtendedMultiMappingStore mappingStore, Tree child1, Tree child2, Constants LANG1, Constants LANG2, List<Tree> invocationsToBeRemoved) {
        mappingStore.addMapping(child1, child2);
        boolean isFirstChildType = child1.getChildren().size() > 0 && child1.getChild(0).getType().name.equals(LANG1.SIMPLE_TYPE);
        Tree name1 = TreeUtilFunctions.findChildByType(child1, LANG1.SIMPLE_NAME);
        Tree name2 = TreeUtilFunctions.findChildByType(child2, LANG2.SIMPLE_NAME);
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
    }

    public static void handleFieldDeclarationMapping(ExtendedMultiMappingStore mappingStore, 
            Tree srcAttr, Tree dstAttr, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2) {
        Tree variableDeclaration2 = TreeUtilFunctions.findChildByType(dstAttr, LANG2.VARIABLE_DECLARATION);
        if(variableDeclaration2 == null)
            variableDeclaration2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.VARIABLE_DECLARATION);
        if(variableDeclaration2 != null) {
            Tree name1 = TreeUtilFunctions.findChildByType(srcAttr, LANG1.SIMPLE_NAME);
            Tree name2 = TreeUtilFunctions.findChildByType(variableDeclaration2, LANG2.SIMPLE_NAME);
            if(name1 != null && name2 != null) {
                mappingStore.addMapping(name1, name2);
            }
            Tree type1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.SIMPLE_TYPE);
            Tree type2 = TreeUtilFunctions.findChildByType(variableDeclaration2, LANG2.USER_TYPE);
            if(type1 != null && type2 != null) {
                mappingStore.addMapping(type1, type2);
                if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                    mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
                }
            }
            Tree annotation1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.MARKER_ANNOTATION);
            Tree modifiers2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.MODIFIERS);
            handleAnnotationMapping(mappingStore, annotation1, modifiers2, LANG1, LANG2);
        }
        if(dstAttr.getType().name.equals(LANG2.CLASS_PARAMETER)) {
            mappingStore.addMapping(srcAttr, dstAttr);
            Tree name1 = TreeUtilFunctions.findChildByType(srcAttr, LANG1.SIMPLE_NAME);
            Tree name2 = TreeUtilFunctions.findChildByType(dstAttr, LANG2.SIMPLE_NAME);
            if(name1 != null && name2 != null) {
                mappingStore.addMapping(name1, name2);
            }
            Tree type1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.SIMPLE_TYPE);
            Tree type2 = TreeUtilFunctions.findChildByType(dstAttr, LANG2.USER_TYPE);
            if(type1 != null && type2 != null) {
                mappingStore.addMapping(type1, type2);
                if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                    mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
                }
            }
            Tree annotation1 = TreeUtilFunctions.findChildByType(srcFieldDeclaration, LANG1.MARKER_ANNOTATION);
            Tree modifiers2 = TreeUtilFunctions.findChildByType(dstFieldDeclaration, LANG2.MODIFIERS);
            handleAnnotationMapping(mappingStore, annotation1, modifiers2, LANG1, LANG2);
        }
    }

    public static void handleAnnotationMapping(ExtendedMultiMappingStore mappingStore, Tree srcClassAnnotationTree, Tree dstClassAnnotationTree, Constants LANG1, Constants LANG2) {
        if(srcClassAnnotationTree != null && dstClassAnnotationTree != null) {
            mappingStore.addMapping(srcClassAnnotationTree, dstClassAnnotationTree);
            Tree classModifiers2 = dstClassAnnotationTree.getType().name.equals(LANG2.CLASS_MODIFIER) ? dstClassAnnotationTree : TreeUtilFunctions.findChildByType(dstClassAnnotationTree, LANG2.CLASS_MODIFIER);
            if(classModifiers2 != null) {
                List<Tree> stringLiteral1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcClassAnnotationTree, LANG1.STRING_LITERAL);
                List<Tree> numberLiteral1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcClassAnnotationTree, LANG1.NUMBER_LITERAL);
                List<Tree> stringLiteral2 = null;
                List<Tree> numberLiteral2 = null;
                Tree typeName1 = TreeUtilFunctions.findChildByType(srcClassAnnotationTree, LANG1.SIMPLE_NAME);
                Tree userType2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.USER_TYPE);
                Tree at = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.AT);
                if(at != null) {
                    at.setLabel("");
                    mappingStore.addMapping(srcClassAnnotationTree, at);
                }
                if(userType2 == null) {
                    Tree constuctorInvocation2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.CONSTRUCTOR_INVOCATION);
                    if(constuctorInvocation2 != null) {
                        stringLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.STRING_LITERAL);
                        numberLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.INTEGER_LITERAL);
                        userType2 = TreeUtilFunctions.findChildByType(constuctorInvocation2, LANG2.USER_TYPE);
                        Tree valueArguments2 = TreeUtilFunctions.findChildByType(constuctorInvocation2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                        if(valueArguments2 != null) {
                            mappingStore.addMapping(srcClassAnnotationTree, valueArguments2);
                        }
                    }
                }
                if(typeName1 != null && userType2 != null && userType2.getChildren().size() > 0) {
                    mappingStore.addMapping(typeName1, userType2.getChild(0));
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
            }
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
        if(leftTree.getType().name.equals(LANG1.SINGLE_VARIABLE_DECLARATION) && rightTree.getType().name.equals(LANG2.PARAMETER))
            mappingStore.addMapping(leftTree, rightTree);
        Tree type1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.SIMPLE_TYPE);
        Tree type2 = TreeUtilFunctions.findChildByType(rightTree, LANG2.USER_TYPE);
        if(type1 != null && type2 != null) {
            mappingStore.addMapping(type1, type2);
            if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
            }
        }
        if(type1 == null && type2 != null) {
            type1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.PARAMETERIZED_TYPE);
            if(type1 != null) {
                mappingStore.addMapping(type1, type2);
                Tree typeArguments = TreeUtilFunctions.findChildByType(type2, LANG2.TYPE_ARGUMENTS);
                if(typeArguments != null) {
                    mappingStore.addMapping(type1, typeArguments);
                }
                List<Tree> typeNames1 = TreeUtilFunctions.findChildrenByTypeRecursively(type1, LANG1.SIMPLE_NAME);
                List<Tree> typeNames2 = TreeUtilFunctions.findChildrenByTypeRecursively(type2, LANG2.TYPE_IDENTIFIER);
                if(typeNames1.size() == typeNames2.size()) {
                    for(int i=0; i< typeNames1.size(); i++) {
                        Tree t1 = typeNames1.get(i);
                        Tree t2 = typeNames2.get(i);
                        mappingStore.addMapping(t1, t2);
                    }
                }
            }
        }
        Tree name1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.SIMPLE_NAME);
        Tree name2 = TreeUtilFunctions.findChildByType(rightTree, LANG2.SIMPLE_NAME);
        if(name1 != null && name2 != null) {
            mappingStore.addMapping(name1, name2);
        }
    }

    public static void handleTypeMapping(ExtendedMultiMappingStore mappingStore, Tree srcNode, Tree dstNode, Constants LANG1, Constants LANG2) {
        Tree type1 = srcNode;
        Tree type2 = dstNode.getType().name.equals(LANG2.USER_TYPE) ? dstNode : TreeUtilFunctions.findChildByType(dstNode, LANG2.USER_TYPE);
        if(type1 != null && type2 != null) {
            mappingStore.addMapping(type1, type2);
            if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
            }
        }
    }

    public static void handleModifierMapping(ExtendedMultiMappingStore mappingStore, Tree srcModifierTree, Tree dstModifierTree, Constants LANG1, Constants LANG2) {
        Tree modifier2 = TreeUtilFunctions.findChildByType(dstModifierTree, LANG2.MODIFIER);
        if(modifier2 != null)
            mappingStore.addMapping(srcModifierTree, modifier2);
    }

    public static void handleFunctionBodyMapping(ExtendedMultiMappingStore mappingStore, Tree srcOperationNode, Tree dstOperationNode, Constants LANG1, Constants LANG2) {
        Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.BLOCK,LANG2.FUNCTION_BODY);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
            if(matched.second.getChildren().size() > 0 && matched.second.getChild(0).getType().name.equals(LANG2.STATEMENTS)) {
                mappingStore.addMapping(matched.first,matched.second.getChild(0));
            }
        }
        Tree kotlinFunctionParameters = TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.FUNCTION_PARAMETERS);
        if(kotlinFunctionParameters != null) {
            mappingStore.addMapping(srcOperationNode, kotlinFunctionParameters);
        }
    }
}
