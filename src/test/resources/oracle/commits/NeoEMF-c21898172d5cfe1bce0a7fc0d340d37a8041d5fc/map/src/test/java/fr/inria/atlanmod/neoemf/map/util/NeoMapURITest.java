/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.neoemf.map.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.eclipse.emf.common.util.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.map.datastore.MapPersistenceBackendFactory;

import static org.junit.Assert.assertEquals;

public class NeoMapURITest {

	private static final Path TEST_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "NeoEMF");
    private static final String TEST_FILENAME = "neoMapURITestFile";

	private AbstractPersistenceBackendFactory persistenceBackendFactory = new MapPersistenceBackendFactory();
	private File testFile = null;
	
	@Before
	public void setUp() {
		PersistenceBackendFactoryRegistry.getFactories().clear();
		PersistenceBackendFactoryRegistry.getFactories().put(NeoMapURI.NEO_MAP_SCHEME, persistenceBackendFactory);
		testFile = TEST_DIR.resolve(TEST_FILENAME + String.valueOf(new Date().getTime())).toFile();
	}
	
	@After
	public void tearDown() {
		if(testFile != null) {
			testFile.delete();
			testFile = null;
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoGraphURIFromStandardURIInvalidScheme() {
		URI invalidURI = URI.createURI("invalid:/test");
		NeoMapURI.createNeoMapURI(invalidURI);
	}
	
	@Test
	public void testCreateNeoGraphURIFromStandardURIValidScheme() {
		URI validURI = URI.createURI(NeoMapURI.NEO_MAP_SCHEME+":/test");
		URI neoURI = NeoMapURI.createNeoMapURI(validURI);
		assertEquals(NeoMapURI.NEO_MAP_SCHEME, neoURI.scheme());
	}
	
	@Test
	public void testCreateNeoGraphURIFromFileURI() {
		URI fileURI = URI.createFileURI(testFile.getAbsolutePath());
		URI neoURI = NeoMapURI.createNeoMapURI(fileURI);
		assertEquals(NeoMapURI.NEO_MAP_SCHEME, neoURI.scheme());
	}
	
	@Test
	public void testCreateNeoURIFromFile() {
		URI neoURI = NeoMapURI.createNeoMapURI(testFile);
		assertEquals(NeoMapURI.NEO_MAP_SCHEME, neoURI.scheme());
	}
	
}
