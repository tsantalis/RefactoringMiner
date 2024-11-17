package com.commercetools.sunrise.productcatalog.productdetail;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.pages.PageContent;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import com.commercetools.sunrise.hooks.*;
import com.commercetools.sunrise.productcatalog.productsuggestions.ProductSuggestionsControllerComponent;
import com.google.inject.Injector;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Controller to show the information about a single product.
 * Loads one {@link ProductProjection} and the selected {@link ProductVariant}.
 *
 * <p>Components that may be a fit</p>
 * <ul>
 *     <li>{@link ProductSuggestionsControllerComponent}</li>
 * </ul>
 * <p id="hooks">supported hooks</p>
 * <ul>
 *     <li>{@link RequestHook}</li>
 *     <li>{@link PageDataHook}</li>
 *     <li>{@link ProductProjectionSearchFilterHook}</li>
 *     <li>{@link SingleProductProjectionHook}</li>
 *     <li>{@link SingleProductVariantHook}</li>
 * </ul>
 * <p>tags</p>
 * <ul>
 *     <li>product-detail</li>
 *     <li>product</li>
 *     <li>product-catalog</li>
 * </ul>
 */
@RequestScoped
public abstract class SunriseProductDetailController extends SunriseFrameworkController implements WithOverwriteableTemplateName {

    protected static final Logger logger = LoggerFactory.getLogger(SunriseProductDetailController.class);

    @Inject
    private Injector injector;
    @Inject
    protected ProductDetailPageContentFactory productDetailPageContentFactory;

    @Nullable
    private String productSlug;
    @Nullable
    private String variantSku;

    @Override
    public String getTemplateName() {
        return "pdp";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("product-detail", "product", "product-catalog"));
    }

    public CompletionStage<Result> showProductBySlugAndSku(final String languageTag, final String slug, final String sku) {
        return doRequest(() -> {
            logger.debug("look for product with slug={} in locale={} and sku={}", slug, languageTag, sku);
            this.productSlug = slug;
            this.variantSku = sku;
            return injector.getInstance(ProductFinderBySlugAndSku.class).findProduct(slug, sku, this::runHookOnProductSearch)
                    .thenComposeAsync(this::showProduct, HttpExecution.defaultContext());
        });
    }

    public CompletionStage<Result> showProductByProductIdAndVariantId(final String languageTag, final String productId, final int variantId) {
        return doRequest(() -> {
            logger.debug("look for product with productId={} and variantId={}", productId, variantId);
            return injector.getInstance(ProductFinderByProductIdAndVariantId.class).findProduct(productId, variantId, this::runHookOnProductSearch)
                    .thenComposeAsync(this::showProduct, HttpExecution.defaultContext());
        });
    }

    protected CompletionStage<Result> showProduct(final ProductFinderResult productFinderResult) {
        final Optional<ProductProjection> product = productFinderResult.product();
        final Optional<ProductVariant> variant = productFinderResult.variant();
        if (product.isPresent() && variant.isPresent()) {
            runHookOnFoundProduct(product.get(), variant.get());
            return showFoundProduct(product.get(), variant.get());
        } else if (product.isPresent()) {
            return handleNotFoundVariant(product.get());
        } else {
            return handleNotFoundProduct();
        }
    }

    protected CompletionStage<Result> showFoundProduct(final ProductProjection product, final ProductVariant variant) {
        return asyncOk(renderPage(product, variant));
    }

    protected CompletionStage<Result> handleNotFoundVariant(final ProductProjection product) {
        return redirectToMasterVariant(product);
    }

    protected CompletionStage<Result> handleNotFoundProduct() {
        if (productSlug != null && variantSku != null) {
            return findNewProductSlug(productSlug).thenApplyAsync(newSlugOpt -> newSlugOpt
                    .map(newSlug -> redirectToNewSlug(newSlug, variantSku))
                    .orElseGet(this::notFoundProductResult),
                    HttpExecution.defaultContext());
        } else {
            return completedFuture(notFoundProductResult());
        }
    }

    private CompletionStage<Html> renderPage(final ProductProjection product, final ProductVariant variant) {
        final PageContent pageContent = productDetailPageContentFactory.create(product, variant);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    protected Result notFoundProductResult() {
        return notFound();
    }

    protected final ProductProjectionSearch runHookOnProductSearch(final ProductProjectionSearch productSearch) {
        return hooks().runFilterHook(ProductProjectionSearchFilterHook.class, (hook, search) -> hook.filterQuery(search), productSearch);
    }

    protected final void runHookOnFoundProduct(final ProductProjection product, final ProductVariant variant) {
        hooks().runAsyncHook(SingleProductProjectionHook.class, hook -> hook.onSingleProductProjectionLoaded(product));
        hooks().runAsyncHook(SingleProductVariantHook.class, hook -> hook.onSingleProductVariantLoaded(product, variant));
    }

    protected final Optional<String> productSlug() {
        return Optional.ofNullable(productSlug);
    }

    protected final Optional<String> variantSku() {
        return Optional.ofNullable(variantSku);
    }

    private Result redirectToNewSlug(final String newSlug, final String sku) {
        final ProductReverseRouter productReverseRouter = injector.getInstance(ProductReverseRouter.class);
        return movedPermanently(productReverseRouter.productDetailPageCall(userContext().languageTag(), newSlug, sku));
    }

    private CompletionStage<Result> redirectToMasterVariant(final ProductProjection product) {
        final ProductReverseRouter productReverseRouter = injector.getInstance(ProductReverseRouter.class);
        return productReverseRouter.productDetailPageCall(userContext().locale(), product, product.getMasterVariant())
                .map(call -> completedFuture(redirect(call)))
                .orElseGet(() -> completedFuture(notFoundProductResult()));
    }

    private CompletionStage<Optional<String>> findNewProductSlug(final String slug) {
        return completedFuture(Optional.empty()); // TODO look for messages and find current slug
    }
}