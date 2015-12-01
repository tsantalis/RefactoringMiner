package br.ufmg.dcc.labsoft.refdetector.model.refactoring;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.Filter;
import br.ufmg.dcc.labsoft.refdetector.model.Multiset;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;

public class SDExtractMethod extends SDRefactoring {

    private final SDMethod extractedMethod;
    
    public SDExtractMethod(SDMethod extractedMethod) {
        super(RefactoringType.EXTRACT_OPERATION, extractedMethod);
        this.extractedMethod = extractedMethod;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(this.extractedMethod.getVerboseSimpleName());
        sb.append(" extracted from ");
        Multiset<SDMethod> sameClassSources = extractedMethod.origins().suchThat(new Filter<SDMethod>(){
            public boolean accept(SDMethod m) {
                return m.container().equals(extractedMethod.container());
            }
        });
        SDMethod source;
        if (sameClassSources.size() > 0) {
            source = sameClassSources.getFirst();
        } else {
            source = extractedMethod.origins().getFirst();
        }
        sb.append(source.getVerboseSimpleName());
        sb.append(" in class ");
        sb.append(source.container().fullName());
        return sb.toString();
    }
}
