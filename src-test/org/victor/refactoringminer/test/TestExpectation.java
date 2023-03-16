package org.victor.refactoringminer.test;

import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;

public interface TestExpectation {
    void toHave(Map<RefactoringType, Integer> refactorings);

    void toHave(List<String> refactorings);
}
