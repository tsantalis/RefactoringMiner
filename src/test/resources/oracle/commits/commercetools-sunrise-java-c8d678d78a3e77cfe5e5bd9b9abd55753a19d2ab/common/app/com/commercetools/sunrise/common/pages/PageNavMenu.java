package com.commercetools.sunrise.common.pages;

import com.commercetools.sunrise.common.models.CategoryBean;
import com.commercetools.sunrise.common.models.ModelBean;

import java.util.List;

public class PageNavMenu extends ModelBean {

    private List<CategoryBean> categories;

    public PageNavMenu() {
    }

    public List<CategoryBean> getCategories() {
        return categories;
    }

    public void setCategories(final List<CategoryBean> categories) {
        this.categories = categories;
    }


}
