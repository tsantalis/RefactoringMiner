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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class AllSavedLoadedResourceTest extends AllBackendTest {

    protected void getAllContentsContainer(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        EObject sampleModel = it.next();
        assertThat("Top Level EObject has a not null container", sampleModel.eContainer(), nullValue());

        EObject sampleContentObject = it.next();
        assertThat("Wrong eContainer value", sampleContentObject.eContainer(), is(sampleModel));
    }

    protected void getAllContentsEResource(PersistentResource persistentResource) {
        Iterator<EObject> it = persistentResource.getAllContents();

        EObject sampleModel = it.next();
        assertThat("Wrong eResource value", sampleModel.eResource().equals(persistentResource), is(true));

        EObject sampleContentObject = it.next();
        assertThat("Wrong eResource value", sampleContentObject.eResource().equals(persistentResource), is(true));
    }

}
