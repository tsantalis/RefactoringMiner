# RefactoringMiner MCP server

RefactoringMiner can also run as a local stdio MCP server. Coding agents can ask it for detected refactorings, intent checks, and local AST diff pages. Results come back as small JSON objects built from the existing RefactoringMiner APIs.

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

## Manual WebDiff browser check

If you only want to inspect the browser output yourself, run the existing WebDiff CLI directly. The WebDiff server stays attached to that terminal while you use the browser.

```bash
java -jar build/libs/RM-fat.jar diff --url https://github.com/tsantalis/RefactoringMiner/pull/1234
```

For a local repository commit:

```bash
java -jar build/libs/RM-fat.jar diff --repo /absolute/path/to/repo --commit abc123
```

Open the printed URL, usually:

```text
http://127.0.0.1:6789
```

Keep the terminal process alive while inspecting the page. Use the browser's Quit button or stop the terminal process when finished.

## Claude Code and stdio clients

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

One-shot `claude -p` calls work well for analysis and validation tools that only return JSON. For AST diff browser tools, use an interactive Claude Code session so the MCP process, and the local WebDiff server it started, stays alive while you open the returned URL:

```bash
claude --mcp-config .mcp.json --strict-mcp-config
```

Then ask Claude Code to call a browser tool, for example:

```text
Call refactoringminer_diff_worktree with baseRef "HEAD", includeUntracked false, port 6789. Return only the URL and keep this session open.
```

## Docker

The Docker image can run the MCP server over stdio:

```bash
docker run --rm -i \
  --pull always \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

Mount a repository when using local worktree or commit tools:

```bash
docker run --rm -i \
  --pull always \
  -v "$PWD:/workspace" \
  -w /workspace \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

For the command above, worktree and commit tools default to `/workspace` because Docker starts the MCP process there with `-w /workspace`. If you pass `repositoryPath` explicitly, use the container path, not the host path. For example, use `/workspace`, not `C:/RefactoringMiner` or `/Users/me/RefactoringMiner`.

For AST diff browser tools, publish the WebDiff port:

