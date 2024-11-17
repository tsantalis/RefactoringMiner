package productcatalog.productoverview.search.searchbox;

import common.controllers.SunrisePageData;
import common.hooks.SunrisePageDataHook;
import framework.ControllerComponent;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import productcatalog.common.BreadcrumbBeanFactory;
import productcatalog.hooks.ProductProjectionSearchFilterHook;
import productcatalog.productoverview.ProductOverviewPageContent;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class SearchBoxComponent implements ControllerComponent, SunrisePageDataHook, ProductProjectionSearchFilterHook {

    @Nullable
    private SearchBox searchBox;

    @Inject
    private SearchBoxFactory searchBoxFactory;
    @Inject
    private BreadcrumbBeanFactory breadcrumbBeanFactory;

    @Override
    public ProductProjectionSearch filterProductProjectionSearch(final ProductProjectionSearch search) {
        this.searchBox = searchBoxFactory.create();
        return searchBox.getSearchTerm()
                .map(search::withText)
                .orElse(search);
    }

    @Override
    public void acceptSunrisePageData(final SunrisePageData sunrisePageData) {
        if (searchBox != null && searchBox.getSearchTerm().isPresent() && sunrisePageData.getContent() instanceof ProductOverviewPageContent) {
            final ProductOverviewPageContent content = (ProductOverviewPageContent) sunrisePageData.getContent();
            final String searchTerm = searchBox.getSearchTerm().get().getValue();
            content.setAdditionalTitle(searchTerm);
            content.setBreadcrumb(breadcrumbBeanFactory.create(searchTerm));
            content.setSearchTerm(searchTerm);
        }
    }
}
