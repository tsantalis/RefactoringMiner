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

public class SavedResourceResourceTest extends AllSavedResourceTest {

    @Test
    public void testEResourceMapDB() {
        checkEResource(mapResource, mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEResourceNeo4j() {
        checkEResource(neo4jResource, neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEResourceTinker() {
        checkEResource(tinkerResource, tinkerSampleModel, tinkerSampleContentObject);
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
    public void testEDirectResourceMapDB() {
        checkEDirectResource(mapResource, mapSampleModel, mapSampleContentObject);
    }

    @Test
    public void testEDirectResourceNeo4j() {
        checkEDirectResource(neo4jResource, neo4jSampleModel, neo4jSampleContentObject);
    }

    @Test
    public void testEDirectResourceTinker() {
        checkEDirectResource(tinkerResource, tinkerSampleModel, tinkerSampleContentObject);
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

    private void checkEResource(PersistentResource persistentResource, SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        assertThat("Wrong eResource value", sampleModel.eResource().equals(persistentResource), is(true));
        assertThat("Wrong eResource value", sampleModelContentObject.eResource().equals(persistentResource), is(true));
    }

    private void checkEDirectResource(PersistentResource persistentResource, SampleModel sampleModel, SampleModelContentObject sampleModelContentObject) {
        InternalEObject internalMapSampleModel = (InternalEObject) sampleModel;
        assertThat("Wrong eDirectResource value", internalMapSampleModel.eDirectResource().equals(persistentResource), is(true));

        InternalEObject internalMapSampleContentObject = (InternalEObject) sampleModelContentObject;
        assertThat("Non top level element eDirectResource is not null", internalMapSampleContentObject.eDirectResource(), nullValue());
    }

    private void getAllContentsEDirectResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        InternalEObject sampleModel = (InternalEObject) it.next();
        assertThat("Wrong eDirectResource value", sampleModel.eDirectResource().equals(persistentResource), is(true));

        InternalEObject sampleContentObject = (InternalEObject) it.next();
        assertThat("Non top level element eDirectResource is not null", sampleContentObject.eDirectResource(), nullValue());
    }

}
