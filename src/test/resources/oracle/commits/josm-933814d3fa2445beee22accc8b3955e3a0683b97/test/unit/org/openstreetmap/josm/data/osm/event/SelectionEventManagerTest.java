// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link SelectionEventManager}
 * @author Michael Zangl
 * @since 12048
 */
class SelectionEventManagerTest {
    private final class SelectionListener implements DataSelectionListener {
        private Collection<? extends OsmPrimitive> newSelection;
        private final String name;

        SelectionListener(String name) {
            this.name = name;
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            this.newSelection = event.getSelection();
        }
    }

    /**
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Tests that events in the active layer are propagated.
     */
    @Test
    void testEventPropagation() {
        // automatically adds the layers
        CommandTestDataWithRelation testData1 = new CommandTestDataWithRelation();
        CommandTestDataWithRelation testData2 = new CommandTestDataWithRelation();
        MainApplication.getLayerManager().setActiveLayer(testData1.layer);
        assertEquals(testData1.layer, MainApplication.getLayerManager().getEditLayer());

        SelectionListener listener3 = new SelectionListener("normal");
        SelectionListener listener4 = new SelectionListener("edt");
        SelectionEventManager instance = SelectionEventManager.getInstance();
        instance.addSelectionListener(listener3);
        instance.addSelectionListenerForEdt(listener4);
        List<SelectionListener> listeners = Arrays.asList(listener3, listener4);
        assertSelectionEquals(listeners, null);

        // active layer, should change
        testData1.layer.getDataSet().setSelected(testData1.existingNode.getPrimitiveId());
        assertSelectionEquals(listeners, new HashSet<OsmPrimitive>(Arrays.asList(testData1.existingNode)));

        testData1.layer.getDataSet().clearSelection(testData1.existingNode.getPrimitiveId());
        assertSelectionEquals(listeners, new HashSet<OsmPrimitive>(Arrays.asList()));

        testData1.layer.getDataSet().addSelected(testData1.existingNode2.getPrimitiveId());
        assertSelectionEquals(listeners, new HashSet<OsmPrimitive>(Arrays.asList(testData1.existingNode2)));

        // changing to other dataset should trigger a empty selection
        MainApplication.getLayerManager().setActiveLayer(testData2.layer);
        assertSelectionEquals(listeners, new HashSet<OsmPrimitive>(Arrays.asList()));

        // This should not trigger anything, since the layer is not active any more.
        testData1.layer.getDataSet().clearSelection(testData1.existingNode.getPrimitiveId());
        assertSelectionEquals(listeners, null);

        testData2.layer.getDataSet().setSelected(testData2.existingNode.getPrimitiveId());
        assertSelectionEquals(listeners, new HashSet<OsmPrimitive>(Arrays.asList(testData2.existingNode)));

        // removal
        instance.removeSelectionListener(listener3);
        instance.removeSelectionListener(listener4);

        // no event triggered now
        testData2.layer.getDataSet().setSelected(testData2.existingNode2.getPrimitiveId());
        assertSelectionEquals(listeners, null);
    }

    private void assertSelectionEquals(List<SelectionListener> listeners, Object should) {
        // sync
        GuiHelper.runInEDTAndWait(() -> { });
        for (SelectionListener listener : listeners) {
            assertEquals(should, listener.newSelection, listener.name);
            listener.newSelection = null;
        }
    }

}
