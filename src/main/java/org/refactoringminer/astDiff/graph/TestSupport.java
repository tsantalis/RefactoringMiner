package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;
import java.util.*;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;

/**
 * What test code tells a reviewer: whether a use of test support tells more of the test than its name (a use deciding
 * which code of the test runs, running the test's own checks, giving the test an object of the code under test), and
 * the test helpers reading by their names, as the ones drawing at random and the steps of fluent APIs.
 */
final class TestSupport {
  private final UMLModelDiff modelDiff;
  private final RMIndex rmIndex;
  private final ReceiverTypeResolver receiverTypeResolver;
  // The files the change touches, in either revision
  private final Set<String> changedPaths;

  TestSupport(UMLModelDiff modelDiff, RMIndex rmIndex, ReceiverTypeResolver receiverTypeResolver, Set<String> changedPaths) {
    this.modelDiff = modelDiff;
    this.rmIndex = rmIndex;
    this.receiverTypeResolver = receiverTypeResolver;
    this.changedPaths = changedPaths;
  }

  // Whether test support gives the test an object of the code under test, by operation (see givesCodeUnderTest)
  private final Map<UMLOperation, Boolean> givesCodeUnderTest = new IdentityHashMap<>();

  // Whether an operation is a step of a fluent API (see isFluentStep)
  private final Map<UMLOperation, Boolean> fluentSteps = new IdentityHashMap<>();

  // Whether the call is a step of test code setting up what it exercises, read by its name: a call of test code that tells
  // nothing more of the test (see tellsOfTest), made by the test itself, not within code it hands to other code (a lambda,
  // a method of an anonymous class), which runs when the code under test decides, as the consumer a changed
  // defaultRequest applies to every request. A fluent step such a call runs (writerIndex(4) setting the readable bytes of
  // a buffer) keeps a value and returns the receiver, which its name tells, as the fluent steps of test code with a body
  // (see isFluentStep and processExtensions). A step of a class the change edits is no such input: it keeps state the
  // changed code of its class works on, as the headers and the uri a request spec keeps, which its changed execution
  // completes with the default request (see HunkNetwork#holdsChanges)
  boolean setsUpTest(AbstractCall call, UMLOperation operation, UMLAbstractClass context, SrcDst srcDst) {
    LocationInfo location = call.getLocationInfo();
    return TraversalPattern.isTest(location.getFilePath()) && !(context instanceof UMLAnonymousClass)
            && operation.getAllLambdas().stream().noneMatch(lambda -> lambda.getLocationInfo().subsumes(location))
            && !tellsOfTest(location, call, null, srcDst);
  }

  // Whether the declaration is test support the change does not touch, as the assertions, random values and fixtures of a
  // test framework (see tellsOfTest)
  boolean isTestSupport(String declaringPath) {
    return TraversalPattern.isTest(declaringPath) && !changedPaths.contains(declaringPath);
  }

  // Whether a use of test support tells a reviewer of the test more than its name: a use by production code, one deciding
  // which code of the test runs (a condition reading it, as a check whether the test can configure what it exercises),
  // or a call running code of the test it is given as the test's own checks (see runsTestCode); a call giving the test an
  // object of the code under test it keeps is another (see givesCodeUnderTest). Other uses by test code, as assertions,
  // random values and builders, read by their names, and their code tells nothing of the change
  boolean tellsOfTest(LocationInfo use, @Nullable AbstractCall call, @Nullable UMLOperation target, SrcDst srcDst) {
    String path = use.getFilePath();
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    return !TraversalPattern.isTest(path)
            || rmIndex.isInCondition(srcDst, path, use) && !(target != null && drawsAtRandom(target, srcDst, umlModel))
            || call != null && runsTestCode(srcDst, path, call, target);
  }

  private final Map<UMLOperation, Boolean> randomDraws = new IdentityHashMap<>();

