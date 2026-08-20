package sortpom.parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import sortpom.exception.FailureException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerifyFailParameterTest {

    @Test
    void stopIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("sToP", "strict")
                .build();

        assertEquals(VerifyFailType.STOP, pluginParameters.verifyFailType);
    }

    @Test
    void warnIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("wArN", "strict")
                .build();

        assertEquals(VerifyFailType.WARN, pluginParameters.verifyFailType);
    }

    @Test
    void sortIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("sOrT", "strict")
                .build();

        assertEquals(VerifyFailType.SORT, pluginParameters.verifyFailType);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "gurka")
    void verifyFailFaultyValues(String value) {

        final Executable testMethod = () -> PluginParameters.builder()
                .setVerifyFail(value, "strict")
                .build();

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat(thrown.getMessage(), is(equalTo("verifyFail must be either SORT, WARN or STOP. Was: " + value)));
    }

}
