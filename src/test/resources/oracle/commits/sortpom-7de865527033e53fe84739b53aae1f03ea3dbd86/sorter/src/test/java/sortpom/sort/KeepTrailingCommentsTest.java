package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class KeepTrailingCommentsTest {
    @Test
    final void commentsInIgnoreSectionShouldNotBeFormatted() throws Exception {
        SortPomImplUtil.create()
                .sortDependencies("scope,groupId,artifactId")
                .lineSeparator("\r\n")
                .keepBlankLines()
                .testFiles("/TrailingComment_input.xml", "/TrailingComment_expected.xml");
    }

}
