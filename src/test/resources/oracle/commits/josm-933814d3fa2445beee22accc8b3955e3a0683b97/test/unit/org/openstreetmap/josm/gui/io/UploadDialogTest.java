// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.io.UploadDialog.UploadAction;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UploadDialog} class.
 */
class UploadDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    private static class MockUploadDialog extends JOptionPane implements IUploadDialog {
        private final String source;
        private final String comment;

        MockUploadDialog(final String comment, final String source) {
            this.source = source;
            this.comment = comment;
        }

        @Override
        public void rememberUserInput() {
            // Do nothing
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void handleMissingSource() {
        }

        @Override
        public void handleMissingComment() {
        }

        @Override
        public void handleIllegalChunkSize() {
            // Do nothing
        }

        @Override
        public UploadStrategySpecification getUploadStrategySpecification() {
            return new UploadStrategySpecification();
        }

        @Override
        public String getUploadSource() {
            return source;
        }

        @Override
        public String getUploadComment() {
            return comment;
        }

        @Override
        public Map<String, String> getTags(boolean keepEmpty) {
            return new ConcurrentHashMap<>();
        }

        @Override
        public void forceUpdateActiveField() {
            // Do nothing
        }
    }

    /**
     * Test of {@link UploadDialog.CancelAction} class.
     */
    @Test
    void testCancelAction() {
        if (GraphicsEnvironment.isHeadless()) {
            TestUtils.assumeWorkingJMockit();
            new WindowMocker();
        }
        MockUploadDialog uploadDialog = new MockUploadDialog(null, null);
        new UploadDialog.CancelAction(uploadDialog).actionPerformed(null);
    }

    /**
     * Test of {@link UploadDialog.UploadAction#isUploadCommentTooShort} method.
     */
    @Test
    void testIsUploadCommentTooShort() {
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort(""));
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("test"));
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("测试"));
        assertFalse(UploadDialog.UploadAction.isUploadCommentTooShort("geometric corrections"));
        assertFalse(UploadDialog.UploadAction.isUploadCommentTooShort("几何校正"));
        // test with unassigned unicode characters ==> no unicode block
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("\u0860"));
    }

    private static void doTestGetLastChangesetTagFromHistory(String historyKey, Supplier<String> methodToTest, String def) {
        Config.getPref().putList(historyKey, null);
        Config.getPref().putInt(BasicUploadSettingsPanel.HISTORY_LAST_USED_KEY, 0);
        Config.getPref().putInt(BasicUploadSettingsPanel.HISTORY_MAX_AGE_KEY, 30);
        assertNull(methodToTest.get());          // age NOK (history empty)
        Config.getPref().putList(historyKey, Arrays.asList("foo"));
        assertNull(methodToTest.get());          // age NOK (history not empty)
        Config.getPref().putLong(BasicUploadSettingsPanel.HISTORY_LAST_USED_KEY, System.currentTimeMillis() / 1000);
        assertEquals("foo", methodToTest.get()); // age OK, history not empty
        Config.getPref().putList(historyKey, null);
        assertEquals(def, methodToTest.get());   // age OK, history empty
    }

    /**
     * Test of {@link UploadDialog#getLastChangesetCommentFromHistory} method.
     */
    @Test
    void testGetLastChangesetCommentFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.HISTORY_KEY,
                UploadDialog::getLastChangesetCommentFromHistory,
                null);
    }

    /**
     * Test of {@link UploadDialog#getLastChangesetSourceFromHistory} method.
     */
    @Test
    void testGetLastChangesetSourceFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.SOURCE_HISTORY_KEY,
                UploadDialog::getLastChangesetSourceFromHistory,
                BasicUploadSettingsPanel.getDefaultSources().get(0));
    }

    private static void doTestValidateUploadTag(String prefix) {
        List<String> def = Collections.emptyList();
        Config.getPref().putList(prefix + ".mandatory-terms", null);
        Config.getPref().putList(prefix + ".forbidden-terms", null);
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def, def));

        Config.getPref().putList(prefix + ".mandatory-terms", Arrays.asList("foo"));
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def, def));
        assertEquals("The following required terms are missing: [foo]",
                UploadAction.validateUploadTag("bar", prefix, def, def, def));

        Config.getPref().putList(prefix + ".forbidden-terms", Arrays.asList("bar"));
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def, def));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("foobar", prefix, def, def, def));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("FOOBAR", prefix, def, def, def));

        Config.getPref().putList(prefix + ".exception-terms", Arrays.asList("barosm"));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("foobar", prefix, def, def, def));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("FOOBAR", prefix, def, def, def));
        assertNull(UploadAction.validateUploadTag("foobarosm", prefix, def, def, def));
        assertNull(UploadAction.validateUploadTag("FOOBAROSM", prefix, def, def, def));
    }

    /**
     * Test of {@link UploadDialog.UploadAction#validateUploadTag} method.
     */
    @Test
    void testValidateUploadTag() {
        doTestValidateUploadTag("upload.comment");
        doTestValidateUploadTag("upload.source");
    }

    @Test
    void testGetCommentWithDataSetHashTag() {
        assertEquals("", UploadDialog.getCommentWithDataSetHashTag(null, null));
        DataSet ds = new DataSet();
        assertEquals("foo", UploadDialog.getCommentWithDataSetHashTag("foo", ds));
        ds.getChangeSetTags().put("hashtags", "bar");
        assertEquals("foo #bar", UploadDialog.getCommentWithDataSetHashTag("foo", ds));
        ds.getChangeSetTags().put("hashtags", "bar;baz;#bar");
        assertEquals("foo #bar #baz", UploadDialog.getCommentWithDataSetHashTag("foo", ds));
    }
}
