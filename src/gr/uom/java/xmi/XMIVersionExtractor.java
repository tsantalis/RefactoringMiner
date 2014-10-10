package gr.uom.java.xmi;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class XMIVersionExtractor {
	private String version;
	
	public XMIVersionExtractor(File xmlFile) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xmlFile);

			Element root = doc.getRootElement();
			if(root.getName().equals("XMI")) {
				String version = null;
				version = root.getAttributeValue("xmi.version");
				if(version != null) {
					this.version = version;
				}
				else {
					version = root.getAttributeValue("version",root.getNamespace());
					if(version != null)
						this.version = version;
				}
			}
			else {
				Element xmiRoot = root.getChild("XMI");
				if(xmiRoot != null) {
					String version = null;
					version = xmiRoot.getAttributeValue("xmi.version");
					if(version != null) {
						this.version = version;
					}
					else {
						version = xmiRoot.getAttributeValue("version",xmiRoot.getNamespace());
						if(version != null)
							this.version = version;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	public String getVersion() {
		return version;
	}
}
