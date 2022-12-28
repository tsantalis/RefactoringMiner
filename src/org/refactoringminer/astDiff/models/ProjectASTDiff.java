package org.refactoringminer.astDiff.models;

import org.refactoringminer.astDiff.models.DiffInfo;
import org.refactoringminer.astDiff.models.ProjectData;
import org.refactoringminer.astDiff.actions.ASTDiff;

import java.util.HashMap;
import java.util.Map;
public class ProjectASTDiff {

    private final Map<DiffInfo, ASTDiff> astDiffMap;
    private ProjectData projectData;
    public ProjectASTDiff(ProjectData projectData)
    {
        this.astDiffMap = new HashMap<>();
        this.projectData = projectData;
    }

    public Map<DiffInfo, ASTDiff> getAstDiffMap() {
        return astDiffMap;
    }

    public ASTDiff getASTDiff(DiffInfo diffInfo)
    {
        for (Map.Entry<DiffInfo,ASTDiff> mapped : astDiffMap.entrySet())
            if (mapped.getKey().first.equals(diffInfo.first) && mapped.getKey().second.equals(diffInfo.second))
                return mapped.getValue();
        return null;
    }
    public boolean isASTDiffAvailable(DiffInfo diffInfo) {
        for (DiffInfo dInfo : astDiffMap.keySet())
            if (dInfo.first.equals(diffInfo.first) && dInfo.second.equals(diffInfo.second))
                return true;
        return false;
    }

    public void addASTDiff(DiffInfo diffInfo, ASTDiff astDiff)
    {
        if (this.isASTDiffAvailable(diffInfo)) {
            this.getASTDiff(diffInfo).getMappings().mergeMappings(astDiff.getMappings());
        }
        else
            this.astDiffMap.put(diffInfo, astDiff);
    }

    public ProjectData getProjectData() {
        return projectData;
    }

    private void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

}