  // Whether the operation gives a value drawn at random: its body draws from the random source of the test framework
  // (the random() a test inherits from RandomizedTest or LuceneTestCase, a generator of randomized testing,
  // ThreadLocalRandom, Math.random()), directly or through methods of the model drawing so, as between through
  // randomIntBetween. A condition of a test reading such a value only picks a variant of the test at random (`if
  // (randomBoolean())`, `switch (between(0, 6))`, a loop over a random number of pages); the helper's code tells nothing
  // beyond its name, while a value of the code or the build a condition reads (canUseQueryPragmas, reading the build
  // type) decides whether the test exercises what the change is about. Knowing the randomized testing frameworks is
  // knowledge of the platform, as knowing the JDK
  boolean drawsAtRandom(UMLOperation operation, SrcDst srcDst, UMLModel umlModel) {
    Boolean known = randomDraws.get(operation);
    if (known != null) {
      return known;
    }
    // A method drawing through itself tells nothing more until it is decided
    randomDraws.put(operation, false);
    boolean draws = false;
    UMLAbstractClass declaring = rmIndex.ownerOf(srcDst, operation.getLocationInfo());
    if (declaring != null && operation.getBody() != null) {
      for (AbstractCall call : operation.getAllOperationInvocations()) {
        String receiver = call.getExpression();
        if (receiver == null && call.getName().equals("random") && call.arguments().isEmpty()
                && receiverTypeResolver.bindInvocationTarget(call, declaring, umlModel) == null
                && inheritsRandomSource(declaring, umlModel)
                || receiver != null && call.getName().equals("random") && receiver.equals("Math")
                || receiver != null && PlatformKnowledge.isRandomDrawingType(RMCode.importedName(declaring, receiver, umlModel))) {
          draws = true;
          break;
        }
        UMLOperation target = receiverTypeResolver.bindInvocationTarget(call, declaring, umlModel);
        if (target != null && target != operation && drawsAtRandom(target, srcDst, umlModel)) {
          draws = true;
          break;
        }
      }
    }
    randomDraws.put(operation, draws);
    return draws;
  }

  // Whether the class inherits the random source of a randomized testing framework: its chain of superclasses in the model
  // ends at one of those frameworks' test bases, by the import naming it
  private boolean inheritsRandomSource(UMLAbstractClass declaring, UMLModel umlModel) {
    Set<UMLAbstractClass> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    UMLAbstractClass current = declaring;
    while (current instanceof UMLClass umlClass && visited.add(umlClass) && umlClass.getSuperclass() != null) {
      UMLType superclass = umlClass.getSuperclass();
      String qualified = RMCode.importedName(umlClass, superclass.getClassType(), umlModel);
      if (qualified != null && PlatformKnowledge.RANDOM_TEST_BASES.contains(qualified)) {
        return true;
      }
      Set<String> paths = receiverTypeResolver.resolvePaths(new ReceiverTypeResolver.ReceiverType(superclass, umlClass));
      String simple = superclass.getClassType().substring(superclass.getClassType().lastIndexOf('.') + 1);
      current = umlModel.getClassList().stream().filter(candidate -> paths.contains(candidate.getLocationInfo().getFilePath())
              && candidate.getNonQualifiedName().equals(simple)).findFirst().orElse(null);
    }
    return false;
  }

  // Whether the call runs code the test gives it (a lambda or a reference, see RMIndex#takesCode) as checks of the test:
  // the method it binds gives nothing back, so the code runs for its own effect within the call, as the assertions a
  // retrying assertion (assertBusy) runs until they hold, which is how the test checks the change. A method giving a value
  // (a constructor gives the object it creates) takes the code as a part of that value: the entries a random supplier
  // fills a list or a map with, the consumer a listener wraps, the builder steps of a mapping. That code is the test's
  // input, which the test itself shows; the helper's body only tells how it assembles the value, which its name says. An
  // agent reading the test then sees the checks it relies on and is not handed the helper code assembling its input
  boolean runsTestCode(SrcDst srcDst, String path, AbstractCall call, @Nullable UMLOperation target) {
    if (!rmIndex.takesCode(srcDst, path, call)) {
      return false;
    }
    if (target == null) {
      return true;
    }
    if (target.isConstructor()) {
      return false;
    }
    UMLParameter returned = target.getReturnParameter();
    return returned == null || returned.getType() == null || target.isVoid();
  }

  // Whether the operation returns an object of the code under test: its return type is declared by code of the repository
  // that is no test code. Test support giving the test such an object, which the test keeps in a local, hands it what it
  // exercises or inspects, as the pragmas a query runs with or the response of the query; one giving strings, numbers or
  // fixtures of the tests (random values, test clusters) tells nothing beyond its name
  // Among such helpers only the ones whose value depends on a check of the build or the platform tell more than their
  // name (see decidesOnEnvironment): randomPragmas sets the concurrency of the query only when the build allows pragmas,
  // which decides whether the test exercises the concurrent code the fix is about; a fixture building the object from its
  // arguments (a driver context, a parser of given bytes, a retrier, a mapper service, a random source) reads by its name
  boolean givesCodeUnderTest(UMLOperation operation, UMLAbstractClass declaring) {
    return givesCodeUnderTest.computeIfAbsent(operation, key -> returnsCodeUnderTest(operation, declaring)
            && decidesOnEnvironment(operation, declaring, modelDiff.getChildModel(), Collections.newSetFromMap(new IdentityHashMap<>())));
  }

