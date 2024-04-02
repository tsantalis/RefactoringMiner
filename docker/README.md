# RefactoringMiner docker image

## Installation

You can directly pull a version of latest RefactoringMiner image from Docker Hub via the command `docker pull tsantalis/refactoringminer`.

You can also compile the image on your machine. Go to the root RefactoringMiner's folder and compile the image: `docker build . -f docker/Dockerfile -t tsantalis/refactoringminer`.

In addition to this image which relies on JDK, you can try our native-image which relies on GraalVM to compile the Java bytecode to native code for better performance: `docker build . -f docker/native/Dockerfile-native -t tsantalis/refactoringminer`.

## Usage

To use refactoringminer image, You need to:
* bind the port `6789` of the container to access web interface

The classical way to run the container is the command `docker run -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 6789:6789 tsantalis/refactoringminer diff --src left/ --dst right/`. You can consult the diff at the URL `http://localhost:6789`.

Of course, all other RefactoringMiner's commands are available.


## Git integration

You can easily integrate RefactoringMiner's container with Git by adding the following configuration into the `$HOME/.gitconfig` file.

### Mac OS

```properties
[difftool "rm-docker"]
    cmd = docker run -v /private/$LOCAL:/diff/left/ -v /private/$REMOTE:/diff/right/ -p 6789:6789 tsantalis/refactoringminer  diff --src left/ --dst right/
```
### Linux - Windows

```properties
[difftool "rm-docker"]
	cmd = docker run -v $LOCAL:/diff/left -v $REMOTE:/diff/right -p 6789:6789 tsantalis/refactoringminer  diff --src left/ --dst right/
```

### Usage

You can invoke RefactoringMiner from git by running the command `git difftool -d --no-symlinks -t rm-docker`. We recommend putting an alias in the alias section of `$HOME/.gitconfig` such as:

```properties
[alias]
	rmd = difftool -d --no-symlinks -t rm-docker
```

You can then use the command `git rmd`.