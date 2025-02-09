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
package fr.inria.atlanmod.neoemf.graph.blueprints.datastore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.InternalEObject.EStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.estores.impl.AutocommitBlueprintsResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.estores.impl.DirectWriteBlueprintsResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.graph.blueprints.resources.BlueprintsResourceOptions;
import fr.inria.atlanmod.neoemf.graph.blueprints.util.NeoBlueprintsURI;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceOptions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class BlueprintsPersistenceBackendFactoryTest {

    private static final Path TEST_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "NeoEMF");
    private static final String TEST_FILENAME = "graphPersistenceBackendFactoryTestFile";
    
    protected AbstractPersistenceBackendFactory persistenceBackendFactory = null;
    protected File testFile = null;
    @SuppressWarnings("rawtypes")
    protected Map options = new HashMap();
    protected List<PersistentResourceOptions.StoreOption> storeOptions = new ArrayList<PersistentResourceOptions.StoreOption>();
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        persistenceBackendFactory = new BlueprintsPersistenceBackendFactory();
        PersistenceBackendFactoryRegistry.getFactories().put(NeoBlueprintsURI.NEO_GRAPH_SCHEME, persistenceBackendFactory);
        testFile = TEST_DIR.resolve(TEST_FILENAME + String.valueOf(new Date().getTime())).toFile();
        options.put(PersistentResourceOptions.STORE_OPTIONS, storeOptions);
        
    }
    
    @After
    public void tearDown() {
        PersistenceBackendFactoryRegistry.getFactories().clear();
        if(testFile != null) {
            try {
                FileUtils.forceDelete(testFile);
            } catch(IOException e) {
                //System.err.println(e);
            }
            testFile = null;
        }
    }
    
    protected PersistenceBackend getInnerBackend(EStore store) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        assertThat("Invalid call, can not get the inner backend if the given EStore is not a DirectWriteGraphResourceEStoreImpl", store, instanceOf(DirectWriteBlueprintsResourceEStoreImpl.class));
        Field graphStoreField = DirectWriteBlueprintsResourceEStoreImpl.class.getDeclaredField("graph");
        graphStoreField.setAccessible(true);
        return (PersistenceBackend)graphStoreField.get(store);
    }
    
    @Test
    public void testCreateTransientBackend() {
        PersistenceBackend transientBackend = persistenceBackendFactory.createTransientBackend();
        assertThat("Invalid backend created", transientBackend, instanceOf(BlueprintsPersistenceBackend.class));
        BlueprintsPersistenceBackend graph = (BlueprintsPersistenceBackend)transientBackend;
        assertThat("The base graph is not a TinkerGraph", graph.getBaseGraph(), instanceOf(TinkerGraph.class));
    }
    
    @Test
    public void testCreateTransientEStore() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PersistenceBackend transientBackend = persistenceBackendFactory.createTransientBackend();
        SearcheableResourceEStore eStore = persistenceBackendFactory.createTransientEStore(null, transientBackend);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteBlueprintsResourceEStoreImpl.class));
        PersistenceBackend innerBackend = getInnerBackend(eStore);
        assertEquals("The backend in the EStore is not the created one", transientBackend, innerBackend);
    }
    
    @Test
    public void testCreatePersistentBackendNoOptionNoConfigFile() throws InvalidDataStoreException {
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        assertThat("Invalid backend created", persistentBackend, instanceOf(BlueprintsPersistenceBackend.class));
        BlueprintsPersistenceBackend graph = (BlueprintsPersistenceBackend)persistentBackend;
        assertThat("The base graph is not the default TinkerGraph", graph.getBaseGraph(), instanceOf(TinkerGraph.class));
    }
    
    @Test
    public void testCreatePersistentEStoreNoOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, Collections.EMPTY_MAP);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteBlueprintsResourceEStoreImpl.class));
        PersistenceBackend innerBackend = getInnerBackend(eStore);
        assertEquals(persistentBackend, innerBackend);
    }
    
    @Test
    public void testCreatePersistentEStoreDirectWriteOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(BlueprintsResourceOptions.EStoreGraphOption.DIRECT_WRITE);
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend,options);
        assertThat("Invalid EStore created", eStore, instanceOf(DirectWriteBlueprintsResourceEStoreImpl.class));
        PersistenceBackend innerBackend = getInnerBackend(eStore);
        assertEquals("The backend in the EStore is not the created one", persistentBackend, innerBackend);
    }
    
    @Test
    public void testCreatePersistentEStoreAutocommitOption() throws InvalidDataStoreException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        storeOptions.add(BlueprintsResourceOptions.EStoreGraphOption.AUTOCOMMIT);
        PersistenceBackend persistentBackend = persistenceBackendFactory.createPersistentBackend(testFile, Collections.EMPTY_MAP);
        SearcheableResourceEStore eStore = persistenceBackendFactory.createPersistentEStore(null, persistentBackend, options);
        assertThat("Invalid EStore created", eStore, instanceOf(AutocommitBlueprintsResourceEStoreImpl.class));
        PersistenceBackend innerBackend = getInnerBackend(eStore);
        assertEquals("The backend in the EStore is not the created one", persistentBackend, innerBackend);
    }

}
