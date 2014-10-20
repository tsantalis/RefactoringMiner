package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.io.File;

import org.eclipse.jgit.api.Git;

public class GitServiceImpl implements GitService {

	@Override
	public void cloneIfNotExists(String folder, String cloneUrl) {
		File f = new File(folder);
		if (!f.exists()) {
			Git.cloneRepository()
			.setDirectory(f)
			.setURI(cloneUrl)
			.setCloneAllBranches(true)
			.call();	
		}
	}

}
