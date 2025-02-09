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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.emf.common.util.URI;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphFactory;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.GraphHelper;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.estores.impl.AutocommitBlueprintsResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.estores.impl.DirectWriteBlueprintsResourceEStoreImpl;
import fr.inria.atlanmod.neoemf.graph.blueprints.resources.BlueprintsResourceOptions;
import fr.inria.atlanmod.neoemf.graph.blueprints.tg.config.AbstractBlueprintsConfig;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;
import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceOptions;

public class BlueprintsPersistenceBackendFactory extends
		AbstractPersistenceBackendFactory {

	/**
	 * The configuration file name. This file stores the metadata information
	 * about the underlying graph, i.e., graph type and other configuration
	 * options
	 */
	protected static final String CONFIG_FILE = "config.properties";
	public static final String BLUEPRINTS_BACKEND = "blueprints";

	@Override
	public PersistenceBackend createTransientBackend() {
		return new BlueprintsPersistenceBackend(new TinkerGraph());
	}
	
	@Override
	public SearcheableResourceEStore createTransientEStore(
			PersistentResource resource, PersistenceBackend backend) {
		assert backend instanceof BlueprintsPersistenceBackend : "Trying to create a Graph-based EStore with an invalid backend";
		return new DirectWriteBlueprintsResourceEStoreImpl(resource, (BlueprintsPersistenceBackend)backend);
	}
	
	@Override
	public BlueprintsPersistenceBackend createPersistentBackend(File file, Map<?, ?> options) throws InvalidDataStoreException {
		BlueprintsPersistenceBackend graphDB = null;
		PropertiesConfiguration neoConfig = null;
		PropertiesConfiguration configuration = null;
		try {
			// Try to load previous configurations
			Path path = Paths.get(file.getAbsolutePath()).resolve(CONFIG_FILE);
			try {
				configuration = new PropertiesConfiguration(path.toFile());
			} catch (ConfigurationException e) {
				throw new InvalidDataStoreException(e);
			}
			// Initialize value if the config file has just been created
			if (!configuration.containsKey(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE)) {
				configuration.setProperty(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE, BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE_DEFAULT);
			} else if (options.containsKey(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE)) {
				// The file already existed, check that the issued options
				// are not conflictive
				String savedGraphType = configuration.getString(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE);
				String issuedGraphType = options.get(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE).toString();
				if (!savedGraphType.equals(issuedGraphType)) {
				    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Unable to create graph as type " + issuedGraphType + ", expected graph type was " + savedGraphType + ")");
					throw new InvalidDataStoreException("Unable to create graph as type " + issuedGraphType + ", expected graph type was " + savedGraphType + ")");
				}
			}

			// Copy the options to the configuration
			for (Entry<?, ?> e : options.entrySet()) {
				configuration.setProperty(e.getKey().toString(), e.getValue().toString());
			}

			// Check we have a valid graph type, it is needed to get the
			// graph name
			String graphType = configuration.getString(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE);
			if (graphType == null) {
				throw new InvalidDataStoreException("Graph type is undefined for " + file.getAbsolutePath());
			}
			
			String[] segments = graphType.split("\\.");
			if(segments.length >= 2) {
    			String graphName = segments[segments.length - 2];
    			String upperCaseGraphName = Character.toUpperCase(graphName.charAt(0))+graphName.substring(1);
    			String configClassQualifiedName = MessageFormat.format("fr.inria.atlanmod.neoemf.graph.blueprints.{0}.config.Blueprints{1}Config", graphName, upperCaseGraphName);
    			try {
                    ClassLoader classLoader = BlueprintsPersistenceBackendFactory.class
                            .getClassLoader();
                    Class<?> configClass = classLoader.loadClass(configClassQualifiedName);
                    Field configClassInstanceField = configClass.getField("eINSTANCE");
                    AbstractBlueprintsConfig configClassInstance = (AbstractBlueprintsConfig) configClassInstanceField
                            .get(configClass);
                    Method configMethod = configClass.getMethod("putDefaultConfiguration",
                            Configuration.class, File.class);
                    configMethod.invoke(configClassInstance, configuration, file);
                    Method setGlobalSettingsMethod = configClass.getMethod("setGlobalSettings");
                    setGlobalSettingsMethod.invoke(configClassInstance);
                } catch (ClassNotFoundException e1) {
                    NeoLogger.log(NeoLogger.SEVERITY_WARNING,
                            "Unable to find the configuration class " + configClassQualifiedName);
                    e1.printStackTrace();
                } catch (NoSuchFieldException e2) {
                    NeoLogger
                            .log(NeoLogger.SEVERITY_WARNING,
                                    MessageFormat
                                            .format("Unable to find the static field eINSTANCE in class Blueprints{0}Config",
                                                    upperCaseGraphName));
                    e2.printStackTrace();
                } catch (NoSuchMethodException e3) {
                    NeoLogger.log(NeoLogger.SEVERITY_ERROR, MessageFormat.format(
                            "Unable to find configuration methods in class Blueprints{0}Config",
                            upperCaseGraphName));
                    e3.printStackTrace();
                } catch (InvocationTargetException e4) {
                    NeoLogger.log(NeoLogger.SEVERITY_ERROR, MessageFormat.format(
                            "An error occured during the exection of a configuration method",
                            upperCaseGraphName));
                    e4.printStackTrace();
                } catch (IllegalAccessException e5) {
                    NeoLogger.log(NeoLogger.SEVERITY_ERROR, MessageFormat.format(
                            "An error occured during the exection of a configuration method",
                            upperCaseGraphName));
                    e5.printStackTrace();
                }
            }
			else {
			    NeoLogger.log(NeoLogger.SEVERITY_WARNING, "Unable to compute graph type name from " + graphType);
			}

			Graph baseGraph = null;
			try {
			    baseGraph = GraphFactory.open(configuration);
			}catch(RuntimeException e) {
			    throw new InvalidDataStoreException(e);
			}
			if (baseGraph instanceof KeyIndexableGraph) {
				graphDB = new BlueprintsPersistenceBackend((KeyIndexableGraph) baseGraph);
			} else {
			    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Graph type " +file.getAbsolutePath()+" does not support Key Indices");
				throw new InvalidDataStoreException("Graph type "+file.getAbsolutePath()+" does not support Key Indices");
			}
			// Save the neoconfig file
			Path neoConfigPath = Paths.get(file.getAbsolutePath()).resolve(NEO_CONFIG_FILE);
            try {
                neoConfig= new PropertiesConfiguration(neoConfigPath.toFile());
            } catch (ConfigurationException e) {
                throw new InvalidDataStoreException(e);
            }
            if (!neoConfig.containsKey(BACKEND_PROPERTY)) {
                neoConfig.setProperty(BACKEND_PROPERTY, BLUEPRINTS_BACKEND);
            }
		} finally {
			if (configuration != null) {
				try {
					configuration.save();
				} catch (ConfigurationException e) {
					// Unable to save configuration, supposedly it's a minor error,
					// so we log it without rising an exception
					NeoLogger.log(NeoLogger.SEVERITY_ERROR, e);
				}
			}
			if(neoConfig != null) {
			    try {
			        neoConfig.save();
			    } catch(ConfigurationException e) {
			        NeoLogger.log(NeoLogger.SEVERITY_ERROR, e);
			    }
			}
		}
		return graphDB;
	}
	
	@Override
	protected SearcheableResourceEStore internalCreatePersistentEStore(
			PersistentResource resource, PersistenceBackend backend, Map<?,?> options) throws InvalidDataStoreException {
		assert backend instanceof BlueprintsPersistenceBackend : "Trying to create a Graph-based EStore with an invalid backend";
    	@SuppressWarnings("unchecked")
        ArrayList<PersistentResourceOptions.StoreOption> storeOptions = (ArrayList<PersistentResourceOptions.StoreOption>)options.get(PersistentResourceOptions.STORE_OPTIONS);
    	if(storeOptions == null || storeOptions.isEmpty() || storeOptions.contains(BlueprintsResourceOptions.EStoreGraphOption.DIRECT_WRITE)) {
    	    // Default store
    	    return new DirectWriteBlueprintsResourceEStoreImpl(resource, (BlueprintsPersistenceBackend)backend);
    	}
    	else {
    	    if(storeOptions.contains(BlueprintsResourceOptions.EStoreGraphOption.AUTOCOMMIT)) {
    	        return new AutocommitBlueprintsResourceEStoreImpl(resource, (BlueprintsPersistenceBackend)backend);
    	    }
    	    else {
    	        throw new InvalidDataStoreException();
    	    }
    	}
	}
	
	@Override
	public void copyBackend(PersistenceBackend from, PersistenceBackend to) {
		assert from instanceof BlueprintsPersistenceBackend && to instanceof BlueprintsPersistenceBackend : "Trying to use Graph backend copy on non Graph databases";
		BlueprintsPersistenceBackend bFrom = (BlueprintsPersistenceBackend)from;
		BlueprintsPersistenceBackend bTo = (BlueprintsPersistenceBackend)to;
	    GraphHelper.copyGraph(bFrom, bTo);
	    bTo.initMetaClassesIndex(bFrom.getIndexedEClasses());
	}
}
