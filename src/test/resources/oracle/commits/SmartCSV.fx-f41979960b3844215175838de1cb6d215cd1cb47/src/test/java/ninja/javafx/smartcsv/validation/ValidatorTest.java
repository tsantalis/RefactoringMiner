package ninja.javafx.smartcsv.validation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ninja.javafx.smartcsv.validation.configuration.ValidationConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * unit test for validator
 */
public class ValidatorTest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @ParameterizedTest
    @MethodSource("validationConfigurations")
    public void validation(String config,
                           String value,
                           Boolean expectedResult,
                           ValidationMessage expectedError) {
        // setup
        Gson gson = new GsonBuilder().create();
        ValidationConfiguration validationConfiguration = gson.fromJson(config, ValidationConfiguration.class);
        Validator sut = new Validator(validationConfiguration, new TestColumnValueProvider());

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
    public static Stream<Arguments> validationConfigurations() {
        return Stream.of(
                Arguments.of( constraintsJSON("string", "required", true),  "value", true, null ),
                Arguments.of( constraintsJSON("string", "required", true),  "", false, new ValidationMessage("validation.message.not.empty") ),
                Arguments.of( constraintsJSON("string", "required", true),  null, false, new ValidationMessage("validation.message.not.empty") ),
                Arguments.of( constraintsJSON("integer", null, true),  "999", true, null ),
                Arguments.of( constraintsJSON("integer", null, true),  "a", false, new ValidationMessage("validation.message.integer") ),
                Arguments.of( constraintsJSON("number", null, true),  "999", true, null ),
                Arguments.of( constraintsJSON("number", null, true),  "999.000", true, null ),
                Arguments.of( constraintsJSON("number", null, true),  "a", false, new ValidationMessage("validation.message.double") ),
                Arguments.of( constraintsJSON("string", "minLength", 2),  "12", true, null ),
                Arguments.of( constraintsJSON("string", "minLength", 2),  "1", false, new ValidationMessage("validation.message.min.length", "2") ),
                Arguments.of( constraintsJSON("string", "maxLength", 2),  "12", true, null ),
                Arguments.of( constraintsJSON("string", "maxLength", 2),  "123", false, new ValidationMessage("validation.message.max.length", "2") ),
                Arguments.of( constraintsJSON("string", "pattern", "[a-z]*"),  "abc", true, null ),
                Arguments.of( constraintsJSON("string", "pattern", "[a-z]*"),  "abcA", false, new ValidationMessage("validation.message.regexp", "[a-z]*") ),
                Arguments.of( constraintsJSON("string", "enum", asList("a","b","c","d","e")),  "c", true, null ),
                Arguments.of( constraintsJSON("string", "enum", asList("a","b","c","d","e")),  "f", false, new ValidationMessage("validation.message.value.of", "f", "a, b, c, d, e") ),

                Arguments.of( formatJSON("string", null), "some string", true, null ),
                Arguments.of( formatJSON("string", "email"), "test@javafx.ninja", true, null ),
                Arguments.of( formatJSON("string", "email"), "wrong email", false, new ValidationMessage("validation.message.email") ),
                Arguments.of( formatJSON("string", "uri"), "http://www.javafx.ninja", true, null ),
                Arguments.of( formatJSON("string", "uri"), "!$%&/()", false, new ValidationMessage("validation.message.uri") ),
                Arguments.of( formatJSON("string", "binary"), "dGVzdA==", true, null ),
                Arguments.of( formatJSON("string", "binary"), "no binary", false, new ValidationMessage("validation.message.binary") ),
                Arguments.of( formatJSON("string", "uuid"), "6ba7b810-9dad-11d1-80b4-00c04fd430c8", true, null ),
                Arguments.of( formatJSON("string", "uuid"), "no uuid", false, new ValidationMessage("validation.message.uuid") ),
                Arguments.of( formatJSON("date", null),  "2015-11-27", true, null ),
                Arguments.of( formatJSON("date", null),  "27.11.2015", false, new ValidationMessage("validation.message.date.format", "yyyy-MM-dd") ),
                Arguments.of( formatJSON("date", "yyyyMMdd"),  "20151127", true, null ),
                Arguments.of( formatJSON("date", "yyyyMMdd"),  "27.11.2015", false, new ValidationMessage("validation.message.date.format", "yyyyMMdd") ),

                Arguments.of( customAttributeJSON( "groovy", "value.contains('a')? 'true' : 'no a inside'"),  "abcdef", true, null ),
                Arguments.of( customAttributeJSON( "groovy", "value.contains('a')? 'true' : 'no a inside'"),  "bcdefg", false, new ValidationMessage("no a inside") )
        );
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