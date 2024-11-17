package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4ParserDefinition;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

public class PreviewEditorMouseListener extends EditorMouseMotionAdapter {
	@Override
	public void mouseMoved(EditorMouseEvent e){
		if ( e.getArea()!=EditorMouseEventArea.EDITING_AREA ) {
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		Editor editor=e.getEditor();
		Point point=new Point(mouseEvent.getPoint());
		LogicalPosition pos=editor.xyToLogicalPosition(point);
		int offset=editor.logicalPositionToOffset(pos);

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
		PreviewPanel.removeTokenInfoHighlighters(editor);

//		System.out.println("offset="+offset);

		if ( mouseEvent.isMetaDown() ) {
			showTokenInfoUponMeta(editor, previewState, offset);
		}
		else { // just moving around, show any errors
			showTooltipsForErrors(editor, previewState, offset);
		}
	}

	/** Show token information if the meta-key is down and mouse movement occurs */
	public void showTokenInfoUponMeta(Editor editor, PreviewState previewState, int offset) {
		CommonTokenStream tokenStream =
			(CommonTokenStream)previewState.parser.getInputStream();

		Token tokenUnderCursor = ANTLRv4ParserDefinition.getTokenUnderCursor(tokenStream, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

//		System.out.println("token = "+tokenUnderCursor);
		String channelInfo = "";
		int channel = tokenUnderCursor.getChannel();
		if ( channel!=Token.DEFAULT_CHANNEL ) {
			String chNum = channel==Token.HIDDEN_CHANNEL ? "hidden" : String.valueOf(channel);
			channelInfo = ", Channel "+ chNum;
		}
		String tokenInfo =
			String.format("Type %s, Line %d:%d, Index %d%s",
						  previewState.g.getTokenDisplayName(tokenUnderCursor.getType()),
						  tokenUnderCursor.getLine(),
						  tokenUnderCursor.getCharPositionInLine(),
						  tokenUnderCursor.getTokenIndex(),
						  channelInfo
			);
		MarkupModel markupModel = PreviewPanel.removeTokenInfoHighlighters(editor);

		// Underline
		CaretModel caretModel = editor.getCaretModel();
		final TextAttributes attr=new TextAttributes();
		attr.setForegroundColor(JBColor.BLUE);
		attr.setEffectColor(JBColor.BLUE);
		attr.setEffectType(EffectType.LINE_UNDERSCORE);
		markupModel.addRangeHighlighter(tokenUnderCursor.getStartIndex(),
										tokenUnderCursor.getStopIndex()+1,
										PreviewPanel.TOKEN_INFO_LAYER, // layer
										attr,
										HighlighterTargetArea.EXACT_RANGE);

		// HINT
		caretModel.moveToOffset(offset); // info tooltip only shows at cursor :(
		HintManager.getInstance().showInformationHint(editor, tokenInfo);
	}

	/** Display syntax errors in tooltips if under the cursor */
	public void showTooltipsForErrors(Editor editor, @NotNull PreviewState previewState, int offset) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(editor.getProject());
		MarkupModel markupModel = editor.getMarkupModel();

		SyntaxError errorUnderCursor =
			ANTLRv4ParserDefinition.getErrorUnderCursor(previewState.syntaxErrorListener.getSyntaxErrors(), offset);
		if (errorUnderCursor == null) {
			// Turn off any tooltips if none under the cursor
			HintManager.getInstance().hideAllHints();
			return;
		}

//		System.out.println("# highlighters=" + markupModel.getAllHighlighters().length);

		// find the highlighter associated with this error by finding error at this offset
		int i = 1;
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
//			System.out.println("highlighter: "+r);
			int a = r.getStartOffset();
			int b = r.getEndOffset();
//			System.out.printf("#%d: %d..%d %s\n", i, a, b, r.toString());
			i++;
			if (offset >= a && offset < b) { // cursor is over some kind of highlighting
				TextAttributes attr = r.getTextAttributes();
				if (attr != null && attr.getEffectType() == EffectType.WAVE_UNDERSCORE) {
					// error tool tips
					String errorDisplayString =
						controller.getPreviewPanel().getErrorDisplayString(errorUnderCursor);
					int flags =
						HintManager.HIDE_BY_ANY_KEY |
							HintManager.HIDE_BY_TEXT_CHANGE |
							HintManager.HIDE_BY_SCROLLING;
					int timeout = 0; // default?
					HintManager.getInstance().showErrorHint(editor, errorDisplayString,
															offset, offset + 1,
															HintManager.ABOVE, flags, timeout);
					return;
				}
			}
		}
	}
}
