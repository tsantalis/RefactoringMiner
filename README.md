# RefDetector
RefDetector is a library/API written in Java that can detect refactorings applied in the history of a Java project.

Currently, it supports the detection of the following refactorings:

1. Extract Method
2. Inline Method
3. Move Method/Attribute
4. Pull Up Method/Attribute
5. Push Down Method/Attribute
6. Extract Superclass/Interface
7. Move Class

In order to build the project, import it to Eclipse IDE as a Java project, and install the Apache IvyDE plug-in.

## Contributors ##
The code in package **gr.uom.java.xmi** has been developed by **Nikolaos Tsantalis**.

The code in package **ca.ualberta.cs.data** has been developed by **Nikolaos Tsantalis** and **Fabio Rocha**.

The code in package **br.ufmg.dcc.labsoft.refactoringanalyzer** has been developed by **Danilo Ferreira e Silva**.

## API usage guidelines ##
```java
UMLModel model1 = new ASTReader(new File("/path/to/version1/")).getUmlModel();
UMLModel model2 = new ASTReader(new File("/path/to/version2/")).getUmlModel();
UMLModelDiff modelDiff = model1.diff(model2);
List<Refactoring> refactorings = modelDiff.getRefactorings();
```
