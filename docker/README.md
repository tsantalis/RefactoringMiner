# RefactoringMiner docker image

## Installation

You can directly pull a version of latest RefactoringMiner image from Docker Hub via the command `docker pull tsantalis/refactoringminer`.

You can also compile the image on your machine. Go to the root RefactoringMiner's folder and compile the image: `docker build --pull always . -f docker/Dockerfile -t tsantalis/refactoringminer`.

In addition to this image which relies on JDK, you can try our native-image which relies on GraalVM to compile the Java bytecode to native code for better performance: `docker build --pull always . -f docker/native/Dockerfile-native -t tsantalis/refactoringminer`.

## Usage

To use the Docker `refactoringminer` image with the WebDiff browser, publish the container port `6789`.

The classical way to run the container is the command `docker run --pull always -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 6789:6789 tsantalis/refactoringminer diff --src left/ --dst right/`. You can consult the diff at the URL `http://localhost:6789`.

Of course, all other RefactoringMiner's commands are available.

> **Note:** `--pull always` ensures Docker always pulls the latest image from Docker Hub before running. Since RefactoringMiner is actively developed and the image is regularly updated, this guarantees you benefit from the latest features and fixes without having to manually run `docker pull`.

## MCP server

The Docker image can also run RefactoringMiner's stdio MCP server. This keeps MCP updates on the same Docker Hub path as the command-line tool.

```terminal
docker run --rm -i --pull always -e OAuthToken=$OAuthToken tsantalis/refactoringminer:latest mcp
```

For the default `refactoringminer_diff` worktree mode, mount the repository into the container and set it as the working directory:

```terminal
docker run --rm -i \
  --pull always \
  -v "$PWD:/workspace" \
  -w /workspace \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

For MCP AST diff browser use, also publish the WebDiff port:

```terminal
docker run --rm -i \
  --pull always \
  -v "$PWD:/workspace" \
  -w /workspace \
  -p 6789:6789 \
  -e OAuthToken=$OAuthToken \
  tsantalis/refactoringminer:latest mcp
```

The MCP tools do not accept host repository paths. Mount the desired repository and start the MCP process from the container working directory that should be analyzed, usually `/workspace`. To work with multiple repositories in the same container, mount their common parent directory and use relative `source.workingDirectory` values for local worktree and commit sources.

The Docker image binds WebDiff inside the container to `0.0.0.0`, but returned links still use `http://127.0.0.1:<port>` for the host browser. Override these only if your Docker setup needs a different address:

```terminal
-e REFACTORINGMINER_WEBDIFF_BIND_HOST=0.0.0.0
-e REFACTORINGMINER_WEBDIFF_PUBLIC_HOST=localhost
```

## Git integration

You can easily integrate RefactoringMiner's container with Git by adding the following configuration into the `$HOME/.gitconfig` file.

### macOS

```properties
[difftool "rm-docker"]
    cmd = docker run --pull always -v /private/$LOCAL:/diff/left/ -v /private/$REMOTE:/diff/right/ -p 6789:6789 tsantalis/refactoringminer  diff --src left/ --dst right/
```

### Linux and Windows

```properties
[difftool "rm-docker"]
	cmd = docker run --pull always -v $LOCAL:/diff/left -v $REMOTE:/diff/right -p 6789:6789 tsantalis/refactoringminer  diff --src left/ --dst right/
```

### Usage

You can invoke RefactoringMiner from git by running the command `git difftool -d --no-symlinks -t rm-docker`. We recommend putting an alias in the alias section of `$HOME/.gitconfig` such as:

```properties
[alias]
	rmd = difftool -d --no-symlinks -t rm-docker
```

You can then use the command `git rmd`.

Note: Ensure your Docker setup includes access to virtual file shares, particularly for the tmp folder, as Git temporarily stores revisions there. Configure necessary read permissions appropriately.

## GitHub Pull Request and Commit integration

You can execute RefactoringMiner's container on a GitHub Pull Request or Commit by passing `OAuthToken` as an environment variable as follows:

```terminal
docker run --pull always -p 6789:6789 -e OAuthToken=ghp_Tz... tsantalis/refactoringminer diff --url https://github.com/JabRef/jabref/pull/14138
```

Replace `ghp_Tz...` with your own personal OAuth token.
This is a [generated classic personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic)

In case you want to store your token in a file (instead of having it in the bash history):

```
docker run -p 6789:6789  -v c:\users\{login}\.refactoringminer:/diff tsantalis/refactoringminer diff --url https://github.com/JabRef/jabref/pull/14138
```

In `c:\users\{login}\.refactoringminer` (`~/.refactoringminer` on Linux/macOS), you need to create a file `github-oauth.properties` where your personal GitHub OAuth resides:

```properties
OAuthToken=ghp_Tz...
```
