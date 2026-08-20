package sortpom.wrapper;

import org.jdom.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.wrapper.content.UnsortedWrapper;

/**
 * @author bjorn
 * @since 2012-06-14
 */
public class UnsortedWrapperTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsBefore() {
        thrown.expect(UnsupportedOperationException.class);
        new UnsortedWrapper<Text>(null).isBefore(null);
    }
}
