package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

// Exercises the full pipeline (GoLexer/GoParser -> GoASTBuilder -> LangASTNode -> UMLModelAdapter)
// end to end. UMLModelAdapter is shared with Python/C#; struct fields and receiver methods are
// routed through the same class-attribute/operation-ownership machinery those languages use.
class GoFileProcessorTest {

	@Test
	void parsesWithoutErrorsAndCapturesTheReturnType() {
		String filePath = "models/user.go";
		String fileContent = String.join("\n",
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

		UMLModel model = new UMLModel(Set.of(""));
		GoFileProcessor processor = new GoFileProcessor(model);
		processor.processGoFile(filePath, fileContent, false);

		List<UMLClass> classes = model.getClassList();
		assertEquals(2, classes.size());

		UMLClass userClass = classes.stream().filter(c -> c.getName().endsWith(".User")).findFirst().orElseThrow();
		assertEquals(2, userClass.getAttributes().size());
		assertEquals(List.of("Name", "Age"), userClass.getAttributes().stream()
				.map(UMLAttribute::getName).collect(Collectors.toList()));
		// Greet's receiver is *User, so it belongs on User, not the synthetic module class.
		assertEquals(1, userClass.getOperations().size());

		UMLClass moduleClass = classes.stream().filter(c -> !c.getName().endsWith(".User")).findFirst().orElseThrow();
		assertEquals(0, moduleClass.getOperations().size());

		UMLOperation greet = userClass.getOperations().get(0);
		assertEquals("Greet", greet.getName());
		assertTrue(greet.getReceiver().isPresent());
		assertEquals("User", greet.getReceiver().get().toString());
		assertEquals("string", greet.getReturnParameter().getType().toString());
	}
}
