# RefactoringMiner
RefactoringMiner is a library/API written in Java that can detect refactorings applied in the history of a Java project.

Currently, it supports the detection of the following refactorings:

1. Extract Method
2. Inline Method
3. Move Method/Attribute
4. Pull Up Method/Attribute
5. Push Down Method/Attribute
6. Extract Superclass/Interface
7. Move Class
8. Rename Class
9. Rename Method
10. Extract and Move Method
11. Change Package (Move, Rename, Split, Merge)

In order to build the project, run `./gradlew jar` (or `gradlew jar`, in Windows) in the project's root directory.
Alternatively, you can generate a complete distribution zip including all runtime dependencies running `./gradlew distZip`.

You can also work with the project with Eclipse IDE. First, run `./gradlew eclipse` to generate Eclipse project metadata files. Then, import it into Eclipse using the *Import Existing Project* feature.

## Research ##
If you are using RefactoringMiner in your research, please cite the following paper:

Nikolaos Tsantalis, Matin Mansouri, Laleh Eshkevari, Davood Mazinanian, and Danny Dig, "[Accurate and Efficient Refactoring Detection in Commit History](https://users.encs.concordia.ca/~nikolaos/publications/ICSE_2018.pdf)," *40th International Conference on Software Engineering* (ICSE 2018), Gothenburg, Sweden, May 27 - June 3, 2018.

RefactoringMiner has been used in the following studies:
* Danilo Silva, Nikolaos Tsantalis, and Marco Tulio Valente, "[Why We Refactor? Confessions of GitHub Contributors](https://doi.org/10.1145/2950290.2950305)," *24th ACM SIGSOFT International Symposium on the Foundations of Software Engineering* (FSE 2016), Seattle, WA, USA, November 13-18, 2016.
* Diego Cedrim, Alessandro Garcia, Melina Mongiovi, Rohit Gheyi, Leonardo Sousa, Rafael de Mello, Baldoino Fonseca, Márcio Ribeiro, and Alexander Chávez, "[Understanding the impact of refactoring on smells: a longitudinal study of 23 software projects](https://doi.org/10.1145/3106237.3106259)," *11th Joint Meeting on Foundations of Software Engineering* (ESEC/FSE 2017), Paderborn, Germany, September 4-8, 2017.
* Alexander Chávez, Isabella Ferreira, Eduardo Fernandes, Diego Cedrim, and Alessandro Garcia, "[How does refactoring affect internal quality attributes?: A multi-project study](https://doi.org/10.1145/3131151.3131171)," *31st Brazilian Symposium on Software Engineering* (SBES 2017), Fortaleza, CE, Brazil, September 20-22, 2017.
* Mehran Mahmoudi, and Sarah Nadi, "[An Empirical Study of Android Changes in CyanogenMod](https://arxiv.org/abs/1801.02716)," 	arXiv:1801.02716, January 2018.

## Contributors ##
The code in package **gr.uom.java.xmi.*** has been developed by [Nikolaos Tsantalis](https://github.com/tsantalis).

The code in package **org.refactoringminer.*** has been developed by [Danilo Ferreira e Silva](https://github.com/danilofes).

## API usage guidelines ##

RefactoringMiner can automatically detect refactorings in the entire history of 
git repositories, between specified commits or tags, or at specified commits.

In the code snippet below we demonstrate how to print all refactorings performed
in the toy project https://github.com/danilofes/refactoring-toy-example.git.

```java
GitService gitService = new GitServiceImpl();
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

Repository repo = gitService.cloneIfNotExists(
    "tmp/refactoring-toy-example",
    "https://github.com/danilofes/refactoring-toy-example.git");

miner.detectAll(repo, "master", new RefactoringHandler() {
  @Override
  public void handle(RevCommit commitData, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitData.getId().getName());
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

You can also analyze between commits using `detectBetweenCommits` or between tags using `detectBetweenTags`. RefactoringMiner will start from commit or tag as specified and iterate backwards. If the end commit or end tag is not specified, RefactoringMiner will detect until the first beginning.

```java
miner.detectBetweenCommits(repo, 
    "d4bce13a443cf12da40a77c16c1e591f4f985b47", "dde3ef036bdddae550c4e98373db4c81d77b5043",
    new RefactoringHandler() {
  @Override
  public void handle(RevCommit commitData, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitData.getId().getName());
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

```java
miner.detectBetweenTags(repo, "1.1", "1.0", new RefactoringHandler() {
  @Override
  public void handle(RevCommit commitData, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitData.getId().getName());
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

It is possible to analyze a specifc commit using `detectAtCommit` instead of `detectAll`. The commit
is identified by its SHA key, such as in the example below:

```java
miner.detectAtCommit(repo, "https://github.com/danilofes/refactoring-toy-example.git",
    "05c1e773878bbacae64112f70964f4f2f7944398", new RefactoringHandler() {
  @Override
  public void handle(RevCommit commitData, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitData.getId().getName());
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

There is also a lower level API that works comparing the source code from two
folders that contain the code before and after the code changes:  

```java
// Assuming you have a List<String> of the changed/added/removed file paths from version1 to version2
// filename format example: /src/gr/uom/java/xmi/UMLModelASTReader.java

File rootFolder1 = new File("/path/to/version1/");
File rootFolder2 = new File("/path/to/version2/");
List<String> filePaths1 = new ArrayList<String>();
filePaths1.add("/src/package/Foo.java");
List<String> filePaths2 = new ArrayList<String>();
filePaths2.add("/src/package/Foo.java");

UMLModel model1 = new UMLModelASTReader(rootFolder1, filePaths1).getUmlModel();
UMLModel model2 = new UMLModelASTReader(rootFolder2, filePaths2).getUmlModel();
UMLModelDiff modelDiff = model1.diff(model2);
List<Refactoring> refactorings = modelDiff.getRefactorings();
```

Note that by doing this you may get different results from the git history analysis, because
the latter uses information from git to better identify moves and renames.


## Running from the command line ##

When you build a distributable application with `./gradlew distZip`, you can run Refactoring Miner as a command line application. Extract the file under `build/distribution/RefactoringMiner.zip` in the desired location, and cd into the `bin` folder (or include it in your path). Then, run `RefactoringMiner -h` to show its usage:

    > RefactoringMiner -h

	-h															Show tips
	-a <git-repo-folder> <branch>								Detect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.
	-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>	Detect refactorings Between <star-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>
	-bt <git-repo-folder> <start-tag> <end-tag>					Detect refactorings Between <start-tag> and <end-tag> for project <git-repo-folder>
	-c <git-repo-folder> <commit-sha1>							Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>
	
For example, suppose that you run:

    > git clone https://github.com/danilofes/refactoring-toy-example.git refactoring-toy-example
    > RefactoringMiner -c refactoring-toy-example 36287f7c3b09eff78395267a3ac0d7da067863fd

The output would be:

    4 refactorings found in commit 36287f7c3b09eff78395267a3ac0d7da067863fd:
      Pull Up Attribute     private age : int from class org.animals.Labrador to class org.animals.Dog
      Pull Up Attribute     private age : int from class org.animals.Poodle to class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog

When you run Refactoring with `-a`, `-bc`, `-bt`, after all commits are analyzed, a result `csv` file which use semicolon `;` as delimiter will be generated in the repository directory.
