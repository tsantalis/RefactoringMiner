package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.tool.Grammar;

/** Track everything associated with the state of the preview window.
 *  For each grammar, we need to track an InputPanel (with <= 2 editor objects)
 *  that we will flip to every time we come back to a specific grammar,
 *  uniquely identified by the fully-qualified grammar name.
 *
 *  Before parsing can begin, we need to know the start rule. That means that
 *  we should not show an editor until this field is filled in.
 *
 *  The plug-in controller should update all of these elements atomically so
 *  they are self consistent.  We must be careful then to send these fields
 *  around together as a unit instead of asking the controller for the
 *  elements piecemeal. That could get g and lg for different grammar files,
 *  for example.
 */
public class PreviewState {
	public String grammarFileName;
	public Grammar g;
	public Grammar lg;
	public String startRuleName;

	public Parser parser;
	public SyntaxErrorListener syntaxErrorListener;

//	public InputPanel inputPanel;

	/** The current input editor (inputEditor or fileEditor) for this grammar */
	public Editor editor;
	public Editor inputEditor;
	public Editor fileEditor;

	public Editor getEditor() {
		return editor;
	}

	public boolean isInputEditor() {
		return editor == inputEditor;
	}

	public boolean isFileEditor() {
		return editor == fileEditor;
	}

	public synchronized void setInputEditor(Editor editor) {
		this.inputEditor = editor;
		this.editor = editor;
	}

	public synchronized void setFileEditor(Editor editor) {
		this.fileEditor = editor;
		this.editor = editor;
	}

	public PreviewState(String grammarFileName) {
		this.grammarFileName = grammarFileName;
	}

	public void releaseEditors() {
		final EditorFactory factory = EditorFactory.getInstance();
		synchronized (this) {
			// It would appear that the project closed event occurs before these close grammars. Very strange.
			// check for null editor.
			if (fileEditor != null) {
				factory.releaseEditor(fileEditor);
				fileEditor = null;
			}
			if (inputEditor != null) {
				factory.releaseEditor(inputEditor);
				inputEditor = null;
			}
			editor = null;
			inputEditor = null;
			fileEditor = null;
		}
	}

}
