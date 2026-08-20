package sortpom.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author bjorn
 * @since 2020-01-11
 */
public class StringLineSeparatorWriterTest {

    private StringLineSeparatorWriter writer;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        writer = new StringLineSeparatorWriter("separator");
    }

    @Test
    public void writeNewlineShouldBeConvertedToSeparator1() {
        writer.write("Hey\nYou!");
        assertThat(writer.toString(), is("HeyseparatorYou!"));
    }

    @Test
    public void writeNewlineShouldBeConvertedToSeparator2() {
        writer.write("Hello");
        writer.write('&');
        writer.write('\n');
        writer.write("Goodbye");
        assertThat(writer.toString(), is("Hello&separatorGoodbye"));
    }

    @Test
    public void clearExtraNewlinesShouldWork() {
        writer.write("<xml>\n");

        //The spaces should be removed
        writer.write("  ");

        writer.clearLineBuffer();
        writer.write("<moreXml>");
        assertThat(writer.toString(), is("<xml>separator<moreXml>"));
    }

    @Test
    public void testWriteDeprecated1() {
        expectedException.expect(UnsupportedOperationException.class);

        writer.write(new char[0]);
    }

    @Test
    public void testWriteDeprecated2() {
        expectedException.expect(UnsupportedOperationException.class);

        writer.write("", 0, 0);
    }

    @Test
    public void testWriteDeprecated3() {
        expectedException.expect(UnsupportedOperationException.class);

        writer.write(new char[0], 0, 0);
    }


}
