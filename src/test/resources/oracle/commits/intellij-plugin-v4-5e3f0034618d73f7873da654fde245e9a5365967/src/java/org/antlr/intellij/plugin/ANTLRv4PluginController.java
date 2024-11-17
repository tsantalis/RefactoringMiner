package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** This object is the controller for the ANTLR plug-in. It receives
 *  events and can send them on to its contained components. For example,
 *  saving the grammar editor or flipping to a new grammar sends an event
 *  to this object, which forwards on update events to the preview tool window.
 *
 *  The main components are related to the console tool window forever output and
 *  the main panel of the preview tool window.
 *
 *  This controller also manages the cache of grammar/editor combinations
 *  needed for the preview window. Updates must be made atomically so that
 *  the grammars and editors are consistently associated with the same window.
 */
public class ANTLRv4PluginController implements ProjectComponent {
	public Grammar BAD_PARSER_GRAMMAR;
	public LexerGrammar BAD_LEXER_GRAMMAR;

	public static final Logger LOG = Logger.getInstance("ANTLR ANTLRv4PluginController");
	public static final String PREVIEW_WINDOW_ID = "ANTLR Preview";
	public static final String CONSOLE_WINDOW_ID = "ANTLR Tool Output";

	public final Object previewStateLock = new Object();

	public boolean projectIsClosed = false;

	public Project project;
	public ConsoleView console;
	public ToolWindow consoleWindow;

	public Map<String, PreviewState> grammarToPreviewState =
		Collections.synchronizedMap(new HashMap<String, PreviewState>());
	public ToolWindow previewWindow;	// same for all grammar editor
	public PreviewPanel previewPanel;	// same for all grammar editor

	public MyVirtualFileAdapter myVirtualFileAdapter = new MyVirtualFileAdapter();
	public MyFileEditorManagerAdapter myFileEditorManagerAdapter = new MyFileEditorManagerAdapter();

	public ANTLRv4PluginController(Project project) {
		this.project = project;
	}

	public static ANTLRv4PluginController getInstance(Project project) {
		ANTLRv4PluginController pc = project.getComponent(ANTLRv4PluginController.class);
		return pc;
	}

	@Override
	public void initComponent() {
		try {
			BAD_PARSER_GRAMMAR = new Grammar("grammar BAD; a : 'bad' ;");
			BAD_PARSER_GRAMMAR.name = "BAD_PARSER_GRAMMAR";
			BAD_LEXER_GRAMMAR = new LexerGrammar("lexer grammar BADLEXER; A : 'bad' ;");
			BAD_LEXER_GRAMMAR.name = "BAD_LEXER_GRAMMAR";
		}
		catch (org.antlr.runtime.RecognitionException re) {
			LOG.error("can't init bad grammar markers");
		}
	}

	@Override
	public void projectOpened() {
		// make sure the tool windows are created early
		createToolWindows();
		installListeners();
	}

	public void createToolWindows() {
		LOG.info("createToolWindows "+project.getName());
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

		previewPanel = new PreviewPanel(project);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(previewPanel, "", false);

		previewWindow = toolWindowManager.registerToolWindow(PREVIEW_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		previewWindow.getContentManager().addContent(content);

		TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = factory.createBuilder(project);
		this.console = consoleBuilder.getConsole();

		JComponent consoleComponent = console.getComponent();
		content = contentFactory.createContent(consoleComponent, "", false);

		consoleWindow = toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		consoleWindow.getContentManager().addContent(content);
	}

	@Override
	public void projectClosed() {
		LOG.info("projectClosed " + project.getName());
		//synchronized ( shutdownLock ) { // They should be called from EDT only so no lock
		projectIsClosed = true;
		uninstallListeners();

		console.dispose();

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		for (PreviewState it : grammarToPreviewState.values()) {
			synchronized (controller.previewStateLock) {
				if (it.editor != null) {
					final EditorFactory factory = EditorFactory.getInstance();
					factory.releaseEditor(it.editor);
					it.editor = null;
				}
			}
		}

		previewPanel = null;
		previewWindow = null;
		consoleWindow = null;
		project = null;
		grammarToPreviewState = null;
	}

	// seems that intellij can kill and reload a project w/o user knowing.
	// a ptr was left around that pointed at a disposed project. led to
	// problem in switchGrammar. Probably was a listener still attached and trigger
	public void uninstallListeners() {
		VirtualFileManager.getInstance().removeVirtualFileListener(myVirtualFileAdapter);
		MessageBusConnection msgBus = project.getMessageBus().connect(project);
		msgBus.disconnect();
	}

	@Override
	public void disposeComponent() {
	}

	@NotNull
	@Override
	public String getComponentName() {
		return "antlr.ProjectComponent";
	}

	// ------------------------------

	public void installListeners() {
		LOG.info("installListeners "+project.getName());
		// Listen for .g4 file saves
		VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileAdapter);

		// Listen for editor window changes
		MessageBusConnection msgBus = project.getMessageBus().connect(project);
		msgBus.subscribe(
			FileEditorManagerListener.FILE_EDITOR_MANAGER,
			myFileEditorManagerAdapter
		);

		// for now let's leave the grammar loading in the selectionChanged event
		// as jetbrains says that I should not rely on event order.
//		msgBus.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER,
//						 new FileEditorManagerListener.Before.Adapter() {
//							 @Override
//							 public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//								 fileOpenedEvent(file);
//							 }
//						 }
//						 );
	}

