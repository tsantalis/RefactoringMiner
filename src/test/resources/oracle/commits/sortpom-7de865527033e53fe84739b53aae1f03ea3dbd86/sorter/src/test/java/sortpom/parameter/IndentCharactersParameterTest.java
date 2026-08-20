package sortpom.parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.exception.FailureException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndentCharactersParameterTest {

    @Test
    void zeroIndentCharactersShouldResultInEmptyIndentString() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(0, true, false)
                .build();

        assertEquals("", pluginParameters.indentCharacters);
    }

    @Test
    void oneIndentCharacterShouldResultInOneSpace() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(1, true, false)
                .build();

        assertEquals(" ", pluginParameters.indentCharacters);
    }

    @Test
    void test255IndentCharacterShouldResultIn255Space() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(255, true, false)
                .build();

        // Test for only space
        assertTrue(pluginParameters.indentCharacters.matches("^ *$"));
        assertEquals(255, pluginParameters.indentCharacters.length());
    }

    @Test
    void minusOneIndentCharacterShouldResultInOneTab() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setIndent(-1, true, false)
                .build();

        assertEquals("\t", pluginParameters.indentCharacters);
    }

    @Test
    void minusTwoShouldFail() {

        final Executable testMethod = () -> PluginParameters.builder()
                .setIndent(-2, true, false)
                .build();

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat(thrown.getMessage(), is(equalTo("nrOfIndentSpace cannot be below -1 or above 255, was: -2")));
    }

    @Test
    void moreThan255ShouldFail() {

        final Executable testMethod = () -> PluginParameters.builder()
                .setIndent(256, true, false)
                .build();

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat(thrown.getMessage(), is(equalTo("nrOfIndentSpace cannot be below -1 or above 255, was: 256")));
    }

}
