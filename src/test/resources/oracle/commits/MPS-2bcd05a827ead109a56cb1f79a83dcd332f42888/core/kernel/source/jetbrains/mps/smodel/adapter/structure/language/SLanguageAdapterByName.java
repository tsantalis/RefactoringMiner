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

import jetbrains.mps.smodel.Language;
import jetbrains.mps.smodel.ModuleRepositoryFacade;
import jetbrains.mps.smodel.adapter.ids.MetaIdByDeclaration;
import jetbrains.mps.smodel.adapter.ids.MetaIdFactory;
import jetbrains.mps.smodel.adapter.ids.SLanguageId;
import jetbrains.mps.smodel.language.LanguageRegistry;
import jetbrains.mps.smodel.language.LanguageRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.language.SLanguage;

public final class SLanguageAdapterByName extends SLanguageAdapter {
  public SLanguageAdapterByName(@NotNull String language) {
    super(language);
  }

  @Override
  @Nullable
  public LanguageRuntime getLanguageDescriptor() {
    return LanguageRegistry.getInstance().getLanguage(myLanguageFqName);
  }

  @Override
  public SLanguageId getId() {
    LanguageRuntime lr = getLanguageDescriptor();
    if (lr != null) {
      return lr.getId();
    }
    Language l = getSourceModule();
    if (l != null) {
      return MetaIdByDeclaration.getLanguageId(l);
    }
    return MetaIdFactory.INVALID_LANGUAGE_ID;
  }

  @Override
  @Nullable
  public Language getSourceModule() {
    return ModuleRepositoryFacade.getInstance().getModule(myLanguageFqName, Language.class);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SLanguage)) return false;
    return myLanguageFqName.equals(((SLanguageAdapter) obj).myLanguageFqName);
  }
}
