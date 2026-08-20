package sortpom.sort;

import org.junit.Test;
import sortpom.util.SortPomImplUtil;

public class KeepTrailingCommentsTest {
    @Test
    public final void commentsInIgnoreSectionShouldNotBeFormatted() throws Exception {
        SortPomImplUtil.create()
                .sortDependencies("scope,groupId,artifactId")
                .lineSeparator("\r\n")
                .keepBlankLines()
                .testFiles("/TrailingComment_input.xml", "/TrailingComment_expected.xml");
    }

}
