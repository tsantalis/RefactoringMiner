package org.cloudfoundry.multiapps.mta.schema;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.handlers.v2.Schemas;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SchemaValidatorTest {

    private final Tester tester = Tester.forClass(getClass());

    private final Element schema;
    private final String file;
    private final Expectation expectation;

    private SchemaValidator validator;

    @Parameters
    public static Iterable<Object[]> data1() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Valid extension descriptor:
            {
                "/mta/sample/v2/config-01-v2.mtaext", Schemas.MTAEXT, new Expectation(null),
            },
            // (01) Valid deployment descriptor:
            {
                "/mta/sample/v2/mtad-01-v2.yaml", Schemas.MTAD, new Expectation(null),
            },
            // (02) Valid platform JSON:
            {
                "/mta/sample/platform-01.json", Schemas.PLATFORM, new Expectation(null),
            },
            // (03) Deployment descriptor is missing a required key:
            {
                "mtad-03.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Missing required key \"ID\""),
            },
            // (04) Deployment descriptor module has invalid content for requires dependency:
            {
                "mtad-04.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid type for key \"modules#0#requires\", expected \"List\" but got \"String\""),
            },
            // (05) Deployment descriptor has an invalid ID:
            {
                "mtad-06.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid value for key \"ID\", matching failed at \"com[/]sap/mta/sample\""),
            },
            // (06) Deployment descriptor has a too long ID:
            {
                "mtad-07.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid value for key \"ID\", maximum length is 128"),
            },
            // (07) Deployment descriptor provided dependency has an invalid name:
            {
                "mtad-08.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid value for key \"modules#0#provides#0#name\", matching failed at \"internal-od[@]ta\""),
            },
            // (08) Deployment descriptor module has an invalid name:
            {
                "mtad-09.yaml", Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid value for key \"modules#0#name\", matching failed at \"web[ ]server\""),
            },
            // (09) Deployment descriptor module provides public has String, but not a Boolean value:
            {
                "mtad-10.yaml", org.cloudfoundry.multiapps.mta.handlers.v2.Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Invalid type for key \"modules#0#provides#0#public\", expected \"Boolean\" but got \"String\""),
            },
            // (10) Null content:
            {
                null, Schemas.MTAD, new Expectation(Expectation.Type.EXCEPTION, "Null content"),
            },
// @formatter:on
        });
    }

    public SchemaValidatorTest(String file, Element schema, Expectation expectation) {
        this.schema = schema;
        this.file = file;
        this.expectation = expectation;
    }

    @Before
    public void setUp() throws Exception {
        validator = new SchemaValidator(schema);
    }

    @Test
    public void testValidateSchema() throws Exception {
        tester.test(() -> {
            if (schema instanceof MapElement) {
                validator.validate(TestUtil.getMap(file, getClass()));
            } else if (schema instanceof ListElement) {
                validator.validate(TestUtil.getList(file, getClass()));
            }
        }, expectation);
    }

}
