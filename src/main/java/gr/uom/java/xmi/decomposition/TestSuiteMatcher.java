package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.ModuleContainer.nestedDescribeMap;
import static gr.uom.java.xmi.ModuleContainer.nestedItMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

/**
 * The matching of the test suites describe('...', () => {...}) and the tests it('...', () => {...}) of JavaScript and TypeScript test files,
 * test by test, within the mapper of a module or of the callback of a matched test suite.
 * <p>
 * A matched test suite is a statement mapping of the module, and its callback, when it changed, is matched by a suite mapper:
 * the nested test suites and the tests are matched by their names, then the renamed ones by the similarity of their callbacks,
 * and the tests moved into (or out of) the test suites added (or removed) in the suite, while the changed tests are matched within their callbacks.
 * The functions and the types declared in the callbacks are matched by their names, and the data objects (i.e., object literals)
 * of the mapped statements are diffed once the statements are matched, as their diffs are skipped while matching the statements.
 * The mappings are kept in the lambda mappers of the statement mappings, i.e., not added to the mapper of the module, whose processing is costly.
 */
class TestSuiteMatcher {
	private static final double RENAMED_TEST_SIMILARITY = 0.5;
	private static final double DATA_OBJECT_SIMILARITY = 0.5;
	private static final Pattern TOKEN = Pattern.compile("[A-Za-z_$][\\w$]*|\\d+|'[^'\\n]*'|\"[^\"\\n]*\"|`[^`]*`");

	private final UMLOperationBodyMapper mapper;

	TestSuiteMatcher(UMLOperationBodyMapper mapper) {
		this.mapper = mapper;
	}

	//a matched test suite of the module, whose callback, when it changed, is matched by a suite mapper
	void addTestSuiteMapping(AbstractStatement suite1, AbstractStatement suite2) throws RefactoringMinerTimedOutException {
		LeafMapping mapping = mapper.createLeafMapping(suite1, suite2, new LinkedHashMap<String, String>(), true, true);
		mapper.addMapping(mapping);
		if(changedTestCallback(suite1, suite2)) {
			UMLOperationBodyMapper suiteMapper = new UMLOperationBodyMapper(suite1, suite2, mapper);
			new TestSuiteMatcher(suiteMapper).matchNestedOperations(suite1.getLambdas().get(0), suite2.getLambdas().get(0));
			mapping.addLambdaMappers(List.of(suiteMapper));
		}
	}

	//the callback of a matched test suite (the mapper is the suite mapper): the nested test suites and the tests are matched by their names
	//(the changed ones within their callbacks), and the other statements (i.e., setup, helper functions) as the statements of a lambda
	void matchSuiteCallback(AbstractStatement suite1, AbstractStatement suite2) throws RefactoringMinerTimedOutException {
		LambdaExpressionObject lambda1 = suite1.getLambdas().get(0);
		LambdaExpressionObject lambda2 = suite2.getLambdas().get(0);
		Set<AbstractCodeFragment> tests1 = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<AbstractCodeFragment> tests2 = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<String, AbstractStatement> describeMap1 = nestedDescribeMap(suite1);
		Map<String, AbstractStatement> describeMap2 = nestedDescribeMap(suite2);
		Map<String, AbstractStatement> itMap1 = nestedItMap(suite1);
		Map<String, AbstractStatement> itMap2 = nestedItMap(suite2);
		tests1.addAll(describeMap1.values());
		tests1.addAll(itMap1.values());
		tests2.addAll(describeMap2.values());
		tests2.addAll(itMap2.values());
		matchTestsByName(describeMap1, describeMap2, true);
		matchTestsByName(itMap1, itMap2, false);
		matchTestsMovedBetweenSuites(describeMap1.values(), describeMap2.values(), itMap1.values(), itMap2.values());
		CompositeStatementObject composite1 = lambda1.getBody().getCompositeStatement();
		CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
		List<AbstractCodeFragment> leaves1 = new ArrayList<>(composite1.getLeaves());
		List<AbstractCodeFragment> leaves2 = new ArrayList<>(composite2.getLeaves());
		leaves1.removeIf(tests1::contains);
		leaves2.removeIf(tests2::contains);
		mapper.processCompositeStatements(leaves1, leaves2, composite1.getInnerNodes(), composite2.getInnerNodes());
		diffDataObjectsOfMappedStatements();
	}

