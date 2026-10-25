package org.refactoringminer.astDiff.tests;

import com.google.gson.Gson;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import org.refactoringminer.astDiff.graph.DiffNode;
import org.refactoringminer.astDiff.graph.Driver;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.ReviewNode;
import org.refactoringminer.astDiff.graph.cluster.Cluster;
import org.refactoringminer.astDiff.graph.cluster.Clusterer;
import org.refactoringminer.astDiff.graph.cluster.traverse.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards the representation the narrator gives to a reviewing agent (the chapters of {@link GrainLevel#SINGLE}) on
 * reviewed samples of the dataset. Each case is a pull request revision whose output was read against the raw diff
 * and the code of both revisions; its expectations pin what the output got right, in terms that survive unrelated
 * improvements (an exact hunk, a dependency, a link, an order), not the whole output.
 * <p>
 * The output is not ideal yet for every case. An expectation with a {@code todo} states the ideal output and why it
 * is not reached: it is reported as skipped while it fails, and fails once it holds, so that its {@code todo} and the
 * notes of its case are updated. The notes of a case describe what is still missing or noisy in its output.
 * <p>
 * Every case also checks the invariants of the representation (see {@link #testInvariants}). The output of each case
 * is written to {@value #ACTUAL_DIR}, to be compared with the expectations. A case is added by its dataset entry, its
 * repository, the base commit of its pull request and the commit its reviews were submitted on; its expectations are written from its output once that output was read against
 * the raw diff of the range and the code of both revisions. The files a case reads are cached in {@value #CACHE_PATH}:
 * a case already cached runs with no GitHub access, and a new one reads GitHub once, with the token of
 * github-oauth.properties.
 */
public class RepresentationTest {

    private static final String ORACLE_DATA_PATH = "src/test/resources/narrator/representations.json";
    private static final String ACTUAL_DIR = "build/narrator/representations";
    // The files read from GitHub, kept so that later runs need no GitHub access
    private static final String CACHE_PATH = "src/test/resources/oracle/commits";
    private static final Pattern ELEMENT = Pattern.compile("<(diff|dependency)((?: [a-z_]+=\"[^\"]*\")*)>\\n(.*?)\\n</\\1>", Pattern.DOTALL);
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-z_]+)=\"([^\"]*)\"");
    private static final Map<String, Representation> REPRESENTATIONS = new HashMap<>();

    static {
        // The cases share one JVM, and the HTTP/2 connection of the default connector gets its streams reset while
        // downloading the recursive trees of large repositories one after the other
        System.setProperty("test.github.connector", "urlconnection");
        System.setProperty(Driver.CACHE_PROPERTY, CACHE_PATH);
    }

    static Stream<TestCase> testCases() throws IOException {
        String content = Files.readString(Path.of(ORACLE_DATA_PATH));
        List<TestCase> cases = new Gson().fromJson(content, new com.google.gson.reflect.TypeToken<List<TestCase>>(){}.getType());
        return cases.stream();
    }

    static Stream<Arguments> expectations() throws IOException {
        return testCases().flatMap(testCase -> testCase.expectations.stream().map(expectation -> Arguments.of(testCase, expectation)));
    }

    /**
     * The output reads in the order it claims and names each change once: an element serves only diffs printed after
     * it in its chapter, diff ids are unique and no longer than needed, a dependency is printed once per chapter, and
     * every change of the graph is presented by a diff.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testInvariants(TestCase testCase) throws Exception {
        Representation representation = load(testCase);

        Set<String> ids = new HashSet<>();
        for (Element element : representation.elements) {
            if (element.kind.equals("diff")) {
                String id = element.attributes.get("id");
                Assertions.assertTrue(ids.add(id), "Diff id printed twice: " + id);
                // A longer id only follows from two diffs presenting the same changes
                Assertions.assertEquals(Node.PROMPT_ID_PREFIX.length() + Node.PROMPT_ID_LENGTH, id.length(), "Diff id longer than needed: " + id);
            }
        }

        Set<String> dependencies = new HashSet<>();
        for (Element element : representation.elements) {
            for (String served : element.list("serves")) {
                Element servedElement = representation.diff(served);
                Assertions.assertNotNull(servedElement, "Serves a diff not printed: " + served + " in\n" + element);
                Assertions.assertEquals(element.chapter, servedElement.chapter, "Serves a diff of another chapter: " + served + " in\n" + element);
                Assertions.assertTrue(element.index < servedElement.index, "Serves a diff printed before it: " + served + " in\n" + element);
            }
            for (String moved : element.list("moved_to", "moved_from")) {
                Assertions.assertNotNull(representation.diff(moved), "Moves to or from a diff not printed: " + moved + " in\n" + element);
            }
            if (element.kind.equals("dependency")) {
                Assertions.assertTrue(dependencies.add(element.chapter + "\n" + element.attributes.get("location") + "\n" + element.body),
                        "Dependency printed twice in a chapter:\n" + element);
            }
        }

        Assertions.assertEquals(List.of(), representation.unpresentedChanges, "Changes presented by no diff");
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("expectations")
    public void testExpectation(TestCase testCase, Expectation expectation) throws Throwable {
        Representation representation = load(testCase);
        try {
            expectation.check(representation);
        } catch (AssertionFailedError failure) {
            if (expectation.todo == null) {
                throw failure;
            }
            Assumptions.abort("TODO, " + expectation.todo + ": " + failure.getMessage());
        }
        if (expectation.todo != null) {
            Assertions.fail("The ideal of this expectation is reached; remove its todo and update the notes of " + testCase + ": "
                    + expectation.todo);
        }
    }

    private static Representation load(TestCase testCase) throws Exception {
        Representation representation = REPRESENTATIONS.get(testCase.dataset + "-" + testCase.commit);
        if (representation != null) {
            return representation;
        }

        String url = "https://github.com/" + testCase.repository + "/compare/" + testCase.base_commit + "..." + testCase.commit;

        Graph<Node, Edge> graph = Driver.getCompareGraph(url);
        List<Cluster> clusters = new Clusterer(graph).getClusters();
        List<Narrator.ChapterUnit> chapters = chapters(clusters);

        List<Element> elements = new ArrayList<>();
        Set<Node> presented = new HashSet<>();
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < chapters.size(); i++) {
            String chapter = chapters.get(i).getContent();
            output.append("===== CHAPTER ").append(i + 1).append("/").append(chapters.size()).append(" =====\n").append(chapter).append("\n");

            Matcher matcher = ELEMENT.matcher(chapter);
            while (matcher.find()) {
                Map<String, String> attributes = new LinkedHashMap<>();
                Matcher attribute = ATTRIBUTE.matcher(matcher.group(2));
                while (attribute.find()) {
                    attributes.put(attribute.group(1), attribute.group(2));
                }
                elements.add(new Element(i, elements.size(), matcher.group(1), attributes, matcher.group(3)));
            }

            for (ReviewNode node : chapters.get(i).getAnchoredNodes()) {
                if (node instanceof DiffNode diffNode) {
                    presented.addAll(diffNode.getSrcChanges());
                    presented.addAll(diffNode.getDstChanges());
                }
            }
        }

        List<String> unpresentedChanges = new ArrayList<>();
        for (Cluster cluster : clusters) {
            for (Node node : cluster.getGraph().vertexSet()) {
                if (node.isBase() && !presented.contains(node)) {
                    unpresentedChanges.add(node.getId());
                }
            }
        }

        Files.createDirectories(Path.of(ACTUAL_DIR));
        Files.writeString(Path.of(ACTUAL_DIR, testCase.dataset + "-" + testCase.commit + ".txt"), output.toString());

        representation = new Representation(elements, unpresentedChanges);
        REPRESENTATIONS.put(testCase.dataset + "-" + testCase.commit, representation);
        return representation;
    }

    /** The chapters of {@link GrainLevel#SINGLE}, with the shortest prompt ids that tell its diffs apart. */
    private static List<Narrator.ChapterUnit> chapters(List<Cluster> clusters) {
        List<TraversalPattern> patterns = new ArrayList<>();
        for (Cluster cluster : clusters) {
            patterns.add(new TraversalEngine(cluster).get());
        }
        TraversalPattern root = patterns.size() == 1 ? patterns.get(0) : new TraversalComponent(patterns, ReasonType.CONTEXT);

        List<Narrator.ChapterUnit> chapters = null;
        for (int length = Node.PROMPT_ID_LENGTH; length <= Node.MAX_PROMPT_ID_LENGTH; length++) {
            chapters = root.getNarrator().getFlatChapters(GrainLevel.SINGLE, length, true);
            List<String> promptIds = chapters.stream().flatMap(chapter -> chapter.getAnchoredNodes().stream())
                    .map(ReviewNode::getPromptId).toList();
            if (promptIds.size() == new HashSet<>(promptIds).size()) {
                break;
            }
        }
        return chapters;
    }

    private record Element(int chapter, int index, String kind, Map<String, String> attributes, String body) {
        List<String> list(String... attributeNames) {
            List<String> values = new ArrayList<>();
            for (String attributeName : attributeNames) {
                String value = attributes.get(attributeName);
                if (value != null) {
                    values.addAll(Arrays.asList(value.split(", ")));
                }
            }
            return values;
        }

        boolean changes(String file, String code) {
            if (!kind.equals("diff") || !attributes.getOrDefault("location", "").startsWith(file)) {
                return false;
            }
            return body.lines().anyMatch(line -> !line.isEmpty() && line.charAt(0) != ' ' && line.substring(1).trim().equals(code.trim()));
        }

        @Override
        public String toString() {
            StringBuilder element = new StringBuilder("<").append(kind);
            attributes.forEach((name, value) -> element.append(' ').append(name).append("=\"").append(value).append('"'));
            return element.append(">\n").append(body).append("\n</").append(kind).append('>').toString();
        }
    }

    private record Representation(List<Element> elements, List<String> unpresentedChanges) {
        Element diff(String id) {
            return elements.stream().filter(element -> element.kind.equals("diff") && id.equals(element.attributes.get("id")))
                    .findFirst().orElse(null);
        }

        List<Element> located(String kind, String location, String body) {
            return elements.stream().filter(element -> element.kind.equals(kind)
                    && (location == null || location.equals(element.attributes.get("location")))
                    && (body == null || body.equals(element.body))).toList();
        }

        /** The only element (of the kind, when given) whose code holds the snippet. */
        Element holding(String snippet, String kind) {
            List<Element> holding = elements.stream()
                    .filter(element -> (kind == null || element.kind.equals(kind)) && element.body.contains(snippet)).toList();
            Assertions.assertEquals(1, holding.size(), "Elements holding <" + snippet + ">: " + holding);
            return holding.get(0);
        }
    }

    public static class TestCase {
        public String dataset;
        public String repository;
        public String commit;
        public String base_commit;
        public String description;
        public List<String> notes = List.of();
        public List<Expectation> expectations = List.of();

        @Override
        public String toString() {
            return dataset + " (" + commit.substring(0, 8) + ")";
        }
    }

    /**
     * What the output of a case must show, by its kind:
     * <ul>
     *   <li>{@code diff}: the diff at {@code location} printed exactly as {@code body}.</li>
     *   <li>{@code dependency}: {@code count} (1 unless given) dependencies at {@code location} printed exactly as
     *   {@code body}.</li>
     *   <li>{@code unchanged}: no diff of {@code file} marks {@code code} (a whole line, trimmed) as removed or added.</li>
     *   <li>{@code serves}: the element holding {@code from} serves the diff holding {@code to}.</li>
     *   <li>{@code order}: the element holding {@code before} is printed before the element holding {@code after}.</li>
     *   <li>{@code diffs}: {@code count} diffs at {@code location}.</li>
     *   <li>{@code contains} / {@code absent}: some element ({@code count} elements, when given) holds {@code text} / no
     *   element holds it; only elements of the kind {@code of} ({@code diff} or {@code dependency}) count, when given.</li>
     * </ul>
     */
    public static class Expectation {
        public String description;
        public String kind;
        public String location;
        public String body;
        public String file;
        public String code;
        public String from;
        public String to;
        public String before;
        public String after;
        public String text;
        public Integer count;
        public String of;
        public String todo;

        void check(Representation representation) {
            switch (kind) {
                case "diff" -> Assertions.assertEquals(1, representation.located("diff", location, body).size(),
                        "Diff not printed as expected; diffs at the location: " + representation.located("diff", location, null));
                case "dependency" -> Assertions.assertEquals(count == null ? 1 : count, representation.located("dependency", location, body).size(),
                        "Dependencies printed so at the location; dependencies at the location: " + representation.located("dependency", location, null));
                case "unchanged" -> Assertions.assertEquals(List.of(),
                        representation.elements.stream().filter(element -> element.changes(file, code)).toList(), "Diffs marking the line as changed");
                case "serves" -> {
                    Element served = representation.holding(to, "diff");
                    Element serving = representation.holding(from, null);
                    Assertions.assertTrue(serving.list("serves").contains(served.attributes.get("id")),
                            "Does not serve " + served.attributes.get("id") + ":\n" + serving);
                }
                case "order" -> {
                    Element first = representation.holding(before, null);
                    Element second = representation.holding(after, null);
                    Assertions.assertTrue(first.chapter == second.chapter && first.index < second.index,
                            "Printed in the other order:\n" + first + "\n" + second);
                }
                case "diffs" -> Assertions.assertEquals(count, representation.located("diff", location, null).size(), "Diffs at the location");
                case "contains" -> {
                    List<Element> holding = holding(representation);
                    if (count == null) {
                        Assertions.assertFalse(holding.isEmpty(), "No element holds the text");
                    } else {
                        Assertions.assertEquals(count, holding.size(), "Elements holding the text: " + holding);
                    }
                }
                case "absent" -> Assertions.assertEquals(List.of(), holding(representation), "Elements holding the text");
                default -> throw new IllegalArgumentException("Unknown expectation kind: " + kind);
            }
        }

        private List<Element> holding(Representation representation) {
            return representation.elements.stream().filter(element -> (of == null || element.kind.equals(of)) && element.body.contains(text))
                    .toList();
        }

        @Override
        public String toString() {
            return description + (todo == null ? "" : " [todo]");
        }
    }
}
