package productcatalog.productoverview;

import common.models.DetailData;

public class BannerBean extends DetailData {

    private String imageMobile;
    private String imageDesktop;

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
