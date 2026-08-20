// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MultiValueCellEditor} class.
 */
class MultiValueCellEditorTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link MultiValueCellEditor#MultiValueCellEditor}.
     */
    @Test
    void testMultiValueCellEditor() {
        assertNotNull(new MultiValueCellEditor().getTableCellEditorComponent(null, new MultiValueResolutionDecision(), false, 0, 0));
    }
}
