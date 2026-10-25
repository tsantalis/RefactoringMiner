package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractStatement;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Where the tree of a node lies in what RM gives of its revision, whatever its language.
 *
 * @param statement      the statement of a body holding the tree, which may be the tree itself; none when it is a
 *                       comment of the body
 * @param statementTree  the tree of that statement or comment
 * @param operation      the innermost operation holding the tree, other than the one it declares
 * @param type           the innermost class holding the tree, other than the one it declares (an anonymous class is
 *                       none)
 * @param typeHeader     the header of that class (see {@link #header})
 * @param declaredOperation the operation the tree declares
 * @param declaredClass  the class the tree declares
 * @param header         the header of what the tree declares: the parts of an operation but its body, or the parts of a
 *                       class before its body (its doc comment, annotations, modifiers, name, type parameters and
 *                       supertypes)
 * @param signatureStart the part of the tree it starts at as a declaration, after its doc comment and annotations, or
 *                       itself
 * @param body           whether the tree is a body: of an operation, an initializer or a lambda, a block a composite runs,
 *                       or the statements of a try or a catch clause
 * @param bodyFrom       the code that body is printed from, its signature for an operation; none when it is printed as
 *                       it is (a block of a block, a block following another block of its composite, a finally block)
 * @param simpleStatement the declaration, expression, return or throw statement, or the field declaration, the tree is
 *                       printed with as an expression, unless it is it or lies in one of its lambdas or anonymous classes
 * @param declaration    the code declaring the local the tree is, when the tree does not hold its type, as the
 *                       declaration statement of one of its variables
 * @param fileParts      the package declaration and the imports of the file
 */
public record RMInfo(@Nullable AbstractStatement statement, @Nullable Tree statementTree, @Nullable UMLOperation operation,
                     @Nullable UMLClass type, List<Tree> typeHeader, @Nullable UMLOperation declaredOperation,
                     @Nullable UMLClass declaredClass, List<Tree> header, Tree signatureStart, boolean body,
                     @Nullable Tree bodyFrom, @Nullable Tree simpleStatement, @Nullable Tree declaration,
                     List<LocationInfo> fileParts) {

  // The part of the header the tree lies in
  @Nullable
  public static Tree headerChild(Tree tree, List<Tree> header) {
    for (Tree ancestor = tree; ancestor != null; ancestor = ancestor.getParent()) {
      Tree part = ancestor;
      if (header.stream().anyMatch(child -> child == part)) {
        return part;
      }
    }
    return null;
  }
}
