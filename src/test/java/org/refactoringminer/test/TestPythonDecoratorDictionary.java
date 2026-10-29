package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;

public class TestPythonDecoratorDictionary {

	@Test
	public void testDictionaryInDecoratorArguments() {
		String source = """
				import pytest

				@pytest.mark.parametrize(("overrides", "expected"), [({}, True), ({"name": "CI"}, False)])
				def test_top_level(overrides, expected):
				    assert overrides is not None

				@pytest.mark.parametrize("settings", [{"github_app_id": "123"}])
				def test_single_argument(settings):
				    assert settings

				@pytest.mark.usefixtures({"settings": "default"})
				class TestSuite:
				    @pytest.mark.parametrize("payload", [({"action": "requested", "conclusion": None}, False)])
				    def test_method(self, payload):
				        assert payload
				""";

		UMLModel model = assertDoesNotThrow(() ->
				new UMLModelASTReader(Map.of("test_sample.py", source), Set.of(""), false).getUmlModel());
		List<UMLOperation> operations = model.getClassList().stream()
				.flatMap(umlClass -> umlClass.getOperations().stream())
				.filter(operation -> operation.getName().startsWith("test_"))
				.toList();
		assertEquals(3, operations.size());
		for(UMLOperation operation : operations) {
			//the dictionary of the decorator is an anonymous class of the decorated operation
			assertEquals(1, operation.getAnnotations().size(), operation.getName());
			assertEquals(1, operation.getAnonymousClassList().size(), operation.getName());
		}
		UMLClass testSuite = model.getClassList().stream()
				.filter(umlClass -> umlClass.getNonQualifiedName().equals("TestSuite"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The decorated class must be included in the model"));
		assertEquals(1, testSuite.getAnnotations().size());
	}
}
