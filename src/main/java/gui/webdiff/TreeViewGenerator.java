package gui.webdiff;

import org.refactoringminer.astDiff.models.ASTDiff;

import com.github.gumtreediff.utils.Pair;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.List;

/* Created by pourya on 2024-04-16*/
public class TreeViewGenerator {
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TreeNodeInfo("", ""));
    private final DefaultMutableTreeNode compressedTree = new DefaultMutableTreeNode(new TreeNodeInfo("", ""));
    private final List<ASTDiff> diffs;

    public DefaultMutableTreeNode getCompressedTree() {
        return compressedTree;
    }

    public TreeViewGenerator(List<Pair<String,String>> modifiedFilesName, List<ASTDiff> diffs){
        this.diffs = diffs;
        for(Pair<String, String> pair : modifiedFilesName) {
            String fileName = pair.second;
            String[] tokens = fileName.split("/");
            int counter = 1;
            for(String token : tokens) {
                String pathToNode = concatWithSlash(tokens, counter);
                DefaultMutableTreeNode parent = findNode(pathToNode);
                TreeNodeInfo parentNodeInfo = (TreeNodeInfo) parent.getUserObject();
                if(!parentNodeInfo.getName().equals(token)) {
                    TreeNodeInfo nodeInfo = new TreeNodeInfo(token, pathToNode);
                    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(nodeInfo);
                    parent.add(newChild);
                }
                counter++;
            }
        }
        compressNode(compressedTree, root);
    }
    private static String concatWithSlash(String[] tokens, int numberOfTokensToConcat) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for(String token : tokens) {
            if(index < numberOfTokensToConcat) {
                sb.append(token);
            }
            if(index < numberOfTokensToConcat - 1) {
                sb.append("/");
            }
            index++;
        }
        return sb.toString();
    }

    private DefaultMutableTreeNode findNode(String pathToNode) {
        String[] tokens = pathToNode.split("/");
        Enumeration<TreeNode> enumeration = root.children();
        int index = 0;
        DefaultMutableTreeNode lastNode = null;
        while(enumeration.hasMoreElements() && index < tokens.length) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)enumeration.nextElement();
            TreeNodeInfo treeNodeInfo = (TreeNodeInfo) treeNode.getUserObject();
            if(treeNodeInfo.getName().equals(tokens[index])) {
                lastNode = treeNode;
                index++;
                enumeration = treeNode.children();
            }
        }
        return lastNode != null ? lastNode : root;
    }

    private void compressNode(DefaultMutableTreeNode newNode, DefaultMutableTreeNode oldNode) {
        Enumeration<TreeNode> enumeration = oldNode.children();
        int childCount = oldNode.getChildCount();
        if(childCount == 1) {
            while(enumeration.hasMoreElements()) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)enumeration.nextElement();
                setIdAccordingly(treeNode);
                TreeNodeInfo treeNodeInfo = (TreeNodeInfo) treeNode.getUserObject();
                String nodeName = treeNodeInfo.getName();
                if(!nodeName.endsWith(".java")) {
                    if(oldNode.isRoot()) {
                        TreeNodeInfo newNodeInfo = new TreeNodeInfo(nodeName, nodeName);
                        newNode.setUserObject(newNodeInfo);
                    }
                    else {
                        TreeNodeInfo newNodeInfo = (TreeNodeInfo) newNode.getUserObject();
                        TreeNodeInfo updatedNodeInfo = new TreeNodeInfo(newNodeInfo.getName() + "/" + nodeName, newNodeInfo.getFullPath() + "/" + nodeName);
                        newNode.setUserObject(updatedNodeInfo);
                    }
                    compressNode(newNode, treeNode);
                }
                else {
                    // this node is a leaf
                    newNode.add(treeNode);
                }
            }
        }
        else {
            while(enumeration.hasMoreElements()) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)enumeration.nextElement();
                setIdAccordingly(treeNode);
                DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(treeNode.getUserObject());
                newNode.add(newChild);
                compressNode(newChild, treeNode);
            }
        }
    }

    private void setIdAccordingly(DefaultMutableTreeNode treeNode) {
        if (!treeNode.isLeaf()) return;
        TreeNodeInfo nodeInfo = (TreeNodeInfo) treeNode.getUserObject();
        for (int i = 0; i < diffs.size(); i++) {
            if (diffs.get(i).getDstPath().equals(nodeInfo.getFullPath())) {
                nodeInfo.setId(i);
                break;
            }
        }
    }
}
