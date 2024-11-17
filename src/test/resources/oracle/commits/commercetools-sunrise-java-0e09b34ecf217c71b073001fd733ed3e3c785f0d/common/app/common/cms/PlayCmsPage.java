package common.cms;

import play.i18n.Messages;

import java.util.Optional;

public class PlayCmsPage implements CmsPage {
    private final Messages messages;
    private final Optional<String> pageKey;

    PlayCmsPage(final Messages messages, final String pageKey) {
        this.messages = messages;
        this.pageKey = Optional.ofNullable(pageKey);
    }

    @Override
    public Optional<String> get(final String messageKey, final Object... args) {
        final String key = key(messageKey);
        if (messages.isDefinedAt(key)) {
            return Optional.of(messages.at(key, args));
        } else {
            return Optional.empty();
        }
    }

    private String key(final String messageKey) {
        return pageKey.map(pk -> pk + "." + messageKey).orElse(messageKey);
    }
}
