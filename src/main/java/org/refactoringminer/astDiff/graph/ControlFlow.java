package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.TryStatementObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The control flow within a member, as RM nests its statements (stage 1 of the TODO on data and control flow in
 * HunkNetwork): the statements leaving a statement on some path (its exits), the earlier statements of the bodies holding
 * a statement which leave on some path before it runs (its guards), whether a statement leaves on every path, and the
 * code an if leaving on every path runs instead of its branch. The bodies of lambdas and anonymous classes are code of
 * their own, which RM does not nest in the statements holding them, so a return in a lambda leaves the lambda, not the
 * member.
 *
 * <p>Every check reads what RM gives for each language it parses alike: the kinds of the statements, their nesting, the
 * try a catch clause or a finally block belongs to, the code RM records in a statement and its locations, and the checks
 * RM makes itself with the constants of the language of the code (a statement only calling a method, a jump without a
 * label, a call on the object itself). A switch whose cases hold their statements (TypeScript) and a switch whose cases
 * are followed by their statements (Java, Python, C++) are both cut at their cases.
 *
 * <p>A check, a statement only calling a method without a receiver (of its own class or a static import), on the object
 * itself or on its super class, leaves on some path when that method throws on some path itself, as
 * {@code ensureOpen()} does; the calls that method makes are not followed (data flow across members, stage 3). Calls on
 * other receivers, and calls whose result the statement uses, are not looked into: binding them takes the types of
 * their receivers, which reads the files declaring them.
 *
 * <h2>Not decided here</h2>
 * <ul>
 *   <li>The type a throw throws and the types a catch clause catches: a throw within the body of a try with catch
 *   clauses is taken as handled by them.</li>
 *   <li>The cases of a switch a statement falls through from: the guards of a statement of a switch are the statements
 *   of its own case before it.</li>
 *   <li>The yield of a switch expression, which leaves the expression, not a statement.</li>
 * </ul>
 *
 * <h2>Not represented by RM</h2>
 * <ul>
 *   <li>The label a labeled statement carries: RM keeps it as the name of the kind of the statement, which all labeled
 *   statements share, so a jump with a label is taken to jump out of the nearest labeled statement around it.</li>
 *   <li>The tests of the type of a value other than a Java instanceof (Kotlin is, TypeScript instanceof, Python
 *   isinstance): their ifs are no cases of a dispatch on the type of a value.</li>
 *   <li>A return or a throw within an expression, as the Kotlin {@code ?: return}: it is no statement of its own.</li>
 * </ul>
 */
final class ControlFlow {
  private ControlFlow() {
  }

  private static final Set<CodeElementType> JUMPS = EnumSet.of(CodeElementType.RETURN_STATEMENT, CodeElementType.THROW_STATEMENT,
          CodeElementType.BREAK_STATEMENT, CodeElementType.CONTINUE_STATEMENT);
  // The composites running their statements on a condition (loops aside)
  private static final Set<CodeElementType> CONDITIONAL = EnumSet.of(CodeElementType.IF_STATEMENT, CodeElementType.SWITCH_STATEMENT,
          CodeElementType.WHEN_STATEMENT);
  // The composites running their only body once, every time they run, as a block does
  private static final Set<CodeElementType> PASS_THROUGH = EnumSet.of(CodeElementType.BLOCK, CodeElementType.SYNCHRONIZED_STATEMENT,
          CodeElementType.LABELED_STATEMENT, CodeElementType.WITH_STATEMENT);

  /**
   * A check before the statement it guards: a statement leaving when at most one condition of its own holds (see
   * {@link #checkExitsOf}), with the statements it leaves at: a jump or a statement calling a method that throws.
   */
  record Guard(AbstractStatement statement, List<AbstractStatement> exits) {
  }

  // Whether the statement jumps: a return, a throw, a break or a continue
  static boolean isJump(AbstractStatement statement) {
    return JUMPS.contains(type(statement));
  }

  private static CodeElementType type(AbstractStatement statement) {
    return statement.getLocationInfo().getCodeElementType();
  }

  private static boolean isLoop(AbstractStatement statement) {
    return statement instanceof CompositeStatementObject composite && composite.isLoop();
  }

  // Whether the statement runs its statements on a condition its header holds, loops aside: an if, a switch, a when
  static boolean isConditional(AbstractStatement statement) {
    return CONDITIONAL.contains(type(statement));
  }

  // Whether the condition of the statement decides whether the code it holds runs: a conditional or a loop
  static boolean decidesOnCondition(AbstractStatement statement) {
    return isConditional(statement) || isLoop(statement);
  }

