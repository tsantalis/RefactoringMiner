package br.ufmg.dcc.labsoft.refdetector.model.builder;

import org.junit.Test;

import br.ufmg.dcc.labsoft.refactoringanalyzer.TestBuilder;

public class TestRefDetector2 {

	@Test
	public void testRefDetector() throws Exception {
		TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2());

//		test
//		.project("https://github.com/structr/structr.git", "master")
		// The body of the methods are not similar enougth to be considered extract method
//		.atCommit("36287f7c3b09eff78395267a3ac0d7da067863fd").containsOnly(
			//"Extract Method public getRelationshipById(uuid String) : RelationshipInterface extracted from public get(uuid String) : GraphObject in class org.structr.core.app.StructrApp",
			//"Extract Method public getNodeById(uuid String) : NodeInterface extracted from public get(uuid String) : GraphObject in class org.structr.core.app.StructrApp"
//		);
		
		test
		.project("https://github.com/jMonkeyEngine/jmonkeyengine.git", "master")
		.atCommit("5989711f7315abe4c3da0f1516a3eb3a81da6716").containsOnly(
			"Extract Method protected movePanel(xoffset int, yoffset int) : void extracted from public mouseDragged(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel",
			"Extract Method protected saveLocation() : void extracted from public mousePressed(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel"
			//"Extract Method protected removeSelectedConnection(selectedItem Selectable) : void extracted from protected removeSelectedConnection() : void in class com.jme3.gde.materialdefinition.editor.Diagram"
		);
		
		test.assertExpectations();
	}
	
}
