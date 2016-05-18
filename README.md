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
The code in package **gr.uom.java.xmi** has been developed by [Nikolaos Tsantalis](https://github.com/tsantalis).

The code in package **ca.ualberta.cs.data** has been developed by [Nikolaos Tsantalis](https://github.com/tsantalis) and **Fabio Rocha**.

The code in package **br.ufmg.dcc.labsoft.refactoringanalyzer** has been developed by [Danilo Ferreira e Silva](https://github.com/danilofes).

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
UMLModel model1 = new ASTReader(new File("/path/to/version1/")).getUmlModel();
UMLModel model2 = new ASTReader(new File("/path/to/version2/")).getUmlModel();
UMLModelDiff modelDiff = model1.diff(model2);
List<Refactoring> refactorings = modelDiff.getRefactorings();
```

Note that by doing this you may get different results from the git history analysis, because
it uses information from git to better identify moves and renames.
