package productcatalog.productoverview;

import common.models.DetailData;

public class JumbotronBean extends DetailData {

    private String subtitle;

    public JumbotronBean() {
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(final String subtitle) {
        this.subtitle = subtitle;
    }
}
