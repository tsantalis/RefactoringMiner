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

In order to build the project, run `./gradlew jar` (or `gradlew jar`, in Windows) in the project's root directory.
Alternatively, you can generate a complete distribution zip including all runtime dependencies running `./gradlew distZip`.

You can also work with the project with Eclipse IDE. First, run `./gradlew eclipse` to generate Eclipse project metadata files. Then, import it into Eclipse using the *Import Existing Project* feature.

## Contributors ##
The code in package **gr.uom.java.xmi.*** has been developed by [Nikolaos Tsantalis](https://github.com/tsantalis).

The code in package **br.ufmg.dcc.labsoft.refactoringanalyzer.*** and **org.refactoringminer.*** has been developed by [Danilo Ferreira e Silva](https://github.com/danilofes).

## API usage guidelines ##

RefactoringMiner can automatically detect refactorings in the entire history of 
git repositories, or at specified commits.
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

It is possible to analyze a specifc commit using `detectAtCommit` instead of `detectAll`. The commit
is identified by its SHA key, such as in the example below:

```java
miner.detectAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398", new RefactoringHandler() {
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

When you build a distributable application with `./gradlew distZip`, you can run Refactoring Miner as a command line application. Extract the file under `build/distribution/RefactoringMiner.zip` in the desired location, and cd into the `bin` folder (or include it in your path). Them, run RefactoringMiner using the following syntax:

    > RefactoringMiner <path-to-git-repo> <commit-sha1>

For example, supose that you run:

    > git clone https://github.com/danilofes/refactoring-toy-example.git refactoring-toy-example
    > RefactoringMiner refactoring-toy-example 36287f7c3b09eff78395267a3ac0d7da067863fd

The output would be:

    4 refactorings found in commit 36287f7c3b09eff78395267a3ac0d7da067863fd:
      Pull Up Attribute     private age : int from class org.animals.Labrador to class org.animals.Dog
      Pull Up Attribute     private age : int from class org.animals.Poodle to class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog


## Research ##

RefactoringMiner has been used in the following papers:
* Danilo Silva, Nikolaos Tsantalis, and Marco Tulio Valente, "[Why We Refactor? Confessions of GitHub Contributors](http://arxiv.org/pdf/1607.02459v1)," *24th ACM SIGSOFT International Symposium on the Foundations of Software Engineering* (FSE'2016), Seattle, WA, USA, November 13-18, 2016.
