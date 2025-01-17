package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.DeltaTokenOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.FormatOption;
import org.apache.olingo.server.api.uri.queryoption.IdOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.SkipTokenOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

public class UriInfoResourceDouble implements UriInfoResource {
  private final List<UriResource> resources;

  public UriInfoResourceDouble(UriResourceNavigation target) {
    resources = new ArrayList<>();
    resources.add(target);
  }

  @Override
  public List<CustomQueryOption> getCustomQueryOptions() {
    fail();
    return null;
  }

  @Override
  public ExpandOption getExpandOption() {
    fail();
    return null;
  }

  @Override
  public FilterOption getFilterOption() {
    return null;
  }

  @Override
  public FormatOption getFormatOption() {
    fail();
    return null;
  }

  @Override
  public IdOption getIdOption() {
    fail();
    return null;
  }

  @Override
  public CountOption getCountOption() {
    fail();
    return null;
  }

  @Override
  public OrderByOption getOrderByOption() {
    fail();
    return null;
  }

  @Override
  public SearchOption getSearchOption() {
    fail();
    return null;
  }

  @Override
  public SelectOption getSelectOption() {
    fail();
    return null;
  }

  @Override
  public SkipOption getSkipOption() {
    fail();
    return null;
  }

  @Override
  public SkipTokenOption getSkipTokenOption() {
    fail();
    return null;
  }

  @Override
  public TopOption getTopOption() {
    fail();
    return null;
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return resources;
  }

  @Override
  public String getValueForAlias(String alias) {
    fail();
    return null;
  }

  @Override
  public ApplyOption getApplyOption() {
    return null;
  }

  @Override
  public DeltaTokenOption getDeltaTokenOption() {
    return null;
  }

}
