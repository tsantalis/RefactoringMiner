# RefactoringMiner MCP Server

RefactoringMiner can run as a local stdio MCP server for coding agents. The server exposes read-only analysis and intent-validation tools backed by the existing RefactoringMiner APIs and returns compact JSON summaries suitable for agent workflows.

## Build

Use Java 17 or newer, then build the packaged jar:

```bash
./gradlew shadowJar --no-daemon --console=plain
```

The MCP server entrypoint is available from the fat jar:

```bash
java -jar build/libs/RM-fat.jar mcp
```

For a startup-only smoke check:

```bash
java -jar build/libs/RM-fat.jar mcp --smoke
```

To verify tool discovery with MCP Inspector CLI:

```bash
npx -y @modelcontextprotocol/inspector --cli --method tools/list --transport stdio \
  java -jar build/libs/RM-fat.jar mcp
```

## Claude Code Or Stdio Client Configuration

Create `.mcp.json` in the project that should use RefactoringMiner. Use the absolute path to the built RefactoringMiner jar.

```json
{
  "mcpServers": {
    "refactoringminer": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/RefactoringMiner/build/libs/RM-fat.jar",
        "mcp"
      ]
    }
  }
}
```

Generic stdio MCP clients should use the same command and arguments:

```text
command: java
args: -jar /absolute/path/to/RefactoringMiner/build/libs/RM-fat.jar mcp
```

Claude Code can load the same file:

```bash
claude -p --mcp-config .mcp.json --strict-mcp-config \
  "Use RefactoringMiner to validate whether my current worktree contains the Rename Method refactoring I intended."
```

## Analysis Tools

Analysis tools report the structural refactorings RefactoringMiner detects. They return `status`, `summary`, `refactoringCount`, `astDiffCount`, `moveAstDiffCount`, `filesBefore`, `filesAfter`, bounded `refactorings`, and `warnings`.

| Tool | Required inputs | Optional inputs |
|------|-----------------|-----------------|
| `refactoringminer_analyze_file_contents` | `beforeFiles`, `afterFiles` | `maxRefactorings` |
| `refactoringminer_analyze_worktree` | `repositoryPath` | `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile`, `maxRefactorings` |
| `refactoringminer_analyze_commit` | `repositoryPath`, `commitId` | `parentIndex`, `maxRefactorings` |
| `refactoringminer_analyze_pull_request` | `cloneUrl`, `pullRequestId` | `timeoutSeconds`, `maxRefactorings` |
| `refactoringminer_analyze_directories` | `beforePath`, `afterPath` | `maxRefactorings` |

Local paths must be absolute. File-content maps use repository-relative paths as keys and file contents as values.

## Intent Validation Tools

Validation tools let an agent state the refactoring it intended and ask RefactoringMiner whether the analyzed structural change supports that intent. Validation returns `status`, `summary`, `intent`, `refactoringCount`, `candidateCount`, bounded `matches`, bounded `candidates`, and `warnings`.

| Tool | Required inputs | Optional inputs |
|------|-----------------|-----------------|
| `refactoringminer_validate_file_contents` | `beforeFiles`, `afterFiles`, `intent` | `maxCandidates` |
| `refactoringminer_validate_worktree` | `repositoryPath`, `intent` | `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile`, `maxCandidates` |
| `refactoringminer_validate_commit` | `repositoryPath`, `commitId`, `intent` | `parentIndex`, `maxCandidates` |
| `refactoringminer_validate_pull_request` | `cloneUrl`, `pullRequestId`, `intent` | `timeoutSeconds`, `maxCandidates` |
| `refactoringminer_validate_directories` | `beforePath`, `afterPath`, `intent` | `maxCandidates` |

The `intent` object has one required field:

```json
{
  "type": "Rename Method"
}
```

It can also include optional filters:

```json
{
  "type": "Rename Method",
  "beforeFilePaths": ["src/main/java/example/Worker.java"],
  "afterFilePaths": ["src/main/java/example/Worker.java"],
  "classNames": ["Worker"],
  "methodNames": ["calculateTotal"],
  "fieldNames": [],
  "descriptionContains": "computeTotal"
}
```

