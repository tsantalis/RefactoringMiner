package br.ufmg.dcc.labsoft.refdetector.model.builder;

import org.junit.Test;

import br.ufmg.dcc.labsoft.refactoringanalyzer.TestBuilder;

public class TestRecall {

    @Test
    public void test() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp").withAggregation();

        test.project("https://github.com/danilofes/argouml-refactorings.git", "master")
        .atCommit("da5ea3acc1b6528cd7ec731c741e5f691c25dc6f")
        .contains(
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
        .contains(
            "Inline Method private buildPanel() : void inlined to public getTabPanel() : JPanel in class org.argouml.notation.ui.SettingsTabNotation",
            "Inline Method private getAssociationActions() : Object[] inlined to protected getUmlActions() : Object[] in class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram",
            "Inline Method private getPersistenceVersionFromFile(file File) : int inlined to protected doLoad(originalFile File, file File, progressMgr ProgressMgr) : Project in class org.argouml.persistence.UmlFilePersister",
            "Inline Method protected removeAllElementListeners(listener PropertyChangeListener) : void inlined to public removeAllElementListeners() : void in class org.argouml.notation.NotationProvider",
            "Inline Method private findTarget(t Object) : Object inlined to public shouldBeEnabled(t Object) : boolean in class org.argouml.cognitive.checklist.ui.TabChecklist",
//              "Inline Method private findTarget(t Object) : Object inlined to public setTarget(t Object) : void in class org.argouml.cognitive.checklist.ui.TabChecklist",
            "Inline Method private initFigs() : void inlined to public AbstractFigNode(owner Object, bounds Rectangle, settings DiagramSettings) in class org.argouml.uml.diagram.deployment.ui.AbstractFigNode",
            "Inline Method private makeSubStatesIcon(x int, y int) : void inlined to private initFigs() : void in class org.argouml.uml.diagram.activity.ui.FigSubactivityState",
            "Inline Method private setTodoList(member AbstractProjectMember) : void inlined to public add(member ProjectMember) : boolean in class org.argouml.kernel.MemberList",
            // FN** parameter substitution?
            "Inline Method appendArrays() inlined to ??? in class org.argouml.ui.explorer.PerspectiveManager",
            "Inline Method private dealWithVisibility(attribute Object, visibility String) : void inlined to protected parseAttribute(text String, attribute Object) : void in class org.argouml.notation.providers.uml.AttributeNotationUml")

        .atCommit("f7daed39b7096a537d138c60b7429affaec368d6")
        .contains(
            "Move Method public makeKey(k1 String, k2 String, k3 String, k4 String, k5 String) : ConfigurationKey from class org.argouml.configuration.Configuration to public makeKey(k1 String, k2 String, k3 String, k4 String, k5 String) : ConfigurationKey from class org.argouml.cognitive.ui.ActionGoToCritique",
            "Move Method protected createTempFile(file File) : File from class org.argouml.persistence.AbstractFilePersister to protected createTempFile(file File, abstractFilePersister AbstractFilePersister) : File from class org.argouml.persistence.ModelMemberFilePersister",
            "Move Method private initDistributeMenu(distribute JMenu) : void from class org.argouml.ui.cmd.GenericArgoMenuBar to package initDistributeMenu(distribute JMenu) : void from class org.argouml.ui.cmd.NavigateTargetForwardAction",
            "Move Method public selectTabNamed(tabName String) : boolean from class org.argouml.ui.DetailsPane to public selectTabNamed(tabName String, pane DetailsPane) : boolean from class org.argouml.ui.ArgoToolbarManager",
            "Move Method public loadUserPerspectives() : void from class org.argouml.ui.explorer.PerspectiveManager to public loadUserPerspectives(perspectiveManager PerspectiveManager) : void from class org.argouml.ui.explorer.ActionDeployProfile",
            // method getCriticizedDesignMaterials not moved, but actually getCreateStereotype from WizAddConstructor to CrTooManyStates  Move Method getCriticizedDesignMaterials moved from class org.argouml.uml.cognitive.critics.WizAddConstructor to class org.argouml.uml.cognitive.critics.CrTooManyStates",
            "Move Method private getCreateStereotype(obj Object) : Object from class org.argouml.uml.cognitive.critics.WizAddConstructor to package getCreateStereotype(obj Object) : Object from class org.argouml.uml.cognitive.critics.CrTooManyStates",
            "Move Method protected isReverseEdge(index int) : boolean from class org.argouml.uml.diagram.deployment.ui.SelectionObject to protected isReverseEdge(index int) : boolean from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode",
            "Move Method protected setStandardBounds(x int, y int, width int, height int) : void from class org.argouml.uml.diagram.state.ui.FigHistoryState to protected setStandardBounds(x int, y int, width int, height int, state FigHistoryState) : void from class org.argouml.uml.diagram.state.ui.SelectionState",
            "Move Method private buildString(st Object) : String from class org.argouml.uml.diagram.ui.ActionAddStereotype to package buildString(st Object) : String from class org.argouml.uml.diagram.ui.SelectionEdgeClarifiers",
            // FN** method moved, but has only one line
            "Move Method protected canEdit(f Fig) : boolean from class org.argouml.uml.diagram.use_case.ui.FigInclude to protected canEdit(f Fig) : boolean from class org.argouml.uml.diagram.use_case.ui.SelectionActor")

        .atCommit("4ba691a56b441f0ce9d1683a8ad3ed5b82d52268")
        .contains(
            "Move Attribute private flatChildren : List<ToDoItem> from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.GoListToDecisionsToItems",
            "Move Attribute private cover : FigRect from class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole to class org.argouml.uml.diagram.collaboration.ui.ActionAddClassifierRole",
            "Move Attribute private description : WizDescription from class org.argouml.cognitive.ui.TabToDo to class org.argouml.cognitive.ui.GoListToGoalsToItems",
            "Move Attribute private slidersToGoals : Hashtable<JSlider,Goal> from class org.argouml.cognitive.ui.GoalsDialog to class org.argouml.cognitive.ui.InitCognitiveUI",
            "Move Attribute private weight : float from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramNode to class org.argouml.uml.diagram.static_structure.layout.ClassdiagramModelElementFactory",
            "Move Attribute private choices : List<String> from class org.argouml.cognitive.ui.WizStepChoice to class org.argouml.cognitive.ui.ActionOpenDecisions",
            "Move Attribute private updatingSelection : boolean from class org.argouml.ui.explorer.ExplorerTree to class org.argouml.ui.explorer.PerspectiveComboBox",
            "Move Attribute private icon : Icon from class org.argouml.notation.NotationNameImpl to class org.argouml.notation.InitNotation",
            // there is a new comment field in StylePanelFigInterface, but not moved. refreshTransaction is moved from  StylePanelFigInterface to SelectionComment
            //"Move Attribute org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface comment org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface",
            "Move Attribute private refreshTransaction : boolean from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface to class org.argouml.uml.diagram.static_structure.ui.SelectionComment",
            "Move Attribute private upperRect : FigRect from class org.argouml.uml.diagram.deployment.ui.AbstractFigComponent to class org.argouml.uml.diagram.deployment.ui.UMLDeploymentDiagram")
        .atCommit("4ba691a56b441f0ce9d1683a8ad3ed5b82d52268")
        //FP** one-liner, more like encapsulate field
        .notContains("Extract Method public getCover() : FigRect extracted from * in class org.argouml.uml.diagram.collaboration.ui.FigClassifierRole")

        .atCommit("0368849555a9d7af5e8ff7c416b0219614543208")
        .contains(
            "Push Down Attribute private priority : int from class org.argouml.cognitive.Critic to class org.argouml.uml.cognitive.critics.CrUML",
            "Push Down Attribute private flatChildren : List<ToDoItem> from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByGoal",
//              "Push Down Attribute private flatChildren : List<ToDoItem> from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByPoster",
//              "Push Down Attribute private flatChildren : List<ToDoItem> from class org.argouml.cognitive.ui.ToDoPerspective to class org.argouml.cognitive.ui.ToDoByDecision",
            "Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.uml.ui.TabStyle",
//              "Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.ui.StylePanel",
//              "Push Down Attribute private OVERLAPP : int from class org.argouml.application.api.AbstractArgoJPanel to class org.argouml.uml.diagram.ui.TabDiagram",
            "Push Down Attribute private uUIDRefs : HashMap<String,Object> from class org.argouml.persistence.ModelMemberFilePersister to class org.argouml.persistence.OldModelMemberFilePersister",
            "Push Down Attribute private displayLabel : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement to class org.argouml.uml.diagram.ui.StylePanelFigMessage",
            "Push Down Attribute private name : String from class org.argouml.ui.PerspectiveSupport to class org.argouml.ui.TreeModelSupport",
            "Push Down Attribute private bgImage : Image from class org.argouml.ui.explorer.ExplorerTree to class org.argouml.ui.explorer.DnDExplorerTree",
            "Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepManyTextFields",
//              "Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepConfirm",
//              "Push Down Attribute private target : Object from class org.argouml.cognitive.ui.WizStep to class org.argouml.cognitive.ui.WizStepCue",
            "Push Down Attribute private started : boolean from class org.argouml.cognitive.critics.Wizard to class org.argouml.uml.cognitive.critics.UMLWizard",
//              "Push Down Attribute private cover : FigCube from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.deployment.ui.FigNodeInstance",
            "Push Down Attribute private cover : FigCube from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.deployment.ui.FigMNode")

        .atCommit("26b089d0c3f74abadcf8645f4f7618bdc0c3738f")
        .contains(
            "Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
            "Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramGeneralizationEdge",
            "Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ActionStateNotation",
         // the method was at SelectionClassifierBox, which is the superclass (it's wrong in the spreadsheet) "Push Down Method org.argouml.uml.diagram.ui.FigEdgeModelElement mouseReleased org.argouml.uml.diagram.static_structure.ui.SelectionClass, org.argouml.uml.diagram.static_structure.ui.SelectionInterface, org.argouml.uml.diagram.ui.SelectionClassifierBox",
            // FN** why?
            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
//            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
//            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
            "Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigClass",
            "Push Down Method public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizMEName to public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizOperName",
            // FN** why?
            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizBreakCircularComp",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizNavigable",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddOperation",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddConstructor",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAssocComposite",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddInstanceVariable",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizMEName",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizTooMany",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizManyNames",
//          "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizCueCards",
            "Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionDataType",
            "Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement",
            "Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByDecision")

        .atCommit("51b0fdc853d6e5188ce8625d3cc4770aad981011")
        .contains(
            "Pull Up Attribute protected configPanelNorth : JPanel from class org.argouml.ui.explorer.PerspectiveConfigurator to class org.argouml.util.ArgoDialog",
            "Pull Up Attribute protected ATTRIBUTES_THRESHOLD : int from class org.argouml.uml.cognitive.critics.CrTooManyAttr to class org.argouml.uml.cognitive.critics.AbstractCrTooMany",
            "Pull Up Attribute protected DEFAULT_X : int from class org.argouml.uml.diagram.deployment.ui.AbstractFigNode to class org.argouml.uml.diagram.ui.FigNodeModelElement",
            "Pull Up Attribute protected dep : Icon from class org.argouml.uml.diagram.deployment.ui.SelectionComponent to class org.argouml.uml.diagram.ui.SelectionNodeClarifiers2",
//              "Pull Up Attribute protected dep : Icon from class org.argouml.uml.diagram.deployment.ui.SelectionComponentInstance to class org.argouml.uml.diagram.ui.SelectionNodeClarifiers2",
            "Pull Up Attribute protected WIDTH : int from class org.argouml.uml.diagram.state.ui.FigSubmachineState to class org.argouml.uml.diagram.state.ui.FigState",
            "Pull Up Attribute protected dashed : boolean from class org.argouml.uml.diagram.state.ui.FigTransition to class org.argouml.uml.diagram.ui.FigEdgeModelElement",
            "Pull Up Attribute protected instructions : String from class org.argouml.uml.diagram.static_structure.ui.SelectionClass to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
//              "Pull Up Attribute protected instructions : String from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface to class org.argouml.uml.diagram.ui.SelectionClassifierBox",
            "Pull Up Attribute protected mMmeiTarget : Object from class org.argouml.uml.ui.TabConstraints to class org.argouml.application.api.AbstractArgoJPanel",
            "Pull Up Attribute protected namespace : Object from class org.argouml.model.TestUmlGeneralization to class org.argouml.model.GenericUmlObjectTestFixture",
            "Pull Up Attribute protected modelImpl : MDRModelImplementation from class org.argouml.model.mdr.ExtensionMechanismsFactoryMDRImpl to class org.argouml.model.mdr.AbstractUmlModelFactoryMDR")

        .atCommit("8c2be520e01a83ee893158aa6cb90ee2d64f6a63")
        .contains(
            // FN** getMechList remains in CompositeCM, but it is also added as abstract in ControlMech and implemented in other subclasses
            "Pull Up Method getMechList from class org.argouml.cognitive.CompositeCM to getMechList from class org.argouml.cognitive.ControlMech",
            "Pull Up Method public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.uml.AttributeNotationUml to public parse(modelElement Object, text String) : void from class org.argouml.notation.providers.AttributeNotation",
            "Pull Up Method public transform(file File, version int) : File from class org.argouml.persistence.UmlFilePersister to public transform(file File, version int) : File from class org.argouml.persistence.AbstractFilePersister",
            "Pull Up Method private colToString(set Collection) : String from class org.argouml.profile.internal.ui.PropPanelCritic to protected colToString(set Collection) : String from class org.argouml.uml.ui.PropPanel",
            "Pull Up Method private getSomeProfileManager() : ProfileManager from class org.argouml.profile.UserDefinedProfile to protected getSomeProfileManager() : ProfileManager from class org.argouml.profile.Profile",
            "Pull Up Method protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrMultipleShallowHistoryStates to protected computeOffenders(ps Object) : ListSet from class org.argouml.uml.cognitive.critics.CrUML",
            "Pull Up Method public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.collaboration.ui.CollabDiagramRenderer to public getFigEdgeFor(gm GraphModel, lay Layer, edge Object, styleAttributes Map) : FigEdge from class org.argouml.uml.diagram.UmlDiagramRenderer",
            "Pull Up Method private initFigs() : void from class org.argouml.uml.diagram.state.ui.FigSynchState to protected initFigs() : void from class org.argouml.uml.diagram.state.ui.FigStateVertex",
            // FN** minnor modifications, but no statement is exactly the same
            "Pull Up Method public createPartition() : Partition from class org.argouml.model.mdr.ActivityGraphsFactoryMDRImpl to public createPartition() : Partition from class org.argouml.model.mdr.AbstractUmlModelFactoryMDR",
            "Pull Up Method public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManagerImpl.ChangeabilityGetterSetter to public set(modelElement Object, value Object) : void from class org.argouml.core.propertypanels.model.GetterSetterManager.OptionGetterSetter")
        ;
        
        test.project("https://github.com/danilofes/atunes-refactorings.git", "master")
        .atCommit("8dbd39562602c1112e7f73b2a1825c78a70ac5c7")
        .contains(
            // correct class is NeroAacEncoder, not FlacEncoder
//              "Extract Method extractedMethod in class net.sourceforge.atunes.kernel.modules.cdripper.FlacEncoder",
            "Extract Method private extractedMethod(wavFile File, mp4File File) : List<String> extracted from public encode(wavFile File, mp4File File) : boolean in class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder",
            "Extract Method private extractedMethod(nonPatternSequencesArray String[], nonPatternSequences List<String>) : void extracted from private getNonPatternSequences(patternsString String) : List<String> in class net.sourceforge.atunes.kernel.modules.pattern.PatternMatcher",
            "Extract Method private extractedMethod(audioObject IAudioObject, location Point, i ImageIcon) : void extracted from package showOSD(audioObject IAudioObject) : void in class net.sourceforge.atunes.kernel.modules.notify.OSDDialogController",
            "Extract Method private extractedMethod() : void extracted from public initialize() : void in class net.sourceforge.atunes.kernel.modules.navigator.FavoritesNavigationViewTablePopupMenu",
            "Extract Method private extractedMethod(file File) : void extracted from protected executeAction() : void in class net.sourceforge.atunes.kernel.actions.LoadPlayListAction")

        .atCommit("667a45ad8bcc94420d5c6d66b15b758c7eab1c1d")
        .contains(
            // FN** There is a inline indeed. Maybe it was not detected because code is small and because of parameter substitution
            "Inline Method private addHeaderRenderers(jtable JTable, model AbstractCommonColumnModel, lookAndFeel ILookAndFeel) : void inlined to public decorate(decorateHeader boolean) : void in class net.sourceforge.atunes.gui.ColumnDecorator",
            "Inline Method private finish(restart boolean) : void inlined to package finish() : void in class net.sourceforge.atunes.kernel.Finisher",
//              "Inline Method private finish(restart boolean) : void inlined to package restart() : void in class net.sourceforge.atunes.kernel.Finisher",
            "Inline Method private getArtistSongs(listOfObjectsDragged List<DragableArtist>) : boolean inlined to public processInternalImport(support TransferSupport) : boolean in class net.sourceforge.atunes.kernel.modules.draganddrop.InternalImportProcessor",
            "Inline Method private formatTrackNumber(trackNumber int) : String inlined to public getAudioFileStringValue(audioFile ILocalAudioObject) : String in class net.sourceforge.atunes.kernel.modules.pattern.TrackPattern",
//              "Inline Method private formatTrackNumber(trackNumber int) : String inlined to public getCDMetadataStringValue(metadata CDMetadata, trackNumber int) : String in class net.sourceforge.atunes.kernel.modules.pattern.TrackPattern",
            "Inline Method private persistRadios() : void inlined to public setLabel(radioList List<IRadio>, label String) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler")
//              "Inline Method private persistRadios() : void inlined to public addRadio(radio IRadio) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler",
//              "Inline Method private persistRadios() : void inlined to public removeRadios(radios List<IRadio>) : void in class net.sourceforge.atunes.kernel.modules.radio.RadioHandler")

        .atCommit("d2bcdb51d88f25a35e37342389ba09bcc52ddba9")
        .contains(
            // FN** Not really a move method. There is still a isSongFavorite method in FavoritesSongsManager, but it only delegates to FavoritesObjectDataStore#isSongFavorite
            "Move Method isSongFavorite from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesSongsManager to isSongFavorite from class net.sourceforge.atunes.kernel.modules.favorites.FavoritesObjectDataStore",
            "Move Method private isFadeAwayInProgress() : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerEngine to package isFadeAwayInProgress(mPlayerEngine MPlayerEngine) : boolean from class net.sourceforge.atunes.kernel.modules.player.mplayer.MPlayerCommandWriter",
            "Move Method package addToHistory(audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory to package addToHistory(playbackHistory PlaybackHistory, audioObject IAudioObject) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlaybackHistory.Heap",
            "Move Method public readObjectFromFile(inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XMLSerializerService to public readObjectFromFile(xmlSerializerService XMLSerializerService, inputStream InputStream) : Object from class net.sourceforge.atunes.utils.XStreamFactory",
            "Move Method public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.CryptoUtils to public encrypt(bytes byte[]) : byte[] from class net.sourceforge.atunes.utils.DateUtils")

        .atCommit("fec9b7bbc31a04c6f15137679faeb7fcfd6cd97d")
        .contains(
            "Move Attribute private imageSize : int from class net.sourceforge.atunes.kernel.modules.fullscreen.Cover to class net.sourceforge.atunes.kernel.modules.fullscreen.CoverFlow",
            "Move Attribute private versionProperty : String from class net.sourceforge.atunes.kernel.modules.updates.UpdateHandler to class net.sourceforge.atunes.kernel.modules.updates.VersionXmlParser",
            "Move Attribute private process : Process from class net.sourceforge.atunes.kernel.modules.cdripper.NeroAacEncoder to class net.sourceforge.atunes.kernel.modules.cdripper.CdRipper",
            "Move Attribute private albumFavoriteSearchOperator : ISearchUnaryOperator<IAlbum> from class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField to class net.sourceforge.atunes.kernel.modules.search.AlbumArtistSearchField",
            "Move Attribute private old : boolean from class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeedEntry to class net.sourceforge.atunes.kernel.modules.podcast.PodcastFeed")
        .atCommit("fec9b7bbc31a04c6f15137679faeb7fcfd6cd97d")
        // FP** one-liner, more like encapsulate field
        .notContains("Extract Method private getAlbumFavoriteSearchOperator() : ISearchUnaryOperator extracted from * in class net.sourceforge.atunes.kernel.modules.search.AlbumSearchField")
            
        .atCommit("ca59aa0f0ad21a3b4a31b8f172dc941c59207c63")
        .contains(
            "Push Down Attribute private frame : IFrame from class net.sourceforge.atunes.kernel.AbstractHandler to class net.sourceforge.atunes.kernel.modules.cdripper.RipperHandler",
            "Push Down Attribute private dialogFactory : IDialogFactory from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
            "Push Down Attribute private columnSet : IColumnSet from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to class net.sourceforge.atunes.kernel.modules.search.SearchResultTableModel",
            "Push Down Attribute private networkHandler : INetworkHandler from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricsDirectoryEngine",
            "Push Down Attribute private cancelButton : JButton from class net.sourceforge.atunes.gui.views.dialogs.ProgressDialog to class net.sourceforge.atunes.gui.views.dialogs.TransferProgressDialog")

        .atCommit("8ecd468c8cb6ff85be55249a82497f7c3dcc46e1")
        .contains(
            "Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrDBEngine",
            "Push Down Method public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.ProgressDialog to public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.TransferProgressDialog",
            "Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
            "Push Down Method public sort(column IColumn<?>) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn<?>) : void from class net.sourceforge.atunes.gui.AlbumTableModel",
            "Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.AlbumTreeCellDecorator")

        .atCommit("f061c05f845933f3e9ca8dba2b37c24b2ff164f2")
        .contains(
            "Pull Up Attribute protected desktop : IDesktop from class net.sourceforge.atunes.gui.views.dialogs.MakeDonationDialog to class net.sourceforge.atunes.gui.views.controls.AbstractCustomDialog",
            "Pull Up Attribute protected albumColumnSet : IColumnSet from class net.sourceforge.atunes.gui.AlbumTableColumnModel to class net.sourceforge.atunes.gui.AbstractCommonColumnModel",
            "Pull Up Attribute protected eventIcon : IIconFactory from class net.sourceforge.atunes.kernel.modules.context.event.EventsContextPanel to class net.sourceforge.atunes.kernel.modules.context.AbstractContextPanel",
            "Pull Up Attribute protected rightSplitPane : JSplitPane from class net.sourceforge.atunes.gui.frame.CommonSingleFrame to class net.sourceforge.atunes.gui.frame.AbstractSingleFrame",
            "Pull Up Attribute protected navigatorSplitPane : JSplitPane from class net.sourceforge.atunes.gui.frame.DefaultSingleFrame to class net.sourceforge.atunes.gui.frame.MainSplitPaneLeftSingleFrame")

        .atCommit("566cbfcc517e5d7e7372b893bb5ea779b7397ffd")
        .contains(
            "Pull Up Method public getButtonOutline(button AbstractButton, insets Insets, w int, h int, isInner boolean) : Shape from class net.sourceforge.atunes.gui.lookandfeel.substance.RoundRectButtonShaper to public getButtonOutline(button AbstractButton, insets Insets, w int, h int, isInner boolean) : Shape from class net.sourceforge.atunes.gui.lookandfeel.substance.AbstractButtonShaper",
            "Pull Up Method private applyVerticalSplitPaneDividerPosition(splitPane JSplitPane, location int, relPos double) : void from class net.sourceforge.atunes.gui.frame.CommonSingleFrame to protected applyVerticalSplitPaneDividerPosition(splitPane JSplitPane, location int, relPos double) : void from class net.sourceforge.atunes.gui.frame.AbstractSingleFrame",
            "Pull Up Method protected getNavigationTablePanelMaximumSize() : Dimension from class net.sourceforge.atunes.gui.frame.NavigatorTopPlayListBottomSingleFrame to protected getNavigationTablePanelMaximumSize() : Dimension from class net.sourceforge.atunes.gui.frame.MainSplitPaneRightSingleFrame",
            // FN** This method has only one line and a field access was replaced by a method invocation (getter)
            "Pull Up Method public initialize() : void from class net.sourceforge.atunes.gui.AlbumTableColumnModel to public initialize() : void from class net.sourceforge.atunes.gui.AbstractCommonColumnModel",
            "Pull Up Method public setLine2(text String) : void from class net.sourceforge.atunes.gui.views.dialogs.ExtendedToolTip to public setLine2(text String) : void from class net.sourceforge.atunes.gui.views.controls.AbstractCustomWindow")
        ;
        
        test.assertExpectations();
    }

}

