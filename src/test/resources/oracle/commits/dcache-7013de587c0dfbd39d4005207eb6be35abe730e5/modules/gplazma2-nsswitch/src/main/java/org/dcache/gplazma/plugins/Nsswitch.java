package org.dcache.gplazma.plugins;

import com.google.common.collect.Sets;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.security.Principal;
import java.util.Properties;
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

import static com.google.common.collect.Iterables.filter;

/**
 * {@code GPlazmaMappingPlugin} and {@code GPlazmaIdentityPlugin} implementation for
 * Unix based systems. The actual mapping happens according to systems {@literal /etc/nsswith.conf}
 * configuration. The following mapping takes place:
 * <pre>
 *     login name: {@link UserNamePrincipal}
 *     uid       : {@link UidPrincipal}
 *     gid       : {@link GidPrincipal}, primary
 *     other gids: {@link GidPrincipal}
 * </pre>
 */
public class Nsswitch implements GPlazmaMappingPlugin, GPlazmaIdentityPlugin, GPlazmaSessionPlugin {

    /**
     * handle to libc.
     */
    private final LibC _libc;

    /**
     * Create an instance of {@link Nsswitch} gplazma plugin.
     *
     * @param properties
     * @throws UnsatisfiedLinkError if unable to load system libraries.
     */
    public Nsswitch(Properties properties) throws UnsatisfiedLinkError {
        _libc = (LibC) Native.loadLibrary("c", LibC.class);
    }

    private __password findPasswordRecord(Set<Principal> principals) {
        for (UserNamePrincipal principal: filter(principals, UserNamePrincipal.class)) {
            __password p = _libc.getpwnam(principal.getName());
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void map(Set<Principal> principals) throws AuthenticationException {
        __password p = findPasswordRecord(principals);
        if (p != null) {
            principals.add(new UidPrincipal(p.uid));
            principals.add(new GidPrincipal(p.gid, true));
            int[] gids = groupsOf(p);
            for (int id : gids) {
                principals.add(new GidPrincipal(id, false));
            }
        } else {
            throw new AuthenticationException("no mapping");
        }
    }

    /**
     * Maps {@link UserNamePrincipal} to corresponding {@link UidPrincipal} and
     * {@link GroupNamePrincipal} to corresponding {@link GidPrincipal}.
     * @param principal to map
     * @return mapped principal.
     * @throws NoSuchPrincipalException if user or group name does can't be mapped.
     */
    @Override
    public Principal map(Principal principal) throws NoSuchPrincipalException {

        if (principal instanceof UserNamePrincipal) {
            __password p = _libc.getpwnam(principal.getName());
            if (p != null) {
                return new UidPrincipal(p.uid);
            }
        } else if (principal instanceof GroupNamePrincipal) {
            __group g = _libc.getgrnam(principal.getName());
            if (g != null) {
                return new GidPrincipal(g.gid, false);
            }
        }
        throw new NoSuchPrincipalException(principal);
    }

    /**
     * Maps {@link UidPrincipal} to corresponding {@link UserNamePrincipal} and
     * {@link GidPrincipal} to corresponding {@link GroupNamePrincipal}.
     * @param principal to map
     * @return mapped principal
     * @throws NoSuchPrincipalException if uid or gid can't be mapped.
     */
    @Override
    public Set<Principal> reverseMap(Principal principal) throws NoSuchPrincipalException {

        if (principal instanceof UidPrincipal) {
            __password p = _libc.getpwuid((int) ((UidPrincipal) principal).getUid());
            if (p != null) {
               return Sets.newHashSet((Principal)new UserNamePrincipal(p.name));
            }
        } else if (principal instanceof GidPrincipal) {
            __group g = _libc.getgrgid((int) ((GidPrincipal) principal).getGid());
            if (g != null) {
               return Sets.newHashSet((Principal)new GroupNamePrincipal(g.name));
            }
        }
        throw new NoSuchPrincipalException(principal);
    }

    @Override
    public void session(Set<Principal> authorizedPrincipals, Set<Object> attrib) throws AuthenticationException {
        attrib.add(new HomeDirectory("/"));
        attrib.add(new RootDirectory("/"));
        attrib.add(new ReadOnly(false));
    }

    private int[] groupsOf(__password pwrecord) {

        boolean done = false;
        int[] groups = new int[0];
        while (!done) {
            IntByReference ngroups = new IntByReference();
            ngroups.setValue(groups.length);
            if (_libc.getgrouplist(pwrecord.name, pwrecord.gid, groups, ngroups) < 0) {
                groups = new int[ngroups.getValue()];
                continue;
            }
            done = true;
        }

        return groups;
    }

    /*
     * struct passwd equivalent  as defined in <pwd.h>
     */
    static public class __password extends Structure {

        public String name;
        public String passwd;
        public int uid;
        public int gid;
        public String gecos;
        public String dir;
        public String shell;
    }

    /*
     * struct group equivalent as defined in <pwd.h>
     */
    static public class __group extends Structure {

        public String name;
        public String passwd;
        public int gid;
        public Pointer mem;
    }

    /*
     * hook required functions from libc
     */
    public interface LibC extends Library {

        __password getpwnam(String name);

        __password getpwuid(int id);

        __group getgrnam(String name);

        __group getgrgid(int id);

        int getgrouplist(String user, int gid, int[] groups, IntByReference ngroups);
    }
}
