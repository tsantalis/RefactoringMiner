---
name: refactoringminer-review
description: Use RefactoringMiner MCP and AST diff views to review pull requests, commits, or local worktrees by separating detected refactoring structure from behavior-relevant code changes. Use this when the user asks Claude Code to review non-refactoring changes, inspect moved or renamed code with WebDiff, or explain which changes are refactorings versus semantic edits.
license: MIT
---

# RefactoringMiner review

Use the connected `refactoringminer` MCP server for detected refactorings and AST diff views. This skill is a review workflow on top of MCP; it should not run RefactoringMiner from the shell unless the MCP server is unavailable.

The goal is to help reviewers spend attention on changes that are not explained by RefactoringMiner-detected refactoring structure.

If the MCP server is not configured, say that RefactoringMiner MCP is required and point the user to `documentation/mcp.md`. Do not pretend the review used RefactoringMiner.

Output should look like a code review. Do not add decorative preambles, insight boxes, praise, or generic conclusions. Start with the tool used or the RefactoringMiner result.

## Inputs

First identify the review target:

- GitHub pull request: repository clone URL and pull request number.
- Local worktree: absolute repository path, base ref, and whether to include untracked files.
- Commit: absolute repository path, commit id, and optional parent index.
- Explicit file contents: before and after file maps.

If the target is ambiguous, ask one concise clarification before calling tools.

## Core workflow

1. Run the matching RefactoringMiner analysis tool:
   - PR: `refactoringminer_analyze_pull_request`
   - worktree: `refactoringminer_analyze_worktree`
   - commit: `refactoringminer_analyze_commit`
   - file contents: `refactoringminer_analyze_file_contents`

2. When the change includes moved, renamed, extracted, or inlined code, or when the ordinary diff is hard to interpret, run the matching RefactoringMiner AST diff browser tool:
   - PR: `refactoringminer_diff_pull_request`
   - worktree: `refactoringminer_diff_worktree`
   - commit: `refactoringminer_diff_commit`
   - file contents: `refactoringminer_diff_file_contents`

3. Use RefactoringMiner's analysis result and AST diff view as the source of truth for refactoring-aware structure. Ordinary GitHub/git diffs are useful for line references, changed-file discovery, and residual context, but they do not preserve code-move mappings the way RefactoringMiner's AST diff does.

4. Build a review map:
   - list detected refactorings by type, entity, and affected file
   - mark code regions whose movement or rename is visible in the AST diff
   - mark files or regions that appear mixed
   - mark files or regions that are not explained by the detected refactoring structure
   - keep unknown cases explicit instead of guessing

5. Review the non-refactoring and mixed regions first. Look for behavior changes, missed tests, compatibility risks, broken API expectations, data loss, performance regressions, and error-handling gaps.

6. Report findings in normal code-review format. Lead with actionable issues and file or line references when available. Use severity labels such as `P1`, `P2`, and `P3` when reporting issues. If there are no findings, say so and mention any remaining risk.

If a result says output was truncated, rerun with a higher MCP limit when the missing detail matters. If the PR is too large for a complete pass, review a bounded set of files and state the boundary.

## Browser workflow

Use a browser tool when the user wants a visual AST diff, asks for a WebDiff page, or the ordinary Git diff shows large delete/add blocks that may be code moves:

- PR: `refactoringminer_diff_pull_request`
- worktree: `refactoringminer_diff_worktree`
- commit: `refactoringminer_diff_commit`
- file contents: `refactoringminer_diff_file_contents`

Return the `url`, `summary`, and any `warnings`. Keep the Claude Code session open while the user inspects the WebDiff page, because the local WebDiff server lives inside the MCP process.

## How to treat refactorings

Detected refactorings and AST mappings are not proof of behavior preservation. Use them as review guidance, not as a verdict.

When listing detected refactorings:

- name the refactoring type, entity, and file
- do not call them safe, clean, harmless, or behavior-preserving
- do not say "no behavior changed" for refactored paths unless you reviewed those exact code regions
- if you have not reviewed a refactoring region, call it "detected by RefactoringMiner", not "verified"

Can be reviewed after higher-risk changes:

- rename, move, extract, inline, and signature-shape changes whose surrounding code has also been checked in the AST diff view
- import/package updates caused only by moves or renames
- call-site updates that exactly follow a detected rename or move

Still review:

- changed conditions, loops, arithmetic, parsing, serialization, persistence, or error handling
- changed public API behavior, configuration keys, CLI flags, JSON fields, file formats, or database access
- added or removed side effects
- test changes that weaken assertions
- any code region in a refactored file that does more than route through the new name/location

## Output shape

For a full review, use:

```text
RefactoringMiner found <N> refactorings.

Refactoring-aware structure:
- <detected refactoring list and AST-diff move/rename evidence, without safety claims>

Changes to review:
- <file or area>: <why this is not covered by detected refactoring structure>

Findings:
- <P1/P2/P3> <file:line> <issue and impact>

Residual risk:
- <limits of this review, including truncated MCP output or unavailable diff context>
```

For a quick answer, keep it short:

```text
RefactoringMiner found <N> refactorings. The changes not explained by the detected refactoring structure are in <files/areas>. I found <issue/no issue>.
```

Keep the answer shaped like a code review.

## Limits

RefactoringMiner detects refactoring structure and can show AST mappings for moved code in WebDiff. It does not prove that the change is behavior-preserving, and it does not replace tests or domain review.

Current MCP JSON results summarize detected refactorings and AST diff counts. They do not stream full machine-readable AST mappings or moved-node ranges. Use the WebDiff browser view for mapping-level review, and state this limitation when it affects confidence.

If MCP output is truncated, rerun with a higher limit or state that the review is partial.
