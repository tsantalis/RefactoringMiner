package sortpom.wrapper.content;

import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author bjorn
 * @since 2016-07-30
 */
class AlphabeticalSortedWrapperTest {

    @Test
    void toStringWithIndentShouldWork() {
        assertThat(new AlphabeticalSortedWrapper(new Element("Gurka")).toString("  "), is("  AlphabeticalSortedWrapper{element=[Element: <Gurka/>]}"));
        assertThat(new AlphabeticalSortedWrapper(null).toString("  "), is("  AlphabeticalSortedWrapper{element=null}"));
    }
}
