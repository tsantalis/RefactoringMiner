package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import java.util.*;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * What a write of a field changes for its readers beyond an assignment: an update of an atomic field, which keeps its
 * object but changes its value, and a write setting or clearing flags of a field holding a set of them, whose readers
 * masking other flags read nothing it changes.
 */
final class FieldWrites {
  private final RMIndex rmIndex;
  private final ReceiverTypeResolver receiverTypeResolver;
  private final MemberUsageFinder memberUsageFinder;
  private final PointsTo pointsTo;

  FieldWrites(RMIndex rmIndex, ReceiverTypeResolver receiverTypeResolver, MemberUsageFinder memberUsageFinder, PointsTo pointsTo) {
    this.rmIndex = rmIndex;
    this.receiverTypeResolver = receiverTypeResolver;
    this.memberUsageFinder = memberUsageFinder;
    this.pointsTo = pointsTo;
  }

  // The call the name of an atomic field is the receiver of when that call updates the value the field holds, as
  // `magazine.usedMemory.getAndAdd(capacity)` moving a chunk's capacity into its magazine's count: the field keeps the
  // same object, but the value its readers get (`magazine.usedMemory.get()`) is what the update leaves, as for an
  // assignment. Knowing java.util.concurrent.atomic is knowledge of the platform, as knowing the JDK
  @Nullable
  LocationInfo atomicUpdate(LeafExpression access, UMLAttribute attribute) {
    if (attribute.getType() == null || !PlatformKnowledge.ATOMIC_TYPES.contains(attribute.getType().getClassType())) {
      return null;
    }
    LocationInfo location = access.getLocationInfo();
    AbstractStatement statement = rmIndex.statementHolding(SrcDst.DST, location);
    if (statement == null) {
      return null;
    }
    List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
    for (AbstractCodeFragment fragment : fragments) {
      for (AbstractCall call : fragment.getMethodInvocations()) {
        if (call.getExpression() != null && call.getExpression().trim().equals(access.getString().trim())
                && call.getLocationInfo().getStartOffset() == location.getStartOffset() && PlatformKnowledge.ATOMIC_UPDATES.contains(call.getName())) {
          return call.getLocationInfo();
        }
      }
    }
    return null;
  }

  // The flags a write of a field holding a set of them sets, clears or toggles: a compound assignment of named constants,
  // as `ioState &= ~READ_SCHEDULED`, which keeps the bits of the field the constants leave alone. Its readers get only
  // those bits anew, so a reader masking the field with other flags (`(ioState & WRITE_SCHEDULED) != 0`) reads nothing
  // the write changes (see testsOtherFlags); none when the write assigns the field plainly, or with a value no constant
  // names. RM keeps no operator of an assignment, only its text: the assignment is compound when the assignment operator
  // of the language does not follow its target (see DataFlow#assignsPlainly), which an arithmetic compound assignment of
  // constants (`size += HEADER`) is too; the readers masking the field tell the flags apart
  @Nullable
  Set<UMLAttribute> flagsAssigned(LeafExpression access, UMLModel umlModel) {
    LocationInfo assignment = memberUsageFinder.assignmentOf(access);
    AbstractStatement statement = assignment == null ? null : rmIndex.statementHolding(SrcDst.DST, assignment);
    UMLAbstractClass context = rmIndex.ownerOf(SrcDst.DST, access.getLocationInfo());
    if (statement == null || context == null) {
      return null;
    }
    LeafExpression assigning = statement.getAssignments().stream()
            .filter(candidate -> candidate.getLocationInfo().equals(assignment)).findFirst().orElse(null);
    if (assigning == null || assigning.getLocationInfo().getStartOffset() != access.getLocationInfo().getStartOffset()
            || DataFlow.assignsPlainly(assigning, access.getString())) {
      return null;
    }
    Set<UMLAttribute> flags = constantsWithin(statement, assignment, access, context, umlModel);
    return flags == null || flags.isEmpty() ? null : flags;
  }

