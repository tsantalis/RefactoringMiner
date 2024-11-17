package productcatalog.productoverview;

import common.contexts.UserContext;
import common.models.DetailData;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;

import java.util.Optional;

public class JumbotronBean extends DetailData {
    private String subtitle;

    public JumbotronBean() {
    }

    public JumbotronBean(final Category category, final UserContext userContext, final CategoryTree categoryTree) {
        setTitle(category.getName().find(userContext.locales()).orElse(""));
        Optional.ofNullable(category.getParent())
                .ifPresent(parentRef -> categoryTree.findById(parentRef.getId())
                        .ifPresent(parent -> this.subtitle = parent.getName().find(userContext.locales()).orElse("")));
        Optional.ofNullable(category.getDescription())
                .ifPresent(description -> setDescription(description.find(userContext.locales()).orElse("")));
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(final String subtitle) {
        this.subtitle = subtitle;
    }
}
