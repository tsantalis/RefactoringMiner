// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;

import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HyperlinkHandler} class.
 */
class HyperlinkHandlerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/17338">#17338</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket17338() throws Exception {
        JosmEditorPane help = new JosmEditorPane();
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        htmlKit.read(new StringReader("<a id=\"foo\" href=\"null#WrongAnchor\">bar</a>"), htmlDoc, 0);
        Element element = htmlDoc.getElement("foo");
        assertNotNull(element);
        help.setDocument(htmlDoc);
        new HyperlinkHandler(HelpBrowserTest.newHelpBrowser(), help)
            .hyperlinkUpdate(new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, null, null, element));
    }
}
