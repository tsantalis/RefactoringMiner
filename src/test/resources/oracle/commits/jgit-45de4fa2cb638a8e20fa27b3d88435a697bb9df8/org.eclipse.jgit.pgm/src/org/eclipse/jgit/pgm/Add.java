/*
 * Copyright (C) 2010, Sasa Zivkov <sasa.zivkov@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.pgm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@Command(common = true, usage = "usage_addFileContentsToTheIndex")
class Add extends TextBuiltin {

	@Option(name = "--update", aliases = { "-u" }, usage = "usage_onlyMatchAgainstAlreadyTrackedFiles")
	private boolean update = false;

	@Argument(required = true, metaVar = "metaVar_filepattern", usage = "usage_filesToAddContentFrom")
	private List<String> filepatterns = new ArrayList<>();

	/** {@inheritDoc} */
	@Override
	protected void run() throws Exception {
		try (Git git = new Git(db)) {
			AddCommand addCmd = git.add();
			addCmd.setUpdate(update);
			for (String p : filepatterns)
				addCmd.addFilepattern(p);
			addCmd.call();
		} catch (GitAPIException e) {
			throw die(e.getMessage(), e);
		}
	}
}
