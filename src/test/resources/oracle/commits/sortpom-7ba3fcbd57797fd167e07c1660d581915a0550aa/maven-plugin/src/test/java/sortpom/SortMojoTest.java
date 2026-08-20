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
public class SortMojoTest {
    private final SortPomImpl sortPom = mock(SortPomImpl.class);
    private SortMojo sortMojo;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        sortMojo = new SortMojo();
        ReflectionHelper mojoHelper = new ReflectionHelper(sortMojo);
        mojoHelper.setField(sortPom);
        mojoHelper.setField("lineSeparator", "\n");
    }

    @Test
    public void executeShouldStartMojo() throws Exception {
        sortMojo.execute();

        verify(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));
        verify(sortPom).sortPom();
        verifyNoMoreInteractions(sortPom);
    }

    @Test
    public void thrownExceptionShouldBeConvertedToMojoException() throws MojoFailureException {
        doThrow(new FailureException("Gurka")).when(sortPom).sortPom();

        expectedException.expect(MojoFailureException.class);

        sortMojo.execute();
    }

    @Test
    public void thrownExceptionShouldBeConvertedToMojoExceptionInSetup() throws MojoFailureException {
        doThrow(new FailureException("Gurka")).when(sortPom).setup(any(SortPomLogger.class), any(PluginParameters.class));

        expectedException.expect(MojoFailureException.class);

        sortMojo.setup();
    }

    @Test
    public void skipParameterShouldSkipExecution() throws Exception {
        new ReflectionHelper(sortMojo).setField("skip", true);

        sortMojo.execute();

        verifyNoMoreInteractions(sortPom);
    }
}
