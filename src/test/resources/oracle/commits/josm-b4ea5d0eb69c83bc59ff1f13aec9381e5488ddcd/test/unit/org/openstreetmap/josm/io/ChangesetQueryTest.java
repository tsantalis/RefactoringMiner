// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of {@link ChangesetQuery}
 */
@BasicPreferences
class ChangesetQueryTest {
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
