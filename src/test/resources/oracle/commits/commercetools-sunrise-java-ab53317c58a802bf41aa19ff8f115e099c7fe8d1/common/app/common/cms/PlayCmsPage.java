package common.cms;

import play.i18n.Messages;

import java.util.Optional;

public class PlayCmsPage implements CmsPage {
    private final Messages messages;

    private PlayCmsPage(final Messages messages) {
        this.messages = messages;
    }

    @Override
    public Optional<String> get(final String key, final Object... args) {
        if (messages.isDefinedAt(key)) {
            return Optional.of(messages.at(key, args));
        } else {
            return Optional.empty();
        }
    }

    public static PlayCmsPage of(final Messages messages) {
        return new PlayCmsPage(messages);
    }
}
