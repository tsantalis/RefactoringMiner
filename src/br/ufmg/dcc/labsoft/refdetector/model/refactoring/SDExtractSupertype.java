package br.ufmg.dcc.labsoft.refdetector.model.refactoring;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class SDExtractSupertype extends SDRefactoring {

    private final SDType extractedMethod;
    
    public SDExtractSupertype(SDType extractedType) {
        super(extractedType.isInterface() ? RefactoringType.EXTRACT_INTERFACE : RefactoringType.EXTRACT_SUPERCLASS, extractedType);
        this.extractedMethod = extractedType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(this.extractedMethod.fullName());
        sb.append(" from classes ");
        sb.append(extractedMethod.origins().asSet().toString());
        return sb.toString();
    }
}
