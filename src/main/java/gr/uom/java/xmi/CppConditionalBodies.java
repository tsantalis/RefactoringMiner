package gr.uom.java.xmi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElseStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

/**
 * The function definitions parsed with the code of the preprocessor branches that CDT does not parse, whose bodies replace the bodies CDT gives:
 * the bodies of the function definitions in a branch that is not taken, i.e., #ifdef CONFIG_X ... #endif without CONFIG_X, which are empty,
 * and the bodies with conditional groups between their statements, whose branches that are not taken have no statements.
 * The file is parsed again with the text of these directives blanked, which keeps the offsets, and for a function definition in a branch that is not taken,
 * with the text of the sibling branches of its enclosing groups blanked too, once per set of enclosing branches.
 * The branches of a group inside a body are combined only when each of them is a sequence of whole statements, i.e., not #if X if (a) { #else if (b) { #endif,
 * and a function definition is replaced only when it has the same location in the code parsed again.
 */
class CppConditionalBodies {
	//a conditional group with the [start, end) of its directives, including their comments and continuation lines, and of the code of its branches
	private static class Group {
		private final List<int[]> directives = new ArrayList<>();
		private final List<int[]> branches = new ArrayList<>();
		private final List<Group> nested = new ArrayList<>();

		private int start() {
			return directives.get(0)[0];
		}

		private int end() {
			return directives.get(directives.size() - 1)[1];
		}
	}

	private record Function(IASTFunctionDefinition definition, int start, int end, int bodyStart) {}

	private final String fileContent;
	private final List<Group> groups = new ArrayList<>();

	private CppConditionalBodies(String fileContent) {
		this.fileContent = fileContent;
	}

	//the function definitions parsed again by their start offsets
	static Map<Integer, IASTFunctionDefinition> parse(IASTTranslationUnit translationUnit, String filePath, String fileContent, boolean astDiff, String preamble) {
		CppConditionalBodies bodies = new CppConditionalBodies(fileContent);
		bodies.collectGroups(translationUnit.getAllPreprocessorStatements());
		if(bodies.groups.isEmpty()) {
			return Map.of();
		}
		List<Function> functions = functions(translationUnit);
		//the directives of the groups combined in the bodies, which are blanked in every code parsed again
		List<int[]> combinedDirectives = new ArrayList<>();
		//the functions to parse again by the branches enclosing them, i.e., "" for the functions in taken branches
		Map<String, List<Function>> functionsByBranches = new LinkedHashMap<>();
		Map<String, List<int[]>> blankedByBranches = new HashMap<>();
		for(Function function : functions) {
			boolean combined = false;
			for(Group group : bodies.groups) {
				if(group.start() > function.bodyStart() && group.end() <= function.end() && bodies.combinable(group, function.bodyStart())) {
					for(int[] directive : group.directives) {
						combinedDirectives.add(directive);
					}
					combined = true;
				}
			}
			if(function.definition().isActive()) {
				if(combined) {
					functionsByBranches.computeIfAbsent("", k -> new ArrayList<>()).add(function);
					blankedByBranches.putIfAbsent("", new ArrayList<>());
				}
				continue;
			}
			StringBuilder key = new StringBuilder();
			List<int[]> blanked = new ArrayList<>();
			for(int i = 0; i < bodies.groups.size(); i++) {
				Group group = bodies.groups.get(i);
				if(group.start() < function.start() && group.end() > function.end()) {
					int branch = -1;
					for(int j = 0; j < group.branches.size(); j++) {
						int[] range = group.branches.get(j);
						if(range[0] <= function.start() && function.end() <= range[1]) {
							branch = j;
						}
					}
					if(branch == -1) {
						//the function definition is not inside one branch, i.e., its signature and its body are in different branches
						key = null;
						break;
					}
					key.append(i).append(':').append(branch).append(' ');
					blanked.addAll(group.directives);
					for(int j = 0; j < group.branches.size(); j++) {
						if(j != branch) {
							blanked.add(group.branches.get(j));
						}
					}
				}
			}
			if(key != null) {
				functionsByBranches.computeIfAbsent(key.toString(), k -> new ArrayList<>()).add(function);
				blankedByBranches.putIfAbsent(key.toString(), blanked);
			}
		}
		Map<Integer, IASTFunctionDefinition> parsed = new HashMap<>();
		for(Map.Entry<String, List<Function>> entry : functionsByBranches.entrySet()) {
			char[] content = fileContent.toCharArray();
			for(int[] range : blankedByBranches.get(entry.getKey())) {
				blank(content, range[0], range[1]);
			}
			for(int[] range : combinedDirectives) {
				blank(content, range[0], range[1]);
			}
			IASTTranslationUnit reparsed;
			try {
				reparsed = CppFileProcessor.parse(filePath, new String(content), astDiff, preamble);
			}
			catch(Exception e) {
				continue;
			}
			Map<Integer, Function> reparsedFunctions = new HashMap<>();
			for(Function function : functions(reparsed)) {
				reparsedFunctions.put(function.start(), function);
			}
			for(Function function : entry.getValue()) {
				Function reparsedFunction = reparsedFunctions.get(function.start());
				if(reparsedFunction != null && reparsedFunction.end() == function.end() && reparsedFunction.definition().isActive() &&
						reparsedFunction.definition().getBody() instanceof IASTCompoundStatement) {
					parsed.put(function.start(), reparsedFunction.definition());
				}
			}
		}
		return parsed;
	}

