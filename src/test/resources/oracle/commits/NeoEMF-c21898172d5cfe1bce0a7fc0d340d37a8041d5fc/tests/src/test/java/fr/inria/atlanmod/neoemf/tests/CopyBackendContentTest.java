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
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSampleFactory;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSamplePackage;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModel;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModelContentObject;
import org.eclipse.emf.common.util.EList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;


public class CopyBackendContentTest extends AllBackendTest {

    private static final String MODEL_NAME = "Model", CONTENT1_NAME = "Content1", CONTENT2_NAME = "Content2";

    protected MapSampleFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = MapSampleFactory.eINSTANCE;
        this.ePackage = MapSamplePackage.eINSTANCE;
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

    private void createResourceContent(PersistentResource r) {
        SampleModel model = factory.createSampleModel();
        model.setName(MODEL_NAME);
        SampleModelContentObject content1 = factory.createSampleModelContentObject();
        content1.setName(CONTENT1_NAME);
        SampleModelContentObject content2 = factory.createSampleModelContentObject();
        content2.setName(CONTENT2_NAME);
        model.getContentObjects().add(content1);
        model.getContentObjects().add(content2);
        r.getContents().add(model);
    }

    @Test
    public void testCopyBackendMapDB() throws IOException {
        mapResource.save(Collections.EMPTY_MAP);
        assertFalse("Map resource content is empty", mapResource.getContents().isEmpty());
        assertThat("Top-level element is not a SampleModel", mapResource.getContents().get(0), instanceOf(SampleModel.class));

        SampleModel sampleModel = (SampleModel) mapResource.getContents().get(0);
        assertEquals("SampleModel has an invalid name attribute", MODEL_NAME, sampleModel.getName());

        EList<SampleModelContentObject> contentObjects = sampleModel.getContentObjects();
        assertFalse("SampleModel contentObjects collection is empty", contentObjects.isEmpty());
        assertEquals("SampleModel contentObjects collection has an invalid size", 2, contentObjects.size());

        assertEquals("First element in contentObjects collection has an invalid name", CONTENT1_NAME, contentObjects.get(0).getName());
        assertEquals("Second element in contentObjects collection has an invalid name", CONTENT2_NAME, contentObjects.get(1).getName());

        assertEquals("First element in contentObjects collection has an invalid container", sampleModel, contentObjects.get(0).eContainer());
        assertEquals("Second element in contentObjects collection has an invalid container", sampleModel, contentObjects.get(1).eContainer());
    }

}
