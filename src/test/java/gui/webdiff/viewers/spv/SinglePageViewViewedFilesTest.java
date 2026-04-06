package gui.webdiff.viewers.spv;

import gui.webdiff.dir.DirComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.rendersnake.HtmlCanvas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SinglePageViewViewedFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void rendersViewedCheckboxAndBootstrapConfigForPullRequestView() throws Exception {
        DirComparator comparator = createComparator();
        DiffMetaInfo metaInfo = new DiffMetaInfo("PR #1016", "https://github.com/tsantalis/RefactoringMiner/pull/1016");
        comparator.getProjectASTDiff().setMetaInfo(metaInfo);

        String html = render(new TestSinglePageView(comparator, metaInfo, true, true, "test-token"));

        assertTrue(html.contains("Viewed"));
        assertTrue(html.contains("viewed-file-checkbox"));
        assertTrue(html.contains("\"owner\":\"tsantalis\""));
        assertTrue(html.contains("\"repoName\":\"RefactoringMiner\""));
        assertTrue(html.contains("\"prNumber\":1016"));
        assertTrue(html.contains("\"oauthToken\":\"test-token\""));
        assertTrue(html.contains("initializeViewedFiles("));
        assertTrue(html.contains("data-file-path=\"src/main/java/example/Sample.java\""));
    }

    @Test
    void skipsViewedControlsWhenInteractiveFeatureIsDisabled() throws Exception {
        DirComparator comparator = createComparator();
        DiffMetaInfo metaInfo = new DiffMetaInfo("PR #1016", "https://github.com/tsantalis/RefactoringMiner/pull/1016");
        comparator.getProjectASTDiff().setMetaInfo(metaInfo);

        String html = render(new TestSinglePageView(comparator, metaInfo, false, false, "test-token"));

        assertFalse(html.contains("viewed-file-checkbox"));
        assertFalse(html.contains("initializeViewedFiles("));
    }

    @Test
    void skipsViewedControlsWithoutOAuthToken() throws Exception {
        DirComparator comparator = createComparator();
        DiffMetaInfo metaInfo = new DiffMetaInfo("PR #1016", "https://github.com/tsantalis/RefactoringMiner/pull/1016");
        comparator.getProjectASTDiff().setMetaInfo(metaInfo);

        String html = render(new TestSinglePageView(comparator, metaInfo, true, true, null));

        assertFalse(html.contains("viewed-file-checkbox"));
        assertFalse(html.contains("initializeViewedFiles("));
    }

    private DirComparator createComparator() throws Exception {
        Path beforeDir = Files.createDirectories(tempDir.resolve("before/src/main/java/example"));
        Path afterDir = Files.createDirectories(tempDir.resolve("after/src/main/java/example"));
        Files.writeString(beforeDir.resolve("Sample.java"), """
                package example;
                class Sample {
                    int value() { return 1; }
                }
                """);
        Files.writeString(afterDir.resolve("Sample.java"), """
                package example;
                class Sample {
                    int value() { return 2; }
                }
                """);
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl()
                .diffAtDirectories(tempDir.resolve("before"), tempDir.resolve("after"));
        projectASTDiff.setMetaInfo(new DiffMetaInfo("Local diff", null));
        return new DirComparator(projectASTDiff, diffs -> diffs);
    }

    private String render(SinglePageView view) throws IOException {
        HtmlCanvas canvas = new HtmlCanvas();
        view.renderOn(canvas);
        return canvas.toHtml();
    }

    private static final class TestSinglePageView extends SinglePageView {
        private final String oAuthToken;

        private TestSinglePageView(DirComparator comparator, DiffMetaInfo metaInfo,
                                   boolean showMergeParentBar, boolean enableViewedFiles, String oAuthToken) {
            super(comparator, metaInfo, showMergeParentBar, enableViewedFiles);
            this.oAuthToken = oAuthToken;
        }

        @Override
        protected String getOAuthToken() {
            return oAuthToken;
        }
    }
}
