package org.dcache.gplazma.plugins;

import com.sun.jna.ptr.IntByReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.dcache.auth.GidPrincipal;
import org.dcache.auth.GroupNamePrincipal;
import org.dcache.auth.UidPrincipal;
import org.dcache.auth.UserNamePrincipal;
import org.dcache.auth.attributes.HomeDirectory;
import org.dcache.auth.attributes.ReadOnly;
import org.dcache.auth.attributes.RootDirectory;
import org.dcache.gplazma.AuthenticationException;
import org.dcache.gplazma.NoSuchPrincipalException;
import org.dcache.gplazma.plugins.Nsswitch.LibC;
import org.dcache.gplazma.plugins.Nsswitch.__group;
import org.dcache.gplazma.plugins.Nsswitch.__password;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.base.Preconditions.checkState;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.sun.jna.Structure"})
@PrepareForTest(Nsswitch.class)
/**
 * Tests for the Nsswitch gPlazma plugin.  This plugin uses JNA to invoke
 * various libc methods to discover the host system's configuration for
 * mapping usernames and groupnames to uids and gids, respectively.
 *
 * For testing purposes, this makes testing awkward for several reasons:
 *
 *   1.  unit tests should not depend on JVM-platform features.  JNA isn't
 *       supported by all JVM platforms.
 *
 *   2.  unit tests should not depend on OS-specific functionality.  OSes exist
 *       that don't provide the required libc functions.
 *
 *   2.  unit tests should not depend on external configuration.  On most Unix-
 *       like systems,  there exists a user with {name=root, uid=0, gid=0},
 *       however this is insufficient for testing all functionality and
 *       additional mappings are much less certain.
 *
 * This class mocks the calls to the LibC class to return predefined responses,
 * so allowing arbitrary testing, independent of underlying JVM and OS.
 * Unfortunately, doing this requires employing a "large hammer" (in the
 * form of PowerMock) to beat the dependent classes into submission.
 */
public class NsswitchTest
{
    private LibC _libc;
    private Nsswitch _plugin;
    private Principal _identityMapResult;
    private Set<Principal> _identityReverseMapResult;
    private Set<Principal> _loginMapResult;
    private Set<Object> _attributes;

    @Before
    public void setUp() throws Exception
    {
        _libc = mock(LibC.class);
        _plugin = new Nsswitch(_libc);
        _identityMapResult = null;
        _identityReverseMapResult = null;
        _loginMapResult = null;
        _attributes = null;

        IntByReference intByRef = Whitebox.newInstance(SimpleIntStorage.class);
        whenNew(IntByReference.class).withNoArguments().thenReturn(intByRef);
    }


    @Test
    public void shouldIdentityMapNameToUid() throws NoSuchPrincipalException
    {
        given(aUser().withName("kermit").withUid(100));

        whenIdentityMap(userNamePrincipal("kermit"));

        assertThat(_identityMapResult, is(equalTo(uidPrincipal(100))));
    }

    @Test(expected=NoSuchPrincipalException.class)
    public void shouldFailWhenIdentityMapUnknownName()
            throws NoSuchPrincipalException
    {
        given(noUser().withName("kermit"));

        whenIdentityMap(userNamePrincipal("kermit"));
    }

    @Test
    public void shouldIdentityReverseMapUidToName()
            throws NoSuchPrincipalException
    {
        given(aUser().withName("kermit").withUid(100));

        whenIdentityReverseMap(uidPrincipal(100));

        assertThat(_identityReverseMapResult, hasItem(userNamePrincipal("kermit")));
    }

    @Test(expected=NoSuchPrincipalException.class)
    public void shouldFailWhenIdentityReverseMapUnknownUid()
            throws NoSuchPrincipalException
    {
        given(noUser().withUid(100));

        whenIdentityReverseMap(uidPrincipal(100));
    }

    @Test
    public void shouldIdentityMapNameToGid() throws NoSuchPrincipalException
    {
        given(aGroup().withName("it").withGid(200));

        whenIdentityMap(groupNamePrincipal("it"));

        // REVISIT: should a group name be mapped to a primary gid?
        assertThat(_identityMapResult, is(nonPrimaryGidPrincipal(200)));
    }

    @Test(expected=NoSuchPrincipalException.class)
    public void shouldFailWhenIdentityMapUnknownGroupName()
            throws NoSuchPrincipalException
    {
        given(noGroup().withName("it"));

        whenIdentityMap(groupNamePrincipal("it"));
    }

