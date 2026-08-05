package org.cloudfoundry.multiapps.mta.serialization.v2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.parsers.v2.DeploymentDescriptorParser;
import org.cloudfoundry.multiapps.mta.parsers.v2.ExtensionDescriptorParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DescriptorSerializationTest {

    protected final Tester tester = Tester.forClass(getClass());

    protected String deploymentDescriptorLocation;
    protected Expectation expectedSerializedDescriptor;
    protected String extensionDescriptorLocation;
    protected Expectation expectedSerializedExtension;

    private InputStream deploymentDescriptorYaml;
    private InputStream extensionDescriptorYaml;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid deployment and extension descriptors:
            {
                "mtad-00.yaml", new Expectation(Expectation.Type.JSON, "serialized-descriptor-00.json"),
                "extension-descriptor-00.mtaext", new Expectation(Expectation.Type.JSON, "serialized-extension-00.json"),
            }
            // @formatter:on
        });
    }

    public DescriptorSerializationTest(String deploymentDescriptorLocation, Expectation expectedSerializedDescriptor,
                                       String extensionDescriptorLocation, Expectation expectedSerializedExtension) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.expectedSerializedDescriptor = expectedSerializedDescriptor;
        this.extensionDescriptorLocation = extensionDescriptorLocation;
        this.expectedSerializedExtension = expectedSerializedExtension;
    }

    @Before
    public void setUp() {
        deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
    }

    @Test
    public void testDescriptorSerialization() {
        tester.test(() -> {
            YamlParser yamlParser = new YamlParser();
            Map<String, Object> map = yamlParser.convertYamlToMap(deploymentDescriptorYaml);
            String serializedMap = yamlParser.convertToYaml(getDescriptorFromMap(map));
            return getDescriptorFromMap(yamlParser.convertYamlToMap(serializedMap));
        }, expectedSerializedDescriptor);
    }

    @Test
    public void testExtensionSerialization() {
        tester.test(() -> {
            YamlParser yamlParser = new YamlParser();
            Map<String, Object> map = yamlParser.convertYamlToMap(extensionDescriptorYaml);
            String serializedMap = yamlParser.convertToYaml(getExtensionDescriptorFromMap(map));
            return getExtensionDescriptorFromMap(yamlParser.convertYamlToMap(serializedMap));
        }, expectedSerializedExtension);
    }

    private DeploymentDescriptor getDescriptorFromMap(Map<String, Object> yamlMap) {
        return getDescriptorParser(yamlMap).parse();
    }

    protected DeploymentDescriptorParser getDescriptorParser(Map<String, Object> yamlMap) {
        return new DeploymentDescriptorParser(yamlMap);
    }

    private ExtensionDescriptor getExtensionDescriptorFromMap(Map<String, Object> yamlMap) {
        return getExtensionDescriptorParser(yamlMap).parse();
    }

    protected ExtensionDescriptorParser getExtensionDescriptorParser(Map<String, Object> yamlMap) {
        return new ExtensionDescriptorParser(yamlMap);
    }

}
