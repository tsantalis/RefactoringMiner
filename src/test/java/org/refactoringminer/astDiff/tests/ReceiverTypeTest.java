package org.refactoringminer.astDiff.tests;

import com.google.gson.*;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.astDiff.graph.ReceiverTypeResolver;
import gr.uom.java.xmi.RepositoryStateFiles;
import org.refactoringminer.astDiff.utils.RmGap;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.PathFileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ReceiverTypeTest {

    // The files read from GitHub, kept so that later runs need no GitHub access
    private static final String CACHE_PATH = "src/test/resources/oracle/commits";

    static {
        // The cases share one JVM, and the HTTP/2 connection of the default connector gets its streams reset while
        // downloading the recursive trees of large repositories one after the other
        System.setProperty("test.github.connector", "urlconnection");
    }
    private static final String ORACLE_DATA_PATH = "src/test/resources/narrator/receivers.json";
    private static final Map<String, State> STATES = new HashMap<>();

    static Stream<Arguments> testCases() throws IOException {
        String content = Files.readString(Path.of(ORACLE_DATA_PATH));
        List<TestCase> cases = new Gson().fromJson(content, new com.google.gson.reflect.TypeToken<List<TestCase>>(){}.getType());
        return cases.stream().flatMap(testCase -> testCase.receivers.stream().map(receiver -> Arguments.of(testCase, receiver)));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("testCases")
    public void testReceiverType(TestCase testCase, Receiver receiver) throws Throwable {
        RmGap.check(receiver.rmGap, () -> checkReceiver(testCase, receiver));
    }

    private static void checkReceiver(TestCase testCase, Receiver receiver) throws Exception {
        State state = load(testCase);
        UMLModel childModel = new UMLModelASTReader(state.contents, new LinkedHashSet<>(), false).getUmlModel();
        UMLModelDiff modelDiff = new UMLModelDiff(new UMLModel(new LinkedHashSet<>()), childModel);
        ReceiverTypeResolver resolver = new ReceiverTypeResolver(modelDiff, state.files);

        Set<String> initialFiles = modelFiles(modelDiff);
        if (receiver.kind.equals("override")) {
            checkOverridden(resolver, childModel, receiver);
            return;
        }
        if (receiver.kind.equals("dispatch")) {
            checkDispatch(resolver, childModel, receiver);
            return;
        }
        ReceiverTypeResolver.ReceiverType receiverType = resolveReceiver(resolver, childModel, receiver);
        if (receiver.type == null) {
            // receivers whose type is not resolved (e.g., the return of a method outside the repository)
            Assertions.assertNull(receiverType, "Unexpected receiver type for " + receiver);
        } else {
            Assertions.assertNotNull(receiverType, "No receiver type for " + receiver);
            Assertions.assertEquals(receiver.type, receiverType.type().toQualifiedString(), "Type mismatch for " + receiver);
        }

        // the files declaring the type and those passed through to resolve it, outside the changed files
        Set<String> files = new TreeSet<>();
        if (receiverType != null) {
            files.addAll(receiverType.files());
            files.addAll(resolver.resolvePaths(receiverType));
        }
        files.removeIf(file -> !PathFileUtils.isSupportedFile(file));
        for (String file : receiver.files) {
            Assertions.assertTrue(files.contains(file) || initialFiles.contains(file), "File of the receiver type not found: " + file);
        }

        Set<String> expectedFiles = new TreeSet<>(receiver.files);
        expectedFiles.addAll(receiver.intermediates);
        expectedFiles.removeAll(initialFiles);
        files.removeAll(initialFiles);
        Assertions.assertEquals(expectedFiles, files, "Files mismatch for " + receiver);
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

    private static ReceiverTypeResolver.ReceiverType resolveReceiver(ReceiverTypeResolver resolver, UMLModel model, Receiver receiver) {
        List<UMLAbstractClass> anonymousClasses = new ArrayList<>();
        for (UMLClass umlClass : model.getClassList()) {
            if (umlClass.getSourceFile().equals(receiver.file)) {
                anonymousClasses.addAll(umlClass.getAnonymousClassList());
            }
        }
        // the anonymous classes have their own code (e.g., those created in field initializers), with themselves as context
        List<UMLAbstractClass> classes = new ArrayList<>(model.getClassList());
        classes.addAll(anonymousClasses);
        for (UMLAbstractClass umlClass : classes) {
            if (!umlClass.getSourceFile().equals(receiver.file)) {
                continue;
            }
            List<VariableDeclarationContainer> containers = new ArrayList<>(umlClass.getOperations());
            containers.addAll(umlClass.getInitializers());
            for (VariableDeclarationContainer container : containers) {
                if (receiver.kind.equals("invocation")) {
                    for (AbstractCall invocation : container.getAllOperationInvocations()) {
                        if (receiver.matches(invocation)) {
                            return resolver.resolveInvocationReceiver(invocation, umlClass, model);
                        }
                    }
                } else if (container.getBody() != null) {
                    for (AbstractStatement statement : container.getBody().getCompositeStatement().getAllStatements()) {
                        for (LeafExpression variable : statement.getVariables()) {
                            if (receiver.matches(variable)) {
                                return resolver.resolveVariableReceiver(variable, umlClass, model);
                            }
                        }
                    }
                }
            }
        }

        Assertions.fail("Receiver not found in the model: " + receiver);
        return null;
    }

    // The methods the method declared at the line overrides or implements, as the hierarchy binds the type variables of
    // its supertypes (the files declaring them are attached to the model)
    private static void checkOverridden(ReceiverTypeResolver resolver, UMLModel model, Receiver receiver) {
        List<UMLAbstractClass> classes = new ArrayList<>();
        for (UMLClass umlClass : new ArrayList<>(model.getClassList())) {
            if (umlClass.getSourceFile().equals(receiver.file)) {
                classes.add(umlClass);
                classes.addAll(umlClass.getAnonymousClassList());
            }
        }
        for (UMLAbstractClass umlClass : classes) {
            for (UMLOperation operation : umlClass.getOperations()) {
                if (operation.getName().equals(receiver.code) && operation.getLocationInfo().getStartLine() <= receiver.line
                        && receiver.line <= operation.getLocationInfo().getEndLine()) {
                    List<String> overridden = new ArrayList<>();
                    for (UMLOperation superOperation : resolver.findAttachedOverridden(operation, umlClass, model)) {
                        StringJoiner parameters = new StringJoiner(",");
                        superOperation.getParametersWithoutReturnType().forEach(parameter -> parameters.add(String.valueOf(parameter.getType())));
                        overridden.add(superOperation.getClassName() + "#" + superOperation.getName() + "(" + parameters + ") @ "
                                + superOperation.getLocationInfo().getFilePath());
                    }
                    Assertions.assertEquals(receiver.overridden, overridden, "Overridden methods of " + receiver);
                    return;
                }
            }
        }
        Assertions.fail("Method not found in the model: " + receiver);
    }

    // The method a call runs on an instance of the runtime class: of the method declared at the line (the code its name),
    // or, with an arity, of a method of that name and arity declared outside the repository
    private static void checkDispatch(ReceiverTypeResolver resolver, UMLModel model, Receiver receiver) {
        UMLAbstractClass runtime = null;
        for (UMLClass umlClass : new ArrayList<>(model.getClassList())) {
            if (umlClass.getName().equals(receiver.runtime)) {
                runtime = umlClass;
            }
        }
        Assertions.assertNotNull(runtime, "Runtime class not found in the model: " + receiver.runtime);
        UMLOperation runs;
        if (receiver.arity != null) {
            runs = resolver.findAttachedDispatchTarget(receiver.code, receiver.arity, runtime, model);
        } else {
            UMLOperation called = null;
            for (UMLClass umlClass : new ArrayList<>(model.getClassList())) {
                for (UMLOperation operation : umlClass.getOperations()) {
                    if (umlClass.getSourceFile().equals(receiver.file) && operation.getName().equals(receiver.code)
                            && operation.getLocationInfo().getStartLine() <= receiver.line && receiver.line <= operation.getLocationInfo().getEndLine()) {
                        called = operation;
                    }
                }
            }
            Assertions.assertNotNull(called, "Method not found in the model: " + receiver);
            runs = resolver.findAttachedDispatchTarget(called, runtime, model);
        }
        String described = null;
        if (runs != null) {
            StringJoiner parameters = new StringJoiner(",");
            runs.getParametersWithoutReturnType().forEach(parameter -> parameters.add(String.valueOf(parameter.getType())));
            described = runs.getClassName() + "#" + runs.getName() + "(" + parameters + ") @ " + runs.getLocationInfo().getFilePath();
        }
        Assertions.assertEquals(receiver.runs, described, "Method run for " + receiver);
    }

    private static Set<String> modelFiles(UMLModelDiff modelDiff) {
        Set<String> files = new HashSet<>();
        Stream.of(modelDiff.getParentModel(), modelDiff.getChildModel())
                .flatMap(model -> model.getClassList().stream())
                .forEach(umlClass -> files.add(umlClass.getSourceFile()));
        return files;
    }

    private record State(RepositoryStateFiles files, Map<String, String> contents) {
    }

    public static class TestCase {
        public String dataset;
        public String repository;
        public String commit;
        public List<String> files;
        public List<Receiver> receivers;

        @Override
        public String toString() {
            return dataset;
        }
    }

    public static class Receiver {
        public String description;
        public String kind;
        public String file;
        public int line;
        public String code;
        public String type;
        public List<String> files;
        public List<String> intermediates;
        // For an override: the methods it overrides, as Class#name(parameter types) @ file
        public List<String> overridden;
        // For a dispatch: the class of the receiver, the arity of a method outside the repository, and the method run
        public String runtime;
        public Integer arity;
        public String runs;
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
}
