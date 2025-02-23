package jetbrains.mps.lang.textGen.editor;

/*Generated by MPS */

import jetbrains.mps.openapi.editor.descriptor.EditorAspectDescriptor;
import java.util.Collection;
import jetbrains.mps.openapi.editor.descriptor.ConceptEditor;
import jetbrains.mps.smodel.runtime.ConceptDescriptor;
import java.util.Arrays;
import java.util.Collections;
import jetbrains.mps.openapi.editor.descriptor.ConceptEditorComponent;

public class EditorAspectDescriptorImpl implements EditorAspectDescriptor {

  public Collection<ConceptEditor> getEditors(ConceptDescriptor descriptor) {
    switch (Arrays.binarySearch(stringSwitchCases_xbvbvu_a0a0b, descriptor.getConceptFqName())) {
      case 0:
        return Collections.<ConceptEditor>singletonList(new AppendOperation_Editor());
      case 1:
        return Collections.<ConceptEditor>singletonList(new CollectionAppendPart_Editor());
      case 2:
        return Collections.<ConceptEditor>singletonList(new ConceptTextGenDeclaration_Editor());
      case 3:
        return Collections.<ConceptEditor>singletonList(new ConstantStringAppendPart_Editor());
      case 4:
        return Collections.<ConceptEditor>singletonList(new EncodingLiteral_Editor());
      case 5:
        return Collections.<ConceptEditor>singletonList(new FoundErrorOperation_Editor());
      case 6:
        return Collections.<ConceptEditor>singletonList(new LanguageTextGenDeclaration_Editor());
      case 7:
        return Collections.<ConceptEditor>singletonList(new NewLineAppendPart_Editor());
      case 8:
        return Collections.<ConceptEditor>singletonList(new NodeAppendPart_Editor());
      case 9:
        return Collections.<ConceptEditor>singletonList(new OperationCall_Editor());
      case 10:
        return Collections.<ConceptEditor>singletonList(new OperationDeclaration_Editor());
      case 11:
        return Collections.<ConceptEditor>singletonList(new ReferenceAppendPart_Editor());
      case 12:
        return Collections.<ConceptEditor>singletonList(new SimpleTextGenOperation_Editor());
      case 13:
        return Collections.<ConceptEditor>singletonList(new StubOperationDeclaration_Editor());
      case 14:
        return Collections.<ConceptEditor>singletonList(new UtilityMethodCall_Editor());
      case 15:
        return Collections.<ConceptEditor>singletonList(new UtilityMethodDeclaration_Editor());
      case 16:
        return Collections.<ConceptEditor>singletonList(new WithIndentOperation_Editor());
      default:
    }
    return Collections.<ConceptEditor>emptyList();
  }
  public Collection<ConceptEditorComponent> getEditorComponents(ConceptDescriptor descriptor, String editorComponentId) {
    return Collections.<ConceptEditorComponent>emptyList();
  }


  private static String[] stringSwitchCases_xbvbvu_a0a0b = new String[]{"jetbrains.mps.lang.textGen.structure.AppendOperation", "jetbrains.mps.lang.textGen.structure.CollectionAppendPart", "jetbrains.mps.lang.textGen.structure.ConceptTextGenDeclaration", "jetbrains.mps.lang.textGen.structure.ConstantStringAppendPart", "jetbrains.mps.lang.textGen.structure.EncodingLiteral", "jetbrains.mps.lang.textGen.structure.FoundErrorOperation", "jetbrains.mps.lang.textGen.structure.LanguageTextGenDeclaration", "jetbrains.mps.lang.textGen.structure.NewLineAppendPart", "jetbrains.mps.lang.textGen.structure.NodeAppendPart", "jetbrains.mps.lang.textGen.structure.OperationCall", "jetbrains.mps.lang.textGen.structure.OperationDeclaration", "jetbrains.mps.lang.textGen.structure.ReferenceAppendPart", "jetbrains.mps.lang.textGen.structure.SimpleTextGenOperation", "jetbrains.mps.lang.textGen.structure.StubOperationDeclaration", "jetbrains.mps.lang.textGen.structure.UtilityMethodCall", "jetbrains.mps.lang.textGen.structure.UtilityMethodDeclaration", "jetbrains.mps.lang.textGen.structure.WithIndentOperation"};
}
