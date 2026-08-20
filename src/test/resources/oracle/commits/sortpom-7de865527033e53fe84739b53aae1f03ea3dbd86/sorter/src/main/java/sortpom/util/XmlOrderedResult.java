package sortpom.util;

/**
 * This is the result returned from the verify operation. It contains a status and a error message (if applicable)
 *
 * @author bjorn
 * @since 2012-08-11
 */
public final class XmlOrderedResult {
    private final boolean ordered;
    private final String errorMessage;

    private XmlOrderedResult(boolean ordered, String errorMessage) {
        this.ordered = ordered;
        this.errorMessage = errorMessage;
    }

    /** pom file was ordered */
    public static XmlOrderedResult ordered() {
        return new XmlOrderedResult(true, "");
    }

    /** The xml elements was not in the right order */
    public static XmlOrderedResult nameDiffers(String originalElementName, String newElementName) {
        return new XmlOrderedResult(false, String.format("The xml element <%s> should be placed before <%s>",
                newElementName, originalElementName));
    }

    /** The child elements of two elements differ. Example: When dependencies should be sorted */
    public static XmlOrderedResult childElementDiffers(String name, int originalSize, int newSize) {
        return new XmlOrderedResult(false, String.format(
                "The xml element <%s> with %s child elements should be placed before element <%s> with %s child elements",
                name, newSize, name, originalSize));
    }

    /** The texts inside two elements differ. Example: when maven properties should be sorted */
    public static XmlOrderedResult textContentDiffers(String name, String originalElementText, String newElementText) {
        return new XmlOrderedResult(false, String.format("The xml element <%s>%s</%s> should be placed before <%s>%s</%s>",
                name, newElementText, name, name, originalElementText, name));
    }

    public static XmlOrderedResult lineDiffers(int lineNumber, String sortedXmlLine) {
        return new XmlOrderedResult(false, String.format("The line %d is not considered sorted, should be %s",
                lineNumber, sortedXmlLine));
    }

    public static XmlOrderedResult lineSeparatorCharactersDiffer() {
        return new XmlOrderedResult(false, "The line separator characters differ from sorted pom");
    }

    /** Returns true when verification tells that the pom file was sorted */
    public boolean isOrdered() {
        return ordered;
    }

    /** An error message that describes what went wrong with the verification operation */
    public String getErrorMessage() {
        return errorMessage;
    }

}
