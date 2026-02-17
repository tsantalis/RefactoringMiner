# Refactoring detection API usage guidelines
## With a locally cloned git repository
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
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

You can also analyze between commits using `detectBetweenCommits` or between tags using `detectBetweenTags`. RefactoringMiner will iterate through all *non-merge* commits from **start** commit/tag to **end** commit/tag.

```java
// start commit: 819b202bfb09d4142dece04d4039f1708735019b
// end commit: d4bce13a443cf12da40a77c16c1e591f4f985b47
miner.detectBetweenCommits(repo, 
    "819b202bfb09d4142dece04d4039f1708735019b", "d4bce13a443cf12da40a77c16c1e591f4f985b47",
    new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

```java
// start tag: 1.0
// end tag: 1.1
miner.detectBetweenTags(repo, "1.0", "1.1", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

It is possible to analyze a specific commit using `detectAtCommit` instead of `detectAll`. The commit
is identified by its SHA key, such as in the example below:

```java
miner.detectAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With two directories containing Java source code

It is possible to detect refactorings between the Java files in two directories
containing the code before and after some changes.
This feature supports the detection of renamed and moved classes,
and automatically excludes from the analysis any files with identical contents:  

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
File dir1 = new File("/home/user/tmp/v1");
File dir2 = new File("/home/user/tmp/v2");
miner.detectAtDirectories(dir1, dir2, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
Path dir1 = Paths.get("/home/user/tmp/v1");
Path dir1 = Paths.get("/home/user/tmp/v2");
miner.detectAtDirectories(dir1, dir2, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With file contents as strings

You can provide two maps (before and after the changes) where the keys are file paths, and the values are the corresponding file contents.
Each key should correspond to a file path starting from the root of the repository. For example, `src/main/java/org/refactoringminer/api/GitHistoryRefactoringMiner.java`.

After populating the maps, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// Each key should correspond to a file path starting from the root of the repository
Map<String, String> fileContentsBefore;
Map<String, String> fileContentsAfter;
// populate the maps
miner.detectAtFileContents(fileContentsBefore, fileContentsAfter, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
});
```

## With all information fetched directly from GitHub

To use this API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

If you don't want to clone locally the repository, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
miner.detectAtCommit("https://github.com/danilofes/refactoring-toy-example.git",
    "36287f7c3b09eff78395267a3ac0d7da067863fd", new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
}, 10);
```

## With each commit in a GitHub Pull request

To use this API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

If you want to analyze all commits of a pull request, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
String repo = "https://github.com/apache/drill.git";
miner.detectAtPullRequest(repo, 1807, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
}, 100);
```

## With a commit range

This is a special API that aggregates the changes between two commits, and detects refactorings between the initial state of the files
and the last-modified state of the files within the specified commit range.

This API is inspired by the work of Lei Chen and Shinpei Hayashi, "Impact of Change Granularity in Refactoring Detection," Proceedings of the 30th IEEE/ACM International Conference on Program Comprehension, 565-569, 2022.

`repo` can be either a JGit Repository object (i.e., locally cloned repository), or the Git URL of a repository (i.e., information fetched directly from GitHub).
`startCommit` and `endCommit` are the SHA-1 of the start and end commits, respectively.

To use the latter API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
List<Refactoring> refactorings = miner.detectAtCommitRange(repo, startCommit, endCommit);
```

# AST Diff API usage guidelines

RefactoringMiner is actually the only tool that generates AST diff at commit level, supports multi-mappings (one-to-many, many-to-one, many-to-many mappings), matches AST nodes of different AST types, and supports semantic diff in a fully refactoring-aware fashion.
You can explore its advanced AST diff capabilities in our [AST Diff Gallery](https://github.com/tsantalis/RefactoringMiner/wiki/AST-Diff-Gallery).

All AST Diff APIs return a `ProjectASTDiff` object. By calling `getDiffSet()` on it, you can obtain a
`Set<ASTDiff>`, where each [ASTDiff](https://github.com/tsantalis/RefactoringMiner/blob/master/src/org/refactoringminer/astDiff/actions/ASTDiff.java) object corresponds to a pair of Java Compilation Units.

`ASTDiff` extends `com.github.gumtreediff.actions.Diff` and thus it is compatible with the [GumTree](https://github.com/GumTreeDiff/gumtree) core APIs.

More detailed documentation can be found in [GitHistoryRefactoringMiner](https://github.com/tsantalis/RefactoringMiner/blob/master/src/main/java/org/refactoringminer/api/GitHistoryRefactoringMiner.java) JavaDoc.

## With commit of a locally cloned git repository
```java
// With a locally cloned git repository
GitService gitService = new GitServiceImpl();
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

