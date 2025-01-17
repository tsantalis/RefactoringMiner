package com.sap.olingo.jpa.processor.core.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationPropertyBinding;
import org.apache.olingo.server.api.uri.UriParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestEdmEntitySetResult {
  private EdmEntitySetResult cut;
  private List<UriParameter> keys;
  private EdmEntitySet es;
  private EdmEntitySet est;

  @BeforeEach
  public void setup() {
    keys = new ArrayList<>();
    es = mock(EdmEntitySet.class);
    when(es.getName()).thenReturn("Persons");
    est = mock(EdmEntitySet.class);
    when(est.getName()).thenReturn("BusinessPartnerRoles");
  }

  @Test
  public void testGetEntitySetName() {
    cut = new EdmEntitySetResult(es, keys, "");
    assertEquals("Persons", cut.getName());
  }

  @Test
  public void testGetEntitySetGetKeys() {
    final UriParameter key = mock(UriParameter.class);
    when(key.getName()).thenReturn("ID");
    keys.add(key);
    cut = new EdmEntitySetResult(es, keys, "");
    assertEquals(keys, cut.getKeyPredicates());
  }

  @Test
  public void testGetEntitySetGet() {
    cut = new EdmEntitySetResult(es, keys, "Roles");
    assertEquals("Roles", cut.getNavigationPath());
  }

  @Test
  public void testDetermineTargetEntitySetWithNaviNull() {
    when(es.getNavigationPropertyBindings()).thenReturn(null);
    cut = new EdmEntitySetResult(es, keys, null);
    assertEquals(es, cut.getTargetEdmEntitySet());
  }

  @Test
  public void testDetermineTargetEntitySetWithNaviEmpty() {
    when(es.getNavigationPropertyBindings()).thenReturn(null);
    cut = new EdmEntitySetResult(es, keys, "");
    assertEquals(es, cut.getTargetEdmEntitySet());
  }

// return edmEntitySet.getEntityContainer().getEntitySet(navi.getTarget());
  @Test
  public void testDetermineTargetEntitySetWithNavigation() {
    final EdmEntityContainer container = mock(EdmEntityContainer.class);
    final List<EdmNavigationPropertyBinding> bindings = new ArrayList<>(2);
    EdmNavigationPropertyBinding binding = mock(EdmNavigationPropertyBinding.class);
    bindings.add(binding);
    when(binding.getPath()).thenReturn("InhouseAddress");

    binding = mock(EdmNavigationPropertyBinding.class);
    bindings.add(binding);
    when(binding.getPath()).thenReturn("Roles");
    when(binding.getTarget()).thenReturn("BusinessPartnerRoles");
    when(es.getEntityContainer()).thenReturn(container);
    when(es.getNavigationPropertyBindings()).thenReturn(bindings);
    when(container.getEntitySet("BusinessPartnerRoles")).thenReturn(est);

    cut = new EdmEntitySetResult(es, keys, "Roles");
    assertEquals(es, cut.getEdmEntitySet());
    assertEquals(est, cut.getTargetEdmEntitySet());
  }
}
