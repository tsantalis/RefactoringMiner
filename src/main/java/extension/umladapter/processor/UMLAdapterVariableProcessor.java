package extension.umladapter.processor;

import java.util.Collections;

import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.expression.LangAssignment;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class UMLAdapterVariableProcessor {

    public static void processVariableDeclarations(LangSingleVariableDeclaration param, UMLParameter umlParam, UMLType typeObject, String sourceFolder, String filePath, VariableDeclarationContainer container, String fileContent){
        VariableDeclaration vd = new VariableDeclaration(
                param.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                param,
                container,
                Collections.emptyMap(),
                fileContent
        );

        vd.setAttribute(param.isAttribute());
        vd.setParameter(param.isParameter());

        umlParam.setVariableDeclaration(vd);
    }

    /**
     * Process assignment-based attribute declarations (self.attr = value)
     */
    public static VariableDeclaration processAttributeAssignment(LangAssignment assignment, String sourceFolder,
                                                                 String filePath, String attributeName,
                                                                 VariableDeclarationContainer container, String fileContent) {

        VariableDeclaration variableDeclaration = new VariableDeclaration(
                assignment.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                assignment,
                container,
                attributeName,
                Collections.emptyMap(),
                fileContent
        );

        // Mark as attribute (this should already be set by the constructor)
        variableDeclaration.setAttribute(true);

        return variableDeclaration;
    }

}
