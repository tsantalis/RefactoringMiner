package org.refactoringminer.api;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

import gr.uom.java.xmi.decomposition.ReplacementUtil;
import gr.uom.java.xmi.diff.CodeRange;

public interface Refactoring extends Serializable, CodeRangeProvider {

	static final Pattern MARKUP_LINK = Pattern.compile("(\\[(.*?)\\])(\\((.*?)\\))");

	public enum Decorator {
		PLAIN("", "", "", "", "", ""),
		HTML("<b>", "</b>", "<code>", "</code>", "<a href=\"\">", "</a>"),
		MARKUP("**", "**", "[", "]()", "`", "`"); 
		
		public final String BOLD_OPEN;
		public final String BOLD_CLOSE;
		public final String CODE_OPEN;
		public final String CODE_CLOSE;
		public final String LINK_OPEN;
		public final String LINK_CLOSE;
		Decorator(String bOLD_OPEN, String bOLD_CLOSE, String cODE_OPEN, String cODE_CLOSE, String lINK_OPEN,
				String lINK_CLOSE) {
			BOLD_OPEN = bOLD_OPEN;
			BOLD_CLOSE = bOLD_CLOSE;
			CODE_OPEN = cODE_OPEN;
			CODE_CLOSE = cODE_CLOSE;
			LINK_OPEN = lINK_OPEN;
			LINK_CLOSE = lINK_CLOSE;
		}
	}

	public RefactoringType getRefactoringType();
	
	public String getName();

	public String toString();
	
	default String toHTMLString() {return toString();}
	
	default String toMarkupString() {return toString();}

	/**
	 * @return a Set of ImmutablePair where left is the file path of a program element, and right is the qualified name of the class containing the program element
	 */
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring();
	
	/**
	 * @return a Set of ImmutablePair where left is the file path of a program element, and right is the qualified name of the class containing the program element
	 */
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring();
	
	default public String toMarkupStringWithGitHubLinks(String commitURL) {
		String description = toMarkupString();
		List<CodeRange> leftElements = leftSide();
		List<CodeRange> rightElements = rightSide();
		StringBuilder sb = new StringBuilder();
		Matcher matcher = MARKUP_LINK.matcher(description);
		while (matcher.find()) {
			String codeElement = matcher.group(2);
			//search first in the leftElements
			boolean found = false;
			Iterator<CodeRange> leftIterator = leftElements.iterator();
			while(leftIterator.hasNext()) {
				CodeRange left = leftIterator.next();
				if(left.getCodeElement() != null && left.getCodeElement().equals(codeElement)) {
					String url = createURL(left, commitURL, true);
					String replacement = "[" + codeElement + "](" + url + ")";
					matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
					found = true;
					leftIterator.remove();
					break;
				}
			}
			//search in the rightElements, if not found in leftElements
			if(!found) {
				Iterator<CodeRange> rightIterator = rightElements.iterator();
				while(rightIterator.hasNext()) {
					CodeRange right = rightIterator.next();
					if(right.getCodeElement() != null && right.getCodeElement().equals(codeElement)) {
						String url = createURL(right, commitURL, false);
						String replacement = "[" + codeElement + "](" + url + ")";
						matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
						found = true;
						rightIterator.remove();
						break;
					}
				}
			}
			//if not found in left and right element, then replace with a code markup
			if(!found) {
				String replacement = "`" + codeElement + "`";
				matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	static String createURL(CodeRange range, String commitURL, boolean leftSide) {
		String filePath = range.getFilePath();
		//make SHA-256 for filePath
		String filePathHash = ReplacementUtil.getSHA256Hash(filePath);
		String side = leftSide ? "L" : "R";
		String lineNumber = String.valueOf(range.getStartLine());
		return commitURL + "?diff=split#diff-" + filePathHash + side + lineNumber;
	}

	default public String toJSON(String commitURL) {
		StringBuilder sb = new StringBuilder();
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		sb.append("{").append("\n");
		sb.append("\t").append("\"").append("type").append("\"").append(": ").append("\"").append(getName()).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("description").append("\"").append(": ").append("\"");
		encoder.quoteAsString(toString().replace('\t', ' '), sb);
		sb.append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("markup").append("\"").append(": ").append("\"");
		encoder.quoteAsString(toMarkupStringWithGitHubLinks(commitURL).replace('\t', ' '), sb);
		sb.append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("leftSideLocations").append("\"").append(": ").append(leftSide()).append(",").append("\n");
		sb.append("\t").append("\"").append("rightSideLocations").append("\"").append(": ").append(rightSide()).append("\n");
		sb.append("}");
		return sb.toString();
	}

	default public String toJSON() {
		StringBuilder sb = new StringBuilder();
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		sb.append("{").append("\n");
		sb.append("\t").append("\"").append("type").append("\"").append(": ").append("\"").append(getName()).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("description").append("\"").append(": ").append("\"");
		encoder.quoteAsString(toString().replace('\t', ' '), sb);
		sb.append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("leftSideLocations").append("\"").append(": ").append(leftSide()).append(",").append("\n");
		sb.append("\t").append("\"").append("rightSideLocations").append("\"").append(": ").append(rightSide()).append("\n");
		sb.append("}");
		return sb.toString();
	}
}