package common.cms;

import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.F;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Optional;

@Singleton
public class PlayCmsService implements CmsService {
    private MessagesApi messages;

    @Inject
    private PlayCmsService(final MessagesApi messages) {
        this.messages = messages;
    }

    @Override
    public F.Promise<Optional<String>> get(final Locale locale, final String key, final Object... args) {
        return F.Promise.pure(getLocal(locale, key, args));
    }

    private Optional<String> getLocal(final Locale locale, final String key, final Object... args) {
        final Lang lang = Lang.forCode(locale.toLanguageTag());
        if (messages.isDefinedAt(lang, key)) {
            return Optional.of(messages.get(lang, key, args));
        } else {
            return Optional.empty();
        }
    }

    public static PlayCmsService of(final MessagesApi messages) {
        return new PlayCmsService(messages);
    }
}
