/*
 * Copyright 2003-2014 JetBrains s.r.o.
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
package jetbrains.mps.smodel.language;

import jetbrains.mps.classloading.ClassLoaderManager;
import jetbrains.mps.classloading.MPSClassesListener;
import jetbrains.mps.components.CoreComponent;
import jetbrains.mps.module.ReloadableModuleBase;
import jetbrains.mps.smodel.Generator;
import jetbrains.mps.smodel.Language;
import jetbrains.mps.smodel.adapter.ids.MetaIdByDeclaration;
import jetbrains.mps.smodel.adapter.ids.SLanguageId;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import jetbrains.mps.smodel.adapter.structure.language.SLanguageAdapter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.module.SRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.format;

/**
 * evgeny, 3/11/11
 */
public class LanguageRegistry implements CoreComponent, MPSClassesListener {
  private static final Logger LOG = LogManager.getLogger(LanguageRegistry.class);

  private static LanguageRegistry INSTANCE;

  public static LanguageRegistry getInstance() {
    return INSTANCE;
  }

  private Map<String, LanguageRuntime> myLanguages = new HashMap<String, LanguageRuntime>();
  private Map<SLanguageId, LanguageRuntime> myLanguagesById = new HashMap<SLanguageId, LanguageRuntime>();

  private final List<LanguageRegistryListener> myLanguageListeners = new CopyOnWriteArrayList<LanguageRegistryListener>();

  private final SRepository myRepository;
  private final ClassLoaderManager myClassLoaderManager;

  public LanguageRegistry(SRepository repository, ClassLoaderManager loaderManager) {
    myRepository = repository;
    myClassLoaderManager = loaderManager;
  }

  @Override
  public void init() {
    if (INSTANCE != null) {
      throw new IllegalStateException("double initialization");
    }
    INSTANCE = this;
    myClassLoaderManager.addClassesHandler(this);
  }

  @Override
  public void dispose() {
    myRepository.getModelAccess().runWriteAction(new Runnable() {
      @Override
      public void run() {
        notifyUnload(myLanguages.values());
        myLanguages.clear();
        myLanguagesById.clear();
      }
    });
    myClassLoaderManager.removeClassesHandler(this);
    INSTANCE = null;
  }

  private void notifyUnload(final Collection<LanguageRuntime> languages) {
    if (languages.isEmpty()) return;

    for (LanguageRegistryListener l : myLanguageListeners) {
      try {
        l.beforeLanguagesUnloaded(languages);
      } catch (Exception ex) {
        LOG.error(format("Exception on language unloading; languages: %s; listener: %s", languages, l), ex);
      }
    }
  }

  private void notifyLoad(final Collection<LanguageRuntime> languages) {
    if (languages.isEmpty()) return;

    for (LanguageRegistryListener l : myLanguageListeners) {
      try {
        l.afterLanguagesLoaded(languages);
      } catch (Exception ex) {
        LOG.error(format("Exception on language loading; languages: %s; listener: %s", languages, l), ex);
      }
    }
  }

  @Nullable
  private static LanguageRuntime createRuntime(Language l) {
    final String rtClassName = l.getModuleName() + ".Language";
    // Here, we consider few cases:
    // (a) there's no LR class
    // (b) there's legacy LR class (if we did changes to LR this release)
    // (c) LR in accordance with actual MPS version
    // Both (b) and (c) may fail during class-loading, which we treat as invalid language, although
    // for legacy versions and careless class evolution we might face otherwise valid languages which
    // fail to load due to class validation errors.
    // We aim to support binary compatibility between any two subsequent releases, thus failures for (b)
    // shall serve as an indicator we failed to maintain binary compatibility between releases
    try {
      final Class<?> rtClass = l.getOwnClass(rtClassName);
      if (rtClass != null && LanguageRuntime.class.isAssignableFrom(rtClass)) {
        return ((Class<LanguageRuntime>) rtClass).newInstance();
      }
      return new InterpretedLanguageRuntime(l);
    } catch (ClassNotFoundException ex) {
      return new InterpretedLanguageRuntime(l);
    } catch (InstantiationException e) {
      LOG.error(String.format("Failed to load language %s", l.getModuleName()), e);
      return null;
    } catch (IllegalAccessException e) {
      LOG.error(String.format("Failed to load language %s", l.getModuleName()), e);
      return null;
    }
  }

  public String toString() {
    return "LanguageRegistry";
  }

  public void addRegistryListener(LanguageRegistryListener listener) {
    myLanguageListeners.add(listener);
  }

  public void removeRegistryListener(LanguageRegistryListener listener) {
    myLanguageListeners.remove(listener);
  }

  /*
   *   Collection is valid until the end of the current read action.
   */
  public Collection<LanguageRuntime> getAvailableLanguages() {
    myRepository.getModelAccess().checkReadAccess();
    return myLanguages.values();
  }

