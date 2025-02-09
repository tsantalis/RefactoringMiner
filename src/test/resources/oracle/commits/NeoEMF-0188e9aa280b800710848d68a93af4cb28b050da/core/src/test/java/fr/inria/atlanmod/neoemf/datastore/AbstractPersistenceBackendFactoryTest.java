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
package fr.inria.atlanmod.neoemf.datastore;

import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.datastore.estores.impl.*;
import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceOptions;
import org.eclipse.emf.ecore.InternalEObject.EStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for the only non-abstract method in @see{AbstractPersistenceBackendFactory::createPersistentEStore}
 *
 */
public class AbstractPersistenceBackendFactoryTest {

    private AbstractPersistenceBackendFactory persistenceBackendFactory = mock(AbstractPersistenceBackendFactory.class);
    private SearcheableResourceEStore mockPersistentEStore = mock(SearcheableResourceEStore.class);
    private PersistenceBackend mockPersistentBackend = mock(PersistenceBackend.class);
    @SuppressWarnings("rawtypes")
    private Map options = new HashMap();
    private List<PersistentResourceOptions.StoreOption> storeOptions = new ArrayList<PersistentResourceOptions.StoreOption>();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws InvalidDataStoreException {
        when(persistenceBackendFactory.createPersistentBackend(any(File.class), any(Map.class))).thenReturn(mockPersistentBackend);
        when(persistenceBackendFactory.createPersistentEStore(any(PersistentResource.class), any(PersistenceBackend.class), any(Map.class))).thenCallRealMethod();
        when(persistenceBackendFactory.internalCreatePersistentEStore(any(PersistentResource.class), any(PersistenceBackend.class), any(Map.class))).thenReturn(mockPersistentEStore);

        PersistenceBackendFactoryRegistry.getFactories().clear();
        PersistenceBackendFactoryRegistry.getFactories().put("mock", persistenceBackendFactory);
        options.put(PersistentResourceOptions.STORE_OPTIONS, storeOptions);
    }

    @After
    public void tearDown() {
        options.clear();
    }

