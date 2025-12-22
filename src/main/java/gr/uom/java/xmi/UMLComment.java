package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.refactoringminer.util.PathFileUtils;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;

public class UMLComment extends UMLAbstractDocumentation {

	//represents non-attached Javadocs found within method bodies
	private Optional<UMLJavadoc> javaDoc;
	private CompositeStatementObject parent;
	private boolean markdown;
	private List<LocationInfo> previousLineLocations = new ArrayList<>();
	private static final Pattern EMPTY_LINES = Pattern.compile("(^\\s*$\\r?\\n)+", Pattern.MULTILINE);

	public UMLComment(String text, LocationInfo locationInfo) {
		super(text, locationInfo);
		this.javaDoc = Optional.empty();
		if(text.startsWith("///"))
			markdown = true;
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

	public List<LocationInfo> getPreviousLocations() {
		return previousLineLocations;
	}

	public void addPreviousLocation(LocationInfo info) {
		previousLineLocations.add(info);
	}

	public boolean isMarkdown() {
		return markdown;
	}

	@Override
	public String getText() {
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			String text = new String(this.text);
			if(text.startsWith("///")) {
				text = text.substring(3);
			}
			else if(text.startsWith("//")) {
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
				if(line.startsWith("///")) {
					line = line.substring(3);
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

	public boolean equalTextIgnoringEmptyLines(UMLComment other) {
		if(this.locationInfo.getCodeElementType().equals(CodeElementType.BLOCK_COMMENT) && other.locationInfo.getCodeElementType().equals(CodeElementType.BLOCK_COMMENT)) {
			String text1 = EMPTY_LINES.matcher(this.getText()).replaceAll("");
			String text2 = EMPTY_LINES.matcher(other.getText()).replaceAll("");
			if(text1.equals(text2)) {
				return true;
			}
			else if(text1.endsWith("\n") && text2.endsWith("\n")) {
				text1 = text1.substring(0, text1.length()-1);
				text2 = text2.substring(0, text2.length()-1);
				return text1.equals(text2 + "*") || text2.equals(text1 + "*");
			}
		}
		return false;
	}

	public boolean isCommentedCode() {
		Constants LANG = PathFileUtils.getLang(locationInfo.getFilePath());
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			String text = getText();
			if(text.equals(LANG.OPEN_BLOCK) || text.equals(LANG.CLOSE_BLOCK) || text.equals(LANG.BREAK_STATEMENT) ||
					text.equals(LANG.CONTINUE_STATEMENT) || text.equals(LANG.RETURN_STATEMENT) ||
					text.equals(LANG.RETURN_TRUE) || text.equals(LANG.RETURN_FALSE) || text.equals(LANG.RETURN_NULL))
				return true;
		}
		return false;
	}

	public boolean nestedInCatchBlock() {
		if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
			TryStatementObject tryParent = (TryStatementObject)parent;
			for(CompositeStatementObject catchClause : tryParent.getCatchClauses()) {
				if(catchClause.getLocationInfo().subsumes(this.getLocationInfo()))
					return true;
			}
			if(tryParent.getFinallyClause() != null) {
				if(tryParent.getFinallyClause().getLocationInfo().subsumes(this.getLocationInfo()))
					return true;
			}
		}
		return false;
	}
}
