package productcatalog.productoverview.search;

import io.sphere.sdk.models.LocalizedStringEntry;
import productcatalog.productoverview.search.facetedsearch.FacetedSearchCriteria;
import productcatalog.productoverview.search.productsperpage.ProductsPerPageCriteria;
import productcatalog.productoverview.search.sort.SortCriteria;

import java.util.Optional;

public interface SearchCriteria extends SortCriteria, ProductsPerPageCriteria, FacetedSearchCriteria {

    int getPage();

    Optional<LocalizedStringEntry> getSearchTerm();

}
