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
package jetbrains.mps.project;

import jetbrains.mps.extapi.module.EditableSModule;
import jetbrains.mps.extapi.module.ModuleFacetBase;
import jetbrains.mps.extapi.module.SModuleBase;
import jetbrains.mps.extapi.persistence.ModelRootBase;
import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.module.SDependencyImpl;
import jetbrains.mps.persistence.MementoImpl;
import jetbrains.mps.persistence.PersistenceRegistry;
import jetbrains.mps.project.dependency.GlobalModuleDependenciesManager;
import jetbrains.mps.project.dependency.GlobalModuleDependenciesManager.Deptype;
import jetbrains.mps.project.facets.JavaModuleFacet;
import jetbrains.mps.project.facets.TestsFacet;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.Dependency;
import jetbrains.mps.project.structure.modules.DeploymentDescriptor;
import jetbrains.mps.project.structure.modules.ModuleDescriptor;
import jetbrains.mps.project.structure.modules.ModuleFacetDescriptor;
import jetbrains.mps.smodel.BootstrapLanguages;
import jetbrains.mps.smodel.DefaultScope;
import jetbrains.mps.smodel.Generator;
import jetbrains.mps.smodel.Language;
import jetbrains.mps.smodel.MPSModuleOwner;
import jetbrains.mps.smodel.MPSModuleRepository;
import jetbrains.mps.smodel.ModuleRepositoryFacade;
import jetbrains.mps.smodel.SLanguageHierarchy;
import jetbrains.mps.smodel.SModelInternal;
import jetbrains.mps.smodel.SModelRepository;
import jetbrains.mps.smodel.SuspiciousModelHandler;
import jetbrains.mps.util.EqualUtil;
import jetbrains.mps.util.FileUtil;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.util.MacrosFactory;
import jetbrains.mps.util.PathManager;
import jetbrains.mps.util.annotation.ToRemove;
import jetbrains.mps.util.iterable.TranslatingIterator;
import jetbrains.mps.vfs.FileSystem;
import jetbrains.mps.vfs.FileSystemListener;
import jetbrains.mps.vfs.IFile;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.model.EditableSModel;
import org.jetbrains.mps.openapi.model.SModel;
import org.jetbrains.mps.openapi.model.SModelId;
import org.jetbrains.mps.openapi.module.FacetsFacade;
import org.jetbrains.mps.openapi.module.SDependency;
import org.jetbrains.mps.openapi.module.SDependencyScope;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.module.SModuleFacet;
import org.jetbrains.mps.openapi.module.SModuleId;
import org.jetbrains.mps.openapi.module.SModuleReference;
import org.jetbrains.mps.openapi.module.SRepository;
import org.jetbrains.mps.openapi.module.SearchScope;
import org.jetbrains.mps.openapi.persistence.Memento;
import org.jetbrains.mps.openapi.persistence.ModelRoot;
import org.jetbrains.mps.openapi.persistence.ModelRootFactory;
import org.jetbrains.mps.openapi.persistence.PersistenceFacade;
import org.jetbrains.mps.openapi.util.ProgressMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.mps.openapi.module.FacetsFacade.FacetFactory;

public abstract class AbstractModule extends SModuleBase implements EditableSModule, FileSystemListener {
  private static final Logger LOG = LogManager.getLogger(AbstractModule.class);

  public static final String MODULE_DIR = "module";
  public static final String CLASSES_GEN = "classes_gen";
  public static final String CLASSES = "classes";

  @Nullable
  protected final IFile myDescriptorFile;
  private SModuleReference myModuleReference;
  private Set<ModelRoot> mySModelRoots = new LinkedHashSet<ModelRoot>();
  private Set<ModuleFacetBase> myFacets = new LinkedHashSet<ModuleFacetBase>();
  private ModuleScope myScope = new ModuleScope();

  protected boolean myChanged = false;

  //----model creation

  protected AbstractModule() {
    this(null);
  }

  protected AbstractModule(@Nullable IFile myDescriptorFile) {
    this.myDescriptorFile = myDescriptorFile;
  }

