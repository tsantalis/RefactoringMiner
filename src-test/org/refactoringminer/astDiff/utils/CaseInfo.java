package org.refactoringminer.astDiff.utils;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

import javax.json.JsonObject;
import java.io.Serializable;
import java.util.Objects;

public class CaseInfo implements Serializable {
    String repo;
    String commit;

    public CaseInfo(String repo, String commit) {
        this.repo = repo;
        this.commit = commit;
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
        return this.repo.replace(".git","") + "/commit/" + this.commit;
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
                return new CaseInfo(json.getString("repo"),json.getString("commit"));
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
}
