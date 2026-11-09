package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The data flow of the locals within a member, as RM nests its statements and records the names and the assignments of
 * their code (stage 2 of the TODO on data and control flow in HunkNetwork): the statements writing a local (a declaration
 * with an initializer, an assignment, an increment) and reading it, the readers a definition reaches before another
 * definition replaces it on every path, and the definitions reaching a reader. A composite statement reads and writes by
 * its own code, its header (a condition, the parts of a for); its statements are statements of their own. The bodies of
 * lambdas and anonymous classes are code of their own, as in {@link ControlFlow}, whose reading of the nesting of the
 * statements this class shares.
 *
 * <p>A name refers to a local as RM finds the statements using a variable: the name itself, or a name it qualifies. The
 * target of an assignment is the longest code RM records in it starting where it starts (a name, an array access, a
 * call), as no part of the value assigned starts there; the assignment is plain when the assignment operator of the
 * language follows its target.
 *
 * <h2>Not decided here</h2>
 * <ul>
 *   <li>A definition within a try reaching its catch clauses: what the body of the try throws is not followed, so the
 *   code after a try is reached by the definitions of its body only along the path completing it.</li>
 *   <li>A definition replacing another within a try, a catch clause, a loop or a switch: they may not run, so the earlier
 *   definition still reaches the code after them.</li>
 *   <li>The fields and the elements of arrays: only locals and parameters are followed.</li>
 * </ul>
 *
 * <h2>Not represented by RM</h2>
 * <ul>
 *   <li>The operator of a prefix or a postfix expression: RM keeps their text only, so an increment or a decrement is
 *   told by its operator ({@code ++}, {@code --}) in the text around the name.</li>
 *   <li>The TypeScript increments and decrements (update expressions), which RM does not record.</li>
 *   <li>The Kotlin compound assignments ({@code +=}), which RM keeps as infix expressions, not as assignments.</li>
 *   <li>The targets of a destructuring assignment ({@code a, b = pair}), which RM keeps as the text of the assignment.</li>
 * </ul>
 */
final class DataFlow {
  private DataFlow() {
  }

  private static final Set<String> STEPS = Set.of("++", "--");

  private static CodeElementType type(AbstractStatement statement) {
    return statement.getLocationInfo().getCodeElementType();
  }

  // The code of the statement itself: a leaf statement, or the header of a composite statement
  private static List<AbstractCodeFragment> ownCode(AbstractStatement statement) {
    return statement instanceof CompositeStatementObject composite ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
  }

  // A name in the code referring to the local, the name of a variable RM gives or the receiver of a call, which RM gives
  // only as the text of the call
  private record Occurrence(LocationInfo location, @Nullable LeafExpression variable) {
  }

  // Whether the text names the local: the name itself or a name it qualifies, as RM finds the statements using a
  // variable (VariableDeclaration#addStatementInScope)
  private static boolean names(String text, String name) {
    return text.equals(name) || text.startsWith(name + ".");
  }

  /**
   * The local a name RM gives (a variable, the receiver of a call) refers to by RM's rule (see {@link #names}): the name
   * itself, or the name it qualifies.
   */
  static String referredLocal(String text) {
    String trimmed = text.trim();
    int qualified = trimmed.indexOf('.');
    return qualified < 0 ? trimmed : trimmed.substring(0, qualified);
  }

  /**
   * The local or the field a name RM gives refers to by RM's rule (VariableDeclaration#addStatementInScope): a field
   * may be named by the reference of the language to the object itself ({@code this.}, {@code self.}).
   */
  static String referredName(String text, Constants lang) {
    String trimmed = text.trim();
    return referredLocal(trimmed.startsWith(lang.THIS_DOT) ? trimmed.substring(lang.THIS_DOT.length()) : trimmed);
  }

  /**
   * Whether the code of the statement itself adds a value into the local by a compound assignment ({@code sum += x}):
   * the local is the target of an assignment that is not plain (see {@link #isPlainTarget}).
   */
  static boolean accumulates(AbstractStatement statement, VariableDeclaration local) {
    for (AbstractCodeFragment fragment : ownCode(statement)) {
      for (Occurrence occurrence : occurrences(fragment, local)) {
        LeafExpression name = occurrence.variable();
        if (name != null && name.getString().equals(local.getVariableName()) && assignmentOf(name, fragment) != null
                && !isPlainTarget(occurrence, fragment)) {
          return true;
        }
      }
    }
    return false;
  }

