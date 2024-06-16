package org.jenkinsci.plugins.gitclient;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;
import hudson.plugins.git.IGitAPI;
import hudson.plugins.git.IndexEntry;
import hudson.plugins.git.Revision;
import hudson.plugins.git.Tag;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.RemoteWriter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link GitClient} that delegates to a remote {@link GitClient}.
 *
 * @author Kohsuke Kawaguchi
 */
class RemoteGitImpl implements GitClient, IGitAPI, Serializable {
    private final GitClient proxy;
    private transient Channel channel;

    RemoteGitImpl(GitClient proxy) {
        this.proxy = proxy;
    }

    private IGitAPI getGitAPI() {
        return (IGitAPI)proxy;
    }

    private Object readResolve() {
        channel = Channel.current();
        return this;
    }

    private Object writeReplace() {
        if (channel!=null)
            return proxy; // when sent back to where it came from, switch back to the original object
        return this;
    }

    static class Invocation implements Serializable {
        private final String methodName;
        private final String[] parameterTypes;
        private final Object[] args;

        Invocation(Method method, @Nonnull Object[] args) {
            this.methodName = method.getName();
            this.args = args;
            this.parameterTypes = new String[args.length];
            Class[] paramTypes = method.getParameterTypes();
            for (int i=0; i<args.length; i++) {
                parameterTypes[i] = paramTypes[i].getName();
            }
            for (int i=0; i<args.length; i++) {
                if (args[i] instanceof OutputStream)
                    args[i] = new RemoteOutputStream((OutputStream)args[i]);
                if (args[i] instanceof Writer)
                    args[i] = new RemoteWriter((Writer)args[i]);
            }
        }