  // Whether the value the test support gives depends on a check of what the test runs on: a condition of its body (an if,
  // a switch, a conditional expression) calling a method that draws nothing at random (see drawsAtRandom), as
  // canUseQueryPragmas() telling a snapshot build, or such a condition of test support whose result its returns use, as
  // run(String) running the query with randomPragmas()
  private boolean decidesOnEnvironment(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel, Set<UMLOperation> visited) {
    if (!visited.add(operation) || operation.getBody() == null) {
      return false;
    }
    SrcDst srcDst = umlModel == modelDiff.getParentModel() ? SrcDst.SRC : SrcDst.DST;
    for (AbstractStatement statement : operation.getBody().getCompositeStatement().getAllStatements()) {
      List<AbstractCodeFragment> conditions = new ArrayList<>();
      if (statement instanceof CompositeStatementObject composite && ControlFlow.isConditional(composite)) {
        conditions.addAll(composite.getExpressions());
      }
      statement.getTernaryOperatorExpressions().forEach(ternary -> conditions.add(ternary.getCondition()));
      for (AbstractCodeFragment condition : conditions) {
        for (AbstractCall call : condition.getMethodInvocations()) {
          UMLOperation bound = receiverTypeResolver.bindInvocationTarget(call, declaring, umlModel);
          if (bound != null && !drawsAtRandom(bound, srcDst, umlModel)) {
            return true;
          }
        }
      }
      if (statement.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.RETURN_STATEMENT) {
        for (AbstractCall call : statement.getMethodInvocations()) {
          UMLOperation bound = receiverTypeResolver.bindInvocationTarget(call, declaring, umlModel);
          UMLAbstractClass boundClass = bound == null || !isTestSupport(bound.getLocationInfo().getFilePath()) ? null
                  : rmIndex.ownerOf(srcDst, bound.getLocationInfo());
          if (boundClass != null && decidesOnEnvironment(bound, boundClass, umlModel, visited)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean returnsCodeUnderTest(UMLOperation operation, UMLAbstractClass declaring) {
    UMLParameter returned = operation.getReturnParameter();
    if (returned == null || returned.getType() == null) {
      return false;
    }
    Set<String> paths = receiverTypeResolver.resolvePaths(new ReceiverTypeResolver.ReceiverType(returned.getType(), declaring));
    return !paths.isEmpty() && paths.stream().noneMatch(TraversalPattern::isTest);
  }

  // Whether the operation is a step of a fluent API, as a setter of a builder: it returns the instance itself on every
  // path, or the result of a step of its class it calls on itself (an overload converting its argument). Test code
  // building its input through such steps reads by their names, and their code (keeping a value, returning the builder)
  // tells nothing of the change; a use of them telling of the test does (see tellsOfTest)
  boolean isFluentStep(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel) {
    Boolean known = fluentSteps.get(operation);
    if (known != null) {
      return known;
    }
    // A step calling itself back tells nothing of its returns until it is decided
    fluentSteps.put(operation, false);
    boolean fluent = returnsItself(operation, declaring, umlModel);
    fluentSteps.put(operation, fluent);
    return fluent;
  }

  private boolean returnsItself(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel) {
    if (operation.getBody() == null || operation.isConstructor()) {
      return false;
    }
    boolean returns = false;
    for (AbstractStatement statement : operation.getBody().getCompositeStatement().getAllStatements()) {
      if (!statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
        continue;
      }
      returns = true;
      if (statement.getString().equals(statement.getLANG().RETURN_THIS)) {
        continue;
      }
      AbstractCall step = RMCode.returnedCall(statement);
      if (step == null || !isStepOnItself(step, statement, declaring, umlModel)) {
        return false;
      }
    }
    return returns;
  }

  // Whether the call is a fluent step of the class called on the instance itself, directly or on the result of such a
  // step (a chain of steps, as field(name).startObject())
  private boolean isStepOnItself(AbstractCall call, AbstractStatement statement, UMLAbstractClass declaring, UMLModel umlModel) {
    String receiver = call.getExpression();
    if (!call.expressionIsNullOrThis()) {
      AbstractCall previous = statement.getMethodInvocations().stream().filter(other -> other.getString().equals(receiver))
              .findFirst().orElse(null);
      if (previous == null || !isStepOnItself(previous, statement, declaring, umlModel)) {
        return false;
      }
    }
    UMLOperation target = receiverTypeResolver.bindInvocationTarget(call, declaring, umlModel);
    return target != null && declaring.getOperations().contains(target) && isFluentStep(target, declaring, umlModel);
  }
}
