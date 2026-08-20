package sortpom.wrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.wrapper.content.SingleNewlineInTextWrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * All method should throw exception since the element should be throw away, except for the toString method
 *
 * @author bjorn
 * @since 2012-06-14
 */
class SingleNewlineInTextWrapperTest {

    @Test
    void testGetContent() {

        final Executable testMethod = () -> SingleNewlineInTextWrapper.INSTANCE.getContent();

        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);

        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testIsBefore() {

        final Executable testMethod = () -> SingleNewlineInTextWrapper.INSTANCE.isBefore(null);

        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);

        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testIsContentElement() {

        final Executable testMethod = () -> SingleNewlineInTextWrapper.INSTANCE.isContentElement();

        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);

        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testIsResortable() {

        final Executable testMethod = () -> SingleNewlineInTextWrapper.INSTANCE.isSortable();

        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);

        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testToString() {
        assertThat(SingleNewlineInTextWrapper.INSTANCE.toString("  "), is("  SingleNewlineInTextWrapper"));
    }

}
