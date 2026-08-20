package sortpom.wrapper.content;

import org.jdom.Element;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author bjorn
 * @since 2016-07-30
 */
public class AlphabeticalSortedWrapperTest {

    @Test
    public void toStringWithIndentShouldWork() {
        assertThat(new AlphabeticalSortedWrapper(new Element("Gurka")).toString("  "), is("  AlphabeticalSortedWrapper{element=[Element: <Gurka/>]}"));
        assertThat(new AlphabeticalSortedWrapper(null).toString("  "), is("  AlphabeticalSortedWrapper{element=null}"));
    }
}