package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CreationReference;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.MethodReference;
import gr.uom.java.xmi.decomposition.TernaryOperatorExpression;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.util.PathFileUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The statements, operations and classes RM gives of both revisions, by file, to tell where a tree lies in them (see
 * {@link RMInfo}). A module RM makes of a file is no class, as no tree but the file declares it.
 *
 * <p>Every rule reads what RM gives of each language it parses alike: the kinds, the nesting and the locations of the
 * statements, operations, classes and lambdas, the code RM records in a statement, and the constants RM keeps of the
 * language (the reference to the object itself, the statement termination, the return keyword, the block delimiters).
 * Names RM gives as text are related to the local or the field they refer to by RM's own rule (see
 * DataFlow#referredName). Where code opens and closes is read from the locations RM gives: a header ends where code of
 * the body's own opens it, or where the code RM gives of the header ends (see headEnd), and code ends with a line
 * closing it only when no statement within it ends where it ends (see closes), as a body indented under its header
 * (Python) ends with its last statement.
 *
 * <h2>Not represented by RM</h2>
 * <ul>
 *   <li>The arguments of a call, which RM gives as text only: a lambda or a method reference is an argument of the
 *   innermost call or creation RM gives holding it (see takesCode).</li>
 *   <li>The receiver of a call, which RM gives as text only: it is related to a name by RM's rule, and is a name the
 *   call may change only when RM gives that name in the code too (see writtenNames), so a call on the result of another
 *   call changes no name.</li>
 *   <li>A name qualified other than by a dot (a Kotlin safe call {@code a?.b}, a C++ access through a pointer
 *   {@code p->b}): RM's rule relates a qualified name to the name it qualifies by a dot only.</li>
 *   <li>The location of the name of an annotation: an annotation printed in brief keeps its code up to the first
 *   occurrence of the name RM gives of its type.</li>
 * </ul>
 */
class RMIndex {
  private final Map<SrcDst, Map<String, List<Unit>>> units = new EnumMap<>(SrcDst.class);
  // Unchanged files are in both revisions with the same code objects
  private final Map<SrcDst, Map<AbstractStatement, Unit>> statementUnits = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<UMLOperation>>> operations = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<UMLClass>>> classes = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> comments = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, Set<Integer>>> blankCommentLines = new EnumMap<>(SrcDst.class);
  private final Map<CompositeStatementObject, List<Unit>> childUnits = new IdentityHashMap<>();
  private final Map<SrcDst, Map<String, List<LocationInfo>>> leadingParts = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<Body>>> bodies = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<SimpleStatement>>> simpleStatements = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> fileParts = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocalDeclaration>>> localDeclarations = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> docComments = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<UMLAbstractClass>>> owners = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> initializers = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> fieldDeclarations = new EnumMap<>(SrcDst.class);

  private final Map<SrcDst, Map<String, List<Declared>>> declared = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<RunScope>>> runScopes = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Set<VariableDeclaration>> lambdaParameters = new EnumMap<>(SrcDst.class);
  private final UMLModelDiff modelDiff;
  private final ContentsContexts contentsContexts;
  private final Map<SrcDst, Set<String>> indexed = new EnumMap<>(SrcDst.class);

  RMIndex(UMLModelDiff modelDiff, ContentsContexts contentsContexts) {
    this.modelDiff = modelDiff;
    this.contentsContexts = contentsContexts;
  }

  // A file is indexed when first asked about, as RM takes in unchanged files only once their code is needed
  private void ensureIndexed(SrcDst srcDst, String path) {
    if (indexed.computeIfAbsent(srcDst, side -> new HashSet<>()).add(path)) {
      index(srcDst, srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel(), path);
    }
  }

