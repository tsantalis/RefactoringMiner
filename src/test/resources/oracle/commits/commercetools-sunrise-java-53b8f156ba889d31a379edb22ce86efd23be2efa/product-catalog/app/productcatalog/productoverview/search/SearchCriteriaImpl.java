package productcatalog.productoverview.search;

import io.sphere.sdk.models.LocalizedStringEntry;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.search.FacetedSearchExpression;
import io.sphere.sdk.search.SortExpression;
import productcatalog.productoverview.search.facetedsearch.FacetedSearchSelector;
import productcatalog.productoverview.search.pagination.Pagination;
import productcatalog.productoverview.search.productsperpage.ProductsPerPageSelector;
import productcatalog.productoverview.search.searchbox.SearchBox;
import productcatalog.productoverview.search.sort.SortSelector;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class SearchCriteriaImpl implements SearchCriteria {

    private final Pagination pagination;
    private final SearchBox searchBox;
    private final SortSelector sortSelector;
    private final ProductsPerPageSelector productsPerPageSelector;
    private final List<FacetedSearchSelector> facetedSearchSelectors;

    protected SearchCriteriaImpl(final Pagination pagination, final SearchBox searchBox, final SortSelector sortSelector,
                                 final ProductsPerPageSelector productsPerPageSelector, final List<FacetedSearchSelector> facetedSearchSelectors) {
        this.pagination = pagination;
        this.searchBox = searchBox;
        this.sortSelector = sortSelector;
        this.productsPerPageSelector = productsPerPageSelector;
        this.facetedSearchSelectors = facetedSearchSelectors;
    }

    @Override
    public int getPage() {
        return pagination.getPage();
    }

    @Override
    public Optional<LocalizedStringEntry> getSearchTerm() {
        return searchBox.getSearchTerm();
    }

    @Override
    public int getPageSize() {
        return productsPerPageSelector.getSelectedPageSize();
    }

    @Override
    public List<SortExpression<ProductProjection>> getSortExpressions() {
        return sortSelector.getSelectedSortExpressions();
    }

    @Override
    public List<FacetedSearchExpression<ProductProjection>> getFacetedSearchExpressions() {
        return facetedSearchSelectors.stream()
                .map(FacetedSearchSelector::getFacetedSearchExpression)
                .collect(toList());
    }

    public static SearchCriteriaImpl of(final Pagination pagination, final SearchBox searchBox, final SortSelector sortSelector,
                                        final ProductsPerPageSelector productsPerPageSelector, final List<FacetedSearchSelector> facetedSearchSelectors) {
        return new SearchCriteriaImpl(pagination, searchBox, sortSelector, productsPerPageSelector, facetedSearchSelectors);
    }
}
