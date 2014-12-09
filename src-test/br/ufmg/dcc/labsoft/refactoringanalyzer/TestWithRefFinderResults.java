package br.ufmg.dcc.labsoft.refactoringanalyzer;

import static br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringMatcher.assertThat;
import static br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringMatcher.project;

import org.junit.Test;

public class TestWithRefFinderResults {

	@Test
	public void test() throws Exception {
		assertThat(

			project("https://github.com/danilofes/refactoring-toy-example.git", "master").contains()
//			.atCommit("36287f7c3b09eff78395267a3ac0d7da067863fd",
//				"Pull Up Attribute	private age : int from class org.animals.Labrador to class org.animals.Dog",
//				"Pull Up Attribute	private age : int from class org.animals.Poodle to class org.animals.Dog",
//				"Pull Up Operation	public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog",
//				"Pull Up Operation	public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog")
//			.atCommit("40950c317bd52ea5ce4cf0d19707fe426b66649c",
//				"Extract Operation	public takeABreath() : void extracted from public bark() : void in class org.animals.Dog"),
//			
//			project("https://github.com/junit-team/junit.git", "master").contains()
//			.atCommit("63cbed99a601e79c6a0ae389b2a57acdbd3e1b44",
//				"Rename Class	org.animals.Cow renamed to org.animals.CowRenamed",
//				"Extract Superclass	org.animals.Bird from classes [org.animals.Chicken, org.animals.Duck]")
		);
	}

}
