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

public class PlayCmsServiceTest extends WithApplication {

    public static final int TIMEOUT = 100;

    @Test
    public void getsMessage() throws Exception {
        final Optional<String> message = cms().get(Locale.forLanguageTag("de"), "foo").get(TIMEOUT);
        assertThat(message).contains("bar");
    }

    @Test
    public void getsMessageWithRegion() throws Exception {
        final Optional<String> message = cms().get(Locale.forLanguageTag("de-AT"), "foo").get(TIMEOUT);
        assertThat(message).contains("baz");
    }

    @Test
    public void getsEmptyWhenKeyNotFound() throws Exception {
        final Optional<String> message = cms().get(Locale.forLanguageTag("de"), "wrong.message").get(TIMEOUT);
        assertThat(message).isEmpty();
    }

    @Override
    protected Application provideApplication() {
        final Configuration config = new Configuration(singletonMap("play.i18n.langs", asList("de", "de-AT")));
        final Configuration fallbackConfig = Configuration.load(new Environment(Mode.TEST));
        return new GuiceApplicationBuilder().loadConfig(config.withFallback(fallbackConfig)).build();

    }

    private CmsService cms() {
        final MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
        return PlayCmsService.of(messagesApi);
    }
}
