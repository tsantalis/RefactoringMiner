package sortpom.wrapper;

import org.jdom.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sortpom.parameter.PluginParameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bjorn
 * @since 2012-06-19
 */
class TextWrapperCreatorTest {
    private final TextWrapperCreator textWrapperCreator = new TextWrapperCreator();

    @BeforeEach
    void setup() {
        textWrapperCreator.setup(PluginParameters.builder().setEncoding("UTF-8")
                .setFormatting("\n", true, true, true).build());
    }

    @Test
    void testIsEmptyLine() {
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
