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
package jetbrains.mps.smodel.adapter.structure;

import jetbrains.mps.smodel.adapter.structure.concept.SConceptAdapterByName;
import jetbrains.mps.smodel.adapter.structure.concept.SInterfaceConceptAdapterByName;
import jetbrains.mps.smodel.adapter.structure.language.SLanguageAdapterByName;
import jetbrains.mps.smodel.adapter.structure.link.SContainmentLinkAdapterByName;
import jetbrains.mps.smodel.adapter.structure.property.SPropertyAdapterByName;
import jetbrains.mps.smodel.adapter.structure.ref.SReferenceLinkAdapterByName;
import jetbrains.mps.smodel.language.ConceptRegistry;
import jetbrains.mps.smodel.runtime.ConceptDescriptor;
import jetbrains.mps.smodel.runtime.illegal.IllegalConceptDescriptor;
import jetbrains.mps.util.annotation.ToRemove;
import org.jetbrains.mps.openapi.language.SAbstractConcept;
import org.jetbrains.mps.openapi.language.SConcept;
import org.jetbrains.mps.openapi.language.SContainmentLink;
import org.jetbrains.mps.openapi.language.SInterfaceConcept;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.language.SProperty;
import org.jetbrains.mps.openapi.language.SReferenceLink;

/**
 * {@link jetbrains.mps.smodel.legacy.ConceptMetaInfoConverter} covers transition from string to meta-object within SConcept scope.
 * To get SLanguage or SConcept/SInterfaceConcept, there's no other alternative at the moment but to use static methods of this class.
 */
public class MetaAdapterFactoryByName {
  public static SLanguage getLanguage(String langName) {
    return new SLanguageAdapterByName(langName);
  }

  @Deprecated
  @ToRemove(version = 3.3)
  //no usages in MPS except SModelUtil.findConceptDeclaration
  public static SConcept getConcept(String conceptName) {
    return new SConceptAdapterByName(conceptName);
  }

  public static SInterfaceConcept getInterfaceConcept(String conceptName) {
    return new SInterfaceConceptAdapterByName(conceptName);
  }

  /**
   * Generally, this method shall not be used directly. Consider using {@link jetbrains.mps.smodel.legacy.ConceptMetaInfoConverter#convertProperty(String)} instead
   */
  public static SProperty getProperty(String conceptName, String propName) {
    return new SPropertyAdapterByName(conceptName, propName);
  }

  /**
   * Generally, this method shall not be used directly. Consider using {@link jetbrains.mps.smodel.legacy.ConceptMetaInfoConverter#convertAssociation(String)} instead
   */
  public static SReferenceLink getReferenceLink(String conceptName, String refName) {
    return new SReferenceLinkAdapterByName(conceptName, refName);
  }

  /**
   * Generally, this method shall not be used directly. Consider using {@link jetbrains.mps.smodel.legacy.ConceptMetaInfoConverter#convertAggregation(String)}} instead
   */
  public static SContainmentLink getContainmentLink(String conceptName, String linkName) {
    return new SContainmentLinkAdapterByName(conceptName, linkName);
  }

  @Deprecated
  @ToRemove(version = 3.3)
  //not used in MPS
  //this is to use only for compatibility reasons between 3.2 and 3.3. This code does not run normally at all
  public static SAbstractConcept getTypedConcept_DoNotUse(String conceptName) {
    final ConceptDescriptor cd = ConceptRegistry.getInstance().getConceptDescriptor(conceptName);
    if (cd instanceof IllegalConceptDescriptor) return MetaAdapterFactoryByName.getConcept(conceptName);
    return MetaAdapterFactory.getAbstractConcept(cd);
  }
}
