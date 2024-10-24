/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package integration.system.inputs;

import integration.BaseRestTest;
import integration.MongoDbSeed;
import integration.RequiresVersion;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@RequiresVersion(">=0.90.0")
public class InputsTest extends BaseRestTest {

    @Test
    @MongoDbSeed(location = "graylog", database = "graylog2")
    public void createInputTest() {

        given().when()
                .body(jsonResourceForMethod()).post("/system/inputs")
                .then()
                    .statusCode(400).statusLine(notNullValue());
    }

    @Test(dependsOnMethods = "createInputTest")
    public void listInput() {
        given().when().get("/system/inputs").then().statusCode(200);
    }
}

