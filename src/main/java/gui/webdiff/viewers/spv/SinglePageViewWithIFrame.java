package gui.webdiff.viewers.spv;

import gui.webdiff.dir.DirComparator;
import gui.webdiff.viewers.monaco.MonacoCore;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-04*/
public class SinglePageViewWithIFrame extends AbstractSinglePageView implements Renderable {
    public SinglePageViewWithIFrame(DirComparator comparator) {
        super(comparator);
    }
    protected void makeHead(HtmlCanvas html) throws IOException {
        html.head().meta(charset("utf8"))
                .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                .macros().stylesheet("/dist/single.css")
                .macros().stylesheet(JQ_UI_CSS)
            ._head();
    }
    protected void makeEachDiff(HtmlCanvas html, int i, MonacoCore core) throws IOException {
        html
                .iframe(src("/monaco-diff/" + i)
                        .id("monaco-diff-" + i)
                        .style("width: 100%; height: 500px; border: none;"))
                ._iframe();
    }
    protected HtmlCanvas addJSMacros(HtmlCanvas html) throws IOException {
        return html.
                macros().javascript("/dist/single.js")
                .macros().javascript(JQ_UI_JS);
    }
}
