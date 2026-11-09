package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLModelDiff;
import java.util.*;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.utils.Constants;

/**
 * Values passed on unchanged (stage 3 of the TODO on data and control flow in HunkNetwork): an assignment relaying the
 * parameters of its member, a field holding a constructor parameter, a getter returning a field, a delegate returning
 * what a method of its class gives, and expressions reading the same constructor parameter through different fields.
 */
final class ValueRelays {
  private final UMLModelDiff modelDiff;
  private final RMIndex rmIndex;
  private final RMLinks rmLinks;
  private final ReceiverTypeResolver receiverTypeResolver;

  ValueRelays(UMLModelDiff modelDiff, RMIndex rmIndex, RMLinks rmLinks, ReceiverTypeResolver receiverTypeResolver) {
    this.modelDiff = modelDiff;
    this.rmIndex = rmIndex;
    this.rmLinks = rmLinks;
    this.receiverTypeResolver = receiverTypeResolver;
  }

  // The field of its class the operation returns, when its body only returns it, as a getter
  @Nullable
  UMLAttribute returnedField(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel) {
    if (operation.getBody() == null) {
      return null;
    }
    List<AbstractStatement> statements = operation.getBody().getCompositeStatement().getStatements();
    if (statements.size() != 1) {
      return null;
    }
    AbstractStatement statement = statements.get(0);
    for (LeafExpression variable : statement.getVariables()) {
      if (RMCode.returnsWhole(statement, variable)) {
        UMLAttribute field = receiverTypeResolver.resolveAccessTarget(variable, declaring, umlModel);
        return field != null && declaring.getAttributes().contains(field) ? field : null;
      }
    }
    return null;
  }

  // Whether the value the assignment gives is computed from the parameters of its member alone, as a constructor keeping
  // what its caller passes, as it is (`this.type = type`) or through a check or a lookup on it (checkNotNull(type),
  // in.readEnum(...), brokerController.getBrokerConfig()): it relays the value the callers give, which only those callers
  // tell (argument -> parameter, stage 3 of the TODO on data and control flow). A value reading a local, the state of the
  // instance (this, a field, a call on the instance) or creating an object is a value of the member's own; so is one
  // reading no parameter, as a literal or a constant
  boolean relaysParameters(MemberUsageFinder.Write write, VariableDeclarationContainer member, UMLModel umlModel) {
    AbstractCodeFragment fragment = write.fragment();
    int start = write.name().getLocationInfo().getEndOffset();
    int end = write.assignment().getEndOffset();
    Predicate<LocationInfo> inValue = location -> start <= location.getStartOffset() && location.getEndOffset() <= end;
    if (fragment.getCreations().stream().anyMatch(creation -> inValue.test(creation.getLocationInfo()))
            || fragment.getThisExpressions().stream().anyMatch(self -> inValue.test(self.getLocationInfo()))) {
      return false;
    }
    Set<String> parameters = new HashSet<>();
    member.getParameterDeclarationList().forEach(parameter -> parameters.add(parameter.getVariableName()));
    boolean readsParameter = false;
    for (LeafExpression variable : fragment.getVariables()) {
      if (!inValue.test(variable.getLocationInfo())) {
        continue;
      }
      String name = DataFlow.referredLocal(variable.getString());
      VariableDeclaration local = fragment.searchVariableDeclaration(name);
      if (parameters.contains(name) && (local == null || member.getParameterDeclarationList().contains(local))) {
        readsParameter = true;
        continue;
      }
      if (local != null) {
        return false;
      }
      // A field of the instance is its state; a constant is a value of its own, as a type name is none
      UMLAttribute field = receiverTypeResolver.resolveAccessTarget(variable, write.context(), umlModel);
      if (field != null && !(field.isStatic() && field.isFinal())) {
        return false;
      }
    }
    for (AbstractCall call : fragment.getMethodInvocations()) {
      if (!inValue.test(call.getLocationInfo())) {
        continue;
      }
      // The name a receiver refers to by RM's rule for the statements using a variable (see DataFlow#referredLocal), as RM
      // gives a receiver as the text of the call only: a parameter, a local, a field of the instance, or a type a static
      // method is called on (a receiver that is a call is a call of the value itself)
      String receiver = call.getExpression();
      if (receiver != null) {
        String name = DataFlow.referredLocal(receiver);
        VariableDeclaration local = fragment.searchVariableDeclaration(name);
        if (parameters.contains(name) && (local == null || member.getParameterDeclarationList().contains(local))) {
          readsParameter = true;
        } else if (local != null || name.equals(write.context().getLANG().THIS)
                || write.context().getAttributes().stream().anyMatch(attribute -> attribute.getName().equals(name) && !attribute.isStatic())) {
          return false;
        }
        continue;
      }
      // A call with no receiver is a call on the instance unless it binds to a static method (or to none of the model,
      // as a method imported statically from a library)
      UMLOperation target = receiverTypeResolver.bindInvocationTarget(call, write.context(), umlModel);
      if (target != null && !target.isStatic()) {
        return false;
      }
    }
    return readsParameter;
  }

