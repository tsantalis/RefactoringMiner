package org.refactoringminer.astDiff.utils;

/* Created by pourya on 2024-01-21*/
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class CaseInfoSerializer extends JsonSerializer<CaseInfo> {

    @Override
    public void serialize(CaseInfo caseInfo, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        // Serialize other properties
        jsonGenerator.writeStringField("repo", caseInfo.getRepo());
        jsonGenerator.writeStringField("commit", caseInfo.getCommit());
        // Add other properties as needed

        if (caseInfo.getSrc_files() != null && !caseInfo.getSrc_files().isEmpty()) {
            jsonGenerator.writeFieldName("src_files");
            jsonGenerator.writeStartArray();
            jsonGenerator.writeRaw("\n");
            for (String file : caseInfo.getSrc_files()) {
                jsonGenerator.writeRaw("   ");
                jsonGenerator.writeString(file);
                jsonGenerator.writeRaw("\n");
            }
            jsonGenerator.writeRaw(" ");
            jsonGenerator.writeEndArray();
        }


        jsonGenerator.writeEndObject();
    }
}