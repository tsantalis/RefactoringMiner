package search;

import com.google.inject.AbstractModule;
import productcatalog.productoverview.search.SearchConfig;

public class SearchTestModule extends AbstractModule {

    private final SearchConfig searchConfig;

    public SearchTestModule(final SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    @Override
    protected void configure() {
        bind(SearchConfig.class).toInstance(searchConfig);
    }


}
