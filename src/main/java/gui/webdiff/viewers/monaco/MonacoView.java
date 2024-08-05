package gui.webdiff.viewers.monaco;

import com.github.gumtreediff.actions.Diff;
import gui.webdiff.WebDiff;
import gui.webdiff.rest.AbstractMenuBar;
import gui.webdiff.viewers.AbstractDiffView;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoView extends AbstractDiffView implements Renderable {
    final MonacoCore core;
    boolean decorate = true;


    public MonacoView(String toolName, String srcFileName, String dstFileName, Diff diff, int id, int numOfDiffs, String routePath, boolean isMovedDiff) {
        super(toolName, srcFileName, dstFileName, diff, id, numOfDiffs, routePath, isMovedDiff);
        core = new MonacoCore(diff, id, isMovedDiff);
    }

    public void setDecorate(boolean decorate) {
        this.decorate = decorate;
        core.setShowFilenames(decorate);
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {

    html
        .render(DocType.HTML5)
        .html(lang("en").class_("h-100"))
            .render(new Header())
            .body(class_("h-100").style("overflow: hidden;"))
                .div(class_("container-fluid h-100"))
                .if_(decorate)
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
                    ._div()._if();
        html.div(id("diff_panel"));
        core.addDiffContainers(html);
        html._div();
        html._div()._div();
        html.macros().javascript("/dist/shortcuts.js")
            ._body()
        ._html();
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
                    .macros().javascript("https://code.jquery.com/ui/1.12.1/jquery-ui.min.js")
//                    .macros().stylesheet(JQ_UI_CSS)
                    .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                    .macros().javascript("/monaco/min/vs/loader.js")
                    .macros().javascript("/dist/monaco.js")
                ._head();
        }
    }
}
