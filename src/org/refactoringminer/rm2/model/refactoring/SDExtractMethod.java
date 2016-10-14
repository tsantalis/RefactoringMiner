package org.refactoringminer.rm2.model.refactoring;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.SDMethod;

public class SDExtractMethod extends SDRefactoring {

    private final SDMethod extractedMethod;
    private final SDMethod origin;
    
    public SDExtractMethod(SDMethod extractedMethod, SDMethod origin) {
        super(RefactoringType.EXTRACT_OPERATION, extractedMethod, origin, extractedMethod);
        this.extractedMethod = extractedMethod;
        this.origin = origin;
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(this.getName());
//        sb.append(' ');
//        sb.append(this.extractedMethod.getVerboseSimpleName());
//        sb.append(" extracted from ");
//        Multiset<SDMethod> sameClassSources = extractedMethod.origins().suchThat(new Filter<SDMethod>(){
//            public boolean accept(SDMethod m) {
//                return m.container().equals(extractedMethod.container());
//            }
//        });
//        SDMethod source;
//        if (sameClassSources.size() > 0) {
//            source = sameClassSources.getFirst();
//        } else {
//            source = extractedMethod.origins().getFirst();
//        }
//        sb.append(source.getVerboseSimpleName());
//        sb.append(" in class ");
//        sb.append(source.container().fullName());
//        return sb.toString();
//    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(this.extractedMethod.getVerboseSimpleName());
        sb.append(" extracted from ");
        sb.append(origin.getVerboseSimpleName());
        sb.append(" in class ");
        sb.append(origin.container().fullName());
        return sb.toString();
    }
}
