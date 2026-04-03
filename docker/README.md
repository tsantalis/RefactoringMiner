# RefactoringMiner docker image

## Installation

You can directly pull a version of latest RefactoringMiner image from Docker Hub via the command `docker pull tsantalis/refactoringminer`.

You can also compile the image on your machine. Go to the root RefactoringMiner's folder and compile the image: `docker build --pull always . -f docker/Dockerfile -t tsantalis/refactoringminer`.

In addition to this image which relies on JDK, you can try our native-image which relies on GraalVM to compile the Java bytecode to native code for better performance: `docker build --pull always . -f docker/native/Dockerfile-native -t tsantalis/refactoringminer`.

## Usage

To use the Docker `refactoringminer` image, you need to bind the port `6789` of the container to access web interface and run the 

The classical way to run the container is the command `docker run --pull always -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 6789:6789 tsantalis/refactoringminer diff --src left/ --dst right/`. You can consult the diff at the URL `http://localhost:6789`.

Of course, all other RefactoringMiner's commands are available.

> **Note:** `--pull always` ensures Docker always pulls the latest image from Docker Hub before running. Since RefactoringMiner is actively developed and the image is regularly updated, this guarantees you benefit from the latest features and fixes without having to manually run `docker pull`.

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