```bash
docker run --rm -i \
  --pull always \
  -v "$PWD:/workspace" \
  -w /workspace \
  -p 6789:6789 \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

The Docker image sets `REFACTORINGMINER_WEBDIFF_BIND_HOST=0.0.0.0` so the published port is reachable from the host. The URL returned to the user still uses `127.0.0.1`.

A Claude Code config can use Docker as the stdio command:

```json
{
  "mcpServers": {
    "refactoringminer": {
      "type": "stdio",
      "command": "docker",
      "args": [
        "run", "--rm", "-i", "--pull", "always",
        "-v", "/absolute/path/to/repo:/workspace",
        "-w", "/workspace",
        "-p", "6789:6789",
        "-e", "OAuthToken",
        "tsantalis/refactoringminer:latest",
        "mcp"
      ]
    }
  }
}
```

Outside Docker, WebDiff binds to `127.0.0.1` by default. These settings can be overridden with environment variables or JVM system properties:

| Setting | Environment variable | JVM system property | Default |
|---------|----------------------|---------------------|---------|
| WebDiff bind host | `REFACTORINGMINER_WEBDIFF_BIND_HOST` | `refactoringminer.webdiff.bindHost` | `127.0.0.1` |
| Returned URL host | `REFACTORINGMINER_WEBDIFF_PUBLIC_HOST` | `refactoringminer.webdiff.publicHost` | `127.0.0.1` |

## Analysis tools

Analysis tools report the refactorings RefactoringMiner detects. They return `status`, `summary`, `refactoringCount`, `astDiffCount`, `moveAstDiffCount`, `filesBefore`, `filesAfter`, a limited `refactorings` list, and `warnings`.

| Tool | Required inputs | Optional inputs |
|------|-----------------|-----------------|
| `refactoringminer_analyze_file_contents` | `beforeFiles`, `afterFiles` | `maxFiles`, `maxBytesPerFile`, `maxRefactorings` |
| `refactoringminer_analyze_worktree` | None | `repositoryPath`, `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile`, `maxRefactorings` |
| `refactoringminer_analyze_commit` | `commitId` | `repositoryPath`, `parentIndex`, `maxRefactorings` |
| `refactoringminer_analyze_pull_request` | `cloneUrl`, `pullRequestId` | `timeoutSeconds`, `maxRefactorings` |
| `refactoringminer_analyze_directories` | `beforePath`, `afterPath` | `maxRefactorings` |

Local paths must be absolute when provided. Worktree and commit tools default `repositoryPath` to the MCP server working directory, which is usually the directory where the agent was started. File-content maps use repository-relative paths as keys and file contents as values. Explicit file-content tools default to `maxFiles=100` and `maxBytesPerFile=200000` to keep calls small. Analysis tools default to `maxRefactorings=20`; set a higher value when the agent needs more returned detail, or `0` when counts and warnings are enough.

Commit tools first try the GitHub API when the working tree has a GitHub `origin` remote. If the commit is not available from GitHub, they fall back to the local repository. This keeps pushed commits small for MCP clients while still supporting local-only commits.

## Intent validation tools

Validation tools let an agent state the refactoring it intended and ask RefactoringMiner whether the detected refactorings match. Validation returns `status`, `summary`, `intent`, `refactoringCount`, `candidateCount`, limited `matches`, limited `candidates`, and `warnings`.

| Tool | Required inputs | Optional inputs |
|------|-----------------|-----------------|
| `refactoringminer_validate_file_contents` | `beforeFiles`, `afterFiles`, `intent` | `maxFiles`, `maxBytesPerFile`, `maxCandidates` |
| `refactoringminer_validate_worktree` | `intent` | `repositoryPath`, `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile`, `maxCandidates` |
| `refactoringminer_validate_commit` | `commitId`, `intent` | `repositoryPath`, `parentIndex`, `maxCandidates` |
| `refactoringminer_validate_pull_request` | `cloneUrl`, `pullRequestId`, `intent` | `timeoutSeconds`, `maxCandidates` |
| `refactoringminer_validate_directories` | `beforePath`, `afterPath`, `intent` | `maxCandidates` |

Validation tools default to `maxCandidates=20`. This only limits what comes back to the MCP client; it does not change the RefactoringMiner detection pass.

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

Use `maxCandidates` to limit returned matches and candidates. If the result is truncated, it includes a warning.

## AST diff browser tools

Diff browser tools generate a RefactoringMiner AST diff, start the existing local WebDiff server, and return a localhost URL that the user can open in a browser. They return `status`, `summary`, `message`, `url`, `port`, `inputSummary`, `refactoringCount`, `astDiffCount`, `moveAstDiffCount`, `filesBefore`, `filesAfter`, a limited `affectedFiles` list, and `warnings`.

| Tool | Required inputs | Optional inputs |
|------|-----------------|-----------------|
| `refactoringminer_diff_file_contents` | `beforeFiles`, `afterFiles` | `maxFiles`, `maxBytesPerFile`, `port` |
| `refactoringminer_diff_worktree` | None | `repositoryPath`, `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile`, `port` |
| `refactoringminer_diff_commit` | `commitId` | `repositoryPath`, `parentIndex`, `port` |
| `refactoringminer_diff_pull_request` | `cloneUrl`, `pullRequestId` | `timeoutSeconds`, `port` |

The default port is `6789`. The returned `message` uses the same wording as the WebDiff CLI startup line:

```text
Starting server: http://127.0.0.1:6789
```

MCP tools do not auto-open the desktop browser. They bind WebDiff to `127.0.0.1` by default and return the URL in the tool result so the user or client can decide when to open it.

The returned URL is only available while the MCP server process is still running. If an MCP client starts RefactoringMiner for a single command and immediately exits, the local WebDiff server can disappear before the user opens the URL. Use an interactive client session for browser tools, or use the direct WebDiff CLI when the goal is only manual browser inspection.

Repeated browser-tool calls replace the active local WebDiff view in the MCP server process. The WebDiff Quit button stops the active local WebDiff view but does not exit the MCP JVM. If the requested port is invalid or already occupied by another process, the tool returns an `error` result with a summary and warnings. It does not corrupt stdio output or discard the previous view on another port.

Example file-content browser request:

```json
{
  "beforeFiles": {
    "src/main/java/example/Worker.java": "package example; class Worker { void computeTotal() {} }"
  },
  "afterFiles": {
    "src/main/java/example/Worker.java": "package example; class Worker { void calculateTotal() {} }"
  },
  "port": 6789
}
```

Example worktree browser request:

```json
{
  "baseRef": "HEAD",
  "includeUntracked": false,
  "port": 6789
}
```

Example commit browser request:

```json
{
  "commitId": "abc123",
  "parentIndex": 0,
  "port": 6789
}
```

Example pull-request browser request:

```json
{
  "cloneUrl": "https://github.com/tsantalis/RefactoringMiner.git",
  "pullRequestId": 1234,
  "timeoutSeconds": 300,
  "port": 6789
}
```

## Example validation request

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

## Example agent prompts

Claude Code:

- "Use the RefactoringMiner MCP server to validate that my current worktree in `/path/to/repo` contains the intended `Rename Method` refactoring. Use `HEAD` as the base ref and return the validation status, summary, matches, candidates, and warnings."
- "Use `refactoringminer_validate_file_contents` with these before/after Java file contents and intent `{ \"type\": \"Extract Method\", \"methodNames\": [\"parseItems\"] }`. Do not infer behavior preservation from the result."

Codex:

- "Use the configured RefactoringMiner MCP server to validate whether this edit structurally matches my intended `Move Class` refactoring. If the result is `ambiguous`, list the candidate evidence and ask me for a narrower filter."
- "Before writing the final answer, call RefactoringMiner validation on the changed worktree and report whether the intended refactoring was `matched`, `missing`, or `ambiguous`."

Generic MCP clients:

- "Call `refactoringminer_validate_commit` for commit `abc123` with intent `{ \"type\": \"Rename Class\", \"classNames\": [\"OrderService\"] }`."
- "Call `refactoringminer_validate_pull_request` for PR 1234 in `https://github.com/tsantalis/RefactoringMiner.git` with intent `{ \"type\": \"Move Method\", \"methodNames\": [\"detect\"] }`."
- "Call `refactoringminer_validate_directories` for `/path/to/before` and `/path/to/after` with intent `{ \"type\": \"Extract Method\" }`."
- "Call `refactoringminer_diff_worktree` and return the local URL."
- "Call `refactoringminer_diff_pull_request` for PR 1234 in `https://github.com/tsantalis/RefactoringMiner.git` and return the startup message and URL."

## GitHub pull requests

Pull-request analysis and validation read GitHub data through the existing RefactoringMiner GitHub API path. For private repositories or higher rate limits, provide an OAuth token with one of these mechanisms:

- Environment variable: `OAuthToken`
- JVM system property: `-DOAuthToken=...`
- `github-oauth.properties` in the MCP process working directory with `OAuthToken=...`

The MCP tool only reads pull-request data. It does not post comments, mark files viewed, edit pull requests, or change repository state.

Use a GitHub number that identifies a pull request, not a plain issue. For pull-request diff browser tools, the returned WebDiff URL is local to the machine running the MCP server. It is not a hosted report link.

## What the server does not do

The MCP server is read-only.

- It does not modify files.
- It does not create commits, branches, checkouts, or pushes.
- It does not post GitHub comments or mutate pull requests.
- It does not run an external LLM.
- It does not host an HTTP MCP server.
- It does not auto-open the desktop browser from diff-browser tools.

Intent validation is not a behavior-preservation proof. A `matched` result means RefactoringMiner found a refactoring with the requested type and filters. It does not prove tests pass, specifications still hold, side effects are unchanged, or the edit contains no non-refactoring behavior changes.

Use tests, review, specifications, or domain-specific checks alongside MCP results. PurityChecker may be useful for the refactoring types it supports, but MCP validation does not run PurityChecker in this release.

This release does not include GitHub Action comments, static hosted HTML reports, hosted HTTP MCP, write tools, commit-range MCP analysis, large artifact or resource streaming, or PurityChecker-backed validation results.

## Troubleshooting

- If a returned WebDiff URL refuses the connection, confirm the MCP client session that started it is still running. For manual inspection, use the direct `diff` CLI instead.
- If port `6789` is already in use, call the MCP browser tool with another `port` value, or stop the existing local WebDiff process.
- If Docker-based worktree or commit tools use the wrong repository, set `-w /workspace` in the Docker command or pass `repositoryPath: "/workspace"` explicitly. The MCP server cannot resolve host paths from inside the container.
- If pull-request tools fail on a private repository or after many calls, configure `OAuthToken` through the environment, JVM system property, or `github-oauth.properties`.
- If file-content validation returns `missing` for tiny snippets, retry with complete parseable Java compilation units so RefactoringMiner can build a full model.
