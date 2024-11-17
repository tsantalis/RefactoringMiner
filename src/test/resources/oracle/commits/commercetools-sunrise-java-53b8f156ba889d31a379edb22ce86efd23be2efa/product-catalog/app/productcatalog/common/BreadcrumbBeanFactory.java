package productcatalog.common;

import com.google.inject.Inject;
import common.contexts.UserContext;
import common.models.LinkBean;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.models.Base;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import wedecidelatercommon.ProductReverseRouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class BreadcrumbBeanFactory extends Base {

    @Inject
    protected UserContext userContext;
    @Inject
    protected CategoryTree categoryTree;
    @Inject
    protected ProductReverseRouter productReverseRouter;

    public BreadcrumbBean create(final ProductProjection product, final ProductVariant variant) {
        final BreadcrumbBean breadcrumbBean = new BreadcrumbBean();
        final List<LinkBean> linkBeans = createLinkBeanList(product, variant);
        fillLinks(breadcrumbBean, linkBeans);
        return breadcrumbBean;
    }

    public BreadcrumbBean create(final Category category) {
        final BreadcrumbBean breadcrumbBean = new BreadcrumbBean();
        fillLinks(breadcrumbBean, createCategoryTreeLinks(category));
        return breadcrumbBean;
    }

    protected List<LinkBean> createLinkBeanList(final ProductProjection product, final ProductVariant variant) {
        final List<LinkBean> linkBeans = createCategoryLinkBeanList(product);
        final LinkBean productLinkData = createProductLinkData(product, variant);
        final List<LinkBean> result = new ArrayList<>(1 + linkBeans.size());
        result.addAll(linkBeans);
        result.add(productLinkData);
        return result;
    }

    protected List<LinkBean> createCategoryLinkBeanList(final ProductProjection product) {
        return product.getCategories().stream()
                    .findFirst()
                    .flatMap(ref -> categoryTree.findById(ref.getId())
                            .map(this::createCategoryTreeLinks))
                    .orElseGet(Collections::emptyList);
    }

    protected void fillLinks(final BreadcrumbBean breadcrumbBean, final List<LinkBean> linkBeans) {
        breadcrumbBean.setLinks(linkBeans);
    }

    public BreadcrumbBean create(final String searchTerm) {
        final BreadcrumbBean breadcrumbBean = new BreadcrumbBean();
        final LinkBean linkBean = new LinkBean();
        linkBean.setText(searchTerm);
        fillLinks(breadcrumbBean, singletonList(linkBean));
        return breadcrumbBean;
    }

    protected List<LinkBean> createCategoryTreeLinks(final Category category) {
        return getCategoryWithAncestors(category).stream()
                .map(this::createCategoryLinkData)
                .collect(toList());
    }

    protected LinkBean createCategoryLinkData(final Category category) {
        final LinkBean linkBean = new LinkBean();
        linkBean.setText(category.getName().find(userContext.locales()).orElse(""));
        linkBean.setUrl(productReverseRouter.productOverviewPageUrlOrEmpty(userContext.locale(), category));
        return linkBean;
    }

    protected LinkBean createProductLinkData(final ProductProjection currentProduct, final ProductVariant variant) {
        final LinkBean linkBean = new LinkBean();
        linkBean.setText(currentProduct.getName().find(userContext.locales()).orElse(""));
        linkBean.setUrl(productReverseRouter.productDetailPageUrlOrEmpty(userContext.locale(), currentProduct, variant));
        return linkBean;
    }

    protected List<Category> getCategoryWithAncestors(final Category category) {
        final List<Category> ancestors = category.getAncestors().stream()
                .map(ref -> categoryTree.findById(ref.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        ancestors.add(category);
        return ancestors;
    }
}
