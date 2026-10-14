package org.refactoringminer.astDiff.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.caoccao.javet.swc4j.Swc4j;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import gr.uom.java.xmi.CppFileProcessor;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.PythonFileProcessor;
import gr.uom.java.xmi.TypeScriptFileProcessor;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;

/**
 * The offsets of the AST diff trees and the RefactoringMiner models are UTF-16 offsets, i.e., the offsets of Java strings,
 * even after emoji (surrogate pairs), which swc4j and ANTLR count as one code point, and tree-sitter as four UTF-8 bytes,
 * and after accented characters, which tree-sitter counts as two UTF-8 bytes.
 */
public class Utf16OffsetsTest {
	private static final String EMOJI = "\uD83D\uDE00\u00E9\uD83C\uDF89";

	@Test
	public void testTypeScript() throws Exception {
		assertSameOffsetsAsAscii("src/emoji.ts", "const s = \"" + EMOJI + "\";\n// " + EMOJI + " comment\nfunction f(x: number) { return x + 1; }\n");
	}

	@Test
	public void testJavaScriptWithJsx() throws Exception {
		assertSameOffsetsAsAscii("src/emoji.js", "const s = <span title=\"" + EMOJI + "\">" + EMOJI + "</span>;\nfunction f(x) { return x + 1; }\n");
	}

	@Test
	public void testPython() throws Exception {
		assertSameOffsetsAsAscii("src/emoji.py", "s = \"" + EMOJI + "\"  # " + EMOJI + "\ndef f(x):\n    return x + 1\n");
	}

	@Test
	public void testCpp() throws Exception {
		assertSameOffsetsAsAscii("src/emoji.cpp", "const char* s = \"" + EMOJI + "\"; // " + EMOJI + "\nint f(int x) { return x + 1; }\n");
	}

	private static UMLModel process(String path, String content) throws Exception {
		UMLModel model = new UMLModel(new HashSet<>());
		if(path.endsWith(".py"))
			new PythonFileProcessor(model).processPythonFile(path, content, true);
		else if(path.endsWith(".cpp"))
			new CppFileProcessor(model).processCppFile(path, content, true);
		else
			new TypeScriptFileProcessor(model).processTypeScriptFile(path, content, true, new Swc4j());
		return model;
	}

	private static UMLOperation operation(UMLModel model) {
		for(UMLClass umlClass : model.getClassList()) {
			for(UMLOperation operation : umlClass.getOperations()) {
				if(operation.getName().equals("f")) {
					return operation;
				}
			}
		}
		return null;
	}

	private static List<String> nodes(TreeContext treeContext) {
		List<String> nodes = new ArrayList<>();
		for(Tree tree : treeContext.getRoot().preOrder()) {
			nodes.add(tree.getType() + "[" + tree.getPos() + "-" + tree.getEndPos() + "]");
		}
		return nodes;
	}

	//the emoji take as many offsets as ASCII characters with the same UTF-16 length, so the model and the tree have the same offsets as for the ASCII content,
	//and the labels of the leaves are their source code
	private static void assertSameOffsetsAsAscii(String path, String content) throws Exception {
		String ascii = content.replace(EMOJI, "abcde");
		assertEquals(content.length(), ascii.length());
		UMLModel model = process(path, content);
		UMLModel asciiModel = process(path, ascii);
		UMLOperation operation = operation(model);
		UMLOperation asciiOperation = operation(asciiModel);
		assertNotNull(operation, "operation f not found");
		assertNotNull(asciiOperation, "operation f not found in the ASCII content");
		LocationInfo location = operation.getLocationInfo();
		LocationInfo asciiLocation = asciiOperation.getLocationInfo();
		assertEquals(asciiLocation.getStartOffset(), location.getStartOffset());
		assertEquals(asciiLocation.getEndOffset(), location.getEndOffset());
		assertEquals(asciiLocation.getStartColumn(), location.getStartColumn());
		assertTrue(content.substring(location.getStartOffset(), location.getEndOffset()).startsWith(path.endsWith(".py") ? "def f" : path.endsWith(".cpp") ? "int f" : "function f"));
		TreeContext treeContext = model.getTreeContextMap().get(path);
		assertNotNull(treeContext, "tree not generated");
		assertEquals(nodes(asciiModel.getTreeContextMap().get(path)), nodes(treeContext));
		for(Tree tree : treeContext.getRoot().preOrder()) {
			if(tree.isLeaf() && !tree.getLabel().isEmpty()) {
				assertEquals(content.substring(tree.getPos(), tree.getEndPos()), tree.getLabel(), "label of " + tree.getType());
			}
		}
	}
}
