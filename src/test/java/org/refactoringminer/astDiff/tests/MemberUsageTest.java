package org.refactoringminer.astDiff.tests;

import com.google.gson.*;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.astDiff.graph.MemberUsageFinder;
import org.refactoringminer.astDiff.graph.ReceiverTypeResolver;
import gr.uom.java.xmi.RepositoryStateFiles;
import org.refactoringminer.astDiff.utils.RmGap;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MemberUsageTest {

    // The files read from GitHub, kept so that later runs need no GitHub access
    private static final String CACHE_PATH = "src/test/resources/oracle/commits";

    static {
        // The cases share one JVM, and the HTTP/2 connection of the default connector gets its streams reset while
        // downloading the recursive trees of large repositories one after the other
        System.setProperty("test.github.connector", "urlconnection");
    }
    private static final String ORACLE_DATA_PATH = "src/test/resources/narrator/usages.json";
    private static final Map<String, State> STATES = new HashMap<>();

    static Stream<Arguments> testCases() throws IOException {
        String content = Files.readString(Path.of(ORACLE_DATA_PATH));
        List<TestCase> cases = new Gson().fromJson(content, new com.google.gson.reflect.TypeToken<List<TestCase>>(){}.getType());
        return cases.stream().flatMap(testCase -> testCase.usages.stream().map(usage -> Arguments.of(testCase, usage)));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("testCases")
    public void testUsage(TestCase testCase, Usage usage) throws Throwable {
        RmGap.check(usage.rmGap, () -> checkUsage(testCase, usage));
    }

    private static void checkUsage(TestCase testCase, Usage usage) throws Exception {
        State state = load(testCase);
        UMLModel model = new UMLModelASTReader(state.contents, new LinkedHashSet<>(), false).getUmlModel();
        UMLModelDiff modelDiff = new UMLModelDiff(new UMLModel(new LinkedHashSet<>()), model);
        ReceiverTypeResolver resolver = new ReceiverTypeResolver(modelDiff, state.files);
        Predicate<LocationInfo> inChange = location -> location.getFilePath().equals(usage.file) && location.getStartLine() == usage.line;

        MemberUsageFinder finder = new MemberUsageFinder(resolver, (umlModel, location) -> inChange.test(location));
        finder.bindChanges(model);

        LeafExpression node = findNode(model, usage);
        Assertions.assertNotNull(node, "Usage not found in the model: " + usage);
        List<String> targets = new ArrayList<>();
        for (UMLAbstractClass umlClass : classesOf(model)) {
            if (usage.kind.equals("invocation")) {
                for (UMLOperation operation : umlClass.getOperations()) {
                    if (finder.findInvocations(operation, model).stream().anyMatch(call -> call.getLocationInfo().equals(node.getLocationInfo()))) {
                        targets.add(describe(operation));
                    }
                }
            } else {
                List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
                attributes.addAll(umlClass.getEnumConstants());
                for (UMLAttribute attribute : attributes) {
                    if (finder.findAccesses(attribute, model).stream().anyMatch(access -> access.getLocationInfo().equals(node.getLocationInfo()))) {
                        targets.add(describe(attribute));
                    }
                }
            }
        }
        List<String> expected = usage.target == null ? List.of() : List.of(usage.target.toString());
        Assertions.assertEquals(expected, targets, "Declarations bound for " + usage);
    }

    private static List<UMLAbstractClass> classesOf(UMLModel model) {
        List<UMLAbstractClass> classes = new ArrayList<>();
        for (UMLClass umlClass : model.getClassList()) {
            classes.add(umlClass);
            classes.addAll(umlClass.getAnonymousClassList());
        }
        return classes;
    }

    /** The call or name of the usage, among the code of the classes (and of their lambdas) of the model. */
    private static LeafExpression findNode(UMLModel model, Usage usage) {
        for (UMLAbstractClass umlClass : classesOf(model)) {
            if (!umlClass.getSourceFile().equals(usage.file)) {
                continue;
            }
            List<VariableDeclarationContainer> members = new ArrayList<>(umlClass.getOperations());
            members.addAll(umlClass.getInitializers());
            members.addAll(umlClass.getAttributes());
            members.addAll(umlClass.getEnumConstants());
            for (VariableDeclarationContainer member : members) {
                for (AbstractCodeFragment fragment : fragmentsOf(member)) {
                    List<LeafExpression> nodes = new ArrayList<>();
                    if (usage.kind.equals("invocation")) {
                        nodes.addAll(fragment.getMethodInvocations());
                        nodes.addAll(fragment.getCreations());
                    } else {
                        nodes.addAll(fragment.getVariables());
                    }
                    for (LeafExpression node : nodes) {
                        if (usage.matches(node)) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<AbstractCodeFragment> fragmentsOf(VariableDeclarationContainer member) {
        List<AbstractCodeFragment> fragments = new ArrayList<>();
        Deque<VariableDeclarationContainer> pending = new ArrayDeque<>(List.of(member));
        Set<VariableDeclarationContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (!pending.isEmpty()) {
            VariableDeclarationContainer container = pending.poll();
            if (!visited.add(container)) {
                continue;
            }
            pending.addAll(container.getAllLambdas());
            if (container instanceof UMLAttribute attribute) {
                if (attribute.getVariableDeclaration().getInitializer() != null) {
                    fragments.add(attribute.getVariableDeclaration().getInitializer());
                }
                continue;
            }
            if (container.getBody() != null) {
                for (AbstractStatement statement : container.getBody().getCompositeStatement().getAllStatements()) {
                    if (statement instanceof CompositeStatementObject composite) {
                        fragments.addAll(composite.getExpressions());
                    } else {
                        fragments.add(statement);
                    }
                }
            }
            if (container.getDefaultExpression() != null) {
                fragments.add(container.getDefaultExpression());
            }
        }
        return fragments;
    }

    private static String describe(UMLOperation operation) {
        StringJoiner parameters = new StringJoiner(",");
        for (UMLParameter parameter : operation.getParametersWithoutReturnType()) {
            parameters.add(String.valueOf(parameter.getType()));
        }
        return new Target(operation.getLocationInfo().getFilePath(), operation.getClassName(),
                operation.getName() + "(" + parameters + ")").toString();
    }

    private static String describe(UMLAttribute attribute) {
        return new Target(attribute.getLocationInfo().getFilePath(), attribute.getClassName(), attribute.getName()).toString();
    }

    private static State load(TestCase testCase) throws Exception {
        String key = testCase.dataset + "-" + testCase.commit;
        State state = STATES.get(key);
        if (state != null) {
            return state;
        }

        String repo = testCase.repository;

        RepositoryStateFiles files = new RepositoryStateFiles(
                () -> new GitHistoryRefactoringMinerImpl().getGitHubRepository("https://github.com/" + repo),
                repo.substring(repo.lastIndexOf('/') + 1), testCase.commit, new File(CACHE_PATH));
        Map<String, String> contents = new LinkedHashMap<>();
        for (String path : testCase.files) {
            contents.put(path, files.content(path));
        }

        state = new State(files, contents);
        STATES.put(key, state);
        return state;
    }

    private record State(RepositoryStateFiles files, Map<String, String> contents) {
    }

    public static class TestCase {
        public String dataset;
        public String repository;
        public String commit;
        public List<String> files;
        public List<Usage> usages;

        @Override
        public String toString() {
            return dataset;
        }
    }

    public static class Usage {
        public String description;
        public String kind;
        public String file;
        public int line;
        public String code;
        public Target target;
        public RmGap rmGap;

        boolean matches(LeafExpression expression) {
            return expression.getLocationInfo().getStartLine() == line
                    && expression.getString().replaceAll("\\s+", "").equals(code.replaceAll("\\s+", ""));
        }

        @Override
        public String toString() {
            return description + ": " + code + " (" + file.substring(file.lastIndexOf('/') + 1) + ":" + line + ")"
                    + (rmGap == null ? "" : " [" + rmGap + "]");
        }
    }

    /** A declaration: the file and the class declaring it, and its name, with the parameter types of an operation. */
    public static class Target {
        public String file;
        public String className;
        public String member;

        Target(String file, String className, String member) {
            this.file = file;
            this.className = className;
            this.member = member;
        }

        @Override
        public String toString() {
            return className + "#" + member + " @ " + file;
        }
    }
}
