package org.jenkinsci.plugins.gitclient;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.plugins.git.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to Git functionality.
 *
 * <p>
 * Since 1.1, this interface is remotable, meaning it can be referenced from a remote closure call.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface GitClient {

    boolean verbose = Boolean.getBoolean(IGitAPI.class.getName() + ".verbose");

    // If true, do not print the list of remote branches.
    boolean quietRemoteBranches = Boolean.getBoolean(GitClient.class.getName() + ".quietRemoteBranches");

    /**
     * The supported credential types.
     * @since 1.2.0
     */
    CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(
            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
            CredentialsMatchers.instanceOf(SSHUserPrivateKey.class)
            // TODO does anyone use SSL client certificates with GIT?
    );

    /**
     * Remove all credentials from the client.
     * @since 1.2.0
     */
    void clearCredentials();

    /**
     * Adds credentials to be used against a specific url.
     * @param url the url for the credentials to be used against.
     * @param credentials the credentials to use.
     * @since 1.2.0
     */
    void addCredentials(String url, StandardCredentials credentials);

    /**
     * Adds credentials to be used when there are not url specific credentials defined.
     *
     * @param credentials the credentials to use.
     * @see #addCredentials(String, com.cloudbees.plugins.credentials.common.StandardCredentials)
     * @since 1.2.0
     */
    void addDefaultCredentials(StandardCredentials credentials);

    /**
     * Sets the identity of the author for future commits and merge operations.
     */
    void setAuthor(String name, String email) throws GitException;
    void setAuthor(PersonIdent p) throws GitException;

    /**
     * Sets the identity of the committer for future commits and merge operations.
     */
    void setCommitter(String name, String email) throws GitException;
    void setCommitter(PersonIdent p) throws GitException;

    /**
     * Expose the JGit repository this GitClient is using.
     * Don't forget to call {@link org.eclipse.jgit.lib.Repository#close()}, to avoid JENKINS-12188.
     *
     * @deprecated as of 1.1
     *      This method was deprecated to make {@link GitClient} remotable. When called on
     *      a proxy object, this method throws {@link NotSerializableException}.
     *      Use {@link #withRepository(RepositoryCallback)} to pass in the closure instead.
     *      This prevents the repository leak (JENKINS-12188), too.
     */
    Repository getRepository() throws GitException;

    /**
     * Runs the computation that requires local access to {@link Repository}.
     */
    <T> T withRepository(RepositoryCallback<T> callable) throws IOException, InterruptedException;

    /**
     * The working tree of this repository.
     */
    FilePath getWorkTree();

    public void init() throws GitException, InterruptedException;

    void add(String filePattern) throws GitException, InterruptedException;

    void commit(String message) throws GitException, InterruptedException;

    /**
     * @deprecated as of 1.1
     *      Use {@link #setAuthor(String, String)} and {@link #setCommitter(String, String)}
     *      then call {@link #commit(String)}
      */
    void commit(String message, PersonIdent author, PersonIdent committer) throws GitException, InterruptedException;

    boolean hasGitRepo() throws GitException, InterruptedException;

    boolean isCommitInRepo(ObjectId commit) throws GitException, InterruptedException;

    /**
     * From a given repository, get a remote's URL
     * @param name The name of the remote (e.g. origin)
     * @throws GitException if executing the git command fails
     */
    String getRemoteUrl(String name) throws GitException, InterruptedException;

    /**
     * For a given repository, set a remote's URL
     * @param name The name of the remote (e.g. origin)
     * @param url The new value of the remote's URL
     * @throws GitException if executing the git command fails
     */
    void setRemoteUrl(String name, String url) throws GitException, InterruptedException;

    void addRemoteUrl(String name, String url) throws GitException, InterruptedException;

    /**
     * Checks out the specified commit/tag/branch into the workspace.
     * (equivalent of <tt>git checkout <em>branch</em></tt>.)
     * @param ref A git object references expression (either a sha1, tag or branch)
     * @deprecated use {@link #checkout()} and {@link CheckoutCommand}
     */
    void checkout(String ref) throws GitException, InterruptedException;

    /**
     * Creates a new branch that points to the specified ref.
     * (equivalent to git checkout -b <em>branch</em> <em>commit</em>)
     *
     * This will fail if the branch already exists.
     *
     * @param ref A git object references expression. For backward compatibility, <tt>null</tt> will checkout current HEAD
     * @param branch name of the branch to create from reference
     * @deprecated use {@link #checkout()} and {@link CheckoutCommand}
     */
    void checkout(String ref, String branch) throws GitException, InterruptedException;

    CheckoutCommand checkout();

    /**
     * Regardless of the current state of the workspace (whether there is some dirty files, etc)
     * and the state of the repository (whether the branch of the specified name exists or not),
     * when this method exits the following conditions hold:
     *
     * <ul>
     *     <li>The branch of the specified name <em>branch</em> exists and points to the specified <em>ref</em>
     *     <li><tt>HEAD</tt> points to <em>branch</em>. IOW, the workspace is on the specified branch.
     *     <li>Both index and workspace are the same tree with <em>ref</em>.
     *         (no dirty files and no staged changes, although this method will not touch untracked files
     *         in the workspace.)
     * </ul>
     *
     * <p>
     * This method is preferred over the {@link #checkout(String, String)} family of methods, as
     * this method is affected far less by the current state of the repository. The <tt>checkout</tt>
     * methods, in their attempt to emulate the "git checkout" command line behaviour, have too many
     * side effects. In Jenkins, where you care a lot less about throwing away local changes and
     * care a lot more about resetting the workspace into a known state, methods like this is more useful.
     *
     * <p>
     * For compatibility reasons, the order of the parameter is different from {@link #checkout(String, String)}.
     * @since 1.0.6
     */
    void checkoutBranch(@CheckForNull String branch, String ref) throws GitException, InterruptedException;


    /**
     * Clone a remote repository
     * @param url URL for remote repository to clone
     * @param origin upstream track name, defaults to <tt>origin</tt> by convention
     * @param useShallowClone option to create a shallow clone, that has some restriction but will make clone operation
     * @param reference (optional) reference to a local clone for faster clone operations (reduce network and local storage costs)
     */
    void clone(String url, String origin, boolean useShallowClone, String reference) throws GitException, InterruptedException;

    /**
     * Returns a {@link CloneCommand} to build up the git-log invocation.
     */
    CloneCommand clone_(); // can't use 'clone' as it collides with Object.clone()

    /**
     * Fetch a remote repository. Assumes <tt>remote.remoteName.url</tt> has been set.
     * @deprecated use {@link #fetch_()} and configure a {@link org.jenkinsci.plugins.gitclient.FetchCommand}
     */
    void fetch(URIish url, List<RefSpec> refspecs) throws GitException, InterruptedException;

    /**
     * @deprecated use {@link #fetch_()} and configure a {@link org.jenkinsci.plugins.gitclient.FetchCommand}
     */
    void fetch(String remoteName, RefSpec... refspec) throws GitException, InterruptedException;

    /**
     * @deprecated use {@link #fetch_()} and configure a {@link org.jenkinsci.plugins.gitclient.FetchCommand}
     */
    void fetch(String remoteName, RefSpec refspec) throws GitException, InterruptedException;

    FetchCommand fetch_(); // can't use 'fetch' as legacy IGitAPI already define this method

    /**
     * @deprecated use {@link #push()} and configure a {@link org.jenkinsci.plugins.gitclient.PushCommand}
     */
    void push(String remoteName, String refspec) throws GitException, InterruptedException;

    /**
     * @deprecated use {@link #push()} and configure a {@link org.jenkinsci.plugins.gitclient.PushCommand}
     */
    void push(URIish url, String refspec) throws GitException, InterruptedException;

    PushCommand push();


    /**
     * @deprecated use {@link #merge()} and configure a {@link org.jenkinsci.plugins.gitclient.MergeCommand}
     */
    void merge(ObjectId rev) throws GitException, InterruptedException;

    MergeCommand merge();

    InitCommand init_(); // can't use 'init' as legacy IGitAPI already define this method

    /**
     * Prune stale remote tracking branches with "git remote prune" on the specified remote.
     */
    void prune(RemoteConfig repository) throws GitException, InterruptedException;

    /**
     * Fully revert working copy to a clean state, i.e. run both
     * <a href="https://www.kernel.org/pub/software/scm/git/docs/git-reset.html">git-reset(1) --hard</a> then
     * <a href="https://www.kernel.org/pub/software/scm/git/docs/git-clean.html">git-clean(1)</a> for working copy to
     * match a fresh clone.
     * @throws GitException
     */
    void clean() throws GitException, InterruptedException;



    // --- manage branches

    void branch(String name) throws GitException, InterruptedException;

    /**
     * (force) delete a branch.
     */
    void deleteBranch(String name) throws GitException, InterruptedException;

    Set<Branch> getBranches() throws GitException, InterruptedException;

    Set<Branch> getRemoteBranches() throws GitException, InterruptedException;


    // --- manage tags

    /**
     * Create (or update) a tag. If tag already exist it gets updated (equivalent to <tt>git tag --force</tt>)
     */
    void tag(String tagName, String comment) throws GitException, InterruptedException;

    boolean tagExists(String tagName) throws GitException, InterruptedException;

    String getTagMessage(String tagName) throws GitException, InterruptedException;

    void deleteTag(String tagName) throws GitException, InterruptedException;

    Set<String> getTagNames(String tagPattern) throws GitException, InterruptedException;
    Set<String> getRemoteTagNames(String tagPattern) throws GitException, InterruptedException;


    // --- manage refs

    /**
     * Create (or update) a ref. The ref will reference HEAD (equivalent to <tt>git update-ref ... HEAD</tt>).
     * @param refName the full name of the ref (e.g. "refs/myref"). Spaces will be replaced with underscores.
     */
    void ref(String refName) throws GitException, InterruptedException;

    /**
     * Check if a ref exists. Equivalent to comparing the return code of <tt>git show-ref</tt> to zero.
     * @param refName the full name of the ref (e.g. "refs/myref"). Spaces will be replaced with underscores.
     * @return True if the ref exists, false otherwse.
     */
    boolean refExists(String refName) throws GitException, InterruptedException;

    /**
     * Deletes a ref. Has no effect if the ref does not exist, equivalent to <tt>git update-ref -d</tt>.
     * @param refName the full name of the ref (e.g. "refs/myref"). Spaces will be replaced with underscores.
     */
    void deleteRef(String refName) throws GitException, InterruptedException;

    /**
     * List refs with the given prefix. Equivalent to <tt>git for-each-ref --format="%(refname)"</tt>.
     * @param refPrefix the literal prefix any ref returned will have. The empty string implies all.
     * @return a set of refs, each beginning with the given prefix. Empty if none.
     */
    Set<String> getRefNames(String refPrefix) throws GitException, InterruptedException;

    // --- lookup revision

    Map<String, ObjectId> getHeadRev(String url) throws GitException, InterruptedException;

    ObjectId getHeadRev(String remoteRepoUrl, String branch) throws GitException, InterruptedException;

    /**
     * Retrieve commit object that is direct child for <tt>revName</tt> revision reference.
     * @param revName a commit sha1 or tag/branch refname
     * @throws GitException when no such commit / revName is found in repository.
     */
    ObjectId revParse(String revName) throws GitException, InterruptedException;

    RevListCommand revList_();

    List<ObjectId> revListAll() throws GitException, InterruptedException;

    List<ObjectId> revList(String ref) throws GitException, InterruptedException;


    // --- submodules

    /**
     * @return a IGitAPI implementation to manage git submodule repository
     */
    GitClient subGit(String subdir);

    /**
     * Returns true if the repository has Git submodules.
     */
    boolean hasGitModules() throws GitException, InterruptedException;

    /**
     * Finds all the submodule references in this repository at the specified tree.
     *
     * @return never null.
     */
    List<IndexEntry> getSubmodules( String treeIsh ) throws GitException, InterruptedException;

    /**
     * Create a submodule in subdir child directory for remote repository
     */
    void addSubmodule(String remoteURL, String subdir) throws GitException, InterruptedException;

    /**
     * Run submodule update optionally recursively on all submodules
     * (equivalent of <tt>git submodule update <em>--recursive</em></tt>.)
     * @deprecated use {@link #submoduleUpdate()} and {@link SubmoduleUpdateCommand}
     */
    void submoduleUpdate(boolean recursive)  throws GitException, InterruptedException;

    /**
     * Run submodule update optionally recursively on all submodules, with a specific
     * reference passed to git clone if needing to --init.
     * (equivalent of <tt>git submodule update <em>--recursive</em> <em>--reference 'reference'</em></tt>.)
     * @deprecated use {@link #submoduleUpdate()} and {@link SubmoduleUpdateCommand}
     */
    void submoduleUpdate(boolean recursive, String reference) throws GitException, InterruptedException;

    /**
     * Run submodule update optionally recursively on all submodules, optionally with remoteTracking submodules
     * (equivalent of <tt>git submodule update <em>--recursive</em> <em>--remote</em></tt>.)
     * @deprecated use {@link #submoduleUpdate()} and {@link SubmoduleUpdateCommand}
     */
    void submoduleUpdate(boolean recursive, boolean remoteTracking)  throws GitException, InterruptedException;
    /**
     * Run submodule update optionally recursively on all submodules, optionally with remoteTracking, with a specific
     * reference passed to git clone if needing to --init.
     * (equivalent of <tt>git submodule update <em>--recursive</em> <em>--remote</em> <em>--reference 'reference'</em></tt>.)
     * @deprecated use {@link #submoduleUpdate()} and {@link SubmoduleUpdateCommand}
     */
    void submoduleUpdate(boolean recursive, boolean remoteTracking, String reference)  throws GitException, InterruptedException;

    SubmoduleUpdateCommand submoduleUpdate();

    void submoduleClean(boolean recursive)  throws GitException, InterruptedException;

    void submoduleInit()  throws GitException, InterruptedException;

    /**
     * Set up submodule URLs so that they correspond to the remote pertaining to
     * the revision that has been checked out.
     */
    void setupSubmoduleUrls( Revision rev, TaskListener listener ) throws GitException, InterruptedException;


    // --- commit log and notes

    /**
     * @deprecated use {@link #changelog(String, String, Writer)}
     */
    void changelog(String revFrom, String revTo, OutputStream os) throws GitException, InterruptedException;

    /**
     * Adds the changelog entries for commits in the range revFrom..revTo.
     *
     * This is just a short cut for calling {@link #changelog()} with appropriate parameters.
     */
    void changelog(String revFrom, String revTo, Writer os) throws GitException, InterruptedException;

    /**
     * Returns a {@link ChangelogCommand} to build up the git-log invocation.
     */
    ChangelogCommand changelog();

    /**
     * Appends to an existing git-note on the current HEAD commit.
     *
     * If a note doesn't exist, it works just like {@link #addNote(String, String)}
     *
     * @param note
     *      Content of the note.
     * @param namespace
     *      If unqualified, interpreted as "refs/notes/NAMESPACE" just like cgit.
     */
    void appendNote(String note, String namespace ) throws GitException, InterruptedException;

    /**
     * Adds a new git-note on the current HEAD commit.
     *
     * @param note
     *      Content of the note.
     * @param namespace
     *      If unqualified, interpreted as "refs/notes/NAMESPACE" just like cgit.
     */
    void addNote(String note, String namespace ) throws GitException, InterruptedException;

    public List<String> showRevision(ObjectId r) throws GitException, InterruptedException;

    /**
     * Given a Revision, show it as if it were an entry from git whatchanged, so that it
     * can be parsed by GitChangeLogParser.
     *
     * <p>
     * Changes are computed on the [from..to] range. If {@code from} is null, this prints
     * just one commit that {@code to} represents.
     *
     * <p>
     * For merge commit, this method reports one diff per each parent. This makes this method
     * behave differently from {@link #changelog()}.
     *
     * @return The git show output, in <tt>raw</tt> format.
     */
    List<String> showRevision(ObjectId from, ObjectId to) throws GitException, InterruptedException;

    /**
     * Equivalent of "git-describe --tags".
     *
     * Find a nearby tag (including unannotated ones) and come up with a short identifier to describe the tag.
     */
    String describe(String commitIsh) throws GitException, InterruptedException;

    void setCredentials(StandardUsernameCredentials cred);

    void setProxy(ProxyConfiguration proxy);

    /**
     * Find all the branches that include the given commit.
     * @param revspec commit id to query for
     * @param allBranches whether remote branches should be also queried (<code>true</code>) or not (<code>false</code>)
     * @return list of branches the specified commit belongs to
     * @throws GitException on Git exceptions
     * @throws InterruptedException on thread interruption
     */
    List<Branch> getBranchesContaining(String revspec, boolean allBranches) throws GitException, InterruptedException;
}
