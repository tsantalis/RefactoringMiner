package common.cms;

import play.libs.F;

import java.util.Locale;

/**
 * Service that provides page content, usually coming from a Content Management System (CMS).
 */
public interface CmsService {

    /**
     * Gets the page content corresponding to the given key for a certain language.
     * @param locale for the localized text
     * @param key identifying the page
     * @return the promise of the page content identified by the key, in the given language
     */
    F.Promise<CmsPage> get(final Locale locale, final String key);
}
