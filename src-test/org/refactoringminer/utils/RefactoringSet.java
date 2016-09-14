package org.refactoringminer.utils;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.refactoringminer.api.RefactoringType;

public class RefactoringSet {

  private final String project;
  private final String revision;
  private final Set<RefactoringRelationship> refactorings;
  
  public RefactoringSet(String project, String revision) {
    super();
    this.project = project;
    this.revision = revision;
    this.refactorings = new HashSet<>();
  }

  public String getProject() {
    return project;
  }

  public String getRevision() {
    return revision;
  }

  public Set<RefactoringRelationship> getRefactorings() {
    return refactorings;
  }

  public void add(RefactoringRelationship r) {
    this.refactorings.add(r);
  }

  public void add(Iterable<RefactoringRelationship> rs) {
    for (RefactoringRelationship r : rs) {
      this.refactorings.add(r);
    }
  }

  public CompareResult compareTo(RefactoringSet set) {
    CompareResult cr = new CompareResult(project, revision);
    for (RefactoringRelationship r : set.refactorings) {
      if (refactorings.contains(r)) {
        cr.addTruePositive(r);
      } else {
        cr.addFalsePositive(r);
      }
    }
    for (RefactoringRelationship r : refactorings) {
      if (!set.refactorings.contains(r)) {
        cr.addFalseNegative(r);
      }
    }
    return cr;
  }

  public RefactoringSet ignoring(EnumSet<RefactoringType> refTypes) {
    RefactoringSet newSet = new RefactoringSet(project, revision);
    newSet.add(refactorings.stream().filter(r ->
      !refTypes.contains(r.getRefactoringType())
    ).collect(Collectors.toList()));
    return newSet;
  }

}