  // Whether the composite runs its statements one after the other: a block (a catch clause and a finally block are
  // blocks for RM), the body of a try, a switch or a case holding its statements
  static boolean isSequence(CompositeStatementObject composite) {
    CodeElementType type = type(composite);
    return composite.isBlock() || type == CodeElementType.TRY_STATEMENT || type == CodeElementType.SWITCH_STATEMENT
            || type == CodeElementType.SWITCH_CASE;
  }

  // Whether the composite runs its statements once, every time it runs: a block, a synchronized, a with or a labeled
  // statement
  static boolean passesThrough(CompositeStatementObject composite) {
    return PASS_THROUGH.contains(type(composite));
  }

  /**
   * The statements within the statement (or the statement itself) leaving it, skipping the code after it in the body
   * holding it, reached under at most one condition within it: a return, a throw no try within it handles, a break or a
   * continue of a loop or a switch around it, or a statement calling a method that throws. The statement is then a
   * check, as an if leaving when its condition holds, a catch clause rethrowing, or a check of a state, as ensureOpen().
   * An exit deeper within the branches of the statement belongs to the logic of those branches (the validation of one
   * case of a dispatch), which tells nothing of when the code after the statement runs.
   */
  static List<AbstractStatement> checkExitsOf(AbstractStatement statement, Predicate<AbstractCall> throwing) {
    List<AbstractStatement> exits = new ArrayList<>();
    collectExits(statement, statement, throwing, 1, 0, exits);
    return exits;
  }

  private static void collectExits(AbstractStatement within, AbstractStatement statement, Predicate<AbstractCall> throwing,
                                   int maxConditions, int conditions, List<AbstractStatement> exits) {
    if (isJump(statement)) {
      if (leaves(within, statement)) {
        exits.add(statement);
      }
      return;
    }
    if (callsThrowing(statement, throwing) && !isHandled(within, statement)) {
      exits.add(statement);
    }
    int childConditions = decidesOnCondition(statement) ? conditions + 1 : conditions;
    if (statement instanceof CompositeStatementObject composite && childConditions <= maxConditions) {
      for (AbstractStatement child : composite.getStatements()) {
        collectExits(within, child, throwing, maxConditions, childConditions, exits);
      }
    }
  }

  // Whether the jump leaves the statement it lies in: a return does, a throw unless a try within it handles it, a break
  // or a continue unless the loop or the switch it jumps out of lies within it (with a label, the labeled statement)
  private static boolean leaves(AbstractStatement within, AbstractStatement jump) {
    CodeElementType type = type(jump);
    if (type == CodeElementType.RETURN_STATEMENT) {
      return true;
    }
    if (type == CodeElementType.THROW_STATEMENT) {
      return !isHandled(within, jump);
    }
    // RM tells a jump without a label by the bare keyword of the language
    boolean labeled = !jump.isKeyword();
    for (CompositeStatementObject ancestor = jump.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
      CodeElementType ancestorType = type(ancestor);
      boolean target = labeled ? ancestorType == CodeElementType.LABELED_STATEMENT
              : ancestor.isLoop() || type == CodeElementType.BREAK_STATEMENT && ancestorType == CodeElementType.SWITCH_STATEMENT;
      if (target) {
        return !isWithin(ancestor, within);
      }
      if (ancestor == within) {
        return true;
      }
    }
    return true;
  }

  // Whether the statement is the other one or lies within it
  private static boolean isWithin(AbstractStatement statement, AbstractStatement other) {
    for (AbstractStatement ancestor = statement; ancestor != null; ancestor = ancestor.getParent()) {
      if (ancestor == other) {
        return true;
      }
    }
    return false;
  }

  // Whether what the statement throws is handled by a try within the code: the statement lies in the body of a try with
  // catch clauses (RM gives the statements of the body to the try, and its catch clauses next to it)
  private static boolean isHandled(AbstractStatement within, AbstractStatement statement) {
    if (statement == within) {
      return false;
    }
    for (CompositeStatementObject ancestor = statement.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
      if (ancestor instanceof TryStatementObject tryStatement && !tryStatement.getCatchClauses().isEmpty()) {
        return true;
      }
      if (ancestor == within) {
        return false;
      }
    }
    return false;
  }

