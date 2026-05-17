package org.refactoringminer.mcp;

import java.util.List;

import org.refactoringminer.astDiff.models.ProjectASTDiff;

@FunctionalInterface
interface DiffBrowserLauncher {
	McpDiffBrowserResult launch(ProjectASTDiff diff, int port, String inputSummary, List<String> warnings,
			int maxAstDiffs, int maxActionsPerAstDiff) throws Exception;

	default McpDiffBrowserResult launch(ProjectASTDiff diff, int port, String inputSummary, List<String> warnings)
			throws Exception {
		return launch(diff, port, inputSummary, warnings, 20, 20);
	}
}
