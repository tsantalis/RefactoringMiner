package sortpom.wrapper;

import org.jdom.Text;
import org.junit.Before;
import org.junit.Test;
import sortpom.parameter.PluginParameters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author bjorn
 * @since 2012-06-19
 */
public class TextWrapperCreatorTest {
    private final TextWrapperCreator textWrapperCreator = new TextWrapperCreator();

    @Before
    public void setup() {
        textWrapperCreator.setup(PluginParameters.builder().setEncoding("UTF-8")
                .setFormatting("\n", true, true).build());
    }

    @Test
    public void testIsEmptyLine() {
        assertFalse(textWrapperCreator.isBlankLineOrLines(new Text("\n      sortpom\n  ")));
        assertFalse(textWrapperCreator.isBlankLineOrLines(new Text("sortpom")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("\n  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \n  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \n\n  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("\n\n")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \r  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \r\r  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("\r\r")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \r\n  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("  \r\n\r\n  ")));
        assertTrue(textWrapperCreator.isBlankLineOrLines(new Text("\r\n\r\n")));
        assertFalse(textWrapperCreator.isBlankLineOrLines(new Text("  ")));
    }
}
