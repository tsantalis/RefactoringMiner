// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Useful methods for annotation extensions
 * @author Taylor Smock
 * @since 18037
 */
final class AnnotationUtils {
    private AnnotationUtils() {
        // Utils class
    }

    /**
     * Find the first parent annotation
     * @param <T> The annotation to find
     * @param context The context to search
     * @param annotation The annotation to find
     * @return See {@link AnnotationSupport#findAnnotation}
     */
    public static <T extends Annotation> Optional<T> findFirstParentAnnotation(ExtensionContext context, Class<T> annotation) {
        ExtensionContext current = context;
        do {
            Optional<T> foundAnnotation = AnnotationSupport.findAnnotation(current.getElement(), annotation);
            if (foundAnnotation.isPresent()) {
                return foundAnnotation;
            }
            current = current.getParent().orElse(null);
        } while (current != null);
        return Optional.empty();
    }

    /**
     * Reset a static class (all static fields are unset). If they are initialized as part of a static block, please be aware of NPEs.
     * @param clazz The class to reset
     * @throws ReflectiveOperationException If reflection doesn't work, for whatever reason.
     */
    public static void resetStaticClass(Class<?> clazz) throws ReflectiveOperationException {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            // Don't reset fields that are not static
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            final boolean isFinal = (field.getModifiers() & Modifier.FINAL) != 0;
            if (field.get(null) instanceof Collection && isFinal) {
                // Clear all collections (assume they start empty)
                try {
                    ((Collection<?>) field.get(null)).clear();
                } catch (UnsupportedOperationException e) {
                    // Probably an unmodifiable collection
                }
            } else if (!isFinal) {
                // Only reset static fields, but not final static fields
                field.set(null, null);
            }
        }
    }

    /**
     * Check if the element is annotated with the
     * @param annotatedElement The annotated element wrapped in an optional.
     * @param annotationClass The annotation class
     * @param <T> Annotation class type
     * @return {@code true} if the element is present and is annotated with the specified class
     */
    public static <T extends Annotation> boolean elementIsAnnotated(Optional<AnnotatedElement> annotatedElement, Class<T> annotationClass) {
        return annotatedElement.isPresent() && annotatedElement.get().isAnnotationPresent(annotationClass);
    }
}
