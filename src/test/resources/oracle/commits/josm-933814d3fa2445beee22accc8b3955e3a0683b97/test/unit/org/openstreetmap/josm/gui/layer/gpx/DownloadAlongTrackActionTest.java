// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadAlongTrackAction} class.
 */
class DownloadAlongTrackActionTest {

    /**
     * The test rules for this test
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    private static PleaseWaitRunnable createTask(String file) throws Exception {
        // click "Download" when presented with the appropriate HelpAwareOptionPane
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(Collections.singletonMap(
            "DownloadAlongPanel", "Download"
        )) {
            // expected "message" for HelpAwareOptionPane call is not a simple string, so instead
            // just detect the class name of the message object
            @Override
            protected String getStringFromMessage(final Object message) {
                return message instanceof String ? (String) message : message.getClass().getSimpleName();
            }
        };

        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), DownloadAlongTrackActionTest.class.getName(), null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            // Perform action
            final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + file);
            final PleaseWaitRunnable retval = new DownloadAlongTrackAction(Collections.singleton(gpx)).createTask();

            // assert that we were indeed presented with the expected HelpAwareOptionPane
            assertEquals(1, haMocker.getInvocationLog().size());
            assertEquals(0, haMocker.getInvocationLog().get(0)[0]);
            assertEquals("DownloadAlongPanel", haMocker.getInvocationLog().get(0)[1]);
            assertEquals("Download from OSM along this track", haMocker.getInvocationLog().get(0)[2]);

            return retval;
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test action with nominal data set.
     * @throws Exception if an error occurs
     */
    @Test
    void testDownload() throws Exception {
        assertNotNull(createTask("minimal.gpx"));
    }

    /**
     * Test action with empty data set.
     * @throws Exception if an error occurs
     */
    @Test
    void testDownloadEmpty() throws Exception {
        assertNull(createTask("empty.gpx"));
    }
}
