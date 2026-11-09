package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import java.util.*;
import javax.annotation.Nullable;

/**
 * Points-to (stage 4 of the TODO on data and control flow in HunkNetwork): the classes a value may be an instance of,
 * by the creation sites reaching it as RM gives the code, which decide the implementation a call on the value runs.
 */
final class PointsTo {
  private final RMIndex rmIndex;
  private final ReceiverTypeResolver receiverTypeResolver;
  private final MemberUsageFinder memberUsageFinder;

  PointsTo(RMIndex rmIndex, ReceiverTypeResolver receiverTypeResolver, MemberUsageFinder memberUsageFinder) {
    this.rmIndex = rmIndex;
    this.receiverTypeResolver = receiverTypeResolver;
    this.memberUsageFinder = memberUsageFinder;
  }

  // The value the call passes for the parameter at the index, as RM gives the code within the call: the outermost code
  // that is the whole of the argument, or the name it is
  @Nullable
  Value argumentValue(AbstractCall call, AbstractCodeFragment fragment, int index, UMLAbstractClass context) {
    List<String> arguments = call.arguments();
    if (index >= arguments.size()) {
      return null;
    }
    String argument = arguments.get(index).trim();
    LocationInfo callLocation = call.getLocationInfo();
    List<LeafExpression> leaves = new ArrayList<>(fragment.getCreations());
    leaves.addAll(fragment.getMethodInvocations());
    leaves.addAll(fragment.getThisExpressions());
    leaves.addAll(fragment.getVariables());
    leaves.addAll(fragment.getTernaryOperatorExpressions());
    LeafExpression passed = null;
    for (LeafExpression leaf : leaves) {
      LocationInfo location = leaf.getLocationInfo();
      if (leaf != call && callLocation.subsumes(location) && leaf.getString().trim().equals(argument)
              && (passed == null || location.getStartOffset() < passed.getLocationInfo().getStartOffset())) {
        passed = leaf;
      }
    }
    if (passed == null) {
      return null;
    }
    LocationInfo location = passed.getLocationInfo();
    return new Value(fragment, location.getStartOffset(), location.getEndOffset(), context, RMCode.memberOf(context, callLocation));
  }

  // The code a value is, where it lies: a range of offsets of a fragment, in a class and a member
  record Value(AbstractCodeFragment fragment, int start, int end, UMLAbstractClass context,
                       @Nullable VariableDeclarationContainer member, @Nullable String name) {
    Value(AbstractCodeFragment fragment, int start, int end, UMLAbstractClass context, @Nullable VariableDeclarationContainer member) {
      this(fragment, start, end, context, member, null);
    }
  }

  // The classes each value may be an instance of (see classesOf), by its file and range
  private final Map<String, Set<UMLAbstractClass>> valueClasses = new HashMap<>();

  // The calls of each operation in the model (see MemberUsageFinder#findCalls)
  private final Map<UMLOperation, List<MemberUsageFinder.Call>> operationCalls = new IdentityHashMap<>();

  // The classes the receiver of the call may be an instance of; none for a call on the instance itself
  Set<UMLAbstractClass> receiverClasses(AbstractCall call, UMLModel umlModel) {
    LocationInfo location = call.getLocationInfo();
    AbstractStatement statement = call.getExpression() == null ? null : rmIndex.statementHolding(SrcDst.DST, location);
    UMLAbstractClass context = rmIndex.ownerOf(SrcDst.DST, location);
    if (statement == null || context == null) {
      return Set.of();
    }
    List<AbstractCodeFragment> fragments = new ArrayList<>(List.of(statement));
    if (statement instanceof CompositeStatementObject composite) {
      fragments.addAll(composite.getExpressions());
    }
    for (AbstractCodeFragment fragment : fragments) {
      if (fragment.getLocationInfo().subsumes(location)) {
        Value receiver = receiverOf(call, fragment, context);
        return receiver == null ? Set.of() : classesOf(receiver, umlModel);
      }
    }
    return Set.of();
  }

