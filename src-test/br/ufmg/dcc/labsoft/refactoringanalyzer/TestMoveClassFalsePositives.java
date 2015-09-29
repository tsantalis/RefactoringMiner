package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestMoveClassFalsePositives {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
//		test.project("https://github.com/google/guava.git", "master").atCommit("cc3b0f8dc48497a2911dfe31f60fe186b3fed8d4").containsNothing();
//		test.project("https://github.com/raphw/byte-buddy.git", "master").atCommit("372f4ae6cebcd664e3b43cade356d1df233f6467").containsOnly(
//			"Move Class net.bytebuddy.dynamic.TargetType.TypeVariableProxy moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor.Substitutor.ForRawType.TypeVariableProxy",
//			"Move Attribute package ARRAY_MODIFIERS : int from class net.bytebuddy.description.type.TypeDescription.ArrayProjection to class net.bytebuddy.description.type.TypeDescription"
//		);
		
		test.project("https://github.com/liferay/liferay-portal.git", "master").atCommit("ed3204fae38bbe9b5f99029ebbf14f162ccea2f1").containsOnly();
		
		
		test.assertExpectations();
	}

}
