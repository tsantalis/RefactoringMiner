// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link MultiValueCellEditor} class.
 */
class MultiValueCellEditorTest {
    /**
     * Unit test of {@link MultiValueCellEditor#MultiValueCellEditor}.
     */
    @Test
    void testMultiValueCellEditor() {
        assertNotNull(new MultiValueCellEditor().getTableCellEditorComponent(null, new MultiValueResolutionDecision(), false, 0, 0));
    }
}
