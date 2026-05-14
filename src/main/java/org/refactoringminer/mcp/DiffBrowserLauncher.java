package org.refactoringminer.mcp;

import java.util.List;

import org.refactoringminer.astDiff.models.ProjectASTDiff;

@FunctionalInterface
interface DiffBrowserLauncher {
	McpDiffBrowserResult launch(ProjectASTDiff diff, int port, String inputSummary, List<String> warnings) throws Exception;
}
