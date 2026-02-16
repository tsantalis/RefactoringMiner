# Current precision and recall

## Java Benchmark 1
**Source**: Nikolaos Tsantalis, Ameya Ketkar, and Danny Dig, "RefactoringMiner 2.0," IEEE Transactions on Software Engineering, vol. 48, no. 3, pp. 930-950, March 2022.

**Properties**: 547 commits from 188 open-source projects

**Commit dates**: between June 8th and August 7th, 2015

**File**: [data.json](https://github.com/tsantalis/RefactoringMiner/blob/master/src/test/resources/oracle/data.json)

The original benchmark has been extended by adding newly supported refactoring types by RefactoringMiner.
As of **January 31, 2026** the precision and recall of RefactoringMiner on this benchmark is:

| Refactoring Type | TP | FP | FN | Precision | Recall |
|:-----------------------|-----------:|--------:|--------:|--------:|--------:|
|**Total**|12646  | 13  | 212  | 0.999  | 0.984|
|Extract Method|1018  |  1  | 20  | 0.999  | 0.981|
|Rename Class|56  |  0  |  2  | 1.000  | 0.966|
|Move Attribute|257  |  0  |  8  | 1.000  | 0.970|
|Move And Rename Attribute|12  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Rename Method|399  |  2  | 19  | 0.995  | 0.955|
|Inline Method|119  |  0  |  1  | 1.000  | 0.992|
|Move Method|390  |  3  |  5  | 0.992  | 0.987|
|Move And Rename Method|130  |  0  |  4  | 1.000  | 0.970|
|Pull Up Method|285  |  0  |  5  | 1.000  | 0.983|
|Move Class|1095  |  0  |  4  | 1.000  | 0.996|
|Move And Rename Class|38  |  0  |  1  | 1.000  | 0.974|
|Move Source Folder| 3  |  0  |  0  | 1.000  | 1.000|
|Pull Up Attribute|145  |  0  |  1  | 1.000  | 0.993|
|Push Down Attribute|35  |  0  |  0  | 1.000  | 1.000|
|Push Down Method|47  |  0  |  0  | 1.000  | 1.000|
|Extract Interface|22  |  0  |  0  | 1.000  | 1.000|
|Extract Superclass|74  |  0  |  0  | 1.000  | 1.000|
|Extract Subclass| 4  |  0  |  0  | 1.000  | 1.000|
|Extract Class|108  |  0  |  0  | 1.000  | 1.000|
|Extract And Move Method|130  |  0  | 60  | 1.000  | 0.684|
|Move And Inline Method|12  |  0  |  4  | 1.000  | 0.750|
|Replace Anonymous With Class| 8  |  0  |  0  | 1.000  | 1.000|
|Rename Package|16  |  0  |  0  | 1.000  | 1.000|
|Move Package|10  |  0  |  0  | 1.000  | 1.000|
|Extract Variable|325  |  0  |  0  | 1.000  | 1.000|
|Extract Attribute|33  |  0  |  0  | 1.000  | 1.000|
|Inline Variable|147  |  0  |  0  | 1.000  | 1.000|
|Inline Attribute| 9  |  0  |  0  | 1.000  | 1.000|
|Rename Variable|346  |  2  | 11  | 0.994  | 0.969|
|Rename Parameter|508  |  2  | 24  | 0.996  | 0.955|
|Rename Attribute|148  |  0  |  8  | 1.000  | 0.949|
|Merge Variable| 6  |  0  |  0  | 1.000  | 1.000|
|Merge Parameter|28  |  0  |  0  | 1.000  | 1.000|
|Merge Attribute| 5  |  0  |  0  | 1.000  | 1.000|
|Split Parameter| 7  |  0  |  0  | 1.000  | 1.000|
|Split Attribute| 2  |  0  |  0  | 1.000  | 1.000|
|Replace Variable With Attribute|125  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute With Variable|32  |  0  |  1  | 1.000  | 0.970|
|Parameterize Variable|112  |  0  |  0  | 1.000  | 1.000|
|Localize Parameter|31  |  0  |  0  | 1.000  | 1.000|
|Parameterize Attribute|25  |  0  |  0  | 1.000  | 1.000|
|Change Return Type|439  |  0  | 12  | 1.000  | 0.973|
|Change Variable Type|817  |  1  |  7  | 0.999  | 0.992|
|Change Parameter Type|661  |  1  | 10  | 0.998  | 0.985|
|Change Attribute Type|246  |  0  |  3  | 1.000  | 0.988|
|Add Method Annotation|332  |  0  |  0  | 1.000  | 1.000|
|Remove Method Annotation|100  |  0  |  0  | 1.000  | 1.000|
|Modify Method Annotation|29  |  0  |  0  | 1.000  | 1.000|
|Add Attribute Annotation|62  |  0  |  1  | 1.000  | 0.984|
|Remove Attribute Annotation|18  |  0  |  0  | 1.000  | 1.000|
|Modify Attribute Annotation| 7  |  0  |  0  | 1.000  | 1.000|
|Add Class Annotation|52  |  0  |  0  | 1.000  | 1.000|
|Remove Class Annotation|20  |  0  |  0  | 1.000  | 1.000|
|Modify Class Annotation|35  |  0  |  0  | 1.000  | 1.000|
|Add Parameter Annotation|34  |  0  |  0  | 1.000  | 1.000|
|Remove Parameter Annotation| 4  |  0  |  0  | 1.000  | 1.000|
|Modify Parameter Annotation| 2  |  0  |  0  | 1.000  | 1.000|
|Add Parameter|855  |  1  |  1  | 0.999  | 0.999|
|Remove Parameter|328  |  0  |  0  | 1.000  | 1.000|
|Reorder Parameter| 9  |  0  |  0  | 1.000  | 1.000|
|Add Variable Annotation| 2  |  0  |  0  | 1.000  | 1.000|
|Remove Variable Annotation| 4  |  0  |  0  | 1.000  | 1.000|
|Add Thrown Exception Type|40  |  0  |  0  | 1.000  | 1.000|
|Remove Thrown Exception Type|271  |  0  |  0  | 1.000  | 1.000|
|Change Thrown Exception Type| 9  |  0  |  0  | 1.000  | 1.000|
|Change Method Access Modifier|334  |  0  |  0  | 1.000  | 1.000|
|Change Attribute Access Modifier|233  |  0  |  0  | 1.000  | 1.000|
|Encapsulate Attribute|52  |  0  |  0  | 1.000  | 1.000|
|Add Method Modifier|90  |  0  |  0  | 1.000  | 1.000|
|Remove Method Modifier|112  |  0  |  0  | 1.000  | 1.000|
|Add Attribute Modifier|142  |  0  |  0  | 1.000  | 1.000|
|Remove Attribute Modifier|143  |  0  |  0  | 1.000  | 1.000|
|Add Variable Modifier|135  |  0  |  0  | 1.000  | 1.000|
|Add Parameter Modifier|133  |  0  |  0  | 1.000  | 1.000|
|Remove Variable Modifier|68  |  0  |  0  | 1.000  | 1.000|
|Remove Parameter Modifier|39  |  0  |  0  | 1.000  | 1.000|
|Change Class Access Modifier|78  |  0  |  0  | 1.000  | 1.000|
|Add Class Modifier|37  |  0  |  0  | 1.000  | 1.000|
|Remove Class Modifier|45  |  0  |  0  | 1.000  | 1.000|
|Split Package| 4  |  0  |  0  | 1.000  | 1.000|
|Merge Package| 2  |  0  |  0  | 1.000  | 1.000|
|Change Type Declaration Kind| 6  |  0  |  0  | 1.000  | 1.000|
|Collapse Hierarchy| 1  |  0  |  0  | 1.000  | 1.000|
|Replace Loop With Pipeline|36  |  0  |  0  | 1.000  | 1.000|
|Replace Pipeline With Loop| 2  |  0  |  0  | 1.000  | 1.000|
|Replace Anonymous With Lambda|45  |  0  |  0  | 1.000  | 1.000|
|Merge Class| 5  |  0  |  0  | 1.000  | 1.000|
|Split Class| 3  |  0  |  0  | 1.000  | 1.000|
|Split Conditional|19  |  0  |  0  | 1.000  | 1.000|
|Invert Condition|49  |  0  |  0  | 1.000  | 1.000|
|Merge Conditional|14  |  0  |  0  | 1.000  | 1.000|
|Merge Catch| 2  |  0  |  0  | 1.000  | 1.000|
|Merge Method| 3  |  0  |  0  | 1.000  | 1.000|
|Split Method| 6  |  0  |  0  | 1.000  | 1.000|
|Move Code|23  |  0  |  0  | 1.000  | 1.000|
|Assert Throws|14  |  0  |  0  | 1.000  | 1.000|
|Try With Resources| 4  |  0  |  0  | 1.000  | 1.000|
|Replace Generic With Diamond|77  |  0  |  0  | 1.000  | 1.000|
|Replace Conditional With Ternary| 8  |  0  |  0  | 1.000  | 1.000|
|Extract Fixture| 3  |  0  |  0  | 1.000  | 1.000|

## Java Benchmark 2
**Source**: Bo Liu, Hui Liu, Nan Niu, Yuxia Zhang, Guangjie Li, He Jiang, and Yanjie Jiang, "An Automated Approach to Discovering Software Refactorings by Comparing Successive Versions," IEEE Transactions on Software Engineering, 2025.

**Properties**: 400 commits from 20 open-source projects (20 commits per project)

**Commit dates**: March 28, 2024 or newer

**Files**: [tse-dataset](https://github.com/tsantalis/RefactoringMiner/tree/master/src/test/resources/oracle/tse-dataset)

The original benchmark has been re-validated and corrected by Nikolaos Tsantalis. The validation process is still in progress.
Moreover, the benchmark has been extended with valid instances for the following refactoring types:
* `Replace Variable With Attribute`
* `Replace Attribute With Variable`
* `Extract Attribute`
* `Change Type Declaration Kind`
* `Replace Pipeline With Loop`
* `Replace Loop With Pipeline`
* `Merge Method`
* `Split Method`
* `Replace Anonymous With Class`
* `Move Code`
* `Split Class`
* `Merge Variable`
* `Merge Parameter`
* `Invert Condition`

As of **January 31, 2026** the precision and recall of RefactoringMiner on this benchmark is:

| Refactoring Type | TP | FP | FN | Precision | Recall |
|:-----------------------|-----------:|--------:|--------:|--------:|--------:|
|**Total**|3543  | 40  | 76  | 0.989  | 0.979|
|Extract Method|375  |  1  |  4  | 0.997  | 0.989|
|Rename Class|233  |  0  |  1  | 1.000  | 0.996|
|Move Attribute|72  |  0  |  7  | 1.000  | 0.911|
|Move And Rename Attribute| 7  |  0  |  1  | 1.000  | 0.875|
|Rename Method|300  |  7  |  5  | 0.977  | 0.984|
|Inline Method|70  |  3  |  2  | 0.959  | 0.972|
|Move Method|266  |  1  |  0  | 0.996  | 1.000|
|Move And Rename Method|26  |  3  |  0  | 0.897  | 1.000|
|Pull Up Method|47  |  0  |  0  | 1.000  | 1.000|
|Move Class|144  |  1  |  1  | 0.993  | 0.993|
|Move And Rename Class|28  |  0  |  0  | 1.000  | 1.000|
|Pull Up Attribute|12  |  0  |  0  | 1.000  | 1.000|
|Push Down Attribute| 6  |  0  |  0  | 1.000  | 1.000|
|Push Down Method|22  |  0  |  0  | 1.000  | 1.000|
|Extract Interface|16  |  2  |  0  | 0.889  | 1.000|
|Extract Superclass| 8  |  0  |  0  | 1.000  | 1.000|
|Extract Subclass| 8  |  0  |  0  | 1.000  | 1.000|
|Extract Class|45  |  1  |  4  | 0.978  | 0.918|
|Extract And Move Method|129  |  1  |  5  | 0.992  | 0.963|
|Move And Inline Method|28  |  1  |  6  | 0.966  | 0.824|
|Replace Anonymous With Class| 5  |  0  |  0  | 1.000  | 1.000|
|Extract Variable|293  |  1  |  6  | 0.997  | 0.980|
|Extract Attribute| 6  |  0  |  0  | 1.000  | 1.000|
|Inline Variable|127  |  0  | 11  | 1.000  | 0.920|
|Rename Variable|308  |  5  |  6  | 0.984  | 0.981|
|Rename Attribute|112  |  4  |  6  | 0.966  | 0.949|
|Merge Variable| 4  |  0  |  0  | 1.000  | 1.000|
|Merge Parameter|11  |  0  |  0  | 1.000  | 1.000|
|Replace Variable With Attribute|11  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute With Variable|58  |  0  |  0  | 1.000  | 1.000|
|Change Return Type|169  |  1  |  6  | 0.994  | 0.966|
|Change Variable Type|378  |  6  |  2  | 0.984  | 0.995|
|Change Attribute Type|164  |  2  |  3  | 0.988  | 0.982|
|Change Type Declaration Kind| 4  |  0  |  0  | 1.000  | 1.000|
|Replace Loop With Pipeline| 4  |  0  |  0  | 1.000  | 1.000|
|Replace Pipeline With Loop| 3  |  0  |  0  | 1.000  | 1.000|
|Split Class| 1  |  0  |  0  | 1.000  | 1.000|
|Invert Condition|23  |  0  |  0  | 1.000  | 1.000|
|Merge Method| 1  |  0  |  0  | 1.000  | 1.000|
|Split Method| 8  |  0  |  0  | 1.000  | 1.000|
|Move Code|11  |  0  |  0  | 1.000  | 1.000|

## Python Benchmark
**Source**: Hassan Atwi, Bin Lin, Nikolaos Tsantalis, Yutaro Kashiwa, Yasutaka Kamei, Naoyasu Ubayashi, Gabriele Bavota, and Michele Lanza, "PyRef: Refactoring Detection in Python Projects," 21st IEEE International Working Conference on Source Code Analysis and Manipulation (SCAM'2021), Engineering Track, Luxembourg City, Luxembourg, September 27-28, 2021.

**Properties**: 201 commits from 3 open-source projects

**Commit dates**: January 2013 - October 2020

**File**: [python-dataset](https://github.com/tsantalis/RefactoringMiner/tree/master/src/test/resources/oracle/python-dataset/data.json)

The original benchmark has been re-validated by Nikolaos Tsantalis. The validation process is still in progress.
Moreover, the benchmark has been extended with valid instances for the following refactoring types:
* `Rename Class`
* `Move Class`
* `Move And Rename Class`
* `Rename Attribute`
* `Move Attribute`
* `Pull Up Attribute`
* `Push Down Attribute`
* `Move And Rename Method`
* `Extract Class`
* `Extract Superclass`
* `Extract Subclass`
* `Extract And Move Method`
* `Rename Variable`
* `Extract Variable`
* `Inline Variable`
* `Replace Variable With Attribute`
* `Replace Attribute With Variable`
* `Parameterize Variable`
* `Localize Parameter`
* `Parameterize Attribute`
* `Change Variable Type`
* `Add Method Annotation`
* `Remove Method Annotation`
* `Add Class Annotation`
* `Reorder Parameter`
* `Split Conditional`
* `Move Code`
* `Encapsulate Attribute`
* `Invert Condition`
* `Split Parameter`

As of **January 20, 2026** the precision and recall of RefactoringMiner on this benchmark is:

| Refactoring Type | TP | FP | FN | Precision | Recall |
|:-----------------------|-----------:|--------:|--------:|--------:|--------:|
|**Total**|1109  |  6  |  2  | 0.995  | 0.998|
|Extract Method|37  |  0  |  0  | 1.000  | 1.000|
|Rename Class|13  |  0  |  0  | 1.000  | 1.000|
|Move Attribute| 5  |  0  |  0  | 1.000  | 1.000|
|Rename Method|140  |  0  |  1  | 1.000  | 0.993|
|Inline Method| 6  |  0  |  0  | 1.000  | 1.000|
|Move Method|21  |  0  |  0  | 1.000  | 1.000|
|Move And Rename Method| 9  |  0  |  0  | 1.000  | 1.000|
|Pull Up Method| 7  |  0  |  0  | 1.000  | 1.000|
|Move Class|13  |  0  |  0  | 1.000  | 1.000|
|Move And Rename Class|10  |  0  |  0  | 1.000  | 1.000|
|Pull Up Attribute| 4  |  0  |  0  | 1.000  | 1.000|
|Push Down Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Push Down Method| 3  |  0  |  0  | 1.000  | 1.000|
|Extract Superclass| 4  |  0  |  0  | 1.000  | 1.000|
|Extract Subclass| 2  |  0  |  0  | 1.000  | 1.000|
|Extract Class| 3  |  0  |  0  | 1.000  | 1.000|
|Extract And Move Method|15  |  0  |  0  | 1.000  | 1.000|
|Extract Variable|56  |  1  |  0  | 0.982  | 1.000|
|Inline Variable|21  |  0  |  0  | 1.000  | 1.000|
|Rename Variable|119  |  1  |  0  | 0.992  | 1.000|
|Rename Parameter|138  |  1  |  0  | 0.993  | 1.000|
|Rename Attribute|27  |  0  |  0  | 1.000  | 1.000|
|Split Parameter| 3  |  0  |  0  | 1.000  | 1.000|
|Replace Variable With Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute With Variable| 2  |  0  |  0  | 1.000  | 1.000|
|Parameterize Variable|11  |  0  |  0  | 1.000  | 1.000|
|Localize Parameter| 9  |  0  |  0  | 1.000  | 1.000|
|Parameterize Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Change Variable Type|26  |  3  |  0  | 0.897  | 1.000|
|Add Method Annotation|26  |  0  |  0  | 1.000  | 1.000|
|Remove Method Annotation|19  |  0  |  0  | 1.000  | 1.000|
|Add Class Annotation| 1  |  0  |  0  | 1.000  | 1.000|
|Add Parameter|248  |  0  |  1  | 1.000  | 0.996|
|Remove Parameter|73  |  0  |  0  | 1.000  | 1.000|
|Reorder Parameter|18  |  0  |  0  | 1.000  | 1.000|
|Encapsulate Attribute| 3  |  0  |  0  | 1.000  | 1.000|
|Split Conditional| 4  |  0  |  0  | 1.000  | 1.000|
|Invert Condition| 1  |  0  |  0  | 1.000  | 1.000|
|Move Code| 9  |  0  |  0  | 1.000  | 1.000|

**Important notes**:
Commit [asyml/texar@36f4b18](https://github.com/asyml/texar/commit/36f4b18340e2974cfee80e5c347bf7ae7459ab88) has been skipped due to a parsing error,
i.e., a missing comma after parameter `w_ppl=1.` of function `append` in file `examples/tsf/stats.py`
```python
def append(self, loss, g, ppl, d, d0, d1,
             w_loss=1., w_g=1., w_ppl=1. w_d=1, w_d0=1., w_d1=1.):
```
Python commits may not always be syntactically valid, leading to parsing errors. Make sure to consult the `System.error` logs printed by the tool for parsing errors, as commits with parsing errors may include incorrect refactoring information.

## Kotlin Benchmark
**Source**: Iman Hemati Moghadam, Mohammad Mehdi Afkhami, Parsa Kamalipour, and Vadim Zaytsev, "Extending Refactoring Detection to Kotlin: A Dataset and Comparative Study," 2024 IEEE International Conference on Software Analysis, Evolution and Reengineering (SANER), Rovaniemi, Finland, 2024, pp. 267-271, doi: 10.1109/SANER60148.2024.00034

**Properties**: 62 commits from 3 open-source projects

**File**: [kotlin-dataset](https://github.com/tsantalis/RefactoringMiner/tree/master/src/test/resources/oracle/kotlin-dataset/data.json)

As of **February 8, 2026** the precision and recall of RefactoringMiner on this benchmark is:

| Refactoring Type | TP | FP | FN | Precision | Recall |
|:-----------------------|-----------:|--------:|--------:|--------:|--------:|
|**Total**|1824  |  4  |  0  | 0.998  | 1.000|
|Extract Method|11  |  0  |  0  | 1.000  | 1.000|
|Rename Class|42  |  0  |  0  | 1.000  | 1.000|
|Move Attribute|53  |  0  |  0  | 1.000  | 1.000|
|Move And Rename Attribute| 2  |  0  |  0  | 1.000  | 1.000|
|Rename Method|134  |  0  |  0  | 1.000  | 1.000|
|Inline Method|16  |  0  |  0  | 1.000  | 1.000|
|Move Method|91  |  0  |  0  | 1.000  | 1.000|
|Move And Rename Method| 8  |  0  |  0  | 1.000  | 1.000|
|Pull Up Method|46  |  0  |  0  | 1.000  | 1.000|
|Move Class|179  |  0  |  0  | 1.000  | 1.000|
|Move And Rename Class|13  |  0  |  0  | 1.000  | 1.000|
|Pull Up Attribute|17  |  0  |  0  | 1.000  | 1.000|
|Push Down Attribute| 4  |  0  |  0  | 1.000  | 1.000|
|Push Down Method|15  |  0  |  0  | 1.000  | 1.000|
|Extract Interface| 5  |  0  |  0  | 1.000  | 1.000|
|Extract Superclass| 9  |  0  |  0  | 1.000  | 1.000|
|Extract Subclass| 2  |  0  |  0  | 1.000  | 1.000|
|Extract Class|13  |  0  |  0  | 1.000  | 1.000|
|Extract And Move Method|19  |  1  |  0  | 0.950  | 1.000|
|Move And Inline Method|24  |  0  |  0  | 1.000  | 1.000|
|Rename Package| 3  |  0  |  0  | 1.000  | 1.000|
|Move Package| 5  |  0  |  0  | 1.000  | 1.000|
|Extract Variable|16  |  0  |  0  | 1.000  | 1.000|
|Inline Variable|10  |  2  |  0  | 0.833  | 1.000|
|Inline Attribute| 3  |  0  |  0  | 1.000  | 1.000|
|Rename Variable|36  |  1  |  0  | 0.973  | 1.000|
|Rename Parameter|73  |  0  |  0  | 1.000  | 1.000|
|Rename Attribute|74  |  0  |  0  | 1.000  | 1.000|
|Split Parameter| 1  |  0  |  0  | 1.000  | 1.000|
|Replace Variable With Attribute| 3  |  0  |  0  | 1.000  | 1.000|
|Replace Attribute With Variable| 2  |  0  |  0  | 1.000  | 1.000|
|Parameterize Variable| 5  |  0  |  0  | 1.000  | 1.000|
|Localize Parameter| 7  |  0  |  0  | 1.000  | 1.000|
|Parameterize Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Change Return Type|116  |  0  |  0  | 1.000  | 1.000|
|Change Variable Type|21  |  0  |  0  | 1.000  | 1.000|
|Change Parameter Type|145  |  0  |  0  | 1.000  | 1.000|
|Change Attribute Type|65  |  0  |  0  | 1.000  | 1.000|
|Add Method Annotation|24  |  0  |  0  | 1.000  | 1.000|
|Remove Method Annotation|10  |  0  |  0  | 1.000  | 1.000|
|Add Attribute Annotation|11  |  0  |  0  | 1.000  | 1.000|
|Remove Attribute Annotation| 5  |  0  |  0  | 1.000  | 1.000|
|Add Class Annotation|20  |  0  |  0  | 1.000  | 1.000|
|Remove Class Annotation| 6  |  0  |  0  | 1.000  | 1.000|
|Add Parameter Annotation| 1  |  0  |  0  | 1.000  | 1.000|
|Remove Parameter Annotation| 2  |  0  |  0  | 1.000  | 1.000|
|Add Parameter|144  |  0  |  0  | 1.000  | 1.000|
|Remove Parameter|110  |  0  |  0  | 1.000  | 1.000|
|Reorder Parameter| 2  |  0  |  0  | 1.000  | 1.000|
|Remove Variable Annotation| 1  |  0  |  0  | 1.000  | 1.000|
|Remove Thrown Exception Type| 1  |  0  |  0  | 1.000  | 1.000|
|Change Method Access Modifier|49  |  0  |  0  | 1.000  | 1.000|
|Change Attribute Access Modifier|81  |  0  |  0  | 1.000  | 1.000|
|Encapsulate Attribute| 1  |  0  |  0  | 1.000  | 1.000|
|Change Class Access Modifier|30  |  0  |  0  | 1.000  | 1.000|
|Add Class Modifier| 7  |  0  |  0  | 1.000  | 1.000|
|Change Type Declaration Kind|12  |  0  |  0  | 1.000  | 1.000|
|Replace Loop With Pipeline| 9  |  0  |  0  | 1.000  | 1.000|
|Merge Class| 1  |  0  |  0  | 1.000  | 1.000|
|Invert Condition| 2  |  0  |  0  | 1.000  | 1.000|
|Merge Conditional| 2  |  0  |  0  | 1.000  | 1.000|
|Move Code| 4  |  0  |  0  | 1.000  | 1.000|