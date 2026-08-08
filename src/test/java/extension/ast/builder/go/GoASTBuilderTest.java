package extension.ast.builder.go;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangASTUtil;
import extension.base.LangSupportedEnum;
import gr.uom.java.xmi.Visibility;

class GoASTBuilderTest {

	private static LangCompilationUnit parse(String source) throws Exception {
		LangASTNode ast = LangASTUtil.getLangAST(LangSupportedEnum.GO, source);
		return assertInstanceOf(LangCompilationUnit.class, ast);
	}

	@Test
	void parsesImportsAndAFunctionWithMultipleReturnValues() throws Exception {
		String source = String.join("\n",
				"package main",
				"",
				"import (",
				"\t\"fmt\"",
				"\tf \"os\"",
				"\t_ \"net/http\"",
				")",
				"",
				"func Divide(a int, b int) (int, error) {",
				"\treturn a / b, nil",
				"}") + "\n";

		LangCompilationUnit cu = parse(source);
		assertEquals(LangSupportedEnum.GO, cu.getLanguage());

		List<LangImportStatement> imports = cu.getImports();
		assertEquals(3, imports.size());
		assertEquals("fmt", imports.get(0).getModuleName());
		assertNull(imports.get(0).getImports().get(0).getAlias());
		assertEquals("os", imports.get(1).getModuleName());
		assertEquals("f", imports.get(1).getImports().get(0).getAlias());
		assertEquals("net/http", imports.get(2).getModuleName());
		assertEquals("_", imports.get(2).getImports().get(0).getAlias());

		assertEquals(1, cu.getMethods().size());
		LangMethodDeclaration divide = cu.getMethods().get(0);
		assertEquals("Divide", divide.getName());
		assertEquals(Visibility.PUBLIC, divide.getVisibility());
		assertEquals(List.of("a", "b"), paramNames(divide));
		assertEquals(List.of("int", "int"), paramTypes(divide));
		assertEquals("int", divide.getReturnTypeAnnotation());
		assertEquals(List.of("int", "error"), divide.getReturnTypeAnnotations());
	}

	@Test
	void parsesStructFieldsAndAPointerReceiverMethod() throws Exception {
		String source = String.join("\n",
				"package models",
				"",
				"type User struct {",
				"\tName string",
				"\tAge  int",
				"}",
				"",
				"func (u *User) Greet() string {",
				"\treturn \"hi\"",
				"}") + "\n";

		LangCompilationUnit cu = parse(source);

		assertEquals(1, cu.getTypes().size());
		LangTypeDeclaration user = cu.getTypes().get(0);
		assertEquals("User", user.getName());
		assertTrue(!user.isInterface());
		assertEquals(Visibility.PUBLIC, user.getVisibility());
		assertEquals(List.of("Name", "Age"), fieldNames(user));
		assertEquals(List.of("string", "int"), fieldTypes(user));

		assertEquals(1, cu.getMethods().size());
		LangMethodDeclaration greet = cu.getMethods().get(0);
		assertEquals("Greet", greet.getName());
		assertEquals("*User", greet.getReceiverType());
		assertEquals("string", greet.getReturnTypeAnnotation());
		assertEquals(Visibility.PUBLIC, greet.getVisibility());
	}

	@Test
	void parsesInterfaceMethodSignatures() throws Exception {
		String source = String.join("\n",
				"package contracts",
				"",
				"type Reader interface {",
				"\tRead(p []byte) (int, error)",
				"\tClose()",
				"}") + "\n";

		LangCompilationUnit cu = parse(source);

		assertEquals(1, cu.getTypes().size());
		LangTypeDeclaration reader = cu.getTypes().get(0);
		assertEquals("Reader", reader.getName());
		assertTrue(reader.isInterface());
		assertEquals(2, reader.getMethods().size());

		LangMethodDeclaration read = reader.getMethods().get(0);
		assertEquals("Read", read.getName());
		assertEquals(List.of("p"), paramNames(read));
		assertEquals(List.of("[]byte"), paramTypes(read));
		assertEquals(List.of("int", "error"), read.getReturnTypeAnnotations());

		LangMethodDeclaration close = reader.getMethods().get(1);
		assertEquals("Close", close.getName());
		assertEquals(0, close.getParameters().size());
		assertEquals("VOID", close.getReturnTypeAnnotation());
	}

	private static List<String> paramNames(LangMethodDeclaration method) {
		return method.getParameters().stream()
				.map(p -> p.getLangSimpleName().getIdentifier())
				.collect(Collectors.toList());
	}

	private static List<String> paramTypes(LangMethodDeclaration method) {
		return method.getParameters().stream()
				.map(LangSingleVariableDeclaration::getTypeAnnotationText)
				.collect(Collectors.toList());
	}

	private static List<String> fieldNames(LangTypeDeclaration type) {
		return type.getFields().stream()
				.map(f -> f.getLangSimpleName().getIdentifier())
				.collect(Collectors.toList());
	}

	private static List<String> fieldTypes(LangTypeDeclaration type) {
		return type.getFields().stream()
				.map(LangSingleVariableDeclaration::getTypeAnnotationText)
				.collect(Collectors.toList());
	}

}
