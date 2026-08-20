package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class SortModulesTest {
    @Test
    final void sortingOfPomFileWithSubmodulesShouldWork() throws Exception {
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortModules()
                .sortDependencies("true")
                .lineSeparator("\n")
                .testFiles("/SortModules_input.xml", "/SortModules_expected.xml");
    }
    @Test
    final void sortingOfPomFileWithSubmodulesAndExtraElementsShouldWork() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("sortOrderFiles/extra_dummy_tags.xml")
                .sortProperties()
                .sortPlugins("true")
                .sortModules()
                .sortDependencies("true")
                .lineSeparator("\n")
                .testFiles("/SortModules_input_extra_elements.xml", "/SortModules_expected_extra_elements.xml");
    }
    @Test
    final void sortingOfPomFileWithSubmodulesNotEnabled() throws Exception {
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortDependencies("true")
                .lineSeparator("\n")
                .testFiles("/SortModules_input.xml", "/SortModules_expected_notsorted.xml");
    }
}
