package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;

import java.util.*;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;

/**
 * Finds the invocations of the operations and the accesses of the attributes of a model, binding each call and each
 * name the way the compiler does, through {@link ReceiverTypeResolver}. It replaces, for the hunk network,
 * {@code UMLModelDiff.findInvocationsInParentModel/ChildModel} and {@code UMLModelDiff.findFieldAccessesInParentModel/ChildModel}.
 *
 * Only the calls and names reaching the changes are bound, overlapping them outside the bodies of the lambdas and
 * anonymous classes within them (the others reach no change node), and the names a changed value is assigned to. Binding one attaches the
 * file declaring its target to the model diff when it is not there yet, so the model gets exactly the unchanged files
 * declaring what the changes use.
 *
 * <h2>Not represented by RM, so not found or not decided here</h2>
 * <ul>
 *   <li>Java names: the Visitor never records a lowercase chain {@code a.b.c} as a whole. When the chain is a call
 *   receiver or an argument ({@code a.b.c.m()}, {@code foo(a.b.c)}, {@code new X(a.b.c)}) it records only {@code a.b},
 *   so the access of {@code c} is not represented; elsewhere (e.g., an assignment) it records {@code c} as a bare simple
 *   name, which reads as an unqualified name. It records the member name of a field access whose qualifier is not {@code this}
 *   ({@code foo().x}, {@code arr[0].x}) and of a super field access ({@code super.x}) as a bare simple name, which reads
 *   as an unqualified name. A FIELD_ACCESS (or QUALIFIED_NAME) node with its qualifier is needed for every member
 *   access.</li>
 *   <li>Enum constant arguments are kept as strings ({@code UMLEnumConstant.getArguments}), with no calls or
 *   variables.</li>
 *   <li>Kotlin callable references do not record their receiver ({@code Foo::bar} and {@code ::bar} both have no
 *   expression), so they are not bound.</li>
 *   <li>Kotlin named arguments are kept only as argument text, with no parameter name, so arguments are matched to
 *   parameters by position.</li>
 *   <li>A class declared inside a method does not link to the method's locals it can use, so a simple name in it is
 *   not checked against those locals.</li>
 *   <li>The type of a cast and the operator of a postfix expression (Kotlin {@code x!!}) are recorded only as the
 *   expression's text, so they are read from that text; besides names, call receivers, arguments and literals (which RM
 *   keeps as strings), this is the only text read.</li>
 * </ul>
 * Members of supertypes outside the repository and the JDK are not visible either.
 *
 * <h2>Derivable from RM, not derived here yet</h2>
 * <ul>
 *   <li>The types of expressions are not recorded by RM, but follow from what it records; they are inferred here for
 *   names, calls, creations, casts, literals, field and array accesses, infix expressions (from the operator RM records
 *   with each), conditional expressions and implicitly typed lambda parameters (from the functional interface of the
 *   call taking the lambda). Other arguments (e.g., switch expressions, lambdas themselves) have an unknown type, which
 *   can leave a call between overloads undecided.</li>
 *   <li>The functional type a method or creation reference is converted to, which gives the arity of the referenced
 *   method, can be found the same way as for lambda parameters; it is not yet, so a reference is bound only when the
 *   receiver type has a single member method (constructor) with the name.</li>
 * </ul>
 * <p>
 * The usages of a model are indexed by {@link #bindChanges}, which attaches the files declaring them; it is called
 * before querying the model, and the model must not otherwise change afterwards.
 */
public class MemberUsageFinder {

  private final ReceiverTypeResolver resolver;
  private final BiPredicate<UMLModel, LocationInfo> inChange;
  private final Map<UMLModel, Index> indexes = new IdentityHashMap<>();
  private final Map<UMLModel, Index> useIndexes = new IdentityHashMap<>();
  // The bound names a value is assigned to, with the assignment
  private final Map<LocationInfo, LocationInfo> writes = new HashMap<>();

