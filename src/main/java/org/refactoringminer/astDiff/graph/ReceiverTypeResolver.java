package org.refactoringminer.astDiff.graph;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.refactoringminer.util.PathFileUtils;

public class ReceiverTypeResolver {

  private static final String[] MEMBER_SEPARATORS = {"?.", "->", "::", "."};
  private static final Set<String> INFERRED_TYPE_NAMES = Set.of("var", "val", "auto", "Inferred");
  private static final Map<String, String> BOXES = Map.of("boolean", "Boolean", "byte", "Byte", "char", "Character",
          "short", "Short", "int", "Integer", "long", "Long", "float", "Float", "double", "Double");
  private static final Map<String, List<String>> WIDENINGS = Map.of(
          "byte", List.of("short", "int", "long", "float", "double"),
          "short", List.of("int", "long", "float", "double"),
          "char", List.of("int", "long", "float", "double"),
          "int", List.of("long", "float", "double"),
          "long", List.of("float", "double"),
          "float", List.of("double"));
  private static final Pattern NAME_CHAIN =Pattern.compile("[\\w$]+(?:(?:\\.|::)[\\w$]+)*");
  private final UMLModelDiff modelDiff;
  private final RepositoryStateFiles repositoryStateFiles;
  private final FilePathResolver filePathResolver;
  @Nullable
  private final ContentsContexts contentsContexts;
  private final Map<Object, NodePool> nodePools = new IdentityHashMap<>();
  private final Map<UMLAnonymousClass, UMLType> anonymousSupertypes = new IdentityHashMap<>();
  private final Map<UMLAbstractClass, Set<String>> nonMemberTypes = new IdentityHashMap<>();
  private final Set<UMLAbstractClass> memberTypeLookups = Collections.newSetFromMap(new IdentityHashMap<>());

  public ReceiverTypeResolver(UMLModelDiff modelDiff, RepositoryStateFiles repositoryStateFiles) {
    this(modelDiff, repositoryStateFiles, null);
  }

  public ReceiverTypeResolver(UMLModelDiff modelDiff, RepositoryStateFiles repositoryStateFiles,
                              @Nullable ContentsContexts contentsContexts) {
    this.modelDiff = modelDiff;
    this.repositoryStateFiles = repositoryStateFiles;
    this.filePathResolver = new FilePathResolver(repositoryStateFiles);
    this.contentsContexts = contentsContexts;
  }

  @Nullable
  public ReceiverType resolveInvocationReceiver(AbstractCall invocation, UMLAbstractClass context, UMLModel umlModel) {
    return getCallReceiverType(invocation, scopeOf(invocation.getContainer(), invocation.getLocationInfo(), context, umlModel));
  }

  @Nullable
  public ReceiverType resolveVariableReceiver(LeafExpression variable, UMLAbstractClass context, UMLModel umlModel) {
    return getQualifierType(variable, scopeOf(variable.getContainer(), variable.getLocationInfo(), context, umlModel));
  }

  @Nullable
  public UMLOperation resolveInvocationTarget(AbstractCall call, UMLAbstractClass context, UMLModel umlModel) {
    UMLOperation target = bindInvocation(call, context, umlModel);
    // bound again once attached, to the operation of the model rather than of the file parsed to bind it
    return target != null && attach(Set.of(target.getLocationInfo().getFilePath())) ? bindInvocation(call, context, umlModel) : target;
  }

  @Nullable
  public UMLAttribute resolveAccessTarget(LeafExpression variable, UMLAbstractClass context, UMLModel umlModel) {
    UMLAttribute target = bindAccess(variable, context, umlModel);
    return target != null && attach(Set.of(target.getLocationInfo().getFilePath())) ? bindAccess(variable, context, umlModel) : target;
  }

  /**
   * Java: the methods of the supertypes of its class that the operation overrides or implements, those of its name and
   * parameter types (a type variable of the supertype method standing for any type) that are not private nor static.
   */
  public List<UMLOperation> findOverridden(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel) {
    if (!declaring.getLANG().equals(Constants.JAVA) || operation.isConstructor() || operation.isStatic()
            || operation.getVisibility() == Visibility.PRIVATE) {
      return List.of();
    }

    ClassRef declaringRef = new ClassRef(declaring, Set.of());
    List<String> signature = candidate(operation, declaringRef).signature();
    List<UMLOperation> overridden = new ArrayList<>();
    walkHierarchy(declaringRef, umlModel, current -> {
      if (current.umlClass() == declaring) {
        return false;
      }
      for (UMLOperation superOperation : current.umlClass().getOperations()) {
        if (!superOperation.getName().equals(operation.getName()) || superOperation.isConstructor() || superOperation.isStatic()
                || superOperation.getVisibility() == Visibility.PRIVATE) {
          continue;
        }
        Candidate superCandidate = candidate(superOperation, current);
        if (superCandidate.signature().size() != signature.size()) {
          continue;
        }
        boolean overrides = true;
        for (int i = 0; i < signature.size() && overrides; i++) {
          overrides = superCandidate.parameters().get(i) == null || superCandidate.signature().get(i).equals(signature.get(i));
        }
        if (overrides) {
          overridden.add(superOperation);
        }
      }
      return false;
    }, external -> {});
    return overridden;
  }

  /** The local variable or parameter a simple name refers to, looked up through the anonymous classes enclosing it. */
  @Nullable
  public VariableDeclaration resolveLocal(LeafExpression name, UMLAbstractClass context, UMLModel umlModel) {
    return lookupName(name.getString().trim(), scopeOf(name.getContainer(), name.getLocationInfo(), context, umlModel)).local();
  }

  @Nullable
  public UMLAttribute resolveCaseLabelTarget(LeafExpression label, AbstractCodeFragment selector, UMLAbstractClass context, UMLModel umlModel) {
    UMLAttribute target = bindCaseLabel(label, selector, context, umlModel);
    return target != null && attach(Set.of(target.getLocationInfo().getFilePath())) ? bindCaseLabel(label, selector, context, umlModel) : target;
  }

  @Nullable
  private UMLAttribute bindCaseLabel(LeafExpression label, AbstractCodeFragment selector, UMLAbstractClass context, UMLModel umlModel) {
    ReceiverScope scope = scopeOf(label.getContainer(), selector.getLocationInfo(), context, umlModel);
    ReceiverType selectorType = getFragmentType(selector, scope);
    ClassRef selectorClass = selectorType == null ? null : getModelClass(selectorType, umlModel);
    if (selectorClass != null && selectorClass.umlClass() instanceof UMLClass umlClass && umlClass.isEnum()) {
      DeclaredField constant = findOwnField(selectorClass, label.getString().trim());
      return constant == null ? null : constant.member();
    }
    Class<?> selectorJdkClass = selectorType == null || selectorClass != null ? null : jdkClassOf(selectorType);
    if (selectorJdkClass != null && selectorJdkClass.isEnum()) {
      // a constant of a JDK enum
      return null;
    }
    return bindAccess(label, context, umlModel);
  }

  @Nullable
  private UMLOperation bindInvocation(AbstractCall call, UMLAbstractClass context, UMLModel umlModel) {
    ReceiverScope scope = scopeOf(call.getContainer(), call.getLocationInfo(), context, umlModel);
    return select(call, invocationCandidates(call, scope), scope);
  }

  @Nullable
  private UMLAttribute bindAccess(LeafExpression variable, UMLAbstractClass context, UMLModel umlModel) {
    ReceiverScope scope = scopeOf(variable.getContainer(), variable.getLocationInfo(), context, umlModel);
    LocationInfo.CodeElementType kind = variable.getLocationInfo().getCodeElementType();
    Found<DeclaredField> field;
    if (kind.equals(LocationInfo.CodeElementType.SIMPLE_NAME)) {
      NameLookup lookup = lookupName(variable.getString().trim(), scope);
      field = lookup.local() != null || lookup.field() == null ? null : lookup.field().field();
    } else if (kind.equals(LocationInfo.CodeElementType.QUALIFIED_NAME) || kind.equals(LocationInfo.CodeElementType.FIELD_ACCESS)) {
      String member = trailingIdentifier(variable.getString());
      ReceiverType qualifierType = getQualifierType(variable, member, scope);
      ClassRef owner = qualifierType == null ? null : getModelClass(qualifierType, umlModel);
      field = owner == null ? null : findField(owner, member, umlModel);
    } else {
      return null;
    }
    return field == null ? null : field.value().member();
  }

  @Nullable
  private ReceiverType getQualifierType(LeafExpression variable, ReceiverScope scope) {
    LocationInfo location = variable.getLocationInfo();
    LocationInfo.CodeElementType kind = location.getCodeElementType();
    if (kind.equals(LocationInfo.CodeElementType.SIMPLE_NAME)) {
      // a field used by its simple name has the implicit receiver declaring it; local variables have none
      NameLookup lookup = lookupName(variable.getString().trim(), scope);
      if (lookup.local() != null || lookup.field() == null || lookup.field().field() == null) {
        return null;
      }
      Found<DeclaredField> found = lookup.field().field();
      return getImplicitReceiverType(new Found<>(found.value().declaringClass(), found.files()));
    }
    if (!kind.equals(LocationInfo.CodeElementType.QUALIFIED_NAME) && !kind.equals(LocationInfo.CodeElementType.FIELD_ACCESS)) {
      return null;
    }
    return getQualifierType(variable, trailingIdentifier(variable.getString()), scope);
  }

