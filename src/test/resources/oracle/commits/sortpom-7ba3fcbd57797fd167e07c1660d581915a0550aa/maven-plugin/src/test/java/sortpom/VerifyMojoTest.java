package sortpom;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import refutils.ReflectionHelper;
import sortpom.exception.FailureException;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

import static org.mockito.Mockito.*;

/**
 * @author bjorn
 * @since 2012-08-23
 */
public class VerifyMojoTest {
    private final SortPomImpl sortPom = mock(SortPomImpl.class);
    private VerifyMojo verifyMojo;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        verifyMojo = new VerifyMojo();
        ReflectionHelper mojoHelper = new ReflectionHelper(verifyMojo);
        mojoHelper.setField(sortPom);
        mojoHelper.setField("lineSeparator", "\n");
        mojoHelper.setField("verifyFail", "SORT");
    }

    @Test
    public void executeShouldStartMojo() throws Exception {
        verifyMojo.execute();

        verify(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));
        verify(sortPom).verifyPom();
        verifyNoMoreInteractions(sortPom);
    }

    @Test
    public void thrownExceptionShouldBeConvertedToMojoExceptionInExecute() throws MojoFailureException {
        doThrow(new FailureException("Gurka")).when(sortPom).verifyPom();

        expectedException.expect(MojoFailureException.class);

        verifyMojo.execute();
    }

    @Test
    public void thrownExceptionShouldBeConvertedToMojoExceptionInSetup() throws MojoFailureException {
        doThrow(new FailureException("Gurka")).when(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));

        expectedException.expect(MojoFailureException.class);

        verifyMojo.setup();
    }

    @Test
    public void skipParameterShouldSkipExecution() throws Exception {
        new ReflectionHelper(verifyMojo).setField("skip", true);

        verifyMojo.execute();

        verifyNoMoreInteractions(sortPom);
    }
}
