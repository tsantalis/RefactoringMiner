package productcatalog.productoverview;

import common.contexts.UserContext;
import common.models.DetailData;
import io.sphere.sdk.categories.Category;

import java.util.Optional;

public class BannerBean extends DetailData {
    private String imageMobile;
    private String imageDesktop;

    public BannerBean(final UserContext userContext, final Category category) {
        setTitle(category.getName().find(userContext.locales()).orElse(""));
        Optional.ofNullable(category.getDescription())
                .ifPresent(description -> setDescription(description.find(userContext.locales()).orElse("")));
    }

    public String getImageMobile() {
        return imageMobile;
    }

    public void setImageMobile(final String imageMobile) {
        this.imageMobile = imageMobile;
    }

    public String getImageDesktop() {
        return imageDesktop;
    }

    public void setImageDesktop(final String imageDesktop) {
        this.imageDesktop = imageDesktop;
    }
}
