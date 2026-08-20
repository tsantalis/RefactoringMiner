// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for class {@link CustomConfigurator}.
 */
@BasicPreferences
class CustomConfiguratorTest {
    /**
     * Test method for {@link CustomConfigurator#exportPreferencesKeysToFile}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testExportPreferencesKeysToFile() throws IOException {
        File tmp = File.createTempFile("josm.testExportPreferencesKeysToFile.lorem_ipsum", ".xml");

        Config.getPref().putList("lorem_ipsum", Arrays.asList(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                "Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor.",
                "Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi.",
                "Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat.",
                "Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim.",
                "Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue.",
                "Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales.",
                "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh.",
                "Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit."));
        CustomConfigurator.exportPreferencesKeysToFile(tmp.getAbsolutePath(), false, "lorem_ipsum");
        String xml = String.join("\n", Files.readAllLines(tmp.toPath(), StandardCharsets.UTF_8));
        assertTrue(xml.contains("<preferences operation=\"replace\">"));
        for (String entry : Config.getPref().getList("lorem_ipsum")) {
            assertTrue(xml.contains(entry), entry + "\nnot found in:\n" + xml);
        }

        Config.getPref().putList("test", Arrays.asList("11111111", "2222222", "333333333"));
        CustomConfigurator.exportPreferencesKeysByPatternToFile(tmp.getAbsolutePath(), true, "test");
        xml = String.join("\n", Files.readAllLines(tmp.toPath(), StandardCharsets.UTF_8));
        assertTrue(xml.contains("<preferences operation=\"append\">"));
        for (String entry : Config.getPref().getList("test")) {
            assertTrue(xml.contains(entry), entry + "\nnot found in:\n" + xml);
        }

        Utils.deleteFile(tmp);
    }

    /**
     * Test method for {@link CustomConfigurator#readXML}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testReadXML() throws IOException {
        // Test 1 - read(dir, file) + append
        Config.getPref().putList("test", Collections.<String>emptyList());
        assertTrue(Config.getPref().getList("test").isEmpty());
        CustomConfigurator.readXML(TestUtils.getTestDataRoot() + "customconfigurator", "append.xml");
        String log = PreferencesUtils.getLog();
        assertFalse(log.contains("Error"), log);
        assertEquals(Arrays.asList("11111111", "2222222", "JOSM"), Config.getPref().getList("test"));

        // Test 2 - read(file, pref) + replace
        Preferences pref = new Preferences();
        // avoid messing up preferences file (that makes all following unit tests fail)
        pref.enableSaveOnPut(false);
        pref.putList("lorem_ipsum", Arrays.asList("only 1 string"));
        assertEquals(1, pref.getList("lorem_ipsum").size());
        CustomConfigurator.readXML(new File(TestUtils.getTestDataRoot() + "customconfigurator", "replace.xml"), pref);
        log = PreferencesUtils.getLog();
        assertFalse(log.contains("Error"), log);
        assertEquals(9, pref.getList("lorem_ipsum").size());
    }
}
