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
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class AllInstancesTest extends AllBackendTest {

    protected MapSampleFactory factory;
    // These variables should be updated if createResourceContent is changed
    protected int packCount = 6;
    protected int abstractPackContentCount = 150;
    protected int packContentCount = 100;
    protected int specializedPackContentCount = 50;
    protected int packContent2Count = 50;
    protected int abstractPackContentStrictCount = 0;
    protected int packContentStrictCount = 50;


    @Before
    public void setUp() throws Exception {
        factory = MapSampleFactory.eINSTANCE;
        this.ePackage = MapSamplePackage.eINSTANCE;
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Creates a Pack hierarchy containing 1 root, 5 sub-pack elements, 10
     * PackContent, and 10 SpecializedPackContent elements in each sub-pack
     *
     * @param r the PersistentResource to fill with the created model
     */
    protected void createResourceContent(PersistentResource r) {
        Pack rootPack = factory.createPack();
        rootPack.setName("root");
        for (int i = 0; i < 5; i++) {
            Pack newPack = factory.createPack();
            newPack.setName("pack" + i);
            rootPack.getPacks().add(newPack);
            for (int j = 0; j < 10; j++) {
                PackContent newPackContent = factory.createPackContent();
                newPackContent.setName("pContent" + i + "-" + j);
                newPack.getOwnedContents().add(newPackContent);
                SpecializedPackContent newSpecializedPackContent = factory.createSpecializedPackContent();
                newSpecializedPackContent.setName("spContent" + i + "-" + j);
                newPack.getOwnedContents().add(newSpecializedPackContent);
                PackContent2 newPackContent2 = factory.createPackContent2();
                newPackContent2.setName("pContent2" + i + "-" + j);
                newPack.getOwnedContents().add(newPackContent2);
            }
        }
        r.getContents().add(rootPack);
    }

    protected void testAllInstancesPersistentTranscient(PersistentResource persistentResource, boolean strict, int abstractPackContentCount, int packContentCount) {
        EList<EObject> allPacks = persistentResource.getAllInstances(MapSamplePackage.eINSTANCE.getPack(), strict);
        assertEquals("Invalid count : expected " + packCount + ", found " + allPacks.size(), packCount, allPacks.size());

        EList<EObject> allAbstractPackContents = persistentResource.getAllInstances(MapSamplePackage.eINSTANCE.getAbstractPackContent(), strict);
        assertEquals("Invalid count : expected " + abstractPackContentCount + ", found " + allAbstractPackContents.size(), abstractPackContentCount, allAbstractPackContents.size());

        EList<EObject> allPackContents = persistentResource.getAllInstances(MapSamplePackage.eINSTANCE.getPackContent(), strict);
        assertEquals("Invalid count : expected " + packContentCount + ", found " + allPackContents.size(), packContentCount, allPackContents.size());

        EList<EObject> allSpecializedPackContents = persistentResource.getAllInstances(MapSamplePackage.eINSTANCE.getSpecializedPackContent(), strict);
        assertEquals("Invalid count : expected " + specializedPackContentCount + ", found " + allSpecializedPackContents.size(), specializedPackContentCount, allSpecializedPackContents.size());

        EList<EObject> allPackContents2 = persistentResource.getAllInstances(MapSamplePackage.eINSTANCE.getPackContent2(), strict);
        assertEquals("Invalid count : expected " + packContent2Count + ", found " + allPackContents2.size(), packContent2Count, allPackContents2.size());
    }

}
