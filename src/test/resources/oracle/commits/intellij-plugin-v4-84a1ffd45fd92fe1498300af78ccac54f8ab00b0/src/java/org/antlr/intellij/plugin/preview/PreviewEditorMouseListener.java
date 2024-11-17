package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.actions.MyActionUtils;

import java.awt.event.MouseEvent;

public class PreviewEditorMouseListener extends EditorMouseMotionAdapter {
	public InputPanel inputPanel;

	public PreviewEditorMouseListener(InputPanel inputPanel) {
		this.inputPanel = inputPanel;
	}

	@Override
	public void mouseMoved(EditorMouseEvent e){
		if ( e.getArea()!=EditorMouseEventArea.EDITING_AREA ) {
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		Editor editor=e.getEditor();
		int offset = MyActionUtils.getMouseOffset(mouseEvent, editor);

		if ( offset >= editor.getDocument().getTextLength() ) {
			return;
		}

		// get state for grammar in current editor, not editor where user is typing preview input!
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(editor.getProject());
		PreviewState previewState =	controller.getPreviewState();
		if ( previewState==null ) {
			return;
		}

		// Mouse has moved so make sure we don't show any token information tooltips
		InputPanel.removeTokenInfoHighlighters(editor);

//		System.out.println("offset="+offset);

		if ( mouseEvent.isMetaDown() ) {
			inputPanel.showTokenInfoUponMeta(editor, previewState, offset);
		}
		else { // just moving around, show any errors
			inputPanel.showTooltipsForErrors(editor, previewState, offset);
		}
	}

}
