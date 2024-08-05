package gui.webdiff.viewers.spv;

import gui.webdiff.WebDiff;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.viewers.monaco.MonacoCore;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-22*/
public class SinglePageView extends AbstractSinglePageView implements Renderable {
    public SinglePageView(DirComparator comparator) {
        super(comparator);
    }

    protected void makeHead(HtmlCanvas html) throws IOException {
        html.meta(charset("utf8"))
                .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                .macros().javascript("https://code.jquery.com/jquery-3.4.1.min.js")
                .macros().javascript("https://code.jquery.com/ui/1.12.1/jquery-ui.min.js")
                .macros().stylesheet(JQ_UI_CSS)
                .macros().javascript("https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs/loader.js")

                .macros().stylesheet("/dist/single.css")
                .macros().stylesheet("/dist/monaco.css")
                .macros().javascript("/dist/monaco.js");


    }
    protected void makeEachDiff(HtmlCanvas html, int i, MonacoCore core) throws IOException {
        core.addDiffContainers(html);
    }
    protected HtmlCanvas addJSMacros(HtmlCanvas html) throws IOException {
        return html.macros().javascript("/dist/single.js");
    }

}
