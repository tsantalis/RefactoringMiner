// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences.BasicPreferencesExtension;

/**
 * Allow tests to use JOSM preferences with default values (see {@link JOSMTestRules#preferences()})
 * @author Taylor Smock
 * @see BasicPreferences (often enough for simple tests).
 * @since 18037
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@JosmHome
@BasicPreferences
@ExtendWith(FullPreferences.UsePreferencesExtension.class)
public @interface FullPreferences {
    /**
     * Initialize preferences.
     */
    class UsePreferencesExtension implements BeforeAllCallback, BeforeEachCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Preferences pref = context.getStore(Namespace.create(BasicPreferencesExtension.class)).get("preferences", Preferences.class);
            @SuppressWarnings("unchecked")
            final Map<String, Setting<?>> defaultsMap = (Map<String, Setting<?>>) TestUtils.getPrivateField(pref, "defaultsMap");
            defaultsMap.clear();
            pref.resetToInitialState();
            pref.enableSaveOnPut(false);
            // No pref init -> that would only create the preferences file.
            // We force the use of a wrong API server, just in case anyone attempts an upload
            Config.getPref().put("osm-server.url", "http://invalid");
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            if (AnnotationUtils.elementIsAnnotated(context.getElement(), FullPreferences.class)) {
                this.beforeAll(context);
            }
        }
    }
}
