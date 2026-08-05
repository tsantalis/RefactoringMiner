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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * unit test for header validator
 */
@RunWith(Parameterized.class)
public class HeaderValidationTest {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ValidationConfiguration config;
    private Boolean expectedResult;
    private List<ValidationMessage> expectedErrors;
    private String[] headerNames;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // subject under test
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private Validator sut;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parameterized constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public HeaderValidationTest(String configHeaderNames,
                                String[] headerNames,
                                Boolean expectedResult,
                                List<ValidationMessage> expectedErrors) {
        Gson gson = new GsonBuilder().create();
        this.config = gson.fromJson(configHeaderNames, ValidationConfiguration.class);
        this.headerNames = headerNames;
        this.expectedResult = expectedResult;
        this.expectedErrors = expectedErrors;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // init
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Before
    public void initialize() {
        sut = new Validator(config, new TestColumnValueProvider());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void validation() {
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
    @Parameterized.Parameters
    public static Collection validationConfigurations() {
        return asList(new Object[][] {
                { json(), new String[] {}, true, null },
                { json("a"), new String[] {"a"}, true, null },
                { json("a"), new String[] {"b"}, false, singletonList(new ValidationMessage("validation.message.header.match", "0", "a", "b"))},
                { json("a"), new String[] {"a","b"}, false, singletonList(new ValidationMessage("validation.message.header.length", "2", "1"))},
                { json("a", "b"), new String[] {"b", "a"}, false, asList(new ValidationMessage("validation.message.header.match", "0", "a", "b"), new ValidationMessage("validation.message.header.match", "1", "b", "a")) }
        });
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
