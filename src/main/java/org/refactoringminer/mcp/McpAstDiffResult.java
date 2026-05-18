package org.refactoringminer.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.TreeDelete;
import com.github.gumtreediff.actions.model.TreeInsert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

public record McpAstDiffResult(String kind, String beforeFilePath, String afterFilePath, int mappingCount,
		int actionCount, Map<String, Integer> actionCounts, List<ActionSummary> sampleActions) {
	private static final String STANDARD = "standard";
	private static final String MOVED = "moved";

	public McpAstDiffResult {
		actionCounts = actionCounts == null ? Collections.emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<>(actionCounts));
		sampleActions = sampleActions == null ? Collections.emptyList() : List.copyOf(sampleActions);
	}

	static List<McpAstDiffResult> from(ProjectASTDiff diff, int maxAstDiffs, int maxActionsPerAstDiff,
			List<String> warnings) {
		if (maxAstDiffs <= 0) {
			return Collections.emptyList();
		}
		List<DiffEntry> entries = entries(diff);
		int boundedSize = Math.min(entries.size(), maxAstDiffs);
		List<McpAstDiffResult> results = new ArrayList<>();
		for (int i = 0; i < boundedSize; i++) {
			DiffEntry entry = entries.get(i);
			results.add(from(entry.diff(), entry.kind(), diff.getFileContentsBefore(), diff.getFileContentsAfter(),
					maxActionsPerAstDiff, warnings));
		}
		if (boundedSize < entries.size()) {
			warnings.add(String.format("AST diffs truncated to %d of %d.", boundedSize, entries.size()));
		}
		return List.copyOf(results);
	}

	private static McpAstDiffResult from(ASTDiff astDiff, String kind, Map<String, String> beforeFiles,
			Map<String, String> afterFiles, int maxActionsPerAstDiff, List<String> warnings) {
		Map<String, Integer> actionCounts = new LinkedHashMap<>();
		List<ActionSummary> sampleActions = new ArrayList<>();
		int actionCount = 0;
		for (Action action : astDiff.editScript) {
			actionCount++;
			actionCounts.merge(action.getName(), 1, Integer::sum);
			if (sampleActions.size() < maxActionsPerAstDiff) {
				sampleActions.add(ActionSummary.from(action, astDiff, beforeFiles, afterFiles));
			}
		}
		if (maxActionsPerAstDiff > 0 && sampleActions.size() < actionCount) {
			warnings.add(String.format("Actions for %s -> %s truncated to %d of %d.", astDiff.getSrcPath(),
					astDiff.getDstPath(), sampleActions.size(), actionCount));
		}
		int mappingCount = astDiff.getAllMappings() == null ? 0 : astDiff.getAllMappings().size();
		return new McpAstDiffResult(kind, astDiff.getSrcPath(), astDiff.getDstPath(), mappingCount, actionCount,
				actionCounts, sampleActions);
	}

	private static List<DiffEntry> entries(ProjectASTDiff diff) {
		List<DiffEntry> entries = new ArrayList<>();
		Set<ASTDiff> moved = diff.getMoveDiffSet() == null ? Collections.emptySet() : diff.getMoveDiffSet();
		Set<ASTDiff> seen = new LinkedHashSet<>();
		if (diff.getDiffSet() != null) {
			for (ASTDiff astDiff : diff.getDiffSet()) {
				entries.add(new DiffEntry(astDiff, moved.contains(astDiff) ? MOVED : STANDARD));
				seen.add(astDiff);
			}
		}
		for (ASTDiff astDiff : moved) {
			if (seen.add(astDiff)) {
				entries.add(new DiffEntry(astDiff, MOVED));
			}
		}
		return entries;
	}

	private record DiffEntry(ASTDiff diff, String kind) {
	}

	public record ActionSummary(String name, String side, String filePath, String targetFilePath, Integer moveGroupId,
			String nodeType, String nodeLabel, int startOffset, int endOffset, int startLine, int endLine,
			String newValue, Integer parentPosition, String parentType) {
		private static ActionSummary from(Action action, ASTDiff astDiff, Map<String, String> beforeFiles,
				Map<String, String> afterFiles) {
			Tree node = action.getNode();
			String side = side(action);
			String filePath = filePath(action, astDiff, side);
			String content = "after".equals(side) ? get(afterFiles, filePath) : get(beforeFiles, filePath);
			LineRange lineRange = LineRange.from(content, node);
			String targetFilePath = targetFilePath(action, astDiff);
			Integer moveGroupId = action instanceof MultiMove multiMove ? multiMove.getGroupId() : null;
			String newValue = action instanceof Update update ? update.getValue() : null;
			Integer parentPosition = action instanceof TreeAddition addition ? addition.getPosition() : null;
			String parentType = action instanceof TreeAddition addition && addition.getParent() != null
					? addition.getParent().getType().toString() : null;
			return new ActionSummary(action.getName(), side, filePath, targetFilePath, moveGroupId, type(node),
					label(node), position(node), endPosition(node), lineRange.startLine(), lineRange.endLine(),
					newValue, parentPosition, parentType);
		}

		private static String side(Action action) {
			if (action instanceof Insert || action instanceof TreeInsert || action instanceof MoveIn) {
				return "after";
			}
			if (action instanceof Delete || action instanceof TreeDelete || action instanceof MoveOut) {
				return "before";
			}
			return "before";
		}

		private static String filePath(Action action, ASTDiff astDiff, String side) {
			if (action instanceof MoveIn) {
				return astDiff.getDstPath();
			}
			if (action instanceof MoveOut) {
				return astDiff.getSrcPath();
			}
			return "after".equals(side) ? astDiff.getDstPath() : astDiff.getSrcPath();
		}

		private static String targetFilePath(Action action, ASTDiff astDiff) {
			if (action instanceof MoveIn moveIn) {
				return moveIn.getSrcFile();
			}
			if (action instanceof MoveOut moveOut) {
				return moveOut.getDstFile();
			}
			if (action instanceof MultiMove) {
				return astDiff.getDstPath();
			}
			return null;
		}

		private static String get(Map<String, String> files, String filePath) {
			return files == null ? null : files.get(filePath);
		}

		private static String type(Tree node) {
			return node == null || node.getType() == null ? null : node.getType().toString();
		}

		private static String label(Tree node) {
			if (node == null || !node.hasLabel()) {
				return null;
			}
			String label = node.getLabel();
			return label == null || label.isBlank() ? null : label;
		}

		private static int position(Tree node) {
			return node == null ? -1 : node.getPos();
		}

		private static int endPosition(Tree node) {
			return node == null ? -1 : node.getEndPos();
		}
	}

	private record LineRange(int startLine, int endLine) {
		private static LineRange from(String content, Tree node) {
			if (content == null || node == null || node.getPos() < 0 || node.getEndPos() < 0) {
				return new LineRange(-1, -1);
			}
			int startOffset = node.getPos();
			int endOffset = Math.max(startOffset, node.getEndPos() - 1);
			return new LineRange(lineNumber(content, startOffset), lineNumber(content, endOffset));
		}

		private static int lineNumber(String content, int offset) {
			int boundedOffset = Math.max(0, Math.min(offset, content.length()));
			int line = 1;
			for (int i = 0; i < boundedOffset; i++) {
				if (content.charAt(i) == '\n') {
					line++;
				}
			}
			return line;
		}
	}
}
