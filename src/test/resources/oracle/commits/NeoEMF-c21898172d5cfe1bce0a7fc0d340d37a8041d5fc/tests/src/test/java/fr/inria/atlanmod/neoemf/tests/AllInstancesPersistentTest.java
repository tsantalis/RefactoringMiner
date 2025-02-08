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
import fr.inria.atlanmod.neoemf.resources.impl.PersistentResourceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class AllInstancesPersistentTest extends AllInstancesTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        super.createPersistentStores();
        createResourceContent(mapResource);
        createResourceContent(neo4jResource);
        createResourceContent(tinkerResource);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testAllInstancesPersistentMapDB() {
        testAllInstancesPersistent(mapResource);
    }

    @Test
    public void testAllInstancesPersistentNeo4j() {
        testAllInstancesPersistent(neo4jResource);
    }

    @Test
    public void testAllInstancesPersistentTinker() {
        testAllInstancesPersistent(tinkerResource);
    }

    @Test
    public void testAllInstancesStricPersistentMapDB() {
        testAllInstancesStrictPersistent(mapResource);
    }

    @Test
    public void testAllInstancesStrictPersistentNeo4j() {
        testAllInstancesStrictPersistent(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictPersistentTinker() {
        testAllInstancesStrictPersistent(tinkerResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedMapDB() throws IOException {
        testAllInstancesPersistentLoaded(mapResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedNeo4j() throws IOException {
        testAllInstancesPersistentLoaded(neo4jResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedTinker() throws IOException {
        testAllInstancesPersistentLoaded(tinkerResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedMapDB() throws IOException {
        testAllInstancesStrictPersistentLoaded(mapResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedNeo4j() throws IOException {
        testAllInstancesStrictPersistentLoaded(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedTinker() throws IOException {
        testAllInstancesStrictPersistentLoaded(tinkerResource);
    }

    private void testAllInstancesPersistentLoaded(PersistentResource persistentResource) throws IOException {
        persistentResource.save(Collections.EMPTY_MAP);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl) persistentResource);
        persistentResource.load(Collections.EMPTY_MAP);

        testAllInstancesPersistent(persistentResource);
    }

    private void testAllInstancesStrictPersistentLoaded(PersistentResource persistentResource) throws IOException {
        persistentResource.save(Collections.EMPTY_MAP);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl) persistentResource);
        persistentResource.load(Collections.EMPTY_MAP);

        testAllInstancesStrictPersistent(persistentResource);
    }

    private void testAllInstancesPersistent(PersistentResource persistentResource) {
        testAllInstancesPersistentTranscient(persistentResource, false, abstractPackContentCount, packContentCount);
    }

    private void testAllInstancesStrictPersistent(PersistentResource persistentResource) {
        testAllInstancesPersistentTranscient(persistentResource, true, abstractPackContentStrictCount, packContentStrictCount);
    }

}
