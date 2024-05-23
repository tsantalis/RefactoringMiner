package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        new LeafMatcher().match(srcTree,dstTree,mappings);
        try {
            String actual = MappingExportModel.exportString(mappings).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
            assertEquals(expectedMappings,actual);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<Arguments> initData() throws Exception {
        List<Arguments> allCases = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(getTreesPath()))) {
            stream.filter(path -> Files.isDirectory(path) && !path.toString().replaceAll("\\\\", "/").equals(getTreesPath()))
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
    static class JdtPartialTreeGenerator extends JdtTreeGenerator{

        private final int kind;
        JdtPartialTreeGenerator(int kind)
        {
            this.kind = kind;
        }
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public TreeContext generate(Reader r) throws IOException {
            ASTParser parser = ASTParser.newParser(AST.JLS14);
            parser.setKind(kind);
            Map pOptions = JavaCore.getOptions();
            pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_14);
            pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_14);
            pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_14);
            pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
            parser.setCompilerOptions(pOptions);
            char[] source = readerToCharArray(r);
            parser.setSource(source);
            IScanner scanner = ToolFactory.createScanner(false, false, false, false);
            scanner.setSource(source);
            AbstractJdtVisitor v = createVisitor(scanner);
            ASTNode node = parser.createAST(null);
            if ((node.getFlags() & ASTNode.MALFORMED) != 0) // bitwise flag to check if the node has a syntax error
                throw new SyntaxException(this, r, null);
            node.accept(v);
            return v.getTreeContext();
        }
        private static char[] readerToCharArray(Reader r) throws IOException {
            StringBuilder fileData = new StringBuilder();
            try (BufferedReader br = new BufferedReader(r)) {
                char[] buf = new char[10];
                int numRead = 0;
                while ((numRead = br.read(buf)) != -1) {
                    String readData = String.valueOf(buf, 0, numRead);
                    fileData.append(readData);
                    buf = new char[1024];
                }
            }
            return  fileData.toString().toCharArray();
        }
    }
}
