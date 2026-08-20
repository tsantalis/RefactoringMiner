// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link CoordinateInfoViewer} class.
 */
class CoordinateInfoViewerTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - {@code null} handling.
     */
    @Test
    void testCoordinateInfoViewerNull() {
        assertThrows(IllegalArgumentException.class, () -> new CoordinateInfoViewer(null));
    }

    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - nominal case.
     */
    @Test
    void testCoordinateInfoViewerNominal() {
        assertNotNull(new CoordinateInfoViewer(new HistoryBrowserModel()));
    }
}
