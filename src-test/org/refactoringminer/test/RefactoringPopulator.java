package org.refactoringminer.test;

import java.util.Iterator;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.test.TestBuilder.ProjectMatcher.CommitMatcher;

public class RefactoringPopulator {

	public enum Systems {
		aTunes(1), argoUML(2), jUnit(4), ANTLER4(8), All(15);
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
				32), PullUpMehod(64), PullUpAttribute(128), All(255);
		private int value;

		private Refactorings(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static void feedRefactoringsInstances(int refactoringsFlag, int systemsFlag, TestBuilder test) {

		// int ref = 0;
		// for (Refactorings type : refactorings) {
		// ref |= type.getValue();
		// }

		if ((systemsFlag & Systems.aTunes.getValue()) > 0) {

			aTunesRefactorings(test, refactoringsFlag);
		}

		if ((systemsFlag & Systems.argoUML.getValue()) > 0) {
			argoRefactorings(test, refactoringsFlag);
		}

//		if ((systemsFlag & Systems.ANTLER4.getValue())>0){
//			antlrRefactorings(test, systemsFlag);
//		}
		
		if ((systemsFlag& Systems.jUnit.getValue())>0) {
			jUnitRefactorings(test, systemsFlag);
		}
		// if ((refactoringsFlag & Refactorings.MoveMethod.getValue()) == 1) {
		// if ((systemsFlag & Systems.aTunes.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.argoUML.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.ANTLER4.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.jUnit.getValue()) == 1) {
		//
		// }
		// }
		// if ((refactoringsFlag & Refactorings.MoveAttribute.getValue()) == 1)
		// {
		// if ((systemsFlag & Systems.aTunes.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.argoUML.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.ANTLER4.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.jUnit.getValue()) == 1) {
		//
		// }
		// }
		// if ((refactoringsFlag & Refactorings.InlineMethod.getValue()) == 1) {
		// if ((systemsFlag & Systems.aTunes.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.argoUML.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.ANTLER4.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.jUnit.getValue()) == 1) {
		//
		// }
		// }
		// if ((refactoringsFlag & Refactorings.ExtractMethod.getValue()) == 1)
		// {
		// if ((systemsFlag & Systems.aTunes.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.argoUML.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.ANTLER4.getValue()) == 1) {
		//
		// }
		// if ((systemsFlag & Systems.jUnit.getValue()) == 1) {
		//
		// }
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
						"Extract Method public setCover(FigRect cover) : void extracted from private initClassifierRoleFigs() : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public setCover(FigRect cover) : void extracted from public clone() : Object in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public setLineColor(col Color) : void in org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public getLineColor() : Color in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public setFillColor(col Color) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public getFillColor() : Color in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public setFilled(f boolean) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public isFilled() : boolean in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public setLineWidth(w int) : void in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getCover() : FigRect extracted from public getLineWidth() : int in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole",
						"Extract Method public getUpperRect() : FigRect extracted from private initFigs() : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
						"Extract Method public getUpperRect() : FigRect extracted from public setLineColor(Color c) : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
						"Extract Method public getUpperRect() : FigRect extracted from protected setStandardBounds(x int,y int, w int, h int) : void in org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
						"Extract Method public setUpperRect(FigRect upperRect) : void extracted from private initFigs() : void in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
						"Extract Method public setUpperRect(FigRect upperRect) : void extracted from public clone() : Object in class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent",
						"Extract Method public getFlatChildren() : List<ToDoItem> extracted from public getChild(parent Object, index int) : Object in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public getFlatChildren() : List<ToDoItem> extracted from public getChildCount(parent Object) : int in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public getFlatChildren() : List<ToDoItem> extracted from public getIndexOfChild(parent Object, child Object) : int in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public getFlatChildren() : List<ToDoItem> extracted from public calcFlatChildren() : void in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public getFlatChildren() : List<ToDoItem> extracted from public addFlatChildren(node Object) : void in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public setFlatChildren(List<ToDoItem> flatChildren) : void extracted from public addFlatChildren(node Object) : void in class org.argouml.cognitive.ui.ToDoPerspective",
						"Extract Method public getChoices() : List<String> extracted from public actionPerformed(e ActionEvent) : void in class org.argouml.cognitive.ui.ActionOpenDecisions",
						"Extract Method public setChoices(choices List<String>) : void extracted from public WizStepChoice(w Wizard, instr String, ch List<String>) : void in class org.argouml.cognitive.ui.ActionOpenDecisions",
						"Extract Method public getSlidersToGoals() : Hashtable<JSlider, Goal> extracted from private initMainPanel() : void in class org.argouml.cognitive.ui.GoalsDialog",
						"Extract Method public getSlidersToGoals() : Hashtable<JSlider, Goal> extracted from public stateChanged(ChangeEvent ce) : void in class org.argouml.cognitive.ui.GoalsDialog",
						"Extract Method public getDescription() : WizDescription extracted from public showDescription() : void in class org.argouml.cognitive.ui.TabToDo",
						"Extract Method public getDescription() : WizDescription extracted from public showStep(ws JPanel) : void in class org.argouml.cognitive.ui.TabToDo",
						"Extract Method public getDescription() : WizDescription extracted from private setTargetInternal(item Object) : void in class org.argouml.cognitive.ui.TabToDo"

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
						//The following cases need more discussion.
						"Push Down Method public getPriority() : int from class org.argouml.cognitive.Critic to public getPriority() : int from class org.argouml.cognitive.CompoundCritic",
						"Push Down Method public getPriority() : int from class org.argouml.cognitive.Critic to public getPriority() : int from class org.argouml.uml.cognitive.critics.CrUML",
						"Push Down Method public setPriority(p int) : void from class org.argouml.cognitive.Critic to public setPriority(p int) : void from class org.argouml.cognitive.CompoundCritic",
						"Push Down Method public setPriority(p int) : void from class org.argouml.cognitive.Critic to public setPriority(p int) : void from class org.argouml.uml.cognitive.critics.CrUML",
						"Push Down Method public getName() : String from class org.argouml.ui.PerspectiveSupport to public getName() : String from class org.argouml.ui.TreeModelSupport",
						"Push Down Method public setName(s String) : void from class org.argouml.ui.PerspectiveSupport to public setName(s String) : void from class org.argouml.ui.TreeModelSupport",
						"Push Down Method public getUUIDRefs() : HashMap from class org.argouml.persistence.ModelMemberFilePersister to public getUUIDRefs() : HashMap from class org.argouml.persistence.OldModelMemberFilePersister",
						"Push Down Method public isStarted() : boolean from class org.argouml.cognitive.critics.Wizard to public isStarted() : boolean from class org.argouml.uml.cognitive.critics.UMLWizard")
				
//				.atCommit("0368849555a9d7af5e8ff7c416b0219614543208").notContains(
//						"Push Down Method public getPriority() : int from class org.argouml.cognitive.Critic to public getPriority() : int from class org.argouml.cognitive.CompoundCritic", 
//						"Push Down Method public setName(s String) : void from class org.argouml.ui.PerspectiveSupport to public setName(s String) : void from class org.argouml.ui.TreeModelSupport",
//						"Push Down Method public setPriority(p int) : void from class org.argouml.cognitive.Critic to public setPriority(p int) : void from class org.argouml.cognitive.CompoundCritic")

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
						// in other subclasses
						"Pull Up Method getMechList from class org.argouml.cognitive.CompositeCM to getMechList from class org.argouml.cognitive.ControlMech",
						"Pull Up Method public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.uml.AttributeNotationUml to public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.AttributeNotation",
						"Pull Up Method public transform(file File, version int) : File from class org.argouml.persistence.UmlFilePersister to public transform(file File, version int) : File from class org.argouml.persistence.AbstractFilePersister",
						"Pull Up Method private colToString(set Collection) : String from class org.argouml.profile.internal.ui.PropPanelCritic to protected colToString(set Collection) : String from class org.argouml.uml.ui.PropPanel",
						"Pull Up Method private getSomeProfileManager() : ProfileManager from class org.argouml.profile.UserDefinedProfile to protected getSomeProfileManager() : ProfileManager from class org.argouml.profile.Profile",
						"Pull Up Method protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrMultipleShallowHistoryStates to protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrUML",
						"Pull Up Method public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.collaboration.ui.CollabDiagramRenderer to public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.UmlDiagramRenderer",
						"Pull Up Method private initFigs() : void from class org.argouml.uml.diagram.state.ui.FigSynchState to protected initFigs() : void from class org.argouml.uml.diagram.state.ui.FigStateVertex",
						// FN** minnor modifications, but no statement is
						// exactly the same
						"Pull Up Method public createPartition() : Partition from class org.argouml.model.mdr.ActivityGraphsFactoryMDRImpl to public createPartition() : Partition from class org.argouml.model.mdr.AbstractUmlModelFactoryMDR",
						"Pull Up Method public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManagerImpl.ChangeabilityGetterSetter to public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManager.OptionGetterSetter");
	}

	private static void jUnitRefactorings(TestBuilder test, int flag) {

		test.project("https://github.com/MatinMan/RefactoringDatasets.git", "junit")
				.atCommit("00e584db35fdb44b58eccaff7dc5ec6b0da7547a").containsOnly(
						"Extract Method	private addMultipleFailureException(mfe MultipleFailureException) : void extracted from public addFailure(targetException Throwable) : void in class org.junit.internal.runners.model.EachTestNotifier",
						"Extract Method	private runNotIgnored(method FrameworkMethod, eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method	private runIgnored(eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method addTestsFromTestCase(theClass final Clss<?>) : void extracted from public TestSuite(final Class<? extends TestCase> theClass) in class in class junit.src.main.java.junit.framework.TestSuite",
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
							"Move Method isSongFavorite from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesSongsManager to isSongFavorite from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesObjectDataStore",
							"Move Method private isFadeAwayInProgress() : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerEngine to package isFadeAwayInProgress(mPlayerEngine MPlayerEngine) : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerCommandWriter",
							"Move Method package addToHistory(audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory to package addToHistory(playbackHistory PlaybackHistory, audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory.Heap",
							"Move Method public readObjectFromFile(inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XMLSerializerService to public readObjectFromFile(xmlSerializerService XMLSerializerService, inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XStreamFactory",
							"Move Method public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.CryptoUtils to public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.DateUtils");

		}

		if ((Refactorings.MoveAttribute.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("fec9b7bbc31a04c6f15137679faeb7fcfd6cd97d");

			getaTunesMoveAttributes(matcher);

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
				getaTunesMoveAttributes(matcher);
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

		if ((Refactorings.PullUpMehod.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("566cbfcc517e5d7e7372b893bb5ea779b7397ffd");

			getaTunesPullUpMethod(matcher);

		}

		if ((Refactorings.PullUpAttribute.getValue() & flag) > 0) {
			CommitMatcher matcher = test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
					.atCommit("f061c05f845933f3e9ca8dba2b37c24b2ff164f2");

			getaTunesPullUpAttrinute(matcher);
		}
	}

	private static void getaTunesPullUpAttrinute(CommitMatcher matcher) {
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

	private static void getaTunesMoveAttributes(CommitMatcher matcher) {
		matcher.containsOnly(
				"Move Attribute private imageSize : int from class net.sourceforge.atunes.kernel.modules.fullscreen.Cover to class net.sourceforge.atunes.kernel.modules.fullscreen.CoverFlow",
				"Move Attribute private versionProperty : String from class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler to class net.sourceforge.atunes.kernel.modules.updates.VersionXmlParser",
				"Move Attribute private process : Process from class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
				"Move Attribute private albumFavoriteSearchOperator : ISearchUnaryOperator<IAlbum> from class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField to class net.sourceforge.atunes.kernel.modules.search.AlbumArtistSearchField",
				"Move Attribute private old : boolean from class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedEntry to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeed",
//				"Extract Method private getAlbumFavoriteSearchOperator() : ISearchUnaryOperator extracted from * in class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField",
				"Extract Method private getVersionProperty() : String extracted from public setVersionProperty(versionProperty String) : void in class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler",
				"Extract Method private getProcess() : Process extracted from public stop() : void in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder",
				"Extract Method private setProcess(process Process) : void extracted from public stop() : void in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder",
				"Extract Method private getProcess() : Process extracted from public testEncoder() : boolean in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder",
				"Extract Method private getAlbumFavoriteSearchOperator() : ISearchUnaryOperator extracted from public getOperators() : List in class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField"

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
				// The rest are related to some push down method which they signature got abstracted in the base class and the body moved to the child. So other cases should be added before this line.
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

	// private static <E> int getFlag(E... e) {
	// int ref = 0;
	//
	// for (E type : e) {
	// ref |= ((Enum) type).getValue();
	// }
	//
	// return ref;
	// }
}
