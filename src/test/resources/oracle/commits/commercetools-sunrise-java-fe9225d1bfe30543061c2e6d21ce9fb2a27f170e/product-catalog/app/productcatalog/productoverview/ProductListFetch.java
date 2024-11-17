package productcatalog.productoverview;

import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;

import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public interface ProductListFetch<T> {

    CompletionStage<PagedSearchResult<ProductProjection>> searchProducts(final T criteria, final UnaryOperator<ProductProjectionSearch> filter);
}
