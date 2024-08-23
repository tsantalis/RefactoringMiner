package gui.webdiff.viewers.spv;

import gui.webdiff.viewers.monaco.MonacoCore;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.DirectoryDiffView;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-22*/
public abstract class AbstractSinglePageView extends DirectoryDiffView implements Renderable {
    protected final String JQ_UI_CSS = "https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css";
    protected final String JQ_UI_JS = "https://code.jquery.com/ui/1.12.1/jquery-ui.js";

    public AbstractSinglePageView(DirComparator comparator) {
        super(comparator);
    }

    @Override
    public void renderOn(HtmlCanvas html) {
        int n = comparator.getNumOfDiffs();
        try {
                    makeHead(html);
                    html.render(DocType.HTML5)
                    .html(lang("en"))
                    .body()
                    .div(class_("container-fluid").style("padding-left: 0"))
                    .div(class_("row h-100"))
                    .div(class_("col-2 bg-light dir-diff"))
                    .render(new DirectoryDiffView(comparator, true))
                    ._div()
                    // Monaco editors 4/5 width
                    .div(class_("col-10 monaco-panel"))
                    .div(id("accordion"));

            // Generate panels for /monaco-0 to /monaco-n
            for (int i = 0; i < n; i++) {
                MonacoCore core = new MonacoCore(comparator.getASTDiff(i), i, comparator.isMoveDiff(i));
                core.setShowFilenames(false);
                html.div(class_("card"))
                        .div(class_("card-header").id("heading-" + i).style("padding-right: 0;"))
                        .div(class_("d-flex align-items-center justify-content-between"))
                        .div(class_("flex-grow-1").style("word-break: break-word; overflow-wrap: anywhere;"))
                        .h5(class_("mb-0"))
                        .a(class_("")
                                .add("data-bs-toggle", "collapse")
                                .add("data-bs-target", "#collapse-" + i)
                                .add("aria-expanded", "true")
                                .add("aria-controls", "collapse-" + i)).content(core.getDiffName())
                        ._h5()
                        ._div()
                        .div(class_("text-end"))
                        .a(href("monaco-page/" + i).class_("btn btn-primary btn sm")).content("Details")
                        ._div()
                        ._div()
                        ._div()
                        .div(id("collapse-" + i).class_("collapse show").add("aria-labelledby", "heading-" + i))
                        .div(class_("card-body").style("overflow: hidden; padding: 0;"));
                makeEachDiff(html, i, core);
                html
                        ._div()
                        ._div()
                        ._div()
                        ._div();

            }

            html._div() // Close accordion div
                    ._div() // Close monaco-panel div
                    ._div() // Close row div
                    ._div(); // Close container-fluid div


            addJSMacros(html)
                    ._body()
                    ._html();

        } catch (Exception e) {
            System.out.println(e);
            // Handle exception
        }

    }

    protected abstract HtmlCanvas addJSMacros(HtmlCanvas html) throws IOException;

    protected abstract void makeEachDiff(HtmlCanvas html, int i, MonacoCore core) throws IOException;

    protected abstract void makeHead(HtmlCanvas html) throws IOException;
}
