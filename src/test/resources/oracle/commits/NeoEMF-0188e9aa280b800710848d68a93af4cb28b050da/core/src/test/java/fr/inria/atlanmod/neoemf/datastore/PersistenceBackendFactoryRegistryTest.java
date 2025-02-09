package fr.inria.atlanmod.neoemf.datastore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

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
        assertThat(PersistenceBackendFactoryRegistry.getFactories().size(), is(1));

        AbstractPersistenceBackendFactory registeredFactory = PersistenceBackendFactoryRegistry.getFactoryProvider("mock1");
        assertThat(registeredFactory, notNullValue());
        assertThat(registeredFactory, is(persistenceBackendFactory1));
    }

    @Test
    public void testMulltipleAdd() {
        PersistenceBackendFactoryRegistry.getFactories().put("mock1", persistenceBackendFactory1);
        PersistenceBackendFactoryRegistry.getFactories().put("mock2", persistenceBackendFactory2);
        assertThat(PersistenceBackendFactoryRegistry.getFactories().size(), is(2));

        AbstractPersistenceBackendFactory registeredFactory1 = PersistenceBackendFactoryRegistry.getFactoryProvider("mock1");
        assertThat(registeredFactory1, notNullValue());
        assertThat(registeredFactory1, is(persistenceBackendFactory1));

        AbstractPersistenceBackendFactory registeredFactory2 = PersistenceBackendFactoryRegistry.getFactoryProvider("mock2");
        assertThat(registeredFactory2, notNullValue());
        assertThat(registeredFactory2, is(persistenceBackendFactory2));
    }

}
