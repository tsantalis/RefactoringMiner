package sortpom.wrapper.content;

import org.jdom.Element;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author bjorn
 * @since 2016-07-30
 */
public class SortedWrapperTest {

    @Test
    public void toStringWithIndentShouldWork() {
        assertThat(new SortedWrapper(new Element("Gurka"), 123).toString("  "), is("  SortedWrapper{element=[Element: <Gurka/>]}"));
        assertThat(new SortedWrapper(null, 123).toString("  "), is("  SortedWrapper{element=null}"));
    }
}