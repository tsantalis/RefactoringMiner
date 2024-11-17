package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
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

	public Project project;

	public InputPanel inputPanel;

	public TreeViewer treeViewer;
	public ParseTree lastTree;

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());

		Splitter splitPane = new Splitter();
		inputPanel = getEditorPanel();
		splitPane.setFirstComponent(inputPanel);
		splitPane.setSecondComponent(createParseTreePanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	public InputPanel getEditorPanel() {
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

		inputPanel.switchToGrammar(grammarFile);

		if ( previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(grammarFile);
		}
		else {
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}
	}

	public void closeGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("closeGrammar "+grammarFileName+" "+project.getName());

		inputPanel.resetStartRuleLabel();
		inputPanel.clearErrorConsole();
		clearParseTree(); // wipe tree

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		inputPanel.releaseEditor(previewState);
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

	public void clearParseTree() {
		setParseTree(Arrays.asList(new String[0]), null);
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		try {
			ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
			PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
			final String inputText = previewState.getEditor().getDocument().getText();
			Object[] results =
				controller.parseText(grammarFile, inputText);
			if (results != null) {
				ParseTree root = (ParseTree) results[1];
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), root);
			}
			else {
				clearParseTree();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}
}