    @Test
    public void shouldIdentityReverseMapPrimaryGidToName()
            throws NoSuchPrincipalException
    {
        given(aGroup().withName("it").withGid(200));

        whenIdentityReverseMap(primaryGidPrincipal(200));

        assertThat(_identityReverseMapResult, hasItem(groupNamePrincipal("it")));
    }

    @Test
    public void shouldIdentityReverseMapGidToName()
            throws NoSuchPrincipalException
    {
        given(aGroup().withName("it").withGid(200));

        whenIdentityReverseMap(nonPrimaryGidPrincipal(200));

        assertThat(_identityReverseMapResult, hasItem(groupNamePrincipal("it")));
    }


    @Test
    public void shouldLoginMapForUserWithSingleGroup()
            throws AuthenticationException
    {
        given(aUser().withName("kermit").withUid(100).withGid(200));

        // REVISIT consider using PrincipalSetMaker
        whenLoginMap(newHashSet(userNamePrincipal("kermit")));

        assertThat(_loginMapResult, hasItem(uidPrincipal(100)));
        assertThat(_loginMapResult, hasItem(primaryGidPrincipal(200)));
    }

    @Test
    public void shouldLoginMapForUserWithMultipleGroups()
            throws AuthenticationException
    {
        given(aUser().withName("kermit").withUid(100).withGid(200).withExtraGids(210,220));

        // REVISIT consider using PrincipalSetMaker
        whenLoginMap(newHashSet(userNamePrincipal("kermit")));

        assertThat(_loginMapResult, hasItem(uidPrincipal(100)));
        assertThat(_loginMapResult, hasItem(primaryGidPrincipal(200)));
        assertThat(_loginMapResult, hasItem(nonPrimaryGidPrincipal(210)));
        assertThat(_loginMapResult, hasItem(nonPrimaryGidPrincipal(220)));
    }

    @Test
    public void shouldLoginSession() throws AuthenticationException
    {
        // no "given"s since behaviour is independent of system state

        whenLoginSession(null); // null is OK since principals are ignored

        assertThat(_attributes, hasItem(homeDirectory("/")));
        assertThat(_attributes, hasItem(rootDirectory("/")));
        assertThat(_attributes, hasItem(readOnly(false)));
    }


    private void whenIdentityMap(Principal p) throws NoSuchPrincipalException
    {
        _identityMapResult = _plugin.map(p);
    }

    private void whenIdentityReverseMap(Principal p) throws NoSuchPrincipalException
    {
        _identityReverseMapResult = _plugin.reverseMap(p);
    }

    private void whenLoginMap(Set<Principal> principals) throws AuthenticationException
    {
        _loginMapResult = new HashSet<>(principals);
        _plugin.map(_loginMapResult);
    }

    private void whenLoginSession(Set<Principal> principals) throws AuthenticationException
    {
        _attributes = new HashSet<>();
        _plugin.session(principals, _attributes);
    }

    private void given(UserInfo user)
    {
        if (user.hasName()) {
            when(_libc.getpwnam(user.getName())).thenReturn(user.buildPassword());
        }

        if (user.hasUid()) {
            when(_libc.getpwuid(user._uid)).thenReturn(user.buildPassword());
        }

        if (user.hasName() && user.hasGid()) {
            when(_libc.getgrouplist(eq(user.getName()), eq(user.getGid()),
                    any(int[].class), any(IntByReference.class))).
                    thenAnswer(new GetGroupListAnswer(user._extraGids));
        }
    }

    private void given(GroupInfo group)
    {
        if (group.hasName()) {
            when(_libc.getgrnam(group.getName())).thenReturn(group.buildGroup());
        }

        if (group.hasGid()) {
            when(_libc.getgrgid(group.getGid())).thenReturn(group.buildGroup());
        }
    }

    private Principal uidPrincipal(int uid)
    {
        return new UidPrincipal(uid);
    }

    private Principal primaryGidPrincipal(int uid)
    {
        return new GidPrincipal(uid, true);
    }

    private Principal nonPrimaryGidPrincipal(int uid)
    {
        return new GidPrincipal(uid, false);
    }

    private Principal userNamePrincipal(String name)
    {
        return new UserNamePrincipal(name);
    }

    private Principal groupNamePrincipal(String name)
    {
        return new GroupNamePrincipal(name);
    }

    private Object homeDirectory(String dir)
    {
        return new HomeDirectory(dir);
    }

    private Object rootDirectory(String dir)
    {
        return new RootDirectory(dir);
    }

    private Object readOnly(boolean isReadOnly)
    {
        return new ReadOnly(isReadOnly);
    }

    private UserInfo aUser()
    {
        return new UserInfo();
    }

