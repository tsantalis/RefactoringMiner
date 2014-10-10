package mains;

import gr.uom.java.xmi.ASTReader;
import gr.uom.java.xmi.UMLAssociation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.List;

/**
 * Receives two source folders as arguments and detects refactoring operations
 * performed between the given versions of the code.
 */
public class Main {

    public static void main(String[] args) {

        for (int i = 1; i <= 11; i++) {
           
            String version0SrcFolder = "test3/egrefctfowlerV" + i + "/org/ref";
            String version1SrcFolder = "test3/egrefctfowlerV" + (i + 1) + "/org/ref";

            UMLModel model0 = new ASTReader(new File(version0SrcFolder)).getUmlModel();
            UMLModel model1 = new ASTReader(new File(version1SrcFolder)).getUmlModel();

            UMLModelDiff modelDiff = model0.diff(model1);
            
            List<Refactoring> refactorings = modelDiff.getRefactorings();

            System.out.println("Comparação versões: " + version0SrcFolder + " e " + version1SrcFolder);
            for (Refactoring refactoring : refactorings) {
                System.out.println(refactoring.toString());
            }
            
            /*for (UMLClass uml : modelDiff.getAddedClasses()) {
                System.out.println("getAddedClasses: " + uml.getName());
               
                
                for(UMLOperation op : uml.getOperations()){
                    System.out.println("operações: " + op.getName());
                }
                
                System.out.println();
            }
            
            
            for (UMLAssociation ass : modelDiff.getAddedAssociations()) {
                System.out.println("association: " + ass.getEnd1());
               
                
               
                
                System.out.println();
            }*/
            
            System.out.println();

            //break;
        }
    }
}
