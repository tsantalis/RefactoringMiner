package gui.webdiff.viewers.spv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.utils.Pair;
import gui.webdiff.viewers.monaco.MonacoCore;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.DirectoryDiffView;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitHubOAuthTokenProvider;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-22*/
public abstract class AbstractSinglePageView extends DirectoryDiffView implements Renderable {
    protected final String JQ_UI_CSS = "https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css";
    protected final String JQ_UI_JS = "https://code.jquery.com/ui/1.12.1/jquery-ui.js";
    private final boolean enableViewedFiles;

    public AbstractSinglePageView(DirComparator comparator, DiffMetaInfo metaInfo, boolean showMergeParentBar, boolean enableViewedFiles) {
        super(comparator, false, metaInfo, showMergeParentBar);
        this.enableViewedFiles = enableViewedFiles;
    }

    @Override
    public void renderOn(HtmlCanvas html) {
        int n = comparator.getNumOfDiffs();
        String viewedFilesBootstrapScript = getViewedFilesBootstrapScript();
        boolean viewedFilesEnabled = viewedFilesBootstrapScript != null;
        try {
                    makeHead(html);
                    html.render(DocType.HTML5)
                    .html(lang("en"))
                    .body()
                    .div(class_("container-fluid").style("padding-left: 0"))
                    .div(class_("row h-100"))
                    .div(class_("col-2 bg-light dir-diff"))
                    .render(new DirectoryDiffView(comparator, true, metaInfo, showMergeParentBar))
                    ._div()
                    // Monaco editors 4/5 width
                    .div(class_("col-10 monaco-panel"))
                    .div(id("accordion").style(viewedFilesEnabled ? "visibility:hidden;" : null));

            // Generate panels for /monaco-0 to /monaco-n
            for (int i = 0; i < n; i++) {
                Pair<String, String> fileContentsPair = comparator.getFileContentsPair(i);
                boolean empty = comparator.getASTDiff(i).isEmpty();
                if(!empty) {
                    MonacoCore core = new MonacoCore(comparator, comparator.getASTDiff(i), i, comparator.isMoveDiff(i), fileContentsPair.first, fileContentsPair.second, comparator.getRefactorings(), metaInfo.getComments());
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
                            .div(class_("text-end d-flex align-items-center gap-2 justify-content-end"))
                            .render_if(new ViewedFileToggle(i, comparator.getASTDiff(i)), viewedFilesEnabled)
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

    protected String getOAuthToken() {
        return GitHubOAuthTokenProvider.getOAuthToken();
    }

    protected boolean supportsViewedFiles() {
        return getViewedFilesBootstrapScript() != null;
    }

    protected String getViewedFilesBootstrapScript() {
        if (!enableViewedFiles || metaInfo == null || !metaInfo.hasUrl() || !URLHelper.isPR(metaInfo.getUrl())) {
            return null;
        }
        String oAuthToken = getOAuthToken();
        if (oAuthToken == null || oAuthToken.isBlank()) {
            return null;
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("oauthToken", oAuthToken);
        config.put("owner", URLHelper.getOwnerStringOnly(metaInfo.getUrl()));
        config.put("repoName", URLHelper.getRepoStringOnly(metaInfo.getUrl()));
        config.put("prNumber", URLHelper.getPullRequestID(metaInfo.getUrl()));
        try {
            String json = new ObjectMapper().writeValueAsString(config);
            return "document.addEventListener('DOMContentLoaded', function () { initializeViewedFiles(" + json + "); });";
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String getViewedFilePath(ASTDiff astDiff) {
        if (astDiff == null) {
            return null;
        }
        if (astDiff.getDstPath() != null && !astDiff.getDstPath().isEmpty()) {
            return astDiff.getDstPath();
        }
        return astDiff.getSrcPath();
    }

    private class ViewedFileToggle implements Renderable {
        private final int diffId;
        private final ASTDiff astDiff;

        private ViewedFileToggle(int diffId, ASTDiff astDiff) {
            this.diffId = diffId;
            this.astDiff = astDiff;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            String filePath = getViewedFilePath(astDiff);
            if (filePath == null || filePath.isEmpty()) {
                return;
            }
            String inputId = "viewed-file-" + diffId;
            html.div(class_("form-check d-inline-flex align-items-center mb-0"))
                    .input(type("checkbox")
                            .class_("form-check-input viewed-file-checkbox me-2")
                            .id(inputId)
                            .add("disabled", "disabled", true)
                            .add("data-file-path", filePath, true)
                            .add("data-collapse-target", "#collapse-" + diffId, true))
                    .label(class_("form-check-label small text-nowrap").for_(inputId)).content("Viewed")
                    ._div();
        }
    }
}
