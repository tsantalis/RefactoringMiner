package common.cms;

import org.junit.Test;
import play.Application;
import play.Configuration;
import play.Environment;
import play.Mode;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithApplication;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class PlayCmsPageTest extends WithApplication {
    private static final Locale DE = Locale.forLanguageTag("de");
    private static final Locale DE_AT = Locale.forLanguageTag("de-AT");

    @Test
    public void getsMessage() throws Exception {
        assertThat(cms(DE).get("foo")).contains("bar");
    }

    @Test
    public void getsMessageWithRegion() throws Exception {
        assertThat(cms(DE_AT).get("foo")).contains("baz");
    }

    @Test
    public void getsEmptyWhenKeyNotFound() throws Exception {
        assertThat(cms(DE).get("wrong.message")).isEmpty();
    }

    @Override
    protected Application provideApplication() {
        final Configuration config = new Configuration(singletonMap("play.i18n.langs", asList(DE.toLanguageTag(), DE_AT.toLanguageTag())));
        final Configuration fallbackConfig = Configuration.load(new Environment(Mode.TEST));
        return new GuiceApplicationBuilder().loadConfig(config.withFallback(fallbackConfig)).build();

    }

    private CmsPage cms(final Locale locale) {
        final MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
        return PlayCmsService.of(messagesApi).get(locale, "").get(0);
    }
}