  // Whether the read of a field holding flags masks it with named constants (`ioState & (WRITE_SCHEDULED |
  // READ_SCHEDULED)`) none of which the changes set or clear: it reads only bits the changes leave as they were. A read
  // of the whole value (`ioState == 0`), or one masking it with a value no constant names, reads the changed bits too.
  // The mask is the innermost infix expression holding the read, whose operator RM records as the bitwise and
  boolean testsOtherFlags(LeafExpression access, Set<UMLAttribute> flags, UMLModel umlModel) {
    LocationInfo location = access.getLocationInfo();
    AbstractStatement statement = rmIndex.statementHolding(SrcDst.DST, location);
    UMLAbstractClass context = rmIndex.ownerOf(SrcDst.DST, location);
    if (statement == null || context == null) {
      return false;
    }
    LeafExpression mask = null;
    for (LeafExpression infix : statement.getInfixExpressions()) {
      LocationInfo infixLocation = infix.getLocationInfo();
      if (infixLocation.subsumes(location) && !infixLocation.equals(location)
              && (mask == null || mask.getLocationInfo().subsumes(infixLocation))) {
        mask = infix;
      }
    }
    if (mask == null || !PlatformKnowledge.BITWISE_AND.equals(RMCode.operatorOf(statement, mask))) {
      return false;
    }
    Set<UMLAttribute> tested = constantsWithin(statement, mask.getLocationInfo(), access, context, umlModel);
    return tested != null && !tested.isEmpty() && tested.stream().noneMatch(flags::contains);
  }

  // The named constants (static final fields, enum constants) the code within the location reads besides the access, if
  // it reads nothing else: no call, creation, number literal or other name
  @Nullable
  private Set<UMLAttribute> constantsWithin(AbstractCodeFragment fragment, LocationInfo location, LeafExpression access,
                                            UMLAbstractClass context, UMLModel umlModel) {
    Predicate<LocationInfo> within = inner -> location.subsumes(inner);
    if (fragment.getMethodInvocations().stream().anyMatch(call -> within.test(call.getLocationInfo()))
            || fragment.getCreations().stream().anyMatch(creation -> within.test(creation.getLocationInfo()))
            || fragment.getNumberLiterals().stream().anyMatch(literal -> within.test(literal.getLocationInfo()))) {
      return null;
    }
    Set<UMLAttribute> constants = Collections.newSetFromMap(new IdentityHashMap<>());
    for (LeafExpression variable : fragment.getVariables()) {
      LocationInfo variableLocation = variable.getLocationInfo();
      if (!within.test(variableLocation) || variableLocation.equals(access.getLocationInfo())) {
        continue;
      }
      UMLAttribute constant = receiverTypeResolver.resolveAccessTarget(variable, context, umlModel);
      if (RMCode.isConstant(constant)) {
        constants.add(constant);
        continue;
      }
      // A parameter the member never reassigns holds what its callers pass (argument -> parameter): the constants they
      // all pass, as the flag each caller of schedulePollAdd hands it as its mask
      Set<UMLAttribute> passed = constant == null ? constantsPassed(variable, context, umlModel) : null;
      if (passed == null) {
        return null;
      }
      constants.addAll(passed);
    }
    return constants;
  }

  // The named constants the callers in the model pass for the parameter the name reads, when the member it lies in never
  // reassigns it and every caller passes one; none otherwise
  @Nullable
  private Set<UMLAttribute> constantsPassed(LeafExpression name, UMLAbstractClass context, UMLModel umlModel) {
    LocationInfo location = name.getLocationInfo();
    UMLOperation member = null;
    for (UMLOperation operation : context.getOperations()) {
      if (operation.getLocationInfo().subsumes(location) && (member == null || member.getLocationInfo().subsumes(operation.getLocationInfo()))) {
        member = operation;
      }
    }
    List<VariableDeclaration> parameters = member == null ? List.of() : member.getParameterDeclarationList();
    int index = -1;
    for (int i = 0; i < parameters.size(); i++) {
      if (parameters.get(i).getVariableName().equals(name.getString())) {
        index = i;
      }
    }
    VariableDeclaration parameter = index < 0 ? null : parameters.get(index);
    if (parameter == null || member.getBody() == null || member.getBody().getCompositeStatement().getAllStatements().stream()
            .anyMatch(statement -> DataFlow.writes(statement, parameter))) {
      return null;
    }
    UMLOperation operation = member;
    List<MemberUsageFinder.Call> calls = pointsTo.callsOf(operation, umlModel);
    if (calls.isEmpty()) {
      return null;
    }
    Set<UMLAttribute> constants = Collections.newSetFromMap(new IdentityHashMap<>());
    for (MemberUsageFinder.Call call : calls) {
      List<String> arguments = call.call().arguments();
      if (index >= arguments.size()) {
        return null;
      }
      String argument = arguments.get(index).trim();
      LocationInfo callLocation = call.call().getLocationInfo();
      LeafExpression passed = call.fragment().getVariables().stream()
              .filter(variable -> callLocation.subsumes(variable.getLocationInfo()) && variable.getString().trim().equals(argument))
              .findFirst().orElse(null);
      UMLAttribute constant = passed == null ? null : receiverTypeResolver.resolveAccessTarget(passed, call.context(), umlModel);
      if (!RMCode.isConstant(constant)) {
        return null;
      }
      constants.add(constant);
    }
    return constants;
  }
}
