package productcatalog.productoverview.search.facetedsearch;

import common.contexts.UserContext;
import common.controllers.SunrisePageData;
import common.hooks.SunrisePageDataHook;
import common.template.i18n.I18nIdentifier;
import common.template.i18n.I18nResolver;
import framework.ControllerComponent;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.facets.Facet;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
import productcatalog.hooks.ProductProjectionPagedSearchResultHook;
import productcatalog.hooks.ProductProjectionSearchFilterHook;
import productcatalog.hooks.SingleCategoryHook;
import productcatalog.productoverview.ProductOverviewPageContent;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public class FacetedSearchComponent implements ControllerComponent, SunrisePageDataHook, ProductProjectionSearchFilterHook, ProductProjectionPagedSearchResultHook, SingleCategoryHook {

    private List<Category> selectedCategories = emptyList();
    private List<FacetedSearchSelector> facetedSearchSelectorList = emptyList();
    private List<FacetSelectorBean> facetBeans = emptyList();

    @Inject
    private FacetedSearchSelectorListFactory facetedSearchSelectorListFactory;
    @Inject
    private UserContext userContext;
    @Inject
    private I18nResolver i18nResolver;

    @Override
    public CompletionStage<?> onSingleCategoryLoaded(final Category category) {
        this.selectedCategories = singletonList(category);
        return completedFuture(null);
    }

    @Override
    public ProductProjectionSearch filterProductProjectionSearch(final ProductProjectionSearch search) {
        this.facetedSearchSelectorList = facetedSearchSelectorListFactory.create(selectedCategories);
        return search.plusFacetedSearch(facetedSearchSelectorList.stream()
                .map(FacetedSearchSelector::getFacetedSearchExpression)
                .collect(toList()));
    }

    @Override
    public void acceptProductProjectionPagedSearchResult(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        facetBeans = facetedSearchSelectorList.stream()
                .sorted((f1, f2) -> Double.compare(f1.getPosition(), f2.getPosition()))
                .map(facetedSearchSelector -> createFacetSelectorBean(facetedSearchSelector, pagedSearchResult))
                .collect(toList());
    }

    @Override
    public void acceptSunrisePageData(final SunrisePageData sunrisePageData) {
        if (sunrisePageData.getContent() instanceof ProductOverviewPageContent) {
            final ProductOverviewPageContent content = (ProductOverviewPageContent) sunrisePageData.getContent();
            content.setFacets(createFacetSelectorList(facetBeans));
        }
    }

    private FacetSelectorBean createFacetSelectorBean(final FacetedSearchSelector facetedSearchSelector, final PagedSearchResult<ProductProjection> searchResult) {
        final FacetSelectorBean bean = new FacetSelectorBean();
        final Facet<ProductProjection> facet = facetedSearchSelector.getFacet(searchResult);
        if (facet.getLabel() != null) {
            final String label = i18nResolver.getOrKey(userContext.locales(), I18nIdentifier.of(facet.getLabel()));
            bean.setFacet(facet.withLabel(label));
        } else {
            bean.setFacet(facet);
        }
        return bean;
    }

    private FacetSelectorListBean createFacetSelectorList(final List<FacetSelectorBean> facetSelectorBeans) {
        final FacetSelectorListBean bean = new FacetSelectorListBean();
        bean.setList(facetSelectorBeans);
        return bean;
    }
}
