package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import java.util.*;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.util.PathFileUtils;

/**
 * Checks of the code RM records, made as RM makes them: by the code elements and the locations RM gives, and by the
 * constants of the language of the code (a statement only returning a value, a call covering a statement, an argument
 * of a call, the locals a statement keeps a value in).
 */
final class RMCode {
  private RMCode() {
  }

  // The local the write assigns as it is, the whole of its value; none for a parameter, which its callers give (see
  // relaysParameters), or for a value computed from it. The statement is the assignment alone, as RM matches a setter
  // with the constants of the language (UMLOperation#isSetter)
  @Nullable
  static VariableDeclaration assignedLocal(MemberUsageFinder.Write write, VariableDeclarationContainer member) {
    AbstractCodeFragment fragment = write.fragment();
    int start = write.name().getLocationInfo().getEndOffset();
    int end = write.assignment().getEndOffset();
    List<LeafExpression> values = fragment.getVariables().stream().filter(variable -> start <= variable.getLocationInfo().getStartOffset()
            && variable.getLocationInfo().getEndOffset() <= end).toList();
    gr.uom.java.xmi.Constants lang = fragment.getLANG();
    if (values.size() != 1 || !fragment.getString().equals(write.name().getString() + lang.ASSIGNMENT + values.get(0).getString()
            + lang.STATEMENT_TERMINATION)) {
      return null;
    }
    VariableDeclaration local = fragment.searchVariableDeclaration(values.get(0).getString().trim());
    return local == null || member.getParameterDeclarationList().contains(local) ? null : local;
  }

  // Whether the name RM gives in the code may name the field, by what RM records: the field's name itself, the field on
  // the object itself (as RM relates a name to an attribute, with the reference of the language to the object), or a
  // name ending where RM records the field's name of its own in the code (the member name ending a qualified name, see
  // MemberUsageFinder#isMemberNameOf). Only such a name is bound: binding a name attaches the files of the types it goes
  // through
  static boolean namesField(LeafExpression name, AbstractCodeFragment code, String field) {
    String text = name.getString();
    if (text.equals(field) || text.equals(name.getLANG().THIS_DOT + field)) {
      return true;
    }
    int end = name.getLocationInfo().getEndOffset();
    return code.getVariables().stream().anyMatch(other -> other.getString().equals(field) && other.getLocationInfo().getEndOffset() == end);
  }

  // The code of the statement itself: a leaf statement, or the header of a composite statement
  static List<AbstractCodeFragment> ownCode(AbstractStatement statement) {
    return statement instanceof CompositeStatementObject composite ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
  }

  // The call made on the value of the code at the location, as the receiver it starts with (`mode.getDefaultMapping()`
  // on `mode`, `getMode().getDefaultMapping()` on `getMode()`); RM gives a receiver as the text of the call only
  @Nullable
  static AbstractCall callOn(AbstractStatement statement, LocationInfo read, String code) {
    String thisDot = PathFileUtils.getLang(read.getFilePath()).THIS_DOT;
    for (AbstractCall call : statement.getMethodInvocations()) {
      String receiver = call.getExpression();
      if (receiver != null && call.getLocationInfo().getStartOffset() <= read.getStartOffset()
              && (receiver.equals(code) || receiver.equals(thisDot + code) || (thisDot + receiver).equals(code))
              && call.getLocationInfo().subsumes(read)) {
        return call;
      }
    }
    return null;
  }

  // Whether the statement only returns the code, the whole of the value it returns, as RM matches such a statement with
  // the constants of the language (UMLOperation#isGetter, AbstractCodeFragment#invocationCoveringEntireFragment)
  static boolean returnsWhole(AbstractCodeFragment statement, LeafExpression code) {
    gr.uom.java.xmi.Constants lang = statement.getLANG();
    return statement.getString().equals(lang.RETURN_SPACE + code.getString() + lang.STATEMENT_TERMINATION);
  }

  // The call the statement only returns the result of, as RM finds the call covering a statement it returns
  @Nullable
  static AbstractCall returnedCall(AbstractCodeFragment statement) {
    AbstractCall call = statement.invocationCoveringEntireFragment();
    return call != null && call.getCoverage() == AbstractCall.StatementCoverageType.RETURN_CALL ? call : null;
  }

  // The member of the class holding the code at the location: an operation, an initializer, or a field it initializes
  @Nullable
  static VariableDeclarationContainer memberOf(UMLAbstractClass context, LocationInfo location) {
    List<VariableDeclarationContainer> members = new ArrayList<>(context.getOperations());
    if (context instanceof UMLClass umlClass) {
      members.addAll(umlClass.getInitializers());
    }
    members.addAll(context.getAttributes());
    for (VariableDeclarationContainer member : members) {
      if (member.getLocationInfo().subsumes(location)) {
        return member;
      }
    }
    return null;
  }