  public Collection<SLanguage> getAllLanguages() {
    final Collection<LanguageRuntime> languages = getAvailableLanguages();
    ArrayList<SLanguage> rv = new ArrayList<SLanguage>(languages.size());
    for (LanguageRuntime lr : languages) {
      rv.add(MetaAdapterFactory.getLanguage(lr.getId(), lr.getNamespace()));
    }
    return rv;
  }

  @Nullable
  public LanguageRuntime getLanguage(SLanguage language) {
    return ((SLanguageAdapter) language).getLanguageDescriptor();
  }

  @Nullable
  public LanguageRuntime getLanguage(SLanguageId id) {
    return myLanguagesById.get(id);
  }

  @Nullable
  public LanguageRuntime getLanguage(String namespace) {
    return myLanguages.get(namespace);
  }

  public LanguageRuntime getLanguage(Language language) {
    return getLanguage(language.getModuleName());
  }

  /**
   * PROVISIONAL API, DO NOT USE
   * Find respective runtime presentation of generator module
   * FIXME shall decide whether need standalone GeneratorRegistry to supply GeneratorRuntimes
   * FIXME or access to GeneratorRuntime through LanguageRegistry is enough.
   */
  public GeneratorRuntime getGenerator(Generator generator) {
    LanguageRuntime lr = getLanguage(generator.getSourceLanguage());
    if (lr == null) {
      return null;
    }
    for (GeneratorRuntime grt : lr.getGenerators()) {
      if (grt.getModuleReference().equals(generator.getModuleReference())) {
        return grt;
      }
    }
    return null;
  }

  // MPSClassesListener part
  @Override
  public void beforeClassesUnloaded(Set<? extends ReloadableModuleBase> unloadedModules) {
    Set<LanguageRuntime> languagesToUnload = new HashSet<LanguageRuntime>();
    for (SLanguageId languageId : collectLanguagesToUnload(unloadedModules)) {
      if (!myLanguagesById.containsKey(languageId)) {
        LOG.warn("No language with id " + languageId + " to unload");
      } else {
        languagesToUnload.add(myLanguagesById.get(languageId));
      }
    }

    notifyUnload(languagesToUnload);

    for (LanguageRuntime languageRuntime : languagesToUnload) {
      myLanguages.remove(languageRuntime.getNamespace());
      myLanguagesById.remove(languageRuntime.getId());
    }
    reinitialize();
  }

  @Override
  public void afterClassesLoaded(Set<? extends ReloadableModuleBase> loadedModules) {
    Set<LanguageRuntime> loadedRuntimes = new HashSet<LanguageRuntime>();
    for (Language language : collectLanguagesToLoad(loadedModules)) {
      SLanguageId languageId = MetaIdByDeclaration.getLanguageId(language);
      if (myLanguagesById.containsKey(languageId)) {
        LOG.error("", new IllegalArgumentException(String.format("There is already a language '%s' with id '%s'", myLanguagesById.get(languageId), languageId)));
        continue;
      }
      try {
        LanguageRuntime langRuntime = createRuntime(language);
        if (langRuntime == null) continue;

        String langName = langRuntime.getNamespace();
        if (myLanguages.containsKey(langName)) {
          LOG.warn(String.format("There is already a language '%s' with a name '%s'", myLanguages.get(langName), langName));
        }
        myLanguages.put(langName, langRuntime);
        myLanguagesById.put(languageId, langRuntime);
        loadedRuntimes.add(langRuntime);
      } catch (LinkageError le) {
        processLinkageErrorForLanguage(language, le);
      }
    }
    reinitialize();
    notifyLoad(loadedRuntimes);
  }

  private Iterable<SLanguageId> collectLanguagesToUnload(Set<? extends SModule> unloadedModules) {
    Collection<SLanguageId> languagesToUnload = new ArrayList<SLanguageId>();
    for (SModule unloadedModule : unloadedModules) {
      if (unloadedModule instanceof Language) {
        languagesToUnload.add(MetaIdByDeclaration.getLanguageId((Language) unloadedModule));
      }
    }
    return languagesToUnload;
  }

  private Iterable<Language> collectLanguagesToLoad(Set<? extends SModule> loadedModules) {
    Collection<Language> languagesToLoad = new ArrayList<Language>();
    for (SModule loadedModule : loadedModules) {
      if (loadedModule instanceof Language) {
        languagesToLoad.add((Language) loadedModule);
      }
    }

    return languagesToLoad;
  }

  private void reinitialize() {
    for (LanguageRuntime languageRuntime : myLanguages.values()) {
      languageRuntime.deinitialize();
    }
    for (LanguageRuntime languageRuntime : myLanguages.values()) {
      languageRuntime.initialize(this);
    }
  }

  private static void processLinkageErrorForLanguage(Language language, LinkageError linkageError) {
    LOG.error("Caught a linkage error while creating LanguageRuntime for the `" + language + "` language." +
        "Probably the language sources/classes are outdated, try rebuilding the project.", linkageError);
    LOG.warn("MPS will attempt running in a inconsistent state.");
  }
}
