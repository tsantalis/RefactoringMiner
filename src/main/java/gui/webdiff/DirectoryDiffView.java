package gui.webdiff;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import static org.rendersnake.HtmlAttributesFactory.*;

public class DirectoryDiffView implements Renderable {
    private final DirComparator comperator;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

    public DirectoryDiffView(DirComparator comperator) {
        this.comperator = comperator;
        for(Map.Entry<String, String> pair : comperator.getModifiedFilesName().entrySet()) {
            String fileName = pair.getValue();
            String[] tokens = fileName.split("/");
            int counter = 1;
            for(String token : tokens) {
                String pathToNode = concatWithSlash(tokens, counter);
                DefaultMutableTreeNode parent = findNode(pathToNode);
                if(!parent.getUserObject().equals(token)) {
                    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(token);
                    parent.add(newChild);
                }
                counter++;
            }
        }
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
            if(treeNode.getUserObject().equals(tokens[index])) {
                lastNode = treeNode;
                index++;
                enumeration = treeNode.children();
            }
        }
        return lastNode != null ? lastNode : root;
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
                                .render_if(new ModifiedFiles(comperator.getModifiedFilesName()), comperator.getModifiedFilesName().size() > 0)
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

    private static class ModifiedFiles implements Renderable {
//        private List<Pair<File, File>> files;

        private Map<String,String> diffInfos;

        private ModifiedFiles(Map<String,String> diffInfos) {
            this.diffInfos = diffInfos;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
            .table(class_("table card-table table-striped table-condensed mb-0"))
                .tbody();

            int id = 0;
            for (Map.Entry<String,String> entry : diffInfos.entrySet())
            {
                String nameBefore = entry.getKey();
                String nameAfter = entry.getValue();
                tbody
                .tr()
//                    .td().content(comparator.getSrc().toAbsolutePath().relativize(file.first.toPath().toAbsolutePath()).toString())
                    .td().content(properText(nameBefore,nameAfter))
                    .td()
                        .div(class_("btn-toolbar justify-content-end"))
                            .div(class_("btn-group"))
                                //TODO: integrate this with the -g option
//                                .if_(TreeGenerators.getInstance().hasGeneratorForFile(file.first.getAbsolutePath()))
                                    .a(class_("btn btn-primary btn-sm").href("/monaco-diff/" + id)).content("MonacoDiff")
                                    .a(class_("btn btn-primary btn-sm").href("/vanilla-diff/" + id)).content("ClassicDiff")
//                                ._if()
//                                .a(class_("btn btn-primary btn-sm").href("/monaco-native-diff/" + id)).content("monaco-native")
//                                .a(class_("btn btn-primary btn-sm").href("/mergely-diff/" + id)).content("mergely")
//                                    .a(class_("btn btn-primary btn-sm").href("/raw-diff/" + id)).content("raw")
                            ._div()
                        ._div()
                    ._td()
                ._tr();
                id++;
            }
            tbody
                ._tbody()
                ._table();
        }

        private String properText(String nameBefore, String nameAfter) {
            if (nameBefore.equals(nameAfter))
                return nameAfter;
            else
                return nameBefore + " -> " + nameAfter;
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