  // The operator of the infix expression of the code: RM adds each infix expression and its operator in the same step, so
  // they share their index (as ReceiverTypeResolver pairs them); none when RM records no operator for some of them
  @Nullable
  static String operatorOf(AbstractCodeFragment fragment, LeafExpression infix) {
    List<LeafExpression> infixExpressions = fragment.getInfixExpressions();
    List<String> operators = fragment.getInfixOperators();
    if (infixExpressions.size() != operators.size()) {
      return null;
    }
    for (int i = 0; i < infixExpressions.size(); i++) {
      if (infixExpressions.get(i) == infix) {
        return operators.get(i);
      }
    }
    return null;
  }

  static boolean isConstant(@Nullable UMLAttribute attribute) {
    return attribute instanceof UMLEnumConstant || attribute != null && attribute.isStatic() && attribute.isFinal();
  }

  // The field of the name in the innermost class of the model enclosing the code that declares one (lexical scope)
  @Nullable
  static UMLAttribute enclosingField(String name, LocationInfo location, UMLModel umlModel) {
    UMLAttribute found = null;
    LocationInfo foundClass = null;
    for (UMLClass umlClass : umlModel.getClassList()) {
      LocationInfo classLocation = umlClass.getLocationInfo();
      if (!classLocation.getFilePath().equals(location.getFilePath()) || !classLocation.subsumes(location)
              || foundClass != null && classLocation.subsumes(foundClass)) {
        continue;
      }
      for (UMLAttribute attribute : umlClass.getAttributes()) {
        if (attribute.getName().equals(name)) {
          found = attribute;
          foundClass = classLocation;
        }
      }
    }
    return found;
  }

  // The one name or call of the fragment written as the text
  @Nullable
  static LeafExpression expressionOf(AbstractCodeFragment fragment, String text) {
    List<LeafExpression> candidates = new ArrayList<>(fragment.getVariables());
    candidates.addAll(fragment.getMethodInvocations());
    List<LeafExpression> matching = candidates.stream().filter(candidate -> candidate.getString().trim().equals(text.trim())).toList();
    return matching.size() == 1 ? matching.get(0) : null;
  }

  // Whether the call takes the code at the location as one of its arguments, as RM records the arguments of the code with
  // their locations: an argument spans the location, within the call and within no other call or creation in it
  private static boolean takesAsArgument(AbstractCodeFragment fragment, AbstractCall call, LocationInfo location) {
    LocationInfo callLocation = call.getLocationInfo();
    if (!callLocation.subsumes(location) || fragment.getArguments().stream()
            .noneMatch(argument -> argument.getLocationInfo().subsumes(location) && location.subsumes(argument.getLocationInfo()))) {
      return false;
    }
    List<AbstractCall> calls = new ArrayList<>(fragment.getMethodInvocations());
    calls.addAll(fragment.getCreations());
    return calls.stream().noneMatch(other -> other != call && callLocation.subsumes(other.getLocationInfo())
            && !other.getLocationInfo().subsumes(callLocation) && other.getLocationInfo().subsumes(location));
  }

  // The reads of the value a local holding a CompletionStage completes with, by a lambda the code passes to a method of
  // the stage called on the local: the reads of the lambda's first parameter in its body, as the `restNum > 0` of
  // `getMessageFuture.thenApply(restNum -> ...)` deciding whether a popped message wakes the waiting consumers. Knowing
  // CompletionStage is knowledge of the platform, as knowing the JDK
  static List<LocationInfo> stageValueReads(AbstractCodeFragment user, VariableDeclaration local) {
    if (local.getType() == null || !PlatformKnowledge.STAGE_TYPES.contains(local.getType().getClassType())) {
      return List.of();
    }
    List<LocationInfo> reads = new ArrayList<>();
    for (AbstractCall call : user.getMethodInvocations()) {
      if (call.getExpression() == null || !call.getExpression().trim().equals(local.getVariableName())
              || !PlatformKnowledge.STAGE_VALUE_METHODS.contains(call.getName())) {
        continue;
      }
      for (LambdaExpressionObject lambda : user.getLambdas()) {
        if (lambda.getParameters().isEmpty() || !takesAsArgument(user, call, lambda.getLocationInfo())) {
          continue;
        }
        VariableDeclaration value = lambda.getParameters().get(0);
        List<AbstractCodeFragment> body = new ArrayList<>();
        if (lambda.getBody() != null) {
          for (AbstractStatement statement : lambda.getBody().getCompositeStatement().getAllStatements()) {
            if (statement instanceof CompositeStatementObject composite) {
              body.addAll(composite.getExpressions());
            } else {
              body.add(statement);
            }
          }
        } else if (lambda.getExpression() != null) {
          body.add(lambda.getExpression());
        }
        for (AbstractCodeFragment fragment : body) {
          reads.addAll(DataFlow.readsAt(fragment, value));
        }
      }
    }
    return reads;
  }

