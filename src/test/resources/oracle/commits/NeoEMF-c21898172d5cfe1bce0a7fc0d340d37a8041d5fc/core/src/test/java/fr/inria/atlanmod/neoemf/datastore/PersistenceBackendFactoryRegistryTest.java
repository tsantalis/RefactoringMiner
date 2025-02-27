package fr.inria.atlanmod.neoemf.datastore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PersistenceBackendFactoryRegistryTest {

	private AbstractPersistenceBackendFactory persistenceBackendFactory1 = Mockito.mock(AbstractPersistenceBackendFactory.class);
	private AbstractPersistenceBackendFactory persistenceBackendFactory2 = Mockito.mock(AbstractPersistenceBackendFactory.class);
	
	@Before
	public void setUp() {
		PersistenceBackendFactoryRegistry.getFactories().clear();
	}
	
	@Test
	public void testSingleAdd() {
		PersistenceBackendFactoryRegistry.getFactories().put("mock1", persistenceBackendFactory1);
		assertEquals(1, PersistenceBackendFactoryRegistry.getFactories().size());
		AbstractPersistenceBackendFactory registeredFactory = PersistenceBackendFactoryRegistry.getFactoryProvider("mock1");
		assertNotNull(registeredFactory);
		assertEquals(persistenceBackendFactory1, registeredFactory);
	}
	
	@Test
	public void testMulltipleAdd() {
		PersistenceBackendFactoryRegistry.getFactories().put("mock1", persistenceBackendFactory1);
		PersistenceBackendFactoryRegistry.getFactories().put("mock2", persistenceBackendFactory2);
		assertEquals(2, PersistenceBackendFactoryRegistry.getFactories().size());
		AbstractPersistenceBackendFactory registeredFactory1 = PersistenceBackendFactoryRegistry.getFactoryProvider("mock1");
		AbstractPersistenceBackendFactory registeredFactory2 = PersistenceBackendFactoryRegistry.getFactoryProvider("mock2");
		assertNotNull(registeredFactory1);
		assertEquals(persistenceBackendFactory1, registeredFactory1);
		assertNotNull(registeredFactory2);
		assertEquals(persistenceBackendFactory2, registeredFactory2);
	}

}
