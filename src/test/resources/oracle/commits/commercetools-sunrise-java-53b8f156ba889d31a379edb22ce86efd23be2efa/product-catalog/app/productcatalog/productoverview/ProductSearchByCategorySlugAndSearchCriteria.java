package productcatalog.productoverview;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;
import productcatalog.productoverview.search.SearchCriteria;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import static common.utils.LogUtils.logProductRequest;

public class ProductSearchByCategorySlugAndSearchCriteria implements ProductSearch<String, SearchCriteria> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSearchByCategorySlugAndSearchCriteria.class);

    @Inject
    private SphereClient sphereClient;

    public CompletionStage<ProductSearchResult> searchProducts(final String categorySlug, final SearchCriteria searchCriteria, final UnaryOperator<ProductProjectionSearch> filter) {
        return findProducts(searchCriteria, filter)
                .thenApplyAsync(ProductSearchResult::of, HttpExecution.defaultContext());
    }

    /**
     * Gets a list of Products from a PagedQueryResult
     * @param searchCriteria all information regarding the request parameters
     * @return A CompletionStage of a paged result of the search request
     */
    protected CompletionStage<PagedSearchResult<ProductProjection>> findProducts(final SearchCriteria searchCriteria, final UnaryOperator<ProductProjectionSearch> filter) {
        final ProductProjectionSearch request = filter.apply(ProductProjectionSearch.ofCurrent());
        return sphereClient.execute(request)
                .whenCompleteAsync((result, t) -> logProductRequest(LOGGER, request, result), HttpExecution.defaultContext());
    }
}
