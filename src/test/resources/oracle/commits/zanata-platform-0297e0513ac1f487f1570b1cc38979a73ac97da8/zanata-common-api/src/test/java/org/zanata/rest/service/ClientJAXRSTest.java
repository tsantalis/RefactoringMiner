/*
 * Copyright 2013, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.zanata.rest.client.IAccountResource;
import org.zanata.rest.client.IAsynchronousProcessResource;
import org.zanata.rest.client.ICopyTransResource;
import org.zanata.rest.client.IFileResource;
import org.zanata.rest.client.IGlossaryResource;
import org.zanata.rest.client.IProjectIterationResource;
import org.zanata.rest.client.IProjectResource;
import org.zanata.rest.client.IProjectsResource;
import org.zanata.rest.client.ISourceDocResource;
import org.zanata.rest.client.IStatisticsResource;
import org.zanata.rest.client.ITranslatedDocResource;
import org.zanata.rest.client.ITranslationMemoryResource;
import org.zanata.rest.client.IVersionResource;
import org.zanata.rest.enunciate.AbstractJAXRSTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ClientJAXRSTest extends AbstractJAXRSTest {

    private Class clientInterface;

    public ClientJAXRSTest(Class clientInterface) {

        this.clientInterface = clientInterface;
    }

    @Parameterized.Parameters
    public static Collection clientInterfaces() {
        return Arrays.asList(new Object[][] { { IAccountResource.class },
                { IAsynchronousProcessResource.class },
                { ICopyTransResource.class }, { IFileResource.class },
                { IGlossaryResource.class },
                { IProjectIterationResource.class },
                { IProjectResource.class }, { IProjectsResource.class },
                { ISourceDocResource.class }, { IStatisticsResource.class },
                { ITranslatedDocResource.class },
                { ITranslationMemoryResource.class },
                { IVersionResource.class } });
    }

    @Test
    public void testAnnotations() throws Exception {
        checkAnnotations(clientInterface, false);
    }

}
