package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestWithRealInstances2 {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/k9mail/k-9.git", "master").atCommit("c00ec35921d4dd4d9dd17c5f5544d183df5c4501").containsOnly("Move Class com.android.email.mail.Folder moved to com.fsck.k9.mail.Folder");
		//test.project("https://github.com/k9mail/k-9.git", "master").atCommit("28e882782776861e54064d863dbb27ddeaa7aa9d").contains("Move Class com.fsck.k9.mail.store.TrustManagerFactory moved to com.android.email.mail.store.TrustManagerFactory");
		test.assertExpectations();
	}

}