	/** The test ANTLR rule action triggers this event. This can occur
	 *  only occur when the current editor the showing a grammar, because
	 *  that is the only time that the action is enabled. We will see
	 *  a file changed event when the project loads the first grammar file.
	 */
	public void setStartRuleNameEvent(VirtualFile grammarFile, String startRuleName) {
		LOG.info("setStartRuleNameEvent " + startRuleName+" "+project.getName());
		PreviewState previewState = getPreviewState(grammarFile.getPath());
		previewState.startRuleName = startRuleName;
		if ( previewPanel!=null ) {
			previewPanel.getInputPanel().setStartRuleName(grammarFile, startRuleName); // notify the view
			previewPanel.updateParseTreeFromDoc(grammarFile);
		}
		else {
			LOG.error("setStartRuleNameEvent called before preview panel created");
		}
	}

	public void grammarFileSavedEvent(VirtualFile grammarFile) {
		LOG.info("grammarFileSavedEvent "+grammarFile.getPath()+" "+project.getName());
		updateGrammarObjectsFromFile(grammarFile.getPath()); // force reload
		if ( previewPanel!=null ) {
			previewPanel.grammarFileSaved(grammarFile);
		}
		else {
			LOG.error("grammarFileSavedEvent called before preview panel created");
		}
		runANTLRTool(grammarFile);
	}

	public void currentEditorFileChangedEvent(VirtualFile oldFile, VirtualFile newFile) {
		LOG.info("currentEditorFileChangedEvent "+(oldFile!=null?oldFile.getPath():"none")+
				 " -> "+(newFile!=null?newFile.getPath():"none")+" "+project.getName());
		if ( newFile==null ) { // all files must be closed I guess
			return;
		}
		if ( newFile.getName().endsWith(".g") ) {
			LOG.info("currentEditorFileChangedEvent ANTLR 4 cannot handle .g files, only .g4");
			previewWindow.hide(null);
			return;
		}
		if ( !newFile.getName().endsWith(".g4") ) {
			previewWindow.hide(null);
			return;
		}
		PreviewState previewState = getPreviewState(newFile.getPath());
		if ( previewState.g==null ) { // only load grammars if not there
			updateGrammarObjectsFromFile(newFile.getPath());
		}
		if ( previewPanel!=null ) {
			previewPanel.grammarFileChanged(oldFile, newFile);
		}
	}

//	public void fileOpenedEvent(VirtualFile vfile) {
//		String grammarFileName = vfile.getPath();
//		LOG.info("fileOpenedEvent "+ grammarFileName+" "+project.getName());
//		if ( !vfile.getName().endsWith(".g4") ) {
//			ApplicationManager.getApplication().invokeLater(
//				new Runnable() {
//					@Override
//					public void run() {
//						previewWindow.hide(null);
//					}
//				}
//			);
//		}
//	}
//
	public void editorFileClosedEvent(VirtualFile vfile) {
		String grammarFileName = vfile.getPath();
		LOG.info("editorFileClosedEvent "+ grammarFileName+" "+project.getName());
		if ( !vfile.getName().endsWith(".g4") ) {
			previewWindow.hide(null);
			return;
		}

		// Dispose of state, editor, and such for this file
		PreviewState previewState = grammarToPreviewState.get(grammarFileName);
		if ( previewState==null ) { // project closing must have done already
			return;
		}

		previewPanel.closeGrammar(vfile);

		grammarToPreviewState.remove(grammarFileName);

		// close tool window
		previewWindow.hide(null);
	}

	public void runANTLRTool(final VirtualFile grammarFile) {
		LOG.info("runANTLRTool launch on "+grammarFile.getPath()+" "+project.getName());
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;
		Task.Backgroundable gen =
			new RunANTLROnGrammarFile(grammarFile,
									  project,
									  title,
									  canBeCancelled,
									  new BackgroundFromStartOption());
		ProgressManager.getInstance().run(gen);
	}

