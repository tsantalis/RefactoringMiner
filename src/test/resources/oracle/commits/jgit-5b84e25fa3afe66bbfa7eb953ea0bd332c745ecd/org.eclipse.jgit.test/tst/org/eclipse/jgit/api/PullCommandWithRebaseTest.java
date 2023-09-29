/*
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
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
package org.eclipse.jgit.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Before;
import org.junit.Test;

public class PullCommandWithRebaseTest extends RepositoryTestCase {
	/** Second Test repository */
	protected Repository dbTarget;

	private Git source;

	private Git target;

	private File sourceFile;

	private File targetFile;

	@Test
	public void testPullFastForward() throws Exception {
		PullResult res = target.pull().call();
		// nothing to update since we don't have different data yet
		assertTrue(res.getFetchResult().getTrackingRefUpdates().isEmpty());
		assertEquals(Status.UP_TO_DATE, res.getRebaseResult().getStatus());

		assertFileContentsEqual(targetFile, "Hello world");

		// change the source file
		writeToFile(sourceFile, "Another change");
		source.add().addFilepattern("SomeFile.txt").call();
		source.commit().setMessage("Some change in remote").call();

		res = target.pull().call();

		assertFalse(res.getFetchResult().getTrackingRefUpdates().isEmpty());
		assertEquals(Status.FAST_FORWARD, res.getRebaseResult().getStatus());
		assertFileContentsEqual(targetFile, "Another change");
		assertEquals(RepositoryState.SAFE, target.getRepository()
				.getRepositoryState());

		res = target.pull().call();
		assertEquals(Status.UP_TO_DATE, res.getRebaseResult().getStatus());
	}

	@Test
	public void testPullFastForwardWithBranchInSource() throws Exception {
		PullResult res = target.pull().call();
		// nothing to update since we don't have different data yet
		assertTrue(res.getFetchResult().getTrackingRefUpdates().isEmpty());
		assertEquals(Status.UP_TO_DATE, res.getRebaseResult().getStatus());

		assertFileContentsEqual(targetFile, "Hello world");

		// change the source file
		writeToFile(sourceFile, "Another change\n\n\n\nFoo");
		source.add().addFilepattern("SomeFile.txt").call();
		RevCommit initialCommit = source.commit()
				.setMessage("Some change in remote").call();

		// modify the source file in a branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		writeToFile(sourceFile, "Another change\n\n\n\nBoo");
		source.add().addFilepattern("SomeFile.txt").call();
		RevCommit sideCommit = source.commit()
				.setMessage("Some change in remote").call();

		// modify the source file on master
		checkoutBranch("refs/heads/master");
		writeToFile(sourceFile, "More change\n\n\n\nFoo");
		source.add().addFilepattern("SomeFile.txt").call();
		source.commit().setMessage("Some change in remote").call();

		// merge side into master
		MergeResult result = source.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());

	}

	@Test
	public void testPullFastForwardDetachedHead() throws Exception {
		Repository repository = source.getRepository();
		writeToFile(sourceFile, "2nd commit");
		source.add().addFilepattern("SomeFile.txt").call();
		source.commit().setMessage("2nd commit").call();

		try (RevWalk revWalk = new RevWalk(repository)) {
			// git checkout HEAD^
			String initialBranch = repository.getBranch();
			Ref initialRef = repository.findRef(Constants.HEAD);
			RevCommit initialCommit = revWalk
					.parseCommit(initialRef.getObjectId());
			assertEquals("this test need linear history", 1,
					initialCommit.getParentCount());
			source.checkout().setName(initialCommit.getParent(0).getName())
					.call();
			assertFalse("expected detached HEAD",
					repository.getFullBranch().startsWith(Constants.R_HEADS));

			// change and commit another file
			File otherFile = new File(sourceFile.getParentFile(),
					System.currentTimeMillis() + ".tst");
			writeToFile(otherFile, "other 2nd commit");
			source.add().addFilepattern(otherFile.getName()).call();
			RevCommit newCommit = source.commit().setMessage("other 2nd commit")
					.call();

			// git pull --rebase initialBranch
			source.pull().setRebase(true).setRemote(".")
					.setRemoteBranchName(initialBranch)
					.call();

			assertEquals(RepositoryState.SAFE,
					source.getRepository().getRepositoryState());
			Ref head = source.getRepository().findRef(Constants.HEAD);
			RevCommit headCommit = revWalk.parseCommit(head.getObjectId());

			// HEAD^ == initialCommit, no merge commit
			assertEquals(1, headCommit.getParentCount());
			assertEquals(initialCommit, headCommit.getParent(0));

			// both contributions for both commits are available
			assertFileContentsEqual(sourceFile, "2nd commit");
			assertFileContentsEqual(otherFile, "other 2nd commit");
			// HEAD has same message as rebased commit
			assertEquals(newCommit.getShortMessage(),
					headCommit.getShortMessage());
		}
	}

	@Test
	public void testPullConflict() throws Exception {
		PullResult res = target.pull().call();
		// nothing to update since we don't have different data yet
		assertTrue(res.getFetchResult().getTrackingRefUpdates().isEmpty());
		assertEquals(Status.UP_TO_DATE, res.getRebaseResult().getStatus());

		assertFileContentsEqual(targetFile, "Hello world");

		// change the source file
		writeToFile(sourceFile, "Source change");
		source.add().addFilepattern("SomeFile.txt").call();
		source.commit().setMessage("Source change in remote").call();

		// change the target file
		writeToFile(targetFile, "Target change");
		target.add().addFilepattern("SomeFile.txt").call();
		target.commit().setMessage("Target change in local").call();

		res = target.pull().call();

		String remoteUri = target
				.getRepository()
				.getConfig()
				.getString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin",
						ConfigConstants.CONFIG_KEY_URL);

		assertFalse(res.getFetchResult().getTrackingRefUpdates().isEmpty());
		assertEquals(Status.STOPPED, res.getRebaseResult().getStatus());
		String result = "<<<<<<< Upstream, based on branch 'master' of "
				+ remoteUri
				+ "\nSource change\n=======\nTarget change\n>>>>>>> 42453fd Target change in local\n";
		assertFileContentsEqual(targetFile, result);
		assertEquals(RepositoryState.REBASING_MERGE, target
				.getRepository().getRepositoryState());
	}

	@Test
	public void testPullLocalConflict() throws Exception {
		target.branchCreate().setName("basedOnMaster").setStartPoint(
				"refs/heads/master").setUpstreamMode(SetupUpstreamMode.NOTRACK)
				.call();
		StoredConfig config = target.getRepository().getConfig();
		config.setString("branch", "basedOnMaster", "remote", ".");
		config.setString("branch", "basedOnMaster", "merge",
				"refs/heads/master");
		config.setBoolean("branch", "basedOnMaster", "rebase", true);
		config.save();
		target.getRepository().updateRef(Constants.HEAD).link(
				"refs/heads/basedOnMaster");
		PullResult res = target.pull().call();
		// nothing to update since we don't have different data yet
		assertNull(res.getFetchResult());
		assertEquals(Status.UP_TO_DATE, res.getRebaseResult().getStatus());

		assertFileContentsEqual(targetFile, "Hello world");

		// change the file in master
		target.getRepository().updateRef(Constants.HEAD).link(
				"refs/heads/master");
		writeToFile(targetFile, "Master change");
		target.add().addFilepattern("SomeFile.txt").call();
		target.commit().setMessage("Source change in master").call();

		// change the file in slave
		target.getRepository().updateRef(Constants.HEAD).link(
				"refs/heads/basedOnMaster");
		writeToFile(targetFile, "Slave change");
		target.add().addFilepattern("SomeFile.txt").call();
		target.commit().setMessage("Source change in based on master").call();

		res = target.pull().call();

		assertNull(res.getFetchResult());
		assertEquals(Status.STOPPED, res.getRebaseResult().getStatus());
		String result = "<<<<<<< Upstream, based on branch 'master' of local repository\n"
				+ "Master change\n=======\nSlave change\n>>>>>>> 4049c9e Source change in based on master\n";
		assertFileContentsEqual(targetFile, result);
		assertEquals(RepositoryState.REBASING_MERGE, target
				.getRepository().getRepositoryState());
	}

    @Test
	public void testPullFastForwardWithLocalCommitAndRebaseFlagSet() throws Exception {
		final String SOURCE_COMMIT_MESSAGE = "Source commit message for rebase flag test";
		final String TARGET_COMMIT_MESSAGE = "Target commit message for rebase flag test";

		assertFalse(SOURCE_COMMIT_MESSAGE.equals(TARGET_COMMIT_MESSAGE));

		final String SOURCE_FILE_CONTENTS = "Source change";
		final String NEW_FILE_CONTENTS = "New file from target";

		// make sure the config for target says we should pull with merge
		// we will override this later with the setRebase method
		StoredConfig targetConfig = dbTarget.getConfig();
		targetConfig.setBoolean("branch", "master", "rebase", false);
		targetConfig.save();

		// create commit in source
		writeToFile(sourceFile, SOURCE_FILE_CONTENTS);
		source.add().addFilepattern(sourceFile.getName()).call();
		source.commit().setMessage(SOURCE_COMMIT_MESSAGE).call();

		// create commit in target, not conflicting with the new commit in source
		File newFile = new File(dbTarget.getWorkTree().getPath() + "/newFile.txt");
		writeToFile(newFile, NEW_FILE_CONTENTS);
		target.add().addFilepattern(newFile.getName()).call();
		target.commit().setMessage(TARGET_COMMIT_MESSAGE).call();

		// verify that rebase is set to false in the config
		assertFalse(targetConfig.getBoolean("branch", "master", "rebase", true));

		// pull with rebase - local commit in target should be on top
		PullResult pullResult = target.pull().setRebase(true).call();

		// make sure pull is considered successful
		assertTrue(pullResult.isSuccessful());

		// verify rebase result is ok
		RebaseResult rebaseResult = pullResult.getRebaseResult();
		assertNotNull(rebaseResult);
		assertNull(rebaseResult.getFailingPaths());
		assertEquals(Status.OK, rebaseResult.getStatus());

		// Get the HEAD and HEAD~1 commits
		Repository targetRepo = target.getRepository();
		try (RevWalk revWalk = new RevWalk(targetRepo)) {
			ObjectId headId = targetRepo.resolve(Constants.HEAD);
			RevCommit root = revWalk.parseCommit(headId);
			revWalk.markStart(root);
			// HEAD
			RevCommit head = revWalk.next();
			// HEAD~1
			RevCommit beforeHead = revWalk.next();

			// verify the commit message on the HEAD commit
			assertEquals(TARGET_COMMIT_MESSAGE, head.getFullMessage());
			// verify the commit just before HEAD
			assertEquals(SOURCE_COMMIT_MESSAGE, beforeHead.getFullMessage());

			// verify file states
			assertFileContentsEqual(sourceFile, SOURCE_FILE_CONTENTS);
			assertFileContentsEqual(newFile, NEW_FILE_CONTENTS);
			// verify repository state
			assertEquals(RepositoryState.SAFE, target
				.getRepository().getRepositoryState());
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		dbTarget = createWorkRepository();
		source = new Git(db);
		target = new Git(dbTarget);

		// put some file in the source repo
		sourceFile = new File(db.getWorkTree(), "SomeFile.txt");
		writeToFile(sourceFile, "Hello world");
		// and commit it
		source.add().addFilepattern("SomeFile.txt").call();
		source.commit().setMessage("Initial commit for source").call();

		// configure the target repo to connect to the source via "origin"
		StoredConfig targetConfig = dbTarget.getConfig();
		targetConfig.setString("branch", "master", "remote", "origin");
		targetConfig
				.setString("branch", "master", "merge", "refs/heads/master");
		RemoteConfig config = new RemoteConfig(targetConfig, "origin");

		config
				.addURI(new URIish(source.getRepository().getWorkTree()
						.getAbsolutePath()));
		config.addFetchRefSpec(new RefSpec(
				"+refs/heads/*:refs/remotes/origin/*"));
		config.update(targetConfig);
		targetConfig.save();

		targetFile = new File(dbTarget.getWorkTree(), "SomeFile.txt");
		// make sure we have the same content
		target.pull().call();
		target.checkout().setStartPoint("refs/remotes/origin/master").setName(
				"master").call();

		targetConfig
				.setString("branch", "master", "merge", "refs/heads/master");
		targetConfig.setBoolean("branch", "master", "rebase", true);
		targetConfig.save();

		assertFileContentsEqual(targetFile, "Hello world");
	}

	private static void writeToFile(File actFile, String string)
			throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(actFile);
			fos.write(string.getBytes("UTF-8"));
			fos.close();
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	private static void assertFileContentsEqual(File actFile, String string)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		byte[] buffer = new byte[100];
		try {
			fis = new FileInputStream(actFile);
			int read = fis.read(buffer);
			while (read > 0) {
				bos.write(buffer, 0, read);
				read = fis.read(buffer);
			}
			String content = new String(bos.toByteArray(), "UTF-8");
			assertEquals(string, content);
		} finally {
			if (fis != null)
				fis.close();
		}
	}
}
