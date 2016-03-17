package org.refactoringminer.test;

import org.junit.Test;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;

public class TestRefDetector2Temp {

    @Test
	public void testInvalidInput() throws Exception {
	    TestBuilder test = new TestBuilder(new GitHistoryRefactoringMiner2(), "c:/Users/danilofs/tmp");
	    
//	    error at https://github.com/neo4j/neo4j/commit/7973485502c707739756a9e712b655f84cb6b492: java.lang.RuntimeException: org.eclipse.jdt.core.compiler.InvalidInputException: Invalid_Character_Constant
//        error at https://github.com/languagetool-org/languagetool/commit/bec15926deb49d2b3f7b979d4cfc819947a434ec: java.lang.RuntimeException: org.eclipse.jdt.core.compiler.InvalidInputException: Invalid_Character_Constant
	    test.project("https://github.com/languagetool-org/languagetool.git", "master") 
        .atCommit("bec15926deb49d2b3f7b979d4cfc819947a434ec").contains(
          "Move Attribute public VIDMINKY_MAP : Map<String,String> from class org.languagetool.tagging.uk.UkrainianTagger to class org.languagetool.tagging.uk.PosTagHelper")
        .atCommit("bec15926deb49d2b3f7b979d4cfc819947a434ec").notContains(
          "Inline Method private hasRequiredPosTag(posTagsToFind Collection<String>, tokenReadings AnalyzedTokenReadings) : boolean inlined to public match(text AnalyzedSentence) : RuleMatch[] in class org.languagetool.rules.uk.TokenAgreementRule");
	    
//        error at https://github.com/VoltDB/voltdb/commit/669e0722324965e3c99f29685517ac24d4ff2848: java.lang.RuntimeException: org.eclipse.jdt.core.compiler.InvalidInputException: Invalid_Character_Constant

	    test.assertExpectations();
	}

    @Test
    public void testNullPointer() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringMiner2(), "c:/Users/danilofs/tmp");
        
//        error at https://github.com/google/guava/commit/31fc19200207ccadc45328037d8a2a62b617c029: java.lang.NullPointerException
        test.project("https://github.com/google/guava.git", "master") 
        .atCommit("31fc19200207ccadc45328037d8a2a62b617c029").contains(
          "Extract Method public tryParse(string String, radix int) : Long extracted from public tryParse(string String) : Long in class com.google.common.primitives.Longs",
          "Move Attribute private asciiDigits : byte[] from class com.google.common.primitives.Ints to class com.google.common.primitives.Longs",
          "Move Method private digit(c char) : int from class com.google.common.primitives.Ints to private digit(c char) : int from class com.google.common.primitives.Longs")
        .atCommit("31fc19200207ccadc45328037d8a2a62b617c029").notContains(
          "Extract Method public tryParse(string String, radix int) : Integer extracted from package tryParse(string String, radix int) : Integer in class com.google.common.primitives.Ints");
        
//        error at https://github.com/VoltDB/voltdb/commit/05bd8ecda456e0901ef7375b9ff7b120ae668eca: java.lang.NullPointerException
//        test.project("https://github.com/VoltDB/voltdb.git", "master") 
//        .atCommit("05bd8ecda456e0901ef7375b9ff7b120ae668eca").contains(
//          "Move Class exportbenchmark.SocketExporter moved to exportbenchmark2.exporter.exportbenchmark.SocketExporter",
//          "Move Class exportbenchmark.NoOpExporter moved to exportbenchmark2.exporter.exportbenchmark.NoOpExporter",
//          "Move Class exportbenchmark.procedures.TruncateTables moved to exportbenchmark2.db.exportbenchmark.procedures.TruncateTables",
//          "Move Class exportbenchmark.procedures.SampleRecord moved to exportbenchmark2.db.exportbenchmark.procedures.SampleRecord",
//          "Move Class exportbenchmark.procedures.InsertExport5 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport5",
//          "Move Class exportbenchmark.procedures.InsertExport10 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport10",
//          "Move Class exportbenchmark.procedures.InsertExport1 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport1",
//          "Move Class exportbenchmark.procedures.InsertExport0 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport0",
//          "Move Class exportbenchmark.procedures.InsertExport moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport",
//          "Move Class exportbenchmark.Connect2Server moved to exportbenchmark2.client.exportbenchmark.Connect2Server")
//        .atCommit("05bd8ecda456e0901ef7375b9ff7b120ae668eca").notContains(
//          "Move Class exportbenchmark.ExportBenchmark.ExportCallback moved to exportbenchmark2.client.exportbenchmark.ExportBenchmark.ExportCallback",
//          "Move Class exportbenchmark.ExportBenchmark.ExportBenchConfig moved to exportbenchmark2.client.exportbenchmark.ExportBenchmark.ExportBenchConfig");
        
        test.assertExpectations();
    }

    @Test
    public void testUnsupportedOperation() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringMiner2(), "c:/Users/danilofs/tmp");
        
//        error at https://github.com/JetBrains/intellij-community/commit/3b1c3bad64e0e7cfabb5ca5d2be7b3aca0b7c197: java.lang.UnsupportedOperationException
        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
        .atCommit("3b1c3bad64e0e7cfabb5ca5d2be7b3aca0b7c197").notContains(
          "Move Class A moved to A",
          "Move Class EventManager moved to EventManager",
          "Move Class aaa.bbb.Margin moved to aaa.bbb.Margin",
          "Move Class Test moved to Test",
          "Move Class Doable moved to Doable",
          "Move Class ChangeFileTypeRequest moved to ChangeFileTypeRequest",
          "Move Class Percolation moved to Percolation");
        
