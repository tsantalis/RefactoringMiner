/*
 * Copyright (C) 2014, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.jgit.gitrepo;

import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.gitrepo.ManifestParser.IncludedFileReader;
import org.eclipse.jgit.gitrepo.RepoProject.CopyFile;
import org.eclipse.jgit.gitrepo.RepoProject.LinkFile;
import org.eclipse.jgit.gitrepo.internal.RepoText;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.FileUtils;

/**
 * A class used to execute a repo command.
 *
 * This will parse a repo XML manifest, convert it into .gitmodules file and the
 * repository config file.
 *
 * If called against a bare repository, it will replace all the existing content
 * of the repository with the contents populated from the manifest.
 *
 * repo manifest allows projects overlapping, e.g. one project's manifestPath is
 * &quot;foo&quot; and another project's manifestPath is &quot;foo/bar&quot;. This won't
 * work in git submodule, so we'll skip all the sub projects
 * (&quot;foo/bar&quot; in the example) while converting.
 *
 * @see <a href="https://code.google.com/p/git-repo/">git-repo project page</a>
 * @since 3.4
 */
public class RepoCommand extends GitCommand<RevCommit> {
	private String manifestPath;
	private String baseUri;
	private URI targetUri;
	private String groupsParam;
	private String branch;
	private String targetBranch = Constants.HEAD;
	private boolean recordRemoteBranch = true;
	private boolean recordSubmoduleLabels = true;
	private boolean recordShallowSubmodules = true;
	private PersonIdent author;
	private RemoteReader callback;
	private InputStream inputStream;
	private IncludedFileReader includedReader;
	private boolean ignoreRemoteFailures = false;

	private List<RepoProject> bareProjects;
	private ProgressMonitor monitor;

	/**
	 * A callback to get ref sha1 of a repository from its uri.
	 *
	 * We provided a default implementation {@link DefaultRemoteReader} to
	 * use ls-remote command to read the sha1 from the repository and clone the
	 * repository to read the file. Callers may have their own quicker
	 * implementation.
	 *
	 * @since 3.4
	 */
	public interface RemoteReader {
		/**
		 * Read a remote ref sha1.
		 *
		 * @param uri
		 *            The URI of the remote repository
		 * @param ref
		 *            The ref (branch/tag/etc.) to read
		 * @return the sha1 of the remote repository, or null if the ref does
		 *         not exist.
		 * @throws GitAPIException
		 */
		@Nullable
		public ObjectId sha1(String uri, String ref) throws GitAPIException;

		/**
		 * Read a file from a remote repository.
		 *
		 * @param uri
		 *            The URI of the remote repository
		 * @param ref
		 *            The ref (branch/tag/etc.) to read
		 * @param path
		 *            The relative path (inside the repo) to the file to read
		 * @return the file content.
		 * @throws GitAPIException
		 * @throws IOException
		 * @since 3.5
		 */
		public byte[] readFile(String uri, String ref, String path)
				throws GitAPIException, IOException;
	}

	/** A default implementation of {@link RemoteReader} callback. */
	public static class DefaultRemoteReader implements RemoteReader {
		@Override
		public ObjectId sha1(String uri, String ref) throws GitAPIException {
			Map<String, Ref> map = Git
					.lsRemoteRepository()
					.setRemote(uri)
					.callAsMap();
			Ref r = RefDatabase.findRef(map, ref);
			return r != null ? r.getObjectId() : null;
		}

		@Override
		public byte[] readFile(String uri, String ref, String path)
				throws GitAPIException, IOException {
			File dir = FileUtils.createTempDir("jgit_", ".git", null); //$NON-NLS-1$ //$NON-NLS-2$
			try (Git git = Git.cloneRepository().setBare(true).setDirectory(dir)
					.setURI(uri).call()) {
				return readFileFromRepo(git.getRepository(), ref, path);
			} finally {
				FileUtils.delete(dir, FileUtils.RECURSIVE);
			}
		}

