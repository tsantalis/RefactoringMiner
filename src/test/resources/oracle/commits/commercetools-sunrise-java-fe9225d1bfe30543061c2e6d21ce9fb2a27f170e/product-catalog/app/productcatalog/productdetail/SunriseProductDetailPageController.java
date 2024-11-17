package productcatalog.productdetail;

import com.google.inject.Injector;
import common.contexts.UserContext;
import common.controllers.SunriseFrameworkController;
import common.controllers.SunrisePageData;
import common.controllers.WithOverwriteableTemplateName;
import common.hooks.SunrisePageDataHook;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;
import productcatalog.hooks.ProductProjectionSearchFilterHook;
import productcatalog.hooks.SingleProductProjectionHook;
import productcatalog.hooks.SingleProductVariantHook;
import wedecidelatercommon.ProductReverseRouter;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseProductDetailPageController extends SunriseFrameworkController implements WithOverwriteableTemplateName {

    protected static final Logger logger = LoggerFactory.getLogger(SunriseProductDetailPageController.class);

    @Inject
    private UserContext userContext;
    @Inject
    private ProductReverseRouter productReverseRouter;
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
        return new HashSet<>(asList("productdetailpage", "product"));
    }

    public CompletionStage<Result> showProductBySlugAndSku(final String languageTag, final String slug, final String sku) {
        return doRequest(() -> {
            logger.debug("look for product with slug={} in locale={} and sku={}", slug, languageTag, sku);
            this.productSlug = slug;
            this.variantSku = sku;
            return injector.getInstance(ProductFetchBySlugAndSku.class).findProduct(slug, sku, this::filter)
                    .thenComposeAsync(this::showProduct, HttpExecution.defaultContext());
        });
    }

    public CompletionStage<Result> showProductByProductIdAndVariantId(final String languageTag, final String productId, final int variantId) {
        return doRequest(() -> {
            logger.debug("look for product with productId={} and variantId={}", productId, variantId);
            return injector.getInstance(ProductFetchByProductIdAndVariantId.class).findProduct(productId, variantId, this::filter)
                    .thenComposeAsync(this::showProduct, HttpExecution.defaultContext());
        });
    }

    protected CompletionStage<Result> showProduct(final ProductFetchResult productFetchResult) {
        final Optional<ProductProjection> product = productFetchResult.getProduct();
        final Optional<ProductVariant> variant = productFetchResult.getVariant();
        if (product.isPresent() && variant.isPresent()) {
            return handleFoundProductAndCallingHooks(product.get(), variant.get());
        } else if (product.isPresent()) {
            return handleNotFoundVariant(product.get());
        } else {
            return handleNotFoundProduct();
        }
    }

    protected CompletionStage<Result> handleFoundProduct(final ProductProjection product, final ProductVariant variant) {
        final ProductDetailPageContent pageContent = createPageContent(product, variant);
        return completedFuture(ok(renderPage(pageContent)));
    }

    protected CompletionStage<Result> handleNotFoundVariant(final ProductProjection product) {
        return redirectToMasterVariant(product);
    }

    protected CompletionStage<Result> handleNotFoundProduct() {
        if (getProductSlug().isPresent() && getVariantSku().isPresent()) {
            return findNewProductSlug(getProductSlug().get()).thenApplyAsync(newSlugOpt -> newSlugOpt
                    .map(newSlug -> redirectToNewSlug(newSlug, getVariantSku().get()))
                    .orElseGet(this::notFoundProductResult),
                    HttpExecution.defaultContext());
        } else {
            return completedFuture(notFoundProductResult());
        }
    }

    protected Result notFoundProductResult() {
        return notFound();
    }

    protected ProductDetailPageContent createPageContent(final ProductProjection product, final ProductVariant variant) {
        return productDetailPageContentFactory.create(product, variant);
    }

    protected Html renderPage(final ProductDetailPageContent pageContent) {
        final SunrisePageData pageData = pageData(pageContent);
        runVoidHook(SunrisePageDataHook.class, hook -> hook.acceptSunrisePageData(pageData));
        return templateEngine().renderToHtml(getTemplateName(), pageData, userContext.locales());
    }

    protected final ProductProjectionSearch filter(ProductProjectionSearch q) {
        return runFilterHook(ProductProjectionSearchFilterHook.class, (hook, r) -> hook.filterProductProjectionSearch(r), q);
    }

    protected final CompletionStage<Result> handleFoundProductAndCallingHooks(final ProductProjection product, final ProductVariant variant) {
        final CompletionStage<Object> hooksCompletionStage = runAsyncHook(SingleProductProjectionHook.class, hook -> hook.onSingleProductProjectionLoaded(product));
        final CompletionStage<Object> hooksCompletionStage2 = runAsyncHook(SingleProductVariantHook.class, hook -> hook.onSingleProductVariantLoaded(product, variant));
        return hooksCompletionStage2.thenComposeAsync(unused ->
                        hooksCompletionStage.thenComposeAsync(unusedResult ->
                                handleFoundProduct(product, variant), HttpExecution.defaultContext()),
                HttpExecution.defaultContext());
    }

    protected final Optional<String> getProductSlug() {
        return Optional.ofNullable(productSlug);
    }

    protected final Optional<String> getVariantSku() {
        return Optional.ofNullable(variantSku);
    }

    private Result redirectToNewSlug(final String newSlug, final String sku) {
        return movedPermanently(productReverseRouter.productDetailPageCall(userContext.locale().toLanguageTag(), newSlug, sku));
    }

    private CompletionStage<Result> redirectToMasterVariant(final ProductProjection product) {
        return productReverseRouter.productDetailPageCall(userContext.locale(), product, product.getMasterVariant())
                .map(call -> completedFuture(redirect(call)))
                .orElseGet(() -> completedFuture(notFoundProductResult()));
    }

    private CompletionStage<Optional<String>> findNewProductSlug(final String slug) {
        return completedFuture(Optional.empty()); // TODO look for messages and find current slug
    }
}