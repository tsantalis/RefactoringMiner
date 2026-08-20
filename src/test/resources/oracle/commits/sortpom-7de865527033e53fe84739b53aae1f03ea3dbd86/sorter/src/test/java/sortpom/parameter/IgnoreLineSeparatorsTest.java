package sortpom.parameter;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class IgnoreLineSeparatorsTest {

    @Test
    final void ignoringLineSeparatorsShouldNotSort() throws Exception {
        SortPomImplUtil.create()
                .lineSeparator("\n")
                .ignoreLineSeparators(true)
                .testNoSorting("/ignore_line_separators_input.xml");
    }

    @Test
    final void doNotIgnoreLineSeparatorsShouldSort() throws Exception {
        SortPomImplUtil.create()
                .lineSeparator("\n")
                .ignoreLineSeparators(false)
                .testFiles("/ignore_line_separators_input.xml", "/ignore_line_separators_output.xml");
    }

}