  // Whether the statement is a check (see the class doc) calling a method that throws: RM finds the call the statement
  // only makes, and whether its receiver is none, the object itself or its super class
  private static boolean callsThrowing(AbstractStatement statement, Predicate<AbstractCall> throwing) {
    if (type(statement) != CodeElementType.EXPRESSION_STATEMENT) {
      return false;
    }
    AbstractCall call = statement.invocationCoveringEntireFragment();
    return call != null && call.getCoverage() == StatementCoverageType.ONLY_CALL
            && (call.expressionIsNullOrThis() || call.isSuperCall()) && throwing.test(call);
  }

  /**
   * The guards of the statement: the earlier statements of the bodies holding it, up to the body of its member or of the
   * lambda it lies in, which are checks leaving before it runs (see {@link #checkExitsOf}), outermost first. A catch
   * clause runs on what the body of its try throws, so the statements of that body guard nothing in it.
   *
   * <p>A case of a dispatch on the type of a value, an if without an else testing that type (instanceof) whose branch
   * returns on every path, is one of the alternatives the cases on the type of the same value before it are: they
   * return for other types. They guard code within its branch only by excluding those types, which tells nothing of that
   * code, so they are no guards of it; they are of code deciding which values reach the case, its condition (see
   * {@code decidesCase}). A dispatch comparing a value with constants is not told apart from checks of that value.
   *
   * @param decidesCase whether the code the guards are of lies in (or edits) the condition of the composite statement
   * @param bearsOn     whether a check, with its exits, bears on the code the guards are of (see RMIndex#guardParts)
   */
  static List<Guard> guardsOf(AbstractStatement statement, Predicate<AbstractCall> throwing,
                              Predicate<CompositeStatementObject> decidesCase,
                              BiPredicate<AbstractStatement, List<AbstractStatement>> bearsOn) {
    List<Guard> guards = new ArrayList<>();
    AbstractStatement current = statement;
    while (current.getParent() != null) {
      CompositeStatementObject parent = current.getParent();
      if (isSequence(parent)) {
        String dispatched = typeCaseSubject(current, throwing);
        if (dispatched != null && decidesCase.test((CompositeStatementObject) current)) {
          dispatched = null;
        }
        List<Guard> levelGuards = new ArrayList<>();
        for (AbstractStatement earlier : earlierStatements(parent, current)) {
          List<AbstractStatement> exits = checkExitsOf(earlier, throwing);
          if (dispatched != null && dispatched.equals(typeCaseSubject(earlier, throwing))) {
            continue;
          }
          if (!exits.isEmpty() && bearsOn.test(earlier, exits)) {
            levelGuards.add(new Guard(earlier, exits));
          }
        }
        guards.addAll(0, levelGuards);
      }
      current = parent;
    }
    return guards;
  }

  // The value the statement dispatches on, when it is a case of a dispatch on its type: an if without an else whose
  // condition tests the type of one value only (instanceof) and whose branch returns on every path
  @Nullable
  private static String typeCaseSubject(AbstractStatement statement, Predicate<AbstractCall> throwing) {
    if (type(statement) != CodeElementType.IF_STATEMENT || !(statement instanceof CompositeStatementObject composite)
            || composite.getStatements().size() != 1 || !alwaysExits(composite.getStatements().get(0))
            || !checkExitsOf(statement, throwing).stream().allMatch(exit -> type(exit) == CodeElementType.RETURN_STATEMENT)) {
      return null;
    }
    Set<String> subjects = new HashSet<>();
    for (AbstractCodeFragment condition : composite.getExpressions()) {
      List<LeafExpression> tests = new ArrayList<>(condition.getInstanceofExpressions());
      tests.addAll(condition.getPatternInstanceofExpressions());
      for (LeafExpression test : tests) {
        String subject = testedValue(condition, test);
        if (subject != null) {
          subjects.add(subject);
        }
      }
    }
    return subjects.size() == 1 ? subjects.iterator().next() : null;
  }

  // The value a test of a type tests, the operand before the type: the longest code RM records in the condition (a
  // name, a call, an array access, a cast or a parenthesized expression) starting where the test starts and ending
  // within it
  @Nullable
  private static String testedValue(AbstractCodeFragment condition, LeafExpression test) {
    LocationInfo location = test.getLocationInfo();
    String value = null;
    int end = -1;
    List<LeafExpression> operands = new ArrayList<>(condition.getVariables());
    operands.addAll(condition.getArrayAccesses());
    operands.addAll(condition.getCastExpressions());
    operands.addAll(condition.getParenthesizedExpressions());
    condition.getMethodInvocations().forEach(call -> operands.add(new LeafExpression(call.getString(), call.getLocationInfo())));
    for (LeafExpression operand : operands) {
      LocationInfo operandLocation = operand.getLocationInfo();
      if (operandLocation.getStartOffset() == location.getStartOffset() && operandLocation.getEndOffset() < location.getEndOffset()
              && operandLocation.getEndOffset() > end) {
        value = operand.getString();
        end = operandLocation.getEndOffset();
      }
    }
    return value;
  }

