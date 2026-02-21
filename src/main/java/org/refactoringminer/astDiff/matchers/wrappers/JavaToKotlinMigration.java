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
        List<Tree> children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.SIMPLE_NAME);
        if(children2.size() > 0 && children2.get(children2.size()-1).getLabel().equals("code")) {
            //remove .code on character literals to convert to int
            children2.remove(children2.size()-1);
        }
        List<Tree> types1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.SIMPLE_TYPE);
        List<Tree> qualifiedNames1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.QUALIFIED_NAME);
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
                    if(qualifiedType.contains(child2.getLabel())) {
                        toBeRemoved2.add(child2);
                    }
                }
            }
            children2.removeAll(toBeRemoved2);
        }
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                if(children2.get(i).getChildren().size() > 0)
                    mappingStore.addMapping(children1.get(i), children2.get(i).getChild(0));
                else
                    mappingStore.addMapping(children1.get(i), children2.get(i));
            }
        }
        else if(children1.size() > children2.size() && firstChild1 != null && firstChild1.getParent().getType().name.equals(LANG1.SIMPLE_TYPE)) {
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
        children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION_ARGUMENTS);
        children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION_ARGUMENTS);
        if(children1.size() == children2.size()) {
            for(int i=0; i<children1.size(); i++) {
                mappingStore.addMapping(children1.get(i), children2.get(i));
                mappingStore.addMapping(children1.get(i).getParent(), children2.get(i).getParent());
            }
        }
        else {
            children1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.METHOD_INVOCATION, LANG1.CLASS_INSTANCE_CREATION);
            children2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.METHOD_INVOCATION);
            if(dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION))
                children2.add(0, dstStatementNode);
            if(children1.size() == children2.size()) {
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
            }
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
            Tree classModifiers2 = TreeUtilFunctions.findChildByType(dstClassAnnotationTree, LANG2.CLASS_MODIFIER);
            if(classModifiers2 != null) {
                Tree typeName1 = TreeUtilFunctions.findChildByType(srcClassAnnotationTree, LANG1.SIMPLE_NAME);
                Tree userType2 = TreeUtilFunctions.findChildByType(classModifiers2, LANG2.USER_TYPE);
                if(typeName1 != null && userType2 != null && userType2.getChildren().size() > 0) {
                    mappingStore.addMapping(typeName1, userType2.getChild(0));
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
