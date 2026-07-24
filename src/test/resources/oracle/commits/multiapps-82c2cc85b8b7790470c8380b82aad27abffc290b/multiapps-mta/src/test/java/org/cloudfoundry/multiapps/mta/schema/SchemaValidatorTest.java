package org.cloudfoundry.multiapps.mta.schema;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.v2.Schemas;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SchemaValidatorTest {

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testValidateSchema() {
        return Stream.of(
                         // Valid extension descriptor:
                         Arguments.of("/mta/sample/v2/config-01-v2.mtaext", Schemas.MTAEXT, new Expectation(null)),
                         // Valid deployment descriptor:
                         Arguments.of("/mta/sample/v2/mtad-01-v2.yaml", Schemas.MTAD, new Expectation(null)),
                         // Valid platform JSON:
                         Arguments.of("/mta/sample/platform-01.json", Schemas.PLATFORM, new Expectation(null)),
                         // Deployment descriptor is missing a required key:
                         Arguments.of("mtad-03.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION, "Missing required key \"ID\"")),
                         // Deployment descriptor module has invalid content for requires dependency:
                         Arguments.of("mtad-04.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Invalid type for key \"modules#0#requires\", expected \"List\" but got \"String\"")),
                         // Deployment descriptor has an invalid ID:
                         Arguments.of("mtad-06.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Invalid value for key \"ID\", matching failed at \"com[/]sap/mta/sample\"")),
                         // Deployment descriptor has a too long ID:
                         Arguments.of("mtad-07.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION, "Invalid value for key \"ID\", maximum length is 128")),
                         // Deployment descriptor provided dependency has an invalid name:
                         Arguments.of("mtad-08.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Invalid value for key \"modules#0#provides#0#name\", matching failed at \"internal-od[@]ta\"")),
                         // Deployment descriptor module has an invalid name:
                         Arguments.of("mtad-09.yaml", Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Invalid value for key \"modules#0#name\", matching failed at \"web[ ]server\"")),
                         // Deployment descriptor module provides public has String, but not a Boolean value:
                         Arguments.of("mtad-10.yaml", org.cloudfoundry.multiapps.mta.handlers.v2.Schemas.MTAD,
                                      new Expectation(Expectation.Type.EXCEPTION,
                                                      "Invalid type for key \"modules#0#provides#0#public\", expected \"Boolean\" but got \"String\"")),
                         // Null content:
                         Arguments.of(null, Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Null content")));
    }

    @ParameterizedTest
    @MethodSource
    void testValidateSchema(String file, Element schema, Expectation expectation) throws Exception {
        SchemaValidator validator = new SchemaValidator(schema);
        tester.test(() -> {
            if (schema instanceof MapElement) {
                validator.validate(TestUtil.getMap(file, getClass()));
            } else if (schema instanceof ListElement) {
                validator.validate(TestUtil.getList(file, getClass()));
            }
        }, expectation);
    }

}