        public void replay(Object target) throws InvocationTargetException, IllegalAccessException {
            OUTER:
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterTypes().length==parameterTypes.length) {
                    Class<?>[] t = m.getParameterTypes();
                    for (int i=0; i<parameterTypes.length; i++) {
                        if (!t[i].getName().equals(parameterTypes[i]))
                            continue OUTER;
                    }
                    // matched
                    m.invoke(target,args);
                    return;
                }
            }
            throw new IllegalStateException("Method not found: "+methodName+"("+ Util.join(Arrays.asList(parameterTypes),",")+")");
        }

        private static final long serialVersionUID = 1L;
    }

    private <T extends GitCommand> T command(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new CommandInvocationHandler(type,this)));
    }

    private static class CommandInvocationHandler implements InvocationHandler, GitCommand, Serializable {
        private final Class<? extends GitCommand> command;
        private final List<Invocation> invocations = new ArrayList<Invocation>();
        private transient final Channel channel;
        private final GitClient proxy;

        private CommandInvocationHandler(Class<? extends GitCommand> command, RemoteGitImpl owner) {
            this.command = command;
            this.channel = owner.channel;
            this.proxy = owner.proxy;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> decl = method.getDeclaringClass();
            if (args == null) args = new Object[0];
            if (GitCommand.class == decl || Object.class==decl) {
                try {
                    return method.invoke(this,args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
            if (GitCommand.class.isAssignableFrom(decl)) {
                invocations.add(new Invocation(method, args));
                return proxy;
            }
            throw new IllegalStateException("Unexpected invocation: "+method);
        }

        public void execute() throws GitException, InterruptedException {
            try {
                channel.call(new Callable<Void, GitException>() {
                    public Void call() throws GitException {
                        try {
                            GitCommand cmd = createCommand();
                            for (Invocation inv : invocations) {
                                inv.replay(cmd);
                            }
                            cmd.execute();
                            return null;
                        } catch (InvocationTargetException e) {
                            throw new GitException(e);
                        } catch (IllegalAccessException e) {
                            throw new GitException(e);
                        } catch (InterruptedException e) {
                            throw new GitException(e);
                        }
                    }

                    private GitCommand createCommand() throws InvocationTargetException, IllegalAccessException {
                        for (Method m : GitClient.class.getMethods()) {
                            if (m.getReturnType()==command && m.getParameterTypes().length==0)
                                return command.cast(m.invoke(proxy));
                        }
                        throw new IllegalStateException("Can't find the factory method for "+command);
                    }
                });
            } catch (IOException e) {
                throw new GitException(e);
            }
        }

        private static final long serialVersionUID = 1L;
    }

    private OutputStream wrap(OutputStream os) {
        return new RemoteOutputStream(os);
    }

    /**
     * @deprecated
     *      There's no way to make this method work. use {@link #withRepository(RepositoryCallback)}
     */
    @NonNull
    public Repository getRepository() throws GitException {
        throw new UnsupportedOperationException();
    }

    public void clearCredentials() {
        proxy.clearCredentials();
    }

    public void addCredentials(String url, StandardCredentials credentials) {
        proxy.addCredentials(url, credentials); // credentials are Serializable
    }

    public void setCredentials(StandardUsernameCredentials cred) {
        proxy.setCredentials(cred);
    }

    public void addDefaultCredentials(StandardCredentials credentials) {
        proxy.addDefaultCredentials(credentials); // credentials are Serializable
    }

    public void setAuthor(String name, String email) throws GitException {
        proxy.setAuthor(name, email);
    }

    public void setAuthor(PersonIdent p) throws GitException {
        proxy.setAuthor(p);
    }

    public void setCommitter(String name, String email) throws GitException {
        proxy.setCommitter(name, email);
    }

    public void setCommitter(PersonIdent p) throws GitException {
        proxy.setCommitter(p);
    }

    public <T> T withRepository(RepositoryCallback<T> callable) throws IOException, InterruptedException {
        return proxy.withRepository(callable);
    }

    public FilePath getWorkTree() {
        return proxy.getWorkTree();
    }

    public void init() throws GitException, InterruptedException {
        proxy.init();
    }

    public void add(String filePattern) throws GitException, InterruptedException {
        proxy.add(filePattern);
    }

    public void commit(String message) throws GitException, InterruptedException {
        proxy.commit(message);
    }

    public void commit(String message, PersonIdent author, PersonIdent committer) throws GitException, InterruptedException {
        proxy.commit(message, author, committer);
    }

    public boolean hasGitRepo() throws GitException, InterruptedException {
        return proxy.hasGitRepo();
    }

    public boolean isCommitInRepo(ObjectId commit) throws GitException, InterruptedException {
        return proxy.isCommitInRepo(commit);
    }

    public String getRemoteUrl(String name) throws GitException, InterruptedException {
        return proxy.getRemoteUrl(name);
    }

    public void setRemoteUrl(String name, String url) throws GitException, InterruptedException {
        proxy.setRemoteUrl(name, url);
    }

    public void addRemoteUrl(String name, String url) throws GitException, InterruptedException {
        proxy.addRemoteUrl(name, url);
    }

    public void checkout(String ref) throws GitException, InterruptedException {
        proxy.checkout(ref);
    }

    public void checkout(String ref, String branch) throws GitException, InterruptedException {
        proxy.checkout(ref, branch);
    }

    public CheckoutCommand checkout() {
        return command(CheckoutCommand.class);
    }

    public void checkoutBranch(String branch, String ref) throws GitException, InterruptedException {
        proxy.checkoutBranch(branch, ref);
    }

    public ObjectId mergeBase(ObjectId sha1, ObjectId sha12) throws InterruptedException {
        return getGitAPI().mergeBase(sha1, sha12);
    }

    public String getAllLogEntries(String branch) throws InterruptedException {
        return getGitAPI().getAllLogEntries(branch);
    }

    public List<String> showRevision(Revision r) throws GitException, InterruptedException {
        return getGitAPI().showRevision(r);
    }

    public void clone(String url, String origin, boolean useShallowClone, String reference) throws GitException, InterruptedException {
        proxy.clone(url, origin, useShallowClone, reference);
    }

    public CloneCommand clone_() {
        return command(CloneCommand.class);
    }

    public MergeCommand merge() {
        return command(MergeCommand.class);
    }

    public InitCommand init_() {
        return command(InitCommand.class);
    }

    public FetchCommand fetch_() {
        return command(FetchCommand.class);
    }

    public PushCommand push() {
        return command(PushCommand.class);
    }

    public void fetch(URIish url, List<RefSpec> refspecs) throws GitException, InterruptedException {
        proxy.fetch(url, refspecs);
    }

    public void fetch(String remoteName, RefSpec... refspec) throws GitException, InterruptedException {
        proxy.fetch(remoteName, refspec);
    }

    public void fetch(String remoteName, RefSpec refspec) throws GitException, InterruptedException {
        fetch(remoteName, new RefSpec[]{refspec});
    }

    public void push(String remoteName, String refspec) throws GitException, InterruptedException {
        proxy.push(remoteName, refspec);
    }

    public void push(URIish url, String refspec) throws GitException, InterruptedException {
        proxy.push(url, refspec);
    }

    public void merge(ObjectId rev) throws GitException, InterruptedException {
        proxy.merge(rev);
    }

    public void prune(RemoteConfig repository) throws GitException, InterruptedException {
        proxy.prune(repository);
    }

    public void clean() throws GitException, InterruptedException {
        proxy.clean();
    }

    public void branch(String name) throws GitException, InterruptedException {
        proxy.branch(name);
    }

    public void deleteBranch(String name) throws GitException, InterruptedException {
        proxy.deleteBranch(name);
    }

    public Set<Branch> getBranches() throws GitException, InterruptedException {
        return proxy.getBranches();
    }

    public Set<Branch> getRemoteBranches() throws GitException, InterruptedException {
        return proxy.getRemoteBranches();
    }

    public void tag(String tagName, String comment) throws GitException, InterruptedException {
        proxy.tag(tagName, comment);
    }

    public boolean tagExists(String tagName) throws GitException, InterruptedException {
        return proxy.tagExists(tagName);
    }

    public String getTagMessage(String tagName) throws GitException, InterruptedException {
        return proxy.getTagMessage(tagName);
    }

    public void deleteTag(String tagName) throws GitException, InterruptedException {
        proxy.deleteTag(tagName);
    }

    public Set<String> getTagNames(String tagPattern) throws GitException, InterruptedException {
        return proxy.getTagNames(tagPattern);
    }

    public void ref(String refName) throws GitException, InterruptedException {
	proxy.ref(refName);
    }

    public boolean refExists(String refName) throws GitException, InterruptedException {
	return proxy.refExists(refName);
    }

    public void deleteRef(String refName) throws GitException, InterruptedException {
	proxy.deleteRef(refName);
    }

    public Set<String> getRefNames(String refPrefix) throws GitException, InterruptedException {
	return proxy.getRefNames(refPrefix);
    }

    public Set<String> getRemoteTagNames(String tagPattern) throws GitException, InterruptedException {
        return proxy.getTagNames(tagPattern);
    }

    public Map<String, ObjectId> getHeadRev(String url) throws GitException, InterruptedException {
        return proxy.getHeadRev(url);
    }

    public ObjectId getHeadRev(String remoteRepoUrl, String branch) throws GitException, InterruptedException {
        return proxy.getHeadRev(remoteRepoUrl, branch);
    }

    public ObjectId revParse(String revName) throws GitException, InterruptedException {
        return proxy.revParse(revName);
    }

    public RevListCommand revList_() {
        return proxy.revList_();
    }

    public List<ObjectId> revListAll() throws GitException, InterruptedException {
        return proxy.revListAll();
    }

    public List<ObjectId> revList(String ref) throws GitException, InterruptedException {
        return proxy.revList(ref);
    }

    public GitClient subGit(String subdir) {
        return proxy.subGit(subdir);
    }

    public boolean hasGitModules() throws GitException, InterruptedException {
        return proxy.hasGitModules();
    }

    public List<IndexEntry> getSubmodules(String treeIsh) throws GitException, InterruptedException {
        return proxy.getSubmodules(treeIsh);
    }

    public void addSubmodule(String remoteURL, String subdir) throws GitException, InterruptedException {
        proxy.addSubmodule(remoteURL, subdir);
    }

    public void submoduleUpdate(boolean recursive) throws GitException, InterruptedException {
        proxy.submoduleUpdate(recursive);
    }

    public void submoduleUpdate(boolean recursive, String ref) throws GitException, InterruptedException {
        proxy.submoduleUpdate(recursive, ref);
    }

    public void submoduleUpdate(boolean recursive, boolean remoteTracking) throws GitException, InterruptedException {
        proxy.submoduleUpdate(recursive, remoteTracking);
    }

    public void submoduleUpdate(boolean recursive, boolean remoteTracking, String reference) throws GitException, InterruptedException {
        proxy.submoduleUpdate(recursive, remoteTracking, reference);
    }

    public SubmoduleUpdateCommand submoduleUpdate() {
        return command(SubmoduleUpdateCommand.class);
    }

    public void submoduleClean(boolean recursive) throws GitException, InterruptedException {
        proxy.submoduleClean(recursive);
    }

    public void setupSubmoduleUrls(Revision rev, TaskListener listener) throws GitException, InterruptedException {
        proxy.setupSubmoduleUrls(rev, listener);
    }

    public void changelog(String revFrom, String revTo, OutputStream os) throws GitException, InterruptedException {
        proxy.changelog(revFrom, revTo, wrap(os));
    }

    public void changelog(String revFrom, String revTo, Writer os) throws GitException, InterruptedException {
        proxy.changelog(revFrom, revTo, os); // TODO: wrap
    }

    public ChangelogCommand changelog() {
        return command(ChangelogCommand.class);
    }

    public void appendNote(String note, String namespace) throws GitException, InterruptedException {
        proxy.appendNote(note, namespace);
    }

    public void addNote(String note, String namespace) throws GitException, InterruptedException {
        proxy.addNote(note, namespace);
    }

    public List<String> showRevision(ObjectId r) throws GitException, InterruptedException {
        return proxy.showRevision(r);
    }

    public List<String> showRevision(ObjectId from, ObjectId to) throws GitException, InterruptedException {
        return proxy.showRevision(from, to);
    }

    public boolean hasGitModules(String treeIsh) throws GitException, InterruptedException {
        return getGitAPI().hasGitModules(treeIsh);
    }

    public String getRemoteUrl(String name, String GIT_DIR) throws GitException, InterruptedException {
        return getGitAPI().getRemoteUrl(name, GIT_DIR);
    }

    public void setRemoteUrl(String name, String url, String GIT_DIR) throws GitException, InterruptedException {
        getGitAPI().setRemoteUrl(name, url, GIT_DIR);
    }

    public String getDefaultRemote(String _default_) throws GitException, InterruptedException {
        return getGitAPI().getDefaultRemote(_default_);
    }

    public boolean isBareRepository() throws GitException, InterruptedException {
        return getGitAPI().isBareRepository();
    }

    public boolean isBareRepository(String GIT_DIR) throws GitException, InterruptedException {
        return getGitAPI().isBareRepository(GIT_DIR);
    }

    public void submoduleInit() throws GitException, InterruptedException {
        getGitAPI().submoduleInit();
    }

    public void submoduleSync() throws GitException, InterruptedException {
        getGitAPI().submoduleSync();
    }

    public String getSubmoduleUrl(String name) throws GitException, InterruptedException {
        return getGitAPI().getSubmoduleUrl(name);
    }

    public void setSubmoduleUrl(String name, String url) throws GitException, InterruptedException {
        getGitAPI().setSubmoduleUrl(name, url);
    }

    public void fixSubmoduleUrls(String remote, TaskListener listener) throws GitException, InterruptedException {
        getGitAPI().fixSubmoduleUrls(remote, listener);
    }

    public void setupSubmoduleUrls(String remote, TaskListener listener) throws GitException, InterruptedException {
        getGitAPI().setupSubmoduleUrls(remote, listener);
    }

    public void fetch(String repository, String refspec) throws GitException, InterruptedException {
        getGitAPI().fetch(repository, refspec);
    }

    public void fetch(RemoteConfig remoteRepository) throws InterruptedException {
        getGitAPI().fetch(remoteRepository);
    }

    public void fetch() throws GitException, InterruptedException {
        getGitAPI().fetch();
    }

    public void reset(boolean hard) throws GitException, InterruptedException {
        getGitAPI().reset(hard);
    }

    public void reset() throws GitException, InterruptedException {
        getGitAPI().reset();
    }

    public void push(RemoteConfig repository, String revspec) throws GitException, InterruptedException {
        getGitAPI().push(repository, revspec);
    }

    public void merge(String revSpec) throws GitException, InterruptedException {
        getGitAPI().merge(revSpec);
    }

    public void clone(RemoteConfig source) throws GitException, InterruptedException {
        getGitAPI().clone(source);
    }

    public void clone(RemoteConfig rc, boolean useShallowClone) throws GitException, InterruptedException {
        getGitAPI().clone(rc, useShallowClone);
    }

    public List<Branch> getBranchesContaining(String revspec) throws GitException, InterruptedException {
        return getGitAPI().getBranchesContaining(revspec);
    }

    public List<IndexEntry> lsTree(String treeIsh) throws GitException, InterruptedException {
        return getGitAPI().lsTree(treeIsh);
    }

    public List<IndexEntry> lsTree(String treeIsh, boolean recursive) throws GitException, InterruptedException {
        return getGitAPI().lsTree(treeIsh, recursive);
    }

    public List<ObjectId> revListBranch(String branchId) throws GitException, InterruptedException {
        return getGitAPI().revListBranch(branchId);
    }

    public String describe(String commitIsh) throws GitException, InterruptedException {
        return getGitAPI().describe(commitIsh);
    }

    public List<Tag> getTagsOnCommit(String revName) throws GitException, IOException, InterruptedException {
        return getGitAPI().getTagsOnCommit(revName);
    }

    public void setProxy(ProxyConfiguration proxyConfiguration) {
        proxy.setProxy(proxyConfiguration);
    }

    private static final long serialVersionUID = 1L;
}
