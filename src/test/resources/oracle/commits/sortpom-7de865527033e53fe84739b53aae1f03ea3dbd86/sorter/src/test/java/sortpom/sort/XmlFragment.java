package sortpom.sort;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

public class XmlFragment {
    public static Document createXmlFragment() {
        Document newDocument = new Document();
        newDocument.setRootElement(new Element("Gurka"));
        return newDocument;
    }

    public static Document createXmlProjectFragment() {
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Element rootElement = new Element("project")
                .setNamespace(Namespace.getNamespace("http://maven.apache.org/POM/4.0.0"))
                .setAttribute(
                        "schemaLocation",
                        "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd",
                        xsi);
        rootElement.addNamespaceDeclaration(xsi);
        rootElement.addContent(new Element("Gurka"));

        return new Document().setRootElement(rootElement);
    }

}
