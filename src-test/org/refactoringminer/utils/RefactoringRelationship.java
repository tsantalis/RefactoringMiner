package org.refactoringminer.utils;

import org.refactoringminer.api.RefactoringType;

public class RefactoringRelationship implements Comparable<RefactoringRelationship> {

  private final RefactoringType refactoringType;
  private final String entityBefore;
  private final String entityAfter;

  public RefactoringRelationship(RefactoringType refactoringType, String entityBefore, String entityAfter) {
    if (refactoringType == null || entityBefore == null || entityAfter == null) {
      throw new IllegalArgumentException("arguments should not be null");
    }
    this.refactoringType = refactoringType;
    this.entityBefore = normalize(entityBefore).trim();
    this.entityAfter = normalize(entityAfter).trim();
  }

  public RefactoringType getRefactoringType() {
    return refactoringType;
  }

  public String getEntityBefore() {
    return entityBefore;
  }

  public String getEntityAfter() {
    return entityAfter;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RefactoringRelationship) {
      RefactoringRelationship other = (RefactoringRelationship) obj;
      return other.refactoringType.equals(this.refactoringType) && other.entityBefore.equals(this.entityBefore) && other.entityAfter.equals(this.entityAfter);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entityAfter.hashCode();
    result = prime * result + entityBefore.hashCode();
    result = prime * result + refactoringType.hashCode();
    return result;
  }
  
  @Override
  public String toString() {
    return String.format("%s : %s %s", refactoringType.getDisplayName(), entityBefore, entityAfter);
  }

  public static String normalize(String entity) {
    return stripQualifiedNamesFromParamTypes(stripTypeArguments(entity).replace('#', '.').replace(" ", ""));
  }

  private static String stripQualifiedNamesFromParamTypes(String r) {
    int indexOfPar = r.indexOf('(');
    if (indexOfPar != -1 && r.lastIndexOf('.') > indexOfPar) {
      String paramsS = r.substring(indexOfPar + 1, r.indexOf(')'));
      String[] paramsA = paramsS.split(",");
      for (int i = 0; i < paramsA.length; i++) {
        paramsA[i] = paramsA[i].substring(Math.max(paramsA[i].lastIndexOf('.') + 1, 0));
      }
      r = r.substring(0, indexOfPar) + "(" + String.join(",", paramsA) + ")";
    }
    return r;
  }

  private static String stripTypeArguments(String entity) {
    StringBuilder sb = new StringBuilder();
    int openGenerics = 0;
    for (int i = 0; i < entity.length(); i++) {
      char c = entity.charAt(i);
      if (c == '<') {
        openGenerics++;
      }
      if (openGenerics == 0) {
        sb.append(c);
      }
      if (c == '>') {
        openGenerics--;
      }
    }
    return sb.toString();
  }

  @Override
  public int compareTo(RefactoringRelationship o) {
    int cb = entityBefore.compareTo(o.entityBefore);
    int ca = entityAfter.compareTo(o.entityAfter);
    int ct = refactoringType.compareTo(o.refactoringType);
    return cb != 0 ? cb : ca != 0 ? ca : ct;
  }

}
