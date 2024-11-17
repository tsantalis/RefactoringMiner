package productcatalog.productoverview;

import common.contexts.RequestContext;
import common.contexts.UserContext;
import common.models.DetailData;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.search.PagedSearchResult;
import productcatalog.common.BreadcrumbBeanFactory;
import productcatalog.common.ProductListBeanFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;

public class ProductOverviewPageContentFactory {

    @Inject
    private UserContext userContext;
    @Inject
    private CategoryTree categoryTree;
    @Inject
    private RequestContext requestContext;
    @Inject
    private BreadcrumbBeanFactory breadcrumbBeanFactory;
    @Inject
    private ProductListBeanFactory productListBeanFactory;

    public ProductOverviewPageContent create(@Nullable final Category category, final PagedSearchResult<ProductProjection> searchResult) {
        final ProductOverviewPageContent content = new ProductOverviewPageContent();
        content.setProducts(productListBeanFactory.create(searchResult.getResults()));
        content.setFilterProductsUrl(requestContext.getPath());
        if (category != null) {
            content.setBreadcrumb(breadcrumbBeanFactory.create(category));
            content.setAdditionalTitle(category.getName().find(userContext.locales()).orElse(""));
            content.setJumbotron(createJumbotron(category));
            content.setBanner(createBanner(category));
            content.setSeo(createSeo(category));
        }
        return content;
    }

    private BannerBean createBanner(final Category category) {
        final BannerBean bean = new BannerBean();
        bean.setTitle(category.getName().find(userContext.locales()).orElse(""));
        Optional.ofNullable(category.getDescription())
                .ifPresent(description -> bean.setDescription(description.find(userContext.locales()).orElse("")));
        bean.setImageMobile("/assets/img/banner_mobile-0a9241da249091a023ecfadde951a53b.jpg"); // TODO obtain from category?
        bean.setImageDesktop("/assets/img/banner_desktop-9ffd148c48068ce2666d6533b4a87d11.jpg"); // TODO obtain from category?
        return bean;
    }

    private JumbotronBean createJumbotron(final Category category) {
        final JumbotronBean bean = new JumbotronBean();
        bean.setTitle(category.getName().find(userContext.locales()).orElse(""));
        Optional.ofNullable(category.getParent())
                .ifPresent(parentRef -> categoryTree.findById(parentRef.getId())
                        .ifPresent(parent -> bean.setSubtitle(parent.getName().find(userContext.locales()).orElse(""))));
        Optional.ofNullable(category.getDescription())
                .ifPresent(description -> bean.setDescription(description.find(userContext.locales()).orElse("")));
        return bean;
    }

    public DetailData createSeo(final Category category) {
        final DetailData bean = new DetailData();
        Optional.ofNullable(category.getMetaTitle())
                .ifPresent(title -> bean.setTitle(title.find(userContext.locales()).orElse("")));
        Optional.ofNullable(category.getMetaDescription())
                .ifPresent(description -> bean.setDescription(description.find(userContext.locales()).orElse("")));
        return bean;
    }
}
