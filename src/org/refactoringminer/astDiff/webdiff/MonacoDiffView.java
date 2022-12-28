package org.refactoringminer.astDiff.webdiff;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.actions.TreeClassifier;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoDiffView implements Renderable {
    private String srcFileName;
    private String dstFileName;

    private ASTDiff diff;

    private int id;

    public MonacoDiffView(String srcFileName, String dstFileName,  String srcFileContent, String dstFileContent, ASTDiff diff, int id, boolean dump) {
        this.srcFileName = srcFileName;
        this.dstFileName = dstFileName;
        this.diff = diff;
        this.id = id;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
    html
        .render(DocType.HTML5)
        .html(lang("en").class_("h-100"))
            .render(new Header())
            .body(class_("h-100").style("overflow: hidden;"))
                .div(class_("container-fluid h-100"))
                    .div(class_("row"))
                        .render(new MenuBar())
                    ._div()
                    .div(class_("row h-100"))
                        .div(class_("col-6 h-100"))
                            .h5().content(srcFileName)
                            .div(id("left-container").style("height: calc(100% - 80px); border:1px solid grey;"))._div()
                        ._div()
                        .div(class_("col-6 h-100"))
                            .h5().content(dstFileName)
                            .div(id("right-container").style("height: calc(100% - 80px); border:1px solid grey;"))._div()
                        ._div()
                    ._div()
                ._div()
                .macros().script("config = { file: \"" + srcFileName + "\", left: " + getLeftJsConfig()
                                 + ", right: " + getRightJsConfig()
                                 + ", mappings: " + getMappingsJsConfig() + "};")
                .macros().javascript("/monaco/min/vs/loader.js")
                .macros().javascript("/dist/monaco.js")
                .macros().javascript("/dist/shortcuts.js")
            ._body()
        ._html();
    }

    private String getLeftJsConfig() {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/left/" + id + "\"").append(",");
        b.append("ranges: [");
        for (Tree t: diff.getSrcTC().getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t))
                appendRange(b, t, "moved", null);
            if (c.getUpdatedSrcs().contains(t))
                appendRange(b, t, "updated", null);
            if (c.getDeletedSrcs().contains(t))
                appendRange(b, t, "deleted", null);
            if (c.getSrcMoveOutTreeMap().containsKey(t))
                appendRange(b,t,"moveOut", c.getSrcMoveOutTreeMap().get(t).toString());
            if (c.getMultiMapSrc().containsKey(t))
            {
                String tag = "mm";
                boolean _isUpdated = ((MultiMove)(c.getMultiMapSrc().get(t))).isUpdated();
                if (_isUpdated) {
                    tag += " updOnTop";
                }
                appendRange(b,t,tag,null);
            }
        }
        b.append("]").append(",");
        b.append("}");
        return b.toString();
    }

    private String getRightJsConfig() {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/right/" + id + "\"").append(",");
        b.append("ranges: [");
        for (Tree t: diff.getDstTC().getRoot().preOrder()) {
            if (c.getMovedDsts().contains(t))
                appendRange(b, t, "moved",null);
            if (c.getUpdatedDsts().contains(t))
                appendRange(b, t, "updated",null);
            if (c.getInsertedDsts().contains(t))
                appendRange(b, t, "inserted",null);
            if (c.getDstMoveInTreeMap().containsKey(t))
                appendRange(b,t,"moveIn",c.getDstMoveInTreeMap().get(t).toString());
            if (c.getMultiMapDst().containsKey(t))
            {
                String tag = "mm";
                boolean _isUpdated = ((MultiMove)(c.getMultiMapDst().get(t))).isUpdated();
                if (_isUpdated)
                {
                    tag += " updOnTop";
                }
                appendRange(b,t,tag,null);
            }

        }
        b.append("]").append(",");
        b.append("}");
        return b.toString();
    }

    private String getMappingsJsConfig() {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("[");
        for (Tree t: diff.getSrcTC().getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                Tree d = diff.getMappings().getDstForSrc(t).iterator().next();
                b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
            }
        }
        b.append("]").append(",");
        return b.toString();
    }

    private void appendRange(StringBuilder b, Tree t, String kind, String tip) {
        String tooltip = tooltip(t);
        if (tip != null) tooltip = tip;
        b.append("{")
                .append("from: ").append(t.getPos())
                .append(",").append("to: ").append(t.getEndPos()).append(",")
                .append("index: ").append(t.getMetrics().depth).append(",")
                .append("kind: ").append("\"" + kind + "\"").append(",")
                .append("tooltip: ").append("\"" + tooltip + "\"").append(",")
                .append("}").append(",");
    }

    private static String tooltip(Tree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }

    private static class MenuBar implements Renderable {

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col")).content("Generated by RefactoringMiner")
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .div(class_("btn-group mr-2"))
                        .button(class_("btn btn-primary btn-sm").id("legend")
                                .add("data-bs-container", "body")
                                .add("data-bs-toggle", "popover")
                                .add("data-bs-placement", "bottom")
                                .add("data-bs-html", "true")
                                .add("data-bs-content", "<span class=&quot;deleted&quot;>&nbsp;&nbsp;</span> deleted<br>"
                                        + "<span class=&quot;inserted&quot;>&nbsp;&nbsp;</span> inserted<br>"
                                        + "<span class=&quot;moved&quot;>&nbsp;&nbsp;</span> moved<br>"
                                        + "<span class=&quot;updated&quot;>&nbsp;&nbsp;</span> updated<br>", false)
                        ).content("Legend")
                        .button(class_("btn btn-primary btn-sm").id("shortcuts")
                                .add("data-bs-toggle", "popover")
                                .add("data-bs-placement", "bottom")
                                .add("data-bs-html", "true")
                                .add("data-bs-content", "<b>q</b> quit<br><b>l</b> list<br>"
                                        + "<b>t</b> top<br><b>b</b> bottom", false)
                        )
                        .content("Shortcuts")
                    ._div()
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content("Back")
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
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
                    .macros().stylesheet("/dist/monaco.css")
                    .macros().javascript(WebDiff.JQUERY_JS_URL)
                    .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                ._head();
        }
    }
}
