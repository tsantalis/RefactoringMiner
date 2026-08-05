package ninja.javafx.smartcsv.validation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ninja.javafx.smartcsv.validation.configuration.ValidationConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * unit test for validator
 */
@RunWith(Parameterized.class)
public class ValidatorTest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String config;
    private String value;
    private Boolean expectedResult;
    private ValidationMessage expectedError;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // subject under test
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private Validator sut;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameterized constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ValidatorTest(String config,
                         String value,
                         Boolean expectedResult,
                         ValidationMessage expectedError) {
        this.config = config;
        this.value = value;
        this.expectedResult = expectedResult;
        this.expectedError = expectedError;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // init
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Before
    public void initialize() {
        Gson gson = new GsonBuilder().create();
        ValidationConfiguration validationConfiguration = gson.fromJson(config, ValidationConfiguration.class);
        sut = new Validator(validationConfiguration, new TestColumnValueProvider());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void validation() {
        System.out.println("===================================================");
        System.out.println(config);
        System.out.println(value + "  " + expectedResult + "  " + expectedError);
        System.out.println("===================================================\n");


        // execution
        ValidationError result = sut.isValid(0, "column", value);

        // assertion
        assertThat(result == null, is(expectedResult));
        if (!expectedResult) {
            assertThat(result.getMessages(), contains(expectedError));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameters for tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Parameterized.Parameters
    public static Collection validationConfigurations() {
        return asList(new Object[][] {
                { constraintsJSON("string", "required", true),  "value", true, null },
                { constraintsJSON("string", "required", true),  "", false, new ValidationMessage("validation.message.not.empty") },
                { constraintsJSON("string", "required", true),  null, false, new ValidationMessage("validation.message.not.empty") },
                { constraintsJSON("integer", null, true),  "999", true, null },
                { constraintsJSON("integer", null, true),  "a", false, new ValidationMessage("validation.message.integer") },
                { constraintsJSON("number", null, true),  "999", true, null },
                { constraintsJSON("number", null, true),  "999.000", true, null },
                { constraintsJSON("number", null, true),  "a", false, new ValidationMessage("validation.message.double") },
                { constraintsJSON("string", "minLength", 2),  "12", true, null },
                { constraintsJSON("string", "minLength", 2),  "1", false, new ValidationMessage("validation.message.min.length", "2") },
                { constraintsJSON("string", "maxLength", 2),  "12", true, null },
                { constraintsJSON("string", "maxLength", 2),  "123", false, new ValidationMessage("validation.message.max.length", "2") },
                { constraintsJSON("string", "pattern", "[a-z]*"),  "abc", true, null },
                { constraintsJSON("string", "pattern", "[a-z]*"),  "abcA", false, new ValidationMessage("validation.message.regexp", "[a-z]*") },
                { constraintsJSON("string", "enum", asList("a","b","c","d","e")),  "c", true, null },
                { constraintsJSON("string", "enum", asList("a","b","c","d","e")),  "f", false, new ValidationMessage("validation.message.value.of", "f", "a, b, c, d, e") },

                { formatJSON("string", null), "some string", true, null },
                { formatJSON("string", "email"), "test@javafx.ninja", true, null },
                { formatJSON("string", "email"), "wrong email", false, new ValidationMessage("validation.message.email") },
                { formatJSON("string", "uri"), "http://www.javafx.ninja", true, null },
                { formatJSON("string", "uri"), "!$%&/()", false, new ValidationMessage("validation.message.uri") },
                { formatJSON("string", "binary"), "dGVzdA==", true, null },
                { formatJSON("string", "binary"), "no binary", false, new ValidationMessage("validation.message.binary") },
                { formatJSON("string", "uuid"), "6ba7b810-9dad-11d1-80b4-00c04fd430c8", true, null },
                { formatJSON("string", "uuid"), "no uuid", false, new ValidationMessage("validation.message.uuid") },
                { formatJSON("date", null),  "2015-11-27", true, null },
                { formatJSON("date", null),  "27.11.2015", false, new ValidationMessage("validation.message.date.format", "yyyy-MM-dd") },
                { formatJSON("date", "yyyyMMdd"),  "20151127", true, null },
                { formatJSON("date", "yyyyMMdd"),  "27.11.2015", false, new ValidationMessage("validation.message.date.format", "yyyyMMdd") },


                { customAttributeJSON( "groovy", "value.contains('a')? 'true' : 'no a inside'"),  "abcdef", true, null },
                { customAttributeJSON( "groovy", "value.contains('a')? 'true' : 'no a inside'"),  "bcdefg", false, new ValidationMessage("no a inside") },

        });
    }

    public static String constraintsJSON(String type, String constraint, Object value) {
        String json = "{\"fields\": [ { \"name\": \"column\", \"type\" : \"" + type +"\"";

        if (constraint != null) {
            json += ", \"constraints\": { \"" + constraint + "\":";
            if (value instanceof String) {
                json += "\"" + value + "\"";
            } else if (value instanceof List) {
                List<String> list = (List<String>) value;
                json += "[" + list.stream().collect(joining(", ")) + "]";
            } else {
                json += value;
            }
            json += "}";
        }
        json += "}]}";
        return json;
    }

    public static String formatJSON(String type, String format) {
        String json = "{\"fields\": [ { \"name\": \"column\", \"type\" : \"" + type +"\"";

        if (format != null) {
            json += ", \"format\": \"" + format + "\"";
        }
        json += "}]}";
        return json;
    }

    public static String customAttributeJSON(String attribute, String value) {
        String json = "{\"fields\": [ { \"name\": \"column\", \"type\" : \"string\"";

        if (attribute != null) {
            json += ", \""+attribute+"\": \"" + value + "\"";
        }
        json += "}]}";
        return json;
    }
}