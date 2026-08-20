package sortpom.parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * The plugin parameter 'sortDependencies' is parsed by this class to determine if dependencies should be sorted
 * and in that case in which order.
 *
 * @author bjorn
 * @since 2012-09-14
 */
public class DependencySortOrder {
    private final String childElementNameList;
    private Collection<String> childElementNames;

    /**
     * Create an instance of the DependencySortOrder
     *
     * @param childElementNameList the plugin parameter argument
     */
    public DependencySortOrder(String childElementNameList) {
        this.childElementNameList = childElementNameList == null ? "" : childElementNameList;
    }

    /**
     * Gets a list of which elements the dependencies should be sorted after. Example:
     * If the list returns "scope, groupId" then the dependencies should be sorted first by scope and then by
     * groupId.
     *
     * @return a list of xml element names
     */
    public Collection<String> getChildElementNames() {
        if (childElementNames == null) {
            childElementNames = Collections.unmodifiableList(Arrays.asList(parseChildElementNameList()));
        }
        return childElementNames;
    }

    private String[] parseChildElementNameList() {
        if (isDeprecatedValueFalse()) {
            return new String[0];
        }
        if (isDeprecatedValueTrue()) {
            return new String[]{"groupId", "artifactId"};
        }
        String list = childElementNameList.replaceAll("\\s", "");
        if (list.isEmpty()) {
            return new String[0];
        }
        return list.split(";|,|:");
    }

    /** Earlier versions only accepted the values 'true' and 'false' as parameter values */
    public boolean isDeprecatedValueTrue() {
        return "true".equalsIgnoreCase(childElementNameList);
    }

    /** Earlier versions only accepted the values 'true' and 'false' as parameter values */
    public boolean isDeprecatedValueFalse() {
        return "false".equalsIgnoreCase(childElementNameList);
    }

    /** If the dependencies should be unsorted */
    public boolean isNoSorting() {
        return getChildElementNames().isEmpty();
    }

    @Override
    public String toString() {
        return "DependencySortOrder{" +
                "childElementNames=" + getChildElementNames() +
                '}';
    }
}