	/** Look for state information concerning this grammar file and update
	 *  the Grammar objects.  This does not necessarily update the grammar file
	 *  in the current editor window.  Either we are already looking at
	 *  this grammar or we will have seen a grammar file changed event.
	 *  (I hope!)
	 */
	public void updateGrammarObjectsFromFile(String grammarFileName) {
		synchronized ( previewStateLock ) { // build atomically
			PreviewState previewState = getPreviewState(grammarFileName);
			/* run later */ Grammar[] grammars = loadGrammars(grammarFileName);
			if ( grammars!=null ) {
				previewState.lg = grammars[0];
				previewState.g = grammars[1];
			}
		}
	}

	public Object[] parseText(VirtualFile grammarFile, String inputText) throws IOException {
		// TODO:Try to reuse the same parser and lexer.
		String grammarFileName = grammarFile.getPath();
		PreviewState previewState = getPreviewState(grammarFileName);
		if (!new File(grammarFileName).exists()) {
			LOG.error("parseText grammar doesn't exit " + grammarFileName);
			return null;
		}

		// Wipes out the console and also any error annotations
		previewPanel.clearParseErrors(grammarFile);

		if ( previewState.g == BAD_PARSER_GRAMMAR ||
			 previewState.lg == BAD_LEXER_GRAMMAR )
		{
			return null;
		}

		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		lexEngine = previewState.lg.createLexerInterpreter(input);

		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		ParserInterpreter parser = previewState.g.createParserInterpreter(tokens);
		previewState.parser = parser;

		previewState.syntaxErrorListener = new SyntaxErrorListener();
		parser.removeErrorListeners();
		parser.addErrorListener(previewState.syntaxErrorListener);
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(previewState.syntaxErrorListener);

		Rule start = previewState.g.getRule(previewState.startRuleName);
		if ( start==null ) {
			return null; // can't find start rule
		}
		ParseTree t = parser.parse(start.index);

		previewPanel.showParseErrors(grammarFile, previewState.syntaxErrorListener.getSyntaxErrors());

		if ( t!=null ) {
			return new Object[] {parser, t};
		}
		return null;
	}

	/** Get lexer and parser grammars */
	public Grammar[] loadGrammars(String grammarFileName) {
		LOG.info("loadGrammars open "+grammarFileName+" "+project.getName());
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		MyANTLRToolListener listener = new MyANTLRToolListener(antlr);
		antlr.addListener(listener);

		String combinedGrammarFileName = null;
		String lexerGrammarFileName = null;
		String parserGrammarFileName = null;

		// basically here I am importing the loadGrammar() method from Tool
		// so that I can check for an empty AST coming back.
		GrammarRootAST grammarRootAST = antlr.parseGrammar(grammarFileName);
		if ( grammarRootAST==null ) {
			LOG.info("Empty or bad grammar "+grammarFileName+" "+project.getName());
			return null;
		}
		Grammar g = antlr.createGrammar(grammarRootAST);
		g.fileName = grammarFileName;
		antlr.process(g, false);

		// examine's Grammar AST from v4 itself;
		// hence use ANTLRParser.X not ANTLRv4Parser from this plugin
		switch ( g.getType() ) {
			case ANTLRParser.PARSER :
				parserGrammarFileName = grammarFileName;
				int i = grammarFileName.indexOf("Parser");
				if ( i>=0 ) {
					lexerGrammarFileName = grammarFileName.substring(0, i) + "Lexer.g4";
				}
				break;
			case ANTLRParser.LEXER :
				lexerGrammarFileName = grammarFileName;
				int i2 = grammarFileName.indexOf("Lexer");
				if ( i2>=0 ) {
					parserGrammarFileName = grammarFileName.substring(0, i2) + "Parser.g4";
				}
				break;
			case ANTLRParser.COMBINED :
				combinedGrammarFileName = grammarFileName;
				lexerGrammarFileName = grammarFileName+"Lexer";
				parserGrammarFileName = grammarFileName+"Parser";
				break;
		}

		if ( lexerGrammarFileName==null ) {
			LOG.error("Can't compute lexer file name from "+grammarFileName, (Throwable)null);
			return null;
		}
		if ( parserGrammarFileName==null ) {
			LOG.error("Can't compute parser file name from "+grammarFileName, (Throwable)null);
			return null;
		}

		LexerGrammar lg = null;

		if ( combinedGrammarFileName!=null ) {
			// already loaded above
			lg = g.getImplicitLexer();
			if ( listener.grammarErrorMessage!=null ) {
				g = null;
			}
		}
		else {
			try {
				lg = (LexerGrammar)Grammar.load(lexerGrammarFileName);
			}
			catch (ClassCastException cce) {
				LOG.error("File " + lexerGrammarFileName + " isn't a lexer grammar", cce);
				lg = null;
			}
			if ( listener.grammarErrorMessage!=null ) {
				lg = null;
			}
			g = loadGrammar(antlr, parserGrammarFileName, lg);
		}

		if ( g==null ) {
			LOG.info("loadGrammars parser "+parserGrammarFileName+" has errors");
			g = BAD_PARSER_GRAMMAR;
		}
		if ( lg==null ) {
			LOG.info("loadGrammars lexer "+lexerGrammarFileName+" has errors");
			lg = BAD_LEXER_GRAMMAR;
		}
		LOG.info("loadGrammars "+lg.getRecognizerName()+", "+g.getRecognizerName());
		return new Grammar[] {lg, g};
	}