  // Whether the write lies in a new constructor storing one of its parameters in a final field of its class, which every
  // constructor of the class stores a parameter in (see heldParameter): the field holds what each creation passes, as it
  // did, and a creation binding to the new constructor passes what it passed before (es_121109: the context a copy_to
  // wrapper wraps). Its readers get nothing new; the creations binding to the new constructor are its usages
  boolean storesCreationArgument(LocationInfo write, UMLAttribute attribute, UMLClass umlClass, UMLModel umlModel) {
    if (!attribute.isFinal() || attribute.isStatic() || rmLinks.matchedAttribute(attribute) == null) {
      return false;
    }
    List<UMLOperation> constructors = umlClass.getOperations().stream().filter(UMLOperation::isConstructor).toList();
    UMLOperation writing = constructors.stream().filter(constructor -> constructor.getLocationInfo().subsumes(write))
            .findFirst().orElse(null);
    return writing != null && rmLinks.isNewOperation(writing) && constructors.stream()
            .allMatch(constructor -> heldParameter(attribute, constructor, umlClass, umlModel, new HashSet<>()) != null);
  }

  // Whether the operation returns, on some path, a constant (an enum constant or a static final field) the conditions of
  // the reader compare with, as the polling result a new branch tests for: those returns are where the condition holds
  boolean returnsConstantOf(UMLOperation operation, AbstractStatement reader, UMLAbstractClass readerContext,
                                    SrcDst srcDst, UMLModel umlModel) {
    List<AbstractCodeFragment> conditions = reader instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(reader);
    Set<UMLAttribute> compared = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AbstractCodeFragment condition : conditions) {
      for (LeafExpression variable : condition.getVariables()) {
        UMLAttribute constant = receiverTypeResolver.resolveAccessTarget(variable, readerContext, umlModel);
        if (constant != null && (constant instanceof UMLEnumConstant || constant.isStatic() && constant.isFinal())) {
          compared.add(constant);
        }
      }
    }
    UMLAbstractClass declaring = rmIndex.ownerOf(srcDst, operation.getLocationInfo());
    if (compared.isEmpty() || declaring == null || operation.getBody() == null) {
      return false;
    }
    for (AbstractStatement statement : operation.getBody().getCompositeStatement().getAllStatements()) {
      for (LeafExpression variable : statement.getVariables()) {
        if (RMCode.returnsWhole(statement, variable) && compared.contains(receiverTypeResolver.resolveAccessTarget(variable, declaring, umlModel))) {
          return true;
        }
      }
    }
    return false;
  }

  // The method of its class the operation only returns the result of, called on the instance with the operation's
  // parameters as they are and literals
  @Nullable
  UMLOperation delegateOf(UMLOperation operation, UMLAbstractClass declaring, UMLModel umlModel) {
    if (operation.getBody() == null) {
      return null;
    }
    List<AbstractStatement> statements = operation.getBody().getCompositeStatement().getStatements();
    if (statements.size() != 1) {
      return null;
    }
    AbstractStatement statement = statements.get(0);
    AbstractCall delegated = RMCode.returnedCall(statement);
    if (delegated == null || !delegated.expressionIsNullOrThis()) {
      return null;
    }
    // Nothing is computed for the arguments: no other call, creation, operator, condition or cast, and every name a
    // parameter
    LocationInfo location = delegated.getLocationInfo();
    Predicate<LocationInfo> within = inner -> location.subsumes(inner) && !inner.equals(location);
    if (statement.getMethodInvocations().stream().anyMatch(call -> within.test(call.getLocationInfo()))
            || statement.getCreations().stream().anyMatch(creation -> within.test(creation.getLocationInfo()))
            || statement.getInfixExpressions().stream().anyMatch(infix -> within.test(infix.getLocationInfo()))
            || statement.getTernaryOperatorExpressions().stream().anyMatch(ternary -> within.test(ternary.getLocationInfo()))
            || statement.getCastExpressions().stream().anyMatch(cast -> within.test(cast.getLocationInfo()))) {
      return null;
    }
    Set<String> parameters = new HashSet<>();
    operation.getParameterDeclarationList().forEach(parameter -> parameters.add(parameter.getVariableName()));
    if (statement.getVariables().stream().anyMatch(variable -> within.test(variable.getLocationInfo())
            && !parameters.contains(variable.getString().trim()))) {
      return null;
    }
    UMLOperation target = receiverTypeResolver.bindInvocationTarget(delegated, declaring, umlModel);
    return target != null && target != operation && declaring.getOperations().contains(target) ? target : null;
  }

  // Whether the change lies in an expression RM gives as replaced by one reading the same value (stage 3 of the TODO on
  // data and control flow, changes giving users nothing): both read a final field of the instance, directly or through a
  // getter returning it, and in every constructor of the class both fields hold the same constructor parameter, as a
  // removed field `source` replaced by the inherited source(), which returns the field super(source, ...) stores. The
  // users of the code get what they got; a reviewer would otherwise check the callers of a change that only reads the
  // value elsewhere
  boolean keepsValue(Node change) {
    AbstractCodeMapping mapping = rmLinks.mappingHolding(change.getSrcDst(), change.getPath(), change.getTree());
    if (mapping == null) {
      return false;
    }
    Tree tree = change.getTree();
    for (Replacement replacement : mapping.getReplacements()) {
      LeafExpression before = RMCode.expressionOf(mapping.getFragment1(), replacement.getBefore());
      LeafExpression after = RMCode.expressionOf(mapping.getFragment2(), replacement.getAfter());
      if (before == null || after == null) {
        continue;
      }
      LocationInfo own = (change.isSrc() ? before : after).getLocationInfo();
      if (own.getStartOffset() <= tree.getPos() && tree.getEndPos() <= own.getEndOffset()) {
        return holdSameParameter(before, mapping.getFragment1(), after, mapping.getFragment2());
      }
    }
    return false;
  }

  // Whether the old and the new expression read final fields holding the same parameter of every constructor of their
  // class, the constructors matched by their parameter types
  private boolean holdSameParameter(LeafExpression before, AbstractCodeFragment beforeFragment, LeafExpression after,
                                    AbstractCodeFragment afterFragment) {
    UMLModel parentModel = modelDiff.getParentModel();
    UMLModel childModel = modelDiff.getChildModel();
    UMLAbstractClass srcClass = rmIndex.ownerOf(SrcDst.SRC, beforeFragment.getLocationInfo());
    UMLAbstractClass dstClass = rmIndex.ownerOf(SrcDst.DST, afterFragment.getLocationInfo());
    UMLAttribute srcField = srcClass == null ? null : heldField(before, srcClass, parentModel);
    UMLAttribute dstField = dstClass == null ? null : heldField(after, dstClass, childModel);
    if (srcField == null || dstField == null) {
      return false;
    }
    List<UMLOperation> srcConstructors = srcClass.getOperations().stream().filter(UMLOperation::isConstructor).toList();
    List<UMLOperation> dstConstructors = dstClass.getOperations().stream().filter(UMLOperation::isConstructor).toList();
    if (dstConstructors.isEmpty() || srcConstructors.size() != dstConstructors.size()) {
      return false;
    }
    for (UMLOperation dstConstructor : dstConstructors) {
      String types = dstConstructor.getParameterTypeList().toString();
      UMLOperation srcConstructor = srcConstructors.stream()
              .filter(candidate -> candidate.getParameterTypeList().toString().equals(types)).findFirst().orElse(null);
      Integer srcParameter = srcConstructor == null ? null
              : heldParameter(srcField, srcConstructor, srcClass, parentModel, new HashSet<>());
      Integer dstParameter = heldParameter(dstField, dstConstructor, dstClass, childModel, new HashSet<>());
      if (srcParameter == null || !srcParameter.equals(dstParameter)) {
        return false;
      }
    }
    return true;
  }

  // The final field of the instance the expression reads: the field it names, or the one the getter it calls on the
  // instance returns as it is
  @Nullable
  private UMLAttribute heldField(LeafExpression expression, UMLAbstractClass context, UMLModel umlModel) {
    UMLAttribute field;
    if (expression instanceof AbstractCall call) {
      if (!call.arguments().isEmpty() || !call.expressionIsNullOrThis()) {
        return null;
      }
      UMLOperation getter = receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
      UMLAbstractClass declaring = getter == null || getter.getBody() == null ? null
              : rmIndex.ownerOf(umlModel == modelDiff.getParentModel() ? SrcDst.SRC : SrcDst.DST, getter.getLocationInfo());
      List<AbstractStatement> statements = declaring == null ? List.of() : getter.getBody().getCompositeStatement().getStatements();
      if (statements.size() != 1 || statements.get(0).getVariables().size() != 1) {
        return null;
      }
      LeafExpression returned = statements.get(0).getVariables().get(0);
      if (!RMCode.returnsWhole(statements.get(0), returned)) {
        return null;
      }
      field = receiverTypeResolver.bindAccessTarget(returned, declaring, umlModel);
    } else {
      field = receiverTypeResolver.bindAccessTarget(expression, context, umlModel);
    }
    return field != null && field.isFinal() && !field.isStatic() ? field : null;
  }

  // The index of the parameter of the constructor the field holds as it is once the constructor ran: one it assigns to
  // the field, or one it passes on to the constructor of its class or of its superclass that stores it (this(...),
  // super(...)); none when the field holds something else. A statement assigns the parameter to the field as RM matches
  // a setter with the constants of the language (UMLOperation#isSetter), and calls another constructor as RM gives the
  // kind of the call
  @Nullable
  private Integer heldParameter(UMLAttribute field, UMLOperation constructor, UMLAbstractClass context, UMLModel umlModel,
                                Set<UMLOperation> visited) {
    if (!visited.add(constructor) || constructor.getBody() == null) {
      return null;
    }
    List<String> parameters = constructor.getParameterNameList();
    for (AbstractStatement statement : constructor.getBody().getCompositeStatement().getStatements()) {
      gr.uom.java.xmi.Constants lang = statement.getLANG();
      for (String target : List.of(field.getName(), lang.THIS_DOT + field.getName())) {
        for (int i = 0; i < parameters.size(); i++) {
          if (statement.getString().equals(target + lang.ASSIGNMENT + parameters.get(i) + lang.STATEMENT_TERMINATION)) {
            return i;
          }
        }
      }
      for (AbstractCall call : statement.getMethodInvocations()) {
        LocationInfo.CodeElementType kind = call.getLocationInfo().getCodeElementType();
        if (kind != LocationInfo.CodeElementType.SUPER_CONSTRUCTOR_INVOCATION && kind != LocationInfo.CodeElementType.CONSTRUCTOR_INVOCATION) {
          continue;
        }
        UMLOperation delegated = receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
        UMLAbstractClass delegatedClass = delegated == null || !delegated.isConstructor() ? null
                : receiverTypeResolver.declaringClass(delegated, umlModel);
        Integer index = delegatedClass == null ? null : heldParameter(field, delegated, delegatedClass, umlModel, visited);
        if (index != null && index < call.arguments().size() && parameters.contains(call.arguments().get(index).trim())) {
          return parameters.indexOf(call.arguments().get(index).trim());
        }
      }
    }
    return null;
  }
}