	private static List<Function> functions(IASTTranslationUnit translationUnit) {
		List<Function> functions = new ArrayList<>();
		translationUnit.accept(new ASTVisitor() {
			{
				shouldVisitDeclarations = true;
				includeInactiveNodes = true;
			}

			@Override
			public int visit(IASTDeclaration declaration) {
				if(declaration instanceof IASTFunctionDefinition definition && CppFileProcessor.isPartOfFile(definition) &&
						definition.getFileLocation() != null && definition.getBody() != null && definition.getBody().getFileLocation() != null) {
					IASTFileLocation location = definition.getFileLocation();
					functions.add(new Function(definition, location.getNodeOffset(), location.getNodeOffset() + location.getNodeLength(),
							definition.getBody().getFileLocation().getNodeOffset()));
				}
				return PROCESS_CONTINUE;
			}
		});
		return functions;
	}

	private void collectGroups(IASTPreprocessorStatement[] statements) {
		Deque<Group> open = new ArrayDeque<>();
		//the start of the code of the current branch of each open group
		Deque<Integer> branchStarts = new ArrayDeque<>();
		for(IASTPreprocessorStatement statement : statements) {
			IASTFileLocation location = statement.getFileLocation();
			if(location == null || !CppFileProcessor.isPartOfFile(statement)) {
				continue;
			}
			int start = location.getNodeOffset();
			int end = directiveLineEnd(start + location.getNodeLength());
			if(statement instanceof IASTPreprocessorIfStatement || statement instanceof IASTPreprocessorIfdefStatement || statement instanceof IASTPreprocessorIfndefStatement) {
				Group group = new Group();
				group.directives.add(new int[] {start, end});
				if(!open.isEmpty()) {
					open.peek().nested.add(group);
				}
				open.push(group);
				branchStarts.push(end);
			}
			else if((statement instanceof IASTPreprocessorElifStatement || statement instanceof IASTPreprocessorElseStatement) && !open.isEmpty()) {
				Group group = open.peek();
				group.branches.add(new int[] {branchStarts.pop(), start});
				group.directives.add(new int[] {start, end});
				branchStarts.push(end);
			}
			else if(statement instanceof IASTPreprocessorEndifStatement && !open.isEmpty()) {
				Group group = open.pop();
				group.branches.add(new int[] {branchStarts.pop(), start});
				group.directives.add(new int[] {start, end});
				groups.add(group);
			}
		}
		//the groups by their start offsets, so that the enclosing groups come first
		groups.sort((g1, g2) -> Integer.compare(g1.start(), g2.start()));
	}

