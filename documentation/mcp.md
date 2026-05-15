# RefactoringMiner MCP server

RefactoringMiner can also run as a local stdio MCP server. It exposes three read-only tools grouped by task: `refactoringminer_analyze`, `refactoringminer_validate`, and `refactoringminer_diff`. Each tool accepts a `source` object so agents choose what they are analyzing without choosing among many near-duplicate tools. `source.type` may be explicit, or inferred when fields such as `pullRequestId`, `url`, `commitId`, `beforeFiles`, or `beforePath` make the source clear.

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
  "Use RefactoringMiner to analyze my current worktree and report detected refactorings."
```

`refactoringminer_diff` starts a local WebDiff server. Use an interactive client session when you want to open the returned URL, because the URL is only available while the MCP process remains alive:

```bash
claude --mcp-config .mcp.json --strict-mcp-config
```

Then ask Claude Code to call a browser tool, for example:

```text
Call refactoringminer_diff with source type "worktree", baseRef "HEAD", includeUntracked false, and port 6789. Return only the URL and keep this session open.
```

## Agent skill for review workflows

The repository includes a project skill at `.claude/skills/refactoringminer-review/SKILL.md`. It gives Claude Code a repeatable review workflow for requests such as:

```text
Use the refactoringminer-review skill to review the non-refactoring changes in this pull request.
```

The skill uses the configured `refactoringminer` MCP server first and treats RefactoringMiner's analysis and AST diff view as the refactoring-aware source of truth. Ordinary GitHub/git diffs are still useful for changed-file discovery, line references, and residual review context, but they do not preserve code-move mappings the way RefactoringMiner's AST diff does.

The intended review focus is the code that is not explained by detected refactoring structure, plus mixed regions where a refactoring and a behavior-relevant edit appear together. Detected refactorings are review guidance, not a proof that behavior is preserved.

The skill does not start RefactoringMiner by itself. Configure the MCP server first with either the local jar or Docker setup above, then start the agent from a repository that can see the skill.

Claude Code can load this project skill automatically when started from the RefactoringMiner checkout. To make it available across projects, copy the skill directory to your personal skills directory:

```bash
mkdir -p ~/.claude/skills
cp -R .claude/skills/refactoringminer-review ~/.claude/skills/
```

Codex can use the same skill folder after copying it into the Codex skills directory:

```bash
mkdir -p "${CODEX_HOME:-$HOME/.codex}/skills"
cp -R .claude/skills/refactoringminer-review "${CODEX_HOME:-$HOME/.codex}/skills/"
```

Restart Codex after copying the skill so the next session discovers it.

OpenCode can also use the same `SKILL.md` shape. Either keep it in `.claude/skills/refactoringminer-review/` for project-local use, or copy it to OpenCode's global skills directory:

```bash
mkdir -p ~/.config/opencode/skills
cp -R .claude/skills/refactoringminer-review ~/.config/opencode/skills/
```

## Docker

RefactoringMiner is recommended to run via Docker to avoid local environment setup and ensure stability.

### Linux and macOS

Use the provided [wrapper script template](../scripts/mcp-wrapper-template.sh). This script ensures a shared container runs in the background:

1. Copy the template to your local machine:
   ```bash
   cp scripts/mcp-wrapper-template.sh mcp-wrapper.sh
   ```
2. Edit `mcp-wrapper.sh` to populate the user configuration variables:
   - `HOST_VOLUME_PATH`: Absolute path to the repository you want to analyze.
   - `TOKEN`: Your GitHub OAuth token for private repositories and higher rate limits.
   - Optional: Adjust `CONTAINER_NAME` or `PORT_MAPPING` as needed.
3. Make the script executable:
   ```bash
   chmod +x mcp-wrapper.sh
   ```
4. Use the script as the command for your MCP client. For example, in a `.mcp.json` config:
   ```json
   {
     "mcpServers": {
       "refactoringminer": {
         "type": "stdio",
         "command": "/absolute/path/to/mcp-wrapper.sh"
       }
     }
   }
   ```

The wrapper should mount the target repository into the container and start the MCP process from that mounted directory. With `source.type: "worktree"`, the MCP tools use the MCP server working directory, so Docker setups should mount the desired project and set the container working directory instead of passing host paths.

To work with multiple repositories in the same long-running container, mount their common parent directory and start the MCP process from that parent. Then pass a relative `source.workingDirectory` such as `repo-a` or `team/repo-b` for local worktree and commit sources.

For a direct Docker command, use:

```bash
docker run --rm -i \
  --pull always \
  -v "$PWD:/workspace" \
  -w /workspace \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

For AST diff browser use, also publish the WebDiff port:

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

### Windows

