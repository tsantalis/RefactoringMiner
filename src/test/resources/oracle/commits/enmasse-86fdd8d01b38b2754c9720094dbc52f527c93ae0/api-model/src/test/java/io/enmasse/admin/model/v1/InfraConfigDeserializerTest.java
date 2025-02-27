/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.InfraConfigDeserializer;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InfraConfigDeserializerTest {

    private final InfraConfigDeserializer deserializer = json -> new ObjectMapper().readValue(json, StandardInfraConfig.class);


    @Test
    public void deserializeNetworkPolicy() throws Exception {


        String str = "{\n" +
                "  \"spec\": {\n" +
                "    \"networkPolicy\": {\n" +
                "      \"ingress\": [\n" +
                "        {\n" +
                "          \"from\": [\n" +
                "            {\n" +
                "              \"podSelector\": {\n" +
                "                \"my\": \"label\"\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";


        InfraConfig infraConfig = deserializer.fromJson(str);

        assertTrue(infraConfig instanceof StandardInfraConfig);

    }

}
