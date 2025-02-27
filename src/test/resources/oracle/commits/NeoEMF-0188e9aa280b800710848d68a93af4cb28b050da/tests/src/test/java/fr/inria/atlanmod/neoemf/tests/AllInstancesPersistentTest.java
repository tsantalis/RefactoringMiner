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
        allInstancesPersistent(mapResource);
    }

    @Test
    public void testAllInstancesPersistentNeo4j() {
        allInstancesPersistent(neo4jResource);
    }

    @Test
    public void testAllInstancesPersistentTinker() {
        allInstancesPersistent(tinkerResource);
    }

    @Test
    public void testAllInstancesStricPersistentMapDB() {
        allInstancesStrictPersistent(mapResource);
    }

    @Test
    public void testAllInstancesStrictPersistentNeo4j() {
        allInstancesStrictPersistent(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictPersistentTinker() {
        allInstancesStrictPersistent(tinkerResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedMapDB() throws IOException {
        allInstancesPersistentLoaded(mapResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedNeo4j() throws IOException {
        allInstancesPersistentLoaded(neo4jResource);
    }

    @Test
    public void testAllInstancesPersistentLoadedTinker() throws IOException {
        allInstancesPersistentLoaded(tinkerResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedMapDB() throws IOException {
        allInstancesStrictPersistentLoaded(mapResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedNeo4j() throws IOException {
        allInstancesStrictPersistentLoaded(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictPersistentLoadedTinker() throws IOException {
        allInstancesStrictPersistentLoaded(tinkerResource);
    }

    private void allInstancesPersistentLoaded(PersistentResource persistentResource) throws IOException {
        persistentResource.save(Collections.EMPTY_MAP);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl) persistentResource);
        persistentResource.load(Collections.EMPTY_MAP);

        allInstancesPersistent(persistentResource);
    }

    private void allInstancesStrictPersistentLoaded(PersistentResource persistentResource) throws IOException {
        persistentResource.save(Collections.EMPTY_MAP);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl) persistentResource);
        persistentResource.load(Collections.EMPTY_MAP);

        allInstancesStrictPersistent(persistentResource);
    }

    private void allInstancesPersistent(PersistentResource persistentResource) {
        allInstancesPersistentTranscient(persistentResource, false, abstractPackContentCount, packContentCount);
    }

    private void allInstancesStrictPersistent(PersistentResource persistentResource) {
        allInstancesPersistentTranscient(persistentResource, true, abstractPackContentStrictCount, packContentStrictCount);
    }

}
