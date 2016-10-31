package org.refactoringminer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.EntityKey;
import org.refactoringminer.utils.RefactoringRelationship;

public class RefactoringDescriptionParser {

    private static ParserDefinition[] parsers = {
        new ParserDefinition(RefactoringType.RENAME_CLASS, "Rename Class (.+) renamed to (.+)", type(1), type(2)),
        new ParserDefinition(RefactoringType.MOVE_CLASS, "Move Class (.+) moved to (.+)", type(1), type(2)),
        new ParserDefinition(RefactoringType.EXTRACT_OPERATION, "Extract Method (.+) extracted from (.+) in class (.+)", method(2, 3), method(1, 3)),
        new ParserDefinition(RefactoringType.RENAME_METHOD, "Rename Method (.+) renamed to (.+) in class (.+)", method(1, 3), method(2, 3)),
        new ParserDefinition(RefactoringType.INLINE_OPERATION, "Inline Method (.+) inlined to (.+) in class (.+)", method(1, 3), method(2, 3)),
        new ParserDefinition(RefactoringType.MOVE_OPERATION, "Move Method (.+) from class (.+) to (.+) from class (.+)", method(1, 2), method(3, 4)),
        new ParserDefinition(RefactoringType.PULL_UP_OPERATION, "Pull Up Method (.+) from class (.+) to (.+) from class (.+)", method(1, 2), method(3, 4)),
        new ParserDefinition(RefactoringType.PUSH_DOWN_OPERATION, "Push Down Method (.+) from class (.+) to (.+) from class (.+)", method(1, 2), method(3, 4)),
        new ParserDefinition(RefactoringType.MOVE_ATTRIBUTE, "Move Attribute (.+) from class (.+) to class (.+)", attribute(1, 2), attribute(1, 3)),
        new ParserDefinition(RefactoringType.PULL_UP_ATTRIBUTE, "Pull Up Attribute (.+) from class (.+) to class (.+)", attribute(1, 2), attribute(1, 3)),
        new ParserDefinition(RefactoringType.PUSH_DOWN_ATTRIBUTE, "Push Down Attribute (.+) from class (.+) to class (.+)", attribute(1, 2), attribute(1, 3)),
        new ParserDefinition(RefactoringType.EXTRACT_INTERFACE, "Extract Interface (.+) from classes \\[(.+)\\]", types(2), type(1)),
        new ParserDefinition(RefactoringType.EXTRACT_SUPERCLASS, "Extract Superclass (.+) from classes \\[(.+)\\]", types(2), type(1))
    };

    private static class ParserDefinition {
        final RefactoringType type;
        final Pattern regex;
        final EntityParser entityBeforeParser;
        final EntityParser entityAfterParser;
        ParserDefinition(RefactoringType type, String regex, EntityParser entityBeforeParser, EntityParser entityAfterParser) {
            super();
            this.type = type;
            this.regex = Pattern.compile(regex);
            this.entityBeforeParser = entityBeforeParser;
            this.entityAfterParser = entityAfterParser;
        }
    }

    public List<RefactoringRelationship> parse(String refactoringDescription) {
        List<RefactoringRelationship> list = new ArrayList<>();
        for (ParserDefinition parser : parsers) {
            Matcher matcher = parser.regex.matcher(refactoringDescription);
            if (matcher.matches()) {
                List<EntityKey> entitiesBefore = parser.entityBeforeParser.parse(matcher);
                List<EntityKey> entitiesAfter = parser.entityAfterParser.parse(matcher);
                for (EntityKey entityBefore : entitiesBefore) {
                    for (EntityKey entityAfter : entitiesAfter) {
                        list.add(new RefactoringRelationship(parser.type, entityBefore.toString(), entityAfter.toString()));
                    }   
                }
            }
        }
        return list;
    }

    private interface EntityParser {
        List<EntityKey> parse(Matcher m);
    }

    private static EntityParser method(final int m, final int c) {
        return new EntityParser() {
            @Override
            public List<EntityKey> parse(Matcher matcher) {
                String key = normalizeType(matcher.group(c)) + "#" + normalizeMethod(matcher.group(m));
                return Collections.singletonList(new EntityKey(key));
            }
        };
    }

    private static EntityParser attribute(int a, int c) {
        return new EntityParser() {
            @Override
            public List<EntityKey> parse(Matcher matcher) {
                String key = normalizeType(matcher.group(c)) + "#" + normalizeAttribute(matcher.group(a));
                return Collections.singletonList(new EntityKey(key));
            }
        };
    }

    private static EntityParser type(int c) {
        return new EntityParser() {
            @Override
            public List<EntityKey> parse(Matcher matcher) {
                String key = normalizeType(matcher.group(c));
                return Collections.singletonList(new EntityKey(key));
            }
        };
    }
    
    private static EntityParser types(int c) {
        return new EntityParser() {
            @Override
            public List<EntityKey> parse(Matcher matcher) {
                String typesString = matcher.group(c);
                String[] array = typesString.split(",\\s*");
                List<EntityKey> list = new ArrayList<>(array.length);
                for (String key : array) {
                    list.add(new EntityKey(normalizeType(key)));
                }
                return list;
            }
        };
    }

    private static String normalizeType(String type) {
        // TODO Auto-generated method stub
        return type;
    }

    private static String normalizeMethod(String method) {
        // TODO Auto-generated method stub
        return method;
    }
    
    private static String normalizeAttribute(String attribute) {
        // TODO Auto-generated method stub
        return attribute;
    }

    /*
    private static String stripTypeParameters(String entityName) {
        StringBuilder sb = new StringBuilder();
        int openGenerics = 0;
        for (int i = 0; i < entityName.length(); i++) {
            char c = entityName.charAt(i);
            if (c == '<') {
                openGenerics++;
            }
            if (openGenerics == 0) {
                sb.append(c);
            }
            if (c == '>') {
                openGenerics--;
            }
        }
        return sb.toString();
    }*/
}
