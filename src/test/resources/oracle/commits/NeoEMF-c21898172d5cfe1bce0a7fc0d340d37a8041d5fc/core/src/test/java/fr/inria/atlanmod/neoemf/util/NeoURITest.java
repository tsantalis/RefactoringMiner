package fr.inria.atlanmod.neoemf.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.eclipse.emf.common.util.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;

import static org.junit.Assert.assertEquals;

public class NeoURITest {

	private static final Path TEST_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "NeoEMF");
    private static final String TEST_FILENAME = "neoURITestFile";

	private AbstractPersistenceBackendFactory persistenceBackendFactory = Mockito.mock(AbstractPersistenceBackendFactory.class);
	private File testFile = null;
	
	@Before
	public void setUp() {
		PersistenceBackendFactoryRegistry.getFactories().clear();
		PersistenceBackendFactoryRegistry.getFactories().put("mock", persistenceBackendFactory);
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
	public void testCreateNeoURIFromStandardURIInvalidScheme() {
		URI invalidURI = URI.createURI("invalid://test");
		NeoURI.createNeoURI(invalidURI);
	}
	
	@Test
	public void testCreateNeoURIFromStandardURIValidScheme() {
		URI validURI = URI.createURI("mock://test");
		URI neoURI = NeoURI.createNeoURI(validURI);
		assertEquals("mock", neoURI.scheme());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoURIFromFileURINoScheme() {
		URI invalidFileURI = URI.createFileURI(testFile.getAbsolutePath());
		NeoURI.createNeoURI(invalidFileURI);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoURIFromFileInvalidScheme() {
		NeoURI.createNeoURI(testFile, "invalid");
	}
	
	@Test
	public void testCreateNeoURIFromFileValidScheme() {
		URI neoURI = NeoURI.createNeoURI(testFile, "mock");
		assertEquals("mock", neoURI.scheme());
		System.out.println(neoURI.devicePath());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoURIFromFileNullScheme() {
		NeoURI.createNeoURI(testFile,null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoURIFromFileURIInvalidScheme() {
		URI fileUri = URI.createFileURI(testFile.getAbsolutePath());
		NeoURI.createNeoURI(fileUri,"invalid");
	}
	
	@Test
	public void testCreateNeoURIFromFileURIValidScheme() {
		URI fileURI = URI.createFileURI(testFile.getAbsolutePath());
		URI neoURI = NeoURI.createNeoURI(fileURI,"mock");
		assertEquals("mock", neoURI.scheme());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateNeoURIFromFileURINullScheme() {
		URI fileURI = URI.createFileURI(testFile.getAbsolutePath());
		NeoURI.createNeoURI(fileURI, null);
	}
}
