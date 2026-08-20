package sortpom.sort;

import org.junit.Test;
import sortpom.util.SortPomImplUtil;

public class ReportPluginsTest {

    @Test
    public final void sortReportPluginsByArtifactIdWithCustomSortOrderFileShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/custom_report_plugins.xml")
                .lineSeparator("\r\n")
                .sortPlugins("artifactId,groupId")
                .testFiles("/ReportPlugins_input.xml",
                        "/ReportPlugins_expected.xml");
    }

    @Test
    public final void sortReportPluginsByGroupIdWithCustomSortOrderFileShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/custom_report_plugins.xml")
                .lineSeparator("\r\n")
                .sortPlugins("groupId,artifactId")
                .testFiles("/ReportPlugins_input.xml",
                        "/ReportPlugins_expected2.xml");
    }

}
