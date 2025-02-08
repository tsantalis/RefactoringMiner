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
package fr.inria.atlanmod.neoemf.map.datastore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Engine;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.AutocommitMapResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.CachedManyDirectWriteMapResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.DirectWriteMapResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.DirectWriteMapResourceWithListsEStoreImpl;
import fr.inria.atlanmod.neoemf.map.datastore.estores.impl.DirectWriteMapWithIndexesResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.map.resources.MapResourceOptions;
import fr.inria.atlanmod.neoemf.map.util.NeoMapURI;
import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceOptions;

public class MapPersistenceBackendFactory extends
		AbstractPersistenceBackendFactory {

    public static final String MAPDB_BACKEND = "mapdb";
    
	@Override
	public PersistenceBackend createTransientBackend() {
	    Engine mapEngine = DBMaker.newMemoryDB().makeEngine();
		return new MapPersistenceBackend(mapEngine);
	}

	@Override
	public SearcheableResourceEStore createTransientEStore(
			PersistentResource resource, PersistenceBackend backend) {
		assert backend instanceof DB : "Trying to create a Map-based EStore with an invalid backend";
		return new DirectWriteMapResourceEStoreImpl(resource, (DB)backend);
	}

	@Override
	public PersistenceBackend createPersistentBackend(File file,
			Map<?, ?> options) throws InvalidDataStoreException {
	    File dbFile = FileUtils.getFile(NeoMapURI.createNeoMapURI(URI.createFileURI(file.getAbsolutePath()).appendSegment("neoemf.mapdb")).toFileString());
	    if (!dbFile.getParentFile().exists()) {
	        dbFile.getParentFile().mkdirs();
	    }
	    PropertiesConfiguration neoConfig = null;
	    Path neoConfigPath = Paths.get(file.getAbsolutePath()).resolve(NEO_CONFIG_FILE);
        try {
            neoConfig= new PropertiesConfiguration(neoConfigPath.toFile());
        } catch (ConfigurationException e) {
            throw new InvalidDataStoreException(e);
        }
        if (!neoConfig.containsKey(BACKEND_PROPERTY)) {
            neoConfig.setProperty(BACKEND_PROPERTY, MAPDB_BACKEND);
        }
        if(neoConfig != null) {
            try {
                neoConfig.save();
            } catch(ConfigurationException e) {
                NeoLogger.log(NeoLogger.SEVERITY_ERROR, e);
            }
        }
//		Engine mapEngine = DBMaker.newFileDB(dbFile).cacheLRUEnable().mmapFileEnableIfSupported().asyncWriteEnable().makeEngine();
        // [TODO] Check the difference when asyncWriteEnable() is set, it has been desactived for MONDO deliverable but 
        // not well tested
	    Engine mapEngine = DBMaker.newFileDB(dbFile).cacheLRUEnable().mmapFileEnableIfSupported().makeEngine();
        return new MapPersistenceBackend(mapEngine);
	}

	@Override
	protected SearcheableResourceEStore internalCreatePersistentEStore(
			PersistentResource resource, PersistenceBackend backend, Map<?,?> options) throws InvalidDataStoreException {
		assert backend instanceof DB : "Trying to create a Map-based EStore with an invalid backend";
    	@SuppressWarnings("unchecked")
        ArrayList<PersistentResourceOptions.StoreOption> storeOptions = (ArrayList<PersistentResourceOptions.StoreOption>)options.get(PersistentResourceOptions.STORE_OPTIONS);
        if(storeOptions == null || storeOptions.isEmpty() || storeOptions.contains(MapResourceOptions.EStoreMapOption.DIRECT_WRITE)) {
            // Default store
            return new DirectWriteMapResourceEStoreImpl(resource, (MapPersistenceBackend)backend);
        }
        else {
            if(storeOptions.contains(MapResourceOptions.EStoreMapOption.AUTOCOMMIT)) {
                return new AutocommitMapResourceEStoreImpl(resource, (MapPersistenceBackend)backend);
            }
            else if(storeOptions.contains(MapResourceOptions.EStoreMapOption.CACHED_MANY)) {
                return new CachedManyDirectWriteMapResourceEStoreImpl(resource, (MapPersistenceBackend)backend);
            }
            else if(storeOptions.contains(MapResourceOptions.EStoreMapOption.DIRECT_WRITE_WITH_LISTS)) {
                return new DirectWriteMapResourceWithListsEStoreImpl(resource, (MapPersistenceBackend)backend);
            }
            else if(storeOptions.contains(MapResourceOptions.EStoreMapOption.DIRECT_WRITE_WITH_INDEXES)) {
                return new DirectWriteMapWithIndexesResourceEStoreImpl(resource, (MapPersistenceBackend)backend);
            }
            else {
                throw new InvalidDataStoreException();
            }
        }
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
	public void copyBackend(PersistenceBackend from, PersistenceBackend to) {
	    assert from instanceof MapPersistenceBackend : "The backend to copy is not an instance of MapPersistenceBackend";
	    assert to instanceof MapPersistenceBackend : "The target copy backend is not an instance of MapPersistenceBackend";
	    MapPersistenceBackend mapFrom = (MapPersistenceBackend)from;
	    MapPersistenceBackend mapTo = (MapPersistenceBackend)to;
	    Map<String,Object> fromAll = mapFrom.getAll();
	    for(String fromKey : fromAll.keySet()) {
	        Object collection = fromAll.get(fromKey);
	        if(collection instanceof Map) {
                Map fromMap = (Map)collection;
                Map toMap = mapTo.getHashMap(fromKey);
	            toMap.putAll(fromMap);
	        }
	        else {
	            throw new UnsupportedOperationException("Cannot copy Map backend: store type " + collection.getClass().getSimpleName() + " is not supported");
	        }
	    }
	}

}
