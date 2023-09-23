/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.plugins;

import org.gradle.api.Action;
import org.gradle.jvm.JarBinarySpec;
import org.gradle.jvm.internal.JarBinarySpecInternal;
import org.gradle.model.Defaults;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.ComponentSpec;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class JarBinaryRules extends RuleSource {
    @Defaults
    void configureJarBinaries(final ComponentSpec jvmLibrary, @Path("buildDir") File buildDir) {
        final File binariesDir = new File(buildDir, "jars");
        final File classesDir = new File(buildDir, "classes");
        jvmLibrary.getBinaries().withType(JarBinarySpec.class).beforeEach(new Action<JarBinarySpec>() {
            @Override
            public void execute(JarBinarySpec jarBinary) {
                JarBinarySpecInternal jarBinaryInternal = (JarBinarySpecInternal) jarBinary;
                ((JarBinarySpecInternal) jarBinary).setBaseName(jvmLibrary.getName());

                File outputDir = new File(classesDir, jarBinary.getName());
                jarBinary.setClassesDir(outputDir);
                jarBinary.setResourcesDir(outputDir);
                jarBinary.setJarFile(new File(binariesDir, String.format("%s/%s.jar", jarBinary.getName(), jarBinaryInternal.getBaseName())));
            }
        });
    }
}