    private SearcheableResourceEStore getChildStore(EStore store) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        assertThat("Invalid call, can not get the child store if the given one is not a DelegatedResourceEStoreImpl", store, instanceOf(DelegatedResourceEStoreImpl.class));
        Field childStoreField = DelegatedResourceEStoreImpl.class.getDeclaredField("eStore");
        childStoreField.setAccessible(true);
        return (SearcheableResourceEStore) childStoreField.get(store);
    }

    @Test
    public void testNoOptions() throws InvalidDataStoreException {
        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, Collections.EMPTY_MAP);
        assertThat(store, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(store.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    @Test
    public void testIsSetCachingOption() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.IS_SET_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(IsSetCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore childStore = getChildStore(store);
        assertThat(childStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(childStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    @Test
    public void testLoggingOption() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.LOGGING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(LoggingDelegatedResourceEStoreImpl.class));

        SearcheableResourceEStore childStore = getChildStore(store);
        assertThat(childStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(childStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    @Test
    public void testSizeCachingOption() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.SIZE_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(SizeCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore childStore = getChildStore(store);
        assertThat(childStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(childStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    @Test
    public void testEStructuralFeatureCachingOption() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.ESTRUCUTRALFEATURE_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(EStructuralFeatureCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore childStore = getChildStore(store);
        assertThat(childStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(childStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    @Test
    public void testLoadedObjectCounterLoggingOption() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.LOADED_OBJECT_COUNTER_LOGGING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(LoadedObjectCounterLoggingDelegatedEStoreImpl.class));

        SearcheableResourceEStore childStore = getChildStore(store);
        assertThat(childStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(childStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    /**
     * Test store containment order (depend on the instantiation policy defined in @see{AbstractPersistenceBackendFactory}
     * 2 stores : @see{IsSetCachingDelegatedEStoreImpl} and @see{LoggingDelegatedEStoreImpl}
     */
    @Test
    public void testIsSetCachingLoggingOptions() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.IS_SET_CACHING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.LOGGING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(LoggingDelegatedResourceEStoreImpl.class));

        SearcheableResourceEStore loggingChildStore = getChildStore(store);
        assertThat(loggingChildStore, instanceOf(IsSetCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore isSetCachingChildStore = getChildStore(loggingChildStore);
        assertThat(isSetCachingChildStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(isSetCachingChildStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    /**
     * Test store containment order (depend on the instantiation policy defined in @see{AbstractPersistenceBackendFactory}
     * 2 stores : @see{IsSetCachingDelegatedEStoreImpl} and @see{SizeCachingDelegatedEStoreImpl}
     */
    @Test
    public void testIsSetCachingSizeCachingOptions() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.IS_SET_CACHING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.SIZE_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(SizeCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore sizeCachingChildStore = getChildStore(store);
        assertThat(sizeCachingChildStore, instanceOf(IsSetCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore isSetCachingChildStore = getChildStore(sizeCachingChildStore);
        assertThat(isSetCachingChildStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(isSetCachingChildStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    /**
     * Test store containment order (depend on the instantiation policy defined in @see{AbstractPersistenceBackendFactory}
     * 2 stores : @see{SizeCachingDelegatedEStoreImpl} and @see{EStructuralFeatureCachingDelegatedEStoreImpl}
     */
    @Test
    public void testSizeCachingEStructuralFeatureCachingOptions() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.SIZE_CACHING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.ESTRUCUTRALFEATURE_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(SizeCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore sizeCachingChildStore = getChildStore(store);
        assertThat(sizeCachingChildStore, instanceOf(EStructuralFeatureCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore eStructuralFeatureCachingChildStore = getChildStore(sizeCachingChildStore);
        assertThat(eStructuralFeatureCachingChildStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(eStructuralFeatureCachingChildStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

    /**
     * Test store containment order (depend on the instantiation policy defined in @see{AbstractPersistenceBackendFactory}
     * 4 stores : @see{EStructuralFeatureCachingDelegatedEStoreImpl}, @see{IsSetCachingDelegatedEStoreImpl}, 
     * @see{LoggingDelegatedResourceEStoreImpl}, and @see{SizeCachingDelegatedEStoreImpl}
     */
    @Test
    public void testEStructuralFeatureCachingIsSetCachingLoggingSizeCachingOptions() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvalidDataStoreException {
        storeOptions.add(PersistentResourceOptions.EStoreOption.ESTRUCUTRALFEATURE_CACHING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.IS_SET_CACHING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.LOGGING);
        storeOptions.add(PersistentResourceOptions.EStoreOption.SIZE_CACHING);

        SearcheableResourceEStore store = persistenceBackendFactory.createPersistentEStore(null, mockPersistentBackend, options);
        assertThat(store, instanceOf(LoggingDelegatedResourceEStoreImpl.class));

        SearcheableResourceEStore loggingChildStore = getChildStore(store);
        assertThat(loggingChildStore, instanceOf(SizeCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore sizeCachingChildStore = getChildStore(loggingChildStore);
        assertThat(sizeCachingChildStore, instanceOf(EStructuralFeatureCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore eStructuralFeatureCachingChildStore = getChildStore(sizeCachingChildStore);
        assertThat(eStructuralFeatureCachingChildStore, instanceOf(IsSetCachingDelegatedEStoreImpl.class));

        SearcheableResourceEStore isSetCachingChildStore = getChildStore(eStructuralFeatureCachingChildStore);
        assertThat(isSetCachingChildStore, instanceOf(SearcheableResourceEStore.class));
        // Ensure this is the mock that is returned by checking the real class name
        assertThat(isSetCachingChildStore.getClass().getSimpleName(), containsString("SearcheableResourceEStore"));
    }

}
