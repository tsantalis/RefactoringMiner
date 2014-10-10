package gr.uom.java.xmi;
import java.io.File;
import java.util.Collection;
import java.util.List;

import com.sdmetrics.model.MetaModel;
import com.sdmetrics.model.MetaModelElement;
import com.sdmetrics.model.Model;
import com.sdmetrics.model.ModelElement;
import com.sdmetrics.model.XMIReader;
import com.sdmetrics.model.XMITransformations;
import com.sdmetrics.util.XMLParser;

public class XMIModelExtractor {
	private String version;
	
	public XMIModelExtractor(File xmiFile) {
		XMIVersionExtractor versionExtractor = new XMIVersionExtractor(xmiFile);
		this.version = versionExtractor.getVersion();
		
		if(version != null) {
			String metaModelURL = null;
			String xmiTransURL = null;
			if(version.equals("1.2")) {
				metaModelURL = "src/com/sdmetrics/resources/metamodel.xml";
				xmiTransURL = "src/com/sdmetrics/resources/xmiTrans1_1.xml";
			}
			else if(version.equals("2.0") || version.equals("2.1")) {
				metaModelURL = "src/com/sdmetrics/resources/metamodel2.xml";
				xmiTransURL = "src/com/sdmetrics/resources/xmiTrans2_0.xml";
			}
			try {
				XMLParser parser = new XMLParser();
				MetaModel metaModel = new MetaModel();
				parser.parse(metaModelURL, metaModel.getSAXParserHandler());

				XMITransformations trans=new XMITransformations(metaModel);
				parser.parse(xmiTransURL, trans.getSAXParserHandler());

				Model model = new Model(metaModel);
				XMIReader xmiReader = new XMIReader(trans, model);
				parser.parse(xmiFile.getAbsolutePath(), xmiReader);

				MetaModelElement operationType = metaModel.getType("operation");
				List<ModelElement> operationElements = model.getAcceptedElements(operationType);
				for(ModelElement me : operationElements) {
					UMLOperation operation = processOperation(me);
					System.out.println(operation);
				}
				
				/*for(ModelElement me : elements) {
					System.out.println("  Element: " + me.getFullName() + " ");

					// write out the value of each attribute of the element
					Collection<String> attributeNames = type.getAttributeNames();
					for (String attr : attributeNames) {
						System.out.print("     Attribute '" + attr);
						if (type.isSetAttribute(attr))
							System.out.println("' has set value "
									+ me.getSetAttribute(attr));
						else if (type.isRefAttribute(attr)) {
							System.out.print("' references ");
							ModelElement referenced = me.getRefAttribute(attr);
							System.out.println((referenced == null) ? "nothing"
									: referenced.getFullName());
						} else
							System.out.println("' has value: "
									+ me.getPlainAttribute(attr));
					}
				}*/
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private UMLOperation processOperation(ModelElement me) {
		String name = me.getPlainAttribute("name");
		String id = me.getPlainAttribute("id");
		String visibility = me.getPlainAttribute("visibility");
		boolean isAbstract = Boolean.valueOf(me.getPlainAttribute("abstract"));
		ModelElement contextElement = me.getRefAttribute("context");
		String context = contextElement.getFullName();
		
		UMLOperation operation = new UMLOperation(name, id);
		operation.setAbstract(isAbstract);
		operation.setVisibility(visibility);
		operation.setClassName(context);
		
		if(version.equals("2.0") || version.equals("2.1")) {
			Collection<ModelElement> ownedParameters = (Collection<ModelElement>) me.getSetAttribute("ownedparameters");
			for(ModelElement ownedElement : ownedParameters) {
				String parameterName = ownedElement.getPlainAttribute("name");
				//String parameterId = ownedElement.getPlainAttribute("id");
				String kind = ownedElement.getPlainAttribute("kind");
				if(kind == null || kind.equals(""))
					kind = "in";
				
				String type = "void";
				ModelElement parameterTypeElement = ownedElement.getRefAttribute("parametertype");
				if(parameterTypeElement != null)
					type = parameterTypeElement.getFullName();
				
				UMLParameter parameter = new UMLParameter(parameterName, UMLType.extractTypeObject(type), kind);
				operation.addParameter(parameter);
			}
		}
		if(version.equals("1.2")) {
			Collection<ModelElement> ownedElements = me.getOwnedElements();
			for(ModelElement ownedElement : ownedElements) {
				if(ownedElement.getType().getName().equals("parameter")) {
					String parameterName = ownedElement.getPlainAttribute("name");
					//String parameterId = ownedElement.getPlainAttribute("id");
					String kind = ownedElement.getPlainAttribute("kind");
					if(kind == null || kind.equals(""))
						kind = "in";

					String type = "void";
					ModelElement parameterTypeElement = ownedElement.getRefAttribute("parametertype");
					if(parameterTypeElement != null)
						type = parameterTypeElement.getFullName();

					UMLParameter parameter = new UMLParameter(parameterName, UMLType.extractTypeObject(type), kind);
					operation.addParameter(parameter);
				}
			}
		}
		return operation;
	}
}