//        error at https://github.com/JetBrains/intellij-community/commit/1b70adbfd49e00194c4c1170ef65e8114d7a2e46: java.lang.UnsupportedOperationException
        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
        .atCommit("1b70adbfd49e00194c4c1170ef65e8114d7a2e46").contains(
          "Extract Method private getFieldInitializerNullness(expression PsiExpression) : Nullness extracted from private calcInherentNullability() : Nullness in class com.intellij.codeInspection.dataFlow.value.DfaVariableValue");

//        error at https://github.com/JetBrains/intellij-community/commit/106d1d51754f454fa673976665e41f463316e084: java.lang.UnsupportedOperationException
        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
        .atCommit("106d1d51754f454fa673976665e41f463316e084").contains(
          "Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");
        
//        error at https://github.com/JetBrains/intellij-community/commit/526af6f1c1206b7f278459757f4b4b22b18069ea: java.lang.UnsupportedOperationException
        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
        .atCommit("526af6f1c1206b7f278459757f4b4b22b18069ea").notContains(
          "Inline Method private convertToJavaLambda(expression PsiExpression, streamApiMethodName String) : PsiExpression inlined to public convertToStream(expression PsiMethodCallExpression, method PsiMethod, force boolean) : PsiExpression in class com.intellij.codeInspection.java18StreamApi.PseudoLambdaReplaceTemplate");
        
//        error at https://github.com/JetBrains/intellij-community/commit/9f7de200c9aef900596b09327a52d33241a68d9c: java.lang.UnsupportedOperationException
        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
        .atCommit("9f7de200c9aef900596b09327a52d33241a68d9c").contains(
          "Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");

        
//        error at https://github.com/baasbox/baasbox/commit/d949fe9079a82ee31aa91244aa67baaf56b7e28f: java.lang.UnsupportedOperationException
        test.project("https://github.com/baasbox/baasbox.git", "master") 
        .atCommit("d949fe9079a82ee31aa91244aa67baaf56b7e28f").contains(
          "Extract Method public execMultiLineCommands(db ODatabaseRecordTx, log boolean, stopOnException boolean, commands String[]) : void extracted from public execMultiLineCommands(db ODatabaseRecordTx, log boolean, commands String) : void in class com.baasbox.db.DbHelper"); // bug varargs

//        error at https://github.com/apache/cassandra/commit/446e2537895c15b404a74107069a12f3fc404b15: java.lang.UnsupportedOperationException
        test.project("https://github.com/apache/cassandra.git", "master") 
        .atCommit("446e2537895c15b404a74107069a12f3fc404b15").contains(
          "Move Class org.apache.cassandra.hadoop.BulkRecordWriter.NullOutputHandler moved to org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter.NullOutputHandler",
          "Move Class org.apache.cassandra.hadoop.AbstractColumnFamilyInputFormat.SplitCallable moved to org.apache.cassandra.hadoop.cql3.CqlInputFormat.SplitCallable");

        
        test.assertExpectations();
    }
    
    @Test
    public void test() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringMiner2(), "c:/Users/danilofs/tmp");
        
//        error at https://github.com/JetBrains/intellij-community/commit/3b1c3bad64e0e7cfabb5ca5d2be7b3aca0b7c197: java.lang.UnsupportedOperationException
        test.project("https://github.com/danilofes/argouml-refactorings.git", "master") 
        .atCommit("26b089d0c3f74abadcf8645f4f7618bdc0c3738f")
        .containsOnly(
            "Push Down Method private onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to protected onButtonClicked(metaType Object) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
            "Push Down Method public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramInheritanceEdge to public layout() : void from class org.argouml.uml.diagram.static_structure.layout.ClassdiagramGeneralizationEdge",
            "Push Down Method public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.NotationProvider to public propertyChange(evt PropertyChangeEvent) : void from class org.argouml.notation.providers.ActionStateNotation",
            // the method was at SelectionClassifierBox, which is the superclass (it's wrong in the spreadsheet) "**Push Down Method org.argouml.uml.diagram.ui.FigEdgeModelElement mouseReleased org.argouml.uml.diagram.static_structure.ui.SelectionClass, org.argouml.uml.diagram.static_structure.ui.SelectionInterface, org.argouml.uml.diagram.ui.SelectionClassifierBox",
            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement",
//            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionInterface",
//            "Push Down Method public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.ui.SelectionClassifierBox to public mouseReleased(me MouseEvent) : void from class org.argouml.uml.diagram.static_structure.ui.SelectionClass",
            "Push Down Method public setTarget(t Object) : void from class org.argouml.ui.StylePanelFigNodeModelElement to public setTarget(t Object) : void from class org.argouml.uml.diagram.static_structure.ui.StylePanelFigClass",
            "Push Down Method public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizMEName to public setSuggestion(s String) : void from class org.argouml.uml.cognitive.critics.WizOperName",
            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizBreakCircularComp",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizNavigable",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddOperation",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddConstructor",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAssocComposite",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizAddInstanceVariable",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizMEName",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizTooMany",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizManyNames",
//            "Push Down Method public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.UMLWizard to public offerSuggestion() : String from class org.argouml.uml.cognitive.critics.WizCueCards",
            
            
            "Push Down Method protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionGeneralizableElement to protected getInstructions(i int) : String from class org.argouml.uml.diagram.static_structure.ui.SelectionDataType",
            "Push Down Method protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFig to protected getBBoxLabel() : JLabel from class org.argouml.ui.StylePanelFigNodeModelElement",
            "Push Down Method public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoPerspective to public setFlat(b boolean) : void from class org.argouml.cognitive.ui.ToDoByDecision");

        
        test.assertExpectations();
    }

}
