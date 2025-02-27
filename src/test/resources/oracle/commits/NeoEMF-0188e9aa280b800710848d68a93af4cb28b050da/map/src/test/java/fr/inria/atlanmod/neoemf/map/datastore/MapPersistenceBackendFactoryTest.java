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
package fr.inria.atlanmod.neoemf.map.datastore;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.*;
import fr.inria.atlanmod.neoemf.map.resources.MapResourceOptions;
import fr.inria.atlanmod.neoemf.map.util.NeoMapURI;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceOptions;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class MapPersistenceBackendFactoryTest {

    private static final Path TEST_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "NeoEMF");
    private static final String TEST_FILENAME = "mapPersistenceBackendFactoryTest";

    private AbstractPersistenceBackendFactory persistenceBackendFactory = null;
    private File testFolder = null;
    private File testFile = null;
    @SuppressWarnings("rawtypes")
    private Map options = new HashMap();
    private List<PersistentResourceOptions.StoreOption> storeOptions = new ArrayList<PersistentResourceOptions.StoreOption>();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        persistenceBackendFactory = new MapPersistenceBackendFactory();
        PersistenceBackendFactoryRegistry.getFactories().put(NeoMapURI.NEO_MAP_SCHEME, persistenceBackendFactory);
        testFolder = TEST_DIR.resolve(TEST_FILENAME + String.valueOf(new Date().getTime())).toFile();
        testFolder.mkdirs();
        testFile = new File(testFolder + "/db");
        options.put(PersistentResourceOptions.STORE_OPTIONS, storeOptions);

    }

    @After
    public void tearDown() {
        PersistenceBackendFactoryRegistry.getFactories().clear();
        if (testFolder != null) {
            try {
                FileUtils.forceDelete(testFolder);
            } catch (IOException e) {
                //System.err.println(e);
            }
            testFolder = null;
            testFile = null;
        }
    }

    private PersistenceBackend getInnerBackend(DirectWriteMapResourceEStoreImpl store) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field mapStoreField = DirectWriteMapResourceEStoreImpl.class.getDeclaredField("db");
        mapStoreField.setAccessible(true);
        return (PersistenceBackend) mapStoreField.get(store);
    }

    private PersistenceBackend getInnerBackend(DirectWriteMapResourceWithListsEStoreImpl store) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field mapStoreField = DirectWriteMapResourceWithListsEStoreImpl.class.getDeclaredField("db");
        mapStoreField.setAccessible(true);
        return (PersistenceBackend) mapStoreField.get(store);
    }

    private PersistenceBackend getInnerBackend(DirectWriteMapWithIndexesResourceEStoreImpl store) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field mapStoreField = DirectWriteMapWithIndexesResourceEStoreImpl.class.getDeclaredField("db");
        mapStoreField.setAccessible(true);
        return (PersistenceBackend) mapStoreField.get(store);
    }

    @Test
    public void testCreateTransientBackend() {
        PersistenceBackend transientBackend = persistenceBackendFactory.createTransientBackend();
        assertThat("Invalid backend created", transientBackend, instanceOf(MapPersistenceBackend.class));
        // Need to test further the nature of the MapDB engine
    }

    @Test
    public void testCreateTransientEStore() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PersistenceBackend transientBackend = persistenceBackendFactory.createTransientBackend();

        SearcheableResourceEStore eStore = persistenceBackendFactory.createTransientEStore(null, transientBackend);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteMapResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceEStoreImpl) eStore);
        assertThat(innerBackend, is(transientBackend));
    }

    @Test
    public void testCreatePersistentBackendNoOption() throws InvalidDataStoreException {
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        assertThat("Invalid backend created", persistentBackend, instanceOf(MapPersistenceBackend.class));
        // Need to test further the nature of the MapDB engine
    }

    @Test
    public void testCreatePersistentEStoreNoOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, Collections.EMPTY_MAP);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteMapResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    @Test
    public void testCreatePersistentEStoreDirectWriteOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(MapResourceOptions.EStoreMapOption.DIRECT_WRITE);

        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteMapResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    @Test
    public void testCreatePersistentEStoreDirectWriteWithListsOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(MapResourceOptions.EStoreMapOption.DIRECT_WRITE_WITH_LISTS);

        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteMapResourceWithListsEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceWithListsEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    @Test
    public void testCreatePersistentEStoreDirectWriteWithIndexesOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(MapResourceOptions.EStoreMapOption.DIRECT_WRITE_WITH_INDEXES);

        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteMapWithIndexesResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapWithIndexesResourceEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    @Test
    public void testCreatePersistentEStoreAutocommitOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(MapResourceOptions.EStoreMapOption.AUTOCOMMIT);

        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invaild EStore created", eStore, instanceOf(AutocommitMapResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    @Test
    public void testCreatePersistentEStoreCachedManyOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(MapResourceOptions.EStoreMapOption.CACHED_MANY);

        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);

        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invalid EStore created", eStore, instanceOf(CachedManyDirectWriteMapResourceEStoreImpl.class));

        PersistenceBackend innerBackend = getInnerBackend((DirectWriteMapResourceEStoreImpl) eStore);
        assertThat("The backend in the EStore is not the created one", innerBackend, is(persistentBackend));
    }

    /**
     * Test if {@link AbstractPersistenceBackendFactory#copyBackend} creates the persistent
     * data stores from the transient ones. Only empty backends are tested.
     * @throws InvalidDataStoreException
     */
    @Test
    public void testCopyBackend() throws InvalidDataStoreException {
        PersistenceBackend transientBackend = persistenceBackendFactory.createTransientBackend();
        assertThat("Transient backend is not an instance of MapPersistenceBackend", transientBackend, instanceOf(MapPersistenceBackend.class));

        MapPersistenceBackend transientMap = (MapPersistenceBackend) transientBackend;
        SearcheableResourceEStore transientEStore = persistenceBackendFactory.createTransientEStore(null, transientBackend);
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        assertThat("Persistent backend is not an instance of MapPersistenceBackend", persistentBackend, instanceOf(MapPersistenceBackend.class));

        MapPersistenceBackend persistentMap = (MapPersistenceBackend) persistentBackend;
        SearcheableResourceEStore persistentEStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        persistenceBackendFactory.copyBackend(transientMap, persistentMap);
        for (String tKey : transientMap.getAll().keySet()) {
            assertThat("Persistent backend does not contain the key " + tKey, persistentMap.getAll(), hasKey(tKey));
            assertThat("Persistent backend structure " + tKey + " is not equal to transient one", persistentMap.getAll().get(tKey), is(transientMap.get(tKey)));
        }
    }

}
