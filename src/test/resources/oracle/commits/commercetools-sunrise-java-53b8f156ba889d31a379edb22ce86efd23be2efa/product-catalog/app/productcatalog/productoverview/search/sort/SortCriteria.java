package productcatalog.productoverview.search.sort;

import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.search.SortExpression;

import java.util.List;

public interface SortCriteria {

    List<SortExpression<ProductProjection>> getSortExpressions();
}
