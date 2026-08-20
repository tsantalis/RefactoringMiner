// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NodeListViewer} class.
 */
class NodeListViewerTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test for {@link NodeListViewer#NodeListViewer} - {@code null} handling.
     */
    @Test
    void testNodeListViewerNull() {
        assertThrows(IllegalArgumentException.class, () -> new NodeListViewer(null));
    }

    /**
     * Test for {@link NodeListViewer#NodeListViewer} - nominal case.
     */
    @Test
    void testNodeListViewerNominal() {
        assertNotNull(new NodeListViewer(new HistoryBrowserModel()));
    }
}
