package org.refactoringminer.mcp;

import java.util.List;

import gr.uom.java.xmi.diff.CodeRange;
import org.refactoringminer.api.Refactoring;

public record McpRefactoringResult(String type, String description, List<Location> leftSideLocations,
		List<Location> rightSideLocations) {

	public static McpRefactoringResult from(Refactoring refactoring) {
		return new McpRefactoringResult(refactoring.getName(), refactoring.toString(),
				refactoring.leftSide().stream().map(Location::from).toList(),
				refactoring.rightSide().stream().map(Location::from).toList());
	}

	public record Location(String filePath, int startLine, int endLine, int startColumn, int endColumn,
			String codeElementType, String description, String codeElement) {

		static Location from(CodeRange range) {
			return new Location(range.getFilePath(), range.getStartLine(), range.getEndLine(),
					range.getStartColumn(), range.getEndColumn(), range.getCodeElementType().name(),
					range.getDescription(), range.getCodeElement());
		}
	}
}
