package search;

import com.google.inject.AbstractModule;
import productcatalog.productoverview.search.facetedsearch.FacetedSearchConfigList;
import productcatalog.productoverview.search.productsperpage.ProductsPerPageConfig;
import productcatalog.productoverview.search.sort.SortConfig;

public class SearchTestModule extends AbstractModule {

    private final ProductsPerPageConfig productsPerPageConfig;
    private final SortConfig sortConfig;
    private final FacetedSearchConfigList facetedSearchConfigList;

    public SearchTestModule(final ProductsPerPageConfig productsPerPageConfig, final SortConfig sortConfig,
                            final FacetedSearchConfigList facetedSearchConfigList) {
        this.productsPerPageConfig = productsPerPageConfig;
        this.sortConfig = sortConfig;
        this.facetedSearchConfigList = facetedSearchConfigList;
    }

    @Override
    protected void configure() {
        bind(ProductsPerPageConfig.class).toInstance(productsPerPageConfig);
        bind(SortConfig.class).toInstance(sortConfig);
        bind(FacetedSearchConfigList.class).toInstance(facetedSearchConfigList);
    }
}
