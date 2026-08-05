/*
   The MIT License (MIT)
   -----------------------------------------------------------------------------

   Copyright (c) 2015-2019 javafx.ninja <info@javafx.ninja>
                                                                                                                    
   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.
  
*/

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
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * unit test for header validator
 */
public class HeaderValidationTest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @ParameterizedTest
    @MethodSource("validationConfigurations")
    public void validation(String configHeaderNames,
                           String[] headerNames,
                           Boolean expectedResult,
                           List<ValidationMessage> expectedErrors) {
        // setup
        Gson gson = new GsonBuilder().create();
        ValidationConfiguration config = gson.fromJson(configHeaderNames, ValidationConfiguration.class);
        Validator sut = new Validator(config, new TestColumnValueProvider());

        // execution
        ValidationError result = sut.isHeaderValid(headerNames);

        // assertion
        assertThat(result == null, is(expectedResult));
        if (!expectedResult) {
            assertTrue(result.getMessages().containsAll(expectedErrors));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameters for tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static Stream<Arguments> validationConfigurations() {
        return Stream.of(
                Arguments.of( json(), new String[] {}, true, null ),
                Arguments.of( json("a"), new String[] {"a"}, true, null ),
                Arguments.of( json("a"), new String[] {"b"}, false, singletonList(new ValidationMessage("validation.message.header.match", "0", "a", "b"))),
                Arguments.of( json("a"), new String[] {"a","b"}, false, singletonList(new ValidationMessage("validation.message.header.length", "2", "1"))),
                Arguments.of( json("a", "b"), new String[] {"b", "a"}, false, asList(new ValidationMessage("validation.message.header.match", "0", "a", "b"), new ValidationMessage("validation.message.header.match", "1", "b", "a")) )
        );
    }

    @SuppressWarnings("StringConcatenationInLoop")
    public static String json(String... headerNames) {

        String json = "{ \"fields\": [";

        for (String headerName: headerNames) {
            json += "{\"name\" : \""+headerName+"\" },";
        }

        if (headerNames != null && headerNames.length > 0) {
            json = json.substring(0, json.length() - 1);
        }

        json += "]}";


        return  json;
    }

}
