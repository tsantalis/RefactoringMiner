package org.refactoringminer.utils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.utils.RefactoringRelationship.GroupKey;

public class RefactoringSet {

  private final String project;
  private final String revision;
  private final Set<RefactoringRelationship> refactorings;
  private final Map<RefactoringRelationship.GroupKey, Set<RefactoringRelationship>> refactoringGroups;
  
  public RefactoringSet(String project, String revision) {
    super();
    this.project = project;
    this.revision = revision;
    this.refactorings = new HashSet<>();
    this.refactoringGroups = new HashMap<>();
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
    GroupKey groupKey = r.getGroupKey();
    Set<RefactoringRelationship> group = refactoringGroups.get(groupKey);
    if (group == null) {
      group = new HashSet<>();
      refactoringGroups.put(groupKey, group);
    }
    group.add(r);
  }

  public void add(Iterable<RefactoringRelationship> rs) {
    for (RefactoringRelationship r : rs) {
      this.add(r);
    }
  }

  public CompareResult compareTo(RefactoringSet set, boolean grouped) {
    if (grouped) {
      return new CompareResult(project, revision, refactoringGroups.keySet(), set.refactoringGroups.keySet());
    } else {
      return new CompareResult(project, revision, refactorings, set.refactorings);
    }
  }

  public RefactoringSet ignoring(EnumSet<RefactoringType> refTypes) {
    RefactoringSet newSet = new RefactoringSet(project, revision);
    newSet.add(refactorings.stream().filter(r ->
      !refTypes.contains(r.getRefactoringType())
    ).collect(Collectors.toList()));
    return newSet;
  }

}
