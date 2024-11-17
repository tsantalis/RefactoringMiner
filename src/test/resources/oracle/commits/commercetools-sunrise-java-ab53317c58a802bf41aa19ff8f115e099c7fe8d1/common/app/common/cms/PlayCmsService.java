package common.cms;

import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.F;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
public class PlayCmsService implements CmsService {
    private final MessagesApi messagesApi;

    @Inject
    private PlayCmsService(final MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    @Override
    public F.Promise<CmsPage> get(final Locale locale, final String key) {
        final Lang lang = Lang.forCode(locale.toLanguageTag());
        final CmsPage cmsPage = PlayCmsPage.of(new Messages(lang, messagesApi));
        return F.Promise.pure(cmsPage);
    }

    public static PlayCmsService of(final MessagesApi messagesApi) {
        return new PlayCmsService(messagesApi);
    }
}