	//the tests it('...', () => {...}) of the test suites describe('...', () => {...}) calling the added functions, matched by their path of names,
	//whose unmatched statements can be extracted to the added functions; the other tests are not matched statement by statement
	void matchTestsCallingAddedOperations(List<UMLOperation> addedOperations) throws RefactoringMinerTimedOutException {
		if(!(mapper.getContainer1() instanceof ModuleContainer module1) || !(mapper.getContainer2() instanceof ModuleContainer module2) || addedOperations.isEmpty()) {
			return;
		}
		Set<String> addedOperationNames = new LinkedHashSet<>();
		for(UMLOperation addedOperation : addedOperations) {
			addedOperationNames.add(addedOperation.getName());
		}
		Map<String, AbstractStatement> tests1 = new LinkedHashMap<>();
		Map<String, AbstractStatement> tests2 = new LinkedHashMap<>();
		for(String key : module1.getDescribeMap().keySet()) {
			collectTests(key, module1.getDescribeMap().get(key), tests1);
		}
		for(String key : module2.getDescribeMap().keySet()) {
			collectTests(key, module2.getDescribeMap().get(key), tests2);
		}
		for(String path : tests2.keySet()) {
			AbstractStatement test2 = tests2.get(path);
			AbstractStatement test1 = tests1.get(path);
			if(test1 != null && !test1.getString().equals(test2.getString()) && test1.getLambdas().size() == 1 && test2.getLambdas().size() == 1 &&
					test1.getLambdas().get(0).getBody() != null && callsAnyOperation(test2.getLambdas().get(0), addedOperationNames)) {
				//the test, matched by the mapper of its test suite, is taken over by this mapper
				Pair<UMLOperationBodyMapper, AbstractCodeMapping> inSuite = mapper.getTestMappingsInSuites().remove(test1);
				if(inSuite != null) {
					inSuite.getLeft().getMappings().remove(inSuite.getRight());
				}
				//the test is the parent of its statements, delimiting the scope of the statements extracted for each call
				LeafMapping testMapping = mapper.createLeafMapping(test1, test2, new LinkedHashMap<String, String>(), true, true);
				mapper.addMapping(testMapping);
				UMLOperationBodyMapper lambdaMapper = new UMLOperationBodyMapper(test1.getLambdas().get(0), test2.getLambdas().get(0), mapper);
				mapper.addAllMappings(lambdaMapper.getMappings());
				mapper.getNonMappedLeavesT1().addAll(lambdaMapper.getNonMappedLeavesT1());
				mapper.getNonMappedLeavesT2().addAll(lambdaMapper.getNonMappedLeavesT2());
				mapper.getNonMappedInnerNodesT1().addAll(lambdaMapper.getNonMappedInnerNodesT1());
				mapper.getNonMappedInnerNodesT2().addAll(lambdaMapper.getNonMappedInnerNodesT2());
			}
		}
	}

	private void matchTestsByName(Map<String, AbstractStatement> tests1, Map<String, AbstractStatement> tests2, boolean testSuites) throws RefactoringMinerTimedOutException {
		List<AbstractStatement> unpaired1 = new ArrayList<>();
		List<AbstractStatement> unpaired2 = new ArrayList<>();
		for(String name : tests1.keySet()) {
			if(tests2.containsKey(name)) {
				matchTest(tests1.get(name), tests2.get(name), testSuites);
			}
			else {
				unpaired1.add(tests1.get(name));
			}
		}
		for(String name : tests2.keySet()) {
			if(!tests1.containsKey(name)) {
				unpaired2.add(tests2.get(name));
			}
		}
		Set<AbstractStatement> paired1 = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<AbstractStatement> paired2 = Collections.newSetFromMap(new IdentityHashMap<>());
		if(testSuites) {
			//the renamed test suites with the same tests
			for(AbstractStatement suite1 : unpaired1) {
				Set<String> tests = nestedItMap(suite1).keySet();
				for(AbstractStatement suite2 : unpaired2) {
					if(!paired2.contains(suite2) && !tests.isEmpty() && tests.equals(nestedItMap(suite2).keySet())) {
						matchTest(suite1, suite2, true);
						paired1.add(suite1);
						paired2.add(suite2);
						break;
					}
				}
			}
		}
		matchRenamedTests(unpaired1, unpaired2, paired1, paired2, testSuites);
		if(!testSuites && unpaired1.size() - paired1.size() == 1 && unpaired2.size() - paired2.size() == 1) {
			//the only renamed test
			AbstractStatement test1 = unpaired1.stream().filter(t -> !paired1.contains(t)).findFirst().get();
			AbstractStatement test2 = unpaired2.stream().filter(t -> !paired2.contains(t)).findFirst().get();
			matchTest(test1, test2, false);
			paired1.add(test1);
			paired2.add(test2);
		}
		for(AbstractStatement test1 : unpaired1) {
			if(!paired1.contains(test1)) {
				mapper.getNonMappedLeavesT1().add(test1);
			}
		}
		for(AbstractStatement test2 : unpaired2) {
			if(!paired2.contains(test2)) {
				mapper.getNonMappedLeavesT2().add(test2);
			}
		}
	}

