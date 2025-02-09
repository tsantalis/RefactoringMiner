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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class LoadedResourceContainerTest extends AllLoadedResourceTest {

    @Test
    public void testGetElementsContainerMapDB() {
        getElementsContainer(mapResource);
    }

    @Test
    public void testGetElementsContainerNeo4j() {
        getElementsContainer(neo4jResource);
    }

    @Test
    public void testGetElementsContainerTinker() {
        getElementsContainer(tinkerResource);
    }

    @Test
    public void testGetAllContentsContainerMapDB() {
        getAllContentsContainer(mapResource);
    }

    @Test
    public void testGetAllContentsContainerNeo4j() {
        getAllContentsContainer(neo4jResource);
    }

    @Test
    public void testGetAllContentsContainerTinker() {
        getAllContentsContainer(tinkerResource);
    }

    @Test
    @Ignore
    public void testGetElementsEInternalContainerMapDB() {
        getElementsEInternalContainer(mapResource);
    }

    @Test
    @Ignore
    public void testGetElementsEInternalContainerNeo4j() {
        getElementsEInternalContainer(neo4jResource);
    }

    @Test
    @Ignore
    public void testGetElementsEInternalContainerTinker() {
        getElementsEInternalContainer(tinkerResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerMapDB() {
        getAllContentsEInternalContainer(mapResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerNeo4j() {
        getAllContentsEInternalContainer(neo4jResource);
    }

    @Test
    @Ignore
    public void testGetAllContentsEInternalContainerTinker() {
        getAllContentsEInternalContainer(tinkerResource);
    }

    private void getElementsContainer(PersistentResource persistentResource) {
        SampleModel model = (SampleModel) persistentResource.getContents().get(0);
        assertThat(model.eContainer(), nullValue());

        SampleModelContentObject modelContent = model.getContentObjects().get(0);
        assertThat(modelContent.eContainer().equals(model), is(true));
    }

    private void getElementsEInternalContainer(PersistentResource persistentResource) {
        // TODO check if we have to correct it or not (performance issues)
        InternalEObject model = (InternalEObject) persistentResource.getContents().get(0);
        assertThat("eInternalContainer must return null if eContainer has not been called", model.eInternalContainer(), nullValue());

        InternalEObject modelContent = (InternalEObject) ((SampleModel) model).getContentObjects().get(0);
        assertThat("eInternalContainer must return null if eContainer has not been called", modelContent.eInternalContainer(), nullValue());
    }

    private void getAllContentsEInternalContainer(PersistentResource persistentResource) {
        // TODO check if we have to correct it or not (performance issues)
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertThat("eInternalContainer must return null if eContainer has not been called", sampleModel.eInternalContainer(), nullValue());

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertThat("eInternalContainer must return null if eContainer has not been called", sampleContentObject.eInternalContainer(), nullValue());
    }

}
