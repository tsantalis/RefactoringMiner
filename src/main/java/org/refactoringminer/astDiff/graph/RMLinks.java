package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The statements RM gives as the same statement in both revisions: the ones the body mappers of the members it matches
 * map, the value of an extracted (inlined) variable and the statements it is replaced in, and the declarations of the
 * fields it matches, which hold the code of their initializers, and the members it matches; also the variables it gives
 * as renamed, inlined or extracted, and the fields it gives as removed (see {@link RMIndex} for what RM gives of one
 * revision).
 */
class RMLinks {
  private final UMLModelDiff modelDiff;
  @Nullable
  private Map<SrcDst, Map<String, List<Statement>>> statements;
  @Nullable
  private Map<VariableDeclarationContainer, Set<VariableDeclarationContainer>> matchedMembers;
  @Nullable
  private Map<UMLAttribute, UMLAttribute> matchedAttributes;
  @Nullable
  private List<Refactoring> variableRefactorings;

  RMLinks(UMLModelDiff modelDiff) {
    this.modelDiff = modelDiff;
  }

  // Whether RM gives the members as one member of both revisions: the same declaration, or the ones a body mapper of
  // their classes matches, but not the members code is moved between (an extracted or inlined method)
  boolean isSameMember(VariableDeclarationContainer srcMember, VariableDeclarationContainer dstMember) {
    if (srcMember == dstMember) {
      return true;
    }
    return matchedMembers().getOrDefault(srcMember, Set.of()).contains(dstMember);
  }

  private Map<VariableDeclarationContainer, Set<VariableDeclarationContainer>> matchedMembers() {
    if (matchedMembers == null) {
      matchedMembers = new IdentityHashMap<>();
      for (UMLAbstractClassDiff classDiff : classDiffs()) {
        for (UMLOperationBodyMapper mapper : mappers(classDiff)) {
          matchedMembers.computeIfAbsent(mapper.getContainer1(), member -> Collections.newSetFromMap(new IdentityHashMap<>()))
                  .add(mapper.getContainer2());
        }
      }
    }
    return matchedMembers;
  }

  // The field (or enum constant) of the old revision RM gives as the same as the one of the new revision, if any
  @Nullable
  UMLAttribute matchedAttribute(UMLAttribute dstAttribute) {
    return matchedAttributes().get(dstAttribute);
  }

  private Map<UMLAttribute, UMLAttribute> matchedAttributes() {
    if (matchedAttributes == null) {
      matchedAttributes = new IdentityHashMap<>();
      for (UMLAbstractClassDiff classDiff : classDiffs()) {
        classDiff.getCommonAtrributes().forEach(pair -> matchedAttributes.put(pair.getRight(), pair.getLeft()));
        classDiff.getAttributeDiffList().forEach(diff -> matchedAttributes.put(diff.getAddedAttribute(), diff.getRemovedAttribute()));
        classDiff.getCommonEnumConstants().forEach(pair -> matchedAttributes.put(pair.getRight(), pair.getLeft()));
        classDiff.getEnumConstantDiffList().forEach(diff -> matchedAttributes.put(diff.getAddedEnumConstant(), diff.getRemovedEnumConstant()));
      }
      modelDiff.getMovedAttributeDiffList().forEach(diff -> matchedAttributes.put(diff.getAddedAttribute(), diff.getRemovedAttribute()));
    }
    return matchedAttributes;
  }

  // Whether the names are the old and the new name of a variable (a local or a parameter) RM gives as renamed (Rename
  // Variable, Rename Parameter) with its declaration kept but for its name (the same type and initializer), at its
  // declaration or at a reference to it
  boolean isRename(String srcPath, Tree srcName, String dstPath, Tree dstName) {
    for (Refactoring refactoring : variableRefactorings()) {
      if (refactoring instanceof RenameVariableRefactoring rename && keepsDeclaration(rename)
              && rename.getOriginalVariable().getVariableName().equals(srcName.getLabel())
              && rename.getRenamedVariable().getVariableName().equals(dstName.getLabel())
              && isOf(rename.getOriginalVariable(), rename.getOperationBefore(), srcPath, srcName)
              && isOf(rename.getRenamedVariable(), rename.getOperationAfter(), dstPath, dstName)) {
        return true;
      }
    }
    return false;
  }

