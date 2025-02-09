package org.dcache.gplazma.plugins;

import org.junit.Before;
import org.junit.Test;

import java.security.Principal;
import java.util.Properties;
import java.util.Set;

import org.dcache.auth.GidPrincipal;
import org.dcache.auth.GroupNamePrincipal;
import org.dcache.auth.UidPrincipal;
import org.dcache.auth.UserNamePrincipal;
import org.dcache.gplazma.NoSuchPrincipalException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NsswitchTest {

    private static final Principal USER_ROOT = new UserNamePrincipal("root");
    private static final Principal GROUP_ROOT = new GroupNamePrincipal("root");
    private static final Principal GROUP_WHEEL = new GroupNamePrincipal("wheel");
    private static final Principal ROOT_UID = new UidPrincipal(0);
    private static final Principal ROOT_GID = new GidPrincipal(0, false);
    private final static Properties EMPTY_PROPERTIES = new Properties();

    private GPlazmaIdentityPlugin _identityPlugin;

    @Before
    public void setUp() {
        _identityPlugin = new Nsswitch(EMPTY_PROPERTIES);
    }


    @Test
    public void testUidByName() throws NoSuchPrincipalException {
         assertEquals(ROOT_UID, _identityPlugin.map(USER_ROOT));
    }

    @Test
    public void testUnameByUid() throws NoSuchPrincipalException {
        assertTrue(_identityPlugin.reverseMap(ROOT_UID).contains(USER_ROOT));
    }

    @Test
    public void testGnameByUid() throws NoSuchPrincipalException {
        Set<Principal> principals = _identityPlugin.reverseMap(ROOT_GID);
        assertTrue(principals.contains(GROUP_ROOT) ||
                principals.contains(GROUP_WHEEL));
    }

}
