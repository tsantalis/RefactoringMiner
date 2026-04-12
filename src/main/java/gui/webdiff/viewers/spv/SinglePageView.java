package gui.webdiff.viewers.spv;

import gui.webdiff.dir.DirComparator;
import gui.webdiff.viewers.monaco.MonacoCore;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-22*/
public class SinglePageView extends AbstractSinglePageView implements Renderable {
    public SinglePageView(DirComparator comparator, DiffMetaInfo metaInfo) {
        this(comparator, metaInfo, true, true);
    }

    public SinglePageView(DirComparator comparator, DiffMetaInfo metaInfo, boolean showMergeParentBar) {
        this(comparator, metaInfo, showMergeParentBar, true);
    }

    public SinglePageView(DirComparator comparator, DiffMetaInfo metaInfo, boolean showMergeParentBar, boolean enableViewedFiles) {
        super(comparator, metaInfo, showMergeParentBar, enableViewedFiles);
    }

    protected void makeHead(HtmlCanvas html) throws IOException {
        html.head().meta(charset("utf8"))
                .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                .macros().stylesheet("/dist/single.css")
                .macros().stylesheet("/dist/monaco.css")
                .macros().javascript("/dist/marked.min.js")
                .macros().javascript("/dist/utils.js")
                .macros().javascript("/dist/folding.js")
                .macros().javascript("/dist/decorations.js")
                .macros().javascript("/dist/pr-utils.js")
                .macros().javascript("/dist/monaco.js")
                .macros().javascript("/monaco/min/vs/loader.js")
                .macros().javascript("/dist/markAsViewed.js")
            ._head();


    }
    protected void makeEachDiff(HtmlCanvas html, int i, MonacoCore core) throws IOException {
        core.addDiffContainers(html);
    }
    protected HtmlCanvas addJSMacros(HtmlCanvas html) throws IOException {
        html.macros().javascript("/dist/single.js");
        String viewedFilesBootstrapScript = getViewedFilesBootstrapScript();
        if (viewedFilesBootstrapScript != null) {
            html.macros().script(viewedFilesBootstrapScript);
        }
        return html;
    }

}