  // Whether the tree is code RM gives as computing what an inlined (extracted) variable held where it was, only kept or
  // handed on differently (see #pushStatements): the declaration of the variable, with nothing else of its statement,
  // its name in a statement it is inlined into (extracted from), or its value there, which the counterpart is
  boolean restatesVariable(SrcDst srcDst, String path, Tree tree, @Nullable Tree counterpart) {
    for (Refactoring refactoring : variableRefactorings()) {
      VariableDeclaration variable;
      VariableDeclarationContainer container;
      List<AbstractCodeFragment> references = new ArrayList<>();
      boolean declaringSide;
      if (refactoring instanceof InlineVariableRefactoring inline) {
        variable = inline.getVariableDeclaration();
        container = srcDst.equals(SrcDst.SRC) ? inline.getOperationBefore() : inline.getOperationAfter();
        declaringSide = srcDst.equals(SrcDst.SRC);
        inline.getReferences().forEach(reference -> references.add(declaringSide ? reference.getFragment1() : reference.getFragment2()));
      } else if (refactoring instanceof ExtractVariableRefactoring extract) {
        variable = extract.getVariableDeclaration();
        container = srcDst.equals(SrcDst.SRC) ? extract.getOperationBefore() : extract.getOperationAfter();
        declaringSide = srcDst.equals(SrcDst.DST);
        extract.getReferences().forEach(reference -> references.add(declaringSide ? reference.getFragment2() : reference.getFragment1()));
      } else {
        continue;
      }
      if (!container.getLocationInfo().getFilePath().equals(path) || variable.getInitializer() == null) {
        continue;
      }
      boolean inReference = references.stream().anyMatch(reference -> holds(reference.getLocationInfo(), tree));
      if (declaringSide) {
        Statement declaring = held(srcDst, path, tree);
        if (holds(variable.getLocationInfo(), tree) && declaring != null && declaring.isWhole(tree)
                || inReference && tree.getChildren().isEmpty() && variable.getVariableName().equals(tree.getLabel())) {
          return true;
        }
      } else {
        LocationInfo value = variable.getInitializer().getLocationInfo();
        if (inReference && counterpart != null && counterpart.getPos() == value.getStartOffset()
                && counterpart.getEndPos() == value.getEndOffset()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean keepsDeclaration(RenameVariableRefactoring rename) {
    VariableDeclaration original = rename.getOriginalVariable();
    VariableDeclaration renamed = rename.getRenamedVariable();
    return Objects.equals(original.getType() == null ? null : original.getType().toString(),
            renamed.getType() == null ? null : renamed.getType().toString())
            && Objects.equals(original.getInitializer() == null ? null : original.getInitializer().getString(),
            renamed.getInitializer() == null ? null : renamed.getInitializer().getString());
  }

  private static boolean isOf(VariableDeclaration variable, VariableDeclarationContainer container, String path, Tree name) {
    LocationInfo location = container.getLocationInfo();
    return location.getFilePath().equals(path) && holds(location, name)
            && (holds(variable.getLocationInfo(), name)
            || variable.getScope().getStartOffset() <= name.getPos() && name.getEndPos() <= variable.getScope().getEndOffset());
  }

  private static boolean holds(LocationInfo location, Tree tree) {
    return location.getStartOffset() <= tree.getPos() && tree.getEndPos() <= location.getEndOffset();
  }

  private List<Refactoring> variableRefactorings() {
    if (variableRefactorings == null) {
      variableRefactorings = new ArrayList<>();
      for (UMLAbstractClassDiff classDiff : classDiffs()) {
        for (UMLOperationBodyMapper mapper : mappers(classDiff)) {
          for (Refactoring refactoring : mapper.getRefactoringsAfterPostProcessing()) {
            if (refactoring instanceof RenameVariableRefactoring || refactoring instanceof InlineVariableRefactoring
                    || refactoring instanceof ExtractVariableRefactoring) {
              variableRefactorings.add(refactoring);
            }
          }
        }
      }
    }
    return variableRefactorings;
  }

  // The fields (and enum constants) of the old revision RM gives as removed: no field of the new revision is the same
  List<UMLAttribute> removedAttributes() {
    List<UMLAttribute> removed = new ArrayList<>();
    for (UMLAbstractClassDiff classDiff : classDiffs()) {
      for (UMLAttribute attribute : classDiff.getRemovedAttributes()) {
        if (matchedAttributes().values().stream().noneMatch(matched -> matched == attribute)) {
          removed.add(attribute);
        }
      }
    }
    return removed;
  }

  // The body mappers of the members of the class diff and of the anonymous classes they hold, at any depth: an anonymous
  // class in a method of an anonymous class has its own diff in the mapper of that method
  private static List<UMLOperationBodyMapper> mappers(UMLAbstractClassDiff classDiff) {
    List<UMLOperationBodyMapper> mappers = new ArrayList<>();
    Deque<UMLOperationBodyMapper> pending = new ArrayDeque<>(classDiff.getOperationBodyMapperList());
    Set<UMLOperationBodyMapper> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    while (!pending.isEmpty()) {
      UMLOperationBodyMapper mapper = pending.poll();
      if (!seen.add(mapper)) {
        continue;
      }
      mappers.add(mapper);
      for (UMLAnonymousClassDiff anonymousDiff : mapper.getAnonymousClassDiffs()) {
        pending.addAll(anonymousDiff.getOperationBodyMapperList());
      }
    }
    return mappers;
  }

  private List<UMLAbstractClassDiff> classDiffs() {
    List<UMLAbstractClassDiff> classDiffs = new ArrayList<>(modelDiff.getCommonClassDiffList());
    classDiffs.addAll(modelDiff.getClassMoveDiffList());
    classDiffs.addAll(modelDiff.getInnerClassMoveDiffList());
    classDiffs.addAll(modelDiff.getClassRenameDiffList());
    return classDiffs;
  }

  // The innermost statement RM holds the tree in
  @Nullable
  Statement held(SrcDst srcDst, String path, Tree tree) {
    Statement held = null;
    for (Statement statement : statements().get(srcDst).getOrDefault(path, List.of())) {
      if (statement.start <= tree.getPos() && tree.getEndPos() <= statement.end
              && (held == null || held.start <= statement.start && statement.end <= held.end)) {
        held = statement;
      }
    }
    return held;
  }

  private Map<SrcDst, Map<String, List<Statement>>> statements() {
    if (statements != null) {
      return statements;
    }

    statements = Map.of(SrcDst.SRC, new HashMap<>(), SrcDst.DST, new HashMap<>());
    Map<String, Statement> located = new HashMap<>();
    for (UMLAbstractClassDiff classDiff : classDiffs()) {
      for (UMLOperationBodyMapper mapper : mappers(classDiff)) {
        pushStatements(mapper, located);
      }

      // The declaration of a field holds the code of its initializer
      List<UMLAttribute[]> attributes = new ArrayList<>();
      classDiff.getCommonAtrributes().forEach(pair -> attributes.add(new UMLAttribute[]{pair.getLeft(), pair.getRight()}));
      classDiff.getAttributeDiffList().forEach(diff -> attributes.add(new UMLAttribute[]{diff.getRemovedAttribute(), diff.getAddedAttribute()}));
      classDiff.getCommonEnumConstants().forEach(pair -> attributes.add(new UMLAttribute[]{pair.getLeft(), pair.getRight()}));
      classDiff.getEnumConstantDiffList().forEach(diff -> attributes.add(new UMLAttribute[]{diff.getRemovedEnumConstant(), diff.getAddedEnumConstant()}));
      for (UMLAttribute[] attribute : attributes) {
        link(statement(SrcDst.SRC, declarationLocation(attribute[0]), located),
                statement(SrcDst.DST, declarationLocation(attribute[1]), located));
      }
    }
    return statements;
  }

  // The statements of the mapper, linked to those RM gives as the same statement: the ones it maps, and the value of an
  // extracted (inlined) variable with the statements it is replaced in. Its child mappers map code moved to another
  // member, which is not carried over
  private void pushStatements(UMLOperationBodyMapper mapper, Map<String, Statement> located) {
    for (AbstractCodeMapping mapping : mapper.getMappings()) {
      link(statement(SrcDst.SRC, mapping.getFragment1().getLocationInfo(), located),
              statement(SrcDst.DST, mapping.getFragment2().getLocationInfo(), located));
    }
    for (AbstractCodeFragment fragment : mapper.getNonMappedLeavesT1()) {
      statement(SrcDst.SRC, fragment.getLocationInfo(), located);
    }
    for (AbstractCodeFragment fragment : mapper.getNonMappedInnerNodesT1()) {
      statement(SrcDst.SRC, fragment.getLocationInfo(), located);
    }
    for (AbstractCodeFragment fragment : mapper.getNonMappedLeavesT2()) {
      statement(SrcDst.DST, fragment.getLocationInfo(), located);
    }
    for (AbstractCodeFragment fragment : mapper.getNonMappedInnerNodesT2()) {
      statement(SrcDst.DST, fragment.getLocationInfo(), located);
    }

    for (Refactoring refactoring : mapper.getRefactoringsAfterPostProcessing()) {
      if (refactoring instanceof ExtractVariableRefactoring extract && extract.getVariableDeclaration().getInitializer() != null) {
        Statement value = statement(SrcDst.DST, extract.getVariableDeclaration().getInitializer().getLocationInfo(), located);
        for (AbstractCodeMapping reference : extract.getReferences()) {
          link(statement(SrcDst.SRC, reference.getFragment1().getLocationInfo(), located), value);
        }
      }
      if (refactoring instanceof InlineVariableRefactoring inline && inline.getVariableDeclaration().getInitializer() != null) {
        Statement value = statement(SrcDst.SRC, inline.getVariableDeclaration().getInitializer().getLocationInfo(), located);
        for (AbstractCodeMapping reference : inline.getReferences()) {
          link(value, statement(SrcDst.DST, reference.getFragment2().getLocationInfo(), located));
        }
      }
    }
  }

  private static LocationInfo declarationLocation(UMLAttribute attribute) {
    return attribute.getFieldDeclarationLocationInfo() != null ? attribute.getFieldDeclarationLocationInfo() : attribute.getLocationInfo();
  }

  private Statement statement(SrcDst srcDst, LocationInfo location, Map<String, Statement> located) {
    String key = srcDst + ":" + location.getFilePath() + ":" + location.getStartOffset() + ":" + location.getEndOffset();
    return located.computeIfAbsent(key, k -> {
      Statement statement = new Statement(location.getStartOffset(), location.getEndOffset());
      statements.get(srcDst).computeIfAbsent(location.getFilePath(), path -> new ArrayList<>()).add(statement);
      return statement;
    });
  }

  private static void link(Statement srcStatement, Statement dstStatement) {
    srcStatement.sameStatements.add(dstStatement);
    dstStatement.sameStatements.add(srcStatement);
  }

  // A code fragment RM holds as a statement, with the ones of the other revision it gives as the same statement
  static final class Statement {
    private final int start;
    private final int end;
    private final Set<Statement> sameStatements = new HashSet<>();

    private Statement(int start, int end) {
      this.start = start;
      this.end = end;
    }

    boolean isSameAs(@Nullable Statement other) {
      return sameStatements.contains(other);
    }

    // Whether the tree is the whole statement
    boolean isWhole(Tree tree) {
      return start == tree.getPos() && end == tree.getEndPos();
    }

    // The outermost tree of the statement holding the tree
    Tree outermostWithin(Tree tree) {
      Tree outermost = tree;
      while (outermost.getParent() != null && start <= outermost.getParent().getPos() && outermost.getParent().getEndPos() <= end) {
        outermost = outermost.getParent();
      }
      return outermost;
    }
  }
}
