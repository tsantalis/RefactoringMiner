package sortpom;

import org.jdom.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.exception.FailureException;
import sortpom.parameter.PluginParameters;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static sortpom.sort.XmlFragment.createXmlFragment;

/**
 * @author bjorn
 * @since 2020-01-12
 */
class XmlOutputGeneratorTest {

    @Test
    void simulateIOExceptionToTriggerExceptionMessage() {

        Document document = spy(createXmlFragment());
        // Simulate an IOException (a check one, no less)
        when(document.getContent()).thenAnswer(invocation -> {
            throw new IOException();
        });

        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setFormatting("\n", true, true, false)
                .build());

        final Executable testMethod = () -> xmlOutputGenerator.getSortedXml(document);

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat("Unexpected message", thrown.getMessage(), is(equalTo("Could not format pom files content")));
    }

}
