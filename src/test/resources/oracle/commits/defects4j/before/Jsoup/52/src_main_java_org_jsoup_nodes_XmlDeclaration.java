package org.jsoup.nodes;

import org.jsoup.helper.Validate;

import java.io.IOException;

/**
 An XML Declaration.

 @author Jonathan Hedley, jonathan@hedley.net */
public class XmlDeclaration extends Node {
    private final String name;
    private final boolean isProcessingInstruction; // <! if true, <? if false, declaration (and last data char should be ?)

    /**
     Create a new XML declaration
     @param name of declaration
     @param baseUri base uri
     @param isProcessingInstruction is processing instruction
     */
    public XmlDeclaration(String name, String baseUri, boolean isProcessingInstruction) {
        super(baseUri);
        Validate.notNull(name);
        this.name = name;
        this.isProcessingInstruction = isProcessingInstruction;
    }

    public String nodeName() {
        return "#declaration";
    }


    /**
     * Get the name of this declaration.
     * @return name of this declaration.
     */
    public String name() {
        return name;
    }

    /**
     Get the unencoded XML declaration.
     @return XML declaration
     */
    public String getWholeDeclaration() {
        final String decl = this.name;
        if(decl.equals("xml") && attributes.size() > 1 ) {
            StringBuilder sb = new StringBuilder(decl);
            final String version = attributes.get("version");
            if( version != null ) {
                sb.append(" version=\"").append(version).append("\"");
            }
            final String encoding = attributes.get("encoding");
            if( encoding != null ) {
                sb.append(" encoding=\"").append(encoding).append("\"");
            }
            return sb.toString();
        }
        else {
            return this.name;
        }
    }

	void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        accum
            .append("<")
            .append(isProcessingInstruction ? "!" : "?")
                .append(getWholeDeclaration())
            .append(">");
    }

	void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {}

    @Override
    public String toString() {
        return outerHtml();
    }
}
