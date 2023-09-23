/*
 * Copyright 2003-2012 JetBrains s.r.o.
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
package org.jetbrains.mps.openapi.language;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.module.SModuleReference;

/**
 * Reference to a deployed/run-time language.
 * If the specified language is missing in the current MPS instance, only
 * {@link #getQualifiedName() qualified name} is guaranteed to return meaningful value.
 * Besides, {@link #getLanguageVersion()} might be sensible as well, depending on source
 * SLanguage instance comes from (e.g. if it's an used/imported language of a model, then
 * version value indicates actual value at the time import was added/updated).
 * <p>
 * At the moment, equality of instances this class doesn't respect version. This might get changed.
 */
public interface SLanguage {

  /**
   * The namespace of the language.
   */
  @NotNull
  String getQualifiedName();

  /**
   * All concepts defined in the language, empty if the language is invalid (missing).
   */
  Iterable<SAbstractConcept> getConcepts();

  /**
   * All the runtime dependencies that a language needs after generation to run the generated code.
   * These will be resolved from the user repository.
   * Empty sequence in case language is invalid/missing.
   */
  Iterable<SModuleReference> getLanguageRuntimes();

  /**
   * The optional reference to a module containing the sources for the language. This is useful, for example, when showing
   * the definition of a concept for a used language element.
   * It may be null.
   */
  @Nullable
  SModule getSourceModule();

  /**
   * Version of the referenced language.
   * Version is an integer indicating state of a language. It is changed when the structure of this language changes.
   * Typically this means that if some module uses an older version of a language, it should be updated before the user
   * will be able to work with it. E.g. generator can fail on generation of such a model.
   *
   * In MPS 3.2, version is changed only by adding language migrations.
   * @return version of the language, or -1 the version could not be deduced.
   */
  @Deprecated //normally, one shouldn't have used it. If you had, switch to getting version from LanguageRuntime
  int getLanguageVersion();
}