  // The receiver of the call in the fragment, as RM gives it: a call, a creation or a name starting where the call does
  @Nullable
  private Value receiverOf(AbstractCall call, AbstractCodeFragment fragment, UMLAbstractClass context) {
    String receiver = call.getExpression();
    LocationInfo location = call.getLocationInfo();
    if (receiver == null || receiver.equals(context.getLANG().THIS)) {
      return null;
    }
    List<LeafExpression> leaves = new ArrayList<>(fragment.getMethodInvocations());
    leaves.addAll(fragment.getCreations());
    leaves.addAll(fragment.getVariables());
    for (LeafExpression leaf : leaves) {
      LocationInfo leafLocation = leaf.getLocationInfo();
      if (leaf != call && leafLocation.getStartOffset() == location.getStartOffset() && leafLocation.getEndOffset() < location.getEndOffset()
              && leaf.getString().trim().equals(receiver.trim())) {
        return new Value(fragment, leafLocation.getStartOffset(), leafLocation.getEndOffset(), context, RMCode.memberOf(context, location));
      }
    }
    // A receiver RM gives only as the text of the call (see RMIndex, not represented by RM): the range of the call holding
    // it, which classesOf reads by that text as the name of a local, a parameter or a field the model declares; a text
    // naming none of them (an array access, a qualified name) gives no class
    String name = receiver.trim();
    return name.isEmpty() ? null : new Value(fragment, location.getStartOffset(), -1, context, RMCode.memberOf(context, location), name);
  }

  // Points-to (stage 4 of the TODO on data and control flow): the classes the value may be an instance of, by the creation
  // sites reaching it, as RM gives the code: a creation is an instance of its class (or of the anonymous class it
  // declares), `this` of the class of the code, a local of what its reaching definitions give (a captured local of its
  // definitions in the body declaring it), a parameter of what the calls of its member pass for it, a field of its
  // initializer and the values assigned to it, a call of what the methods it may run return, each run on the classes of
  // its receiver, and a conditional expression of what its branches give. A value of no known class (a method reference,
  // a value of a library) adds none. A value reaching itself through a cycle adds nothing more
  Set<UMLAbstractClass> classesOf(Value value, UMLModel umlModel) {
    String key = value.fragment().getLocationInfo().getFilePath() + ":" + value.start() + ":" + value.end()
            + (value.name() == null ? "" : ":" + value.name());
    Set<UMLAbstractClass> known = valueClasses.get(key);
    if (known != null) {
      return known;
    }
    valueClasses.put(key, Set.of());
    Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
    if (value.name() != null) {
      classes.addAll(nameClasses(value.name(), null, value, umlModel));
    } else {
      for (int[] branch : RMCode.valueBranches(value.fragment(), new int[]{value.start(), value.end()})) {
        classes.addAll(branchClasses(value, branch, umlModel));
      }
    }
    valueClasses.put(key, classes);
    return classes;
  }

  // The classes of a branch of a value: of the outermost code it is, ending where the branch does
  private Set<UMLAbstractClass> branchClasses(Value value, int[] branch, UMLModel umlModel) {
    AbstractCodeFragment fragment = value.fragment();
    LeafExpression outermost = null;
    List<LeafExpression> leaves = new ArrayList<>(fragment.getCreations());
    leaves.addAll(fragment.getMethodInvocations());
    leaves.addAll(fragment.getThisExpressions());
    leaves.addAll(fragment.getVariables());
    for (LeafExpression leaf : leaves) {
      LocationInfo location = leaf.getLocationInfo();
      if (location.getEndOffset() == branch[1] && branch[0] <= location.getStartOffset()
              && (outermost == null || location.getStartOffset() < outermost.getLocationInfo().getStartOffset())) {
        outermost = leaf;
      }
    }
    if (outermost == null) {
      return Set.of();
    }
    if (fragment.getCreations().contains(outermost)) {
      UMLAbstractClass created = createdClass((AbstractCall) outermost, value.context(), umlModel);
      return created == null ? Set.of() : Set.of(created);
    }
    if (outermost instanceof AbstractCall call) {
      return callClasses(call, value, umlModel);
    }
    if (fragment.getThisExpressions().contains(outermost)) {
      return Set.of(value.context());
    }
    return nameClasses(outermost.getString().trim(), outermost, value, umlModel);
  }

