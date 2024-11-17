package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;

public class TestRuleAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR TestRuleAction");

	/** Only show if selection is a grammar and in a rule */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		presentation.setText("Test ANTLR Rule"); // default text

		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) { // we clicked somewhere outside text or non grammar file
			presentation.setEnabled(false);
			presentation.setVisible(false);
			return;
		}

		ParserRuleRefNode r = getSelectedRuleName(e);
		if ( r==null ) {
			presentation.setEnabled(false);
			return;
		}

		presentation.setVisible(true);
		String ruleName = r.getText();
		if ( Character.isLowerCase(ruleName.charAt(0)) ) {
			presentation.setEnabled(true);
			presentation.setText("Test Rule "+ruleName);
		}
		else {
			presentation.setEnabled(false);
		}
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) return;

		LOG.info("actionPerformed "+grammarFile);

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(e.getProject());
		controller.getPreviewWindow().show(null);

		ParserRuleRefNode r = getSelectedRuleName(e);
		if ( r==null ) {
			return; // weird. no rule name.
		}
		String ruleName = r.getText();
		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(grammarFile);
		if ( doc!=null ) {
			docMgr.saveDocument(doc);
		}

		controller.setStartRuleNameEvent(grammarFile, ruleName);
	}

	public ParserRuleRefNode getSelectedRuleName(AnActionEvent e) {
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if ( editor==null ) { // not in editor
			PsiElement selectedNavElement = e.getData(LangDataKeys.PSI_ELEMENT);
			// in nav bar?
			if ( selectedNavElement==null || !(selectedNavElement instanceof ParserRuleRefNode) ) {
				return null;
			}
			return (ParserRuleRefNode)selectedNavElement;
		}

		// in editor
		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
		if ( file==null ) {
			return null;
		}

		int offset = MyActionUtils.getMouseOffset(editor);

		PsiElement selectedPsiRuleNode = file.findElementAt(offset);
		if ( selectedPsiRuleNode==null ) { // didn't select a node in parse tree
			return null;
		}

		// find root of rule def
		if ( !(selectedPsiRuleNode instanceof ParserRuleSpecNode) ) {
			selectedPsiRuleNode = PsiTreeUtil.findFirstParent(selectedPsiRuleNode, new Condition<PsiElement>() {
				@Override
				public boolean value(PsiElement psiElement) {
					return psiElement instanceof ParserRuleSpecNode;
				}
			});
			if ( selectedPsiRuleNode==null ) { // not in rule I guess.
				return null;
			}
			// found rule
		}

		ParserRuleRefNode r = PsiTreeUtil.findChildOfType(selectedPsiRuleNode,
														  ParserRuleRefNode.class);
		return r;
	}

}
