Table of Contents
=================

   * [General info](#general-info)
   * [How to build RefactoringMiner](#how-to-build-refactoringminer)
   * [How to use RefactoringMiner as a maven dependency](#how-to-use-refactoringminer-as-a-maven-dependency)
   * [Research](#research)
      * [How to cite RefactoringMiner](#how-to-cite-refactoringminer)
      * [Empirical studies based on RefactoringMiner](#empirical-studies-based-on-refactoringminer)
   * [Contributors](#contributors)
   * [API usage guidelines](#api-usage-guidelines)
      * [With a locally cloned git repository](#with-a-locally-cloned-git-repository)
      * [With two directories containing Java source code](#with-two-directories-containing-java-source-code)
      * [With all information fetched directly from GitHub](#with-all-information-fetched-directly-from-github)
   * [Location information for the detected refactorings](#location-information-for-the-detected-refactorings)
   * [Statement matching information for the detected refactorings](#statement-matching-information-for-the-detected-refactorings)
   * [Running RefactoringMiner from the command line](#running-refactoringminer-from-the-command-line)

# General info
RefactoringMiner is a library/API written in Java that can detect refactorings applied in the history of a Java project.

Currently, it supports the detection of the following refactorings:

**<ins>supported by RefactoringMiner 1.0 & 2.0</ins>**

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
15. Change Package (Move, Rename, Split, Merge)

**<ins>supported by RefactoringMiner 2.0</ins>**

16. Move and Rename Class
17. Extract Class
18. Extract Subclass
19. Extract Variable
20. Inline Variable
21. Parameterize Variable
22. Rename Variable
23. Rename Parameter
24. Rename Attribute
25. Move and Rename Attribute
26. Replace Variable with Attribute
27. Replace Attribute (with Attribute)
28. Merge Variable
29. Merge Parameter
30. Merge Attribute
31. Split Variable
32. Split Parameter
33. Split Attribute
34. Change Variable Type
35. Change Parameter Type
36. Change Return Type
37. Change Attribute Type
38. Extract Attribute

# How to build RefactoringMiner

In order to build the project, run `./gradlew jar` (or `gradlew jar`, in Windows) in the project's root directory.
Alternatively, you can generate a complete distribution zip including all runtime dependencies running `./gradlew distZip`.

You can also work with the project with Eclipse IDE. First, run `./gradlew eclipse` to generate Eclipse project metadata files. Then, import it into Eclipse using the *Import Existing Project* feature.

# How to use RefactoringMiner as a maven dependency

In order to use RefactoringMiner as a maven dependency in your project, run `./gradlew install` to add `RefactoringMiner.jar` into your local mvn repository, and then add the following dependency to your project:

    <dependency>
      <groupId>org.refactoringminer</groupId>
      <artifactId>RefactoringMiner</artifactId>
      <version>1.0</version>
    </dependency>

# Research
## How to cite RefactoringMiner
If you are using RefactoringMiner in your research, please cite the following paper:

Nikolaos Tsantalis, Matin Mansouri, Laleh Eshkevari, Davood Mazinanian, and Danny Dig, "[Accurate and Efficient Refactoring Detection in Commit History](https://users.encs.concordia.ca/~nikolaos/publications/ICSE_2018.pdf)," *40th International Conference on Software Engineering* (ICSE 2018), Gothenburg, Sweden, May 27 - June 3, 2018.

    @inproceedings{Tsantalis:ICSE:2018:RefactoringMiner,
	author = {Tsantalis, Nikolaos and Mansouri, Matin and Eshkevari, Laleh M. and Mazinanian, Davood and Dig, Danny},
	title = {Accurate and Efficient Refactoring Detection in Commit History},
	booktitle = {Proceedings of the 40th International Conference on Software Engineering},
	series = {ICSE '18},
	year = {2018},
	isbn = {978-1-4503-5638-1},
	location = {Gothenburg, Sweden},
	pages = {483--494},
	numpages = {12},
	url = {http://doi.acm.org/10.1145/3180155.3180206},
	doi = {10.1145/3180155.3180206},
	acmid = {3180206},
	publisher = {ACM},
	address = {New York, NY, USA},
	keywords = {Git, Oracle, abstract syntax tree, accuracy, commit, refactoring},
    }

## Empirical studies based on RefactoringMiner
RefactoringMiner has been used in the following studies:
1. Danilo Silva, Nikolaos Tsantalis, and Marco Tulio Valente, "[Why We Refactor? Confessions of GitHub Contributors](https://doi.org/10.1145/2950290.2950305)," *24th ACM SIGSOFT International Symposium on the Foundations of Software Engineering* (FSE 2016), Seattle, WA, USA, November 13-18, 2016.
2. Diego Cedrim, Alessandro Garcia, Melina Mongiovi, Rohit Gheyi, Leonardo Sousa, Rafael de Mello, Baldoino Fonseca, Márcio Ribeiro, and Alexander Chávez, "[Understanding the impact of refactoring on smells: a longitudinal study of 23 software projects](https://doi.org/10.1145/3106237.3106259)," *11th Joint Meeting on Foundations of Software Engineering* (ESEC/FSE 2017), Paderborn, Germany, September 4-8, 2017.
3. Alexander Chávez, Isabella Ferreira, Eduardo Fernandes, Diego Cedrim, and Alessandro Garcia, "[How does refactoring affect internal quality attributes?: A multi-project study](https://doi.org/10.1145/3131151.3131171)," *31st Brazilian Symposium on Software Engineering* (SBES 2017), Fortaleza, CE, Brazil, September 20-22, 2017.
4. Navdeep Singh, and Paramvir Singh, "[How Do Code Refactoring Activities Impact Software Developers' Sentiments? - An Empirical Investigation Into GitHub Commits](https://doi.org/10.1109/APSEC.2017.79)," *24th Asia-Pacific Software Engineering Conference* (APSEC 2017), Nanjing, Jiangsu, China, December 4-8, 2017.
5. Mehran Mahmoudi, and Sarah Nadi, "[The Android Update Problem: An Empirical Study](https://doi.org/10.1145/3196398.3196434)," *15th International Conference on Mining Software Repositories* (MSR 2018), Gothenburg, Sweden, May 28-29, 2018.
6. Anthony Peruma, Mohamed Wiem Mkaouer, Michael J. Decker, and Christian D. Newman, "[An empirical investigation of how and why developers rename identifiers](https://doi.org/10.1145/3242163.3242169)," *2nd International Workshop on Refactoring* (IWoR 2018), Montpellier, France, September 4, 2018.
7. Patanamon Thongtanunam, Weiyi Shang, and Ahmed E. Hassan, "[Will this clone be short-lived? Towards a better understanding of the characteristics of short-lived clones](https://doi.org/10.1007/s10664-018-9645-2)," Empirical Software Engineering, pp. 1-36, 2018.
8. Isabella Ferreira, Eduardo Fernandes, Diego Cedrim, Anderson Uchôa, Ana Carla Bibiano, Alessandro Garcia, João Lucas Correia, Filipe Santos, Gabriel Nunes, Caio Barbosa, Baldoino Fonseca, and Rafael de Mello, "[The buggy side of code refactoring: understanding the relationship between refactorings and bugs](https://doi.org/10.1145/3183440.3195030)," *40th International Conference on Software Engineering: Companion Proceedings* (ICSE 2018), Gothenburg, Sweden, May 27-June 3, 2018.
9. Matheus Paixao, "[Software Restructuring: Understanding Longitudinal Architectural Changes and Refactoring](http://discovery.ucl.ac.uk/10060511/)," Ph.D. thesis, Computer Science Department, University College London, July 2018.
10. Mehran Mahmoudi, Sarah Nadi, and Nikolaos Tsantalis, "[Are Refactorings to Blame? An Empirical Study of Refactorings in Merge Conflicts](https://doi.org/10.1109/SANER.2019.8668012)," *26th IEEE International Conference on Software Analysis, Evolution and Reengineering* (SANER 2019), Hangzhou, China, February 24-27, 2019.
11. Bin Lin, Csaba Nagy, Gabriele Bavota and Michele Lanza, "[On the Impact of Refactoring Operations on Code Naturalness](https://doi.org/10.1109/SANER.2019.8667992)," *26th IEEE International Conference on Software Analysis, Evolution and Reengineering* (SANER 2019), Hangzhou, China, February 24-27, 2019.
12. Sarah Fakhoury, Devjeet Roy, Sk. Adnan Hassan, and Venera Arnaoudova, "[Improving Source Code Readability: Theory and Practice](https://dl.acm.org/citation.cfm?id=3339080)," *27th IEEE/ACM International Conference on Program Comprehension* (ICPC 2019), Montreal, QC, Canada, May 25-26, 2019.
13. Carmine Vassallo, Giovanni Grano, Fabio Palomba, Harald C. Gall, and Alberto Bacchelli, "[A large-scale empirical exploration on refactoring activities in open source software projects](https://doi.org/10.1016/j.scico.2019.05.002)," *Science of Computer Programming*, 2019.
14. Eman Abdullah AlOmar, Mohamed Wiem Mkaouer, and Ali Ouni, "[Can refactoring be self-affirmed?: An exploratory study on how developers document their refactoring activities in commit messages](https://dl.acm.org/citation.cfm?id=3340647)," *3rd International Workshop on Refactoring* (IWOR 2019), Montreal, QC, Canada, May 28, 2019.
15. Ana Carla Bibiano, Eduardo Fernandes, Daniel Oliveira, Alessandro Garcia, Marcos Kalinowski, Baldoino Fonseca, Roberto Oliveira, Anderson Oliveira, and Diego Cedrim, "A Quantitative Study on Characteristics and Effect of Batch Refactoring on Code Smells," *13th ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2019), Porto de Galinhas, Brazil, September 16-20, 2019.
16. Eman Abdullah AlOmar, Mohamed Wiem Mkaouer, Ali Ouni, and Marouane Kessentini, "Do Design Metrics Capture Developers Perception of Quality? An Empirical Study on Self-Affirmed Refactoring Activities," *13th ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2019), Porto de Galinhas, Brazil, September 16-20, 2019.
17. Valentina Lenarduzzi, Nyyti Saarimäki, and Davide Taibi, "The Technical Debt Dataset," *15th International Conference on Predictive Models and Data Analytics in Software Engineering* (PROMISE 2019), Porto de Galinhas, Brazil, September 18, 2019.
18. Anthony Peruma, "[A preliminary study of Android refactorings](http://dl.acm.org/citation.cfm?id=3340730.3340760)," *6th International Conference on Mobile Software Engineering and Systems* (MOBILESoft 2019), Montreal, Quebec, Canada, May 25-26, 2019.
19. Anthony Peruma, Mohamed Wiem Mkaouer, Michael J. Decker, and Christian D. Newman, "Contextualizing Rename Decisions using
Refactorings and Commit Messages," *19th IEEE International Working Conference on Source Code Analysis and Manipulation* (SCAM 2019), Cleveland, OH, USA, September 30-October 1, 2019.
20. Willian Oizumi, Leonardo Da Silva Sousa, Anderson Oliveira, Luiz Matheus Alencar, Alessandro Garcia, Thelma E. Colanzi and Roberto Oliveira, "On the density and diversity of degradation symptoms in refactored classes: A multi-case study," *30th International Symposium on Software Reliability Engineering* (ISSRE 2019), Berlin, Germany, October 28-31, 2019.
21. Marcos César de Oliveira, Davi Freitas, Rodrigo Bonifácio, Gustavo Pinto, and David Lo, "[Finding Needles in a Haystack: Leveraging Co-change Dependencies to Recommend Refactorings](https://doi.org/10.1016/j.jss.2019.110420)," Journal of Systems and Software, 2019.

# Contributors
The code in package **gr.uom.java.xmi.*** has been developed by [Nikolaos Tsantalis](https://github.com/tsantalis).

The code in package **org.refactoringminer.*** has been developed by [Danilo Ferreira e Silva](https://github.com/danilofes).

# API usage guidelines
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

It is possible to analyze a specifc commit using `detectAtCommit` instead of `detectAll`. The commit
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
You can get the churn of a specific commit using `churnAtCommit` as follows:
```java
Churn churn = miner.churnAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398", handler);
```
## With two directories containing Java source code

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
## With all information fetched directly from GitHub

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
If you want to analyze all commits of a pull request, you can use the following code snippet:

```java
GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
miner.detectAtPullRequest("https://github.com/apache/drill.git", 1807, new RefactoringHandler() {
  @Override
  public void handle(String commitId, List<Refactoring> refactorings) {
    System.out.println("Refactorings at " + commitId);
    for (Refactoring ref : refactorings) {
      System.out.println(ref.toString());
    }
  }
}, 10);
```
Please make sure to provide valid GitHub credentials in the `github-credentials.properties` file.

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

# Running RefactoringMiner from the command line

When you build a distributable application with `./gradlew distZip`, you can run Refactoring Miner as a command line application. Extract the file under `build/distribution/RefactoringMiner.zip` in the desired location, and cd into the `bin` folder (or include it in your path). Then, run `RefactoringMiner -h` to show its usage:

    > RefactoringMiner -h

	-h								Show tips
	-a <git-repo-folder> <branch>					Detect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.
	-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>	Detect refactorings Between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>
	-bt <git-repo-folder> <start-tag> <end-tag>			Detect refactorings Between <start-tag> and <end-tag> for project <git-repo-folder>
	-c <git-repo-folder> <commit-sha1>				Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>
	-gc <git-URL> <commit-sha1> <timeout>				Detect refactorings at specified commit <commit-sha1> for project <git-URL> within the given <timeout> in seconds. All required information is obtained directly from GitHub using the credentials in github-credentials.properties
	-gp <git-URL> <pull-request> <timeout>				Detect refactorings at specified pull request <pull-request> for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. All required information is obtained directly from GitHub using the credentials in github-credentials.properties
	
For example, suppose that you run:

    > git clone https://github.com/danilofes/refactoring-toy-example.git refactoring-toy-example
    > ./RefactoringMiner -c refactoring-toy-example 36287f7c3b09eff78395267a3ac0d7da067863fd

The output will be:

    4 refactorings found in commit 36287f7c3b09eff78395267a3ac0d7da067863fd:
      Pull Up Attribute     private age : int from class org.animals.Labrador to class org.animals.Dog
      Pull Up Attribute     private age : int from class org.animals.Poodle to class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog
      Pull Up Method        public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog

When you run Refactoring with `-a`, `-bc`, `-bt`, after all commits are analyzed, a result `csv` file which use semicolon `;` as delimiter will be generated in the repository directory.

If you don't want to clone locally the repository, run:

    > ./RefactoringMiner -gc https://github.com/danilofes/refactoring-toy-example.git 36287f7c3b09eff78395267a3ac0d7da067863fd 10

and you will get the output in JSON format:

    {
	"commits": [{
		"repository": "https://github.com/danilofes/refactoring-toy-example.git",
		"sha1": "36287f7c3b09eff78395267a3ac0d7da067863fd",
		"url": "https://github.com/danilofes/refactoring-toy-example/commit/36287f7c3b09eff78395267a3ac0d7da067863fd",
		"refactorings": [{
				"type": "Pull Up Attribute",
				"description": "Pull Up Attribute private age : int from class org.animals.Labrador to class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Labrador.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "original attribute declaration",
					"codeElement": "age : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "pulled up attribute declaration",
					"codeElement": "age : int"
				}]
			},
			{
				"type": "Pull Up Attribute",
				"description": "Pull Up Attribute private age : int from class org.animals.Poodle to class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Poodle.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "original attribute declaration",
					"codeElement": "age : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 5,
					"endLine": 5,
					"startColumn": 14,
					"endColumn": 21,
					"codeElementType": "FIELD_DECLARATION",
					"description": "pulled up attribute declaration",
					"codeElement": "age : int"
				}]
			},
			{
				"type": "Pull Up Method",
				"description": "Pull Up Method public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Labrador.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "original method declaration",
					"codeElement": "public getAge() : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "pulled up method declaration",
					"codeElement": "public getAge() : int"
				}]
			},
			{
				"type": "Pull Up Method",
				"description": "Pull Up Method public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog",
				"leftSideLocations": [{
					"filePath": "src/org/animals/Poodle.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "original method declaration",
					"codeElement": "public getAge() : int"
				}],
				"rightSideLocations": [{
					"filePath": "src/org/animals/Dog.java",
					"startLine": 7,
					"endLine": 9,
					"startColumn": 2,
					"endColumn": 3,
					"codeElementType": "METHOD_DECLARATION",
					"description": "pulled up method declaration",
					"codeElement": "public getAge() : int"
				}]
			}
		]
	}]
	}

If you want to get the refactorings in each commit of a pull request, run:

    > ./RefactoringMiner -gp https://github.com/apache/drill.git 1807 10

and you will get the following output:

    12 refactorings found in commit 21fc7b6d3e6064ff2c28bb1b9920487e7cf995ba: 
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.ColumnDefn moved to org.apache.drill.exec.store.log.LogBatchReader.ColumnDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.VarCharDefn moved to org.apache.drill.exec.store.log.LogBatchReader.VarCharDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.BigIntDefn moved to org.apache.drill.exec.store.log.LogBatchReader.BigIntDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.SmallIntDefn moved to org.apache.drill.exec.store.log.LogBatchReader.SmallIntDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.IntDefn moved to org.apache.drill.exec.store.log.LogBatchReader.IntDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.Float4Defn moved to org.apache.drill.exec.store.log.LogBatchReader.Float4Defn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.DoubleDefn moved to org.apache.drill.exec.store.log.LogBatchReader.DoubleDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.DateDefn moved to org.apache.drill.exec.store.log.LogBatchReader.DateDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.TimeDefn moved to org.apache.drill.exec.store.log.LogBatchReader.TimeDefn
      Move Class	org.apache.drill.exec.store.log.LogRecordReader.TimeStampDefn moved to org.apache.drill.exec.store.log.LogBatchReader.TimeStampDefn
      Rename Parameter	patternIndex : int to fieldIndex : int in method public getDateFormat(fieldIndex int) : String in class org.apache.drill.exec.store.log.LogFormatConfig
      Move Attribute	private formatConfig : LogFormatConfig from class org.apache.drill.exec.store.log.LogFormatPlugin to class org.apache.drill.exec.store.log.LogBatchReader
    4 refactorings found in commit 2041aca8887882b6f33a1a4366f44b5f2dac681c: 
      Rename Method	public testExplicit() : void renamed to public testExplicitProject() : void in class org.apache.drill.exec.store.log.TestLogReader
      Rename Method	public testMissing() : void renamed to public testMissingColumns() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	batches : List<QueryDataBatch> to iter : QueryRowSetIterator in method public testWildcardLargeFile() : void in class org.apache.drill.exec.store.log.TestLogReader
      Rename Variable	batches : List<QueryDataBatch> to iter : QueryRowSetIterator in method public testWildcardLargeFile() : void in class org.apache.drill.exec.store.log.TestLogReader
    No refactorings found in commit 0d521265e79ac05b33480dd3adb2a078ca28e54b
    2 refactorings found in commit 02fb0e9945353e187f5eaa8bea6a5763f3f9b9fb: 
      Change Attribute Type	logger : org.slf4j.Logger to logger : Logger in class org.apache.drill.exec.store.log.LogBatchReader
      Change Attribute Type	logger : org.slf4j.Logger to logger : Logger in class org.apache.drill.exec.store.log.LogFormatPlugin
    No refactorings found in commit ab512953b0e097b02fb25e33529bba0e27651fb7
    4 refactorings found in commit a07936715378f41d7e375be53218d3dfc0a8e45e: 
      Extract Method	private buildTable(dir File, ws String, tableName String, fileName String, resource String) : String extracted from public testProvidedSchema() : void in class org.apache.drill.exec.store.log.TestLogReader
      Rename Attribute	testDir : File to schemaAndConfigDir : File in class org.apache.drill.exec.store.log.TestLogReader
      Rename Method	private defineOutputSchema(capturingGroups int) : TupleMetadata renamed to private defineOutputSchemaFromConfig(capturingGroups int) : TupleMetadata in class org.apache.drill.exec.store.log.LogFormatPlugin
      Extract Variable	regex : String in method private setupPattern(providedSchema TupleMetadata) : Pattern from class org.apache.drill.exec.store.log.LogFormatPlugin
    No refactorings found in commit 6320a77caca59b886a50c5ef47cbb1d6461d98fb
    No refactorings found in commit 6a712ddf7fe4693594805f64692c2677239fbe08
    No refactorings found in commit a14ebf31d6c924cbe877e1f1f672e211e3207e89
    No refactorings found in commit e816deba8dfbd937f89c216cc8c74aa6adf01aed
    14 refactorings found in commit 539bd0edd8348d03df6d17ae4ff2387c10dd10e9: 
      Inline Method	private defineReaderSchema(outputSchema TupleMetadata) : TupleMetadata inlined to protected frameworkBuilder(options OptionManager, scan EasySubScan) : FileScanBuilder in class org.apache.drill.exec.store.log.LogFormatPlugin
      Rename Variable	capturingGroups : int to groupCount : int in method protected frameworkBuilder(options OptionManager, scan EasySubScan) : FileScanBuilder in class org.apache.drill.exec.store.log.LogFormatPlugin
      Extract Method	public hasSchema() : boolean extracted from public getFieldNames() : List<String> in class org.apache.drill.exec.store.log.LogFormatConfig
      Extract Class	org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig from class org.apache.drill.exec.store.log.LogBatchReader
      Move Attribute	public COLUMNS_COL : String from class org.apache.drill.exec.physical.impl.scan.columns.ColumnsArrayManager to class org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework
      Move Attribute	protected plugin : LogFormatPlugin from class org.apache.drill.exec.store.log.LogFormatPlugin.LogReaderFactory to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected pattern : Pattern from class org.apache.drill.exec.store.log.LogFormatPlugin.LogReaderFactory to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected schema : TupleMetadata from class org.apache.drill.exec.store.log.LogFormatPlugin.LogReaderFactory to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected maxErrors : int from class org.apache.drill.exec.store.log.LogFormatPlugin.LogReaderFactory to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected pattern : Pattern from class org.apache.drill.exec.store.log.LogBatchReader to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected schema : TupleMetadata from class org.apache.drill.exec.store.log.LogBatchReader to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Move Attribute	protected maxErrors : int from class org.apache.drill.exec.store.log.LogBatchReader to class org.apache.drill.exec.store.log.LogBatchReader.LogReaderConfig
      Extract And Move Method	public allowOtherCols(flag boolean) : void extracted from protected frameworkBuilder(options OptionManager, scan EasySubScan) : FileScanBuilder in class org.apache.drill.exec.store.log.LogFormatPlugin & moved to class org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework.ColumnsScanBuilder
      Move Method	private loadVectors(m Matcher, rowWriter RowSetLoader) : void from class org.apache.drill.exec.store.log.LogBatchReader to public loadVectors(m Matcher) : void from class org.apache.drill.exec.store.log.LogBatchReader.ScalarGroupWriter
    9 refactorings found in commit b1a5446174485c55a162771b1e804a0587eef361: 
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testStarQueryNoSchema() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testSomeFieldsQueryNoSchema() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testProvidedSchema() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testSchemaOnlyNoCols() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testSchemaOnlyWithCols() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testSchemaOnlyWithMissingCols() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testTableFunction() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testTableFunctionWithSchema() : void in class org.apache.drill.exec.store.log.TestLogReader
      Change Variable Type	expectedSchema : BatchSchema to expectedSchema : TupleMetadata in method public testTableFunctionWithConfigAndSchema() : void in class org.apache.drill.exec.store.log.TestLogReader

      
