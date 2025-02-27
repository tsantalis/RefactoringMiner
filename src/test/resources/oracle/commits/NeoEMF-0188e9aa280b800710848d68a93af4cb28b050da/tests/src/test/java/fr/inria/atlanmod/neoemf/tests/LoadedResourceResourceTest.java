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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class LoadedResourceResourceTest extends AllLoadedResourceTest {

    @Test
    public void testGetElementsEResourceMapDB() {
        getElementsEResource(mapResource);
    }

    @Test
    public void testGetElementsEResourceNeo4j() {
        getElementsEResource(neo4jResource);
    }

    @Test
    public void testGetElementsEResourceTinker() {
        getElementsEResource(tinkerResource);
    }

    @Test
    public void testGetAllContentsEResourceMapDB() {
        getAllContentsEResource(mapResource);
    }

    @Test
    public void testGetAllContentsEResourceNeo4j() {
        getAllContentsEResource(neo4jResource);
    }

    @Test
    public void testGetAllContentsEResourceTinker() {
        getAllContentsEResource(tinkerResource);
    }

    @Test
    public void testGetElementsEDirectResourceMapDB() {
        getElementsEDirectResource(mapResource);
    }

    @Test
    public void testGetElementsEDirectResourceNeo4j() {
        getElementsEDirectResource(neo4jResource);
    }

    @Test
    public void testGetElementsEDirectResourceTinker() {
        getElementsEDirectResource(tinkerResource);
    }

    @Test
    public void testGetAllContentsEDirectResourceMapDB() {
        getAllContentsEDirectResource(mapResource);
    }

    @Test
    public void testGetAllContentsEDirectResourceNeo4j() {
        getAllContentsEDirectResource(neo4jResource);
    }

    @Test
    public void testGetAllContentsEDirectResourceTinker() {
        getAllContentsEDirectResource(tinkerResource);
    }

    private void getElementsEResource(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertThat("Wrong eResource value", model.eResource().equals(persistentResource), is(true));

        SampleModelContentObject modelContent = model.getContentObjects().get(0);
        assertThat("Wrong eResource value", modelContent.eResource().equals(persistentResource), is(true));
    }

    private void getElementsEDirectResource(PersistentResource persistentResource) {
        InternalEObject model = (InternalEObject) persistentResource.getContents().get(0);
        assertThat("eDirectResource must return null", model.eDirectResource(), nullValue());

        InternalEObject modelContent = (InternalEObject) ((SampleModel) model).getContentObjects().get(0);
        assertThat("eDirectResource must return null", modelContent.eDirectResource(), nullValue());
    }

    private void getAllContentsEDirectResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertThat("eDirectResource must return null", sampleModel.eDirectResource(), nullValue());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertThat("eDirectResource must return null", sampleContentObject.eDirectResource(), nullValue());
    }

}
