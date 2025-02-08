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
package fr.inria.atlanmod.neoemf.issues;

import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSampleFactory;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSamplePackage;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case to reproduce the issue #7 @link{https://github.com/atlanmod/NeoEMF/issues/7}
 */
public class Issue7Test {

    @Test
    public void testIssue7() {
        MapSamplePackage samplePackage = MapSamplePackage.eINSTANCE;
        MapSampleFactory factory = MapSampleFactory.eINSTANCE;

        SampleModel model = factory.createSampleModel();
        assertNotNull("Created SampleModel is null", model);
        assertNotNull("Accessed List is null", model.getContentObjects());
        assertTrue("Accessed List is not empty", model.getContentObjects().isEmpty());
    }

}
