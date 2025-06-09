package gui.webdiff.viewers.monaco;

import gui.webdiff.WebDiff;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.rest.AbstractMenuBar;
import gui.webdiff.viewers.AbstractDiffView;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.List;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoView extends AbstractDiffView implements Renderable {
    final MonacoCore core;
    boolean decorate = true;
    boolean _buttons = true;
    public MonacoView(String toolName, DirComparator comparator, String routePath, int id) {
        super(toolName, 
            comparator.getProjectASTDiff().getMetaInfo(), 
            comparator.getASTDiff(id).getSrcPath(), 
            comparator.getASTDiff(id).getDstPath(), 
            comparator.getASTDiff(id), 
            id, 
            comparator.getNumOfDiffs(), 
            routePath, 
            comparator.isMoveDiff(id) 
        );
        ASTDiff diff = comparator.getASTDiff(id);
        boolean isMovedDiff = comparator.isMoveDiff(id);
        String srcFileContent = comparator.getProjectASTDiff().getFileContentsBefore().get(diff.getSrcPath());
        String dstFileContent = comparator.getProjectASTDiff().getFileContentsAfter().get(diff.getDstPath());
        List<Refactoring> refactorings = comparator.getRefactorings();
        core = new MonacoCore(comparator, diff, id, isMovedDiff, srcFileContent, dstFileContent, refactorings, comparator.getProjectASTDiff().getMetaInfo().getComments());
    }

    public MonacoView(String toolName, DirComparator comparator, String routePath, int id, ASTDiff diff) {
        super(toolName, 
        		comparator.getProjectASTDiff().getMetaInfo(), 
                diff.getSrcPath(), 
                diff.getDstPath(), 
                diff, 
                id, 
                comparator.getNumOfDiffs(), 
                routePath, 
                false 
            );
            String srcFileContent = comparator.getProjectASTDiff().getFileContentsBefore().get(diff.getSrcPath());
            String dstFileContent = comparator.getProjectASTDiff().getFileContentsAfter().get(diff.getDstPath());
            List<Refactoring> refactorings = comparator.getRefactorings();
            core = new MonacoCore(comparator, diff, id, false, srcFileContent, dstFileContent, refactorings,
                    comparator.getProjectASTDiff().getMetaInfo().getComments());
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
                .if_(decorate && _buttons)
                    .div(class_("row"))
                    .render(new AbstractMenuBar(toolName, routePath, id, numOfDiffs, isMovedDiff, metaInfo){
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

    public void setButtons(boolean b) {
        this._buttons = b;
        core.setMinimal(!b);
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
                    .macros().javascript("/dist/utils.js")
                    .macros().javascript("/dist/folding.js")
                    .macros().javascript("/dist/decorations.js")
                    .macros().javascript("/dist/pr-utils.js")
                    .macros().javascript("/dist/monaco.js")
                    .macros().javascript("/dist/listeners.js")
                ._head();
        }
    }
}
