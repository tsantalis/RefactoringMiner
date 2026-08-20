package sortpom.wrapper;

import org.jdom.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.wrapper.content.UnsortedWrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author bjorn
 * @since 2012-06-14
 */
class UnsortedWrapperTest {

    @Test
    void testIsBefore() {

        final Executable testMethod = () -> new UnsortedWrapper<Text>(null)
                .isBefore(null);;

        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);

        assertThat(thrown.getMessage(), is(equalTo("Cannot be sorted")));
    }

}
