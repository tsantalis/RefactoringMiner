package sortpom.exception;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author bjorn
 * @since 2013-10-19
 */
class ExceptionConverterTest {

    @Test
    void noExceptionShouldRunJustFine() throws MojoFailureException {
        ExceptionConverter exceptionConverter = new ExceptionConverter(() -> {
        });
        exceptionConverter.executeAndConvertException();
        assertThat(exceptionConverter, is(notNullValue()));
    }

    @Test
    void failureExceptionShouldThrowMojoFailureException() {
        FailureException failureException = new FailureException("Gurka");

        final Executable testMethod = () -> new ExceptionConverter(() -> {
            throw failureException;
        }).executeAndConvertException();

        final MojoFailureException thrown = assertThrows(MojoFailureException.class, testMethod);

        assertThat("Unexpected message", thrown.getMessage(), is(equalTo("Gurka")));
    }

    @Test
    void failureExceptionShouldKeepCause() {
        IllegalArgumentException cause = new IllegalArgumentException("not valid");
        FailureException failureException = new FailureException("Gurka", cause);

        final Executable testMethod = () -> new ExceptionConverter(() -> {
            throw failureException;
        }).executeAndConvertException();

        final MojoFailureException thrown = assertThrows(MojoFailureException.class, testMethod);

        assertAll(
                () -> assertThat("Unexpected message", thrown.getMessage(), is(equalTo("Gurka"))),
                () -> assertThat("Unexpected cause", thrown.getCause(), is(equalTo(cause)))
        );
    }

}
