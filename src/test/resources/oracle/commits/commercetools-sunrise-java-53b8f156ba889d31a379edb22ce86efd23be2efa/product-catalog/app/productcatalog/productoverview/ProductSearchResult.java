package productcatalog.productoverview;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.search.PagedSearchResult;

import javax.annotation.Nullable;
import java.util.Optional;

public class ProductSearchResult {

//    @Nullable
//    private final Category category;
    @Nullable
    private final PagedSearchResult<ProductProjection> searchResult;

    public ProductSearchResult(final Category category, final PagedSearchResult<ProductProjection> searchResult) {
//        this.category = category;
        this.searchResult = searchResult;
    }

//    public Optional<Category> getCategory() {
//        return Optional.ofNullable(category);
//    }

    public Optional<PagedSearchResult<ProductProjection>> getPagedSearchResult() {
        return Optional.ofNullable(searchResult);
    }

    public static ProductSearchResult of(final PagedSearchResult<ProductProjection> searchResult) {
        return new ProductSearchResult(null, searchResult);
    }

    public static ProductSearchResult ofNotFoundCategory() {
        return new ProductSearchResult(null, null);
    }
}
