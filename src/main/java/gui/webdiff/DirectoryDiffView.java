package gui.webdiff;

import org.refactoringminer.astDiff.models.ASTDiff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.rendersnake.HtmlAttributesFactory.*;

public class DirectoryDiffView implements Renderable {
    private final DirComparator comperator;

    public DirectoryDiffView(DirComparator comperator) {
        this.comperator = comperator;
    }

    private boolean isModifiedFile(TreeNodeInfo info) {
    	ASTDiff astDiff = comperator.getASTDiff(info.getId());
    	if(astDiff != null && astDiff.getSrcPath() != null)
    		return astDiff.getSrcPath().equals(astDiff.getDstPath());
    	return false;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new Header())
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBar())
                    ._div()
                    .div(class_("row mt-3 mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Modified files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comperator.getModifiedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new ModifiedFiles(comperator), !comperator.getModifiedFilesName().isEmpty())
                            ._div()
                        ._div()
                    ._div()
                    .div(class_("row mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-danger"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Deleted files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comperator.getRemovedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comperator.getRemovedFilesName()),
                                        comperator.getRemovedFilesName().size() > 0)
                            ._div()
                        ._div()
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-success"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Added files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comperator.getAddedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comperator.getAddedFilesName()),
                                        comperator.getAddedFilesName().size() > 0)
                            ._div()
                        ._div()
                    ._div()
                ._div()
            ._body()
        ._html();
    }

    private class ModifiedFiles implements Renderable {
        private final DefaultMutableTreeNode root;

        private ModifiedFiles(DirComparator comparator) {
            this.root = comparator.getCompressedTree();
        }

        private void renderNode(HtmlCanvas ul, DefaultMutableTreeNode node) throws IOException {
            if (node == null) {
                return;
            }

            // Start a list item for this node
            HtmlCanvas li = null;
            if (!node.isLeaf())
                li = ul.li().details(add("open", "true", false));

            // Content of the current node
            if (node.getUserObject() != null) {
                TreeNodeInfo nodeInfo = (TreeNodeInfo) node.getUserObject();
                if (node.isLeaf()) {
                	String iconPath = null, description = nodeInfo.getName();
                	int iconWidth = 0, iconHeight = 0;
                	if(isModifiedFile(nodeInfo)) {
                		iconPath = "dist/icons8-file-edit.svg";
                		iconWidth = 15;
                		iconHeight = 17;
                	}
                	else {
                		iconPath = "dist/icons8-file-move.svg";
                		iconWidth = 15;
                		iconHeight = 17;
                		ASTDiff astDiff = comperator.getASTDiff(nodeInfo.getId());
                    	if(astDiff != null && astDiff.getSrcPath() != null) {
                    		String srcName = astDiff.getSrcPath();
                    		if(astDiff.getSrcPath().contains("/")) {
                    			srcName = srcName.substring(srcName.lastIndexOf("/") + 1, srcName.length());
                    		}
                    		if(!srcName.equals(nodeInfo.getName())) {
                    			//file is renamed
                    			description = srcName + " → " + nodeInfo.getName();
                    		}
                    	}
                	}
                    ul.tr()
                            //.td().content(nodeInfo.getName())
                    		.td().a(href("/monaco-diff/" + nodeInfo.getId())).img(src(iconPath).width(iconWidth).height(iconHeight)).write(" " + description)._a()._td()
                            .td()
                            .div(class_("btn-toolbar justify-content-end"))
                            .div(class_("btn-group"))
                            .a(class_("btn btn-primary btn-sm").href("/monaco-diff/" + nodeInfo.getId())).content("MonacoDiff")
                            .a(class_("btn btn-primary btn-sm").href("/vanilla-diff/" + nodeInfo.getId())).content("ClassicDiff")
                            ._div()
                            ._div()
                            ._td()
                    ._tr();
                }
                else {
                    li.summary().content(nodeInfo.getName());
                }
            }

            // Increment the ID for the next node

            // Check if this node has children
            if (!node.isLeaf()) {
                // Start a nested list for the children
                HtmlCanvas childUl = li.ul();
                for (int i = 0; i < node.getChildCount(); i++) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    if (childNode.isLeaf())
                        childUl.table(class_("table card-table table-striped table-condensed mb-0"))
                            .tbody();
                    // Recursively render the child node
                    renderNode(childUl, childNode);
                    if (childNode.isLeaf())
                        childUl._tbody()._table();
                }
                // End the nested list
                childUl._ul();
            }

            // End the list item
            if (li != null)
                li._details()._li();
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            if (root != null) {
                // Start the outer list
                HtmlCanvas ul = html.div(class_("tree-root")).ul();

                // Recursively process each node
                renderNode(ul, root);

                // Close the list and div
                ul._ul()._div();
            }
        }
    }

    private static class AddedOrDeletedFiles implements Renderable {
        private Set<String> files;

        private AddedOrDeletedFiles(Set<String> files) {
            this.files = files;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
            .table(class_("table card-table table-striped table-condensed mb-0"))
                .tbody();
            for (String filename : files) {
                tbody
                    .tr()
                        .td().content(filename)
                    ._tr();
            }
            tbody
                ._tbody()
            ._table();
        }
    }

    private static class Header implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
             html
                     .head()
                        .meta(charset("utf8"))
                        .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                        .title().content("RefactoringMiner")
                        .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                        .macros().javascript(WebDiff.JQUERY_JS_URL)
                        .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                        .macros().javascript("/dist/shortcuts.js")
                        .macros().stylesheet("/dist/style.css")
                     ._head();
        }
    }

    private static class MenuBar implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
    }
}
