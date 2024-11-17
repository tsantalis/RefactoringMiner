package com.commercetools.sunrise.common.pages;

import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.models.CategoryBean;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import play.Configuration;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class PageNavMenuFactoryImpl implements PageNavMenuFactory {

    private String saleCategoryExtId;
    @Inject
    private CategoryTree categoryTree;
    @Inject
    private UserContext userContext;
    @Inject
    private ProductReverseRouter productReverseRouter;

    @Inject
    public void initializeFields(Configuration configuration) {
        this.saleCategoryExtId = configuration.getString("common.saleCategoryExternalId");
    }

    @Override
    public PageNavMenu create() {
        final PageNavMenu bean = new PageNavMenu();
        final List<CategoryBean> categories = new LinkedList<>();
        categoryTree.getRoots().forEach(root -> {
            final CategoryBean categoryData = createCategoryData(root, categoryTree, userContext, productReverseRouter, saleCategoryExtId);
            categories.add(categoryData);
        });
        bean.setCategories(categories);
        return bean;
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
