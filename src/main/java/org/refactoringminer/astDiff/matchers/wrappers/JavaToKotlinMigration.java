package org.refactoringminer.astDiff.matchers.wrappers;

import java.util.ArrayList;
import java.util.List;

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
        List<Tree> children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_NAME);
        Tree firstChild1 = children1.size() > 0 ? children1.get(0) : null;
        boolean firstChildIsType1 = firstChild1 != null && firstChild1.getParent().getType().name.equals(LANG1.SIMPLE_TYPE);
        List<Tree> children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.SIMPLE_NAME);
        if(children2.size() > 0 && children2.get(children2.size()-1).getLabel().equals("code")) {
            //remove .code on character literals to convert to int
            children2.remove(children2.size()-1);
        }
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
        removeFromParent(children2, lambdas2, LANG2.SIMPLE_NAME);
        if(types1.size() > 0 && children1.size() != children2.size()) {
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree type1 : types1) {
                if(type1.getChildren().size() > 0 && type1.getChild(0).getType().name.equals(LANG1.QUALIFIED_NAME)) {
                    String qualifiedType = type1.getChild(0).getLabel();
                    for(Tree child2 : children2) {
                        if(qualifiedType.contains(child2.getLabel() + ".") || qualifiedType.contains("." + child2.getLabel())) {
                            toBeRemoved2.add(child2);
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
                        toBeRemoved2.add(child2);
                    }
                }
            }
            children2.removeAll(toBeRemoved2);
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
        if(children1.size() != children2.size()) {
            List<Tree> toBeRemoved1 = new ArrayList<>();
            for(Tree child1 : children1) {
                if(child1.getLabel().equals("get")) {
                    toBeRemoved1.add(child1);
                }
            }
            List<Tree> toBeRemoved2 = new ArrayList<>();
            for(Tree child2 : children2) {
                if(child2.getLabel().equals("get")) {
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
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION, LANG1.CLASS_INSTANCE_CREATION);
        if(srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION) || srcStatementNode.getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)) {
            children1.add(0, srcStatementNode);
        }
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION);
        if(dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            children2.add(0, dstStatementNode);
        }
        removeFromParent(children1, anonymous1, LANG1.METHOD_INVOCATION);
        removeFromParent(children1, anonymous1, LANG1.CLASS_INSTANCE_CREATION);
        removeFromParent(children2, anonymous2, LANG2.METHOD_INVOCATION);
        removeFromParent(children1, lambdas1, LANG1.METHOD_INVOCATION);
        removeFromParent(children1, lambdas1, LANG1.CLASS_INSTANCE_CREATION);
        removeFromParent(children2, lambdas2, LANG2.METHOD_INVOCATION);
        if(nameCompliance(children1, children2, LANG1, LANG2)) {
            if(children1.size() <= children2.size()) {
                for(int i=0; i<children1.size(); i++) {
                    Tree child1 = children1.get(i);
                    Tree child2 = children2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2);
                }
            }
            else if(children2.size() < children1.size()) {
                for(int i=0; i<children2.size(); i++) {
                    Tree child1 = children1.get(i);
                    Tree child2 = children2.get(i);
                    processPair(mappingStore, child1, child2, LANG1, LANG2);
                }
            }
        }
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
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.STRING_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.STRING_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.NUMBER_LITERAL);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.INTEGER_LITERAL);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
                //handle -literal
                //if(children2.get(i).getParent().getType().name.equals(LANG2.PREFIX_EXPRESSION)) {
                //    if(children2.get(i).getParent().getChild(0).getType().name.equals(LANG2.ARITHMETIC_OPERATOR)) {
                //        mappingStore.addMapping(children1.get(i), children2.get(i).getParent());
                //    }
                //}
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
        for(Tree child2 : children2) {
            Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
            if(receiver2 != null) {
                receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                if(receiver2 != null) {
                    Tree simpleName = TreeUtilFunctions.findChildByType(receiver2, LANG2.SIMPLE_NAME);
                    if(simpleName.getChildren().size() > 0)
                        callNames2.add(simpleName.getChild(0).getLabel());
                    else
                        callNames2.add(simpleName.getLabel());
                }
            }
            else {
                Tree simpleName = TreeUtilFunctions.findChildByType(child2, LANG2.SIMPLE_NAME);
                callNames2.add(simpleName.getLabel());
            }
        }
        if(callNames1.size() <= callNames2.size()) {
            int matches = 0;
            for(int i=0; i<callNames1.size(); i++) {
                String s1 = callNames1.get(i);
                String s2 = callNames2.get(i);
                if(s1.equals(s2) || s1.contains("." + s2)) {
                    matches++;
                }
                else if(s1.equals("url") && s2.equals("toUrl")) {
                    matches++;
                }
                else if(s1.equals("getBytes") && s2.equals("toByteArray")) {
                    matches++;
                }
            }
            if(matches == callNames1.size()) {
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
                else if(s1.equals("url") && s2.equals("toUrl")) {
                    matches++;
                }
                else if(s1.equals("getBytes") && s2.equals("toByteArray")) {
                    matches++;
                }
            }
            if(matches == callNames2.size()) {
                return true;
            }
        }
        if(callNames1.size() > callNames2.size() && callNames1.containsAll(callNames2)) {
            //sort callNames1 based on callNames2
            List<Tree> newChildren1 = new ArrayList<>();
            for(String s : callNames2) {
                int index = callNames1.indexOf(s);
                newChildren1.add(children1.get(index));
            }
            for(String s : callNames1) {
                if(!callNames2.contains(s)) {
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

    private static void processPair(ExtendedMultiMappingStore mappingStore, Tree child1, Tree child2, Constants LANG1, Constants LANG2) {
        mappingStore.addMapping(child1, child2);
        Tree args1 = TreeUtilFunctions.findChildByType(child1, LANG1.METHOD_INVOCATION_ARGUMENTS);
        if(args1 != null) {
            Tree args2 = TreeUtilFunctions.findChildByType(child2, LANG2.CALL_SUFFIX);
            if(args2 != null) {
                args2 = TreeUtilFunctions.findChildByType(args2, LANG2.METHOD_INVOCATION_ARGUMENTS);
                mappingStore.addMapping(args1, args2);
            }
        }
        Tree receiver1 = TreeUtilFunctions.findChildByType(child1, LANG1.METHOD_INVOCATION_RECEIVER);
        if(receiver1 != null) {
            Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
            if(receiver2 != null) {
                receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                mappingStore.addMapping(receiver1, receiver2);
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
                    }
                }
                else if(type.getChild(0).getType().name.equals(LANG1.SIMPLE_NAME)) {
                    Tree receiver2 = TreeUtilFunctions.findChildByType(child2, LANG2.NAVIGATION_EXPRESSION);
                    if(receiver2 != null) {
                        receiver2 = TreeUtilFunctions.findChildByType(receiver2, LANG2.NAVIGATION_SUFFIX);
                        mappingStore.addMapping(type.getChild(0), receiver2);
                    }
                }
            }
        }
    }

    public static void handleFieldDeclarationMapping(ExtendedMultiMappingStore mappingStore, 
            Tree srcAttr, Tree dstAttr, Tree srcFieldDeclaration, Tree dstFieldDeclaration, Constants LANG1, Constants LANG2) {
        Tree variableDeclaration2 = TreeUtilFunctions.findChildByType(dstAttr, LANG2.VARIABLE_DECLARATION);
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
                if(userType2 == null) {
                    Tree constuctorInvocation2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.CONSTRUCTOR_INVOCATION);
                    if(constuctorInvocation2 != null) {
                        stringLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.STRING_LITERAL);
                        numberLiteral2 = TreeUtilFunctions.findChildrenByTypeRecursively(constuctorInvocation2, LANG2.INTEGER_LITERAL);
                        userType2 = TreeUtilFunctions.findChildByType(constuctorInvocation2, LANG2.USER_TYPE);
                    }
                }
                if(typeName1 != null && userType2 != null && userType2.getChildren().size() > 0) {
                    mappingStore.addMapping(typeName1, userType2.getChild(0));
                }
                if(stringLiteral2 != null && stringLiteral1.size() == stringLiteral2.size()) {
                    for(int i=0; i<stringLiteral1.size(); i++) {
                        mappingStore.addMapping(stringLiteral1.get(i), stringLiteral2.get(i));
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
            mappingStore.addMapping(qualifiedName, identifier);
        }
    }

    public static void handlePackageDeclarationMapping(ExtendedMultiMappingStore mappingStore, Tree srcPackageDeclaration, Tree dstPackageDeclaration, Constants LANG1, Constants LANG2) {
        mappingStore.addMapping(srcPackageDeclaration, dstPackageDeclaration);
        Tree packageName = TreeUtilFunctions.findChildByType(srcPackageDeclaration, LANG1.QUALIFIED_NAME);
        if(packageName == null)
            packageName = TreeUtilFunctions.findChildByType(srcPackageDeclaration, LANG1.SIMPLE_NAME);
        Tree identifier = TreeUtilFunctions.findChildByType(dstPackageDeclaration, LANG1.IMPORT_IDENTIFIER);
        if(packageName != null && identifier != null) {
            mappingStore.addMapping(packageName, identifier);
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
    }
}
