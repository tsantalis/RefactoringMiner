package org.refactoringminer.utils;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringType;

public class RefactoringRelationshipGroup {

  private List<RefactoringRelationship> refactoringRelationships = new ArrayList<>();

  public RefactoringRelationshipGroup(RefactoringRelationship refactoringRelationship) {
    refactoringRelationships.add(refactoringRelationship);
  }

  public RefactoringType addRefactoringRelationship(RefactoringRelationship r) {
    if (r.getRefactoringType().equals(this.getRefactoringType()) && r.getMainEntity().equals(this.getMainEntity())) {
      refactoringRelationships.add(r);
    }
    throw new IllegalArgumentException(String.format("refactoring relatiships are note from the same group: [] []", r, refactoringRelationships.get(0)));
  }

  public RefactoringType getRefactoringType() {
    return refactoringRelationships.get(0).getRefactoringType();
  }

  public String getMainEntity() {
    return refactoringRelationships.get(0).getMainEntity();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RefactoringRelationshipGroup) {
      RefactoringRelationshipGroup other = (RefactoringRelationshipGroup) obj;
      return other.getRefactoringType().equals(this.getRefactoringType()) && other.getMainEntity().equals(this.getMainEntity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getMainEntity().hashCode();
    result = prime * result + getRefactoringType().hashCode();
    return result;
  }
}
