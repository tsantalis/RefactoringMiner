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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LoadedResourceContainerTest extends AllLoadedResourceTest {

    @Test
    public void testGetElementsContainerMapDB() {
        testGetElementsContainer(mapResource);
    }

    @Test
    public void testGetElementsContainerNeo4j() {
        testGetElementsContainer(neo4jResource);
    }

    @Test
    public void testGetElementsContainerTinker() {
        testGetElementsContainer(tinkerResource);
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
    @Ignore
    public void testGetElementsEInternalContainerMapDB() {
        testGetElementsEInternalContainer(mapResource);
    }

    @Test
    @Ignore
    public void testGetElementsEInternalContainerNeo4j() {
        testGetElementsEInternalContainer(neo4jResource);
    }

    @Test
    @Ignore
    public void testGetElementsEInternalContainerTinker() {
        testGetElementsEInternalContainer(tinkerResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerMapDB() {
        testGetAllContentsEInternalContainer(mapResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerNeo4j() {
        testGetAllContentsEInternalContainer(neo4jResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerTinker() {
        testGetAllContentsEInternalContainer(tinkerResource);
    }

    private void testGetElementsContainer(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertNull(model.eContainer());

        SampleModelContentObject modelContent = model.getContentObjects().get(0);
        assertEquals(model, modelContent.eContainer());
    }

    private void testGetElementsEInternalContainer(PersistentResource persistentResource) {
        // TODO check if we have to correct it or not (performance issues)
        InternalEObject model = (InternalEObject) persistentResource.getContents().get(0);
        assertNull("eInternalContainer must return null if eContainer has not been called", model.eInternalContainer());

        InternalEObject modelContent = (InternalEObject) ((SampleModel) model).getContentObjects().get(0);
        assertNull("eInternalContainer must return null if eContainer has not been called", modelContent.eInternalContainer());
    }

    private void testGetAllContentsEInternalContainer(PersistentResource persistentResource) {
        // TODO check if we have to correct it or not (performance issues)
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertNull("eInternalContainer must return null if eContainer has not been called", sampleModel.eInternalContainer());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertNull("eInternalContainer must return null if eContainer has not been called", sampleContentObject.eInternalContainer());
    }

}
