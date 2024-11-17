package common.cms;

import org.junit.Test;
import play.libs.F;

import java.util.Optional;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

public class CmsServiceTest {

    public static final int TIMEOUT = 100;

    @Test
    public void getsMessage() throws Exception {
        final CmsService cmsService = ((l, k, a) -> F.Promise.pure(Optional.of("bar")));
        final String message = cmsService.getOrEmpty(ENGLISH, "foo").get(TIMEOUT);
        assertThat(message).isEqualTo("bar");
    }

    @Test
    public void getsEmptyStringWhenKeyNotFound() throws Exception {
        final CmsService cmsService = ((l, k, a) -> F.Promise.pure(Optional.empty()));
        final String message = cmsService.getOrEmpty(ENGLISH, "foo").get(TIMEOUT);
        assertThat(message).isEmpty();
    }
}
