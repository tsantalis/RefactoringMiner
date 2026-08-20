// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.CancelAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.ChangesetDetailViewSynchronizer;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.CloseSelectedChangesetsAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadMyChangesets;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadSelectedChangesetContentAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadSelectedChangesetsAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.QueryAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.RemoveFromCacheAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.ShowDetailAction;
import org.openstreetmap.josm.gui.dialogs.changeset.query.ChangesetQueryDialog;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetCacheManager} class.
 */
@BasicPreferences
class ChangesetCacheManagerTest {
    /**
     * Unit test of {@link ChangesetCacheManager#destroyInstance}.
     */
    @Test
    void testDestroyInstance() {
        ChangesetCacheManager.destroyInstance();
    }

    /**
     * Unit test of {@link ChangesetCacheManager#buildButtonPanel},
     *              {@link ChangesetCacheManager#buildToolbarPanel}.
     *              {@link ChangesetCacheManager#buildModel}.
     */
    @Test
    void testBuild() {
        assertNotNull(ChangesetCacheManager.buildButtonPanel());
        assertNotNull(ChangesetCacheManager.buildToolbarPanel());
        assertNotNull(ChangesetCacheManager.buildModel());
    }

    /**
     * Unit test of {@link ChangesetCacheManager.ChangesetDetailViewSynchronizer} class.
     */
    @Test
    void testChangesetDetailViewSynchronizer() {
        new ChangesetDetailViewSynchronizer(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.emptyList();
            }
        }).valueChanged(null);

        new ChangesetDetailViewSynchronizer(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.singletonList(new Changeset());
            }
        }).valueChanged(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.CancelAction} class.
     */
    @Test
    void testCancelAction() {
        new CancelAction().actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.CloseSelectedChangesetsAction} class.
     */
    @Test
    void testCloseSelectedChangesetsAction() {
        CloseSelectedChangesetsAction action = new CloseSelectedChangesetsAction(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.singletonList(new Changeset());
            }
        });
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadMyChangesets} class.
     */
    @Test
    void testDownloadMyChangesets() {
        TestUtils.assumeWorkingJMockit();
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            Collections.singletonMap(
                "<html>JOSM is currently running with an anonymous user. It cannot download<br>"
                + "your changesets from the OSM server unless you enter your OSM user name<br>"
                + "in the JOSM preferences.</html>",
                "OK"
            )
        );

        new DownloadMyChangesets().actionPerformed(null);

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadSelectedChangesetContentAction} class.
     */
    @Test
    void testDownloadSelectedChangesetContentAction() {
        if (GraphicsEnvironment.isHeadless()) {
            TestUtils.assumeWorkingJMockit();
            // to allow us to construct a JDialog
            new WindowMocker();
        }

        DownloadSelectedChangesetContentAction action = new DownloadSelectedChangesetContentAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(new ActionEvent(new JDialog().getComponent(0), ActionEvent.ACTION_PERFORMED, "foo"));
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadSelectedChangesetsAction} class.
     */
    @Test
    void testDownloadSelectedChangesetsAction() {
        if (GraphicsEnvironment.isHeadless()) {
            TestUtils.assumeWorkingJMockit();
            // to allow us to construct a JDialog
            new WindowMocker();
        }

        DownloadSelectedChangesetsAction action = new DownloadSelectedChangesetsAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(new ActionEvent(new JDialog().getComponent(0), ActionEvent.ACTION_PERFORMED, "foo"));
    }

    /**
     * Unit test of {@link ChangesetCacheManager.QueryAction} class.
     */
    @Test
    void testQueryAction() {
        TestUtils.assumeWorkingJMockit();

        // set up mockers to simulate the dialog being cancelled
        final boolean[] dialogShown = new boolean[] {false};
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        new MockUp<JDialog>() {
            @Mock
            void setVisible(final Invocation invocation, final boolean visible) throws Exception {
                if (visible) {
                    ((JButton) TestUtils.getComponentByName((JDialog) invocation.getInvokedInstance(), "cancelButton")).doClick();
                    dialogShown[0] = true;
                }
                // critically, don't proceed into implementation
            }
        };
        new MockUp<ChangesetQueryDialog>() {
            @Mock
            void setVisible(final Invocation invocation, final boolean visible) throws Exception {
                if (GraphicsEnvironment.isHeadless()) {
                    // we have to mock the behaviour quite coarsely as much of ChangesetQueryDialog will
                    // raise a HeadlessException
                    if (visible) {
                        TestUtils.setPrivateField(ChangesetQueryDialog.class, invocation.getInvokedInstance(), "canceled", true);
                        dialogShown[0] = true;
                    }
                } else {
                    // proceeding into the implementation allows a bit more of the target code to be
                    // covered, actual mocking is performed on JDialog's setVisible()
                    invocation.proceed(visible);
                }
            }
        };

        new QueryAction().actionPerformed(null);

        assertTrue(dialogShown[0]);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.RemoveFromCacheAction} class.
     */
    @Test
    void testRemoveFromCacheAction() {
        RemoveFromCacheAction action = new RemoveFromCacheAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.ShowDetailAction} class.
     */
    @Test
    void testShowDetailAction() {
        new ShowDetailAction(ChangesetCacheManager.buildModel()).actionPerformed(null);
    }
}
