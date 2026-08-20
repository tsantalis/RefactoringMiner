package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.exception.FailureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IndentCharactersParameterTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void zeroIndentCharactersShouldResultInEmptyIndentString() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(0, true)
                .build();

        assertEquals("", pluginParameters.indentCharacters);
    }

    @Test
    public void oneIndentCharacterShouldResultInOneSpace() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(1, true)
                .build();

        assertEquals(" ", pluginParameters.indentCharacters);
    }

    @Test
    public void test255IndentCharacterShouldResultIn255Space() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(255, true)
                .build();

        // Test for only space
        assertTrue(pluginParameters.indentCharacters.matches("^ *$"));
        assertEquals(255, pluginParameters.indentCharacters.length());
    }

    @Test
    public void minusOneIndentCharacterShouldResultInOneTab() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(-1, true)
                .build();

        assertEquals("\t", pluginParameters.indentCharacters);
    }

    @Test
    public void minusTwoShouldFail() {
        thrown.expect(FailureException.class);
        thrown.expectMessage("");

        PluginParameters.builder()
                .setIndent(-2, true)
                .build();
    }

    @Test
    public void moreThan255ShouldFail() {
        thrown.expect(FailureException.class);
        thrown.expectMessage("");

        PluginParameters.builder()
                .setIndent(256, true)
                .build();
    }

}
