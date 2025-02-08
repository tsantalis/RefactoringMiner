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
        allInstancesTransient(mapResource);
    }

    @Test
    public void testAllInstancesTransientNeo4j() {
        allInstancesTransient(neo4jResource);
    }

    @Test
    public void testAllInstancesTransientTinker() {
        allInstancesTransient(tinkerResource);
    }

    @Test
    public void testAllInstancesStrictTransientMapDB() {
        allInstancesStrictTransient(mapResource);
    }

    @Test
    public void testAllInstancesStrictTransientNeo4j() {
        allInstancesStrictTransient(neo4jResource);
    }

    @Test
    public void testAllInstancesStrictTransientTinker() {
        allInstancesStrictTransient(tinkerResource);
    }

    private void allInstancesTransient(PersistentResource persistentResource) {
        allInstancesPersistentTranscient(persistentResource, false, abstractPackContentCount, packContentCount);
    }

    private void allInstancesStrictTransient(PersistentResource persistentResource) {
        allInstancesPersistentTranscient(persistentResource, true, abstractPackContentStrictCount, packContentStrictCount);
    }
}
