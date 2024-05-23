package org.refactoringminer.astDiff.moved;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.actions.classifier.ExtendedOnlyRootsClassifier;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.refactoringminer.astDiff.utils.Helpers.findNameByTree;

/* Created by pourya on 2024-05-16*/
public class MovedDeclarationGenerator extends MovedASTDiffGenerator {
    public MovedDeclarationGenerator(UMLModelDiff modelDiff, ProjectASTDiff projectASTDiff) {
        super(modelDiff, projectASTDiff);
    }

    @Override
    public Map<Pair<String, String>, List<Mapping>> makeFilePairMappings() {
        Map<Pair<String, String>, List<Mapping>> filePairMappings = new LinkedHashMap<>();
        for(ASTDiff diff : projectASTDiff.getDiffSet()) {
            Map<Tree, List<Mapping>> methodDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, List<Mapping>> fieldDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, List<Mapping>> typeDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, Action> actionMap = new LinkedHashMap<Tree, Action>();
            ExtendedOnlyRootsClassifier classifier = (ExtendedOnlyRootsClassifier) diff.createRootNodesClassifier();
            populateMoveMappings(diff, methodDeclarationMappings, fieldDeclarationMappings, typeDeclarationMappings, actionMap, classifier.getSrcMoveOutTreeMap());
            populateMoveMappings(diff, methodDeclarationMappings, fieldDeclarationMappings, typeDeclarationMappings, actionMap, classifier.getMultiMapSrc());
            //group the mappings based on the pair of src and dst files.
            String srcPath = diff.getSrcPath();
            for(Tree key : methodDeclarationMappings.keySet()) {
                if(actionMap.containsKey(key)) {
                    Action action = actionMap.get(key);
                    if(action instanceof MoveOut) {
                        MoveOut moveOut = (MoveOut)action;
                        String dstPath = moveOut.getDstFile();
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(methodDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(methodDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                    else if(action instanceof MultiMove) {
                        MultiMove moveOut = (MultiMove)action;
                        String dstPath = findNameByTree(modelDiff.getChildModel().getTreeContextMap(), moveOut.getParent());
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(methodDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(methodDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                }
            }
            for(Tree key : fieldDeclarationMappings.keySet()) {
                if(actionMap.containsKey(key)) {
                    Action action = actionMap.get(key);
                    if(action instanceof MoveOut) {
                        MoveOut moveOut = (MoveOut)action;
                        String dstPath = moveOut.getDstFile();
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(fieldDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(fieldDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                    else if(action instanceof MultiMove) {
                        MultiMove moveOut = (MultiMove)action;
                        String dstPath = findNameByTree(modelDiff.getChildModel().getTreeContextMap(), moveOut.getParent());
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(fieldDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(fieldDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                }
            }
            for(Tree key : typeDeclarationMappings.keySet()) {
                if(actionMap.containsKey(key)) {
                    Action action = actionMap.get(key);
                    if(action instanceof MoveOut) {
                        MoveOut moveOut = (MoveOut)action;
                        String dstPath = moveOut.getDstFile();
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(typeDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(typeDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                    else if(action instanceof MultiMove) {
                        MultiMove moveOut = (MultiMove)action;
                        String dstPath = findNameByTree(modelDiff.getChildModel().getTreeContextMap(), moveOut.getParent());
                        Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
                        if(filePairMappings.containsKey(pair)) {
                            filePairMappings.get(pair).addAll(typeDeclarationMappings.get(key));
                        }
                        else {
                            List<Mapping> mappings = new ArrayList<Mapping>();
                            mappings.addAll(typeDeclarationMappings.get(key));
                            filePairMappings.put(pair, mappings);
                        }
                    }
                }
            }
        }
        return filePairMappings;
    }

    private void populateMoveMappings(ASTDiff diff, Map<Tree, List<Mapping>> methodDeclarationMappings,
                                      Map<Tree, List<Mapping>> fieldDeclarationMappings, Map<Tree, List<Mapping>> typeDeclarationMappings,
                                      Map<Tree, Action> actionMap, Map<Tree, Action> map) {
        for(Tree src : map.keySet()) {
            Tree methodRoot = TreeUtilFunctions.getParentUntilType(src, Constants.METHOD_DECLARATION);
            populateMappingsRoot(diff, methodDeclarationMappings, actionMap, map, src, methodRoot);
            Tree fieldRoot = TreeUtilFunctions.getParentUntilType(src, Constants.FIELD_DECLARATION);
            populateMappingsRoot(diff, fieldDeclarationMappings, actionMap, map, src, fieldRoot);
            if(src.getType().name.equals(Constants.TYPE_DECLARATION) ||
                    src.getType().name.equals(Constants.ENUM_DECLARATION) ||
                    src.getType().name.equals(Constants.RECORD_DECLARATION)) {
                actionMap.put(src, map.get(src));
                List<Mapping> mappings = new ArrayList<Mapping>();
                mappings.addAll(getMappingForLeft(diff, src));
                typeDeclarationMappings.put(src, mappings);
            }
        }
    }

    private void populateMappingsRoot(ASTDiff diff, Map<Tree, List<Mapping>> elementDeclMappings, Map<Tree, Action> actionMap, Map<Tree, Action> map, Tree src, Tree elementRoot) {
        if(elementRoot != null) {
            if(elementDeclMappings.containsKey(elementRoot)) {
                elementDeclMappings.get(elementRoot).addAll(getMappingForLeft(diff, src));
            }
            else {
                List<Mapping> mappings = new ArrayList<Mapping>();
                mappings.addAll(getMappingForLeft(diff, src));
                elementDeclMappings.put(elementRoot, mappings);
            }
            if(map.containsKey(elementRoot)) {
                actionMap.put(elementRoot, map.get(elementRoot));
            }
        }
    }
    private List<Mapping> getMappingForLeft(ASTDiff diff, Tree left) {
        List<Mapping> matchingMappings = new ArrayList<Mapping>();
        for(Mapping mapping : diff.getAllMappings()) {
            if(mapping.first.equals(left)) {
                matchingMappings.add(mapping);
            }
        }
        return matchingMappings;
    }
}
