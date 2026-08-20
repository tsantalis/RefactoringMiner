package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SortDependenciesTest {

    @Test
    final void scopeInSortDependenciesShouldSortByScope() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("custom_1.xml")
                .sortDependencies("scope,GROUPID,artifactId")
                .lineSeparator("\r\n")
                .testFiles("/SortDep_input_simpleWithScope.xml", "/SortDep_expected_simpleWithScope2.xml");
    }

    /**
     * This is an odd test since we add an extra tag in the sort order file (under plugins and dependencies)
     * so that it will be sorted beside dependency and plugin tags. The extra tag does not play well with
     * the pom xml validation.
     */
    @Test
    final void extraTagInDependenciesAndPluginShouldBeSortedFirst() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/extra_dummy_tags.xml")
                .sortDependencies("scope,groupId,artifactId")
                .sortPlugins("groupId,artifactId")
                .lineSeparator("\r\n")
                .testFiles("/Extra_tags_dep_and_plugin.xml", "/Extra_tags_dep_and_plugin_expected.xml");
    }

    @Test
    final void defaultGroupIdForPluginsShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("custom_1.xml")
                .sortPlugins("groupId,artifactId")
                .lineSeparator("\n")
                .nrOfIndentSpace(4)
                .testFiles("/PluginDefaultName_input.xml", "/PluginDefaultName_expect.xml");
    }

    @Test
    final void deprecatedSortPluginsTrueMessageShouldWork() throws Exception {
        List<String> logs = SortPomImplUtil.create()
                .defaultOrderFileName("custom_1.xml")
                .sortPlugins("true")
                .lineSeparator("\n")
                .nrOfIndentSpace(4)
                .testFilesAndReturnLogs("/PluginDefaultName_input.xml", "/PluginDefaultName_expect.xml");

        assertThat(logs.get(0), is("[WARNING] [DEPRECATED] The 'true' value in sortPlugins is not used anymore, please use value 'groupId,artifactId' instead. In the next major version 'true' or 'false' will cause an error!"));
    }

    @Test
    final void deprecatedSortPluginsFalseMessageShouldWork() throws Exception {
        List<String> logs = SortPomImplUtil.create()
                .sortPlugins("false")
                .testFilesAndReturnLogs("/full_unsorted_input.xml", "/full_expected.xml");

        assertThat(logs.get(0), is("[WARNING] [DEPRECATED] The 'false' value in sortPlugins is not used anymore, please use empty value '' or omit sortPlugins instead. In the next major version 'true' or 'false' will cause an error!"));
    }

    @Test
    final void deprecatedSortDependenciesTrueMessageShouldWork() throws Exception {
        List<String> logs = SortPomImplUtil.create()
                .sortDependencies("true")
                .sortPlugins("true")
                .testFilesAndReturnLogs("/Simple_input.xml", "/Simple_expected_sortDep.xml");

        assertThat(logs.get(0), is("[WARNING] [DEPRECATED] The 'true' value in sortDependencies is not used anymore, please use value 'groupId,artifactId' instead. In the next major version 'true' or 'false' will cause an error!"));
    }

    @Test
    final void deprecatedSortDependenciesFalseMessageShouldWork() throws Exception {
        List<String> logs = SortPomImplUtil.create()
                .sortDependencies("false")
                .testFilesAndReturnLogs("/full_unsorted_input.xml", "/full_expected.xml");

        assertThat(logs.get(0), is("[WARNING] [DEPRECATED] The 'false' value in sortDependencies is not used anymore, please use empty value '' or omit sortDependencies instead. In the next major version 'true' or 'false' will cause an error!"));
    }
}
