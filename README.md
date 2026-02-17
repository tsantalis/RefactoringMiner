![Alt](https://repobeats.axiom.co/api/embed/aaedbff1e1adfcefc54fff9dbdac2f5384cbcf87.svg "Repobeats analytics image")

[![Maven Central Version](https://img.shields.io/maven-central/v/com.github.tsantalis/refactoring-miner)](https://mvnrepository.com/artifact/com.github.tsantalis/refactoring-miner)
[![Docker Pulls](https://img.shields.io/docker/pulls/tsantalis/refactoringminer)](https://hub.docker.com/r/tsantalis/refactoringminer)
[![Chrome Web Store Version](https://img.shields.io/chrome-web-store/v/lnloiaibmonmmpnfibfjjlfcddoppmgd)](https://chrome.google.com/webstore/detail/refactoring-aware-commit/lnloiaibmonmmpnfibfjjlfcddoppmgd)
[![Chrome Web Store Users](https://img.shields.io/chrome-web-store/users/lnloiaibmonmmpnfibfjjlfcddoppmgd)](https://chrome.google.com/webstore/detail/refactoring-aware-commit/lnloiaibmonmmpnfibfjjlfcddoppmgd)
[![Tests](https://github.com/tsantalis/RefactoringMiner/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/tsantalis/RefactoringMiner/actions/workflows/gradle.yml)
[![Test Results](https://img.shields.io/endpoint?url=https%3A%2F%2Fgist.githubusercontent.com%2Ftsantalis%2F19fb416d06d1b4d40820e0209540f6c0%2Fraw%2FRefactoringMiner-junit-tests.json)](https://github.com/tsantalis/RefactoringMiner/actions/workflows/gradle.yml)

### Programming languages supported by RefactoringMiner:
| Language | Refactoring detection | AST diff generation | Roadmap |
|:-----------------------|:-----------|:--------|:--------|
|<img src="https://upload.wikimedia.org/wikipedia/en/3/30/Java_programming_language_logo.svg" alt="Java" height="60"/> | ✅ | ✅ | |
|<img src="https://upload.wikimedia.org/wikipedia/commons/f/f8/Python_logo_and_wordmark.svg" alt="Python" width="120"/> | ✅ | ✅ | <ul><li>- [ ] Support comments</li><li>- [ ] Support version 3.14</li></ul> |
|<img src="https://upload.wikimedia.org/wikipedia/commons/7/76/Kotlin_logo_%282021-present%29.svg" alt="Kotlin" width="100"/> | ✅ | ✅ | <ul><li>- [ ] Validate precision/recall</li></ul> |


### Open source projects using RefactoringMiner diff tool for code reviews:
[<img src="https://www.jabref.org/_nuxt/jabref.4Rtv1swz.svg" alt="JabRef" width="30"/>](https://github.com/JabRef/jabref)

Table of Contents
=================

   * [General info](#general-info)
      * [AST diff features](#ast-diff-features)
         * [Refactoring-aware tooltips](#refactoring-aware-tooltips)
         * [Refactoring listing with links to diffs](#refactoring-listing-with-links-to-diffs)
         * [Single Page View](#single-page-view)
         * [Embedded GitHub Code Review Comments](#embedded-github-code-review-comments)
         * [Diff for code moved between different files](#diff-for-code-moved-between-different-files)
         * [On-demand diff generation](#on-demand-diff-generation)
         * [Javadoc and comment reformatting](#javadoc-and-comment-reformatting)
   * [Refactoring Support History](documentation/history.md#refactoring-support-history) 
   * [Supported Refactoring Types](#supported-refactoring-types)
   * [Contributors](#contributors)
   * [Current precision and recall](documentation/accuracy.md#current-precision-and-recall)
      * [Java Benchmark 1](documentation/accuracy.md#java-benchmark-1)
      * [Java Benchmark 2](documentation/accuracy.md#java-benchmark-2)
      * [Python Benchmark](documentation/accuracy.md#python-benchmark) 
	  * [Kotlin Benchmark](documentation/accuracy.md#kotlin-benchmark) 
   * [How to build RefactoringMiner](documentation/how-to.md#how-to-build-refactoringminer)
   * [How to test RefactoringMiner](documentation/how-to.md#how-to-test-refactoringminer)
   * [How to use RefactoringMiner as a maven dependency](documentation/how-to.md#how-to-use-refactoringminer-as-a-maven-dependency)
   * [How to use RefactoringMiner as a docker image](documentation/how-to.md#how-to-use-refactoringminer-as-a-docker-image)
   * [How to use RefactoringMiner as a Chrome extension](documentation/how-to.md#how-to-use-refactoringminer-as-a-chrome-extension)
   * [How to run RefactoringMiner from the command line](documentation/how-to.md#how-to-run-refactoringminer-from-the-command-line)
      * [Refactoring detection command line options](documentation/how-to.md#refactoring-detection-command-line-options)
      * [AST diff command line options](documentation/how-to.md#ast-diff-command-line-options)
   * [Research](documentation/research.md#research)
      * [How to cite RefactoringMiner](documentation/research.md#how-to-cite-refactoringminer)
      * [Talks about RefactoringMiner](documentation/research.md#talks-about-refactoringminer)
      * [Papers using RefactoringMiner](documentation/research.md#papers-using-refactoringminer)
   * [Support for other programming languages](#support-for-other-programming-languages)
      * [Kotlin](#kotlin)
      * [Python](#python)
      * [C++](#cpp)
   * [Refactoring detection API usage guidelines](#refactoring-detection-api-usage-guidelines)
      * [With a locally cloned git repository](#with-a-locally-cloned-git-repository)
      * [With two directories containing Java source code](#with-two-directories-containing-java-source-code)
      * [With file contents as strings](#with-file-contents-as-strings)
      * [With all information fetched directly from GitHub](#with-all-information-fetched-directly-from-github)
      * [With each commit in a GitHub Pull request](#with-each-commit-in-a-github-pull-request)
      * [With a commit range](#with-a-commit-range)
   * [AST Diff API usage guidelines](#ast-diff-api-usage-guidelines)
      * [With commit of a locally cloned git repository](#with-commit-of-a-locally-cloned-git-repository)
      * [With commit fetched directly from GitHub](#with-commit-fetched-directly-from-github)
      * [With the files changed in a GitHub Pull Request](#with-the-files-changed-in-a-github-pull-request)
      * [With two directories](#with-two-directories)
      * [With commit range](#with-commit-range)
   * [Purity Checker](#purity-checker)
   * [Location information for the detected refactorings](#location-information-for-the-detected-refactorings)
   * [Statement matching information for the detected refactorings](#statement-matching-information-for-the-detected-refactorings)

# General info
RefactoringMiner is a library/API written in Java that can detect refactorings applied in the history of a Java project.
Since version 3.0, RefactoringMiner can also generate Abstract Syntax Tree (AST) diff at **commit**, **pull request** and **commit range** levels.
You can also use our tool to visualize the diffs in your browser.

## AST diff features

### Refactoring-aware tooltips
<img width="1920" height="1080" alt="RefactoringAwareTooltips" src="https://github.com/user-attachments/assets/e4e14c4c-a966-46e3-b4c7-6808a0506a9c" />
<img width="1920" height="1080" alt="RefactoringAwareTooltips2" src="https://github.com/user-attachments/assets/51731aaa-fcbc-4704-ae39-8365b93bcdf3" />

### Refactoring listing with links to diffs
<img width="1920" height="1080" alt="RefactoringAnalysis" src="https://github.com/user-attachments/assets/351d059a-1767-4032-a541-b9f6589ec35b" />

### Single Page View
All AST diffs loaded in a single page similar to GitHub
<img width="1920" height="1080" alt="SinglePageView" src="https://github.com/user-attachments/assets/09f3007b-db87-46e0-b61a-ab3076d3f1ce" />

### Embedded GitHub Code Review Comments
<img width="1920" height="1080" alt="GitHubCodeReviewComments" src="https://github.com/user-attachments/assets/03afa1de-7e68-4951-a466-eac74cfe5385" />

### Diff for code moved between different files
The diffs show also overlapping refactorings within the moved code
<img width="1920" height="1080" alt="MoveDiff" src="https://github.com/user-attachments/assets/5a6de2ed-9535-4a89-b025-b9f4f41e8a69" />
<img width="1920" height="1080" alt="MoveDiff2" src="https://github.com/user-attachments/assets/2a1bd916-7fd7-4bf9-ad3e-e319b7452fb5" />
<img width="1920" height="1080" alt="MoveDiff3" src="https://github.com/user-attachments/assets/170779c2-68f7-4ef6-aebd-67fcc0fb7e70" />

### On-demand diff generation
The user can select any pair of files (between modified, added and deleted files) and generate a diff
<img width="1920" height="1080" alt="OnDemandDiffGeneration" src="https://github.com/user-attachments/assets/7525366e-2377-4a05-9819-9cefaf5de86b" />

### Javadoc and comment reformatting
Our tool can match Javadoc and inline comments with formatting changes. You can also exclude diffs including only comment formatting changes by adding `--ignore-formatting` in the command-line arguments.
<img width="1920" height="1080" alt="CommentFormatting" src="https://github.com/user-attachments/assets/f69aabbd-fcce-49e2-a18b-d1a80b1676a8" />

# Supported Refactoring Types

<details>
<summary>From Fowler's book (40)</summary>

	1. Extract Method
	2. Inline Method
	3. Rename Method
	4. Move Method
	5. Move Attribute
	6. Pull Up Method
	7. Pull Up Attribute
	8. Push Down Method
	9. Push Down Attribute
	10. Extract Superclass
	11. Extract Interface
	12. Move Class
	13. Rename Class
	14. Extract and Move Method
	15. Rename Package
	16. Move and Rename Class
	17. Extract Class
	18. Extract Subclass
	19. Extract Variable
	20. Inline Variable
	21. Parameterize Variable
 	22. Extract Attribute
	23. Move and Rename Method
	24. Move and Inline Method
 	25. Encapsulate Attribute
	26. Parameterize Attribute
 	27. Move Package
	28. Split Package
	29. Merge Package
	30. Localize Parameter
 	31. Collapse Hierarchy
  	32. Merge Class
	33. Inline Attribute
 	34. Split Class
	35. Split Conditional
	36. Invert Condition
	37. Merge Conditional
	38. Merge Method
	39. Split Method
 	40. Move Code (between methods)
</details>

<details>
<summary>API changes (52)</summary>

	1. Rename Variable
	2. Rename Parameter
	3. Rename Attribute
	4. Move and Rename Attribute
	5. Replace Variable with Attribute
	6. Replace Attribute (with Attribute)
	7. Merge Variable
	8. Merge Parameter
	9. Merge Attribute
	10. Split Variable
	11. Split Parameter
	12. Split Attribute
	13. Change Variable Type
	14. Change Parameter Type
	15. Change Return Type
	16. Change Attribute Type
	17. Add Method Annotation
	18. Remove Method Annotation
	19. Modify Method Annotation
	20. Add Attribute Annotation
	21. Remove Attribute Annotation
	22. Modify Attribute Annotation
	23. Add Class Annotation
	24. Remove Class Annotation
	25. Modify Class Annotation
	26. Add Parameter Annotation
	27. Remove Parameter Annotation
	28. Modify Parameter Annotation
	29. Add Variable Annotation
	30. Remove Variable Annotation
	31. Modify Variable Annotation
	32. Add Parameter
	33. Remove Parameter
	34. Reorder Parameter
	35. Add Thrown Exception Type
	36. Remove Thrown Exception Type
	37. Change Thrown Exception Type
	38. Change Method Access Modifier
 	39. Change Attribute Access Modifier
	40. Replace Attribute with Variable
	41. Add Method Modifier (final, static, abstract, synchronized)
	42. Remove Method Modifier (final, static, abstract, synchronized)
	43. Add Attribute Modifier (final, static, transient, volatile)
	44. Remove Attribute Modifier (final, static, transient, volatile)
	45. Add Variable Modifier (final)
	46. Add Parameter Modifier (final)
	47. Remove Variable Modifier (final)
	48. Remove Parameter Modifier (final)
	49. Change Class Access Modifier
	50. Add Class Modifier (final, static, abstract)
	51. Remove Class Modifier (final, static, abstract)
 	52. Change Type Declaration Kind (class, interface, enum, annotation, record)
</details>

<details>
<summary>Migrations (8)</summary>

	1. Replace Loop with Pipeline
	2. Replace Anonymous with Lambda
 	3. Replace Pipeline with Loop
  	4. Merge Catch
   	5. Replace Anonymous with Class
   	6. Replace Generic With Diamond
	7. Try With Resources
	8. Replace Conditional With Ternary
</details>

<details>
<summary>Test-specific (5)</summary>

	1. Parameterize Test (JUnit 5 @ParameterizedTest with @ValueSource)
	2. Assert Throws
	3. Assert Timeout
	4. Replace Conditional with Assumption
	5. Extract Fixture
</details>

# Contributors
* [Nikolaos Tsantalis](https://github.com/tsantalis): Core + APIs
* [Dimitris Kalaitzidis](https://github.com/dkalaitz): Lang AST infrastructure for multi-language extension
* [Danilo Ferreira e Silva](https://github.com/danilofes): Git repository mining infrastructure + APIs
* [Pouria Alikhani Fard](https://github.com/pouryafard75): [AST diff](https://github.com/pouryafard75/RM-ASTDiff) + [AST diff benchmark](https://github.com/pouryafard75/DiffBenchmark)
* [Victor Veloso](https://github.com/victorgveloso/): [Test-specific refactoring detection](https://github.com/victorgveloso/RefactoringMiner)
* [Pedram Nouri](https://github.com/pedramnoori): [Refactoring purity checker](https://github.com/pedramnoori/RefactoringMiner)
* [Tayeeb Hasan](https://github.com/flozender): [CodeTracker - block tracking](https://github.com/jodavimehran/code-tracker) + [CodeTracker Chrome extension](https://github.com/flozender/CodeTracker-Extension)
* [Mehran Jodavi](https://github.com/jodavimehran): [CodeTracker - method & variable tracking](https://github.com/jodavimehran/code-tracker)
* [Sadegh Aalizadeh](https://github.com/mosaliza): [Refactoring motivation detection](https://github.com/mosaliza/RefactoringMiner)
* [Hassan Mansour](https://github.com/hassanmansour90): [Refactoring Aware Commit Review Chrome extension](https://chrome.google.com/webstore/detail/refactoring-aware-commit/lnloiaibmonmmpnfibfjjlfcddoppmgd)
* [Ameya Ketkar](https://github.com/ameyaKetkar): [Refactoring oracle 2.0 validation](https://github.com/ameyaKetkar/RMinerEvaluationTools)
* [Davood Mazinanian](https://github.com/dmazinanian): [Refactoring oracle web application](http://refactoring.encs.concordia.ca/oracle/) + Refactoring oracle 1.0 validation
* Matin Mansouri: Rename Variable detection + Refactoring oracle 1.0 validation
* Laleh M. Eshkevari: Refactoring oracle 1.0 validation

# Support for other programming languages
## Kotlin
* [KotlinRMiner](https://github.com/JetBrains-Research/kotlinRMiner) has been developed by [JetBrains Research](https://research.jetbrains.org/).
The project is led and maintained by [Zarina Kurbatova](https://github.com/onewhl).

## Python
* [PyRef](https://github.com/PyRef/PyRef) has been developed by Hassan Atwi and [Bin Lin](https://binlin.info/) from the Software Institute at USI - Università della Svizzera Italiana, Switzerland.
* [Py-RefactoringMiner](https://github.com/maldil/RefactoringMiner) has been developed by [Malinda Dilhara](https://maldil.github.io/), a Ph.D. student in the department of Computer Science at University of Colorado Boulder under the suprevision of [Danny Dig](https://dig.cs.illinois.edu/).

## Cpp
* [RefactoringMiner++](https://github.com/benzoinoo/RefactoringMinerPP) has been developed by [Benjamin Ritz](https://github.com/benzoinoo) from Graz University of Technology, Austria.

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
