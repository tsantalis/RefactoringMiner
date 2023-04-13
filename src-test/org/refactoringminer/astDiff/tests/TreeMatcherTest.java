package org.refactoringminer.astDiff.tests;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.gumtreediff.tree.Tree;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.LeafMatcher;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.refactoringminer.astDiff.utils.UtilMethods.getTreesPath;


/* Created by pourya on 2023-02-15 10:52 p.m. */
public class TreeMatcherTest {
    private static final String srcFilePath = "src.xml";
    private static final String dstFilePath = "dst.xml";
    private static final String mappingsFilePath = "mappings.json";

    @ParameterizedTest(name= "{index}: Folder: {3}")
    @MethodSource("initData")
    public void testMappings(Tree srcTree, Tree dstTree, String expectedMappings, String folderPath)
    {
        ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(srcTree,dstTree);
        new LeafMatcher(true).match(srcTree,dstTree,null,mappings);
        try {
            String actual = MappingExportModel.exportString(mappings);
            assertEquals(expectedMappings,actual);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<Arguments> initData() throws Exception {
        List<Arguments> allCases = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(getTreesPath()))) {
            stream.filter(path -> Files.isDirectory(path) && !path.toString().equals(getTreesPath()))
                    .forEach(path ->
                    {
                        Path srcPath = path.resolve(srcFilePath);
                        Path dstPath = path.resolve(dstFilePath);
                        Path mappingsPath = path.resolve(mappingsFilePath);
                        try {
                            allCases.add(Arguments.of(
                                    TreeUtilFunctions.loadTree(srcPath.toString()),
                                    TreeUtilFunctions.loadTree(dstPath.toString()),
                                    FileUtils.readFileToString(new File(mappingsPath.toUri()), "utf-8"),
                                    path.toString()
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return allCases.stream();
    }
}
