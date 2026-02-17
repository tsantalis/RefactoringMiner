# Purity Checker
To check whether a refactoring detected in a commit is pure (i.e., it does not include overlapping behavior-changing edits) or impure, you can use the following APIs:

For the moment, PurityChecker supports 9 refactoring types, namely `Extract Method`, `Inline Method`, `Move Method`, `Pull Up Method`, `Push Down Method`, `Split Method`, `Extract and Move Method`, `Move and Inline Method`, `Move and Rename Method`.

**With commit of a locally cloned git repository** 
```java
GitService gitService = new GitServiceImpl();
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

Repository repo = gitService.cloneIfNotExists(
    "tmp/refactoring-toy-example",
    "https://github.com/danilofes/refactoring-toy-example.git");

miner.detectAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398", new RefactoringHandler() {
  @Override
  public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
      PurityCheckResult result = PurityChecker.check(ref, refactorings, modelDiff);
      if (result != null) {
        System.out.println(result);
      }
    }
  }
});
```

**With commit fetched directly from GitHub**

To use the following API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
miner.detectAtCommit("https://github.com/danilofes/refactoring-toy-example.git",
    "36287f7c3b09eff78395267a3ac0d7da067863fd", new RefactoringHandler() {
  @Override
  public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
      PurityCheckResult result = PurityChecker.check(ref, refactorings, modelDiff);
      if (result != null) {
        System.out.println(result);
      }
    }
  }
}, 10);
```