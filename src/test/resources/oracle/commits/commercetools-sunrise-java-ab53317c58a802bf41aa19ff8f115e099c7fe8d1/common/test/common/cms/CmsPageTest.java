package common.cms;

import org.junit.Test;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CmsPageTest {

    @Test
    public void getsMessage() throws Exception {
        final CmsPage cmsPage = ((k, a) -> Optional.of("bar"));
        assertThat(cmsPage.getOrEmpty("foo")).isEqualTo("bar");
    }

    @Test
    public void getsEmptyStringWhenKeyNotFound() throws Exception {
        final CmsPage cmsPage = ((k, a) -> Optional.empty());
        assertThat(cmsPage.getOrEmpty("foo")).isEmpty();
    }
}
