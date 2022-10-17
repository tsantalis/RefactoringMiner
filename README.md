Table of Contents
=================

   * [General info](#general-info)
   * [Current precision and recall](#current-precision-and-recall)
   * [How to build RefactoringMiner](#how-to-build-refactoringminer)
   * [How to use RefactoringMiner as a maven dependency](#how-to-use-refactoringminer-as-a-maven-dependency)
   * [Chrome extension](#chrome-extension)
   * [Research](#research)
      * [How to cite RefactoringMiner](#how-to-cite-refactoringminer)
      * [Talks about RefactoringMiner](#talks-about-refactoringminer)
      * [Papers using RefactoringMiner](#papers-using-refactoringminer)
   * [Support for other programming languages](#support-for-other-programming-languages)
      * [Kotlin](#kotlin)
      * [Python](#python)
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

**<ins>supported by RefactoringMiner 1.0 and newer versions</ins>**

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
15. Rename Package ~~Change Package (Move, Rename, Split, Merge)~~

**<ins>supported by RefactoringMiner 2.0 and newer versions</ins>**

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
39. Move and Rename Method
40. Move and Inline Method

**<ins>supported by RefactoringMiner 2.1 and newer versions</ins>**

41. Add Method Annotation
42. Remove Method Annotation
43. Modify Method Annotation
44. Add Attribute Annotation
45. Remove Attribute Annotation
46. Modify Attribute Annotation
47. Add Class Annotation
48. Remove Class Annotation
49. Modify Class Annotation
50. Add Parameter Annotation
51. Remove Parameter Annotation
52. Modify Parameter Annotation
53. Add Variable Annotation
54. Remove Variable Annotation
55. Modify Variable Annotation
56. Add Parameter
57. Remove Parameter
58. Reorder Parameter
59. Add Thrown Exception Type
60. Remove Thrown Exception Type
61. Change Thrown Exception Type
62. Change Method Access Modifier

**<ins>supported by RefactoringMiner 2.2 and newer versions</ins>**

63. Change Attribute Access Modifier
64. Encapsulate Attribute
65. Parameterize Attribute
66. Replace Attribute with Variable
67. Add Method Modifier (`final`, `static`, `abstract`, `synchronized`)
68. Remove Method Modifier (`final`, `static`, `abstract`, `synchronized`)
69. Add Attribute Modifier (`final`, `static`, `transient`, `volatile`)
70. Remove Attribute Modifier (`final`, `static`, `transient`, `volatile`)
71. Add Variable Modifier (`final`)
72. Add Parameter Modifier (`final`)
73. Remove Variable Modifier (`final`)
74. Remove Parameter Modifier (`final`)
75. Change Class Access Modifier
76. Add Class Modifier (`final`, `static`, `abstract`)
77. Remove Class Modifier (`final`, `static`, `abstract`)
78. Move Package
79. Split Package
80. Merge Package
81. Localize Parameter
82. Change Type Declaration Kind (`class`, `interface`, `enum`)
83. Collapse Hierarchy
84. Replace Loop with Pipeline
85. Replace Anonymous with Lambda

**<ins>supported by RefactoringMiner 2.3  and newer versions</ins>**

86. Merge Class
87. Inline Attribute
88. Replace Pipeline with Loop

**<ins>supported by RefactoringMiner 2.3.2</ins>**

89. Split Class
90. Split Conditional

# Current precision and recall
As of **October 17, 2022** the precision and recall of the tool on an oracle consisting of **540 commits** from **186 open-source projects** is:

| Refactoring Type | TP | FP | FN | Precision | Recall |
|:-----------------------|-----------:|--------:|--------:|--------:|--------:|
|**Total**|11330  | 23  | 288  | 0.998  | 0.975|
|Extract Method|959  |  1  | 31  | 0.999  | 0.969|
|Rename Class|53  |  0  |  2  | 1.000  | 0.964|
|Move Attribute|242  |  4  | 10  | 0.984  | 0.960|
|Move And Rename Attribute|12  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute|10  |  0  |  0  | 1.000  | 1.000|
|Rename Method|350  |  4  | 32  | 0.989  | 0.916|
|Inline Method|109  |  0  |  2  | 1.000  | 0.982|
|Move Method|352  |  3  |  9  | 0.992  | 0.975|
|Move And Rename Method|115  |  0  |  6  | 1.000  | 0.950|
|Pull Up Method|290  |  0  |  6  | 1.000  | 0.980|
|Move Class|1093  |  0  |  4  | 1.000  | 0.996|
|Move And Rename Class|34  |  0  |  1  | 1.000  | 0.971|
|Move Source Folder| 3  |  0  |  0  | 1.000  | 1.000|
|Pull Up Attribute|128  |  0  |  1  | 1.000  | 0.992|
|Push Down Attribute|33  |  0  |  0  | 1.000  | 1.000|
|Push Down Method|43  |  0  |  1  | 1.000  | 0.977|
|Extract Interface|22  |  0  |  0  | 1.000  | 1.000|
|Extract Superclass|73  |  0  |  0  | 1.000  | 1.000|
|Extract Subclass| 4  |  0  |  0  | 1.000  | 1.000|
|Extract Class|96  |  0  |  0  | 1.000  | 1.000|
|Extract And Move Method|99  |  0  | 69  | 1.000  | 0.589|
|Move And Inline Method|16  |  0  |  4  | 1.000  | 0.800|
|Rename Package|16  |  0  |  0  | 1.000  | 1.000|
|Move Package|10  |  0  |  0  | 1.000  | 1.000|
|Extract Variable|204  |  0  |  0  | 1.000  | 1.000|
|Extract Attribute|16  |  0  |  0  | 1.000  | 1.000|
|Inline Variable|75  |  0  |  0  | 1.000  | 1.000|
|Inline Attribute| 7  |  0  |  0  | 1.000  | 1.000|
|Rename Variable|296  |  3  | 13  | 0.990  | 0.958|
|Rename Parameter|471  |  2  | 28  | 0.996  | 0.944|
|Rename Attribute|128  |  0  | 16  | 1.000  | 0.889|
|Merge Variable| 5  |  0  |  0  | 1.000  | 1.000|
|Merge Parameter|28  |  0  |  0  | 1.000  | 1.000|
|Merge Attribute| 5  |  0  |  0  | 1.000  | 1.000|
|Split Variable| 1  |  0  |  0  | 1.000  | 1.000|
|Split Parameter| 8  |  0  |  0  | 1.000  | 1.000|
|Split Attribute| 2  |  0  |  0  | 1.000  | 1.000|
|Replace Variable With Attribute|20  |  0  |  0  | 1.000  | 1.000|
|Parameterize Variable|72  |  0  |  0  | 1.000  | 1.000|
|Localize Parameter|20  |  0  |  0  | 1.000  | 1.000|
|Parameterize Attribute|23  |  0  |  0  | 1.000  | 1.000|
|Change Return Type|419  |  0  | 12  | 1.000  | 0.972|
|Change Variable Type|763  |  2  | 11  | 0.997  | 0.986|
|Change Parameter Type|629  |  1  | 16  | 0.998  | 0.975|
|Change Attribute Type|223  |  0  |  8  | 1.000  | 0.965|
|Add Method Annotation|327  |  0  |  4  | 1.000  | 0.988|
|Remove Method Annotation|99  |  0  |  0  | 1.000  | 1.000|
|Modify Method Annotation|29  |  0  |  0  | 1.000  | 1.000|
|Add Attribute Annotation|62  |  0  |  1  | 1.000  | 0.984|
|Remove Attribute Annotation|18  |  0  |  0  | 1.000  | 1.000|
|Modify Attribute Annotation| 7  |  0  |  0  | 1.000  | 1.000|
|Add Class Annotation|52  |  0  |  0  | 1.000  | 1.000|
|Remove Class Annotation|20  |  0  |  0  | 1.000  | 1.000|
|Modify Class Annotation|32  |  0  |  0  | 1.000  | 1.000|
|Add Parameter Annotation|32  |  0  |  0  | 1.000  | 1.000|
|Remove Parameter Annotation| 3  |  0  |  0  | 1.000  | 1.000|
|Modify Parameter Annotation| 2  |  0  |  0  | 1.000  | 1.000|
|Add Parameter|937  |  2  |  1  | 0.998  | 0.999|
|Remove Parameter|334  |  0  |  0  | 1.000  | 1.000|
|Reorder Parameter| 9  |  0  |  0  | 1.000  | 1.000|
|Add Variable Annotation| 1  |  0  |  0  | 1.000  | 1.000|
|Remove Variable Annotation| 3  |  0  |  0  | 1.000  | 1.000|
|Add Thrown Exception Type|39  |  0  |  0  | 1.000  | 1.000|
|Remove Thrown Exception Type|246  |  0  |  0  | 1.000  | 1.000|
|Change Thrown Exception Type| 9  |  0  |  0  | 1.000  | 1.000|
|Change Method Access Modifier|319  |  0  |  0  | 1.000  | 1.000|
|Change Attribute Access Modifier|220  |  0  |  0  | 1.000  | 1.000|
|Encapsulate Attribute|48  |  0  |  0  | 1.000  | 1.000|
|Add Method Modifier|79  |  0  |  0  | 1.000  | 1.000|
|Remove Method Modifier|100  |  0  |  0  | 1.000  | 1.000|
|Add Attribute Modifier|134  |  0  |  0  | 1.000  | 1.000|
|Remove Attribute Modifier|141  |  1  |  0  | 0.993  | 1.000|
|Add Variable Modifier|128  |  0  |  0  | 1.000  | 1.000|
|Remove Variable Modifier|57  |  0  |  0  | 1.000  | 1.000|
|Change Class Access Modifier|77  |  0  |  0  | 1.000  | 1.000|
|Add Class Modifier|34  |  0  |  0  | 1.000  | 1.000|
|Remove Class Modifier|44  |  0  |  0  | 1.000  | 1.000|
|Split Package| 4  |  0  |  0  | 1.000  | 1.000|
|Merge Package| 2  |  0  |  0  | 1.000  | 1.000|
|Change Type Declaration Kind| 6  |  0  |  0  | 1.000  | 1.000|
|Collapse Hierarchy| 1  |  0  |  0  | 1.000  | 1.000|
|Replace Loop With Pipeline|35  |  0  |  0  | 1.000  | 1.000|
|Replace Pipeline With Loop| 2  |  0  |  0  | 1.000  | 1.000|
|Replace Anonymous With Lambda|45  |  0  |  0  | 1.000  | 1.000|
|Merge Class| 6  |  0  |  0  | 1.000  | 1.000|
|Split Class| 3  |  0  |  0  | 1.000  | 1.000|
|Split Conditional| 7  |  0  |  0  | 1.000  | 1.000|

# How to build RefactoringMiner

In order to build the project, run `./gradlew jar` (or `gradlew jar`, in Windows) in the project's root directory.
Alternatively, you can generate a complete distribution zip including all runtime dependencies running `./gradlew distZip`.

You can also work with the project with Eclipse IDE. First, run `./gradlew eclipse` to generate Eclipse project metadata files. Then, import it into Eclipse using the *Import Existing Project* feature.

# How to use RefactoringMiner as a maven dependency
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.tsantalis/refactoring-miner/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tsantalis/refactoring-miner)

Since version 2.0, RefactoringMiner is available in the [Maven Central Repository](https://mvnrepository.com/artifact/com.github.tsantalis/refactoring-miner).
In order to use RefactoringMiner as a maven dependency in your project, add the following snippet to your project's build configuration file:

    <dependency>
      <groupId>com.github.tsantalis</groupId>
      <artifactId>refactoring-miner</artifactId>
      <version>2.3.2</version>
    </dependency>

# Chrome Extension
If you want to get refactoring information when inspecting a commit on GitHub,
you can install our [Refactoring Aware Commit Review](https://chrome.google.com/webstore/detail/refactoring-aware-commit/lnloiaibmonmmpnfibfjjlfcddoppmgd) Chrome Extension.

The Chrome extension can detect refactorings for **public** projects and commits matching the following URL patterns:
* `https://github.com/user/project/commit/id`
* `https://github.com/user/project/pull/id/commits/id`

![Refactoring Aware Commit Review Chrome Extension](https://user-images.githubusercontent.com/1483516/80326324-62fab400-8806-11ea-9595-4906018b831a.png)

# Research
## How to cite RefactoringMiner
If you are using RefactoringMiner in your research, please cite the following papers:

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

Nikolaos Tsantalis, Ameya Ketkar, and Danny Dig, "[RefactoringMiner 2.0](https://users.encs.concordia.ca/~nikolaos/publications/TSE_2020.pdf)," *IEEE Transactions on Software Engineering*, vol. 48, no. 3, pp. 930-950, March 2022.

    @article{Tsantalis:TSE:2020:RefactoringMiner2.0,
	author={Tsantalis, Nikolaos and Ketkar, Ameya and Dig, Danny},
	title={RefactoringMiner 2.0},
	journal={IEEE Transactions on Software Engineering},
	year={2022},
	volume={48},
	number={3},
	pages={930-950},
	doi={10.1109/TSE.2020.3007722}
    }

## Talks about RefactoringMiner
**[Keynote at the Fifth International Workshop on Refactoring (IWoR 2021)](https://iwor.github.io/iwor2021/keynote.html)**

[![2021-11-14 22_16_25-Greenshot](https://user-images.githubusercontent.com/1483516/142782871-7f7545d3-3fe7-4d0c-8860-db892757a152.png)](https://www.youtube.com/watch?v=CXO32d1ih3E)


## Papers using RefactoringMiner
RefactoringMiner has been used in the following studies:
1. Danilo Silva, Nikolaos Tsantalis, and Marco Tulio Valente, "[Why We Refactor? Confessions of GitHub Contributors](https://doi.org/10.1145/2950290.2950305)," *24th ACM SIGSOFT International Symposium on the Foundations of Software Engineering* (FSE 2016), Seattle, WA, USA, November 13-18, 2016.
2. Davood Mazinanian, Ameya Ketkar, Nikolaos Tsantalis, and Danny Dig, "[Understanding the use of lambda expressions in Java](https://doi.org/10.1145/3133909)",  *Proceedings of the ACM on Programming Languages*, vol. 1, issue OOPSLA, Article 85, 31 pages, October 2017.
3. Diego Cedrim, Alessandro Garcia, Melina Mongiovi, Rohit Gheyi, Leonardo Sousa, Rafael de Mello, Baldoino Fonseca, Márcio Ribeiro, and Alexander Chávez, "[Understanding the impact of refactoring on smells: a longitudinal study of 23 software projects](https://doi.org/10.1145/3106237.3106259)," *11th Joint Meeting on Foundations of Software Engineering* (ESEC/FSE 2017), Paderborn, Germany, September 4-8, 2017.
4. Alexander Chávez, Isabella Ferreira, Eduardo Fernandes, Diego Cedrim, and Alessandro Garcia, "[How does refactoring affect internal quality attributes?: A multi-project study](https://doi.org/10.1145/3131151.3131171)," *31st Brazilian Symposium on Software Engineering* (SBES 2017), Fortaleza, CE, Brazil, September 20-22, 2017.
5. Navdeep Singh, and Paramvir Singh, "[How Do Code Refactoring Activities Impact Software Developers' Sentiments? - An Empirical Investigation Into GitHub Commits](https://doi.org/10.1109/APSEC.2017.79)," *24th Asia-Pacific Software Engineering Conference* (APSEC 2017), Nanjing, Jiangsu, China, December 4-8, 2017.
6. Mehran Mahmoudi, and Sarah Nadi, "[The Android Update Problem: An Empirical Study](https://doi.org/10.1145/3196398.3196434)," *15th International Conference on Mining Software Repositories* (MSR 2018), Gothenburg, Sweden, May 28-29, 2018.
7. Anthony Peruma, Mohamed Wiem Mkaouer, Michael J. Decker, and Christian D. Newman, "[An empirical investigation of how and why developers rename identifiers](https://doi.org/10.1145/3242163.3242169)," *2nd International Workshop on Refactoring* (IWoR 2018), Montpellier, France, September 4, 2018.
8. Patanamon Thongtanunam, Weiyi Shang, and Ahmed E. Hassan, "[Will this clone be short-lived? Towards a better understanding of the characteristics of short-lived clones](https://doi.org/10.1007/s10664-018-9645-2)," *Empirical Software Engineering*, Volume 24, Issue 2, pp. 937–972, April 2019.
9. Isabella Ferreira, Eduardo Fernandes, Diego Cedrim, Anderson Uchôa, Ana Carla Bibiano, Alessandro Garcia, João Lucas Correia, Filipe Santos, Gabriel Nunes, Caio Barbosa, Baldoino Fonseca, and Rafael de Mello, "[The buggy side of code refactoring: understanding the relationship between refactorings and bugs](https://doi.org/10.1145/3183440.3195030)," *40th International Conference on Software Engineering: Companion Proceedings* (ICSE 2018), Gothenburg, Sweden, May 27-June 3, 2018.
10. Matheus Paixao, "[Software Restructuring: Understanding Longitudinal Architectural Changes and Refactoring](http://discovery.ucl.ac.uk/10060511/)," Ph.D. thesis, Computer Science Department, University College London, July 2018.
11. Mehran Mahmoudi, Sarah Nadi, and Nikolaos Tsantalis, "[Are Refactorings to Blame? An Empirical Study of Refactorings in Merge Conflicts](https://doi.org/10.1109/SANER.2019.8668012)," *26th IEEE International Conference on Software Analysis, Evolution and Reengineering* (SANER 2019), Hangzhou, China, February 24-27, 2019.
12. Bin Lin, Csaba Nagy, Gabriele Bavota and Michele Lanza, "[On the Impact of Refactoring Operations on Code Naturalness](https://doi.org/10.1109/SANER.2019.8667992)," *26th IEEE International Conference on Software Analysis, Evolution and Reengineering* (SANER 2019), Hangzhou, China, February 24-27, 2019.
13. Sarah Fakhoury, Devjeet Roy, Sk. Adnan Hassan, and Venera Arnaoudova, "[Improving Source Code Readability: Theory and Practice](https://doi.org/10.1109/ICPC.2019.00014)," *27th IEEE/ACM International Conference on Program Comprehension* (ICPC 2019), Montreal, QC, Canada, May 25-26, 2019.
14. Carmine Vassallo, Giovanni Grano, Fabio Palomba, Harald C. Gall, and Alberto Bacchelli, "[A large-scale empirical exploration on refactoring activities in open source software projects](https://doi.org/10.1016/j.scico.2019.05.002)," *Science of Computer Programming*, Volume 180, Pages 1-15, July 2019.
15. Eman Abdullah AlOmar, Mohamed Wiem Mkaouer, and Ali Ouni, "[Can refactoring be self-affirmed?: An exploratory study on how developers document their refactoring activities in commit messages](https://doi.org/10.1109/IWoR.2019.00017)," *3rd International Workshop on Refactoring* (IWOR 2019), Montreal, QC, Canada, May 28, 2019.
16. Ana Carla Bibiano, Eduardo Fernandes, Daniel Oliveira, Alessandro Garcia, Marcos Kalinowski, Baldoino Fonseca, Roberto Oliveira, Anderson Oliveira, and Diego Cedrim, "[A Quantitative Study on Characteristics and Effect of Batch Refactoring on Code Smells](https://doi.org/10.1109/ESEM.2019.8870183)," *13th ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2019), Porto de Galinhas, Brazil, September 16-20, 2019.
17. Eman Abdullah AlOmar, Mohamed Wiem Mkaouer, Ali Ouni, and Marouane Kessentini, "[On the Impact of Refactoring on the Relationship between Quality Attributes and Design Metrics](https://doi.org/10.1109/ESEM.2019.8870177)," *13th ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2019), Porto de Galinhas, Brazil, September 16-20, 2019.
18. Edmilson Campos Neto, Daniel Alencar da Costa, and Uirá Kulesza, "[Revisiting and Improving SZZ Implementations](https://doi.org/10.1109/ESEM.2019.8870178)," *13th ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2019), Porto de Galinhas, Brazil, September 16-20, 2019.
19. Valentina Lenarduzzi, Nyyti Saarimäki, and Davide Taibi, "[The Technical Debt Dataset](https://doi.org/10.1145/3345629.3345630)," *15th International Conference on Predictive Models and Data Analytics in Software Engineering* (PROMISE 2019), Porto de Galinhas, Brazil, September 18, 2019.
20. Anthony Peruma, "[A preliminary study of Android refactorings](https://doi.org/10.1109/MOBILESoft.2019.00030)," *6th International Conference on Mobile Software Engineering and Systems* (MOBILESoft 2019), Montreal, Quebec, Canada, May 25-26, 2019.
21. Anthony Peruma, Mohamed Wiem Mkaouer, Michael J. Decker, and Christian D. Newman, "[Contextualizing Rename Decisions using
Refactorings and Commit Messages](https://doi.org/10.1109/SCAM.2019.00017)," *19th IEEE International Working Conference on Source Code Analysis and Manipulation* (SCAM 2019), Cleveland, OH, USA, September 30-October 1, 2019.
22. Soumaya Rebai, Oussama Ben Sghaier, Vahid Alizadeh, Marouane Kessentini, and Meriem Chater, "[Interactive Refactoring Documentation Bot](https://doi.org/10.1109/SCAM.2019.00026)," *19th IEEE International Working Conference on Source Code Analysis and Manipulation* (SCAM 2019), Cleveland, OH, USA, September 30-October 1, 2019.
23. Matheus Paixao, and Paulo Henrique Maia, "[Rebasing in Code Review Considered Harmful: A Large-Scale Empirical Investigation](https://doi.org/10.1109/SCAM.2019.00014)," *19th IEEE International Working Conference on Source Code Analysis and Manipulation* (SCAM 2019), Cleveland, OH, USA, September 30-October 1, 2019.
24. Willian Oizumi, Leonardo Da Silva Sousa, Anderson Oliveira, Luiz Matheus Alencar, Alessandro Garcia, Thelma E. Colanzi and Roberto Oliveira, "[On the density and diversity of degradation symptoms in refactored classes: A multi-case study](https://doi.org/10.1109/ISSRE.2019.00042)," *30th International Symposium on Software Reliability Engineering* (ISSRE 2019), Berlin, Germany, October 28-31, 2019.
25. Marcos César de Oliveira, Davi Freitas, Rodrigo Bonifácio, Gustavo Pinto, and David Lo, "[Finding Needles in a Haystack: Leveraging Co-change Dependencies to Recommend Refactorings](https://doi.org/10.1016/j.jss.2019.110420)," *Journal of Systems and Software*, Volume 158, December 2019.
26. Walter Lucas, Rodrigo Bonifácio, Edna Dias Canedo, Diego Marcílio, and Fernanda Lima, "[Does the Introduction of Lambda Expressions Improve the Comprehension of Java Programs?](https://doi.org/10.1145/3350768.3350791)," *XXXIII Brazilian Symposium on Software Engineering* (SBES 2019), Salvador, Brazil, September 23-27, 2019.
27. Bo Shen, Wei Zhang, Haiyan Zhao, Guangtai Liang, Zhi Jin, and Qianxiang Wang, "[IntelliMerge: A Refactoring-Aware Software Merging Technique](https://doi.org/10.1145/3360596)," *Proceedings of the ACM on Programming Languages*, vol. 3, OOPSLA, Article 170, October 2019.
28. Martina Iammarino, Fiorella Zampetti, Lerina Aversano, and Massimiliano Di Penta, "[Self-Admitted Technical Debt Removal and Refactoring Actions: Co-Occurrence or More?](https://doi.org/10.1109/ICSME.2019.00029)," *35th IEEE International Conference on Software Maintenance and Evolution* (ICSME 2019), Cleveland, OH, USA, September 29-October 4, 2019.
29. Ally S. Nyamawe, Hui Liu, Nan Niu, Qasim Umer, and Zhendong Niu, "[Automated Recommendation of Software Refactorings based on Feature Requests](https://doi.org/10.1109/RE.2019.00029)," *27th IEEE International Requirements Engineering Conference* (RE 2019), Jeju Island, South Korea, September 23-27, 2019.
30. Maurício Aniche, Erick Maziero, Rafael Durelli, and Vinicius Durelli, "[The Effectiveness of Supervised Machine Learning Algorithms in Predicting Software Refactoring](https://doi.org/10.1109/TSE.2020.3021736)," *IEEE Transactions on Software Engineering*, 2020.
31. Ana Bibiano, Vinicius Soares, Daniel Coutinho, Eduardo Fernandes, João Correia, Kleber Tarcísio, Anderson Oliveira, Alessandro Garcia, Rohit Gheyi, Marcio Ribeiro, Baldoino Fonseca, Caio Barbosa, and Daniel Oliveira, "[How Does Incomplete Composite Refactoring Affect Internal Quality Attributes?](https://doi.org/10.1145/3387904.3389264)," *28th IEEE International Conference on Program Comprehension* (ICPC 2020), Seoul, South Korea, 2020.
32. Leonardo Sousa, Willian Oizumi, Alessandro Garcia, Anderson Oliveira, Diego Cedrim, and Carlos Lucena, "[When Are Smells Indicators of Architectural Refactoring Opportunities? A Study of 50 Software Projects](https://doi.org/10.1145/3387904.3389276)," *28th IEEE International Conference on Program Comprehension* (ICPC 2020), Seoul, South Korea, 2020.
33. Devjeet Roy, Sarah Fakhoury, John Lee, and Venera Arnaoudova, "[A Model to Detect Readability Improvements in Incremental Changes](https://doi.org/10.1145/3387904.3389255)," *28th IEEE International Conference on Program Comprehension* (ICPC 2020), Seoul, South Korea, 2020.
34. Akira Fujimoto, Yoshiki Higo, Junnosuke Matsumoto, and Shinji Kusumoto, "[Staged Tree Matching for Detecting Code Move across Files](https://doi.org/10.1145/3387904.3389289)," *28th IEEE International Conference on Program Comprehension* (ICPC 2020), Seoul, South Korea, 2020.
35. Matheus Paixão, Anderson Uchôa, Ana Carla Bibiano, Daniel Oliveira, Alessandro Garcia, Jens Krinke, and Emilio Arvonio, "[Behind the Intents: An In-depth Empirical Study on Software Refactoring in Modern Code Review](https://doi.org/10.1145/3379597.3387475)," *17th International Conference on Mining Software Repositories* (MSR 2020), Seoul, South Korea, 2020.
36. Leonardo da Silva Sousa, Diego Cedrim, Alessandro Garcia, Willian Oizumi, Ana Carla Bibiano, Daniel Oliveira, Miryung Kim, and Anderson Oliveira, "[Characterizing and Identifying Composite Refactorings: Concepts, Heuristics and Patterns](https://doi.org/10.1145/3379597.3387477)," *17th International Conference on Mining Software Repositories* (MSR 2020), Seoul, South Korea, 2020.
37. Anthony Peruma, Christian D. Newman, Mohamed Wiem Mkaouer, Ali Ouni, and Fabio Palomba, "[An Exploratory Study on the Refactoring of Unit Test Files in Android Applications](https://doi.org/10.1145/3387940.3392189)," *4th International Workshop on Refactoring* (IWoR 2020), Seoul, South Korea, 2020.
38. Eman Abdullah AlOmar, Anthony Peruma, Christian D. Newman, Mohamed Wiem Mkaouer, and Ali Ouni, "[On the Relationship Between Developer Experience and Refactoring: An Exploratory Study and Preliminary Results](https://doi.org/10.1145/3387940.3392193)," *4th International Workshop on Refactoring* (IWoR 2020), Seoul, South Korea, 2020.
39. Yoshiki Higo, Shinpei Hayashi, and Shinji Kusumoto, "[On Tracking Java Methods with Git Mechanisms](https://doi.org/10.1016/j.jss.2020.110571)," *Journal of Systems and Software*, Volume 165, July 2020.
40. Eduardo Fernandes, Alexander Chávez, Alessandro Garcia, Isabella Ferreira, Diego Cedrim, Leonardo Sousa, and Willian Oizumi, "[Refactoring Effect on Internal Quality Attributes: What Haven't They Told You Yet?](https://doi.org/10.1016/j.infsof.2020.106347)," *Information and Software Technology*, 2020.
41. Rrezarta Krasniqi, and Jane Cleland-Huang, "[Enhancing Source Code Refactoring Detection with Explanations from Commit Messages](https://doi.org/10.1109/SANER48275.2020.9054816)," *IEEE 27th International Conference on Software Analysis, Evolution and Reengineering* (SANER 2020), London, ON, Canada, February 18-21, 2020.
42. Anthony Peruma, Mohamed Wiem Mkaouer, Michael J.Decker, and Christian D.Newman, "[Contextualizing rename decisions using refactorings, commit messages, and data types](https://doi.org/10.1016/j.jss.2020.110704)," *Journal of Systems and Software*, Volume 169, November 2020.
43. Lerina Aversano, Umberto Carpenito, and Martina Iammarino, "[An Empirical Study on the Evolution of Design Smells](http://dx.doi.org/10.3390/info11070348)," *Information*, vol. 11, no. 7:348, 2020.
44. Jevgenija Pantiuchina, Fiorella Zampetti, Simone Scalabrino, Valentina Piantadosi, Rocco Oliveto, Gabriele Bavota, and Massimiliano Di Penta, "[Why Developers Refactor Source Code: A Mining-based Study](https://doi.org/10.1145/3408302)," *ACM Transactions on Software Engineering and Methodology*, Volume 29, Issue 4, Article 29, September 2020.
45. Ally S. Nyamawe, Hui Liu, Nan Niu, Qasim Umer, and Zhendong Niu, "[Feature requests-based recommendation of software refactorings](https://doi.org/10.1007/s10664-020-09871-2)," *Empirical Software Engineering*, Volume 25, pp. 4315–4347, 2020.
46. Eman Abdullah AlOmar, Mohamed Wiem Mkaouer, and Ali Ouni, "[Toward the automatic classification of Self-Affirmed Refactoring](https://doi.org/10.1016/j.jss.2020.110821)," *Journal of Systems and Software*, Volume 171, January 2021.
47. Vinícius Soares, Anderson Oliveira, Juliana Alves Pereira, Ana Carla Bibano, Alessandro Garcia, Paulo Roberto Farah, Silvia Regina Vergilio, Marcelo Schots, Caio Silva, Daniel Coutinho, Daniel Oliveira, and Anderson Uchôa, "[On the Relation between Complexity, Explicitness, Effectiveness of Refactorings and Non-Functional Concerns](https://doi.org/10.1145/3422392.3422439)," *34th Brazilian Symposium on Software Engineering* (SBES 2020), October 19–23, 2020.
48. Willian Oizumi, Diego Cedrim, Leonardo Sousa, Ana Carla Bibiano, Anderson Oliveira, Alessandro Garcia, and Daniel Oliveira, "[Recommending Composite Refactorings for Smell Removal: Heuristics and Evaluation](https://doi.org/10.1145/3422392.3422423)," *34th Brazilian Symposium on Software Engineering* (SBES 2020), October 19–23, 2020.
49. Massimiliano Di Penta, Gabriele Bavota, and Fiorella Zampetti, "[On the Relationship between Refactoring Actions and Bugs: A Differentiated Replication](https://doi.org/10.1145/3368089.3409695)," *ACM Joint European Software Engineering Conference and Symposium on the Foundations of Software Engineering* (ESEC/FSE 2020), Sacramento, California, United States, November 8-13, 2020.
50. Ameya Ketkar, Nikolaos Tsantalis, and Danny Dig, "[Understanding Type Changes in Java](https://doi.org/10.1145/3368089.3409725)," *ACM Joint European Software Engineering Conference and Symposium on the Foundations of Software Engineering* (ESEC/FSE 2020), Sacramento, California, United States, November 8-13, 2020.
51. Zhongxin Liu, Xin Xia, Meng Yan, and Shanping Li, "[Automating Just-In-Time Comment Updating](https://doi.org/10.1145/3324884.3416581)," *35th IEEE/ACM International Conference on Automated Software Engineering* (ASE 2020), September 21–25, 2020.
52. Zadia Codabux and Christopher Dutchyn, "[Profiling Developers Through the Lens of Technical Debt](https://doi.org/10.1145/3382494.3422172)," *ACM/IEEE International Symposium on Empirical Software Engineering and Measurement* (ESEM 2020), October 8–9, 2020, Bari, Italy.
53. Yiming Tang, Raffi Khatchadourian, Mehdi Bagherzadeh, Rhia Singh, Ajani Stewart, and Anita Raja, "[An Empirical Study of Refactorings and Technical Debt in Machine Learning Systems](https://doi.org/10.1109/ICSE43902.2021.00033)," *43rd International Conference on Software Engineering* (ICSE 2021), Madrid, Spain, May 25-28, 2021.
54. Dong Jae Kim, Nikolaos Tsantalis, Tse-Hsun (Peter) Chen, and Jinqiu Yang, "[Studying Test Annotation Maintenance in the Wild](https://doi.org/10.1109/ICSE43902.2021.00019)," *43rd International Conference on Software Engineering* (ICSE 2021), Madrid, Spain, May 25-28, 2021.
55. Yanjie Jiang, Hui Liu, Nan Niu, Lu Zhang, and Yamin Hu, "[Extracting Concise Bug-Fixing Patches from Human-Written Patches in Version Control Systems](https://doi.ieeecomputersociety.org/10.1109/ICSE43902.2021.00069)," *43rd International Conference on Software Engineering* (ICSE 2021), Madrid, Spain, May 25-28, 2021.
56. Giovanni Rosa, Luca Pascarella, Simone Scalabrino, Rosalia Tufano, Gabriele Bavota, Michele Lanza, and Rocco Oliveto, "[Evaluating SZZ Implementations Through a Developer-informed Oracle](https://doi.ieeecomputersociety.org/10.1109/ICSE43902.2021.00049)," *43rd International Conference on Software Engineering* (ICSE 2021), Madrid, Spain, May 25-28, 2021.
57. Bo Shen, Wei Zhang, Christian Kästner, Haiyan Zhao, Zhao Wei, Guangtai Liang, and Zhi Jin, "[SmartCommit: a graph-based interactive assistant for activity-oriented commits](https://doi.org/10.1145/3468264.3468551)," *29th ACM Joint Meeting on European Software Engineering Conference and Symposium on the Foundations of Software Engineering* (ESEC/FSE 2021), Athens, Greece, August 23-28, 2021.
58. Dimitrios Tsoukalas, Nikolaos Mittas, Alexander Chatzigeorgiou, Dionysios Kehagias, Apostolos Ampatzoglou, Theodoros Amanatidis, and Lefteris Angelis, "[Machine Learning for Technical Debt Identification](https://doi.ieeecomputersociety.org/10.1109/TSE.2021.3129355)," *IEEE Transactions on Software Engineering*, 2021.
59. Luca Traini, Daniele Di Pompeo, Michele Tucci, Bin Lin, Simone Scalabrino, Gabriele Bavota, Michele Lanza, Rocco Oliveto, and Vittorio Cortellessa, "[How Software Refactoring Impacts Execution Time](https://doi.org/10.1145/3485136)," *ACM Transactions on Software Engineering and Methodology*, Volume 31, Issue 2, Article 25, pp. 1-23, April 2022.
60. Jarosław Pokropiński, Jakub Gąsiorek, Patryk Kramarczyk, and Lech Madeyski, "[SZZ Unleashed-RA-C: An Improved Implementation of the SZZ Algorithm and Empirical Comparison with Existing Open Source Solutions](https://doi.org/10.1007/978-3-030-77916-0_7)," Developments in Information & Knowledge Management for Business Applications : Volume 3, Springer International Publishing, pp. 181-199, 2022.
61. Eman Abdullah AlOmar, Jiaqian Liu, Kenneth Addo, Mohamed Wiem Mkaouer, Christian Newman, Ali Ouni, and Zhe Yu, "[On the documentation of refactoring types](https://doi.org/10.1007/s10515-021-00314-w)," *Automated Software Engineering*, Volume 29, Article 9, 2022.
62. Giulia Sellitto, Emanuele Iannone, Zadia Codabux, Valentina Lenarduzzi, Andrea De Lucia, Fabio Palomba, and Filomena Ferrucci, "Toward Understanding the Impact of Refactoring on Program Comprehension," *29th IEEE International Conference on Software Analysis, Evolution and Reengineering* (SANER 2022), Honolulu, Hawaii, USA, March 15-18, 2022.
63. Eman Abdullah AlOmar, Tianjia Wang, Vaibhavi Raut, Mohamed Wiem Mkaouer, Christian Newman, and Ali Ouni, "[Refactoring for Reuse: An Empirical Study](https://arxiv.org/abs/2111.07002)," arXiv:2111.07002v1, 13 Nov 2021.
64. Anton Ivanov, Zarina Kurbatova, Yaroslav Golubev, Andrey Kirilenko, and Timofey Bryksin, "[AntiCopyPaster: Extracting Code Duplicates As Soon As They Are Introduced in the IDE](https://arxiv.org/abs/2112.15230)," arXiv:2112.15230v1, 30 Dec 2021.
65. Max Ellis, Sarah Nadi, and Danny Dig, "[A Systematic Comparison of Two Refactoring-aware Merging Techniques](https://arxiv.org/abs/2112.10370)," arXiv:2112.10370v1, 20 Dec 2021.

# Support for other programming languages
## Kotlin
* [KotlinRMiner](https://github.com/JetBrains-Research/kotlinRMiner) has been developed by [JetBrains Research](https://research.jetbrains.org/).
The project is led and maintained by [Zarina Kurbatova](https://github.com/onewhl).

## Python
* [PyRef](https://github.com/PyRef/PyRef) has been developed by Hassan Atwi and [Bin Lin](https://binlin.info/) from the Software Institute at USI - Università della Svizzera Italiana, Switzerland.
* [Py-RefactoringMiner](https://github.com/maldil/RefactoringMiner) has been developed by [Malinda Dilhara](https://maldil.github.io/), a Ph.D. student in the department of Computer Science at University of Colorado Boulder under the suprevision of [Danny Dig](https://dig.cs.illinois.edu/).

# Contributors
The code in package **gr.uom.java.xmi.*** is developed by [Nikolaos Tsantalis](https://github.com/tsantalis).

The code in package **org.refactoringminer.*** was initially developed by [Danilo Ferreira e Silva](https://github.com/danilofes) and later extended by [Nikolaos Tsantalis](https://github.com/tsantalis).

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

There is also a lower level API that compares the Java files in two directories
containing the code before and after some changes:  

```java
UMLModel model1 = new UMLModelASTReader(new File("/path/to/version1")).getUmlModel();
UMLModel model2 = new UMLModelASTReader(new File("/path/to/version2")).getUmlModel();
UMLModelDiff modelDiff = model1.diff(model2);
List<Refactoring> refactorings = modelDiff.getRefactorings();
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

	-h											Show options
	-a <git-repo-folder> <branch> -json <path-to-json-file>					Detect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.
	-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1> -json <path-to-json-file>	Detect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>
	-bt <git-repo-folder> <start-tag> <end-tag> -json <path-to-json-file>			Detect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>
	-c <git-repo-folder> <commit-sha1> -json <path-to-json-file>				Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>
	-gc <git-URL> <commit-sha1> <timeout> -json <path-to-json-file>				Detect refactorings at specified commit <commit-sha1> for project <git-URL> within the given <timeout> in seconds. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties
	-gp <git-URL> <pull-request> <timeout> -json <path-to-json-file>			Detect refactorings at specified pull request <pull-request> for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties
	
With a locally cloned repository, run:

    > git clone https://github.com/danilofes/refactoring-toy-example.git refactoring-toy-example
    > ./RefactoringMiner -c refactoring-toy-example 36287f7c3b09eff78395267a3ac0d7da067863fd

If you don't want to clone locally the repository, run:

    > ./RefactoringMiner -gc https://github.com/danilofes/refactoring-toy-example.git 36287f7c3b09eff78395267a3ac0d7da067863fd 10

**For all options you can add the `-json <path-to-json-file>` command arguments to save the JSON output in a file. The results are appended to the file after each processed commit.**

For the `-gc` and `-gp` options you must provide a valid OAuth token in the `github-oauth.properties` file stored in the `bin` folder.
You can generate an OAuth token in GitHub `Settings` -> `Developer settings` -> `Personal access tokens`.

In both cases, you will get the output in JSON format:

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
