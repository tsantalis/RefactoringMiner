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

        // Check if src_files is not null before serializing
        if (caseInfo.getSrc_files() != null && !caseInfo.getSrc_files().isEmpty()) {
            jsonGenerator.writeObjectField("src_files", caseInfo.getSrc_files());
        }

        jsonGenerator.writeEndObject();
    }
}