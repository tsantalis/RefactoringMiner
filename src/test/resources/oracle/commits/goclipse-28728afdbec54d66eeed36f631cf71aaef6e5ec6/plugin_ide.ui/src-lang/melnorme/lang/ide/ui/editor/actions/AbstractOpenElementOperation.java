/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.ui.editor.actions;


import static melnorme.utilbox.core.CoreUtil.areEqual;
import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.utils.EclipseUtils;
import melnorme.lang.ide.ui.EditorSettings_Actual;
import melnorme.lang.ide.ui.editor.AbstractLangEditor;
import melnorme.lang.ide.ui.editor.EditorUtils;
import melnorme.lang.ide.ui.editor.EditorUtils.OpenNewEditorMode;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.lang.tooling.ops.FindDefinitionResult;
import melnorme.lang.tooling.ops.SourceLineColumnRange;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class AbstractOpenElementOperation extends AbstractEditorOperation {
	
	protected final SourceRange range; // range of element to open. Usually only offset matters
	protected final OpenNewEditorMode openEditorMode;
	protected final IProject project;
	protected final SourceOperationContext context;
	
	// prepare_items
	protected int line_0;
	protected int col_0;
	
	protected String statusErrorMessage;
	protected FindDefinitionResult findResult;
	
	public AbstractOpenElementOperation(String operationName, ITextEditor editor, SourceRange range,
			OpenNewEditorMode openEditorMode) {
		super(operationName, editor);
		
		this.range = range;
		this.openEditorMode = openEditorMode;
		
		IFile file = EditorUtils.findFileOfEditor(editor);
		this.project = file == null ? null : file.getProject();
		
		ITextViewer viewer = null;
		if(editor instanceof AbstractLangEditor) {
			AbstractLangEditor langEditor = (AbstractLangEditor) editor;
			viewer = langEditor.getSourceViewer_();
		}
		
		this.context = new SourceOperationContext(viewer, range, editor);
	}
	
	public int getInvocationOffest() {
		return context.getInvocationOffset();
	}
	
	public SourceOperationContext getContext() {
		return context;
	}
	
	@Override
	protected void prepareOperation() throws CoreException {
		super.prepareOperation();
		
		line_0 = getContext().getInvocationLine_0();
		col_0 = getContext().getInvocationColumn_0();
	}
	
	@Override
	protected void performLongRunningComputation(IProgressMonitor monitor) 
			throws CoreException, CommonException, OperationCancellation {
		findResult = performLongRunningComputation_doAndGetResult(monitor);
	}
	
	protected abstract FindDefinitionResult performLongRunningComputation_doAndGetResult(IProgressMonitor monitor) 
			throws CoreException, CommonException, OperationCancellation;
	
	@Override
	protected void handleComputationResult() throws CoreException {
		if(statusErrorMessage != null) {
			handleStatusErrorMessage();
		}
		if(findResult == null) {
			return;
		}
		
		if(findResult.getErrorMessage() != null) {
			dialogError(findResult.getErrorMessage());
			return;
		}
		
		if(findResult.getInfoMessage() != null) {
			dialogInfo(findResult.getInfoMessage());
		}
		
		SourceLineColumnRange location = findResult.getLocation();
		if(location == null) {
			Display.getCurrent().beep();
			return;
		}
		
		openEditorForLocation(location);
	}
	
	protected void handleStatusErrorMessage() {
		if(editor instanceof AbstractTextEditor) {
			AbstractTextEditor abstractTextEditor = (AbstractTextEditor) editor;
			EditorUtils.setStatusLineErrorMessage(abstractTextEditor, statusErrorMessage, null);
		}
		Display.getCurrent().beep();
	}
	
	protected void openEditorForLocation(SourceLineColumnRange sourceLocation) throws CoreException {
		Location loc = EclipseUtils.location(sourceLocation.path);
		IEditorInput newInput = getNewEditorInput(loc);
		
		ITextEditor newEditor = EditorUtils.openTextEditorAndSetSelection(editor, EditorSettings_Actual.EDITOR_ID, 
			newInput, openEditorMode, null);
		
		IDocument doc = EditorUtils.getEditorDocument(newEditor);
		int selectionOffset = getOffsetFrom(doc, sourceLocation.line, sourceLocation.column);
		
		EditorUtils.setEditorSelection(newEditor, new SourceRange(selectionOffset, 0));
	}
	
	protected IEditorInput getNewEditorInput(Location newEditorFilePath) throws CoreException {
		if(newEditorFilePath == null) {
			throw LangCore.createCoreException("No path provided for new element. ", null);
		}
		
		if(areEqual(newEditorFilePath, inputLoc)) {
			return editor.getEditorInput();
		} else {
			return EditorUtils.getBestEditorInputForLoc(newEditorFilePath);
		}
	}
	
	protected static int getOffsetFrom(IDocument doc, int line_oneBased, int column_oneBased) throws CoreException {
		int lineOffset;
		try {
			lineOffset = doc.getLineOffset(line_oneBased-1);
		} catch (BadLocationException e) {
			throw LangCore.createCoreException("Invalid line number: " + line_oneBased, e);
		}
		
		return lineOffset + column_oneBased-1;
	}
	
}