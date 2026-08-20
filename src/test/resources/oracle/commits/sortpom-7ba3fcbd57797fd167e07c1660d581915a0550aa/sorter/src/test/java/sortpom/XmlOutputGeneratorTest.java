package sortpom;

import org.jdom.Document;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.exception.FailureException;
import sortpom.parameter.PluginParameters;

import java.io.IOException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static sortpom.sort.ExpandEmptyElementTest.createXmlFragment;

/**
 * @author bjorn
 * @since 2020-01-12
 */
public class XmlOutputGeneratorTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void simulateIOExceptionToTriggerExceptionMessage() {
        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not format pom files content");

        Document document = spy(createXmlFragment());
        // Simulate an IOException (a check one, no less)
        when(document.getContent()).thenAnswer(invocation -> {
            throw new IOException();
        });

        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setFormatting("\n", true, false)
                .build());

        xmlOutputGenerator.getSortedXml(document);
    }
}