	/** Same as loadGrammar(fileName) except import vocab from existing lexer */
	public static Grammar loadGrammar(Tool tool, String fileName, LexerGrammar lexerGrammar) {
		GrammarRootAST grammarRootAST = tool.parseGrammar(fileName);
		final Grammar g = tool.createGrammar(grammarRootAST);
		g.fileName = fileName;
		g.importVocab(lexerGrammar);
		tool.process(g, false);
		return g;
	}

	public PreviewPanel getPreviewPanel() {
		return previewPanel;
	}

	public ConsoleView getConsole() {
		return console;
	}

	public ToolWindow getConsoleWindow() {
		return consoleWindow;
	}

	public ToolWindow getPreviewWindow() {
		return previewWindow;
	}

	public @NotNull PreviewState getPreviewState(String grammarFileName) {
		// Have we seen this grammar before?
		PreviewState stateForCurrentGrammar = grammarToPreviewState.get(grammarFileName);
		if ( stateForCurrentGrammar!=null ) {
			return stateForCurrentGrammar; // seen this before
		}

		// not seen, must create state
		stateForCurrentGrammar = new PreviewState(grammarFileName);
		grammarToPreviewState.put(grammarFileName, stateForCurrentGrammar);

		return stateForCurrentGrammar;
	}

	/** Get the state information associated with the grammar in the current
	 *  editor window. If there is no grammar in the editor window, return null.
	 *  If there is a grammar, return any existing preview state else
	 *  create a new one in store in the map.
	 */
	public @org.jetbrains.annotations.Nullable PreviewState getPreviewState() {
		VirtualFile currentGrammarFile = getCurrentGrammarFile();
		if ( currentGrammarFile==null ) {
			return null;
		}
		String currentGrammarFileName = currentGrammarFile.getPath();
		if ( currentGrammarFileName==null ) {
			return null; // we are not looking at a grammar file
		}
		return getPreviewState(currentGrammarFileName);
	}

	// These "get current editor file" routines should only be used
	// when you are sure the user is in control and is viewing the
	// right file (i.e., don't use these during project loading etc...)

	public static VirtualFile getCurrentEditorFile(Project project) {
		FileEditorManager fmgr = FileEditorManager.getInstance(project);
		VirtualFile files[] = fmgr.getSelectedFiles();
		if ( files.length == 0 ) {
			return null;
		}
		return files[0];
	}

	public VirtualFile getCurrentGrammarFile() {
		return getCurrentGrammarFile(project);
	}

	public static VirtualFile getCurrentGrammarFile(Project project) {
		VirtualFile f = getCurrentEditorFile(project);
		if ( f==null ) {
			return null;
		}
		if ( f.getName().endsWith(".g4") ) return f;
		return null;
	}

	static class MyANTLRToolListener extends DefaultToolListener {
		public String grammarErrorMessage;
		public MyANTLRToolListener(Tool tool) { super(tool); }

		@Override
		public void error(ANTLRMessage msg) {
//			super.error(msg);
			ST msgST = tool.errMgr.getMessageTemplate(msg);
			grammarErrorMessage = msgST.render();
			if (tool.errMgr.formatWantsSingleLineMessage()) {
				grammarErrorMessage = grammarErrorMessage.replace('\n', ' ');
			}
		}
	}

	private class MyVirtualFileAdapter extends VirtualFileAdapter {
		@Override
		public void contentsChanged(VirtualFileEvent event) {
			final VirtualFile vfile = event.getFile();
			if ( !vfile.getName().endsWith(".g4") ) return;
			if ( !projectIsClosed ) grammarFileSavedEvent(vfile);
		}
	}

	private class MyFileEditorManagerAdapter extends FileEditorManagerAdapter {
		@Override
		public void selectionChanged(FileEditorManagerEvent event) {
			if ( !projectIsClosed ) currentEditorFileChangedEvent(event.getOldFile(), event.getNewFile());
		}

		@Override
		public void fileClosed(FileEditorManager source, VirtualFile file) {
			if ( !projectIsClosed ) editorFileClosedEvent(file);
		}
	}
}