  // The names in the code of the fragment referring to the local within its scope (a parameter RM gives none of is in
  // the scope of its member), and not in a lambda or an anonymous class of the fragment
  private static List<Occurrence> occurrences(AbstractCodeFragment fragment, VariableDeclaration local) {
    String name = local.getVariableName();
    List<LocationInfo> apart = new ArrayList<>();
    fragment.getLambdas().forEach(lambda -> apart.add(lambda.getLocationInfo()));
    fragment.getAnonymousClassDeclarations().forEach(anonymous -> apart.add(anonymous.getLocationInfo()));
    Predicate<LocationInfo> inScope = location -> (local.getScope() == null || local.getScope().subsumes(location))
            && apart.stream().noneMatch(body -> body.subsumes(location))
            && location.getStartOffset() != local.getLocationInfo().getStartOffset();
    List<Occurrence> occurrences = new ArrayList<>();
    for (LeafExpression variable : fragment.getVariables()) {
      if (names(variable.getString(), name) && inScope.test(variable.getLocationInfo())) {
        occurrences.add(new Occurrence(variable.getLocationInfo(), variable));
      }
    }
    for (AbstractCall call : fragment.getMethodInvocations()) {
      String receiver = call.getExpression();
      if (receiver != null && names(receiver, name) && inScope.test(call.getLocationInfo())
              && occurrences.stream().noneMatch(occurrence -> occurrence.location().getStartOffset() == call.getLocationInfo().getStartOffset())) {
        occurrences.add(new Occurrence(call.getLocationInfo(), null));
      }
    }
    return occurrences;
  }

  // The assignment of the fragment the name is the target of: it starts with the name, and no longer code RM records
  // in it (an array access or a qualified name holding the name) starts there
  @Nullable
  private static LeafExpression assignmentOf(LeafExpression name, AbstractCodeFragment fragment) {
    LocationInfo location = name.getLocationInfo();
    for (LeafExpression assignment : fragment.getAssignments()) {
      LocationInfo assigned = assignment.getLocationInfo();
      if (assigned.getStartOffset() == location.getStartOffset() && location.getEndOffset() < assigned.getEndOffset()
              && targetEnd(assignment, fragment) == location.getEndOffset()) {
        return assignment;
      }
    }
    return null;
  }

  // The end of the target of the assignment: the longest code RM records in the fragment starting where the assignment
  // starts and ending within it
  private static int targetEnd(LeafExpression assignment, AbstractCodeFragment fragment) {
    LocationInfo assigned = assignment.getLocationInfo();
    List<LocationInfo> candidates = new ArrayList<>();
    fragment.getVariables().forEach(variable -> candidates.add(variable.getLocationInfo()));
    fragment.getArrayAccesses().forEach(access -> candidates.add(access.getLocationInfo()));
    fragment.getMethodInvocations().forEach(call -> candidates.add(call.getLocationInfo()));
    int end = -1;
    for (LocationInfo candidate : candidates) {
      if (candidate.getStartOffset() == assigned.getStartOffset() && candidate.getEndOffset() < assigned.getEndOffset()) {
        end = Math.max(end, candidate.getEndOffset());
      }
    }
    return end;
  }

  // Whether the name is the target of a plain assignment, which writes the local without reading it: the assignment
  // operator of the language follows the name (a compound assignment puts its operator before it)
  private static boolean isPlainTarget(Occurrence occurrence, AbstractCodeFragment fragment) {
    LeafExpression name = occurrence.variable();
    if (name == null) {
      return false;
    }
    LeafExpression assignment = assignmentOf(name, fragment);
    return assignment != null && assignsPlainly(assignment, name.getString());
  }

  /**
   * Whether the assignment assigns its target, the code it starts with, plainly, writing it without reading it: the
   * assignment operator of the language follows the target, as RM matches an assignment with the constants of the
   * language ({@code variable + ASSIGNMENT}); a compound assignment puts an operator of its own there.
   */
  static boolean assignsPlainly(LeafExpression assignment, String target) {
    String text = assignment.getString();
    return text.startsWith(target) && text.substring(target.length()).trim().startsWith(assignment.getLANG().ASSIGNMENT.trim());
  }

  // Whether the prefix or postfix expression increments or decrements the name, its operand
  private static boolean steps(LeafExpression step, LeafExpression name) {
    LocationInfo location = step.getLocationInfo();
    LocationInfo operand = name.getLocationInfo();
    String text = step.getString();
    String operator = null;
    if (operand.getStartOffset() == location.getStartOffset() && text.startsWith(name.getString())) {
      operator = text.substring(name.getString().length());
    }
    else if (operand.getEndOffset() == location.getEndOffset() && text.endsWith(name.getString())) {
      operator = text.substring(0, text.length() - name.getString().length());
    }
    return operator != null && STEPS.contains(operator.trim());
  }

  /** Whether the code of the statement itself writes the local: declares it with an initializer, assigns or increments it. */
  static boolean writes(AbstractStatement statement, VariableDeclaration local) {
    for (AbstractCodeFragment fragment : ownCode(statement)) {
      if (fragment.getVariableDeclarations().stream().anyMatch(declared -> declared == local) && local.getInitializer() != null) {
        return true;
      }
      List<LeafExpression> steps = new ArrayList<>(fragment.getPostfixExpressions());
      steps.addAll(fragment.getPrefixExpressions());
      for (Occurrence occurrence : occurrences(fragment, local)) {
        LeafExpression name = occurrence.variable();
        if (name != null && name.getString().equals(local.getVariableName())
                && (assignmentOf(name, fragment) != null || steps.stream().anyMatch(step -> steps(step, name)))) {
          return true;
        }
      }
    }
    return false;
  }

