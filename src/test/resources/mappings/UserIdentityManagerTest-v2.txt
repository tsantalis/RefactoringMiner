// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Function;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests of {@link UserIdentityManager} class.
 */
@BasicPreferences
class UserIdentityManagerTest {
    private static UserInfo newUserInfo() {
        return newUserInfo(1, "a description");
    }

    private static UserInfo newUserInfo(int id, String description) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setDescription(description);
        return userInfo;
    }

    /**
     * Test singleton access.
     */
    @Test
    void testSingletonAccess() {

        UserIdentityManager im = UserIdentityManager.getInstance();

        // created ?
        assertNotNull(im);

        UserIdentityManager im2 = UserIdentityManager.getInstance();

        // only one instance
        assertSame(im, im2);
    }

    /**
     * Unit test of {@link UserIdentityManager#setAnonymous}.
     */
    @Test
    void testSetAnonymous() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        im.setPartiallyIdentified("test");
        im.setAnonymous();

        assertTrue(im.isAnonymous());
        assertFalse(im.isPartiallyIdentified());
        assertFalse(im.isFullyIdentified());

        assertEquals(0, im.getUserId());
        assertNull(im.getUserName());
        assertNull(im.getUserInfo());
        assertSame(User.getAnonymous(), im.asUser());
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - nominal case.
     */
    @Test
    void testSetPartiallyIdentified() {
        UserIdentityManager im = UserIdentityManager.getInstance();
        im.setPartiallyIdentified("test");

        assertFalse(im.isAnonymous());
        assertTrue(im.isPartiallyIdentified());
        assertFalse(im.isFullyIdentified());

        assertEquals(0, im.getUserId());
        assertEquals("test", im.getUserName());
        assertNull(im.getUserInfo());
        User usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test", usr.getName());
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - null case.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    void testSetPartiallyIdentifiedNull() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setPartiallyIdentified(null));
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - empty case.
     */
    @Test
    void testSetPartiallyIdentifiedEmpty() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setPartiallyIdentified(""));
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - blank case.
     */
    @Test
    void testSetPartiallyIdentifiedBlank() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setPartiallyIdentified("  \t  "));
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - nominal case.
     */
    @Test
    void testSetFullyIdentified() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        UserInfo userInfo = newUserInfo();

        im.setFullyIdentified("test", userInfo);

        assertFalse(im.isAnonymous());
        assertFalse(im.isPartiallyIdentified());
        assertTrue(im.isFullyIdentified());

        assertEquals(1, im.getUserId());
        assertEquals("test", im.getUserName());
        assertEquals(userInfo, im.getUserInfo());
        User usr = im.asUser();
        assertEquals(1, usr.getId());
        assertEquals("test", usr.getName());
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - null name case.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    void testSetFullyIdentifiedNullName() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setFullyIdentified(null, newUserInfo()));
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - empty name case.
     */
    @Test
    void testSetFullyIdentifiedEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setFullyIdentified("", newUserInfo()));
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - blank name case.
     */
    @Test
    void testSetFullyIdentifiedBlankName() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setFullyIdentified(" \t ", newUserInfo()));
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - null info case.
     */
    @Test
    void testSetFullyIdentifiedNullInfo() {
        assertThrows(IllegalArgumentException.class, () -> UserIdentityManager.getInstance().setFullyIdentified("test", null));
    }

    static Stream<Arguments> testInitFromPreferences() {
        return Stream.of(
          Arguments.of((Function<UserIdentityManager, Boolean>) UserIdentityManager::isAnonymous,
            new String[] {"osm-server.url", null, "osm-server.username", null},
            "Preferences include neither an url nor a user name => we have an anonymous user"),
          Arguments.of((Function<UserIdentityManager, Boolean>) UserIdentityManager::isAnonymous,
            new String[] {"osm-server.url", "http://api.openstreetmap.org", "osm-server.username", null},
            "Preferences include neither an url nor a user name => we have an anonymous user"),
          Arguments.of((Function<UserIdentityManager, Boolean>) UserIdentityManager::isPartiallyIdentified,
            new String[] {"osm-server.url", "http://api.openstreetmap.org", "osm-server.username", "test"},
            "Preferences include an user name => we have a partially identified user")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInitFromPreferences(Function<UserIdentityManager, Boolean> verifier, String[] config, String failureMessage) {
        if (config.length % 2 != 0) {
            fail("The arguments must be paired");
        }
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            for (int i = 0; i < config.length / 2; i++) {
                Config.getPref().put(config[2 * i], config[2 * i + 1]);
            }

            im.initFromPreferences();

            assertTrue(verifier.apply(im), failureMessage);
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is different from the current
     * user name and we are currently fully identifed => josm user becomes
     * partially identified
     *
     * Note: Test #4 since the other three are parameterized
     */
    @Test
    void testInitFromPreferences4() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", newUserInfo());

            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", "test2");

            im.initFromPreferences();

            assertTrue(im.isPartiallyIdentified());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is the same as the current
     * user name and we are currently fully identifed => josm user remains
     * fully identified
     */
    @Test
    void testInitFromPreferences5() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", new UserInfo());

            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", "test1");

            im.initFromPreferences();

            assertTrue(im.isFullyIdentified());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    @Test
    void testApiUrlChanged() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isAnonymous());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setPartiallyIdentified("test");

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());
    }

    @Test
    void testUserNameChanged() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Config.getPref().put("osm-server.username", "test");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());
        assertEquals(User.getAnonymous(), im.asUser());

        // reset it
        im.setPartiallyIdentified("test1");

        Config.getPref().put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        User usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Config.getPref().put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());
    }
}
