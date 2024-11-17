/*
 * Copyright (c) 2012 Data Harmonisation Panel
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     HUMBOLDT EU Integrated Project #030962
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.io.gml.writer.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geotools.gml3.GML;

import com.vividsolutions.jts.geom.Geometry;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.core.io.IOProvider;
import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOProvider;
import eu.esdihumboldt.hale.common.core.io.impl.SubtaskProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.Locatable;
import eu.esdihumboldt.hale.common.instance.io.impl.AbstractGeoInstanceWriter;
import eu.esdihumboldt.hale.common.instance.io.impl.AbstractInstanceWriter;
import eu.esdihumboldt.hale.common.instance.model.Group;
import eu.esdihumboldt.hale.common.instance.model.Instance;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection;
import eu.esdihumboldt.hale.common.instance.model.ResourceIterator;
import eu.esdihumboldt.hale.common.schema.geometry.CRSDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.DefinitionGroup;
import eu.esdihumboldt.hale.common.schema.model.DefinitionUtil;
import eu.esdihumboldt.hale.common.schema.model.GroupPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.ChoiceFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.NillableFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.AbstractFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.Binding;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.ElementType;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.HasValueFlag;
import eu.esdihumboldt.hale.io.gml.geometry.GMLConstants;
import eu.esdihumboldt.hale.io.gml.internal.simpletype.SimpleTypeUtil;
import eu.esdihumboldt.hale.io.gml.writer.XmlWrapper;
import eu.esdihumboldt.hale.io.gml.writer.XmlWriterBase;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.AbstractTypeMatcher;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.DefinitionPath;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.Descent;
import eu.esdihumboldt.hale.io.gml.writer.internal.geometry.StreamGeometryWriter;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements;
import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
import eu.esdihumboldt.hale.io.xsd.model.XmlIndex;
import eu.esdihumboldt.hale.io.xsd.reader.XmlSchemaReader;
import eu.esdihumboldt.util.Pair;

/**
 * Writes GML/XML using a {@link XMLStreamWriter}
 * 
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
public class StreamGmlWriter extends AbstractGeoInstanceWriter implements XmlWriterBase,
		GMLConstants {

	/**
	 * Schema instance namespace (for specifying schema locations)
	 */
	public static final String SCHEMA_INSTANCE_NS = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI; //$NON-NLS-1$

	private static final ALogger log = ALoggerFactory.getLogger(StreamGmlWriter.class);

	/**
	 * The parameter name for the flag specifying if a geometry should be
	 * simplified before writing it, if possible. Defaults to true.
	 */
	public static final String PARAM_SIMPLIFY_GEOMETRY = "gml.geometry.simplify";

	/**
	 * The parameter name for the flag specifying if the output should be
	 * indented. Defaults to <code>false</code>.
	 */
	public static final String PARAM_PRETTY_PRINT = "xml.pretty";

	/**
	 * The parameter name for the flag specifying an identifier (XML ID) for the
	 * container.
	 */
	public static final String PARAM_CONTAINER_ID = "xml.containerId";

	/**
	 * The parameter name of the flag specifying if nilReason attributes should
	 * be omitted if an element is not nil.
	 */
	public static final String PARAM_OMIT_NIL_REASON = "xml.notNil.omitNilReason";

	/**
	 * The XML stream writer
	 */
	private XMLStreamWriter writer;

	/**
	 * The GML namespace
	 */
	private String gmlNs;

