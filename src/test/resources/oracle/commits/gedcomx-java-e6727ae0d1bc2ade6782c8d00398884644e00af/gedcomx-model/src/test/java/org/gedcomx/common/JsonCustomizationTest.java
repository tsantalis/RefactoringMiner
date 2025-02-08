package org.gedcomx.common;

import org.gedcomx.Gedcomx;
import org.gedcomx.rt.GedcomNamespaceManager;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.util.ArrayList;
import java.util.List;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ryan Heaton
 */
public class JsonCustomizationTest {

  /**
   * tests source reference xml
   */
  @Test
  public void testKeyedItemsXml() throws Exception {
    CustomEntity custom = new CustomEntity();
    custom.setRefToSomething(new org.gedcomx.common.URI("uri:hello"));

    custom.setUniqueKeyedItems(new ArrayList<UniqueCustomKeyedItem>());
    UniqueCustomKeyedItem item1 = new UniqueCustomKeyedItem();
    item1.setVal1("1");
    item1.setVal2("2");
    String key1 = item1.getKey();
    custom.getUniqueKeyedItems().add(item1);
    UniqueCustomKeyedItem item2 = new UniqueCustomKeyedItem();
    item2.setVal1("one");
    item2.setVal2("two");
    String key2 = item2.getKey();
    custom.getUniqueKeyedItems().add(item2);

    custom.setKeyedItems(new ArrayList<CustomKeyedItem>());
    CustomKeyedItem item3 = new CustomKeyedItem();
    item3.setVal1("1");
    item3.setVal2("2");
    String key3 = item3.getKey();
    custom.getKeyedItems().add(item3);
    CustomKeyedItem item4 = new CustomKeyedItem();
    item4.setVal1("one");
    item4.setVal2("two");
    item4.setKey(key3);
    custom.getKeyedItems().add(item4);

    custom = processThroughXml(custom);

    assertEquals("uri:hello", custom.getRefToSomething().toString());
    assertEquals(2, custom.getUniqueKeyedItems().size());
    assertEquals("1", custom.getUniqueKeyedItems().get(0).getVal1());
    assertEquals("2", custom.getUniqueKeyedItems().get(0).getVal2());
    assertEquals(key1, custom.getUniqueKeyedItems().get(0).getKey());
    assertEquals("one", custom.getUniqueKeyedItems().get(1).getVal1());
    assertEquals("two", custom.getUniqueKeyedItems().get(1).getVal2());
    assertEquals(key2, custom.getUniqueKeyedItems().get(1).getKey());
    assertEquals(2, custom.getKeyedItems().size());
    assertEquals("1", custom.getKeyedItems().get(0).getVal1());
    assertEquals("2", custom.getKeyedItems().get(0).getVal2());
    assertEquals(key3, custom.getKeyedItems().get(0).getKey());
    assertEquals("one", custom.getKeyedItems().get(1).getVal1());
    assertEquals("two", custom.getKeyedItems().get(1).getVal2());
    assertEquals(key3, custom.getKeyedItems().get(1).getKey());
  }

  /**
   * tests source reference json
   */
  @Test
  public void testKeyedItemsJson() throws Exception {
    CustomEntity custom = new CustomEntity();
    custom.setRefToSomething(new org.gedcomx.common.URI("uri:hello"));

    custom.setUniqueKeyedItems(new ArrayList<UniqueCustomKeyedItem>());
    UniqueCustomKeyedItem item1 = new UniqueCustomKeyedItem();
    item1.setVal1("1");
    item1.setVal2("2");
    String key1 = item1.getKey();
    custom.getUniqueKeyedItems().add(item1);
    UniqueCustomKeyedItem item2 = new UniqueCustomKeyedItem();
    item2.setVal1("one");
    item2.setVal2("two");
    String key2 = item2.getKey();
    custom.getUniqueKeyedItems().add(item2);

    custom.setKeyedItems(new ArrayList<CustomKeyedItem>());
    CustomKeyedItem item3 = new CustomKeyedItem();
    item3.setVal1("1");
    item3.setVal2("2");
    String key3 = item3.getKey();
    custom.getKeyedItems().add(item3);
    CustomKeyedItem item4 = new CustomKeyedItem();
    item4.setVal1("one");
    item4.setVal2("two");
    item4.setKey(key3);
    custom.getKeyedItems().add(item4);

    custom = processThroughJson(custom);
    assertEquals("uri:hello", custom.getRefToSomething().toString());
    assertEquals(2, custom.getUniqueKeyedItems().size());
    for (UniqueCustomKeyedItem keyedItem : custom.getUniqueKeyedItems()) {
      if ("1".equals(keyedItem.getVal1())) {
        assertEquals("2", keyedItem.getVal2());
        assertEquals(key1, keyedItem.getKey());
      }
      else if ("one".equals(keyedItem.getVal1())) {
        assertEquals("two", keyedItem.getVal2());
        assertEquals(key2, keyedItem.getKey());
      }
      else {
        fail("Unknown keyed item.");
      }
    }
    assertEquals(2, custom.getKeyedItems().size());
    for (CustomKeyedItem keyedItem : custom.getKeyedItems()) {
      if ("1".equals(keyedItem.getVal1())) {
        assertEquals("2", keyedItem.getVal2());
        assertEquals(key3, keyedItem.getKey());
      }
      else if ("one".equals(keyedItem.getVal1())) {
        assertEquals("two", keyedItem.getVal2());
        assertEquals(key3, keyedItem.getKey());
      }
      else {
        fail("Unknown keyed item.");
      }
    }
  }

