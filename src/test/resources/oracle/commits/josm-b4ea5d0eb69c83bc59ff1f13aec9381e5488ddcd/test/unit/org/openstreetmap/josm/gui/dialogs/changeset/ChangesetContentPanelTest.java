// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Unit tests of {@link ChangesetContentPanel} class.
 */
class ChangesetContentPanelTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().main();

    /**
     * Unit test of {@link ChangesetContentPanel#ChangesetContentPanel}.
     */
    @Test
    void testChangesetContentPanel() {
        assertNotNull(new ChangesetContentPanel());
    }
}
