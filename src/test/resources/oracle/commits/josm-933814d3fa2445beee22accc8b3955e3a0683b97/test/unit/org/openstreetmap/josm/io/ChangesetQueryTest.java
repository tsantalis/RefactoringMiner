// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of {@link ChangesetQuery}
 */
class ChangesetQueryTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit tests of {@link ChangesetQuery#getQueryString()}
     */
    @Test
    void testQueryString() {
        assertEquals("", new ChangesetQuery().getQueryString());
        assertThrows(IllegalStateException.class, () -> ChangesetQuery.forCurrentUser().getQueryString());
        assertEquals("display_name=foobar",
                new ChangesetQuery().forUser("foobar").getQueryString());
        assertEquals("user=4713",
                new ChangesetQuery().forUser(4713).getQueryString());
        assertEquals("time=1970-01-01T00:00:00Z",
                new ChangesetQuery().closedAfter(Instant.EPOCH).getQueryString());
        assertEquals("changesets=47,13",
                new ChangesetQuery().forChangesetIds(Arrays.asList(47L, 13L)).getQueryString());
        assertEquals("time=1971-02-02T16:22:18.368Z,1970-01-01T00:00:00Z",
                new ChangesetQuery().closedAfterAndCreatedBefore(Instant.ofEpochMilli(1L << 35L), Instant.EPOCH).getQueryString());
        assertEquals("bbox=12.0,34.0,56.0,78.0",
                new ChangesetQuery().inBbox(12, 34, 56, 78).getQueryString());
        assertEquals("closed=true",
                new ChangesetQuery().beingClosed(true).getQueryString());
        assertEquals("open=true",
                new ChangesetQuery().beingOpen(true).getQueryString());
    }
}
