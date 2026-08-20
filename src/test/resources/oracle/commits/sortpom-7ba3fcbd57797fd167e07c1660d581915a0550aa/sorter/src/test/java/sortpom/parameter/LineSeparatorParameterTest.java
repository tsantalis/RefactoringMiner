package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.exception.FailureException;

import static org.junit.Assert.assertEquals;

public class LineSeparatorParameterTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void lineSeparatorWithSomethingElseShouldThrowException() {
        thrown.expect(FailureException.class);
        thrown.expectMessage("LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were [42, 42, 42]");

        PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting("***", false, false)
                .setIndent(2, false);

    }

    @Test
    public void testCharInput() {
        assertEquals("\n", PluginParameters.builder().setFormatting("\n", true, true).build().lineSeparatorUtil.toString());
        assertEquals("\r", PluginParameters.builder().setFormatting("\r", true, true).build().lineSeparatorUtil.toString());
        assertEquals("\r\n", PluginParameters.builder().setFormatting("\r\n", true, true).build().lineSeparatorUtil.toString());
    }

    @Test
    public void testFailedInput1() {
        thrown.expectMessage("LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were [10, 110]");

        PluginParameters.builder().setFormatting("\nn", true, true);
    }

    @Test
    public void testFailedInput2() {
        thrown.expectMessage("LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were [10, 10]");
        PluginParameters.builder().setFormatting("\n\n", true, true);
    }

    @Test
    public void testFailedInput3() {
        thrown.expectMessage("LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were [103, 117, 114, 107, 97]");
        PluginParameters.builder().setFormatting("gurka", true, true);
    }

    @Test
    public void testFailedInput4() {
        thrown.expectMessage("LineSeparator must be either \\n, \\r or \\r\\n, but separator characters were []");
        PluginParameters.builder().setFormatting("", true, true);
    }

    @Test
    public void testMixedInput() {
        assertEquals("\r\n", new LineSeparatorUtil("\\r\n").toString());
        assertEquals("\r\n", new LineSeparatorUtil("\r\\n").toString());
    }

    @Test
    public void testStringInput() {
        assertEquals("\n", new LineSeparatorUtil("\\n").toString());
        assertEquals("\r", new LineSeparatorUtil("\\r").toString());
        assertEquals("\r\n", new LineSeparatorUtil("\\r\\n").toString());
    }

}
