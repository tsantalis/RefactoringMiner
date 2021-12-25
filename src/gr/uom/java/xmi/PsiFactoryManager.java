package gr.uom.java.xmi;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiFileFactory;

public class PsiFactoryManager {
    private static final PsiFileFactory factory =
        PsiFileFactory.getInstance(ProjectManager.getInstance().getDefaultProject());

    private PsiFactoryManager() {}

    public static PsiFileFactory getFactory() {
        return factory;
    }
}
