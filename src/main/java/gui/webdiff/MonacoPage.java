package gui.webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoPage extends AbstractDiffView implements Renderable {
    public MonacoPage(String toolName, String srcFileName, String dstFileName, Diff diff, int id, int numOfDiffs, String routePath, boolean isMovedDiff) {
        super(toolName, srcFileName, dstFileName, diff, id, numOfDiffs, routePath, isMovedDiff);
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
                    .render(new AbstractMenuBar(toolName, routePath, id, numOfDiffs, isMovedDiff){
                        @Override
                        public String getShortcutDescriptions() {
                            return super.getShortcutDescriptions() + "<b>Alt + w</b> toggle word wrap";
                        }
                        @Override
                        public String getLegendValue() {
                            return "<span class=&quot;deleted&quot;>&nbsp;&nbsp;</span> deleted<br>"
                                    + "<span class=&quot;inserted&quot;>&nbsp;&nbsp;</span> inserted<br>"
                                    + "<span class=&quot;moved&quot;>&nbsp;&nbsp;</span> moved<br>"
                                    + "<span class=&quot;updated&quot;>&nbsp;&nbsp;</span> updated<br>";
                        }
                    })
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
                .macros().script("config = { moved: " + isMovedDiff + ", file: \"" + srcFileName + "\", left: " + getLeftJsConfig()
                                 + ", right: " + getRightJsConfig()
                                 + ", mappings: " + getMappingsJsConfig() + "};")
                .macros().javascript("/monaco/min/vs/loader.js")
                .macros().javascript("/dist/monaco.js")
                .macros().javascript("/dist/shortcuts.js")
            ._body()
        ._html();
    }

    private String getLeftJsConfig() {
        if (diff instanceof ASTDiff) {
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/left/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t : diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t))
                    appendRange(b, t, "moved", null);
                if (c.getUpdatedSrcs().contains(t))
                    appendRange(b, t, "updated", null);
                if (c.getDeletedSrcs().contains(t))
                    appendRange(b, t, "deleted", null);
                if (c.getSrcMoveOutTreeMap().containsKey(t))
                    appendRange(b, t, "moveOut", c.getSrcMoveOutTreeMap().get(t).toString());
                if (c.getMultiMapSrc().containsKey(t)) {
                    String tag = "mm";
                    boolean _isUpdated = ((MultiMove) (c.getMultiMapSrc().get(t))).isUpdated();
                    if (_isUpdated) {
                        tag += " updOnTop";
                    }
                    appendRange(b, t, tag, null);
                }
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
        else {
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/left/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t: diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t))
                    appendRange(b, t, "moved","");
                if (c.getUpdatedSrcs().contains(t))
                    appendRange(b, t, "updated","");
                if (c.getDeletedSrcs().contains(t))
                    appendRange(b, t, "deleted","");
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
    }

    private String getRightJsConfig() {
        if (diff instanceof ASTDiff) {
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/right/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t : diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t))
                    appendRange(b, t, "moved", null);
                if (c.getUpdatedDsts().contains(t))
                    appendRange(b, t, "updated", null);
                if (c.getInsertedDsts().contains(t))
                    appendRange(b, t, "inserted", null);
                if (c.getDstMoveInTreeMap().containsKey(t))
                    appendRange(b, t, "moveIn", c.getDstMoveInTreeMap().get(t).toString());
                if (c.getMultiMapDst().containsKey(t)) {
                    String tag = "mm";
                    boolean _isUpdated = ((MultiMove) (c.getMultiMapDst().get(t))).isUpdated();
                    if (_isUpdated) {
                        tag += " updOnTop";
                    }
                    appendRange(b, t, tag, null);
                }
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
        else{
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/right/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t: diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t))
                    appendRange(b, t, "moved","");
                if (c.getUpdatedDsts().contains(t))
                    appendRange(b, t, "updated","");
                if (c.getInsertedDsts().contains(t))
                    appendRange(b, t, "inserted","");
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
    }

    private String getMappingsJsConfig() {
        if (diff instanceof ASTDiff) {
            ASTDiff astDiff = (ASTDiff) diff;
            MappingStore monoMappingStore = astDiff.getAllMappings().getMonoMappingStore();
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("[");
            for (Tree t : diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                    Tree d = ((ASTDiff)diff).getAllMappings().getDsts(t).iterator().next();
                    b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
                }
                else {
                    if (monoMappingStore.isSrcMapped(t)) {
                        b.append(String.format("[%s, %s, %s, %s], ",
                                t.getPos(),
                                t.getEndPos(),
                                monoMappingStore.getDstForSrc(t).getPos(),
                                monoMappingStore.getDstForSrc(t).getEndPos()));
                    }
                }
            }
            b.append("]").append(",");
            return b.toString();
        }
        else {
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("[");
            for (Tree t: diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                    Tree d = diff.mappings.getDstForSrc(t);
                    b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
                }
            }
            b.append("]").append(",");
            return b.toString();
        }
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
                    ? t.getParent().getType() + "/" + t.getType() + "/" + t.getPos() + "/" +  t.getEndPos() : t.getType().toString() + t.getPos() + t.getEndPos();
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
