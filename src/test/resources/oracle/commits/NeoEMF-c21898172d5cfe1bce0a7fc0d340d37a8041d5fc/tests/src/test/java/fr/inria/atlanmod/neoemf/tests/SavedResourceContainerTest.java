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

public class SavedResourceContainerTest extends AllSavedResourceTest {

    @Test
    public void testEContainerMapDB() {
        testEContainer(mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEContainerNeo4j() {
        testEContainer(neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEContainerTinker() {
        testEContainer(tinkerSampleModel, tinkerSampleContentObject);
    }

    @Test
    public void testGetAllContentsContainerMapDB() {
        testGetAllContentsContainer(mapResource);
    }

    @Test
    public void testGetAllContentsContainerNeo4j() {
        testGetAllContentsContainer(neo4jResource);
    }

    @Test
    public void testGetAllContentsContainerTinker() {
        testGetAllContentsContainer(tinkerResource);
    }

    @Test
    public void testEInternalContainerMapDB() {
        testEInternalContainer(mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEInternalContainerNeo4j() {
        testEInternalContainer(neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEInternalContainerTinker() {
        testEInternalContainer(tinkerSampleModel, tinkerSampleContentObject);
    }

    @Test
    public void testGetAllContentsEInternalContainerMapDB() {
        testGetAllContentsEInternalContainer(mapResource);
    }

    @Test
    public void testGetAllContentsEInternalContainerNeo4j() {
        testGetAllContentsEInternalContainer(neo4jResource);
    }

    @Test
    public void testGetAllContentsEInternalContainerTinker() {
        testGetAllContentsEInternalContainer(tinkerResource);
    }

    private void testEContainer(SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        assertNull("Top Level EObject has a not null container", sampleModel.eContainer());
        assertEquals("Wrong eContainer value", sampleModel, sampleModelContentObject.eContainer());
    }

    private void testEInternalContainer(SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        InternalEObject internalMapSampleModel = (InternalEObject) sampleModel;
        assertNull("Top Level EObject has a not null internal container", internalMapSampleModel.eInternalContainer());

        InternalEObject internalMapSampleContentObject = (InternalEObject) sampleModelContentObject;
        assertEquals("Wrong eInternalContainer value", internalMapSampleModel, internalMapSampleContentObject.eInternalContainer());
    }

    private void testGetAllContentsEInternalContainer(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertNull("Top Level EObject has a not null container", sampleModel.eInternalContainer());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertEquals("Wrong eInternalContainer value", sampleModel, sampleContentObject.eInternalContainer());
    }

}
