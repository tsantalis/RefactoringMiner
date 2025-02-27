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
package fr.inria.atlanmod.neoemf.graph.blueprints.neo4j.resources;

import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.graph.blueprints.resources.BlueprintsResourceSaveTest;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BlueprintsNeo4jResourceSaveTest extends BlueprintsResourceSaveTest {

    private static final String TEST_FILENAME = "graphNeo4jResourceSaveOptionTestFile";

    /**
     * Used to verify a property added by Blueprints during the graph creation
     */
    private static final String OPTIONS_GRAPH_NEO4J_STORE_DIR = "blueprints.neo4j.conf.store_dir";
    /**
     * Number of properties for an empty Neo4j instance (with no option provided)
     */
    private static final int defaultPropertyCount = 3;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        this.testFilePath = TEST_FILENAME;
        super.setUp();
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE, BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_TYPE_NEO4J);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNeo4jTypeOption() throws IOException, ConfigurationException {
        options.clear();
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE, BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_TYPE_NEO4J);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_GRAPH_TYPE), is(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_TYPE_NEO4J));
        /*
         * Check the configuration file contains the store_dir property (computed by blueprints at graph creation)
         */
        assertThat(configuration.containsKey(OPTIONS_GRAPH_NEO4J_STORE_DIR), is(true));
        assertThat(configuration.getString(OPTIONS_GRAPH_NEO4J_STORE_DIR), is(testFile.getAbsolutePath()));

        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNoneCacheTypeOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE, BlueprintsNeo4jResourceOptions.CACHE_TYPE.NONE);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(BlueprintsNeo4jResourceOptions.CACHE_TYPE.NONE.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the cache_type properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceWeakCacheTypeOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE, BlueprintsNeo4jResourceOptions.CACHE_TYPE.WEAK);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(BlueprintsNeo4jResourceOptions.CACHE_TYPE.WEAK.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the cache_type properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceSoftCacheTypeOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE, BlueprintsNeo4jResourceOptions.CACHE_TYPE.SOFT);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(BlueprintsNeo4jResourceOptions.CACHE_TYPE.SOFT.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the cache_type properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceStrongCacheTypeOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE, BlueprintsNeo4jResourceOptions.CACHE_TYPE.STRONG);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(BlueprintsNeo4jResourceOptions.CACHE_TYPE.STRONG.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the use_memory_mapped_buffers properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceTrueUseMemoryMappedBuffersOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS, BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.TRUE);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.TRUE.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the use_memory_mapped_buffers properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceTrueBooleanUseMemoryMappedBuffersOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS, true);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.TRUE.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the use_memory_mapped_buffers properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceFalseUseMemoryMappedBuffersOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS, BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.FALSE);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.FALSE.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the use_memory_mapped_buffers properties in the configuration file.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceFalseBooleanUseMemoryMappedBuffersOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS, false);
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.FALSE.toString()));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the strings_mapped_memory properties in the configuration file (with
     * a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositiveStringsMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property strings_mapped_memory
     * is set with a negative value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourceNegativeStringsMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY, "-64M");
        resource.save(options);
    }

    /**
     * Test the existence of the strings_mapped_memory properties in the configuration file (with
     * a value set to 0M).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNullStringsMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY, "0M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is("0M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the arrays_mapped_memory properties in the configuration file (with
     * a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositiveArraysMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property arrays_mapped_memory
     * is set with a negative value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourceNegativeArraysMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY, "-64M");
        resource.save(options);
    }

    /**
     * Test the existence of the arrays_mapped_memory properties in the configuration file (with
     * a value set to 0M).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNullArraysMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY, "0M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is("0M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the nodes_mapped_memory properties in the configuration file (with
     * a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositiveNodesMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property nodes_mapped_memory
     * is set with a negative value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourceNegativeNodesMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY, "-64");
        resource.save(options);
    }

    /**
     * Test the existence of the nodes_mapped_memory properties in the configuration file (with
     * a value set to 0M).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNullNodesMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY, "0M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is("0M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the properties_mapped_memory properties in the configuration file (with
     * a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositivePropertiesMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property properties_mapped_memory
     * is set with a negative value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourceNegativePropertiesMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "-64M");
        resource.save(options);
    }

    /**
     * Test the existence of the properties_mapped_memory properties in the configuration file (with
     * a value set to 0M).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNullPropertiesMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "0M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is("0M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of the relationships_mapped_memory properties in the configuration file (with
     * a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositiveRelationshipsMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property relationships_mapped_memory
     * is set with a negative value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourceNegativeRelationshipsMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "-64M");
        resource.save(options);
    }

    /**
     * Test the existence of the relationships_mapped_memory properties in the configuration file (with
     * a value set to 0M).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceNullRelationshipsMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "0M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is("0M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 1));
    }

    /**
     * Test the existence of relationships_mapped_memory and properties_mapped_memory properties 
     * in the configuration file (with a positive value).
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourcePositiveRelationshipMappedMemoryPositivePropertiesMappedMemoryOption() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is("64M"));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 2));
    }

    /**
     * Test an exception is thrown when the resource is saved if the property properties_mapped_memory
     * is set with a negative value and the property relationships_mapped_memory is set with a positive
     * value.
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidDataStoreException.class)
    public void testSaveGraphNeo4jResourcePositiveRelationshipMappedMemoryNegativePropertiesMappedMemoryOption() throws IOException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "-64M");
        resource.save(options);
    }

    /**
     * Test the existence of all the properties in the configuration file (all options given to the
     * resource are valid).
     * Tested options are:
     *  - cache_type
     *  - use_memory_mapped_buffers
     *  - strings_mapped_memory
     *  - arrays_mapped_memory
     *  - nodes_mapped_memory
     *  - properties_mapped_memory
     *  - relationships_mapped_memory
     * This test use the option @see{BlueprintsNeo4jResourceOptions.OPTIONS_GRAPH_TYPE_NEO4J}
     * but does not test it (it is done in @see{testSaveGraphNeo4jResourceNeo4jTypeOption()})
     * In addition, there is no verification on the OPTIONS_GRAPH_NEO4J_STORE_DIR (it is done in
     * @see{testSaveGraphNeo4jResourceNeo4jTypeOption()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSaveGraphNeo4jResourceAllOptionsValid() throws IOException, ConfigurationException {
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE, BlueprintsNeo4jResourceOptions.CACHE_TYPE.SOFT);
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS, BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.TRUE);
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY, "64M");
        options.put(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY, "64M");
        resource.save(options);

        File configFile = new File(testFile + configFileName);
        assertThat(configFile.exists(), is(true));

        PropertiesConfiguration configuration = new PropertiesConfiguration(configFile);
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_CACHE_TYPE), is(BlueprintsNeo4jResourceOptions.CACHE_TYPE.SOFT.toString()));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_USE_MEMORY_MAPPED_BUFFERS), is(BlueprintsNeo4jResourceOptions.USE_MEMORY_MAPPED_BUFFER.TRUE.toString()));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_STRINGS_MAPPED_MEMORY), is("64M"));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_ARRAYS_MAPPED_MEMORY), is("64M"));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_NODES_MAPPED_MEMORY), is("64M"));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_PROPERTIES_MAPPED_MEMORY), is("64M"));
        assertThat(configuration.containsKey(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is(true));
        assertThat(configuration.getString(BlueprintsNeo4jResourceOptions.OPTIONS_BLUEPRINTS_NEO4J_RELATIONSHIPS_MAPPED_MEMORY), is("64M"));
        assertThat("The number of properties in the configuration file is not consistent with the given options", getKeyCount(configuration), is(defaultPropertyCount + 7));
    }
}
