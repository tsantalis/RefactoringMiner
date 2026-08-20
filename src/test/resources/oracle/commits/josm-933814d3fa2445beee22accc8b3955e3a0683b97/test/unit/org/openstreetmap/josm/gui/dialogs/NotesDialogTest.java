// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import javax.swing.JLabel;
import javax.swing.JList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.dialogs.NotesDialog.NoteRenderer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NotesDialog}
 */
class NotesDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules josmTestRules = new JOSMTestRules().preferences();

    private Note createMultiLineNote() {
        Note note = new Note(LatLon.ZERO);
        note.setCreatedAt(Instant.now());
        note.addComment(new NoteComment(Instant.now(), User.createLocalUser(null), "foo\nbar\n\nbaz:\nfoo", null, false));
        return note;
    }

    /**
     * Unit test of {@link NoteRenderer}
     */
    @Test
    void testMultiLineNoteRendering() {
        Note note = createMultiLineNote();
        assertEquals("0: foo; bar; baz: foo",
                ((JLabel) new NoteRenderer().getListCellRendererComponent(new JList<>(), note, 0, false, false)).getText());
    }

    /**
     * Unit test of {@link NotesDialog#matchesNote}
     */
    @Test
    void testMatchesNote() {
        Note note = createMultiLineNote();
        assertTrue(NotesDialog.matchesNote(null, note));
        assertTrue(NotesDialog.matchesNote("", note));
        assertTrue(NotesDialog.matchesNote("foo", note));
        assertFalse(NotesDialog.matchesNote("xxx", note));
        assertFalse(NotesDialog.matchesNote("open", note));
        assertFalse(NotesDialog.matchesNote("new", note));
        assertFalse(NotesDialog.matchesNote("reopened", note));
    }
}
