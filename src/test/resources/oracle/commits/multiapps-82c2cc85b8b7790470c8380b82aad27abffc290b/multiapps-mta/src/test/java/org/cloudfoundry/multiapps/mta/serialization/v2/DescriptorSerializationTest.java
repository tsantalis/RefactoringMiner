package org.cloudfoundry.multiapps.mta.serialization.v2;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.parsers.v2.DeploymentDescriptorParser;
import org.cloudfoundry.multiapps.mta.parsers.v2.ExtensionDescriptorParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DescriptorSerializationTest {

    protected final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testDeploymentDescriptorSerialization() {
        return Stream.of(Arguments.of("mtad-00.yaml", new Expectation(Expectation.Type.JSON, "serialized-descriptor-00.json")));
    }

    static Stream<Arguments> testExtensionDescriptorSerialization() {
        return Stream.of(Arguments.of("extension-descriptor-00.mtaext",
                                      new Expectation(Expectation.Type.JSON, "serialized-extension-00.json")));
    }

    @ParameterizedTest
    @MethodSource
    void testDeploymentDescriptorSerialization(String deploymentDescriptorLocation, Expectation expectation) {
        InputStream deploymentDescriptorYaml = getClass().getResourceAsStream(deploymentDescriptorLocation);
        tester.test(() -> {
            YamlParser yamlParser = new YamlParser();
            Map<String, Object> map = yamlParser.convertYamlToMap(deploymentDescriptorYaml);
            String serializedMap = yamlParser.convertToYaml(parseDeploymentDescriptor(map));
            return parseDeploymentDescriptor(yamlParser.convertYamlToMap(serializedMap));
        }, expectation);
    }

    @ParameterizedTest
    @MethodSource
    void testExtensionDescriptorSerialization(String extensionDescriptorLocation, Expectation expectation) {
        InputStream extensionDescriptorYaml = getClass().getResourceAsStream(extensionDescriptorLocation);
        tester.test(() -> {
            YamlParser yamlParser = new YamlParser();
            Map<String, Object> map = yamlParser.convertYamlToMap(extensionDescriptorYaml);
            String serializedMap = yamlParser.convertToYaml(parseExtensionDescriptor(map));
            return parseExtensionDescriptor(yamlParser.convertYamlToMap(serializedMap));
        }, expectation);
    }

    private DeploymentDescriptor parseDeploymentDescriptor(Map<String, Object> yamlMap) {
        return createDeploymentDescriptorParser(yamlMap).parse();
    }

    private ExtensionDescriptor parseExtensionDescriptor(Map<String, Object> yamlMap) {
        return createExtensionDescriptorParser(yamlMap).parse();
    }

    protected DeploymentDescriptorParser createDeploymentDescriptorParser(Map<String, Object> yamlMap) {
        return new DeploymentDescriptorParser(yamlMap);
    }

    protected ExtensionDescriptorParser createExtensionDescriptorParser(Map<String, Object> yamlMap) {
        return new ExtensionDescriptorParser(yamlMap);
    }

}
