// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Use the JOSM home directory. See {@link JOSMTestRules}.
 * Typically only used by {@link FullPreferences}.
 *
 * @author Taylor Smock
 * @since 18037
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(JosmHome.JosmHomeExtension.class)
public @interface JosmHome {
    /**
     * Create a JOSM home directory. Prefer using {@link JosmHome}.
     * @author Taylor Smock
     */
    class JosmHomeExtension implements BeforeAllCallback, AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            Path tempDir = context.getStore(Namespace.create(JosmHome.class)).get("home", Path.class);
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Path tempDir = Files.createTempDirectory(UUID.randomUUID().toString());
            context.getStore(Namespace.create(JosmHome.class)).put("home", tempDir);
            File home = tempDir.toFile();
            System.setProperty("josm.home", home.getAbsolutePath());
            JosmBaseDirectories.getInstance().clearMemos();
        }
    }
}
