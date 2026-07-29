package org.cloudfoundry.multiapps.mta.serialization.v3;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.parsers.v3.DeploymentDescriptorParser;
import org.cloudfoundry.multiapps.mta.parsers.v3.ExtensionDescriptorParser;
import org.junit.jupiter.params.provider.Arguments;

public class DescriptorSerializationTest extends org.cloudfoundry.multiapps.mta.serialization.v2.DescriptorSerializationTest {

    static Stream<Arguments> testDeploymentDescriptorSerialization() {
        return Stream.of(Arguments.of("mtad-00.yaml", new Expectation(Expectation.Type.JSON, "serialized-descriptor-00.json")),
                         Arguments.of("mtad-01.yaml", new Expectation(Expectation.Type.JSON, "serialized-descriptor-01.json")));
    }

    static Stream<Arguments> testExtensionDescriptorSerialization() {
        return Stream.of(Arguments.of("extension-descriptor-00.mtaext",
                                      new Expectation(Expectation.Type.JSON, "serialized-extension-00.json")),
                         Arguments.of("extension-descriptor-01.mtaext",
                                      new Expectation(Expectation.Type.JSON, "serialized-extension-01.json")));
    }

    @Override
    protected DeploymentDescriptorParser createDeploymentDescriptorParser(Map<String, Object> yamlMap) {
        return new DeploymentDescriptorParser(yamlMap);
    }

    @Override
    protected ExtensionDescriptorParser createExtensionDescriptorParser(Map<String, Object> yamlMap) {
        return new ExtensionDescriptorParser(yamlMap);
    }

}