Repository repo = gitService.cloneIfNotExists(
    "tmp/refactoring-toy-example",
    "https://github.com/danilofes/refactoring-toy-example.git");

ProjectASTDiff projectASTDiff = miner.diffAtCommit(repo,
    "36287f7c3b09eff78395267a3ac0d7da067863fd");
Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
// To visualize the diff add the following line
new WebDiff(projectASTDiff).run();
```
## With commit fetched directly from GitHub
To use the following API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.
```java
// With all information fetched directly from GitHub
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
String repo = "https://github.com/danilofes/refactoring-toy-example.git";
ProjectASTDiff projectASTDiff = miner.diffAtCommit(repo,
    "36287f7c3b09eff78395267a3ac0d7da067863fd", 10);
Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
// To visualize the diff add the following line
new WebDiff(projectASTDiff).run();
```

## With the files changed in a GitHub Pull Request
To use the following API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.
```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
String repo = "https://github.com/JabRef/jabref.git";
int PR = 10847;
ProjectASTDiff projectASTDiff = miner.diffAtPullRequest(repo, PR, 100);
Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
// To visualize the diff add the following line
new WebDiff(projectASTDiff).run();
```

## With two directories
```java
// With two directories containing Java source code (File API)
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
File dir1 = new File("/home/user/tmp/v1");
File dir2 = new File("/home/user/tmp/v2");
ProjectASTDiff projectASTDiff = miner.diffAtDirectories(dir1, dir2);
Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
// To visualize the diff add the following line
new WebDiff(projectASTDiff).run();
```

```java
// With two directories containing Java source code (Path API)
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
// You must provide absolute paths to the directories. Relative paths will cause exceptions.
Path dir1 = Paths.get("/home/user/tmp/v1");
Path dir1 = Paths.get("/home/user/tmp/v2");
ProjectASTDiff projectASTDiff = miner.diffAtDirectories(dir1, dir2);
Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
// To visualize the diff add the following line
new WebDiff(projectASTDiff).run();
```

## With commit range

This is a special API that aggregates the changes between two commits, and generates AST diff between the initial state of the files
and the last-modified state of the files within the specified commit range.

This API is inspired by the work of Lei Chen and Shinpei Hayashi, "Impact of Change Granularity in Refactoring Detection," Proceedings of the 30th IEEE/ACM International Conference on Program Comprehension, 565-569, 2022.

`repo` can be either a JGit Repository object (i.e., locally cloned repository), or the Git URL of a repository (i.e., information fetched directly from GitHub).
`startCommit` and `endCommit` are the SHA-1 of the start and end commits, respectively.

To use the latter API, please provide a valid OAuth token in the `github-oauth.properties` file.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
ProjectASTDiff projectASTDiff = miner.diffAtCommitRange(repo, startCommit, endCommit);
// To visualize the diff add the following line
new WebDiff(projectASTDiff).openInBrowser();
```

# Location information for the detected refactorings
All classes implementing the `Refactoring` interface include refactoring-specific location information.
For example, `ExtractOperationRefactoring` offers the following methods:

1. `getSourceOperationCodeRangeBeforeExtraction()` : Returns the code range of the source method in the **parent** commit
2. `getSourceOperationCodeRangeAfterExtraction()` : Returns the code range of the source method in the **child** commit
3. `getExtractedOperationCodeRange()` : Returns the code range of the extracted method in the **child** commit
4. `getExtractedCodeRangeFromSourceOperation()` : Returns the code range of the extracted code fragment from the source method in the **parent** commit
5. `getExtractedCodeRangeToExtractedOperation()` : Returns the code range of the extracted code fragment to the extracted method in the **child** commit
6. `getExtractedOperationInvocationCodeRange()` : Returns the code range of the invocation to the extracted method inside the source method in the **child** commit