  @Test
  public void testKeyedItemsAsExtensionsXml() throws Exception {
    Gedcomx set = new Gedcomx();
    UniqueCustomKeyedItem item1 = new UniqueCustomKeyedItem();
    item1.setVal1("1");
    item1.setVal2("2");
    String key1 = item1.getKey();
    set.addExtensionElement(item1);
    UniqueCustomKeyedItem item2 = new UniqueCustomKeyedItem();
    item2.setVal1("one");
    item2.setVal2("two");
    String key2 = item2.getKey();
    set.addExtensionElement(item2);

    CustomKeyedItem item3 = new CustomKeyedItem();
    item3.setVal1("1");
    item3.setVal2("2");
    String key3 = item3.getKey();
    set.addExtensionElement(item3);

    CustomKeyedItem item4 = new CustomKeyedItem();
    item4.setVal1("one");
    item4.setVal2("two");
    item4.setKey(key3);
    set.addExtensionElement(item4);

    set = processThroughXml(set, Gedcomx.class, JAXBContext.newInstance(Gedcomx.class, UniqueCustomKeyedItem.class, CustomKeyedItem.class));
    List<UniqueCustomKeyedItem> keyedItems = set.findExtensionsOfType(UniqueCustomKeyedItem.class);
    assertEquals(2, keyedItems.size());
    assertEquals("1", keyedItems.get(0).getVal1());
    assertEquals("2", keyedItems.get(0).getVal2());
    assertEquals(key1, keyedItems.get(0).getKey());
    assertEquals("one", keyedItems.get(1).getVal1());
    assertEquals("two", keyedItems.get(1).getVal2());
    assertEquals(key2, keyedItems.get(1).getKey());
    List<CustomKeyedItem> keyedItems2 = set.findExtensionsOfType(CustomKeyedItem.class);
    assertEquals("1", keyedItems2.get(0).getVal1());
    assertEquals("2", keyedItems2.get(0).getVal2());
    assertEquals(key3, keyedItems2.get(0).getKey());
    assertEquals("one", keyedItems2.get(1).getVal1());
    assertEquals("two", keyedItems2.get(1).getVal2());
    assertEquals(key3, keyedItems2.get(1).getKey());
  }

  @Test
  public void testKeyedItemsAsExtensionsJson() throws Exception {
    Gedcomx set = new Gedcomx();
    UniqueCustomKeyedItem item1 = new UniqueCustomKeyedItem();
    item1.setVal1("1");
    item1.setVal2("2");
    String key1 = item1.getKey();
    set.addExtensionElement(item1);
    UniqueCustomKeyedItem item2 = new UniqueCustomKeyedItem();
    item2.setVal1("one");
    item2.setVal2("two");
    String key2 = item2.getKey();
    set.addExtensionElement(item2);

    CustomKeyedItem item3 = new CustomKeyedItem();
    item3.setVal1("1");
    item3.setVal2("2");
    String key3 = item3.getKey();
    set.addExtensionElement(item3);

    CustomKeyedItem item4 = new CustomKeyedItem();
    item4.setVal1("one");
    item4.setVal2("two");
    item4.setKey(key3);
    set.addExtensionElement(item4);

    GedcomNamespaceManager.registerKnownJsonType(CustomKeyedItem.class);
    GedcomNamespaceManager.registerKnownJsonType(UniqueCustomKeyedItem.class);
    set = processThroughJson(set);
    List<UniqueCustomKeyedItem> keyedItems = set.findExtensionsOfType(UniqueCustomKeyedItem.class);
    assertEquals(2, keyedItems.size());
    for (UniqueCustomKeyedItem keyedItem : keyedItems) {
      if ("1".equals(keyedItem.getVal1())) {
        assertEquals("2", keyedItem.getVal2());
        assertEquals(key1, keyedItem.getKey());
      }
      else if ("one".equals(keyedItem.getVal1())) {
        assertEquals("two", keyedItem.getVal2());
        assertEquals(key2, keyedItem.getKey());
      }
      else {
        fail("Unknown keyed item.");
      }
    }
    List<CustomKeyedItem> keyedItems2 = set.findExtensionsOfType(CustomKeyedItem.class);
    assertEquals(2, keyedItems2.size());
    for (CustomKeyedItem keyedItem : keyedItems2) {
      if ("1".equals(keyedItem.getVal1())) {
        assertEquals("2", keyedItem.getVal2());
        assertEquals(key3, keyedItem.getKey());
      }
      else if ("one".equals(keyedItem.getVal1())) {
        assertEquals("two", keyedItem.getVal2());
        assertEquals(key3, keyedItem.getKey());
      }
      else {
        fail("Unknown keyed item.");
      }
    }
  }

}