  /**
   * @param inChange whether code of a model reaches a change, which limits the calls and names bound
   */
  public MemberUsageFinder(ReceiverTypeResolver resolver, BiPredicate<UMLModel, LocationInfo> inChange) {
    this.resolver = resolver;
    this.inChange = inChange;
  }

  public List<AbstractCall> findInvocations(UMLOperation operation, UMLModel umlModel) {
    return indexes.get(umlModel).invocations().getOrDefault(operation, List.of());
  }

  public List<LeafExpression> findAccesses(UMLAttribute attribute, UMLModel umlModel) {
    return indexes.get(umlModel).accesses().getOrDefault(attribute, List.of());
  }

  /**
   * The names in anonymous classes referring to the local variable or parameter, which RM records as used in its scope
   * only by the statements of its container and of the anonymous classes these statements create directly.
   */
  public List<LeafExpression> findAnonymousClassAccesses(VariableDeclaration local, UMLModel umlModel) {
    return indexes.get(umlModel).anonymousClassAccesses().getOrDefault(local, List.of());
  }

  /**
   * Binds the calls and names of the changes in the model, attaching the files declaring their targets; called once
   * per model, before querying it or iterating over its classes, which this grows.
   */
  public void bindChanges(UMLModel umlModel) {
    indexes.computeIfAbsent(umlModel, this::buildIndex);
  }

  /**
   * Whether a value is assigned to the name, a bound access of an attribute: it is the target of an assignment RM
   * records in its code.
   */
  public boolean isWrite(LeafExpression access) {
    return writes.containsKey(access.getLocationInfo());
  }

  /**
   * The assignment of a value to the name, a bound access of an attribute {@link #isWrite} tells is written: the name
   * with the value assigned to it.
   */
  public LocationInfo assignmentOf(LeafExpression access) {
    return writes.get(access.getLocationInfo());
  }

  /**
   * The calls of the operation outside the changes, once {@link #bindUses} bound them.
   */
  public List<AbstractCall> findUnchangedInvocations(UMLOperation operation, UMLModel umlModel) {
    Index index = useIndexes.get(umlModel);
    return index == null ? List.of() : index.invocations().getOrDefault(operation, List.of());
  }

  /**
   * The names referring to the attribute outside the changes, once {@link #bindUses} bound them.
   */
  public List<LeafExpression> findUnchangedAccesses(UMLAttribute attribute, UMLModel umlModel) {
    Index index = useIndexes.get(umlModel);
    return index == null ? List.of() : index.accesses().getOrDefault(attribute, List.of());
  }

