package com.commercetools.sunrise.common.pages;

import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import io.sphere.sdk.categories.CategoryTree;
import play.Configuration;

import javax.inject.Inject;

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
        return new PageNavMenu(categoryTree, userContext, productReverseRouter, saleCategoryExtId);
    }
}