//	/**
//	 * The type index
//	 */
//	private TypeIndex types;

	/**
	 * The geometry writer
	 */
	private StreamGeometryWriter geometryWriter;

	/**
	 * Additional schemas included in the document
	 */
	private final Map<String, Locatable> additionalSchemas = new HashMap<>();
	private final Map<String, String> additionalSchemaPrefixes = new HashMap<>();

	/**
	 * States if a feature collection shall be used
	 */
	private final boolean useFeatureCollection;

	private XmlIndex targetIndex;

	private XmlWrapper documentWrapper;

	/**
	 * Create a GML writer
	 * 
	 * @param useFeatureCollection if a GML feature collection shall be used to
	 *            store the instances (if possible)
	 */
	public StreamGmlWriter(boolean useFeatureCollection) {
		super();
		this.useFeatureCollection = useFeatureCollection;

		addSupportedParameter(PARAM_ROOT_ELEMENT_NAMESPACE);
		addSupportedParameter(PARAM_ROOT_ELEMENT_NAME);
		addSupportedParameter(PARAM_SIMPLIFY_GEOMETRY);
	}

	/**
	 * @return the document wrapper
	 */
	@Nullable
	public XmlWrapper getDocumentWrapper() {
		return documentWrapper;
	}

	/**
	 * @param documentWrapper the document wrapper to set
	 */
	public void setDocumentWrapper(@Nullable XmlWrapper documentWrapper) {
		this.documentWrapper = documentWrapper;
	}

	@Override
	public List<? extends Locatable> getValidationSchemas() {
		List<Locatable> result = new ArrayList<Locatable>();
		result.addAll(super.getValidationSchemas());

		for (Locatable schema : additionalSchemas.values()) {
			result.add(schema);
		}

		return result;
	}

	/**
	 * Add a schema that should be included for validation. Should be called
	 * before or in
	 * {@link #write(InstanceCollection, ProgressIndicator, IOReporter)} prior
	 * to writing the schema locations, but after {@link #init(IOReporter)}
	 * 
	 * @param namespace the schema namespace
	 * @param schema the schema location
	 * @param prefix the desired namespace prefix, may be <code>null</code>
	 */
	protected void addValidationSchema(String namespace, Locatable schema, @Nullable String prefix) {
		additionalSchemas.put(namespace, schema);
		if (prefix != null) {
			additionalSchemaPrefixes.put(namespace, prefix);
		}
	}

	/**
	 * @see AbstractIOProvider#execute(ProgressIndicator, IOReporter)
	 */
	@Override
	protected IOReport execute(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		OutputStream out;
		try {
			out = init(reporter);
		} catch (XMLStreamException e) {
			throw new IOException("Creating the XML stream writer failed", e);
		}

		try {
			write(getInstances(), progress, reporter);
			reporter.setSuccess(reporter.getErrors().isEmpty());
		} catch (Exception e) {
			reporter.error(new IOMessageImpl(e.getLocalizedMessage(), e));
			reporter.setSuccess(false);
		} finally {
			progress.end();
			out.close();
		}

		return reporter;
	}

	// FIXME
//	/**
//	 * @see AbstractInstanceWriter#getValidationSchemas()
//	 */
//	@Override
//	public List<Schema> getValidationSchemas() {
//		List<Schema> result = new ArrayList<Schema>(super.getValidationSchemas());
//		result.addAll(additionalSchemas);
//		return result;
//	}

	@Override
	public boolean isPassthrough() {
		return true;
	}

	/**
	 * @see AbstractInstanceWriter#validate()
	 */
	@Override
	public void validate() throws IOProviderConfigurationException {
		super.validate();

		if (getXMLIndex() == null) {
			fail("No XML target schema");
		}
	}

	/**
	 * @see AbstractInstanceWriter#checkCompatibility()
	 */
	@Override
	public void checkCompatibility() throws IOProviderConfigurationException {
		super.checkCompatibility();

		XmlIndex xmlIndex = getXMLIndex();
		if (xmlIndex == null) {
			fail("No XML target schema");
		}

		if (requiresDefaultContainer()) {
			XmlElement element;
			try {
				element = findDefaultContainter(xmlIndex, null);
			} catch (Exception e) {
				// ignore
				element = null;
			}
			if (element == null) {
				fail("Cannot find container element in schema.");
			}
		}
	}

	/**
	 * States if the instance writer in all cases requires that the default
	 * container is being found.
	 * 
	 * @return if the default container must be present in the target schema
	 */
	protected boolean requiresDefaultContainer() {
		return false; // not needed, we allow specifying it through a parameter
	}

	/**
	 * Get the XML type index.
	 * 
	 * @return the target type index
	 */
	protected XmlIndex getXMLIndex() {
		if (targetIndex == null) {
			targetIndex = getXMLIndex(getTargetSchema());
		}
		return targetIndex;
	}

	/**
	 * Get the XML index from the given schema space
	 * 
	 * @param schemas the schema space
	 * @return the XML index or <code>null</code>
	 */
	public static XmlIndex getXMLIndex(SchemaSpace schemas) {
		// XXX respect a container, types?
		for (Schema schema : schemas.getSchemas()) {
			if (schema instanceof XmlIndex) {
				// TODO respect root element for schema selection?
				return (XmlIndex) schema;
			}
		}

		return null;
	}

	/**
	 * Create and setup the stream writer, the type index and the GML namespace
	 * (Initializes {@link #writer}, {@link #gmlNs} and {@link #targetIndex},
	 * resets {@link #geometryWriter} and {@link #additionalSchemas}).
	 * 
	 * @param reporter the reporter for any errors
	 * 
	 * @return the opened output stream
	 * 
	 * @throws XMLStreamException if creating the {@link XMLStreamWriter} fails
	 * @throws IOException if creating the output stream fails
	 */
	private OutputStream init(IOReporter reporter) throws XMLStreamException, IOException {
		// reset target index
		targetIndex = null;
		// reset geometry writer
		geometryWriter = null;
		// reset additional schemas
		additionalSchemas.clear();
		additionalSchemaPrefixes.clear();

		// create and set-up a writer
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		// will set namespaces if these not set explicitly
		outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", //$NON-NLS-1$
				Boolean.valueOf(true));
		// create XML stream writer with UTF-8 encoding
		OutputStream outStream = getTarget().getOutput();
		XMLStreamWriter tmpWriter = outputFactory.createXMLStreamWriter(outStream, getCharset()
				.name()); //$NON-NLS-1$

		String defNamespace = null;

		XmlIndex index = getXMLIndex();
		// read the namespaces from the map containing namespaces
		if (index.getPrefixes() != null) {
			for (Entry<String, String> entry : index.getPrefixes().entrySet()) {
				if (entry.getValue().isEmpty()) {
					// XXX don't use a default namespace, as this results in
					// problems with schemas w/o elementFormQualified=true
					// defNamespace = entry.getKey();
				}
				else {
					tmpWriter.setPrefix(entry.getValue(), entry.getKey());
				}
			}
		}

		GmlWriterUtil.addNamespace(tmpWriter, SCHEMA_INSTANCE_NS, "xsi"); //$NON-NLS-1$

		// determine default namespace