	//whether the branches of the group in a body can be combined, as the group is between statements and each of its branches is a sequence of whole statements
	private boolean combinable(Group group, int bodyStart) {
		char before = lastSignificant(bodyStart, group.start());
		if(before != '{' && before != '}' && before != ';' && before != ':') {
			return false;
		}
		for(int[] branch : group.branches) {
			char last = lastSignificant(branch[0], branch[1]);
			if(last != 0 && last != ';' && last != '}' && last != ':') {
				return false;
			}
			if(!balanced(branch[0], branch[1])) {
				return false;
			}
		}
		for(Group nested : group.nested) {
			if(!combinable(nested, bodyStart)) {
				return false;
			}
		}
		return true;
	}

	//the last character of the code between the offsets outside the comments, the literals and the directives, or 0 if there is none
	private char lastSignificant(int start, int end) {
		char[] last = {0};
		scan(start, end, c -> last[0] = c);
		return last[0];
	}

	//whether the parentheses, brackets and braces of the code between the offsets are balanced
	private boolean balanced(int start, int end) {
		Deque<Character> open = new ArrayDeque<>();
		boolean[] valid = {true};
		scan(start, end, c -> {
			if(c == '(' || c == '[' || c == '{') {
				open.push(c);
			}
			else if(c == ')' || c == ']' || c == '}') {
				char expected = c == ')' ? '(' : c == ']' ? '[' : '{';
				if(open.isEmpty() || open.pop() != expected) {
					valid[0] = false;
				}
			}
		});
		return valid[0] && open.isEmpty();
	}

	private interface CharConsumer {
		void accept(char c);
	}

	//the characters of the code between the offsets outside the whitespace, the comments, the string and character literals and the directive lines,
	//with the literals given as their opening quote
	private void scan(int start, int end, CharConsumer consumer) {
		boolean lineStart = true;
		int i = start;
		while(i < end) {
			char c = fileContent.charAt(i);
			if(c == '\n') {
				lineStart = true;
				i++;
			}
			else if(Character.isWhitespace(c)) {
				i++;
			}
			else if(c == '#' && lineStart) {
				i = Math.min(directiveLineEnd(i), end);
			}
			else if(fileContent.startsWith("//", i)) {
				int lineEnd = fileContent.indexOf('\n', i);
				i = lineEnd < 0 || lineEnd > end ? end : lineEnd;
			}
			else if(fileContent.startsWith("/*", i)) {
				int close = fileContent.indexOf("*/", i + 2);
				i = close < 0 || close + 2 > end ? end : close + 2;
			}
			else if(c == '"' || c == '\'') {
				consumer.accept(c);
				i++;
				while(i < end && fileContent.charAt(i) != c && fileContent.charAt(i) != '\n') {
					i += fileContent.charAt(i) == '\\' ? 2 : 1;
				}
				i++;
				lineStart = false;
			}
			else {
				consumer.accept(c);
				lineStart = false;
				i++;
			}
		}
	}

	//the end of a directive after the line break of its line, including the comments and continuation lines
	private int directiveLineEnd(int end) {
		while(end < fileContent.length() && fileContent.charAt(end) != '\n') {
			if(fileContent.startsWith("/*", end)) {
				int close = fileContent.indexOf("*/", end + 2);
				end = close < 0 ? fileContent.length() : close + 2;
			}
			else if(fileContent.startsWith("\\\n", end)) {
				end += 2;
			}
			else if(fileContent.startsWith("\\\r\n", end)) {
				end += 3;
			}
			else {
				end++;
			}
		}
		if(end < fileContent.length()) {
			end++;
		}
		return end;
	}

	private static void blank(char[] content, int start, int end) {
		for(int i = start; i < end && i < content.length; i++) {
			if(content[i] != '\n' && content[i] != '\r') {
				content[i] = ' ';
			}
		}
	}
}