For Windows, you can use a lighter functional config directly in your MCP client configuration without needing a wrapper script:

```json
 {
   "mcpServers": {
     "refactoringminer": {
       "type": "stdio",
       "command": "cmd",
       "args": [
         "/c",
         "docker rm -f refactoringminer-server 2>nul && docker run --rm -i --name refactoringminer-server --pull always -v \"C:/absolute/path/to/your/repo:/workspace\" -w /workspace -p 6785-6795:6785-6795 -e OAuthToken=%OAuthToken% tsantalis/refactoringminer:latest mcp"
       ]
     }
   }
 }
```
Replace `C:/absolute/path/to/your/repo` and `%OAuthToken%` with your actual path and token.

The MCP tools do not accept `repositoryPath`. Mount the desired repository and start the MCP process from the container working directory that should be analyzed, usually `/workspace`.

Outside Docker, WebDiff binds to `127.0.0.1` by default. These settings can be overridden with environment variables or JVM system properties:

| Setting | Environment variable | JVM system property | Default |
|---------|----------------------|---------------------|---------|
| WebDiff bind host | `REFACTORINGMINER_WEBDIFF_BIND_HOST` | `refactoringminer.webdiff.bindHost` | `127.0.0.1` |
| Returned URL host | `REFACTORINGMINER_WEBDIFF_PUBLIC_HOST` | `refactoringminer.webdiff.publicHost` | `127.0.0.1` |

## Tools

The MCP surface has three tools:

| Tool | Purpose |
|------|---------|
| `refactoringminer_analyze` | Detect refactorings from a source and return JSON for the agent. |
| `refactoringminer_validate` | Check whether detected refactorings from a source match an intended refactoring. |
| `refactoringminer_diff` | Start a local WebDiff page for a source and return the browser URL plus compact JSON. |

All three tools accept a `source` object. If `source` is omitted, it defaults to `{ "type": "worktree" }`. If `source.type` is omitted, the server infers it from unambiguous fields: `url` selects `url`, `pullRequestId` or `cloneUrl` selects `pullRequest`, `commitId` selects `commit`, `beforeFiles` or `afterFiles` selects `fileContents`, and `beforePath` or `afterPath` selects `directories`.

| `source.type` | Supported by | Source fields |
|---------------|--------------|---------------|
| `worktree` | analyze, validate, diff | `workingDirectory`, `baseRef`, `includeUntracked`, `maxFiles`, `maxBytesPerFile` |
| `fileContents` | analyze, validate, diff | `beforeFiles`, `afterFiles`, `maxFiles`, `maxBytesPerFile` |
| `commit` | analyze, validate, diff | `workingDirectory`, `commitId`, `parentIndex` |
| `pullRequest` | analyze, validate, diff | `cloneUrl`, `pullRequestId`, `timeoutSeconds` |
| `directories` | analyze, validate | `beforePath`, `afterPath` |
| `url` | diff | `url`, `parentIndex`, `timeoutSeconds` |

The MCP tools do not accept `repositoryPath`. For local worktree and commit sources, start the MCP server in the target repository, or pass `source.workingDirectory` as a relative path under the MCP server working directory. Absolute paths and paths that escape the MCP server working directory are rejected.

`refactoringminer_analyze` accepts `source` and `maxRefactorings`. Its result contains `status`, `summary`, `refactoringCount`, `astDiffCount`, `moveAstDiffCount`, `filesBefore`, `filesAfter`, a limited `refactorings` list, and `warnings`.

`refactoringminer_validate` accepts `source`, `intent`, and `maxCandidates`. Validation results contain `status`, `summary`, `intent`, `refactoringCount`, `candidateCount`, limited `matches`, limited `candidates`, and `warnings`.

`refactoringminer_diff` accepts `source`, `port`, and `maxRefactorings`. Its result contains `status`, `summary`, `message`, `url`, `port`, `inputSummary`, `refactoringCount`, `astDiffCount`, `moveAstDiffCount`, `filesBefore`, `filesAfter`, a limited `refactorings` list, a limited `affectedFiles` list, and `warnings`.

The default port is `6789`. The returned `message` uses the same wording as the WebDiff CLI startup line:

```text
Starting server: http://127.0.0.1:6789
```

MCP tools do not auto-open the desktop browser. They bind WebDiff to `127.0.0.1` by default and return the URL in the tool result so the user or client can decide when to open it.

The returned URL is only available while the MCP server process is still running. If an MCP client starts RefactoringMiner for a single command and immediately exits, the local WebDiff server can disappear before the user opens the URL. Use an interactive client session for browser tools, or use the direct WebDiff CLI when the goal is only manual browser inspection.

