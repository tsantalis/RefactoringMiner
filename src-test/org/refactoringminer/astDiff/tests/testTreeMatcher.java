package org.refactoringminer.astDiff.tests;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.gumtreediff.tree.Tree;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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


@RunWith(Parameterized.class)
/* Created by pourya on 2023-02-15 10:52 p.m. */
public class testTreeMatcher {
    private static final String srcFilePath = "src.xml";
    private static final String dstFilePath = "dst.xml";
    private static final String mappingsFilePath = "mappings.json";

    @Parameterized.Parameter(0)
    public Tree srcTree;
    @Parameterized.Parameter(1)
    public Tree dstTree;
    @Parameterized.Parameter(2)
    public String expectedMappings;
    @Parameterized.Parameter(3)
    public String folderPath;

    @Test
    public void testMappings()
    {
        ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(srcTree,dstTree);
        new LeafMatcher(true).match(srcTree,dstTree,null,mappings);
        try {
            String actual = MappingExportModel.exportString(mappings);
            Assert.assertEquals(expectedMappings,actual);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Parameterized.Parameters(name= "{index}: Folder: {3}")
    public static Iterable<Object[]> initData() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(getTreesPath()))) {
            stream.filter(path -> Files.isDirectory(path) && !path.toString().equals(getTreesPath()))
                    .forEach(path ->
                    {
                        Path srcPath = path.resolve(srcFilePath);
                        Path dstPath = path.resolve(dstFilePath);
                        Path mappingsPath = path.resolve(mappingsFilePath);
                        try {
                            allCases.add(new Object[]{
                                    TreeUtilFunctions.loadTree(srcPath.toString()),
                                    TreeUtilFunctions.loadTree(dstPath.toString()),
                                    FileUtils.readFileToString(new File(mappingsPath.toUri()), "utf-8"),
                                    path.toString()
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return allCases;
    }
}
