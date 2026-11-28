package gui.webdiff.viewers.monaco;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.filters.DiffFilterKind;

class MonacoCoreTest {

	private static final String CUSTOMER_BEFORE = String.join("\n",
			"public class CustomerProfile {",
			"    private String name;",
			"    private Address address;",
			"",
			"    public String greet() {",
			"        return \"Hello, \" + name;",
			"    }",
			"",
			"    public String formatAddress(String street, String city) {",
			"        String line = street + \", \" + city;",
			"        return line.trim();",
			"    }",
			"}",
			"");
	private static final String CUSTOMER_AFTER = String.join("\n",
			"public class CustomerProfile {",
			"    private String name;",
			"    private Address address;",
			"",
			"    public String greet() {",
			"        return \"Hello, \" + name;",
			"    }",
			"}",
			"");
	private static final String ADDRESS_BEFORE = String.join("\n",
			"public class Address {",
			"    private String street;",
			"    private String city;",
			"",
			"    public String getCity() {",
			"        return city;",
			"    }",
			"}",
			"");
	private static final String ADDRESS_AFTER = String.join("\n",
			"public class Address {",
			"    private String street;",
			"    private String city;",
			"",
			"    public String getCity() {",
			"        return city;",
			"    }",
			"",
			"    public String formatAddress(String street, String city) {",
			"        String line = street + \", \" + city;",
			"        return line.trim();",
			"    }",
			"}",
			"");

	private static final Set<String> MOVES = Set.of("moved", "moveIn", "moveOut", "mm", "mm updOnTop");
	private static final Pattern RANGE = Pattern.compile("\\{from: (\\d+),to: (\\d+),index: \\d+,kind: \"([^\"]+)\"");

	private record Range(int from, int to, String kind) {
		boolean within(Range other) {
			return other.from <= from && to <= other.to;
		}
	}

	private static MonacoCore moveDiff;

	@BeforeAll
	static void diffTheMove() throws Exception {
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtFileContents(
				Map.of("CustomerProfile.java", CUSTOMER_BEFORE, "Address.java", ADDRESS_BEFORE),
				Map.of("CustomerProfile.java", CUSTOMER_AFTER, "Address.java", ADDRESS_AFTER));
		DirComparator comparator = new DirComparator(projectASTDiff, DiffFilterKind.NO_FILTER);
		for (int id = 0; id < comparator.getNumOfDiffs(); id++) {
			if (comparator.isMoveDiff(id)) {
				moveDiff = new MonacoCore(comparator, comparator.getASTDiff(id), id, true, CUSTOMER_BEFORE, ADDRESS_AFTER,
						comparator.getRefactorings(), Collections.emptyMap());
			}
		}
		assertTrue(moveDiff != null, "no move diff for CustomerProfile.java -> Address.java");
	}

	private static List<Range> ranges(String jsConfig) {
		List<Range> ranges = new ArrayList<>();
		Matcher m = RANGE.matcher(jsConfig);
		while (m.find()) {
			ranges.add(new Range(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), m.group(3)));
		}
		return ranges;
	}

	private static void assertOnlyTheMoveIsMarked(List<Range> ranges, String kind) {
		List<Range> moves = ranges.stream().filter(r -> MOVES.contains(r.kind())).toList();
		assertFalse(moves.isEmpty(), "the moved method is not marked");
		for (Range r : ranges) {
			if (r.kind().equals(kind)) {
				assertTrue(moves.stream().anyMatch(r::within), kind + " outside the moved code: " + r);
			}
		}
	}

	@Test
	void moveDiffMarksNothingDeletedOutsideTheMovedCode() {
		assertOnlyTheMoveIsMarked(ranges(moveDiff.getLeftJsConfig()), "deleted");
	}

	@Test
	void moveDiffMarksNothingInsertedOutsideTheMovedCode() {
		assertOnlyTheMoveIsMarked(ranges(moveDiff.getRightJsConfig()), "inserted");
	}
}
