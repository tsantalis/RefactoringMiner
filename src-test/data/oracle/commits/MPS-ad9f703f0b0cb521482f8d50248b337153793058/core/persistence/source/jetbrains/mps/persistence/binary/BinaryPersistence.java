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
package jetbrains.mps.persistence.binary;

import jetbrains.mps.extapi.model.GeneratableSModel;
import jetbrains.mps.generator.ModelDigestUtil;
import jetbrains.mps.generator.ModelDigestUtil.DigestBuilderOutputStream;
import jetbrains.mps.persistence.IdHelper;
import jetbrains.mps.persistence.IndexAwareModelFactory.Callback;
import jetbrains.mps.persistence.MetaModelInfoProvider;
import jetbrains.mps.persistence.MetaModelInfoProvider.BaseMetaModelInfo;
import jetbrains.mps.persistence.MetaModelInfoProvider.RegularMetaModelInfo;
import jetbrains.mps.persistence.MetaModelInfoProvider.StuffedMetaModelInfo;
import jetbrains.mps.persistence.registry.AggregationLinkInfo;
import jetbrains.mps.persistence.registry.AssociationLinkInfo;
import jetbrains.mps.persistence.registry.ConceptInfo;
import jetbrains.mps.persistence.registry.IdInfoRegistry;
import jetbrains.mps.persistence.registry.LangInfo;
import jetbrains.mps.persistence.registry.PropertyInfo;
import jetbrains.mps.smodel.DefaultSModel;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SModelHeader;
import jetbrains.mps.smodel.adapter.ids.SConceptId;
import jetbrains.mps.smodel.adapter.ids.SContainmentLinkId;
import jetbrains.mps.smodel.adapter.ids.SLanguageId;
import jetbrains.mps.smodel.adapter.ids.SPropertyId;
import jetbrains.mps.smodel.adapter.ids.SReferenceLinkId;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import jetbrains.mps.smodel.loading.ModelLoadResult;
import jetbrains.mps.smodel.loading.ModelLoadingState;
import jetbrains.mps.smodel.persistence.def.ModelReadException;
import jetbrains.mps.smodel.persistence.def.v9.IdInfoCollector;
import jetbrains.mps.smodel.runtime.ConceptKind;
import jetbrains.mps.smodel.runtime.StaticScope;
import jetbrains.mps.util.FileUtil;
import jetbrains.mps.util.IterableUtil;
import jetbrains.mps.util.NameUtil;
import jetbrains.mps.util.io.ModelInputStream;
import jetbrains.mps.util.io.ModelOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.model.SModelReference;
import org.jetbrains.mps.openapi.model.SNode;
import org.jetbrains.mps.openapi.model.SNodeId;
import org.jetbrains.mps.openapi.module.SModuleReference;
import org.jetbrains.mps.openapi.persistence.StreamDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static jetbrains.mps.smodel.SModel.ImportElement;

/**
 * @author evgeny, 11/21/12
 * @author Artem Tikhomirov
 */
public final class BinaryPersistence {

  private final MetaModelInfoProvider myMetaInfoProvider;
  private final SModel myModelData;

  public static SModelHeader readHeader(@NotNull StreamDataSource source) throws ModelReadException {
    ModelInputStream mis = null;
    try {
      mis = new ModelInputStream(source.openInputStream());
      return loadHeader(mis);
    } catch (IOException e) {
      throw new ModelReadException("Couldn't read model: " + e.getMessage(), e);
    } finally {
      FileUtil.closeFileSafe(mis);
    }
  }

  public static ModelLoadResult readModel(@NotNull SModelHeader header, @NotNull StreamDataSource source, boolean interfaceOnly) throws ModelReadException {
    final SModelReference desiredModelRef = header.getModelReference();
    try {
      ModelLoadResult rv = loadModel(source.openInputStream(), interfaceOnly, header.getMetaInfoProvider());
      SModelReference actualModelRef = rv.getModel().getReference();
      if (!actualModelRef.equals(desiredModelRef)) {
        throw new ModelReadException(String.format("Intended to read model %s, actually read %s", desiredModelRef, actualModelRef), null, actualModelRef);
      }
      return rv;
    } catch (IOException e) {
      throw new ModelReadException("Couldn't read model: " + e.toString(), e, desiredModelRef);
    }
  }