	//the tests moved into a test suite added in the suite, or out of a test suite removed from it, i.e., it('should build ticks using date objects', ...) ->
	//describe('when given inputs of different types', () => { it('should accept labels as date objects', ...) }), matched by their names, and the renamed ones by their callbacks
	private void matchTestsMovedBetweenSuites(Collection<AbstractStatement> suites1, Collection<AbstractStatement> suites2,
			Collection<AbstractStatement> tests1, Collection<AbstractStatement> tests2) throws RefactoringMinerTimedOutException {
		List<AbstractCodeFragment> nonMappedLeavesT1 = mapper.getNonMappedLeavesT1();
		List<AbstractCodeFragment> nonMappedLeavesT2 = mapper.getNonMappedLeavesT2();
		List<AbstractStatement> unpairedSuites1 = suites1.stream().filter(nonMappedLeavesT1::contains).toList();
		List<AbstractStatement> unpairedSuites2 = suites2.stream().filter(nonMappedLeavesT2::contains).toList();
		if(unpairedSuites1.isEmpty() && unpairedSuites2.isEmpty()) {
			return;
		}
		List<AbstractStatement> candidates1 = new ArrayList<>(tests1.stream().filter(nonMappedLeavesT1::contains).toList());
		for(AbstractStatement suite : unpairedSuites1) {
			Map<String, AbstractStatement> tests = new LinkedHashMap<>();
			collectTests("", suite, tests);
			candidates1.addAll(tests.values());
		}
		List<AbstractStatement> candidates2 = new ArrayList<>(tests2.stream().filter(nonMappedLeavesT2::contains).toList());
		for(AbstractStatement suite : unpairedSuites2) {
			Map<String, AbstractStatement> tests = new LinkedHashMap<>();
			collectTests("", suite, tests);
			candidates2.addAll(tests.values());
		}
		if(candidates1.isEmpty() || candidates2.isEmpty()) {
			return;
		}
		Set<AbstractStatement> paired1 = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<AbstractStatement> paired2 = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<String, List<AbstractStatement>> byName1 = groupByName(candidates1, TestSuiteMatcher::testName);
		Map<String, List<AbstractStatement>> byName2 = groupByName(candidates2, TestSuiteMatcher::testName);
		for(String name : byName1.keySet()) {
			if(byName1.get(name).size() == 1 && byName2.containsKey(name) && byName2.get(name).size() == 1) {
				AbstractStatement test1 = byName1.get(name).get(0);
				AbstractStatement test2 = byName2.get(name).get(0);
				matchTest(test1, test2, false);
				paired1.add(test1);
				paired2.add(test2);
			}
		}
		matchRenamedTests(candidates1, candidates2, paired1, paired2, false);
		nonMappedLeavesT1.removeIf(paired1::contains);
		nonMappedLeavesT2.removeIf(paired2::contains);
	}

