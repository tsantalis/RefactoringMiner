package gui.webdiff.viewers.monaco;

import gui.webdiff.WebDiff;
import gui.webdiff.dir.PullRequestReviewComment;
import gui.webdiff.rest.AbstractMenuBar;

import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static gui.webdiff.viewers.monaco.MonacoCore.filterComments;
import static org.rendersnake.HtmlAttributesFactory.*;

public class SingleMonacoContent implements Renderable {
    private final String toolName;
    private final String routePath;
    private final int numOfDiffs;
    private final DiffMetaInfo metaInfo;
    private final List<String> deletedFilePaths;
    private final List<String> addedFilePaths;
    private final boolean isAdded;
    private final String path;
    private final String content;
    Map<String, List<PullRequestReviewComment>> comments;

    public SingleMonacoContent(String toolName, String routePath, int numOfDiffs, boolean isAdded, String path, String content, DiffMetaInfo metaInfo,
            List<String> deletedFilePaths, List<String> addedFilePaths) {
        this.toolName = toolName;
        this.routePath = routePath;
        this.numOfDiffs = numOfDiffs;
        this.metaInfo = metaInfo;
        this.isAdded = isAdded;
        this.path = path;
        this.content = content;
        this.comments = metaInfo.getComments();
        this.deletedFilePaths = deletedFilePaths;
        this.addedFilePaths = addedFilePaths;
    }

    public void setComments(Map<String, List<PullRequestReviewComment>> comments) {
        this.comments = comments;
    }

    public Map<String, List<PullRequestReviewComment>> getComments() {
        return comments;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        String editorId = "monaco-editor-" + Math.abs(path.hashCode());
        String comments = filterComments(this.comments, path, isAdded ? PullRequestReviewComment.Side.RIGHT : PullRequestReviewComment.Side.LEFT);
        String code = "loadSingleMonacoEditor({ id: '" + editorId + "', value: `" + content + "`, language: 'java', " + "comments: " + comments + ", });";

        html
                .render(DocType.HTML5)
                .html(lang("en").class_("h-100"))
                .div(class_("row").style("--bs-gutter-x:0"))
                .render(new AbstractMenuBar(toolName, routePath, path, numOfDiffs, metaInfo, deletedFilePaths, addedFilePaths){
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
                .render(new SingleMonacoHeader(path, isAdded))
                .div(style("flex: 1 1 auto; width: 100%;"))
                .div(id(editorId).style("width:100%; height:100%;"))
                ._div()
                ._div()
                .div(style("height: 10px; flex-shrink: 0; background:#eee;"))
                ._div();
                html.macros().script(code)
                ._body()
                ._html();

    }

    private static class SingleMonacoHeader implements Renderable {
        private final String path;
        private final boolean isAdded;

        public SingleMonacoHeader(String path, boolean isAdded) {
            this.path = path;
            this.isAdded = isAdded;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            String boxColor = isAdded ? "#d4edda" : "#f8d7da";
            String textColor = isAdded ? "#155724" : "#721c24";
            String borderColor = isAdded ? "#c3e6cb" : "#f5c6cb";

            html
                    .head()
                    .meta(charset("utf8"))
                    .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                    .title().content("RefactoringMiner")
                    .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                    .macros().stylesheet("/dist/single-monaco.css")
                    .macros().stylesheet("/dist/monaco.css")
                    .macros().javascript("/dist/marked.min.js")
                    .macros().javascript("/dist/pr-utils.js")
                    .macros().javascript("/dist/single-monaco.js")
                    .macros().javascript(WebDiff.JQUERY_JS_URL)
                    .macros().javascript("https://code.jquery.com/ui/1.12.1/jquery-ui.min.js")
                    .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                    .macros().javascript("/monaco/min/vs/loader.js")
                    .style().content(
                            "html, body { margin: 0; padding: 0; height: 100%; overflow: hidden; }"
                    )
                    ._head()

                    // Top bar with back button and file path
                    .body(style("margin:0; height:100%; display:flex; flex-direction:column;"))
                    .div(style("flex: 0 0 auto; padding: 10px; display: flex; align-items: center; gap: 10px;" +
                               "background-color:" + boxColor + ";" +
                               "color:" + textColor + ";" +
                               "border-bottom: 1px solid " + borderColor + ";"))
                    .a(href("javascript:history.back()").class_("btn btn-secondary btn-sm"))
                    .content("← Back")
                    .div(style("font-weight: bold;")).content(path)
                    ._div();
        }
    }
}
