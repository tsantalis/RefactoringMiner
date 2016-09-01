package org.refactoringminer.test;

import static org.junit.Assert.*;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;

import org.junit.Test;

public class TestTempPushDown {

	@Test
	public void test() throws Exception {

		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "tmp");

		test.project("https://github.com/danilofes/atunes-refactorings.git", "master")

				.atCommit("8ecd468c8cb6ff85be55249a82497f7c3dcc46e1").containsOnly(
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.playlist.UpdateDynamicPlayListBackgroundWorker",
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.radio.RetrieveRadioBrowserDataBackgroundWorker",
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.CalculateSynchronizationBetweenDeviceAndPlayListBackgroundWorker",
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.DeleteFilesTask",
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.repository.RemoveFoldersFromDiskBackgroundWorker",
						"Push Down Method protected before() : void from class net.sourceforge.atunes.kernel.BackgroundWorkerWithIndeterminateProgress to protected before() : void from class net.sourceforge.atunes.kernel.modules.state.ValidateAndProcessPreferencesBackgroundWorker",
						"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrcEngine",
						"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyrDBEngine",
						"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricsDirectoryEngine",
						"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.LyricWikiEngine",
						"Push Down Method protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.AbstractLyricsEngine to protected encodeString(str String) : String from class net.sourceforge.atunes.kernel.modules.webservices.lyrics.WinampcnEngine",
						"Push Down Method public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.ProgressDialog to public initialize() : void from class net.sourceforge.atunes.gui.views.dialogs.TransferProgressDialog",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.AlbumTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.ArtistTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.FolderTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.GenreTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.IncompleteTagsTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.NavigationTreeRootTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.PodcastFeedTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.RadioTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.StringTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.UnknownElementTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.views.decorators.YearTreeCellDecorator",
						"Push Down Method public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.gui.AbstractTreeCellDecorator to public setLookAndFeelManager(lookAndFeelManager ILookAndFeelManager) : void from class net.sourceforge.atunes.kernel.modules.navigator.TooltipTreeCellDecorator",
						"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AlbumTableModel",
						"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.gui.NavigationTableModel",
						"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.kernel.modules.playlist.PlayListTableModel",
						"Push Down Method public sort(column IColumn) : void from class net.sourceforge.atunes.gui.AbstractColumnSetTableModel to public sort(column IColumn) : void from class net.sourceforge.atunes.kernel.modules.search.SearchResultTableModel")

		;

		test.project("https://github.com/danilofes/argouml-refactorings.git", "master")
				.atCommit("26b089d0c3f74abadcf8645f4f7618bdc0c3738f").containsOnly(
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
						"Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigRRect",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigText",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.uml.diagram.ui.SPFigEdgeModelElement",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionException",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionSignal",
						"Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramRealizationEdge",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddConstructor",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddInstanceVariable",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddOperation",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAssocComposite",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizCueCards",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizManyNames",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizMEName",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizNavigable",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizTooMany",
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
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByGoal",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByOffender",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByPoster",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByPriority",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByType",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigInterface",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigPackage",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.ui.StylePanelFigAssociationClass",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.ui.StylePanelFigMessage",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.use_case.ui.StylePanelFigUseCase",
						"Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
						"Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByDecision",
						"Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizBreakCircularComp",
						"Push Down Method public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizMEName to public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizOperName",
						"Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement",
						"Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramGeneralizationEdge",
						"Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ActionStateNotation",
						"Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigClass",
						"Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionDataType"

		)

		;

		test.assertExpectations();

	}

}
