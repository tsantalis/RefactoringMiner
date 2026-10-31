package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The statements RM gives as the same statement in both revisions: the ones the body mappers of the members it matches
 * map, the value of an extracted (inlined) variable and the statements it is replaced in, and the declarations of the
 * fields it matches, which hold the code of their initializers, and the members it matches (see {@link RMIndex} for what RM
 * gives of one revision).
 */
class RMLinks {
  private final UMLModelDiff modelDiff;
  @Nullable
  private Map<SrcDst, Map<String, List<Statement>>> statements;
  @Nullable
  private Map<VariableDeclarationContainer, Set<VariableDeclarationContainer>> matchedMembers;

  RMLinks(UMLModelDiff modelDiff) {
    this.modelDiff = modelDiff;
  }

  // Whether RM gives the members as one member of both revisions: the same declaration, or the ones a body mapper of
  // their classes matches, but not the members code is moved between (an extracted or inlined method)
  boolean isSameMember(VariableDeclarationContainer srcMember, VariableDeclarationContainer dstMember) {
    if (srcMember == dstMember) {
      return true;
    }
    if (matchedMembers == null) {
      matchedMembers = new IdentityHashMap<>();
      for (UMLAbstractClassDiff classDiff : classDiffs()) {
        for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperListIncludingNestedMappersInAnonymousClassDiffs()) {
          matchedMembers.computeIfAbsent(mapper.getContainer1(), member -> Collections.newSetFromMap(new IdentityHashMap<>()))
                  .add(mapper.getContainer2());
        }
      }
    }
    return matchedMembers.getOrDefault(srcMember, Set.of()).contains(dstMember);
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
      for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperListIncludingNestedMappersInAnonymousClassDiffs()) {
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