	//the renamed tests are matched by the similarity of their callbacks, the most similar first,
	//i.e., it('skips dart:async', ...) -> it('absorbs dart:async with empty-files sentinel', ...)
	private void matchRenamedTests(List<AbstractStatement> unpaired1, List<AbstractStatement> unpaired2,
			Set<AbstractStatement> paired1, Set<AbstractStatement> paired2, boolean testSuites) throws RefactoringMinerTimedOutException {
		List<AbstractStatement> tests1 = unpaired1.stream().filter(t -> !paired1.contains(t)).toList();
		List<AbstractStatement> tests2 = unpaired2.stream().filter(t -> !paired2.contains(t)).toList();
		if(tests1.isEmpty() || tests2.isEmpty()) {
			return;
		}
		List<Map<String, Integer>> tokens2 = new ArrayList<>();
		for(AbstractStatement test2 : tests2) {
			tokens2.add(tokens(callbackCode(test2)));
		}
		List<double[]> candidates = new ArrayList<>();
		for(int i = 0; i < tests1.size(); i++) {
			Map<String, Integer> tokens1 = tokens(callbackCode(tests1.get(i)));
			for(int j = 0; j < tests2.size(); j++) {
				double similarity = dice(tokens1, tokens2.get(j));
				if(similarity >= RENAMED_TEST_SIMILARITY) {
					candidates.add(new double[] {similarity, i, j});
				}
			}
		}
		candidates.sort((c1, c2) -> Double.compare(c2[0], c1[0]));
		for(double[] candidate : candidates) {
			AbstractStatement test1 = tests1.get((int)candidate[1]);
			AbstractStatement test2 = tests2.get((int)candidate[2]);
			if(!paired1.contains(test1) && !paired2.contains(test2)) {
				matchTest(test1, test2, testSuites);
				paired1.add(test1);
				paired2.add(test2);
			}
		}
	}

	private void matchTest(AbstractStatement test1, AbstractStatement test2, boolean testSuite) throws RefactoringMinerTimedOutException {
		LeafMapping mapping = mapper.createLeafMapping(test1, test2, new LinkedHashMap<String, String>(), true, true);
		mapper.addMapping(mapping);
		if(changedTestCallback(test1, test2)) {
			UMLOperationBodyMapper callbackMapper;
			if(testSuite) {
				callbackMapper = new UMLOperationBodyMapper(test1, test2, mapper);
				new TestSuiteMatcher(callbackMapper).matchNestedOperations(test1.getLambdas().get(0), test2.getLambdas().get(0));
			}
			else {
				//the functions and types declared in the callback are matched by the mapper of the callback
				callbackMapper = new UMLOperationBodyMapper(test1.getLambdas().get(0), test2.getLambdas().get(0), mapper, true);
				//the comments of the tests are matched by the comment diff of the module
				callbackMapper.clearCommentListDiff();
			}
			mapping.addLambdaMappers(List.of(callbackMapper));
		}
		if(!testSuite) {
			UMLOperationBodyMapper moduleMapper = mapper;
			while(moduleMapper.getParentMapper() != null) {
				moduleMapper = moduleMapper.getParentMapper();
			}
			moduleMapper.getTestMappingsInSuites().put(test1, Pair.of(mapper, mapping));
		}
	}

	//the functions declared in the callbacks are matched by their names, when each name is declared once in each callback,
	//while the functions that are operations of the module (i.e., the arrow functions declared in the callbacks) are matched by the class diff
	void matchNestedOperations(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2) throws RefactoringMinerTimedOutException {
		matchNestedClasses(lambda1, lambda2);
		UMLAbstractClassDiff classDiff = mapper.getClassDiff();
		if(classDiff == null || lambda1.getNestedOperations().isEmpty() || lambda2.getNestedOperations().isEmpty()) {
			return;
		}
		Map<String, List<UMLOperation>> operations1 = groupByName(lambda1.getNestedOperations(), UMLOperation::getName);
		Map<String, List<UMLOperation>> operations2 = groupByName(lambda2.getNestedOperations(), UMLOperation::getName);
		List<UMLOperationBodyMapper> mappers = new ArrayList<>();
		for(String name : operations1.keySet()) {
			if(operations1.get(name).size() == 1 && operations2.containsKey(name) && operations2.get(name).size() == 1) {
				UMLOperation operation1 = operations1.get(name).get(0);
				UMLOperation operation2 = operations2.get(name).get(0);
				if(!classDiff.getOriginalClass().getOperations().contains(operation1) && !classDiff.getNextClass().getOperations().contains(operation2)) {
					UMLOperationBodyMapper nestedMapper = new UMLOperationBodyMapper(operation1, operation2, classDiff);
					//the matched variables (i.e., the parameters) of the mapper, which are computed with the refactorings within the body
					nestedMapper.computeRefactoringsWithinBody();
					mappers.add(nestedMapper);
				}
			}
		}
		mapper.setNestedOperationMappers(mappers);
	}

