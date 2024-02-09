package org.refactoringminer.astDiff.utils;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = CaseInfoSerializer.class)
public class CaseInfo implements Serializable {
    String repo;
    String commit;

    //Only consider these files for the diff, if empty, consider all files
    Set<String> src_files;

    public CaseInfo(String repo, String commit) {
        this.repo = repo;
        this.commit = commit;
    }

    public CaseInfo(String repo, String commit, Set<String> src_files) {
        this.repo = repo;
        this.commit = commit;
        this.src_files = src_files;
    }

    public void setSrc_files(Set<String> src_files) {
        this.src_files = src_files;
    }

    public Set<String> getSrc_files() {
        return src_files;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repo, commit);
    }

    public CaseInfo() {
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaseInfo)) return false;
        CaseInfo second = (CaseInfo) obj;
        return second.getRepo().equals(this.getRepo()) &&
                second.getCommit().equals(this.getCommit());
    }

    public String makeURL() {
        String infix = (this.repo.contains(".git")) ? "/commit/" : "";

        return this.repo.replace(".git","") + infix + this.commit;
    }
    public static class CaseInfoConverter implements ArgumentConverter {
        @Override
        public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
            if (!(source instanceof JsonObject)) {
                throw new ArgumentConversionException("Not a JsonObject");
            }
            JsonObject json = (JsonObject) source;
            String name = context.getParameter().getName();
            Class<?> type = context.getParameter().getType();
            if (type == CaseInfo.class) {
                Set<String> src_files = new LinkedHashSet<>();
                JsonArray jsonArray = json.getJsonArray("src_files");
                if (jsonArray == null) src_files = null;
                else
                    for (JsonValue jsonValue : jsonArray)
                        if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
                            src_files.add(((JsonString) jsonValue).getString());
                        }
                return new CaseInfo(json.getString("repo"),
                        json.getString("commit"),
                        src_files);
            } else if (type == String.class) {
                return json.getString(name);
            } else if (type == int.class) {
                return json.getInt(name);
            } else if (type == boolean.class) {
                return json.getBoolean(name);
            }
            throw new ArgumentConversionException("Can't convert to type: '" + type.getName() + "'");
        }
    }

    @Override
    public String toString() {
        return "CaseInfo{" +
                "repo='" + repo + '\'' +
                ", commit='" + commit + '\'' +
                '}';
    }
}
