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
package fr.inria.atlanmod.neoemf.graph.blueprints.resources;

import fr.inria.atlanmod.neoemf.datastore.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.BlueprintsPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.graph.blueprints.util.NeoBlueprintsURI;
import fr.inria.atlanmod.neoemf.resources.impl.PersistentResourceFactoryImpl;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BlueprintsResourceSaveTest {

    private static final Path TEST_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "NeoEMF");
    private static final String TEST_FILENAME = "graphResourceSaveOptionTestFile";

    protected String testFilePath = TEST_FILENAME;

    protected String configFileName = "/config.properties";

    protected AbstractPersistenceBackendFactory persistenceBackendFactory = null;
    protected File testFile = null;
    @SuppressWarnings("rawtypes")
    protected Map options;
    protected ResourceSet resSet;
    protected Resource resource;

    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() {
        options = new HashMap();
        persistenceBackendFactory = new BlueprintsPersistenceBackendFactory();

        PersistenceBackendFactoryRegistry.getFactories().put(NeoBlueprintsURI.NEO_GRAPH_SCHEME, persistenceBackendFactory);
        testFile = TEST_DIR.resolve(testFilePath + String.valueOf(new Date().getTime())).toFile();
        resSet = new ResourceSetImpl();
        resSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put(NeoBlueprintsURI.NEO_GRAPH_SCHEME, new PersistentResourceFactoryImpl());
        resource = resSet.createResource(NeoBlueprintsURI.createNeoGraphURI(testFile));
    }

    @After
    public void tearDown() {
        resource.unload();
        resSet.getResourceFactoryRegistry().getProtocolToFactoryMap().clear();
        PersistenceBackendFactoryRegistry.getFactories().clear();
        if (testFile != null) {
            try {
                FileUtils.forceDelete(testFile);
            } catch (IOException e) {
                //System.err.println(e);
            }
            testFile = null;
        }
    }

    protected int getKeyCount(PropertiesConfiguration configuration) {
        @SuppressWarnings("unchecked")
        Iterator<String> keyIterator = configuration.getKeys();
        int keyCount = 0;
        while (keyIterator.hasNext()) {
            keyCount++;
            keyIterator.next();
        }
        return keyCount;
    }

    @Test
    public void testSaveGraphResourceNoOption() throws IOException, ConfigurationException {
        resource.save(Collections.EMPTY_MAP);

        File configFile = new File(testFile + configFileName);
        assertThat("Config file does not exist", configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE_DEFAULT));
        assertThat("Too much content in the .properties file", getKeyCount(configuration), is(3));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphResourceDefaultGraphTypeOption() throws IOException, ConfigurationException {
        options.put(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE, BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE_DEFAULT);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat("Config file does not exist", configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(BlueprintsResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE_DEFAULT));
        assertThat("Too much content in the .properties file", getKeyCount(configuration), is(3));
    }

}