    private UserInfo noUser()
    {
        return new UserInfo().isAbsent();
    }

    private GroupInfo aGroup()
    {
        return new GroupInfo();
    }

    private GroupInfo noGroup()
    {
        return new GroupInfo().isAbsent();
    }

    /**
     * Fluent class for collecting information about a POSIX user, which is
     * used to build a __password object.  It is also used to hold additional
     * group membership (the gids) of this user.
     */
    private class UserInfo
    {
        String _name;
        boolean _hasName;
        int _uid;
        boolean _hasUid;
        int _gid;
        boolean _hasGid;
        int[] _extraGids = new int[0];
        boolean _isAbsent;

        UserInfo withName(String name)
        {
            _name = name;
            _hasName = true;
            return this;
        }

        UserInfo withUid(int uid)
        {
            _uid = uid;
            _hasUid = true;
            return this;
        }

        UserInfo withGid(int gid)
        {
            _gid = gid;
            _hasGid = true;
            return this;
        }

        UserInfo withExtraGids(int... gids)
        {
            _extraGids = gids;
            return this;
        }

        UserInfo isAbsent()
        {
            _isAbsent = true;
            return this;
        }

        boolean hasName()
        {
            return _hasName;
        }

        boolean hasUid()
        {
            return _hasUid;
        }

        boolean hasGid()
        {
            return _hasGid;
        }

        String getName()
        {
            checkState(_hasName, "no name");
            return _name;
        }

        int getUid()
        {
            checkState(_hasUid, "no uid");
            return _uid;
        }

        int getGid()
        {
            checkState(_hasGid, "no gid");
            return _gid;
        }

        __password buildPassword()
        {
            if (_isAbsent) {
                return null;
            }

            __password password = Whitebox.newInstance(__password.class);
            password.name = _name;
            password.uid = _uid;
            password.gid = _gid;
            return password;
        }
    }

    /**
     * Fluent class to hold information about a POSIX group and build a
     * corresponding __group object.
     */
    private class GroupInfo
    {
        private String _name;
        private int _gid;
        private boolean _hasName;
        private boolean _hasGid;
        private boolean _isAbsent;

        GroupInfo withName(String name)
        {
            _name = name;
            _hasName = true;
            return this;
        }

        GroupInfo withGid(int gid)
        {
            _gid = gid;
            _hasGid = true;
            return this;
        }

        GroupInfo isAbsent()
        {
            _isAbsent = true;
            return this;
        }

        boolean hasName()
        {
            return _hasName;
        }

        boolean hasGid()
        {
            return _hasGid;
        }

        int getGid()
        {
            checkState(_hasGid, "gid not set");
            return _gid;
        }

        String getName()
        {
            checkState(_hasName, "name not set");
            return _name;
        }

        __group buildGroup()
        {
            if (_isAbsent) {
                return null;
            }
            __group group = Whitebox.newInstance(__group.class);
            group.name = _name;
            group.gid = _gid;
            return group;
        }
    }

    /**
     * Class to hold login for the getgrouplist method of _libc.  The Nsswitch
     * class makes use of the getgrouplist(3) function, which uses
     * negotiation to establishing how many groups a user has membership.  In
     * general, the Nsswitch class invokes getgrouplist twice; once to discover
     * the number of gids and the second time to acquire the list.  Therefore
     * logic is needed to respond correctly.
     */
    private class GetGroupListAnswer implements Answer
    {
        private final int[] _gids;

        GetGroupListAnswer(int[] gids)
        {
            _gids = gids;
        }

        @Override
        public Object answer(InvocationOnMock invocation)
        {
            Object[] args = invocation.getArguments();
            IntByReference ngroups = (IntByReference) args[3];
            int count = _gids.length;

            if (ngroups.getValue() < count) {
                ngroups.setValue(count);
                return Integer.valueOf(-1);
            }

            int[] gids = (int[]) args[2];
            System.arraycopy(_gids, 0, gids, 0, count);
            return Integer.valueOf(count);
        }
    }

    /**
     * The IntByReference class provides pointer-like behaviour that allows
     * a method to update the supplied argument.  The JNA implementation
     * achieves this using JNA code, which we wish to avoid.  This method
     * is a stand-in replacement that, while useless for JNA, provides the same
     * Java-side functionality.  Although this class works, it requires
     * additional PowerMock magic to suppress the default constructor of the
     * super class.
     */
    private class SimpleIntStorage extends IntByReference
    {
        private int _value;

        @Override
        public int getValue()
        {
            return _value;
        }

        @Override
        public void setValue(int value)
        {
            _value = value;
        }
    }
}
