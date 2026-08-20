// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;

import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link HyperlinkHandler} class.
 */
@BasicPreferences
class HyperlinkHandlerTest {
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
