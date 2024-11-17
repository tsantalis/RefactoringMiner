package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	private JPanel dialogContents;
	private JCheckBox generateParseTreeListenerCheckBox;
	private JCheckBox generateParseTreeVisitorCheckBox;
	private JTextField packageField;
	private TextFieldWithBrowseButton outputDirField;
	private TextFieldWithBrowseButton libDirField;
	private JTextField fileEncodingField;

	public ConfigANTLRPerGrammar(final Project project, String qualFileName) {
		super(project, false);
		init();

		FileChooserDescriptor dirChooser =
		        FileChooserDescriptorFactory.createSingleFolderDescriptor();
		outputDirField.addBrowseFolderListener("Select output dir", null, project, dirChooser);
		outputDirField.setTextFieldPreferredWidth(50);

		dirChooser =
		        FileChooserDescriptorFactory.createSingleFolderDescriptor();
		libDirField.addBrowseFolderListener("Select lib dir", null, project, dirChooser);
		libDirField.setTextFieldPreferredWidth(50);

		loadValues(project, qualFileName);
	}

	public void loadValues(Project project, String qualFileName) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		String s;
		s = props.getValue(getPropNameForFile(qualFileName, "output-dir"), "");
		outputDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "lib-dir"), "");
		libDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "encoding"), "");
		fileEncodingField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "package"), "");
		packageField.setText(s);

		boolean b;
		b = props.getBoolean(getPropNameForFile(qualFileName, "gen-listener"), true);
		generateParseTreeListenerCheckBox.setSelected(b);
		b =	props.getBoolean(getPropNameForFile(qualFileName, "gen-visitor"), false);
		generateParseTreeVisitorCheckBox.setSelected(b);
	}

	public void saveValues(Project project, String qualFileName) {
		String v;
		PropertiesComponent props = PropertiesComponent.getInstance(project);

		v = outputDirField.getText();
		if ( v.trim().length() > 0 ) {
			props.setValue(getPropNameForFile(qualFileName, "output-dir"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "output-dir"));
		}

		v = libDirField.getText();
		if ( v.trim().length() > 0 ) {
			props.setValue(getPropNameForFile(qualFileName, "lib-dir"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "lib-dir"));
		}

		v = fileEncodingField.getText();
		if ( v.trim().length() > 0 ) {
			props.setValue(getPropNameForFile(qualFileName, "encoding"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "encoding"));
		}

		v = packageField.getText();
		if ( v.trim().length() > 0 ) {
			props.setValue(getPropNameForFile(qualFileName, "package"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "package"));
		}

		props.setValue(getPropNameForFile(qualFileName, "gen-listener"),
					   String.valueOf(generateParseTreeListenerCheckBox.isSelected()));
		props.setValue(getPropNameForFile(qualFileName, "gen-visitor"),
					   String.valueOf(generateParseTreeVisitorCheckBox.isSelected()));
	}

	public static String getPropNameForFile(String qualFileName, String prop) {
		return qualFileName + "::/"+prop;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return dialogContents;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}


	@Override
	public String toString() {
		return "ConfigANTLRPerGrammar{" +
			"textField3=" +
			", generateParseTreeListenerCheckBox=" + generateParseTreeListenerCheckBox +
			", generateParseTreeVisitorCheckBox=" + generateParseTreeVisitorCheckBox +
			", packageField=" + packageField +
			", outputDirField=" + outputDirField +
			", libDirField=" + libDirField +
			'}';
	}
}