Repeated calls replace the active local WebDiff view in the MCP server process. MCP-managed WebDiff pages hide the WebDiff Quit button so the MCP server can keep serving later calls. If the requested port is invalid or already occupied by another process, the tool returns an `error` result with a summary and warnings.

Example analyze request:

```json
{
  "source": {
    "type": "worktree",
    "workingDirectory": "repo-a",
    "baseRef": "HEAD",
    "includeUntracked": false
  },
  "maxRefactorings": 20
}
```

Example validation request:

```json
{
  "source": {
    "type": "fileContents",
    "beforeFiles": {
      "src/main/java/example/Worker.java": "package example; class Worker { void computeTotal() {} }"
    },
    "afterFiles": {
      "src/main/java/example/Worker.java": "package example; class Worker { void calculateTotal() {} }"
    }
  },
  "intent": {
    "type": "Rename Method",
    "methodNames": ["calculateTotal"]
  }
}
```

Example URL browser request:

```json
{
  "source": {
    "type": "url",
    "url": "https://github.com/tsantalis/RefactoringMiner/pull/1234",
    "timeoutSeconds": 300
  },
  "port": 6789
}
```

Example pull-request browser request with inferred source type:

```json
{
  "source": {
    "cloneUrl": "https://github.com/tsantalis/RefactoringMiner.git",
    "pullRequestId": 1055
  },
  "port": 6789
}
```

## Example Agent Prompts

Claude Code:

- "Use the RefactoringMiner MCP server to analyze my current worktree against `HEAD` and report detected refactorings."
- "Use `refactoringminer_diff` with a URL source for `https://github.com/tsantalis/RefactoringMiner/pull/1234` and return the startup message and URL."

Codex:

- "Before writing the final answer, call `refactoringminer_analyze` on the changed worktree and report the detected refactorings."
- "Call `refactoringminer_diff` with this commit URL as a URL source and `maxRefactorings: 0`; use the counts and warnings only."

Generic MCP clients:

- "Call `refactoringminer_analyze` with `{ \"source\": { \"type\": \"worktree\", \"baseRef\": \"HEAD\", \"includeUntracked\": false } }`."
- "Call `refactoringminer_diff` with `{ \"source\": { \"type\": \"url\", \"url\": \"https://github.com/owner/repo/commit/abc123\", \"parentIndex\": 1 } }`."

## GitHub URLs

GitHub pull-request, commit, and URL sources read GitHub data through the existing RefactoringMiner GitHub API path. For private repositories or higher rate limits, provide an OAuth token with one of these mechanisms:

- Environment variable: `OAuthToken`
- JVM system property: `-DOAuthToken=...`
- `github-oauth.properties` in the MCP process working directory with `OAuthToken=...`

The MCP tools only read GitHub data. They do not post comments, mark files viewed, edit pull requests, or change repository state.

For `refactoringminer_diff`, pass a GitHub pull-request URL or commit URL as `source.url` with `source.type: "url"`. Plain issue URLs are not supported. The returned WebDiff URL is local to the machine running the MCP server. It is not a hosted report link.

## What the server does not do

The MCP server is read-only.

- It does not modify files.
- It does not create commits, branches, checkouts, or pushes.
- It does not post GitHub comments or mutate pull requests.
- It does not run an external LLM.
- It does not host an HTTP MCP server.
- It does not auto-open the desktop browser from MCP tools.

Detected refactorings are not a behavior-preservation proof. They do not prove tests pass, specifications still hold, side effects are unchanged, or the edit contains no non-refactoring behavior changes.

Use tests, review, specifications, or domain-specific checks alongside MCP results.

MCP JSON results summarize detected refactorings, AST diff counts, moved AST diff counts, and affected files. They do not stream full machine-readable AST mappings or moved-node ranges in this release. Use the WebDiff browser tools when a review needs mapping-level evidence for moved, renamed, extracted, or inlined code.

This release does not include GitHub Action comments, static hosted HTML reports, hosted HTTP MCP, write tools, commit-range MCP analysis, large artifact or resource streaming, or PurityChecker-backed validation results.

## Troubleshooting

- If a returned WebDiff URL refuses the connection, confirm the MCP client session that started it is still running. For manual inspection, use the direct `diff` CLI instead.
- If port `6789` is already in use, call `refactoringminer_diff` with another `port` value, or stop the existing local WebDiff process.
- If Docker-based worktree sources use the wrong repository, mount the desired project and set `-w /workspace` in the Docker command. For multiple repositories, mount a common parent and use relative `source.workingDirectory`. The MCP tools cannot resolve host paths from inside the container.
- If URL diffs fail on a private repository or after many calls, configure `OAuthToken` through the environment, JVM system property, or `github-oauth.properties`.
