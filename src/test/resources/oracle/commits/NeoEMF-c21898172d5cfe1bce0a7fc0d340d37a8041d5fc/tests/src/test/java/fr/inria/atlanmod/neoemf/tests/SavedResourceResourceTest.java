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
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModel;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModelContentObject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SavedResourceResourceTest extends AllSavedResourceTest {

    @Test
    public void testEResourceMapDB() {
        testEResource(mapResource, mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEResourceNeo4j() {
        testEResource(neo4jResource, neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEResourceTinker() {
        testEResource(tinkerResource, tinkerSampleModel, tinkerSampleContentObject);
    }

    @Test
    public void testGetAllContentsEResourceMapDB() {
        testGetAllContentsEResource(mapResource);
    }

    @Test
    public void testGetAllContentsEResourceNeo4j() {
        testGetAllContentsEResource(neo4jResource);
    }

    @Test
    public void testGetAllContentsEResourceTinker() {
        testGetAllContentsEResource(tinkerResource);
    }

    @Test
    public void testEDirectResourceMapDB() {
        testEDirectResource(mapResource, mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEDirectResourceNeo4j() {
        testEDirectResource(neo4jResource, neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEDirectResourceTinker() {
        testEDirectResource(tinkerResource, tinkerSampleModel, tinkerSampleContentObject);
    }

    @Test
    public void testGetAllContentsEDirectResourceMapDB() {
        testGetAllContentsEDirectResource(mapResource);
    }

    @Test
    public void testGetAllContentsEDirectResourceNeo4j() {
        testGetAllContentsEDirectResource(neo4jResource);
    }

    @Test
    public void testGetAllContentsEDirectResourceTinker() {
        testGetAllContentsEDirectResource(tinkerResource);
    }

    private void testEResource(PersistentResource persistentResource, SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        assertEquals("Wrong eResource value", persistentResource, sampleModel.eResource());
        assertEquals("Wrong eResource value", persistentResource, sampleModelContentObject.eResource());
    }

    private void testEDirectResource(PersistentResource persistentResource, SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        InternalEObject internalMapSampleModel = (InternalEObject) sampleModel;
        assertEquals("Wrong eDirectResource value", persistentResource, internalMapSampleModel.eDirectResource());

        InternalEObject internalMapSampleContentObject = (InternalEObject) sampleModelContentObject;
        assertNull("Non top level element eDirectResource is not null", internalMapSampleContentObject.eDirectResource());
    }

    private void testGetAllContentsEDirectResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertEquals("Wrong eDirectResource value", persistentResource, sampleModel.eDirectResource());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertNull("Non top level element eDirectResource is not null", sampleContentObject.eDirectResource());
    }

}
