package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.models.DiffInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import java.util.*;

public class DirComparator {
    private final Map<String, String> fileContentsBeforeMap;
    private final Map<String, String> fileContentsAfterMap;
    private final ProjectASTDiff projectASTDiff;


    private Set<String> removedFilesName;
    private Set<String> addedFilesName;
    private List<DiffInfo> diffInfos = new ArrayList<>();

    public Set<String> getRemovedFilesName() {
        return removedFilesName;
    }

    public Set<String> getAddedFilesName() {
        return addedFilesName;
    }

    public List<DiffInfo> getModifiedFilesName() {
        return diffInfos;
    }
    public Pair<String,String> getFileContentsPair(int id)
    {
        DiffInfo diffInfo = getDiffInfo(id);
        return new Pair<>(
                fileContentsBeforeMap.get(diffInfo.first),
                fileContentsAfterMap.get(diffInfo.second)
        );
    }

    public DirComparator(ProjectASTDiff projectASTDiff)
    {
        this.projectASTDiff = projectASTDiff;
        this.fileContentsAfterMap = projectASTDiff.getProjectData().getFileContentsCurrent();
        this.fileContentsBeforeMap = projectASTDiff.getProjectData().getFileContentsBefore();
        compare();
    }

    private void compare() {
        Set<String> beforeFiles = fileContentsBeforeMap.keySet();
        Set<String> afterFiles = fileContentsAfterMap.keySet();

        removedFilesName = new HashSet<>(beforeFiles);
        addedFilesName = new HashSet<>(afterFiles);

        for (DiffInfo diffInfo : projectASTDiff.getAstDiffMap().keySet()) {
            diffInfos.add(diffInfo);
            removedFilesName.remove(diffInfo.first);
            addedFilesName.remove(diffInfo.second);
        }
    }

    public DiffInfo getDiffInfo(int id) {
        return diffInfos.get(id);
    }
}
