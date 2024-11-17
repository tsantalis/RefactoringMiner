package productcatalog.productoverview;

import common.contexts.UserContext;
import common.controllers.SunriseFrameworkController;
import common.controllers.SunrisePageData;
import common.controllers.WithOverwriteableTemplateName;
import common.hooks.SunrisePageDataHook;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.Injector;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;
import productcatalog.hooks.ProductProjectionPagedSearchResultHook;
import productcatalog.hooks.ProductProjectionSearchFilterHook;
import productcatalog.hooks.SingleCategoryHook;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseProductOverviewPageController extends SunriseFrameworkController implements WithOverwriteableTemplateName {

    private static final Logger LOGGER = LoggerFactory.getLogger(SunriseProductOverviewPageController.class);

    @Inject
    private UserContext userContext;
    @Inject
    private CategoryTree categoryTree;
    @Inject
    private Injector injector;
    @Inject
    private ProductOverviewPageContentFactory productOverviewPageContentFactory;
    @Inject
    private ProductListFetchSimple productListFetchSimple;

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
        return new HashSet<>(asList("product-overview-page", "product-catalog", "search", "product", "category"));
    }

    public CompletionStage<Result> searchProductsByCategorySlug(final String languageTag, final String categorySlug) {
        return doRequest(() -> {
            this.categorySlug = categorySlug;
            final Optional<Category> category = categoryTree.findBySlug(userContext.locale(), categorySlug);
            if (category.isPresent()) {
                this.category = category.get();
                return runAsyncHook(SingleCategoryHook.class, hook -> hook.onSingleCategoryLoaded(category.get()))
                        .thenComposeAsync(unused -> searchProducts(), HttpExecution.defaultContext());
            } else {
                return handleNotFoundCategory();
            }
        });
    }

    public CompletionStage<Result> searchProductsBySearchTerm(final String languageTag) {
        return searchProducts();
    }

    protected CompletionStage<Result> searchProducts() {
        return injector.instanceOf(ProductListFetchSimple.class).searchProducts(null, this::filter)
                .thenComposeAsync(this::listProducts, HttpExecution.defaultContext());
    }

    protected CompletionStage<Result> listProducts(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        if (pagedSearchResult.getResults().isEmpty()) {
            return handleEmptySearch(pagedSearchResult);
        } else {
            return handleFoundProductsAndCallingHooks(pagedSearchResult);
        }
    }

    protected CompletionStage<Result> handleFoundProducts(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        final ProductOverviewPageContent pageContent = productOverviewPageContentFactory.create(getCategory().orElse(null), pagedSearchResult);
        return completedFuture(ok(renderPage(pageContent)));
    }

    protected CompletionStage<Result> handleNotFoundCategory() {
        return completedFuture(notFoundCategoryResult());
    }

    protected CompletionStage<Result> handleEmptySearch(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        final ProductOverviewPageContent pageContent = productOverviewPageContentFactory.create(getCategory().orElse(null), pagedSearchResult);
        return completedFuture(ok(renderPage(pageContent)));
    }

    protected Result notFoundCategoryResult() {
        return notFound("Category not found: " + getCategorySlug().orElse("[unknown]"));
    }

    protected Html renderPage(final ProductOverviewPageContent pageContent) {
        final SunrisePageData pageData = pageData(pageContent);
        runVoidHook(SunrisePageDataHook.class, hook -> hook.acceptSunrisePageData(pageData));
        return templateEngine().renderToHtml(getTemplateName(), pageData, userContext.locales());
    }

    protected final CompletionStage<Result> handleFoundProductsAndCallingHooks(final PagedSearchResult<ProductProjection> pagedSearchResult) {
        runVoidHook(ProductProjectionPagedSearchResultHook.class, hook -> hook.acceptProductProjectionPagedSearchResult(pagedSearchResult));
        return handleFoundProducts(pagedSearchResult);
    }

    protected final ProductProjectionSearch filter(ProductProjectionSearch q) {
        return runFilterHook(ProductProjectionSearchFilterHook.class, (hook, r) -> hook.filterProductProjectionSearch(r), q);
    }

    protected final Optional<Category> getCategory() {
        return Optional.ofNullable(category);
    }

    protected final Optional<String> getCategorySlug() {
        return Optional.ofNullable(categorySlug);
    }
}