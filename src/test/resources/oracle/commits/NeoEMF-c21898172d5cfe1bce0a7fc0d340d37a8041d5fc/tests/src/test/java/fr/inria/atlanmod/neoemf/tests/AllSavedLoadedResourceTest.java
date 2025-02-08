/*
 * Copyright 2016 Yoann Vernageau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.inria.atlanmod.neoemf.tests;

import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import org.eclipse.emf.ecore.EObject;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AllSavedLoadedResourceTest extends AllBackendTest {

    protected void testGetAllContentsContainer(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        EObject sampleModel = it.next();
        assertNull("Top Level EObject has a not null container", sampleModel.eContainer());

        EObject sampleContentObject = it.next();
        assertEquals("Wrong eContainer value", sampleModel, sampleContentObject.eContainer());
    }

    protected void testGetAllContentsEResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        EObject sampleModel = it.next();
        assertEquals("Wrong eResource value", persistentResource, sampleModel.eResource());

        EObject sampleContentObject = it.next();
        assertEquals("Wrong eResource value", persistentResource, sampleContentObject.eResource());
    }

}
