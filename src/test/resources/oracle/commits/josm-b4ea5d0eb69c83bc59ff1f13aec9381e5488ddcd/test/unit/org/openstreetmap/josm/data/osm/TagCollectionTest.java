// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link TagCollection}.
 * @author Michael Zangl
 */
// We need prefs for using primitives
@BasicPreferences
class TagCollectionTest {
    private final Tag tagA = new Tag("k", "v");
    private final Tag tagB = new Tag("k", "b");
    private final Tag tagC = new Tag("k2", "b");
    private final Tag tagD = new Tag("k3", "c");
    private final Tag tagEmpty = new Tag("k", "");
    private final Tag tagNullKey = new Tag(null, "b");
    private final Tag tagNullValue = new Tag("k2", null);

    private void assertTagCounts(TagCollection collection, int a, int b, int c, int d) {
        assertEquals(a, collection.getTagOccurrence(tagA));
        assertEquals(b, collection.getTagOccurrence(tagB));
        assertEquals(c, collection.getTagOccurrence(tagC));
        assertEquals(d, collection.getTagOccurrence(tagD));
    }

    /**
     * Test method for {@link TagCollection#from(org.openstreetmap.josm.data.osm.Tagged)}.
     */
    @Test
    void testFromTagged() {
        TagCollection c = TagCollection.from(tagA);
        assertTagCounts(c, 1, 0, 0, 0);

        NodeData p1 = new NodeData();
        p1.put(tagA);
        p1.put(tagC);
        TagCollection d = TagCollection.from(p1);
        assertTagCounts(d, 1, 0, 1, 0);

        TagCollection e = TagCollection.from((Tagged) null);
        assertTagCounts(e, 0, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#from(Map)}.
     */
    @Test
    void testFromMapOfStringString() {
        TagCollection c = TagCollection.from(tagA.getKeys());
        assertTagCounts(c, 1, 0, 0, 0);

        HashMap<String, String> map = new HashMap<>();
        map.putAll(tagA.getKeys());
        map.putAll(tagC.getKeys());
        TagCollection d = TagCollection.from(map);
        assertTagCounts(d, 1, 0, 1, 0);

        TagCollection e = TagCollection.from((Map<String, String>) null);
        assertTagCounts(e, 0, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#unionOfAllPrimitives(Collection)}.
     */
    @Test
    void testUnionOfAllPrimitivesCollectionOfQextendsTagged() {
        TagCollection c = TagCollection.unionOfAllPrimitives(Arrays.asList(tagA));
        assertEquals(1, c.getTagOccurrence(tagA));

        TagCollection d = TagCollection.unionOfAllPrimitives(Arrays.asList(tagA, tagC));
        assertTagCounts(d, 1, 0, 1, 0);

        TagCollection e = TagCollection.unionOfAllPrimitives((Collection<? extends Tagged>) null);
        assertTagCounts(e, 0, 0, 0, 0);

        TagCollection f = TagCollection.unionOfAllPrimitives(Arrays.<Tagged>asList());
        assertTagCounts(f, 0, 0, 0, 0);

        TagCollection g = TagCollection.unionOfAllPrimitives(Arrays.asList(tagA, tagC, tagC, null));
        assertTagCounts(g, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#TagCollection()}.
     */
    @Test
    void testTagCollection() {
        TagCollection c = new TagCollection();
        assertTagCounts(c, 0, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#TagCollection(TagCollection)}.
     */
    @Test
    void testTagCollectionTagCollection() {
        TagCollection blueprint = TagCollection.unionOfAllPrimitives(Arrays.asList(tagA, tagC, tagC));
        TagCollection c = new TagCollection(blueprint);
        assertTagCounts(c, 1, 0, 2, 0);

        TagCollection d = new TagCollection((TagCollection) null);
        assertTagCounts(d, 0, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#TagCollection(Collection)}.
     */
    @Test
    void testTagCollectionCollectionOfTag() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertTagCounts(c, 1, 0, 2, 0);

        TagCollection d = new TagCollection((Collection<Tag>) null);
        assertTagCounts(d, 0, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#size()}.
     */
    @Test
    void testSize() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertEquals(2, c.size());

        TagCollection d = new TagCollection();
        assertEquals(0, d.size());
    }

    /**
     * Test method for {@link TagCollection#isEmpty()}.
     */
    @Test
    void testIsEmpty() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertFalse(c.isEmpty());

        TagCollection d = new TagCollection();
        assertTrue(d.isEmpty());
    }

    /**
     * Test method for {@link TagCollection#add(Tag)}.
     */
    @Test
    void testAddTag() {
        TagCollection c = new TagCollection();
        assertTagCounts(c, 0, 0, 0, 0);
        c.add(tagC);
        assertTagCounts(c, 0, 0, 1, 0);
        c.add(tagA);
        c.add(tagC);
        assertTagCounts(c, 1, 0, 2, 0);
        c.add((Tag) null);
        assertTagCounts(c, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#getTagOccurrence}.
     */
    @Test
    void testGetTagCount() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertEquals(2, c.getTagOccurrence(tagC));
        assertEquals(0, c.getTagOccurrence(tagB));
        assertEquals(0, c.getTagOccurrence(tagNullKey));
        assertEquals(0, c.getTagOccurrence(tagNullValue));
    }

    /**
     * Test method for {@link TagCollection#add(Collection)}.
     */
    @Test
    void testAddCollectionOfTag() {
        TagCollection c = new TagCollection();
        assertTagCounts(c, 0, 0, 0, 0);
        c.add(Arrays.asList(tagC));
        assertTagCounts(c, 0, 0, 1, 0);
        c.add(Arrays.asList(tagA, tagC));
        assertTagCounts(c, 1, 0, 2, 0);
        c.add(Collections.emptyList());
        assertTagCounts(c, 1, 0, 2, 0);
        c.add((Collection<Tag>) null);
        assertTagCounts(c, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#add(TagCollection)}.
     */
    @Test
    void testAddTagCollection() {
        TagCollection c = new TagCollection();
        assertTagCounts(c, 0, 0, 0, 0);
        c.add(new TagCollection(Arrays.asList(tagC)));
        assertTagCounts(c, 0, 0, 1, 0);
        c.add(new TagCollection(Arrays.asList(tagA, tagC)));
        assertTagCounts(c, 1, 0, 2, 0);
        c.add(new TagCollection());
        assertTagCounts(c, 1, 0, 2, 0);
        c.add((TagCollection) null);
        assertTagCounts(c, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#remove(Tag)}.
     */
    @Test
    void testRemoveTag() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertTagCounts(c, 1, 0, 2, 0);
        c.remove(tagC);
        assertTagCounts(c, 1, 0, 0, 0);
        c.remove(tagB);
        assertTagCounts(c, 1, 0, 0, 0);
        c.remove((Tag) null);
        assertTagCounts(c, 1, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#remove(Collection)}.
     */
    @Test
    void testRemoveCollectionOfTag() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertTagCounts(c, 1, 0, 2, 0);
        c.remove(Arrays.asList(tagC, tagB));
        assertTagCounts(c, 1, 0, 0, 0);
        c.remove((Collection<Tag>) null);
        assertTagCounts(c, 1, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#remove(TagCollection)}.
     */
    @Test
    void testRemoveTagCollection() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagC));
        assertTagCounts(c, 1, 0, 2, 0);
        c.remove(new TagCollection(Arrays.asList(tagC, tagB)));
        assertTagCounts(c, 1, 0, 0, 0);
        c.remove(new TagCollection());
        assertTagCounts(c, 1, 0, 0, 0);
        c.remove((TagCollection) null);
        assertTagCounts(c, 1, 0, 0, 0);
    }

    /**
     * Test method for {@link TagCollection#removeByKey(String)}.
     */
    @Test
    void testRemoveByKeyString() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagB, tagC));
        assertTagCounts(c, 1, 2, 1, 0);
        c.removeByKey("k");
        assertTagCounts(c, 0, 0, 1, 0);
        c.removeByKey((String) null);
        assertTagCounts(c, 0, 0, 1, 0);
    }

    /**
     * Test method for {@link TagCollection#removeByKey(Collection)}.
     */
    @Test
    void testRemoveByKeyCollectionOfString() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagB, tagC, tagD));
        assertTagCounts(c, 1, 2, 1, 1);
        c.removeByKey(Arrays.asList("k", "k2", null));
        assertTagCounts(c, 0, 0, 0, 1);
        c.removeByKey((Collection<String>) null);
        assertTagCounts(c, 0, 0, 0, 1);
    }

    /**
     * Test method for {@link TagCollection#contains(Tag)}.
     */
    @Test
    void testContains() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagB));
        assertTrue(c.contains(tagA));
        assertTrue(c.contains(tagB));
        assertFalse(c.contains(tagC));
    }

    /**
     * Test method for {@link TagCollection#containsAll(Collection)}.
     */
    @Test
    void testContainsAll() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagB));
        assertTrue(c.containsAll(Arrays.asList(tagA, tagB)));
        assertFalse(c.containsAll(Arrays.asList(tagA, tagC)));
        assertTrue(c.containsAll(Arrays.asList()));
        assertFalse(c.containsAll(null));
    }

    /**
     * Test method for {@link TagCollection#containsAllKeys(Collection)}.
     */
    @Test
    void testContainsAllKeys() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagC));
        assertTrue(c.containsAllKeys(Arrays.asList("k", "k2")));
        assertFalse(c.containsAllKeys(Arrays.asList("k", "k3")));
        assertTrue(c.containsAllKeys(Arrays.asList()));
        assertFalse(c.containsAllKeys(null));
    }

    /**
     * Test method for {@link TagCollection#getNumTagsFor(String)}.
     */
    @Test
    void testGetNumTagsFor() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagC));
        assertEquals(2, c.getNumTagsFor("k"));
        assertEquals(1, c.getNumTagsFor("k2"));
        assertEquals(0, c.getNumTagsFor("k3"));
    }

    /**
     * Test method for {@link TagCollection#hasTagsFor(String)}.
     */
    @Test
    void testHasTagsFor() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagB, tagC));
        assertTrue(c.hasTagsFor("k"));
        assertTrue(c.hasTagsFor("k2"));
        assertFalse(c.hasTagsFor("k3"));
    }

    /**
     * Test method for {@link TagCollection#hasValuesFor(String)}.
     */
    @Test
    void testHasValuesFor() {
        TagCollection c = new TagCollection(Arrays.asList(tagC, tagEmpty));
        assertFalse(c.hasValuesFor("k"));
        assertTrue(c.hasValuesFor("k2"));
        assertFalse(c.hasValuesFor("k3"));
    }

    /**
     * Test method for {@link TagCollection#hasUniqueNonEmptyValue(String)}.
     */
    @Test
    void testHasUniqueNonEmptyValue() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagEmpty));
        assertTrue(c.hasUniqueNonEmptyValue("k"));
        assertTrue(c.hasUniqueNonEmptyValue("k2"));
        assertFalse(c.hasUniqueNonEmptyValue("k3"));

        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagEmpty));
        assertFalse(d.hasUniqueNonEmptyValue("k"));
        assertTrue(d.hasUniqueNonEmptyValue("k2"));
        assertFalse(d.hasUniqueNonEmptyValue("k3"));
    }

    /**
     * Test method for {@link TagCollection#hasEmptyValue(String)}.
     */
    @Test
    void testHasEmptyValue() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC, tagEmpty));
        assertTrue(c.hasEmptyValue("k"));
        assertFalse(c.hasEmptyValue("k2"));
        assertFalse(c.hasEmptyValue("k3"));
    }

    /**
     * Test method for {@link TagCollection#hasUniqueEmptyValue(String)}.
     */
    @Test
    void testHasUniqueEmptyValue() {
        TagCollection c = new TagCollection(Arrays.asList(tagC, tagEmpty));
        assertTrue(c.hasUniqueEmptyValue("k"));
        assertFalse(c.hasUniqueEmptyValue("k2"));
        assertFalse(c.hasUniqueEmptyValue("k3"));

        TagCollection d = new TagCollection(Arrays.asList());
        assertFalse(d.hasUniqueEmptyValue("k"));
        assertFalse(d.hasUniqueEmptyValue("k2"));
        assertFalse(d.hasUniqueEmptyValue("k3"));
    }

    /**
     * Test method for {@link TagCollection#getTagsFor(String)}.
     */
    @Test
    void testGetTagsForString() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagEmpty));
        TagCollection collection = d.getTagsFor("k");
        assertTagCounts(collection, 1, 1, 0, 0);
        assertEquals(1, collection.getTagOccurrence(tagEmpty));
    }

    /**
     * Test method for {@link TagCollection#getTagsFor(Collection)}.
     */
    @Test
    void testGetTagsForCollectionOfString() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagEmpty));
        TagCollection collection = d.getTagsFor(Arrays.asList("k", "k2"));
        assertTagCounts(collection, 1, 1, 1, 0);
        assertEquals(1, collection.getTagOccurrence(tagEmpty));
    }

    /**
     * Test method for {@link TagCollection#asSet()}.
     */
    @Test
    void testAsSet() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagC));
        Set<Tag> set = d.asSet();
        assertEquals(3, set.size());
        assertTrue(set.contains(tagA));
        assertTrue(set.contains(tagB));
        assertTrue(set.contains(tagC));
    }

    /**
     * Test method for {@link TagCollection#asList()}.
     */
    @Test
    void testAsList() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagC));
        List<Tag> set = d.asList();
        assertEquals(3, set.size());
        assertTrue(set.contains(tagA));
        assertTrue(set.contains(tagB));
        assertTrue(set.contains(tagC));
    }

    /**
     * Test method for {@link TagCollection#iterator()}.
     */
    @Test
    void testIterator() {
        TagCollection d = new TagCollection(Arrays.asList(tagA));
        Iterator<Tag> it = d.iterator();
        assertTrue(it.hasNext());
        assertEquals(tagA, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Test method for {@link TagCollection#getKeys()}.
     */
    @Test
    void testGetKeys() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagC));
        Set<String> set = d.getKeys();
        assertEquals(2, set.size());
        assertTrue(set.contains("k"));
        assertTrue(set.contains("k2"));
    }

    /**
     * Test method for {@link TagCollection#getKeysWithMultipleValues()}.
     */
    @Test
    void testGetKeysWithMultipleValues() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagB, tagC, tagC));
        Set<String> set = d.getKeysWithMultipleValues();
        assertEquals(1, set.size());
        assertTrue(set.contains("k"));
    }

    /**
     * Test method for {@link TagCollection#setUniqueForKey(Tag)}.
     */
    @Test
    void testSetUniqueForKeyTag() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagA, tagB, tagC, tagC));
        assertTagCounts(d, 2, 1, 2, 0);
        d.setUniqueForKey(tagA);
        assertTagCounts(d, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#setUniqueForKey(String, String)}.
     */
    @Test
    void testSetUniqueForKeyStringString() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagA, tagB, tagC, tagC));
        assertTagCounts(d, 2, 1, 2, 0);
        d.setUniqueForKey(tagA.getKey(), tagA.getValue());
        assertTagCounts(d, 1, 0, 2, 0);
    }

    /**
     * Test method for {@link TagCollection#getValues()}.
     */
    @Test
    void testGetValues() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagA, tagB, tagC, tagEmpty));
        Set<String> set = d.getValues();
        assertEquals(3, set.size());
        assertTrue(set.contains("v"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(""));
    }

    /**
     * Test method for {@link TagCollection#getValues(String)}.
     */
    @Test
    void testGetValuesString() {
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagA, tagC, tagEmpty));
        Set<String> set = d.getValues("k");
        assertEquals(2, set.size());
        assertTrue(set.contains("v"));
        assertTrue(set.contains(""));
    }

    /**
     * Test method for {@link TagCollection#isApplicableToPrimitive()}.
     */
    @Test
    void testIsApplicableToPrimitive() {
        TagCollection c = new TagCollection();
        assertTrue(c.isApplicableToPrimitive());
        TagCollection d = new TagCollection(Arrays.asList(tagA, tagA, tagC, tagEmpty));
        assertFalse(d.isApplicableToPrimitive());
        TagCollection e = new TagCollection(Arrays.asList(tagA, tagC));
        assertTrue(e.isApplicableToPrimitive());
    }

    /**
     * Test method for {@link TagCollection#applyTo(Tagged)}.
     */
    @Test
    void testApplyToTagged() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC));
        NodeData tagged = new NodeData();
        tagged.put("k", "x");
        tagged.put("k3", "x");
        c.applyTo(tagged);
        assertEquals("v", tagged.get("k"));
        assertEquals("b", tagged.get("k2"));
        assertEquals("x", tagged.get("k3"));
        TagCollection d = new TagCollection(Arrays.asList(tagEmpty));
        d.applyTo(tagged);
        assertEquals(null, tagged.get("k"));
    }

    /**
     * Test method for {@link TagCollection#applyTo(Collection)}.
     */
    @Test
    void testApplyToCollectionOfQextendsTagged() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC));
        NodeData tagged = new NodeData();
        NodeData tagged2 = new NodeData();
        tagged2.put("k", "x");
        tagged2.put("k3", "x");
        c.applyTo(Arrays.asList(tagged, tagged2));
        assertEquals("v", tagged.get("k"));
        assertEquals("b", tagged.get("k2"));
        assertEquals("v", tagged2.get("k"));
        assertEquals("b", tagged2.get("k2"));
        assertEquals("x", tagged2.get("k3"));
    }

    /**
     * Test method for {@link TagCollection#replaceTagsOf(Tagged)}.
     */
    @Test
    void testReplaceTagsOfTagged() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC));
        NodeData tagged = new NodeData();
        tagged.put("k", "x");
        tagged.put("k3", "x");
        c.replaceTagsOf(tagged);
        assertEquals("v", tagged.get("k"));
        assertEquals("b", tagged.get("k2"));
        assertEquals(null, tagged.get("k3"));
    }

    /**
     * Test method for {@link TagCollection#replaceTagsOf(Collection)}.
     */
    @Test
    void testReplaceTagsOfCollectionOfQextendsTagged() {
        TagCollection c = new TagCollection(Arrays.asList(tagA, tagC));
        NodeData tagged = new NodeData();
        NodeData tagged2 = new NodeData();
        tagged2.put("k", "x");
        tagged2.put("k3", "x");
        c.replaceTagsOf(Arrays.asList(tagged, tagged2));
        assertEquals("v", tagged.get("k"));
        assertEquals("b", tagged.get("k2"));
        assertEquals("v", tagged2.get("k"));
        assertEquals("b", tagged2.get("k2"));
        assertEquals(null, tagged2.get("k3"));
    }

    /**
     * Test method for {@link TagCollection#intersect(TagCollection)}.
     */
    @Test
    void testIntersect() {
        TagCollection c1 = new TagCollection(Arrays.asList(tagA, tagC, tagD, tagEmpty));
        TagCollection c2 = new TagCollection(Arrays.asList(tagA, tagB, tagD));
        TagCollection c = c1.intersect(c2);
        assertEquals(2, c.getKeys().size());
        assertEquals(1, c.getTagOccurrence(tagA));
        assertEquals(1, c.getTagOccurrence(tagD));
    }

    /**
     * Test method for {@link TagCollection#minus(TagCollection)}.
     */
    @Test
    void testMinus() {
        TagCollection c1 = new TagCollection(Arrays.asList(tagA, tagC, tagD, tagEmpty));
        TagCollection c2 = new TagCollection(Arrays.asList(tagA, tagB, tagD));
        TagCollection c = c1.minus(c2);
        assertEquals(2, c.getKeys().size());
        assertEquals(1, c.getTagOccurrence(tagC));
        assertEquals(1, c.getTagOccurrence(tagEmpty));
    }

    /**
     * Test method for {@link TagCollection#union(TagCollection)}.
     */
    @Test
    void testUnion() {
        TagCollection c1 = new TagCollection(Arrays.asList(tagA, tagC, tagD, tagEmpty));
        TagCollection c2 = new TagCollection(Arrays.asList(tagA, tagB, tagD));
        TagCollection c = c1.union(c2);
        assertEquals(2, c.getTagOccurrence(tagA));
        assertEquals(1, c.getTagOccurrence(tagB));
        assertEquals(1, c.getTagOccurrence(tagC));
        assertEquals(2, c.getTagOccurrence(tagD));
        assertEquals(1, c.getTagOccurrence(tagEmpty));
    }

    /**
     * Test method for {@link TagCollection#emptyTagsForKeysMissingIn(TagCollection)}.
     */
    @Test
    void testEmptyTagsForKeysMissingIn() {
        TagCollection c1 = new TagCollection(Arrays.asList(tagA, tagC, tagD, tagEmpty));
        TagCollection c2 = new TagCollection(Arrays.asList(tagA, tagB, tagD));
        TagCollection c = c1.emptyTagsForKeysMissingIn(c2);
        assertEquals(2, c.getKeys().size());
        assertEquals(1, c.getTagOccurrence(new Tag(tagC.getKey(), "")));
        assertEquals(1, c.getTagOccurrence(tagEmpty));
    }

    /**
     * Test method for {@link TagCollection#getJoinedValues(String)}.
     */
    @Test
    void testGetJoinedValues() {
        TagCollection c = new TagCollection(Arrays.asList(new Tag("k", "a")));
        assertEquals("a", c.getJoinedValues("k"));
        TagCollection d = new TagCollection(Arrays.asList(new Tag("k", "a"), new Tag("k", "b")));
        assertEquals("a;b", d.getJoinedValues("k"));
        TagCollection e = new TagCollection(Arrays.asList(new Tag("k", "b"), new Tag("k", "a"), new Tag("k", "b;a")));
        assertEquals("b;a", e.getJoinedValues("k"));
        TagCollection f = new TagCollection(Arrays.asList(new Tag("k", "b"), new Tag("k", "a"), new Tag("k", "b"),
                new Tag("k", "c"), new Tag("k", "d"), new Tag("k", "a;b;c;d")));
        assertEquals("a;b;c;d", f.getJoinedValues("k"));
        TagCollection g = new TagCollection(Arrays.asList(new Tag("k", "b"), new Tag("k", "a"), new Tag("k", "b"),
                new Tag("k", "c"), new Tag("k", "d")));
        assertEquals("a;b;c;d", Stream.of(g.getJoinedValues("k").split(";", -1)).sorted().collect(Collectors.joining(";")));
    }

    /**
     * Test method for {@link TagCollection#getSummedValues(String)}.
     */
    @Test
    void testGetSummedValues() {
        TagCollection c = new TagCollection(Arrays.asList(new Tag("k", "10"), new Tag("k", "20")));
        assertEquals("30", c.getSummedValues("k"));
        TagCollection d = new TagCollection(Arrays.asList(new Tag("k", "10"), new Tag("k", "10")));
        assertEquals("10", d.getSummedValues("k"));
        TagCollection e = new TagCollection(Arrays.asList(new Tag("k", "10"), new Tag("k", "x")));
        assertEquals("10", e.getSummedValues("k"));
        TagCollection f = new TagCollection();
        assertEquals("0", f.getSummedValues("k"));
    }

    /**
     * Test method for {@link TagCollection#commonToAllPrimitives(Collection)}.
     */
    @Test
    void testCommonToAllPrimitives() {
        Tagged t1 = new Node();
        t1.put("k1", "10");
        t1.put("k2", "20");
        Tagged t2 = new Node();
        t2.put("k2", "20");
        TagCollection c = TagCollection.commonToAllPrimitives(Arrays.asList(t1, t2));
        assertEquals(1, c.size());
        assertFalse(c.hasValuesFor("k1"));
        assertTrue(c.hasValuesFor("k2"));
        assertEquals(1, c.getValues("k2").size());
        assertEquals("20", c.getValues("k2").iterator().next());
    }
}
