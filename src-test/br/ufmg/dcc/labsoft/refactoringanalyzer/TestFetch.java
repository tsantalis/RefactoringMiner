package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

public class TestFetch {

	@Test
	public void testFetch() throws Exception {
		GitServiceImpl service = new GitServiceImpl();
		String cloneUrl = "https://github.com/clojure/clojure.git";
		String folder = "tmp/" + cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
		Repository repo = service.cloneIfNotExists(folder, cloneUrl);
		
		RevWalk w = service.fetchAndCreateNewRevsWalk(repo);
		try {
			for (RevCommit commit : w) {
				System.out.print(commit.getName());
				System.out.print(' ');
				System.out.println(commit.getShortMessage());
			}
		} finally {
			w.dispose();
		}
	}
	
}
