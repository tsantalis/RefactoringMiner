package gui.webdiff.viewers.monaco;

import gui.webdiff.WebDiff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class SingleMonacoContent implements Renderable {

    private final boolean isAdded;
    private final String path;
    private final String content;

    //TODO: PRComment support

    public SingleMonacoContent(boolean isAdded, String path, String content) {
        this.isAdded = isAdded;
        this.path = path;
        this.content = content;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        String boxColor = isAdded ? "#d4edda" : "#f8d7da";
        String textColor = isAdded ? "#155724" : "#721c24";
        String borderColor = isAdded ? "#c3e6cb" : "#f5c6cb";
        String editorId = "monaco-editor-" + Math.abs(path.hashCode());
        String code = "loadSingleMonacoEditor({ id: '" + editorId + "', value: `" + content + "`, language: 'java' });";

        html
                .render(DocType.HTML5)
                .html(lang("en").class_("h-100"))
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
                    .head().macros().javascript("/dist/single-monaco.js")
                    .meta(charset("utf8"))
                    .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                    .title().content("RefactoringMiner")
                    .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                    .macros().stylesheet("/dist/monaco.css")
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
