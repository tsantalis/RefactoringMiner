package productcatalog.productoverview.search;

import productcatalog.productoverview.search.facetedsearch.FacetedSearchConfigList;
import productcatalog.productoverview.search.productsperpage.ProductsPerPageConfig;
import productcatalog.productoverview.search.sort.SortConfig;

public class SearchConfig {

    private final String paginationKey;
    private final String searchTermKey;
    private final ProductsPerPageConfig productsPerPageConfig;
    private final SortConfig sortConfig;
    private final FacetedSearchConfigList facetedSearchConfigList;

    private SearchConfig(final String paginationKey, final String searchTermKey, final ProductsPerPageConfig productsPerPageConfig,
                        final SortConfig sortConfig, final FacetedSearchConfigList facetedSearchConfigList) {
        this.paginationKey = paginationKey;
        this.searchTermKey = searchTermKey;
        this.productsPerPageConfig = productsPerPageConfig;
        this.sortConfig = sortConfig;
        this.facetedSearchConfigList = facetedSearchConfigList;
    }

    public String getPaginationKey() {
        return paginationKey;
    }

    public String getSearchTermKey() {
        return searchTermKey;
    }

    public ProductsPerPageConfig getProductsPerPageConfig() {
        return productsPerPageConfig;
    }

    public SortConfig getSortConfig() {
        return sortConfig;
    }

    public FacetedSearchConfigList getFacetedSearchConfigList() {
        return facetedSearchConfigList;
    }

    public static SearchConfig of(final String paginationKey, final String searchTermKey, final ProductsPerPageConfig productsPerPageConfig,
                                  final SortConfig sortConfig, final FacetedSearchConfigList facetedSearchConfigList) {
        return new SearchConfig(paginationKey, searchTermKey, productsPerPageConfig, sortConfig, facetedSearchConfigList);
    }
}
