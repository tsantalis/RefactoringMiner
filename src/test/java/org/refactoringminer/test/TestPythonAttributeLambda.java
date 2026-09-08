package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;

public class TestPythonAttributeLambda {

	@Test
	public void testDictionaryAttributeContainingLambda() {
		String source = """
				mapping = {"number": lambda x: x + 1}

				def anchor():
				    return mapping
				""";

		UMLModel model = assertDoesNotThrow(() ->
				new UMLModelASTReader(Map.of("sample.py", source), Set.of(""), false).getUmlModel());
		UMLAttribute mapping = model.getClassList().stream()
				.flatMap(umlClass -> umlClass.getAttributes().stream())
				.filter(attribute -> attribute.getName().equals("mapping"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The mapping attribute must be included in the model"));

		List<LambdaExpressionObject> lambdas = mapping.getAllLambdas();
		assertEquals(1, lambdas.size());
		LambdaExpressionObject lambda = lambdas.get(0);
		assertEquals(List.of("x"), lambda.getParameterNameList());
		assertNotNull(lambda.getExpression());
	}
}