  // The statements of the composite running before the statement, within the case of a switch
  private static List<AbstractStatement> earlierStatements(CompositeStatementObject parent, AbstractStatement statement) {
    AbstractStatement tryStatement = tryOf(statement);
    AbstractStatement anchor = tryStatement != null ? tryStatement : statement;
    List<AbstractStatement> siblings = parent.getStatements();
    int index = indexOf(siblings, anchor);
    if (index < 0 || isCase(anchor)) {
      return List.of();
    }
    int from = 0;
    for (int i = index - 1; i >= 0; i--) {
      if (isCase(siblings.get(i))) {
        from = i + 1;
        break;
      }
    }
    return siblings.subList(from, index);
  }

  // Whether the statement is a case of a switch, which starts the statements of the case following it or holds them
  private static boolean isCase(AbstractStatement statement) {
    return type(statement) == CodeElementType.SWITCH_CASE;
  }

  private static int indexOf(List<AbstractStatement> statements, AbstractStatement statement) {
    for (int i = 0; i < statements.size(); i++) {
      if (statements.get(i) == statement) {
        return i;
      }
    }
    return -1;
  }

  /**
   * The statements of the composite running after the statement, within the case of a switch; a catch clause or a
   * finally block of the try the statement is or belongs to is no statement after it.
   */
  static List<AbstractStatement> laterStatements(CompositeStatementObject parent, AbstractStatement statement) {
    List<AbstractStatement> siblings = parent.getStatements();
    int index = indexOf(siblings, statement);
    AbstractStatement tryStatement = tryOf(statement);
    List<AbstractStatement> later = new ArrayList<>();
    for (int i = index + 1; index >= 0 && i < siblings.size(); i++) {
      AbstractStatement sibling = siblings.get(i);
      if (isCase(sibling)) {
        break;
      }
      if (tryStatement != null && tryOf(sibling) == tryStatement) {
        continue;
      }
      later.add(sibling);
    }
    return later;
  }

  // The try the statement is, or the try of a catch clause or a finally block
  @Nullable
  private static AbstractStatement tryOf(AbstractStatement statement) {
    if (statement instanceof TryStatementObject) {
      return statement;
    }
    // RM leaves the try of a composite unset (null) for the composites some of its constructors build
    if (statement instanceof CompositeStatementObject composite && composite.getTryContainer() != null
            && composite.getTryContainer().isPresent()) {
      return composite.getTryContainer().get();
    }
    return null;
  }

  /**
   * Whether the statement leaves on every path: a jump, a body whose last statement does, a conditional both of whose
   * branches do.
   */
  static boolean alwaysExits(AbstractStatement statement) {
    if (isJump(statement)) {
      return true;
    }
    if (!(statement instanceof CompositeStatementObject composite) || composite.getStatements().isEmpty()) {
      return false;
    }
    List<AbstractStatement> statements = composite.getStatements();
    if (type(statement) == CodeElementType.IF_STATEMENT) {
      return statements.size() == 2 && alwaysExits(statements.get(0)) && alwaysExits(statements.get(1));
    }
    if (passesThrough(composite)) {
      return alwaysExits(statements.get(statements.size() - 1));
    }
    return false;
  }

  /**
   * The code an if without an else whose branch leaves on every path runs instead of that branch: the statements after
   * it in its body, which its condition decides as an else would. None for another statement.
   */
  static List<AbstractStatement> implicitElse(AbstractStatement statement) {
    if (type(statement) != CodeElementType.IF_STATEMENT || !(statement instanceof CompositeStatementObject composite)
            || composite.getStatements().size() != 1 || !alwaysExits(composite.getStatements().get(0))
            || statement.getParent() == null || !isSequence(statement.getParent())) {
      return List.of();
    }
    return laterStatements(statement.getParent(), statement);
  }

  /**
   * Whether the body of the operation throws on some path, a throw no try of it handles, as a check rejecting a state
   * or an argument does.
   */
  static boolean throwsOnSomePath(UMLOperation operation) {
    if (operation.getBody() == null) {
      return false;
    }
    CompositeStatementObject body = operation.getBody().getCompositeStatement();
    for (AbstractStatement statement : body.getAllStatements()) {
      if (type(statement) == CodeElementType.THROW_STATEMENT && !isHandled(body, statement)) {
        return true;
      }
    }
    return false;
  }
}
