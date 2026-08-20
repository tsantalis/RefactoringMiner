// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.PseudoClasses;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import net.trajano.commons.testing.UtilityClassTestUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Unit tests of {@link ConditionFactory}.
 */
@BasicPreferences
class ConditionFactoryTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14368">#14368</a>.
     */
    @Test
    void testTicket14368() {
        assertThrows(MapCSSException.class,
                () -> ConditionFactory.createKeyValueCondition("name", "Rodovia ([A-Z]{2,3}-[0-9]{2,4}", Op.REGEX, Context.PRIMITIVE, false));
    }

    /**
     * Tests that {@code PseudoClasses} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(PseudoClasses.class);
    }

    /**
     * Tests that all functions have been registered to {@link ConditionFactory.PseudoClassCondition#CONDITION_MAP}
     */
    @Test
    void testAllPseudoClassesRegistered() {
        for (Method method : PseudoClasses.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.toString().contains("$jacocoInit")) {
                return;
            }
            String name = method.getName().replaceFirst("^_new$", "new");
            Context context = name.equals("sameTags") ? Context.LINK : Context.PRIMITIVE;
            ConditionFactory.PseudoClassCondition.createPseudoClassCondition(name, false, context);
            ConditionFactory.PseudoClassCondition.createPseudoClassCondition(name, true, context);
        }
    }
}
