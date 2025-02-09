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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AllInstancesTransientTest extends AllInstancesTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        super.createTransientStores();
        createResourceContent(mapResource);
        createResourceContent(neo4jResource);
        createResourceContent(tinkerResource);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testAllInstancesTransientMapDB() {
        testAllInstancesTransient(mapResource);
    }

    @Test
    public void testAllInstancesTransientNeo4j() {
        testAllInstancesTransient(neo4jResource);
    }

    @Test
    public void testAllInstancesTransientTinker() {
        testAllInstancesTransient(tinkerResource);
    }

    @Test
    public void testAllInstancesStrictTransientMapDB() {
        testAllInstancesStrictTransient(mapResource);
    }

    @Test
    public void testAllInstancesStrictTransientNeo4j() {
        testAllInstancesStrictTransient(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictTransientTinker() {
        testAllInstancesStrictTransient(tinkerResource);
    }

    private void testAllInstancesTransient(PersistentResource persistentResource) {
        testAllInstancesPersistentTranscient(persistentResource, false, abstractPackContentCount, packContentCount);
    }

    private void testAllInstancesStrictTransient(PersistentResource persistentResource) {
        testAllInstancesPersistentTranscient(persistentResource, true, abstractPackContentStrictCount, packContentStrictCount);
    }
}
