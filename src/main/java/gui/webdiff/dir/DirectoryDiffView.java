package gui.webdiff.dir;

import gui.webdiff.tree.TreeNodeInfo;
import gui.webdiff.WebDiff;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.MethodLevelRefactoring;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.rendersnake.HtmlAttributesFactory.*;

public class DirectoryDiffView implements Renderable {
    protected final DirComparator comparator;
    private final boolean external;
    protected final DiffMetaInfo metaInfo;

    public DirectoryDiffView(DirComparator comparator, DiffMetaInfo metaInfo) {
        this(comparator, false, metaInfo);
    }

    public DirectoryDiffView(DirComparator comparator, boolean external, DiffMetaInfo metaInfo) {
        this.comparator = comparator;
        this.external = external;
        this.metaInfo = metaInfo;
    }

    protected boolean isMovedCode(TreeNodeInfo info) {
        return comparator.isMoveDiff(info.getId());
    }

    protected boolean isModifiedFile(TreeNodeInfo info) {
        ASTDiff astDiff = comparator.getASTDiff(info.getId());
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
                        .render(new MenuBar(external, metaInfo))
                    ._div()
                    .div(class_("row"))
                    .render(new RefactoringBar(comparator))
                    ._div()
//                .if_(!external)
//                    .div(class_("row justify-content-center"))
//                        .div(class_("col-6 text-center"))
//                            .button(class_("btn btn-primary").style("height: 50px;").onClick("window.location.href='/singleView'"))
//                                .content("Single Page View (Beta)")
//                                .button(class_("btn btn-success").id("compareBtn")
//                                .style("position: fixed; bottom: 20px; right: 20px; display: none; z-index: 1000;")
//                                .onClick("handleCompareClick()")
//                                    )
//                                .content("Compare Selected Files")
//                        ._div()
//                    ._div()
//                ._if()
                .div(class_("row mt-3 mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Modified files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comparator.getModifiedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new ModifiedFiles(comparator), !comparator.getModifiedFilesName().isEmpty())
                            ._div()
                        ._div()
                    ._div()
                    .if_(!external)
                    .div(class_("row mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-danger"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Deleted files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comparator.getRemovedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comparator.getRemovedFilesName(), "deleted"),
                                        !comparator.getRemovedFilesName().isEmpty())
                            ._div()
                        ._div()
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-success"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Added files ")
                                        .span(class_("badge badge-secondary").style("color:black")).content(comparator.getAddedFilesName().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comparator.getAddedFilesName(), "added"),
                                        !comparator.getAddedFilesName().isEmpty())
                            ._div()
                        ._div()
                    ._div()
                    ._if()
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
                    String iconPath = null, description = nodeInfo.getName(), title = "", hoverText = "";
                    int iconWidth = 0, iconHeight = 0;
                    boolean _checkBox = false;
                    if(isModifiedFile(nodeInfo)) {
                        _checkBox = true;
                        iconPath = "dist/icons8-file-edit.svg";
                        iconWidth = 15;
                        iconHeight = 17;
                        title = "modified file";
                        ASTDiff astDiff = comparator.getASTDiff(nodeInfo.getId());
                        if (astDiff != null)
                            hoverText = astDiff.getSrcPath();
                    }
                    else if(isMovedCode(nodeInfo)) {
                        iconPath = "dist/file-transfer.svg";
                        iconWidth = 22;
                        iconHeight = 28;
                        title = "moved code between files";
                        ASTDiff astDiff = comparator.getASTDiff(nodeInfo.getId());
                        if(astDiff != null && astDiff.getSrcPath() != null) {
                            String srcName = astDiff.getSrcPath();
                            if(astDiff.getSrcPath().contains("/")) {
                                srcName = srcName.substring(srcName.lastIndexOf("/") + 1, srcName.length());
                                hoverText = astDiff.getSrcPath();
                            }
                            if(!srcName.equals(nodeInfo.getName())) {
                                //file is renamed
                                description = srcName + " ⇨ " + nodeInfo.getName();
                                hoverText = astDiff.getSrcPath() + " ⇨ " + astDiff.getDstPath();
                            }
                    	}
                    }
                    else {
                    	_checkBox = true;
                        iconPath = "dist/icons8-file-move.svg";
                        iconWidth = 15;
                        iconHeight = 17;
                        title = "moved/renamed file";
                        ASTDiff astDiff = comparator.getASTDiff(nodeInfo.getId());
                        if(astDiff != null && astDiff.getSrcPath() != null) {
                            String srcName = astDiff.getSrcPath();
                            if(astDiff.getSrcPath().contains("/")) {
                                srcName = srcName.substring(srcName.lastIndexOf("/") + 1, srcName.length());
                                hoverText = astDiff.getSrcPath();
                            }
                            if(!srcName.equals(nodeInfo.getName())) {
                                //file is renamed
                                description = srcName + " → " + nodeInfo.getName();
                                hoverText = astDiff.getSrcPath() + " → " + astDiff.getDstPath();
                            }
                        }
                    }
                    boolean empty = comparator.getASTDiff(nodeInfo.getId()).isEmpty();
                    if(!empty) {
                        ASTDiff astDiff = comparator.getASTDiff(nodeInfo.getId());
                        ul.tr()
                        .td(style("white-space: normal; word-wrap: break-word; word-break: break-all;"))
                        .if_(!external && _checkBox)
                        .input(type("checkbox")
                                .name("fileSelect").value(nodeInfo.getId())
                                .data("id", nodeInfo.getId())
                                .data("path", astDiff.getSrcPath())
                                .data("type", "modified"))
                        ._if()
                        .a(id("diff_row_" + nodeInfo.getId()).href("/monaco-page/" + nodeInfo.getId()))
                        .img(src(iconPath).width(iconWidth).height(iconHeight).title(title))
                        .span(title(hoverText))
                        .write(" " + description)
                        ._span()
                        ._a()
                        ._td()
                        .if_(!external)
                        .td()
                        .div(class_("btn-toolbar justify-content-end"))
                        .div(class_("btn-group"))
                        .a(class_("btn btn-primary btn-sm").href("/monaco-page/" + nodeInfo.getId())).content("MonacoDiff")
                        .a(class_("btn btn-primary btn-sm").href("/vanilla-diff/" + nodeInfo.getId())).content("ClassicDiff")
                        ._div() // Close btn-group
                        ._div() // Close btn-toolbar
                        ._td()
                        ._if()
                        ._tr();
                    }
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
        private final String tag;
        private final Set<String> files;

        private AddedOrDeletedFiles(Set<String> files, String tag) {
            this.files = files;
            this.tag = tag;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
                    .div(class_("row").style("--bs-gutter-x:0"))
                    .div(class_("col-md-12"))
                    .table(class_("table card-table table-striped table-condensed mb-0"))
                    .tbody();
            for (String filename : files) {
                html
                        .tr()
                        .td()
                        .input(type("checkbox").name("fileSelect").value(filename)
                        .data("path", filename)
                        .data("type", tag))

                        .write(" " + filename)
                        ._td()
                        ._tr();
            }
            html
                    ._tbody()
                    ._table()
                    ._div()
                    ._div();
        }
    }