	//the types declared in the callbacks (and not classes of the module) are matched by their names, when each name is declared once in each callback
	private void matchNestedClasses(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2) throws RefactoringMinerTimedOutException {
		if(lambda1.getNestedClasses().isEmpty() || lambda2.getNestedClasses().isEmpty()) {
			return;
		}
		Map<String, List<UMLClass>> classes1 = groupByName(lambda1.getNestedClasses(), UMLClass::getNonQualifiedName);
		Map<String, List<UMLClass>> classes2 = groupByName(lambda2.getNestedClasses(), UMLClass::getNonQualifiedName);
		List<UMLClassDiff> diffs = new ArrayList<>();
		for(String name : classes1.keySet()) {
			if(classes1.get(name).size() == 1 && classes2.containsKey(name) && classes2.get(name).size() == 1) {
				UMLClassDiff nestedClassDiff = new UMLClassDiff(classes1.get(name).get(0), classes2.get(name).get(0), mapper.getModelDiff());
				nestedClassDiff.process();
				diffs.add(nestedClassDiff);
			}
		}
		mapper.setNestedClassDiffs(diffs);
	}

	//the data objects of the mapped statements of a test callback, whose diffs are skipped while matching the statements
	void diffDataObjectsOfMappedStatements() throws RefactoringMinerTimedOutException {
		for(AbstractCodeMapping mapping : new ArrayList<>(mapper.getMappings())) {
			if(mapping instanceof LeafMapping && !mapping.getFragment1().getAnonymousClassDeclarations().isEmpty() && !mapping.getFragment2().getAnonymousClassDeclarations().isEmpty()) {
				diffDataObjects(mapping.getFragment1(), mapping.getFragment2());
			}
		}
	}

	//the objects not nested in other objects (i.e., the rows of it.each([...]) tables) of a mapped statement pair are aligned in order
	//by the similarity of their tokens, and the changed ones are diffed, so that their properties are matched by their keys
	private void diffDataObjects(AbstractCodeFragment statement1, AbstractCodeFragment statement2) throws RefactoringMinerTimedOutException {
		if(statement1.getString().equals(statement2.getString())) {
			return;
		}
		List<AnonymousClassDeclarationObject> objects1 = new ArrayList<>();
		List<AnonymousClassDeclarationObject> objects2 = new ArrayList<>();
		List<UMLAnonymousClass> classes1 = new ArrayList<>();
		List<UMLAnonymousClass> classes2 = new ArrayList<>();
		for(AnonymousClassDeclarationObject declaration : statement1.getAnonymousClassDeclarations()) {
			UMLAnonymousClass anonymousClass = mapper.findAnonymousClass1(declaration);
			if(anonymousClass != null && anonymousClass.getOperations().isEmpty() && !ReplacementAlgorithm.nestedInAnotherAnonymousClass(statement1, declaration) && !insideLambda(statement1, declaration)) {
				objects1.add(declaration);
				classes1.add(anonymousClass);
			}
		}
		if(objects1.isEmpty()) {
			return;
		}
		for(AnonymousClassDeclarationObject declaration : statement2.getAnonymousClassDeclarations()) {
			UMLAnonymousClass anonymousClass = mapper.findAnonymousClass2(declaration);
			if(anonymousClass != null && anonymousClass.getOperations().isEmpty() && !ReplacementAlgorithm.nestedInAnotherAnonymousClass(statement2, declaration) && !insideLambda(statement2, declaration)) {
				objects2.add(declaration);
				classes2.add(anonymousClass);
			}
		}
		if(objects2.isEmpty()) {
			return;
		}
		int n = objects1.size(), m = objects2.size();
		List<Map<String, Integer>> tokens1 = new ArrayList<>();
		List<Map<String, Integer>> tokens2 = new ArrayList<>();
		for(AnonymousClassDeclarationObject object : objects1) {
			tokens1.add(tokens(object.toString()));
		}
		for(AnonymousClassDeclarationObject object : objects2) {
			tokens2.add(tokens(object.toString()));
		}
		double[][] similarity = new double[n][m];
		for(int i=0; i<n; i++) {
			for(int j=0; j<m; j++) {
				similarity[i][j] = dice(tokens1.get(i), tokens2.get(j));
			}
		}
		//the order-preserving alignment maximizing the total similarity of the aligned objects
		double[][] score = new double[n+1][m+1];
		for(int i=n-1; i>=0; i--) {
			for(int j=m-1; j>=0; j--) {
				double best = Math.max(score[i+1][j], score[i][j+1]);
				if(similarity[i][j] >= DATA_OBJECT_SIMILARITY) {
					best = Math.max(best, similarity[i][j] + score[i+1][j+1]);
				}
				score[i][j] = best;
			}
		}
		UMLAbstractClassDiff classDiff = mapper.getClassDiff();
		UMLModelDiff modelDiff = mapper.getModelDiff();
		int i = 0, j = 0;
		while(i < n && j < m) {
			if(similarity[i][j] >= DATA_OBJECT_SIMILARITY && score[i][j] == similarity[i][j] + score[i+1][j+1]) {
				if(!objects1.get(i).toString().equals(objects2.get(j).toString())) {
					UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(classes1.get(i), classes2.get(j), classDiff, modelDiff);
					anonymousClassDiff.process();
					mapper.addPendingAnonymousClassDiff(anonymousClassDiff, statement1, statement2);
				}
				i++;
				j++;
			}
			else if(score[i][j] == score[i+1][j]) {
				i++;
			}
			else {
				j++;
			}
		}
	}

