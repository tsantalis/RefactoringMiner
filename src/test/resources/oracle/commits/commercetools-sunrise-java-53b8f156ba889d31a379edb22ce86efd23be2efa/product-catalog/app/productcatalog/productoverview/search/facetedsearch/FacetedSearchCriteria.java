package productcatalog.productoverview.search.facetedsearch;

import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.search.FacetedSearchExpression;

import java.util.List;

public interface FacetedSearchCriteria {

    List<FacetedSearchExpression<ProductProjection>> getFacetedSearchExpressions();
}
