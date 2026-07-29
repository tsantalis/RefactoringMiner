package org.cloudfoundry.multiapps.mta.handlers;

import java.io.InputStream;
import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ConfigurationParserTest {

    private final Tester tester = Tester.forClass(getClass());

    private final String platformLocation;
    private InputStream platformInputStream;

    private final Expectation expectation;

    private ConfigurationParser parser = new ConfigurationParser();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid JSON:
            {
                "/mta/sample/platform-01.json", new Expectation(Expectation.Type.JSON, "platform-01.json.json"),
            },
            // (1) Invalid JSON (invalid key 'properties' in resource types):
            {
                "/mta/sample/platform-02.json", new Expectation(Expectation.Type.JSON, "platform-02.json.json"),
            },
            // (3) Valid JSON (containing only a name):
            {
                "/mta/sample/platform-03.json", new Expectation(Expectation.Type.JSON, "platform-03.json.json"),
            },
// @formatter:on
        });
    }

    public ConfigurationParserTest(String platformsLocation, Expectation expectation) {
        this.platformLocation = platformsLocation;
        this.expectation = expectation;
    }

    @Before
    public void setUp() throws Exception {
        if (platformLocation != null) {
            platformInputStream = getClass().getResourceAsStream(platformLocation);
        }
    }

    @Test
    public void testParsePlatformsJson() throws Exception {
        tester.test(() -> parser.parsePlatformJson(platformInputStream), expectation);
    }

}
