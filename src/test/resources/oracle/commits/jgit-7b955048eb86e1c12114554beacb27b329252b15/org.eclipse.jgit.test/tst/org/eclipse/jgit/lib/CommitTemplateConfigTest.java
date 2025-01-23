/*
 * Copyright (C) 2021, 2022 SAP SE and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/*
 * This test was moved from ConfigTest to allow skipping it when running the
 * test using bazel which doesn't allow tests to create files in the home
 * directory
 */
public class CommitTemplateConfigTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void testCommitTemplatePathInHomeDirecory()
			throws ConfigInvalidException, IOException {
		Config config = new Config(null);
		File tempFile = tmp.newFile("testCommitTemplate-");
		File workTree = tmp.newFolder("dummy-worktree");
		Repository repo = FileRepositoryBuilder.create(workTree);
		String templateContent = "content of the template";
		JGitTestUtil.write(tempFile, templateContent);
		// proper evaluation of the ~/ directory
		String homeDir = System.getProperty("user.home");
		File tempFileInHomeDirectory = File.createTempFile("fileInHomeFolder",
				".tmp", new File(homeDir));
		tempFileInHomeDirectory.deleteOnExit();
		JGitTestUtil.write(tempFileInHomeDirectory, templateContent);
		String expectedTemplatePath = "~/" + tempFileInHomeDirectory.getName();
		config = ConfigTest
				.parse("[commit]\n\ttemplate = " + expectedTemplatePath + "\n");
		String templatePath = config.get(CommitConfig.KEY)
				.getCommitTemplatePath();
		assertEquals(expectedTemplatePath, templatePath);
		assertEquals(templateContent,
				config.get(CommitConfig.KEY).getCommitTemplateContent(repo));
	}
}
