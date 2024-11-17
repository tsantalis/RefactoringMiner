package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The top level contents of the preview tool window created by
 *  intellij automatically. Since we need grammars to interpret,
 *  this object creates and caches lexer/parser grammars for
 *  each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel {
	public static final Logger LOG = Logger.getInstance("ANTLR PreviewPanel");
	public static final int TOKEN_INFO_LAYER = HighlighterLayer.SELECTION; // Show token info over errors
	public static final int ERROR_LAYER = HighlighterLayer.ERROR;

	/** switchToGrammar() was seeing an empty slot instead of a previous
	 *  editor or placeHolder. Figured it was an order of operations thing
	 *  and synchronized add/remove ops. Works now w/o error.
	 */
	public final Object swapEditorComponentLock = new Object();

	public static final JTextArea placeHolder = new JTextArea();

	public Project project;

	public InputPanel inputPanel;

	public TreeViewer treeViewer;
	public ParseTree lastTree;

	EditorMouseMotionAdapter editorMouseMoveListener = new PreviewEditorMouseListener();
	EditorMouseAdapter editorMouseListener = new EditorMouseAdapter() {
		@Override
		public void mouseExited(EditorMouseEvent e) {
			removeTokenInfoHighlighters(e.getEditor());
		}
	};

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());

		Splitter splitPane = new Splitter();
		inputPanel = createEditorPanel();
		splitPane.setFirstComponent(inputPanel);
		splitPane.setSecondComponent(createParseTreePanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	public InputPanel createEditorPanel() {
		LOG.info("createEditorPanel"+" "+project.getName());
		return new InputPanel(this);
/*
		editorConsole = new JTextArea();
		editorConsole.setRows(3);
		editorConsole.setEditable(false);
		editorConsole.setLineWrap(true);
		JBScrollPane spane = new JBScrollPane(editorConsole); // wrap in scroller
		inputPanel = new JPanel(new BorderLayout(0,0));

		JBPanel startRuleAndFilePanel = new JBPanel(new BorderLayout(0, 0));
		startRuleLabel = new JLabel(missingStartRuleLabelText);
		startRuleLabel.setForeground(JBColor.RED);
		startRuleAndFilePanel.add(startRuleLabel, BorderLayout.WEST);

		TextFieldWithBrowseButton fileChooser = new TextFieldWithBrowseButton();
		FileChooserDescriptor singleFileDescriptor =
		        FileChooserDescriptorFactory.createSingleFolderDescriptor();
		fileChooser.addBrowseFolderListener("Select input file", null, project, singleFileDescriptor);
		fileChooser.setTextFieldPreferredWidth(40);

		JRadioButton manualButton = new JRadioButton("Manual");
		manualButton.setSelected(true);
		JRadioButton fileButton = new JRadioButton("File");
		manualButton.setSelected(false);
		ButtonGroup group = new ButtonGroup();
		group.add(manualButton);
		group.add(fileButton);

		JPanel radioPanel = new JPanel(new GridLayout(1, 2));
		radioPanel.add(manualButton);
		JPanel f = new JPanel();
		f.add(fileButton);
		f.add(fileChooser);
		radioPanel.add(fileButton);
		radioPanel.add(f);

		startRuleAndFilePanel.add(startRuleLabel, BorderLayout.WEST);
		startRuleAndFilePanel.add(radioPanel, BorderLayout.CENTER);

		synchronized ( swapEditorComponentLock ) {
			inputPanel.add(startRuleAndFilePanel, BorderLayout.NORTH);
			inputPanel.add(placeHolder, BorderLayout.CENTER);
			inputPanel.add(spane, BorderLayout.SOUTH);
		}
		return inputPanel;
		*/
	}

	public JPanel createParseTreePanel() {
		LOG.info("createParseTreePanel"+" "+project.getName());
		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0,0));
		treePanel.setBackground(JBColor.white);
		// Wrap tree viewer component in scroll pane
		treeViewer = new TreeViewer(null, null);
		JScrollPane scrollPane = new JBScrollPane(treeViewer); // use Intellij's scroller
		treePanel.add(scrollPane, BorderLayout.CENTER);

		// Add scale slider to bottom, under tree view scroll panel
		int sliderValue = (int) ((treeViewer.getScale()-1.0) * 1000);
		final JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL,
										  -999,1000,sliderValue);
		scaleSlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int v = scaleSlider.getValue();
					if ( lastTree!=null ) {
						treeViewer.setScale(v / 1000.0 + 1.0);
					}
				}
			}
									 );
		treePanel.add(scaleSlider, BorderLayout.SOUTH);
		return treePanel;
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile grammarFile) {
		switchToGrammar(grammarFile);
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammar(newFile);
	}

	/** Load grammars and set editor component. */
	public void switchToGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar " + grammarFileName+" "+project.getName());
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFileName);

		if ( previewState.editor==null ) { // this grammar is new; no editor yet
			previewState.editor = createEditor(grammarFile, ""); // nothing there, create
		}

		BorderLayout layout = (BorderLayout) inputPanel.getLayout();
		// atomically remove old
		synchronized ( swapEditorComponentLock ) {
			Component editorSpotComp = layout.getLayoutComponent(BorderLayout.CENTER);
			if (editorSpotComp != null) {
				inputPanel.remove(editorSpotComp); // remove old editor if it's there
			}
			inputPanel.add(previewState.editor.getComponent(), BorderLayout.CENTER);
		}
		clearParseErrors(grammarFile);

		if ( previewState.startRuleName!=null ) {
			inputPanel.setStartRuleName(grammarFile, previewState.startRuleName);
			updateParseTreeFromDoc(grammarFile);
		}
		else {
			inputPanel.resetStartRuleLabel();
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}
	}

	public void closeGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("closeGrammar "+grammarFileName+" "+project.getName());

		inputPanel.resetStartRuleLabel();
		inputPanel.clearErrorConsole();
		setParseTree(Arrays.asList(new String[0]), null); // wipe tree

		// release the editor
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFileName);
		final EditorFactory factory = EditorFactory.getInstance();
		synchronized ( controller.previewStateLock ) {
			// It would appear that the project closed event occurs before these close grammars. Very strange.
			// check for null editor.
			if ( previewState.editor!=null ) {
				factory.releaseEditor(previewState.editor);
				previewState.editor = null;
			}
		}

		// restore the GUI
		BorderLayout layout = (BorderLayout) inputPanel.getLayout();
		Component editorSpotComp = layout.getLayoutComponent(BorderLayout.CENTER);
		synchronized ( swapEditorComponentLock ) {
			inputPanel.remove(editorSpotComp);
			inputPanel.add(placeHolder, BorderLayout.CENTER); // put placeholder back after we remove the editor component.
		}
	}

	public void setParseTree(final List<String> ruleNames, final ParseTree tree) {
		ApplicationManager.getApplication().invokeLater(
			new Runnable() {
				@Override
				public void run() {
					lastTree = tree;
					treeViewer.setRuleNames(ruleNames);
					treeViewer.setTree(tree);
				}
			}
		);
	}

	public Editor createEditor(final VirtualFile grammarFile, String inputText) {
		LOG.info("createEditor: create new editor for "+grammarFile.getPath()+" "+project.getName());
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(inputText);
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent event) {
					updateParseTreeFromDoc(grammarFile);
				}
			});
		final Editor editor = factory.createEditor(doc, project);
		EditorSettings settings = editor.getSettings();
		settings.setWhitespacesShown(true); // hmm...doesn't work.  maybe show when showing token tooltip?

		editor.addEditorMouseMotionListener(editorMouseMoveListener);
		editor.addEditorMouseListener(editorMouseListener);

		return editor;
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		try {
			ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
			PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
			final String inputText = previewState.editor.getDocument().getText();
			Object[] results =
				controller.parseText(grammarFile, inputText);
			if (results != null) {
				ParseTree root = (ParseTree) results[1];
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), root);
			}
			else {
				setParseTree(Arrays.asList(new String[0]), null);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void clearParseErrors(VirtualFile grammarFile) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		Editor editor = previewState.editor;
		MarkupModel markupModel = editor.getMarkupModel();
		markupModel.removeAllHighlighters();

		HintManager.getInstance().hideAllHints();

		inputPanel.clearErrorConsole();
	}

	/** Display error messages to the console and also add annotations
	 *  to the preview input window.
	 */
	public void showParseErrors(final VirtualFile grammarFile, final List<SyntaxError> errors) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		MarkupModel markupModel = previewState.editor.getMarkupModel();
		if ( errors.size()==0 ) {
			markupModel.removeAllHighlighters();
			return;
		}
		for (SyntaxError e : errors) {
			annotateErrorsInPreviewInputEditor(grammarFile, e);
			inputPanel.displayErrorInParseErrorConsole(e);
		}
	}

	public void annotateErrorsInPreviewInputEditor(VirtualFile grammarFile, SyntaxError e) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		Editor editor = previewState.editor;
		MarkupModel markupModel = editor.getMarkupModel();

		int a,b; // Start and stop index
		RecognitionException cause = e.getException();
		if ( cause instanceof LexerNoViableAltException ) {
			a = ((LexerNoViableAltException) cause).getStartIndex();
			b = ((LexerNoViableAltException) cause).getStartIndex()+1;
		}
		else {
			Token offendingToken = (Token)e.getOffendingSymbol();
			a = offendingToken.getStartIndex();
			b = offendingToken.getStopIndex()+1;
		}
		final TextAttributes attr=new TextAttributes();
		attr.setForegroundColor(JBColor.RED);
		attr.setEffectColor(JBColor.RED);
		attr.setEffectType(EffectType.WAVE_UNDERSCORE);
		RangeHighlighter rangehighlighter=
			markupModel.addRangeHighlighter(a,
											b,
											ERROR_LAYER, // layer
											attr,
											HighlighterTargetArea.EXACT_RANGE);
	}

	public static MarkupModel removeTokenInfoHighlighters(Editor editor) {
		// Remove any previous underlining, but not anything else like errors
		MarkupModel markupModel=editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			TextAttributes attr = r.getTextAttributes();
			if ( attr!=null && attr.getEffectType() == EffectType.LINE_UNDERSCORE ) {
				markupModel.removeHighlighter(r);
			}
		}
		return markupModel;
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}
}