  /** Whether the code of the statement itself reads the local: names it other than as the target of a plain assignment. */
  static boolean reads(AbstractStatement statement, VariableDeclaration local) {
    return ownCode(statement).stream().anyMatch(fragment -> readsIn(fragment, local));
  }

  /** The names in the code of the fragment reading the local (see {@link #reads}). */
  static List<LocationInfo> readsAt(AbstractCodeFragment fragment, VariableDeclaration local) {
    return occurrences(fragment, local).stream().filter(name -> !isPlainTarget(name, fragment)).map(Occurrence::location).toList();
  }

  /** Whether the code of the fragment reads the local (see {@link #reads}). */
  static boolean readsIn(AbstractCodeFragment fragment, VariableDeclaration local) {
    return occurrences(fragment, local).stream().anyMatch(name -> !isPlainTarget(name, fragment));
  }

  /**
   * The statements reading the local that the definition at the statement reaches: the ones running after it on some
   * path before a statement replacing the local on every path (a definition, or a jump leaving the path), in the order
   * they run once; within a loop, the next round reaches its condition and the code of its body before the definition.
   */
  static List<AbstractStatement> readersReached(AbstractStatement definition, VariableDeclaration local) {
    Set<AbstractStatement> readers = Collections.newSetFromMap(new IdentityHashMap<>());
    List<AbstractStatement> ordered = new ArrayList<>();
    List<AbstractStatement> chain = new ArrayList<>();
    AbstractStatement current = definition;
    while (current.getParent() != null) {
      CompositeStatementObject parent = current.getParent();
      chain.add(current);
      if (ControlFlow.isSequence(parent)) {
        for (AbstractStatement later : laterStatements(parent, current)) {
          if (visit(later, local, readers, ordered)) {
            return ordered;
          }
        }
      }
      if (parent.isLoop()) {
        // The next round runs the condition of the loop, then its body up to the definition
        if (reads(parent, local) && readers.add(parent)) {
          ordered.add(parent);
        }
        if (visitBefore(chain, local, readers, ordered)) {
          return ordered;
        }
      }
      current = parent;
    }
    return ordered;
  }

  // Visits, within the loop holding the chain, the statements of each body of the chain before the statement of the
  // chain it holds, outermost first; whether they replace the local on every path
  private static boolean visitBefore(List<AbstractStatement> chain, VariableDeclaration local, Set<AbstractStatement> readers,
                                     List<AbstractStatement> ordered) {
    for (int i = chain.size() - 1; i >= 0; i--) {
      AbstractStatement held = chain.get(i);
      CompositeStatementObject holder = held.getParent();
      if (holder == null || !ControlFlow.isSequence(holder)) {
        continue;
      }
      for (AbstractStatement earlier : holder.getStatements()) {
        if (earlier == held) {
          break;
        }
        if (visit(earlier, local, readers, ordered)) {
          return true;
        }
      }
    }
    return false;
  }

  // Visits the statement, running after the definition; whether it replaces the local, or leaves, on every path
  private static boolean visit(AbstractStatement statement, VariableDeclaration local, Set<AbstractStatement> readers,
                               List<AbstractStatement> ordered) {
    if (reads(statement, local) && readers.add(statement)) {
      ordered.add(statement);
    }
    if (!(statement instanceof CompositeStatementObject composite)) {
      return writes(statement, local) || ControlFlow.isJump(statement);
    }
    if (writes(statement, local) && !composite.isLoop()) {
      return true;
    }
    List<AbstractStatement> children = composite.getStatements();
    if (type(statement) == CodeElementType.IF_STATEMENT) {
      boolean replaced = children.size() == 2;
      for (AbstractStatement branch : children) {
        replaced &= visit(branch, local, readers, ordered);
      }
      return replaced;
    }
    if (ControlFlow.passesThrough(composite)) {
      for (AbstractStatement child : children) {
        if (visit(child, local, readers, ordered)) {
          return true;
        }
      }
      return false;
    }
    // A loop, a switch, a try, a catch clause or a finally block may not run its statements (or not to their end)
    for (AbstractStatement child : children) {
      if (visit(child, local, readers, ordered)) {
        break;
      }
    }
    return false;
  }

  // The statements of the sequence after the statement (see ControlFlow); a catch clause runs only on what its try
  // throws, so it is none
  private static List<AbstractStatement> laterStatements(CompositeStatementObject parent, AbstractStatement statement) {
    return ControlFlow.laterStatements(parent, statement).stream()
            .filter(sibling -> type(sibling) != CodeElementType.CATCH_CLAUSE).toList();
  }
}
