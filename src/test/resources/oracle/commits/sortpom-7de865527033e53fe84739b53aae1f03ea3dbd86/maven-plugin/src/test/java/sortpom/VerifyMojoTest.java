package sortpom;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import refutils.ReflectionHelper;
import sortpom.exception.FailureException;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author bjorn
 * @since 2012-08-23
 */
class VerifyMojoTest {
    private final SortPomImpl sortPom = mock(SortPomImpl.class);
    private VerifyMojo verifyMojo;

    @BeforeEach
    void setup() {
        verifyMojo = new VerifyMojo();
        ReflectionHelper mojoHelper = new ReflectionHelper(verifyMojo);
        mojoHelper.setField(sortPom);
        mojoHelper.setField("lineSeparator", "\n");
        mojoHelper.setField("verifyFail", "SORT");
        mojoHelper.setField("verifyFailOn", "xmlElements");
    }

    @Test
    void executeShouldStartMojo() throws Exception {
        verifyMojo.execute();

        verify(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));
        verify(sortPom).verifyPom();
        verifyNoMoreInteractions(sortPom);
    }

    @Test
    void thrownExceptionShouldBeConvertedToMojoExceptionInExecute() {
        doThrow(new FailureException("Gurka")).when(sortPom).verifyPom();

        final Executable testMethod = () -> verifyMojo.execute();

        final MojoFailureException thrown = assertThrows(MojoFailureException.class, testMethod);

        assertThat("Unexpected message", thrown.getMessage(), is(equalTo("Gurka")));
    }

    @Test
    void thrownExceptionShouldBeConvertedToMojoExceptionInSetup() {
        doThrow(new FailureException("Gurka")).when(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));

        final Executable testMethod = () -> verifyMojo.setup();

        final MojoFailureException thrown = assertThrows(MojoFailureException.class, testMethod);

        assertThat("Unexpected message", thrown.getMessage(), is(equalTo("Gurka")));
    }

    @Test
    void skipParameterShouldSkipExecution() throws Exception {
        new ReflectionHelper(verifyMojo).setField("skip", true);

        verifyMojo.execute();

        verifyNoMoreInteractions(sortPom);
    }

}
