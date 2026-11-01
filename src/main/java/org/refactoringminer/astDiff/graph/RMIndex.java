package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.TernaryOperatorExpression;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/**
 * The statements, operations and classes RM gives of both revisions, by file, to tell where a tree lies in them (see
 * {@link RMInfo}). A module RM makes of a file is no class, as no tree but the file declares it.
 */
class RMIndex {
  private final Map<SrcDst, Map<String, List<Unit>>> units = new EnumMap<>(SrcDst.class);
  // Unchanged files are in both revisions with the same code objects
  private final Map<SrcDst, Map<AbstractStatement, Unit>> statementUnits = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<UMLOperation>>> operations = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<UMLClass>>> classes = new EnumMap<>(SrcDst.class);
  private final Map<SrcDst, Map<String, List<LocationInfo>>> comments = new EnumMap<>(SrcDst.class);
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
    umlModel.getCommentMap().getOrDefault(path, List.of()).forEach(comment -> pushComment(fileComments, seenComments, comment));
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
      pushLeadingParts(fileLeadingParts, umlClass.getJavadoc(), umlClass.getAnnotations());
      pushDocComment(fileDocComments, umlClass.getJavadoc());
      }

        fileOwners.computeIfAbsent(context.getLocationInfo().getFilePath(), file -> new ArrayList<>()).add(context);
        context.getComments().forEach(comment -> pushComment(fileComments, seenComments, comment));
        context.getAttributes().forEach(attribute -> attribute.getComments().forEach(comment -> pushComment(fileComments, seenComments, comment)));
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
            attribute.getAllLambdas().forEach(lambda -> later.add(lambda.getLocationInfo()));
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
            push(fileBodies, new Body(initializer.getBody().getCompositeStatement().getLocationInfo(), initializer.getLocationInfo(), false));
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
            push(fileBodies, new Body(operation.getBody().getCompositeStatement().getLocationInfo(), operation.getLocationInfo(), true));
          }
          operation.getComments().forEach(comment -> pushComment(fileComments, seenComments, comment));
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
              push(fileBodies, new Body(lambda.getBody().getCompositeStatement().getLocationInfo(), lambda.getLocationInfo(), false));
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
                List<LocationInfo> later = new ArrayList<>();
                statement.getLambdas().forEach(lambda -> later.add(lambda.getLocationInfo()));
                statement.getAnonymousClassDeclarations().forEach(anonymous -> later.add(anonymous.getLocationInfo()));
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
    if (type.equals(LocationInfo.CodeElementType.FINALLY_BLOCK)) {
      push(fileBodies, new Body(statement.getLocationInfo(), null, false));
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
    push(fileBodies, new Body(statement.getLocationInfo(), asIs ? null : parent.getLocationInfo(), false));
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

  // A body, with the code it is printed from when it is not printed as it is, from the signature of that code when it is
  // a declaration
  private record Body(LocationInfo location, @Nullable LocationInfo owner, boolean declaration) implements Located {
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

  private static void pushComment(Map<String, List<LocationInfo>> fileComments, Set<String> seen, UMLComment comment) {
    LocationInfo location = comment.getLocationInfo();
    if (seen.add(location.getFilePath() + ":" + location.getStartOffset() + ":" + location.getEndOffset())) {
      fileComments.computeIfAbsent(location.getFilePath(), file -> new ArrayList<>()).add(location);
    }
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
      int headerEnd = location.getStartOffset();
      if (unit.statement() instanceof CompositeStatementObject composite) {
        for (AbstractCodeFragment expression : composite.getExpressions()) {
          headerEnd = Math.max(headerEnd, expression.getLocationInfo().getEndOffset() - 1);
        }
      }
      parts.add(new int[]{location.getStartOffset(), headerEnd});
      parts.add(new int[]{location.getEndOffset() - 1, location.getEndOffset() - 1});
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
      int bodyStart = operation.getBody() == null ? location.getEndOffset() - 1
              : operation.getBody().getCompositeStatement().getLocationInfo().getStartOffset();
      parts.add(new int[]{signature, bodyStart});
      parts.add(new int[]{location.getEndOffset() - 1, location.getEndOffset() - 1});
      if (location.getStartOffset() <= member[0] && member[1] <= location.getEndOffset()) {
        member = new int[]{location.getStartOffset(), location.getEndOffset()};
      }
    }
    for (Body body : bodies.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo owner = body.owner();
      if (owner == null || body.declaration() || !holdsStrictly(owner, start, end)) {
        continue;
      }
      parts.add(new int[]{owner.getStartOffset(), body.location().getStartOffset()});
      parts.add(new int[]{owner.getEndOffset() - 1, owner.getEndOffset() - 1});
      if (owner.getStartOffset() <= member[0] && member[1] <= owner.getEndOffset()
              && initializers.get(srcDst).getOrDefault(path, List.of()).contains(owner)) {
        member = new int[]{owner.getStartOffset(), owner.getEndOffset()};
      }
    }
    for (UMLAbstractClass owner : owners.get(srcDst).getOrDefault(path, List.of())) {
      LocationInfo location = owner.getLocationInfo();
      if (owner instanceof UMLAnonymousClass && holdsStrictly(location, start, end)) {
        parts.add(new int[]{location.getStartOffset(), location.getStartOffset()});
        parts.add(new int[]{location.getEndOffset() - 1, location.getEndOffset() - 1});
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
    boolean decides = runScopes(srcDst, path).stream().anyMatch(scope -> !scope.later() && scope.start() == statement.getPos()
            && scope.end() == statement.getEndPos());
    if (decides || !(statementAt(srcDst, path, statement) instanceof CompositeStatementObject)) {
      List<LocationInfo> leading = leadingParts.get(srcDst).getOrDefault(path, List.of());
      parts.add(new int[]{signatureStart(statement, leading).getPos(), statement.getEndPos() - 1});
      for (RunScope scope : runScopes(srcDst, path)) {
        if (scope.later() && statement.getPos() <= scope.start() && scope.end() <= statement.getEndPos()
                && uses.stream().noneMatch(use -> scope.start() <= use.getPos() && use.getEndPos() <= scope.end()
                        || use.getPos() <= scope.start() && scope.end() == use.getEndPos())) {
          hidden.add(new int[]{scope.start(), scope.end() - 1});
        }
      }
    }
    return new Node.Enclosing(member[0], member[1], parts, hidden);
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
            return List.of(new Brief(bodyLocation.getStartOffset(), bodyLocation.getEndOffset(), "{ ... }"));
          }
        }
        return List.of();
      }
      for (UMLOperation operation : owner.getOperations()) {
        if (!isDeclaredBy(tree, operation.getLocationInfo()) || operation.getBody() == null) {
          continue;
        }
        CompositeStatementObject body = operation.getBody().getCompositeStatement();
        if (body.getStatements().size() != 1 || !returnsState(body.getStatements().get(0), operation, owner)) {
          return List.of();
        }
        LocationInfo statement = body.getStatements().get(0).getLocationInfo();
        LocationInfo bodyLocation = body.getLocationInfo();
        return List.of(new Brief(bodyLocation.getStartOffset(), bodyLocation.getEndOffset(),
                "{ " + content.substring(statement.getStartOffset(), statement.getEndOffset()) + " }"));
      }
    }
    return List.of();
  }

  // Whether the statement returns a field of the class, by its name, or the instance itself
  private static boolean returnsState(AbstractStatement statement, UMLOperation operation, UMLAbstractClass owner) {
    if (!statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
      return false;
    }
    String thisName = owner.getLANG().THIS;
    for (LeafExpression thisExpression : statement.getThisExpressions()) {
      if (statement.getString().equals("return " + thisExpression.getString() + ";\n")) {
        return true;
      }
    }
    for (LeafExpression variable : statement.getVariables()) {
      String name = variable.getString();
      String field = name.startsWith(thisName + ".") ? name.substring(thisName.length() + 1) : name;
      if (statement.getString().equals("return " + name + ";\n")
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

  private static boolean holdsStrictly(LocationInfo location, int start, int end) {
    return location.getStartOffset() <= start && end <= location.getEndOffset()
            && (location.getStartOffset() != start || location.getEndOffset() != end);
  }

  // Whether the tree lies in a doc comment
  boolean isInDocComment(SrcDst srcDst, String path, Tree tree) {
    ensureIndexed(srcDst, path);
    return docComments.get(srcDst).getOrDefault(path, List.of()).stream().anyMatch(doc -> holds(doc, tree));
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
  // unless the expression is it or lies in one of its lambdas or anonymous classes
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
