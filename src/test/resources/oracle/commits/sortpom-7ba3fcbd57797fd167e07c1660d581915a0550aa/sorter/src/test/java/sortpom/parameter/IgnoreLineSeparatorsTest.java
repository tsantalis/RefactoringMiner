package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.util.SortPomImplUtil;

public class IgnoreLineSeparatorsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public final void ignoringLineSeparatorsShouldNotSort() throws Exception {
        SortPomImplUtil.create()
                .lineSeparator("\n")
                .ignoreLineSeparators(true)
                .testNoSorting("/ignore_line_separators_input.xml");
    }

    @Test
    public final void doNotIgnoreLineSeparatorsShouldSort() throws Exception {
        SortPomImplUtil.create()
                .lineSeparator("\n")
                .ignoreLineSeparators(false)
                .testFiles("/ignore_line_separators_input.xml", "/ignore_line_separators_output.xml");
    }

}
