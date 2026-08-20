// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link VectorRelation}
 * @author Taylor Smock
 * @since 17862
 */
class VectorRelationTest {
    @RegisterExtension
    JOSMTestRules rule = new JOSMTestRules();

    @Test
    void testMembers() {
        VectorNode node1 = new VectorNode("test");
        VectorNode node2 = new VectorNode("test");
        VectorWay way1 = new VectorWay("test");
        way1.setNodes(Arrays.asList(node1, node2));
        VectorRelationMember member1 = new VectorRelationMember("randomRole", node1);
        VectorRelationMember member2 = new VectorRelationMember("role2", way1);
        assertSame(node1, member1.getMember());
        assertSame(node1.getType(), member1.getType());
        assertEquals("randomRole", member1.getRole());
        assertSame(node1.getId(), member1.getUniqueId());
        // Not a way.
        assertThrows(ClassCastException.class, member1::getWay);

        assertTrue(member1.isNode());
        assertFalse(member1.isWay());
        assertFalse(member2.isNode());
        assertTrue(member2.isWay());
    }
}
