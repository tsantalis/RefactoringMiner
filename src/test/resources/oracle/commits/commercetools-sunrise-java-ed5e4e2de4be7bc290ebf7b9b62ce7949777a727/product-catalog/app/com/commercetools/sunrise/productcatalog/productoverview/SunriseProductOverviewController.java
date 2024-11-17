package com.commercetools.sunrise.productcatalog.productoverview;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.pages.PageContent;
import com.commercetools.sunrise.hooks.*;
import com.commercetools.sunrise.productcatalog.productoverview.search.facetedsearch.FacetedSearchComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.pagination.PaginationComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.searchbox.SearchBoxComponent;
import com.commercetools.sunrise.productcatalog.productoverview.search.sort.SortSelectorComponent;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
import play.inject.Injector;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Provides facilities to search and display products.
 *
 * <p>Components that may be a fit</p>
 * <ul>
 *     <li>{@link SortSelectorComponent}</li>
 *     <li>{@link PaginationComponent}</li>
 *     <li>{@link SearchBoxComponent}</li>
 *     <li>{@link FacetedSearchComponent}</li>
 * </ul>
 * <p id="hooks">supported hooks</p>
 * <ul>
 *     <li>{@link RequestHook}</li>
 *     <li>{@link PageDataHook}</li>
 *     <li>{@link ProductProjectionSearchFilterHook}</li>
 *     <li>{@link SingleCategoryHook}</li>
 *     <li>{@link ProductProjectionPagedSearchResultHook}</li>
 * </ul>
 * <p>tags</p>
 * <ul>
 *     <li>product-overview</li>
 *     <li>product-catalog</li>
 *     <li>search</li>
 *     <li>product</li>
 *     <li>category</li>
 * </ul>
 */
@RequestScoped
public abstract class SunriseProductOverviewController extends SunriseFrameworkController implements WithOverwriteableTemplateName {

    @Inject
    private UserContext userContext;
    @Inject
    private CategoryTree categoryTree;
    @Inject
    private Injector injector;
    @Inject
    private ProductOverviewPageContentFactory productOverviewPageContentFactory;

    @Nullable
    private String categorySlug;
    @Nullable
    private Category category;

    @Override
    public String getTemplateName() {
        return "pop";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("product-overview", "product-catalog", "search", "product", "category"));
    }

    public CompletionStage<Result> searchProductsByCategorySlug(final String languageTag, final String categorySlug) {
        return doRequest(() -> {
            this.categorySlug = categorySlug;
            final Optional<Category> category = categoryTree.findBySlug(userContext.locale(), categorySlug);
            if (category.isPresent()) {
                this.category = category.get();
                runHookOnFoundCategory(category.get());
                return searchProducts();
            } else {
                return handleNotFoundCategory();
            }
        });
    }

    public CompletionStage<Result> searchProductsBySearchTerm(final String languageTag) {
        return searchProducts();
    }

    protected CompletionStage<Result> searchProducts() {
        return injector.instanceOf(ProductListFetchSimple.class).searchProducts(null, this::runHookOnProductSearch)
                .thenComposeAsync(this::listProducts, HttpExecution.defaultContext());
    }

    protected CompletionStage<Result> listProducts(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        if (pagedSearchResult.getResults().isEmpty()) {
            return handleEmptySearch(pagedSearchResult);
        } else {
            runHookOnProductSearchResult(pagedSearchResult);
            return handleFoundProducts(pagedSearchResult);
        }
    }

    protected CompletionStage<Result> handleFoundProducts(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        final PageContent pageContent = createPageContent(pagedSearchResult);
        return asyncOk(renderPageWithTemplate(pageContent, getTemplateName()));
    }

    protected CompletionStage<Result> handleEmptySearch(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        final PageContent pageContent = createPageContent(pagedSearchResult);
        return asyncOk(renderPageWithTemplate(pageContent, getTemplateName()));
    }

    protected CompletionStage<Result> handleNotFoundCategory() {
        return completedFuture(notFoundCategoryResult());
    }

    protected PageContent createPageContent(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        return productOverviewPageContentFactory.create(category, pagedSearchResult);
    }

    protected Result notFoundCategoryResult() {
        return notFound("Category not found: " + categorySlug().orElse("[unknown]"));
    }

    protected final ProductProjectionSearch runHookOnProductSearch(final ProductProjectionSearch productSearch) {
        return hooks().runFilterHook(ProductProjectionSearchFilterHook.class, (hook, search) -> hook.filterQuery(search), productSearch);
    }

    protected final CompletionStage<?> runHookOnFoundCategory(final Category category) {
        return hooks().runAsyncHook(SingleCategoryHook.class, hook -> hook.onSingleCategoryLoaded(category));
    }

    protected final void runHookOnProductSearchResult(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        hooks().runVoidHook(ProductProjectionPagedSearchResultHook.class, hook -> hook.acceptProductProjectionPagedSearchResult(pagedSearchResult));
    }

    protected final Optional<Category> category() {
        return Optional.ofNullable(category);
    }

    protected final Optional<String> categorySlug() {
        return Optional.ofNullable(categorySlug);
    }
}