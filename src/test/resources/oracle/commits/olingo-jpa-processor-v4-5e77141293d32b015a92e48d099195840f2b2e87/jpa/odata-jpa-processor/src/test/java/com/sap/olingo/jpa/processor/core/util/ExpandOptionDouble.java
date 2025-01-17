package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;

public class ExpandOptionDouble implements ExpandOption {
  private final String text;
  private final List<ExpandItem> items;

  public ExpandOptionDouble(final String text, final List<ExpandItem> items) {
    super();
    this.text = text;
    this.items = items;
  }

  @Override
  public SystemQueryOptionKind getKind() {
    fail();
    return null;
  }

  @Override
  public String getName() {
    fail();
    return null;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public List<ExpandItem> getExpandItems() {
    return items;
  }

}
