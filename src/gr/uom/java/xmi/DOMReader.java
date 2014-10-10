package gr.uom.java.xmi;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.Namespace;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class DOMReader {
    private Namespace namespace;
    private String version;
    private List<UMLParameter> entireParameterList = new ArrayList<UMLParameter>();
    private List<UMLAttribute> entireAttributeList = new ArrayList<UMLAttribute>();

    public UMLModel read(File xmlFile) throws IOException, JDOMException {
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
        	List namespaces = root.getAdditionalNamespaces();
        	namespace = (Namespace)namespaces.get(0);
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
        		List namespaces = xmiRoot.getAdditionalNamespaces();
        		namespace = (Namespace)namespaces.get(0);
        		root = xmiRoot;
        	}
        }

        //Element xmiHeader = root.getChild("XMI.header");
        //XMIHeader header = parseHeader(xmiHeader);

        Element xmiContent = root.getChild("XMI.content");
        if(xmiContent != null)
        	return parseContent(xmiContent);
        else
        	return new UMLModel();
    }

    private XMIHeader parseHeader(Element xmiHeader) {
        Element xmiDocumentation = xmiHeader.getChild("XMI.documentation");
        Element xmiExporter = xmiDocumentation.getChild("XMI.exporter");
        Element xmiExporterVersion = xmiDocumentation.getChild("XMI.exporterVersion");

        Element xmiMetamodel = xmiHeader.getChild("XMI.metamodel");

        return new XMIHeader(xmiExporter.getValue(), xmiExporterVersion.getText(),
            xmiMetamodel.getAttributeValue("xmi.name"), xmiMetamodel.getAttributeValue("xmi.version"));
    }

	private UMLModel parseContent(Element xmiContent) {
        Element modelElement = xmiContent.getChild("Model",namespace);
        Element namespaceOwnedElement = modelElement.getChild("Namespace.ownedElement",namespace);
        UMLModel umlModel = new UMLModel();
        
        processClassDiagram(namespaceOwnedElement, "", umlModel);
        
        processCollaborations(namespaceOwnedElement, umlModel);
        
        for(UMLAttribute attribute : entireAttributeList) {
        	String typeID = attribute.getType().getClassType();
        	String typeClassName = umlModel.getElementName(typeID);
        	if(typeClassName != null) {
        		UMLType type = UMLType.extractTypeObject(typeClassName);
        		attribute.setType(type);
        	}
        }
        
        for(UMLParameter parameter : entireParameterList) {
        	String typeID = parameter.getType().getClassType();
        	String typeClassName = umlModel.getElementName(typeID);
        	if(typeClassName != null) {
        		UMLType type = UMLType.extractTypeObject(typeClassName);
        		parameter.setType(type);
        	}
        }
        
        for(UMLDependency dependency : umlModel.getDependencyList()) {
        	String clientID = dependency.getClient();
        	String clientClassName = umlModel.getElementName(clientID);
        	if(clientClassName != null)
        		dependency.setClient(clientClassName);
        	String supplierID = dependency.getSupplier();
        	String supplierClassName = umlModel.getElementName(supplierID);
        	if(supplierClassName != null)
        		dependency.setSupplier(supplierClassName);
        }
        
        for(UMLGeneralization generalization : umlModel.getGeneralizationList()) {
        	String childID = generalization.getChild();
        	String childClassName = umlModel.getElementName(childID);
        	UMLClass childClass = null;
        	if(childClassName != null) {
        		generalization.setChild(childClassName);
        		childClass = umlModel.getClass(childClassName);
        	}
        	String parentID = generalization.getParent();
        	String parentClassName = umlModel.getElementName(parentID);
        	if(parentClassName != null) {
        		generalization.setParent(parentClassName);
        		if(childClass != null)
        			childClass.setSuperclass(UMLType.extractTypeObject(parentClassName));
        	}
        }
        
        for(UMLRealization realization : umlModel.getRealizationList()) {
        	String clientID = realization.getClient();
        	String clientClassName = umlModel.getElementName(clientID);
        	if(clientClassName != null)
        		realization.setClient(clientClassName);
        	String supplierID = realization.getSupplier();
        	String supplierClassName = umlModel.getElementName(supplierID);
        	if(supplierClassName != null)
        		realization.setSupplier(supplierClassName);
        }
        
        for(UMLAssociation association : umlModel.getAssociationList()) {
        	UMLAssociationEnd end1 = association.getEnd1();
        	String endID1 = end1.getParticipant();
        	String endClassName1 = umlModel.getElementName(endID1);
        	if(endClassName1 != null)
        		end1.setParticipant(endClassName1);
        	UMLAssociationEnd end2 = association.getEnd2();
        	String endID2 = end2.getParticipant();
        	String endClassName2 = umlModel.getElementName(endID2);
        	if(endClassName2 != null)
        		end2.setParticipant(endClassName2);
        }
        
        for(UMLInclude include : umlModel.getIncludeList()) {
        	String additionID = include.getAddition();
        	String additionName = umlModel.getElementName(additionID);
        	if(additionName != null)
        		include.setAddition(additionName);
        	String baseID = include.getBase();
        	String baseName = umlModel.getElementName(baseID);
        	if(baseName != null)
        		include.setBase(baseName);
        }
        
        for(UMLExtend extend : umlModel.getExtendList()) {
        	String extensionID = extend.getExtension();
        	String extensionName = umlModel.getElementName(extensionID);
        	if(extensionName != null)
        		extend.setExtension(extensionName);
        	String baseID = extend.getBase();
        	String baseName = umlModel.getElementName(baseID);
        	if(baseName != null)
        		extend.setBase(baseName);
        }
        
        for(UMLActor actor : umlModel.getActorList()) {
        	List<String> generalizationXmiIdList = actor.getGeneralizationXmiIdList();
        	for(String xmiID : generalizationXmiIdList) {
        		UMLGeneralization generalization = umlModel.getGeneralization(xmiID);
        		if(generalization != null) {
        			actor.addGeneralization(generalization);
        		}
        	}
        	List<String> dependencyXmiIdList = actor.getDependencyXmiIdList();
        	for(String xmiID : dependencyXmiIdList) {
        		UMLDependency dependency = umlModel.getDependency(xmiID);
        		if(dependency != null) {
        			actor.addDependency(dependency);
        		}
        	}
        }
        
        for(UMLUseCase useCase : umlModel.getUseCaseList()) {
        	List<String> includeXmiIdList = useCase.getIncludeXmiIdList();
        	for(String xmiID : includeXmiIdList) {
        		UMLInclude include = umlModel.getInclude(xmiID);
        		if(include != null) {
        			useCase.addInclude(include);
        		}
        	}
        	List<String> extendXmiIdList = useCase.getExtendXmiIdList();
        	for(String xmiID : extendXmiIdList) {
        		UMLExtend extend = umlModel.getExtend(xmiID);
        		if(extend != null) {
        			useCase.addExtend(extend);
        		}
        	}
        	List<String> generalizationXmiIdList = useCase.getGeneralizationXmiIdList();
        	for(String xmiID : generalizationXmiIdList) {
        		UMLGeneralization generalization = umlModel.getGeneralization(xmiID);
        		if(generalization != null) {
        			useCase.addGeneralization(generalization);
        		}
        	}
        	List<String> dependencyXmiIdList = useCase.getDependencyXmiIdList();
        	for(String xmiID : dependencyXmiIdList) {
        		UMLDependency dependency = umlModel.getDependency(xmiID);
        		if(dependency != null) {
        			useCase.addDependency(dependency);
        		}
        	}
        }
        return umlModel;
    }

	@SuppressWarnings("unchecked")
	private void processClassDiagram(Element namespaceOwnedElement, String packageName, UMLModel umlModel) {
		List<Element> classList = namespaceOwnedElement.getChildren("Class",namespace);
        List<Element> interfaceList = namespaceOwnedElement.getChildren("Interface",namespace);
        List<Element> classAndInterfaceList = new ArrayList<Element>();
        classAndInterfaceList.addAll(classList);
        classAndInterfaceList.addAll(interfaceList);
        ListIterator<Element> classAndInterfaceIt = classAndInterfaceList.listIterator();
        
        while(classAndInterfaceIt.hasNext()) {
            Element classElement = classAndInterfaceIt.next();
            processClassElement(classElement, packageName, umlModel);
        }
        
        List<Element> dataTypeList = namespaceOwnedElement.getChildren("DataType",namespace);
        ListIterator<Element> dataTypeIt = dataTypeList.listIterator();
        while(dataTypeIt.hasNext()) {
        	Element dataTypeElement = dataTypeIt.next();
        	String dataTypeName = dataTypeElement.getAttributeValue("name");
            String xmiID = dataTypeElement.getAttributeValue("xmi.id");
            UMLDataType umlDataType = new UMLDataType(dataTypeName, xmiID);
            umlModel.addDataType(umlDataType);
        }
        
        List<Element> packageList = namespaceOwnedElement.getChildren("Package",namespace);
        ListIterator<Element> packageIt = packageList.listIterator();
        while(packageIt.hasNext()) {
        	Element packageElement = packageIt.next();
        	String innerPackageName = packageElement.getAttributeValue("name");
        	String fullPackageName;
        	if(packageName.equals(""))
        		fullPackageName = innerPackageName;
        	else
        		fullPackageName = packageName + "." + innerPackageName;
        	Element packageNamespaceOwnedElement = packageElement.getChild("Namespace.ownedElement",namespace);
        	processClassDiagram(packageNamespaceOwnedElement, fullPackageName, umlModel);
        }
        
        List<Element> actorList = namespaceOwnedElement.getChildren("Actor",namespace);
        ListIterator<Element> actorIterator = actorList.listIterator();
        while(actorIterator.hasNext()) {
        	Element actorElement = actorIterator.next();
        	String actorName = actorElement.getAttributeValue("name");
        	String xmiID = actorElement.getAttributeValue("xmi.id");
        	UMLActor actor = new UMLActor(actorName, xmiID);
        	umlModel.addActor(actor);
        	
        	Element generalizableElement = actorElement.getChild("GeneralizableElement.generalization",namespace);
        	if(generalizableElement != null) {
        		List<Element> generalizationList = generalizableElement.getChildren("Generalization",namespace);
        		ListIterator<Element> generalizationIterator = generalizationList.listIterator();
                while(generalizationIterator.hasNext()) {
                	Element generalizationElement = generalizationIterator.next();
                	String generalizationID = generalizationElement.getAttributeValue("xmi.idref");
                	actor.addGeneralizationXmiID(generalizationID);
                }
        	}
        	
        	Element clientDependencyElement = actorElement.getChild("ModelElement.clientDependency",namespace);
        	if(clientDependencyElement != null) {
        		List<Element> clientDependencyList = clientDependencyElement.getChildren("Dependency",namespace);
        		ListIterator<Element> clientDependencyIterator = clientDependencyList.listIterator();
        		while(clientDependencyIterator.hasNext()) {
        			Element dependencyElement = clientDependencyIterator.next();
        			String dependencyID = dependencyElement.getAttributeValue("xmi.idref");
        			actor.addDependencyXmiID(dependencyID);
        		}
        	}
        	
        	Element innerNamespaceOwnedElement = actorElement.getChild("Namespace.ownedElement",namespace);
    		if(innerNamespaceOwnedElement != null) {
    			List<Element> dependencyList = innerNamespaceOwnedElement.getChildren("Dependency",namespace);
    			ListIterator<Element> dependencyIt = dependencyList.listIterator();
    			while(dependencyIt.hasNext()) {
    				Element dependencyElement = dependencyIt.next();
    				String dependencyID = dependencyElement.getAttributeValue("xmi.id");
    				Element dependencyClientElement = dependencyElement.getChild("Dependency.client",namespace);
    				String clientID = getClassOrInterfaceIdRef(dependencyClientElement);

    				Element dependencySupplierElement = dependencyElement.getChild("Dependency.supplier",namespace);
    				String supplierID = getClassOrInterfaceIdRef(dependencySupplierElement);

    				if(clientID != null && supplierID != null) {
    					UMLDependency umlDependency = new UMLDependency(clientID, supplierID);
    					umlDependency.setXmiID(dependencyID);
    					umlModel.addDependency(umlDependency);
    				}
    			}
    		}
        }
        
        List<Element> useCaseList = namespaceOwnedElement.getChildren("UseCase",namespace);
        ListIterator<Element> useCaseIterator = useCaseList.listIterator();
        while(useCaseIterator.hasNext()) {
        	Element useCaseElement = useCaseIterator.next();
        	String useCaseName = useCaseElement.getAttributeValue("name");
        	String xmiID = useCaseElement.getAttributeValue("xmi.id");
        	UMLUseCase useCase = new UMLUseCase(useCaseName, xmiID);
        	umlModel.addUseCase(useCase);
        	
        	Element useCaseExtendElement = useCaseElement.getChild("UseCase.extend",namespace);
        	if(useCaseExtendElement != null) {
        		List<Element> extendList = useCaseExtendElement.getChildren("Extend",namespace);
        		ListIterator<Element> extendIterator = extendList.listIterator();
        		while(extendIterator.hasNext()) {
        			Element extendElement = extendIterator.next();
        			String extendID = extendElement.getAttributeValue("xmi.idref");
        			useCase.addExtendXmiID(extendID);
        		}
        	}
        	
        	Element useCaseIncludeElement = useCaseElement.getChild("UseCase.include",namespace);
        	if(useCaseIncludeElement != null) {
        		List<Element> includeList = useCaseIncludeElement.getChildren("Include",namespace);
        		ListIterator<Element> includeIterator = includeList.listIterator();
        		while(includeIterator.hasNext()) {
        			Element includeElement = includeIterator.next();
        			String includeID = includeElement.getAttributeValue("xmi.idref");
        			useCase.addIncludeXmiID(includeID);
        		}
        	}
        	
        	Element useCaseGeneralizationElement = useCaseElement.getChild("GeneralizableElement.generalization",namespace);
        	if(useCaseGeneralizationElement != null) {
        		List<Element> generalizationList = useCaseGeneralizationElement.getChildren("Generalization",namespace);
        		ListIterator<Element> generalizationIterator = generalizationList.listIterator();
                while(generalizationIterator.hasNext()) {
                	Element generalizationElement = generalizationIterator.next();
                	String generalizationID = generalizationElement.getAttributeValue("xmi.idref");
                	useCase.addGeneralizationXmiID(generalizationID);
                }
        	}
        	
        	Element clientDependencyElement = useCaseElement.getChild("ModelElement.clientDependency",namespace);
        	if(clientDependencyElement != null) {
        		List<Element> clientDependencyList = clientDependencyElement.getChildren("Dependency",namespace);
        		ListIterator<Element> clientDependencyIterator = clientDependencyList.listIterator();
        		while(clientDependencyIterator.hasNext()) {
        			Element dependencyElement = clientDependencyIterator.next();
        			String dependencyID = dependencyElement.getAttributeValue("xmi.idref");
        			useCase.addDependencyXmiID(dependencyID);
        		}
        	}
        	
        	Element innerNamespaceOwnedElement = useCaseElement.getChild("Namespace.ownedElement",namespace);
    		if(innerNamespaceOwnedElement != null) {
    			List<Element> dependencyList = innerNamespaceOwnedElement.getChildren("Dependency",namespace);
    			ListIterator<Element> dependencyIt = dependencyList.listIterator();
    			while(dependencyIt.hasNext()) {
    				Element dependencyElement = dependencyIt.next();
    				String dependencyID = dependencyElement.getAttributeValue("xmi.id");
    				Element dependencyClientElement = dependencyElement.getChild("Dependency.client",namespace);
    				String clientID = getClassOrInterfaceIdRef(dependencyClientElement);

    				Element dependencySupplierElement = dependencyElement.getChild("Dependency.supplier",namespace);
    				String supplierID = getClassOrInterfaceIdRef(dependencySupplierElement);

    				if(clientID != null && supplierID != null) {
    					UMLDependency umlDependency = new UMLDependency(clientID, supplierID);
    					umlDependency.setXmiID(dependencyID);
    					umlModel.addDependency(umlDependency);
    				}
    			}
    		}
        }
        
        List<Element> includeList = namespaceOwnedElement.getChildren("Include",namespace);
        ListIterator<Element> includeIterator = includeList.listIterator();
        while(includeIterator.hasNext()) {
        	Element includeElement = includeIterator.next();
        	String xmiID = includeElement.getAttributeValue("xmi.id");
        	
        	Element additionElement = includeElement.getChild("Include.addition",namespace);
        	String additionID = getClassOrInterfaceIdRef(additionElement);
        	
        	Element baseElement = includeElement.getChild("Include.base",namespace);
        	String baseID = getClassOrInterfaceIdRef(baseElement);
        	if(additionID != null && baseID != null) {
        		UMLInclude umlInclude = new UMLInclude(xmiID, additionID, baseID);
        		umlModel.addInclude(umlInclude);
        	}
        }
        
        List<Element> extendList = namespaceOwnedElement.getChildren("Extend",namespace);
        ListIterator<Element> extendIterator = extendList.listIterator();
        while(extendIterator.hasNext()) {
        	Element extendElement = extendIterator.next();
        	String xmiID = extendElement.getAttributeValue("xmi.id");
        	
        	Element extensionElement = extendElement.getChild("Extend.extension",namespace);
        	String extensionID = getClassOrInterfaceIdRef(extensionElement);
        	
        	Element baseElement = extendElement.getChild("Extend.base",namespace);
        	String baseID = getClassOrInterfaceIdRef(baseElement);
        	if(extensionID != null && baseID != null) {
        		UMLExtend umlExtend = new UMLExtend(xmiID, extensionID, baseID);
        		umlModel.addExtend(umlExtend);
        	}
        }
        
        List<Element> generalizationList = namespaceOwnedElement.getChildren("Generalization",namespace);
        ListIterator<Element> generalizationIterator = generalizationList.listIterator();
        while(generalizationIterator.hasNext()) {
        	Element generalizationElement = generalizationIterator.next();
        	String xmiID = generalizationElement.getAttributeValue("xmi.id");
        	Element generalizationChildElement = generalizationElement.getChild("Generalization.child",namespace);
            Element generalizationParentElement = generalizationElement.getChild("Generalization.parent",namespace);
            String childID = getClassOrInterfaceIdRef(generalizationChildElement);
            String parentID = getClassOrInterfaceIdRef(generalizationParentElement);
            if(childID != null && parentID != null) {
            	UMLGeneralization umlGeneralization = new UMLGeneralization(childID,parentID);
            	umlGeneralization.setXmiID(xmiID);
            	umlModel.addGeneralization(umlGeneralization);
            }
        }

        List<Element> abstractionList = namespaceOwnedElement.getChildren("Abstraction",namespace);
        ListIterator<Element> abstractionIterator = abstractionList.listIterator();
        while(abstractionIterator.hasNext()) {
        	Element abstractionElement = abstractionIterator.next();
        	Element dependencyClientElement = abstractionElement.getChild("Dependency.client",namespace);
            Element dependencySupplierElement = abstractionElement.getChild("Dependency.supplier",namespace);
            String clientID = getClassOrInterfaceIdRef(dependencyClientElement);
            String supplierID = getClassOrInterfaceIdRef(dependencySupplierElement);
            if(clientID != null && supplierID != null) {
            	UMLRealization umlRealization = new UMLRealization(clientID,supplierID);
            	umlModel.addRealization(umlRealization);
            }
        }

        List<Element> associationList = namespaceOwnedElement.getChildren("Association",namespace);
        ListIterator<Element> associationIt = associationList.listIterator();
        while(associationIt.hasNext()) {
            Element associationElement = associationIt.next();

            Element associationConnectionElement = associationElement.getChild("Association.connection",namespace);
            List<Element> associationEndElementList = associationConnectionElement.getChildren("AssociationEnd",namespace);

            UMLAssociationEnd associationEnd1 = processUMLAssociationEnd(associationEndElementList.get(0), umlModel);
            UMLAssociationEnd associationEnd2 = processUMLAssociationEnd(associationEndElementList.get(1), umlModel);
            if(associationEnd1 != null && associationEnd2 != null) {
            	UMLAssociation association = new UMLAssociation(associationEnd1, associationEnd2);
            	umlModel.addAssociation(association);
            }
        }
	}

	@SuppressWarnings("unchecked")
	private void processClassElement(Element classElement, String packageName, UMLModel umlModel) {
		String className = classElement.getAttributeValue("name");
		String xmiID = classElement.getAttributeValue("xmi.id");
		UMLClass umlClass = new UMLClass(packageName, className, xmiID);
		
		if(classElement.getName().equals("Interface"))
			umlClass.setInterface(true);
		String classVisibility = classElement.getAttributeValue("visibility");
		umlClass.setVisibility(classVisibility);
		String abstractionAttributeValue = classElement.getAttributeValue("isAbstract");
		if(abstractionAttributeValue != null) {
			boolean isAbstract = Boolean.valueOf(abstractionAttributeValue);
			umlClass.setAbstract(isAbstract);
		}
		
		umlModel.addClass(umlClass);
		
		Element innerNamespaceOwnedElement = classElement.getChild("Namespace.ownedElement",namespace);
		if(innerNamespaceOwnedElement != null) {
			List<Element> dependencyList = innerNamespaceOwnedElement.getChildren("Dependency",namespace);
			ListIterator<Element> dependencyIt = dependencyList.listIterator();
			while(dependencyIt.hasNext()) {
				Element dependencyElement = dependencyIt.next();
				String dependencyID = dependencyElement.getAttributeValue("xmi.id");
				Element dependencyClientElement = dependencyElement.getChild("Dependency.client",namespace);
				String clientID = getClassOrInterfaceIdRef(dependencyClientElement);

				Element dependencySupplierElement = dependencyElement.getChild("Dependency.supplier",namespace);
				String supplierID = getClassOrInterfaceIdRef(dependencySupplierElement);

				if(clientID != null && supplierID != null) {
					UMLDependency umlDependency = new UMLDependency(clientID, supplierID);
					umlDependency.setXmiID(dependencyID);
					umlModel.addDependency(umlDependency);
				}
			}
			List<Element> innerClassList = innerNamespaceOwnedElement.getChildren("Class",namespace);
			List<Element> innerInterfaceList = innerNamespaceOwnedElement.getChildren("Interface",namespace);
			List<Element> innerClassAndInterfaceList = new ArrayList<Element>();
			innerClassAndInterfaceList.addAll(innerClassList);
			innerClassAndInterfaceList.addAll(innerInterfaceList);
			ListIterator<Element> innerClassAndInterfaceIt = innerClassAndInterfaceList.listIterator();
			while(innerClassAndInterfaceIt.hasNext()) {
				Element innerClassElement = innerClassAndInterfaceIt.next();
				processClassElement(innerClassElement, umlClass.getName(), umlModel);
			}
		}
		
		Element classifierFeatureElement = classElement.getChild("Classifier.feature",namespace);
		if(classifierFeatureElement != null) {
			List<Element> operationList = classifierFeatureElement.getChildren("Operation",namespace);
			ListIterator<Element> operationIt = operationList.listIterator();
			while(operationIt.hasNext()) {
				Element operationElement = operationIt.next();
				String operationName = operationElement.getAttributeValue("name");
				String operationID = operationElement.getAttributeValue("xmi.id");

				UMLOperation operation = new UMLOperation(operationName, operationID);
				operation.setClassName(umlClass.getName());
				if(operationName.equals(className))
					operation.setConstructor(true);
				
				String operationVisibility = operationElement.getAttributeValue("visibility");
				operation.setVisibility(operationVisibility);
				boolean isAbstract = Boolean.valueOf(operationElement.getAttributeValue("isAbstract"));
				operation.setAbstract(isAbstract);
				
				Element behavioralFeatureParameterElement = operationElement.getChild("BehavioralFeature.parameter",namespace);
				if(behavioralFeatureParameterElement != null) {
					List<Element> parameterList = behavioralFeatureParameterElement.getChildren("Parameter",namespace);
					ListIterator<Element> parameterIt = parameterList.listIterator();
					while(parameterIt.hasNext()) {
						Element parameterElement = parameterIt.next();
						String parameterName = parameterElement.getAttributeValue("name");
						String parameterKind = parameterElement.getAttributeValue("kind");
						String parameterType = "void";

						Element parameterTypeElement  = parameterElement.getChild("Parameter.type",namespace);
						if(parameterTypeElement != null) {
							Element dataType = parameterTypeElement.getChild("DataType",namespace);
							if(dataType != null) {
								String href = dataType.getAttributeValue("href");
								if(href != null) {
									if(href.equals("http://argouml.org/profiles/uml14/default-uml14.xmi#-84-17--56-5-43645a83:11466542d86:-8000:000000000000087E"))
										parameterType = "String";
									else if(href.equals("http://argouml.org/profiles/uml14/default-uml14.xmi#-84-17--56-5-43645a83:11466542d86:-8000:000000000000087C"))
										parameterType = "Integer";
								}
							}
							String classOrInterfaceType = getClassOrInterfaceIdRef(parameterTypeElement);
							if(classOrInterfaceType != null)
								parameterType = classOrInterfaceType;
						}

						UMLParameter umlParameter = new UMLParameter(parameterName, UMLType.extractTypeObject(parameterType), parameterKind);
						entireParameterList.add(umlParameter);
						operation.addParameter(umlParameter);
					}
				}
				umlClass.addOperation(operation);
			}
			
			List<Element> attributeList = classifierFeatureElement.getChildren("Attribute",namespace);
			ListIterator<Element> attributeIt = attributeList.listIterator();
			while(attributeIt.hasNext()) {
				Element attributeElement = attributeIt.next();
				String attributeName = attributeElement.getAttributeValue("name");
				String attributeVisibility = attributeElement.getAttributeValue("visibility");
				String attributeType = null;
				Element structuralFeatureTypeElement = attributeElement.getChild("StructuralFeature.type",namespace);
				if(structuralFeatureTypeElement != null) {
					Element dataType = structuralFeatureTypeElement.getChild("DataType",namespace);
					if(dataType != null) {
						String href = dataType.getAttributeValue("href");
						if(href != null) {
							if(href.equals("http://argouml.org/profiles/uml14/default-uml14.xmi#-84-17--56-5-43645a83:11466542d86:-8000:000000000000087E"))
								attributeType = "String";
							else if(href.equals("http://argouml.org/profiles/uml14/default-uml14.xmi#-84-17--56-5-43645a83:11466542d86:-8000:000000000000087C"))
								attributeType = "Integer";
						}
					}
					String classOrInterfaceType = getClassOrInterfaceIdRef(structuralFeatureTypeElement);
					if(classOrInterfaceType != null)
						attributeType = classOrInterfaceType;
				}
				
				UMLAttribute umlAttribute = new UMLAttribute(attributeName, UMLType.extractTypeObject(attributeType));
				umlAttribute.setClassName(umlClass.getName());
				umlAttribute.setVisibility(attributeVisibility);
				entireAttributeList.add(umlAttribute);
				umlClass.addAttribute(umlAttribute);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processCollaborations(Element namespaceOwnedElement, UMLModel umlModel) {
		List<Element> collaborationList = namespaceOwnedElement.getChildren("Collaboration",namespace);
        ListIterator<Element> collaborationIt = collaborationList.listIterator();
        while(collaborationIt.hasNext()) {
        	Element collaborationElement = collaborationIt.next();
        	String collaborationName = collaborationElement.getAttributeValue("name");
        	String collaborationID = collaborationElement.getAttributeValue("xmi.id");
        	UMLCollaboration umlCollaboration = new UMLCollaboration(collaborationName, collaborationID);
        	
        	Element collaborationNamespaceOwnedElement = collaborationElement.getChild("Namespace.ownedElement",namespace);
        	
        	List<Element> classifierRoleList = collaborationNamespaceOwnedElement.getChildren("ClassifierRole",namespace);
        	ListIterator<Element> classifierRoleIt = classifierRoleList.listIterator();
        	while(classifierRoleIt.hasNext()) {
        		Element classifierRoleElement = classifierRoleIt.next();
        		String classifierRoleName = classifierRoleElement.getAttributeValue("name");
        		String classifierRoleID = classifierRoleElement.getAttributeValue("xmi.id");
        		
        		UMLClassifierRole umlClassifierRole = new UMLClassifierRole(classifierRoleName, classifierRoleID);
        		
        		Element classifierRoleBase = classifierRoleElement.getChild("ClassifierRole.base",namespace);
        		if(classifierRoleBase != null) {
        			String classifierRoleBaseClassID = getClassOrInterfaceIdRef(classifierRoleBase);
        			if(classifierRoleBaseClassID != null) {
        				String classifierRoleBaseType = umlModel.getElementName(classifierRoleBaseClassID);
        				umlClassifierRole.setBase(classifierRoleBaseType);
        			}
        		}
        		umlCollaboration.addClassifierRole(umlClassifierRole);
        	}
        	
        	List<Element> associationRoleList = collaborationNamespaceOwnedElement.getChildren("AssociationRole",namespace);
        	ListIterator<Element> associationRoleIt = associationRoleList.listIterator();
        	while(associationRoleIt.hasNext()) {
        		Element associationRoleElement = associationRoleIt.next();
        		String associationRoleID = associationRoleElement.getAttributeValue("xmi.id");
        		
        		Element associationConnectionElement = associationRoleElement.getChild("Association.connection",namespace);
        		List<Element> associationEndRoleList = associationConnectionElement.getChildren("AssociationEndRole",namespace);
        		UMLAssociationRole associationRole = new UMLAssociationRole(associationRoleID,
                		processUMLAssociationEndRole(associationEndRoleList.get(0), umlCollaboration),
                		processUMLAssociationEndRole(associationEndRoleList.get(1), umlCollaboration));
        		
        		Element associationRoleMessageElement = associationRoleElement.getChild("AssociationRole.message",namespace);
        		if(associationRoleMessageElement != null) {
        			List<Element> associationRoleMessageList = associationRoleMessageElement.getChildren("Message",namespace);
        			ListIterator<Element> associationRoleMessageIt = associationRoleMessageList.listIterator();
        			while(associationRoleMessageIt.hasNext()) {
        				Element messageElement = associationRoleMessageIt.next();
        				String messageID = messageElement.getAttributeValue("xmi.idref");
        				associationRole.addMessageID(messageID);
        			}
        		}
        		umlCollaboration.addAssociationRole(associationRole);
        	}
        	
        	List<Element> callActionList = collaborationNamespaceOwnedElement.getChildren("CallAction",namespace);
        	ListIterator<Element> callActionIt = callActionList.listIterator();
        	while(callActionIt.hasNext()) {
        		Element callActionElement = callActionIt.next();
        		String callActionID = callActionElement.getAttributeValue("xmi.id");
        		UMLCallAction umlCallAction = new UMLCallAction(callActionID);
        		
        		Element callActionOperationElement = callActionElement.getChild("CallAction.operation",namespace);
        		if(callActionOperationElement != null) {
        			Element operationElement = callActionOperationElement.getChild("Operation",namespace);
        			String operationID = operationElement.getAttributeValue("xmi.idref");
        			UMLOperation umlOperation = umlModel.getOperation(operationID);
        			umlCallAction.setOperation(umlOperation);
        		}
        		Element actionScriptElement = callActionElement.getChild("Action.script",namespace);
        		if(actionScriptElement != null) {
        			Element actionExpressionElement = actionScriptElement.getChild("ActionExpression",namespace);
        			String actionExpressionBody = actionExpressionElement.getAttributeValue("body");
        			umlCallAction.setActionExpression(actionExpressionBody);
        		}
        		umlCollaboration.addAction(umlCallAction);
        	}
        	
        	List<Element> createActionList = collaborationNamespaceOwnedElement.getChildren("CreateAction",namespace);
        	ListIterator<Element> createActionIt = createActionList.listIterator();
        	while(createActionIt.hasNext()) {
        		Element createActionElement = createActionIt.next();
        		String createActionID = createActionElement.getAttributeValue("xmi.id");
        		UMLCreateAction umlCreateAction = new UMLCreateAction(createActionID);
        		umlCollaboration.addAction(umlCreateAction);
        	}
        	
        	List<Element> returnActionList = collaborationNamespaceOwnedElement.getChildren("ReturnAction",namespace);
        	ListIterator<Element> returnActionIt = returnActionList.listIterator();
        	while(returnActionIt.hasNext()) {
        		Element returnActionElement = returnActionIt.next();
        		String returnActionID = returnActionElement.getAttributeValue("xmi.id");
        		UMLReturnAction umlReturnAction = new UMLReturnAction(returnActionID);
        		umlCollaboration.addAction(umlReturnAction);
        	}
        	
        	Element collaborationInteractionElement = collaborationElement.getChild("Collaboration.interaction",namespace);
        	Element interactionElement = collaborationInteractionElement.getChild("Interaction",namespace);
        	Element interactionMessageElement = interactionElement.getChild("Interaction.message",namespace);
        	List<Element> messageList = interactionMessageElement.getChildren("Message",namespace);
        	ListIterator<Element> messageIt = messageList.listIterator();
        	while(messageIt.hasNext()) {
        		Element messageElement = messageIt.next();
        		String messageName = messageElement.getAttributeValue("name");
        		String messageID = messageElement.getAttributeValue("xmi.id");
        		String messageActivatorMessageID = null;
        		
        		Element messageActivatorElement = messageElement.getChild("Message.activator",namespace);
        		if(messageActivatorElement != null) {
        			Element messageActivatorMessageElement = messageActivatorElement.getChild("Message",namespace);
        			messageActivatorMessageID = messageActivatorMessageElement.getAttributeValue("xmi.idref");
        		}
        		
        		Element messageSenderElement = messageElement.getChild("Message.sender",namespace);
        		Element messageSenderClassifierRoleElement = messageSenderElement.getChild("ClassifierRole",namespace);
        		String messageSenderClassifierRoleID = messageSenderClassifierRoleElement.getAttributeValue("xmi.idref");
        		UMLClassifierRole messageSenderUMLClassifierRole = umlCollaboration.getClassifierRole(messageSenderClassifierRoleID);
        		
        		Element messageReceiverElement = messageElement.getChild("Message.receiver",namespace);
        		Element messageReceiverClassifierRoleElement = messageReceiverElement.getChild("ClassifierRole",namespace);
        		String messageReceiverClassifierRoleID = messageReceiverClassifierRoleElement.getAttributeValue("xmi.idref");
        		UMLClassifierRole messageReceiverUMLClassifierRole = umlCollaboration.getClassifierRole(messageReceiverClassifierRoleID);
        		
        		Element messageActionElement = messageElement.getChild("Message.action",namespace);
        		UMLAction action = null;
        		
        		Element callActionElement = messageActionElement.getChild("CallAction",namespace);
        		if(callActionElement != null) {
        			String callActionID = callActionElement.getAttributeValue("xmi.idref");
        			action = umlCollaboration.getAction(callActionID);
        		}
        		Element createActionElement = messageActionElement.getChild("CreateAction",namespace);
        		if(createActionElement != null) {
        			String createActionID = createActionElement.getAttributeValue("xmi.idref");
        			action = umlCollaboration.getAction(createActionID);
        		}
        		Element returnActionElement = messageActionElement.getChild("ReturnAction",namespace);
        		if(returnActionElement != null) {
        			String returnActionID = returnActionElement.getAttributeValue("xmi.idref");
        			action = umlCollaboration.getAction(returnActionID);
        		}
        		
        		UMLMessage umlMessage = new UMLMessage(messageID,
    					messageSenderUMLClassifierRole, messageReceiverUMLClassifierRole,
    					messageActivatorMessageID, action);
    			if(messageName != null)
    				umlMessage.setName(messageName);
    			Element messagePredecessorElement = messageElement.getChild("Message.predecessor",namespace);
    			if(messagePredecessorElement != null) {
    				Element messagePredecessorMessageElement = messageActivatorElement.getChild("Message",namespace);
        			String messagePredecessorMessageID = messagePredecessorMessageElement.getAttributeValue("xmi.idref");
        			umlMessage.setPredecessorID(messagePredecessorMessageID);
    			}
    			umlCollaboration.addMessage(umlMessage);
        	}
        	
        	ListIterator<UMLMessage> umlMessageIt = umlCollaboration.getMessageListIterator();
        	while(umlMessageIt.hasNext()) {
        		UMLMessage umlMessage = umlMessageIt.next();
        		String activatorID = umlMessage.getActivatorID();
        		if(activatorID != null) {
        			UMLMessage activatorMessage = umlCollaboration.getMessage(activatorID);
        			if(activatorMessage != null)
        				umlMessage.setActivatorMessage(activatorMessage);
        		}
        		String predecessorID = umlMessage.getPredecessorID();
        		if(predecessorID != null) {
        			UMLMessage predecessorMessage = umlCollaboration.getMessage(predecessorID);
        			if(predecessorMessage != null)
        				umlMessage.setPredecessorMessage(predecessorMessage);
        		}
        	}
        	
        	umlModel.addCollaboration(umlCollaboration);
        }
	}

	private UMLAssociationEnd processUMLAssociationEnd(Element associationEndElement, UMLModel umlModel) {
		Element associationEndParticipantElement = associationEndElement.getChild("AssociationEnd.participant",namespace);
		String participantID = getClassOrInterfaceIdRef(associationEndParticipantElement);
		if(participantID != null) {
			UMLAssociationEnd associationEnd = new UMLAssociationEnd(participantID);

			Element associationEndMultiplicityElement = associationEndElement.getChild("AssociationEnd.multiplicity",namespace);
			if(associationEndMultiplicityElement != null) {
				Element multiplicity = associationEndMultiplicityElement.getChild("Multiplicity",namespace);
				Element multiplicityRange = multiplicity.getChild("Multiplicity.range",namespace);
				Element range = multiplicityRange.getChild("MultiplicityRange",namespace);
				int lower = Integer.valueOf(range.getAttributeValue("lower"));
				int upper = Integer.valueOf(range.getAttributeValue("upper"));
				associationEnd.setMultiplicityLower(lower);
				associationEnd.setMultiplicityUpper(upper);
			}

			String aggregation = associationEndElement.getAttributeValue("aggregation");
			associationEnd.setAggregation(aggregation);
			String name = associationEndElement.getAttributeValue("name");
			associationEnd.setName(name);
			boolean isNavigable = Boolean.valueOf(associationEndElement.getAttributeValue("isNavigable"));
			associationEnd.setNavigable(isNavigable);
			return associationEnd;
		}
		else {
			return null;
		}
	}

	private UMLClassifierRole processUMLAssociationEndRole(Element associationEndRoleElement, UMLCollaboration umlCollaboration) {
		Element associationEndRoleParticipantElement = associationEndRoleElement.getChild("AssociationEnd.participant",namespace);
		Element classifierRoleElement = associationEndRoleParticipantElement.getChild("ClassifierRole",namespace);
		String classifierRoleID = classifierRoleElement.getAttributeValue("xmi.idref");
		return umlCollaboration.getClassifierRole(classifierRoleID);
	}

	private String getClassOrInterfaceIdRef(Element element) {
		String xmiIdRef = null;
		Element classElement = element.getChild("Class",namespace);
		if(classElement != null)
			xmiIdRef = classElement.getAttributeValue("xmi.idref");
		Element interfaceElement = element.getChild("Interface",namespace);
		if(interfaceElement != null)
			xmiIdRef = interfaceElement.getAttributeValue("xmi.idref");
		Element dataTypeElement = element.getChild("DataType",namespace);
		if(dataTypeElement != null)
			xmiIdRef = dataTypeElement.getAttributeValue("xmi.idref");
		Element actorElement = element.getChild("Actor",namespace);
		if(actorElement != null)
			xmiIdRef = actorElement.getAttributeValue("xmi.idref");
		Element useCaseElement = element.getChild("UseCase",namespace);
		if(useCaseElement != null)
			xmiIdRef = useCaseElement.getAttributeValue("xmi.idref");
		return xmiIdRef;
	}
}