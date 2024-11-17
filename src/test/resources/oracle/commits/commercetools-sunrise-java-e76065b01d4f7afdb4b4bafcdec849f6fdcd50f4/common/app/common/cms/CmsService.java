package common.cms;

import play.libs.F;

import java.util.Locale;
import java.util.Optional;

/**
 * Service that provides content, usually coming from a Content Management System (CMS).
 */
public interface CmsService {

    /**
     * Gets the message corresponding to the given key for a certain language.
     * @param locale for the localized text
     * @param key identifying the message
     * @param args optional list of arguments
     * @return the message identified by the key, in the given language with the given arguments, or absent if not found
     */
    F.Promise<Optional<String>> get(final Locale locale, final String key, final Object... args);

    /**
     * Gets the message corresponding to the given key for a certain language, or empty in case it does not exist.
     * @param locale for the localized text
     * @param key identifying the message
     * @param args optional list of arguments
     * @return the message identified by the key, in the given language with the given arguments, or empty string if not found
     */
    default F.Promise<String> getOrEmpty(final Locale locale, final String key, final Object... args) {
        return get(locale, key, args).map(msg -> msg.orElse(""));
    }
}
