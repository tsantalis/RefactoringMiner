/*
 * Copyright (C) 2023 Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.dircache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Tests for checking out with invalid paths.
 */
public class InvalidPathCheckoutTest extends RepositoryTestCase {

	private DirCacheEntry brokenEntry(String fileName, RevBlob blob) {
		DirCacheEntry entry = new DirCacheEntry("XXXX/" + fileName);
		entry.path[0] = '.';
		entry.path[1] = 'g';
		entry.path[2] = 'i';
		entry.path[3] = 't';
		entry.setFileMode(FileMode.REGULAR_FILE);
		entry.setObjectId(blob);
		return entry;
	}

	@Test
	public void testCheckoutIntoDotGit() throws Exception {
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			// DirCacheEntry does not allow any path component to contain
			// ".git". C git also forbids this. But what if somebody creates
			// such an entry explicitly?
			RevCommit base = repo
					.commit(repo.tree(brokenEntry("b", repo.blob("test"))));
			try (Git git = new Git(db)) {
				assertThrows(InvalidPathException.class, () -> git.reset()
						.setMode(ResetType.HARD).setRef(base.name()).call());
				File b = new File(new File(trash, ".git"), "b");
				assertFalse(".git/b should not exist", b.exists());
			}
		}
	}

}