Each method returns a `CodeRange` object including the following properties:
```java
String filePath
int startLine
int endLine
int startColumn
int endColumn
```
Alternatively, you can use the methods `List<CodeRange> leftSide()` and `List<CodeRange> rightSide()` to get a list of `CodeRange` objects for the left side (i.e., **parent** commit) and right side (i.e., **child** commit) of the refactoring, respectively.

# Statement matching information for the detected refactorings
All method-related refactoring (Extract/Inline/Move/Rename/ExtractAndMove Operation) objects come with a `UMLOperationBodyMapper` object, which can be obtained by calling method `getBodyMapper()` on the refactoring object.

Let's consider the Extract Method refactoring in commit [JetBrains/intellij-community@7ed3f27](https://github.com/JetBrains/intellij-community/commit/7ed3f273ab0caf0337c22f0b721d51829bb0c877)

![example|1665x820](https://user-images.githubusercontent.com/1483516/52974463-b0240000-338f-11e9-91e2-966f20be2514.png)

#1. You can use the following code snippet to obtain the **newly added statements** in the extracted method:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
List<StatementObject> newLeaves = mapper.getNonMappedLeavesT2(); //newly added leaf statements
List<CompositeStatementObject> newComposites = mapper.getNonMappedInnerNodesT2(); //newly added composite statements
List<StatementObject> deletedLeaves = mapper.getNonMappedLeavesT1(); //deleted leaf statements
List<CompositeStatementObject> deletedComposites = mapper.getNonMappedInnerNodesT1(); //deleted composite statements
```
For the Extract Method Refactoring example shown above `mapper.getNonMappedLeavesT2()` returns the following statements:
```java
final String url = pageNumber == 0 ? "courses" : "courses?page=" + String.valueOf(pageNumber);
final CoursesContainer coursesContainer = getFromStepic(url,CoursesContainer.class);
return coursesContainer.meta.containsKey("has_next") && coursesContainer.meta.get("has_next") == Boolean.TRUE;
```
#2. You can use the following code snippet to obtain the **matched statements** between the original and the extracted methods:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
for(AbstractCodeMapping mapping : mapper.getMappings()) {
  AbstractCodeFragment fragment1 = mapping.getFragment1();
  AbstractCodeFragment fragment2 = mapping.getFragment2();
  Set<Replacement> replacements = mapping.getReplacements();
  for(Replacement replacement : replacements) {
    String valueBefore = replacement.getBefore();
    String valueAfter = replacement.getAfter();
    ReplacementType type = replacement.getType();
  }
}
```
For the Extract Method Refactoring example shown above `mapping.getReplacements()` returns the following AST node replacement for the pair of matched statements:
```java
final List<CourseInfo> courseInfos = getFromStepic("courses",CoursesContainer.class).courses;
final List<CourseInfo> courseInfos = coursesContainer.courses;
```
**Replacement**: `getFromStepic("courses",CoursesContainer.class)` -> `coursesContainer`

**ReplacementType**: VARIABLE_REPLACED_WITH_METHOD_INVOCATION

#3. You can use the following code snippet to obtain the **overlapping refactorings** in the extracted method:
```java
ExtractOperationRefactoring refactoring = ...;
UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
Set<Refactoring> overlappingRefactorings = mapper.getRefactorings();
```
For the Extract Method Refactoring example shown above `mapper.getRefactorings()` returns the following refactoring:

**Extract Variable** `coursesContainer : CoursesContainer` in method
`private addCoursesFromStepic(result List<CourseInfo>, pageNumber int) : boolean`
from class `com.jetbrains.edu.stepic.EduStepicConnector`

because variable `coursesContainer = getFromStepic(url,CoursesContainer.class)` has been extracted from the following statement of the original method by replacing string literal `"courses"` with variable `url`:
```java
final List<CourseInfo> courseInfos = getFromStepic("courses",CoursesContainer.class).courses;
```