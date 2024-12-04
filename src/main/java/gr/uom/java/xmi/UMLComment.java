package gr.uom.java.xmi;

import java.util.Optional;
import java.util.Scanner;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;

import static gr.uom.java.xmi.Constants.JAVA;

public class UMLComment extends UMLAbstractDocumentation {

	//represents non-attached Javadocs found within method bodies
	private Optional<UMLJavadoc> javaDoc;
	private CompositeStatementObject parent;

	public UMLComment(String text, LocationInfo locationInfo) {
		super(text, locationInfo);
		this.javaDoc = Optional.empty();
	}

	public void setJavaDoc(UMLJavadoc doc) {
		this.javaDoc = Optional.of(doc);
	}

	public Optional<UMLJavadoc> getJavaDoc() {
		return javaDoc;
	}

	public CompositeStatementObject getParent() {
		return parent;
	}

	public void setParent(CompositeStatementObject parent) {
		this.parent = parent;
	}

	@Override
	public String getText() {
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			String text = new String(this.text);
			if(text.startsWith("//")) {
				text = text.substring(2);
			}
			text = text.trim();
			return text;
		}
		else {
			StringBuilder sb = new StringBuilder();
			Scanner scanner = new Scanner(this.text);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.trim();
				if(line.startsWith("/*")) {
					line = line.substring(2);
				}
				if(line.endsWith("*/")) {
					line = line.substring(0, line.length()-2);
				}
				if(line.startsWith("//")) {
					line = line.substring(2);
				}
				if(line.startsWith("*")) {
					line = line.substring(1);
				}
				line = line.trim();
				sb.append(line).append("\n");
			}
			scanner.close();
			return sb.toString();
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			sb.append(locationInfo.getStartLine());
		}
		if(locationInfo.getCodeElementType().equals(CodeElementType.BLOCK_COMMENT)) {
			sb.append(locationInfo.getStartLine()).append("-").append(locationInfo.getEndLine());
		}
		sb.append(": ");
		sb.append(text);
		return sb.toString();
	}

	public boolean isCommentedCode() {
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			String text = getText();
			if(text.equals(JAVA.OPEN_BLOCK) || text.equals(JAVA.CLOSE_BLOCK) || text.equals(JAVA.BREAK_STATEMENT) ||
					text.equals(JAVA.CONTINUE_STATEMENT) || text.equals(JAVA.RETURN_STATEMENT) ||
					text.equals(JAVA.RETURN_TRUE) || text.equals(JAVA.RETURN_FALSE) || text.equals(JAVA.RETURN_NULL))
				return true;
		}
		return false;
	}
}