  // The range of offsets of the value the statement gives the local: the initializer declaring it, or the value of an
  // assignment to it, which ends where the assignment does
  @Nullable
  static int[] valueOf(AbstractStatement statement, VariableDeclaration local) {
    if (statement.getVariableDeclarations().stream().anyMatch(declared -> declared == local) && local.getInitializer() != null) {
      LocationInfo initializer = local.getInitializer().getLocationInfo();
      return new int[]{initializer.getStartOffset(), initializer.getEndOffset()};
    }
    for (LeafExpression assignment : statement.getAssignments()) {
      for (LeafExpression variable : statement.getVariables()) {
        LocationInfo target = variable.getLocationInfo();
        LocationInfo assigned = assignment.getLocationInfo();
        if (target.getStartOffset() == assigned.getStartOffset() && target.getEndOffset() < assigned.getEndOffset()
                && statement.searchVariableDeclaration(variable.getString()) == local) {
          return new int[]{target.getEndOffset(), assigned.getEndOffset()};
        }
      }
    }
    return null;
  }

  // The ranges of offsets of the code the value is the result of: the value itself, or, for a conditional expression
  // (as RM gives it), the values of its branches, as the value it gives is the one of the branch it takes
  static List<int[]> valueBranches(AbstractCodeFragment statement, int[] value) {
    List<int[]> branches = new ArrayList<>(List.of(value));
    for (int i = 0; i < branches.size(); i++) {
      int[] branch = branches.get(i);
      for (TernaryOperatorExpression ternary : statement.getTernaryOperatorExpressions()) {
        LocationInfo location = ternary.getLocationInfo();
        if (location.getStartOffset() == branch[0] && location.getEndOffset() == branch[1]) {
          LocationInfo then = ternary.getThenExpression().getLocationInfo();
          LocationInfo otherwise = ternary.getElseExpression().getLocationInfo();
          branches.set(i, new int[]{then.getStartOffset(), then.getEndOffset()});
          branches.add(new int[]{otherwise.getStartOffset(), otherwise.getEndOffset()});
          i--;
          break;
        }
      }
    }
    return branches;
  }

  // The locals the statement keeps a value holding the code at the location in: declared with it, or assigned it
  static List<VariableDeclaration> keepingLocals(AbstractStatement statement, LocationInfo use) {
    List<VariableDeclaration> locals = new ArrayList<>();
    for (VariableDeclaration declared : statement.getVariableDeclarations()) {
      if (declared.getInitializer() != null && declared.getInitializer().getLocationInfo().subsumes(use)) {
        locals.add(declared);
      }
    }
    for (LeafExpression assignment : statement.getAssignments()) {
      LocationInfo assigned = assignment.getLocationInfo();
      if (!assigned.subsumes(use)) {
        continue;
      }
      for (LeafExpression variable : statement.getVariables()) {
        LocationInfo target = variable.getLocationInfo();
        if (target.getStartOffset() != assigned.getStartOffset() || assigned.getEndOffset() <= target.getEndOffset()) {
          continue;
        }
        VariableDeclaration local = statement.searchVariableDeclaration(variable.getString());
        if (local != null && local.getScope() != null && local.getScope().subsumes(target) && !locals.contains(local)) {
          locals.add(local);
        }
      }
    }
    return locals;
  }

  // The qualified name of the type the class names, as written when qualified, else by a single-type import of its file
  @Nullable
  static String importedName(UMLAbstractClass context, String type, UMLModel umlModel) {
    if (type.contains(".")) {
      return type;
    }
    String path = context.getLocationInfo().getFilePath();
    for (UMLClass umlClass : umlModel.getClassList()) {
      if (!umlClass.getLocationInfo().getFilePath().equals(path)) {
        continue;
      }
      for (UMLImport umlImport : umlClass.getImportedTypes()) {
        if (!umlImport.isOnDemand() && umlImport.getName().endsWith("." + type)) {
          return umlImport.getName();
        }
      }
    }
    return null;
  }
}