  @NotNull
  public static SModel readModel(@NotNull final InputStream content) throws ModelReadException {
    try {
      return loadModel(content, false, null).getModel();
    } catch (IOException e) {
      throw new ModelReadException("Couldn't read model: " + e.toString(), e);
    }
  }

  public static void writeModel(@NotNull SModel model, @NotNull StreamDataSource dataSource) throws IOException {
    if (dataSource.isReadOnly()) {
      throw new IOException(String.format("`%s' is read-only", dataSource.getLocation()));
    }
    writeModel(model, dataSource.openOutputStream());
  }
  public static void writeModel(@NotNull SModel model, @NotNull OutputStream stream) throws IOException {
    ModelOutputStream os = null;
    try {
      os = new ModelOutputStream(stream);
      saveModel(model, os);
    } finally {
      FileUtil.closeFileSafe(os);
    }
  }

  public static Map<String, String> getDigestMap(jetbrains.mps.smodel.SModel model) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    IdInfoRegistry meta = null;
    DigestBuilderOutputStream os = ModelDigestUtil.createDigestBuilderOutputStream();
    try {
      BinaryPersistence bp = new BinaryPersistence(new RegularMetaModelInfo(), model);
      ModelOutputStream mos = new ModelOutputStream(os);
      meta = bp.saveModelProperties(mos);
      mos.flush();
    } catch (IOException ignored) {
      assert false;
      /* should never happen */
    }
    result.put(GeneratableSModel.HEADER, os.getResult());

    assert meta != null;
    // In fact, would be better to translate index attribute of any XXXInfo element into
    // a value not related to meta-element position in the registry. Otherwise, almost any change
    // in a model (e.g. addition of a new root or new property value) might affect all other root hashes
    // as the index of meta-model elements might change. However, as long as our binary models are not exposed
    // for user editing, we don't care.

    for (SNode node : model.getRootNodes()) {
      os = ModelDigestUtil.createDigestBuilderOutputStream();
      try {
        ModelOutputStream mos = new ModelOutputStream(os);
        new NodesWriter(model.getReference(), mos, meta).writeNode(node);
        mos.flush();
      } catch (IOException ignored) {
        assert false;
        /* should never happen */
      }
      SNodeId nodeId = node.getNodeId();
      if (nodeId != null) {
        result.put(nodeId.toString(), os.getResult());
      }
    }

