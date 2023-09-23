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
package jetbrains.mps.smodel.adapter.structure.language;

import jetbrains.mps.project.ModuleId;
import jetbrains.mps.smodel.Language;
import jetbrains.mps.smodel.MPSModuleRepository;
import jetbrains.mps.smodel.adapter.ids.SLanguageId;
import jetbrains.mps.smodel.language.LanguageRegistry;
import jetbrains.mps.smodel.language.LanguageRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SLanguage;

public final class SLanguageAdapterById extends SLanguageAdapter {
  private final SLanguageId myLanguage;

  public SLanguageAdapterById(@NotNull SLanguageId language, @NotNull String fqName) {
    this(language, fqName, -1);
  }
  public SLanguageAdapterById(@NotNull SLanguageId language, @NotNull String fqName, int version) {
    super(fqName, version);
    this.myLanguage = language;
  }

  @NotNull
  public SLanguageId getId() {
    return myLanguage;
  }

  @Override
  public String getQualifiedName() {
    LanguageRuntime ld = getLanguageDescriptor();
    if (ld == null) {
      return myLanguageFqName;
    }
    return ld.getNamespace();
  }

  @Override
  @Nullable
  public LanguageRuntime getLanguageDescriptor() {
    return LanguageRegistry.getInstance().getLanguage(myLanguage);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SLanguage)) return  false;
    return ( obj instanceof SLanguageAdapterById) ? myLanguage.equals(((SLanguageAdapterById) obj).myLanguage) : myLanguageFqName.equals(((SLanguageAdapter) obj).myLanguageFqName);
  }

  @Override
  @Nullable
  public Language getSourceModule() {
    return ((Language) MPSModuleRepository.getInstance().getModule(ModuleId.regular(myLanguage.getIdValue())));
  }
}