Verdicts:

| Status | Meaning |
|--------|---------|
| `matched` | Exactly one detected refactoring matched the requested type and filters. |
| `missing` | RefactoringMiner found no matching refactoring; same-type candidates may be returned for inspection. |
| `ambiguous` | More than one detected refactoring matched, so the agent should refine the intent filters before claiming success. |
| `error` | Inputs were invalid or analysis failed. |

Use `maxCandidates` to bound returned evidence. If matching or candidate evidence is truncated, the result includes a warning.

## Example Validation Request

For generic MCP clients, call `refactoringminer_validate_file_contents` with arguments like:

```json
{
  "beforeFiles": {
    "src/main/java/example/Worker.java": "class Worker { void computeTotal() {} }"
  },
  "afterFiles": {
    "src/main/java/example/Worker.java": "class Worker { void calculateTotal() {} }"
  },
  "intent": {
    "type": "Rename Method",
    "methodNames": ["calculateTotal"]
  },
  "maxCandidates": 5
}
```

For file-content validation, provide complete parseable Java compilation units when possible. Small snippets that are not valid source files may still be accepted by the protocol but can produce `missing` because RefactoringMiner has no complete model to compare.

## Example Agent Prompts

Claude Code:

- "Use the RefactoringMiner MCP server to validate that my current worktree in `/path/to/repo` contains the intended `Rename Method` refactoring. Use `HEAD` as the base ref and return the validation status, summary, matches, candidates, and warnings."
- "Use `refactoringminer_validate_file_contents` with these before/after Java file contents and intent `{ \"type\": \"Extract Method\", \"methodNames\": [\"parseItems\"] }`. Do not infer behavior preservation from the result."

Codex:

- "Use the configured RefactoringMiner MCP server to validate whether this edit structurally matches my intended `Move Class` refactoring. If the result is `ambiguous`, list the candidate evidence and ask me for a narrower filter."
- "Before writing the final answer, call RefactoringMiner validation on the changed worktree and report whether the intended refactoring was `matched`, `missing`, or `ambiguous`."

Generic MCP clients:

- "Call `refactoringminer_validate_commit` for commit `abc123` in `/path/to/repo` with intent `{ \"type\": \"Rename Class\", \"classNames\": [\"OrderService\"] }`."
- "Call `refactoringminer_validate_pull_request` for PR 1058 in `https://github.com/tsantalis/RefactoringMiner.git` with intent `{ \"type\": \"Move Method\", \"methodNames\": [\"detect\"] }`."
- "Call `refactoringminer_validate_directories` for `/path/to/before` and `/path/to/after` with intent `{ \"type\": \"Extract Method\" }`."

## GitHub Pull Requests

Pull-request analysis and validation read GitHub data through the existing RefactoringMiner GitHub API path. For private repositories or higher rate limits, provide an OAuth token with one of these mechanisms:

- Environment variable: `OAuthToken`
- JVM system property: `-DOAuthToken=...`
- `github-oauth.properties` in the MCP process working directory with `OAuthToken=...`

The MCP tool only reads pull-request data. It does not post comments, mark files viewed, edit pull requests, or change repository state.

## Safety Boundaries

The MCP server is read-only.

- It does not modify files.
- It does not create commits, branches, checkouts, or pushes.
- It does not post GitHub comments or mutate pull requests.
- It does not run an external LLM.
- It does not host an HTTP MCP server.

Intent validation is structural evidence, not a behavior-preservation proof. A `matched` result means RefactoringMiner detected a structural refactoring matching the requested type and filters. It does not prove tests pass, specifications still hold, side effects are unchanged, or the edit contains no non-refactoring behavior changes.

Use test results, review, specifications, or domain-specific checks as complementary evidence. PurityChecker can become a useful companion for the subset of refactoring types it supports, but default MCP validation does not run PurityChecker in v1.1.

Deferred features include GitHub Action comments, hosted HTTP MCP, write tools, commit-range MCP analysis, large artifact or resource streaming, and PurityChecker-backed validation results.
