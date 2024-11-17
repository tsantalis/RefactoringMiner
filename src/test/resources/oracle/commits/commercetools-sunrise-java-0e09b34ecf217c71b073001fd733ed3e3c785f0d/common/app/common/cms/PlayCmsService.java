package common.cms;

import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.F;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

/**
 * Service that provides content data from Messages files as handled by Play.
 * In order to share a common interface with other CMS, a normal message key has been split into Page Key and Message Key.
 * A possible example: "home.title" is split into "home" as Page Key and "title" as Message Key.
 * Nonetheless, you can always skip the Page Key and provide only a Message Key "home.title".
 */
@Singleton
public class PlayCmsService implements CmsService {
    private final MessagesApi messagesApi;

    @Inject
    private PlayCmsService(final MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    @Override
    public F.Promise<CmsPage> get(final Locale locale, final String pageKey) {
        final Lang lang = Lang.forCode(locale.toLanguageTag());
        final Messages messages = new Messages(lang, messagesApi);
        final CmsPage cmsPage = new PlayCmsPage(messages, pageKey);
        return F.Promise.pure(cmsPage);
    }

    public static PlayCmsService of(final MessagesApi messagesApi) {
        return new PlayCmsService(messagesApi);
    }
}