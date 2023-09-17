/*
 * Copyright 2003-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.mps.text.impl;

import jetbrains.mps.smodel.DynamicReference;
import jetbrains.mps.text.TextArea;
import jetbrains.mps.text.TextMark;
import jetbrains.mps.text.rt.TextGenContext;
import jetbrains.mps.textGen.TextGen;
import jetbrains.mps.textGen.TextGenBuffer;
import jetbrains.mps.textGen.TextGenRegistry;
import jetbrains.mps.textGen.TraceInfoGenerationUtil;
import jetbrains.mps.textgen.trace.ScopePositionInfo;
import jetbrains.mps.textgen.trace.TraceablePositionInfo;
import jetbrains.mps.textgen.trace.UnitPositionInfo;
import jetbrains.mps.util.SNodeOperations;
import jetbrains.mps.util.annotation.ToRemove;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.model.SModelReference;
import org.jetbrains.mps.openapi.model.SNode;
import org.jetbrains.mps.openapi.model.SReference;

import java.util.List;

/**
 * Facility to keep implementation methods I don't want to get exposed in TextGenDescriptorBase or elsewhere deemed API.
 * Besides it helps to keep state of the {@link jetbrains.mps.text.rt.TextGenDescriptor#generateText(TextGenContext)} invocation
 * context and to shorten argument list of utility methods.
 * Generated code shall use this class to perform various operations that are not straightforward enough to get generated
 * right into <code>TextGenDescriptor</code>.
 * @author Artem Tikhomirov
 */
public final class TextGenSupport implements TextArea {
  private final TextGenContext myContext;
  private final TraceInfoCollector myTraceInfoCollector;

  public TextGenSupport(@NotNull TextGenContext context) {
    myContext = context;
    final TextGenBuffer buffer = ((TextGenTransitionContext) context).getLegacyBuffer();
    myTraceInfoCollector = TraceInfoGenerationUtil.getTraceInfoCollector(buffer);
  }

  public boolean needPositions() {
    return getTraceInfoCollector() != null;
  }

  public void createPositionInfo() {
    if (needPositions()) {
      myContext.getBuffer().pushMark();
    }
  }

  public void createScopeInfo( ) {
    if (needPositions()) {
      myContext.getBuffer().pushMark();
    }
  }

  public void createUnitInfo() {
    if (needPositions()) {
      myContext.getBuffer().pushMark();
    }
  }

  public void fillPositionInfo(String propertyString) {
    final TraceInfoCollector tic = getTraceInfoCollector();
    if (tic == null) {
      return;
    }
    TextMark m = myContext.getBuffer().popMark();
    final TraceablePositionInfo pi = tic.createTracePosition(m, myContext.getPrimaryInput());
    pi.setPropertyString(propertyString);
  }

  public void fillScopeInfo(List<SNode> vars) {
    final TraceInfoCollector tic = getTraceInfoCollector();
    if (tic == null) {
      return;
    }
    TextMark m = myContext.getBuffer().popMark();
    final ScopePositionInfo pi = tic.createScopePosition(m, myContext.getPrimaryInput());
    for (SNode var : vars) {
      if (var != null) {
        pi.addVarInfo(var);
      }
    }
  }

  public void fillUnitInfo(String unitName) {
    final TraceInfoCollector tic = getTraceInfoCollector();
    if (tic == null) {
      return;
    }
    TextMark m = myContext.getBuffer().popMark();
    final UnitPositionInfo pi = tic.createUnitPosition(m, myContext.getPrimaryInput());
    pi.setUnitName(unitName);
    TraceInfoGenerationUtil.warnIfUnitNameInvalid(unitName, myContext.getPrimaryInput());
  }

  private TraceInfoCollector getTraceInfoCollector() {
    return myTraceInfoCollector;
  }

  public void appendNode(SNode node) {
    final TextGenBuffer buffer = getLegacyBuffer();
    TextGenRegistry.getInstance().getTextGenDescriptor(node).generateText(new TextGenTransitionContext(node, buffer));
  }

  // FIXME copy of SNodeTextGen.foundError()
  public void reportError(String info) {
    String message = info != null ?
        "textgen error: '" + info + "' in " + SNodeOperations.getDebugText(myContext.getPrimaryInput()) :
        "textgen error in " + SNodeOperations.getDebugText(myContext.getPrimaryInput());
    getLegacyBuffer().foundError(message, myContext.getPrimaryInput(), null);
  }

  // FIXME copy of SNodeTextGen.setEncoding()
  public void setEncoding(@Nullable String encoding) {
    getLegacyBuffer().putUserObject(TextGen.OUTPUT_ENCODING, encoding);
  }

  // FIXME copy of SNodeTextGen.getReferentPresentation(), slightly modified to drop dead code branches
  public void appendReferentText(SReference reference) {
    if (reference == null) {
      reportError("null reference");
      append("<err:null reference>");
      return;
    }

    String shortName;
    if (reference instanceof DynamicReference) {
      shortName = ((DynamicReference) reference).getResolveInfo();
      if (shortName.startsWith("[")) {
        // todo: hack, remove with [] removing
        shortName = shortName.substring(shortName.lastIndexOf("]") + 1).trim();
      } else {
        final SModelReference modelReference = reference.getTargetSModelReference();
        if (modelReference == null) {
          int lastDot = shortName.lastIndexOf('.');
          if (lastDot >= 0) {
            shortName = shortName.substring(lastDot + 1);
            if (shortName.indexOf('$') >= 0) {
              shortName = shortName.replace('$', '.');
            }
          }
        }
      }
    } else {
      SNode targetNode = reference.getTargetNode();
      if (targetNode == null) {
        reportError(String.format("Unknown target for role %s", reference.getRole()));
        append("???");
        return;
      }
      shortName = jetbrains.mps.util.SNodeOperations.getResolveInfo(targetNode);
    }
    append(shortName);
  }

  // For compatibility with code that uses TextGenBuffer through 'buffer' concept function parameter
  @ToRemove(version = 3.3)
  public TextGenBuffer getLegacyBuffer() {
    return ((TextGenTransitionContext) myContext).getLegacyBuffer();
  }

  ////////////
  // TextArea. Simply delegate to whatever actual text area of the buffer is.

  @Override
  public TextArea append(CharSequence text) {
    return myContext.getBuffer().area().append(text);
  }

  @Override
  public TextArea newLine() {
    return myContext.getBuffer().area().newLine();
  }

  @Override
  public TextArea indent() {
    return myContext.getBuffer().area().indent();
  }

  @Override
  public TextArea increaseIndent() {
    return myContext.getBuffer().area().increaseIndent();
  }

  @Override
  public TextArea decreaseIndent() {
    return myContext.getBuffer().area().decreaseIndent();
  }

  @Override
  public int length() {
    return myContext.getBuffer().area().length();
  }
}
