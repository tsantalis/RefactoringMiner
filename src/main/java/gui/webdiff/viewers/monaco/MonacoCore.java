package gui.webdiff.viewers.monaco;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.rendersnake.HtmlCanvas;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-05*/
public class MonacoCore {
    private boolean showFilenames;
    private final Diff diff;
    private final int id;
    private final boolean isMoved;
    private String srcFileName;
    private String dstFileName;

    public MonacoCore(Diff diff, int id, boolean isMovedDiff) {
        this.showFilenames = true;
        this.diff = diff;
        this.id = id;
        this.isMoved = isMovedDiff;
        if (diff instanceof ASTDiff){
            this.srcFileName = ((ASTDiff) diff).getSrcPath();
            this.dstFileName = ((ASTDiff) diff).getDstPath();
        }
    }

    public void setShowFilenames(boolean showFilenames) {
        this.showFilenames = showFilenames;
    }

    public void addDiffContainers(HtmlCanvas html) throws IOException {
        int offset = (showFilenames) ? 80 : 0;
        html.div(class_("row h-100"));

        html.div(class_("col-6 h-100"));
        if (showFilenames) {
            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
                    .content(srcFileName);
        }
        String heightFormula = "";
        if (showFilenames)
            heightFormula = "height: calc(100% - " + offset + "px);";
        html.div(class_("edc").id(getLeftContainerId()).style(heightFormula + "border: 1px solid grey; overflow: auto;"))._div()
                ._div();

        html.div(class_("col-6"));
        if (showFilenames) {
            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
                    .content(dstFileName);
        }

        html.div(class_("edc").id(getRightContainerId()).style(heightFormula + "border: 1px solid grey; overflow: auto;"))._div()
                ._div();
        String code = "monaco(" + makeDiffConfig() + ");";
        html.macros().script(code); // Pass the config to the main function


//
//        html.div(class_("row h-100"))
//                .div(class_("col-6 h-100"));
//        int offset = (showFilenames) ? 80 : 0;
//        if (showFilenames) {
//            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
//                    .content(srcFileName);
//        }
//
//        String heightFormula = "height: calc(100% - " + offset + "px);";
//        html.div(id(getLeftContainerId()).style(heightFormula + "border:1px solid grey;"))._div()
//                ._div()
//                .div(class_("col-6 h-100"));
//
//        if (showFilenames) {
//            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
//                    .content(dstFileName);
//        }
//
//
//        html.div(id(getRightContainerId()).style(heightFormula + "border:1px solid grey;"))._div()
//                ._div();
//        String code = "monaco(" + makeDiffConfig() + ");";
//        html.macros().script(code); // Pass the config to the main function
    }

    String getLeftJsConfig() {
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

    String getRightJsConfig() {
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

    String getMappingsJsConfig() {
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

    public String getDiffName() {
        return srcFileName + " -> " + dstFileName;
    }

    public String makeDiffConfig() {
        boolean spv = !showFilenames;
        return "{ "
                + "moved: " + this.isMoved
                + ", id: " + id
                + ", spv: " + spv
                + ", file: \"" + srcFileName + "\""
                + ", left: " + this.getLeftJsConfig()
                + ", lcid: \"" + this.getLeftContainerId() + "\""
                + ", right: " + this.getRightJsConfig()
                + ", rcid: \"" + this.getRightContainerId() + "\""
                + ", mappings: " + this.getMappingsJsConfig() + "}";
    }

    private String getLeftContainerId() {
        return "left-container-" + id;
    }
    private String getRightContainerId() {
        return "right-container-" + id;
    }
}