    return result;
  }


  private static final int HEADER_START   = 0x91ABABA9;
  private static final int STREAM_ID_V1   = 0x00000300;
  private static final int STREAM_ID_V2   = 0x00000400;
  private static final int STREAM_ID      = STREAM_ID_V2;
  private static final byte HEADER_ATTRIBUTES = 0x7e;
  private static final int HEADER_END     = 0xabababab;
  private static final int MODEL_START    = 0xbabababa;
  private static final int REGISTRY_START = 0x5a5a5a5a;
  private static final int REGISTRY_END   = 0xa5a5a5a5;
  private static final byte STUB_NONE     = 0x12;
  private static final byte STUB_ID       = 0x13;



  @NotNull
  private static SModelHeader loadHeader(ModelInputStream is) throws IOException {
    if (is.readInt() != HEADER_START) {
      throw new IOException("bad stream, no header");
    }

    int streamId = is.readInt();
    if (streamId == STREAM_ID_V1) {
      throw new IOException(String.format("Can't read old binary persistence version (%x), please re-save models", streamId));
    }
    if (streamId != STREAM_ID) {
      throw new IOException(String.format("bad stream, unknown version: %x", streamId));
    }

    SModelReference modelRef = is.readModelReference();
    SModelHeader result = new SModelHeader();
    result.setModelReference(modelRef);
    is.readInt(); //left for compatibility: old version was here
    is.mark(4);
    if (is.readByte() == HEADER_ATTRIBUTES) {
      result.setDoNotGenerate(is.readBoolean());
      int propsCount = is.readShort();
      for (; propsCount > 0; propsCount--) {
        String key = is.readString();
        String value = is.readString();
        result.setOptionalProperty(key, value);
      }
    } else {
      is.reset();
    }
    assertSyncToken(is, HEADER_END);
    return result;
  }
  @NotNull
  private static ModelLoadResult loadModel(InputStream is, boolean interfaceOnly, @Nullable MetaModelInfoProvider mmiProvider) throws IOException {
    ModelInputStream mis = null;
    try {
      mis = new ModelInputStream(is);
      SModelHeader modelHeader = loadHeader(mis);

      DefaultSModel model = new DefaultSModel(modelHeader.getModelReference(), modelHeader);
      BinaryPersistence bp = new BinaryPersistence(mmiProvider == null ? new RegularMetaModelInfo() : mmiProvider, model);
      ReadHelper rh = bp.loadModelProperties(mis);
      rh.requestInterfaceOnly(interfaceOnly);

      NodesReader reader = new NodesReader(modelHeader.getModelReference(), mis, rh);
      reader.readNodesInto(model);
      return new ModelLoadResult((SModel) model, reader.hasSkippedNodes() ? ModelLoadingState.INTERFACE_LOADED : ModelLoadingState.FULLY_LOADED);
    } finally {
      FileUtil.closeFileSafe(mis);
    }
  }

  private static void saveModel(SModel model, ModelOutputStream os) throws IOException {
    final MetaModelInfoProvider mmiProvider;
    if (model instanceof DefaultSModel && ((DefaultSModel) model).getSModelHeader().getMetaInfoProvider() != null) {
      mmiProvider = ((DefaultSModel) model).getSModelHeader().getMetaInfoProvider();
    } else {
      mmiProvider = new RegularMetaModelInfo();
    }
    BinaryPersistence bp = new BinaryPersistence(mmiProvider, model);
    IdInfoRegistry meta = bp.saveModelProperties(os);

    Collection<SNode> roots = IterableUtil.asCollection(model.getRootNodes());
    new NodesWriter(model.getReference(), os, meta).writeNodes(roots);
  }

  private BinaryPersistence(@NotNull MetaModelInfoProvider mmiProvider, SModel modelData) {
    myMetaInfoProvider = mmiProvider;
    myModelData = modelData;
  }

  private ReadHelper loadModelProperties(ModelInputStream is) throws IOException {
    final ReadHelper readHelper = loadRegistry(is);

    loadUsedLanguages(is);

    for (SModuleReference ref : loadModuleRefList(is)) myModelData.addEngagedOnGenerationLanguage(ref);
    for (SModuleReference ref : loadModuleRefList(is)) myModelData.addDevKit(ref);

    for (ImportElement imp : loadImports(is)) myModelData.addModelImport(imp);

    assertSyncToken(is, MODEL_START);

    return readHelper;
  }

  private IdInfoRegistry saveModelProperties(ModelOutputStream os) throws IOException {
    // header
    os.writeInt(HEADER_START);
    os.writeInt(STREAM_ID);
    os.writeModelReference(myModelData.getReference());
    os.writeInt(-1);  //old model version
    if (myModelData instanceof DefaultSModel) {
      os.writeByte(HEADER_ATTRIBUTES);
      SModelHeader mh = ((DefaultSModel) myModelData).getSModelHeader();
      os.writeBoolean(mh.isDoNotGenerate());
      Map<String, String> props = new HashMap<String, String>(mh.getOptionalProperties());
      os.writeShort(props.size());
      for (Entry<String, String> e : props.entrySet()) {
        os.writeString(e.getKey());
        os.writeString(e.getValue());
      }
    }
    os.writeInt(HEADER_END);

    final IdInfoRegistry rv = saveRegistry(os);

    //languages
    saveUsedLanguages(os);
    saveModuleRefList(myModelData.engagedOnGenerationLanguages(), os);
    saveModuleRefList(myModelData.importedDevkits(), os);

    // imports
    saveImports(myModelData.importedModels(), os);
    // no need to save implicit imports as we serialize them ad-hoc, the moment we find external reference from a node

    os.writeInt(MODEL_START);
    return rv;
  }

  private IdInfoRegistry saveRegistry(ModelOutputStream os) throws IOException {
    os.writeInt(REGISTRY_START);
    IdInfoRegistry metaInfo = new IdInfoRegistry();
    new IdInfoCollector(metaInfo, myMetaInfoProvider).fill(myModelData.getRootNodes());
    List<LangInfo> languagesInUse = metaInfo.getLanguagesInUse();
    os.writeShort(languagesInUse.size());
    // We use position of an element during persistence as its index, thus don't need to
    // keep the index value - can restore it during read
    int langIndex, conceptIndex, propertyIndex, associationIndex, aggregationIndex;
    langIndex = conceptIndex = propertyIndex = associationIndex = aggregationIndex = 0;
    for(LangInfo ul : languagesInUse) {
      os.writeUUID(ul.getLanguageId().getIdValue());
      os.writeString(ul.getName());
      ul.setIntIndex(langIndex++);
      //
      List<ConceptInfo> conceptsInUse = ul.getConceptsInUse();
      os.writeShort(conceptsInUse.size());
      for (ConceptInfo ci : conceptsInUse) {
        os.writeLong(ci.getConceptId().getIdValue());
        assert ul.getName().equals(NameUtil.namespaceFromConceptFQName(ci.getName())) : "We save concept short name. This check ensures we can re-construct fqn based on language name";
        os.writeString(ci.getBriefName());
        os.writeByte(ci.getKind().ordinal() << 4 | ci.getScope().ordinal());
        if (ci.isImplementationWithStub()) {
          os.writeByte(STUB_ID);
          os.writeLong(ci.getStubCounterpart().getIdValue());
        } else {
          os.writeByte(STUB_NONE);
        }
        ci.setIntIndex(conceptIndex++);
        //
        List<PropertyInfo> propertiesInUse = ci.getPropertiesInUse();
        os.writeShort(propertiesInUse.size());
        for(PropertyInfo pi : propertiesInUse) {
          os.writeLong(pi.getPropertyId().getIdValue());
          os.writeString(pi.getName());
          pi.setIntIndex(propertyIndex++);
        }
        //
        List<AssociationLinkInfo> associationsInUse = ci.getAssociationsInUse();
        os.writeShort(associationsInUse.size());
        for (AssociationLinkInfo li : associationsInUse) {
          os.writeLong(li.getLinkId().getIdValue());
          os.writeString(li.getName());
          li.setIntIndex(associationIndex++);
        }
        //
        List<AggregationLinkInfo> aggregationsInUse = ci.getAggregationsInUse();
        os.writeShort(aggregationsInUse.size());
        for (AggregationLinkInfo li : aggregationsInUse) {
          os.writeLong(li.getLinkId().getIdValue());
          os.writeString(li.getName());
          os.writeBoolean(li.isUnordered());
          li.setIntIndex(aggregationIndex++);
        }
      }
    }
    os.writeInt(REGISTRY_END);
    return metaInfo;
  }

  private ReadHelper loadRegistry(ModelInputStream is) throws IOException {
    assertSyncToken(is, REGISTRY_START);
    // see #saveRegistry, we use position of an element in persistence as its index
    int langIndex, conceptIndex, propertyIndex, associationIndex, aggregationIndex;
    langIndex = conceptIndex = propertyIndex = associationIndex = aggregationIndex = 0;

    ReadHelper rh = new ReadHelper(myMetaInfoProvider);

    int langCount = is.readShort();
    while (langCount-- > 0) {
      final SLanguageId languageId = new SLanguageId(is.readUUID());
      final String langName = is.readString();
      rh.withLanguage(languageId, langName, langIndex++);
      //
      int conceptCount = is.readShort();
      while (conceptCount-- > 0) {
        final SConceptId conceptId = new SConceptId(languageId, is.readLong());
        final String conceptName = NameUtil.conceptFQNameFromNamespaceAndShortName(langName, is.readString());
        int flags = is.readByte();
        int stubToken = is.readByte();
        final SConceptId stubId;
        if (stubToken == STUB_NONE) {
          stubId = null;
        } else {
          assert stubToken == STUB_ID;
          stubId = new SConceptId(languageId, is.readLong());
        }
        rh.withConcept(conceptId, conceptName, StaticScope.values()[flags & 0x0f], ConceptKind.values()[flags >> 4 & 0x0f], stubId, conceptIndex++);
        //
        int propertyCount = is.readShort();
        while (propertyCount-- > 0) {
          rh.property(new SPropertyId(conceptId, is.readLong()), is.readString(), propertyIndex++);
        }
        //
        int associationCount = is.readShort();
        while (associationCount-- > 0) {
          rh.association(new SReferenceLinkId(conceptId, is.readLong()), is.readString(), associationIndex++);
        }
        //
        int aggregationCount = is.readShort();
        while (aggregationCount-- > 0) {
          rh.aggregation(new SContainmentLinkId(conceptId, is.readLong()), is.readString(), is.readBoolean(), aggregationIndex++);
        }
      }
    }
    assertSyncToken(is, REGISTRY_END);
    return rh;
  }

  private void saveUsedLanguages(ModelOutputStream os) throws IOException {
    Collection<SLanguage> refs = myModelData.usedLanguages();
    os.writeShort(refs.size());
    for (SLanguage l : refs) {
      // id, name, version
      os.writeUUID(IdHelper.getLanguageId(l).getIdValue());
      os.writeString(l.getQualifiedName());
      os.writeInt(l.getLanguageVersion());
    }
  }

  private void loadUsedLanguages(ModelInputStream is) throws IOException {
    int size = is.readShort();
    for (int i = 0; i < size; i++) {
      SLanguageId id = new SLanguageId(is.readUUID());
      String name = is.readString();
      int version = is.readInt();
      SLanguage l = MetaAdapterFactory.getLanguage(id, name, version);
      myModelData.addLanguage(l);
      myMetaInfoProvider.setLanguageName(id, name);
    }
  }

  private static void saveModuleRefList(Collection<SModuleReference> refs, ModelOutputStream os) throws IOException {
    os.writeShort(refs.size());
    for (SModuleReference ref : refs) {
      os.writeModuleReference(ref);
    }
  }

  private static Collection<SModuleReference> loadModuleRefList(ModelInputStream is) throws IOException {
    int size = is.readShort();
    List<SModuleReference> result = new ArrayList<SModuleReference>(size);
    for (int i = 0; i < size; i++) {
      result.add(is.readModuleReference());
    }
    return result;
  }

  private static void saveImports(Collection<ImportElement> elements, ModelOutputStream os) throws IOException {
    os.writeInt(elements.size());
    for (ImportElement element : elements) {
      os.writeModelReference(element.getModelReference());
      os.writeInt(element.getUsedVersion());
    }
  }

  private static List<ImportElement> loadImports(ModelInputStream is) throws IOException {
    int size = is.readInt();
    List<ImportElement> result = new ArrayList<ImportElement>();
    for (int i = 0; i < size; i++) {
      SModelReference ref = is.readModelReference();
      result.add(new ImportElement(ref, -1, is.readInt()));
    }
    return result;
  }

  public static void index(InputStream content, final Callback consumer) throws IOException {
    ModelInputStream mis = null;
    try {
      mis = new ModelInputStream(content);
      SModelHeader modelHeader = loadHeader(mis);
      SModel model = new DefaultSModel(modelHeader.getModelReference(), modelHeader);
      BinaryPersistence bp = new BinaryPersistence(new StuffedMetaModelInfo(new BaseMetaModelInfo()), model);
      final ReadHelper readHelper = bp.loadModelProperties(mis);
      for (ImportElement element : model.importedModels()) {
        consumer.imports(element.getModelReference());
      }
      for (SConceptId cid : readHelper.getParticipatingConcepts()) {
        consumer.instances(cid);
      }
      readHelper.requestInterfaceOnly(false);
      final NodesReader reader = new NodesReader(modelHeader.getModelReference(), mis, readHelper);
      HashSet<SNodeId> externalNodes = new HashSet<SNodeId>();
      HashSet<SNodeId> localNodes = new HashSet<SNodeId>();
      reader.collectExternalTargets(externalNodes);
      reader.collectLocalTargets(localNodes);
      reader.readChildren(null);
      for (SNodeId n : externalNodes) {
        consumer.externalNodeRef(n);
      }
      for (SNodeId n : localNodes) {
        consumer.localNodeRef(n);
      }
    } finally {
      FileUtil.closeFileSafe(mis);
    }
  }

  private static void assertSyncToken(ModelInputStream is, int token) throws IOException {
    if (is.readInt() != token) {
      throw new IOException("bad stream, no sync token");
    }
  }
}
