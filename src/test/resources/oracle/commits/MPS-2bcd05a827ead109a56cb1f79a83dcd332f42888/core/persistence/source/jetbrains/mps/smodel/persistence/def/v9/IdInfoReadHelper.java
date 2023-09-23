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
package jetbrains.mps.smodel.persistence.def.v9;

import jetbrains.mps.persistence.MetaModelInfoProvider;
import jetbrains.mps.persistence.registry.ConceptInfo;
import jetbrains.mps.persistence.registry.IdInfoRegistry;
import jetbrains.mps.persistence.registry.LangInfo;
import jetbrains.mps.smodel.adapter.ids.SConceptId;
import jetbrains.mps.smodel.adapter.ids.SContainmentLinkId;
import jetbrains.mps.smodel.adapter.ids.SLanguageId;
import jetbrains.mps.smodel.adapter.ids.SPropertyId;
import jetbrains.mps.smodel.adapter.ids.SReferenceLinkId;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import jetbrains.mps.smodel.runtime.ConceptKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SConcept;
import org.jetbrains.mps.openapi.language.SContainmentLink;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.language.SProperty;
import org.jetbrains.mps.openapi.language.SReferenceLink;

import java.util.HashMap;
import java.util.Map;

/**
 * Facility to read meta-model information persisted in a model file, to fill {@link jetbrains.mps.smodel.persistence.def.v9.IdInfoCollector} back from the
 * serialized registry. Serves the task to parametrize ModelReader as well.
 *
 * Although barely a mediator to few other facilities, grabs great portion of code one would otherwise write in ModelReaderHandler.
 *
 * Stateful, withLanguage() identifies language for subsequent withConcept, which, furthermore, identify concept for any
 * subsequent #property(), #association() and #aggregation call.
 */
class IdInfoReadHelper {
  private final IdInfoRegistry myMetaRegistry;
  private final IdEncoder myIdEncoder;
  private final MetaModelInfoProvider myMetaInfoProvider;
  private LangInfo myActualLang;
  private ConceptInfo myActualConcept;
  private final Map<String, SConcept> myConcepts = new HashMap<String, SConcept>();
  private final Map<String, SProperty> myProperties = new HashMap<String, SProperty>();
  private final Map<String, SReferenceLink> myAssociations = new HashMap<String, SReferenceLink>();
  private final Map<String, SContainmentLink> myAggregations = new HashMap<String, SContainmentLink>();
  private final boolean myInterfaceOnly;
  private final boolean myStripImplementation;

  public IdInfoReadHelper(@NotNull MetaModelInfoProvider mmiProvider, boolean interfaceOnly, boolean stripImplementation) {
    myMetaInfoProvider = mmiProvider;
    myIdEncoder = new IdEncoder();
    myMetaRegistry = new IdInfoRegistry();
    myInterfaceOnly = interfaceOnly;
    myStripImplementation = stripImplementation;
  }

  @NotNull
  public IdEncoder getIdEncoder() {
    return myIdEncoder;
  }

  public boolean isRequestedInterfaceOnly() {
    return myInterfaceOnly;
  }

  public boolean isRequestedStripImplementation() {
    return myStripImplementation;
  }

  // Fill methods, populate myInfoCollector with persisted meta-model info

  public void withLanguage(String id, String name) {
    final SLanguageId languageId = myIdEncoder.parseLanguageId(id);
    myActualLang = myMetaRegistry.registerLanguage(languageId, name);
    myMetaInfoProvider.setLanguageName(languageId, name);
  }

  // @param stub is optional
  public void withConcept(String id, String name, String index, String nodeInfo, String stub) {
    assert myActualLang != null;
    SConceptId conceptId = myIdEncoder.parseConceptId(myActualLang.getLanguageId(), id);
    myActualConcept = myMetaRegistry.registerConcept(conceptId, name);
    myActualConcept.parseImplementationKind(nodeInfo);
    myConcepts.put(index, MetaAdapterFactory.getConcept(conceptId, name));
    myMetaInfoProvider.setConceptName(conceptId, name);
    myMetaInfoProvider.setKind(conceptId, myActualConcept.getKind());
    myMetaInfoProvider.setScope(conceptId, myActualConcept.getScope());
    if (stub != null) {
      // XXX here we imply stub concepts live in the save language as their origin
      final SConceptId stubId = myIdEncoder.parseConceptId(myActualLang.getLanguageId(), stub);
      myActualConcept.setStubCounterpart(stubId);
      myMetaInfoProvider.setStubConcept(conceptId, stubId);
    }
  }