		/**
		 * Read a file from the repository
		 *
		 * @param repo
		 *            The repository containing the file
		 * @param ref
		 *            The ref (branch/tag/etc.) to read
		 * @param path
		 *            The relative path (inside the repo) to the file to read
		 * @return the file's content
		 * @throws GitAPIException
		 * @throws IOException
		 * @since 3.5
		 */
		protected byte[] readFileFromRepo(Repository repo,
				String ref, String path) throws GitAPIException, IOException {
			try (ObjectReader reader = repo.newObjectReader()) {
				ObjectId oid = repo.resolve(ref + ":" + path); //$NON-NLS-1$
				return reader.open(oid).getBytes(Integer.MAX_VALUE);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class ManifestErrorException extends GitAPIException {
		ManifestErrorException(Throwable cause) {
			super(RepoText.get().invalidManifest, cause);
		}
	}

	@SuppressWarnings("serial")
	private static class RemoteUnavailableException extends GitAPIException {
		RemoteUnavailableException(String uri) {
			super(MessageFormat.format(RepoText.get().errorRemoteUnavailable, uri));
		}
	}

	/**
	 * Constructor for RepoCommand
	 *
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository}
	 */
	public RepoCommand(Repository repo) {
		super(repo);
	}

	/**
	 * Set path to the manifest XML file.
	 * <p>
	 * Calling {@link #setInputStream} will ignore the path set here.
	 *
	 * @param path
	 *            (with <code>/</code> as separator)
	 * @return this command
	 */
	public RepoCommand setPath(String path) {
		this.manifestPath = path;
		return this;
	}

	/**
	 * Set the input stream to the manifest XML.
	 * <p>
	 * Setting inputStream will ignore the path set. It will be closed in
	 * {@link #call}.
	 *
	 * @param inputStream a {@link java.io.InputStream} object.
	 * @return this command
	 * @since 3.5
	 */
	public RepoCommand setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
		return this;
	}

	/**
	 * Set base URI of the paths inside the XML. This is typically the name of
	 * the directory holding the manifest repository, eg. for
	 * https://android.googlesource.com/platform/manifest, this should be
	 * /platform (if you would run this on android.googlesource.com) or
	 * https://android.googlesource.com/platform elsewhere.
	 *
	 * @param uri
	 *            the base URI
	 * @return this command
	 */
	public RepoCommand setURI(String uri) {
		this.baseUri = uri;
		return this;
	}

	/**
	 * Set the URI of the superproject (this repository), so the .gitmodules
	 * file can specify the submodule URLs relative to the superproject.
	 *
	 * @param uri
	 *            the URI of the repository holding the superproject.
	 * @return this command
	 * @since 4.8
	 */
	public RepoCommand setTargetURI(String uri) {
		// The repo name is interpreted as a directory, for example
		// Gerrit (http://gerrit.googlesource.com/gerrit) has a
		// .gitmodules referencing ../plugins/hooks, which is
		// on http://gerrit.googlesource.com/plugins/hooks,
		this.targetUri = URI.create(uri + "/"); //$NON-NLS-1$
		return this;
	}

	/**
	 * Set groups to sync
	 *
	 * @param groups groups separated by comma, examples: default|all|G1,-G2,-G3
	 * @return this command
	 */
	public RepoCommand setGroups(String groups) {
		this.groupsParam = groups;
		return this;
	}

	/**
	 * Set default branch.
	 * <p>
	 * This is generally the name of the branch the manifest file was in. If
	 * there's no default revision (branch) specified in manifest and no
	 * revision specified in project, this branch will be used.
	 *
	 * @param branch
	 *            a branch name
	 * @return this command
	 */
	public RepoCommand setBranch(String branch) {
		this.branch = branch;
		return this;
	}

	/**
	 * Set target branch.
	 * <p>
	 * This is the target branch of the super project to be updated. If not set,
	 * default is HEAD.
	 * <p>
	 * For non-bare repositories, HEAD will always be used and this will be
	 * ignored.
	 *
	 * @param branch
	 *            branch name
	 * @return this command
	 * @since 4.1
	 */
	public RepoCommand setTargetBranch(String branch) {
		this.targetBranch = Constants.R_HEADS + branch;
		return this;
	}

	/**
	 * Set whether the branch name should be recorded in .gitmodules.
	 * <p>
	 * Submodule entries in .gitmodules can include a "branch" field
	 * to indicate what remote branch each submodule tracks.
	 * <p>
	 * That field is used by "git submodule update --remote" to update
	 * to the tip of the tracked branch when asked and by Gerrit to
	 * update the superproject when a change on that branch is merged.
	 * <p>
	 * Subprojects that request a specific commit or tag will not have
	 * a branch name recorded.
	 * <p>
	 * Not implemented for non-bare repositories.
	 *
	 * @param enable Whether to record the branch name
	 * @return this command
	 * @since 4.2
	 */
	public RepoCommand setRecordRemoteBranch(boolean enable) {
		this.recordRemoteBranch = enable;
		return this;
	}

	/**
	 * Set whether the labels field should be recorded as a label in
	 * .gitattributes.
	 * <p>
	 * Not implemented for non-bare repositories.
	 *
	 * @param enable Whether to record the labels in the .gitattributes
	 * @return this command
	 * @since 4.4
	 */
	public RepoCommand setRecordSubmoduleLabels(boolean enable) {
		this.recordSubmoduleLabels = enable;
		return this;
	}

	/**
	 * Set whether the clone-depth field should be recorded as a shallow
	 * recommendation in .gitmodules.
	 * <p>
	 * Not implemented for non-bare repositories.
	 *
	 * @param enable Whether to record the shallow recommendation.
	 * @return this command
	 * @since 4.4
	 */
	public RepoCommand setRecommendShallow(boolean enable) {
		this.recordShallowSubmodules = enable;
		return this;
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see org.eclipse.jgit.lib.NullProgressMonitor
	 * @param monitor
	 *            a {@link org.eclipse.jgit.lib.ProgressMonitor}
	 * @return this command
	 */
	public RepoCommand setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	/**
	 * Set whether to skip projects whose commits don't exist remotely.
	 * <p>
	 * When set to true, we'll just skip the manifest entry and continue
	 * on to the next one.
	 * <p>
	 * When set to false (default), we'll throw an error when remote
	 * failures occur.
	 * <p>
	 * Not implemented for non-bare repositories.
	 *
	 * @param ignore Whether to ignore the remote failures.
	 * @return this command
	 * @since 4.3
	 */
	public RepoCommand setIgnoreRemoteFailures(boolean ignore) {
		this.ignoreRemoteFailures = ignore;
		return this;
	}

	/**
	 * Set the author/committer for the bare repository commit.
	 * <p>
	 * For non-bare repositories, the current user will be used and this will be
	 * ignored.
	 *
	 * @param author
	 *            the author's {@link org.eclipse.jgit.lib.PersonIdent}
	 * @return this command
	 */
	public RepoCommand setAuthor(PersonIdent author) {
		this.author = author;
		return this;
	}

	/**
	 * Set the GetHeadFromUri callback.
	 *
	 * This is only used in bare repositories.
	 *
	 * @param callback
	 *            a {@link org.eclipse.jgit.gitrepo.RepoCommand.RemoteReader}
	 *            object.
	 * @return this command
	 */
	public RepoCommand setRemoteReader(RemoteReader callback) {
		this.callback = callback;
		return this;
	}

	/**
	 * Set the IncludedFileReader callback.
	 *
	 * @param reader
	 *            a
	 *            {@link org.eclipse.jgit.gitrepo.ManifestParser.IncludedFileReader}
	 *            object.
	 * @return this command
	 * @since 4.0
	 */
	public RepoCommand setIncludedFileReader(IncludedFileReader reader) {
		this.includedReader = reader;
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public RevCommit call() throws GitAPIException {
		checkCallable();
		if (baseUri == null) {
			baseUri = ""; //$NON-NLS-1$
		}
		if (inputStream == null) {
			if (manifestPath == null || manifestPath.length() == 0)
				throw new IllegalArgumentException(
						JGitText.get().pathNotConfigured);
			try {
				inputStream = new FileInputStream(manifestPath);
			} catch (IOException e) {
				throw new IllegalArgumentException(
						JGitText.get().pathNotConfigured);
			}
		}

		List<RepoProject> filteredProjects;
		try {
			ManifestParser parser = new ManifestParser(includedReader,
					manifestPath, branch, baseUri, groupsParam, repo);
			parser.read(inputStream);
			filteredProjects = parser.getFilteredProjects();
		} catch (IOException e) {
			throw new ManifestErrorException(e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// Just ignore it, it's not important.
			}
		}

		if (repo.isBare()) {
			bareProjects = new ArrayList<>();
			if (author == null)
				author = new PersonIdent(repo);
			if (callback == null)
				callback = new DefaultRemoteReader();
			for (RepoProject proj : filteredProjects) {
				addSubmoduleBare(proj.getUrl(), proj.getPath(),
						proj.getRevision(), proj.getCopyFiles(),
						proj.getLinkFiles(), proj.getGroups(),
						proj.getRecommendShallow());
			}
			DirCache index = DirCache.newInCore();
			DirCacheBuilder builder = index.builder();
			ObjectInserter inserter = repo.newObjectInserter();
			try (RevWalk rw = new RevWalk(repo)) {
				Config cfg = new Config();
				StringBuilder attributes = new StringBuilder();
				for (RepoProject proj : bareProjects) {
					String path = proj.getPath();
					String nameUri = proj.getName();
					ObjectId objectId;
					if (ObjectId.isId(proj.getRevision())) {
						objectId = ObjectId.fromString(proj.getRevision());
					} else {
						objectId = callback.sha1(nameUri, proj.getRevision());
						if (objectId == null && !ignoreRemoteFailures) {
							throw new RemoteUnavailableException(nameUri);
						}
						if (recordRemoteBranch) {
							// can be branch or tag
							cfg.setString("submodule", path, "branch", //$NON-NLS-1$ //$NON-NLS-2$
									proj.getRevision());
						}

						if (recordShallowSubmodules && proj.getRecommendShallow() != null) {
							// The shallow recommendation is losing information.
							// As the repo manifests stores the recommended
							// depth in the 'clone-depth' field, while
							// git core only uses a binary 'shallow = true/false'
							// hint, we'll map any depth to 'shallow = true'
							cfg.setBoolean("submodule", path, "shallow", //$NON-NLS-1$ //$NON-NLS-2$
									true);
						}
					}
					if (recordSubmoduleLabels) {
						StringBuilder rec = new StringBuilder();
						rec.append("/"); //$NON-NLS-1$
						rec.append(path);
						for (String group : proj.getGroups()) {
							rec.append(" "); //$NON-NLS-1$
							rec.append(group);
						}
						rec.append("\n"); //$NON-NLS-1$
						attributes.append(rec.toString());
					}

					URI submodUrl = URI.create(nameUri);
					if (targetUri != null) {
						submodUrl = relativize(targetUri, submodUrl);
					}
					cfg.setString("submodule", path, "path", path); //$NON-NLS-1$ //$NON-NLS-2$
					cfg.setString("submodule", path, "url", submodUrl.toString()); //$NON-NLS-1$ //$NON-NLS-2$

					// create gitlink
					if (objectId != null) {
						DirCacheEntry dcEntry = new DirCacheEntry(path);
						dcEntry.setObjectId(objectId);
						dcEntry.setFileMode(FileMode.GITLINK);
						builder.add(dcEntry);

						for (CopyFile copyfile : proj.getCopyFiles()) {
							byte[] src = callback.readFile(
								nameUri, proj.getRevision(), copyfile.src);
							objectId = inserter.insert(Constants.OBJ_BLOB, src);
							dcEntry = new DirCacheEntry(copyfile.dest);
							dcEntry.setObjectId(objectId);
							dcEntry.setFileMode(FileMode.REGULAR_FILE);
							builder.add(dcEntry);
						}
						for (LinkFile linkfile : proj.getLinkFiles()) {
							String link;
							if (linkfile.dest.contains("/")) { //$NON-NLS-1$
								link = FileUtils.relativizeGitPath(
									linkfile.dest.substring(0,
										linkfile.dest.lastIndexOf('/')),
									proj.getPath() + "/" + linkfile.src); //$NON-NLS-1$
							} else {
								link = proj.getPath() + "/" + linkfile.src; //$NON-NLS-1$
							}

							objectId = inserter.insert(Constants.OBJ_BLOB,
								link.getBytes(
									Constants.CHARACTER_ENCODING));
							dcEntry = new DirCacheEntry(linkfile.dest);
							dcEntry.setObjectId(objectId);
							dcEntry.setFileMode(FileMode.SYMLINK);
							builder.add(dcEntry);
						}
					}
				}
				String content = cfg.toText();

				// create a new DirCacheEntry for .gitmodules file.
				final DirCacheEntry dcEntry = new DirCacheEntry(Constants.DOT_GIT_MODULES);
				ObjectId objectId = inserter.insert(Constants.OBJ_BLOB,
						content.getBytes(Constants.CHARACTER_ENCODING));
				dcEntry.setObjectId(objectId);
				dcEntry.setFileMode(FileMode.REGULAR_FILE);
				builder.add(dcEntry);

				if (recordSubmoduleLabels) {
					// create a new DirCacheEntry for .gitattributes file.
					final DirCacheEntry dcEntryAttr = new DirCacheEntry(Constants.DOT_GIT_ATTRIBUTES);
					ObjectId attrId = inserter.insert(Constants.OBJ_BLOB,
							attributes.toString().getBytes(Constants.CHARACTER_ENCODING));
					dcEntryAttr.setObjectId(attrId);
					dcEntryAttr.setFileMode(FileMode.REGULAR_FILE);
					builder.add(dcEntryAttr);
				}

				builder.finish();
				ObjectId treeId = index.writeTree(inserter);

				// Create a Commit object, populate it and write it
				ObjectId headId = repo.resolve(targetBranch + "^{commit}"); //$NON-NLS-1$
				if (headId != null && rw.parseCommit(headId).getTree().getId().equals(treeId)) {
					// No change. Do nothing.
					return rw.parseCommit(headId);
				}

				CommitBuilder commit = new CommitBuilder();
				commit.setTreeId(treeId);
				if (headId != null)
					commit.setParentIds(headId);
				commit.setAuthor(author);
				commit.setCommitter(author);
				commit.setMessage(RepoText.get().repoCommitMessage);

				ObjectId commitId = inserter.insert(commit);
				inserter.flush();

				RefUpdate ru = repo.updateRef(targetBranch);
				ru.setNewObjectId(commitId);
				ru.setExpectedOldObjectId(headId != null ? headId : ObjectId.zeroId());
				Result rc = ru.update(rw);

				switch (rc) {
					case NEW:
					case FORCED:
					case FAST_FORWARD:
						// Successful. Do nothing.
						break;
					case REJECTED:
					case LOCK_FAILURE:
						throw new ConcurrentRefUpdateException(
								MessageFormat.format(
										JGitText.get().cannotLock, targetBranch),
								ru.getRef(),
								rc);
					default:
						throw new JGitInternalException(MessageFormat.format(
								JGitText.get().updatingRefFailed,
								targetBranch, commitId.name(), rc));
				}

				return rw.parseCommit(commitId);
			} catch (GitAPIException | IOException e) {
				throw new ManifestErrorException(e);
			}
		} else {
			try (Git git = new Git(repo)) {
				for (RepoProject proj : filteredProjects) {
					addSubmodule(proj.getUrl(), proj.getPath(),
							proj.getRevision(), proj.getCopyFiles(),
							proj.getLinkFiles(), git);
				}
				return git.commit().setMessage(RepoText.get().repoCommitMessage)
						.call();
			} catch (GitAPIException | IOException e) {
				throw new ManifestErrorException(e);
			}
		}
	}

	private void addSubmodule(String url, String path, String revision,
			List<CopyFile> copyfiles, List<LinkFile> linkfiles, Git git)
			throws GitAPIException, IOException {
		assert (!repo.isBare());
		assert (git != null);
		if (!linkfiles.isEmpty()) {
			throw new UnsupportedOperationException(
					JGitText.get().nonBareLinkFilesNotSupported);
		}

		SubmoduleAddCommand add = git.submoduleAdd().setPath(path).setURI(url);
		if (monitor != null)
			add.setProgressMonitor(monitor);

		Repository subRepo = add.call();
		if (revision != null) {
			try (Git sub = new Git(subRepo)) {
				sub.checkout().setName(findRef(revision, subRepo)).call();
			}
			subRepo.close();
			git.add().addFilepattern(path).call();
		}
		for (CopyFile copyfile : copyfiles) {
			copyfile.copy();
			git.add().addFilepattern(copyfile.dest).call();
		}
	}

	private void addSubmoduleBare(String url, String path, String revision,
			List<CopyFile> copyfiles, List<LinkFile> linkfiles,
			Set<String> groups, String recommendShallow) {
		assert (repo.isBare());
		assert (bareProjects != null);
		RepoProject proj = new RepoProject(url, path, revision, null, groups,
				recommendShallow);
		proj.addCopyFiles(copyfiles);
		proj.addLinkFiles(linkfiles);
		bareProjects.add(proj);
	}

	/*
	 * Assume we are document "a/b/index.html", what should we put in a href to get to "a/" ?
	 * Returns the child if either base or child is not a bare path. This provides a missing feature in
	 * java.net.URI (see http://bugs.java.com/view_bug.do?bug_id=6226081).
	 */
	private static final String SLASH = "/"; //$NON-NLS-1$
	static URI relativize(URI current, URI target) {
		if (!Objects.equals(current.getHost(), target.getHost())) {
			return target;
		}

		String cur = current.normalize().getPath();
		String dest = target.normalize().getPath();

		// TODO(hanwen): maybe (absolute, relative) should throw an exception.
		if (cur.startsWith(SLASH) != dest.startsWith(SLASH)) {
			return target;
		}

		while (cur.startsWith(SLASH)) {
			cur = cur.substring(1);
		}
		while (dest.startsWith(SLASH)) {
			dest = dest.substring(1);
		}

		if (cur.indexOf('/') == -1 || dest.indexOf('/') == -1) {
			// Avoid having to special-casing in the next two ifs.
			String prefix = "prefix/"; //$NON-NLS-1$
			cur = prefix + cur;
			dest = prefix + dest;
		}

		if (!cur.endsWith(SLASH)) {
			// The current file doesn't matter.
			int lastSlash = cur.lastIndexOf('/');
			cur = cur.substring(0, lastSlash);
		}
		String destFile = ""; //$NON-NLS-1$
		if (!dest.endsWith(SLASH)) {
			// We always have to provide the destination file.
			int lastSlash = dest.lastIndexOf('/');
			destFile = dest.substring(lastSlash + 1, dest.length());
			dest = dest.substring(0, dest.lastIndexOf('/'));
		}

		String[] cs = cur.split(SLASH);
		String[] ds = dest.split(SLASH);

		int common = 0;
		while (common < cs.length && common < ds.length && cs[common].equals(ds[common])) {
			common++;
		}

		StringJoiner j = new StringJoiner(SLASH);
		for (int i = common; i < cs.length; i++) {
			j.add(".."); //$NON-NLS-1$
		}
		for (int i = common; i < ds.length; i++) {
			j.add(ds[i]);
		}

		j.add(destFile);
		return URI.create(j.toString());
	}

	private static String findRef(String ref, Repository repo)
			throws IOException {
		if (!ObjectId.isId(ref)) {
			Ref r = repo.exactRef(R_REMOTES + DEFAULT_REMOTE_NAME + "/" + ref); //$NON-NLS-1$
			if (r != null)
				return r.getName();
		}
		return ref;
	}
}
