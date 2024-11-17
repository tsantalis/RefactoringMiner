package com.commercetools.sunrise.common.pages;

import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.models.CategoryBean;
import com.commercetools.sunrise.common.models.ModelBean;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class PageNavMenu extends ModelBean {

    private List<CategoryBean> categories;

    public PageNavMenu() {
    }

    public PageNavMenu(final CategoryTree categoryTree, final UserContext userContext, final ProductReverseRouter productReverseRouter,
                       @Nullable final String saleCategoryExtId) {
        this.categories = new ArrayList<>();
        categoryTree.getRoots().forEach(root -> {
            final CategoryBean categoryData = createCategoryData(root, categoryTree, userContext, productReverseRouter, saleCategoryExtId);
            this.categories.add(categoryData);
        });
    }

    public List<CategoryBean> getCategories() {
        return categories;
    }

    public void setCategories(final List<CategoryBean> categories) {
        this.categories = categories;
    }

    private static CategoryBean createCategoryData(final Category category, final CategoryTree categoryTree,
                                                   final UserContext userContext, final ProductReverseRouter productReverseRouter,
                                                   @Nullable final String saleCategoryExtId) {
        final CategoryBean categoryData = new CategoryBean();
        categoryData.setText(category.getName().find(userContext.locales()).orElse(""));
        categoryData.setUrl(productReverseRouter.productOverviewPageUrlOrEmpty(userContext.locale(), category));
        categoryData.setSale(Optional.ofNullable(category.getExternalId())
                .map(id -> id.equals(saleCategoryExtId))
                .orElse(false));
        categoryData.setChildren(categoryTree.findChildren(category).stream()
                .map(child -> createCategoryData(child, categoryTree, userContext, productReverseRouter, saleCategoryExtId))
                .collect(toList()));
        return categoryData;
    }
}
