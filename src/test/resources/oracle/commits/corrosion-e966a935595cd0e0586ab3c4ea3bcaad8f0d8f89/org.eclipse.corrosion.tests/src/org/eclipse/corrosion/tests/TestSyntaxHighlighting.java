/*********************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - Source Reference
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestSyntaxHighlighting extends AbstractCorrosionTest {

	@ParameterizedTest
	@ValueSource(strings = { "Cargo.toml", "src/main.rs" })
	public void testSyntaxHighlighting(String fileName) throws CoreException, IOException {
		IFile rustFile = getProject(BASIC_PROJECT_NAME).getFile(fileName);
		TextEditor editor = (TextEditor) IDE.openEditor(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), rustFile,
				"org.eclipse.ui.genericeditor.GenericEditor");
		StyledText editorTextWidget = (StyledText) editor.getAdapter(Control.class);
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return editorTextWidget.getStyleRanges().length > 1;
			}
		}.waitForCondition(editorTextWidget.getDisplay(), 4000);
		assertTrue(editorTextWidget.getStyleRanges().length > 1, "There should be multiple styles in editor");
	}

}
