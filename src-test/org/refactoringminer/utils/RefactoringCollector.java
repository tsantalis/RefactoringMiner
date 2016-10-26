package org.refactoringminer.utils;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm2.model.refactoring.SDRefactoring;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;

public class RefactoringCollector extends RefactoringHandler {
  private final RefactoringSet rs;
  private Exception ex = null;
  public RefactoringCollector(String cloneUrl, String commitId) {
    rs = new RefactoringSet(cloneUrl, commitId);
  }
  @Override
  public void handle(RevCommit commitData, List<Refactoring> refactorings) {
    for (Refactoring r : refactorings) {
      if (r instanceof SDRefactoring) {
        SDRefactoring sdr = (SDRefactoring) r;
        rs.add(new RefactoringRelationship(sdr.getRefactoringType(), sdr.getEntityBefore().toString(), sdr.getEntityAfter().toString()));
      } else if (r instanceof MoveClassRefactoring) {
        MoveClassRefactoring ref = (MoveClassRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getOriginalClassName(), ref.getMovedClassName()));
      } else if (r instanceof RenameClassRefactoring) {
        RenameClassRefactoring ref = (RenameClassRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getOriginalClassName(), ref.getRenamedClassName()));
      } else if (r instanceof ExtractSuperclassRefactoring) {
        ExtractSuperclassRefactoring ref = (ExtractSuperclassRefactoring) r;
        for (String subclass : ref.getSubclassSet()) {
          rs.add(new RefactoringRelationship(r.getRefactoringType(), subclass, ref.getExtractedClass().getName()));
        }
      } else if (r instanceof MoveOperationRefactoring) {
        MoveOperationRefactoring ref = (MoveOperationRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getOriginalOperation().getKey(), ref.getMovedOperation().getKey()));
      } else if (r instanceof RenameOperationRefactoring) {
        RenameOperationRefactoring ref = (RenameOperationRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getOriginalOperation().getKey(), ref.getRenamedOperation().getKey()));
      } else if (r instanceof ExtractOperationRefactoring) {
        ExtractOperationRefactoring ref = (ExtractOperationRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getExtractedFromOperation().getKey(), ref.getExtractedOperation().getKey()));
      } else if (r instanceof InlineOperationRefactoring) {
        InlineOperationRefactoring ref = (InlineOperationRefactoring) r;
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getInlinedOperation().getKey(), ref.getInlinedToOperation().getKey()));
      } else if (r instanceof MoveAttributeRefactoring) {
        MoveAttributeRefactoring ref = (MoveAttributeRefactoring) r;
        String attrName = ref.getMovedAttribute().getName();
        rs.add(new RefactoringRelationship(r.getRefactoringType(), ref.getSourceClassName() + "#" + attrName, ref.getTargetClassName() + "#" + attrName));
      } else {
        throw new RuntimeException("refactoring not supported");
      }
    }
  }
  @Override
  public void handleException(String commitId, Exception e) {
    this.ex = e;
  }
  public RefactoringSet assertAndGetResult() {
    if (ex == null) {
      return rs;
    }
    throw new RuntimeException(ex); 
  }
}