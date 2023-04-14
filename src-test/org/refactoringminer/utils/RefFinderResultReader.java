package org.refactoringminer.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.refactoringminer.api.RefactoringType;

public class RefFinderResultReader {

    private static Map<String, Function<List<String>, RefactoringRelationship>> mappers = initMappings();
    
    public static RefactoringSet read(String project, String revision, String folderPath) {
        try {
            RefactoringSet result = new RefactoringSet(project, revision);
            for (RefactoringRelationship r : readFolder(folderPath)) {
                result.add(r);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<RefactoringRelationship> readFolder(String path) throws Exception {
        List<RefactoringRelationship> result = new ArrayList<>();
        File folder = new File(path);
        for (File f : folder.listFiles()) {
            if (f.isFile()) {
                readXml(f.getPath(), result);
            }
        }
        return result;
    }

    public static void readXml(String path, List<RefactoringRelationship> result) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    RefactoringRelationship r = parse(line);
                    if (r != null) {
                        result.add(r);
                    }
                }
            }
        }
    }

    private static RefactoringRelationship parse(String line) {
        int openPar = line.indexOf('(');
        String type = line.substring(0, openPar);
        String args = line.substring(openPar + 2, line.length() - 2);
        Function<List<String>, RefactoringRelationship> mapper = mappers.get(type);
        //rename_method("org.springframework.boot.bind%.RelaxedDataBinder#setIgnoreNestedProperties()","org.springframework.boot.bind%.RelaxedDataBinder#setIgnoreNestedPropertiesRenamed()","org.springframework.boot.bind%.RelaxedDataBinder")
        if (mapper != null) {
            List<String> argList = Arrays.asList(args.split("\",\""));
            return mapper.apply(argList);
        }
        return null;
    }

    private static Map<String, Function<List<String>, RefactoringRelationship>> initMappings() {
        Map<String, Function<List<String>, RefactoringRelationship>> mappers = new HashMap<>();
        mappers.put("extract_superclass", args -> parse(args, RefactoringType.EXTRACT_SUPERCLASS, type(1), type(2)));
        mappers.put("extract_interface", args -> parse(args, RefactoringType.EXTRACT_INTERFACE, type(2), type(1)));
        mappers.put("rename_method", args -> parse(args, RefactoringType.RENAME_METHOD, member(1), member(2)));
        mappers.put("move_method", args -> parse(args, RefactoringType.MOVE_OPERATION, member(2, 1), member(3, 1)));
        mappers.put("push_down_method", args -> parse(args, RefactoringType.PUSH_DOWN_OPERATION, member(2, 1), member(3, 1)));
        mappers.put("pull_up_method", args -> parse(args, RefactoringType.PULL_UP_OPERATION, member(2, 1), member(3, 1)));
        mappers.put("extract_method", args -> parse(args, RefactoringType.EXTRACT_OPERATION, member(1), member(2)));
        mappers.put("inline_method", args -> parse(args, RefactoringType.INLINE_OPERATION, member(2), member(1)));
        mappers.put("move_field", args -> parse(args, RefactoringType.MOVE_ATTRIBUTE, member(2, 1), member(3, 1)));
        mappers.put("push_down_field", args -> parse(args, RefactoringType.PUSH_DOWN_ATTRIBUTE, member(2, 1), member(3, 1)));
        mappers.put("pull_up_field", args -> parse(args, RefactoringType.PULL_UP_ATTRIBUTE, member(2, 1), member(3, 1)));
        
        return mappers;
    }
    
    private static RefactoringRelationship parse(List<String> args, RefactoringType type, EntityParser parserBefore, EntityParser parserAfter) {
        return new RefactoringRelationship(type, parserBefore.parse(args), parserAfter.parse(args));
    }
    
    private static EntityParser type(final int i) {
        return new EntityParser() {
            @Override
            String parse(List<String> args) {
                return normalize(args.get(i - 1));
            }
        };
    }
    
    private static EntityParser member(final int i, final int j) {
        return new EntityParser() {
            @Override
            String parse(List<String> args) {
                return normalize(args.get(i - 1)) + "#" + normalize(args.get(j - 1));
            }
        };
    }
    
    private static EntityParser member(final int i) {
        return new EntityParser() {
            @Override
            String parse(List<String> args) {
                return normalize(args.get(i - 1));
            }
        };
    }

    private static String normalize(String entity) {
        return entity.replace("%.", ".").replace('#', '.');
    }
}

abstract class EntityParser {
    abstract String parse(List<String> args);
}










