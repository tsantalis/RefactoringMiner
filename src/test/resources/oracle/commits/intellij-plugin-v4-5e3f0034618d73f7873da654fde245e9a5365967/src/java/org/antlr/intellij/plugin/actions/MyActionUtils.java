package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;

public class MyActionUtils {
	public static void selectedFileIsGrammar(AnActionEvent e) {
		VirtualFile vfile = getGrammarFileFromEvent(e);
		if ( vfile==null ) {
			e.getPresentation().setEnabled(false);
			return;
		}
		e.getPresentation().setEnabled(true); // enable action if we're looking at grammar file
		e.getPresentation().setVisible(true);
	}

	public static VirtualFile getGrammarFileFromEvent(AnActionEvent e) {
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null || files.length==0 ) return null;
		VirtualFile vfile = files[0];
		if ( vfile!=null && vfile.getName().endsWith(".g4") ) {
			return vfile;
		}
		return null;
	}
}
