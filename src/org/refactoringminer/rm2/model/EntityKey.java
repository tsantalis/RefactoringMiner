package org.refactoringminer.rm2.model;

public class EntityKey {

  private final String key;

  public EntityKey(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return key;
  }

  @Override
  public int hashCode() {
    return this.key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof EntityKey)) {
      return false;
    }
    EntityKey other = (EntityKey) obj;
    return other.key.equals(key);
  }

  public String toName() {
    if (key.lastIndexOf('/') != -1) {
      return key.substring(key.lastIndexOf('/') + 1);
    }
    return key;
  }
}
