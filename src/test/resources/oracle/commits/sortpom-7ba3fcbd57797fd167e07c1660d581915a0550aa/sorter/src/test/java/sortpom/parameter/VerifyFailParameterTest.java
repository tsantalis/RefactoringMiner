package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class VerifyFailParameterTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void stopIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("sToP")
                .build();

        assertEquals(VerifyFailType.STOP, pluginParameters.verifyFailType);
    }

    @Test
    public void warnIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("wArN")
                .build();

        assertEquals(VerifyFailType.WARN, pluginParameters.verifyFailType);
    }

    @Test
    public void sortIgnoreCaseValueIsOk() {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setVerifyFail("sOrT")
                .build();

        assertEquals(VerifyFailType.SORT, pluginParameters.verifyFailType);
    }

    @Test
    public void nullValueIsNotOk() {
        thrown.expectMessage("verifyFail must be either SORT, WARN or STOP. Was: null");

        PluginParameters.builder()
                .setVerifyFail(null)
                .build();
    }

    @Test
    public void emptyValueIsNotOk() {
        thrown.expectMessage("verifyFail must be either SORT, WARN or STOP. Was: ");

        PluginParameters.builder()
                .setVerifyFail("")
                .build();
    }

    @Test
    public void wrongValueIsNotOk() {
        thrown.expectMessage("verifyFail must be either SORT, WARN or STOP. Was: gurka");

        PluginParameters.builder()
                .setVerifyFail("gurka")
                .build();
    }

}
