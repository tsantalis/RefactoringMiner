package sortpom.sort;

import org.junit.Test;
import sortpom.util.SortPomImplUtil;

public class SortModulesTest {
    @Test
    public final void sortingOfPomFileWithSubmodulesShouldWork() throws Exception {
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortModules()
                .sortDependencies("true")
                .lineSeparator("\n")
                .testFiles("/SortModules_input.xml", "/SortModules_expected.xml");
    }
    @Test
    public final void sortingOfPomFileWithSubmodulesAndExtraElementsShouldWork() throws Exception {
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
    public final void sortingOfPomFileWithSubmodulesNotEnabled() throws Exception {
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortDependencies("true")
                .lineSeparator("\n")
                .testFiles("/SortModules_input.xml", "/SortModules_expected_notsorted.xml");
    }
}