	//the objects in the callbacks of the statement, i.e., of it(...), are matched by the mappers of the callbacks
	private static boolean insideLambda(AbstractCodeFragment statement, AnonymousClassDeclarationObject declaration) {
		for(LambdaExpressionObject lambda : statement.getLambdas()) {
			if(lambda.getLocationInfo().subsumes(declaration.getLocationInfo())) {
				return true;
			}
		}
		return false;
	}

	private static boolean changedTestCallback(AbstractStatement test1, AbstractStatement test2) {
		return !test1.getString().equals(test2.getString()) && test1.getLambdas().size() > 0 && test2.getLambdas().size() > 0 &&
				test1.getLambdas().get(0).getBody() != null && test2.getLambdas().get(0).getBody() != null;
	}

	//the tests it('...', () => {...}) of a test suite, at any depth of nested test suites, by their path of names
	private static void collectTests(String path, AbstractStatement describe, Map<String, AbstractStatement> tests) {
		Map<String, AbstractStatement> nestedDescribeMap = nestedDescribeMap(describe);
		for(String key : nestedDescribeMap.keySet()) {
			collectTests(path + " > " + key, nestedDescribeMap.get(key), tests);
		}
		Map<String, AbstractStatement> nestedItMap = nestedItMap(describe);
		for(String key : nestedItMap.keySet()) {
			tests.put(path + " > " + key, nestedItMap.get(key));
		}
	}

	//the name of a test, i.e., the first argument of it('...', () => {...}), as in the maps of the tests of the suites
	private static String testName(AbstractStatement test) {
		return test.invocationCoveringEntireFragment().arguments().get(0);
	}

	//the elements (tests, functions, types) by their names, in their order, to find the names declared once on each side
	private static <T> Map<String, List<T>> groupByName(List<T> elements, Function<T, String> name) {
		Map<String, List<T>> map = new LinkedHashMap<>();
		for(T element : elements) {
			map.computeIfAbsent(name.apply(element), k -> new ArrayList<>()).add(element);
		}
		return map;
	}

	private static boolean callsAnyOperation(LambdaExpressionObject lambda, Set<String> operationNames) {
		for(AbstractCall call : lambda.getAllOperationInvocations()) {
			if(operationNames.contains(call.getName())) {
				return true;
			}
		}
		return false;
	}

	//the code of the callback of a test
	private static String callbackCode(AbstractStatement test) {
		return test.getLambdas().size() > 0 ? test.getLambdas().get(0).toString() : test.getString();
	}

	//the tokens of the code (identifiers, numbers, string literals), with their occurrences
	private static Map<String, Integer> tokens(String code) {
		Map<String, Integer> tokens = new HashMap<>();
		Matcher matcher = TOKEN.matcher(code);
		while(matcher.find()) {
			tokens.merge(matcher.group(), 1, Integer::sum);
		}
		return tokens;
	}

	private static double dice(Map<String, Integer> tokens1, Map<String, Integer> tokens2) {
		int size1 = 0, size2 = 0, common = 0;
		for(int count : tokens1.values()) size1 += count;
		for(int count : tokens2.values()) size2 += count;
		for(Map.Entry<String, Integer> entry : tokens1.entrySet()) {
			common += Math.min(entry.getValue(), tokens2.getOrDefault(entry.getKey(), 0));
		}
		return size1 + size2 == 0 ? 0 : 2.0 * common / (size1 + size2);
	}
}
