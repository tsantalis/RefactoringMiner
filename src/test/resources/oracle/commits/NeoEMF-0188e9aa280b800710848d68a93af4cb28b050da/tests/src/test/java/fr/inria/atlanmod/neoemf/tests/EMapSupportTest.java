/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.neoemf.tests;

import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.*;
import org.eclipse.emf.common.util.EMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class EMapSupportTest extends AllBackendTest {

    private static final String KEY1 = "key1", KEY2 = "key2", VALUE1 = "value1", VALUE2 = "value2";

    protected MapSampleFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = MapSampleFactory.eINSTANCE;
        this.ePackage = MapSamplePackage.eINSTANCE;
        super.setUp();
        super.createPersistentStores();
        mapResource.getContents().add(factory.createSampleModel());
        neo4jResource.getContents().add(factory.createSampleModel());
        tinkerResource.getContents().add(factory.createSampleModel());

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetMapStringStringEmptyMapDB() {
        getMapStringStringEmpty(mapResource);
    }

    @Test
    public void testGetMapStringStringEmptyNeo4j() {
        getMapStringStringEmpty(neo4jResource);
    }

    @Test
    public void testGetMapStringStringEmptyTinker() {
        getMapStringStringEmpty(tinkerResource);
    }

    @Test
    public void testPutMapStringStringMapDB() {
        putMapStringString(mapResource);
    }

    @Test
    public void testPutMapStringStringNeo4j() {
        putMapStringString(neo4jResource);
    }

    @Test
    public void testPutMapStringStringTinker() {
        putMapStringString(tinkerResource);
    }

    @Test
    public void testGetMapKVEmptyMapDB() {
        getMapKVEmpty(mapResource);
    }

    @Test
    public void testGetMapKVEmptyNeo4j() {
        getMapKVEmpty(neo4jResource);
    }

    @Test
    public void testGetMapKVEmptyTinker() {
        getMapKVEmpty(tinkerResource);
    }

    @Test
    public void testPutMapKVMapDB() {
        putMapKV(mapResource);
    }

    @Test
    public void testPutMapKVNeo4j() {
        putMapKV(neo4jResource);
    }

    @Test
    public void testPutMapKVTinker() {
        putMapKV(tinkerResource);
    }

    private void getMapStringStringEmpty(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertThat("Map field is not an instance of EMap", model.getMap(), instanceOf(EMap.class));

        EMap<String, String> map = model.getMap();
        assertThat("EMap is not empty", map, empty());
    }

    private void putMapStringString(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        EMap<String, String> map = model.getMap();
        map.put(KEY1, VALUE1);
        map.put(KEY2, VALUE2);

        assertThat("Map does not contain " + KEY1, map.containsKey(KEY1), is(true));
        assertThat("Map does not contain " + KEY2, map.containsKey(KEY2), is(true));

        assertThat("Wrong value for " + KEY1, map.get(KEY1), is(VALUE1));
        assertThat("Wrong  value for " + KEY2, map.get(KEY2), is(VALUE2));
    }

    private void getMapKVEmpty(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertThat("KvMap field is not an instance of EMap", model.getKvMap(), instanceOf(EMap.class));

        EMap<K, V> map = model.getKvMap();
        assertThat("KvMap is not empty", map, empty());
    }

    private void putMapKV(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        EMap<K, V> map = model.getKvMap();

        K k1 = factory.createK();
        k1.setKName(KEY1);
        k1.setKInt(10);

        K k2 = factory.createK();
        k2.setKName(KEY2);
        k2.setKInt(100);

        V v1 = factory.createV();
        v1.setVName(VALUE1);
        v1.setVInt(1);

        V v2 = factory.createV();
        v2.setVName(VALUE2);
        v2.setVInt(5);

        map.put(k1, v1);
        map.put(k2, v2);

        assertThat("Map does not contain " + KEY1, map.containsKey(k1), is(true));
        assertThat("Map does not contain " + KEY2, map.containsKey(k2), is(true));

        assertThat("Wrong value for " + KEY1, map.get(k1), is(v1));
        assertThat("Wrong value for " + KEY2, map.get(k2), is(v2));
    }

}
