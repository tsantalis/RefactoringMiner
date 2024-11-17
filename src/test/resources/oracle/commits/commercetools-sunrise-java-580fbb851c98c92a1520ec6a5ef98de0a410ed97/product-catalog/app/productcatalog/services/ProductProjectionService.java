package productcatalog.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.PlayJavaSphereClient;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.PagedResult;
import play.libs.F;

import javax.inject.Inject;
import java.util.*;

public class ProductProjectionService {

    private final PlayJavaSphereClient sphere;

    @Inject
    public ProductProjectionService(final PlayJavaSphereClient sphere) {
        this.sphere = sphere;
    }

    public F.Promise<List<ProductProjection>> searchProducts(final int page, final int pageSize) {
        final int offset = (page - 1) * pageSize;
        return sphere.execute(ProductProjectionSearch.ofCurrent()
                .withOffset(offset)
                .withLimit(pageSize))
                .map(PagedResult::getResults);
    }

    public F.Promise<Optional<ProductProjection>> searchProductBySlug(final Locale locale, final String slug) {
        return sphere.execute(ProductProjectionQuery.ofCurrent().bySlug(locale, slug))
                .map(PagedQueryResult::head);
    }

    public Optional<ProductVariant> findVariantBySku(final ProductProjection product, final String sku) {
        return product.getAllVariants().stream()
                .filter(variant -> variantHasSku(variant, sku))
                .findFirst();
    }

    public F.Promise<List<ProductProjection>> getSuggestions(final List<Category> categories, final int numSuggestions) {
        final ProductProjectionQuery productProjectionQuery = ProductProjectionQuery.ofCurrent()
                .withPredicate(p -> p.categories().isIn(categories));

        return sphere.execute(productProjectionQuery)
                .map(PagedQueryResult::getResults)
                .map(results -> pickNRandom(results, numSuggestions));
    }

    private boolean variantHasSku(final ProductVariant variant, final String sku) {
        return variant.getSku().map(variantSku -> variantSku.equals(sku)).orElse(false);
    }

    private <T> List<T> pickNRandom(final List<T> elements, final int n) {
        if(elements.size() < n) {
            return pickNRandom(elements, elements.size());
        }

        final List<T> picked = new ArrayList<>();
        final Random random = new Random();

        for(int i = 0; i < n; i++) {
            pick(elements, picked, random.nextInt(elements.size()));
        }

        return picked;
    }

    private <T> void pick(final List<T> elements, final List<T> picked, int index) {
        picked.add(elements.get(index));
        elements.remove(index);
    }
}
