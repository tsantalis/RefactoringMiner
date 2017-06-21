package org.refactoringminer.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration.JaxbSessionFactory;
import org.refactoringminer.test.TestBuilder.ProjectMatcher.CommitMatcher;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RefactoringPopulator {

	public enum Systems {
		aTunes(1), argoUML(2), jUnit(4), ANTLR4(8), FSE(16), All(31);
		private int value;

		private Systems(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum Refactorings {
		MoveMethod(1), MoveAttribute(2), InlineMethod(4), ExtractMethod(8), PushDownMethod(16), PushDownAttribute(
				32), PullUpMethod(64), PullUpAttribute(128), ExtractInterface(256), ExtractSuperclass(512), MoveClass(
						1024), RenamePackage(2048), RenameMethod(4096), ExtractAndMoveMethod(
								8192), RenameClass(16384), MoveSourceFolder(32768), All(65535);
		private int value;

		private Refactorings(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static void feedRefactoringsInstances(int refactoringsFlag, int systemsFlag, TestBuilder test)
			throws JsonParseException, JsonMappingException, IOException {

		if ((systemsFlag & Systems.FSE.getValue()) > 0) {
			prepareFSERefactorings(test, refactoringsFlag);
		}

		if ((systemsFlag & Systems.aTunes.getValue()) > 0) {
			aTunesRefactorings(test, refactoringsFlag);
		}

		if ((systemsFlag & Systems.argoUML.getValue()) > 0) {
			argoRefactorings(test, refactoringsFlag);
		}

		// if ((systemsFlag & Systems.ANTLER4.getValue())>0){
		// antlrRefactorings(test, systemsFlag);
		// }

		if ((systemsFlag & Systems.jUnit.getValue()) > 0) {
			jUnitRefactorings(test, systemsFlag);
		}

		// if ((systemsFlag & Systems.FSE.getValue()) > 0) {
		// FSE_ExtractMethodRefactorings(test, systemsFlag);
		// FSE_PullUpMethodRefactorings(test, systemsFlag);
		// FSE_InlineMethodRefactorings(test, systemsFlag);
		// }
	}

	private static void argoRefactorings(TestBuilder test, int flag) {

		test.project("https://github.com/danilofes/argouml-refactorings.git", "master")
				.atCommit("da5ea3acc1b6528cd7ec731c741e5f691c25dc6f")
				.containsOnly(
						"Extract Method private extracted(contents List, size int, list List) : void extracted from public getContents(diagram Diagram) : List in class org.argouml.persistence.PgmlUtility",
						"Extract Method private extracted(col int) : Class<?> extracted from public getColumnClass(col int) : Class<?> in class org.argouml.ui.cmd.SettingsTabShortcuts.ShortcutTableModel",
						"Extract Method private extracted(multiplicity StringBuilder, name String, properties List<String>, stereotype StringBuilder, type String, value StringBuilder, visibility String) : void extracted from protected parseAttribute(text String, attribute Object) : void in class org.argouml.notation.providers.uml.AttributeNotationUml",
						"Extract Method private extracted(node Object) : boolean extracted from public isLeaf(node Object) : boolean in class org.argouml.cognitive.ui.GoListToOffenderToItem",
						"Extract Method private extracted(targets Object[]) : void extracted from private setTargets(targets Object[]) : void in class org.argouml.ui.explorer.ExplorerTree.ExplorerTargetListener",
						"Extract Method private extracted() : void extracted from public structureChanged() : void in class org.argouml.ui.explorer.ExplorerEventAdaptor",
						"Extract Method private extracted(pce PropertyChangeEvent) : void extracted from public updateListener(modelElement Object, pce PropertyChangeEvent) : void in class org.argouml.notation.providers.AttributeNotation",
						"Extract Method private extracted() : void extracted from public componentHidden(e ComponentEvent) : void in class org.argouml.cognitive.checklist.ui.TabChecklist",
						"Extract Method private extracted() : void extracted from public NotationComboBox() in class org.argouml.notation.ui.NotationComboBox",
						"Extract Method private extracted(m Object, profile Profile) : void extracted from private checkProfileFor(o Object, m Object) : void in class org.argouml.kernel.ProjectImpl")

				.atCommit("d8215ce80309b4bf8c92ac2e096cb8b06275aee6")
				.containsOnly(
						"Inline Method private buildPanel() : void inlined to public getTabPanel() : JPanel in class org.argouml.notation.ui.SettingsTabNotation",
						"Inline Method private getAssociationActions() : Object[] inlined to protected getUmlActions() : Object[] in class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Inline Method private getPersistenceVersionFromFile(file File) : int inlined to protected doLoad(originalFile File, file File, progressMgr ProgressMgr) : Project in class org.argouml.persistence.UmlFilePersister",
						"Inline Method protected removeAllElementListeners(listener PropertyChangeListener) : void inlined to public removeAllElementListeners() : void in class org.argouml.notation.NotationProvider",
						"Inline Method private findTarget(t Object) : Object inlined to public shouldBeEnabled(t Object) : boolean in class org.argouml.cognitive.checklist.ui.TabChecklist",
						"Inline Method private findTarget(t Object) : Object inlined to public setTarget(t Object) : void in class org.argouml.cognitive.checklist.ui.TabChecklist",
						"Inline Method private initFigs() : void inlined to public AbstractFigNode(owner Object, bounds Rectangle, settings DiagramSettings) in class org.argouml.uml.diagram.deployment.ui.AbstractFigNode",
						"Inline Method private makeSubStatesIcon(x int, y int) : void inlined to private initFigs() : void in class org.argouml.uml.diagram.activity.ui.FigSubactivityState",
						"Inline Method private setTodoList(member AbstractProjectMember) : void inlined to public add(member ProjectMember) : boolean in class org.argouml.kernel.MemberList",
						"Inline Method private setTodoList(member AbstractProjectMember) : void inlined to public remove(member Object) : boolean in class org.argouml.kernel.MemberList",
						// FN** parameter substitution?
						"Inline Method private appendArrays(first T[], second T[]) : T[] inlined to public loadRules() : void in class org.argouml.ui.explorer.PerspectiveManager",
						"Inline Method private dealWithVisibility(attribute Object, visibility String) : void inlined to protected parseAttribute(text String, attribute Object) : void in class org.argouml.notation.providers.uml.AttributeNotationUml")

				.atCommit("f7daed39b7096a537d138c60b7429affaec368d6")
				.containsOnly(
						"Move Method public makeKey(k1 String, k2 String, k3 String, k4 String, k5 String) : ConfigurationKey from class org.argouml.configuration.Configuration to public makeKey(k1 String, k2 String, k3 String, k4 String, k5 String) : ConfigurationKey from class org.argouml.cognitive.ui.ActionGoToCritique",
						"Move Method protected createTempFile(file File) : File from class org.argouml.persistence.AbstractFilePersister to protected createTempFile(file File, abstractFilePersister AbstractFilePersister) : File from class org.argouml.persistence.ModelMemberFilePersister",
						"Move Method private initDistributeMenu(distribute JMenu) : void from class org.argouml.ui.cmd.GenericArgoMenuBar to package initDistributeMenu(distribute JMenu) : void from class org.argouml.ui.cmd.NavigateTargetForwardAction",
						"Move Method public selectTabNamed(tabName String) : boolean from class org.argouml.ui.DetailsPane to public selectTabNamed(tabName String, pane DetailsPane) : boolean from class org.argouml.ui.ArgoToolbarManager",
						"Move Method public loadUserPerspectives() : void from class org.argouml.ui.explorer.PerspectiveManager to public loadUserPerspectives(perspectiveManager PerspectiveManager) : void from class org.argouml.ui.explorer.ActionDeployProfile",
						// method getCriticizedDesignMaterials not moved, but
						// actually getCreateStereotype from WizAddConstructor
						// to CrTooManyStates Move Method
						// getCriticizedDesignMaterials moved from class
						// org.argouml.uml.cognitive.critics.WizAddConstructor
						// to class
						// org.argouml.uml.cognitive.critics.CrTooManyStates",
						"Move Method private getCreateStereotype(obj Object) : Object from class org.argouml.uml.cognitive.critics.WizAddConstructor to package getCreateStereotype(obj Object) : Object from class org.argouml.uml.cognitive.critics.CrTooManyStates",
						"Move Method protected isReverseEdge(index int) : boolean from class org.argouml.uml.diagram.deployment.ui.SelectionObject to protected isReverseEdge(index int) : boolean from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode",
						"Move Method protected setStandardBounds(x int, y int, width int, height int) : void from class org.argouml.uml.diagram.state.ui.FigHistoryState to protected setStandardBounds(x int, y int, width int, height int, state FigHistoryState) : void from class org.argouml.uml.diagram.state.ui.SelectionState",
						"Move Method private buildString(st Object) : String from class org.argouml.uml.diagram.ui.ActionAddStereotype to package buildString(st Object) : String from class org.argouml.uml.diagram.ui.SelectionEdgeClarifiers",
						// FN** method moved, but has only one line
						"Move Method protected canEdit(f Fig) : boolean from class org.argouml.uml.diagram.use_case.ui.FigInclude to protected canEdit(f Fig) : boolean from class org.argouml.uml.diagram.use_case.ui.SelectionActor")

				.atCommit("4ba691a56b441f0ce9d1683a8ad3ed5b82d52268")
				.containsOnly(
						"Move Attribute private flatChildren : List<ToDoItem> from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Move Attribute private cover : FigRect from class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Move Attribute private description : WizDescription from class org.argouml.cognitive.ui.TabToDo to class org.argouml.cognitive.ui.GoListToGoalsToItems",
						"Move Attribute private slidersToGoals : Hashtable<JSlider,Goal> from class org.argouml.cognitive.ui.GoalsDialog to class org.argouml.cognitive.ui.InitCognitiveUI",
						"Move Attribute private weight : float from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramNode to class org.argouml.uml.diagram.static_structure.layout.ClassdiagramModelElementFactory",
						"Move Attribute private choices : List<String> from class org.argouml.cognitive.ui.WizStepChoice to class org.argouml.cognitive.ui.ActionOpenDecisions",
						"Move Attribute private updatingSelection : boolean from class org.argouml.ui.explorer.ExplorerTree to class org.argouml.ui.explorer.PerspectiveComboBox",
						"Move Attribute private icon : Icon from class org.argouml.notation.NotationNameImpl to class org.argouml.notation.InitNotation",
						// there is a new comment field in
						// StylePanelFigInterface, but not moved.
						// refreshTransaction is moved from
						// StylePanelFigInterface to SelectionComment
						// "Move Attribute
						// org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface
						// comment
						// org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface",
						"Move Attribute private refreshTransaction : boolean from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface to class org.argouml.uml.diagram.static_structure.ui.SelectionComment",
						"Move Attribute private upperRect : FigRect from class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public setCover(cover FigRect) : void extracted from public clone() : Object in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public setCover(cover FigRect) : void extracted from private initClassifierRoleFigs() : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getSlidersToGoals() : Hashtable<JSlider, Goal> extracted from private initMainPanel() : void in class org.argouml.cognitive.ui.GoalsDialog & moved to class org.argouml.cognitive.ui.InitCognitiveUI",
						"Extract And Move Method public getSlidersToGoals() : Hashtable<JSlider, Goal> extracted from public stateChanged(ce ChangeEvent) : void in class org.argouml.cognitive.ui.GoalsDialog & moved to class org.argouml.cognitive.ui.InitCognitiveUI",
						"Extract And Move Method public getDescription() : WizDescription extracted from public showStep(ws JPanel) : void in class org.argouml.cognitive.ui.TabToDo & moved to class org.argouml.cognitive.ui.GoListToGoalsToItems",
						"Extract And Move Method public getDescription() : WizDescription extracted from public showDescription() : void in class org.argouml.cognitive.ui.TabToDo & moved to class org.argouml.cognitive.ui.GoListToGoalsToItems",
						"Extract And Move Method public getDescription() : WizDescription extracted from private setTargetInternal(item Object) : void in class org.argouml.cognitive.ui.TabToDo & moved to class org.argouml.cognitive.ui.GoListToGoalsToItems",
						"Extract And Move Method public getFlatChildren() : List<ToDoItem> extracted from public getChildCount(parent Object) : int in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public getFlatChildren() : List<ToDoItem> extracted from public getChild(parent Object, index int) : Object in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public getFlatChildren() : List<ToDoItem> extracted from public getIndexOfChild(parent Object, child Object) : int in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public getFlatChildren() : List<ToDoItem> extracted from public addFlatChildren(node Object) : void in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public getFlatChildren() : List<ToDoItem> extracted from public calcFlatChildren() : void in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public getChoices() : List<String> extracted from public actionPerformed(e ActionEvent) : void in class org.argouml.cognitive.ui.WizStepChoice & moved to class org.argouml.cognitive.ui.ActionOpenDecisions",
						"Extract And Move Method public setUpperRect(upperRect FigRect) : void extracted from public clone() : Object in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent & moved to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public setUpperRect(upperRect FigRect) : void extracted from private initFigs() : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent & moved to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public getUpperRect() : FigRect extracted from private initFigs() : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent & moved to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public getUpperRect() : FigRect extracted from protected setStandardBounds(x int, y int, w int, h int) : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent & moved to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public getUpperRect() : FigRect extracted from public setLineColor(c Color) : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent & moved to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
						"Extract And Move Method public getCover() : FigRect extracted from public setLineColor(col Color) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public setFillColor(col Color) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public setFilled(f boolean) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public getFillColor() : Color in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public getLineColor() : Color in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public getLineWidth() : int in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public setLineWidth(w int) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from public isFilled() : boolean in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						// EXTRA
						"Extract And Move Method public getCover() : FigRect extracted from private initClassifierRoleFigs() : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public getCover() : FigRect extracted from protected setStandardBounds(x int, y int, w int, h int) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole & moved to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
						"Extract And Move Method public setIcon(icon Icon) : void extracted from protected NotationNameImpl(myName String, myVersion String, myIcon Icon) in class org.argouml.notation.NotationNameImpl & moved to class org.argouml.notation.InitNotation",
						"Extract And Move Method public setChoices(choices List) : void extracted from public WizStepChoice(w Wizard, instr String, ch List) in class org.argouml.cognitive.ui.WizStepChoice & moved to class org.argouml.cognitive.ui.ActionOpenDecisions",
						"Extract And Move Method public setFlatChildren(flatChildren List) : void extracted from public ToDoPerspective(name String) in class org.argouml.cognitive.ui.ToDoPerspective & moved to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
						"Extract And Move Method public setRefreshTransaction(refreshTransaction boolean) : void extracted from public refresh() : void in class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface & moved to class org.argouml.uml.diagram.static_structure.ui.SelectionComment",
						"Extract And Move Method public isRefreshTransaction() : boolean extracted from public itemStateChanged(e ItemEvent) : void in class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface & moved to class org.argouml.uml.diagram.static_structure.ui.SelectionComment",
						// TBD
						"Extract And Move Method public getWeight() : float extracted from public getWeight() : float in class org.argouml.uml.diagram.static_structure.layout.ClassdiagramNode & moved to class org.argouml.uml.diagram.static_structure.layout.ClassdiagramModelElementFactory",
						"Extract And Move Method public setWeight(weight float) : void extracted from public setWeight(w float) : void in class org.argouml.uml.diagram.static_structure.layout.ClassdiagramNode & moved to class org.argouml.uml.diagram.static_structure.layout.ClassdiagramModelElementFactory",
						"Extract And Move Method public getIcon() : Icon extracted from public getIcon() : Icon in class org.argouml.notation.NotationNameImpl & moved to class org.argouml.notation.InitNotation"

				// "Extract Method public setCover(FigRect cover) : void
				// extracted from private initClassifierRoleFigs() : void in
				// class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public setCover(FigRect cover) : void
				// extracted from public clone() : Object in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public setLineColor(col Color) : void in
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public getLineColor() : Color in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public setFillColor(col Color) : void in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public getFillColor() : Color in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public setFilled(f boolean) : void in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public isFilled() : boolean in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public setLineWidth(w int) : void in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getCover() : FigRect extracted from
				// public getLineWidth() : int in class
				// org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
				// "Extract Method public getUpperRect() : FigRect extracted
				// from private initFigs() : void in class
				// org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
				// "Extract Method public getUpperRect() : FigRect extracted
				// from public setLineColor(Color c) : void in class
				// org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
				// "Extract Method public getUpperRect() : FigRect extracted
				// from protected setStandardBounds(x int,y int, w int, h int) :
				// void in
				// org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
				// "Extract Method public setUpperRect(FigRect upperRect) : void
				// extracted from private initFigs() : void in class
				// org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
				// "Extract Method public setUpperRect(FigRect upperRect) : void
				// extracted from public clone() : Object in class
				// org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
				// "Extract Method public getFlatChildren() : List<ToDoItem>
				// extracted from public getChild(parent Object, index int) :
				// Object in class org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public getFlatChildren() : List<ToDoItem>
				// extracted from public getChildCount(parent Object) : int in
				// class org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public getFlatChildren() : List<ToDoItem>
				// extracted from public getIndexOfChild(parent Object, child
				// Object) : int in class
				// org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public getFlatChildren() : List<ToDoItem>
				// extracted from public calcFlatChildren() : void in class
				// org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public getFlatChildren() : List<ToDoItem>
				// extracted from public addFlatChildren(node Object) : void in
				// class org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public setFlatChildren(List<ToDoItem>
				// flatChildren) : void extracted from public
				// addFlatChildren(node Object) : void in class
				// org.argouml.cognitive.ui.ToDoPerspective",
				// "Extract Method public getChoices() : List<String> extracted
				// from public actionPerformed(e ActionEvent) : void in class
				// org.argouml.cognitive.ui.ActionOpenDecisions",
				// "Extract Method public setChoices(choices List<String>) :
				// void extracted from public WizStepChoice(w Wizard, instr
				// String, ch List<String>) : void in class
				// org.argouml.cognitive.ui.ActionOpenDecisions"
				// "Extract Method public getSlidersToGoals() :
				// Hashtable<JSlider, Goal> extracted from private
				// initMainPanel() : void in class
				// org.argouml.cognitive.ui.GoalsDialog",
				// "Extract Method public getSlidersToGoals() :
				// Hashtable<JSlider, Goal> extracted from public
				// stateChanged(ChangeEvent ce) : void in class
				// org.argouml.cognitive.ui.GoalsDialog",
				// "Extract Method public getDescription() : WizDescription
				// extracted from public showDescription() : void in class
				// org.argouml.cognitive.ui.TabToDo",
				// "Extract Method public getDescription() : WizDescription
				// extracted from public showStep(ws JPanel) : void in class
				// org.argouml.cognitive.ui.TabToDo",
				// "Extract Method public getDescription() : WizDescription
				// extracted from private setTargetInternal(item Object) : void
				// in class org.argouml.cognitive.ui.TabToDo"

				).atCommit("4ba691a56b441f0ce9d1683a8ad3ed5b82d52268")
				// FP** one-liner, more like encapsulate field
				.notContains(
						"Extract Method public getCover() : FigRect extracted from * in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole")

				.atCommit("0368849555a9d7af5e8ff7c416b0219614543208")
				.containsOnly(
						"Push Down Attribute private bgImage : Image from class org.argouml.ui.explorer.ExplorerTree to class org.argouml.ui.explorer.DnDExplorerTree",
						"Push Down Attribute private cover : FigCube from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.deployment.ui.FigMNode",
						"Push Down Attribute private cover : FigCube from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.deployment.ui.FigNodeInstance",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.static_structure.ui.StylePanelFigClass",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.static_structure.ui.StylePanelFigPackage",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.ui.StylePanelFigAssociationClass",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.ui.StylePanelFigMessage",
						"Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.use_case.ui.StylePanelFigUseCase",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByDecision",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByGoal",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByOffender",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByPoster",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByPriority",
						"Push Down Attribute private flatChildren : List from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByType",
						"Push Down Attribute private name : String from class org.argouml.ui.PerspectiveSupport to class org.argouml.ui.TreeModelSupport",
						"Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.ui.StylePanel",
						"Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.uml.diagram.ui.TabDiagram",
						"Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.uml.ui.TabStyle",
						"Push Down Attribute private priority : int from class org.argouml.cognitive.Critic to class org.argouml.cognitive.CompoundCritic",
						"Push Down Attribute private priority : int from class org.argouml.cognitive.Critic to class org.argouml.uml.cognitive.critics.CrUML",
						"Push Down Attribute private started : boolean from class org.argouml.cognitive.critics.Wizard to class org.argouml.uml.cognitive.critics.UMLWizard",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizDescription",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepChoice",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepConfirm",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepCue",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepManyTextFields",
						"Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepTextField",
						"Push Down Attribute private uUIDRefs : HashMap from class org.argouml.persistence.ModelMemberFilePersister to class org.argouml.persistence.OldModelMemberFilePersister",
						// The following cases need more discussion.
						"Push Down Method public getPriority() : int from class org.argouml.cognitive.Critic to public getPriority() : int from class org.argouml.cognitive.CompoundCritic",
						"Push Down Method public getPriority() : int from class org.argouml.cognitive.Critic to public getPriority() : int from class org.argouml.uml.cognitive.critics.CrUML",
						"Push Down Method public setPriority(p int) : void from class org.argouml.cognitive.Critic to public setPriority(p int) : void from class org.argouml.cognitive.CompoundCritic",
						"Push Down Method public setPriority(p int) : void from class org.argouml.cognitive.Critic to public setPriority(p int) : void from class org.argouml.uml.cognitive.critics.CrUML",
						"Push Down Method public getName() : String from class org.argouml.ui.PerspectiveSupport to public getName() : String from class org.argouml.ui.TreeModelSupport",
						"Push Down Method public setName(s String) : void from class org.argouml.ui.PerspectiveSupport to public setName(s String) : void from class org.argouml.ui.TreeModelSupport",
						"Push Down Method public getUUIDRefs() : HashMap from class org.argouml.persistence.ModelMemberFilePersister to public getUUIDRefs() : HashMap from class org.argouml.persistence.OldModelMemberFilePersister",
						"Push Down Method public isStarted() : boolean from class org.argouml.cognitive.critics.Wizard to public isStarted() : boolean from class org.argouml.uml.cognitive.critics.UMLWizard")

				// .atCommit("0368849555a9d7af5e8ff7c416b0219614543208").notContains(
				// "Push Down Method public getPriority() : int from class
				// org.argouml.cognitive.Critic to public getPriority() : int
				// from class org.argouml.cognitive.CompoundCritic",
				// "Push Down Method public setName(s String) : void from class
				// org.argouml.ui.PerspectiveSupport to public setName(s String)
				// : void from class org.argouml.ui.TreeModelSupport",
				// "Push Down Method public setPriority(p int) : void from class
				// org.argouml.cognitive.Critic to public setPriority(p int) :
				// void from class org.argouml.cognitive.CompoundCritic")

				.atCommit("26b089d0c3f74abadcf8645f4f7618bdc0c3738f")
				.containsOnly(
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigRRect",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigText",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.uml.diagram.ui.SPFigEdgeModelElement",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionDataType",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionException",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionSignal",
						"Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramGeneralizationEdge",
						"Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramRealizationEdge",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddConstructor",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddInstanceVariable",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddOperation",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAssocComposite",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizBreakCircularComp",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizCueCards",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizManyNames",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizMEName",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizNavigable",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizTooMany",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ActionStateNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.AssociationEndNameNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.AssociationNameNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.AssociationRoleNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.AttributeNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.CallStateNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ClassifierRoleNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ComponentInstanceNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.EnumerationLiteralNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ExtensionPointNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.MessageNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ModelElementNameNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.MultiplicityNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.NodeInstanceNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ObjectFlowStateStateNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ObjectFlowStateTypeNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ObjectNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.OperationNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.StateBodyNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.TestNotationProviderFactory2.MyNP",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.TransitionNotation",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.TestNotationProvider.NPImpl",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByDecision",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByGoal",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByOffender",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByPoster",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByPriority",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByType",
						"Push Down Method public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizMEName to public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizOperName",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigClass",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigPackage",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.ui.StylePanelFigAssociationClass",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.ui.StylePanelFigMessage",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.use_case.ui.StylePanelFigUseCase")

				.atCommit("51b0fdc853d6e5188ce8625d3cc4770aad981011")
				.containsOnly(
						"Pull Up Attribute protected ATTRIBUTES_THRESHOLD : int from class org.argouml.uml.cognitive.critics.CrTooManyAttr to class org.argouml.uml.cognitive.critics.AbstractCrTooMany",
						"Pull Up Attribute protected configPanelNorth : JPanel from class org.argouml.ui.explorer.PerspectiveConfigurator to class org.argouml.util.ArgoDialog",
						"Pull Up Attribute protected dashed : boolean from class org.argouml.uml.diagram.state.ui.FigTransition to class org.argouml.uml.diagram.ui.FigEdgeModelElement",
						"Pull Up Attribute protected DEFAULT_X : int from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.ui.FigNodeModelElement",
						"Pull Up Attribute protected dep : Icon from class org.argouml.uml.diagram.deployment.ui.SelectionComponent to class org.argouml.uml.diagram.ui.SelectionNodeClarifiers2",
						"Pull Up Attribute protected dep : Icon from class org.argouml.uml.diagram.deployment.ui.SelectionComponentInstance to class org.argouml.uml.diagram.ui.SelectionNodeClarifiers2",
						"Pull Up Attribute protected instructions : String[] from class org.argouml.uml.diagram.static_structure.ui.SelectionClass to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
						"Pull Up Attribute protected instructions : String[] from class org.argouml.uml.diagram.static_structure.ui.SelectionEnumeration to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
						"Pull Up Attribute protected instructions : String[] from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
						"Pull Up Attribute protected instructions : String[] from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
						"Pull Up Attribute protected mMmeiTarget : Object from class org.argouml.uml.ui.TabConstraints to class org.argouml.application.api.AbstractArgoJPanel",
						"Pull Up Attribute protected modelImpl : MDRModelImplementation from class org.argouml.model.mdr.ExtensionMechanismsFactoryMDRImpl to class org.argouml.model.mdr.AbstractUmlModelFactoryMDR",
						"Pull Up Attribute protected namespace : Object from class org.argouml.model.TestUmlGeneralization to class org.argouml.model.GenericUmlObjectTestFixture",
						"Pull Up Attribute protected WIDTH : int from class org.argouml.uml.diagram.state.ui.FigSubmachineState to class org.argouml.uml.diagram.state.ui.FigState")

				.atCommit("8c2be520e01a83ee893158aa6cb90ee2d64f6a63").containsOnly(
						// FN** getMechList remains in CompositeCM, but it is
						// also added as abstract in ControlMech and implemented
						// in other subclasses. THIS IS NOT A FALSE NEGATIVE. DO
						// NOT UNCOMMENT
						// "Pull Up Method public getMechList() :
						// List<ControlMech> from class
						// org.argouml.cognitive.CompositeCM to public
						// getMechList() : List<ControlMech> from class
						// org.argouml.cognitive.ControlMech",
						"Pull Up Method public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.uml.AttributeNotationUml to public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.AttributeNotation",
						"Pull Up Method public transform(file File, version int) : File from class org.argouml.persistence.UmlFilePersister to public transform(file File, version int) : File from class org.argouml.persistence.AbstractFilePersister",
						"Pull Up Method private colToString(set Collection) : String from class org.argouml.profile.internal.ui.PropPanelCritic to protected colToString(set Collection) : String from class org.argouml.uml.ui.PropPanel",
						"Pull Up Method private getSomeProfileManager() : ProfileManager from class org.argouml.profile.UserDefinedProfile to protected getSomeProfileManager() : ProfileManager from class org.argouml.profile.Profile",
						"Pull Up Method protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrMultipleShallowHistoryStates to protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrUML",
						"Pull Up Method public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.collaboration.ui.CollabDiagramRenderer to public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.UmlDiagramRenderer",
						"Pull Up Method private initFigs() : void from class org.argouml.uml.diagram.state.ui.FigSynchState to protected initFigs() : void from class org.argouml.uml.diagram.state.ui.FigStateVertex",
						// FN** minor modifications, but no statement is
						// exactly the same
						// "Extract Method public getModelImpl() :
						// MDRModelImplementation extracted from public
						// createPartition() : Partition in class
						// org.argouml.model.mdr.ActivityGraphsFactoryMDRImpl",
						// "Extract Method public initialize2(myPartition
						// Partition) : void extracted from public
						// createPartition() : Partition in class
						// org.argouml.model.mdr.ActivityGraphsFactoryMDRImpl",
						"Pull Up Method public createPartition() : Partition from class org.argouml.model.mdr.ActivityGraphsFactoryMDRImpl to public createPartition() : Partition from class org.argouml.model.mdr.AbstractUmlModelFactoryMDR",
						"Pull Up Method public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManagerImpl.ChangeabilityGetterSetter to public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManager.OptionGetterSetter");
	}

	private static void jUnitRefactorings(TestBuilder test, int flag) {

		test.project("https://github.com/MatinMan/RefactoringDatasets.git", "junit")
				.atCommit("00e584db35fdb44b58eccaff7dc5ec6b0da7547a").containsOnly(
						"Extract Method	private addMultipleFailureException(mfe MultipleFailureException) : void extracted from public addFailure(targetException Throwable) : void in class org.junit.internal.runners.model.EachTestNotifier",
						"Extract Method	private runNotIgnored(method FrameworkMethod, eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method	private runIgnored(eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method private addTestsFromTestCase(theClass Class<?>) : void extracted from public TestSuite(theClass Class<? extends TestCase>) in class junit.framework.TestSuite",
						"Rename Method	public testsThatAreBothIncludedAndExcludedAreIncluded() : void renamed to public testsThatAreBothIncludedAndExcludedAreExcluded() : void in class org.junit.tests.experimental.categories.CategoryTest",
						"Rename Method	public saffSqueezeExample() : void renamed to public filterSingleMethodFromOldTestClass() : void in class org.junit.tests.experimental.max.MaxStarterTest",
						"Inline Method	private ruleFields() : List<FrameworkField> inlined to private validateFields(errors List<Throwable>) : void in class org.junit.runners.BlockJUnit4ClassRunner");

	}

	private static void antlrRefactorings(TestBuilder test, int flag) {
		test.project("https://github.com/MatinMan/RefactoringDatasets.git", "ANTLR4")
				.atCommit("5587eebd3cb141b6ab10f394bf5ddcd47d86477c")
				.containsOnly(
						// its a new one should be checked....
						// "Extract Method protected addDFAEdge(dfa DFA, from
						// DFAState, t int, to DFAState) : DFAState extracted
						// from public execDFA(dfa DFA, s0 DFAState, input
						// TokenStream, startIndex int, outerContext
						// ParserRuleContext) : int in class
						// org.antlr.v4.runtime.atn.ParserATNSimulator",
						"Extract Method protected computeTargetState(dfa DFA, previousD DFAState, t int) : DFAState extracted from public execATN(dfa DFA, s0 DFAState, input TokenStream, startIndex int, outerContext ParserRuleContext) : int in class org.antlr.v4.runtime.atn.ParserATNSimulator",
						"Extract Method protected getExistingTargetState(previousD DFA, previousD DFAState, t int) : DFAState extracted from public execATN(dfa DFA, s0 DFAState, input TokenStream, startIndex int, outerContext ParserRuleContext) : int in class org.antlr.v4.runtime.atn.ParserATNSimulator",
						"Extract Method protected computeTargetState(input CharStream, s DFAState, t int) : DFAState extracted from protected execATN(input CharStream, ds0 DFAState) : int in class org.antlr.v4.runtime.atn.LexerATNSimulator",
						"Extract Method protected getExistingTargetState(s DFAState, t int) : DFAState extracted from protected execATN(input CharStream, ds0 DFAState) : int in class org.antlr.v4.runtime.atn.LexerATNSimulator",
						"Extract Method public addTransition(index int, e Transition) : void extracted from public addTransition(e Transition) : void in class org.antlr.v4.runtime.atn.ATNState",
						"Extract Method public LOOK(s ATNState, stopState ATNState, ctx RuleContext) : IntervalSet extracted from public LOOK(s ATNState, ctx RuleContext) : IntervalSet in class org.antlr.v4.runtime.atn.LL1Analyzer",
						"Extract Method public toString(rendered boolean) : String extracted from public toString() : String in class org.antlr.v4.test.ErrorQueue",
						"Inline Method public predictATN(dfa DFA, input TokenStream, outerContext ParserRuleContext) : int inlined to public adaptivePredict(input TokenStream, decision int, outerContext ParserRuleContext) : int in class org.antlr.v4.runtime.atn.ParserATNSimulator",
						"Move Attribute private templates : STGroup from class org.antlr.v4.codegen.CodeGenerator to class org.antlr.v4.codegen.Target")
				.atCommit("5587eebd3cb141b6ab10f394bf5ddcd47d86477c").notContains(
						"Extract Method protected execATNWithFullContext(dfa DFA, D DFAState, s0 ATNConfigSet, input TokenStream, startIndex int, outerContext ParserRuleContext) : int extracted from public execDFA(dfa DFA, s0 DFAState, input TokenStream, startIndex int, outerContext ParserRuleContext) : int in class org.antlr.v4.runtime.atn.ParserATNSimulator",
						"Extract Method protected reportUnwantedToken(recognizer Parser) : void extracted from public reportUnwantedToken(recognizer Parser) : void in class org.antlr.v4.runtime.DefaultErrorStrategy",
						"Extract Method public reportMatch(recognizer Parser) : void extracted from public reportUnwantedToken(recognizer Parser) : void in class org.antlr.v4.runtime.DefaultErrorStrategy");
	}

	private static void aTunesRefactorings(TestBuilder test, int flag) {

		if ((Refactorings.MoveMethod.getValue() & flag) > 0) {

			test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("d2bcdb51d88f25a35e37342389ba09bcc52ddba9").containsOnly(
							// FN** Not really a move method. There is still a
							// isSongFavorite method in FavoritesSongsManager,
							// but it only delegates to
							// FavoritesObjectDataStore#isSongFavorite
							"Extract And Move Method public isSongFavorite(favorites IFavorites, artist String, title String) : boolean extracted from public isSongFavorite(favorites IFavorites, artist String, title String) : boolean in class net.sourceforge.atunes.kernel.modules.favorites.FavoritesSongsManager & moved to class net.sourceforge.atunes.kernel.modules.favorites.FavoritesObjectDataStore",
							"Move Method private isFadeAwayInProgress() : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerEngine to package isFadeAwayInProgress(mPlayerEngine MPlayerEngine) : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerCommandWriter",
							"Move Method package addToHistory(audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory to package addToHistory(playbackHistory PlaybackHistory, audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory.Heap",
							"Move Method public readObjectFromFile(inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XMLSerializerService to public readObjectFromFile(xmlSerializerService XMLSerializerService, inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XStreamFactory",
							"Move Method public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.CryptoUtils to public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.DateUtils");

		}

		if ((Refactorings.MoveAttribute.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("fec9b7bbc31a04c6f15137679faeb7fcfd6cd97d");

			getaTunesMoveAttribute(matcher);

		}

		if ((Refactorings.PushDownAttribute.getValue() & flag) > 0) {

			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("ca59aa0f0ad21a3b4a31b8f172dc941c59207c63");

			getaTunerPushDownAttribute(matcher);

		}

		if ((Refactorings.InlineMethod.getValue() & flag) > 0) {

			test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("667a45ad8bcc94420d5c6d66b15b758c7eab1c1d").containsOnly(
							"Inline Method private finish(restart boolean) : void inlined to package finish() : void in class net.sourceforge.atunes.kernel.Finisher",
							"Inline Method private finish(restart boolean) : void inlined to package restart() : void in class net.sourceforge.atunes.kernel.Finisher",
							"Inline Method private formatTrackNumber(trackNumber int) : String inlined to public getAudioFileStringValue(audioFile ILocalAudioObject) : String in class net.sourceforge.atunes.kernel.modules.pattern.TrackPattern",
							"Inline Method private formatTrackNumber(trackNumber int) : String inlined to public getCDMetadataStringValue(metadata CDMetadata, trackNumber int) : String in class net.sourceforge.atunes.kernel.modules.pattern.TrackPattern",
							"Inline Method private getArtistSongs(listOfObjectsDragged List) : boolean inlined to public processInternalImport(support TransferSupport) : boolean in class net.sourceforge.atunes.kernel.modules.draganddrop.InternalImportProcessor",
							"Inline Method private persistRadios() : void inlined to public addRadio(radio IRadio) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler",
							"Inline Method private persistRadios() : void inlined to public removeRadios(radios List) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler",
							"Inline Method private persistRadios() : void inlined to public setLabel(radioList List, label String) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler",
							"Inline Method private addHeaderRenderers(jtable JTable, model AbstractCommonColumnModel, lookAndFeel ILookAndFeel) : void inlined to public decorate(decorateHeader boolean) : void in class net.sourceforge.atunes.gui.ColumnDecorator");

		}

		if ((Refactorings.ExtractMethod.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("8dbd39562602c1112e7f73b2a1825c78a70ac5c7");
			getaTunesExtractMethod(matcher);
			if ((Refactorings.MoveAttribute.getValue() & flag) == 0) {
				getaTunesMoveAttribute(matcher);
			}
		}

		if ((Refactorings.PushDownMethod.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("8ecd468c8cb6ff85be55249a82497f7c3dcc46e1");

			getaTunesPushDownMethod(matcher);
			if ((Refactorings.PushDownAttribute.getValue() & flag) == 0) {
				getaTunerPushDownAttribute(matcher);
			}
		}

		if ((Refactorings.PullUpMethod.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("566cbfcc517e5d7e7372b893bb5ea779b7397ffd");

			getaTunesPullUpMethod(matcher);

		}

		if ((Refactorings.PullUpAttribute.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("f061c05f845933f3e9ca8dba2b37c24b2ff164f2");

			getaTunesPullUpAttribute(matcher);
		}
	}

	private static void getaTunesPullUpAttribute(CommitMatcher matcher) {
		matcher.containsOnly(
				"Pull Up Attribute protected desktop : IDesktop from class net.sourceforge.atunes.gui.views.dialogs.MakeDonationDialog to class net.sourceforge.atunes.gui.views.controls.AbstractCustomDialog",
				"Pull Up Attribute protected albumColumnSet : IColumnSet from class net.sourceforge.atunes.gui.AlbumTableColumnModel to class net.sourceforge.atunes.gui.AbstractCommonColumnModel",
				"Pull Up Attribute protected eventIcon : IIconFactory from class net.sourceforge.atunes.kernel.modules.context.event.EventsContextPanel to class net.sourceforge.atunes.kernel.modules.context.AbstractContextPanel",
				"Pull Up Attribute protected rightSplitPane : JSplitPane from class net.sourceforge.atunes.gui.frame.CommonSingleFrame to class net.sourceforge.atunes.gui.frame.AbstractSingleFrame",
				"Pull Up Attribute protected navigatorSplitPane : JSplitPane from class net.sourceforge.atunes.gui.frame.DefaultSingleFrame to class net.sourceforge.atunes.gui.frame.MainSplitPaneLeftSingleFrame");

	}

	private static void getaTunesPullUpMethod(CommitMatcher matcher) {
		matcher.containsOnly(
				"Pull Up Method public getButtonOutline(button AbstractButton, insets Insets, w int, h int, isInner boolean) : Shape from class net.sourceforge.atunes.gui.lookandfeel.substance.RoundRectButtonShaper to public getButtonOutline(button AbstractButton, insets Insets, w int, h int, isInner boolean) : Shape from class net.sourceforge.atunes.gui.lookandfeel.substance.AbstractButtonShaper",
				"Pull Up Method private applyVerticalSplitPaneDividerPosition(splitPane JSplitPane, location int, relPos double) : void from class net.sourceforge.atunes.gui.frame.CommonSingleFrame to protected applyVerticalSplitPaneDividerPosition(splitPane JSplitPane, location int, relPos double) : void from class net.sourceforge.atunes.gui.frame.AbstractSingleFrame",
				"Pull Up Method protected getNavigationTablePanelMaximumSize() : Dimension from class net.sourceforge.atunes.gui.frame.NavigatorTopPlayListBottomSingleFrame to protected getNavigationTablePanelMaximumSize() : Dimension from class net.sourceforge.atunes.gui.frame.MainSplitPaneRightSingleFrame",
				// FN** This method has only one line and a field access was
				// replaced by a method invocation (getter)
				"Pull Up Method public initialize() : void from class net.sourceforge.atunes.gui.AlbumTableColumnModel to public initialize() : void from class net.sourceforge.atunes.gui.AbstractCommonColumnModel",
				"Pull Up Method public setLine2(text String) : void from class net.sourceforge.atunes.gui.views.dialogs.ExtendedToolTip to public setLine2(text String) : void from class net.sourceforge.atunes.gui.views.controls.AbstractCustomWindow");
	}

	private static void getaTunesExtractMethod(CommitMatcher matcher) {
		matcher.containsOnly(
				// correct class is NeroAacEncoder, not FlacEncoder
				// "Extract Method extractedMethod in class
				// net.sourceforge.atunes.kernel.modules.cdripper.FlacEncoder",
				"Extract Method private extractedMethod(wavFile File, mp4File File) : List<String> extracted from public encode(wavFile File, mp4File File) : boolean in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder",
				"Extract Method private extractedMethod(nonPatternSequencesArray String[], nonPatternSequences List<String>) : void extracted from private getNonPatternSequences(patternsString String) : List<String> in class net.sourceforge.atunes.kernel.modules.pattern.PatternMatcher",
				"Extract Method private extractedMethod(audioObject IAudioObject, location Point, i ImageIcon) : void extracted from package showOSD(audioObject IAudioObject) : void in class net.sourceforge.atunes.kernel.modules.notify.OSDDialogController",
				"Extract Method private extractedMethod() : void extracted from public initialize() : void in class net.sourceforge.atunes.kernel.modules.navigator.FavoritesNavigationViewTablePopupMenu",
				"Extract Method private extractedMethod(file File) : void extracted from protected executeAction() : void in class net.sourceforge.atunes.kernel.actions.LoadPlayListAction");

	}

	private static void getaTunesMoveAttribute(CommitMatcher matcher) {
		matcher.containsOnly(
				"Move Attribute private imageSize : int from class net.sourceforge.atunes.kernel.modules.fullscreen.Cover to class net.sourceforge.atunes.kernel.modules.fullscreen.CoverFlow",
				"Move Attribute private versionProperty : String from class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler to class net.sourceforge.atunes.kernel.modules.updates.VersionXmlParser",
				"Move Attribute private process : Process from class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
				"Move Attribute private albumFavoriteSearchOperator : ISearchUnaryOperator<IAlbum> from class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField to class net.sourceforge.atunes.kernel.modules.search.AlbumArtistSearchField",
				"Move Attribute private old : boolean from class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedEntry to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeed",
				// "Extract Method private getAlbumFavoriteSearchOperator() :
				// ISearchUnaryOperator extracted from * in class
				// net.sourceforge.atunes.kernel.modules.search.AlbumSearchField",
				// "Extract Method private getAlbumFavoriteSearchOperator() :
				// ISearchUnaryOperator extracted from public getOperators() :
				// List in class
				// net.sourceforge.atunes.kernel.modules.search.AlbumSearchField",
				"Extract And Move Method public getProcess() : Process extracted from public encode(wavFile File, mp4File File) : boolean in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder & moved to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
				"Extract And Move Method public setProcess(process Process) : void extracted from public encode(wavFile File, mp4File File) : boolean in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder & moved to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
				"Extract And Move Method public getProcess() : Process extracted from public stop() : void in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder & moved to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
				"Extract And Move Method public getVersionProperty() : String extracted from private checkIfVersionChanged() : void in class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler & moved to class net.sourceforge.atunes.kernel.modules.updates.VersionXmlParser",
				"Extract And Move Method public getAlbumFavoriteSearchOperator() : ISearchUnaryOperator extracted from public getOperators() : List in class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField & moved to class net.sourceforge.atunes.kernel.modules.search.AlbumArtistSearchField",
				"Extract And Move Method public setImageSize(imageSize int) : void extracted from public Cover(imageSize int) in class net.sourceforge.atunes.kernel.modules.fullscreen.Cover & moved to class net.sourceforge.atunes.kernel.modules.fullscreen.CoverFlow",
				// TBD
				"Extract And Move Method public getImageSize() : int extracted from public getImageSize() : int in class net.sourceforge.atunes.kernel.modules.fullscreen.Cover & moved to class net.sourceforge.atunes.kernel.modules.fullscreen.CoverFlow",
				"Extract And Move Method public setAlbumFavoriteSearchOperator(albumFavoriteSearchOperator ISearchUnaryOperator) : void extracted from public setAlbumFavoriteSearchOperator(albumFavoriteSearchOperator ISearchUnaryOperator) : void in class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField & moved to class net.sourceforge.atunes.kernel.modules.search.AlbumArtistSearchField",
				"Extract And Move Method public isOld() : boolean extracted from public isOld() : boolean in class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedEntry & moved to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeed",
				"Extract And Move Method public setOld(old boolean) : void extracted from public setOld(old boolean) : void in class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedEntry & moved to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeed",
				"Extract And Move Method public setVersionProperty(versionProperty String) : void extracted from public setVersionProperty(versionProperty String) : void in class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler & moved to class net.sourceforge.atunes.kernel.modules.updates.VersionXmlParser"

		);
		// .atCommit("fec9b7bbc31a04c6f15137679faeb7fcfd6cd97d")
		// // FP** one-liner, more like encapsulate field
		// .notContains(
		// "Extract Method private getAlbumFavoriteSearchOperator() :
		// ISearchUnaryOperator extracted from * in class
		// net.sourceforge.atunes.kernel.modules.search.AlbumSearchField");

	}

	private static void getaTunesPushDownMethod(CommitMatcher matcher) {
		matcher.contains(
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
				"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricsDirectoryEngine",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.kernel.modules.navigator.TooltipTreeCellDecorator",
				"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.kernel.modules.search.SearchResultTableModel",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.FolderTreeCellDecorator",
				"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.WinampcnEngine",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.AlbumTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.StringTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.ArtistTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.IncompleteTagsTreeCellDecorator",
				"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricWikiEngine",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.NavigationTreeRootTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.YearTreeCellDecorator",
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.state.ValidateAndProcessPreferencesBackgroundWorker",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.GenreTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.PodcastFeedTreeCellDecorator",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.UnknownElementTreeCellDecorator",
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.DeleteFilesTask",
				"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrDBEngine",
				"Push Down Method public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.ProgressDialog to public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.TransferProgressDialog",
				"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.RadioTreeCellDecorator",
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.radio.RetrieveRadioBrowserDataBackgroundWorker",
				"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.CalculateSynchronizationBetweenDeviceAndPlayListBackgroundWorker",
				"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrcEngine",
				"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.gui.NavigationTableModel",
				"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlayListTableModel",
				"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AlbumTableModel");
	}

	private static void getaTunerPushDownAttribute(CommitMatcher matcher) {
		matcher.containsOnly(

				"Push Down Attribute private columnSet : IColumnSet from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to class net.sourceforge.atunes.gui.AlbumTableModel",
				"Push Down Attribute private columnSet : IColumnSet from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to class net.sourceforge.atunes.gui.NavigationTableModel",
				"Push Down Attribute private columnSet : IColumnSet from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to class net.sourceforge.atunes.kernel.modules.search.SearchResultTableModel",
				"Push Down Attribute private columnSet : IColumnSet from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to class net.sourceforge.atunes.kernel.modules.playlist.PlayListTableModel",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.radio.RetrieveRadioBrowserDataBackgroundWorker",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.repository.CalculateSynchronizationBetweenDeviceAndPlayListBackgroundWorker",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.repository.DeleteFilesTask",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.state.ValidateAndProcessPreferencesBackgroundWorker",
				"Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.command.CommandHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.context.ContextHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.favorites.FavoritesHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.filter.FilterHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.fullscreen.FullScreenHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.hotkeys.HotkeyHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.instances.MultipleInstancesHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.navigator.NavigationHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.network.NetworkHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.notify.NotificationsHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.player.AdvancedPlayingModeHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.player.PlayerHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.playlist.PlayListHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.playlist.SmartPlayListHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.radio.RadioHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.repository.DeviceHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.repository.RepositoryHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.search.SearchHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.statistics.StatisticsHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.tags.TagHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.tray.SystemTrayHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.ui.UIHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.webservices.WebServicesHandler",
				"Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.cdripper.RipperHandler",
				"Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrcEngine",
				"Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrDBEngine",
				"Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricWikiEngine",
				"Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.WinampcnEngine",
				"Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricsDirectoryEngine",
				"Push Down Attribute private cancelButton : JButton from class net.sourceforge.atunes.gui.views.dialogs.ProgressDialog to class net.sourceforge.atunes.gui.views.dialogs.TransferProgressDialog",
				"Push Down Method public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.AlbumTableModel",
				// The rest are related to some push down method which they
				// signature got abstracted in the base class and the body moved
				// to the child. So other cases should be added before this
				// line.
				"Push Down Method public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrcEngine",
				"Push Down Method public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrDBEngine",
				"Push Down Method public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricsDirectoryEngine",
				"Push Down Method public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.WinampcnEngine",
				"Push Down Method public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to public setNetworkHandler(networkHandler INetworkHandler) : void from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricWikiEngine",
				"Push Down Method public setFrame(frame IFrame) : void from class net.sourceforge.atunes.kernel.AbstractHandler to public setFrame(frame IFrame) : void from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesHandler",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.radio.RetrieveRadioBrowserDataBackgroundWorker",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.repository.CalculateSynchronizationBetweenDeviceAndPlayListBackgroundWorker",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.repository.DeleteFilesTask",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
				"Push Down Method public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public setDialogFactory(dialogFactory IDialogFactory) : void from class net.sourceforge.atunes.kernel.modules.state.ValidateAndProcessPreferencesBackgroundWorker",
				"Push Down Method public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.NavigationTableModel",
				"Push Down Method public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlayListTableModel",
				"Push Down Method public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public setColumnSet(columnSet IColumnSet) : void from class net.sourceforge.atunes.kernel.modules.search.SearchResultTableModel",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.radio.RetrieveRadioBrowserDataBackgroundWorker",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.repository.CalculateSynchronizationBetweenDeviceAndPlayListBackgroundWorker",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.repository.DeleteFilesTask",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
				"Push Down Method protected getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to public getDialogFactory() : IDialogFactory from class net.sourceforge.atunes.kernel.modules.state.ValidateAndProcessPreferencesBackgroundWorker",
				"Push Down Method protected getFrame() : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to protected getFrame() : IFrame from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesHandler"

		);
	}

	private static void FSE_ExtractMethodRefactorings(TestBuilder test, int flag) {
		test.project("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "master")
				.atCommit("e235f884f2e0bc258da77b9c80492ad33386fa86").containsOnly(
						"Extract Method private createCellSignalTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createOpenCellIDTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createDefaultMCCTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createLocationTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createSilentSmsTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createCellTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
						"Extract Method private createEventLogTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper");

		test.project("https://github.com/crashub/crash.git", "master")
				.atCommit("2801269c7e47bd6e243612654a74cee809d20959").containsOnly(
						"Extract Method private convertPemKeyPair(pemKeyPair PEMKeyPair) : KeyPair extracted from public loadKeys() : Iterable<KeyPair> in class org.crsh.auth.FilePublicKeyProvider");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("7655200f58293e5a30bf8b3cbb29ebadae374564").containsOnly(
						"Extract Method private checkRemap() : void extracted from public getLine() : int in class com.intellij.debugger.engine.RemappedSourcePosition",
						"Extract Method private checkRemap() : void extracted from public getOffset() : int in class com.intellij.debugger.engine.RemappedSourcePosition");

		test.project("https://github.com/oblac/jodd.git", "master").atCommit("722ef9156896248ef3fbe83adde0f6ff8f46856a")
				.containsOnly(
						"Extract Method protected resolveFormEncoding() : String extracted from protected formBuffer() : Buffer in class jodd.http.HttpBase");

		test.project("https://github.com/realm/realm-java.git", "master")
				.atCommit("6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb").containsOnly(
						"Extract Method private showStatus(txt String) : void extracted from private showStatus(realm Realm) : void in class io.realm.examples.realmmigrationexample.MigrationExampleActivity");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("7a4dab88185553bd09e827839fdf52e870ef7088").containsOnly(
						"Extract Method private getDataFile(name String) : File extracted from private getJarFile(name String) : VirtualFile in class com.intellij.codeInsight.JavaExternalDocumentationTest",
						"Extract Method private getDocumentationText(sourceEditorText String) : String extracted from public testImagesInsideJavadocJar() : void in class com.intellij.codeInsight.JavaExternalDocumentationTest");

		test.project("https://github.com/fabric8io/fabric8.git", "master")
				.atCommit("8127b21a220ca677c4e59961d019e7753da7ea6e").containsOnly(
						"Extract Method protected getProbe(prefix String) : Probe extracted from protected getLivenessProbe() : Probe in class io.fabric8.maven.JsonMojo");

		test.project("https://github.com/puniverse/quasar.git", "master")
				.atCommit("c22d40fab8dfe4c5cad9ba582caf0855ff64b324").containsOnly(
						"Extract Method protected failedSubscribe(s Subscription) : void extracted from public onSubscribe(s Subscription) : void in class co.paralleluniverse.strands.channels.reactivestreams.ChannelSubscriber",
						"Extract Method protected failedSubscribe(s Subscriber, t Throwable) : void extracted from public subscribe(s Subscriber) : void in class co.paralleluniverse.strands.channels.reactivestreams.ChannelPublisher");

		test.project("https://github.com/rstudio/rstudio.git", "master")
				.atCommit("cb49e436b9d7ee55f2531ebc2ef1863f5c9ba9fe").containsOnly(
						"Extract Method protected setMaxHeight(maxHeight int) : void extracted from protected wrapMenuBar(menuBar ToolbarMenuBar) : Widget in class org.rstudio.core.client.widget.ScrollableToolbarPopupMenu");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("7ed3f273ab0caf0337c22f0b721d51829bb0c877").containsOnly(
						"Extract Method private addCoursesFromStepic(result List<CourseInfo>, pageNumber int) : boolean extracted from public getCourses() : List<CourseInfo> in class com.jetbrains.edu.stepic.EduStepicConnector");

		test.project("https://github.com/Athou/commafeed.git", "master")
				.atCommit("18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7").containsOnly(
						"Extract Method private fetch(url String) : byte[] extracted from public fetch(feed Feed) : byte[] in class com.commafeed.backend.favicon.DefaultFaviconFetcher");

		test.project("https://github.com/datastax/java-driver.git", "master")
				.atCommit("1edac0e92080e7c5e971b2d56c8753bf44ea8a6c").containsOnly(
						"Extract Method public setMaxRequestsPerConnection(distance HostDistance, newMaxRequests int) : PoolingOptions extracted from public setMaxSimultaneousRequestsPerHostThreshold(distance HostDistance, newMaxRequests int) : PoolingOptions in class com.datastax.driver.core.PoolingOptions",
						"Extract Method public getMaxRequestsPerConnection(distance HostDistance) : int extracted from public getMaxSimultaneousRequestsPerHostThreshold(distance HostDistance) : int in class com.datastax.driver.core.PoolingOptions",
						"Extract Method public getNewConnectionThreshold(distance HostDistance) : int extracted from public getMaxSimultaneousRequestsPerConnectionThreshold(distance HostDistance) : int in class com.datastax.driver.core.PoolingOptions",
						"Extract Method public setNewConnectionThreshold(distance HostDistance, newValue int) : PoolingOptions extracted from public setMaxSimultaneousRequestsPerConnectionThreshold(distance HostDistance, newMaxSimultaneousRequests int) : PoolingOptions in class com.datastax.driver.core.PoolingOptions");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("cc0eaf7faa408a04b68e2b5820f3ebcc75420b5b").containsOnly(
						"Extract Method private canBinaryExpressionBeUnboxed(lhs PsiExpression, rhs PsiExpression) : boolean extracted from private canBeUnboxed(expression PsiExpression) : boolean in class com.siyeh.ig.migration.UnnecessaryBoxingInspection.UnnecessaryBoxingVisitor");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("138911ce88b05039242b8d1b2bb5b7a59008f5ee").containsOnly(
						"Extract Method public getHTMLEditorKit(noGapsBetweenParagraphs boolean) : HTMLEditorKit extracted from public getHTMLEditorKit() : HTMLEditorKit in class com.intellij.util.ui.UIUtil");

		test.project("https://github.com/hibernate/hibernate-orm.git", "master")
				.atCommit("2b89553db5081fe4e55b7b34d636d0ea2acf71c5").containsOnly(
						"Extract Method private categorizeAnnotatedClass(annotatedClass Class, attributeConverterManager AttributeConverterManager) : void extracted from public AnnotationMetadataSourceProcessorImpl(sources MetadataSources, rootMetadataBuildingContext MetadataBuildingContextRootImpl, jandexView IndexView) in class org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl");

		test.project("https://github.com/elastic/elasticsearch.git", "master")
				.atCommit("ff9041dc486adf0a8dec41f80bbfbdd49f97016a").containsOnly(
						"Extract Method protected buildFQuery(builder XContentBuilder, params Params) : void extracted from protected doXContent(builder XContentBuilder, params Params) : void in class org.elasticsearch.index.query.QueryFilterBuilder");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("484038e916dc40bf87eca10c77889d79eca96c4d").containsOnly(
						"Extract Method public removeNodes(paths Collection<TreePath>) : void extracted from public removeNode(nodePath TreePath) : void in class com.intellij.compiler.options.AnnotationProcessorsPanel.MyTreeModel");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("219d6ddfd1db62c11efb57e0216436874e087834").containsOnly(
						"Extract Method private addAdditionalLoggingHandler(loggingHandler LoggingHandlerBase) : void extracted from public addAdditionalLog(presentableName String) : LoggingHandler in class com.intellij.remoteServer.impl.runtime.log.DeploymentLogManagerImpl");

		test.project("https://github.com/liferay/liferay-portal.git", "master")
				.atCommit("59fd9e696cec5f2ed44c27422bbc426b11647321").containsOnly(
						"Extract Method public addDependency(project Project, configurationName String, group String, name String, version String, classifier String, transitive boolean) : Dependency extracted from public addDependency(project Project, configurationName String, group String, name String, version String, transitive boolean) : Dependency in class com.liferay.gradle.util.GradleUtil");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("96a2c3410f3c71d3ab20857036422f1d64c3a6d3").containsOnly(
						"Extract Method private cleanupProximityLocked() : void extracted from private cleanupProximity() : void in class com.android.server.power.PowerManagerService");

		test.project("https://github.com/checkstyle/checkstyle.git", "master")
				.atCommit("5a9b7249e3d092a78ac8e7d48aeeb62bf1c44e20").containsOnly(
						"Extract Method private processField(ast DetailAST, parentType int) : void extracted from private processIDENT(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.RequireThisCheck");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("a97341973c3b683d62d1422e5404ed5c7ccf45f8").containsOnly(
						"Extract Method private setNewName(newText String) : PsiElement extracted from public handleElementRename(newElementName String) : PsiElement in class org.jetbrains.plugins.javaFX.fxml.refs.FxmlReferencesContributor.MyJavaClassReferenceProvider.JavaClassReferenceWrapper",
						"Extract Method private setNewName(newText String) : PsiElement extracted from public bindToElement(element PsiElement) : PsiElement in class org.jetbrains.plugins.javaFX.fxml.refs.FxmlReferencesContributor.MyJavaClassReferenceProvider.JavaClassReferenceWrapper");

		test.project("https://github.com/datastax/java-driver.git", "master")
				.atCommit("3a0603f8f778be3219a5a0f3a7845cda65f1e172").containsOnly(
						"Extract Method public values(names List<String>, values List<Object>) : Insert extracted from public values(names String[], values Object[]) : Insert in class com.datastax.driver.core.querybuilder.Insert");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("d71154ed21e2d5c65bb0ddb000bcb04ca5735048").containsOnly(
						"Extract Method public canonicalizePath(url String, baseUrl Url, baseUrlIsFile boolean) : String extracted from protected canonicalizeUrl(url String, baseUrl Url, trimFileScheme boolean, sourceIndex int, baseUrlIsFile boolean) : Url in class org.jetbrains.debugger.sourcemap.SourceResolver");

		test.project("https://github.com/undertow-io/undertow.git", "master")
				.atCommit("d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4").containsOnly(
						"Extract Method public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper, elseBranch HandlerWrapper) : PredicatesHandler extracted from public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper) : PredicatesHandler in class io.undertow.predicate.PredicatesHandler");
		// TBD
		// Move Method
		// Extract Method public tokenize(string String) : Deque extracted from
		// public parse(contents String, classLoader ClassLoader) : List in
		// class io.undertow.server.handlers.builder.PredicatedHandlersParser
		// https://github.com/undertow-io/undertow/commit/d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4
		test.project("https://github.com/Netflix/eureka.git", "master")
				.atCommit("f6212a7e474f812f31ddbce6d4f7a7a0d498b751").containsOnly(
						"Extract Method protected onRemoteStatusChanged(oldStatus InstanceStatus, newStatus InstanceStatus) : void extracted from private updateInstanceRemoteStatus() : void in class com.netflix.discovery.DiscoveryClient");

		test.project("https://github.com/orientechnologies/orientdb.git", "master")
				.atCommit("b40adc25008b6f608ee3eb3422c8884fff987337").containsOnly(
						"Extract Method protected readSynchResult(network OChannelBinaryAsynchClient, database ODatabaseDocument) : Object extracted from public command(iCommand OCommandRequestText) : Object in class com.orientechnologies.orient.client.remote.OStorageRemote",
						"Extract Method public serializeValue(listener OAbstractCommandResultListener, result Object) : void extracted from private indexGet() : void in class com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary",
						"Extract Method public serializeValue(listener OAbstractCommandResultListener, result Object) : void extracted from protected command() : void in class com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary");

		test.project("https://github.com/droolsjbpm/drools.git", "master")
				.atCommit("1bf2875e9d73e2d1cd3b58200d5300485f890ff5").containsOnly(
						"Extract Method protected initPriorityQueue(kBase InternalKnowledgeBase) : BinaryHeapQueue extracted from public AgendaGroupQueueImpl(name String, kBase InternalKnowledgeBase) in class org.drools.core.common.AgendaGroupQueueImpl",
						"Extract Method private internalAddEntry(entry PropagationEntry) : void extracted from public addEntry(entry PropagationEntry) : void in class org.drools.core.phreak.SynchronizedPropagationList");
		// TBD
		// Extract Method private fire(wm InternalWorkingMemory, filter
		// AgendaFilter, fireCount int, fireLimit int, agenda InternalAgenda) :
		// int extracted from public evaluateNetworkAndFire(wm
		// InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit
		// int) : int in class org.drools.core.phreak.RuleExecutor
		// https://github.com/droolsjbpm/drools/commit/1bf2875e9d73e2d1cd3b58200d5300485f890ff5#diff-48ebc0346526122dd9a4568d42907628L55

		test.project("https://github.com/spring-projects/spring-data-rest.git", "master")
				.atCommit("b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3").containsOnly(
						"Extract Method protected setupMockMvc() : void extracted from public setUp() : void in class org.springframework.data.rest.webmvc.AbstractWebIntegrationTests");

		test.project("https://github.com/datastax/java-driver.git", "master")
				.atCommit("d5134b15fe6545ec8ab5c2256006cd6fe19eac92").containsOnly(
						"Extract Method package getPreparedQuery(type QueryType, columns Set<ColumnMapper<?>>, options Option[]) : PreparedStatement extracted from package getPreparedQuery(type QueryType, options Option[]) : PreparedStatement in class com.datastax.driver.mapping.Mapper");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("669e0722324965e3c99f29685517ac24d4ff2848").containsOnly(
						"Extract Method public getClient(timeout long, scheme ClientAuthHashScheme, useAdmin boolean) : Client extracted from public getClient(timeout long, scheme ClientAuthHashScheme) : Client in class org.voltdb.regressionsuites.RegressionSuite",
						"Extract Method private runPausedMode(isAdmin boolean) : void extracted from public testPausedMode() : void in class org.voltdb.TestClientInterface",
						"Extract Method public makeStoredProcAdHocPlannerWork(replySiteId long, sql String, userParams Object[], singlePartition boolean, context CatalogContext, completionHandler AsyncCompilerWorkCompletionHandler, isAdmin boolean) : AdHocPlannerWork extracted from public makeStoredProcAdHocPlannerWork(replySiteId long, sql String, userParams Object[], singlePartition boolean, context CatalogContext, completionHandler AsyncCompilerWorkCompletionHandler) : AdHocPlannerWork in class org.voltdb.compiler.AdHocPlannerWork",
						"Extract Method public mockStatementBatch(replySiteId long, sql String, extractedValues Object[], paramTypes VoltType[], userParams Object[], partitionParamIndex int, catalogHash byte[], readOnly boolean, isAdmin boolean) : AdHocPlannedStmtBatch extracted from public mockStatementBatch(replySiteId long, sql String, extractedValues Object[], paramTypes VoltType[], userParams Object[], partitionParamIndex int, catalogHash byte[]) : AdHocPlannedStmtBatch in class org.voltdb.compiler.AdHocPlannedStmtBatch",
						"Extract Method private getListenerAddress(hostId int, useAdmin boolean) : String extracted from public getListenerAddress(hostId int) : String in class org.voltdb.regressionsuites.LocalCluster");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("e9efc045fbc6fa893c66a03b72b7eedb388cf96c").containsOnly(
						"Extract Method public setMpUniqueIdListener(listener DurableMpUniqueIdListener) : void extracted from public setMpDRGateway(mpGateway PartitionDRGateway) : void in class org.voltdb.iv2.SpScheduler");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("658a918eebcbdeb4f920c2947ca8d0e79ad86d89").containsOnly(
						"Extract Method private initTickerView() : void extracted from protected makeStatusBarView() : PhoneStatusBarView in class com.android.systemui.statusbar.phone.PhoneStatusBar");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("20d39f7af2165c67d5221f556f58820c992d2cc6").containsOnly(
						"Extract Method private getFullKey(path String, key String) : String extracted from private flatten(properties Properties, input Map<String,Object>, path String) : void in class org.springframework.boot.cloudfoundry.VcapApplicationListener");

		test.project("https://github.com/fabric8io/fabric8.git", "master")
				.atCommit("9e61a71540da58c3208fd2c7737f793c3f81e5ae").containsOnly(
						"Extract Method public createGogsWebhook(kubernetes KubernetesClient, log Log, gogsUser String, gogsPwd String, repoName String, webhookUrl String, webhookSecret String) : boolean extracted from public execute() : void in class io.fabric8.maven.CreateGogsWebhook");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("1cfc6f64f64353bc5530a8ce8cdacfc3eba3e7b2").containsOnly(
						"Extract Method private addEntityScanBeanPostProcessor(registry BeanDefinitionRegistry, packagesToScan Set<String>) : void extracted from public registerBeanDefinitions(importingClassMetadata AnnotationMetadata, registry BeanDefinitionRegistry) : void in class org.springframework.boot.orm.jpa.EntityScanRegistrar");

		test.project("https://github.com/Netflix/eureka.git", "master")
				.atCommit("1cacbe2ad700275bc575234ff2b32ee0d6493817").containsOnly(
						"Extract Method protected fireEvent(event DiscoveryEvent) : void extracted from protected onRemoteStatusChanged(oldStatus InstanceStatus, newStatus InstanceStatus) : void in class com.netflix.discovery.DiscoveryClient");

		test.project("https://github.com/linkedin/rest.li.git", "master")
				.atCommit("bd0d3bf75d31a8b5db34b8b66dfb28e5e1f492de").containsOnly(
						"Extract Method protected extendRecordBaseClass(templateClass JDefinedClass) : void extracted from protected generateRecord(templateClass JDefinedClass, recordSpec RecordTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
						"Extract Method protected extendWrappingMapBaseClass(valueJClass JClass, mapClass JDefinedClass) : void extracted from protected generateMap(mapClass JDefinedClass, mapSpec MapTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
						"Extract Method protected extendUnionBaseClass(unionClass JDefinedClass) : void extracted from protected generateUnion(unionClass JDefinedClass, unionSpec UnionTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
						"Extract Method protected extendWrappingArrayBaseClass(itemJClass JClass, arrayClass JDefinedClass) : void extracted from protected generateArray(arrayClass JDefinedClass, arrayDataTemplateSpec ArrayTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator");

		test.project("https://github.com/orientechnologies/orientdb.git", "master")
				.atCommit("1089957b645bde069d3864563bbf1f7c7da8045c").containsOnly(
						"Extract Method protected rewriteLinksInDocument(document ODocument, rewrite OIndex<OIdentifiable>) : void extracted from private rewriteLinksInDocument(document ODocument) : void in class com.orientechnologies.orient.core.db.tool.ODatabaseImport");

		test.project("https://github.com/belaban/JGroups.git", "master")
				.atCommit("f1533756133dec84ce8218202585ac85904da7c9").containsOnly(
						"Extract Method public isInMembersList(sender IpAddress) : boolean extracted from public authenticate(token AuthToken, msg Message) : boolean in class org.jgroups.auth.FixedMembershipToken");

		test.project("https://github.com/nutzam/nutz.git", "master")
				.atCommit("6599c748ef35d38085703cf3bd41b9b5b6af5f32").containsOnly(
						"Extract Method public from(dao Dao, obj Object, filter FieldFilter, ignoreNull boolean, ignoreZero boolean, ignoreDate boolean, ignoreId boolean, ignoreName boolean, ignorePk boolean) : Cnd extracted from public from(dao Dao, obj Object, filter FieldFilter) : Cnd in class org.nutz.dao.Cnd");

		test.project("https://github.com/infinispan/infinispan.git", "master")
				.atCommit("e3b0d87b3ca0fd27cec39937cb3dc3a05b0cfc4e").containsOnly(
						"Extract Method protected waitForCacheToStabilize(cache Cache<Object,Object>, cacheConfig Configuration) : void extracted from public perform(ctx InvocationContext) : Object in class org.infinispan.commands.CreateCacheCommand");

		test.project("https://github.com/crate/crate.git", "master")
				.atCommit("c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9").containsOnly(
						"Extract Method public plan(relation AnalyzedRelation, consumerContext ConsumerContext) : PlannedAnalyzedRelation extracted from public plan(rootRelation AnalyzedRelation, plannerContext Context) : Plan in class io.crate.planner.consumer.ConsumingPlanner");

		test.project("https://github.com/BuildCraft/BuildCraft.git", "master")
				.atCommit("6abc40ed4850d74ee6c155f5a28f8b34881a0284").containsOnly(
						"Extract Method private initTemplate() : void extracted from public initialize() : void in class buildcraft.builders.TileFiller",
						"Extract Method private initTemplate() : void extracted from public updateEntity() : void in class buildcraft.builders.TileFiller");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("ec52e77ecde749e7c5a483b26cbd8041f2a5a33c").containsOnly(
						"Extract Method public submitBackground(cfs ColumnFamilyStore, autoFill boolean) : List<Future<?>> extracted from public submitBackground(cfs ColumnFamilyStore) : List<Future<?>> in class org.apache.cassandra.db.compaction.CompactionManager");

		test.project("https://github.com/robovm/robovm.git", "master")
				.atCommit("bf5ee44b3b576e01ab09cae9f50300417b01dc07").containsOnly(
						"Extract Method public has(key CFString) : boolean extracted from public getMakerOlympusData() : CFDictionary in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public shouldExcludeXMP() : boolean in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getMakerPentaxData() : CFDictionary in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getSolicitedServiceUUIDs() : NSArray in class org.robovm.apple.corebluetooth.CBCentralManagerScanOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getMakerNikonData() : CGImagePropertyNikonData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public shouldCreateThumbnailFromImageIfAbsent() : boolean in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public shouldCacheImmediately() : boolean in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public shouldCreateThumbnailWithTransform() : boolean in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public shouldExcludeGPS() : boolean in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getOrientation() : CGImagePropertyOrientation in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getMakerMinoltaData() : CFDictionary in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getThumbnailMaxPixelSize() : long in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getLossyCompressionQuality() : double in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getMakerCanonData() : CGImagePropertyCanonData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getSolicitedServiceUUIDs() : NSArray in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key NSString) : boolean extracted from public getOverflowServiceUUIDs() : NSArray in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key CFString) : boolean extracted from public getFileSize() : long in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getRawData() : CFDictionary in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getPixelHeight() : long in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getBackgroundColor() : CGColor in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getPeripherals() : NSArray in class org.robovm.apple.corebluetooth.CBCentralManagerRestoredState",
						"Extract Method public has(key CFString) : boolean extracted from public getDPIHeight() : long in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getDepth() : int in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getDNGData() : CGImagePropertyDNGData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getTypeIdentifierHint() : String in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getGPSData() : CGImagePropertyGPSData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getGIFData() : CGImagePropertyGIFData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getColorModel() : CGImagePropertyColorModel in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getOrientation() : CGImagePropertyOrientation in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public shouldCache() : boolean in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getPNGData() : CGImagePropertyPNGData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getMakerFujiData() : CFDictionary in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getCIFFData() : CGImagePropertyCIFFData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getExifAuxData() : CGImagePropertyExifAuxData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getExifData() : CGImagePropertyExifData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getIPTCData() : CGImagePropertyIPTCData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getRestoreIdentifier() : String in class org.robovm.apple.corebluetooth.CBCentralManagerOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getICCProfile() : String in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getManufacturerData() : NSData in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key NSString) : boolean extracted from public isConnectable() : boolean in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key NSString) : boolean extracted from public getScanOptions() : CBCentralManagerScanOptions in class org.robovm.apple.corebluetooth.CBCentralManagerRestoredState",
						"Extract Method public has(key CFString) : boolean extracted from public getDateTime() : String in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public getJFIFData() : CGImagePropertyJFIFData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getLocalName() : String in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key CFString) : boolean extracted from public getMetadata() : CGImageMetadata in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key NSString) : boolean extracted from public getScanServices() : NSArray in class org.robovm.apple.corebluetooth.CBCentralManagerRestoredState",
						"Extract Method public has(key CFString) : boolean extracted from public getTIFFData() : CGImagePropertyTIFFData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getAdvertisementData() : CBAdvertisementData in class org.robovm.apple.corebluetooth.CBPeripheralManagerRestoredState",
						"Extract Method public has(key NSString) : boolean extracted from public getRestoreIdentifier() : String in class org.robovm.apple.corebluetooth.CBPeripheralManagerOptions",
						"Extract Method public has(key CFString) : boolean extracted from public isIndexed() : boolean in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public hasAlphaChannel() : boolean in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getMaxPixelSize() : long in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Extract Method public has(key CFString) : boolean extracted from public getDPIWidth() : long in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public shouldAllowFloat() : boolean in class org.robovm.apple.imageio.CGImageSourceOptions",
						"Extract Method public has(key NSString) : boolean extracted from public getTxPowerLevel() : double in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key CFString) : boolean extracted from public getPixelWidth() : long in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key NSString) : boolean extracted from public getServiceUUIDs() : NSArray<CBUUID> in class org.robovm.apple.corebluetooth.CBAdvertisementData",
						"Extract Method public has(key NSString) : boolean extracted from public getServices() : NSArray<CBMutableService> in class org.robovm.apple.corebluetooth.CBPeripheralManagerRestoredState",
						// Extract Method in renamed method
						"Extract Method public has(key NSString) : boolean extracted from public isNotifyingOnConnection() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public isNotifyingOnConnection() : boolean renamed to public notifiesOnConnection() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Extract Method public has(key NSString) : boolean extracted from public isNotifyingOnDisconnection() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public isNotifyingOnDisconnection() : boolean renamed to public notifiesOnDisconnection() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Extract Method public has(key NSString) : boolean extracted from public isNotifyingOnNotification() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public isNotifyingOnNotification() : boolean renamed to public notifiesOnNotification() : boolean in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Extract Method public has(key NSString) : boolean extracted from public isShowingPowerAlert() : boolean in class org.robovm.apple.corebluetooth.CBCentralManagerOptions",
						"Rename Method public isShowingPowerAlert() : boolean renamed to public showsPowerAlert() : boolean in class org.robovm.apple.corebluetooth.CBCentralManagerOptions",
						"Extract Method public has(key NSString) : boolean extracted from public isShowingPowerAlert() : boolean in class org.robovm.apple.corebluetooth.CBPeripheralManagerOptions",
						"Rename Method public isShowingPowerAlert() : boolean renamed to public showsPowerAlert() : boolean in class org.robovm.apple.corebluetooth.CBPeripheralManagerOptions",
						"Extract Method public has(key CFString) : boolean extracted from public isMergingMetadata() : boolean in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Rename Method public isMergingMetadata() : boolean renamed to public mergesMetadata() : boolean in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Extract Method public has(key CFString) : boolean extracted from public isEmbeddingThumbnail() : boolean in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Rename Method public isEmbeddingThumbnail() : boolean renamed to public embedsThumbnail() : boolean in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Extract Method public has(key NSString) : boolean extracted from public isAllowingDuplicates() : boolean in class org.robovm.apple.corebluetooth.CBCentralManagerScanOptions",
						"Rename Method public isAllowingDuplicates() : boolean renamed to public allowsDuplicates() : boolean in class org.robovm.apple.corebluetooth.CBCentralManagerScanOptions",
						"Extract Method public has(key CFString) : boolean extracted from public isEnumeratingRecursively() : boolean in class org.robovm.apple.imageio.CGImageMetadataEnumerationOptions",
						"Rename Method public isEnumeratingRecursively() : boolean renamed to public enumeratesRecursively() : boolean in class org.robovm.apple.imageio.CGImageMetadataEnumerationOptions",
						"Extract Method public has(key CFString) : boolean extracted from public get8BIMData() : CGImageProperty8BIMData in class org.robovm.apple.imageio.CGImageProperties",
						"Rename Method public get8BIMData() : CGImageProperty8BIMData renamed to public getData() : CGImageProperty8BIMData in class org.robovm.apple.imageio.CGImageProperties",
						"Extract Method public has(key CFString) : boolean extracted from public isContainingFloatingPointPixels() : boolean in class org.robovm.apple.imageio.CGImageProperties",
						"Rename Method public isContainingFloatingPointPixels() : boolean renamed to public containsFloatingPointPixels() : boolean in class org.robovm.apple.imageio.CGImageProperties",
						// renamed methods
						"Rename Method public setContactInfo(contactInfo CGImagePropertyIPTCContactInfoData) : CGImagePropertyIPTCData renamed to public setCreatorContactInfo(creatorContactInfo CGImagePropertyIPTCContactInfoData) : CGImagePropertyIPTCData in class org.robovm.apple.imageio.CGImagePropertyIPTCData",
						"Rename Method public getContactInfo() : CGImagePropertyIPTCContactInfoData renamed to public getCreatorContactInfo() : CGImagePropertyIPTCContactInfoData in class org.robovm.apple.imageio.CGImagePropertyIPTCData",
						"Rename Method public setNotifyOnDisconnection(notify boolean) : CBConnectPeripheralOptions renamed to public setNotifiesOnDisconnection(notifiesOnDisconnection boolean) : CBConnectPeripheralOptions in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public setNotifyOnNotification(notify boolean) : CBConnectPeripheralOptions renamed to public setNotifiesOnNotification(notifiesOnNotification boolean) : CBConnectPeripheralOptions in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public setNotifyOnConnection(notify boolean) : CBConnectPeripheralOptions renamed to public setNotifiesOnConnection(notifiesOnConnection boolean) : CBConnectPeripheralOptions in class org.robovm.apple.corebluetooth.CBConnectPeripheralOptions",
						"Rename Method public setMergeMetadata(merge boolean) : CGImageDestinationCopySourceOptions renamed to public setMergesMetadata(mergesMetadata boolean) : CGImageDestinationCopySourceOptions in class org.robovm.apple.imageio.CGImageDestinationCopySourceOptions",
						"Rename Method public setShowPowerAlert(showAlert boolean) : CBCentralManagerOptions renamed to public setShowsPowerAlert(showsPowerAlert boolean) : CBCentralManagerOptions in class org.robovm.apple.corebluetooth.CBCentralManagerOptions",
						"Rename Method public setIndexed(isIndexed boolean) : CGImageProperties renamed to public setIsIndexed(isIndexed boolean) : CGImageProperties in class org.robovm.apple.imageio.CGImageProperties",
						"Rename Method public setEmbedThumbnail(embed boolean) : CGImageDestinationProperties renamed to public setEmbedsThumbnail(embedsThumbnail boolean) : CGImageDestinationProperties in class org.robovm.apple.imageio.CGImageDestinationProperties",
						"Rename Method public setEnumerateRecursively(recursive boolean) : CGImageMetadataEnumerationOptions renamed to public setEnumeratesRecursively(enumeratesRecursively boolean) : CGImageMetadataEnumerationOptions in class org.robovm.apple.imageio.CGImageMetadataEnumerationOptions",
						"Rename Method public setShowPowerAlert(showAlert boolean) : CBPeripheralManagerOptions renamed to public setShowsPowerAlert(showsPowerAlert boolean) : CBPeripheralManagerOptions in class org.robovm.apple.corebluetooth.CBPeripheralManagerOptions");

		test.project("https://github.com/GoClipse/goclipse.git", "master")
				.atCommit("851ab757698304e9d8d4ae24ab75be619ddae31a").containsOnly(
						"Extract Method public contains(otherOffset int) : boolean extracted from public inclusiveContains(otherOffset int) : boolean in class melnorme.lang.tooling.ast.SourceRange",
						"Extract Method public contains(other SourceRange) : boolean extracted from public inclusiveContains(other SourceRange) : boolean in class melnorme.lang.tooling.ast.SourceRange");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("c9b2006381301c99b66c50c4b31f329caac06137").containsOnly(
						"Extract Method private open(forWrite boolean, truncate boolean) : void extracted from public open(forWrite boolean) : void in class org.voltdb.utils.PBDRegularSegment",
						"Extract Method private open(forWrite boolean, truncate boolean) : void extracted from public open(forWrite boolean) : void in class org.voltdb.utils.PBDMMapSegment");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("f26d234e8d3458f34454583c22e3bd5f4b2a5da8").containsOnly(
						"Extract Method public getDevices() : List<IDevice> extracted from public adbCall(adbCallable AdbCallable) : boolean in class com.facebook.buck.android.AdbHelper");

		test.project("https://github.com/google/closure-compiler.git", "master")
				.atCommit("ea96643364e91125f560e9508a5cbcdb776bde64").containsOnly(
						"Extract Method private parseFormalParameterList(inTypeExpression boolean) : FormalParameterListTree extracted from private parseFormalParameterList() : FormalParameterListTree in class com.google.javascript.jscomp.parsing.parser.Parser");

		test.project("https://github.com/bumptech/glide.git", "master")
				.atCommit("0d4b27952751de0caab01774048c3e0ec74824ce").containsOnly(
						"Extract Method package clearCallbacksAndListener() : void extracted from private checkCurrentDimens() : void in class com.bumptech.glide.request.target.ViewTarget.SizeDeterminer");

		test.project("https://github.com/eclipse/jetty.project.git", "master")
				.atCommit("1f3be625e62f44d929c01f6574678eea05754474").containsOnly(
						"Extract Method public gatherScannables() : void extracted from public configureScanner() : void in class org.eclipse.jetty.maven.plugin.JettyRunMojo");

		test.project("https://github.com/brianfrankcooper/YCSB.git", "master")
				.atCommit("0b024834549c53512ef18bce89f60ef9225d4819").containsOnly(
						"Extract Method private throttle(currTimeMillis long) : void extracted from public run() : void in class com.yahoo.ycsb.ClientThread");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("b47634176fa48ad925f79886c6aaca225cb9af64").containsOnly(
						"Extract Method private findAll(predicate Predicate<String>) : Iterable<Metric<?>> extracted from public findAll() : Iterable<Metric<?>> in class org.springframework.boot.actuate.metrics.buffer.BufferMetricReader",
						"Extract Method private findAll(predicate Predicate<String>) : Iterable<Metric<?>> extracted from public findAll(prefix String) : Iterable<Metric<?>> in class org.springframework.boot.actuate.metrics.buffer.BufferMetricReader");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("cb98ee25ff52bf97faebe3f45cdef0ced9b4416e").containsOnly(
						"Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideMessageCodesFormat() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests",
						"Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideLocale() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests",
						"Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideDateFormat() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("15fd4f9caea01e53725086e290d3b35ec4bd4cd9").containsOnly(
						"Extract Method protected reset(animateTransition boolean) : void extracted from public reset() : void in class com.android.keyguard.KeyguardAbsKeyInputView");

		test.project("https://github.com/Netflix/eureka.git", "master")
				.atCommit("5103ace802b2819438318dd53b5b07512aae0d25").containsOnly(
						"Extract Method public fillUpRegistryOfServer(serverIdx int, count int, instanceTemplate InstanceInfo) : void extracted from public fillUpRegistry(count int, instanceTemplate InstanceInfo) : void in class com.netflix.eureka2.integration.EurekaDeploymentClients");

		test.project("https://github.com/AsyncHttpClient/async-http-client.git", "master")
				.atCommit("f01d8610b9ceebc1de59d42f569b8af3efbe0a0f").containsOnly(
						"Extract Method package signatureBaseString(method String, uri Uri, oauthTimestamp long, nonce String, formParams List<Param>, queryParams List<Param>) : StringBuilder extracted from public calculateSignature(method String, uri Uri, oauthTimestamp long, nonce String, formParams List<Param>, queryParams List<Param>) : String in class org.asynchttpclient.oauth.OAuthSignatureCalculator");

		test.project("https://github.com/rstudio/rstudio.git", "master")
				.atCommit("9a581e07cb6381d70f3fd9bb2055e810e2a682a9").containsOnly(
						"Extract Method private getBoolean(key String) : boolean extracted from public init(widget AceEditorWidget, position Position) : void in class org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOptionsPopupPanel",
						"Extract Method private has(key String) : boolean extracted from public init(widget AceEditorWidget, position Position) : void in class org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOptionsPopupPanel");

		test.project("https://github.com/spring-projects/spring-integration.git", "master")
				.atCommit("247232bdde24b81814a82100743f77d881aaf06b").containsOnly(
						"Extract Method private handleInputStreamMessage(sourceFileInputStream InputStream, originalFile File, tempFile File, resultFile File) : File extracted from private handleFileMessage(sourceFile File, tempFile File, resultFile File) : File in class org.springframework.integration.file.FileWritingMessageHandler");

		test.project("https://github.com/open-keychain/open-keychain.git", "master")
				.atCommit("c11fef6e7c80681ce69e5fdc7f4796b0b7a18e2b").containsOnly(
						"Extract Method public displayInputFragment(showOpenDialog boolean) : void extracted from private handleActions(savedInstanceState Bundle, intent Intent) : void in class org.sufficientlysecure.keychain.ui.DecryptFilesActivity",
						"Extract Method protected cryptoOperation(cryptoInput CryptoInputParcel, showProgress boolean) : void extracted from protected cryptoOperation(cryptoInput CryptoInputParcel) : void in class org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment");

		test.project("https://github.com/apache/pig.git", "master").atCommit("7a1659c12d76b510809dea1dea1f5100bcf4cd60")
				.containsOnly(
						"Extract Method private initialize() : void extracted from public launchPig(physicalPlan PhysicalPlan, grpName String, pigContext PigContext) : PigStats in class org.apache.pig.backend.hadoop.executionengine.spark.SparkLauncher");

		test.project("https://github.com/infinispan/infinispan.git", "master")
				.atCommit("ce4f6292d6350a2c6b82d995352fdf6d07042c9c").containsOnly(
						"Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private localGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand, isGetCacheEntry boolean) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private remoteGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand) : InternalCacheEntry in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Inline Method private sendCommitCommand(ctx TxInvocationContext, command CommitCommand) : void inlined to public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Extract Method private replayRemoteTransactionIfNeeded(ctx RemoteTxInvocationContext, topologyId int) : void extracted from public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
						"Extract Method public throwRollbackExceptionIfAny() : void extracted from public commit() : void in class org.infinispan.transaction.tm.DummyTransaction",
						"Extract Method protected assertNoTransactions(cacheName String) : void extracted from protected assertNoTransactions() : void in class org.infinispan.test.MultipleCacheManagersTest",
						"Extract Method protected eventually(message String, ec Condition, timeout long, pollInterval long, unit TimeUnit) : void extracted from protected eventually(ec Condition, timeout long, pollInterval long, unit TimeUnit) : void in class org.infinispan.test.AbstractInfinispanTest",
						"Extract Method protected eventually(message String, ec Condition, timeoutMillis long, loops int) : void extracted from protected eventually(ec Condition, timeoutMillis long, loops int) : void in class org.infinispan.test.AbstractInfinispanTest",
						"Extract Method private verifyRemoteTransaction(ctx RemoteTxInvocationContext, command AbstractTransactionBoundaryCommand) : void extracted from private invokeNextInterceptorAndVerifyTransaction(ctx TxInvocationContext, command AbstractTransactionBoundaryCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
						"Extract Method private createRollbackRpcOptions() : RpcOptions extracted from public visitRollbackCommand(ctx TxInvocationContext, command RollbackCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor");

		test.project("https://github.com/grails/grails-core.git", "master")
				.atCommit("480537e0f8aaf50a7648bf445b33230aa32a9b44").containsOnly(
						"Extract Method public weaveTestMixins(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveTestMixins(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation",
						"Extract Method public weaveMixinsIntoClass(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveMixinsIntoClass(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation");

		test.project("https://github.com/jfinal/jfinal.git", "master")
				.atCommit("881baed894540031bd55e402933bcad28b74ca88").containsOnly(
						"Extract Method private validateLongValue(value String, min long, max long, errorKey String, errorMessage String) : void extracted from protected validateLong(field String, min long, max long, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator",
						"Extract Method private validateIntegerValue(value String, min int, max int, errorKey String, errorMessage String) : void extracted from protected validateInteger(field String, min int, max int, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator",
						"Extract Method private validateLongValue(value String, errorKey String, errorMessage String) : void extracted from protected validateLong(field String, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator");

		test.project("https://github.com/SonarSource/sonarqube.git", "master")
				.atCommit("021bf45623b748e70f20d956e86d595191241786").containsOnly(
						"Extract Method package getPluginMetrics() : List<Metric> extracted from public start() : void in class org.sonar.server.startup.RegisterMetrics",
						"Extract Method package register(metrics Iterable) : void extracted from public start() : void in class org.sonar.server.startup.RegisterMetrics");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("f8d5cf01f123a1d0a65857aa2db0571fe9cd1911").containsOnly(
						"Extract Method private getIconImageURL(size int, iconUrl String, blogUrl String) : String extracted from public getIconImageURL(size int) : String in class org.wordpress.android.models.Blog");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("b83e6a535cbca21d5ea764b0c49bfca8a9ff9db4").containsOnly(
						"Extract Method protected query(query Query) : PrimitiveLongIterator extracted from public lookup(value Object) : PrimitiveLongIterator in class org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader",
						"Extract Method protected query(query Query) : PrimitiveLongIterator extracted from public scan() : PrimitiveLongIterator in class org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader");

		test.project("https://github.com/baasbox/baasbox.git", "master")
				.atCommit("d949fe9079a82ee31aa91244aa67baaf56b7e28f").containsOnly(
						"Extract Method public execMultiLineCommands(db ODatabaseRecordTx, log boolean, stopOnException boolean, commands String[]) : void extracted from public execMultiLineCommands(db ODatabaseRecordTx, log boolean, commands String[]) : void in class com.baasbox.db.DbHelper");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("ab298886b59f4ad0235cd6d5764854189eb59eb6").containsOnly(
						"Extract Method public openPostInReaderOrInAppWebview(ctx Context, remoteBlogID String, remoteItemID String, itemType String, itemURL String) : void extracted from public openPostInReaderOrInAppWebview(ctx Context, post PostModel) : void in class org.wordpress.android.ui.stats.StatsUtils");

		test.project("https://github.com/jberkel/sms-backup-plus.git", "master")
				.atCommit("c265bde2ace252bc1e1c65c6af93520e5994edd2").containsOnly(
						"Extract Method public getTokenForLogging() : String extracted from public toString() : String in class com.zegoggles.smssync.auth.OAuth2Token");

		test.project("https://github.com/amplab/tachyon.git", "master")
				.atCommit("ed966510ccf8441115614e2258aea61df0ea55f5").containsOnly(
						"Extract Method private reserveSpace(size long) : void extracted from public addBlockMeta(block BlockMeta) : Optional<BlockMeta> in class tachyon.worker.block.meta.StorageDir");

		test.project("https://github.com/k9mail/k-9.git", "master").atCommit("23c49d834d3859fc76a604da32d1789d2e863303")
				.containsOnly(
						"Extract Method private setNotificationContent(context Context, message Message, sender CharSequence, subject CharSequence, builder Builder, accountDescr String) : Builder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController",
						"Extract Method private buildNotificationNavigationStack(context Context, account Account, message LocalMessage, newMessages int, unreadCount int, allRefs ArrayList<MessageReference>) : TaskStackBuilder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController");

		test.project("https://github.com/antlr/antlr4.git", "master")
				.atCommit("a9ca2efae56815dc464189b055ffe9da23766f7f").containsOnly(
						"Extract Method public getDescendants(t ParseTree) : List<ParseTree> extracted from public descendants(t ParseTree) : List<ParseTree> in class org.antlr.v4.runtime.tree.Trees",
						"Extract Method public getAmbuityParserInterpreter(g Grammar, originalParser Parser, tokens TokenStream) : ParserInterpreter extracted from public getAllPossibleParseTrees(g Grammar, originalParser Parser, tokens TokenStream, decision int, alts BitSet, startIndex int, stopIndex int, startRuleIndex int) : List<ParserRuleContext> in class org.antlr.v4.tool.GrammarParserInterpreter");

		test.project("https://github.com/spring-projects/spring-framework.git", "master")
				.atCommit("ece12f9d370108549fffac105e4bcb7faeaaf124").containsOnly(
						"Extract Method private assertMissingTextAttribute(attributes Map) : void extracted from public synthesizeAnnotationFromMapWithNullAttributeValue() : void in class org.springframework.core.annotation.AnnotationUtilsTests",
						"Extract Method private assertMissingTextAttribute(attributes Map) : void extracted from public synthesizeAnnotationFromMapWithMissingAttributeValue() : void in class org.springframework.core.annotation.AnnotationUtilsTests");

		test.project("https://github.com/infinispan/infinispan.git", "master")
				.atCommit("03573a655bcbb77f7a76d8e22d851cc22796b4f8").containsOnly(
						"Extract Method protected shouldInvoke(event Event<K,V>) : boolean extracted from protected shouldInvoke(event CacheEntryEvent<K,V>, isLocalNodePrimaryOwner boolean) : CacheEntryEvent<K,V> in class org.infinispan.notifications.cachelistener.CacheNotifierImpl.BaseCacheEntryListenerInvocation");

		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("797fb7fc1415ac0ebe9a8262677dfa4462ed6cb4").containsOnly(
						"Extract Method private doAppendNode(node SNode) : void extracted from public appendNode(node SNode) : void in class jetbrains.mps.text.impl.TextGenSupport");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("4bfe164cc8b4556b98df18098b162e0a84038b32").containsOnly(
						"Extract Method private trackLastVisibleTab(position int) : void extracted from protected onResume() : void in class org.wordpress.android.ui.main.WPMainActivity");

		test.project("https://github.com/apache/camel.git", "master")
				.atCommit("5e08a9e8e93a2f117b5fbec9c6d54500d8e99a4d").containsOnly(
						"Extract Method public copyAttachments(that Message) : void extracted from public copyFrom(that Message) : void in class org.apache.camel.impl.MessageSupport");

		test.project("https://github.com/rstudio/rstudio.git", "master")
				.atCommit("4983f83d1bedb7b737fc56d409c1c06b04e34e4e").containsOnly(
						"Extract Method private setValue(value boolean, force boolean) : void extracted from public setValue(value boolean) : void in class org.rstudio.core.client.widget.ThemedCheckBox");

		test.project("https://github.com/liferay/liferay-plugins.git", "master")
				.atCommit("7c7ecf4cffda166938efd0ae34830e2979c25c73").containsOnly(
						"Extract Method protected updateSyncDLObject(syncDLObject SyncDLObject) : void extracted from public onAfterUpdate(resourcePermission ResourcePermission) : void in class com.liferay.sync.hook.listeners.ResourcePermissionModelListener");

		test.project("https://github.com/mockito/mockito.git", "master")
				.atCommit("2d036ecf1d7170b4ec7346579a1ef8904109530a").containsOnly(
						"Extract Method private allMockedTypes(features MockFeatures<T>) : Class<?>[] extracted from public generateMockClass(features MockFeatures<T>) : Class<? extends T> in class org.mockito.internal.creation.bytebuddy.MockBytecodeGenerator");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("7e104c3ed4b80ec8e9b72356396f879d1067cc40").containsOnly(
						"Extract Method private downloadArtifact(artifactToDownload Artifact, repoSys RepositorySystem, session RepositorySystemSession, buckFiles Map<Path,SortedSet<Prebuilt>>, graph MutableDirectedGraph<Artifact>) : void extracted from public resolve(mavenCoords String[]) : void in class com.facebook.buck.maven.Resolver",
						"Extract Method private createBuckFiles(buckFilesData Map<Path,SortedSet<Prebuilt>>) : void extracted from public resolve(mavenCoords String[]) : void in class com.facebook.buck.maven.Resolver");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("e2dd54ab180b577b08cf6b0e69310ac81fc99fd3").containsOnly(
						"Extract Method private foldExprFull(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc extracted from private foldExpr(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc in class org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcFactory");

		// TBD
		// Extract Method private foldExprShortcut(desc ExprNodeDesc, constants
		// Map, cppCtx ConstantPropagateProcCtx, op Operator, tag int, propagate
		// boolean) : ExprNodeDesc extracted from private foldExpr(desc
		// ExprNodeDesc, constants Map, cppCtx ConstantPropagateProcCtx, op
		// Operator, tag int, propagate boolean) : ExprNodeDesc in class
		// org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcFactory

		test.project("https://github.com/ratpack/ratpack.git", "master")
				.atCommit("da6167af3bdbf7663af6c20fb603aba27dd5e174").containsOnly(
						"Extract Method private post(responseStatus HttpResponseStatus, lastContentFuture ChannelFuture) : void extracted from private post(responseStatus HttpResponseStatus) : void in class ratpack.server.internal.DefaultResponseTransmitter");

		test.project("https://github.com/scobal/seyren.git", "master")
				.atCommit("5fb36a321af7df470d4c845cb18da8f85be31c38").containsOnly(
						"Extract Method private evaluateTemplate(check Check, subscription Subscription, alerts List<Alert>, templateContent String) : String extracted from public createBody(check Check, subscription Subscription, alerts List<Alert>) : String in class com.seyren.core.util.velocity.VelocityEmailHelper");

		test.project("https://github.com/open-keychain/open-keychain.git", "master")
				.atCommit("de50b3becb31c367f867382ff9cd898ba1628350").containsOnly(
						"Extract Method public isOrbotInRequiredState(middleButton int, middleButtonRunnable Runnable, fragmentActivity FragmentActivity) : boolean extracted from public isOrbotInRequiredState(middleButton int, middleButtonRunnable Runnable, proxyPrefs ProxyPrefs, fragmentActivity FragmentActivity) : boolean in class org.sufficientlysecure.keychain.util.orbot.OrbotHelper");

		test.project("https://github.com/osmandapp/Osmand.git", "master")
				.atCommit("c45b9e6615181b7d8f4d7b5b1cc141169081c02c").containsOnly(
						"Extract Method private addPreviousToActionPoints(lastProjection Location, routeNodes List<Location>, DISTANCE_ACTION double, prevFinishPoint int, routePoint int, loc Location) : void extracted from private calculateActionPoints(topLatitude double, leftLongitude double, bottomLatitude double, rightLongitude double, lastProjection Location, routeNodes List<Location>, cd int, it Iterator<RouteDirectionInfo>, zoom int) : void in class net.osmand.plus.views.RouteLayer");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("ffae1691c0cd526ed1095fbabbc0855d016790d7").containsOnly(
						"Extract Method protected validateAndConvert(sqlNode SqlNode) : ConvertedRelNode extracted from public getPlan(sqlNode SqlNode) : PhysicalPlan in class org.apache.drill.exec.planner.sql.handlers.DefaultSqlHandler",
						"Extract Method protected convertToDrel(relNode RelNode) : DrillRel extracted from protected convertToDrel(relNode RelNode, validatedRowType RelDataType) : DrillRel in class org.apache.drill.exec.planner.sql.handlers.DefaultSqlHandler");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("5d1a70a4d32ac4c96a32535c68c69b20288d8968").containsOnly(
						"Extract Method public killProcessGroup(uid int, pid int) : void extracted from package removeLruProcessLocked(app ProcessRecord) : void in class com.android.server.am.ActivityManagerService",
						"Extract Method public killProcessGroup(uid int, pid int) : void extracted from package appDiedLocked(app ProcessRecord, pid int, thread IApplicationThread, fromBinderDied boolean) : void in class com.android.server.am.ActivityManagerService",
						"Extract Method public killProcessGroup(uid int, pid int) : void extracted from package startProcessLocked(processName String, info ApplicationInfo, knownToBeDead boolean, intentFlags int, hostingType String, hostingName ComponentName, allowWhileBooting boolean, isolated boolean, isolatedUid int, keepIfLarge boolean, abiOverride String, entryPoint String, entryPointArgs String[], crashHandler Runnable) : ProcessRecord in class com.android.server.am.ActivityManagerService",
						"Extract Method public killProcessGroup(uid int, pid int) : void extracted from private crashApplication(r ProcessRecord, crashInfo CrashInfo) : void in class com.android.server.am.ActivityManagerService");

		test.project("https://github.com/codefollower/Lealone.git", "master")
				.atCommit("7a2e0ae5f6172cbe34f9bc4a5cde666314ff75dd").containsOnly(
						"Extract Method package setPassword(user User, session Session, password Expression) : void extracted from public update() : int in class org.lealone.command.ddl.CreateUser",
						"Extract Method package setSaltAndHash(user User, session Session, salt Expression, hash Expression) : void extracted from public update() : int in class org.lealone.command.ddl.CreateUser");

		test.project("https://github.com/phishman3579/java-algorithms-implementation.git", "master")
				.atCommit("4ffcb5a65e6d24c58ef75a5cd7692e875619548d").containsOnly(
						"Extract Method private collectGarbage() : void extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming");

		test.project("https://github.com/phishman3579/java-algorithms-implementation.git", "master")
				.atCommit("f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29").containsOnly(
						"Extract Method private putOutTheGarbage() : void extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming");

		test.project("https://github.com/droolsjbpm/drools.git", "master")
				.atCommit("c8e09e2056c54ead97bce4386a25b222154223b1").containsOnly(
						"Extract Method public loadClass(className String, classLoader ClassLoader) : Class<?> extracted from public instantiateObject(className String, classLoader ClassLoader) : Object in class org.drools.core.util.ClassUtils",
						"Extract Method public instantiateObject(className String, args Object[]) : Object extracted from public instantiateObject(className String) : Object in class org.drools.core.util.ClassUtils");

		test.project("https://github.com/k9mail/k-9.git", "master").atCommit("9d44f0e06232661259681d64002dd53c7c43099d")
				.containsOnly(
						"Extract Method private handleSendFailure(account Account, localStore Store, localFolder Folder, message Message, exception Exception, permanentFailure boolean) : void extracted from public sendPendingMessagesSynchronous(account Account) : void in class com.fsck.k9.controller.MessagingController");

		test.project("https://github.com/languagetool-org/languagetool.git", "master")
				.atCommit("01cddc5afb590b4d36cb784637a8ea8aa31d3561").containsOnly(
						"Extract Method private getMouseAdapter() : MouseAdapter extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
						"Extract Method private createNonOfficeElements(cons GridBagConstraints, portPanel JPanel) : void extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
						"Extract Method private getMotherTonguePanel(cons GridBagConstraints) : JPanel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
						"Extract Method private getTreeModel(rootNode DefaultMutableTreeNode) : DefaultTreeModel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
						"Extract Method private getTreeButtonPanel() : JPanel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("74d2cc420e5590ba3bc0ffcc15b30b76a9cbef0b").containsOnly(
						"Extract Method private availability() : Availability extracted from private availability(millis long) : Availability in class org.neo4j.kernel.AvailabilityGuard");

		test.project("https://github.com/wicketstuff/core.git", "master")
				.atCommit("8ea46f48063c38473c12ca7c114106ca910b6e74").containsOnly(
						"Extract Method private testRenderedTab() : void extracted from public renderSimpleTab() : void in class org.wicketstuff.foundation.tab.FoundationTabTest");

		test.project("https://github.com/mongodb/morphia.git", "master")
				.atCommit("70a25d4afdc435e9cad4460b2a20b7aabdd21e35").containsOnly(
						"Extract Method private performBasicMappingTest() : void extracted from public testBasicMapping() : void in class org.mongodb.morphia.TestMapping");

		test.project("https://github.com/spring-projects/spring-framework.git", "master")
				.atCommit("31a5434ea433bdec2283797bf9415c02bb2f41c1").containsOnly(
						"Extract Method protected addDefaultHeaders(headers HttpHeaders, t T, contentType MediaType) : void extracted from public write(t T, contentType MediaType, outputMessage HttpOutputMessage) : void in class org.springframework.http.converter.AbstractHttpMessageConverter");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("3b95d10985776fb7b710089ff71074fd2bf860ee").containsOnly(
						"Extract Method private getBlogsForCurrentView() : List<Map<String,Object>> extracted from protected doInBackground(params Void[]) : SiteList in class org.wordpress.android.ui.main.SitePickerAdapter.LoadSitesTask");

		test.project("https://github.com/mrniko/redisson.git", "master")
				.atCommit("186357ac6c2da1a5a12c0287a08408ac5ec6683b").containsOnly(
						"Extract Method public createClient(host String, port int, timeout int) : RedisClient extracted from public createClient(host String, port int) : RedisClient in class org.redisson.connection.MasterSlaveConnectionManager");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("ebb1c2c364e888d4a0f47abe691cb2bad4eb4e75").containsOnly(
						"Extract Method private isGroupbyMatchingIndex(matchingCase MatViewIndexMacthingGroupby, groupbyColRefs List<ColumnRef>, groupbyExprs List<AbstractExpression>, indexedColRefs List<ColumnRef>, indexedExprs List<AbstractExpression>, srcColumnArray List<Column>) : boolean extracted from private findBestMatchIndexForMatviewMinOrMax(matviewinfo MaterializedViewInfo, srcTable Table, groupbyExprs List<AbstractExpression>, singleUniqueMinMaxAggExpr AbstractExpression) : Index in class org.voltdb.compiler.DDLCompiler",
						"Extract Method private isIndexOptimalForMinMax(matchingCase MatViewIndexMacthingGroupby, singleUniqueMinMaxAggExpr AbstractExpression, indexedColRefs List<ColumnRef>, indexedExprs List<AbstractExpression>, srcColumnArray List<Column>) : boolean extracted from private findBestMatchIndexForMatviewMinOrMax(matviewinfo MaterializedViewInfo, srcTable Table, groupbyExprs List<AbstractExpression>, singleUniqueMinMaxAggExpr AbstractExpression) : Index in class org.voltdb.compiler.DDLCompiler");

		test.project("https://github.com/openhab/openhab.git", "master")
				.atCommit("a9b1e5d67421ed98b49ae25c3bbd6e27a0ab1590").containsOnly(
						"Extract Method private bail(txt String) : void extracted from public processData() : Msg in class org.openhab.binding.insteonplm.internal.message.MsgFactory",
						"Extract Method private processBindingConfiguration() : void extracted from public updated(config Dictionary<String,?>) : void in class org.openhab.binding.insteonplm.InsteonPLMActiveBinding");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("711992f22ae6d6dfc43bdb4c01bf8f921d175b38").containsOnly(
						"Extract Method private nextRowInternally() : boolean extracted from public next() : boolean in class org.apache.drill.jdbc.impl.DrillCursor");

		test.project("https://github.com/facebook/presto.git", "master")
				.atCommit("b7f4914d81a7a618acf2eba52af1093fc23cfba9").containsOnly(
						"Extract Method private tryGetLookupSource() : void extracted from public needsInput() : boolean in class com.facebook.presto.operator.LookupJoinOperator",
						"Extract Method private tryGetLookupSource() : void extracted from public getOutput() : Page in class com.facebook.presto.operator.LookupJoinOperator");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("becced5f0b7bac8200df7a5706b568687b517b90").containsOnly(
						"Extract Method private createPreparedEvent(propName String, propValue String) : SpringApplicationEvent extracted from public overridePidFileWithSpring() : void in class org.springframework.boot.actuate.system.ApplicationPidFileWriterTests",
						"Extract Method private createEnvironmentPreparedEvent(propName String, propValue String) : SpringApplicationEvent extracted from public differentEventTypes() : void in class org.springframework.boot.actuate.system.ApplicationPidFileWriterTests");

		test.project("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "master")
				.atCommit("0b93231025f51c7ec62fd8588985c5dc807854e4").containsOnly(
						"Extract Method protected doSomething(virtualFile VirtualFile, module Module, project Project, title String, withProgress boolean) : boolean extracted from protected doSomething(virtualFile VirtualFile, module Module, project Project, title String) : boolean in class com.goide.actions.fmt.GoExternalToolsAction");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("9a3fa887cfa03c082f249d1d4003d87c14ba5d24").containsOnly(
						"Extract Method public getRandomToken(r Random) : LongToken extracted from public getRandomToken() : LongToken in class org.apache.cassandra.dht.Murmur3Partitioner",
						"Extract Method private generateFakeEndpoints(tmd TokenMetadata, numOldNodes int, numVNodes int) : void extracted from private generateFakeEndpoints(numOldNodes int) : void in class org.apache.cassandra.dht.BootStrapperTest",
						"Extract Method private getSpecifiedTokens(metadata TokenMetadata, initialTokens Collection<String>) : Collection<Token> extracted from public getBootstrapTokens(metadata TokenMetadata) : Collection<Token> in class org.apache.cassandra.dht.BootStrapper");

		test.project("https://github.com/mongodb/morphia.git", "master")
				.atCommit("5db323b99f7064af8780f2c35f245461cf55cc8e").containsOnly(
						"Extract Method private performBasicMappingTest() : void extracted from public testBasicMapping() : void in class org.mongodb.morphia.TestMapping");

		test.project("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "master")
				.atCommit("b8929ccb4057c74ac64679216487a4abcd3ae1c3").containsOnly(
						"Extract Method protected isAvailableInModule(module Module) : boolean extracted from protected setupConfigurationFromContext(configuration GoTestRunConfigurationBase, context ConfigurationContext, sourceElement Ref) : boolean in class com.goide.runconfig.testing.GoTestRunConfigurationProducerBase");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("d3533c1a0716ca114d294b3ea183504c9725698f").containsOnly(
						"Extract Method private createNewThread(group Group, job Runnable, metadata Map<String,String>) : Thread extracted from public schedule(group Group, job Runnable, metadata Map<String,String>) : JobHandle in class org.neo4j.kernel.impl.util.Neo4jJobScheduler");

		test.project("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "master")
				.atCommit("3d5e343df6a39ce3b41624b90974d83e9899541e").containsOnly(
						"Extract Method public processResolveVariants(processor GoScopeProcessor) : void extracted from public resolveInner() : PsiElement in class com.goide.psi.impl.GoVarReference");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("5f78f9ef1e6c798849d34cc66721e6c1d9709b6f").containsOnly(
						"Extract Method package generateSplitsInfo(conf Configuration, numSplits int) : List<OrcSplit> extracted from package generateSplitsInfo(conf Configuration) : List<OrcSplit> in class org.apache.hadoop.hive.ql.io.orc.OrcInputFormat");

		test.project("https://github.com/wildfly/wildfly.git", "master")
				.atCommit("37d842bfed9779e662321a5ee43c36b058386843").containsOnly(
						"Extract Method public executeReloadAndWaitForCompletion(client ModelControllerClient, timeout int) : void extracted from public executeReloadAndWaitForCompletion(client ModelControllerClient) : void in class org.jboss.as.test.shared.ServerReload");

		test.project("https://github.com/Netflix/zuul.git", "master")
				.atCommit("b25d3f32ed2e2da86f5c746098686445c2e2a314").containsOnly(
						"Extract Method private putFilter(sName String, filter ZuulFilter, lastModified long) : void extracted from public putFilter(file File) : boolean in class com.netflix.zuul.FilterLoader");

		test.project("https://github.com/jersey/jersey.git", "master")
				.atCommit("fab1516773d50bf86d9cc37e2f6db13496f0ecae").containsOnly(
						"Extract Method public close() : void extracted from public hasNext() : boolean in class org.glassfish.jersey.server.internal.scanning.JarFileScanner",
						"Extract Method private init() : void extracted from public FilesScanner(fileNames String[], recursive boolean) in class org.glassfish.jersey.server.internal.scanning.FilesScanner",
						"Extract Method private init() : void extracted from public reset() : void in class org.glassfish.jersey.server.internal.scanning.FilesScanner");

		test.project("https://github.com/jankotek/MapDB.git", "master")
				.atCommit("32dd05fc13b53873bf18c589622b55d12e3883c7").containsOnly(
						"Pull Up Method protected longStackValParitySet(value long) : long from class org.mapdb.StoreDirect to protected longParitySet(value long) : long from class org.mapdb.Store",
						"Extract Method private insertOrUpdate(recid long, out DataOutputByteArray, isInsert boolean) : void extracted from protected update2(recid long, out DataOutputByteArray) : void in class org.mapdb.StoreAppend");

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
				.atCommit("2fd96c777164dd812e8b5a4294b162889601df1d").containsOnly(
						"Extract Method public newSha256Digest() : MessageDigest extracted from public sha256hash160(input byte[]) : byte[] in class org.bitcoinj.core.Utils");

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
				.atCommit("1d96e1ad1dca6e2151603e10515bb04f0c2730fc").containsOnly(
						"Extract Method public updatedChannel(channel StoredServerChannel) : void extracted from public closeChannel(channel StoredServerChannel) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelServerStates",
						"Extract Method package updatedChannel(channel StoredClientChannel) : void extracted from private putChannel(channel StoredClientChannel, updateWallet boolean) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates",
						"Extract Method package updatedChannel(channel StoredClientChannel) : void extracted from package removeChannel(channel StoredClientChannel) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates");

		test.project("https://github.com/structr/structr.git", "master")
				.atCommit("6c59050b8b03adf6d8043f3fb7add0496f447edf").containsOnly(
						"Extract Method private getSchemaProperties(schemaNode SchemaNode) : List extracted from private getPropertiesForView(type Class, view String, schemaNode SchemaNode) : Map in class org.structr.rest.resource.SchemaTypeResource");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("cfea606b129dbfc5703eb279d4803185afc99c58").containsOnly(
						"Extract Method public getPathToJSBundleDir(target BuildTarget) : Path extracted from protected ReactNativeBundle(ruleParams BuildRuleParams, resolver SourcePathResolver, entryPath SourcePath, isDevMode boolean, bundleName String, jsPackager SourcePath, platform ReactNativePlatform, depsFinder ReactNativeDeps) in class com.facebook.buck.js.ReactNativeBundle");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("f797bfa4da53315b49f8d97b784047f33ba1bf5f").containsOnly(
						"Extract Method private fromUnixTimestamp(timestamp long, nanos long) : long extracted from private fromUnixTimestamp(timestamp long) : long in class org.apache.cassandra.utils.UUIDGen",
						"Extract Method protected createTableName() : String extracted from protected createTable(query String) : String in class org.apache.cassandra.cql3.CQLTester",
						"Extract Method private makeCasRequest(queryState QueryState, options QueryOptions) : CQL3CasRequest extracted from public executeWithCondition(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement",
						"Extract Method private executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
						"Extract Method private makeCasRequest(options BatchQueryOptions, state QueryState) : Pair<CQL3CasRequest,Set<ColumnDefinition>> extracted from private executeWithConditions(options BatchQueryOptions, state QueryState) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
						"Extract Method protected assertInvalidThrowMessage(errorMessage String, exception Class<? extends Throwable>, query String, values Object[]) : void extracted from protected assertInvalidMessage(errorMessage String, query String, values Object[]) : void in class org.apache.cassandra.cql3.CQLTester",
						"Extract Method public executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("d49765899cb9df6781fff9773ffc244b5167351c").containsOnly(
						"Extract Method private getTestPathPredicate(enableStringWhitelisting boolean, whitelistedStringDirs ImmutableSet<Path>, locales ImmutableSet<String>) : Predicate<Path> extracted from public testFilterLocales() : void in class com.facebook.buck.android.FilterResourcesStepTest");

		test.project("https://github.com/apache/giraph.git", "master")
				.atCommit("add1d4f07c925b8a9044cb3aa5bb4abdeaf49fc7").containsOnly(
						"Extract Method private registerSerializer(kryo HadoopKryo, className String, serializer Serializer) : void extracted from private createKryo() : HadoopKryo in class org.apache.giraph.writable.kryo.HadoopKryo");

		test.project("https://github.com/phishman3579/java-algorithms-implementation.git", "master")
				.atCommit("ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9").containsOnly(
						"Extract Method private runTest(testable Testable, unsorted Integer[], sorted Integer[], results double[], count int) : int extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming",
						"Extract Method private runTests(testable Testable, tests int, unsorteds Integer[][], sorteds Integer[][], strings String[]) : boolean extracted from private runTests() : boolean in class com.jwetherell.algorithms.data_structures.timing.DataStructuresTiming");

		test.project("https://github.com/gwtproject/gwt.git", "master")
				.atCommit("22fb2c9c6974bd1fe0f6ff684f52e6cfbed1a387").containsOnly(
						"Extract Method private rescueMembersAndInstantiateSuperInterfaces(type JDeclaredType) : void extracted from public visit(type JInterfaceType, ctx Context) : boolean in class com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("00aa01fb90f3210d1e3027d7f759fb1085b814bd").containsOnly(
						"Inline Method private assertCancelled(controls String, testQuery String, listener WaitUntilCompleteListener) : void inlined to private assertCancelledWithoutException(controls String, listener WaitUntilCompleteListener, query String) : void in class org.apache.drill.exec.server.TestDrillbitResilience",
						"Extract Method public setSessionOption(drillClient DrillClient, option String, value String) : void extracted from public setControls(drillClient DrillClient, controls String) : void in class org.apache.drill.exec.testing.ControlsInjectionUtil");

		test.project("https://github.com/aws/aws-sdk-java.git", "master")
				.atCommit("4baf0a4de8d03022df48d696d210cc8b3117d38a").containsOnly(
						"Inline Method private killServer() : void inlined to public cleanUp() : void in class com.amazonaws.util.EC2MetadataUtilsTest",
						"Extract Method private pause(delay long) : void extracted from private pauseExponentially(retries int) : void in class com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("3f0d9221d3f96c20db10e868cc33c2e972318ba6").containsOnly(
						"Extract Method public createNewWrapperCurrent(batchRecordCount int) : SelectionVector4 extracted from public createNewWrapperCurrent() : SelectionVector4 in class org.apache.drill.exec.record.selection.SelectionVector4");

		test.project("https://github.com/SonarSource/sonarqube.git", "master")
				.atCommit("0d9fcaa4415ee996e423a97cfe0d965586ca59a5").containsOnly(
						"Extract Method private doStop(swallowException boolean) : void extracted from public stop() : void in class org.sonar.batch.bootstrapper.Batch");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("c1b847acdc8cb90a1498b236b3bb5c81ca75c044").containsOnly(
						"Inline Method public createFileSystem(proxyUserName String, fsConf Configuration, stats OperatorStats) : DrillFileSystem inlined to public createFileSystem(proxyUserName String, fsConf Configuration) : DrillFileSystem in class org.apache.drill.exec.util.ImpersonationUtil",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationMetadata to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorageAndGenerateTestData() : void from class org.apache.drill.exec.impersonation.TestImpersonationQueries to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorageAndGenerateTestData() : void in class org.apache.drill.exec.impersonation.TestImpersonationQueries");

		test.project("https://github.com/facebook/facebook-android-sdk.git", "master")
				.atCommit("e813a0be86c87366157a0201e6c61662cadee586").containsOnly(
						"Extract Method private getAndroidIdViaReflection(context Context) : AttributionIdentifiers extracted from private getAndroidId(context Context) : AttributionIdentifiers in class com.facebook.internal.AttributionIdentifiers");
		// TBD
		// seems the same thing extracted to two methods
		// Extract Method private getAndroidIdViaService(context Context) :
		// AttributionIdentifiers extracted from private getAndroidId(context
		// Context) : AttributionIdentifiers in class
		// com.facebook.internal.AttributionIdentifiers
		// https://github.com/facebook/facebook-android-sdk/commit/e813a0be86c87366157a0201e6c61662cadee586#diff-ffb6ca289dbd8b98fdfd8f6b376a1464L62

		test.project("https://github.com/fabric8io/fabric8.git", "master")
				.atCommit("e068eb7f484f24dee285d29b8a910d9019592020").containsOnly(
						"Extract Method private getHTTPGetAction(prefix String, properties Properties) : HTTPGetAction extracted from protected getProbe(prefix String) : Probe in class io.fabric8.maven.JsonMojo");

		test.project("https://github.com/killbill/killbill.git", "master")
				.atCommit("4b5b74b6467a28fb9b7712f8091e4aa61c2d64b6").containsOnly(
						"Extract Method public updatePaymentAndTransactionIfNeeded(payment PaymentModelDao, paymentTransaction PaymentTransactionModelDao, paymentTransactionInfoPlugin PaymentTransactionInfoPlugin, internalTenantContext InternalTenantContext) : boolean extracted from public doIteration(paymentTransaction PaymentTransactionModelDao) : void in class org.killbill.billing.payment.core.janitor.IncompletePaymentTransactionTask");

		test.project("https://github.com/deeplearning4j/deeplearning4j.git", "master")
				.atCommit("d4992887291cc0a7eeda87ad547fa9e1e7fda41c").containsOnly(
						"Extract Method public output(x INDArray, test boolean) : INDArray extracted from public output(x INDArray) : INDArray in class org.deeplearning4j.nn.layers.OutputLayer");

		test.project("https://github.com/AntennaPod/AntennaPod.git", "master")
				.atCommit("c64217e2b485f3c6b997a55b1ef910c8b72779d3").containsOnly(
						"Extract Method public addQueueItem(context Context, performAutoDownload boolean, itemIds long[]) : Future<?> extracted from public addQueueItem(context Context, itemIds long[]) : Future<?> in class de.danoeh.antennapod.core.storage.DBWriter");

		test.project("https://github.com/datastax/java-driver.git", "master")
				.atCommit("9de5f0d408f861455716b8410fd53f62b360787d").containsOnly(
						"Extract Method package sendRequest(reportNoMoreHosts boolean) : boolean extracted from package sendRequest() : void in class com.datastax.driver.core.RequestHandler.SpeculativeExecution",
						"Extract Method protected query(session Session) : ResultSet extracted from protected query() : ResultSet in class com.datastax.driver.core.policies.AbstractRetryPolicyIntegrationTest",
						"Extract Method private retry(retryCurrent boolean, newConsistencyLevel ConsistencyLevel, connection Connection, response Response) : void extracted from private retry(retryCurrent boolean, newConsistencyLevel ConsistencyLevel) : void in class com.datastax.driver.core.RequestHandler.SpeculativeExecution");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("7eb3567e7880511b76b8b65e8eb7d373927f2fb6").containsOnly(
						"Extract Method private unionTester(ws Schema, rs Schema, record Record) : ResultPair extracted from private unionTester(s Schema, record Record) : ResultPair in class org.apache.hadoop.hive.serde2.avro.TestAvroDeserializer");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("e58c9c3eef4c6e44b21a97cfbd2862bb2eb4627a").containsOnly(
						"Extract Method public hasSize(size int) : SymbolTableAssert extracted from public isEmpty() : SymbolTableAssert in class org.voltdb.sqlparser.symtab.SymbolTableAssert");

		test.project("https://github.com/koush/AndroidAsync.git", "master")
				.atCommit("1bc7905b07821f840068089343e6b77a8686d1ab").containsOnly(
						"Extract Method private terminate() : void extracted from protected report(e Exception) : void in class com.koushikdutta.async.http.AsyncHttpResponseImpl");

		test.project("https://github.com/gradle/gradle.git", "master")
				.atCommit("0e7345a9c10863dca9217ad902b825db50fed01f").containsOnly(
						"Extract Method private getConfiguration() : Configuration extracted from package getFileCollection() : FileCollection in class org.gradle.play.plugins.PlayPluginConfigurations.PlayConfiguration");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("102b23b16bf26cbf439009b4b95542490a082710").containsOnly(
						"Extract Method private executeInternal(sql String, call boolean) : boolean extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands",
						"Extract Method public handleMultiLineCmd(line String) : String extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands");

		test.project("https://github.com/gradle/gradle.git", "master")
				.atCommit("79c66ceab11dae0b9fd1dade7bb4120028738705").containsOnly(
						"Extract Method public getInputs() : Set<LanguageSourceSet> extracted from public getAllSources() : Set<LanguageSourceSet> in class org.gradle.platform.base.binary.BaseBinarySpec");

		test.project("https://github.com/spring-projects/spring-data-jpa.git", "master")
				.atCommit("d69e5cb21c04d9eede314aaa9ad059fc603fb025").containsOnly(
						"Extract Method protected getCountQuery(spec Specification<T>, mode QueryMode) : TypedQuery<Long> extracted from protected getCountQuery(spec Specification<T>) : TypedQuery<Long> in class org.springframework.data.jpa.repository.support.SimpleJpaRepository");

		test.project("https://github.com/droolsjbpm/jbpm.git", "master")
				.atCommit("a739d16d301f0e89ab0b9dfa56b4585bbad6b793").containsOnly(
						"Extract Method private createUser(id String) : User extracted from public testActivateFromIncorrectStatus() : void and 10 others in class org.jbpm.services.task.LifeCycleBaseTest");

		test.project("https://github.com/droolsjbpm/jbpm.git", "master")
				.atCommit("83cfa21578e63956bca0715eefee2860c3b6d39a").containsOnly(
						"Extract Method private prepareWorkItemWithTaskVariables(taskDescriptionParam String) : WorkItemImpl extracted from public testTaskWithVariables() : void in class org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest",
						"Extract Method private testTaskWithExpectedDescription(task Task, expectedDescription String) : void extracted from public testTaskWithVariables() : void in class org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("35668435090eb47cf8c5e704243510b6cee35a7b").containsOnly(
						"Extract Method private onCreateFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onCreateAggregate(ksName String, aggregateName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
						"Extract Method private onDropFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onDropAggregate(ksName String, aggregateName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
						"Extract Method private onDropFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onDropFunction(ksName String, functionName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
						"Extract Method private onCreateFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onCreateFunction(ksName String, functionName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber");

		test.project("https://github.com/FasterXML/jackson-databind.git", "master")
				.atCommit("cfe88fe3fbcc6b02ca55cee7b1f4ab13e249edea").containsOnly(
						"Extract Method protected classForName(name String) : Class<?> extracted from public findClass(className String) : Class<?> in class com.fasterxml.jackson.databind.type.TypeFactory",
						"Extract Method protected classForName(name String, initialize boolean, loader ClassLoader) : Class<?> extracted from public findClass(className String) : Class<?> in class com.fasterxml.jackson.databind.type.TypeFactory");

		test.project("https://github.com/rstudio/rstudio.git", "master")
				.atCommit("229d1b60c03a3f8375451c68a6911660a3993777").containsOnly(
						"Inline Method private fireValidatedRSconnectPublish(result RSConnectPublishResult, launchBrowser boolean) : void inlined to public fireRSConnectPublishEvent(result RSConnectPublishResult, launchBrowser boolean) : void in class org.rstudio.studio.client.rsconnect.RSConnect",
						"Extract Method private isUpdate() : boolean extracted from public getResult() : RSConnectPublishResult in class org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("d1a6ae2a16ba1d53b1de02eea8745d67c6a1a005").containsOnly(
						"Extract Method private fileSelection() : File extracted from public actionPerformed(e ActionEvent) : void in class org.neo4j.desktop.ui.BrowseForDatabaseActionListener");

		test.project("https://github.com/katzer/cordova-plugin-local-notifications.git", "master")
				.atCommit("51f498a96b2fa1822e392027982c20e950535fd1").containsOnly(
						"Inline Method public postMessage(id String, data Object) : void inlined to public onCreateOptionsMenu(menu Menu) : boolean in class org.apache.cordova.CordovaActivity",
						"Inline Method public postMessage(id String, data Object) : void inlined to public onOptionsItemSelected(item MenuItem) : boolean in class org.apache.cordova.CordovaActivity",
						"Inline Method public postMessage(id String, data Object) : void inlined to public onPrepareOptionsMenu(menu Menu) : boolean in class org.apache.cordova.CordovaActivity",
						"Extract Method public handleEndTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser",
						"Extract Method public handleStartTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser");

		test.project("https://github.com/square/wire.git", "master")
				.atCommit("85a690e3cdbbb8447342eefdf690e22ad1b33e02").containsOnly(
						"Extract Method private fieldInitializer(type ProtoTypeName, value Object) : CodeBlock extracted from private defaultValue(field WireField) : CodeBlock in class com.squareup.wire.java.TypeWriter");

		test.project("https://github.com/slapperwan/gh4a.git", "master")
				.atCommit("b8fffb706258db4c4d2f608d8e8dad9312e2230d").containsOnly(
						"Extract Method private isExtensionIn(filename String, extensions List<String>) : boolean extracted from public isImage(filename String) : boolean in class com.gh4a.utils.FileUtils");

		test.project("https://github.com/deeplearning4j/deeplearning4j.git", "master")
				.atCommit("b1fb1192daa1647b0bd525600dd41063765eca70").containsOnly(
						"Extract Method public loadGoogleModel(modelFile File, binary boolean, lineBreaks boolean) : Word2Vec extracted from public loadGoogleModel(modelFile File, binary boolean) : Word2Vec in class org.deeplearning4j.models.embeddings.loader.WordVectorSerializer");

		test.project("https://github.com/crate/crate.git", "master")
				.atCommit("d5f10a4958f5e870680be906689d92d1efb42480").containsOnly(
						"Extract Method public add(info ReferenceInfo, partitionBy boolean) : Builder extracted from public add(column String, type DataType, path List<String>, columnPolicy ColumnPolicy, indexType IndexType, partitionBy boolean) : Builder in class io.crate.metadata.table.TestingTableInfo.Builder");

		test.project("https://github.com/google/truth.git", "master")
				.atCommit("200f1577d238a6d3fbcf99cb2a2585b2071214a6").containsOnly(
						"Extract Method public isOrdered(comparator Comparator<? super T>) : void extracted from public isPartiallyOrdered(comparator Comparator<? super T>) : void in class com.google.common.truth.IterableSubject",
						"Extract Method public isOrdered() : void extracted from public isPartiallyOrdered() : void in class com.google.common.truth.IterableSubject");

		test.project("https://github.com/wildfly/wildfly.git", "master")
				.atCommit("c0f8a7f2b4341601df63c5470f41f157dbd83781").containsOnly(
						"Extract Method private standaloneCollect(cli CLI, protocol String, host String, port int) : void extracted from public main(args String[]) : void in class org.jboss.as.jdr.CommandLineMain");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("4587c32ab8a1c8e2169e4f93491a8c927216a6ab").containsOnly(
						"Extract Method private startAsync() : void extracted from public start() : void in class com.android.systemui.usb.StorageNotification");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("b2bbd9941be6b132a83d27c0ae02c935e1dec5dd").containsOnly(
						"Extract Method private allocateBytes(size long) : void extracted from public allocateNew(valueCount int) : void in class org.apache.drill.exec.vector.BitVector",
						"Extract Method private allocateBytes(size long) : void extracted from public allocateNewSafe() : boolean in class org.apache.drill.exec.vector.$",
						"Extract Method private allocateBytes(size long) : void extracted from public allocateNew(valueCount int) : void in class org.apache.drill.exec.vector.$",
						"Extract Method private allocateBytes(size long) : void extracted from public allocateNewSafe() : boolean in class org.apache.drill.exec.vector.BitVector");

		test.project("https://github.com/google/j2objc.git", "master")
				.atCommit("fa3e6fa02dadc675f0d487a15cd842b3ac4a0c11").containsOnly(
						"Extract Method private getOperatorFunctionModifier(expr Expression) : String extracted from private rewriteBoxedPrefixOrPostfix(node TreeNode, operand Expression, funcName String) : void in class com.google.devtools.j2objc.translate.Autoboxer",
						"Extract Method private getOperatorFunctionModifier(expr Expression) : String extracted from private rewriteBoxedAssignment(node Assignment) : void in class com.google.devtools.j2objc.translate.Autoboxer");

		test.project("https://github.com/deeplearning4j/deeplearning4j.git", "master")
				.atCommit("91cdfa1ffd937a4cb01cdc0052874ef7831955e2").containsOnly(
						"Extract Method private getNewScore(oldParameters INDArray) : double extracted from public optimize(initialStep double, parameters INDArray, gradients INDArray) : double in class org.deeplearning4j.optimize.solvers.BackTrackLineSearch");

		test.project("https://github.com/spring-projects/spring-framework.git", "master")
				.atCommit("ef0eb01f93d6c485cf37692fd193833a6821272a").containsOnly(
						"Extract Method protected checkRequest(request HttpServletRequest) : void extracted from protected checkAndPrepare(request HttpServletRequest, response HttpServletResponse, cacheControl CacheControl) : void in class org.springframework.web.servlet.support.WebContentGenerator");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("b8d2140fe4faccadcf1a6343ec8cd0cc58c315f9").containsOnly(
						"Extract Method private doFirstFetchIfNeeded() : void extracted from private joinFinalLeftData() : void in class org.apache.hadoop.hive.ql.exec.CommonMergeJoinOperator",
						"Extract Method private doFirstFetchIfNeeded() : void extracted from public process(row Object, tag int) : void in class org.apache.hadoop.hive.ql.exec.CommonMergeJoinOperator");

		test.project("https://github.com/nutzam/nutz.git", "master")
				.atCommit("de7efe40dad0f4bb900c4fffa80ed377745532b3").containsOnly(
						"Extract Method public migration(dao Dao, klass Class<?>, add boolean, del boolean, tableName Object) : void extracted from public migration(dao Dao, klass Class<?>, add boolean, del boolean) : void in class org.nutz.dao.util.Daos");

		test.project("https://github.com/amplab/tachyon.git", "master")
				.atCommit("b0938501f1014cf663e33b44ed5bb9b24d19a358").containsOnly(
						"Extract Method private getBlockOutStream(filename String, isLocalWrite boolean) : BlockOutStream extracted from public enableLocalWriteTest() : void in class tachyon.client.BlockOutStreamIntegrationTest",
						"Extract Method private getBlockOutStream(filename String, isLocalWrite boolean) : BlockOutStream extracted from public disableLocalWriteTest() : void in class tachyon.client.BlockOutStreamIntegrationTest");

		test.project("https://github.com/orfjackal/retrolambda.git", "master")
				.atCommit("46b0d84de9c309bca48a99e572e6611693ed5236").containsOnly(
						"Extract Method public saveResource(relativePath Path, content byte[]) : void extracted from public save(bytecode byte[]) : void in class net.orfjackal.retrolambda.files.ClassSaver");

		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("7b5622d41537315710b6fd57b2739a3a64698375").containsOnly(
						"Extract Method private getTreePath(components List<String>, escapePathSep boolean) : TreePath extracted from private stringToPath(pathString String) : TreePath in class jetbrains.mps.ide.ui.tree.MPSTree");

		test.project("https://github.com/facebook/facebook-android-sdk.git", "master")
				.atCommit("19d1936c3b07d97d88646aeae30de747715e3248").containsOnly(
						"Extract Method private getErrorMessage(extras Bundle) : String extracted from private handleResultOk(request Request, data Intent) : Result in class com.facebook.login.KatanaProxyLoginMethodHandler",
						"Extract Method public sdkInitialize(applicationContext Context, callbackRequestCodeOffset int, callback InitializeCallback) : void extracted from public sdkInitialize(applicationContext Context, callbackRequestCodeOffset int) : void in class com.facebook.FacebookSdk",
						"Extract Method public sdkInitialize(applicationContext Context, callback InitializeCallback) : void extracted from public sdkInitialize(applicationContext Context) : void in class com.facebook.FacebookSdk",
						"Extract Method private getError(extras Bundle) : String extracted from private handleResultOk(request Request, data Intent) : Result in class com.facebook.login.KatanaProxyLoginMethodHandler");

		test.project("https://github.com/WhisperSystems/TextSecure.git", "master")
				.atCommit("f0b2cc559026871c1b4d8e008666afb590553004").containsOnly(
						"Extract Method private craftIntent(context Context, intentAction String, extras Bundle) : PendingIntent extracted from public getMarkAsReadIntent(context Context, masterSecret MasterSecret) : PendingIntent in class org.thoughtcrime.securesms.notifications.NotificationState");

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
				.atCommit("95bfa40630e34f6f369e0055d9f37f49bca60247").containsOnly(
						"Extract Method public getUTXOs(outPoints List<TransactionOutPoint>, includeMempool boolean) : ListenableFuture<UTXOsMessage> extracted from public getUTXOs(outPoints List<TransactionOutPoint>) : ListenableFuture<UTXOsMessage> in class org.bitcoinj.core.Peer");

		test.project("https://github.com/WhisperSystems/TextSecure.git", "master")
				.atCommit("c4a37e38aba926c2bef27e4fc00e3a4848ce46bd").containsOnly(
						"Extract Method public setMedia(slide Slide, masterSecret MasterSecret) : void extracted from public setMedia(slide Slide) : void in class org.thoughtcrime.securesms.mms.AttachmentManager");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("f664789737d516ac664462732664121acc111a1e").containsOnly(
						"Extract Method private dumpConfig(conf Configuration, sb StringBuilder) : void extracted from private dumpEnvironent() : String in class org.apache.hive.hcatalog.templeton.AppConfig");

		test.project("https://github.com/amplab/tachyon.git", "master")
				.atCommit("9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825").containsOnly(
						"Extract Method private getCandidateDirInTier(tier StorageTier, blockSize long) : StorageDir extracted from public allocateBlock(userId long, blockId long, blockSize long, location BlockStoreLocation) : TempBlockMeta in class tachyon.worker.block.allocator.MaxFreeAllocator");

		test.project("https://github.com/crate/crate.git", "master")
				.atCommit("563d281b61e9f8748858e911eaa810e981f1e953").containsOnly(
						"Extract Method private getCustomRoutingCol() : ColumnIdent extracted from private getRoutingCol() : ColumnIdent in class io.crate.metadata.doc.DocIndexMetaData");

		test.project("https://github.com/siacs/Conversations.git", "master")
				.atCommit("bdc9f9a44f337ab595a3570833dc6a0558df904c").containsOnly(
						"Extract Method private getIdentityKeyCursor(account Account, name String, own boolean, fingerprint String) : Cursor extracted from private getIdentityKeyCursor(account Account, name String, own boolean) : Cursor in class eu.siacs.conversations.persistance.DatabaseBackend");

		test.project("https://github.com/openhab/openhab.git", "master")
				.atCommit("cf1efb6d27a4037cdbe5a780afa6053859a60d4a").containsOnly(
						"Extract Method private initializeGeneralGlobals() : void extracted from private initializeSciptGlobals() : void in class org.openhab.core.jsr223.internal.engine.scriptmanager.Script",
						"Extract Method private initializeNashornGlobals() : void extracted from private initializeSciptGlobals() : void in class org.openhab.core.jsr223.internal.engine.scriptmanager.Script");

		test.project("https://github.com/spotify/helios.git", "master")
				.atCommit("910397f2390d6821a006991ed6035c76cbc74897").containsOnly(
						"Extract Method package run0(client HeliosClient, out PrintStream, json boolean, name String, full boolean) : int extracted from package run(options Namespace, client HeliosClient, out PrintStream, json boolean, stdin BufferedReader) : int in class com.spotify.helios.cli.command.DeploymentGroupStatusCommand");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("910397f2390d6821a006991ed6035c76cbc74897").containsOnly(
						"Extract Method private queryProperty(id int, fromDock boolean) : long extracted from private queryProperty(id int) : long in class android.os.BatteryManager",
						"Extract Method protected getBoltPointsArrayResource() : int extracted from private loadBoltPoints(res Resources) : float[] in class com.android.systemui.BatteryMeterView.NormalBatteryMeterDrawable",
						"Extract Method public internalStoreStatsHistoryInFile(stats BatteryStats, fname String) : void extracted from public storeStatsHistoryInFile(fname String) : void in class com.android.internal.os.BatteryStatsHelper",
						"Extract Method protected setCpuSpeedSteps(numSpeedSteps int) : void extracted from public readSummaryFromParcel(in Parcel) : void in class com.android.internal.os.BatteryStatsImpl",
						"Extract Method protected setCpuSpeedSteps(numSpeedSteps int) : void extracted from public setNumSpeedSteps(steps int) : void in class com.android.internal.os.BatteryStatsImpl",
						"Extract Method protected setCpuSpeedSteps(numSpeedSteps int) : void extracted from package readFromParcelLocked(in Parcel) : void in class com.android.internal.os.BatteryStatsImpl");

		test.project("https://github.com/thymeleaf/thymeleaf.git", "master")
				.atCommit("aed371dac5e1248880e869930c636994c3d0f8dc").containsOnly(
						"Extract Method private fillUpOverflow() : void extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.markup.ThymeleafMarkupTemplateReader",
						"Extract Method private processReadBuffer(buffer char[], off int, len int) : int extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.markup.ThymeleafMarkupTemplateReader");

		test.project("https://github.com/HubSpot/Singularity.git", "master")
				.atCommit("944aea445051891280a8ab7fbbd514c19646f1ab").containsOnly(
						"Extract Method protected launchTask(request SingularityRequest, deploy SingularityDeploy, launchTime long, updateTime long, instanceNo int, initialTaskState TaskState) : SingularityTask extracted from protected launchTask(request SingularityRequest, deploy SingularityDeploy, launchTime long, instanceNo int, initialTaskState TaskState) : SingularityTask in class com.hubspot.singularity.SingularitySchedulerTestBase");

		test.project("https://github.com/google/auto.git", "master")
				.atCommit("8fc60d81fe0e46e7e5c96e71d4a93fcadc6bde4f").containsOnly(
						"Extract Method private deferredElements() : ImmutableMap<String,Optional<? extends Element>> extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor",
						"Extract Method private validElements(deferredElements ImmutableMap<String,Optional<? extends Element>>, roundEnv RoundEnvironment) : ImmutableSetMultimap<Class<? extends Annotation>,Element> extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor",
						"Extract Method private process(validElements ImmutableSetMultimap<Class<? extends Annotation>,Element>) : void extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor");

		test.project("https://github.com/google/auto.git", "master")
				.atCommit("8967e7c33c59e1336e1e3b4671293ced5697fca6").containsOnly(
						"Extract Method private doTestMissingClass(tempDir File) : void extracted from public testMissingClass() : void in class com.google.auto.value.processor.AutoAnnotationCompilationTest");

		test.project("https://github.com/thymeleaf/thymeleaf.git", "master")
				.atCommit("378ba37750a9cb1b19a6db434dfa59308f721ea6").containsOnly(
						"Extract Method private matchOverflow(structure char[]) : boolean extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.reader.BlockAwareReader");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("e1625136ba12907696ef4c6e922ce073293f3a2c").containsOnly(
						"Extract Method private addAnnotationProcessorOption(compilerArg String, optionsMap Map<String,String>) : void extracted from private getAnnotationProcessorOptionsFromCompilerConfig(compilerConfig Element) : Map<String,String> in class org.jetbrains.idea.maven.project.MavenProject");

		test.project("https://github.com/apache/camel.git", "master")
				.atCommit("cb0935e3af05b333b5c85a4fb3b1846836218f11").containsOnly(
						"Extract Method private storeCamelContextInQuartzContext() : SchedulerContext extracted from private createAndInitScheduler() : void in class org.apache.camel.component.quartz2.QuartzComponent");

		test.project("https://github.com/spotify/helios.git", "master")
				.atCommit("dd8753cfb0f67db4dde6c5254e2df3104b635dae").containsOnly(
						"Extract Method private getDeploymentGroup(client ZooKeeperClient, name String) : DeploymentGroup extracted from public getDeploymentGroup(name String) : DeploymentGroup in class com.spotify.helios.master.ZooKeeperMasterModel");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("76331570e68446c17e4ff5287f5b7b2b6b472895").containsOnly(
						"Extract Method public clearFailedUnlockAttempts(clearFingers boolean) : void extracted from public clearFailedUnlockAttempts() : void in class com.android.keyguard.KeyguardUpdateMonitor");

		test.project("https://github.com/github/android.git", "master")
				.atCommit("a7401e5091c06c68fae499ea1972b40143c66fa9").containsOnly(
						"Extract Method private onUserLoggedIn(uri Uri) : void extracted from protected onNewIntent(intent Intent) : void in class com.github.mobile.accounts.LoginActivity");

		test.project("https://github.com/deeplearning4j/deeplearning4j.git", "master")
				.atCommit("3d080545362794ac5ab63a6cf1bdfb523a0d92a5").containsOnly(
						"Extract Method public readCaffeModel(is InputStream, sizeLimitMb int) : NetParameter extracted from public readCaffeModel(caffeModelPath String, sizeLimitMb int) : NetParameter in class org.deeplearning4j.translate.CaffeModelToJavaClass");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("679d38d4316c16ccba4982d7f3ba13c147a451cb").containsOnly(
						"Extract Method protected getFromNearCache(keyData Data, async boolean) : Object extracted from protected getInternal(key K, expiryPolicy ExpiryPolicy, async boolean) : Object in class com.hazelcast.client.cache.impl.AbstractClientCacheProxy");

		test.project("https://github.com/SimonVT/schematic.git", "master")
				.atCommit("c1a9dd63aca8bf488f9a671aa6281538540397f8").containsOnly(
						"Extract Method private printNotifyInsert(writer JavaWriter, uri UriContract) : void extracted from public write(filer Filer) : void in class net.simonvt.schematic.compiler.ContentProviderWriter");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("5fa74fbb4a307571e3807c1201b8b05d3d60a99b").containsOnly(
						"Extract Method private createCountsTracker(pageCache PageCache) : CountsTracker extracted from public shouldCreateEmptyCountsTrackerStoreWhenCreatingDatabase() : void and 2 others in class org.neo4j.kernel.impl.store.counts.CountsRotationTest",
						"Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountStoreWhenDBContainsDenseNodes() : void and 5 others in class org.neo4j.kernel.impl.store.counts.CountsComputerTest");

		test.project("https://github.com/jline/jline2.git", "master")
				.atCommit("80d3ffb5aafa90992385c17e8338c2cc5def3cec").containsOnly(
						"Extract Method public readCharacter(checkForAltKeyCombo boolean) : int extracted from public readCharacter() : int in class jline.console.ConsoleReader",
						"Extract Method public readCharacter(checkForAltKeyCombo boolean, allowed char[]) : int extracted from public readCharacter(allowed char[]) : int in class jline.console.ConsoleReader");

		test.project("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "master")
				.atCommit("9687048f76519fc89b4151cbe2841bbba61a401d").containsOnly(
						"Extract Method protected getEntityForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm extracted from protected getBlankDynamicFieldTemplateForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm in class org.broadleafcommerce.openadmin.web.controller.AdminAbstractController");

		test.project("https://github.com/Netflix/feign.git", "master")
				.atCommit("b2b4085348de32f10903970dded99fdf0376a43c").containsOnly(
						"Extract Method protected parseAndValidateMetadata(targetType Class<?>, method Method) : MethodMetadata extracted from public parseAndValidatateMetadata(method Method) : MethodMetadata in class feign.Contract.BaseContract",
						"Extract Method private headersFromAnnotation(targetType Class<?>, data MethodMetadata) : void extracted from public parseAndValidatateMetadata(method Method) : MethodMetadata in class feign.Contract.Default",
						"Extract Method public configKey(targetType Class, method Method) : String extracted from public configKey(method Method) : String in class feign.Feign");

		test.project("https://github.com/structr/structr.git", "master")
				.atCommit("15afd616cba5fb3d432d11a6de0d4f7805b202db").containsOnly(
						"Extract Method package handleObject(nodeFactory NodeFactory, relFactory RelationshipFactory, key String, value Object, includeHiddenAndDeleted boolean, publicOnly boolean, level int) : Object extracted from public execute(query String, parameters Map<String,Object>, includeHiddenAndDeleted boolean, publicOnly boolean) : List<GraphObject> in class org.structr.core.graph.CypherQueryCommand");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("76d7f5e3fe4eb41b383c1d884bc1217b9fa7192e").containsOnly(
						"Extract Method protected createAddress(host String, port int) : Address extracted from private createAddresses(count int) : Address[] in class com.hazelcast.test.TestHazelcastInstanceFactory",
						"Extract Method protected createAddress(host String, port int) : Address extracted from private createAddresses(addressArray String[]) : Address[] in class com.hazelcast.test.TestHazelcastInstanceFactory",
						"Extract Method protected startSelectors() : void extracted from public start() : void in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl",
						"Extract Method protected initializeSelectors(client HazelcastClientInstanceImpl) : void extracted from public ClientConnectionManagerImpl(client HazelcastClientInstanceImpl, addressTranslator AddressTranslator) in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl",
						"Extract Method protected shutdownSelectors() : void extracted from public shutdown() : void in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl");

		test.project("https://github.com/geometer/FBReaderJ.git", "master")
				.atCommit("42e0649f82779ecd48bff6448924fc7dc2534554").containsOnly(
						"Extract Method private allTopLevelNodes() : List<MenuNode> extracted from public topLevelNodes() : List<MenuNode> in class org.geometerplus.android.fbreader.MenuData");

		test.project("https://github.com/spring-projects/spring-security.git", "master")
				.atCommit("64938ebcfc2fc8cd9ccd6cf31dbcd8cdd0660aca").containsOnly(
						"Extract Method public createExpressionMessageMetadataSource(matcherToExpression LinkedHashMap<MessageMatcher<?>,String>, handler SecurityExpressionHandler<Message<Object>>) : MessageSecurityMetadataSource extracted from public createExpressionMessageMetadataSource(matcherToExpression LinkedHashMap<MessageMatcher<?>,String>) : MessageSecurityMetadataSource in class org.springframework.security.messaging.access.expression.ExpressionBasedMessageSecurityMetadataSourceFactory");

		test.project("https://github.com/plutext/docx4j.git", "master")
				.atCommit("e29924b33ec0c0298ba4fc3f7a8c218c8e6cfa0c").containsOnly(
						"Extract Method public save(outStream OutputStream, flags int, password String) : void extracted from public save(outStream OutputStream, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage",
						"Extract Method public save(outFile File, flags int, password String) : void extracted from public save(outFile File, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("1b21ba4bcea986988d4bbd578e3bb9a20ec69606").containsOnly(
						"Extract Method private privacyStringForValue(value int) : String extracted from public onPreferenceChange(preference Preference, newValue Object) : boolean in class org.wordpress.android.ui.prefs.SiteSettingsFragment",
						"Extract Method private changeEditTextPreferenceValue(pref EditTextPreference, newValue String) : void extracted from public onPreferenceChange(preference Preference, newValue Object) : boolean in class org.wordpress.android.ui.prefs.SiteSettingsFragment");

		test.project("https://github.com/robovm/robovm.git", "master")
				.atCommit("7837d0baf1aef45340eec699516a8c3a22aeb553").containsOnly(
						"Extract Method private signFrameworks(appDir File, getTaskAllow boolean) : void extracted from protected prepareLaunch(appDir File) : void in class org.robovm.compiler.target.ios.IOSTarget");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("af618666043f21b3db7e6a1be2aa225ae0f432b4").containsOnly(
						"Extract Method private isNavigationBlocked(renderer GutterIconRenderer, project Project) : boolean extracted from public mouseReleased(e MouseEvent) : void in class com.intellij.openapi.editor.impl.EditorGutterComponentImpl");

		test.project("https://github.com/droolsjbpm/drools.git", "master")
				.atCommit("7ffc62aa554f5884064b81ee80078e35e3833006").containsOnly(
						"Extract Method protected addInterceptor(interceptor Interceptor, store boolean) : void extracted from public addInterceptor(interceptor Interceptor) : void in class org.drools.persistence.SingleSessionCommandService");

		test.project("https://github.com/Activiti/Activiti.git", "master")
				.atCommit("a70ca1d9ad2ea07b19c5e1f9540c809d7a12d3fb").containsOnly(
						"Extract Method protected flushPersistentObjects(persistentObjectClass Class<? extends PersistentObject>, persistentObjectsToInsert List<PersistentObject>) : void extracted from protected flushInserts() : void in class org.activiti.engine.impl.db.DbSqlSession");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("106d1d51754f454fa673976665e41f463316e084").containsOnly(
						"Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("8184a32a019b2ed956e8f24c18cb49a266af47bf").containsOnly(
						"Extract Method private generateSingleCopyFilesBuildPhase(target PBXNativeTarget, destinationSpec CopyFilePhaseDestinationSpec, targetNodes Iterable<TargetNode<?>>) : void extracted from private generateCopyFilesBuildPhases(target PBXNativeTarget, copiedNodes Iterable<TargetNode<?>>) : void in class com.facebook.buck.apple.ProjectGenerator");

		test.project("https://github.com/linkedin/rest.li.git", "master")
				.atCommit("ec5ea36faa3dd74585bb339beabdba6149ed63be").containsOnly(
						"Extract Method private buildErrorResponse(result RestLiServiceException, errorResponseFormat ErrorResponseFormat) : ErrorResponse extracted from public buildErrorResponse(result RestLiServiceException) : ErrorResponse in class com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("999e0e3616525d77cf46c5865f9981b5a6b5609a").containsOnly(
						"Extract Method private hepPlan(basePlan RelNode, followPlanChanges boolean, mdProvider RelMetadataProvider, order HepMatchOrder, rules RelOptRule[]) : RelNode extracted from private hepPlan(basePlan RelNode, followPlanChanges boolean, mdProvider RelMetadataProvider, rules RelOptRule[]) : RelNode in class org.apache.hadoop.hive.ql.parse.CalcitePlanner.CalcitePlannerAction");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("9f7de200c9aef900596b09327a52d33241a68d9c").containsOnly(
						"Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("a9379ee529ed87e28c0736c3c6657dcd6a0680e4").containsOnly(
						"Extract Method private toCanonicalPath(path String, separatorChar char, removeLastSlash boolean, resolveSymlinksIfNecessary boolean) : String extracted from private toCanonicalPath(path String, separatorChar char, removeLastSlash boolean) : String in class com.intellij.openapi.util.io.FileUtil");

		test.project("https://github.com/AdoptOpenJDK/jitwatch.git", "master")
				.atCommit("3b1f4e56fea289860b31ef83ccfe96a3a003cc8b").containsOnly(
						"Extract Method private visitTagParse(tag Tag, parseDictionary IParseDictionary) : void extracted from public visitTag(tag Tag, parseDictionary IParseDictionary) : void in class org.adoptopenjdk.jitwatch.model.bytecode.BytecodeAnnotationBuilder",
						"Extract Method private visitTagEliminateAllocation(tag Tag, parseDictionary IParseDictionary) : void extracted from public visitTag(tag Tag, parseDictionary IParseDictionary) : void in class org.adoptopenjdk.jitwatch.model.bytecode.BytecodeAnnotationBuilder");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("52cfd39ecba349c4d8e2c46eac76ed4d75b7ebae").containsOnly(
						"Extract Method private createSymLinkSdks(sdks Iterable<String>, root Path, version String) : void extracted from private createSymLinkIosSdks(root Path, version String) : void in class com.facebook.buck.apple.AppleSdkDiscoveryTest");

		test.project("https://github.com/vaadin/vaadin.git", "master")
				.atCommit("0f9d0b0bf1cd5fb58f47f22bd6d52a9fac31c530").containsOnly(
						"Extract Method private getVisibleFrozenColumnCount() : int extracted from private updateFrozenColumns() : void in class com.vaadin.client.widgets.Grid",
						"Extract Method protected createGrid(container PersonContainer) : Grid extracted from protected setup(request VaadinRequest) : void in class com.vaadin.tests.components.grid.GridEditorUI");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("7c59f2a4f9b03a9e48ca15554291a03477aa19c1").containsOnly(
						"Extract Method public addJarsToRoots(jarPaths List<String>, libraryName String, module Module, location PsiElement) : void extracted from public addJarToRoots(libPath String, module Module, location PsiElement) : void in class com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("10f769a60c7c7b73982e978959d381df487bbe2d").containsOnly(
						"Extract Method public getLibraryPaths() : List<String> extracted from public getLibraryPath() : String in class com.intellij.execution.junit.JUnit4Framework");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("619a6012da868d0d42d9628460f2264effe9bdba").containsOnly(
						"Extract Method private fillWithScopeExpansion(elements Set<Object>, pattern String) : boolean extracted from public computeInReadAction(indicator ProgressIndicator) : void in class com.intellij.ide.util.gotoByName.ChooseByNameBase.CalcElementsThread");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("e1f0dbc2f09541fc64ce88ee22d8f8f4648004fe").containsOnly(
						"Extract Method public resolveAndDownloadImpl(project Project, coord String, attachJavaDoc boolean, attachSources boolean, copyTo String, repositories List<MavenRepositoryInfo>) : List<OrderRoot> extracted from public resolveAndDownload(project Project, coord String, attachJavaDoc boolean, attachSources boolean, copyTo String, repositories List<MavenRepositoryInfo>) : NewLibraryConfiguration in class org.jetbrains.idea.maven.utils.library.RepositoryAttachHandler");

		test.project("https://github.com/jMonkeyEngine/jmonkeyengine.git", "master")
				.atCommit("5989711f7315abe4c3da0f1516a3eb3a81da6716").containsOnly(
						"Extract Method protected movePanel(xoffset int, yoffset int) : void extracted from public mouseDragged(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel",
						"Extract Method protected saveLocation() : void extracted from public mousePressed(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel");
		// TBD
		// method signature changed
		// Extract Method private doSelect(selectable Selectable, multi boolean)
		// : Selectable extracted from public select(key String) : Selectable in
		// class com.jme3.gde.materialdefinition.editor.Diagram
		// https://github.com/jMonkeyEngine/jmonkeyengine/commit/5989711f7315abe4c3da0f1516a3eb3a81da6716#diff-fac57d828f8b8ffbc1f94d953107a9d3L384

		test.project("https://github.com/facebook/presto.git", "master")
				.atCommit("8b1f5ce432bd6f579c646705d79ff0c4690495ae").containsOnly(
						"Extract Method public checkArrayIndex(index long) : void extracted from public readBlockAndCheckIndex(array Slice, index long) : Block in class com.facebook.presto.operator.scalar.ArraySubscriptOperator");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("30c4ae09745d6062077925a54f27205b7401d8df").containsOnly(
						"Extract Method private renderConnection() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderThread() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderOperationService() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderEvents() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderNativeMemory() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderHeap() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderClient() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderPhysicalMemory() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderProcessors() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method package getConnectionAddress() : Object extracted from public close(t Throwable) : void in class com.hazelcast.nio.tcp.TcpIpConnection",
						"Extract Method private renderSwap() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderCluster() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderExecutors() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderProxy() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
						"Extract Method private renderGc() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics");

		test.project("https://github.com/jersey/jersey.git", "master")
				.atCommit("d57b1401f874f96a53f1ec1c0f8a6089ae66a4ce").containsOnly(
						"Extract Method public _testParamValidatedResourceNoParam(target WebTarget) : void extracted from public testParamValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testParamValidatedResourceParamProvided(target WebTarget) : void extracted from public testParamValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testPropertyValidatedResourceParamProvided(target WebTarget) : void extracted from public testPropertyValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testOldFashionedResourceNoParam(target WebTarget) : void extracted from public testOldFashionedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testPropertyValidatedResourceNoParam(target WebTarget) : void extracted from public testPropertyValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testFieldValidatedResourceNoParam(target WebTarget) : void extracted from public testFieldValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testOldFashionedResourceParamProvided(target WebTarget) : void extracted from public testOldFashionedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
						"Extract Method public _testFieldValidatedResourceParamProvided(target WebTarget) : void extracted from public testFieldValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest");

		test.project("https://github.com/deeplearning4j/deeplearning4j.git", "master")
				.atCommit("c50064efb325b0c94cc62788b4c8935b7c39ac40").containsOnly(
						"Extract Method private getOptimizer(oa OptimizationAlgorithm, conf NeuralNetConfiguration, m Model) : ConvexOptimizer extracted from public testSphereFnOptHelper(oa OptimizationAlgorithm, numLineSearchIter int, nDimensions int) : void in class org.deeplearning4j.optimize.solver.TestOptimizers");

		test.project("https://github.com/Graylog2/graylog2-server.git", "master")
				.atCommit("2d98ae165ea43e9a1ac6a905d6094f077abb2e55").containsOnly(
						"Extract Method private postProcessMessage(raw RawMessage, codec Codec, inputIdOnCurrentNode String, baseMetricName String, message Message, decodeTime long) : Message extracted from private processMessage(raw RawMessage) : Message in class org.graylog2.shared.buffers.processors.DecodingProcessor",
						"Extract Method private dispatchMessage(msg Message) : void extracted from public onEvent(event MessageEvent) : void in class org.graylog2.shared.buffers.processors.ProcessBufferProcessor");

		test.project("https://github.com/cgeo/cgeo.git", "master").atCommit("c142b8ca3e9f9467931987ee16805cf53e6bc5d2")
				.containsOnly(
						"Extract Method private getWaymarkingConnector() : IConnector extracted from public testCanHandle() : void in class cgeo.geocaching.connector.WaymarkingConnectorTest",
						"Extract Method private getWaymarkingConnector() : IConnector extracted from public testGetGeocodeFromUrl() : void in class cgeo.geocaching.connector.WaymarkingConnectorTest");

		test.project("https://github.com/netty/netty.git", "master")
				.atCommit("9f422ed0f44516bea8116ed7730203e4eb316252").containsOnly(
						"Extract Method private resetCtx() : void extracted from public windowUpdateAndFlushShouldTriggerWrite() : void in class io.netty.handler.codec.http2.DefaultHttp2RemoteFlowControllerTest",
						"Extract Method private initConnectionAndController() : void extracted from public setup() : void in class io.netty.handler.codec.http2.DefaultHttp2RemoteFlowControllerTest");

		test.project("https://github.com/liferay/liferay-plugins.git", "master")
				.atCommit("d99695841fa675ea9150602b1132f037093e867d").containsOnly(
						"Extract Method protected getGetOAuthRequest(portletRequest PortletRequest, portletResponse PortletResponse) : OAuthRequest extracted from protected remoteRender(renderRequest RenderRequest, renderResponse RenderResponse) : void in class com.liferay.marketplace.store.portlet.RemoteMVCPortlet");

		test.project("https://github.com/plutext/docx4j.git", "master")
				.atCommit("59b8e89e61432d1d8f25cb003b62b3ac004d1b6f").containsOnly(
						"Extract Method private setProtectionPassword(password String, hashAlgo HashAlgorithm) : void extracted from public setEnforcementEditValue(editValue STDocProtect, password String, hashAlgo HashAlgorithm) : void in class org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart");

		test.project("https://github.com/datastax/java-driver.git", "master")
				.atCommit("14abb6919a99a0d6d500198dd2e30c83b1bb6709").containsOnly(
						"Extract Method private validateParameters() : void extracted from public prepare(manager MappingManager, ps PreparedStatement) : void in class com.datastax.driver.mapping.MethodMapper");

		test.project("https://github.com/BuildCraft/BuildCraft.git", "master")
				.atCommit("a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0").containsOnly(
						"Extract Method private getAvailableRequests(station DockingStation) : Collection<StackRequest> extracted from private getOrderFromRequestingStation(station DockingStation, take boolean) : StackRequest in class buildcraft.robotics.ai.AIRobotSearchStackRequest",
						"Extract Method private releaseCurrentRequest() : void extracted from public delegateAIEnded(ai AIRobot) : void in class buildcraft.robotics.boards.BoardRobotDelivery");

		test.project("https://github.com/apache/camel.git", "master")
				.atCommit("ab1d1dd78fe53edb50c4ede447e4ac5a55ee2ac9").containsOnly(
						"Extract Method public createExchange(message Message, endpoint Endpoint, keyFormatStrategy KeyFormatStrategy) : Exchange extracted from public createExchange(message Message, endpoint Endpoint) : Exchange in class org.apache.camel.component.sjms.jms.JmsMessageHelper");

		test.project("https://github.com/xetorthio/jedis.git", "master")
				.atCommit("d4b4aecbc69bbd04ba87c4e32a52cff3d129906a").containsOnly(
						"Extract Method private poolInactive() : boolean extracted from public getNumWaiters() : int in class redis.clients.util.Pool",
						"Extract Method private poolInactive() : boolean extracted from public getNumIdle() : int in class redis.clients.util.Pool",
						"Extract Method private poolInactive() : boolean extracted from public getNumActive() : int in class redis.clients.util.Pool");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("dc199688d69416da58b370ca2aa728e935fc8e0d").containsOnly(
						"Extract Method private getSortedIndexUpdates(descriptor IndexDescriptor) : TreeMap<DefinedProperty,DiffSets<Long>> extracted from private getIndexUpdatesForPrefix(descriptor IndexDescriptor, prefix String) : ReadableDiffSets<Long> in class org.neo4j.kernel.impl.api.state.TxState");
		// TBD
		// Extract Method private
		// filterIndexStateChangesForRangeSeekByNumber(state
		// KernelStatement,index IndexDescriptor, lower Number, includeLower
		// boolean, upper Number, includeUpper boolean, nodeIds
		// PrimitiveLongIterator) : PrimitiveLongIterator extracted from public
		// nodesGetFromIndexRangeSeekByNumber(state KernelStatement, index
		// IndexDescriptor, lower Number, includeLower boolean, upper Number,
		// includeUpper boolean) : PrimitiveLongIterator in class
		// org.neo4j.kernel.impl.api.StateHandlingStatementOperations

		test.project("https://github.com/apache/camel.git", "master")
				.atCommit("14a7dd79148f9306dcd2f748b56fd6550e9406ab").containsOnly(
						"Extract Method private readClassFromCamelResource(file File, buffer StringBuilder, buildContext BuildContext) : String extracted from public prepareDataFormat(log Log, project MavenProject, projectHelper MavenProjectHelper, dataFormatOutDir File, schemaOutDir File, buildContext BuildContext) : void in class org.apache.camel.maven.packaging.PackageDataFormatMojo",
						"Extract Method private readClassFromCamelResource(file File, buffer StringBuilder, buildContext BuildContext) : String extracted from public prepareLanguage(log Log, project MavenProject, projectHelper MavenProjectHelper, languageOutDir File, schemaOutDir File, buildContext BuildContext) : void in class org.apache.camel.maven.packaging.PackageLanguageMojo");

		test.project("https://github.com/siacs/Conversations.git", "master")
				.atCommit("e6cb12dfe414497b4317820497985c110cb81864").containsOnly(
						"Extract Method public getItemViewType(message Message) : int extracted from public getItemViewType(position int) : int in class eu.siacs.conversations.ui.adapter.MessageAdapter");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("1b70adbfd49e00194c4c1170ef65e8114d7a2e46").containsOnly(
						"Extract Method private getFieldInitializerNullness(expression PsiExpression) : Nullness extracted from private calcInherentNullability() : Nullness in class com.intellij.codeInspection.dataFlow.value.DfaVariableValue");

		test.project("https://github.com/spotify/helios.git", "master")
				.atCommit("3ffd70929c08be5cf14f156189e8050969caa87e").containsOnly(
						"Extract Method private isRolloutTimedOut(deploymentGroup DeploymentGroup, client ZooKeeperClient) : boolean extracted from private rollingUpdateAwaitRunning(deploymentGroup DeploymentGroup, host String) : RollingUpdateTaskResult in class com.spotify.helios.master.ZooKeeperMasterModel");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("b70f7ea0ce27b5defa0a7773d448732364e7aee0").containsOnly(
						"Extract Method private listSnapshots() : List<File> extracted from public getSnapshotDetails() : Map<String,Pair<Long,Long>> in class org.apache.cassandra.db.Directories");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("c53c6f45988db869d56abe3b1d831ff775f4fa73").containsOnly(
						"Extract Method private statsForOneColumnFromProtoBuf(partitionColumnStats ColumnStatistics, proto ColumnStats) : ColumnStatisticsObj extracted from package deserializeStatsForOneColumn(partitionColumnStats ColumnStatistics, bytes byte[]) : ColumnStatisticsObj in class org.apache.hadoop.hive.metastore.hbase.HBaseUtils",
						"Extract Method private protoBufStatsForOneColumn(partitionColumnStats ColumnStatistics, colStats ColumnStatisticsObj) : ColumnStats extracted from package serializeStatsForOneColumn(partitionColumnStats ColumnStatistics, colStats ColumnStatisticsObj) : byte[] in class org.apache.hadoop.hive.metastore.hbase.HBaseUtils");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("6ff3fe00d7ffe04dbe0904b8bad98285b6988d6d").containsOnly(
						"Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder",
						"Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder",
						"Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl",
						"Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("2b0a8f6bdac621badabcb9921c077260f2470c26").containsOnly(
						"Extract Method public deleteRowAt(metadata CFMetaData, timestamp long, localDeletionTime int, key Object, clusteringValues Object[]) : Mutation extracted from public deleteRow(metadata CFMetaData, timestamp long, key Object, clusteringValues Object[]) : Mutation in class org.apache.cassandra.db.RowUpdateBuilder");

		test.project("https://github.com/dropwizard/metrics.git", "master")
				.atCommit("4c6ab3d77cc67c7a91155d884077520dcf1509c6").containsOnly(
						"Extract Method private closeGraphiteConnection() : void extracted from public report(gauges SortedMap<String,Gauge>, counters SortedMap<String,Counter>, histograms SortedMap<String,Histogram>, meters SortedMap<String,Meter>, timers SortedMap<String,Timer>) : void in class com.codahale.metrics.graphite.GraphiteReporter");

		test.project("https://github.com/gradle/gradle.git", "master")
				.atCommit("681dc6346ce3cf5be5c5985faad120a18949cee0").containsOnly(
						"Extract Method private createPlatformToolProvider(targetPlatform NativePlatformInternal) : PlatformToolProvider extracted from public select(targetPlatform NativePlatformInternal) : PlatformToolProvider in class org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("9fbf6b852bd9766060307aff960fb597d55e24d3").containsOnly(
						"Extract Method public clear() : void extracted from public close() : void in class com.intellij.util.io.CachingEnumerator");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("5790b4a44ba85e7e8ece64613d9e6a1b737a6cde").containsOnly(
						"Extract Method protected compose(argDataTypes DataType[], protocolVersion int, argIndex int, value ByteBuffer) : Object extracted from protected compose(protocolVersion int, argIndex int, value ByteBuffer) : Object in class org.apache.cassandra.cql3.functions.UDFunction",
						"Extract Method protected decompose(dataType DataType, protocolVersion int, value Object) : ByteBuffer extracted from protected decompose(protocolVersion int, value Object) : ByteBuffer in class org.apache.cassandra.cql3.functions.UDFunction");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("a1525ac9a0bb8f727167a8be94c81a3415128ef4").containsOnly(
						"Extract Method private getAllPathsWork(workingDir Path) : ImmutableBiMap<Path,Path> extracted from private getAllPaths(workingDir Optional<Path>) : ImmutableBiMap<Path,Path> in class com.facebook.buck.cxx.DebugPathSanitizer");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("db024f5ec3e9611ddf8103bdc4c3817c704f7b27").containsOnly(
						"Extract Method public getTargetsAndDependencies(params CommandRunnerParams, argumentsFormattedAsBuildTargets List<String>, showTransitive boolean, showTests boolean, enableProfiling boolean) : Multimap<BuildTarget,BuildTarget> extracted from public runWithoutHelp(params CommandRunnerParams) : int in class com.facebook.buck.cli.AuditDependenciesCommand");

		test.project("https://github.com/JetBrains/intellij-plugins.git", "master")
				.atCommit("0df7cb00757fe0d4fac8d8b0d5fc46af95feb238").containsOnly(
						"Extract Method public findPsiFile(project Project, path String) : PsiFile extracted from private getElementForNavigationTarget(project Project, target PluginNavigationTarget) : PsiElement in class com.jetbrains.lang.dart.resolve.DartResolver");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("7dd55014f9840ce03867bb175cf37a4c151dc806").containsOnly(
						"Extract Method private createConfigurable(ep ConfigurableEP<T>, log boolean) : T extracted from public wrapConfigurable(ep ConfigurableEP<T>) : T in class com.intellij.openapi.options.ex.ConfigurableWrapper");

		test.project("https://github.com/google/guava.git", "master")
				.atCommit("31fc19200207ccadc45328037d8a2a62b617c029").containsOnly(
						"Extract Method public tryParse(string String, radix int) : Long extracted from public tryParse(string String) : Long in class com.google.common.primitives.Longs");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("92e98858e742bbb669ccbf790a71a618c581df21").containsOnly(
						"Extract Method public use(ctx ParserRuleContext, sql String) : Integer extracted from public use(ctx Use_stmtContext) : Integer in class org.apache.hive.hplsql.Stmt");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("573a1d115b86abbe3fb53ff930464d7d8fd95600").containsOnly(
						"Extract Method public incrementDroppedMessages(verb Verb, isCrossNodeTimeout boolean) : void extracted from public incrementDroppedMessages(verb Verb) : void in class org.apache.cassandra.net.MessagingService",
						"Extract Method package getDroppedMessagesLogs() : List<String> extracted from private logDroppedMessages() : void in class org.apache.cassandra.net.MessagingService");

		test.project("https://github.com/checkstyle/checkstyle.git", "master")
				.atCommit("2f7481ee4e20ae785298c31ec2f979752dd7eb03").containsOnly(
						"Extract Method private checkInterfaceModifiers(ast DetailAST) : void extracted from public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.modifier.RedundantModifierCheck");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("021d17c8234904dcb1d54596662352395927fe7b").containsOnly(
						"Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode1(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore",
						"Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode2(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore");

		test.project("https://github.com/skylot/jadx.git", "master")
				.atCommit("2d8d4164830631d3125575f055b417c5addaa22f").containsOnly(
						"Extract Method public getJavaNodeAtPosition(line int, offset int) : JavaNode extracted from public getDefinitionPosition(line int, offset int) : CodePosition in class jadx.api.JavaClass",
						"Extract Method public getDefinitionPosition(javaNode JavaNode) : CodePosition extracted from public getDefinitionPosition(line int, offset int) : CodePosition in class jadx.api.JavaClass");

		test.project("https://github.com/wildfly/wildfly.git", "master")
				.atCommit("d7675fb0b19d3d22978e79954f441eeefd74a3b2").containsOnly(
						"Extract Method private handleExcludeMethods(componentDescription EJBComponentDescription, excludeList ExcludeListMetaData) : void extracted from protected handleDeploymentDescriptor(deploymentUnit DeploymentUnit, deploymentReflectionIndex DeploymentReflectionIndex, componentClass Class<?>, componentDescription EJBComponentDescription) : void in class org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor",
						"Extract Method private handleMethodPermissions(componentDescription EJBComponentDescription, methodPermissions MethodPermissionsMetaData) : void extracted from protected handleDeploymentDescriptor(deploymentUnit DeploymentUnit, deploymentReflectionIndex DeploymentReflectionIndex, componentClass Class<?>, componentDescription EJBComponentDescription) : void in class org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor");

		test.project("https://github.com/openhab/openhab.git", "master")
				.atCommit("f25fa3ae35e4a60a2b7f79a88f14d46ce6cebf55").containsOnly(
						"Extract Method private initTimeMap() : Map<String,Integer> extracted from public parameters() : Collection<Object[]> in class org.openhab.core.library.types.DateTimeTypeTest");

		test.project("https://github.com/selendroid/selendroid.git", "master")
				.atCommit("e4a309c160285708f917ea23238573da3b677f7f").containsOnly(
						"Extract Method protected toByteArray(image BufferedImage) : byte[] extracted from public takeScreenshot() : byte[] in class io.selendroid.standalone.android.impl.AbstractDevice");

		test.project("https://github.com/jOOQ/jOOQ.git", "master").atCommit("227254cf769f3e821ed1b2ef2d88c4ec6b20adea")
				.containsOnly(
						"Extract Method public formatCSV(stream OutputStream, header boolean) : void extracted from public formatCSV(stream OutputStream) : void in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(writer Writer, header boolean) : void extracted from public formatCSV(writer Writer) : void in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(header boolean) : String extracted from public formatCSV() : String in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(header boolean, delimiter char, nullString String) : String extracted from public formatCSV(delimiter char, nullString String) : String in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(header boolean, delimiter char) : String extracted from public formatCSV(delimiter char) : String in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(writer Writer, header boolean, delimiter char) : void extracted from public formatCSV(writer Writer, delimiter char) : void in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(stream OutputStream, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(stream OutputStream, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(writer Writer, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(writer Writer, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
						"Extract Method public formatCSV(stream OutputStream, header boolean, delimiter char) : void extracted from public formatCSV(stream OutputStream, delimiter char) : void in class org.jooq.impl.ResultImpl");

		test.project("https://github.com/eclipse/vert.x.git", "master")
				.atCommit("0ef66582ffaba9a8df1cad846880df2074d34505").containsOnly(
						"Extract Method private init() : void extracted from public ClientOptionsBase(json JsonObject) in class io.vertx.core.net.ClientOptionsBase",
						"Extract Method private init() : void extracted from public TCPSSLOptions(json JsonObject) in class io.vertx.core.net.TCPSSLOptions",
						"Extract Method private init() : void extracted from public NetClientOptions(json JsonObject) in class io.vertx.core.net.NetClientOptions",
						"Extract Method private init() : void extracted from public NetClientOptions() in class io.vertx.core.net.NetClientOptions",
						"Extract Method private init() : void extracted from public NetServerOptions(json JsonObject) in class io.vertx.core.net.NetServerOptions",
						"Extract Method private init() : void extracted from public NetServerOptions() in class io.vertx.core.net.NetServerOptions",
						"Extract Method private init() : void extracted from public HttpClientOptions(json JsonObject) in class io.vertx.core.http.HttpClientOptions",
						"Extract Method private init() : void extracted from public HttpClientOptions() in class io.vertx.core.http.HttpClientOptions",
						"Extract Method private init() : void extracted from public HttpServerOptions(json JsonObject) in class io.vertx.core.http.HttpServerOptions",
						"Extract Method private init() : void extracted from public HttpServerOptions() in class io.vertx.core.http.HttpServerOptions",
						"Extract Method private init() : void extracted from public TCPSSLOptions() in class io.vertx.core.net.TCPSSLOptions",
						"Extract Method private init() : void extracted from public NetClientOptions() in class io.vertx.core.net.NetClientOptions",
						"Extract Method private init() : void extracted from public HttpClientOptions() in class io.vertx.core.http.HttpClientOptions",
						"Extract Method private init() : void extracted from public NetServerOptions() in class io.vertx.core.net.NetServerOptions",
						"Extract Method private init() : void extracted from public HttpServerOptions() in class io.vertx.core.http.HttpServerOptions",
						"Extract Method private init() : void extracted from public ClientOptionsBase() in class io.vertx.core.net.ClientOptionsBase");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6").containsOnly(
						"Extract Method private restoreLocalInfoFromTableInfo() : void extracted from package setTableInfo(thatTableInfo HCatTableInfo) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo",
						"Extract Method private restoreLocalInfoFromTableInfo() : void extracted from private writeObject(oos ObjectOutputStream) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo",
						"Extract Method private dedupWithTableInfo() : void extracted from private writeObject(oos ObjectOutputStream) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("c1359c843bd03a694f846c8140e24ed646bbb913").containsOnly(
						"Extract Method private createSchema(config Configuration, ddl String, sitesPerHost int, hostCount int, replication int) : void extracted from public testBasicCreateIndex() : void and public testCreateDropIndexonView() : void in class org.voltdb.TestAdhocCreateDropIndex");

		test.project("https://github.com/google/closure-compiler.git", "master")
				.atCommit("545a7d027b4c55c116dc52d9cd8121fbb09777f0").containsOnly(
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoResolvedType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.UnionType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.RecordType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.TemplatizedType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.PrototypeObjectType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.FunctionType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.ProxyObjectType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.EnumElementType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoObjectType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.UnknownType",
						"Extract Method package isSubtype(typeA ObjectType, typeB RecordType, implicitImplCache ImplCache) : boolean extracted from package isSubtype(typeA ObjectType, typeB RecordType) : boolean in class com.google.javascript.rhino.jstype.RecordType",
						"Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.EnumType",
						"Extract Method protected isSubtype(other JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(other JSType) : boolean in class com.google.javascript.rhino.jstype.ArrowType");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("204bf49cba03fe5d581a35ff82dd22587a681f46").containsOnly(
						"Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterSlave(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl",
						"Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterMaster(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl",
						"Extract Method private createConfig() : Config extracted from private testWaitingInvocations_whenNodeSplitFromCluster(splitAction SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest",
						"Extract Method private createConfig() : Config extracted from private testWaitNotifyService_whenNodeSplitFromCluster(action SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("e84e96ff5c2bdc48cea7f75fd794506159c4e1f7").containsOnly(
						"Extract Method private createCodecModel(methodElement ExecutableElement, lang Lang) : CodecModel extracted from public generateCodec(methodElement ExecutableElement, lang Lang) : void in class com.hazelcast.client.protocol.generator.CodecCodeGenerator");

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
				.atCommit("12602650ce99f34cb530fc24266c23e39733b0bb").containsOnly(
						"Extract Method public makeInventoryMessage(payloadBytes byte[], length int) : InventoryMessage extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer", // use
																																																																									// the
																																																																									// combination
																																																																									// of
																																																																									// fields
																																																																									// and
																																																																									// parameters
																																																																									// to
																																																																									// detect
																																																																									// extract
																																																																									// or...
						"Extract Method public makeAddressMessage(payloadBytes byte[], length int) : AddressMessage extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
						"Extract Method protected parseTransactions(transactionsOffset int) : void extracted from protected parseTransactions() : void in class org.bitcoinj.core.Block",
						"Extract Method public makeTransaction(payloadBytes byte[], offset int, length int, hash byte[]) : Transaction extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
						"Extract Method public makeBlock(payloadBytes byte[], length int) : Block extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
						"Extract Method public makeBloomFilter(payloadBytes byte[]) : Message extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
						"Extract Method public makeAlertMessage(payloadBytes byte[]) : Message extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer");

		test.project("https://github.com/alibaba/druid.git", "master")
				.atCommit("87f3f8144b7a6cb57b6e21cd3753d09ecde0d88f").containsOnly(
						"Extract Method protected printJoinType(joinType JoinType) : void extracted from public visit(x SQLJoinTableSource) : boolean in class com.alibaba.druid.sql.visitor.SQLASTOutputVisitor",
						"Extract Method protected printJoinType(joinType JoinType) : void extracted from public visit(x SQLJoinTableSource) : boolean in class com.alibaba.druid.sql.dialect.odps.visitor.OdpsOutputVisitor");

		test.project("https://github.com/eclipse/jetty.project.git", "master")
				.atCommit("837d1a74bb7d694220644a2539c4440ce55462cf").containsOnly(
						"Extract Method private testTransparentProxyWithQuery(proxyToContext String, prefix String, target String) : void extracted from public testTransparentProxyWithQuery() : void in class org.eclipse.jetty.proxy.ProxyServletTest");

		test.project("https://github.com/clojure/clojure.git", "master")
				.atCommit("309c03055b06525c275b278542c881019424760e").containsOnly(
						"Extract Method package sigTag(argcount int, v Var) : Object extracted from public InvokeExpr(source String, line int, column int, tag Symbol, fexpr Expr, args IPersistentVector, tailPosition boolean) in class clojure.lang.Compiler.InvokeExpr");

		test.project("https://github.com/osmandapp/Osmand.git", "master")
				.atCommit("e95aa8ab32a0334b9c941799060fd601297d05e4").containsOnly(
						"Extract Method public showItemPopupOptionsMenu(point FavouritePoint, activity Activity, view View) : void extracted from public onListItemClick(l ListView, v View, position int, id long) : void in class net.osmand.plus.activities.FavoritesListFragment",
						"Extract Method public showItemPopupOptionsMenu(point FavouritePoint, view View) : void extracted from public onChildClick(parent ExpandableListView, v View, groupPosition int, childPosition int, id long) : boolean in class net.osmand.plus.activities.FavoritesTreeFragment");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("ecd0ad5ab99b8d14f28881cf4f49ec01f2221776").containsOnly(
						"Extract Method private computeRuleFlags(source CxxSource) : ImmutableList<String> extracted from public createPreprocessBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
						"Extract Method private computePlatformCompilerFlags(pic PicType, source CxxSource) : ImmutableList<String> extracted from public createPreprocessAndCompileBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType, strategy CxxPreprocessMode) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
						"Extract Method private computePlatformFlags(pic PicType, source CxxSource) : ImmutableList<String> extracted from public createPreprocessBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
						"Extract Method private computeRuleCompilerFlags(source CxxSource) : ImmutableList<String> extracted from public createPreprocessAndCompileBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType, strategy CxxPreprocessMode) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory");

		test.project("https://github.com/ReactiveX/RxJava.git", "master")
				.atCommit("8ad226067434cd39ce493b336bd0659778625959").containsOnly(
						"Extract Method private awaitForComplete(latch CountDownLatch, subscription Subscription) : void extracted from private blockForSingle(observable Observable<? extends T>) : T in class rx.observables.BlockingObservable",
						"Extract Method private awaitForComplete(latch CountDownLatch, subscription Subscription) : void extracted from public forEach(onNext Action1<? super T>) : void in class rx.observables.BlockingObservable");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("3bdcaa336a6e6a9727c333b433bb9f5d3afc0fb1").containsOnly(
						"Extract Method public dumpMemtable() : void extracted from public truncateBlocking() : void in class org.apache.cassandra.db.ColumnFamilyStore");

		test.project("https://github.com/netty/netty.git", "master")
				.atCommit("9d347ffb91f34933edb7b1124f6b70c3fc52e220").containsOnly(
						"Extract Method private expand() : void extracted from public append(c char) : AppendableCharSequence in class io.netty.util.internal.AppendableCharSequence");

		test.project("https://github.com/restlet/restlet-framework-java.git", "master")
				.atCommit("7ffe37983e2f09637b0c84d526a2f824de652de4").containsOnly(
						"Extract Method private fillRepresentation(model Model, name String, contract Contract) : void extracted from private fillRepresentations(swagger Swagger, contract Contract) : void in class org.restlet.ext.apispark.internal.conversion.swagger.v2_0.Swagger2Reader");

		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("ce4b0e22659c16ae83d421f9621fd3e922750764").containsOnly(
						"Extract Method protected renameModels(oldName String, newName String) : void extracted from public rename(newName String) : void in class jetbrains.mps.project.AbstractModule");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("deb8e5ca64fcf633edbd89523af472da813b6772").containsOnly(
						"Extract Method private getNormalValue(r Random, magnitude double, min long, max long) : long extracted from private fillTable(client Client, tbl String) : void in class org.voltdb.regressionsuites.TestApproxCountDistinctSuite");

		test.project("https://github.com/brettwooldridge/HikariCP.git", "master")
				.atCommit("e19c6874431dc2c3046436c2ac249a0ab2ef3457").containsOnly(
						"Extract Method private closeOpenStatements() : void extracted from public close() : void in class com.zaxxer.hikari.proxy.ConnectionProxy");

		test.project("https://github.com/jOOQ/jOOQ.git", "master").atCommit("58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357")
				.containsOnly(
						"Extract Method private millis(temporal Temporal) : long extracted from public from(from Object) : U in class org.jooq.tools.Convert.ConvertAll");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("6c93f15f502f39dff99ecb01b56dcad7dddb0f0d").containsOnly(
						"Extract Method package getEnumerator(rType RType) : ResourceIdEnumerator extracted from public addIntResourceIfNotPresent(rType RType, name String) : void in class com.facebook.buck.android.aapt.AaptResourceCollector");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("0fa45e4a562fc2586b1ef06a88e9c186a0835316").containsOnly(
						"Extract Method private setupObjectStore(rdbms RawStore, roles String[], dbNames String[], tokenIds String[], tokens String[], masterKeys String[], now int) : void extracted from public doImport() : void in class org.apache.hadoop.hive.metastore.hbase.TestHBaseImport",
						"Extract Method private copyOneFunction(dbName String, funcName String) : void extracted from private copyFunctions() : void in class org.apache.hadoop.hive.metastore.hbase.HBaseImport");

		test.project("https://github.com/JetBrains/intellij-plugins.git", "master")
				.atCommit("83b3092c1ee11b70489732f9e69b8e01c2a966f0").containsOnly(
						"Extract Method private getShortErrorMessage(methodName String, filePath String, error RequestError) : String extracted from private logError(methodName String, filePath String, error RequestError) : void in class com.jetbrains.lang.dart.analyzer.DartAnalysisServerService");

		test.project("https://github.com/gwtproject/gwt.git", "master")
				.atCommit("892d1760c8e4c76c369cd5ec1eaed215d3a22c8a").containsOnly(
						"Extract Method public startRow(rowValue T) : TableRowBuilder extracted from public startRow() : TableRowBuilder in class com.google.gwt.user.cellview.client.AbstractCellTableBuilder");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("d47e58f9bbce9a816378e8a7930c1de67a864c29").containsOnly(
						"Extract Method public callProcedure(ic ImportContext, procCallback ProcedureCallback, proc String, fieldList Object[]) : boolean extracted from public callProcedure(ic ImportContext, proc String, fieldList Object[]) : boolean in class org.voltdb.ImportHandler");

		test.project("https://github.com/raphw/byte-buddy.git", "master")
				.atCommit("fd000ca2e78fce2f8aa11e6a81e4f23c2f1348e6").containsOnly(
						"Extract Method private invokeMethod(methodToken Token) : SpecialMethodInvocation extracted from public invokeSuper(methodToken Token) : SpecialMethodInvocation in class net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget");

		test.project("https://github.com/processing/processing.git", "master")
				.atCommit("f36b736cf1206dd1af794d6fb4cee967a3553b1f").containsOnly(
						"Extract Method private createDepthAndStencilBuffer(multisample boolean, depthBits int, stencilBits int, packed boolean) : void extracted from private createFBOLayer() : void in class processing.opengl.PGL");

		test.project("https://github.com/infinispan/infinispan.git", "master")
				.atCommit("043030723632627b0908dca6b24dae91d3dfd938").containsOnly(
						"Extract Method private rehashAwareIteration(complete AtomicBoolean, consumer Consumer<R>, supplier IteratorSupplier<R>, iteratorParallelDistribute boolean) : void extracted from package remoteIterator() : Iterator<R> in class org.infinispan.stream.impl.DistributedCacheStream",
						"Extract Method private ignoreRehashIteration(consumer Consumer<R>, supplier IteratorSupplier<R>, iteratorParallelDistribute boolean) : void extracted from package remoteIterator() : Iterator<R> in class org.infinispan.stream.impl.DistributedCacheStream",
						"Extract Method protected supplierForSegments(ch ConsistentHash, targetSegments Set, excludedKeys Set, primaryOwnerOnly boolean) : Supplier extracted from protected supplierForSegments(ch ConsistentHash, targetSegments Set, excludedKeys Set) : Supplier in class org.infinispan.stream.impl.AbstractCacheStream");

		test.project("https://github.com/MovingBlocks/Terasology.git", "master")
				.atCommit("8f63cc5c8edb8e740026447bc4827f8e8e6c34b1").containsOnly(
						"Extract Method private ensurePreviewUnloaded() : boolean extracted from public onClosed() : void in class org.terasology.rendering.nui.layers.mainMenu.PreviewWorldScreen");

		test.project("https://github.com/dropwizard/metrics.git", "master")
				.atCommit("2331fe19ea88a22de32f15375de8118226eaa1e6").containsOnly(
						"Extract Method private registerMetricsForModel(resourceModel ResourceModel) : void extracted from public onEvent(event ApplicationEvent) : void in class io.dropwizard.metrics.jersey2.InstrumentedResourceMethodApplicationListener");

		test.project("https://github.com/JoanZapata/android-iconify.git", "master")
				.atCommit("eb500cca282e39d01a9882e1d0a83186da6d1a26").containsOnly(
						"Extract Method private copy(inputStream InputStream, outputFile File) : void extracted from package resourceToFile(context Context, resourceName String) : File in class com.joanzapata.android.iconify.Utils");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("e37d577b6cfc2d3e11252cef87ab9ebba72e1d52").containsOnly(
						"Extract Method public assertUdfsEnabled(language String) : void extracted from public create(name FunctionName, argNames List<ColumnIdentifier>, argTypes List<AbstractType<?>>, returnType AbstractType<?>, calledOnNullInput boolean, language String, body String) : UDFunction in class org.apache.cassandra.cql3.functions.UDFunction");

		test.project("https://github.com/apache/pig.git", "master").atCommit("92dce401344a28ff966ad4cf3dd969a676852315")
				.containsOnly(
						"Extract Method public depthFirstSearchForFile(statusArray FileStatus[], fileSystem FileSystem, filter PathFilter) : Path extracted from public depthFirstSearchForFile(statusArray FileStatus[], fileSystem FileSystem) : Path in class org.apache.pig.impl.util.Utils");

		test.project("https://github.com/linkedin/rest.li.git", "master")
				.atCommit("54fa890a6af4ccf564fb481d3e1b6ad4d084de9e").containsOnly(
						"Extract Method public addResponseCompressionHeaders(responseCompressionOverride CompressionOption, req RestRequest) : RestRequest extracted from public onRestRequest(req RestRequest, requestContext RequestContext, wireAttrs Map<String,String>, nextFilter NextFilter<RestRequest,RestResponse>) : void in class com.linkedin.r2.filter.compression.ClientCompressionFilter",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompressionBetter(compressor Compressor) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testAcceptEncoding(acceptedEncoding String, contentEncoding String) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompressionWorse(compressor Compressor) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompatibleDefault(acceptEncoding String, contentEncoding String) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public test406Error(acceptContent String) : void in class com.linkedin.restli.examples.TestCompressionServer");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("1a2c1bcdc7267abec9b19d77726aedbb045d79a8").containsOnly(
						"Extract Method public minorWasTriggered(keyspace String, cf String) : boolean extracted from public testTriggerMinorCompaction() : void in class org.apache.cassandra.db.compaction.CompactionsCQLTest");

		test.project("https://github.com/spring-projects/spring-hateoas.git", "master")
				.atCommit("8bdc57ba8975d851fe91edc908761aacea624766").containsOnly(
						"Extract Method private assertCanWrite(converter GenericHttpMessageConverter<Object>, type Class<?>, expected boolean) : void extracted from public canWriteTypeIfAssignableToConfiguredType() : void in class org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverterUnitTest",
						"Extract Method private assertCanRead(converter GenericHttpMessageConverter<Object>, type Class<?>, expected boolean) : void extracted from public canReadTypeIfAssignableToConfiguredType() : void in class org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverterUnitTest");

		test.project("https://github.com/addthis/hydra.git", "master")
				.atCommit("664923815b5aeeba2025bfe1dc5a0cd1a02a80e2").containsOnly(
						"Extract Method public updateHits(state DataTreeNodeUpdater, path TreeDataParent) : boolean extracted from public updateChildData(state DataTreeNodeUpdater, path TreeDataParent) : void in class com.addthis.hydra.data.tree.concurrent.ConcurrentTreeNode");

		test.project("https://github.com/HdrHistogram/HdrHistogram.git", "master")
				.atCommit("0e65ac4da70c6ca5c67bb8418e67db914218042f").containsOnly(
						"Extract Method private getIntervalHistogram() : EncodableHistogram extracted from public run() : void in class org.HdrHistogram.HistogramLogProcessor");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("98ecc4cfd927f388645f6a6bc492c80868b1a11d").containsOnly(
						"Extract Method private getFileNamesToCreate() : Set<String> extracted from private createPropertiesFiles() : List<PsiFile> in class com.intellij.lang.properties.create.CreateResourceBundleDialogComponent");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("e0072aac53b3b88de787e7ca653c7e17f9499018").containsOnly(
						"Extract Method private unpackBytesHeader() : int extracted from public unpackBytes() : byte[] in class org.neo4j.packstream.PackStream.Unpacker");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("61215911ef28ca783c5106d7c01e74cf3000a866").containsOnly(
						"Extract Method public format(path String, pegRevision SVNRevision) : String extracted from public put(parameters List<String>, path String, pegRevision SVNRevision) : void in class org.jetbrains.idea.svn.commandLine.CommandUtil");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("153611deab149accd8aeaf03fd102c0b069bd322").containsOnly(
						"Extract Method public of(cells Cell[][], row int, column int, size byte) : Cell extracted from public of(row int, column int, size byte) : Cell in class com.android.internal.widget.LockPatternView.Cell",
						"Extract Method public stringToPattern(string String, size byte) : List<LockPatternView.Cell> extracted from public stringToPattern(string String) : List<LockPatternView.Cell> in class com.android.internal.widget.LockPatternUtils");

		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("61b5decd4a4e5e6bbdea99eb76f136ca10915b73").containsOnly(
						"Extract Method public startInsertMode(editorContext EditorContext, anchorCell EditorCell, insertBefore boolean) : void extracted from public insertNewChild(editorContext EditorContext, anchorCell EditorCell, insertBefore boolean) : void in class jetbrains.mps.nodeEditor.cellProviders.AbstractCellListHandler");

		test.project("https://github.com/ignatov/intellij-erlang.git", "master")
				.atCommit("e3b84c8753a21b1b15cfc9aa90b5e0c56d290f41").containsOnly(
						"Extract Method private collectFiles(module Module, onlyTestModules boolean, filesCollector Processor<VirtualFile>) : void extracted from private addErlangModules(module Module, onlyTestModules boolean, erlangModules Collection<ErlangFile>) : Collection<ErlangFile> in class org.intellij.erlang.utils.ErlangModulesUtil",
						"Extract Method private addSourceRoot(module Module, sourceRootName String, rootType JpsModuleSourceRootType<?>) : VirtualFile extracted from private addSourceRoot(module Module, sourceRootName String, isTestSourceRoot boolean) : VirtualFile in class org.intellij.erlang.compilation.ErlangCompilationTestBase");

		test.project("https://github.com/apache/hive.git", "master")
				.atCommit("8398fbf3dd0937a0a4a3d540977a95f97425f566").containsOnly(
						"Extract Method public closeSparkSession() : void extracted from public close() : void in class org.apache.hadoop.hive.ql.session.SessionState");

		test.project("https://github.com/spotify/helios.git", "master")
				.atCommit("cc02c00b8a92ef34d1a8bcdf44a45fb01a8dea6c").containsOnly(
						"Extract Method protected createJobRawOutput(job Job) : String extracted from protected createJob(job Job) : JobId in class com.spotify.helios.system.SystemTestBase");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("f1b8ae1c44e6ba46118c2f66eae1725259acdccc").containsOnly(
						"Extract Method public of(cells Cell[][], row int, column int, size byte) : Cell extracted from public of(row int, column int, size byte) : Cell in class com.android.internal.widget.LockPatternView.Cell",
						"Extract Method public stringToPattern(string String, size byte) : List<LockPatternView.Cell> extracted from public stringToPattern(string String) : List<LockPatternView.Cell> in class com.android.internal.widget.LockPatternUtils");

		test.project("https://github.com/spring-projects/spring-framework.git", "master")
				.atCommit("fffdd1e9e9dc887c3e8973147904d47d9fffbb47").containsOnly(
						"Extract Method private assertExistsAndReturn(content String) : Object extracted from public exists(content String) : void in class org.springframework.test.util.JsonPathExpectationsHelper");

		test.project("https://github.com/liferay/liferay-plugins.git", "master")
				.atCommit("720b0d2064ecc4403809e794075e9fe8cfa857f1").containsOnly(
						"Extract Method protected validate(titleMap Map<Locale,String>, startTimeJCalendar Calendar, endTimeJCalendar Calendar, untilJCalendar Calendar) : void extracted from protected validate(titleMap Map<Locale,String>, startTimeJCalendar Calendar, endTimeJCalendar Calendar) : void in class com.liferay.calendar.service.impl.CalendarBookingLocalServiceImpl");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("8d14e557e01cc607dd2db66c29d106ef01aa81f7").containsOnly(
						"Extract Method public get(buildTarget BuildTarget, eventBus Optional<BuckEventBus>) : TargetNode<?> extracted from public get(buildTarget BuildTarget) : TargetNode<?> in class com.facebook.buck.parser.Parser.CachedState");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("89973a5e4f188040c5fcf87fb5a3e9167329d175").containsOnly(
						"Extract Method private installAppleBundleForSimulator(params CommandRunnerParams, appleBundle AppleBundle, projectFilesystem ProjectFilesystem, processExecutor ProcessExecutor) : InstallResult extracted from private installAppleBundle(params CommandRunnerParams, appleBundle AppleBundle, projectFilesystem ProjectFilesystem, processExecutor ProcessExecutor) : InstallResult in class com.facebook.buck.cli.InstallCommand");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("cfc54e8afa7ee7d5376525a84559e90b21487ccf").containsOnly(
						"Extract Method private resetLeader() : void extracted from public getLastOffset(whichTime long) : long in class org.voltdb.importclient.kafka.KafkaStreamImporter.TopicPartitionFetcher",
						"Extract Method private resetLeader() : void extracted from public run() : void in class org.voltdb.importclient.kafka.KafkaStreamImporter.TopicPartitionFetcher");
	}

	private static void FSE_PullUpMethodRefactorings(TestBuilder test, int flag) {
		test.project("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "master")
				.atCommit("4ef35268bb96bb78b2dc698fa68e7ce763cde32e").containsOnly(
						"Pull Up Method public setColumn(column Integer) : void from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to class org.broadleafcommerce.openadmin.dto.FieldMetadata",
						"Pull Up Method public getColumn() : Integer from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to class org.broadleafcommerce.openadmin.dto.FieldMetadata");

		test.project("https://github.com/dreamhead/moco.git", "master")
				.atCommit("55ffa2f3353c5dc77fe6b790e5e045b76a07a772").containsOnly(
						"Pull Up Method protected onRequestAttached(matcher RequestMatcher) : HttpResponseSetting from class com.github.dreamhead.moco.internal.ActualHttpServer to protected onRequestAttached(matcher RequestMatcher) : HttpResponseSetting from class com.github.dreamhead.moco.internal.HttpConfiguration",
						"Pull Up Method public redirectTo(url String) : HttpResponseSetting from class com.github.dreamhead.moco.internal.ActualHttpServer to public redirectTo(url String) : HttpResponseSetting from class com.github.dreamhead.moco.internal.HttpConfiguration");

		test.project("https://github.com/raphw/byte-buddy.git", "master")
				.atCommit("f1dfb66a368760e77094ac1e3860b332cf0e4eb5").containsOnly(
						"Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.Explicit to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase",
						"Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.ForLoadedExecutable to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase");

		test.project("https://github.com/kuujo/copycat.git", "master")
				.atCommit("19a49f8f36b2f6d82534dc13504d672e41a3a8d1").containsOnly(
						"Pull Up Method private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
						"Pull Up Method private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
						"Pull Up Method private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
						"Pull Up Method private applyCommits(commitIndex long) : CompletableFuture<Void> from class net.kuujo.copycat.raft.state.ActiveState to private applyCommits(commitIndex long) : CompletableFuture from class net.kuujo.copycat.raft.state.PassiveState",
						"Pull Up Method private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.ActiveState to private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.PassiveState");

		test.project("https://github.com/codinguser/gnucash-android.git", "master")
				.atCommit("bba4af3f52064b5a2de2c9a57f9d34ba67dcdd8c").containsOnly(
						"Pull Up Method public getAllTransactionsCount() : long from class org.gnucash.android.db.TransactionsDbAdapter to public getRecordsCount() : long from class org.gnucash.android.db.DatabaseAdapter");

		test.project("https://github.com/crate/crate.git", "master")
				.atCommit("72b5348307d86b1a118e546c24d97f1ac1895bdb").containsOnly(
						"Pull Up Method public downstreamNodes(nodes Set<String>) : void from classes io.crate.planner.node.dql.MergeNode and io.crate.planner.node.dql.join.NestedLoopNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
						"Pull Up Method public downstreamExecutionNodeId(downstreamExecutionNodeId int) : void from class io.crate.planner.node.dql.MergeNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
						"Pull Up Method public downstreamNodes(downStreamNodes List<String>) : void from class io.crate.planner.node.dql.CollectNode to class io.crate.planner.node.dql.AbstractDQLPlanNode");

		test.project("https://github.com/geoserver/geoserver.git", "master")
				.atCommit("182f4d1174036417aad9d6db908ceaf64234fd5f").containsOnly(
						"Pull Up Method public pre(item ImportTask, data ImportData) : void from class org.geoserver.importer.transform.VectorTransformChain to public pre(item ImportTask, data ImportData) : void from class org.geoserver.importer.transform.TransformChain",
						"Pull Up Method public post(task ImportTask, data ImportData) : void from class org.geoserver.importer.transform.VectorTransformChain to public post(task ImportTask, data ImportData) : void from class org.geoserver.importer.transform.TransformChain");

		test.project("https://github.com/fabric8io/fabric8.git", "master")
				.atCommit("07807aed847e1d0589c094461544e54a2677cbf5").containsOnly(
						"Pull Up Method protected isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.JsonMojo to package isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
						"Pull Up Method private hasKubernetesJson(f File) : boolean from class io.fabric8.maven.ApplyMojo to package hasKubernetesJson(f File) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
						"Pull Up Method private getDependencies() : Set<File> from class io.fabric8.maven.ApplyMojo to package getDependencies() : Set<File> from class io.fabric8.maven.AbstractFabric8Mojo");

		test.project("https://github.com/jankotek/MapDB.git", "master")
				.atCommit("32dd05fc13b53873bf18c589622b55d12e3883c7").containsOnly(
						"Extract Method private insertOrUpdate(recid long, out DataOutputByteArray, isInsert boolean) : void extracted from protected update2(recid long, out DataOutputByteArray) : void in class org.mapdb.StoreAppend",
						"Pull Up Method protected longStackValParitySet(value long) : long from class org.mapdb.StoreDirect to protected longParitySet(value long) : long from class org.mapdb.Store");

		test.project("https://github.com/apache/zookeeper.git", "master")
				.atCommit("3fd77b419673ce6ec41e06cdc27558b1d8f4ca06").containsOnly(
						"Pull Up Method private cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.NIOServerCnxn to class org.apache.zookeeper.server.ServerCnxn",
						"Pull Up Method private cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.NettyServerCnxn to class org.apache.zookeeper.server.ServerCnxn");

		test.project("https://github.com/QuantumBadger/RedReader.git", "master")
				.atCommit("51b8b0e1ad4be1b137d67774eab28dc0ef52cb0a").containsOnly(
						"Pull Up Method public onSharedPreferenceChanged(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.MainActivity to protected onSharedPreferenceChangedInner(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.RefreshableActivity");

		test.project("https://github.com/Activiti/Activiti.git", "master")
				.atCommit("53036cece662f9c796d2a187b0077059c3d9088a").containsOnly(
						"Pull Up Method public isAsynchronous() : boolean from class org.activiti.bpmn.model.Activity to public isAsynchronous() : boolean from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public isAsynchronous() : boolean from class org.activiti.bpmn.model.Gateway to public isAsynchronous() : boolean from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public isNotExclusive() : boolean from class org.activiti.bpmn.model.Gateway to public isNotExclusive() : boolean from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public isNotExclusive() : boolean from class org.activiti.bpmn.model.Activity to public isNotExclusive() : boolean from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.Gateway to public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.Activity to public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.Gateway to public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.FlowNode",
						"Pull Up Method public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.Activity to public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.FlowNode");

		test.project("https://github.com/orientechnologies/orientdb.git", "master")
				.atCommit("b8fffb706258db4c4d2f608d8e8dad9312e2230d").containsOnly(
						"Pull Up Method public getDatabase() : ODatabaseDocumentInternal from class com.orientechnologies.orient.core.sql.parser.OStatement to public getDatabase() : ODatabaseDocumentInternal from class com.orientechnologies.orient.core.sql.parser.SimpleNode");

		test.project("https://github.com/tomahawk-player/tomahawk-android.git", "master")
				.atCommit("56c273ee11296288cb15320c3de781b94a1e8eb4").containsOnly(
						"Pull Up Method public wipe() : void from class org.tomahawk.libtomahawk.collection.NativeCollection to public wipe() : void from class org.tomahawk.libtomahawk.collection.Collection");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("6ff3fe00d7ffe04dbe0904b8bad98285b6988d6d").containsOnly(
						"Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl",
						"Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl",
						"Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder",
						"Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("2b0a8f6bdac621badabcb9921c077260f2470c26").containsOnly(
						"Pull Up Method private iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set<VirtualFile>, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndexImpl to public iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndex");

		test.project("https://github.com/spring-projects/spring-roo.git", "master")
				.atCommit("0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8").containsOnly(
						"Pull Up Method public getGenericDefinition() : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public getGenericDefinition() : String from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
						"Pull Up Method public setGenericDefinition(definition String) : void from class org.springframework.roo.classpath.details.MethodMetadataBuilder to class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder");

		test.project("https://github.com/VoltDB/voltdb.git", "master")
				.atCommit("e2de877a29217a50afbd142454a330e423d86045").containsOnly(
						"Pull Up Method private findAllAggPlanNodes(node AbstractPlanNode) : List<AbstractPlanNode> from class org.voltdb.planner.TestPlansApproxCountDistinct to class org.voltdb.planner.PlannerTestCase");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("c1b847acdc8cb90a1498b236b3bb5c81ca75c044").containsOnly(
						"Inline Method public createFileSystem(proxyUserName String, fsConf Configuration, stats OperatorStats) : DrillFileSystem inlined to public createFileSystem(proxyUserName String, fsConf Configuration) : DrillFileSystem in class org.apache.drill.exec.util.ImpersonationUtil",
						"Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorageAndGenerateTestData() : void in class org.apache.drill.exec.impersonation.TestImpersonationQueries",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationMetadata to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorageAndGenerateTestData() : void from class org.apache.drill.exec.impersonation.TestImpersonationQueries to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation");
	}

	private static void FSE_InlineMethodRefactorings(TestBuilder test, int flag) {
		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("2bcd05a827ead109a56cb1f79a83dcd332f42888").containsOnly(
						"Inline Method public getLanguage(id SLanguageId, langName String, version int) : SLanguage inlined to public getLanguage(id SLanguageId, langName String) : SLanguage in class jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory");

		test.project("https://github.com/vaadin/vaadin.git", "master")
				.atCommit("b0d5315e8ba95d099f93dc2d16339033a6525b59").containsOnly(
						"Inline Method private remove() : void inlined to public testColExpandRatioIsForgotten() : void in class com.vaadin.ui.GridLayoutExpandRatioTest");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("04397f41107bd6de41b31d45a4e8e2ed65628bbe").containsOnly(
						"Inline Method private checkForTestRoots(srcModule Module, testFolders Set<VirtualFile>, processed Set<Module>) : void inlined to protected checkForTestRoots(srcModule Module, testFolders Set<VirtualFile>) : void in class com.intellij.testIntegration.createTest.CreateTestAction");

		test.project("https://github.com/puniverse/quasar.git", "master")
				.atCommit("56d4b999e8be70be237049708f019c278c356e71").containsOnly(
						"Inline Method public pushMethod(entry int, numSlots int, method String, sourceLine int) : void inlined to public pushMethod(entry int, numSlots int) : void in class co.paralleluniverse.fibers.Stack",
						"Inline Method package verifySuspend(current Fiber, exc boolean) : Fiber inlined to package verifySuspend(current Fiber) : Fiber in class co.paralleluniverse.fibers.Fiber",
						"Inline Method public checkInstrumentation(exc boolean) : boolean inlined to public checkInstrumentation() : boolean in class co.paralleluniverse.fibers.Fiber",
						"Inline Method public popMethod(catchAll boolean) : void inlined to public popMethod() : void in class co.paralleluniverse.fibers.Stack");

		test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master")
				.atCommit("2252ed3754bff8c39db48d172ac76ac5a4e15359").containsOnly(
						"Inline Method private shouldShowTagToolbar() : boolean inlined to public onCreateView(inflater LayoutInflater, container ViewGroup, savedInstanceState Bundle) : View in class org.wordpress.android.ui.reader.ReaderPostListFragment");

		test.project("https://github.com/google/closure-compiler.git", "master")
				.atCommit("ba5e6d44526a2491a7004423ca2ad780c6992c46").containsOnly(
						"Inline Method private getRawTypeFromJSType(t JSType) : RawNominalType inlined to private visitOtherPropertyDeclaration(getProp Node) : void in class com.google.javascript.jscomp.GlobalTypeInfo.ProcessScope");

		test.project("https://github.com/eclipse/vert.x.git", "master")
				.atCommit("32a8c9086040fd6d6fa11a214570ee4f75a4301f").containsOnly(
						"Inline Method private handleHttp(conn ServerConnection, ch Channel, msg Object) : void inlined to protected doMessageReceived(conn ServerConnection, ctx ChannelHandlerContext, msg Object) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler");

		test.project("https://github.com/killbill/killbill.git", "master")
				.atCommit("66901e86e8bea2b999ed9f33e013fa5ed21507c7").containsOnly(
						"Inline Method private sanityOnPaymentInfoPlugin(paymentInfoPlugin PaymentTransactionInfoPlugin) : void inlined to private doOperation() : OperationResult in class org.killbill.billing.payment.core.sm.payments.PaymentOperation");

		test.project("https://github.com/real-logic/Aeron.git", "master")
				.atCommit("35893c115ba23bd62a7036a33390420f074ce660").containsOnly(
						"Inline Method private verifyPublicationClosed(times VerificationMode) : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyPublicationClosed(times VerificationMode) : void inlined to public shouldNotTimeoutPublicationOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnRemoveChannelOnUnknownSessionId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnAddSubscriptionWithInvalidUri() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnRemoveChannelOnUnknownSessionId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnAddSubscriptionWithInvalidUri() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldBeAbleToAddSinglePublication() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldNotTimeoutPublicationOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldTimeoutPublication() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyReceiverRemovesSubscription(times VerificationMode) : void inlined to public shouldNotTimeoutSubscriptionOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyReceiverRemovesSubscription(times VerificationMode) : void inlined to public shouldTimeoutSubscription() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyReceiverSubscribes() : void inlined to public shouldNotTimeoutSubscriptionOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
						"Inline Method private verifyReceiverSubscribes() : void inlined to public shouldTimeoutSubscription() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest");

		test.project("https://github.com/netty/netty.git", "master")
				.atCommit("303cb535239a6f07cbe24a033ef965e2f55758eb").containsOnly(
						"Inline Method private principal(certs Certificate[]) : Principal inlined to public getLocalPrincipal() : Principal in class io.netty.handler.ssl.OpenSslEngine.OpenSslSession",
						"Inline Method private principal(certs Certificate[]) : Principal inlined to public getPeerPrincipal() : Principal in class io.netty.handler.ssl.OpenSslEngine.OpenSslSession");

		test.project("https://github.com/FasterXML/jackson-databind.git", "master")
				.atCommit("da29a040ebae664274b28117b157044af0f525fa").containsOnly(
						"Inline Method private _writeCloseableValue(gen JsonGenerator, value Object, cfg SerializationConfig) : void inlined to public writeValue(gen JsonGenerator, value Object) : void in class com.fasterxml.jackson.databind.ObjectWriter");

		test.project("https://github.com/infinispan/infinispan.git", "master")
				.atCommit("ce4f6292d6350a2c6b82d995352fdf6d07042c9c").containsOnly(
						"Extract Method private replayRemoteTransactionIfNeeded(ctx RemoteTxInvocationContext, topologyId int) : void extracted from public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
						"Extract Method public throwRollbackExceptionIfAny() : void extracted from public commit() : void in class org.infinispan.transaction.tm.DummyTransaction",
						"Extract Method protected assertNoTransactions(cacheName String) : void extracted from protected assertNoTransactions() : void in class org.infinispan.test.MultipleCacheManagersTest",
						"Extract Method protected eventually(message String, ec Condition, timeout long, pollInterval long, unit TimeUnit) : void extracted from protected eventually(ec Condition, timeout long, pollInterval long, unit TimeUnit) : void in class org.infinispan.test.AbstractInfinispanTest",
						"Extract Method protected eventually(message String, ec Condition, timeoutMillis long, loops int) : void extracted from protected eventually(ec Condition, timeoutMillis long, loops int) : void in class org.infinispan.test.AbstractInfinispanTest",
						"Extract Method private verifyRemoteTransaction(ctx RemoteTxInvocationContext, command AbstractTransactionBoundaryCommand) : void extracted from private invokeNextInterceptorAndVerifyTransaction(ctx TxInvocationContext, command AbstractTransactionBoundaryCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
						"Extract Method private createRollbackRpcOptions() : RpcOptions extracted from public visitRollbackCommand(ctx TxInvocationContext, command RollbackCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private localGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand, isGetCacheEntry boolean) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private remoteGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand) : InternalCacheEntry in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
						"Inline Method private sendCommitCommand(ctx TxInvocationContext, command CommitCommand) : void inlined to public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor");

		test.project("https://github.com/processing/processing.git", "master")
				.atCommit("8707194f003444a9fb8e00bffa2893ef0c2492c6").containsOnly(
						"Inline Method private setFrameCentered() : void inlined to public placeWindow(location int[], editorLocation int[]) : void in class processing.opengl.PSurfaceJOGL");

		test.project("https://github.com/gradle/gradle.git", "master")
				.atCommit("f841d8dda2bf461f595755f85c3eba786783702d").containsOnly(
						"Inline Method private adaptResult(result BuildResult, startTime long, endTime long) : AbstractOperationResult inlined to private adaptResult(source BuildOperationInternal) : AbstractOperationResult in class org.gradle.tooling.internal.provider.runner.ClientForwardingBuildListener");

		test.project("https://github.com/processing/processing.git", "master")
				.atCommit("acf67c8cb58d13827e14bbeeec11a66f9277015f").containsOnly(
						"Inline Method protected runSketchEDT(args String[], constructedSketch PApplet) : void inlined to public runSketch(args String[], constructedSketch PApplet) : void in class processing.core.PApplet");

		test.project("https://github.com/neo4j/neo4j.git", "master")
				.atCommit("77fab3caea4495798a248035f0e928f745c7c2db").containsOnly(
						"Inline Method public releaseAllShared() : void inlined to public releaseAll() : void in class org.neo4j.kernel.impl.locking.community.CommunityLockClient",
						"Inline Method public releaseAllExclusive() : void inlined to public releaseAll() : void in class org.neo4j.kernel.impl.locking.community.CommunityLockClient");

		test.project("https://github.com/CyanogenMod/android_frameworks_base.git", "master")
				.atCommit("f166866cd68efa963534c5bc7fc9ca38e4aa2838").containsOnly(
						"Inline Method public is24HourFormatLocale(context Context) : boolean inlined to public is24HourFormat(context Context, userHandle int) : boolean in class android.text.format.DateFormat");

		test.project("https://github.com/WhisperSystems/TextSecure.git", "master")
				.atCommit("99528dcc3b4a82b5e52a87d3e7aed5c6479028c7").containsOnly(
						"Inline Method private getSynchronousRecipient(context Context, recipientId long) : Recipient inlined to package getRecipient(context Context, recipientId long, asynchronous boolean) : Recipient in class org.thoughtcrime.securesms.recipients.RecipientProvider");

		test.project("https://github.com/spring-projects/spring-integration.git", "master")
				.atCommit("ec5230abc7500734d7b78a176c291378e100a927").containsOnly(
						"Inline Method private doClose() : void inlined to public close() : void in class org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory.CachedConnection");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("00aa01fb90f3210d1e3027d7f759fb1085b814bd").containsOnly(
						"Extract Method public setSessionOption(drillClient DrillClient, option String, value String) : void extracted from public setControls(drillClient DrillClient, controls String) : void in class org.apache.drill.exec.testing.ControlsInjectionUtil",
						"Inline Method private assertCancelled(controls String, testQuery String, listener WaitUntilCompleteListener) : void inlined to private assertCancelledWithoutException(controls String, listener WaitUntilCompleteListener, query String) : void in class org.apache.drill.exec.server.TestDrillbitResilience");

		test.project("https://github.com/aws/aws-sdk-java.git", "master")
				.atCommit("4baf0a4de8d03022df48d696d210cc8b3117d38a").containsOnly(
						"Extract Method private pause(delay long) : void extracted from private pauseExponentially(retries int) : void in class com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper",
						"Inline Method private killServer() : void inlined to public cleanUp() : void in class com.amazonaws.util.EC2MetadataUtilsTest");

		test.project("https://github.com/libgdx/libgdx.git", "master")
				.atCommit("2bd1557bc293cb8c2348374771aad832befbe26f").containsOnly(
						"Inline Method public setCheckBoxRight(right boolean) : void inlined to public CheckBox(text String, style CheckBoxStyle) in class com.badlogic.gdx.scenes.scene2d.ui.CheckBox");

		test.project("https://github.com/JetBrains/intellij-community.git", "master")
				.atCommit("8d7a26edd1fedb9505b4f2b4fe57b2d2958b4dd9").containsOnly(
						"Inline Method private writeContentToFile(revision byte[]) : void inlined to private write(revision byte[]) : void in class com.intellij.openapi.vcs.history.FileHistoryPanelImpl.MyGetVersionAction");

		test.project("https://github.com/spring-projects/spring-boot.git", "master")
				.atCommit("1e464da2480568014a87dd0bac6febe63a76c889").containsOnly(
						"Inline Method private addStaticIndexHtmlViewControllers(registry ViewControllerRegistry) : void inlined to public addViewControllers(registry ViewControllerRegistry) : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter");

		test.project("https://github.com/rstudio/rstudio.git", "master")
				.atCommit("229d1b60c03a3f8375451c68a6911660a3993777").containsOnly(
						"Extract Method private isUpdate() : boolean extracted from public getResult() : RSConnectPublishResult in class org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy",
						"Inline Method private fireValidatedRSconnectPublish(result RSConnectPublishResult, launchBrowser boolean) : void inlined to public fireRSConnectPublishEvent(result RSConnectPublishResult, launchBrowser boolean) : void in class org.rstudio.studio.client.rsconnect.RSConnect");

		test.project("https://github.com/JetBrains/MPS.git", "master")
				.atCommit("fe6653db5fb9f1a25d5ee30e4d5d54ccdaba65fa").containsOnly(
						"Inline Method private createManyCells() : EditorCell_Collection inlined to public createCell() : EditorCell in class jetbrains.mps.lang.editor.cellProviders.SingleRoleCellProvider");

		test.project("https://github.com/google/guava.git", "master")
				.atCommit("5bab9e837cf273250aa26702204f139fdcfd9e7a").containsOnly(
						"Inline Method private checkForConcurrentModification() : void inlined to public hasNext() : boolean in class com.google.common.collect.HashBiMap.Itr",
						"Inline Method private checkForConcurrentModification() : void inlined to public remove() : void in class com.google.common.collect.HashBiMap.Itr");

		test.project("https://github.com/abarisain/dmix.git", "master")
				.atCommit("885771d57c97bd2dd48951e8aeaaa87ceb87532b").containsOnly(
						"Inline Method package processIntent(action String, mpd MPD) : void inlined to protected onHandleIntent(intent Intent) : void in class com.namelessdev.mpdroid.widgets.WidgetHelperService");

		test.project("https://github.com/WhisperSystems/TextSecure.git", "master")
				.atCommit("fa62b9bde224341e0c2d43c0694fc10c4df7336f").containsOnly(
						"Inline Method private init() : void inlined to public EmojiDrawer(context Context, attrs AttributeSet, defStyle int) in class org.thoughtcrime.securesms.components.emoji.EmojiDrawer",
						"Inline Method private initializeResources() : void inlined to private initialize() : void in class org.thoughtcrime.securesms.components.emoji.EmojiToggle");

		test.project("https://github.com/spring-projects/spring-data-mongodb.git", "master")
				.atCommit("3224fa8ce7e0079d6ad507e17534cdf01f758876").containsOnly(
						"Inline Method private processTypeHintForNestedDocuments(source Object, info TypeInformation<?>) : TypeInformation<?> inlined to private getTypeHintForEntity(source Object, entity MongoPersistentEntity<?>) : TypeInformation<?> in class org.springframework.data.mongodb.core.convert.UpdateMapper");

		test.project("https://github.com/google/closure-compiler.git", "master")
				.atCommit("b9a17665b158955ad28ef7f50cc0a8585460f053").containsOnly(
						"Inline Method private createUntaggedTemplateLiteral(n Node) : void inlined to package visitTemplateLiteral(t NodeTraversal, n Node) : void in class com.google.javascript.jscomp.Es6TemplateLiterals");

		test.project("https://github.com/apache/cassandra.git", "master")
				.atCommit("f283ed29814403bde6350a2598cdd6e2c8b983d5").containsOnly(
						"Inline Method public submitBackground(cfs ColumnFamilyStore, autoFill boolean) : List<Future<?>> inlined to public submitBackground(cfs ColumnFamilyStore) : List<Future<?>> in class org.apache.cassandra.db.compaction.CompactionManager");

		test.project("https://github.com/checkstyle/checkstyle.git", "master")
				.atCommit("a07cae0aca9f9072256b3a5fd05779e8d69b9748").containsOnly(
						"Inline Method private leaveLiteralIf(literalIf DetailAST) : void inlined to public leaveToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck",
						"Inline Method private visitLiteralTry(literalTry DetailAST) : void inlined to public visitToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
						"Inline Method private leaveLiteralTry() : void inlined to public leaveToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
						"Inline Method private visitLiteralIf(literalIf DetailAST) : void inlined to public visitToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck");

		test.project("https://github.com/netty/netty.git", "master")
				.atCommit("8a16081a9322b4a4062baaf32edc6b6b8b4afa88").containsOnly(
						"Inline Method private cancelPendingStreams() : void inlined to public close() : void in class io.netty.handler.codec.http2.StreamBufferingEncoder");

		test.project("https://github.com/amplab/tachyon.git", "master")
				.atCommit("5b184ac783784c1ca4baf1437888c79bd9460763").containsOnly(
						"Inline Method private freeSpace(bytesToBeAvailable long, location BlockStoreLocation) : EvictionPlan inlined to public freeSpaceWithView(bytesToBeAvailable long, location BlockStoreLocation, view BlockMetadataManagerView) : EvictionPlan in class tachyon.worker.block.evictor.LRUEvictor");

		test.project("https://github.com/zeromq/jeromq.git", "master")
				.atCommit("02d3fa171d02c9d82c7bdcaeb739f47d0c0006a0").containsOnly(
						"Inline Method private makeFdPair() : void inlined to public Signaler() in class zmq.Signaler");

		test.project("https://github.com/MovingBlocks/Terasology.git", "master")
				.atCommit("543a9808a85619dbe5acc2373cb4fe5344442de7").containsOnly(
						"Inline Method private initTimer(context Context) : void inlined to public preInitialise(context Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
						"Inline Method private initOpenAL(context Context) : void inlined to public initialise(rootContext Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglAudio");

		test.project("https://github.com/ignatov/intellij-erlang.git", "master")
				.atCommit("3855f0ca82795f7481b34342c7d9e5644a1d42c3").containsOnly(
						"Inline Method private getModuleFileName() : String inlined to public resolve() : PsiElement in class org.intellij.erlang.psi.impl.ErlangFunctionReferenceImpl");

		test.project("https://github.com/linkedin/rest.li.git", "master")
				.atCommit("f61db44ca4a862f1a84450643d92f85449016cfa").containsOnly(
						"Inline Method public generate(schema DataSchema) : ClassTemplateSpec inlined to private generateRecord(schema RecordDataSchema) : RecordTemplateSpec in class com.linkedin.pegasus.generator.TemplateSpecGenerator");

		test.project("https://github.com/jenkinsci/workflow-plugin.git", "master")
				.atCommit("d0e374ce8ecb687b4dc046d1edea9e52da17706f").containsOnly(
						"Inline Method private setBranch(property BranchJobProperty, branch Branch, project WorkflowJob) : void inlined to public setBranch(project WorkflowJob, branch Branch) : WorkflowJob in class org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory");

		test.project("https://github.com/facebook/buck.git", "master")
				.atCommit("6ed4cf9e83fe24fc6ab6fc9ebede016c777c9725").containsOnly(
						"Inline Method public sanitize(workingDir Optional<Path>, contents String, expandPaths boolean) : String inlined to public sanitize(workingDir Optional<Path>, contents String) : String in class com.facebook.buck.cxx.DebugPathSanitizer");

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
				.atCommit("7744a00629514b9539acac05596d64af878fe747").containsOnly(
						"Inline Method protected parseTransactions() : void inlined to protected parse() : void in class org.bitcoinj.core.Block",
						"Inline Method protected parseHeader() : void inlined to protected parse() : void in class org.bitcoinj.core.Block",
						"Inline Method public getMessageSize() : int inlined to protected parse() : void in class org.bitcoinj.core.AddressMessage",
						"Inline Method private testCachedParsing(lazy boolean) : void inlined to public testCachedParsing() : void in class org.bitcoinj.core.BitcoinSerializerTest");

		test.project("https://github.com/netty/netty.git", "master")
				.atCommit("d31fa31cdcc5ea2fa96116e3b1265baa180df58a").containsOnly(
						"Inline Method private comparator(ignoreCase boolean) : Comparator<CharSequence> inlined to public contains(name CharSequence, value CharSequence, ignoreCase boolean) : boolean in class io.netty.handler.codec.DefaultTextHeaders");

		test.project("https://github.com/gradle/gradle.git", "master")
				.atCommit("c41466b6fd11ef4edc40cb9fd42dc13cf4f6fde1").containsOnly(
						"Inline Method public resolveMetaDataArtifact(artifact ModuleComponentArtifactMetaData, result ResourceAwareResolveResult) : LocallyAvailableExternalResource inlined to public resolveArtifact(artifact ModuleComponentArtifactMetaData, result ResourceAwareResolveResult) : LocallyAvailableExternalResource in class org.gradle.api.internal.artifacts.repositories.resolver.DefaultExternalResourceArtifactResolver");

		test.project("https://github.com/katzer/cordova-plugin-local-notifications.git", "master")
				.atCommit("51f498a96b2fa1822e392027982c20e950535fd1").containsOnly(
						"Extract Method public handleEndTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser",
						"Extract Method public handleStartTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser",
						"Inline Method public postMessage(id String, data Object) : void inlined to public onCreateOptionsMenu(menu Menu) : boolean in class org.apache.cordova.CordovaActivity",
						"Inline Method public postMessage(id String, data Object) : void inlined to public onOptionsItemSelected(item MenuItem) : boolean in class org.apache.cordova.CordovaActivity",
						"Inline Method public postMessage(id String, data Object) : void inlined to public onPrepareOptionsMenu(menu Menu) : boolean in class org.apache.cordova.CordovaActivity");

		test.project("https://github.com/apache/drill.git", "master")
				.atCommit("c1b847acdc8cb90a1498b236b3bb5c81ca75c044").containsOnly(
						"Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorageAndGenerateTestData() : void in class org.apache.drill.exec.impersonation.TestImpersonationQueries",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorage() : void from class org.apache.drill.exec.impersonation.TestImpersonationMetadata to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Pull Up Method public addMiniDfsBasedStorageAndGenerateTestData() : void from class org.apache.drill.exec.impersonation.TestImpersonationQueries to protected addMiniDfsBasedStorage(workspaces Map) : void from class org.apache.drill.exec.impersonation.BaseTestImpersonation",
						"Inline Method public createFileSystem(proxyUserName String, fsConf Configuration, stats OperatorStats) : DrillFileSystem inlined to public createFileSystem(proxyUserName String, fsConf Configuration) : DrillFileSystem in class org.apache.drill.exec.util.ImpersonationUtil");

		test.project("https://github.com/linkedin/rest.li.git", "master")
				.atCommit("54fa890a6af4ccf564fb481d3e1b6ad4d084de9e").containsOnly(
						"Extract Method public addResponseCompressionHeaders(responseCompressionOverride CompressionOption, req RestRequest) : RestRequest extracted from public onRestRequest(req RestRequest, requestContext RequestContext, wireAttrs Map<String,String>, nextFilter NextFilter<RestRequest,RestResponse>) : void in class com.linkedin.r2.filter.compression.ClientCompressionFilter",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompressionBetter(compressor Compressor) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testAcceptEncoding(acceptedEncoding String, contentEncoding String) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompressionWorse(compressor Compressor) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public testCompatibleDefault(acceptEncoding String, contentEncoding String) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Extract Method public addCompressionHeaders(getMessage HttpGet, acceptEncoding String) : void extracted from public test406Error(acceptContent String) : void in class com.linkedin.restli.examples.TestCompressionServer",
						"Inline Method public createServer(engine Engine, port int, supportedCompression String, useAsyncServletApi boolean, asyncTimeOut int, requestFilters List, responseFilters List) : HttpServer inlined to public createServer(engine Engine, port int, supportedCompression String, useAsyncServletApi boolean, asyncTimeOut int) : HttpServer in class com.linkedin.restli.examples.RestLiIntTestServer");

		test.project("https://github.com/hazelcast/hazelcast.git", "master")
				.atCommit("204bf49cba03fe5d581a35ff82dd22587a681f46").containsOnly(
						"Extract Method private createConfig() : Config extracted from private testWaitingInvocations_whenNodeSplitFromCluster(splitAction SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest",
						"Extract Method private createConfig() : Config extracted from private testWaitNotifyService_whenNodeSplitFromCluster(action SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest",
						"Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterSlave(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl",
						"Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterMaster(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl");
	}

	private static List<Root> refactoringsMem;

	private static void prepareFSERefactorings(TestBuilder test, int flag)
			throws JsonParseException, JsonMappingException, IOException {
		List<Root> refactorings = getFSERefactorings(flag);

		for (Root root : refactorings) {
			test.project(root.repository, "master").atCommit(root.sha1)
					.containsOnly(extractRefactorings(root.refactorings));

		}

		refactoringsMem = refactorings;
	}

	public static String[] extractRefactorings(List<Refactoring> refactoring) {

		int count = 0;
		for (Refactoring ref : refactoring) {
			if (ref.validation.contains("TP"))
				count++;
		}
		String[] refactorings = new String[count];

		int counter = 0;
		for (Refactoring ref : refactoring) {
			if (ref.validation.contains("TP")) {

				refactorings[counter++] = ref.description;
			}
		}

		return refactorings;
	}

	public static List<Root> getFSERefactorings(int flag) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		String jsonFile = System.getProperty("user.dir") + "/src-test/Data/data.json";

		List<Root> roots = mapper.readValue(new File(jsonFile),
				mapper.getTypeFactory().constructCollectionType(List.class, Root.class));

		List<Root> filtered = new ArrayList<>();

		for (Root root : roots) {
			List<Refactoring> refactorings = new ArrayList<>();

			root.refactorings.forEach((refactoring) -> {
				if (isAdded(refactoring, flag)) // refactoring.type.equals("Extract
												// Method"))
					refactorings.add(refactoring);
			});

			if (refactorings.size() > 0) {

				Root tmp = root;
				tmp.refactorings = refactorings;
				filtered.add(tmp);
			}
		}

		return filtered;
	}

	private static boolean isAdded(Refactoring refactoring, int flag) {
		try {
			return ((Enum.valueOf(Refactorings.class, refactoring.type.replace(" ", "")).getValue() & flag) > 0);

		} catch (Exception e) {
			return false;
		}
	}

	public static void printRefDiffResults(int flag) {
		Hashtable<String, Tuple> result = new Hashtable<>();
		try {
			List<Root> roots = getFSERefactorings(flag);

			for (Refactorings ref : Refactorings.values()) {
				if (ref == Refactorings.All)
					continue;
				result.put(ref.toString(), new Tuple());
			}
			for (Root root : roots) {
				for (Refactoring ref : root.refactorings) {
					Tuple tuple = result.get(ref.type.replace(" ", ""));
					tuple.totalTruePositives += ref.validation.contains("TP") ? 1 : 0;
					tuple.unknown += ref.validation.equals("UKN") ? 1 : 0;

					if (ref.detectionTools.contains("RefDiff")) {
						tuple.refDiffTruePositives += ref.validation.contains("TP") ? 1 : 0;
						tuple.refDiffFalsePositives += ref.validation.equals("FP") ? 1 : 0;
					}

				}
			}
			Tuple[] tmp = {};
			System.out.println("Total\t" + buildResultMessage(result.values().toArray(tmp)));
			for (String key : result.keySet()) {
				System.out.println(getInitials(key) + "\t" + buildResultMessage(result.get(key)));
			}

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String getInitials(String str) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < str.length(); i++) {
			String character = str.substring(i, i + 1);
			if (character == character.toUpperCase())
				sb.append(character);
		}
		return sb.toString();
	}

	private static String buildResultMessage(Tuple... result) {
		int trueP = 0;
		int total = 0;
		int ukn = 0;
		int falseP = 0;
		for (Tuple res : result) {
			trueP += res.refDiffTruePositives;
			total += res.totalTruePositives;
			ukn += res.unknown;
			falseP += res.refDiffFalsePositives;
		}
		double precision = trueP / (double) (trueP + falseP);
		double recall = trueP / (double) (total);
		try {
			String mainResultMessage = String.format("TP: %2d  FP: %2d  FN: %2d  Unk.: %2d  Prec.: %.3f  Recall: %.3f",
					(int) trueP, (int) falseP, (int) (total - trueP), ukn, precision, recall);
			return mainResultMessage;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("");
		}
		return "";
	}

	public static class Tuple {
		public int totalTruePositives;
		public int refDiffTruePositives;
		public int falseNegatives;
		public int unknown;
		public int refDiffFalsePositives;
	}

	public static class Root {
		public int id;
		public String repository;
		public String sha1;
		public String url;
		public String author;
		public String time;
		public List<Refactoring> refactorings;
		public long refDiffExecutionTime;

	}

	public static class Refactoring {
		public String type;
		public String description;
		public String comment;
		public String validation;
		public String detectionTools; 
		public String validators;

	}

	public static class Comment {
		public String refactored;
		public String link;
		public String message;
		public String type;
		public String reportedCase;
	}
 
}