  //----reference
  @Override
  public SModuleId getModuleId() {
//    assertCanRead(); @see getModuleReference()
    return getModuleReference().getModuleId();
  }

  @Override
  public String getModuleName() {
//    assertCanRead(); @see getModuleReference()
    return getModuleReference().getModuleName();
  }

  @Override
  public Iterable<SDependency> getDeclaredDependencies() {
    assertCanRead();
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) {
      return Collections.emptyList();
    }
    HashSet<SDependency> result = new HashSet<SDependency>();
    final SRepository repo = getRepository();
    if (repo == null) {
      throw new IllegalStateException("It is not possible to resolve all declared dependencies with a null repository : module " + this);
    }

    // add declared dependencies
    for (Dependency d : descriptor.getDependencies()) {
      result.add(new SDependencyImpl(d.getModuleRef(), repo, d.getScope(), d.isReexport()));
    }

    // add dependencies provided by devkits as nonreexport dependencies
    for (SModuleReference usedDevkit : descriptor.getUsedDevkits()) {
      final SModule devkit = usedDevkit.resolve(repo);
      if (DevKit.class.isInstance(devkit)) {
        for (Solution solution : ((DevKit) devkit).getAllExportedSolutions()) {
          result.add(new SDependencyImpl(solution.getModuleReference(), repo, SDependencyScope.DEFAULT, false));
        }
      }
    }
    return result;
  }


  /**
   * @deprecated it's just a short-hand for <code>new SLanguageHierarchy(getUsedLanguages())</code>, it's hardly a justification for a cast to AbstractModule
   */
  @Deprecated
  @ToRemove(version = 3.3)
  public Set<SLanguage> getAllUsedLanguages() {
    return new SLanguageHierarchy(getUsedLanguages()).getExtended();
  }

  @Override
  public Set<SLanguage> getUsedLanguages() {
    assertCanRead();

    LinkedHashSet<SLanguage> usedLanguages = new LinkedHashSet<SLanguage>();
    LinkedHashSet<SModuleReference> devkits = new LinkedHashSet<SModuleReference>();
    for (SModel m : getModels()) {
      final SModelInternal modelInternal = (SModelInternal) m;
      usedLanguages.addAll(modelInternal.importedLanguageIds());
      devkits.addAll(modelInternal.importedDevkits());
    }
    final SRepository repository = getRepository();
    if (repository != null) {
      for (SModuleReference devkitRef : devkits) {
        final SModule module = devkitRef.resolve(repository);
        if (module instanceof DevKit) {
          for (SLanguage l : ((DevKit) module).getAllExportedLanguageIds()) {
            usedLanguages.add(l);
          }
        }
      }
    }
    usedLanguages.add(BootstrapLanguages.getLangCore());

    return usedLanguages;
  }

  @Override
  public SModel resolveInDependencies(SModelId ref) {
    assertCanRead();
    SModel rv = getModel(ref);
    if (rv != null) {
      return rv;
    }
    for (SModule visibleModule : new GlobalModuleDependenciesManager(this).getModules(Deptype.VISIBLE)) {
      rv = visibleModule.getModel(ref);
      if (rv != null) {
        return rv;
      }
    }
//    TODO: Work in progress. At the moment, there are two cases I'm aware of, that
//    doesn't allow us to return null here (i.e. not to go to global registry):
//    1) Use of java stub counterparts where their original (source) module is in dependencies.
//       E.g. closures.runtime and its trick to use java stubs instead of plain node references
//    2) References to accessory model of used language. E.g. mps.build generator uses mps.wf language,
//       which exposes fw.preset models as its accessories. These are not deemed visible by GMDM
//    Uncomment next warning to see these.
//    LOG.warn(String.format("Failed to resolve %s in module %s", ref, getModuleName()));
    return SModelRepository.getInstance().getModelDescriptor(ref);
  }

  protected void setModuleReference(@NotNull SModuleReference reference) {
    assertCanChange();

    assert reference.getModuleId() != null : "module must have an id";
    assert myModuleReference == null || reference.getModuleId().equals(myModuleReference.getModuleId()) : "module id can't be changed";

    SModuleReference oldValue = myModuleReference;
    myModuleReference = reference;
    if (oldValue != null &&
        oldValue.getModuleName() != null &&
        !oldValue.getModuleName().equals(myModuleReference.getModuleName())) {

      MPSModuleRepository.getInstance().moduleFqNameChanged(this, oldValue.getModuleName());
    }
  }

  @Override
  @NotNull
  //module reference is immutable, so we cn return original
  public SModuleReference getModuleReference() {
//    assertCanRead(); ClassLoaderManager needs module reference. Do we need CLM to obtain read lock?
    return myModuleReference;
  }

  //----save

  //todo move to EditableModule class
  @Nullable
  public ModuleDescriptor getModuleDescriptor() {
    assertCanRead();

    return null;
  }

  //todo should be replaced with events
  public final void setModuleDescriptor(ModuleDescriptor moduleDescriptor) {
    assertCanChange();
    doSetModuleDescriptor(moduleDescriptor);
    setChanged();
    reloadAfterDescriptorChange();
    fireChanged();
    dependenciesChanged();
  }

  // no notifications are sent
  protected void doSetModuleDescriptor(ModuleDescriptor moduleDescriptor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setChanged() {
    assertCanChange();
    myChanged = true;
  }

  @Override
  public void save() {
    assertCanChange();
    validateLanguageVersions();
    myChanged = false;
  }

  //----adding different deps

  @Nullable
  public Dependency addDependency(@NotNull SModuleReference moduleRef, boolean reexport) {
    assertCanChange();
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) return null;
    for (Dependency dep : descriptor.getDependencies()) {
      if (!EqualUtil.equals(dep.getModuleRef(), moduleRef)) continue;

      if (reexport && !dep.isReexport()) {
        dep.setReexport(true);
        dependenciesChanged();
        setChanged();
      }
      return dep;
    }

    Dependency dep = new Dependency();
    dep.setModuleRef(moduleRef);
    dep.setReexport(reexport);
    descriptor.getDependencies().add(dep);

    dependenciesChanged();
    setChanged();
    return dep;
  }

  public void removeDependency(@NotNull Dependency dependency) {
    assertCanChange();
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) return;
    if (!descriptor.getDependencies().contains(dependency)) return;

    descriptor.getDependencies().remove(dependency);

    dependenciesChanged();
    setChanged();
  }

  /**
   * @deprecated set of used languages for a module is derived from used languages of owned models
   */
  @Deprecated
  @ToRemove(version = 3.3)
  public void addUsedLanguage(SModuleReference langRef) {
    // no-op
  }

  /**
   * @deprecated set of used language for a module is derived from used languages of owned models
   */
  @Deprecated
  @ToRemove(version = 3.3)
  public void removeUsedLanguage(SModuleReference langRef) {
    // no-op
  }

  /**
   * @deprecated set of used language for a module is derived from used languages of owned models
   */
  @Deprecated
  @ToRemove(version = 3.3)
  public void addUsedDevkit(SModuleReference devkitRef) {
    // no-op
  }

  /**
   * @deprecated set of used language for a module is derived from used languages of owned models
   */
  @Deprecated
  @ToRemove(version = 3.3)
  public void removeUsedDevkit(SModuleReference devkitRef) {
    // no-op
  }

  //----languages & devkits

  /**
   * @deprecated shall be removed once tests in MPS plugin got fixed (FacetTests.testAddRemoveUsedLanguage(), testFacetInitialized()
   */
  @Deprecated
  public final Collection<SModuleReference> getUsedLanguagesReferences() {
    assertCanRead();
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) return Collections.emptySet();
    return Collections.unmodifiableCollection(descriptor.getUsedLanguages());
  }

  //----stubs

  // FIXME: MPS-19756
  // TODO: get rid of this code - generate the deployment descriptor during build process
  protected void updatePackagedDescriptor() {
    // things to do:
    // 1) load/prepare stub libraries (getAdditionalJavaStubPaths) from sources descriptor
    // 2) load/prepare stub model roots from sources descriptor
    // 3) load libraries from deployment descriptor (/classes_gen ?)

    // possible cases:
    // 1) without deployment descriptor (nothing to do; todo: ?)
    // 2) with deployment descriptor, without sources (to do: 3)
    // 3) with deployment descriptor, with sources (to do: 1,2,3)

    if (!isPackaged()) return;

    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) return;
    DeploymentDescriptor deplDescriptor = descriptor.getDeploymentDescriptor();
    if (deplDescriptor == null) return;

    final IFile bundleHomeFile = FileSystem.getInstance().getBundleHome(getDescriptorFile());
    if (bundleHomeFile == null) return;

    IFile bundleParent = bundleHomeFile.getParent();
    if (bundleParent == null || !bundleParent.exists()) return;

    IFile sourcesDescriptorFile = ModulesMiner.getRealDescriptorFile(getDescriptorFile().getPath(), deplDescriptor);
    if (sourcesDescriptorFile == null) {
      // todo: for now it's impossible
      assert descriptor instanceof DeploymentDescriptor;
    } else {
      assert !(descriptor instanceof DeploymentDescriptor);
    }

    // 1 && 2
    if (sourcesDescriptorFile != null) {
      // stub libraries
      // todo: looks like module.xml contains info about model libs
      // ignore stub libraries from source module descriptor, use libs from DeploymentDescriptor
      descriptor.getAdditionalJavaStubPaths().clear();

      // stub model roots
      List<ModelRootDescriptor> toRemove = new ArrayList<ModelRootDescriptor>();
      List<ModelRootDescriptor> toAdd = new ArrayList<ModelRootDescriptor>();
      for (ModelRootDescriptor rootDescriptor : descriptor.getModelRootDescriptors()) {
        String rootDescriptorType = rootDescriptor.getType();
        if (rootDescriptorType.equals(PersistenceRegistry.JAVA_CLASSES_ROOT)) {
          // trying to load old format from deployment descriptor
          String pathElement = rootDescriptor.getMemento().get("path");
          boolean update = false;
          Memento newMemento = new MementoImpl();
          if (pathElement != null) {
            // See JavaSourceStubModelRoot & JavaClassStubsModelRoot load methods need to replace with super
            String convertedPath = convertPath(pathElement, bundleHomeFile, sourcesDescriptorFile, descriptor);

            if (convertedPath != null) {
              newMemento.put("path", convertedPath);
              update = true;
            }
          } else {
            // trying to load new format : replacing paths like **.jar!/module ->
            String contentPath = rootDescriptor.getMemento().get("contentPath");
            List<String> paths = new LinkedList<String>();
            for (Memento sourceRoot : rootDescriptor.getMemento().getChildren("sourceRoot")) {
              paths.add(contentPath + File.separator + sourceRoot.get("location"));
            }
            newMemento.put("contentPath", bundleParent.getPath());
            Memento newMementoChild = newMemento.createChild("sourceRoot");
            for (String path : paths) {
              String convertedPath = convertPath(path, bundleHomeFile, sourcesDescriptorFile, descriptor);
              if (convertedPath != null) {
                newMementoChild.put("location", convertedPath.replace(newMemento.get("contentPath"), ""));
                update = true;
              }
            }
          }
          if (update) toAdd.add(new ModelRootDescriptor(rootDescriptorType, newMemento));
          toRemove.add(rootDescriptor);
        }
      }
      descriptor.getModelRootDescriptors().removeAll(toRemove);
      descriptor.getModelRootDescriptors().addAll(toAdd);
    }

    // 3
    for (String jarFile : deplDescriptor.getLibraries()) {
      IFile jar = jarFile.startsWith("/")
          ? FileSystem.getInstance().getFileByPath(PathManager.getHomePath() + jarFile)
          : bundleParent.getDescendant(jarFile);
      if (jar.exists()) {
        String path = jar.getPath();
        descriptor.getAdditionalJavaStubPaths().add(path);
        descriptor.getModelRootDescriptors().add(ModelRootDescriptor.getJavaStubsModelRoot(path));
      }
    }
  }

  /**
   * Convert path from sources module descriptor for using on distribution
   * /classes && /classes_gen converts to bundle home path
   *
   * @param originalPath Original path from sources module descriptor
   * @return Converted path, null if path meaningless on packaged module
   */
  @Nullable
  private String convertPath(String originalPath, IFile bundleHome, IFile sourcesDescriptorFile, ModuleDescriptor descriptor) {
    MacroHelper macroHelper = MacrosFactory.forModuleFile(sourcesDescriptorFile);

    String canonicalPath = FileUtil.getCanonicalPath(originalPath).toLowerCase();

    // /classes && /classes_gen hack
    String suffix = descriptor.getCompileInMPS() ? CLASSES_GEN : CLASSES;
    if (canonicalPath.endsWith(suffix)) {
      // MacrosFactory based on original descriptor file because we use original descriptor file for ModuleDescriptor reading, so all paths expanded to original descriptor file
      String classes = macroHelper.expandPath("${module}/" + suffix);
      if (FileUtil.getCanonicalPath(classes).equalsIgnoreCase(canonicalPath)) {
        return bundleHome.getPath();
      }
    } else if (FileUtil.getCanonicalPath(bundleHome.getPath()).equalsIgnoreCase(canonicalPath)) {
      return bundleHome.getPath();
    }

    // ${mps_home}/lib
    String mpsHomeLibPath = FileUtil.getCanonicalPath(PathManager.getHomePath() + File.separator + "lib").toLowerCase();
    if (canonicalPath.startsWith(mpsHomeLibPath)) {
      return canonicalPath;
    }

    if (MacrosFactory.containsNonMPSMacros(macroHelper.shrinkPath(originalPath))) {
      return originalPath;
    } else {
      // ignore paths starts from ${module}/${project} etc
      return null;
    }
  }


