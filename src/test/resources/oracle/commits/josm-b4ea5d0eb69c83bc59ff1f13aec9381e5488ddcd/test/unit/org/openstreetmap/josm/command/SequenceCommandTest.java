// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link SequenceCommand} class.
 */
// We need prefs for nodes.
@BasicPreferences
class SequenceCommandTest {
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @BeforeEach
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test {@link SequenceCommand#executeCommand()}
     */
    @Test
    void testExecute() {
        DataSet ds = new DataSet();
        final TestCommand command1 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode));
        TestCommand command2 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode2)) {
            @Override
            public boolean executeCommand() {
                assertTrue(command1.executed);
                return super.executeCommand();
            }
        };
        SequenceCommand command = new SequenceCommand("seq", Arrays.<Command>asList(command1, command2));

        command.executeCommand();

        assertTrue(command1.executed);
        assertTrue(command2.executed);
    }

    /**
     * Test {@link SequenceCommand#undoCommand()}
     */
    @Test
    void testUndo() {
        DataSet ds = new DataSet();
        final TestCommand command2 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode2));
        TestCommand command1 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode)) {
            @Override
            public void undoCommand() {
                assertFalse(command2.executed);
                super.undoCommand();
            }
        };
        SequenceCommand command = new SequenceCommand("seq", Arrays.<Command>asList(command1, command2));

        command.executeCommand();

        command.undoCommand();

        assertFalse(command1.executed);
        assertFalse(command2.executed);

        command.executeCommand();

        assertTrue(command1.executed);
        assertTrue(command2.executed);
    }

    /**
     * Test {@link SequenceCommand#executeCommand()} rollback if case of subcommand failure.
     */
    @Test
    void testExecuteRollback() {
        DataSet ds = new DataSet();
        TestCommand command1 = new TestCommand(ds, null);
        FailingCommand command2 = new FailingCommand(ds);
        TestCommand command3 = new TestCommand(ds, null);
        SequenceCommand command = new SequenceCommand("seq", Arrays.<Command>asList(command1, command2, command3));
        assertFalse(command.executeCommand());
        assertFalse(command1.executed);
        // Don't check command2 executed state as it's possible but not necessary to undo failed commands
        assertFalse(command3.executed);
        command.undoCommand();
    }

    /**
     * Test {@link SequenceCommand#executeCommand()} with continueOnError = true
     */
    @Test
    void testContinueOnErrors() {
        DataSet ds = new DataSet();
        TestCommand command1 = new TestCommand(ds, null);
        FailingCommand command2 = new FailingCommand(ds);
        TestCommand command3 = new TestCommand(ds, null);
        SequenceCommand command = new SequenceCommand("seq", Arrays.<Command>asList(command1, command2, command3), true);
        assertTrue(command.executeCommand());
        assertTrue(command1.executed);
        assertTrue(command3.executed);
        command.undoCommand();
        assertFalse(command1.executed);
        // Don't check command2 executed state as it's possible but not necessary to undo failed commands
        assertFalse(command3.executed);
    }

    /**
     * Test {@link SequenceCommand#undoCommand()}
     */
    @Test
    void testGetLastCommand() {
        DataSet ds = new DataSet();
        final TestCommand command1 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode));
        final TestCommand command2 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode2));

        assertEquals(command2, new SequenceCommand(ds, "seq", Arrays.asList(command1, command2), false).getLastCommand());
        assertNull(new SequenceCommand(ds, "seq", Collections.emptyList(), false).getLastCommand());
    }

    /**
     * Tests {@link SequenceCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    void testFillModifiedData() {
        DataSet ds = new DataSet();
        Command command1 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode));
        Command command2 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode2));
        Command command3 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingWay)) {
            @Override
            public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
                    Collection<OsmPrimitive> added) {
                deleted.addAll(primitives);
            }
        };
        Command command4 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingRelation)) {
            @Override
            public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
                    Collection<OsmPrimitive> added) {
                added.addAll(primitives);
            }
        };

        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        SequenceCommand command = new SequenceCommand("seq", command1, command2, command3, command4);
        command.fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {testData.existingNode, testData.existingNode2}, modified.toArray());
        assertArrayEquals(new Object[] {testData.existingWay}, deleted.toArray());
        assertArrayEquals(new Object[] {testData.existingRelation}, added.toArray());
    }

    /**
     * Tests {@link SequenceCommand#getParticipatingPrimitives()}
     */
    @Test
    void testGetParticipatingPrimitives() {
        DataSet ds = new DataSet();
        Command command1 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode));
        Command command2 = new TestCommand(ds, Arrays.<OsmPrimitive>asList(testData.existingNode2));

        SequenceCommand command = new SequenceCommand("seq", command1, command2);
        command.executeCommand();
        Collection<? extends OsmPrimitive> primitives = command.getParticipatingPrimitives();
        assertEquals(2, primitives.size());
        assertTrue(primitives.contains(testData.existingNode));
        assertTrue(primitives.contains(testData.existingNode2));
    }

    /**
     * Test {@link SequenceCommand#getDescriptionText()}
     */
    @Test
    void testDescription() {
        assertTrue(new SequenceCommand(new DataSet(), "test", Collections.emptyList(), false).getDescriptionText().matches("Sequence: test"));
    }

    /**
     * Unit test of methods {@link SequenceCommand#equals} and {@link SequenceCommand#hashCode}.
     */
    @Test
    void testEqualsContract() {
        DataSet ds = new DataSet();
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(SequenceCommand.class).usingGetClass()
            .withPrefabValues(Command.class,
                new AddCommand(ds, new Node(1)), new AddCommand(ds, new Node(2)))
            .withPrefabValues(DataSet.class,
                    new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    private static class TestCommand extends Command {
        protected final Collection<? extends OsmPrimitive> primitives;
        protected boolean executed;

        TestCommand(DataSet ds, Collection<? extends OsmPrimitive> primitives) {
            super(ds);
            this.primitives = primitives;
        }

        @Override
        public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
                Collection<OsmPrimitive> added) {
            modified.addAll(primitives);
        }

        @Override
        public String getDescriptionText() {
            fail("Should not be called");
            return "";
        }

        @Override
        public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
            return primitives;
        }

        @Override
        public boolean executeCommand() {
            assertFalse(executed, "Cannot execute twice");
            executed = true;
            return true;
        }

        @Override
        public void undoCommand() {
            assertTrue(executed, "Cannot undo without execute");
            executed = false;
        }

        @Override
        public String toString() {
            return "TestCommand [primitives=" + primitives + "]";
        }

    }

    private static class FailingCommand extends TestCommand {

        FailingCommand(DataSet ds) {
            super(ds, null);
        }

        @Override
        public boolean executeCommand() {
            executed = true;
            return false;
        }

        @Override
        public void undoCommand() {
            assertTrue(executed, "Cannot undo without execute");
            executed = false;
        }

        @Override
        public String toString() {
            return "FailingCommand";
        }
    }

    /**
     * Test {@link SequenceCommand#wrapIfNeeded}
     */
    @Test
    void testWrapIfNeeded() {
        DataSet ds = new DataSet();
        TestCommand command1 = new TestCommand(ds, Collections.<OsmPrimitive>singletonList(testData.existingNode));
        TestCommand command2 = new TestCommand(ds, Collections.<OsmPrimitive>singletonList(testData.existingNode2));
        assertSame(command1, SequenceCommand.wrapIfNeeded("foo", command1));
        assertNotSame(command1, SequenceCommand.wrapIfNeeded("foo", command1, command2));
        assertEquals(new SequenceCommand("foo", command1, command2), SequenceCommand.wrapIfNeeded("foo", command1, command2));
    }

    /**
     * Test SequenceCommand#createReportedException
     */
    @Test
    void testCreateReportedException() {
        DataSet ds = new DataSet();
        Command c1 = new TestCommand(ds, Collections.emptyList()) {
            @Override
            public boolean executeCommand() {
                fail("foo");
                return false;
            }

            @Override
            public String getDescriptionText() {
                return "foo command";
            }
        };
        SequenceCommand command = new SequenceCommand("test", c1);
        ReportedException reportedException = assertThrows(ReportedException.class, command::executeCommand);
        StringWriter stringWriter = new StringWriter();
        reportedException.printReportDataTo(new PrintWriter(stringWriter));
        assertEquals("=== REPORTED CRASH DATA ===\n" +
                "sequence_information:\n" +
                " - sequence_name: Sequence: test\n" +
                " - sequence_command: foo command\n" +
                " - sequence_index: 0\n" +
                " - sequence_commands: [null]\n" +
                " - sequence_commands_descriptions: [foo command]\n" +
                "\n", stringWriter.toString().replace(System.lineSeparator(), "\n"));
    }
}