    static class Header implements Renderable {
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
                        .macros().javascript(WebDiff.HIGHLIGHT_JS_URL)
                        //.macros().javascript(WebDiff.HIGHLIGHT_JAVA_URL)
                        .macros().stylesheet(WebDiff.HIGHLIGHT_CSS_URL)
                        .macros().javascript("/dist/shortcuts.js")
                        .macros().javascript("/dist/dircompare.js")
                        .macros().stylesheet("/dist/style.css")
                     ._head();
        }
    }

    private static class MenuBar implements Renderable {
        private final boolean external;
        private final DiffMetaInfo metaInfo;


        public MenuBar(boolean external, DiffMetaInfo metaInfo) {
            this.external = external;
            this.metaInfo = metaInfo;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .if_(metaInfo != null)
                    .div(class_("col"))
                    .a(href(metaInfo.getUrl()).target("_blank")).content(metaInfo.getInfo())
                    ._div()
                    ._if()
                    .div(class_("btn-group"))
                        .if_(!external)
                        .button(class_("btn btn-primary").onClick("window.location.href='/singleView'"))
                        .content("Single Page View")
                        .button(class_("btn btn-success").id("compareBtn")
                        .style("position: fixed; bottom: 20px; right: 20px; display: none; z-index: 1000;")
                        .onClick("handleCompareClick()")
                            )
                        .content("Compare Selected Files")
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                        ._if()
                        .if_(external)
                        .a(class_("btn btn-default btn-sm btn-danger").href("/list")).content("Back")
                        ._if()
                    ._div()
                ._div()
            ._div();
        }
    }

    private static class RefactoringBar implements Renderable {
        private final List<Refactoring> refactorings;
        private final Map<Pair<String, String>, Integer> filePathPairToDiffId = new LinkedHashMap<>();
        private final Map<String, Set<Refactoring>> refactoringTypeMap = new LinkedHashMap<>();
        public RefactoringBar(DirComparator comparator) {
            this.refactorings = comparator.getRefactorings();
            for(Refactoring r : refactorings) {
            	String type = r.getRefactoringType().getDisplayName();
            	if(refactoringTypeMap.containsKey(type)) {
            		refactoringTypeMap.get(type).add(r);
            	}
            	else {
            		Set<Refactoring> set = new LinkedHashSet<>();
            		set.add(r);
            		refactoringTypeMap.put(type, set);
            	}
            }
            int counter = 0;
            for(ASTDiff diff : comparator.getDiffs()) {
                Pair<String, String> pair = Pair.of(diff.getSrcPath(), diff.getDstPath());
                filePathPairToDiffId.put(pair, counter);
                counter++;
            }
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .a(class_("btn btn-primary").href("#collapseRefactorings").role("button")
                    .add("data-bs-toggle", "collapse")
                    .add("aria-expanded", "false")
                    .add("aria-controls", "collapseRefactorings")
                    ).content("Refactorings")
                .div(class_("row").style("--bs-gutter-x:0"))
                    .div(class_("col"))
                        .div(class_("collapse").id("collapseRefactorings"))
                        .div(class_("card card-body"));
                        //create checkboxes for refactoring types
                        for(String refactoringType : refactoringTypeMap.keySet()) {
                            int instances = refactoringTypeMap.get(refactoringType).size();
                            html
                            .div(class_("form-check"))
                            .input(class_("form-check-input").type("checkbox").id("refactoringType").value("").checked(""))
                            .label(class_("form-check-label").for_("refactoringType")).write(refactoringType + " (" + instances + ")")
                            ._label()
                            ._div();
                        }
                    html.ol(class_("list-group list-group-numbered"));
                        for(Refactoring r : refactorings) {
                        	Set<ImmutablePair<String, String>> before = r.getInvolvedClassesBeforeRefactoring();
                        	Set<ImmutablePair<String, String>> after = r.getInvolvedClassesAfterRefactoring();
                        	Iterator<ImmutablePair<String, String>> beforeIterator = before.iterator();
                        	ImmutablePair<String, String> beforeIteratorNext = beforeIterator.next();
                        	String filePathBefore = beforeIteratorNext.left;
                        	String classNameBefore = beforeIteratorNext.right;
                        	Iterator<ImmutablePair<String, String>> afterIterator = after.iterator();
                        	ImmutablePair<String, String> afterIteratorNext = afterIterator.next();
                        	String filePathAfter = afterIteratorNext.left;
                        	String classNameAfter = afterIteratorNext.right;
                        	//for Extract and Move Method take the second
                        	if(r.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION) && afterIterator.hasNext()) {
                        		afterIteratorNext = afterIterator.next();
                        		filePathAfter = afterIteratorNext.left;
                        		classNameAfter = afterIteratorNext.right;
                        	}
                        	Pair<String, String> pair = Pair.of(filePathBefore, filePathAfter);
                        	int id = -1;
                        	if(filePathPairToDiffId.containsKey(pair)) {
                        		id = filePathPairToDiffId.get(pair);
                        	}
                        	String description = r.toString();
                        	String refactoringTypeWithBold = "<b>" + r.getRefactoringType().getDisplayName() + "</b>";
                        	description = description.replace(r.getRefactoringType().getDisplayName(), refactoringTypeWithBold);
                        	if(id != -1) {
                        		if(description.contains(classNameBefore)) {
                        			String classNameWithLink = "<a href=\"" + "/monaco-page/" + id + "\">" + classNameBefore + "</a>"; 
                        			description = description.replace(classNameBefore, classNameWithLink);
                        		}
                        		if(description.contains(classNameAfter) && !classNameBefore.equals(classNameAfter)) {
                        			String classNameWithLink = "<a href=\"" + "/monaco-page/" + id + "\">" + classNameAfter + "</a>"; 
                        			description = description.replace(classNameAfter, classNameWithLink);
                        		}
                        	}
                        	Set<String> processed = new LinkedHashSet<>();
                        	for(CodeRange range : r.leftSide()) {
                        		String codeElement = range.getCodeElement();
								if(codeElement != null && description.contains(codeElement) && !processed.contains(codeElement)) {
                        			String codeElementTag = "<code>" + codeElement + "</code>";
                        			description = description.replace(codeElement, codeElementTag);
                        			processed.add(codeElement);
                        		}
                        	}
                        	for(CodeRange range : r.rightSide()) {
                        		String codeElement = range.getCodeElement();
								if(codeElement != null && description.contains(codeElement) && !processed.contains(codeElement)) {
                        			String codeElementTag = "<code>" + codeElement + "</code>";
                        			description = description.replace(codeElement, codeElementTag);
                        			processed.add(codeElement);
                        		}
                        	}
                        	if(r instanceof MethodLevelRefactoring) {
                        		MethodLevelRefactoring ref = (MethodLevelRefactoring)r;
                        		String operationBefore = ref.getOperationBefore().toString();
                        		if(operationBefore != null && description.contains(operationBefore) && !processed.contains(operationBefore)) {
                        			String codeElementTag = "<code>" + operationBefore + "</code>";
                        			description = description.replace(operationBefore, codeElementTag);
                        			processed.add(operationBefore);
                        		}
                        		String operationAfter = ref.getOperationAfter().toString();
                        		if(operationAfter != null && description.contains(operationAfter) && !processed.contains(operationAfter)) {
                        			String codeElementTag = "<code>" + operationAfter + "</code>";
                        			description = description.replace(operationAfter, codeElementTag);
                        			processed.add(operationAfter);
                        		}
                        	}
                            html.li(class_("list-group-item")).write(description, NO_ESCAPE)
                            ._li();
                        }
                    html._ol()
                        ._div()
                        ._div()
                    ._div()
                ._div()
            ._div();
        }
    }
}
