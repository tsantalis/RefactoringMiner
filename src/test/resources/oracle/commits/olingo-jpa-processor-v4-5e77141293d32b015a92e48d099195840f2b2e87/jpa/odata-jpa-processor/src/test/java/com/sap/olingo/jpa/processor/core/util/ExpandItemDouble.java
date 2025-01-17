package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.LevelsExpandOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

public class ExpandItemDouble implements ExpandItem {
  private UriResourceNavigation target;

  public ExpandItemDouble(final EdmEntityType naviTargetEntity) {
    target = new UriResourceNavigationDouble(naviTargetEntity, new EdmNavigationPropertyDouble(
        naviTargetEntity.getName()));
  }

  @Override
  public LevelsExpandOption getLevelsOption() {
    return null;
  }

  @Override
  public FilterOption getFilterOption() {
    fail();
    return null;
  }

  @Override
  public SearchOption getSearchOption() {
    fail();
    return null;
  }

  @Override
  public OrderByOption getOrderByOption() {
    fail();
    return null;
  }

  @Override
  public SkipOption getSkipOption() {
    fail();
    return null;
  }

  @Override
  public TopOption getTopOption() {
    fail();
    return null;
  }

  @Override
  public CountOption getCountOption() {
    fail();
    return null;
  }

  @Override
  public SelectOption getSelectOption() {
    fail();
    return null;
  }

  @Override
  public ExpandOption getExpandOption() {
    fail();
    return null;
  }

  @Override
  public UriInfoResource getResourcePath() {
    return new UriInfoResourceDouble(target);
  }

  @Override
  public boolean isStar() {
    return false;
  }

  @Override
  public boolean isRef() {
    fail();
    return false;
  }

  @Override
  public EdmType getStartTypeFilter() {
    fail();
    return null;
  }

  @Override
  public boolean hasCountPath() {
    fail();
    return false;
  }

  @Override
  public ApplyOption getApplyOption() {
    return null;
  }

}
