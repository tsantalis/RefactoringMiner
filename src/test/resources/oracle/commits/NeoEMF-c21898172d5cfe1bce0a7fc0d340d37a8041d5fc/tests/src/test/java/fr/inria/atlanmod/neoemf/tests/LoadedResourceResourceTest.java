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

public class LoadedResourceResourceTest extends AllLoadedResourceTest {

    @Test
    public void testGetElementsEResourceMapDB() {
        testGetElementsEResource(mapResource);
    }

    @Test
    public void testGetElementsEResourceNeo4j() {
        testGetElementsEResource(neo4jResource);
    }

    @Test
    public void testGetElementsEResourceTinker() {
        testGetElementsEResource(tinkerResource);
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
    public void testGetElementsEDirectResourceMapDB() {
        testGetElementsEDirectResource(mapResource);
    }

    @Test
    public void testGetElementsEDirectResourceNeo4j() {
        testGetElementsEDirectResource(neo4jResource);
    }

    @Test
    public void testGetElementsEDirectResourceTinker() {
        testGetElementsEDirectResource(tinkerResource);
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

    private void testGetElementsEResource(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertEquals("Wrong eResource value", persistentResource, model.eResource());

        SampleModelContentObject modelContent = model.getContentObjects().get(0);
        assertEquals("Wrong eResource value", persistentResource, modelContent.eResource());
    }

    private void testGetElementsEDirectResource(PersistentResource persistentResource) {
        InternalEObject model = (InternalEObject) persistentResource.getContents().get(0);
        assertNull("eDirectResource must return null", model.eDirectResource());

        InternalEObject modelContent = (InternalEObject) ((SampleModel) model).getContentObjects().get(0);
        assertNull("eDirectResource must return null", modelContent.eDirectResource());
    }

    private void testGetAllContentsEDirectResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertNull("eDirectResource must return null", sampleModel.eDirectResource());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertNull("eDirectResource must return null", sampleContentObject.eDirectResource());
    }

}
