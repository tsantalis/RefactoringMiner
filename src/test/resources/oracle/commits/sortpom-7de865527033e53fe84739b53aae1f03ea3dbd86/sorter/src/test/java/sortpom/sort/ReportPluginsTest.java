package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class ReportPluginsTest {

    @Test
    final void sortReportPluginsByArtifactIdWithCustomSortOrderFileShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/custom_report_plugins.xml")
                .lineSeparator("\r\n")
                .sortPlugins("artifactId,groupId")
                .testFiles("/ReportPlugins_input.xml",
                        "/ReportPlugins_expected.xml");
    }

    @Test
    final void sortReportPluginsByGroupIdWithCustomSortOrderFileShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/custom_report_plugins.xml")
                .lineSeparator("\r\n")
                .sortPlugins("groupId,artifactId")
                .testFiles("/ReportPlugins_input.xml",
                        "/ReportPlugins_expected2.xml");
    }

}
