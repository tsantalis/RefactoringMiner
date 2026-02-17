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
   * [Refactoring detection API usage guidelines](documentation/api.md#refactoring-detection-api-usage-guidelines)
      * [With a locally cloned git repository](documentation/api.md#with-a-locally-cloned-git-repository)
      * [With two directories containing Java source code](documentation/api.md#with-two-directories-containing-java-source-code)
      * [With file contents as strings](documentation/api.md#with-file-contents-as-strings)
      * [With all information fetched directly from GitHub](documentation/api.md#with-all-information-fetched-directly-from-github)
      * [With each commit in a GitHub Pull request](documentation/api.md#with-each-commit-in-a-github-pull-request)
      * [With a commit range](documentation/api.md#with-a-commit-range)
   * [AST Diff API usage guidelines](documentation/api.md#ast-diff-api-usage-guidelines)
      * [With commit of a locally cloned git repository](documentation/api.md#with-commit-of-a-locally-cloned-git-repository)
      * [With commit fetched directly from GitHub](documentation/api.md#with-commit-fetched-directly-from-github)
      * [With the files changed in a GitHub Pull Request](documentation/api.md#with-the-files-changed-in-a-github-pull-request)
      * [With two directories](documentation/api.md#with-two-directories)
      * [With commit range](documentation/api.md#with-commit-range)
   * [Purity Checker](documentation/purity.md#purity-checker)
   * [Location information for the detected refactorings](documentation/api.md#location-information-for-the-detected-refactorings)
   * [Statement matching information for the detected refactorings](documentation/api.md#statement-matching-information-for-the-detected-refactorings)

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