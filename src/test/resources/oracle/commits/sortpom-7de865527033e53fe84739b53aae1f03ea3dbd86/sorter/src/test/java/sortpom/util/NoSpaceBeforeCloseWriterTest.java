package sortpom.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NoSpaceBeforeCloseWriterTest {
    private NoSpaceBeforeCloseWriter writer;

    private XmlWriter xmlWriter = mock(XmlWriter.class);

    @BeforeEach
    void setUp() {
        writer = new NoSpaceBeforeCloseWriter(xmlWriter);
    }

    @Test
    void closeXmlTagShouldNotHaveSpace() {
        writer.write(" />");
        verify(xmlWriter).write("/>");
    }

    @Test
    void anyOtherStringShouldNotBeModified() {
        String uuid = UUID.randomUUID().toString();
        writer.write(uuid);
        verify(xmlWriter).write(uuid);
    }

    @Test
    void charShouldBeDelegated() {
        int i = new Random().nextInt();
        writer.write(i);
        verify(xmlWriter).write(i);
    }

    @Test
    void flushShouldBeDelegated() {
        writer.flush();
        verify(xmlWriter).flush();
    }

    @Test
    void closeShouldBeDelegated() {
        writer.close();
        verify(xmlWriter).close();
    }

    @Test
    void clearLineBufferShouldBeDelegated() {
        writer.clearLineBuffer();
        verify(xmlWriter).clearLineBuffer();
    }

    @Test
    void testWriteDeprecated1() {
        final Executable testMethod = () -> writer.write(new char[0]);
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testWriteDeprecated2() {
        final Executable testMethod = () -> writer.write("", 0, 0);
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testWriteDeprecated3() {
        final Executable testMethod = () -> writer.write(new char[0], 0, 0);
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testAppendDeprecated1() {
        final Executable testMethod = () -> writer.append('e');
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testAppendDeprecated2() {
        final Executable testMethod = () -> writer.append("");
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }

    @Test
    void testAppendDeprecated3() {
        final Executable testMethod = () -> writer.append("", 0, 0);
        final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, testMethod);
        assertThat(thrown.getMessage(), is(nullValue()));
    }
}