  public void property(String id, String name, String index) {
    assert myActualConcept != null;
    SPropertyId propertyId = myIdEncoder.parsePropertyId(myActualConcept.getConceptId(), id);
    myActualConcept.addProperty(propertyId, name);
    myProperties.put(index, MetaAdapterFactory.getProperty(propertyId, name));
    myMetaInfoProvider.setPropertyName(propertyId, name);
  }

  public void association(String id, String name, String index) {
    assert myActualConcept != null;
    SReferenceLinkId linkId = myIdEncoder.parseAssociation(myActualConcept.getConceptId(), id);
    myActualConcept.addLink(linkId, name);
    myAssociations.put(index, MetaAdapterFactory.getReferenceLink(linkId, name));
    myMetaInfoProvider.setAssociationName(linkId, name);
  }

  public void aggregation(String id, String name, String index, boolean unordered) {
    assert myActualConcept != null;
    SContainmentLinkId linkId = myIdEncoder.parseAggregation(myActualConcept.getConceptId(), id);
    myActualConcept.addLink(linkId, name, unordered);
    myAggregations.put(index, MetaAdapterFactory.getContainmentLink(linkId, name));
    myMetaInfoProvider.setAggregationName(linkId, name);
    myMetaInfoProvider.setUnordered(linkId, unordered);
  }

  // Query. De-serialize ids, resolve indexes and retrieve meta-objects according to myInfoCollector state

  public SConcept readConcept(@NotNull String index) {
    assert myConcepts.containsKey(index);
    return myConcepts.get(index);
  }

  public SProperty readProperty(@NotNull String index) {
    assert myProperties.containsKey(index);
    return myProperties.get(index);
  }

  public SReferenceLink readAssociation(@NotNull String index) {
    assert myAssociations.containsKey(index);
    return myAssociations.get(index);
  }

  // nullable for root nodes; to minimize code in the sax reader check is done here
  public SContainmentLink readAggregation(@Nullable String index) {
    if (index == null) {
      return null;
    }
    assert myAggregations.containsKey(index);
    return myAggregations.get(index);
  }

  public boolean isInterface(@NotNull SConcept concept) {
    return ConceptKind.INTERFACE == myMetaRegistry.find(concept).getKind();
  }

  public boolean isImplementation(@NotNull SConcept concept) {
    return myMetaRegistry.find(concept).isImplementation();
  }
  public boolean isImplementationWithStub(@NotNull SConcept concept) {
    return myMetaRegistry.find(concept).isImplementationWithStub();
  }

  /**
   * This method shall be invoked only if {@link #isImplementationWithStub(org.jetbrains.mps.openapi.language.SConcept)} == <code>true</code>
   */
  @NotNull
  public SConcept getStubConcept(@NotNull SConcept original) {
    final ConceptInfo ci = myMetaRegistry.find(original);
    assert ci.getKind() == ConceptKind.IMPLEMENTATION_WITH_STUB;
    final SConceptId stub = ci.getStubCounterpart();
    assert stub != null;
    return MetaAdapterFactory.getConcept(stub, ci.constructStubConceptName());
  }

  public SLanguage getLanguage(@NotNull SLanguageId langId, @NotNull String langName) {
    // used languages is a subset of languages detected for meta-registry, don't want to use
    // set of languages available from myInfoCollector, which might not be yet ready, unless we ensure
    // proper read order (first registry, then used languages). It's even more complicated for per-root
    // persistence, where usedLanguages are kept in a header file only, while registry spans few.
    return MetaAdapterFactory.getLanguage(langId, langName);
  }
}
