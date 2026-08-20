// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Basic tests for the clipboard utils class.
 * @author Michael Zangl
 */
class ClipboardUtilsTest {
    private static final class ThrowIllegalStateClipboard extends Clipboard {
        private int failingAccesses = 3;

        private ThrowIllegalStateClipboard(String name) {
            super(name);
        }

        @Override
        public synchronized Transferable getContents(Object requestor) {
            if (failingAccesses >= 0) {
                failingAccesses--;
                throw new IllegalStateException();
            }
            return super.getContents(requestor);
        }

        protected synchronized void setFailingAccesses(int failingAccesses) {
            this.failingAccesses = failingAccesses;
        }
    }

    private static final class SupportNothingTransferable implements Transferable {
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return false;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[0];
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * No dependencies
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link ClipboardUtils#getClipboard()}
     */
    @Test
    void testGetClipboard() {
        Clipboard c = ClipboardUtils.getClipboard();
        assertNotNull(c);
        assertSame(c, ClipboardUtils.getClipboard());
    }

    /**
     * Test {@link ClipboardUtils#copyString(String)} and {@link ClipboardUtils#getClipboardStringContent()}
     */
    @Test
    void testCopyPasteString() {
        ClipboardUtils.copyString("");
        assertEquals("", ClipboardUtils.getClipboardStringContent());
        ClipboardUtils.copyString("xxx\nx");
        assertEquals("xxx\nx", ClipboardUtils.getClipboardStringContent());

        ClipboardUtils.copy(new SupportNothingTransferable());
        assertEquals(null, ClipboardUtils.getClipboardStringContent());
    }

    /**
     * Test that {@link ClipboardUtils#getClipboardContent(Clipboard)} handles illegal state exceptions
     */
    @Test
    void testGetContentIllegalState() {
        ThrowIllegalStateClipboard throwingClipboard = new ThrowIllegalStateClipboard("test");

        throwingClipboard.setContents(new StringSelection(""), null);
        Transferable content = ClipboardUtils.getClipboardContent(throwingClipboard);
        assertTrue(content.isDataFlavorSupported(DataFlavor.stringFlavor));

        throwingClipboard.setFailingAccesses(50);
        content = ClipboardUtils.getClipboardContent(new ThrowIllegalStateClipboard("test"));
        assertNull(content);
    }

    /**
     * Test that {@link ClipboardUtils#getSystemSelection()} works in headless mode.
     */
    @Test
    void testSystemSelectionDoesNotFail() {
        assumeTrue(GraphicsEnvironment.isHeadless());
        assertNull(ClipboardUtils.getSystemSelection());
    }

    /**
     * Tests that {@code ClipboardUtils} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(ClipboardUtils.class);
    }
}