  private void index(SrcDst srcDst, UMLModel umlModel, String path) {
    Map<String, List<Unit>> fileUnits = units.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<AbstractStatement, Unit> sideUnits = statementUnits.computeIfAbsent(srcDst, side -> new IdentityHashMap<>());
    Map<String, List<UMLOperation>> fileOperations = operations.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<UMLClass>> fileClasses = classes.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<LocationInfo>> fileComments = comments.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, Set<Integer>> fileBlankCommentLines = blankCommentLines.computeIfAbsent(srcDst, side -> new HashMap<>());

    Map<String, List<LocationInfo>> fileLeadingParts = leadingParts.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<Body>> fileBodies = bodies.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<SimpleStatement>> fileSimpleStatements = simpleStatements.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<LocationInfo>> filePackageImports = fileParts.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<LocationInfo>> fileDocComments = docComments.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<UMLAbstractClass>> fileOwners = owners.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<LocationInfo>> fileInitializers = initializers.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<LocationInfo>> fileFieldDeclarations = fieldDeclarations.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<VariableDeclaration, LocationInfo> declaringFragments = new IdentityHashMap<>();

    Set<String> seenComments = new HashSet<>();
    Set<UMLComment> bodyComments = Collections.newSetFromMap(new IdentityHashMap<>());
    umlModel.getCommentMap().getOrDefault(path, List.of()).forEach(comment -> pushComment(fileComments, fileBlankCommentLines, seenComments, comment));
    Set<String> seenFileParts = new HashSet<>();
    Map<String, List<Declared>> fileDeclared = declared.computeIfAbsent(srcDst, side -> new HashMap<>());
    Map<String, List<RunScope>> fileRunScopes = runScopes.computeIfAbsent(srcDst, side -> new HashMap<>());
    Set<VariableDeclaration> sideLambdaParameters = lambdaParameters.computeIfAbsent(srcDst,
            side -> Collections.newSetFromMap(new IdentityHashMap<>()));
    Set<String> locatedScopes = new HashSet<>();
    String content = contentsContexts.getContent(srcDst, path);
    for (UMLAbstractClass context : contextsOf(umlModel, path)) {
      if (context instanceof UMLAnonymousClass) {
        pushRunScope(fileRunScopes, locatedScopes, context.getLocationInfo(), List.of(), true);
      }
      if (context instanceof UMLClass umlClass) {
      fileDeclared.computeIfAbsent(path, file -> new ArrayList<>())
              .add(new Declared(ranges(List.of(umlClass.getLocationInfo()), content), umlClass, null));
      if (!umlClass.isModule()) {
        fileClasses.computeIfAbsent(umlClass.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(umlClass);
      }
      if (umlClass.getPackageDeclaration() != null && umlClass.getPackageDeclaration().isPresent()) {
        pushLocation(filePackageImports, seenFileParts, umlClass.getPackageDeclaration().get().getLocationInfo());
      }
      umlClass.getImportedTypes().forEach(umlImport -> pushLocation(filePackageImports, seenFileParts, umlImport.getLocationInfo()));
      // The comments before the package declaration, as a license header, which RM takes out of the comments of the file
      umlClass.getPackageDeclarationComments().forEach(comment -> pushComment(fileComments, fileBlankCommentLines, seenComments, comment));
      pushLeadingParts(fileLeadingParts, umlClass.getJavadoc(), umlClass.getAnnotations());
      pushDocComment(fileDocComments, umlClass.getJavadoc());
      }

        fileOwners.computeIfAbsent(context.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(context);
        context.getComments().forEach(comment -> pushComment(fileComments, fileBlankCommentLines, seenComments, comment));
        context.getAttributes().forEach(attribute -> attribute.getComments().forEach(comment -> pushComment(fileComments, fileBlankCommentLines, seenComments, comment)));
        for (UMLAttribute attribute : context.getAttributes()) {
          List<LocationInfo> attributeLocations = new ArrayList<>(List.of(attribute.getLocationInfo(),
                  attribute.getVariableDeclaration().getLocationInfo()));
          if (attribute.getFieldDeclarationLocationInfo() != null) {
            attributeLocations.add(attribute.getFieldDeclarationLocationInfo());
          }
          fileDeclared.computeIfAbsent(path, file -> new ArrayList<>()).add(new Declared(ranges(attributeLocations, content), attribute, null));
          pushLeadingParts(fileLeadingParts, attribute.getJavadoc(), attribute.getAnnotations());
          pushDocComment(fileDocComments, attribute.getJavadoc());
          fileFieldDeclarations.computeIfAbsent(path, file -> new ArrayList<>()).add(attribute.getFieldDeclarationLocationInfo() != null
                  ? attribute.getFieldDeclarationLocationInfo() : attribute.getLocationInfo());
          if (attribute.getFieldDeclarationLocationInfo() != null) {
            List<LocationInfo> later = new ArrayList<>();
            attribute.getAllLambdas().stream().filter(lambda -> lambda.getBody() != null).forEach(lambda -> later.add(lambda.getLocationInfo()));
            attribute.getAnonymousClassList().forEach(anonymous -> later.add(anonymous.getLocationInfo()));
            push(fileSimpleStatements, new SimpleStatement(attribute.getFieldDeclarationLocationInfo(), null, later));
          }
        }
        for (UMLEnumConstant enumConstant : context.getEnumConstants()) {
          fileFieldDeclarations.computeIfAbsent(path, file -> new ArrayList<>()).add(enumConstant.getLocationInfo());
          pushLeadingParts(fileLeadingParts, enumConstant.getJavadoc(), enumConstant.getAnnotations());
          pushDocComment(fileDocComments, enumConstant.getJavadoc());
        }
        for (UMLInitializer initializer : context.getInitializers()) {
          fileInitializers.computeIfAbsent(path, file -> new ArrayList<>()).add(initializer.getLocationInfo());
          pushDocComment(fileDocComments, initializer.getJavadoc());
          if (initializer.getBody() != null) {
            CompositeStatementObject body = initializer.getBody().getCompositeStatement();
            push(fileBodies, body(body, initializer.getLocationInfo(), false, true, List.of(), body.getAllStatements()));
          }
        }
        for (UMLOperation operation : operationsOf(context)) {
          fileDeclared.computeIfAbsent(path, file -> new ArrayList<>())
                  .add(new Declared(ranges(List.of(operation.getLocationInfo()), content), operation, null));
          for (VariableDeclaration variable : operation.getAllVariableDeclarations()) {
            fileDeclared.computeIfAbsent(path, file -> new ArrayList<>())
                    .add(new Declared(ranges(List.of(variable.getLocationInfo()), content), variable, null));
          }
          for (VariableDeclaration parameter : operation.getParameterDeclarationList()) {
            fileDeclared.computeIfAbsent(path, file -> new ArrayList<>())
                    .add(new Declared(ranges(List.of(parameter.getLocationInfo()), content), parameter, operation));
          }
          fileOperations.computeIfAbsent(operation.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(operation);
          pushLeadingParts(fileLeadingParts, operation.getJavadoc(), operation.getAnnotations());
          pushDocComment(fileDocComments, operation.getJavadoc());
          operation.getComments().forEach(comment -> comment.getJavaDoc().ifPresent(doc -> pushDocComment(fileDocComments, doc)));
          operation.getAllVariableDeclarations().forEach(variable -> pushLeadingParts(fileLeadingParts, null, variable.getAnnotations()));
          operation.getParameterDeclarationList().forEach(parameter -> pushLeadingParts(fileLeadingParts, null, parameter.getAnnotations()));
          if (operation.getBody() != null) {
            CompositeStatementObject body = operation.getBody().getCompositeStatement();
            push(fileBodies, body(body, operation.getLocationInfo(), true, true, heading(operation), body.getAllStatements()));
          }
          operation.getComments().forEach(comment -> pushComment(fileComments, fileBlankCommentLines, seenComments, comment));
          operation.getComments().stream().filter(comment -> comment.getParent() != null).forEach(bodyComments::add);
        }

        List<VariableDeclarationContainer> members = new ArrayList<>();
        members.addAll(operationsOf(context));
        members.addAll(context.getInitializers());
        members.addAll(context.getAttributes());
        members.addAll(context.getEnumConstants());
        for (VariableDeclarationContainer member : members) {
          for (VariableDeclarationContainer container : ReceiverTypeResolver.containerAndLambdas(member)) {
            pushRunScopes(fileRunScopes, locatedScopes, container);
            if (container instanceof LambdaExpressionObject lambda) {
              sideLambdaParameters.addAll(lambda.getParameters());
            }
            if (container.getBody() == null) {
              continue;
            }
            if (container instanceof LambdaExpressionObject lambda) {
              CompositeStatementObject body = lambda.getBody().getCompositeStatement();
              push(fileBodies, body(body, lambda.getLocationInfo(), false, false,
                      lambda.getParameters().stream().map(VariableDeclaration::getLocationInfo).toList(), body.getAllStatements()));
            }
            for (AbstractStatement statement : container.getBody().getCompositeStatement().getAllStatements()) {
              if (sideUnits.containsKey(statement)) {
                continue;
              }
              Unit unit = new Unit(statement.getLocationInfo(), statement.getParent(), statement);
              sideUnits.put(statement, unit);
              fileUnits.computeIfAbsent(unit.location().getFilePath(), file -> new ArrayList<>()).add(unit);
              if (statement.getParent() != null) {
                childUnits.computeIfAbsent(statement.getParent(), parent -> new ArrayList<>()).add(unit);
              }
              pushBody(fileBodies, statement);
              List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
                      ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
              for (AbstractCodeFragment fragment : fragments) {
                for (VariableDeclaration declaration : fragment.getVariableDeclarations()) {
                  LocationInfo declaring = declaringFragments.get(declaration);
                  if (declaring == null || declaring.subsumes(fragment.getLocationInfo())) {
                    declaringFragments.put(declaration, fragment.getLocationInfo());
                  }
                }
              }
              if (!(statement instanceof CompositeStatementObject)) {
                // The body of a block lambda holds statements of its own; an expression lambda is a part of the
                // expression of the statement, as a step of a chain mapping each element
                List<LocationInfo> later = new ArrayList<>();
                statement.getLambdas().stream().filter(lambda -> lambda.getBody() != null).forEach(lambda -> later.add(lambda.getLocationInfo()));
                statement.getAnonymousClassDeclarations().forEach(anonymous -> later.add(anonymous.getLocationInfo()));
                // An anonymous class of an expression lambda runs apart too; RM gives it to the expression of the lambda
                statement.getLambdas().stream().filter(lambda -> lambda.getBody() == null && lambda.getExpression() != null)
                        .flatMap(lambda -> lambda.getExpression().getAnonymousClassDeclarations().stream())
                        .filter(anonymous -> later.stream().noneMatch(known -> known.equals(anonymous.getLocationInfo())))
                        .forEach(anonymous -> later.add(anonymous.getLocationInfo()));
                push(fileSimpleStatements, new SimpleStatement(statement.getLocationInfo(), statement.getLocationInfo().getCodeElementType(), later));
              }
            }
          }
        }
    }

    // A local not holding its type, as a variable of a declaration statement, is declared by the code declaring it
    Map<String, List<LocalDeclaration>> fileLocalDeclarations = localDeclarations.computeIfAbsent(srcDst, side -> new HashMap<>());
    declaringFragments.forEach((declaration, fragment) -> {
      UMLType type = declaration.getType();
      if (type == null || type.getLocationInfo() == null || !declaration.getLocationInfo().subsumes(type.getLocationInfo())) {
        push(fileLocalDeclarations, new LocalDeclaration(declaration.getLocationInfo(), fragment));
      }
    });

    // A comment of a body is a unit of it, as a statement is
    for (UMLComment comment : bodyComments) {
      Unit unit = new Unit(comment.getLocationInfo(), comment.getParent(), null);
      fileUnits.computeIfAbsent(comment.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(unit);
      childUnits.computeIfAbsent(comment.getParent(), parent -> new ArrayList<>()).add(unit);
    }
  }

  private static <T extends Located> void push(Map<String, List<T>> byFile, T element) {
    byFile.computeIfAbsent(element.location().getFilePath(), file -> new ArrayList<>()).add(element);
  }

  private static void pushLocation(Map<String, List<LocationInfo>> byFile, Set<String> seen, LocationInfo location) {
    if (location != null && seen.add(location.getFilePath() + ":" + location.getStartOffset() + ":" + location.getEndOffset())) {
      byFile.computeIfAbsent(location.getFilePath(), file -> new ArrayList<>()).add(location);
    }
  }

  private static void pushLeadingParts(Map<String, List<LocationInfo>> byFile, @Nullable UMLJavadoc javadoc,
                                       List<UMLAnnotation> annotations) {
    if (javadoc != null && javadoc.getLocationInfo() != null) {
      byFile.computeIfAbsent(javadoc.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(javadoc.getLocationInfo());
    }
    for (UMLAnnotation annotation : annotations) {
      byFile.computeIfAbsent(annotation.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(annotation.getLocationInfo());
    }
  }

  // A block a composite runs is printed from the composite; one of a block or of a switch (a statement among the others
  // of its cases), or following another block of its composite (an else), and a finally block, are printed as they are
  private static void pushBody(Map<String, List<Body>> fileBodies, AbstractStatement statement) {
    LocationInfo.CodeElementType type = statement.getLocationInfo().getCodeElementType();
    if (!(statement instanceof CompositeStatementObject block)) {
      return;
    }
    if (type.equals(LocationInfo.CodeElementType.FINALLY_BLOCK)) {
      push(fileBodies, body(block, null, false, false, List.of(), block.getAllStatements()));
      return;
    }
    if (!type.equals(LocationInfo.CodeElementType.BLOCK)) {
      return;
    }

    CompositeStatementObject parent = statement.getParent();
    boolean followsBlock = parent != null && parent.getStatements().stream()
            .takeWhile(sibling -> sibling != statement)
            .anyMatch(sibling -> sibling.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK));
    boolean asIs = parent == null || parent.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
            || parent.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_STATEMENT) || followsBlock;
    push(fileBodies, asIs ? body(block, null, false, false, List.of(), block.getAllStatements())
            : body(block, parent.getLocationInfo(), false, false, heading(parent), statementsWithin(parent)));
  }

  private static void pushDocComment(Map<String, List<LocationInfo>> byFile, @Nullable UMLAbstractDocumentation doc) {
    if (doc != null && doc.getLocationInfo() != null) {
      byFile.computeIfAbsent(doc.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(doc.getLocationInfo());
    }
  }

  // The parts of a statement running as the statement itself does: the condition of a conditional, the selector of a
  // switch, the iterable of a loop over it and the declaration of a catch clause. The conditions of the other loops run
  // in the loop
  private static final Set<LocationInfo.CodeElementType> RUNNING_ON_CONDITION = EnumSet.of(LocationInfo.CodeElementType.IF_STATEMENT,
          LocationInfo.CodeElementType.SWITCH_STATEMENT, LocationInfo.CodeElementType.WHEN_STATEMENT,
          LocationInfo.CodeElementType.CATCH_CLAUSE, LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT);
  private static final Set<LocationInfo.CodeElementType> RUNNING_IN_LOOP = EnumSet.of(LocationInfo.CodeElementType.FOR_STATEMENT,
          LocationInfo.CodeElementType.WHILE_STATEMENT, LocationInfo.CodeElementType.DO_STATEMENT);

  // The code of a container running a part of itself on a condition, in a loop or later: conditional and loop
  // statements, conditional expressions and the container itself when it is a lambda
  private static void pushRunScopes(Map<String, List<RunScope>> scopes, Set<String> located, VariableDeclarationContainer container) {
    if (container instanceof LambdaExpressionObject) {
      pushRunScope(scopes, located, container.getLocationInfo(), List.of(), true);
    }

    List<AbstractCodeFragment> fragments = new ArrayList<>();
    if (container instanceof UMLAttribute attribute) {
      // The code of an attribute is its initializer (its body is the one of a lambda initializing it)
      if (attribute.getVariableDeclaration().getInitializer() != null) {
        fragments.add(attribute.getVariableDeclaration().getInitializer());
      }
    } else if (container.getBody() != null) {
      for (AbstractStatement statement : container.getBody().getCompositeStatement().getAllStatements()) {
        if (statement instanceof CompositeStatementObject composite) {
          LocationInfo.CodeElementType type = composite.getLocationInfo().getCodeElementType();
          if (RUNNING_ON_CONDITION.contains(type)) {
            List<LocationInfo> sameRunParts = new ArrayList<>(composite.getExpressions().stream()
                    .map(AbstractCodeFragment::getLocationInfo).toList());
            // The else-if branch of an if is the next branch of one chain, which runs on its own condition: an if added
            // to or removed from a chain guards the branches after it as an if returning early guards the code after it
            if (type == LocationInfo.CodeElementType.IF_STATEMENT && composite.getStatements().size() == 2
                    && composite.getStatements().get(1).getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.IF_STATEMENT) {
              sameRunParts.add(composite.getStatements().get(1).getLocationInfo());
            }
            pushRunScope(scopes, located, composite.getLocationInfo(), sameRunParts, false);
          } else if (RUNNING_IN_LOOP.contains(type)) {
            pushRunScope(scopes, located, composite.getLocationInfo(), List.of(), false);
          }
          fragments.addAll(composite.getExpressions());
        } else {
          fragments.add(statement);
        }
      }
    }
    if (container.getDefaultExpression() != null) {
      fragments.add(container.getDefaultExpression());
    }

    for (AbstractCodeFragment fragment : fragments) {
      for (TernaryOperatorExpression ternary : fragment.getTernaryOperatorExpressions()) {
        pushRunScope(scopes, located, ternary.getLocationInfo(), List.of(ternary.getCondition().getLocationInfo()), false);
      }
    }
  }

  private static void pushRunScope(Map<String, List<RunScope>> scopes, Set<String> located, LocationInfo location,
                                   List<LocationInfo> sameRunParts, boolean later) {
    if (located.add(location.getFilePath() + ":" + location.getStartOffset() + ":" + location.getEndOffset())) {
      scopes.computeIfAbsent(location.getFilePath(), file -> new ArrayList<>()).add(new RunScope(location.getStartOffset(),
              location.getEndOffset(), sameRunParts, later));
    }
  }

  // Code running a part of itself on a condition, in a loop or later (a lambda or an anonymous class, which runs its
  // whole body later), but the parts running as the code itself does, or as the code after it (an else-if branch)
  record RunScope(int start, int end, List<LocationInfo> sameRunParts, boolean later) {
    // Whether the code runs the code at the range differently, below itself
    boolean holdsBelow(int pos, int endPos) {
      return start <= pos && endPos <= end && (start != pos || end != endPos) && sameRunParts.stream()
              .noneMatch(part -> part.getStartOffset() <= pos && endPos <= part.getEndOffset());
    }
  }

  // The code of the file running a part of itself on a condition, in a loop or later
  List<RunScope> runScopes(SrcDst srcDst, String path) {
    ensureIndexed(srcDst, path);
    return runScopes.get(srcDst).getOrDefault(path, List.of());
  }

  // Whether the variable is a parameter of a lambda of the file
  boolean isLambdaParameter(SrcDst srcDst, String path, VariableDeclaration variable) {
    ensureIndexed(srcDst, path);
    return lambdaParameters.get(srcDst).contains(variable);
  }

  // The code declaring the local the tree is, when the tree does not hold its type, or the tree itself
  Tree declaringTree(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    Tree declaration = declarationOf(srcDst, path, tree);
    return declaration == null ? tree : declaration;
  }

  @Nullable
  private Tree declarationOf(SrcDst srcDst, String path, Tree tree) {
    Tree declaration = null;
    for (LocalDeclaration local : localDeclarations.get(srcDst).getOrDefault(path, List.of())) {
      if (local.location().getStartOffset() == tree.getPos() && local.location().getEndOffset() == tree.getEndPos()) {
        declaration = outermostWithin(tree, local.declaring());
      }
    }
    return declaration;
  }

  private interface Located {
    LocationInfo location();
  }

  // A body, with the code it is the body of; declaration when that code is an operation it is printed from the signature
  // of, member when it is the body of a member (an operation or an initializer) rather than of a lambda or a statement;
  // headEnd the last offset of the header of that code (see headEnd), closed whether that code ends with code of its own
  // after its statements (see closes)
  private record Body(LocationInfo location, @Nullable LocationInfo owner, boolean declaration, boolean member, int headEnd,
                      boolean closed) implements Located {
  }

  // The body, with the code it is the body of (none when it is printed as it is), the code RM gives of the header of that
  // code (see headEnd) and the statements RM gives within that code (see closes)
  private static Body body(CompositeStatementObject body, @Nullable LocationInfo owner, boolean declaration, boolean member,
                           List<LocationInfo> heading, List<AbstractStatement> within) {
    LocationInfo code = owner == null ? body.getLocationInfo() : owner;
    return new Body(body.getLocationInfo(), owner, declaration, member, headEnd(body, code.getStartOffset(), heading),
            closes(code, within));
  }

  /**
   * The last offset of the header of the code a body is the body of, as the code before the body prints it, the end
   * included: the start of the body when code of the body's own opens it before its statements (the brace opening a
   * block); otherwise the end of the code RM gives of the header (the parameters and the return type of an operation,
   * the parameters of a lambda, the condition of a statement) or the start of the code. A body indented under its header
   * (Python) starts with its first statement, whose line is no line of the header.
   */
  private static int headEnd(CompositeStatementObject body, int codeStart, List<LocationInfo> heading) {
    int bodyStart = body.getLocationInfo().getStartOffset();
    List<AbstractStatement> statements = body.getStatements();
    if (statements.isEmpty() || bodyStart < statements.get(0).getLocationInfo().getStartOffset()) {
      return bodyStart;
    }
    int end = codeStart;
    for (LocationInfo part : heading) {
      if (part != null && part.getEndOffset() <= bodyStart) {
        end = Math.max(end, part.getEndOffset() - 1);
      }
    }
    return end;
  }

  // The code RM gives of the header of the operation: its parameters and its return type
  private static List<LocationInfo> heading(UMLOperation operation) {
    List<LocationInfo> heading = new ArrayList<>();
    operation.getParameterDeclarationList().forEach(parameter -> heading.add(parameter.getLocationInfo()));
    UMLParameter returned = operation.getReturnParameter();
    if (returned != null && returned.getType() != null) {
      heading.add(returned.getType().getLocationInfo());
      heading.add(returned.getType().getLocationInfo());
    }
    return heading;
  }

  // The code RM gives of the header of the composite statement: its conditions, resources, declarations
  private static List<LocationInfo> heading(CompositeStatementObject composite) {
    return composite.getExpressions().stream().map(AbstractCodeFragment::getLocationInfo).toList();
  }

  // The last offset of the signature of the operation before its body (see headEnd); the whole operation when it has none
  private static int signatureEnd(UMLOperation operation) {
    LocationInfo location = operation.getLocationInfo();
    return operation.getBody() == null ? location.getEndOffset() - 1
            : headEnd(operation.getBody().getCompositeStatement(), location.getStartOffset(), heading(operation));
  }

  /**
   * Whether the code ends with code of its own after the statements RM gives within it, as the brace closing a block,
   * whose line tells where the code ends: no simple statement within it ends where it ends. A body indented under its
   * header (Python), or the branch of an if without braces, ends with its last statement, whose own line is no line
   * closing the code.
   */
  private static boolean closes(LocationInfo code, Collection<AbstractStatement> within) {
    return within.stream().noneMatch(statement -> !(statement instanceof CompositeStatementObject)
            && statement.getLocationInfo().getEndOffset() == code.getEndOffset());
  }

  // The line closing the code, as the range of its last offset (see closes); none when the code has no code closing it
  private static List<int[]> closing(LocationInfo code, Collection<AbstractStatement> within) {
    return closes(code, within) ? List.of(new int[]{code.getEndOffset() - 1, code.getEndOffset() - 1}) : List.of();
  }

  // The line closing the composite statement (see closing)
  private static List<int[]> closing(CompositeStatementObject composite) {
    return closing(composite.getLocationInfo(), statementsWithin(composite));
  }

  // The line closing the operation (see closing): an operation of no body, as an abstract one, ends its signature
  private static List<int[]> closing(UMLOperation operation) {
    return closing(operation.getLocationInfo(), operation.getBody() == null ? List.of()
            : operation.getBody().getCompositeStatement().getAllStatements());
  }

  // The statements RM gives within the composite statement: its own, and those of the catch clauses and the finally block
  // of a try, which RM puts next to it
  private static List<AbstractStatement> statementsWithin(CompositeStatementObject composite) {
    List<AbstractStatement> within = new ArrayList<>(composite.getAllStatements());
    if (composite instanceof TryStatementObject tryStatement) {
      List<CompositeStatementObject> handlers = new ArrayList<>(tryStatement.getCatchClauses());
      if (tryStatement.getFinallyClause() != null) {
        handlers.add(tryStatement.getFinallyClause());
      }
      for (CompositeStatementObject handler : handlers) {
        within.add(handler);
        within.addAll(handler.getAllStatements());
      }
    }
    return within;
  }

  /**
   * The code of the body after the changes that what they write reaches by name, as offsets of its statements (a composite
   * by its header): a statement reading a name a change declares, assigns or calls a method on (a forward effect slice,
   * see the TODO on data and control flow in HunkNetwork), and in turn one reading a name such a statement declares,
   * assigns or calls on. It follows values through the state of the objects the code calls on, which the reaching
   * definitions of locals do not: the decRef of the refs a moved incRef takes (es_110118), the call of the code under
   * test with the properties a changed parameter map was set on (spring_40493). Names are compared by the local or the
   * field each refers to by RM's rule (see DataFlow#referredName); what the changes only read seeds nothing, as code
   * reading the same value is not affected by the change.
   */
  List<int[]> effectParts(SrcDst srcDst, String path, Tree body, List<int[]> changes) {
    ensureIndexed(srcDst, path);
    List<AbstractStatement> statements = new ArrayList<>();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && body.getPos() <= unit.location().getStartOffset() && unit.location().getEndOffset() <= body.getEndPos()
              && !unit.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
        statements.add(unit.statement());
      }
    }
    statements.sort(Comparator.comparingInt(statement -> statement.getLocationInfo().getStartOffset()));
    Set<String> tracked = new HashSet<>();
    int first = Integer.MAX_VALUE;
    for (int[] change : changes) {
      first = Math.min(first, change[0]);
      for (AbstractStatement statement : statements) {
        for (AbstractCodeFragment fragment : ownCode(statement)) {
          LocationInfo location = fragment.getLocationInfo();
          if (location.getStartOffset() < change[1] && change[0] < location.getEndOffset()) {
            tracked.addAll(writtenNames(fragment, change[0], change[1]));
          }
        }
      }
    }
    List<int[]> parts = new ArrayList<>();
    if (tracked.isEmpty()) {
      return parts;
    }
    for (AbstractStatement statement : statements) {
      if (statement.getLocationInfo().getStartOffset() < first) {
        continue;
      }
      for (AbstractCodeFragment fragment : ownCode(statement)) {
        if (readNames(fragment).stream().noneMatch(tracked::contains)) {
          continue;
        }
        LocationInfo location = fragment.getLocationInfo();
        parts.add(new int[]{Math.min(statement.getLocationInfo().getStartOffset(), location.getStartOffset()), location.getEndOffset() - 1});
        tracked.addAll(writtenNames(fragment, location.getStartOffset(), location.getEndOffset()));
      }
    }
    return parts;
  }

  /**
   * The definitions of the locals the changes read, and in turn of the locals those definitions read, within the body, as
   * offsets of their statements (a composite by its header): a backward slice over the reaching definitions (see
   * {@link #definitionsReaching}). A changed assertion of a test reads with the input it checks, as the query an expected
   * plan is the plan of (es_115624), and a changed statement of a member with how the values it takes are computed there.
   */
  List<int[]> definitionParts(SrcDst srcDst, String path, Tree body, List<int[]> changes) {
    ensureIndexed(srcDst, path);
    Deque<AbstractStatement> pending = new ArrayDeque<>();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && body.getPos() <= unit.location().getStartOffset() && unit.location().getEndOffset() <= body.getEndPos()
              && !unit.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
              && ownCode(unit.statement()).stream().anyMatch(fragment -> changes.stream().anyMatch(change ->
              fragment.getLocationInfo().getStartOffset() < change[1] && change[0] < fragment.getLocationInfo().getEndOffset()))) {
        pending.add(unit.statement());
      }
    }
    Set<AbstractStatement> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    visited.addAll(pending);
    List<int[]> parts = new ArrayList<>();
    while (!pending.isEmpty()) {
      AbstractStatement reader = pending.poll();
      for (AbstractCodeFragment fragment : ownCode(reader)) {
        for (String name : localNames(fragment)) {
          VariableDeclaration local = fragment.searchVariableDeclaration(name);
          if (local == null) {
            continue;
          }
          for (AbstractStatement definition : definitionsReaching(srcDst, reader, local)) {
            LocationInfo location = definition.getLocationInfo();
            if (location.getStartOffset() < body.getPos() || body.getEndPos() < location.getEndOffset() || !visited.add(definition)) {
              continue;
            }
            for (AbstractCodeFragment own : ownCode(definition)) {
              parts.add(new int[]{Math.min(location.getStartOffset(), own.getLocationInfo().getStartOffset()), own.getLocationInfo().getEndOffset() - 1});
            }
            pending.add(definition);
          }
        }
      }
    }
    return parts;
  }

  // The code of the statement itself: a leaf statement, or the header of a composite statement
  private static List<AbstractCodeFragment> ownCode(AbstractStatement statement) {
    return statement instanceof CompositeStatementObject composite ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
  }

  // The names RM gives in the fragment, by the local or the field each refers to (see DataFlow#referredName), with the
  // object itself (this, self) when RM gives it as the receiver of a call
  private static Set<String> givenNames(AbstractCodeFragment fragment) {
    Set<String> names = new HashSet<>();
    fragment.getVariables().forEach(variable -> names.add(DataFlow.referredName(variable.getString(), fragment.getLANG())));
    for (AbstractCall call : fragment.getMethodInvocations()) {
      if (call.getExpression() != null && call.getExpression().trim().equals(fragment.getLANG().THIS)) {
        names.add(fragment.getLANG().THIS);
      }
    }
    return names;
  }

  // The names the fragment reads: the names RM gives in it (see givenNames)
  private static Set<String> readNames(AbstractCodeFragment fragment) {
    return givenNames(fragment);
  }

  // The locals the names RM gives in the fragment (its variables and the receivers of its calls) may refer to (see
  // DataFlow#referredLocal)
  private static Set<String> localNames(AbstractCodeFragment fragment) {
    Set<String> names = new LinkedHashSet<>();
    fragment.getVariables().forEach(variable -> names.add(DataFlow.referredLocal(variable.getString())));
    fragment.getMethodInvocations().stream().map(AbstractCall::getExpression).filter(Objects::nonNull)
            .forEach(receiver -> names.add(DataFlow.referredLocal(receiver)));
    return names;
  }

  // The names the code of the fragment overlapping the offsets writes: the variables it declares (a changed initializer
  // gives the variable a new value), the targets of its assignments and the receivers of its calls, whose state a call
  // may change, each by the name RM gives of it (see givenNames)
  private static Set<String> writtenNames(AbstractCodeFragment fragment, int start, int end) {
    Set<String> names = new HashSet<>();
    Set<String> given = givenNames(fragment);
    Predicate<LocationInfo> within = location -> location.getStartOffset() < end && start < location.getEndOffset();
    for (VariableDeclaration declaration : fragment.getVariableDeclarations()) {
      if (within.test(declaration.getLocationInfo())) {
        names.add(declaration.getVariableName());
      }
    }
    for (LeafExpression assignment : fragment.getAssignments()) {
      if (!within.test(assignment.getLocationInfo())) {
        continue;
      }
      for (LeafExpression variable : fragment.getVariables()) {
        if (variable.getLocationInfo().getStartOffset() == assignment.getLocationInfo().getStartOffset()) {
          names.add(DataFlow.referredName(variable.getString(), fragment.getLANG()));
        }
      }
    }
    for (AbstractCall call : fragment.getMethodInvocations()) {
      if (within.test(call.getLocationInfo()) && call.getExpression() != null) {
        String receiver = DataFlow.referredName(call.getExpression(), fragment.getLANG());
        if (given.contains(receiver)) {
          names.add(receiver);
        }
      }
    }
    return names;
  }

  /** Whether the tree lies in the doc comment or the annotations of a declaration. */
  boolean isInLeadingPart(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    return leadingParts.get(srcDst).getOrDefault(path, List.of()).stream().anyMatch(part -> holds(part, tree));
  }

  /** Whether RM gives the tree as a composite statement, as an if, a loop or a try. */
  boolean isCompositeStatement(SrcDst srcDst, String path, Tree tree) {
    return statementAt(srcDst, path, tree) instanceof CompositeStatementObject;
  }

  /** The comments of the file as RM gives them, by their offsets (both ends included). */
  List<int[]> comments(SrcDst srcDst, String path) {
    ensureIndexed(srcDst, path);
    return comments.get(srcDst).getOrDefault(path, List.of()).stream()
            .map(comment -> new int[]{comment.getStartOffset(), comment.getEndOffset() - 1}).toList();
  }

  /** The catch clauses of the file, those of its lambdas and anonymous classes included. */
  List<CompositeStatementObject> catchClauses(SrcDst srcDst, String path) {
    ensureIndexed(srcDst, path);
    List<CompositeStatementObject> clauses = new ArrayList<>();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() instanceof CompositeStatementObject clause
              && unit.location().getCodeElementType() == LocationInfo.CodeElementType.CATCH_CLAUSE) {
        clauses.add(clause);
      }
    }
    return clauses;
  }

  /** Whether the tree is the body of a member of its class: an operation or an initializer. */
  boolean isMemberBody(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    return bodies.get(srcDst).getOrDefault(path, List.of()).stream().anyMatch(body -> body.member()
            && body.location().getStartOffset() == tree.getPos() && body.location().getEndOffset() == tree.getEndPos());
  }

  private record LocalDeclaration(LocationInfo location, LocationInfo declaring) implements Located {
  }

  // A leaf statement or a field declaration, with the lambdas and the anonymous classes within it
  private record SimpleStatement(LocationInfo location, @Nullable LocationInfo.CodeElementType type, List<LocationInfo> later)
          implements Located {
  }

  // The classes of the file and the anonymous classes and the classes their operations hold, as the operations
  // nested in operations
  private static List<UMLAbstractClass> contextsOf(UMLModel umlModel, String path) {
    List<UMLAbstractClass> contexts = new ArrayList<>();
    Set<UMLAbstractClass> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<UMLAbstractClass> pending = new ArrayDeque<>();
    umlModel.getClassList().stream().filter(umlClass -> umlClass.getLocationInfo().getFilePath().equals(path)).forEach(pending::add);
    while (!pending.isEmpty()) {
      UMLAbstractClass context = pending.poll();
      if (!context.getLocationInfo().getFilePath().equals(path) || !seen.add(context)) {
        continue;
      }
      contexts.add(context);
      pending.addAll(context.getAnonymousClassList());
      context.getAttributes().forEach(attribute -> pending.addAll(attribute.getAnonymousClassList()));
      for (UMLOperation operation : operationsOf(context)) {
        pending.addAll(operation.getNestedClasses());
        pending.addAll(operation.getAnonymousClassList());
      }
    }
    return contexts;
  }

  private static List<UMLOperation> operationsOf(UMLAbstractClass context) {
    List<UMLOperation> operations = new ArrayList<>();
    Deque<UMLOperation> pending = new ArrayDeque<>(context.getOperations());
    while (!pending.isEmpty()) {
      UMLOperation operation = pending.poll();
      operations.add(operation);
      pending.addAll(operation.getNestedOperations());
    }
    return operations;
  }

  // A class, an operation, an attribute, a variable or a parameter (of its operation), with the ranges of its code:
  // its location and the location trimmed of the whitespace around it
  private record Declared(Set<Long> ranges, Object element, @Nullable UMLOperation operation) {
  }

  private static Set<Long> ranges(List<LocationInfo> locations, @Nullable String content) {
    Set<Long> ranges = new HashSet<>();
    for (LocationInfo location : locations) {
      ranges.add(range(location.getStartOffset(), location.getEndOffset()));
      if (content != null) {
        String trimmed = content.substring(location.getStartOffset(), location.getEndOffset()).trim();
        int start = content.indexOf(trimmed, location.getStartOffset());
        ranges.add(range(start, start + trimmed.length()));
      }
    }
    return ranges;
  }

  private static long range(int start, int end) {
    return ((long) start << 32) | (end & 0xffffffffL);
  }

  // The classes, operations, attributes, variables and parameters the code declares: the ones the tree is the code of,
  // or any tree within it is when the tree is a change
  UMLs umls(SrcDst srcDst, String path, Tree tree, boolean context) {
    ensureIndexed(srcDst, path);
    Set<Long> trees = new HashSet<>();
    trees.add(range(tree.getPos(), tree.getEndPos()));
    if (!context) {
      tree.getDescendants().forEach(descendant -> trees.add(range(descendant.getPos(), descendant.getEndPos())));
    }

    UMLs umls = new UMLs();
    for (Declared declaration : declared.get(srcDst).getOrDefault(path, List.of())) {
      if (declaration.ranges().stream().noneMatch(trees::contains)) {
        continue;
      }
      if (declaration.element() instanceof UMLClass umlClass) {
        umls.umlClasses.add(umlClass);
      } else if (declaration.element() instanceof UMLOperation operation) {
        umls.umlOperations.add(operation);
      } else if (declaration.element() instanceof UMLAttribute attribute) {
        umls.umlAttributes.add(attribute);
      } else if (declaration.operation() != null) {
        umls.operationParameters.computeIfAbsent(declaration.operation(), operation -> new HashSet<>())
                .add((VariableDeclaration) declaration.element());
      } else {
        umls.variableDeclarations.add((VariableDeclaration) declaration.element());
      }
    }
    return umls;
  }

  // A statement or a comment RM gives in a body, with the composite holding it
  private record Unit(LocationInfo location, @Nullable CompositeStatementObject parent, @Nullable AbstractStatement statement) {
  }

  private static final Set<LocationInfo.CodeElementType> BODIES = EnumSet.of(LocationInfo.CodeElementType.BLOCK,
          LocationInfo.CodeElementType.TRY_STATEMENT, LocationInfo.CodeElementType.CATCH_CLAUSE,
          LocationInfo.CodeElementType.FINALLY_BLOCK);

  // The statement of a body holding the tree: the innermost statement or comment holding it (a block is a body, not a
  // statement), or the statement holding that one when its composite does not hold it as a body, as the statement a
  // conditional runs without a block. A catch clause or a finally block is a part of its try, and a statement RM gives
  // within another statement of the body is a part of that one, as the switch expression a return returns
  @Nullable
  private Unit statementOf(SrcDst srcDst, String path, Tree tree) {
    Map<AbstractStatement, Unit> sideUnits = statementUnits.getOrDefault(srcDst, Map.of());
    Unit unit = null;
    for (Unit candidate : units.get(srcDst).getOrDefault(path, List.of())) {
      if (holds(candidate.location(), tree) && !candidate.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
              && (unit == null || unit.location().subsumes(candidate.location()))) {
        unit = candidate;
      }
    }
    if (unit == null) {
      return null;
    }

    while (unit.parent() != null && !BODIES.contains(unit.parent().getLocationInfo().getCodeElementType())
            && sideUnits.containsKey(unit.parent())) {
      unit = sideUnits.get(unit.parent());
    }
    // RM leaves the try of any other composite unset
    if (unit.statement() instanceof CompositeStatementObject composite && composite.getTryContainer() != null
            && composite.getTryContainer().isPresent()
            && sideUnits.containsKey(composite.getTryContainer().get())) {
      unit = sideUnits.get(composite.getTryContainer().get());
    }
    if (unit.parent() != null) {
      for (AbstractStatement sibling : unit.parent().getStatements()) {
        LocationInfo siblingLocation = sibling.getLocationInfo();
        if (sibling != unit.statement() && siblingLocation.subsumes(unit.location()) && !unit.location().subsumes(siblingLocation)
                && sideUnits.containsKey(sibling)) {
          unit = sideUnits.get(sibling);
        }
      }
    }
    return unit;
  }

  private static void pushComment(Map<String, List<LocationInfo>> fileComments, Map<String, Set<Integer>> fileBlankLines,
                                  Set<String> seen, UMLComment comment) {
    LocationInfo location = comment.getLocationInfo();
    if (seen.add(location.getFilePath() + ":" + location.getStartOffset() + ":" + location.getEndOffset())) {
      fileComments.computeIfAbsent(location.getFilePath(), file -> new ArrayList<>()).add(location);
      // RM gives the text of a comment without its markers, a line of text for each line of the comment
      String[] text = comment.getText().split("\n", -1);
      Set<Integer> blank = fileBlankLines.computeIfAbsent(location.getFilePath(), file -> new HashSet<>());
      for (int line = location.getStartLine(); line <= location.getEndLine() && line - location.getStartLine() < text.length; line++) {
        if (text[line - location.getStartLine()].isBlank()) {
          blank.add(line);
        }
      }
    }
  }

  /** The lines of the comments of the file holding nothing but the markers of a comment, as RM gives their text. */
  Set<Integer> blankCommentLines(SrcDst srcDst, String path) {
    ensureIndexed(srcDst, path);
    return blankCommentLines.get(srcDst).getOrDefault(path, Set.of());
  }

  /** The package declaration and the imports of the file, as RM gives them with the classes of the file. */
  List<LocationInfo> fileParts(SrcDst srcDst, String path) {
    ensureIndexed(srcDst, path);
    return fileParts.get(srcDst).getOrDefault(path, List.of());
  }

  /** Whether the tree declares a class RM gives: a class, an interface, an enum, a record or an annotation type. */
  boolean declaresClass(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    UMLClass umlClass = innermost(classes.get(srcDst).getOrDefault(path, List.of()), tree, UMLClass::getLocationInfo, false);
    return umlClass != null && isDeclaredBy(tree, umlClass.getLocationInfo());
  }

  RMInfo locate(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    Unit statement = statementOf(srcDst, path, tree);
    UMLOperation operation = innermost(operations.get(srcDst).getOrDefault(path, List.of()), tree,
            UMLOperation::getLocationInfo, false);
    UMLClass umlClass = innermost(classes.get(srcDst).getOrDefault(path, List.of()), tree, UMLClass::getLocationInfo, false);
    UMLOperation declaredOperation = operation != null && isDeclaredBy(tree, operation.getLocationInfo()) ? operation : null;
    UMLClass declaredClass = umlClass != null && isDeclaredBy(tree, umlClass.getLocationInfo()) ? umlClass : null;

    // What a tree declares does not hold it
    UMLOperation holdingOperation = declaredOperation == null ? operation
            : innermost(operations.get(srcDst).getOrDefault(path, List.of()), tree, UMLOperation::getLocationInfo, true);
    UMLClass holdingClass = declaredClass == null ? umlClass
            : innermost(classes.get(srcDst).getOrDefault(path, List.of()), tree, UMLClass::getLocationInfo, true);

    List<Tree> header = declaredOperation != null ? operationHeader(declaredOperation, tree)
            : declaredClass != null ? classHeader(srcDst, declaredClass, tree) : List.of();
    List<Tree> typeHeader = holdingClass == null ? List.of()
            : classHeader(srcDst, holdingClass, outermostWithin(tree, holdingClass.getLocationInfo()));
    List<LocationInfo> leading = leadingParts.get(srcDst).getOrDefault(path, List.of());
    Tree declarationTree = declarationOf(srcDst, path, tree);
    BodyStart bodyStart = bodyStart(srcDst, path, tree, leading);
    return new RMInfo(statement == null ? null : statement.statement(), statement == null ? null : outermostWithin(tree, statement.location()),
            holdingOperation, holdingClass, typeHeader, declaredOperation, declaredClass, header, signatureStart(tree, leading),
            bodyStart != null, bodyStart == null ? null : bodyStart.from(), simpleStatement(srcDst, path, tree),
            declarationTree, fileParts.get(srcDst).getOrDefault(path, List.of()));
  }

  // The innermost statement RM holds the tree in, the composite statement itself for a tree of its condition; none for
  // a tree of no body, as an initializer of a field
  @Nullable
  Tree innermostStatement(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    Unit innermost = null;
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && holds(unit.location(), tree)
              && !unit.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
              && (innermost == null || innermost.location().subsumes(unit.location()))) {
        innermost = unit;
      }
    }
    return innermost == null ? null : outermostWithin(tree, innermost.location());
  }

  // The statement RM gives at the tree, if any
  @Nullable
  AbstractStatement statementAt(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && isDeclaredBy(tree, unit.location())) {
        return unit.statement();
      }
    }
    return null;
  }

  /**
   * The code holding the tree in the member it lies in, as RM nests it, telling where and when the tree runs: for each
   * statement holding it, its header (its code before its body: a condition, a resource list, the declaration of a catch
   * clause) or, for a simple statement holding a lambda or an anonymous class, its start; for each body holding it, the
   * code it is the body of up to the body (the signature of an operation, the parameters of a lambda, an initializer);
   * and the end of each of them. The member is the outermost operation, initializer or field declaration holding the
   * tree, or the tree itself when it is a field declaration. The catch clauses and the finally block of a try holding the
   * tree are part of that code too, as they handle what it throws. The bodies of the lambdas and anonymous classes of that
   * code which do not hold the tree are left out, but their first and last lines.
   *
   * @return the member, the ranges of offsets of that code, both ends included, and the bodies left out
   */
  Node.Enclosing enclosing(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    int start = tree.getPos();
    int end = tree.getEndPos();
    List<int[]> parts = new ArrayList<>();
    int[] member = {start, end};
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo location = unit.location();
      if (unit.statement() == null || !holdsStrictly(location, start, end)) {
        continue;
      }
      if (unit.statement() instanceof CompositeStatementObject composite) {
        parts.add(header(composite));
        parts.addAll(closing(composite));
      }
      else {
        parts.add(new int[]{location.getStartOffset(), location.getStartOffset()});
        parts.add(new int[]{location.getEndOffset() - 1, location.getEndOffset() - 1});
      }
      // What the code holding the tree throws is handled by the catch clauses of the try, and the finally block runs after it
      if (unit.statement() instanceof TryStatementObject tryStatement) {
        List<AbstractStatement> handlers = new ArrayList<>(tryStatement.getCatchClauses());
        if (tryStatement.getFinallyClause() != null) {
          handlers.add(tryStatement.getFinallyClause());
        }
        for (AbstractStatement handler : handlers) {
          parts.add(new int[]{handler.getLocationInfo().getStartOffset(), handler.getLocationInfo().getEndOffset() - 1});
        }
      }
    }
    // A statement of a switch runs on the case labels before it
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      CompositeStatementObject parent = unit.parent();
      if (unit.statement() == null || parent == null || start < unit.location().getStartOffset() || unit.location().getEndOffset() < end
              || parent.getLocationInfo().getCodeElementType() != LocationInfo.CodeElementType.SWITCH_STATEMENT) {
        continue;
      }
      List<AbstractStatement> siblings = parent.getStatements();
      int index = siblings.indexOf(unit.statement());
      while (index > 0 && siblings.get(index).getLocationInfo().getCodeElementType() != LocationInfo.CodeElementType.SWITCH_CASE) {
        index--;
      }
      while (index >= 0 && siblings.get(index).getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.SWITCH_CASE) {
        LocationInfo label = siblings.get(index).getLocationInfo();
        parts.add(new int[]{label.getStartOffset(), label.getEndOffset() - 1});
        index--;
      }
    }
    List<LocationInfo> leading = leadingParts.get(srcDst).getOrDefault(path, List.of());
    for (UMLOperation operation : operations.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo location = operation.getLocationInfo();
      if (!holdsStrictly(location, start, end)) {
        continue;
      }
      int signature = signatureStart(outermostWithin(tree, location), leading).getPos();
      parts.add(new int[]{signature, signatureEnd(operation)});
      parts.addAll(closing(operation));
      if (location.getStartOffset() <= member[0] && member[1] <= location.getEndOffset()) {
        member = new int[]{location.getStartOffset(), location.getEndOffset()};
      }
    }
    for (Body body : bodies.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo owner = body.owner();
      if (owner == null || body.declaration() || !holdsStrictly(owner, start, end)) {
        continue;
      }
      parts.add(new int[]{owner.getStartOffset(), body.headEnd()});
      if (body.closed()) {
        parts.add(new int[]{owner.getEndOffset() - 1, owner.getEndOffset() - 1});
      }
      if (owner.getStartOffset() <= member[0] && member[1] <= owner.getEndOffset()
              && initializers.get(srcDst).getOrDefault(path, List.of()).contains(owner)) {
        member = new int[]{owner.getStartOffset(), owner.getEndOffset()};
      }
    }
    for (UMLAbstractClass owner : owners.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo location = owner.getLocationInfo();
      if (owner instanceof UMLAnonymousClass && holdsStrictly(location, start, end)) {
        parts.add(new int[]{location.getStartOffset(), location.getStartOffset()});
        List<AbstractStatement> within = new ArrayList<>();
        owner.getOperations().stream().filter(operation -> operation.getBody() != null)
                .forEach(operation -> within.addAll(operation.getBody().getCompositeStatement().getAllStatements()));
        parts.addAll(closing(location, within));
      }
    }
    for (LocationInfo field : fieldDeclarations.get(srcDst).getOrDefault(path, List.of())) {
      if (field.getStartOffset() <= start && end <= field.getEndOffset()
              && field.getStartOffset() <= member[0] && member[1] <= field.getEndOffset()) {
        member = new int[]{field.getStartOffset(), field.getEndOffset()};
      }
    }
    // The bodies of the lambdas and anonymous classes of that code, but the one holding the tree, run apart from it
    List<int[]> hidden = new ArrayList<>();
    for (RunScope scope : runScopes(srcDst, path)) {
      if (scope.later() && !(scope.start() <= start && end <= scope.end())
              && parts.stream().anyMatch(part -> part[0] <= scope.start() && scope.end() - 1 <= part[1])) {
        hidden.add(new int[]{scope.start(), scope.end() - 1});
      }
    }
    return new Node.Enclosing(member[0], member[1], parts, hidden);
  }

  /**
   * The code of the statement holding uses (a usage) that tells how they run and what they decide, as {@link #enclosing}
   * gives it for each use: a simple statement (or a field declaration) holding them reads whole, but for the bodies of its
   * lambdas and anonymous classes holding none of them (the class body of an object a use creates is that object); so
   * does a composite statement running its body on a condition or in a loop, which the uses in its header decide. Another
   * composite statement holding them in its header (the resource list of a try) reads by its header and its end, its body
   * running apart from them.
   */
  Node.Enclosing usageEnclosing(SrcDst srcDst, String path, Tree statement, List<Tree> uses) {
    ensureIndexed(srcDst, path);
    List<int[]> parts = new ArrayList<>();
    List<int[]> hidden = new ArrayList<>();
    int[] member = {statement.getPos(), statement.getEndPos()};
    for (Tree use : uses) {
      Node.Enclosing enclosing = enclosing(srcDst, path, use);
      parts.addAll(enclosing.parts());
      hidden.addAll(enclosing.hidden());
      if (enclosing.memberStart() <= member[0] && member[1] <= enclosing.memberEnd()) {
        member = new int[]{enclosing.memberStart(), enclosing.memberEnd()};
      }
    }
    // A use runs only when no guard before it leaves (see ControlFlow#guardsOf), and on the values of the conditions
    // holding it, which their definitions tell (see conditionDefinitionParts)
    for (Tree use : uses) {
      AbstractStatement holding = statementHolding(srcDst, path, use);
      if (holding != null) {
        // A check bears on the use when it bears on the code the usage shows of it: its statement, the conditions holding
        // it, and the definitions of what those conditions read
        List<AbstractStatement> deciding = new ArrayList<>(List.of(holding));
        List<int[]> definitionParts = new ArrayList<>();
        for (AbstractStatement definition : conditionDefinitions(srcDst, path, holding)) {
          deciding.add(definition);
          definitionParts.addAll(pathParts(definition));
        }
        for (CompositeStatementObject holder = holding.getParent(); holder != null; holder = holder.getParent()) {
          if (DECIDING.contains(holder.getLocationInfo().getCodeElementType())) {
            deciding.add(holder);
          }
        }
        List<int[]> flowParts = new ArrayList<>(guardParts(srcDst, path, holding, List.of(new int[]{use.getPos(), use.getEndPos()}),
                valuesRead(srcDst, path, deciding), fieldsUsed(srcDst, path, deciding)));
        flowParts.addAll(definitionParts);
        // A use whose statement adds its value into a local (a compound assignment) reads with what else goes into it:
        // the readers that total reaches and the other definitions reaching them, as the capacities of the queued chunks
        // summed with the counts of the magazines into the memory in use
        if (!(holding instanceof CompositeStatementObject)) {
          for (VariableDeclaration local : accumulated(holding)) {
            for (AbstractStatement reader : readersReached(holding, local)) {
              if (logStatement.test(srcDst, reader)) {
                continue;
              }
              flowParts.addAll(pathParts(reader));
              for (AbstractStatement definition : definitionsReaching(reader, local)) {
                flowParts.addAll(pathParts(definition));
              }
            }
          }
        }
        parts.addAll(flowParts);
        hidden.addAll(laterBodiesWithin(srcDst, path, flowParts));
      }
    }
    boolean decides = runScopes(srcDst, path).stream().anyMatch(scope -> !scope.later() && scope.start() == statement.getPos()
            && scope.end() == statement.getEndPos());
    if (decides || !(statementAt(srcDst, path, statement) instanceof CompositeStatementObject)) {
      List<LocationInfo> leading = leadingParts.get(srcDst).getOrDefault(path, List.of());
      // A body run later holding uses, as the function a future runs on the value it completes with, reads by the code
      // of it holding them, as a member body does (see enclosing): the statement reads whole around it
      List<int[]> usingBodies = new ArrayList<>();
      for (RunScope scope : runScopes(srcDst, path)) {
        if (!scope.later() || scope.start() < statement.getPos() || statement.getEndPos() < scope.end()) {
          continue;
        }
        boolean holdsUse = uses.stream().anyMatch(use -> scope.start() <= use.getPos() && use.getEndPos() <= scope.end()
                && !(use.getPos() <= scope.start() && scope.end() == use.getEndPos()));
        boolean isUse = uses.stream().anyMatch(use -> use.getPos() <= scope.start() && scope.end() == use.getEndPos());
        if (holdsUse && !isUse && isBlockLambda(srcDst, path, statement, scope)) {
          usingBodies.add(new int[]{scope.start(), scope.end()});
          // A use in the condition of a statement in that body decides what the statement runs, which reads with it, as
          // the notification a positive count triggers
          for (Tree use : uses) {
            AbstractStatement holding = statementHolding(srcDst, path, use);
            if (holding instanceof CompositeStatementObject composite && scope.start() <= use.getPos() && use.getEndPos() <= scope.end()
                    && DECIDING.contains(composite.getLocationInfo().getCodeElementType())
                    && composite.getExpressions().stream().anyMatch(expression -> expression.getLocationInfo().getStartOffset() <= use.getPos()
                    && use.getEndPos() <= expression.getLocationInfo().getEndOffset())) {
              parts.add(new int[]{composite.getLocationInfo().getStartOffset(), composite.getLocationInfo().getEndOffset() - 1});
            }
          }
        } else if (!holdsUse && !isUse) {
          hidden.add(new int[]{scope.start(), scope.end() - 1});
        }
      }
      int from = signatureStart(statement, leading).getPos();
      usingBodies.sort(Comparator.comparingInt(body -> body[0]));
      for (int[] body : usingBodies) {
        if (body[0] < from) {
          continue;
        }
        // The line opening the body and the one closing it (when code of the lambda's own closes it, see closes) stay
        // with the statement
        parts.add(new int[]{from, body[0]});
        boolean closed = bodies.get(srcDst).getOrDefault(path, List.of()).stream().noneMatch(lambda -> lambda.owner() != null
                && lambda.owner().getStartOffset() == body[0] && lambda.owner().getEndOffset() == body[1] && !lambda.closed());
        from = closed ? body[1] - 1 : body[1];
      }
      parts.add(new int[]{from, statement.getEndPos() - 1});
    }
    return new Node.Enclosing(member[0], member[1], parts, hidden);
  }

  // The locals the statement adds a value into by a compound assignment (`sum += ...`, see DataFlow#accumulates): the
  // local holds a total of several contributions, which read together
  private static List<VariableDeclaration> accumulated(AbstractStatement statement) {
    List<VariableDeclaration> locals = new ArrayList<>();
    for (LeafExpression assignment : statement.getAssignments()) {
      for (LeafExpression variable : statement.getVariables()) {
        if (variable.getLocationInfo().getStartOffset() != assignment.getLocationInfo().getStartOffset()) {
          continue;
        }
        VariableDeclaration local = statement.searchVariableDeclaration(variable.getString());
        if (local != null && !local.isParameter() && !locals.contains(local) && DataFlow.accumulates(statement, local)) {
          locals.add(local);
        }
      }
    }
    return locals;
  }

  // Whether the scope is a lambda of the statement with a block body, whose statements read as a member body's do; an
  // expression lambda reads with its statement
  private boolean isBlockLambda(SrcDst srcDst, String path, Tree statement, RunScope scope) {
    AbstractStatement holding = statementAt(srcDst, path, statement);
    if (holding == null) {
      return false;
    }
    List<AbstractCodeFragment> fragments = new ArrayList<>(List.of(holding));
    if (holding instanceof CompositeStatementObject composite) {
      fragments.addAll(composite.getExpressions());
    }
    return fragments.stream().flatMap(fragment -> fragment.getLambdas().stream())
            .anyMatch(lambda -> lambda.getLocationInfo().getStartOffset() == scope.start()
                    && lambda.getLocationInfo().getEndOffset() == scope.end() && lambda.getBody() != null);
  }

  // The readers each definition of a local reaches, and the definitions reaching each reader (see DataFlow), kept as
  // they are asked for each usage and diff printed
  private final Map<AbstractStatement, Map<VariableDeclaration, List<AbstractStatement>>> reachedReaders = new IdentityHashMap<>();
  private final Map<AbstractStatement, Map<VariableDeclaration, List<AbstractStatement>>> reachingDefinitions = new IdentityHashMap<>();

  List<AbstractStatement> readersReached(AbstractStatement definition, VariableDeclaration local) {
    return reachedReaders.computeIfAbsent(definition, key -> new IdentityHashMap<>())
            .computeIfAbsent(local, key -> DataFlow.readersReached(definition, local));
  }

  // The definitions of the local in the body holding the reader (its member, or its lambda) that reach it
  List<AbstractStatement> definitionsReaching(AbstractStatement reader, VariableDeclaration local) {
    return reachingDefinitions.computeIfAbsent(reader, key -> new IdentityHashMap<>()).computeIfAbsent(local, key -> {
      CompositeStatementObject body = reader instanceof CompositeStatementObject composite ? composite : reader.getParent();
      while (body != null && body.getParent() != null) {
        body = body.getParent();
      }
      List<AbstractStatement> definitions = new ArrayList<>();
      for (AbstractStatement statement : body == null ? List.<AbstractStatement>of() : body.getAllStatements()) {
        if (DataFlow.writes(statement, local) && readersReached(statement, local).stream().anyMatch(reached -> reached == reader)) {
          definitions.add(statement);
        }
      }
      return definitions;
    });
  }

  /**
   * The definitions of the local reaching its read by the reader (see {@link #definitionsReaching}), or, for a local a
   * lambda or an anonymous class of the reader captures, its definitions in the body declaring it, those of its lambdas
   * and anonymous classes included: the code captured runs when it is called, not where it lies, so any of them may give
   * the value it reads, as the shard generations a failure handler passes on, computed before the listeners are built
   * (stage 5 of the TODO on data and control flow in HunkNetwork). A language requiring a captured local to be
   * effectively final (Java) has no write of it after its definition nor in the code capturing it; others (Kotlin,
   * TypeScript, Python) do. A parameter captured has no definition of its member.
   */
  List<AbstractStatement> definitionsReaching(SrcDst srcDst, AbstractStatement reader, VariableDeclaration local) {
    List<AbstractStatement> definitions = definitionsReaching(reader, local);
    if (!definitions.isEmpty()) {
      return definitions;
    }
    CompositeStatementObject body = topBody(reader);
    boolean declaredOutside = body != null && !body.getLocationInfo().subsumes(local.getLocationInfo());
    // A local read only within a lambda or an anonymous class of the reader, which its own code does not read
    if (!declaredOutside && DataFlow.reads(reader, local)) {
      return definitions;
    }
    AbstractStatement declaring = statementHolding(srcDst, local.getLocationInfo().getFilePath(), local.getLocationInfo());
    CompositeStatementObject declaringBody = declaring == null ? null : topBody(declaring);
    if (declaringBody == null) {
      return definitions;
    }
    List<AbstractStatement> captured = new ArrayList<>();
    String path = local.getLocationInfo().getFilePath();
    ensureIndexed(srcDst, path);
    LocationInfo declaringLocation = declaringBody.getLocationInfo();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      AbstractStatement statement = unit.statement();
      // A local of a lambda or an anonymous class of the same name hides the local captured
      VariableDeclaration named = statement == null ? null : statement.searchVariableDeclaration(local.getVariableName());
      if (statement != null && declaringLocation.subsumes(unit.location()) && (named == null || named == local)
              && DataFlow.writes(statement, local)) {
        captured.add(statement);
      }
    }
    captured.sort(Comparator.comparingInt(statement -> statement.getLocationInfo().getStartOffset()));
    return captured;
  }

  // The body of the member or the lambda holding the statement
  @Nullable
  private static CompositeStatementObject topBody(AbstractStatement statement) {
    CompositeStatementObject body = statement instanceof CompositeStatementObject composite ? composite : statement.getParent();
    while (body != null && body.getParent() != null) {
      body = body.getParent();
    }
    return body;
  }

  /** The innermost statement of the revision holding the code at the location (see {@link #statementHolding}). */
  @Nullable
  AbstractStatement statementHolding(SrcDst srcDst, LocationInfo location) {
    return statementHolding(srcDst, location.getFilePath(), location);
  }

  // Whether a call of a revision binds to a method that throws on some path (see ControlFlow), as HunkNetwork binds it
  private BiPredicate<SrcDst, AbstractCall> throwingCall = (srcDst, call) -> false;

  // Whether a statement only logs (see HunkNetwork#isLogStatement): a log line reading a value gives no one anything
  private BiPredicate<SrcDst, AbstractStatement> logStatement = (srcDst, statement) -> false;

  void setLogStatement(BiPredicate<SrcDst, AbstractStatement> logStatement) {
    this.logStatement = logStatement;
  }

  void setThrowingCall(BiPredicate<SrcDst, AbstractCall> throwingCall) {
    this.throwingCall = throwingCall;
  }

  // The innermost statement RM holds the tree in, a composite statement for a tree of its header; none for a tree of no
  // body, as an initializer of a field
  @Nullable
  AbstractStatement statementHolding(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    Unit innermost = null;
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && holds(unit.location(), tree)
              && !unit.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
              && (innermost == null || innermost.location().subsumes(unit.location()))) {
        innermost = unit;
      }
    }
    return innermost == null ? null : innermost.statement();
  }

  /**
   * The code telling the guards of the statement (see ControlFlow#guardsOf), as ranges of offsets, both ends included:
   * each guard by the paths to its exits, the headers and the ends of the statements between it and an exit, and the
   * exit itself. A catch clause reads with the start of its try.
   */
  List<int[]> guardParts(SrcDst srcDst, String path, AbstractStatement statement, List<int[]> targets, Set<VariableDeclaration> read,
                         Set<String> fields) {
    List<int[]> parts = new ArrayList<>();
    Predicate<CompositeStatementObject> decidesCase = composite -> composite.getExpressions().stream()
            .anyMatch(condition -> targets.stream().anyMatch(target -> target[0] < condition.getLocationInfo().getEndOffset()
                    && condition.getLocationInfo().getStartOffset() < target[1]));
    for (ControlFlow.Guard guard : ControlFlow.guardsOf(statement, call -> throwingCall.test(srcDst, call), decidesCase,
            (check, exits) -> bearsOn(check, exits, read, fields))) {
      for (AbstractStatement exit : guard.exits()) {
        // An exit reads by its first line, which tells how it leaves (a return, a throw, a check), not by what it returns
        LocationInfo exitLocation = exit.getLocationInfo();
        parts.add(exit instanceof CompositeStatementObject composite ? header(composite)
                : new int[]{exitLocation.getStartOffset(), exitLocation.getStartOffset()});
        for (AbstractStatement holder = exit; holder != null; holder = holder.getParent()) {
          if (holder instanceof CompositeStatementObject composite) {
            parts.add(header(composite));
            parts.addAll(closing(composite));
            if (composite.getTryContainer() != null && composite.getTryContainer().isPresent()) {
              int tryStart = composite.getTryContainer().get().getLocationInfo().getStartOffset();
              parts.add(new int[]{tryStart, tryStart});
            }
          }
          if (holder == guard.statement()) {
            break;
          }
        }
      }
    }
    return parts;
  }

  // The bodies of the lambdas and anonymous classes within the parts, which run apart from the code holding them
  private List<int[]> laterBodiesWithin(SrcDst srcDst, String path, List<int[]> parts) {
    List<int[]> bodies = new ArrayList<>();
    for (RunScope scope : runScopes(srcDst, path)) {
      if (scope.later() && parts.stream().anyMatch(part -> part[0] <= scope.start() && scope.end() - 1 <= part[1])) {
        bodies.add(new int[]{scope.start(), scope.end() - 1});
      }
    }
    return bodies;
  }

  /**
   * Whether the check, leaving at the exits, bears on code reading the locals (stage 2 of the TODO on data and control
   * flow in HunkNetwork): a condition on the way to an exit reads one of them or tests the value of one of the fields the
   * code passes on or calls on (see testsFieldValue), the check gives one of them a value, or it is a check of a state (a
   * call of a method that throws, as ensureOpen()), which reads none. A check on other values
   * only tells when the member goes on at all, as the steps of a long member checking their own inputs.
   */
  private static boolean bearsOn(AbstractStatement check, List<AbstractStatement> exits, Set<VariableDeclaration> read,
                                 Set<String> fields) {
    for (AbstractStatement exit : exits) {
      if (!ControlFlow.isJump(exit)) {
        return true;
      }
      for (AbstractStatement holder = exit.getParent(); holder != null; holder = holder.getParent()) {
        AbstractStatement condition = holder;
        if (read.stream().anyMatch(local -> DataFlow.reads(condition, local))
                || condition instanceof CompositeStatementObject composite && testsFieldValue(composite, fields)) {
          return true;
        }
        if (holder == check) {
          break;
        }
      }
    }
    List<AbstractStatement> statements = new ArrayList<>(List.of(check));
    if (check instanceof CompositeStatementObject composite) {
      statements.addAll(composite.getAllStatements());
    }
    return statements.stream().anyMatch(statement -> read.stream().anyMatch(local -> DataFlow.writes(statement, local)));
  }

  /**
   * The locals and the parameters the code of the statements reads, with the code of their lambdas: the names it holds
   * referring to the declarations RM gives in the statements holding them, or to the parameters of the operation holding
   * them.
   */
  Set<VariableDeclaration> localsRead(SrcDst srcDst, String path, List<AbstractStatement> statements) {
    Set<VariableDeclaration> locals = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AbstractStatement statement : statements) {
      LocationInfo location = statement.getLocationInfo();
      UMLOperation operation = null;
      for (UMLOperation candidate : operations.get(srcDst).getOrDefault(path, List.of())) {
        if (candidate.getLocationInfo().subsumes(location)
                && (operation == null || operation.getLocationInfo().subsumes(candidate.getLocationInfo()))) {
          operation = candidate;
        }
      }
      List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
              ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
      for (AbstractCodeFragment fragment : fragments) {
        // The names RM gives, and the receivers of calls, which it gives as text only, by the local each may refer to
        for (String name : localNames(fragment)) {
          VariableDeclaration local = statement.searchVariableDeclaration(name);
          if (local == null && operation != null) {
            local = operation.getParameterDeclarationList().stream().filter(parameter -> parameter.getVariableName().equals(name))
                    .findFirst().orElse(null);
          }
          if (local != null && DataFlow.readsIn(fragment, local)) {
            locals.add(local);
          }
        }
      }
    }
    return locals;
  }

  /**
   * The fields the code of the statements passes on or calls on, by name: the names it holds (or calls on, which RM gives
   * as the text of a call only) that are no local or parameter of their member but a field of a class enclosing the
   * code, in its file (lexical scope), as the registration of a channel an inner unsafe class passes to a cancel.
   */
  Set<String> fieldsUsed(SrcDst srcDst, String path, List<AbstractStatement> statements) {
    Set<String> fields = new HashSet<>();
    for (AbstractStatement statement : statements) {
      LocationInfo location = statement.getLocationInfo();
      UMLOperation operation = null;
      for (UMLOperation candidate : operations.get(srcDst).getOrDefault(path, List.of())) {
        if (candidate.getLocationInfo().subsumes(location)
                && (operation == null || operation.getLocationInfo().subsumes(candidate.getLocationInfo()))) {
          operation = candidate;
        }
      }
      Set<String> enclosingFields = new HashSet<>();
      for (UMLAbstractClass owner : owners.get(srcDst).getOrDefault(path, List.of())) {
        if (owner.getLocationInfo().subsumes(location)) {
          owner.getAttributes().forEach(attribute -> enclosingFields.add(attribute.getName()));
        }
      }
      List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
              ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
      for (AbstractCodeFragment fragment : fragments) {
        List<String> names = new ArrayList<>();
        fragment.getVariables().forEach(variable -> names.add(variable.getString()));
        fragment.getMethodInvocations().stream().map(AbstractCall::getExpression).filter(Objects::nonNull).forEach(names::add);
        String thisDot = PathFileUtils.getLang(path).THIS_DOT;
        for (String text : names) {
          String name = text.trim().startsWith(thisDot) ? text.trim().substring(thisDot.length()) : text.trim();
          UMLOperation member = operation;
          if (!enclosingFields.contains(name) || statement.searchVariableDeclaration(name) != null
                  || member != null && member.getParameterDeclarationList().stream().anyMatch(parameter -> parameter.getVariableName().equals(name))) {
            continue;
          }
          fields.add(name);
        }
      }
    }
    return fields;
  }

  /**
   * Whether the conditions of the composite statement test the value of one of the fields itself: compare it (as a null
   * check) or call a predicate on it, rather than read another object through it, as a check of the role in the config a
   * controller field gives, which tells nothing of the controller the code passes on.
   */
  private static boolean testsFieldValue(CompositeStatementObject composite, Set<String> fields) {
    if (fields.isEmpty()) {
      return false;
    }
    for (AbstractCodeFragment condition : composite.getExpressions()) {
      List<AbstractCall> calls = condition.getMethodInvocations();
      for (String field : fields) {
        // The field by its name, or by the reference of the language to the object itself (this., self.)
        Set<String> texts = Set.of(field, condition.getLANG().THIS_DOT + field);
        // A call on the field whose result no other call is made on: a predicate of the field's own object
        for (AbstractCall call : calls) {
          if (call.getExpression() != null && texts.contains(call.getExpression().trim())
                  && calls.stream().noneMatch(other -> other.getExpression() != null && other.getExpression().trim().equals(call.getString().trim()))) {
            return true;
          }
        }
        // The field compared as it is, no call made on it in the condition (RM may give the name a call is made on as a
        // name too)
        for (LeafExpression variable : condition.getVariables()) {
          if (texts.contains(variable.getString().trim())
                  && calls.stream().noneMatch(call -> call.getExpression() != null && texts.contains(call.getExpression().trim()))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * The locals the statements read (see {@link #localsRead}), and the ones the definitions reaching those reads read: the
   * values the code works on, as the object a local is cast from
   */
  private Set<VariableDeclaration> valuesRead(SrcDst srcDst, String path, List<AbstractStatement> statements) {
    Set<VariableDeclaration> values = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AbstractStatement statement : statements) {
      for (VariableDeclaration local : localsRead(srcDst, path, List.of(statement))) {
        values.add(local);
        for (AbstractStatement definition : definitionsReaching(srcDst, statement, local)) {
          values.addAll(localsRead(srcDst, path, List.of(definition)));
        }
      }
    }
    return values;
  }

  /**
   * The definitions of the locals the conditions holding the statement read (see DataFlow#definitionsReaching), printed
   * whole with the headers and the ends of the statements holding them: where the values deciding whether the statement
   * runs come from.
   */
  List<AbstractStatement> conditionDefinitions(SrcDst srcDst, String path, AbstractStatement statement) {
    List<AbstractStatement> definitions = new ArrayList<>();
    for (CompositeStatementObject holder = statement.getParent(); holder != null; holder = holder.getParent()) {
      if (!DECIDING.contains(holder.getLocationInfo().getCodeElementType())) {
        continue;
      }
      for (VariableDeclaration local : localsRead(srcDst, path, List.of(holder))) {
        for (AbstractStatement definition : definitionsReaching(srcDst, holder, local)) {
          if (definitions.stream().noneMatch(known -> known == definition)) {
            definitions.add(definition);
          }
        }
      }
    }
    return definitions;
  }

  // The statement whole, with the headers and the ends of the statements holding it (a catch clause with the start of its
  // try), up to the body of its member or lambda, which the code around them shows
  private static List<int[]> pathParts(AbstractStatement statement) {
    List<int[]> parts = new ArrayList<>();
    LocationInfo location = statement.getLocationInfo();
    parts.add(statement instanceof CompositeStatementObject composite ? header(composite)
            : new int[]{location.getStartOffset(), location.getEndOffset() - 1});
    for (AbstractStatement holder = statement; holder != null && holder.getParent() != null; holder = holder.getParent()) {
      if (holder instanceof CompositeStatementObject composite) {
        parts.add(header(composite));
        parts.addAll(closing(composite));
        if (composite.getTryContainer() != null && composite.getTryContainer().isPresent()) {
          int tryStart = composite.getTryContainer().get().getLocationInfo().getStartOffset();
          parts.add(new int[]{tryStart, tryStart});
        }
      }
    }
    return parts;
  }

  /**
   * The readers outside the window the definitions within it reach (see DataFlow#readersReached): where the values the
   * window gives its locals go. A leaf reader reads whole; a composite one by its header, its end and the statements of
   * it giving the local another value, as the fallback of an if.
   */
  private List<int[]> readerParts(SrcDst srcDst, String path, Tree window) {
    List<int[]> parts = new ArrayList<>();
    int start = window.getPos();
    int end = window.getEndPos();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      AbstractStatement definition = unit.statement();
      if (definition == null || unit.location().getStartOffset() < start || end < unit.location().getEndOffset()) {
        continue;
      }
      for (VariableDeclaration local : localsWritten(definition)) {
        parts.addAll(readerParts(definition, local, start, end));
      }
    }
    // The statement holding the window gives a local the value the window lies in, as a stream mapped by a changed lambda
    // into the keys a later call walks: the readers of that local are where the window's code goes too
    AbstractStatement holding = statementHolding(srcDst, path, window);
    if (holding != null && (holding.getLocationInfo().getStartOffset() < start || end < holding.getLocationInfo().getEndOffset())) {
      for (VariableDeclaration local : localsHolding(holding, start, end)) {
        parts.addAll(readerParts(holding, local, start, end));
      }
    }
    return parts;
  }

  /**
   * The locals the simple statement holding the tree declares with an initializer the tree lies in: the code of the tree
   * makes the value they hold, as a changed lambda mapping the stream a declaration collects.
   */
  List<VariableDeclaration> valueLocals(SrcDst srcDst, String path, Tree tree) {
    AbstractStatement holding = statementHolding(srcDst, path, tree);
    return holding == null ? List.of() : localsHolding(holding, tree.getPos(), tree.getEndPos());
  }

  private static List<VariableDeclaration> localsHolding(AbstractStatement statement, int start, int end) {
    if (statement instanceof CompositeStatementObject) {
      return List.of();
    }
    List<VariableDeclaration> locals = new ArrayList<>();
    for (VariableDeclaration local : statement.getVariableDeclarations()) {
      LocationInfo value = local.getInitializer() == null ? null : local.getInitializer().getLocationInfo();
      if (value != null && value.getStartOffset() <= start && end <= value.getEndOffset()) {
        locals.add(local);
      }
    }
    return locals;
  }

  // The readers outside the window that the definition of the local reaches, a composite one by its header, its end and
  // the statements of it giving the local another value
  private List<int[]> readerParts(AbstractStatement definition, VariableDeclaration local, int start, int end) {
    List<int[]> parts = new ArrayList<>();
    for (AbstractStatement reader : readersReached(definition, local)) {
      LocationInfo location = reader.getLocationInfo();
      if (start <= location.getStartOffset() && location.getEndOffset() <= end) {
        continue;
      }
      parts.addAll(pathParts(reader));
      if (reader instanceof CompositeStatementObject composite) {
        parts.addAll(closing(composite));
        for (AbstractStatement within : composite.getAllStatements()) {
          if (!(within instanceof CompositeStatementObject) && DataFlow.writes(within, local)) {
            parts.addAll(pathParts(within));
          }
        }
      }
    }
    return parts;
  }

  // The locals the code of the statement itself gives a value: the ones it declares with an initializer or assigns
  private static List<VariableDeclaration> localsWritten(AbstractStatement statement) {
    List<VariableDeclaration> locals = new ArrayList<>();
    List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
    for (AbstractCodeFragment fragment : fragments) {
      for (VariableDeclaration declared : fragment.getVariableDeclarations()) {
        if (!locals.contains(declared) && DataFlow.writes(statement, declared)) {
          locals.add(declared);
        }
      }
      for (LeafExpression assignment : fragment.getAssignments()) {
        for (LeafExpression variable : fragment.getVariables()) {
          if (variable.getLocationInfo().getStartOffset() != assignment.getLocationInfo().getStartOffset()) {
            continue;
          }
          VariableDeclaration local = statement.searchVariableDeclaration(variable.getString());
          if (local != null && !locals.contains(local) && DataFlow.writes(statement, local)) {
            locals.add(local);
          }
        }
      }
    }
    return locals;
  }

  // The code of a composite statement before its body: its condition, its resource list, the declaration of a catch
  // clause, the start of a block. The condition of a do loop follows its body, which it ends with
  private static int[] header(CompositeStatementObject composite) {
    int start = composite.getLocationInfo().getStartOffset();
    int bodyStart = composite.getStatements().isEmpty() ? Integer.MAX_VALUE
            : composite.getStatements().get(0).getLocationInfo().getStartOffset();
    int headerEnd = start;
    for (AbstractCodeFragment expression : composite.getExpressions()) {
      if (expression.getLocationInfo().getStartOffset() < bodyStart) {
        headerEnd = Math.max(headerEnd, expression.getLocationInfo().getEndOffset() - 1);
      }
    }
    return new int[]{start, headerEnd};
  }

  // The innermost statement of the file RM holds the code at the location in, a composite statement for code of its
  // header; none for code of no body
  @Nullable
  private AbstractStatement statementHolding(SrcDst srcDst, String path, LocationInfo location) {
    ensureIndexed(srcDst, path);
    Unit innermost = null;
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && unit.location().subsumes(location)
              && !unit.location().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)
              && (innermost == null || innermost.location().subsumes(unit.location()))) {
        innermost = unit;
      }
    }
    return innermost == null ? null : innermost.statement();
  }

  /**
   * Whether the code at the location lies in the condition of a statement deciding which code runs or how often: an
   * if, a switch, a loop, a catch clause.
   */
  boolean isInCondition(SrcDst srcDst, String path, LocationInfo location) {
    AbstractStatement statement = statementHolding(srcDst, path, location);
    return statement instanceof CompositeStatementObject composite && DECIDING.contains(composite.getLocationInfo().getCodeElementType())
            && composite.getExpressions().stream().anyMatch(expression -> expression.getLocationInfo().subsumes(location));
  }

  /**
   * Whether the call takes code as an argument, a lambda or a method or creation reference, which the method it calls
   * runs: code of the caller's own that it runs, as the assertions a retrying assertion runs until they hold. The code is
   * an argument of the call when it lies in the call and no other call or creation RM gives within the call holds it, as
   * RM gives the arguments of a call as text only.
   */
  boolean takesCode(SrcDst srcDst, String path, AbstractCall call) {
    LocationInfo location = call.getLocationInfo();
    AbstractStatement statement = statementHolding(srcDst, path, location);
    if (statement == null) {
      return false;
    }
    List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
    for (AbstractCodeFragment fragment : fragments) {
      List<LocationInfo> code = new ArrayList<>();
      List<LocationInfo> calls = new ArrayList<>();
      fragment.getLambdas().forEach(lambda -> code.add(lambda.getLocationInfo()));
      for (AbstractCall invocation : fragment.getMethodInvocations()) {
        (invocation instanceof MethodReference || invocation instanceof CreationReference ? code : calls).add(invocation.getLocationInfo());
      }
      fragment.getCreations().forEach(creation -> calls.add(creation.getLocationInfo()));
      for (LocationInfo argument : code) {
        if (!location.subsumes(argument) || sameRange(location, argument)) {
          continue;
        }
        boolean nested = calls.stream().anyMatch(other -> location.subsumes(other) && !sameRange(location, other)
                && other.subsumes(argument) && !sameRange(other, argument));
        if (!nested) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean sameRange(LocationInfo location, LocationInfo other) {
    return location.getStartOffset() == other.getStartOffset() && location.getEndOffset() == other.getEndOffset();
  }

  /**
   * Whether the code keeps the result of the call in a local: the call is the initializer of a variable the statement
   * declares (a try resource included), or the value assigned to one.
   */
  boolean keptInLocal(SrcDst srcDst, String path, LocationInfo call) {
    AbstractStatement statement = statementHolding(srcDst, path, call);
    if (statement == null) {
      return false;
    }
    List<AbstractCodeFragment> fragments = new ArrayList<>(List.of(statement));
    if (statement instanceof CompositeStatementObject composite) {
      fragments.addAll(composite.getExpressions());
    }
    for (AbstractCodeFragment fragment : fragments) {
      for (VariableDeclaration declared : fragment.getVariableDeclarations()) {
        if (declared.getInitializer() != null && declared.getInitializer().getLocationInfo().getStartOffset() == call.getStartOffset()
                && declared.getInitializer().getLocationInfo().getEndOffset() == call.getEndOffset()) {
          return true;
        }
      }
      for (LeafExpression assignment : fragment.getAssignments()) {
        LocationInfo assigned = assignment.getLocationInfo();
        if (assigned.getEndOffset() == call.getEndOffset() && assigned.getStartOffset() < call.getStartOffset()) {
          return true;
        }
      }
    }
    return false;
  }

  private static final Set<LocationInfo.CodeElementType> DECIDING = EnumSet.of(LocationInfo.CodeElementType.IF_STATEMENT,
          LocationInfo.CodeElementType.SWITCH_STATEMENT, LocationInfo.CodeElementType.WHEN_STATEMENT,
          LocationInfo.CodeElementType.CATCH_CLAUSE, LocationInfo.CodeElementType.FOR_STATEMENT,
          LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT, LocationInfo.CodeElementType.WHILE_STATEMENT,
          LocationInfo.CodeElementType.DO_STATEMENT);

  /**
   * The code deciding whether and how often the code at the tree runs within the body it lies in (its member, or the
   * lambda it lies in), as ranges of offsets, both ends included: the headers and the ends of the conditionals, the
   * loops and the catch clauses holding it (a catch clause with the start of its try, a branch of an if by the lines its
   * block starts and ends on, which tell the other branch, an else-if branch by its own condition), the case labels it
   * runs on, and its guards (see {@link #guardParts}); the condition of the if whose branch the tree is (or lies in); the
   * start of the try and its catch and finally clauses, whole, when the tree holds a throw the targets add within its
   * body; and the signature of the member (or the header of the lambda) declaring a parameter the tree reads (see
   * {@link #parameterHeaders}). None for code of no body, nor for a member, whose class the code around it does not run.
   * The bodies of the lambdas and anonymous classes within that code are left out but their first and last lines, as they
   * run apart from it.
   */
  Node.Enclosing controlParts(SrcDst srcDst, String path, Tree tree, List<int[]> targets) {
    AbstractStatement statement = statementHolding(srcDst, path, tree);
    // A member (of an anonymous or a local class) is a body of its own, which the code around its class does not run
    UMLOperation member = innermost(operations.get(srcDst).getOrDefault(path, List.of()), tree, UMLOperation::getLocationInfo, false);
    if (statement == null || member != null && !member.getLocationInfo().subsumes(statement.getLocationInfo())
            || owners.get(srcDst).getOrDefault(path, List.of()).stream().anyMatch(owner -> isDeclaredBy(tree, owner.getLocationInfo()))) {
      return new Node.Enclosing(-1, -1, List.of(), List.of());
    }
    List<AbstractStatement> windowStatements = windowStatements(srcDst, path, tree, statement);
    List<int[]> parts = new ArrayList<>(guardParts(srcDst, path, statement, targets, valuesRead(srcDst, path, windowStatements),
            fieldsUsed(srcDst, path, windowStatements)));
    parts.addAll(readerParts(srcDst, path, tree));
    parts.addAll(parameterHeaders(srcDst, path, tree, localsRead(srcDst, path, windowStatements)));
    // A throw the changes add leaves to the catch clauses of the innermost try holding it, which tell what becomes of it
    // (logged, wrapped, retried, swallowed) where a reviewer would otherwise take it to leave the member
    List<AbstractStatement> addedThrows = windowStatements.stream()
            .filter(windowStatement -> windowStatement.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.THROW_STATEMENT
                    && targets.stream().anyMatch(target -> target[0] < windowStatement.getLocationInfo().getEndOffset()
                    && windowStatement.getLocationInfo().getStartOffset() < target[1]))
            .toList();
    boolean handled = false;
    // A window within the body of the statement holding it, as the else branch of an if, runs under that statement as the
    // code of any other statement holding it does: the condition choosing the branch reads with it, not only its
    // `} else {` line, which tells only that some other condition did not hold
    boolean inBody = statement instanceof CompositeStatementObject composite && composite.getLocationInfo().getStartOffset() < tree.getPos()
            && header(composite)[1] < tree.getPos();
    AbstractStatement child = inBody ? ((CompositeStatementObject) statement).getStatements().stream()
            .filter(inner -> inner.getLocationInfo().getStartOffset() <= tree.getPos() && tree.getEndPos() <= inner.getLocationInfo().getEndOffset())
            .findFirst().orElse(null) : statement;
    for (CompositeStatementObject holder = inBody ? (CompositeStatementObject) statement : statement.getParent(); holder != null;
         child = holder, holder = holder.getParent()) {
      LocationInfo location = holder.getLocationInfo();
      LocationInfo.CodeElementType type = location.getCodeElementType();
      if (!handled && holder instanceof TryStatementObject tryStatement && !tryStatement.getCatchClauses().isEmpty()
              && addedThrows.stream().anyMatch(thrown -> location.subsumes(thrown.getLocationInfo()))) {
        handled = true;
        parts.add(new int[]{location.getStartOffset(), location.getStartOffset()});
        List<AbstractStatement> handlers = new ArrayList<>(tryStatement.getCatchClauses());
        if (tryStatement.getFinallyClause() != null) {
          handlers.add(tryStatement.getFinallyClause());
        }
        for (AbstractStatement handler : handlers) {
          parts.add(new int[]{handler.getLocationInfo().getStartOffset(), handler.getLocationInfo().getEndOffset() - 1});
        }
      }
      boolean branch = type == LocationInfo.CodeElementType.BLOCK && holder.getParent() != null
              && holder.getParent().getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.IF_STATEMENT;
      // An else-if branch reads by its own condition, which its line shows to be one of a chain: the conditions of the
      // branches before it only tell they did not hold
      boolean elseIf = type == LocationInfo.CodeElementType.IF_STATEMENT && holder.getStatements().size() == 2
              && child != null && holder.getStatements().get(1) == child
              && child.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.IF_STATEMENT;
      if (elseIf) {
        continue;
      }
      if (DECIDING.contains(type) || branch) {
        parts.add(header(holder));
        parts.addAll(closing(holder));
      }
      if (holder.getTryContainer() != null && holder.getTryContainer().isPresent()) {
        int tryStart = holder.getTryContainer().get().getLocationInfo().getStartOffset();
        parts.add(new int[]{tryStart, tryStart});
      }
      if (type == LocationInfo.CodeElementType.SWITCH_STATEMENT) {
        List<AbstractStatement> siblings = holder.getStatements();
        int index = siblings.indexOf(child);
        while (index > 0 && siblings.get(index).getLocationInfo().getCodeElementType() != LocationInfo.CodeElementType.SWITCH_CASE) {
          index--;
        }
        while (index >= 0 && siblings.get(index).getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.SWITCH_CASE) {
          LocationInfo label = siblings.get(index).getLocationInfo();
          parts.add(new int[]{label.getStartOffset(), label.getEndOffset() - 1});
          index--;
        }
      }
    }
    return new Node.Enclosing(-1, -1, parts, laterBodiesWithin(srcDst, path, parts));
  }

  // The statements within the window, and the statement holding it, which runs it, as the call taking a lambda the window
  // lies in
  private List<AbstractStatement> windowStatements(SrcDst srcDst, String path, Tree tree, AbstractStatement statement) {
    List<AbstractStatement> windowStatements = new ArrayList<>();
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (unit.statement() != null && tree.getPos() <= unit.location().getStartOffset() && unit.location().getEndOffset() <= tree.getEndPos()) {
        windowStatements.add(unit.statement());
      }
    }
    if (windowStatements.stream().noneMatch(windowStatement -> windowStatement == statement)) {
      windowStatements.add(statement);
    }
    return windowStatements;
  }

  /**
   * The code defining the parameters the code at the tree reads (see {@link #parameterHeaders}); none for code of no
   * body. A diff prints the control lines of one revision, so the parameters the code of the other one reads, as removed
   * code, are told by it (see RawRepresentation#controlled).
   */
  List<int[]> parameterParts(SrcDst srcDst, String path, Tree tree) {
    AbstractStatement statement = statementHolding(srcDst, path, tree);
    if (statement == null) {
      return List.of();
    }
    return parameterHeaders(srcDst, path, tree, localsRead(srcDst, path, windowStatements(srcDst, path, tree, statement)));
  }

  /**
   * The signature of the member holding the code at the tree, down to the brace of its body, and its end, with the first
   * and last lines of the lambdas between it and the code (see {@link #signatureParts}); none outside a member.
   */
  List<int[]> memberSignature(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    UMLOperation operation = innermost(operations.get(srcDst).getOrDefault(path, List.of()), tree, UMLOperation::getLocationInfo, false);
    return operation == null ? List.of() : signatureParts(srcDst, path, tree, operation);
  }

  // The signature of the operation down to its body (see signatureEnd), and its end (see closing), with the first and
  // last lines of the lambdas within it holding the tree, which tell the code there runs apart from the member, as two
  // callbacks of one registration do
  private List<int[]> signatureParts(SrcDst srcDst, String path, Tree tree, UMLOperation operation) {
    List<int[]> parts = new ArrayList<>();
    LocationInfo location = operation.getLocationInfo();
    int signature = signatureStart(outermostWithin(tree, location), leadingParts.get(srcDst).getOrDefault(path, List.of())).getPos();
    parts.add(new int[]{signature, signatureEnd(operation)});
    parts.addAll(closing(operation));
    for (Body lambda : bodies.get(srcDst).getOrDefault(path, List.of())) {
      if (lambda.owner() != null && !lambda.declaration() && holds(lambda.location(), tree) && location.subsumes(lambda.owner())) {
        parts.addAll(lambdaHeader(srcDst, lambda));
      }
    }
    return parts;
  }

  // A lambda by the code before its body (see headEnd), from the start of the statement holding it, which tells what
  // runs it (the gauge a callback measures, the listener a write completes), and by the line closing it (see closes)
  private List<int[]> lambdaHeader(SrcDst srcDst, Body lambda) {
    LocationInfo owner = lambda.owner();
    AbstractStatement holding = statementHolding(srcDst, owner);
    int start = holding == null ? owner.getStartOffset() : Math.min(holding.getLocationInfo().getStartOffset(), owner.getStartOffset());
    List<int[]> parts = new ArrayList<>(List.of(new int[]{start, lambda.headEnd()}));
    if (lambda.closed()) {
      parts.add(new int[]{owner.getEndOffset() - 1, owner.getEndOffset() - 1});
    }
    return parts;
  }

  /**
   * The code defining the parameters the window reads: the signature of the member declaring one, down to the brace of
   * its body, with the end of the member and the first lines of the lambdas between it and the window, or the header of
   * the lambda declaring one, with its end. A parameter is defined by the call of its member, which its signature stands
   * for: it tells the type of the parameter, that the caller gives its value, and where the window lies. Printed apart
   * from its member, as a declaration of its own (`QueryParams params`), it tells none of that and reads as one more
   * element to place.
   */
  private List<int[]> parameterHeaders(SrcDst srcDst, String path, Tree tree, Set<VariableDeclaration> read) {
    List<int[]> parts = new ArrayList<>();
    List<Body> lambdas = bodies.get(srcDst).getOrDefault(path, List.of()).stream()
            .filter(body -> body.owner() != null && !body.declaration() && holds(body.location(), tree)).toList();
    for (VariableDeclaration parameter : read) {
      LocationInfo declared = parameter.getLocationInfo();
      for (UMLOperation operation : operations.get(srcDst).getOrDefault(path, List.of())) {
        LocationInfo location = operation.getLocationInfo();
        if (!holds(location, tree) || operation.getParameterDeclarationList().stream().noneMatch(declaration -> declaration == parameter)) {
          continue;
        }
        parts.addAll(signatureParts(srcDst, path, tree, operation));
      }
      for (Body lambda : lambdas) {
        LocationInfo owner = lambda.owner();
        if (owner.subsumes(declared) && !lambda.location().subsumes(declared)) {
          parts.addAll(lambdaHeader(srcDst, lambda));
        }
      }
    }
    return parts;
  }

  /**
   * The end offset of the code the if at the tree runs instead of its branch, when the branch leaves on every path and
   * no else follows (see ControlFlow#implicitElse); -1 for other code.
   */
  int implicitElseEnd(SrcDst srcDst, String path, Tree tree) {
    AbstractStatement statement = statementAt(srcDst, path, tree);
    if (statement == null) {
      return -1;
    }
    List<AbstractStatement> implicitElse = ControlFlow.implicitElse(statement);
    return implicitElse.isEmpty() ? -1 : implicitElse.get(implicitElse.size() - 1).getLocationInfo().getEndOffset();
  }

  // The conditions of the composite statement at the tree, if it is one
  List<LocationInfo> conditionsAt(SrcDst srcDst, String path, Tree tree) {
    AbstractStatement statement = statementAt(srcDst, path, tree);
    return statement instanceof CompositeStatementObject composite
            ? composite.getExpressions().stream().map(AbstractCodeFragment::getLocationInfo).toList() : List.of();
  }

  /**
   * The code of the declaration at the tree a dependency prints in brief, as RM gives it, each as a range of offsets
   * (start included, end excluded) and the text it prints instead: the class body of an enum constant, as a constant used
   * as a value tells its name and arguments, and the body of an accessor whose only statement returns a field of its
   * class or the instance itself, which prints on the line of its signature.
   */
  List<Brief> briefs(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    String content = contentsContexts.getContent(srcDst, path);
    for (UMLAbstractClass owner : owners.get(srcDst).getOrDefault(path, List.of())) {
      for (UMLEnumConstant constant : owner instanceof UMLClass umlClass ? umlClass.getEnumConstants() : List.<UMLEnumConstant>of()) {
        if (!isDeclaredBy(tree, constant.getLocationInfo())) {
          continue;
        }
        LocationInfo location = constant.getLocationInfo();
        for (UMLAbstractClass body : owners.get(srcDst).getOrDefault(path, List.of())) {
          LocationInfo bodyLocation = body.getLocationInfo();
          if (body instanceof UMLAnonymousClass && location.subsumes(bodyLocation) && bodyLocation.getEndOffset() == location.getEndOffset()) {
            return List.of(new Brief(bodyLocation.getStartOffset(), bodyLocation.getEndOffset(),
                    body.getLANG().OPEN_BLOCK + " ... " + body.getLANG().CLOSE_BLOCK));
          }
        }
        return List.of();
      }
      for (UMLOperation operation : owner.getOperations()) {
        if (!isDeclaredBy(tree, operation.getLocationInfo())) {
          continue;
        }
        // The members of the annotations of a method and of its parameters (descriptions, examples, documented types)
        // say how tools and documentation see it, not what a caller relies on; an annotation of a single value, as the
        // warnings it suppresses, is that value, which its name tells the meaning of
        List<Brief> briefs = new ArrayList<>();
        List<UMLAnnotation> annotations = new ArrayList<>(operation.getAnnotations());
        operation.getParameterDeclarationList().forEach(parameter -> annotations.addAll(parameter.getAnnotations()));
        for (UMLAnnotation annotation : annotations) {
          // The annotation by its code up to its name, as written
          LocationInfo location = annotation.getLocationInfo();
          int name = annotation.isNormalAnnotation()
                  ? content.substring(location.getStartOffset(), location.getEndOffset()).indexOf(annotation.getTypeName()) : -1;
          if (name >= 0) {
            briefs.add(new Brief(location.getStartOffset(), location.getEndOffset(),
                    content.substring(location.getStartOffset(), location.getStartOffset() + name + annotation.getTypeName().length()) + "(...)"));
          }
        }
        if (operation.getBody() == null) {
          return briefs;
        }
        CompositeStatementObject body = operation.getBody().getCompositeStatement();
        if (body.getStatements().size() != 1 || !returnsState(body.getStatements().get(0), operation, owner)) {
          return briefs;
        }
        LocationInfo statement = body.getStatements().get(0).getLocationInfo();
        LocationInfo bodyLocation = body.getLocationInfo();
        // A body opened and closed by code of its own (braces) prints on one line; one indented under the signature
        // (Python) is its statement already
        if (bodyLocation.getStartOffset() < statement.getStartOffset() && statement.getEndOffset() < bodyLocation.getEndOffset()) {
          briefs.add(new Brief(bodyLocation.getStartOffset(), bodyLocation.getEndOffset(), owner.getLANG().OPEN_BLOCK + " "
                  + content.substring(statement.getStartOffset(), statement.getEndOffset()) + " " + owner.getLANG().CLOSE_BLOCK));
        }
        return briefs;
      }
    }
    return List.of();
  }

  // Whether the statement returns a field of the class, by its name, or the instance itself
  private static boolean returnsState(AbstractStatement statement, UMLOperation operation, UMLAbstractClass owner) {
    if (!statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
      return false;
    }
    Constants lang = statement.getLANG();
    for (LeafExpression thisExpression : statement.getThisExpressions()) {
      if (statement.getString().equals(lang.RETURN_SPACE + thisExpression.getString() + lang.STATEMENT_TERMINATION)) {
        return true;
      }
    }
    for (LeafExpression variable : statement.getVariables()) {
      String name = variable.getString();
      String field = name.startsWith(lang.THIS_DOT) ? name.substring(lang.THIS_DOT.length()) : name;
      if (statement.getString().equals(lang.RETURN_SPACE + name + lang.STATEMENT_TERMINATION)
              && owner.getAttributes().stream().anyMatch(attribute -> attribute.getName().equals(field))
              && (!field.equals(name) || operation.getParameterDeclarationList().stream()
                      .noneMatch(parameter -> parameter.getVariableName().equals(field)))) {
        return true;
      }
    }
    return false;
  }

  record Brief(int start, int end, String text) {
  }

  /**
   * The code a type printed by its header alone (from its signature to the end of its header) prints after the header,
   * with its members left out, as the file has it around them: the code between the header and the first member RM gives
   * in the type (the brace opening its body, the colon of a Python class), a line {@code ...} at the indentation of that
   * member, and the line closing the type after its last member (its closing brace; none for a body indented under its
   * header, which ends with its last member). A member is any code RM gives in the body: an operation, a field, an enum
   * constant, an initializer, a nested type, a comment, a doc comment or an annotation. A type with no member prints its
   * body as it is.
   */
  String elidedTypeBody(SrcDst srcDst, String path, int headerStart, int headerEnd) {
    ensureIndexed(srcDst, path);
    String content = contentsContexts.getContent(srcDst, path);
    UMLClass type = null;
    for (UMLClass candidate : classes.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo location = candidate.getLocationInfo();
      if (location.getStartOffset() <= headerStart && headerEnd <= location.getEndOffset()
              && (type == null || type.getLocationInfo().subsumes(location))) {
        type = candidate;
      }
    }
    if (type == null) {
      return "";
    }
    int typeEnd = type.getLocationInfo().getEndOffset();
    List<LocationInfo> members = new ArrayList<>();
    type.getOperations().forEach(operation -> members.add(operation.getLocationInfo()));
    type.getAttributes().forEach(attribute -> members.add(attribute.getFieldDeclarationLocationInfo() != null
            ? attribute.getFieldDeclarationLocationInfo() : attribute.getLocationInfo()));
    type.getEnumConstants().forEach(constant -> members.add(constant.getLocationInfo()));
    type.getInitializers().forEach(initializer -> members.add(initializer.getLocationInfo()));
    classes.get(srcDst).getOrDefault(path, List.of()).forEach(nested -> members.add(nested.getLocationInfo()));
    members.addAll(comments.get(srcDst).getOrDefault(path, List.of()));
    members.addAll(docComments.get(srcDst).getOrDefault(path, List.of()));
    members.addAll(leadingParts.get(srcDst).getOrDefault(path, List.of()));
    int first = Integer.MAX_VALUE;
    int last = -1;
    for (LocationInfo member : members) {
      if (member != null && headerEnd <= member.getStartOffset() && member.getEndOffset() <= typeEnd
              && !sameRange(member, type.getLocationInfo())) {
        first = Math.min(first, member.getStartOffset());
        last = Math.max(last, member.getEndOffset());
      }
    }
    if (last < 0) {
      return content.substring(headerEnd, typeEnd);
    }
    StringBuilder body = new StringBuilder(content.substring(headerEnd, first).stripTrailing());
    body.append("\n").append(indentBefore(content, first)).append("...");
    String after = content.substring(last, typeEnd);
    if (!after.isBlank()) {
      int closer = last + (after.length() - after.stripLeading().length());
      int lineStart = content.lastIndexOf('\n', typeEnd - 1) + 1;
      int from = lineStart >= closer ? lineStart : closer;
      body.append("\n").append(content, from, typeEnd);
    }
    return body.toString();
  }

  // The whitespace before the offset on its line, none when code precedes it there
  private static String indentBefore(String content, int offset) {
    int lineStart = content.lastIndexOf('\n', offset - 1) + 1;
    String before = content.substring(lineStart, offset);
    return before.isBlank() ? before : "";
  }

  private static boolean holdsStrictly(LocationInfo location, int start, int end) {
    return location.getStartOffset() <= start && end <= location.getEndOffset()
            && (location.getStartOffset() != start || location.getEndOffset() != end);
  }

  // Whether the tree lies in a doc comment
  boolean isInDocComment(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    return docComments.get(srcDst).getOrDefault(path, List.of()).stream().anyMatch(doc -> holds(doc, tree));
  }

  // The class (or the anonymous class) holding the code at the location most closely
  @Nullable
  UMLAbstractClass ownerOf(SrcDst srcDst, LocationInfo location) {
    String path = location.getFilePath();
    ensureIndexed(srcDst, path);
    UMLAbstractClass owner = null;
    for (UMLAbstractClass candidate : owners.get(srcDst).getOrDefault(path, List.of())) {
      if (candidate.getLocationInfo().subsumes(location) && (owner == null || owner.getLocationInfo().subsumes(candidate.getLocationInfo()))) {
        owner = candidate;
      }
    }
    return owner;
  }

  // The operations named so of the class (or the anonymous class) holding the tree most closely
  long operationsNamed(SrcDst srcDst, String path, Tree tree, String name) {
    ensureIndexed(srcDst, path);
    UMLAbstractClass owner = innermost(owners.get(srcDst).getOrDefault(path, List.of()), tree, UMLAbstractClass::getLocationInfo, false);
    return owner == null ? 0 : owner.getOperations().stream().filter(operation -> operation.getName().equals(name)).count();
  }


  // The part of the code a declaration starts at, after its doc comment and annotations; the code itself when it starts
  // with neither, as a statement, whose first child may start after it (the first argument of super(...))
  private static Tree signatureStart(Tree tree, List<LocationInfo> leading) {
    for (int i = 0; i < tree.getChildren().size(); i++) {
      Tree child = tree.getChildren().get(i);
      if (leading.stream().noneMatch(part -> holds(part, child))) {
        return i == 0 ? tree : child;
      }
    }
    return tree;
  }

  private record BodyStart(@Nullable Tree from) {
  }

  // Where the body the tree is, if it is one, is printed from: the start of the code it is the body of, or itself
  @Nullable
  private BodyStart bodyStart(SrcDst srcDst, String path, Tree tree, List<LocationInfo> leading) {
    for (Body body : bodies.get(srcDst).getOrDefault(path, List.of())) {
      if (body.location().getStartOffset() == tree.getPos() && body.location().getEndOffset() == tree.getEndPos()) {
        if (body.owner() == null) {
          return new BodyStart(null);
        }
        Tree owner = outermostWithin(tree, body.owner());
        return new BodyStart(body.declaration() ? signatureStart(owner, leading) : owner);
      }
    }

    // RM gives the statements of the body of a try or a catch clause to the try or the catch clause itself
    for (Unit unit : units.get(srcDst).getOrDefault(path, List.of())) {
      if (!(unit.statement() instanceof CompositeStatementObject composite) || !holds(unit.location(), tree)
              || tree.getParent() == null || tree.getParent() != outermostWithin(tree, unit.location())) {
        continue;
      }
      List<Unit> children = childUnits.getOrDefault(composite, List.of());
      if (!children.isEmpty() && children.stream().allMatch(child -> holds(tree, child.location()))) {
        return new BodyStart(tree.getParent());
      }
    }
    return null;
  }

  private static boolean holds(Tree tree, LocationInfo location) {
    return tree.getPos() <= location.getStartOffset() && location.getEndOffset() <= tree.getEndPos();
  }

  // The declaration, expression, return or throw statement, or the field declaration, an expression is printed with,
  // unless the expression is it or lies in one of its block lambdas or anonymous classes
  @Nullable
  private Tree simpleStatement(SrcDst srcDst, String path, Tree tree) {
    SimpleStatement statement = null;
    for (SimpleStatement candidate : simpleStatements.get(srcDst).getOrDefault(path, List.of())) {
      if (holds(candidate.location(), tree) && (statement == null || statement.location().subsumes(candidate.location()))) {
        statement = candidate;
      }
    }
    if (statement == null || isDeclaredBy(tree, statement.location())
            || statement.later().stream().anyMatch(later -> holds(later, tree))) {
      return null;
    }
    if (statement.type() != null && !SIMPLE.contains(statement.type())) {
      return null;
    }
    return outermostWithin(tree, statement.location());
  }

  // The bodies of the block lambdas and the anonymous classes of the simple statement at the tree (see simpleStatement),
  // which run apart from it, as ranges of offsets (end included)
  List<int[]> apartBodies(SrcDst srcDst, String path, Tree statementTree) {
    ensureIndexed(srcDst, path);
    for (SimpleStatement statement : simpleStatements.get(srcDst).getOrDefault(path, List.of())) {
      if (isDeclaredBy(statementTree, statement.location())) {
        return statement.later().stream().map(later -> new int[]{later.getStartOffset(), later.getEndOffset() - 1}).toList();
      }
    }
    return List.of();
  }

  private static final Set<LocationInfo.CodeElementType> SIMPLE = EnumSet.of(LocationInfo.CodeElementType.VARIABLE_DECLARATION_STATEMENT,
          LocationInfo.CodeElementType.EXPRESSION_STATEMENT, LocationInfo.CodeElementType.RETURN_STATEMENT,
          LocationInfo.CodeElementType.THROW_STATEMENT);

  // The element whose location holds the tree most closely; when strict, one the tree is not the outermost code of
  @Nullable
  private static <T> T innermost(List<T> elements, Tree tree, java.util.function.Function<T, LocationInfo> location,
                                 boolean strict) {
    T innermost = null;
    for (T element : elements) {
      LocationInfo elementLocation = location.apply(element);
      if (holds(elementLocation, tree) && !(strict && isDeclaredBy(tree, elementLocation))
              && (innermost == null || location.apply(innermost).subsumes(elementLocation))) {
        innermost = element;
      }
    }
    return innermost;
  }

  private static boolean holds(LocationInfo location, Tree tree) {
    return location.getStartOffset() <= tree.getPos() && tree.getEndPos() <= location.getEndOffset();
  }

  // Whether the tree is the outermost code within the location
  private static boolean isDeclaredBy(Tree tree, LocationInfo location) {
    return holds(location, tree) && (tree.getParent() == null || !holds(location, tree.getParent()));
  }

  private static Tree outermostWithin(Tree tree, LocationInfo location) {
    Tree outermost = tree;
    while (outermost.getParent() != null && holds(location, outermost.getParent())) {
      outermost = outermost.getParent();
    }
    return outermost;
  }

  // All of an operation but its body
  private static List<Tree> operationHeader(UMLOperation operation, Tree declaration) {
    if (operation.getBody() == null) {
      return declaration.getChildren();
    }
    LocationInfo body = operation.getBody().getCompositeStatement().getLocationInfo();
    return declaration.getChildren().stream().filter(child -> !holds(body, child)).toList();
  }

  // The parts of a class before its body, which opens at its first member, or at the first comment after the parts RM
  // gives of the header (its doc comment, annotations, modifiers, type parameters, record components and supertypes)
  private List<Tree> classHeader(SrcDst srcDst, UMLClass umlClass, Tree declaration) {
    LocationInfo location = umlClass.getLocationInfo();
    String path = location.getFilePath();

    List<LocationInfo> headerParts = new ArrayList<>();
    if (umlClass.getJavadoc() != null) {
      headerParts.add(umlClass.getJavadoc().getLocationInfo());
    }
    umlClass.getAnnotations().forEach(annotation -> headerParts.add(annotation.getLocationInfo()));
    umlClass.getModifiers().forEach(modifier -> headerParts.add(modifier.getLocationInfo()));
    umlClass.getTypeParameters().forEach(typeParameter -> headerParts.add(typeParameter.getLocationInfo()));
    if (umlClass.getSuperclass() != null) {
      headerParts.add(umlClass.getSuperclass().getLocationInfo());
    }
    umlClass.getImplementedInterfaces().forEach(supertype -> headerParts.add(supertype.getLocationInfo()));
    umlClass.getAttributes().stream().filter(attribute -> attribute instanceof UMLRecordComponent)
            .forEach(component -> headerParts.add(component.getLocationInfo()));
    int headerEnd = headerParts.stream().filter(Objects::nonNull).mapToInt(LocationInfo::getEndOffset).max()
            .orElse(location.getStartOffset());

    List<LocationInfo> members = new ArrayList<>();
    umlClass.getOperations().forEach(operation -> members.add(operation.getLocationInfo()));
    umlClass.getInitializers().forEach(initializer -> members.add(initializer.getLocationInfo()));
    umlClass.getAttributes().stream().filter(attribute -> !(attribute instanceof UMLRecordComponent))
            .forEach(attribute -> members.add(attribute.getFieldDeclarationLocationInfo() != null
                    ? attribute.getFieldDeclarationLocationInfo() : attribute.getLocationInfo()));
    umlClass.getEnumConstants().forEach(enumConstant -> members.add(enumConstant.getLocationInfo()));
    classes.get(srcDst).getOrDefault(path, List.of()).stream()
            .filter(nested -> nested != umlClass && location.subsumes(nested.getLocationInfo()))
            .forEach(nested -> members.add(nested.getLocationInfo()));
    comments.get(srcDst).getOrDefault(path, List.of()).stream()
            .filter(comment -> location.subsumes(comment) && headerEnd < comment.getStartOffset())
            .forEach(members::add);

    int bodyStart = Stream.concat(members.stream().filter(member -> location.subsumes(member)).map(LocationInfo::getStartOffset),
            Stream.of(location.getEndOffset())).min(Integer::compare).orElseThrow();
    return declaration.getChildren().stream().filter(child -> child.getPos() < bodyStart).toList();
  }
}