  /**
   * Binds the calls and names outside the changes that may refer to one of the given members, by their names, the way
   * {@link #bindChanges} binds the ones of the changes: the uses of changed members by unchanged code. Called once per
   * model, after {@link #bindChanges}.
   */
  public void bindUses(UMLModel umlModel, Collection<UMLOperation> operations, Collection<UMLAttribute> attributes) {
    Set<UMLOperation> targetOperations = Collections.newSetFromMap(new IdentityHashMap<>());
    targetOperations.addAll(operations);
    Set<UMLAttribute> targetAttributes = Collections.newSetFromMap(new IdentityHashMap<>());
    targetAttributes.addAll(attributes);
    Set<String> operationNames = new HashSet<>();
    operations.forEach(operation -> operationNames.add(operation.getName()));
    // RM names a constructor invocation by its keyword (super(...), this(...)), not by the class
    if (operations.stream().anyMatch(UMLOperation::isConstructor)) {
      operationNames.add("super");
      operationNames.add("this");
    }
    Set<String> attributeNames = new HashSet<>();
    attributes.forEach(attribute -> attributeNames.add(attribute.getName()));

    Index index = new Index(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>());
    Set<LocationInfo> seenCalls = new HashSet<>();
    Set<LocationInfo> seenVariables = new HashSet<>();
    Set<UMLAbstractClass> seenClasses = Collections.newSetFromMap(new IdentityHashMap<>());
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLAbstractClass> classes = new ArrayList<>(List.of(umlClass));
      classes.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass context : classes) {
        if (!seenClasses.add(context)) {
          continue;
        }
        for (AbstractCodeFragment fragment : fragmentsOf(context)) {
          for (AbstractCall call : callsOf(fragment)) {
            if (!operationNames.contains(simpleName(call.getName())) || !seenCalls.add(call.getLocationInfo())
                    || inChange.test(umlModel, call.getLocationInfo())) {
              continue;
            }
            UMLOperation target = resolver.resolveInvocationTarget(call, context, umlModel);
            if (target != null && targetOperations.contains(target)) {
              index.invocations().computeIfAbsent(target, key -> new ArrayList<>()).add(call);
            }
          }
          for (LeafExpression variable : accessCandidatesOf(fragment)) {
            if (!attributeNames.contains(simpleName(variable.getString())) || !seenVariables.add(variable.getLocationInfo())
                    || inChange.test(umlModel, variable.getLocationInfo())) {
              continue;
            }
            AbstractCodeFragment selector = switchSelectorOf(fragment);
            UMLAttribute target = selector != null && variable.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SIMPLE_NAME)
                    ? resolver.resolveCaseLabelTarget(variable, selector, context, umlModel)
                    : resolver.resolveAccessTarget(variable, context, umlModel);
            if (target != null && targetAttributes.contains(target)) {
              index.accesses().computeIfAbsent(target, key -> new ArrayList<>()).add(variable);
              LocationInfo assignment = assignmentOf(variable, fragment);
              if (assignment != null) {
                writes.put(variable.getLocationInfo(), assignment);
              }
            }
          }
        }
      }
    }
    useIndexes.put(umlModel, index);
  }

  // The assignment of the fragment the name is the target of, which starts with it
  @Nullable
  private static LocationInfo assignmentOf(LeafExpression variable, AbstractCodeFragment fragment) {
    LocationInfo location = variable.getLocationInfo();
    for (LeafExpression assignment : fragment.getAssignments()) {
      LocationInfo assigned = assignment.getLocationInfo();
      if (assigned.getStartOffset() == location.getStartOffset() && location.getEndOffset() < assigned.getEndOffset()) {
        return assigned;
      }
    }
    return null;
  }

  // The name a call or a name refers to a member by, without its qualifier or the type arguments of a created type
  private static String simpleName(String name) {
    int typeArguments = name.indexOf('<');
    String raw = typeArguments < 0 ? name : name.substring(0, typeArguments);
    return raw.substring(raw.lastIndexOf('.') + 1);
  }

  private Index buildIndex(UMLModel umlModel) {
    Index index = new Index(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>());
    Set<LocationInfo> seenCalls = new HashSet<>();
    Set<LocationInfo> seenVariables = new HashSet<>();
    Set<UMLAbstractClass> seenClasses = Collections.newSetFromMap(new IdentityHashMap<>());
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLAbstractClass> classes = new ArrayList<>(List.of(umlClass));
      classes.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass context : classes) {
        if (!seenClasses.add(context)) {
          continue;
        }
        for (AbstractCodeFragment fragment : fragmentsOf(context)) {
          for (AbstractCall call : callsOf(fragment)) {
            if (!seenCalls.add(call.getLocationInfo()) || !inChange.test(umlModel, call.getLocationInfo())) {
              continue;
            }
            UMLOperation target = resolver.resolveInvocationTarget(call, context, umlModel);
            if (target != null) {
              index.invocations().computeIfAbsent(target, key -> new ArrayList<>()).add(call);
            }
          }
          for (LeafExpression variable : accessCandidatesOf(fragment)) {
            // A name a changed value is assigned to reaches the change through its assignment
            LocationInfo assignment = assignmentOf(variable, fragment);
            if (!seenVariables.add(variable.getLocationInfo()) || !(inChange.test(umlModel, variable.getLocationInfo())
                    || assignment != null && inChange.test(umlModel, assignment))) {
              continue;
            }
            AbstractCodeFragment selector = switchSelectorOf(fragment);
            UMLAttribute target = selector != null && variable.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SIMPLE_NAME)
                    ? resolver.resolveCaseLabelTarget(variable, selector, context, umlModel)
                    : resolver.resolveAccessTarget(variable, context, umlModel);
            if (target != null) {
              index.accesses().computeIfAbsent(target, key -> new ArrayList<>()).add(variable);
              if (assignment != null) {
                writes.put(variable.getLocationInfo(), assignment);
              }
            }
            VariableDeclaration local = context instanceof UMLAnonymousClass
                    && variable.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SIMPLE_NAME)
                    ? resolver.resolveLocal(variable, context, umlModel) : null;
            if (local != null) {
              index.anonymousClassAccesses().computeIfAbsent(local, key -> new ArrayList<>()).add(variable);
            }
          }
        }
      }
    }
    return index;
  }

  /** The code of the members of a class (its anonymous classes have their own), with the code of their lambdas. */
  private static List<AbstractCodeFragment> fragmentsOf(UMLAbstractClass umlClass) {
    List<VariableDeclarationContainer> members = new ArrayList<>();
    members.addAll(umlClass.getOperations());
    members.addAll(umlClass.getInitializers());
    members.addAll(umlClass.getAttributes());
    members.addAll(umlClass.getEnumConstants());
    List<AbstractCodeFragment> fragments = new ArrayList<>();
    for (VariableDeclarationContainer member : members) {
      for (VariableDeclarationContainer container : ReceiverTypeResolver.containerAndLambdas(member)) {
        if (container instanceof UMLAttribute attribute) {
          // the code of an attribute is its initializer (its body is the one of a lambda initializing it)
          AbstractExpression initializer = attribute.getVariableDeclaration().getInitializer();
          if (initializer != null) {
            fragments.add(initializer);
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
    }
    return fragments;
  }

  /** The expression a switch selects on, when the fragment is one of its case labels. */
  private static AbstractCodeFragment switchSelectorOf(AbstractCodeFragment fragment) {
    if (!(fragment instanceof StatementObject statement)
            || !statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_CASE)
            || statement.getParent() == null || statement.getParent().getExpressions().isEmpty()) {
      return null;
    }
    return statement.getParent().getExpressions().get(0);
  }

  private static List<AbstractCall> callsOf(AbstractCodeFragment fragment) {
    List<AbstractCall> calls = new ArrayList<>(fragment.getMethodInvocations());
    calls.addAll(fragment.getCreations());
    return calls;
  }

  /**
   * The simple names, qualified names and field accesses of a fragment, leaving out a simple name recorded for the member
   * name ending a qualified name or a field access (which stands for the access).
   */
  private static List<LeafExpression> accessCandidatesOf(AbstractCodeFragment fragment) {
    List<LeafExpression> variables = fragment.getVariables();
    List<LeafExpression> candidates = new ArrayList<>();
    for (LeafExpression variable : variables) {
      LocationInfo location = variable.getLocationInfo();
      LocationInfo.CodeElementType kind = location.getCodeElementType();
      if (kind.equals(LocationInfo.CodeElementType.QUALIFIED_NAME) || kind.equals(LocationInfo.CodeElementType.FIELD_ACCESS)) {
        candidates.add(variable);
      } else if (kind.equals(LocationInfo.CodeElementType.SIMPLE_NAME) && variables.stream().noneMatch(other -> isMemberNameOf(location, other))) {
        candidates.add(variable);
      }
    }
    return candidates;
  }

  private static boolean isMemberNameOf(LocationInfo name, LeafExpression memberAccess) {
    LocationInfo location = memberAccess.getLocationInfo();
    LocationInfo.CodeElementType kind = location.getCodeElementType();
    return (kind.equals(LocationInfo.CodeElementType.QUALIFIED_NAME) || kind.equals(LocationInfo.CodeElementType.FIELD_ACCESS))
            && location.getStartOffset() < name.getStartOffset() && location.getEndOffset() == name.getEndOffset();
  }

  private record Index(Map<UMLOperation, List<AbstractCall>> invocations, Map<UMLAttribute, List<LeafExpression>> accesses,
                       Map<VariableDeclaration, List<LeafExpression>> anonymousClassAccesses) {
  }
}