//		if (defNamespace == null) {
		// XXX don't use a default namespace, as this results in problems
		// with schemas w/o elementFormQualified=true
		// defNamespace = index.getNamespace();

		// TODO remove prefix for target schema namespace?
//		}

		tmpWriter.setDefaultNamespace(defNamespace);

		if (documentWrapper != null) {
			documentWrapper.configure(tmpWriter, reporter);
		}

		// prettyPrint if enabled
		if (isPrettyPrint()) {
			writer = new IndentingXMLStreamWriter(tmpWriter);
		}
		else {
			writer = tmpWriter;
		}

		// determine GML namespace from target schema
		String gml = null;
		if (index.getPrefixes() != null) {
			Set<String> candidates = new TreeSet<>();
			for (String ns : index.getPrefixes().keySet()) {
				if (ns.startsWith(GML_NAMESPACE_CORE)) { //$NON-NLS-1$
					candidates.add(ns);
				}
			}
			if (!candidates.isEmpty()) {
				if (candidates.size() == 1) {
					gml = candidates.iterator().next();
				}
				else {
					log.warn("Multiple candidates for GML namespace found");
					// TODO how to choose the right one?

					// prefer known GML namespaces
					if (candidates.contains(NS_GML_32)) {
						gml = NS_GML_32;
					}
					else if (candidates.contains(NS_GML)) {
						gml = NS_GML;
					}
					else {
						// fall back to first namespace
						gml = candidates.iterator().next();
					}
				}
			}
		}

		if (gml == null) {
			// default to GML 2/3 namespace
			gml = GML.NAMESPACE;
		}

		gmlNs = gml;
		if (log.isDebugEnabled()) {
			log.debug("GML namespace is " + gmlNs); //$NON-NLS-1$
		}

		return outStream;
	}

	/**
	 * @return if the output should be pretty printed
	 */
	public boolean isPrettyPrint() {
		return getParameter(PARAM_PRETTY_PRINT).as(Boolean.class, false);
	}

	/**
	 * Set if the output should be pretty printed.
	 * 
	 * @param prettyPrint <code>true</code> if the output should be indented,
	 *            <code>false</code> otherwise
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		setParameter(PARAM_PRETTY_PRINT, Value.of(prettyPrint));
	}

	/**
	 * @see IOProvider#isCancelable()
	 */
	@Override
	public boolean isCancelable() {
		return true;
	}

	/**
	 * @see AbstractIOProvider#getDefaultTypeName()
	 */
	@Override
	protected String getDefaultTypeName() {
		return "GML/XML";
	}

	/**
	 * Write the given instances.
	 * 
	 * @param instances the instance collection
	 * @param reporter the reporter
	 * @param progress the progress
	 * @throws XMLStreamException if writing the feature collection fails
	 */
	public void write(InstanceCollection instances, ProgressIndicator progress, IOReporter reporter)
			throws XMLStreamException {
		final SubtaskProgressIndicator sub = new SubtaskProgressIndicator(progress) {

			@Override
			protected String getCombinedTaskName(String taskName, String subtaskName) {
				return taskName + " (" + subtaskName + ")";
			}

		};
		progress = sub;

		progress.begin(getTaskName(), instances.size());

		XmlElement container = findDefaultContainter(targetIndex, reporter);

		TypeDefinition containerDefinition = (container == null) ? (null) : (container.getType());
		QName containerName = (container == null) ? (null) : (container.getName());

		if (containerDefinition == null) {
			XmlElement containerElement = getConfiguredContainerElement(this, getXMLIndex());
			containerDefinition = containerElement.getType();
			containerName = containerElement.getName();
		}

		if (containerDefinition == null || containerName == null) {
			throw new IllegalStateException("No root element/container found");
		}

		// additional schema namespace prefixes
		for (Entry<String, String> schemaNs : additionalSchemaPrefixes.entrySet()) {
			GmlWriterUtil.addNamespace(writer, schemaNs.getKey(), schemaNs.getValue());
		}

		writer.writeStartDocument();
		if (documentWrapper != null) {
			documentWrapper.startWrap(writer, reporter);
		}
		GmlWriterUtil.writeStartElement(writer, containerName);

		// generate mandatory id attribute (for feature collection)
		String containerId = getParameter(PARAM_CONTAINER_ID).as(String.class);
		GmlWriterUtil.writeID(writer, containerDefinition, null, false, containerId);

		// write schema locations
		StringBuffer locations = new StringBuffer();
		locations.append(targetIndex.getNamespace());
		locations.append(" "); //$NON-NLS-1$
		locations.append(targetIndex.getLocation().toString());
		for (Entry<String, Locatable> schema : additionalSchemas.entrySet()) {
			locations.append(" "); //$NON-NLS-1$
			locations.append(schema.getKey());
			locations.append(" "); //$NON-NLS-1$
			locations.append(schema.getValue().getLocation().toString());
		}
		writer.writeAttribute(SCHEMA_INSTANCE_NS, "schemaLocation", locations.toString()); //$NON-NLS-1$

		writeAdditionalElements(writer, containerDefinition, reporter);

		// write the instances
		ResourceIterator<Instance> itInstance = instances.iterator();
		try {
			Map<TypeDefinition, DefinitionPath> paths = new HashMap<TypeDefinition, DefinitionPath>();

			long lastUpdate = 0;
			int count = 0;
			Descent lastDescent = null;
			while (itInstance.hasNext() && !progress.isCanceled()) {
				Instance instance = itInstance.next();

				TypeDefinition type = instance.getDefinition();

				/*
				 * Skip all objects that are no features when writing to a GML
				 * feature collection.
				 */
				boolean skip = useFeatureCollection && !GmlWriterUtil.isFeatureType(type);
				if (skip) {
					progress.advance(1);
					continue;
				}

				// get stored definition path for the type
				DefinitionPath defPath;
				if (paths.containsKey(type)) {
					// get the stored path, may be null
					defPath = paths.get(type);
				}
				else {
					// determine a valid definition path in the container
					defPath = findMemberAttribute(containerDefinition, containerName, type);
					// store path (may be null)
					paths.put(type, defPath);
				}
				if (defPath != null) {
					// write the feature
					lastDescent = Descent.descend(writer, defPath, lastDescent, false);
					writeMember(instance, type, reporter);
				}
				else {
					reporter.warn(new IOMessageImpl(
							MessageFormat
									.format("No compatible member attribute for type {0} found in root element {1}, one instance was skipped",
											type.getDisplayName(), containerName.getLocalPart()),
							null));
				}

				progress.advance(1);
				count++;

				long now = System.currentTimeMillis();
				// only update every 100 milliseconds
				if (now - lastUpdate > 100 || !itInstance.hasNext()) {
					lastUpdate = now;
					sub.subTask(String.valueOf(count) + " instances");
				}
			}
			if (lastDescent != null) {
				lastDescent.close();
			}
		} finally {
			itInstance.close();
		}

		writer.writeEndElement(); // FeatureCollection

		if (documentWrapper != null) {
			documentWrapper.endWrap(writer, reporter);
		}
		writer.writeEndDocument();

		writer.close();
	}

	/**
	 * @return the execution task name
	 */
	protected String getTaskName() {
		return "Generating " + getTypeName();
	}

	/**
	 * This method is called after the container element is started and filled
	 * with needed attributes. The default implementation ensures that a
	 * mandatory boundedBy of GML 2 FeatureCollection is written.
	 * 
	 * @param writer the XML stream writer
	 * @param containerDefinition the container type definition
	 * @param reporter the reporter
	 * @throws XMLStreamException if writing anything fails
	 */
	protected void writeAdditionalElements(XMLStreamWriter writer,
			TypeDefinition containerDefinition, IOReporter reporter) throws XMLStreamException {
		// boundedBy is needed for GML 2 FeatureCollections
		// XXX working like this - getting the child with only a local name?
		ChildDefinition<?> boundedBy = containerDefinition.getChild(new QName("boundedBy")); //$NON-NLS-1$
		if (boundedBy != null && boundedBy.asProperty() != null
				&& boundedBy.asProperty().getConstraint(Cardinality.class).getMinOccurs() > 0) {
			writer.writeStartElement(boundedBy.getName().getNamespaceURI(), boundedBy.getName()
					.getLocalPart());
			writer.writeStartElement(gmlNs, "null"); //$NON-NLS-1$
			writer.writeCharacters("missing"); //$NON-NLS-1$
			writer.writeEndElement();
			writer.writeEndElement();
		}
	}

	/**
	 * Get the for an I/O provider configured target container element, assuming
	 * the I/O provider uses the {@link #PARAM_ROOT_ELEMENT_NAMESPACE} and
	 * {@value #PARAM_ROOT_ELEMENT_NAME} parameters for this.
	 * 
	 * @param provider the I/O provider
	 * @param targetIndex the target XML index
	 * @return the container element or <code>null</code> if it was not found
	 */
	public static XmlElement getConfiguredContainerElement(IOProvider provider, XmlIndex targetIndex) {
		// no container defined, try to use a custom root element
		String namespace = provider.getParameter(PARAM_ROOT_ELEMENT_NAMESPACE).as(String.class);
		// determine target namespace
		if (namespace == null) {
			// default to target namespace
			namespace = targetIndex.getNamespace();
		}
		String elementName = provider.getParameter(PARAM_ROOT_ELEMENT_NAME).as(String.class);

		// find root element
		XmlElement containerElement = null;
		if (elementName != null) {
			QName name = new QName(namespace, elementName);
			containerElement = targetIndex.getElements().get(name);
		}

		return containerElement;
	}

	/**
	 * Find the default container element.
	 * 
	 * @param targetIndex the target type index
	 * @param reporter the reporter, may be <code>null</code>
	 * @return the container XML element or <code>null</code>
	 */
	protected XmlElement findDefaultContainter(XmlIndex targetIndex, IOReporter reporter) {
		if (useFeatureCollection) {
			// try to find FeatureCollection element
			Iterator<XmlElement> it = targetIndex.getElements().values().iterator();
			Collection<XmlElement> fcElements = new HashSet<XmlElement>();
			while (it.hasNext()) {
				XmlElement el = it.next();
				if (isFeatureCollection(el)) {
					fcElements.add(el);
				}
			}

			if (fcElements.isEmpty() && gmlNs != null && gmlNs.equals(NS_GML)) { //$NON-NLS-1$
				// no FeatureCollection defined and "old" namespace -> GML 2
				// include WFS 1.0.0 for the FeatureCollection element
				try {
					URI location = StreamGmlWriter.class.getResource(
							"/schemas/wfs/1.0.0/WFS-basic.xsd").toURI(); //$NON-NLS-1$
					XmlSchemaReader schemaReader = new XmlSchemaReader();
					schemaReader.setSource(new DefaultInputSupplier(location));
					// FIXME to work with the extra schema it must be integrated
					// with the main schema
//					schemaReader.setSharedTypes(sharedTypes);

					IOReport report = schemaReader.execute(null);

					if (report.isSuccess()) {
						XmlIndex wfsSchema = schemaReader.getSchema();

						// look for FeatureCollection element
						for (XmlElement el : wfsSchema.getElements().values()) {
							if (isFeatureCollection(el)) {
								fcElements.add(el);
							}
						}

						// add as additional schema, replace location for
						// verification
						additionalSchemas.put(wfsSchema.getNamespace(), new SchemaDecorator(
								wfsSchema) {

							@Override
							public URI getLocation() {
								return URI
										.create("http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd");
							}
						});

						// add namespace
						GmlWriterUtil.addNamespace(writer, wfsSchema.getNamespace(), "wfs"); //$NON-NLS-1$
					}
				} catch (Exception e) {
					log.warn("Using WFS schema for the FeatureCollection definition failed", e); //$NON-NLS-1$
				}
			}

			if (fcElements.isEmpty() && reporter != null) {
				reporter.warn(new IOMessageImpl(
						"No element describing a FeatureCollection found", null)); //$NON-NLS-1$
			}
			else {
				// select fc element TODO priorized selection (root element
				// parameters)
				XmlElement fcElement = fcElements.iterator().next();

				log.info("Found " + fcElements.size() + " possible FeatureCollection elements" + //$NON-NLS-1$ //$NON-NLS-2$
						", using element " + fcElement.getName()); //$NON-NLS-1$

				return fcElement;
			}
		}

		return null;
	}

	/**
	 * Find a matching attribute for the given member type in the given
	 * container type
	 * 
	 * @param container the container type
	 * @param containerName the container element name
	 * @param memberType the member type
	 * 
	 * @return the attribute definition or <code>null</code>
	 */
	protected DefinitionPath findMemberAttribute(TypeDefinition container, QName containerName,
			final TypeDefinition memberType) {
		// XXX not working if property is no substitution of the property type -
		// use matching instead
//		for (PropertyDefinition property : GmlWriterUtil.collectProperties(container.getChildren())) {
//			// direct match - 
//			if (property.getPropertyType().equals(memberType)) {
//				long max = property.getConstraint(Cardinality.class).getMaxOccurs();
//				return new DefinitionPath(
//						property.getPropertyType(), 
//						property.getName(),
//						max != Cardinality.UNBOUNDED && max <= 1);
//			}
//		}

		AbstractTypeMatcher<TypeDefinition> matcher = new AbstractTypeMatcher<TypeDefinition>() {

			@Override
			protected DefinitionPath matchPath(TypeDefinition type, TypeDefinition matchParam,
					DefinitionPath path) {
				if (type.equals(memberType)) {
					return path;
				}

				// XXX special case: FeatureCollection from foreign schema
				Collection<? extends XmlElement> elements = matchParam.getConstraint(
						XmlElements.class).getElements();
				Collection<? extends XmlElement> containerElements = type.getConstraint(
						XmlElements.class).getElements();
				if (!elements.isEmpty() && !containerElements.isEmpty()) {
					TypeDefinition parent = matchParam.getSuperType();
					while (parent != null) {
						if (parent.equals(type)) {
							// FIXME will not work with separately loaded
							// schemas because e.g. the choice allowing the
							// specific type is missing
							// FIXME add to path
//							return new DefinitionPath(path).addSubstitution(elements.iterator().next());
						}

						parent = parent.getSuperType();
					}
				}

				return null;
			}
		};

		// candidate match (go down at maximum ten levels)
		List<DefinitionPath> candidates = matcher.findCandidates(container, containerName, true,
				memberType, 10);
		if (candidates != null && !candidates.isEmpty()) {
			return candidates.get(0); // TODO notification? FIXME will this
										// work? possible problem: attribute is
										// selected even though better candidate
										// is in other attribute
		}

		return null;
	}

	private boolean isFeatureCollection(XmlElement el) {
		// TODO improve condition?
		// FIXME working like this?!
		return el.getName().getLocalPart().contains("FeatureCollection") && //$NON-NLS-1$
				!el.getType().getConstraint(AbstractFlag.class).isEnabled()
				&& hasChild(el.getType(), "featureMember"); //$NON-NLS-1$
	}

	private boolean hasChild(TypeDefinition type, String localName) {
		for (ChildDefinition<?> child : DefinitionUtil.getAllProperties(type)) {
			if (localName.equals(child.getName().getLocalPart())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Write a given instance
	 * 
	 * @param instance the instance to writer
	 * @param type the feature type definition
	 * @param report the reporter
	 * @throws XMLStreamException if writing the feature fails
	 */
	protected void writeMember(Instance instance, TypeDefinition type, IOReporter report)
			throws XMLStreamException {
//		Name elementName = GmlWriterUtil.getElementName(type);
//		writer.writeStartElement(elementName.getNamespaceURI(), elementName.getLocalPart());

		writeProperties(instance, type, true, false, report);

//		writer.writeEndElement(); // type element name
	}

	/**
	 * Write the given feature's properties
	 * 
	 * @param group the feature
	 * @param definition the feature type
	 * @param allowElements if element properties may be written
	 * @param parentIsNil if the parent property is nil
	 * @param report the reporter
	 * @throws XMLStreamException if writing the properties fails
	 */
	private void writeProperties(Group group, DefinitionGroup definition, boolean allowElements,
			boolean parentIsNil, IOReporter report) throws XMLStreamException {
		// eventually generate mandatory ID that is not set
		GmlWriterUtil.writeRequiredID(writer, definition, group, true);

		// writing the feature is controlled by the type definition
		// so retrieving values from instance must happen based on actual
		// structure! (e.g. including groups)

		// write the attributes, as they must be handled first
		writeProperties(group, DefinitionUtil.getAllChildren(definition), true, parentIsNil, report);

		if (allowElements) {
			// write the elements
			writeProperties(group, DefinitionUtil.getAllChildren(definition), false, parentIsNil,
					report);
		}
	}

	/**
	 * Write attribute or element properties.
	 * 
	 * @param parent the parent group
	 * @param children the child definitions
	 * @param attributes <code>true</code> if attribute properties shall be
	 *            written, <code>false</code> if element properties shall be
	 *            written
	 * @param parentIsNil if the parent property is nil
	 * @param report the reporter
	 * @throws XMLStreamException if writing the attributes/elements fails
	 */
	private void writeProperties(Group parent, Collection<? extends ChildDefinition<?>> children,
			boolean attributes, boolean parentIsNil, IOReporter report) throws XMLStreamException {
		if (parent == null) {
			return;
		}

		boolean parentIsChoice = parent.getDefinition() instanceof GroupPropertyDefinition
				&& ((GroupPropertyDefinition) parent.getDefinition()).getConstraint(
						ChoiceFlag.class).isEnabled();

		for (ChildDefinition<?> child : children) {
			Object[] values = parent.getProperty(child.getName());

			if (child.asProperty() != null) {
				PropertyDefinition propDef = child.asProperty();
				boolean isAttribute = propDef.getConstraint(XmlAttributeFlag.class).isEnabled();

				if (attributes && isAttribute) {
					if (values != null && values.length > 0) {
						boolean allowWrite = true;

						// special case handling: nilReason
						if (getParameter(PARAM_OMIT_NIL_REASON).as(Boolean.class, true)) {
							Cardinality propCard = propDef.getConstraint(Cardinality.class);
							if ("nilReason".equals(propDef.getName().getLocalPart())
									&& propCard.getMinOccurs() < 1) {
								allowWrite = parentIsNil;
							}
						}

						// write attribute
						if (allowWrite) {
							writeAttribute(values[0], propDef);
						}

						if (values.length > 1) {
							// TODO warning?!
						}
					}
				}
				else if (!attributes && !isAttribute) {
					int numValues = 0;
					if (values != null) {
						// write element
						for (Object value : values) {
							writeElement(value, propDef, report);
						}
						numValues = values.length;
					}

					// write additional elements to satisfy minOccurrs
					// only if parent is not a choice
					if (!parentIsChoice) {
						Cardinality cardinality = propDef.getConstraint(Cardinality.class);
						if (cardinality.getMinOccurs() > numValues) {
							if (propDef.getConstraint(NillableFlag.class).isEnabled()) {
								// nillable element
								for (int i = numValues; i < cardinality.getMinOccurs(); i++) {
									// write nil element
									writeElement(null, propDef, report);
								}
							}
							else {
								// no value for non-nillable element

								for (int i = numValues; i < cardinality.getMinOccurs(); i++) {
									// write empty element
									GmlWriterUtil.writeEmptyElement(writer, propDef.getName());
								}

								// TODO add warning to report
							}
						}
					}
				}
			}
			else if (child.asGroup() != null) {
				// handle to child groups
				if (values != null) {
					for (Object value : values) {
						if (value instanceof Group) {
							writeProperties((Group) value,
									DefinitionUtil.getAllChildren(child.asGroup()), attributes,
									parentIsNil, report);
						}
						else {
							// TODO warning/error?
						}
					}
				}
			}
		}
	}

	/**
	 * Write a property element.
	 * 
	 * @param value the element value
	 * @param propDef the property definition
	 * @param report the reporter
	 * @throws XMLStreamException if writing the element fails
	 */
	private void writeElement(Object value, PropertyDefinition propDef, IOReporter report)
			throws XMLStreamException {
		Group group = null;
		if (value instanceof Group) {
			group = (Group) value;
			if (value instanceof Instance) {
				// extract value from instance
				value = ((Instance) value).getValue();
			}
		}

		if (group == null) {
			// just a value

			if (value == null) {
				// null value
				if (propDef.getConstraint(Cardinality.class).getMinOccurs() > 0) {
					// write empty element
					GmlWriterUtil.writeEmptyElement(writer, propDef.getName());

					// mark as nil
					writeElementValue(null, propDef);
				}
				// otherwise just skip it
			}
			else {
				GmlWriterUtil.writeStartElement(writer, propDef.getName());

				Pair<Geometry, CRSDefinition> pair = extractGeometry(value, true, report);
				if (pair != null) {
					String srsName = extractCode(pair.getSecond());
					// write geometry
					writeGeometry(pair.getFirst(), propDef, srsName, report);
				}
				else {
					// simple element with value
					// write value as content
					writeElementValue(value, propDef);
				}

				writer.writeEndElement();
			}
		}
		else {
			// children and maybe a value

			GmlWriterUtil.writeStartElement(writer, propDef.getName());

			boolean hasValue = propDef.getPropertyType().getConstraint(HasValueFlag.class)
					.isEnabled();

			Pair<Geometry, CRSDefinition> pair = extractGeometry(value, true, report);
			// handle about annotated geometries
			if (!hasValue && pair != null) {
				String srsName = extractCode(pair.getSecond());
				// write geometry
				writeGeometry(pair.getFirst(), propDef, srsName, report);
			}
			else {
				boolean hasOnlyNilReason = hasOnlyNilReason(group);

				// write no elements if there is a value or only a nil reason
				boolean writeElements = !hasValue && !hasOnlyNilReason;
				boolean isNil = !writeElements && (!hasValue || value == null);

				// write all children
				writeProperties(group, group.getDefinition(), writeElements, isNil, report);

				// write value
				if (hasValue) {
					writeElementValue(value, propDef);
				}
				else if (hasOnlyNilReason) {
					// complex element with a nil value -> write xsi:nil if
					// possible

					/*
					 * XXX open question: should xsi:nil be there also if there
					 * are other attributes than nilReason?
					 */

					writeElementValue(null, propDef);
				}
			}

			writer.writeEndElement();
		}
	}

	/**
	 * Determines if a group has as its only property the nilReason attribute.
	 * 
	 * @param group the group to test
	 * @return <code>true</code> if the group has the nilReason attribute and no
	 *         other children, or no children at all, <code>false</code>
	 *         otherwise
	 */
	private boolean hasOnlyNilReason(Group group) {
		int count = 0;
		QName nilReasonName = null;
		for (QName name : group.getPropertyNames()) {
			if (count > 0)
				// more than one property
				return false;
			if (!name.getLocalPart().equals("nilReason"))
				// a property different from nilReason
				return false;
			nilReasonName = name;
			count++;
		}

		if (nilReasonName != null) {
			// make sure it is an attribute
			DefinitionGroup parent = group.getDefinition();
			ChildDefinition<?> child = parent.getChild(nilReasonName);
			if (child.asProperty() == null) {
				// is a group
				return false;
			}
			if (!child.asProperty().getConstraint(XmlAttributeFlag.class).isEnabled()) {
				// not an attribute
				return false;
			}
		}

		return true;
	}

	/**
	 * Write an element value, either as element content or as <code>nil</code>.
	 * 
	 * @param value the element value
	 * @param propDef the property definition the value is associated to
	 * @throws XMLStreamException if an error occurs writing the value
	 */
	private void writeElementValue(Object value, PropertyDefinition propDef)
			throws XMLStreamException {
		if (value == null) {
			// null value
			if (!propDef.getConstraint(NillableFlag.class).isEnabled()) {
				log.warn("Non-nillable element " + propDef.getName() + " is null"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else {
				// nillable -> we may mark it as nil
				writer.writeAttribute(SCHEMA_INSTANCE_NS, "nil", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			TypeDefinition propType = propDef.getPropertyType();

			if (value instanceof Iterable
					&& List.class.isAssignableFrom(propType.getConstraint(Binding.class)
							.getBinding())
					&& propType.getConstraint(ElementType.class).getBinding() != null) {
				// element is a list
				// TODO more robust detection?

				boolean first = true;
				for (Object element : ((Iterable<?>) value)) {
					if (first) {
						first = false;
					}
					else {
						// space delimits list elements
						writer.writeCharacters(" ");
					}

					// write the element
					writer.writeCharacters(SimpleTypeUtil.convertToXml(element, propType
							.getConstraint(ElementType.class).getDefinition()));
				}
			}
			else {
				// write value as content
				writer.writeCharacters(SimpleTypeUtil.convertToXml(value, propDef.getPropertyType()));
			}
		}
	}

	/**
	 * Write a geometry
	 * 
	 * @param geometry the geometry
	 * @param property the geometry property
	 * @param srsName the common SRS name, may be <code>null</code>
	 * @param report the reporter
	 * @throws XMLStreamException if an error occurs writing the geometry
	 */
	private void writeGeometry(Geometry geometry, PropertyDefinition property, String srsName,
			IOReporter report) throws XMLStreamException {
		// write geometries
		getGeometryWriter().write(writer, geometry, property, srsName, report);
	}

	/**
	 * Get the geometry writer
	 * 
	 * @return the geometry writer instance to use
	 */
	protected StreamGeometryWriter getGeometryWriter() {
		if (geometryWriter == null) {
			// default to true
			boolean simplifyGeometry = getParameter(PARAM_SIMPLIFY_GEOMETRY)
					.as(Boolean.class, true);

			geometryWriter = StreamGeometryWriter.getDefaultInstance(gmlNs, simplifyGeometry);
		}

		return geometryWriter;
	}

	/**
	 * Write a property attribute
	 * 
	 * @param value the attribute value, may be <code>null</code>
	 * @param propDef the associated property definition
	 * @throws XMLStreamException if writing the attribute fails
	 */
	private void writeAttribute(Object value, PropertyDefinition propDef) throws XMLStreamException {
		GmlWriterUtil.writeAttribute(writer, value, propDef);
	}

}
