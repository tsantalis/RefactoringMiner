// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreferenceTest;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.plugins.PluginHandler.DeprecatedPlugin;
import org.openstreetmap.josm.plugins.PluginHandler.PluginInformationAction;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PluginHandler} class.
 */
@BasicPreferences
class PluginHandlerTest {
    /**
     * Unit test of methods {@link DeprecatedPlugin#equals} and {@link DeprecatedPlugin#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(DeprecatedPlugin.class).usingGetClass().verify();
    }

    /**
     * Unit test of {@link PluginHandler#buildListOfPluginsToLoad}.
     */
    @Test
    void testBuildListOfPluginsToLoad() {
        TestUtils.assumeWorkingJMockit();
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
                PluginHandler.UNMAINTAINED_PLUGINS.stream().collect(
                Collectors.toMap(PluginHandler::getUnmaintainedPluginMessage, p -> "Disable plugin")));
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();
        jopsMocker.getMockResultMap().put(
                PluginHandler.getRemovedPluginsMessage(PluginHandler.DEPRECATED_PLUGINS),
                JOptionPane.OK_OPTION
            );
        final String old = System.getProperty("josm.plugins");
        try {
            final String plugins = Stream.concat(
                    PluginHandler.DEPRECATED_PLUGINS.stream().map(p -> p.name),
                    PluginHandler.UNMAINTAINED_PLUGINS.stream()
            ).collect(Collectors.joining(","));
            System.setProperty("josm.plugins", plugins);
            List<PluginInformation> list = PluginHandler.buildListOfPluginsToLoad(null, null);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        } finally {
            if (old != null) {
                System.setProperty("josm.plugins", old);
            } else {
                System.clearProperty("josm.plugins");
            }
        }

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);

        assertEquals(PluginHandler.UNMAINTAINED_PLUGINS.size(), haMocker.getInvocationLog().size());
        invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Disable plugin", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginHandler#filterDeprecatedPlugins}.
     */
    @Test
    void testFilterDeprecatedPlugins() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            Collections.singletonMap(
                "<html>The following plugin is no longer necessary and has been deactivated:" +
                    "<ul><li>imagery (integrated into main program)</li></ul></html>",
                0
            )
        );

        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "imagery"));
        PluginHandler.filterDeprecatedPlugins(MainApplication.getMainFrame(), plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("imagery"));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginHandler#filterUnmaintainedPlugins}.
     */
    @Test
    void testFilterUnmaintainedPlugins() {
        TestUtils.assumeWorkingJMockit();
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            Collections.singletonMap(
                "<html>Loading of the plugin \"gpsbabelgui\" was requested.<br>" +
                    "This plugin is no longer developed and very likely will produce errors.<br>" +
                    "It should be disabled.<br>Delete from preferences?</html>",
                "Disable plugin"
            )
        );

        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "gpsbabelgui"));
        PluginHandler.filterUnmaintainedPlugins(MainApplication.getMainFrame(), plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("gpsbabelgui"));

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Disable plugin", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginInformationAction} class.
     * @throws PluginException if an error occurs
     */
    @Test
    void testPluginInformationAction() throws PluginException {
        TestUtils.assumeWorkingJMockit();
        final String expectedText = "Ant-Version: Apache Ant 1.9.6\n" +
                "Author: Don-vip\n" +
                "Created-By: 1.7.0_91-b02 (Oracle Corporation)\n" +
                "Manifest-Version: 1.0\n" +
                "Plugin-Canloadatruntime: true\n" +
                "Plugin-Class: org.openstreetmap.josm.plugins.fr.epci.EpciPlugin\n" +
                "Plugin-Date: 2015-11-19T08:21:07.645033Z\n" +
                "Plugin-Description: Handling of French EPCIs (boundary=local_authority)\n" +
                "Plugin-Early: true\n" +
                "Plugin-Link: http://wiki.openstreetmap.org/wiki/FR:JOSM/Fr:Plugin/EPCI-fr\n" +
                "Plugin-Mainversion: 7001\n" +
                "Plugin-Version: 31772\n";
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker() {
            @Override
            public String getStringFromMessage(final Object message) {
                return ((JosmTextArea) ((JScrollPane) message).getViewport().getView()).getText();
            }
        };
        jopsMocker.getMockResultMap().put(expectedText, 0);

        PluginInformationAction action = new PluginInformationAction(PluginPreferenceTest.getDummyPluginInformation());
        assertEquals(expectedText, action.getText());
        action.actionPerformed(null);

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Plugin information", invocationLogEntry[2]);
    }
}
