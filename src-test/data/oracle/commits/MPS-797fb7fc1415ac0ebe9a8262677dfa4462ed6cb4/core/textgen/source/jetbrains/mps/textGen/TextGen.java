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
package jetbrains.mps.textGen;

import jetbrains.mps.messages.IMessage;
import jetbrains.mps.messages.Message;
import jetbrains.mps.messages.MessageKind;
import jetbrains.mps.text.BufferSnapshot;
import jetbrains.mps.text.MissingTextGenDescriptor;
import jetbrains.mps.text.impl.TextGenSupport;
import jetbrains.mps.text.impl.TextGenTransitionContext;
import jetbrains.mps.text.impl.TraceInfoCollector;
import jetbrains.mps.text.rt.TextGenDescriptor;
import jetbrains.mps.textgen.trace.ScopePositionInfo;
import jetbrains.mps.textgen.trace.TraceablePositionInfo;
import jetbrains.mps.textgen.trace.UnitPositionInfo;
import jetbrains.mps.util.EncodingUtil;
import jetbrains.mps.util.NameUtil;
import jetbrains.mps.util.annotation.ToRemove;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.model.SNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated Use {@link jetbrains.mps.text.TextGeneratorEngine} to produce text from models.
 * There's yet no alternative to transform single node to text, FIXME shall implement
 */
@Deprecated
@ToRemove(version = 3.3)
public class TextGen {
  public static final String PACKAGE_NAME = "PACKAGE_NAME";
  public static final String DEPENDENCY = "DEPENDENCY";
  public static final String EXTENDS = "EXTENDS";
  public static final String OUTPUT_ENCODING = "OUTPUT_ENCODING";
  public static final String ROOT_NODE = "ROOT_NODE";

  public static final String NO_TEXTGEN = "\33\33NO TEXTGEN\33\33";

  // api
  public static TextGenerationResult generateText(SNode node) {
    return generateText(node, false, false, null);
  }

  public static boolean canGenerateTextFor(SNode node) {
    return !(getTextGenForNode(node) instanceof MissingTextGenDescriptor);
  }

  public static String getExtension(@NotNull SNode node) {
    return getLegacyTextGen(node).getExtension(node);
  }

  public static String getFileName(@NotNull SNode node) {
    final SNodeTextGen tg = getLegacyTextGen(node);
    String fname = tg.getFilename(node);
    String extension = tg.getExtension(node);
    return (extension == null) ? fname : fname + '.' + extension;
  }

  public static TextGenerationResult generateText(SNode node, boolean failIfNoTextgen, boolean withDebugInfo, @Nullable StringBuilder[] buffers) {
    if (canGenerateTextFor(node)) {
      return generateText(node, withDebugInfo, buffers);
    } else if (failIfNoTextgen) {
      String error = "Can't generate text from " + node;
      Message m = new Message(MessageKind.ERROR, error);
      if (node != null) {
        m.setHintObject(node.getReference());
      }
      return new TextGenerationResult(node, NO_TEXTGEN, true, Collections.<IMessage>singleton(m), null, null, null, null);
    } else {
      return new TextGenerationResult(node, NO_TEXTGEN, false, Collections.<IMessage>emptyList(), null, null, null, null);
    }
  }

  public static TextGenerationResult generateText(SNode node, boolean withDebugInfo, @Nullable StringBuilder[] buffers) {
    TextGenBuffer buffer = new TextGenBuffer(withDebugInfo, buffers);
    buffer.putUserObject(PACKAGE_NAME, jetbrains.mps.util.SNodeOperations.getModelLongName(node.getModel()));
    buffer.putUserObject(ROOT_NODE, node);
    final TraceInfoCollector tic;
    if (withDebugInfo)  {
      tic = new TraceInfoCollector();
      TraceInfoGenerationUtil.setTraceInfoCollector(buffer, tic);
    } else {
      tic = null;
    }

    appendNodeText(buffer, node);

    // position info
    Map<SNode, TraceablePositionInfo> positionInfo = null;
    Map<SNode, ScopePositionInfo> scopeInfo = null;
    Map<SNode, UnitPositionInfo> unitInfo = null;
    final BufferSnapshot textSnapshot = buffer.getTextSnapshot();
    if (tic != null) {
      tic.populatePositions(textSnapshot);
      //
      positionInfo = tic.getTracePositions();
      scopeInfo = tic.getScopePositions();
      unitInfo = tic.getUnitPositions();
    }

    // dependencies
    List<String> dependencies = getUserObjectCollection(DEPENDENCY, node, buffer, (Set<String>) buffer.getUserObject(EXTENDS));
    List<String> extend = getUserObjectCollection(EXTENDS, node, buffer, null);

    Map<String, List<String>> deps = new HashMap<String, List<String>>(2);
    deps.put(DEPENDENCY, dependencies);
    deps.put(EXTENDS, extend);

    final String bufferOutcome = textSnapshot.getText().toString();
    Object result = bufferOutcome;
    String outputEncoding = (String) buffer.getUserObject(OUTPUT_ENCODING);
    if (outputEncoding != null) {
      if (outputEncoding.equals("binary")) {
        result = EncodingUtil.decodeBase64(bufferOutcome);
      } else {
        try {
          result = EncodingUtil.encode(bufferOutcome, outputEncoding);
        } catch (IOException ex) {
          buffer.foundError("cannot encode the output stream", null, ex);
        }
      }
    }
    return new TextGenerationResult(node, result, buffer.hasErrors(), buffer.problems(), positionInfo, scopeInfo, unitInfo, deps);
  }

  private static void appendNodeText(TextGenBuffer buffer, SNode node) {
    if (node == null) {
      buffer.append("???");
      return;
    }

    TextGenSupport tgs = new TextGenSupport(new TextGenTransitionContext(node, buffer));
    tgs.appendNode(node);
  }

  // helper stuff
  @NotNull
  /*package*/ static TextGenDescriptor getTextGenForNode(@NotNull SNode node) {
    return TextGenRegistry.getInstance().getTextGenDescriptor(node);
  }

  // compatibility code until TextUnit and code to break input model into these units, with filename assigned, are introduced.
  private static SNodeTextGen getLegacyTextGen(@NotNull SNode node) {
    try {
      Class<? extends SNodeTextGen> textgenClass = TextGenRegistry.getInstance().getLegacyTextGenClass(node.getConcept());
      if (textgenClass != null && SNodeTextGen.class.isAssignableFrom(textgenClass)) {
        return textgenClass.newInstance();
      }
    } catch (InstantiationException ex) {
      Logger.getLogger(TextGen.class).error("Failed to instantiate textgen", ex);
      // fall-through
    } catch (IllegalAccessException ex) {
      Logger.getLogger(TextGen.class).error("Failed to instantiate textgen", ex);
      // fall-through
    }
    return new DefaultTextGen();
  }

  private static List<String> getUserObjectCollection(String key, SNode node, TextGenBuffer buffer, Set<String> skipSet) {
    Set<String> dependenciesObject = (Set<String>) buffer.getUserObject(key);
    final String nodeFQName = NameUtil.nodeFQName(node);
    if (dependenciesObject != null) {
      List<String> dependencies = new ArrayList<String>(dependenciesObject.size());
      for (String dependObj : dependenciesObject) {
        if (dependObj == null || nodeFQName.equals(dependObj)) {
          continue;
        }
        if (skipSet != null && skipSet.contains(dependObj)) {
          continue;
        }
        dependencies.add(dependObj);
      }
      Collections.sort(dependencies);
      return dependencies;
    }
    return Collections.emptyList();
  }
}