//----

  @Override
  public Iterable<ModelRoot> getModelRoots() {
    // We check read lock here because mySModelRoots is updated inside write.
    assertCanRead();
    return Collections.unmodifiableCollection(mySModelRoots);
  }

  protected void reloadAfterDescriptorChange() {
    initFacetsAndModels();
  }

  private void initFacetsAndModels() {
    updatePackagedDescriptor();
    updateFacets();
    updateModelsSet();
  }

  protected void collectFacetTypes(Set<String> types) {
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) {
      return;
    }

    types.addAll(FacetsFacade.getInstance().getApplicableFacetTypes(
        new TranslatingIterator<SModuleReference, String>(descriptor.getUsedLanguages().iterator()) {
          @Override
          protected String translate(SModuleReference node) {
            return node.getModuleName();
          }
        }));

    types.add(JavaModuleFacet.FACET_TYPE);
  }

  protected ModuleFacetBase setupFacet(ModuleFacetBase facet, Memento memento) {
    if (!facet.setModule(this)) {
      return null;
    }
    facet.load(memento != null ? memento : new MementoImpl());
    facet.attach();
    return facet;
  }

  protected void updateFacets() {
    assertCanChange();

    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) {
      return;
    }

    for (ModuleFacetBase facet : myFacets) {
      facet.dispose();
    }
    myFacets.clear();

    Map<String, Memento> config = new HashMap<String, Memento>();
    for (ModuleFacetDescriptor facetDescriptors : descriptor.getModuleFacetDescriptors()) {
      config.put(facetDescriptors.getType(), facetDescriptors.getMemento());
    }

    Set<String> types = new HashSet<String>();
    collectFacetTypes(types);
    types.addAll(config.keySet());

    for (String facetType : types) {
      FacetFactory factory = FacetsFacade.getInstance().getFacetFactory(facetType);
      if (factory == null) {
        LOG.error("no registered factory for a facet with type=`" + facetType + "'");
        continue;
      }
      SModuleFacet newFacet = factory.create();
      if (!(newFacet instanceof ModuleFacetBase)) {
        LOG.error("broken facet factory: " + factory.getClass().getName());
        continue;
      }

      ModuleFacetBase facet = (ModuleFacetBase) newFacet;
      Memento m = config.get(facetType);
      facet = setupFacet(facet, m);
      if (facet != null) {
        myFacets.add(facet);
      }
    }
  }

  public void onModuleLoad() {
    updateSModelReferences();
    updateModuleReferences();
  }

  @Override
  public boolean isReadOnly() {
//    assertCanRead(); getModuleSourceDir() doesn't require read, why isPackaged() does?
    return isPackaged();
  }

  @Override
  public boolean isPackaged() {
//    assertCanRead(); getModuleSourceDir() doesn't require read, why isPackaged() does?
    return getModuleSourceDir() == null || FileSystem.getInstance().isPackaged(getModuleSourceDir());
  }

  /**
   * Module sources folder
   * In case of working on sources == dir with module descriptor
   * In case of working on distribution = {module-name}-src.jar/module/
   * In case of Generator = sourceLanguage.getModuleSourceDir()
   * ${module} expands to this method
   */
  public IFile getModuleSourceDir() {
    return myDescriptorFile != null ? myDescriptorFile.getParent() : null;
  }

  public IFile getDescriptorFile() {
//    assertCanRead();   if getModuleSourceDir doesn't require read, why getDescriptorFile does?
    return myDescriptorFile;
  }

  public void rename(String newName) {
    renameModels(getModuleName(), newName);

    //see MPS-18743, need to save before setting descriptor
    getRepository().saveAll();

    ModuleDescriptor descriptor = getModuleDescriptor();
    if (myDescriptorFile != null) {
      myDescriptorFile.rename(newName + "." + FileUtil.getExtension(myDescriptorFile.getName()));
    }

    descriptor.setNamespace(newName);
    setModuleDescriptor(descriptor);
  }

  protected void renameModels(String oldName, String newName) {
    //if module name is a prefix of it's model's name - rename the model, too
    for (SModel m : getModels()) {
      if (m.isReadOnly()) continue;
      if (!m.getModelName().startsWith(oldName + ".")) continue;
      if (!(m instanceof EditableSModel)) continue;

      ((EditableSModel) m).rename(newName + m.getModelName().substring(getModuleName().length()), true);
    }
  }

  @NotNull
  public SearchScope getScope() {
    assertCanRead();
    return myScope;
  }

  @Override
  public void attach(@NotNull SRepository repository) {
    super.attach(repository);
    if (myDescriptorFile != null) {
      FileSystem.getInstance().addListener(this);
    }
    initFacetsAndModels();
  }

  @Nullable
  @Override
  public IFile getFileToListen() {
    return myDescriptorFile;
  }

  @Override
  public Iterable<FileSystemListener> getListenerDependencies() {
    List<FileSystemListener> listeners = new ArrayList<FileSystemListener>();
    for (MPSModuleOwner owner : MPSModuleRepository.getInstance().getOwners(this)) {
      if (owner instanceof FileSystemListener) {
        listeners.add((FileSystemListener) owner);
      }
    }
    return listeners.isEmpty() ? null : listeners;
  }

  @Override
  public void update(ProgressMonitor monitor, FileSystemEvent event) {
    assertCanChange();
    for (IFile file : event.getRemoved()) {
      if (file.equals(myDescriptorFile)) {
        ModuleRepositoryFacade.getInstance().removeModuleForced(this);
        return;
      }
    }
    for (IFile file : event.getChanged()) {
      if (file.equals(myDescriptorFile)) {
        SModuleOperations.reloadFromDisk(this);
        return;
      }
    }
  }

  @Override
  public String toString() {
    String namespace = getModuleName();
    return namespace + " [module]";
  }

  /**
   * @deprecated use {@link #getModuleName}
   */
  @Deprecated
  public String getName() {
    return getModuleName();
  }

  @Override
  public void dispose() {
    assertCanChange();
    LOG.trace("Disposing the module " + this);
    FileSystem.getInstance().removeListener(this);
    for (ModuleFacetBase f : myFacets) {
      f.dispose();
    }
    myFacets.clear();
    for (ModelRoot m : mySModelRoots) {
      ((ModelRootBase) m).dispose();
    }
    mySModelRoots.clear();
    super.dispose();
  }

  public List<String> getSourcePaths() {
    assertCanRead();
    return new ArrayList<String>(SModuleOperations.getAllSourcePaths(this));
  }

  public void updateModelsSet() {
    doUpdateModelsSet();
  }

  protected Iterable<ModelRoot> loadRoots() {
    ModuleDescriptor descriptor = getModuleDescriptor();
    if (descriptor == null) {
      return Collections.emptyList();
    }

    List<ModelRoot> result = new ArrayList<ModelRoot>();
    for (ModelRootDescriptor modelRoot : descriptor.getModelRootDescriptors()) {
      try {
        ModelRootFactory modelRootFactory = PersistenceFacade.getInstance().getModelRootFactory(modelRoot.getType());
        if (modelRootFactory == null) {
          LOG.error("Unknown model root type: `" + modelRoot.getType() + "'. Requested by: " + this);
          continue;
        }

        ModelRoot root = modelRootFactory.create();
        root.load(modelRoot.getMemento());
        result.add(root);
      } catch (Exception e) {
        LOG.error("Error loading models from root with type: `" + modelRoot.getType() + "'. Requested by: " + this, e);
      }
    }
    return result;
  }

  private void doUpdateModelsSet() {
    assertCanChange();

    for (SModel model : getModels()) {
      if (model instanceof EditableSModel && ((EditableSModel) model).isChanged()) {
        LOG.error(
            "Trying to reload module " + getModuleName() + " which contains a non-saved model" +
                model.getModelName() + "To prevent data loss, MPS will not update models in this module. " +
                "Please save your work and restart MPS. See MPS-18743 for details."
        );
        return;
      }
    }

    Set<ModelRoot> toRemove = new HashSet<ModelRoot>(mySModelRoots);
    Set<ModelRoot> toUpdate = new HashSet<ModelRoot>(mySModelRoots);
    Set<ModelRoot> toAttach = new HashSet<ModelRoot>();

    for (ModelRoot root : loadRoots()) {
      try {
        if (mySModelRoots.contains(root)) {
          toRemove.remove(root);
        } else {
          toAttach.add(root);
        }
      } catch (Exception e) {
        LOG.error("Error loading models from root `" + root.getPresentation() + "'. Requested by: " + this, e);
      }
    }
    toUpdate.removeAll(toRemove);

    for (ModelRoot modelRoot : toRemove) {
      ((ModelRootBase) modelRoot).dispose();
    }
    mySModelRoots.removeAll(toRemove);
    for (ModelRoot modelRoot : toAttach) {
      ModelRootBase rootBase = (ModelRootBase) modelRoot;
      rootBase.setModule(this);
      mySModelRoots.add(modelRoot);
      rootBase.attach();
    }
    for (ModelRoot modelRoot : toUpdate) {
      ((ModelRootBase) modelRoot).update();
    }
  }

  public static void handleReadProblem(AbstractModule module, Exception e, boolean isInConflict) {
    SuspiciousModelHandler.getHandler().handleSuspiciousModule(module, isInConflict);
    LOG.error(e.getMessage());
    e.printStackTrace();
  }

  public void updateSModelReferences() {
    ModuleDescriptor moduleDescriptor = getModuleDescriptor();
    if (moduleDescriptor == null) return;
    if (moduleDescriptor.updateModelRefs()) {
      setChanged();
    }
  }

  public void updateModuleReferences() {
    ModuleDescriptor moduleDescriptor = getModuleDescriptor();
    if (moduleDescriptor == null) return;
    if (moduleDescriptor.updateModuleRefs()) {
      setChanged();
    }
  }

  protected void dependenciesChanged() {
    // todo: review all usages after migration!

    // callback on dependencies (any of them) changed event
    // you can override this method with some invalidation action
    // call super.dependenciesChanged() at the end

    // todo: as we haven't dependencies listeners...

    myScope.invalidateCaches();
  }

  protected ModuleDescriptor loadDescriptor() {
    return null;
  }

  @Override
  public boolean isChanged() {
    return myChanged;
  }

  @Nullable
  @Override
  public <T extends SModuleFacet> T getFacet(Class<T> clazz) {
    for (SModuleFacet facet : getFacets()) {
      if (clazz.isInstance(facet)) {
        return (T) facet;
      }
    }
    return null;
  }

  @Override
  public Iterable<SModuleFacet> getFacets() {
    return Collections.<SModuleFacet>unmodifiableSet(myFacets);
  }

  public class ModuleScope extends DefaultScope {
    protected ModuleScope() {
    }

    public AbstractModule getModule() {
      return AbstractModule.this;
    }

    @Override
    protected Set<SModule> getInitialModules() {
      Set<SModule> result = new HashSet<SModule>();
      result.add(AbstractModule.this);
      return result;
    }

    @Override
    protected Set<Language> getInitialUsedLanguages() {
      HashSet<Language> result = new HashSet<Language>();
      for (SLanguage l : AbstractModule.this.getUsedLanguages()) {
        SModule langModule = l.getSourceModule();
        if (langModule instanceof Language) {
          result.add((Language) langModule);
        }
      }
      if (AbstractModule.this instanceof Language) {
        result.add((Language) AbstractModule.this);
        // XXX why Language(SModule)#getUsedLanguages doesn't care about descriptor language being used?
        result.add(ModuleRepositoryFacade.getInstance().getModule(BootstrapLanguages.descriptorLanguageRef(), Language.class));
      }
      if (AbstractModule.this instanceof Generator) {
        result.add(((Generator) AbstractModule.this).getSourceLanguage());
      }
      return result;
    }

    public String toString() {
      return "Scope of module " + AbstractModule.this;
    }
  }

  public IFile getOutputPath() {
    return ProjectPathUtil.getGeneratorOutputPath(getModuleSourceDir(), getModuleDescriptor());
  }

  @Deprecated
  public final String getGeneratorOutputPath() {
    IFile outputPath = getOutputPath();
    return outputPath != null ? outputPath.getPath() : null;
  }

  @Deprecated
  public final String getTestsGeneratorOutputPath() {
    TestsFacet testsFacet = this.getFacet(TestsFacet.class);
    if (testsFacet == null) {
      return null;
    }
    IFile testsOutputPath = testsFacet.getTestsOutputPath();
    if (testsOutputPath == null) {
      return null;
    }
    return testsOutputPath.getPath();
  }

  public void validateLanguageVersions() {
    assertCanChange();
    ModuleDescriptor md = getModuleDescriptor();
    Map<SLanguage, Integer> oldLanguageVersions = md.getLanguageVersions();
    Map<SLanguage, Integer> newLanguageVersions = new HashMap<SLanguage, Integer>();
    if (!md.hasLanguageVersions()) {
      for (SLanguage lang : getAllUsedLanguages()) {
        newLanguageVersions.put(lang, 0);
      }
      md.setHasLanguageVersions(true);
    } else {
      for (SLanguage lang : getAllUsedLanguages()) {
        if (oldLanguageVersions.containsKey(lang)) {
          newLanguageVersions.put(lang, oldLanguageVersions.get(lang));
        } else {
          newLanguageVersions.put(lang, lang.getLanguageVersion());
        }
      }
    }
    oldLanguageVersions.clear();
    oldLanguageVersions.putAll(newLanguageVersions);
  }

  @Override
  public int getUsedLanguageVersion(SLanguage usedLanguage) {
    Integer res = getModuleDescriptor().getLanguageVersions().get(usedLanguage);
    if (res == null) {
      LOG.error(
          "getUsedLanguageVersion can't find a version for language " + usedLanguage.getQualifiedName() +
              " in module " + getModuleName() + "." +
              " This can either mean that the language is not imported into this module or that " +
              "validateLanguageVersions was not called on this module in appropriate moment.",
          new Throwable());
      return usedLanguage.getLanguageVersion();
    }
    return res;
  }
}