  @Nullable
  private ReceiverType getQualifierType(LeafExpression memberAccess, String member, ReceiverScope scope) {
    LocationInfo location = memberAccess.getLocationInfo();
    String qualifier = qualifierName(memberAccess.getString(), member);
    if (qualifier == null) {
      return null;
    }
    LeafExpression qualifierNode = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), memberAccess);
    boolean wholeQualifier = qualifierNode != null && qualifierNode.getString().trim().equals(qualifier);
    return wholeQualifier ? getNodeType(qualifierNode, scope) : getNameType(qualifier, scope);
  }

  private boolean attach(Set<String> paths) {
    Map<String, String> unchangedContents = new LinkedHashMap<>();
    for (String path : paths) {
      if (!PathFileUtils.isSupportedFile(path) || isInModelDiff(path)) {
        continue;
      }
      try {
        unchangedContents.put(path, repositoryStateFiles.content(path));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (unchangedContents.isEmpty()) {
      return false;
    }
    modelDiff.addUnchangedFiles(unchangedContents, true);
    if (contentsContexts != null) {
      unchangedContents.forEach(contentsContexts::addUnchangedContent);
    }
    return true;
  }

  public boolean isInModelDiff(String path) {
    return isInModel(modelDiff.getParentModel(), path) || isInModel(modelDiff.getChildModel(), path);
  }

  private static boolean isInModel(UMLModel umlModel, String path) {
    return umlModel.getClassList().stream().anyMatch(umlClass -> umlClass.getSourceFile().equals(path))
            || umlModel.getModuleList().stream().anyMatch(umlModule -> umlModule.getLocationInfo().getFilePath().equals(path));
  }

  private ReceiverScope scopeOf(@Nullable VariableDeclarationContainer container, LocationInfo location,
                                UMLAbstractClass context, UMLModel umlModel) {
    return new ReceiverScope(nodePool(container), container, location, context, umlModel, new HashSet<>());
  }

  private NodePool nodePool(@Nullable VariableDeclarationContainer container) {
    return nodePools.computeIfAbsent(container, key -> NodePool.of(container));
  }

  public Set<String> resolvePaths(ReceiverType receiverType) {
    String name = mainTypeName(receiverType.type());
    return name == null ? Set.of() : resolveName(name, receiverType.context());
  }

  private Set<String> resolveName(String name, UMLAbstractClass context) {
    try {
      return filePathResolver.resolveName(name, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private static String mainTypeName(UMLType type) {
    return FilePathResolver.mainTypeName(type);
  }

  @Nullable
  private ReceiverType getNodeType(LeafExpression node, ReceiverScope scope) {
    UMLAbstractClass context = scope.context();
    if (node instanceof ObjectCreation creation) {
      return new ReceiverType(creation.getType(), context);
    }
    if (node instanceof AbstractCall call) {
      return getReturnType(call, scope);
    }
    LocationInfo location = node.getLocationInfo();
    TernaryOperatorExpression conditional = scope.nodes().conditional(node);
    if (conditional != null) {
      return getConditionalType(conditional, scope);
    }
    switch (location.getCodeElementType()) {
      case SIMPLE_NAME:
      case QUALIFIED_NAME:
      case THIS_EXPRESSION:
        return getNameType(node.getString(), scope);
      case FIELD_ACCESS: {
        // the qualifier is the node the field access starts with (e.g., a call), or else a name or this
        String member = trailingIdentifier(node.getString());
        ReceiverType qualifierType = getQualifierType(node, member, scope);
        return qualifierType == null ? null : getMemberType(qualifierType, member, scope);
      }
      case TYPE_LITERAL: {
        // Java "Foo.class" has the type Class<Foo>
        String literal = node.getString();
        if (!context.getLANG().equals(Constants.JAVA) || !literal.endsWith(".class")) {
          return null;
        }
        String typeName = literal.substring(0, literal.length() - ".class".length());
        return new ReceiverType(UMLType.extractTypeObject("Class<" + typeName + ">", context.getLANG()), context);
      }
      case STRING_LITERAL:
      case TEXT_BLOCK:
        return isJvm(context) ? new ReceiverType(UMLType.extractTypeObject("String", context.getLANG()), context) : null;
      case NUMBER_LITERAL:
        return isJvm(context) ? new ReceiverType(UMLType.extractTypeObject(numberLiteralType(node.getString().trim(), context.getLANG()),
                context.getLANG()), context) : null;
      case BOOLEAN_LITERAL:
        return isJvm(context) ? new ReceiverType(UMLType.extractTypeObject(context.getLANG().equals(Constants.JAVA) ? "boolean" : "Boolean",
                context.getLANG()), context) : null;
      case CHAR_LITERAL:
        return isJvm(context) ? new ReceiverType(UMLType.extractTypeObject(context.getLANG().equals(Constants.JAVA) ? "char" : "Char",
                context.getLANG()), context) : null;
      case INFIX_EXPRESSION:
        return getInfixType(node, scope);
      case PARENTHESIZED_EXPRESSION: {
        LeafExpression inner = scope.nodes().outermostInside(location.getStartOffset(), location.getEndOffset());
        return inner == null ? null : getNodeType(inner, scope);
      }
      case CAST_EXPRESSION:
        return getCastType(node, scope);
      case ARRAY_ACCESS: {
        LeafExpression array = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), node);
        ReceiverType arrayType = array == null ? null : getNodeType(array, scope);
        return arrayType == null ? null : getIndexedElementType(arrayType);
      }
      case POSTFIX_EXPRESSION: {
        // Kotlin "x!!" and TypeScript "x!" assert that the operand is not null, keeping its type; RM records no operator
        // for postfix expressions, so it is the text after the operand node
        LeafExpression operand = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), node);
        boolean nonNullAssertion = operand != null && node.getString().startsWith(operand.getString())
                && node.getString().substring(operand.getString().length()).trim().matches("!!?");
        return nonNullAssertion ? getNodeType(operand, scope) : null;
      }
      default:
        return null;
    }
  }

  @Nullable
  private ReceiverType getNameType(String name, ReceiverScope scope) {
    UMLAbstractClass context = scope.context();
    if (name.equals(context.getLANG().THIS)) {
      return new ReceiverType(typeOf(context), context);
    }
    if (name.equals("super")) {
      // the type an anonymous class extends is the type of its creation
      UMLType superclass = superclassOf(context);
      return superclass == null ? null : new ReceiverType(superclass, context);
    }
    if (!NAME_CHAIN.matcher(name).matches()) {
      return null;
    }
    int separator = Math.max(name.lastIndexOf('.'), name.lastIndexOf("::"));
    if (separator < 0) {
      SimpleName simpleName = resolveSimpleName(name, scope);
      return simpleName.typeName() ? new ReceiverType(UMLType.extractTypeObject(name, context.getLANG()), context) : simpleName.type();
    }
    String qualifier = name.substring(0, separator);
    String member = name.substring(name.charAt(separator) == ':' ? separator + 2 : separator + 1);
    if (member.equals(context.getLANG().THIS)) {
      // the enclosing class named by the qualifier
      ClassRef enclosing = enclosingClass(lastSegment(qualifier), scope);
      return enclosing == null ? null : new ReceiverType(typeOf(enclosing.umlClass()), enclosing.umlClass());
    }
    if (member.equals("super")) {
      // the superclass of an enclosing class, or else a superinterface of the class
      ClassRef enclosing = enclosingClass(lastSegment(qualifier), scope);
      if (enclosing != null) {
        UMLType superclass = superclassOf(enclosing.umlClass());
        return superclass == null ? null : new ReceiverType(superclass, enclosing.umlClass());
      }
      return new ReceiverType(UMLType.extractTypeObject(qualifier, context.getLANG()), context);
    }
    ReceiverType qualifierType = getNameType(qualifier, scope);
    ReceiverType memberType = qualifierType == null ? null : getMemberType(qualifierType, member, scope);
    if (memberType != null) {
      return memberType;
    }
    if (isStaticName(name, scope)) {
      return new ReceiverType(UMLType.extractTypeObject(name, context.getLANG()), context);
    }
    return null;
  }

  @Nullable
  private ReceiverType getMemberType(ReceiverType qualifierType, String member, ReceiverScope scope) {
    ClassRef owner = getModelClass(qualifierType, scope.umlModel());
    if (owner == null) {
      return getJdkFieldType(qualifierType, member);
    }
    ReceiverType fieldType = getFieldType(findField(owner, member, scope.umlModel()), scope);
    if (fieldType != null) {
      return fieldType;
    }
    if (hasNestedClass(owner.umlClass(), member, scope.umlModel())) {
      String nestedName = mainTypeName(qualifierType.type()) + "." + member;
      return new ReceiverType(UMLType.extractTypeObject(nestedName, qualifierType.context().getLANG()), qualifierType.context(), owner.files());
    }
    return null;
  }

  @Nullable
  private ReceiverType getConditionalType(TernaryOperatorExpression conditional, ReceiverScope scope) {
    AbstractCodeFragment thenBranch = conditional.getThenExpression();
    AbstractCodeFragment elseBranch = conditional.getElseExpression();
    boolean thenNull = isNullLiteral(thenBranch);
    boolean elseNull = isNullLiteral(elseBranch);
    ReceiverType thenType = thenNull ? null : getFragmentType(thenBranch, scope);
    ReceiverType elseType = elseNull ? null : getFragmentType(elseBranch, scope);
    if (thenNull || elseNull) {
      return thenNull ? elseType : thenType;
    }
    if (thenType == null || elseType == null) {
      return null;
    }
    if (thenType.type().toQualifiedString().equals(elseType.type().toQualifiedString())) {
      return thenType.withFiles(elseType.files());
    }
    ClassRef thenClass = getModelClass(thenType, scope.umlModel());
    ClassRef elseClass = getModelClass(elseType, scope.umlModel());
    if (thenClass == null || elseClass == null) {
      return null;
    }
    Map<UMLAbstractClass, ClassRef> elseSupertypes = new LinkedHashMap<>();
    walkHierarchy(elseClass, scope.umlModel(), umlClass -> {
      elseSupertypes.put(umlClass.umlClass(), umlClass);
      return false;
    }, external -> {});
    List<ClassRef> common = new ArrayList<>();
    walkHierarchy(thenClass, scope.umlModel(), umlClass -> {
      if (elseSupertypes.containsKey(umlClass.umlClass())) {
        common.add(umlClass);
      }
      return false;
    }, external -> {});
    // the common supertypes no other common supertype is a subtype of
    List<ClassRef> minimal = common.stream().filter(candidate -> common.stream().noneMatch(other -> other != candidate
            && hierarchyOf(other, scope.umlModel()).classes().contains(candidate.umlClass()))).toList();
    if (minimal.size() != 1) {
      return null;
    }
    ClassRef supertype = minimal.get(0);
    Set<String> files = new LinkedHashSet<>(thenType.files());
    files.addAll(elseType.files());
    files.addAll(thenClass.files());
    files.addAll(elseClass.files());
    files.addAll(supertype.files());
    files.addAll(elseSupertypes.get(supertype.umlClass()).files());
    return new ReceiverType(typeOf(supertype.umlClass()), supertype.umlClass(), files);
  }

  @Nullable
  private ReceiverType getInfixType(LeafExpression infixExpression, ReceiverScope scope) {
    UMLAbstractClass context = scope.context();
    String operator = scope.nodes().operator(infixExpression);
    if (operator == null || !context.getLANG().equals(Constants.JAVA)) {
      return null;
    }
    if (Set.of("==", "!=", "<", ">", "<=", ">=", "&&", "||").contains(operator)) {
      return new ReceiverType(UMLType.extractTypeObject("boolean", context.getLANG()), context);
    }
    LocationInfo location = infixExpression.getLocationInfo();
    LeafExpression first = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), infixExpression);
    LeafExpression last = scope.nodes().earliestEndingAt(location.getStartOffset(), location.getEndOffset());
    ReceiverType firstType = first == null ? null : getNodeType(first, scope);
    ReceiverType lastType = last == null ? null : getNodeType(last, scope);
    if (operator.equals("+") && (isString(firstType) || isString(lastType))) {
      return new ReceiverType(UMLType.extractTypeObject("String", context.getLANG()), context);
    }
    String firstNumeric = numericType(firstType);
    String lastNumeric = numericType(lastType);
    if (firstNumeric == null || lastNumeric == null) {
      return null;
    }
    if (Set.of("<<", ">>", ">>>").contains(operator)) {
      return new ReceiverType(UMLType.extractTypeObject(promoted(firstNumeric, "int"), context.getLANG()), context);
    }
    if (Set.of("&", "|", "^").contains(operator) && firstNumeric.equals("boolean") && lastNumeric.equals("boolean")) {
      return new ReceiverType(UMLType.extractTypeObject("boolean", context.getLANG()), context);
    }
    if (firstNumeric.equals("boolean") || lastNumeric.equals("boolean")) {
      return null;
    }
    return new ReceiverType(UMLType.extractTypeObject(promoted(firstNumeric, lastNumeric), context.getLANG()), context);
  }

  private boolean isString(@Nullable ReceiverType type) {
    return type != null && type.type().getArrayDimension() == 0 && jdkClassOf(type) == String.class;
  }

  @Nullable
  private String numericType(@Nullable ReceiverType type) {
    if (type == null || type.type().getArrayDimension() > 0) {
      return null;
    }
    if (isPrimitive(type.type())) {
      return type.type().getClassType();
    }
    Class<?> boxed = jdkClassOf(type);
    return boxed == null ? null : BOXES.entrySet().stream().filter(entry -> boxed.getName().equals("java.lang." + entry.getValue()))
            .map(Map.Entry::getKey).findFirst().orElse(null);
  }

  private static String promoted(String type, String other) {
    for (String wider : List.of("double", "float", "long")) {
      if (type.equals(wider) || other.equals(wider)) {
        return wider;
      }
    }
    return "int";
  }

  private static boolean isNullLiteral(AbstractCodeFragment fragment) {
    return fragment.getNullLiterals().size() == 1 && fragment.getString().trim().equals(fragment.getNullLiterals().get(0).getString().trim());
  }

  private static String numberLiteralType(String literal, Constants language) {
    // the suffix, or else a fraction or an exponent of a decimal literal, gives the type; other literals are ints
    String text = literal.replace("_", "").toLowerCase();
    boolean java = language.equals(Constants.JAVA);
    boolean decimal = !text.startsWith("0x") && !text.startsWith("0b");
    if (text.endsWith("l")) {
      return java ? "long" : "Long";
    }
    if (decimal && text.endsWith("f")) {
      return java ? "float" : "Float";
    }
    if (decimal && (text.endsWith("d") || text.contains(".") || text.contains("e"))) {
      return java ? "double" : "Double";
    }
    return java ? "int" : "Int";
  }

  private static ReceiverType getIndexedElementType(ReceiverType indexedType) {
    if (indexedType.type().getArrayDimension() == 0) {
      return indexedType;
    }
    return new ReceiverType(arrayComponent(indexedType.type(), indexedType.context().getLANG()), indexedType.context(),
            indexedType.files(), indexedType.bindings());
  }

  private static UMLType arrayComponent(UMLType arrayType, Constants language) {
    String qualified = arrayType.toQualifiedString();
    return UMLType.extractTypeObject(qualified.substring(0, qualified.lastIndexOf("[]")), language);
  }

  @Nullable
  private ReceiverType getCastType(LeafExpression cast, ReceiverScope scope) {
    // RM records a cast only as an expression with its text (no UMLType), so the type is the text around its operand node
    LocationInfo location = cast.getLocationInfo();
    // the operand ends the Java cast "(Foo) x", and starts the TypeScript/Kotlin cast "x as Foo"
    LeafExpression operand = scope.nodes().earliestEndingAt(location.getStartOffset(), location.getEndOffset());
    if (operand == null) {
      operand = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), cast);
    }
    String text = cast.getString().trim();
    String typeText = null;
    if (operand != null && text.endsWith(operand.getString().trim())) {
      // "(Foo) x"
      typeText = text.substring(0, text.length() - operand.getString().trim().length()).trim();
      if (typeText.startsWith("(") && typeText.endsWith(")")) {
        typeText = typeText.substring(1, typeText.length() - 1).trim();
      }
    } else if (operand != null && text.startsWith(operand.getString().trim())) {
      // "x as Foo", "x as? Foo"
      typeText = text.substring(operand.getString().trim().length()).trim().replaceFirst("^as\\??\\s+", "");
    }
    if (typeText == null || typeText.isEmpty()) {
      return null;
    }
    return new ReceiverType(UMLType.extractTypeObject(typeText, scope.context().getLANG()), scope.context());
  }

  @Nullable
  private ReceiverType getCallReceiverType(AbstractCall call, ReceiverScope scope) {
    if (call.getExpression() == null) {
      // an unqualified call has the implicit receiver declaring the method (this, an enclosing instance, or a class
      // imported statically); constructor invocations have none
      LocationInfo.CodeElementType kind = call.getLocationInfo().getCodeElementType();
      if (kind.equals(LocationInfo.CodeElementType.CONSTRUCTOR_INVOCATION)
              || kind.equals(LocationInfo.CodeElementType.SUPER_CONSTRUCTOR_INVOCATION)) {
        return null;
      }
      if (call instanceof MethodReference) {
        // a method reference without a recorded receiver (e.g., Kotlin Foo::bar) cannot be told from ::bar
        return null;
      }
      List<Candidate> candidates = unqualifiedCandidates(call.getName(), scope);
      Candidate bound = selectCandidate(call, candidates, scope);
      if (bound == null && !candidates.isEmpty() && candidates.stream().map(ReceiverTypeResolver::declarer).distinct().count() == 1) {
        // the overload is not decided, but all candidates are declared in the same class
        bound = candidates.get(0);
      }
      return bound == null ? null : declaringType(bound);
    }
    LocationInfo location = call.getLocationInfo();
    LeafExpression receiver = scope.nodes().longestStartingAt(location.getStartOffset(), location.getEndOffset(), call);
    if (location.getCodeElementType().equals(LocationInfo.CodeElementType.SUPER_METHOD_INVOCATION)) {
      return getNameType(receiver == null ? "super" : receiver.getString() + ".super", scope);
    }
    // a node starting the call may be only a part of a receiver not recorded as a node (e.g., the package of a.b.C)
    boolean wholeReceiver = receiver != null && receiver.getString().trim().equals(call.getExpression().trim());
    return wholeReceiver ? getNodeType(receiver, scope) : getNameType(call.getExpression().trim(), scope);
  }

  private List<Candidate> invocationCandidates(AbstractCall call, ReceiverScope scope) {
    UMLModel umlModel = scope.umlModel();
    UMLAbstractClass context = scope.context();
    if (call instanceof ObjectCreation || call instanceof CreationReference) {
      UMLType type = call instanceof ObjectCreation creation ? (creation.isArray() ? null : creation.getType())
              : ((CreationReference) call).getType();
      ClassRef created = type == null ? null : getModelClass(new ReceiverType(type, context), umlModel);
      return created == null ? List.of() : constructors(created);
    }
    LocationInfo.CodeElementType kind = call.getLocationInfo().getCodeElementType();
    if (kind.equals(LocationInfo.CodeElementType.CONSTRUCTOR_INVOCATION)) {
      return constructors(new ClassRef(context, Set.of()));
    }
    if (kind.equals(LocationInfo.CodeElementType.SUPER_CONSTRUCTOR_INVOCATION)) {
      UMLType superclass = superclassOf(context);
      ClassRef superClass = superclass == null ? null : getModelClass(new ReceiverType(superclass, context), umlModel);
      return superClass == null ? List.of() : constructors(superClass);
    }
    if (call.getExpression() == null) {
      // a method reference without a recorded receiver (e.g., Kotlin Foo::bar) cannot be told from one naming a member of
      // the enclosing classes (::bar)
      return call instanceof MethodReference ? List.of() : unqualifiedCandidates(call.getName(), scope);
    }
    ReceiverType receiverType = getCallReceiverType(call, scope);
    if (receiverType == null) {
      return List.of();
    }
    ClassRef owner = getModelClass(receiverType, umlModel);
    return owner != null ? methods(owner, call.getName(), umlModel) : jdkMethods(receiverType, call.getName());
  }

  private List<Candidate> unqualifiedCandidates(String name, ReceiverScope scope) {
    // the methods of the innermost class having a member method with the name, or else the statically imported ones
    for (ClassRef umlClass : enclosingClasses(scope)) {
      List<Candidate> members = methods(umlClass, name, scope.umlModel());
      if (!members.isEmpty()) {
        return members;
      }
    }
    UMLAbstractClass context = scope.context();
    Optional<String> staticOwner = singleStaticImportOwner(name, context);
    if (staticOwner.isPresent()) {
      ClassRef importedClass = getImportedClass(staticOwner.get(), scope);
      return importedClass == null ? List.of() : methods(importedClass, name, scope.umlModel());
    }
    List<Candidate> imported = new ArrayList<>();
    for (String onDemandOwner : onDemandStaticImportOwners(context)) {
      ClassRef importedClass = getImportedClass(onDemandOwner, scope);
      if (importedClass != null) {
        imported.addAll(methods(importedClass, name, scope.umlModel()));
      }
    }
    return imported;
  }

  private List<Candidate> constructors(ClassRef owner) {
    List<Candidate> candidates = new ArrayList<>();
    for (UMLOperation operation : owner.umlClass().getOperations()) {
      if (operation.isConstructor()) {
        candidates.add(candidate(operation, owner));
      }
    }
    return candidates;
  }

  private List<Candidate> methods(ClassRef owner, String name, UMLModel umlModel) {
    List<Candidate> candidates = new ArrayList<>();
    Set<List<String>> signatures = new HashSet<>();
    List<ReceiverType> externals = new ArrayList<>();
    walkHierarchy(owner, umlModel, umlClass -> {
      for (UMLOperation operation : umlClass.umlClass().getOperations()) {
        if (operation.getName().equals(name) && !operation.isConstructor()) {
          Candidate candidate = candidate(operation, umlClass);
          if (signatures.add(candidate.signature())) {
            candidates.add(candidate);
          }
        }
      }
      return false;
    }, externals::add);
    for (ReceiverType external : externals) {
      Class<?> jdkClass = jdkClassOf(external);
      if (jdkClass != null) {
        candidates.addAll(jdkCandidates(jdkClass, external, name, signatures));
      }
    }
    if (owner.umlClass().getLANG().equals(Constants.JAVA)) {
      candidates.addAll(jdkCandidates(Object.class, objectType(owner.umlClass(), owner.files()), name, signatures));
    }
    return candidates;
  }

  private List<Candidate> jdkMethods(ReceiverType type, String name) {
    Class<?> jdkClass = type.type().getArrayDimension() > 0 ? null : jdkClassOf(type);
    if (jdkClass != null) {
      return jdkCandidates(jdkClass, type, name, new HashSet<>());
    }
    return type.context().getLANG().equals(Constants.JAVA)
            ? jdkCandidates(Object.class, objectType(type.context(), type.files()), name, new HashSet<>()) : List.of();
  }

  private List<Candidate> jdkCandidates(Class<?> jdkClass, ReceiverType owner, String name, Set<List<String>> signatures) {
    Map<java.lang.reflect.TypeVariable<?>, ReceiverType> bindings = jdkTypeBindings(jdkClass, owner);
    List<java.lang.reflect.Method> methods = new ArrayList<>(Arrays.asList(jdkClass.getMethods()));
    if (jdkClass.isInterface() && owner.context().getLANG().equals(Constants.JAVA)) {
      // the public methods of Object are members of interfaces too
      methods.addAll(Arrays.asList(Object.class.getMethods()));
    }
    List<Candidate> candidates = new ArrayList<>();
    for (java.lang.reflect.Method method : methods) {
      if (!method.getName().equals(name) || method.isBridge()) {
        continue;
      }
      List<String> signature = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList();
      if (!signatures.add(signature)) {
        continue;
      }
      List<ReceiverType> parameters = new ArrayList<>();
      for (java.lang.reflect.Type parameterType : method.getGenericParameterTypes()) {
        parameters.add(fromJdkType(parameterType, bindings, owner.context(), owner.files()));
      }
      candidates.add(new Candidate(null, null, method, owner, bindings, parameters, parameters.size(), method.isVarArgs(), signature));
    }
    return candidates;
  }

  private static ReceiverType objectType(UMLAbstractClass context, Set<String> files) {
    return new ReceiverType(UMLType.extractTypeObject("java.lang.Object", context.getLANG()), context, files);
  }

  private static Candidate candidate(UMLOperation operation, ClassRef declaring) {
    List<ReceiverType> parameters = new ArrayList<>();
    List<String> signature = new ArrayList<>();
    int required = 0;
    for (UMLParameter parameter : operation.getParametersWithoutReturnType()) {
      UMLType type = parameter.getType();
      signature.add(type == null ? "" : erasure(type));
      parameters.add(type == null || isUnboundTypeVariable(type, operation, declaring) ? null : declaredType(type, operation, declaring));
      // parameters with a default value may be left out
      if (parameter.getVariableDeclaration() == null || parameter.getVariableDeclaration().getInitializer() == null) {
        required++;
      }
    }
    return new Candidate(operation, declaring, null, null, Map.of(), parameters, required, operation.hasVarargsParameter(), signature);
  }

  private static boolean isUnboundTypeVariable(UMLType type, UMLOperation operation, ClassRef declaring) {
    String name = typeVariableName(type);
    if (name == null || declaring.bindings().containsKey(name)) {
      return false;
    }
    boolean declared = operation.getTypeParameters().stream().anyMatch(parameter -> parameter.getName().equals(name))
            || (declaring.umlClass() instanceof UMLClass umlClass
            && umlClass.getTypeParameters().stream().anyMatch(parameter -> parameter.getName().equals(name)));
    // a bounded type variable stands for its bound
    return declared && boundOf(type, operation, declaring.umlClass()) == type;
  }

  private static String erasure(UMLType type) {
    String name = mainTypeName(type);
    return (name == null ? type.toString() : lastSegment(name)) + "[]".repeat(type.getArrayDimension());
  }

  @Nullable
  private UMLOperation select(AbstractCall call, List<Candidate> candidates, ReceiverScope scope) {
    Candidate candidate = selectCandidate(call, candidates, scope);
    return candidate == null ? null : candidate.operation();
  }

  @Nullable
  private Candidate selectCandidate(AbstractCall call, List<Candidate> candidates, ReceiverScope scope) {
    if (candidates.isEmpty()) {
      return null;
    }
    if (call instanceof MethodReference || call instanceof CreationReference) {
      // the arity of a method reference comes from its target functional type, which RM does not represent
      return candidates.size() == 1 ? candidates.get(0) : null;
    }
    List<Argument> arguments = argumentsOf(call, scope);
    Set<Candidate> possibleInAnyPhase = new LinkedHashSet<>();
    boolean undecidedEarlierPhase = false;
    for (Phase phase : Phase.values()) {
      List<Candidate> possible = new ArrayList<>();
      List<Candidate> applicable = new ArrayList<>();
      for (Candidate candidate : candidates) {
        Fit fit = applicability(candidate, arguments, phase, scope.umlModel());
        if (fit != Fit.NO) {
          possible.add(candidate);
        }
        if (fit == Fit.YES) {
          applicable.add(candidate);
        }
      }
      possibleInAnyPhase.addAll(possible);
      if (possible.isEmpty()) {
        continue;
      }
      if (applicable.isEmpty() || undecidedEarlierPhase) {
        // the call may be bound in this phase or in a later one
        undecidedEarlierPhase = true;
        continue;
      }
      for (Candidate candidate : applicable) {
        if (possible.stream().allMatch(other -> other == candidate
                || strictlyMoreSpecific(candidate, other, phase, arguments.size(), scope.umlModel()))) {
          return candidate;
        }
      }
      return null;
    }
    return possibleInAnyPhase.size() == 1 ? possibleInAnyPhase.iterator().next() : null;
  }

  private List<Argument> argumentsOf(AbstractCall call, ReceiverScope scope) {
    List<LeafExpression> nodes = scope.nodes().argumentsOf(call);
    List<Argument> arguments = new ArrayList<>();
    for (int i = 0; i < call.arguments().size(); i++) {
      LeafExpression node = nodes.get(i);
      if (node != null && node.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.NULL_LITERAL)) {
        arguments.add(new Argument(null, true));
      } else {
        arguments.add(new Argument(node != null ? getNodeType(node, scope) : getNameType(call.arguments().get(i).trim(), scope), false));
      }
    }
    return arguments;
  }

  private Fit applicability(Candidate candidate, List<Argument> arguments, Phase phase, UMLModel umlModel) {
    int count = arguments.size();
    int parameters = candidate.parameters().size();
    if (!phase.equals(Phase.VARIABLE_ARITY)) {
      if (count > parameters || count < candidate.required()) {
        return Fit.NO;
      }
      return fitAll(arguments, i -> candidate.parameters().get(i), phase.equals(Phase.LOOSE), umlModel);
    }
    if (!candidate.varargs() || count < parameters - 1) {
      return Fit.NO;
    }
    ReceiverType last = candidate.parameters().get(parameters - 1);
    ReceiverType component = last == null || last.type().getArrayDimension() == 0 ? null
            : new ReceiverType(arrayComponent(last.type(), last.context().getLANG()), last.context(), last.files(), last.bindings());
    return fitAll(arguments, i -> i < parameters - 1 ? candidate.parameters().get(i) : component, true, umlModel);
  }

  private Fit fitAll(List<Argument> arguments, IntFunction<ReceiverType> parameterAt, boolean loose, UMLModel umlModel) {
    Fit result = Fit.YES;
    for (int i = 0; i < arguments.size(); i++) {
      Fit fit = fit(arguments.get(i), parameterAt.apply(i), loose, umlModel);
      if (fit == Fit.NO) {
        return Fit.NO;
      }
      if (fit == Fit.UNKNOWN) {
        result = Fit.UNKNOWN;
      }
    }
    return result;
  }

  private Fit fit(Argument argument, @Nullable ReceiverType parameter, boolean loose, UMLModel umlModel) {
    if (argument.nullLiteral()) {
      return parameter != null && isPrimitive(parameter.type()) ? Fit.NO : Fit.YES;
    }
    if (parameter == null) {
      // a type variable takes any reference, and a primitive by boxing
      if (argument.type() == null) {
        return loose ? Fit.YES : Fit.UNKNOWN;
      }
      return loose || !isPrimitive(argument.type().type()) ? Fit.YES : Fit.NO;
    }
    return argument.type() == null ? Fit.UNKNOWN : fitType(argument.type(), parameter, loose, umlModel);
  }

  private boolean strictlyMoreSpecific(Candidate candidate, Candidate other, Phase phase, int arguments, UMLModel umlModel) {
    return moreSpecific(candidate, other, phase, arguments, umlModel) && !moreSpecific(other, candidate, phase, arguments, umlModel);
  }

  private boolean moreSpecific(Candidate candidate, Candidate other, Phase phase, int arguments, UMLModel umlModel) {
    if (!phase.equals(Phase.VARIABLE_ARITY)) {
      if (candidate.parameters().size() != other.parameters().size()) {
        return false;
      }
      for (int i = 0; i < candidate.parameters().size(); i++) {
        if (!parameterMoreSpecific(candidate.parameters().get(i), other.parameters().get(i), umlModel)) {
          return false;
        }
      }
      return true;
    }
    for (int i = 0; i < arguments; i++) {
      if (!parameterMoreSpecific(variableArityParameter(candidate, i), variableArityParameter(other, i), umlModel)) {
        return false;
      }
    }
    return other.parameters().size() != arguments + 1
            || parameterMoreSpecific(variableArityParameter(candidate, arguments), variableArityParameter(other, arguments), umlModel);
  }

  @Nullable
  private static ReceiverType variableArityParameter(Candidate candidate, int index) {
    int last = candidate.parameters().size() - 1;
    if (index < last) {
      return candidate.parameters().get(index);
    }
    ReceiverType varargs = candidate.parameters().get(last);
    return varargs == null || varargs.type().getArrayDimension() == 0 ? varargs
            : new ReceiverType(arrayComponent(varargs.type(), varargs.context().getLANG()), varargs.context(), varargs.files(), varargs.bindings());
  }

  private boolean parameterMoreSpecific(@Nullable ReceiverType parameter, @Nullable ReceiverType otherParameter, UMLModel umlModel) {
    if (otherParameter == null) {
      return parameter == null || !isPrimitive(parameter.type());
    }
    if (parameter == null) {
      return jdkClassOf(otherParameter) == Object.class;
    }
    return fitType(parameter, otherParameter, false, umlModel) == Fit.YES;
  }

  private Fit fitType(ReceiverType argument, ReceiverType parameter, boolean loose, UMLModel umlModel) {
    UMLType argumentType = argument.type();
    UMLType parameterType = parameter.type();
    String argumentName = mainTypeName(argumentType);
    String parameterName = mainTypeName(parameterType);
    if (argumentName == null || parameterName == null) {
      return Fit.UNKNOWN;
    }
    int argumentDimension = argumentType.getArrayDimension();
    int parameterDimension = parameterType.getArrayDimension();
    if (argumentDimension != parameterDimension) {
      // an array is an Object, a Cloneable and a Serializable
      if (argumentDimension < parameterDimension) {
        return Fit.NO;
      }
      Class<?> parameterClass = jdkClassOf(parameter);
      return parameterClass == Object.class || parameterClass == Cloneable.class || parameterClass == java.io.Serializable.class
              ? Fit.YES : parameterClass != null || getModelClass(parameter, umlModel) != null ? Fit.NO : Fit.UNKNOWN;
    }
    if (argumentDimension > 0) {
      if (isPrimitive(argumentName) || isPrimitive(parameterName)) {
        return argumentName.equals(parameterName) ? Fit.YES : Fit.NO;
      }
      return fitReference(component(argument), component(parameter), umlModel);
    }
    boolean primitiveArgument = isPrimitive(argumentName);
    boolean primitiveParameter = isPrimitive(parameterName);
    if (primitiveArgument && primitiveParameter) {
      return argumentName.equals(parameterName) || widens(argumentName, parameterName) ? Fit.YES : Fit.NO;
    }
    if (primitiveArgument) {
      if (!loose) {
        return Fit.NO;
      }
      UMLType boxed = UMLType.extractTypeObject("java.lang." + BOXES.get(argumentName), argument.context().getLANG());
      return fitReference(new ReceiverType(boxed, argument.context()), parameter, umlModel);
    }
    if (primitiveParameter) {
      if (!loose) {
        return Fit.NO;
      }
      Class<?> argumentClass = jdkClassOf(argument);
      if (argumentClass == null) {
        return getModelClass(argument, umlModel) != null ? Fit.NO : Fit.UNKNOWN;
      }
      String unboxed = BOXES.entrySet().stream().filter(entry -> argumentClass.getSimpleName().equals(entry.getValue())
              && argumentClass.getPackageName().equals("java.lang")).map(Map.Entry::getKey).findFirst().orElse(null);
      return unboxed != null && (unboxed.equals(parameterName) || widens(unboxed, parameterName)) ? Fit.YES : Fit.NO;
    }
    return fitReference(argument, parameter, umlModel);
  }

  private Fit fitReference(ReceiverType argument, ReceiverType parameter, UMLModel umlModel) {
    Class<?> parameterClass = jdkClassOf(parameter);
    if (parameterClass == Object.class) {
      return Fit.YES;
    }
    ClassRef parameterModelClass = getModelClass(parameter, umlModel);
    ClassRef argumentModelClass = getModelClass(argument, umlModel);
    if (parameterModelClass == null && parameterClass != null && java.lang.reflect.Modifier.isFinal(parameterClass.getModifiers())) {
      // a final class has no subtypes
      return argumentModelClass == null && jdkClassOf(argument) == parameterClass ? Fit.YES : Fit.NO;
    }
    if (argumentModelClass != null) {
      Hierarchy hierarchy = hierarchyOf(argumentModelClass, umlModel);
      if (parameterModelClass != null && hierarchy.classes().contains(parameterModelClass.umlClass())) {
        return Fit.YES;
      }
      if (parameterModelClass == null && parameterClass != null && hierarchy.jdkClasses().stream().anyMatch(parameterClass::isAssignableFrom)) {
        return Fit.YES;
      }
      return hierarchy.complete() ? Fit.NO : Fit.UNKNOWN;
    }
    Class<?> argumentClass = jdkClassOf(argument);
    if (argumentClass == null) {
      // types outside the repository and the JDK are the same type when their names stand for the same imported type
      return parameterModelClass == null && parameterClass == null && sameImportedType(argument, parameter) ? Fit.YES : Fit.UNKNOWN;
    }
    // a JDK class has no supertype outside the JDK
    return parameterModelClass == null && parameterClass != null && parameterClass.isAssignableFrom(argumentClass) ? Fit.YES : Fit.NO;
  }

  private boolean sameImportedType(ReceiverType type, ReceiverType other) {
    String name = mainTypeName(type.type());
    String otherName = mainTypeName(other.type());
    if (name == null || otherName == null) {
      return false;
    }
    List<String> qualified = qualifiedNames(name, type.context());
    return qualified.size() == 1 && qualified.equals(qualifiedNames(otherName, other.context()));
  }

  private List<String> qualifiedNames(String name, UMLAbstractClass context) {
    try {
      String first = name.split("\\.")[0];
      String rest = name.substring(first.length());
      return filePathResolver.qualifiedNameCandidates(first, context).stream().map(candidate -> candidate + rest).toList();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ReceiverType component(ReceiverType arrayType) {
    UMLType type = arrayType.type();
    UMLType component = UMLType.extractTypeObject(type.toQualifiedString().replaceAll("(\\[])+$", ""), arrayType.context().getLANG());
    return new ReceiverType(component, arrayType.context(), arrayType.files(), arrayType.bindings());
  }

  @Nullable
  private Class<?> jdkClassOf(ReceiverType receiverType) {
    String name = mainTypeName(receiverType.type());
    return name == null ? null : jdkClass(name, receiverType.context());
  }

  private static boolean isPrimitive(UMLType type) {
    return type.getArrayDimension() == 0 && isPrimitive(type.getClassType());
  }

  private static boolean isPrimitive(String name) {
    return BOXES.containsKey(name);
  }

  private static boolean widens(String from, String to) {
    return WIDENINGS.getOrDefault(from, List.of()).contains(to);
  }

  @Nullable
  private ReceiverType getFragmentType(AbstractCodeFragment fragment, ReceiverScope scope) {
    LocationInfo location = fragment.getLocationInfo();
    LeafExpression node = scope.nodes().exactly(location.getStartOffset(), location.getEndOffset());
    return node != null ? getNodeType(node, scope) : getNameType(fragment.getString().trim(), scope);
  }

  private static String trailingIdentifier(String text) {
    int start = text.length();
    while (start > 0 && (Character.isJavaIdentifierPart(text.charAt(start - 1)))) {
      start--;
    }
    return text.substring(start);
  }

  @Nullable
  private static String qualifierName(String text, String member) {
    String qualifier = text.substring(0, text.length() - member.length()).trim();
    for (String separator : MEMBER_SEPARATORS) {
      if (qualifier.endsWith(separator)) {
        return qualifier.substring(0, qualifier.length() - separator.length()).trim();
      }
    }
    return null;
  }

  private SimpleName resolveSimpleName(String name, ReceiverScope scope) {
    NameLookup lookup = lookupName(name, scope);
    if (lookup.local() != null) {
      ReceiverScope declaring = lookup.scope();
      return SimpleName.variable(getDeclarationType(lookup.local(), declaring.container(), new ClassRef(declaring.context(), Set.of()), declaring));
    }
    return lookup.field() != null ? SimpleName.variable(getFieldType(lookup.field().field(), scope)) : SimpleName.TYPE_NAME;
  }

  private NameLookup lookupName(String name, ReceiverScope scope) {
    // the locals of the use, then the members of its class; the body of an anonymous class is in turn enclosed by the
    // locals and the class of the code creating it
    ReceiverScope current = scope;
    while (true) {
      VariableDeclaration local = findLocal(name, current);
      if (local != null) {
        return new NameLookup(local, null, current);
      }
      if (!(current.context() instanceof UMLAnonymousClass anonymous)) {
        break;
      }
      Found<DeclaredField> field = findField(new ClassRef(anonymous, Set.of()), name, current.umlModel());
      if (field != null) {
        return new NameLookup(null, new UnqualifiedField(field), current);
      }
      ReceiverScope enclosing = enclosingScope(anonymous, current);
      if (enclosing == null) {
        break;
      }
      current = enclosing;
    }
    return new NameLookup(null, findUnqualifiedField(name, current), current);
  }

  @Nullable
  private ReceiverScope enclosingScope(UMLAnonymousClass anonymous, ReceiverScope scope) {
    LocationInfo location = anonymous.getLocationInfo();
    VariableDeclarationContainer parent = null;
    for (VariableDeclarationContainer candidate : anonymous.getParentContainers()) {
      if (candidate.getLocationInfo().subsumes(location)
              && (parent == null || parent.getLocationInfo().subsumes(candidate.getLocationInfo()))) {
        parent = candidate;
      }
    }
    UMLAbstractClass owner = parent == null ? null : declaringClass(parent, scope.umlModel());
    if (owner == null) {
      return null;
    }
    VariableDeclarationContainer container = parent;
    for (VariableDeclarationContainer lambda : containerAndLambdas(parent)) {
      if (lambda.getLocationInfo().subsumes(location) && container.getLocationInfo().subsumes(lambda.getLocationInfo())) {
        container = lambda;
      }
    }
    return new ReceiverScope(nodePool(container), container, location, owner, scope.umlModel(), scope.visiting());
  }

  @Nullable
  private UMLAbstractClass declaringClass(VariableDeclarationContainer member, UMLModel umlModel) {
    for (UMLClass umlClass : classesOfFile(member.getLocationInfo().getFilePath(), umlModel)) {
      List<UMLAbstractClass> classes = new ArrayList<>(List.of(umlClass));
      classes.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass candidate : classes) {
        if (Stream.of(candidate.getOperations(), candidate.getInitializers(), candidate.getAttributes(), candidate.getEnumConstants())
                .flatMap(List::stream).anyMatch(declared -> declared == member)) {
          return candidate;
        }
      }
    }
    return null;
  }

  private List<ClassRef> enclosingClasses(ReceiverScope scope) {
    List<ClassRef> classes = new ArrayList<>();
    ReceiverScope current = scope;
    while (current.context() instanceof UMLAnonymousClass anonymous) {
      classes.add(new ClassRef(anonymous, Set.of()));
      ReceiverScope enclosing = enclosingScope(anonymous, current);
      if (enclosing == null) {
        return classes;
      }
      current = enclosing;
    }
    classes.addAll(classAndEnclosingClasses(current.context(), current.umlModel()));
    return classes;
  }

  @Nullable
  private static VariableDeclaration findLocal(String name, ReceiverScope scope) {
    if (scope.container() == null) {
      return null;
    }
    VariableDeclaration declaration = null;
    // the variables of the container, of the lambdas enclosing the use, and of the containers enclosing a lambda; the
    // innermost declaration shadows the others
    List<VariableDeclarationContainer> containers = new ArrayList<>(containerAndLambdas(scope.container()));
    for (VariableDeclarationContainer current = scope.container(); current instanceof LambdaExpressionObject lambda
            && lambda.getOwner() != null; current = lambda.getOwner()) {
      containers.add(lambda.getOwner());
    }
    for (VariableDeclarationContainer container : containers) {
      if (container != scope.container() && !container.getLocationInfo().subsumes(scope.location())) {
        continue;
      }
      for (VariableDeclaration inScope : container.getVariableDeclarationsInScope(scope.location())) {
        if (inScope.getVariableName().equals(name) && (declaration == null
                || inScope.getLocationInfo().getStartOffset() > declaration.getLocationInfo().getStartOffset())) {
          declaration = inScope;
        }
      }
    }
    return declaration;
  }

  @Nullable
  private UnqualifiedField findUnqualifiedField(String name, ReceiverScope scope) {
    UMLAbstractClass context = scope.context();
    for (ClassRef umlClass : classAndEnclosingClasses(context, scope.umlModel())) {
      Found<DeclaredField> field = findField(umlClass, name, scope.umlModel());
      if (field != null) {
        return new UnqualifiedField(field);
      }
    }
    Optional<String> staticOwner = singleStaticImportOwner(name, context);
    if (staticOwner.isPresent()) {
      ClassRef owner = getImportedClass(staticOwner.get(), scope);
      return new UnqualifiedField(owner == null ? null : findField(owner, name, scope.umlModel()));
    }
    for (String onDemandOwner : onDemandStaticImportOwners(context)) {
      ClassRef owner = getImportedClass(onDemandOwner, scope);
      Found<DeclaredField> field = owner == null ? null : findField(owner, name, scope.umlModel());
      if (field != null) {
        return new UnqualifiedField(field);
      }
    }
    return null;
  }

  private static ReceiverType declaringType(Candidate candidate) {
    if (candidate.declaring() != null) {
      UMLAbstractClass declaringClass = candidate.declaring().umlClass();
      return new ReceiverType(typeOf(declaringClass), declaringClass, candidate.declaring().files());
    }
    ReceiverType owner = candidate.jdkOwner();
    return new ReceiverType(UMLType.extractTypeObject(jdkTypeName(candidate.jdkMethod().getDeclaringClass()), owner.context().getLANG()),
            owner.context(), owner.files());
  }

  private static Object declarer(Candidate candidate) {
    return candidate.declaring() != null ? candidate.declaring().umlClass() : candidate.jdkMethod().getDeclaringClass();
  }

  @Nullable
  private static ReceiverType getImplicitReceiverType(Found<ClassRef> declaring) {
    UMLAbstractClass declaringClass = declaring.value().umlClass();
    return new ReceiverType(typeOf(declaringClass), declaringClass, declaring.files());
  }

  private List<ClassRef> classAndEnclosingClasses(UMLAbstractClass umlClass, UMLModel umlModel) {
    List<ClassRef> classes = new ArrayList<>();
    classes.add(new ClassRef(umlClass, Set.of()));
    String name = umlClass.getName();
    List<UMLClass> fileClasses = classesOfFile(umlClass.getSourceFile(), umlModel);
    for (int dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.', dot - 1)) {
      String enclosingName = name.substring(0, dot);
      fileClasses.stream().filter(fileClass -> fileClass.getName().equals(enclosingName)).findFirst()
              .ifPresent(enclosing -> classes.add(new ClassRef(enclosing, Set.of())));
    }
    return classes;
  }

  @Nullable
  private ClassRef enclosingClass(String simpleName, ReceiverScope scope) {
    return classAndEnclosingClasses(scope.context(), scope.umlModel()).stream()
            .filter(umlClass -> lastSegment(umlClass.umlClass().getNonQualifiedName()).equals(simpleName)).findFirst().orElse(null);
  }

  private static boolean isJvm(UMLAbstractClass umlClass) {
    return umlClass.getLANG().equals(Constants.JAVA) || umlClass.getLANG().equals(Constants.KOTLIN);
  }

  @Nullable
  private ClassRef getImportedClass(String qualifiedName, ReceiverScope scope) {
    try {
      // the qualified name of an import is resolved as the import itself
      Set<String> paths = filePathResolver.resolvePaths(scope.context().getSourceFile(), qualifiedName);
      return findClass(paths, lastSegment(qualifiedName), Set.of(), scope.umlModel());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<String> singleStaticImportOwner(String name, UMLAbstractClass context) {
    try {
      return filePathResolver.singleStaticImportOwner(name, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> onDemandStaticImportOwners(UMLAbstractClass context) {
    try {
      return filePathResolver.onDemandStaticImportOwners(context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean hasNestedClass(UMLAbstractClass owner, String name, UMLModel umlModel) {
    return classesOfFile(owner.getSourceFile(), umlModel).stream()
            .anyMatch(umlClass -> umlClass.getName().equals(owner.getName() + "." + name));
  }

  @Nullable
  private ReceiverType getFieldType(@Nullable Found<DeclaredField> found, ReceiverScope scope) {
    if (found == null) {
      return null;
    }
    DeclaredField declared = found.value();
    UMLAttribute attribute = declared.attribute();
    if (attribute == null) {
      // an enum constant has the type of its enum
      UMLAbstractClass enumClass = declared.declaringClass().umlClass();
      return new ReceiverType(typeOf(enumClass), enumClass, found.files());
    }
    return getDeclarationType(attribute.getVariableDeclaration(), null, new ClassRef(declared.declaringClass().umlClass(),
            found.files(), declared.declaringClass().bindings()), scope);
  }

  @Nullable
  private Found<DeclaredField> findField(ClassRef owner, String name, UMLModel umlModel) {
    // a private field is no member of the subclasses of its class, so a name there is looked up further (in the
    // enclosing scope of an anonymous subclass, as a local the anonymous class captures)
    return findInHierarchy(owner, umlModel, umlClass -> {
      DeclaredField field = findOwnField(umlClass, name);
      return field != null && umlClass.umlClass() != owner.umlClass() && field.member() != null
              && field.member().getVisibility() == Visibility.PRIVATE ? null : field;
    });
  }

  @Nullable
  private static DeclaredField findOwnField(ClassRef umlClass, String name) {
    for (UMLEnumConstant enumConstant : umlClass.umlClass().getEnumConstants()) {
      if (enumConstant.getName().equals(name)) {
        return new DeclaredField(null, umlClass, enumConstant);
      }
    }
    for (UMLAttribute attribute : umlClass.umlClass().getAttributes()) {
      if (attribute.getName().equals(name)) {
        return new DeclaredField(attribute, umlClass, null);
      }
    }
    return null;
  }

  @Nullable
  private <T> Found<T> findInHierarchy(ClassRef start, UMLModel umlModel, Function<ClassRef, T> finder) {
    List<Found<T>> found = new ArrayList<>();
    walkHierarchy(start, umlModel, current -> {
      T value = finder.apply(current);
      if (value != null) {
        found.add(new Found<>(value, current.files()));
      }
      return value != null;
    }, external -> {});
    return found.isEmpty() ? null : found.get(0);
  }

  private void walkHierarchy(ClassRef start, UMLModel umlModel, Predicate<ClassRef> visitor, Consumer<ReceiverType> externalVisitor) {
    Set<UMLAbstractClass> visited = new HashSet<>();
    Deque<PendingSupertype> interfaces = new ArrayDeque<>();
    for (ClassRef current = start; current != null && visited.add(current.umlClass()); ) {
      if (visitor.test(current)) {
        return;
      }
      for (UMLType implementedInterface : current.umlClass().getImplementedInterfaces()) {
        interfaces.add(new PendingSupertype(implementedInterface, current));
      }
      UMLType superclass = superclassOf(current.umlClass());
      ReceiverType superclassType = superclass == null ? null : supertypeOf(superclass, current);
      ClassRef next = superclassType == null ? null : getModelClass(superclassType, umlModel);
      if (superclassType != null && next == null) {
        externalVisitor.accept(superclassType);
      }
      current = next;
    }
    while (!interfaces.isEmpty()) {
      PendingSupertype pending = interfaces.poll();
      ReceiverType supertype = supertypeOf(pending.type(), pending.subtype());
      ClassRef current = getModelClass(supertype, umlModel);
      if (current == null) {
        externalVisitor.accept(supertype);
        continue;
      }
      if (!visited.add(current.umlClass())) {
        continue;
      }
      if (visitor.test(current)) {
        return;
      }
      if (current.umlClass().getSuperclass() != null) {
        interfaces.add(new PendingSupertype(current.umlClass().getSuperclass(), current));
      }
      for (UMLType implementedInterface : current.umlClass().getImplementedInterfaces()) {
        interfaces.add(new PendingSupertype(implementedInterface, current));
      }
    }
  }

  private Hierarchy hierarchyOf(ClassRef start, UMLModel umlModel) {
    Set<UMLAbstractClass> classes = new HashSet<>();
    Set<Class<?>> jdkClasses = new LinkedHashSet<>();
    boolean[] complete = {true};
    walkHierarchy(start, umlModel, current -> {
      classes.add(current.umlClass());
      return false;
    }, external -> {
      Class<?> jdkClass = jdkClassOf(external);
      if (jdkClass != null) {
        jdkClasses.add(jdkClass);
      } else {
        complete[0] = false;
      }
    });
    if (start.umlClass().getLANG().equals(Constants.JAVA)) {
      jdkClasses.add(Object.class);
    }
    return new Hierarchy(classes, jdkClasses, complete[0]);
  }

  @Nullable
  private UMLType superclassOf(UMLAbstractClass umlClass) {
    return umlClass instanceof UMLAnonymousClass anonymous ? anonymousSupertype(anonymous) : umlClass.getSuperclass();
  }

  @Nullable
  private UMLType anonymousSupertype(UMLAnonymousClass anonymous) {
    return anonymousSupertypes.computeIfAbsent(anonymous, key -> {
      ObjectCreation innermost = null;
      for (VariableDeclarationContainer parent : anonymous.getParentContainers()) {
        if (parent instanceof UMLEnumConstant enumConstant) {
          return UMLType.extractTypeObject(lastSegment(enumConstant.getClassName()), anonymous.getLANG());
        }
        for (VariableDeclarationContainer container : containerAndLambdas(parent)) {
          for (AbstractCall creation : container.getAllCreations()) {
            if (creation instanceof ObjectCreation objectCreation && objectCreation.getAnonymousClassDeclaration() != null
                    && creation.getLocationInfo().subsumes(anonymous.getLocationInfo())
                    && (innermost == null || innermost.getLocationInfo().subsumes(creation.getLocationInfo()))) {
              innermost = objectCreation;
            }
          }
        }
      }
      return innermost == null ? null : innermost.getType();
    });
  }

  private static ReceiverType supertypeOf(UMLType supertype, ClassRef subtype) {
    return new ReceiverType(supertype, subtype.umlClass(), subtype.files(), subtype.bindings());
  }

  private static UMLType typeOf(UMLAbstractClass umlClass) {
    // the simple name, as the class is referred to in its own scope
    return UMLType.extractTypeObject(lastSegment(umlClass.getNonQualifiedName()), umlClass.getLANG());
  }

  @Nullable
  private ReceiverType getReturnType(AbstractCall call, ReceiverScope scope) {
    if (call instanceof MethodReference || call instanceof CreationReference) {
      // a reference is a value of the functional type it is converted to, not of what the referenced method returns
      return null;
    }
    Candidate bound = selectCandidate(call, invocationCandidates(call, scope), scope);
    if (bound == null) {
      return null;
    }
    if (bound.jdkMethod() != null) {
      return fromJdkType(bound.jdkMethod().getGenericReturnType(), bound.jdkBindings(), bound.jdkOwner().context(), bound.jdkOwner().files());
    }
    UMLOperation operation = bound.operation();
    if (operation.getReturnParameter() == null || !isWrittenType(operation.getReturnParameter().getType())) {
      return null;
    }
    UMLType returnType = operation.getReturnParameter().getType();
    String returnTypeVariable = typeVariableName(returnType);
    ReceiverType inferred = returnTypeVariable == null ? null : inferTypeParameter(operation, returnTypeVariable, call, scope);
    return (inferred != null ? inferred : declaredType(returnType, operation, bound.declaring())).withFiles(bound.declaring().files());
  }

  @Nullable
  private ReceiverType inferTypeParameter(UMLOperation operation, String typeParameter, AbstractCall call, ReceiverScope scope) {
    if (operation.getTypeParameters().stream().noneMatch(parameter -> parameter.getName().equals(typeParameter))) {
      return null;
    }
    List<VariableDeclaration> parameters = operation.getParameterDeclarationList();
    List<LeafExpression> arguments = scope.nodes().argumentsOf(call);
    for (int i = 0; i < parameters.size() && i < call.arguments().size(); i++) {
      UMLType parameterType = parameters.get(i).getType();
      if (parameterType == null) {
        continue;
      }
      List<UMLType> parameterArguments = parameterType.getTypeArguments() == null ? List.of() : parameterType.getTypeArguments();
      int argumentIndex = -1;
      for (int j = 0; j < parameterArguments.size(); j++) {
        if (typeParameter.equals(typeVariableName(parameterArguments.get(j)))) {
          argumentIndex = j;
        }
      }
      if (!typeParameter.equals(typeVariableName(parameterType)) && argumentIndex < 0) {
        continue;
      }
      ReceiverType argumentType = arguments.get(i) != null ? getNodeType(arguments.get(i), scope)
              : getNameType(call.arguments().get(i).trim(), scope);
      if (argumentType == null) {
        continue;
      }
      if (argumentIndex < 0) {
        return argumentType;
      }
      List<UMLType> argumentArguments = argumentType.type().getTypeArguments();
      if (argumentArguments != null && argumentIndex < argumentArguments.size()) {
        ReceiverType inferred = typeArgument(argumentType, argumentArguments.get(argumentIndex));
        if (inferred != null) {
          return inferred;
        }
      }
    }
    return null;
  }

  private static UMLType boundOf(UMLType type, @Nullable VariableDeclarationContainer declaration, UMLAbstractClass umlClass) {
    List<UMLTypeParameter> typeParameters = new ArrayList<>();
    if (declaration != null) {
      typeParameters.addAll(declaration.getTypeParameters());
    }
    if (umlClass instanceof UMLClass classWithTypeParameters) {
      typeParameters.addAll(classWithTypeParameters.getTypeParameters());
    }
    Set<String> replaced = new HashSet<>();
    UMLType bound = type;
    // a bound may itself be a type parameter (e.g., <T extends U, U extends Foo>)
    for (String name = typeVariableName(bound); name != null && replaced.add(name); name = typeVariableName(bound)) {
      String typeVariable = name;
      Optional<UMLTypeParameter> typeParameter = typeParameters.stream()
              .filter(parameter -> parameter.getName().equals(typeVariable) && !parameter.getTypeBounds().isEmpty()).findFirst();
      if (typeParameter.isEmpty()) {
        break;
      }
      bound = typeParameter.get().getTypeBounds().get(0);
    }
    return bound;
  }

  @Nullable
  private ReceiverType getDeclarationType(@Nullable VariableDeclaration declaration, @Nullable VariableDeclarationContainer container,
                                          ClassRef declaring, ReceiverScope scope) {
    if (declaration == null) {
      return null;
    }
    if (isWrittenType(declaration.getType())) {
      return declaredType(declaration.getType(), container, declaring);
    }
    if (!scope.visiting().add(declaration)) {
      return null;
    }
    UMLAbstractClass context = declaring.umlClass();
    if (declaration.getInitializer() == null) {
      // an inferred enhanced-for variable has the element type of the iterated expression
      AbstractExpression iterated = container == null ? null : enhancedForExpression(declaration, container);
      if (iterated == null) {
        // an implicitly typed lambda parameter has the type the functional interface of the lambda gives it
        ReceiverType lambdaParameterType = container == null ? null : getLambdaParameterType(declaration, container, scope);
        return lambdaParameterType == null ? null : lambdaParameterType.withFiles(declaring.files());
      }
      ReceiverType iteratedType = getFragmentType(iterated, new ReceiverScope(scope.nodes(), container,
              iterated.getLocationInfo(), context, scope.umlModel(), scope.visiting()));
      ReceiverType elementType = iteratedType == null ? null : getElementType(iteratedType, scope.umlModel());
      return elementType == null ? null : elementType.withFiles(declaring.files());
    }
    // inferred or untyped declarations take the type of their initializer
    AbstractExpression initializer = declaration.getInitializer();
    NodePool initializerNodes = nodePools.computeIfAbsent(initializer, key -> NodePool.of(initializer));
    ReceiverType initializerType = getFragmentType(initializer,
            new ReceiverScope(initializerNodes, container, declaration.getLocationInfo(), context, scope.umlModel(), scope.visiting()));
    return initializerType == null ? null : initializerType.withFiles(declaring.files());
  }

  @Nullable
  private ReceiverType getLambdaParameterType(VariableDeclaration parameter, VariableDeclarationContainer container, ReceiverScope scope) {
    LambdaExpressionObject lambda = lambdaDeclaring(parameter, container);
    if (lambda == null || lambda.getOwner() == null) {
      return null;
    }
    int index = -1;
    for (int i = 0; i < lambda.getParameters().size(); i++) {
      if (lambda.getParameters().get(i) == parameter) {
        index = i;
      }
    }
    NodePool nodes = nodePool(lambda.getOwner());
    Map.Entry<AbstractCall, Integer> taking = nodes.callTaking(lambda);
    if (index < 0 || taking == null) {
      return null;
    }
    AbstractCall call = taking.getKey();
    ReceiverScope callScope = new ReceiverScope(nodes, lambda.getOwner(), call.getLocationInfo(), scope.context(), scope.umlModel(), scope.visiting());
    Candidate candidate = selectCandidate(call, invocationCandidates(call, callScope), callScope);
    if (candidate == null) {
      return null;
    }
    if (candidate.jdkMethod() != null) {
      // the parameter of a JDK method stays a reflected type, with what the type variables in it stand for
      java.lang.reflect.Type[] parameterTypes = candidate.jdkMethod().getGenericParameterTypes();
      int last = parameterTypes.length - 1;
      java.lang.reflect.Type parameterType = !candidate.varargs() || taking.getValue() < last ? parameterTypes[taking.getValue()]
              : parameterTypes[last] instanceof java.lang.reflect.GenericArrayType array ? array.getGenericComponentType()
              : ((Class<?>) parameterTypes[last]).getComponentType();
      return getJdkFunctionalParameterType(parameterType, candidate.jdkBindings(), candidate.jdkOwner(), index);
    }
    ReceiverType functionalType = calledParameterType(candidate, taking.getValue());
    return functionalType == null ? null : getFunctionalParameterType(functionalType, index, scope.umlModel());
  }

  @Nullable
  private static LambdaExpressionObject lambdaDeclaring(VariableDeclaration parameter, VariableDeclarationContainer container) {
    VariableDeclarationContainer outermost = container;
    while (outermost instanceof LambdaExpressionObject lambda && lambda.getOwner() != null) {
      outermost = lambda.getOwner();
    }
    for (VariableDeclarationContainer candidate : containerAndLambdas(outermost)) {
      if (candidate instanceof LambdaExpressionObject lambda && lambda.getParameters().stream().anyMatch(declared -> declared == parameter)) {
        return lambda;
      }
    }
    return null;
  }

  @Nullable
  private static ReceiverType calledParameterType(Candidate candidate, int argument) {
    int parameters = candidate.parameters().size();
    if (candidate.varargs() && argument >= parameters - 1) {
      return variableArityParameter(candidate, argument);
    }
    return argument < parameters ? candidate.parameters().get(argument) : null;
  }

  @Nullable
  private ReceiverType getJdkFunctionalParameterType(java.lang.reflect.Type functionalType,
                                                     Map<java.lang.reflect.TypeVariable<?>, ReceiverType> bindings, ReceiverType owner, int index) {
    Class<?> functionalInterface = functionalType instanceof Class<?> typeClass ? typeClass
            : functionalType instanceof java.lang.reflect.ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw ? raw
            : null;
    java.lang.reflect.Method method = singleAbstractMethod(functionalInterface);
    if (method == null || index >= method.getParameterCount()) {
      return null;
    }
    Map<java.lang.reflect.TypeVariable<?>, ReceiverType> interfaceBindings = new HashMap<>();
    if (functionalType instanceof java.lang.reflect.ParameterizedType parameterized) {
      java.lang.reflect.TypeVariable<?>[] variables = functionalInterface.getTypeParameters();
      java.lang.reflect.Type[] arguments = parameterized.getActualTypeArguments();
      for (int i = 0; i < variables.length && i < arguments.length; i++) {
        // "? super T" and "? extends T" give the lambda parameter the type T
        ReceiverType bound = fromJdkType(arguments[i], bindings, owner.context(), owner.files());
        if (bound != null) {
          interfaceBindings.put(variables[i], bound);
        }
      }
    }
    bindSupertypes(functionalInterface, interfaceBindings, owner, new HashSet<>());
    return fromJdkType(method.getGenericParameterTypes()[index], interfaceBindings, owner.context(), owner.files());
  }

  @Nullable
  private ReceiverType getFunctionalParameterType(ReceiverType functionalType, int index, UMLModel umlModel) {
    ClassRef modelInterface = getModelClass(functionalType, umlModel);
    if (modelInterface != null) {
      if (!(modelInterface.umlClass() instanceof UMLClass umlClass) || !umlClass.isInterface()) {
        return null;
      }
      // the type arguments of a lambda's target bind the interface's type parameters, also through wildcards
      Map<String, ReceiverType> bindings = new HashMap<>();
      List<UMLType> arguments = functionalType.type().getTypeArguments();
      if (arguments != null && arguments.size() == umlClass.getTypeParameters().size()) {
        for (int i = 0; i < arguments.size(); i++) {
          ReceiverType bound = lambdaTypeArgument(functionalType, arguments.get(i));
          if (bound != null) {
            bindings.put(umlClass.getTypeParameters().get(i).getName(), bound);
          }
        }
      }
      List<Map.Entry<UMLOperation, ClassRef>> abstractMethods = new ArrayList<>();
      Set<List<String>> signatures = new HashSet<>();
      walkHierarchy(new ClassRef(umlClass, modelInterface.files(), bindings), umlModel, current -> {
        for (UMLOperation operation : current.umlClass().getOperations()) {
          if (operation.isStatic() || operation.isConstructor() || !signatures.add(candidate(operation, current).signature())) {
            continue;
          }
          if (operation.getBody() == null && !operation.isDefault() && !isObjectMethod(operation)) {
            abstractMethods.add(Map.entry(operation, current));
          }
        }
        return false;
      }, external -> {});
      if (abstractMethods.size() != 1) {
        return null;
      }
      UMLOperation method = abstractMethods.get(0).getKey();
      List<UMLParameter> parameters = method.getParametersWithoutReturnType();
      if (index >= parameters.size() || parameters.get(index).getType() == null
              || isUnboundTypeVariable(parameters.get(index).getType(), method, abstractMethods.get(0).getValue())) {
        return null;
      }
      return declaredType(parameters.get(index).getType(), method, abstractMethods.get(0).getValue());
    }
    Class<?> jdkInterface = jdkClassOf(functionalType);
    java.lang.reflect.Method method = singleAbstractMethod(jdkInterface);
    if (method == null || index >= method.getParameterCount()) {
      return null;
    }
    return fromJdkType(method.getGenericParameterTypes()[index], jdkTypeBindings(jdkInterface, functionalType), functionalType.context(),
            functionalType.files());
  }

  private static boolean isObjectMethod(UMLOperation operation) {
    return Arrays.stream(Object.class.getMethods()).anyMatch(method -> method.getName().equals(operation.getName())
            && method.getParameterCount() == operation.getParametersWithoutReturnType().size());
  }

  @Nullable
  private static java.lang.reflect.Method singleAbstractMethod(@Nullable Class<?> type) {
    if (type == null || !type.isInterface()) {
      return null;
    }
    List<java.lang.reflect.Method> abstractMethods = new ArrayList<>();
    for (java.lang.reflect.Method method : type.getMethods()) {
      if (!java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
        continue;
      }
      try {
        Object.class.getMethod(method.getName(), method.getParameterTypes());
        continue;
      } catch (NoSuchMethodException e) {
        // not a method of Object
      }
      if (abstractMethods.stream().noneMatch(other -> other.getName().equals(method.getName())
              && Arrays.equals(other.getParameterTypes(), method.getParameterTypes()))) {
        abstractMethods.add(method);
      }
    }
    return abstractMethods.size() == 1 ? abstractMethods.get(0) : null;
  }

  private Map<java.lang.reflect.TypeVariable<?>, ReceiverType> jdkTypeBindings(Class<?> rawType, ReceiverType type) {
    Map<java.lang.reflect.TypeVariable<?>, ReceiverType> bindings = new HashMap<>();
    java.lang.reflect.TypeVariable<?>[] variables = rawType.getTypeParameters();
    List<UMLType> arguments = type.type().getTypeArguments();
    if (arguments != null && arguments.size() == variables.length) {
      for (int i = 0; i < variables.length; i++) {
        ReceiverType bound = lambdaTypeArgument(type, arguments.get(i));
        if (bound != null) {
          bindings.put(variables[i], bound);
        }
      }
    }
    bindSupertypes(rawType, bindings, type, new HashSet<>());
    return bindings;
  }

  private void bindSupertypes(Class<?> rawType, Map<java.lang.reflect.TypeVariable<?>, ReceiverType> bindings, ReceiverType type,
                              Set<Class<?>> visited) {
    if (!visited.add(rawType)) {
      return;
    }
    List<java.lang.reflect.Type> supertypes = new ArrayList<>(Arrays.asList(rawType.getGenericInterfaces()));
    if (rawType.getGenericSuperclass() != null) {
      supertypes.add(rawType.getGenericSuperclass());
    }
    for (java.lang.reflect.Type supertype : supertypes) {
      if (supertype instanceof java.lang.reflect.ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> superclass) {
        java.lang.reflect.TypeVariable<?>[] variables = superclass.getTypeParameters();
        java.lang.reflect.Type[] arguments = parameterized.getActualTypeArguments();
        for (int i = 0; i < variables.length && i < arguments.length; i++) {
          ReceiverType bound = fromJdkType(arguments[i], bindings, type.context(), type.files());
          if (bound != null) {
            bindings.put(variables[i], bound);
          }
        }
        bindSupertypes(superclass, bindings, type, visited);
      } else if (supertype instanceof Class<?> superclass) {
        bindSupertypes(superclass, bindings, type, visited);
      }
    }
  }

  @Nullable
  private static ReceiverType fromJdkType(java.lang.reflect.Type type, Map<java.lang.reflect.TypeVariable<?>, ReceiverType> bindings,
                                          UMLAbstractClass context, Set<String> files) {
    if (type instanceof java.lang.reflect.TypeVariable<?> variable) {
      return bindings.get(variable);
    }
    if (type instanceof java.lang.reflect.WildcardType wildcard) {
      // the type a lambda parameter gets from "? super T" or "? extends T" is T
      if (wildcard.getLowerBounds().length > 0) {
        return fromJdkType(wildcard.getLowerBounds()[0], bindings, context, files);
      }
      return wildcard.getUpperBounds().length > 0 && wildcard.getUpperBounds()[0] != Object.class
              ? fromJdkType(wildcard.getUpperBounds()[0], bindings, context, files) : null;
    }
    if (type instanceof java.lang.reflect.GenericArrayType array) {
      ReceiverType component = fromJdkType(array.getGenericComponentType(), bindings, context, files);
      return component == null ? null : new ReceiverType(UMLType.extractTypeObject(component.type().toQualifiedString() + "[]",
              component.context().getLANG()), component.context(), component.files(), component.bindings());
    }
    String name = jdkTypeName(type);
    if (name.equals("void")) {
      return null;
    }
    Map<String, ReceiverType> typeBindings = new HashMap<>();
    for (java.lang.reflect.TypeVariable<?> variable : typeVariablesOf(type, new LinkedHashSet<>())) {
      ReceiverType bound = bindings.get(variable);
      if (bound != null) {
        typeBindings.put(variable.getName(), bound);
      }
    }
    return new ReceiverType(UMLType.extractTypeObject(name, context.getLANG()), context, files, typeBindings);
  }

  private static Set<java.lang.reflect.TypeVariable<?>> typeVariablesOf(java.lang.reflect.Type type, Set<java.lang.reflect.TypeVariable<?>> variables) {
    if (type instanceof java.lang.reflect.TypeVariable<?> variable) {
      variables.add(variable);
    } else if (type instanceof java.lang.reflect.ParameterizedType parameterized) {
      for (java.lang.reflect.Type argument : parameterized.getActualTypeArguments()) {
        typeVariablesOf(argument, variables);
      }
    } else if (type instanceof java.lang.reflect.WildcardType wildcard) {
      Arrays.stream(wildcard.getLowerBounds()).forEach(bound -> typeVariablesOf(bound, variables));
      Arrays.stream(wildcard.getUpperBounds()).forEach(bound -> typeVariablesOf(bound, variables));
    } else if (type instanceof java.lang.reflect.GenericArrayType array) {
      typeVariablesOf(array.getGenericComponentType(), variables);
    }
    return variables;
  }

  @Nullable
  private static ReceiverType lambdaTypeArgument(ReceiverType type, UMLType argument) {
    if (argument instanceof WildcardType wildcard) {
      if (wildcard.getClassType().equals("Object") || wildcard.getClassType().equals("?")) {
        return null;
      }
      argument = UMLType.extractTypeObject(wildcard.getClassType(), type.context().getLANG());
    }
    return typeArgument(type, argument);
  }

  private static ReceiverType declaredType(UMLType type, @Nullable VariableDeclarationContainer declaration, ClassRef declaring) {
    String typeParameter = type instanceof LeafType leaf && (type.getTypeArguments() == null || type.getTypeArguments().isEmpty())
            ? leaf.getClassType() : null;
    boolean declarationTypeParameter = declaration != null && typeParameter != null
            && declaration.getTypeParameters().stream().anyMatch(parameter -> parameter.getName().equals(typeParameter));
    ReceiverType bound = typeParameter == null || declarationTypeParameter ? null : declaring.bindings().get(typeParameter);
    if (bound != null) {
      if (type.getArrayDimension() == 0) {
        return bound.withFiles(declaring.files());
      }
      UMLType arrayType = UMLType.extractTypeObject(bound.type().toQualifiedString() + "[]".repeat(type.getArrayDimension()),
              bound.context().getLANG());
      return new ReceiverType(arrayType, bound.context(), bound.files(), bound.bindings()).withFiles(declaring.files());
    }
    return new ReceiverType(boundOf(type, declaration, declaring.umlClass()), declaring.umlClass(), declaring.files(),
            declaring.bindings());
  }

  @Nullable
  private static ReceiverType typeArgument(ReceiverType receiverType, UMLType argument) {
    if (argument instanceof WildcardType wildcard) {
      // "? extends Foo" stands for its upper bound, while "?" and "? super Foo" bound no type to read from
      if (wildcard.getClassType().equals("Object") || wildcard.toQualifiedString().contains(" super ")) {
        return null;
      }
      argument = UMLType.extractTypeObject(wildcard.getClassType(), receiverType.context().getLANG());
    }
    String typeVariable = typeVariableName(argument);
    ReceiverType bound = typeVariable == null ? null : receiverType.bindings().get(typeVariable);
    if (bound != null) {
      return bound.withFiles(receiverType.files());
    }
    return new ReceiverType(argument, receiverType.context(), receiverType.files(), receiverType.bindings());
  }

  @Nullable
  private static String typeVariableName(UMLType type) {
    if (!(type instanceof LeafType leaf) || type.getArrayDimension() > 0
            || (type.getTypeArguments() != null && !type.getTypeArguments().isEmpty())) {
      return null;
    }
    return leaf.getClassType();
  }

  @Nullable
  private static AbstractExpression enhancedForExpression(VariableDeclaration declaration, VariableDeclarationContainer container) {
    for (VariableDeclarationContainer bodyContainer : containerAndLambdas(container)) {
      if (bodyContainer.getBody() == null) {
        continue;
      }
      for (CompositeStatementObject node : bodyContainer.getBody().getCompositeStatement().getInnerNodes()) {
        if (!node.getVariableDeclarations().contains(declaration)) {
          continue;
        }
        for (AbstractExpression expression : node.getExpressions()) {
          if (expression.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION)) {
            return expression;
          }
        }
      }
    }
    return null;
  }

  static List<VariableDeclarationContainer> containerAndLambdas(VariableDeclarationContainer container) {
    List<VariableDeclarationContainer> containers = new ArrayList<>();
    Set<VariableDeclarationContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<VariableDeclarationContainer> pending = new ArrayDeque<>(List.of(container));
    while (!pending.isEmpty()) {
      VariableDeclarationContainer current = pending.poll();
      if (visited.add(current)) {
        containers.add(current);
        pending.addAll(current.getAllLambdas());
      }
    }
    return containers;
  }

  @Nullable
  private ReceiverType getElementType(ReceiverType iterableType, UMLModel umlModel) {
    if (iterableType.type().getArrayDimension() > 0) {
      return getIndexedElementType(iterableType);
    }
    ClassRef iterableClass = getModelClass(iterableType, umlModel);
    if (iterableClass == null) {
      return getJdkElementType(iterableType);
    }
    Found<ReceiverType> found = findInHierarchy(iterableClass, umlModel, umlClass -> {
      List<UMLType> supertypes = new ArrayList<>();
      if (umlClass.umlClass().getSuperclass() != null) {
        supertypes.add(umlClass.umlClass().getSuperclass());
      }
      supertypes.addAll(umlClass.umlClass().getImplementedInterfaces());
      for (UMLType supertype : supertypes) {
        ReceiverType supertypeType = supertypeOf(supertype, umlClass);
        if (getModelClass(supertypeType, umlModel) == null) {
          ReceiverType elementType = getJdkElementType(supertypeType);
          if (elementType != null) {
            return elementType;
          }
        }
      }
      return null;
    });
    return found == null ? null : found.value().withFiles(found.files());
  }

  @Nullable
  private ReceiverType getJdkElementType(ReceiverType iterableType) {
    Class<?> rawType = jdkClassOf(iterableType);
    if (rawType == null || !Iterable.class.isAssignableFrom(rawType)) {
      return null;
    }
    return jdkTypeBindings(rawType, iterableType).get(Iterable.class.getTypeParameters()[0]);
  }

  @Nullable
  private Class<?> jdkClass(String name, UMLAbstractClass context) {
    if (!context.getLANG().equals(Constants.JAVA)) {
      return null;
    }
    try {
      String first = name.split("\\.")[0];
      String rest = name.substring(first.length());
      List<String> candidates = new ArrayList<>();
      for (String candidate : filePathResolver.qualifiedNameCandidates(first, context)) {
        candidates.add(candidate + rest);
      }
      candidates.add(name);
      for (String candidate : candidates) {
        // nested classes are named with $ in their binary names
        for (String binaryName = candidate; ; ) {
          try {
            return Class.forName(binaryName, false, ClassLoader.getPlatformClassLoader());
          } catch (ClassNotFoundException | LinkageError e) {
            // not a JDK class
          }
          int dot = binaryName.lastIndexOf('.');
          if (dot < 0) {
            break;
          }
          binaryName = binaryName.substring(0, dot) + "$" + binaryName.substring(dot + 1);
        }
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private ReceiverType getJdkFieldType(ReceiverType ownerType, String name) {
    Class<?> rawType = jdkClassOf(ownerType);
    if (rawType == null) {
      return null;
    }
    try {
      return fromJdkType(rawType.getField(name).getGenericType(), jdkTypeBindings(rawType, ownerType), ownerType.context(), ownerType.files());
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static String jdkTypeName(java.lang.reflect.Type type) {
    if (type instanceof Class<?> typeClass) {
      return typeClass.isArray() ? jdkTypeName(typeClass.getComponentType()) + "[]"
              : typeClass.getCanonicalName() != null ? typeClass.getCanonicalName() : typeClass.getName();
    }
    if (type instanceof java.lang.reflect.ParameterizedType parameterized) {
      StringBuilder name = new StringBuilder(jdkTypeName(parameterized.getRawType())).append('<');
      java.lang.reflect.Type[] arguments = parameterized.getActualTypeArguments();
      for (int i = 0; i < arguments.length; i++) {
        name.append(i > 0 ? "," : "").append(jdkTypeName(arguments[i]));
      }
      return name.append('>').toString();
    }
    if (type instanceof java.lang.reflect.GenericArrayType array) {
      return jdkTypeName(array.getGenericComponentType()) + "[]";
    }
    if (type instanceof java.lang.reflect.WildcardType wildcard) {
      if (wildcard.getLowerBounds().length > 0) {
        return "? super " + jdkTypeName(wildcard.getLowerBounds()[0]);
      }
      return wildcard.getUpperBounds().length > 0 && wildcard.getUpperBounds()[0] != Object.class
              ? "? extends " + jdkTypeName(wildcard.getUpperBounds()[0]) : "?";
    }
    return type.getTypeName();
  }

  @Nullable
  private ClassRef getModelClass(ReceiverType receiverType, UMLModel umlModel) {
    if (!isWrittenType(receiverType.type())) {
      return null;
    }
    if (receiverType.context() instanceof UMLAnonymousClass anonymous && receiverType.type().getArrayDimension() == 0
            && receiverType.type().getClassType().equals(typeOf(anonymous).getClassType())) {
      // the type of this in an anonymous class (its name is a number, which no named class has)
      return new ClassRef(anonymous, receiverType.files());
    }
    ClassRef found = memberType(receiverType, umlModel);
    if (found == null) {
      // the class may be declared in the file of the scope, which the resolver does not report
      Set<String> paths = new HashSet<>(resolvePaths(receiverType));
      paths.add(receiverType.context().getSourceFile());
      found = findClass(paths, lastSegment(receiverType.type().getClassType()), receiverType.files(), umlModel);
    }
    if (found == null || !(found.umlClass() instanceof UMLClass umlClass)) {
      return found;
    }
    List<UMLTypeParameter> typeParameters = umlClass.getTypeParameters();
    List<UMLType> typeArguments = receiverType.type().getTypeArguments();
    if (typeParameters.isEmpty() || typeArguments == null || typeArguments.size() != typeParameters.size()) {
      return found;
    }
    Map<String, ReceiverType> bindings = new HashMap<>();
    for (int i = 0; i < typeParameters.size(); i++) {
      ReceiverType bound = typeArgument(receiverType, typeArguments.get(i));
      if (bound != null) {
        bindings.put(typeParameters.get(i).getName(), bound);
      }
    }
    return new ClassRef(found.umlClass(), found.files(), bindings);
  }

  // Java: a simple type name names first a member type, declared or inherited, of the class using it or of a class
  // enclosing that class, which shadows the imports; an inherited one is bound by no import nor by the package of the
  // file, but by the supertypes of the class. A class whose supertypes are being looked up is skipped, as its own
  // members are not in the scope of its supertype names.
  @Nullable
  private ClassRef memberType(ReceiverType receiverType, UMLModel umlModel) {
    String name = receiverType.type().getClassType();
    UMLAbstractClass context = receiverType.context();
    if (!context.getLANG().equals(Constants.JAVA) || name.contains(".")) {
      return null;
    }

    // A name found is looked up again, as the class it names may be attached to the model since
    Set<String> contextNonMemberTypes = nonMemberTypes.computeIfAbsent(context, key -> new HashSet<>());
    if (contextNonMemberTypes.contains(name)) {
      return null;
    }

    for (ClassRef scopeClass : classAndEnclosingClasses(context, umlModel)) {
      if (!memberTypeLookups.add(scopeClass.umlClass())) {
        continue;
      }
      try {
        Found<ClassRef> found = findInHierarchy(scopeClass, umlModel, current -> nestedClass(current, name, umlModel));
        if (found != null) {
          return new ClassRef(found.value().umlClass(), union(receiverType.files(), found.value().files()));
        }
      } finally {
        memberTypeLookups.remove(scopeClass.umlClass());
      }
    }
    if (memberTypeLookups.isEmpty()) {
      contextNonMemberTypes.add(name);
    }
    return null;
  }

  @Nullable
  private ClassRef nestedClass(ClassRef owner, String name, UMLModel umlModel) {
    String path = owner.umlClass().getSourceFile();
    return classesOfFile(path, umlModel).stream().filter(umlClass -> umlClass.getName().equals(owner.umlClass().getName() + "." + name))
            .findFirst().map(umlClass -> new ClassRef(umlClass, union(owner.files(), Set.of(path)))).orElse(null);
  }

  private static Set<String> union(Set<String> files, Set<String> moreFiles) {
    Set<String> union = new LinkedHashSet<>(files);
    union.addAll(moreFiles);
    return union;
  }

  @Nullable
  private ClassRef findClass(Set<String> paths, String simpleName, Set<String> files, UMLModel umlModel) {
    for (String path : paths) {
      for (UMLClass umlClass : classesOfFile(path, umlModel)) {
        if (lastSegment(umlClass.getNonQualifiedName()).equals(simpleName)) {
          Set<String> classFiles = new LinkedHashSet<>(files);
          classFiles.add(path);
          return new ClassRef(umlClass, classFiles);
        }
      }
    }
    return null;
  }

  private List<UMLClass> classesOfFile(String path, UMLModel umlModel) {
    List<UMLClass> modelClasses = umlModel.getClassList().stream().filter(umlClass -> umlClass.getSourceFile().equals(path)).toList();
    if (!modelClasses.isEmpty() || !PathFileUtils.isSupportedFile(path)) {
      return modelClasses;
    }
    try {
      return filePathResolver.classesOf(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isStaticName(String expression, ReceiverScope scope) {
    if (!NAME_CHAIN.matcher(expression).matches()) {
      return false;
    }
    String first = expression.split("\\.|::")[0];
    if (first.equals(scope.context().getLANG().THIS) || first.equals("super")) {
      return false;
    }
    return resolveSimpleName(first, scope).typeName();
  }

  private static boolean isWrittenType(@Nullable UMLType type) {
    return type != null && !(type instanceof InferredType) && !INFERRED_TYPE_NAMES.contains(type.getClassType());
  }

  private static String lastSegment(String name) {
    String[] segments = name.split("::|\\.");
    return segments.length == 0 ? name : segments[segments.length - 1];
  }

  public record ReceiverType(UMLType type, UMLAbstractClass context, Set<String> files, Map<String, ReceiverType> bindings) {
    public ReceiverType(UMLType type, UMLAbstractClass context) {
      this(type, context, Set.of(), Map.of());
    }

    public ReceiverType(UMLType type, UMLAbstractClass context, Set<String> files) {
      this(type, context, files, Map.of());
    }

    ReceiverType withFiles(Set<String> moreFiles) {
      if (moreFiles.isEmpty()) {
        return this;
      }
      Set<String> allFiles = new LinkedHashSet<>(moreFiles);
      allFiles.addAll(files);
      return new ReceiverType(type, context, allFiles, bindings);
    }
  }

  private record ClassRef(UMLAbstractClass umlClass, Set<String> files, Map<String, ReceiverType> bindings) {
    ClassRef(UMLAbstractClass umlClass, Set<String> files) {
      this(umlClass, files, Map.of());
    }
  }

  private record Found<T>(T value, Set<String> files) {
  }

  private record UnqualifiedField(@Nullable Found<DeclaredField> field) {
  }

  private record PendingSupertype(UMLType type, ClassRef subtype) {
  }

  private record DeclaredField(@Nullable UMLAttribute attribute, ClassRef declaringClass, @Nullable UMLEnumConstant enumConstant) {
    UMLAttribute member() {
      return attribute != null ? attribute : enumConstant;
    }
  }

  private record NameLookup(@Nullable VariableDeclaration local, @Nullable UnqualifiedField field, ReceiverScope scope) {
  }

  private record Hierarchy(Set<UMLAbstractClass> classes, Set<Class<?>> jdkClasses, boolean complete) {
  }

  private record Candidate(@Nullable UMLOperation operation, @Nullable ClassRef declaring, @Nullable java.lang.reflect.Method jdkMethod,
                           @Nullable ReceiverType jdkOwner, Map<java.lang.reflect.TypeVariable<?>, ReceiverType> jdkBindings,
                           List<ReceiverType> parameters, int required, boolean varargs, List<String> signature) {
  }

  private record Argument(@Nullable ReceiverType type, boolean nullLiteral) {
  }

  private enum Fit { YES, NO, UNKNOWN }

  // the phases of Java overload resolution: subtyping, then boxing, then variable arity
  private enum Phase { STRICT, LOOSE, VARIABLE_ARITY }

  private static final class NodePool {
    private final List<LeafExpression> nodes = new ArrayList<>();
    private final Map<LeafExpression, TernaryOperatorExpression> conditionals = new IdentityHashMap<>();
    private final Map<LeafExpression, String> operators = new IdentityHashMap<>();

    @Nullable
    String operator(LeafExpression infixExpression) {
      return operators.get(infixExpression);
    }

    @Nullable
    Map.Entry<AbstractCall, Integer> callTaking(LambdaExpressionObject lambda) {
      LocationInfo location = lambda.getLocationInfo();
      String text = lambda.getString().replaceAll("\\s+", "");
      Map.Entry<AbstractCall, Integer> innermost = null;
      for (LeafExpression node : nodes) {
        if (!(node instanceof AbstractCall call) || !call.getLocationInfo().subsumes(location)) {
          continue;
        }
        for (int i = 0; i < call.arguments().size(); i++) {
          if (call.arguments().get(i).replaceAll("\\s+", "").equals(text)
                  && (innermost == null || innermost.getKey().getLocationInfo().subsumes(call.getLocationInfo()))) {
            innermost = Map.entry(call, i);
          }
        }
      }
      return innermost;
    }

    @Nullable
    TernaryOperatorExpression conditional(LeafExpression node) {
      return conditionals.get(node);
    }

    static NodePool of(@Nullable VariableDeclarationContainer container) {
      NodePool pool = new NodePool();
      if (container != null) {
        pool.addContainer(container);
      }
      return pool;
    }

    static NodePool of(AbstractExpression expression) {
      NodePool pool = new NodePool();
      pool.add(expression);
      for (LambdaExpressionObject lambda : expression.getLambdas()) {
        pool.addContainer(lambda);
      }
      return pool;
    }

    private void addContainer(VariableDeclarationContainer container) {
      Deque<VariableDeclarationContainer> pending = new ArrayDeque<>(List.of(container));
      Set<VariableDeclarationContainer> visited = new HashSet<>();
      while (!pending.isEmpty()) {
        VariableDeclarationContainer current = pending.poll();
        if (!visited.add(current)) {
          continue;
        }
        if (current instanceof UMLAttribute attribute) {
          // the code of an attribute is its initializer (its body is the one of a lambda initializing it)
          AbstractExpression initializer = attribute.getVariableDeclaration().getInitializer();
          if (initializer != null) {
            add(initializer);
          }
        } else if (current.getBody() != null) {
          for (AbstractStatement statement : current.getBody().getCompositeStatement().getAllStatements()) {
            add(statement);
            if (statement instanceof CompositeStatementObject composite) {
              composite.getExpressions().forEach(this::add);
            }
          }
        }
        if (current.getDefaultExpression() != null) {
          add(current.getDefaultExpression());
        }
        pending.addAll(current.getAllLambdas());
        for (UMLAnonymousClass anonymousClass : current.getAnonymousClassList()) {
          pending.addAll(anonymousClass.getOperations());
        }
      }
    }

    private void add(AbstractCodeFragment fragment) {
      for (List<? extends LeafExpression> kind : List.of(fragment.getVariables(), fragment.getMethodInvocations(),
              fragment.getCreations(), fragment.getTextBlocks(), fragment.getStringLiterals(), fragment.getCharLiterals(),
              fragment.getNumberLiterals(), fragment.getNullLiterals(), fragment.getBooleanLiterals(), fragment.getTypeLiterals(),
              fragment.getInfixExpressions(), fragment.getAssignments(), fragment.getArrayAccesses(), fragment.getPrefixExpressions(),
              fragment.getPostfixExpressions(), fragment.getThisExpressions(), fragment.getParenthesizedExpressions(),
              fragment.getCastExpressions(), fragment.getInstanceofExpressions(), fragment.getPatternInstanceofExpressions())) {
        nodes.addAll(kind);
      }
      // RM adds each infix expression and its operator in the same step, so they share their index
      List<LeafExpression> infixExpressions = fragment.getInfixExpressions();
      List<String> infixOperators = fragment.getInfixOperators();
      if (infixExpressions.size() == infixOperators.size()) {
        for (int i = 0; i < infixExpressions.size(); i++) {
          operators.put(infixExpressions.get(i), infixOperators.get(i));
        }
      }
      for (TernaryOperatorExpression ternary : fragment.getTernaryOperatorExpressions()) {
        LeafExpression node = ternary.asLeafExpression();
        nodes.add(node);
        conditionals.put(node, ternary);
      }
    }

    @Nullable
    LeafExpression longestStartingAt(int start, int end, @Nullable LeafExpression excluded) {
      LeafExpression longest = null;
      for (LeafExpression node : nodes) {
        LocationInfo location = node.getLocationInfo();
        if (node == excluded || location.getStartOffset() != start || location.getEndOffset() >= end) {
          continue;
        }
        if (longest == null || location.getEndOffset() > longest.getLocationInfo().getEndOffset()
                || (location.getEndOffset() == longest.getLocationInfo().getEndOffset() && node instanceof AbstractCall)) {
          longest = node;
        }
      }
      return longest;
    }

    @Nullable
    LeafExpression exactly(int start, int end) {
      LeafExpression found = null;
      for (LeafExpression node : nodes) {
        LocationInfo location = node.getLocationInfo();
        if (location.getStartOffset() == start && location.getEndOffset() == end && (found == null || node instanceof AbstractCall)) {
          found = node;
        }
      }
      return found;
    }

    @Nullable
    LeafExpression outermostInside(int start, int end) {
      LeafExpression outermost = null;
      for (LeafExpression node : nodes) {
        LocationInfo location = node.getLocationInfo();
        if (location.getStartOffset() <= start || location.getEndOffset() >= end) {
          continue;
        }
        LocationInfo current = outermost == null ? null : outermost.getLocationInfo();
        if (current == null || location.getStartOffset() < current.getStartOffset()
                || (location.getStartOffset() == current.getStartOffset() && location.getEndOffset() > current.getEndOffset())) {
          outermost = node;
        }
      }
      return outermost;
    }

    @Nullable
    LeafExpression earliestEndingAt(int start, int end) {
      LeafExpression earliest = null;
      for (LeafExpression node : nodes) {
        LocationInfo location = node.getLocationInfo();
        if (location.getStartOffset() <= start || location.getEndOffset() != end) {
          continue;
        }
        if (earliest == null || location.getStartOffset() < earliest.getLocationInfo().getStartOffset()) {
          earliest = node;
        }
      }
      return earliest;
    }

    List<LeafExpression> argumentsOf(AbstractCall call) {
      LocationInfo callLocation = call.getLocationInfo();
      List<LeafExpression> callArguments = new ArrayList<>();
      int after = callLocation.getStartOffset();
      for (String argument : call.arguments()) {
        LeafExpression found = null;
        for (LeafExpression node : nodes) {
          LocationInfo location = node.getLocationInfo();
          if (location.getStartOffset() <= after || location.getEndOffset() >= callLocation.getEndOffset()
                  || !node.getString().trim().equals(argument.trim())) {
            continue;
          }
          boolean nested = nodes.stream().anyMatch(other -> other instanceof AbstractCall otherCall && otherCall != call
                  && strictlyInside(otherCall.getLocationInfo(), callLocation) && strictlyInside(location, otherCall.getLocationInfo()));
          if (!nested && (found == null || location.getStartOffset() < found.getLocationInfo().getStartOffset())) {
            found = node;
          }
        }
        callArguments.add(found);
        if (found != null) {
          after = found.getLocationInfo().getEndOffset() - 1;
        }
      }
      return callArguments;
    }

    private static boolean strictlyInside(LocationInfo inner, LocationInfo outer) {
      return outer.subsumes(inner) && (inner.getStartOffset() > outer.getStartOffset() || inner.getEndOffset() < outer.getEndOffset());
    }
  }

  private record SimpleName(boolean typeName, @Nullable ReceiverType type) {
    static final SimpleName TYPE_NAME = new SimpleName(true, null);

    static SimpleName variable(@Nullable ReceiverType type) {
      return new SimpleName(false, type);
    }
  }

  private record ReceiverScope(NodePool nodes, @Nullable VariableDeclarationContainer container, LocationInfo location,
                               UMLAbstractClass context, UMLModel umlModel, Set<VariableDeclaration> visiting) {
  }
}
