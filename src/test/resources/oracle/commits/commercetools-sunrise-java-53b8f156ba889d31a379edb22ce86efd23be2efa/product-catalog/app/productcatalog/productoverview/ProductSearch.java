package productcatalog.productoverview;

import io.sphere.sdk.products.search.ProductProjectionSearch;

import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public interface ProductSearch<C, S> {

    CompletionStage<ProductSearchResult> searchProducts(final C categoryIdentifier, final S searchCriteria, final UnaryOperator<ProductProjectionSearch> filter);
}