  // The classes of what the methods a call may run return: the method it binds to, or the implementations the classes of
  // its receiver run
  private Set<UMLAbstractClass> callClasses(AbstractCall call, Value value, UMLModel umlModel) {
    UMLOperation target = receiverTypeResolver.resolveInvocationTarget(call, value.context(), umlModel);
    if (target == null) {
      return Set.of();
    }
    Value receiver = receiverOf(call, value.fragment(), value.context());
    Set<UMLAbstractClass> runtimes = receiver == null ? Set.of() : classesOf(receiver, umlModel);
    Set<UMLOperation> runs = Collections.newSetFromMap(new IdentityHashMap<>());
    for (UMLAbstractClass runtime : runtimes) {
      UMLOperation implementation = receiverTypeResolver.findAttachedDispatchTarget(target, runtime, umlModel);
      runs.add(implementation != null ? implementation : target);
    }
    if (runs.isEmpty()) {
      runs.add(target);
    }
    Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
    for (UMLOperation operation : runs) {
      classes.addAll(returnedClasses(operation, umlModel));
    }
    return classes;
  }

  // The classes of what the operation returns, by the values of its return statements
  private Set<UMLAbstractClass> returnedClasses(UMLOperation operation, UMLModel umlModel) {
    UMLAbstractClass declaring = rmIndex.ownerOf(SrcDst.DST, operation.getLocationInfo());
    if (operation.getBody() == null || declaring == null) {
      return Set.of();
    }
    Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AbstractStatement statement : operation.getBody().getCompositeStatement().getAllStatements()) {
      // The value returned, as RM gives the code: the outermost code that is the whole of it
      List<LeafExpression> leaves = new ArrayList<>(statement.getCreations());
      leaves.addAll(statement.getMethodInvocations());
      leaves.addAll(statement.getThisExpressions());
      leaves.addAll(statement.getVariables());
      leaves.addAll(statement.getTernaryOperatorExpressions());
      for (LeafExpression leaf : leaves) {
        if (RMCode.returnsWhole(statement, leaf)) {
          LocationInfo location = leaf.getLocationInfo();
          classes.addAll(classesOf(new Value(statement, location.getStartOffset(), location.getEndOffset(), declaring, operation), umlModel));
          break;
        }
      }
    }
    return classes;
  }

  // The classes of what a name holds: a local, a parameter or a field (by the name RM gives, or, for a name RM gives only
  // as the text of a call, a field of a class enclosing the code)
  private Set<UMLAbstractClass> nameClasses(String text, @Nullable LeafExpression name, Value value, UMLModel umlModel) {
    AbstractCodeFragment fragment = value.fragment();
    VariableDeclarationContainer member = value.member();
    if (!text.contains(".")) {
      VariableDeclaration local = fragment.searchVariableDeclaration(text);
      List<VariableDeclaration> parameters = member == null ? List.of() : member.getParameterDeclarationList();
      for (int i = 0; i < parameters.size(); i++) {
        if (parameters.get(i).getVariableName().equals(text) && (local == null || local == parameters.get(i))) {
          return parameterClasses(member, i, umlModel);
        }
      }
      if (local != null) {
        AbstractStatement reader = fragment instanceof AbstractStatement statement ? statement
                : rmIndex.statementHolding(SrcDst.DST, fragment.getLocationInfo());
        Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractStatement definition : reader == null ? List.<AbstractStatement>of() : rmIndex.definitionsReaching(SrcDst.DST, reader, local)) {
          int[] defined = RMCode.valueOf(definition, local);
          UMLAbstractClass context = rmIndex.ownerOf(SrcDst.DST, definition.getLocationInfo());
          if (defined != null && context != null) {
            classes.addAll(classesOf(new Value(definition, defined[0], defined[1], context,
                    RMCode.memberOf(context, definition.getLocationInfo())), umlModel));
          }
        }
        return classes;
      }
    }
    UMLAttribute field = name != null ? receiverTypeResolver.resolveAccessTarget(name, value.context(), umlModel)
            : RMCode.enclosingField(text, fragment.getLocationInfo(), umlModel);
    return field == null ? Set.of() : fieldClasses(field, umlModel);
  }

  // The classes of what a field holds: its initializer and the values assigned to it in the model
  private Set<UMLAttribute> computingFields = Collections.newSetFromMap(new IdentityHashMap<>());

  Set<UMLAbstractClass> fieldClasses(UMLAttribute field, UMLModel umlModel) {
    if (!computingFields.add(field)) {
      return Set.of();
    }
    try {
      Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
      UMLAbstractClass declaring = rmIndex.ownerOf(SrcDst.DST, field.getLocationInfo());
      AbstractExpression initializer = field.getVariableDeclaration().getInitializer();
      if (initializer != null && declaring != null) {
        LocationInfo location = initializer.getLocationInfo();
        classes.addAll(classesOf(new Value(initializer, location.getStartOffset(), location.getEndOffset(), declaring, field), umlModel));
      }
      for (MemberUsageFinder.Write write : memberUsageFinder.findWrites(umlModel, List.of(field)).getOrDefault(field, List.of())) {
        classes.addAll(classesOf(new Value(write.fragment(), write.name().getLocationInfo().getEndOffset(), write.assignment().getEndOffset(),
                write.context(), RMCode.memberOf(write.context(), write.assignment())), umlModel));
      }
      return classes;
    } finally {
      computingFields.remove(field);
    }
  }

  // The classes of what the calls of the member pass for its parameter at the index (argument -> parameter)
  private Set<UMLAbstractClass> parameterClasses(VariableDeclarationContainer member, int parameter, UMLModel umlModel) {
    if (!(member instanceof UMLOperation operation)) {
      return Set.of();
    }
    List<MemberUsageFinder.Call> calls = callsOf(operation, umlModel);
    Set<UMLAbstractClass> classes = Collections.newSetFromMap(new IdentityHashMap<>());
    for (MemberUsageFinder.Call call : calls) {
      List<String> arguments = call.call().arguments();
      if (parameter >= arguments.size()) {
        continue;
      }
      Value argument = argumentValue(call.call(), call.fragment(), parameter, call.context());
      if (argument != null) {
        classes.addAll(classesOf(argument, umlModel));
      }
    }
    return classes;
  }

  // The class the creation makes an instance of: the anonymous class it declares, or the class of the constructor it
  // binds to
  @Nullable
  private UMLAbstractClass createdClass(AbstractCall creation, UMLAbstractClass context, UMLModel umlModel) {
    LocationInfo location = creation.getLocationInfo();
    if (creation instanceof ObjectCreation objectCreation && objectCreation.getAnonymousClassDeclaration() != null) {
      // The outermost anonymous class within the creation, the others lying in its body
      UMLAnonymousClass declared = null;
      for (UMLClass umlClass : umlModel.getClassList()) {
        for (UMLAnonymousClass anonymous : umlClass.getAnonymousClassList()) {
          LocationInfo anonymousLocation = anonymous.getLocationInfo();
          if (anonymousLocation.getFilePath().equals(location.getFilePath()) && location.subsumes(anonymousLocation)
                  && (declared == null || anonymousLocation.subsumes(declared.getLocationInfo()))) {
            declared = anonymous;
          }
        }
      }
      return declared;
    }
    UMLOperation constructor = receiverTypeResolver.bindInvocationTarget(creation, context, umlModel);
    return constructor == null ? null : rmIndex.ownerOf(SrcDst.DST, constructor.getLocationInfo());
  }

  // The calls of the operation in the model (see MemberUsageFinder#findCalls)
  List<MemberUsageFinder.Call> callsOf(UMLOperation operation, UMLModel umlModel) {
    return operationCalls.computeIfAbsent(operation, key -> memberUsageFinder.findCalls(umlModel, List.of(key)).getOrDefault(key, List.of()));
  }
}
