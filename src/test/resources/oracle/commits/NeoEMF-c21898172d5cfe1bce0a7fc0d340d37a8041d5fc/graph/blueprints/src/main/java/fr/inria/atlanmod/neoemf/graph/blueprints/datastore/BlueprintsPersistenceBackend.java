/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.neoemf.graph.blueprints.datastore;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jboss.util.collection.SoftValueHashMap;
import org.jboss.util.collection.WeakValueHashMap;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.id.IdEdge;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;
import com.tinkerpop.blueprints.util.wrappers.id.IdVertex;

import fr.inria.atlanmod.neoemf.core.PersistenceFactory;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.core.impl.NeoEObjectAdapterFactoryImpl;
import fr.inria.atlanmod.neoemf.core.impl.StringId;
import fr.inria.atlanmod.neoemf.datastore.InternalPersistentEObject;
import fr.inria.atlanmod.neoemf.datastore.InvalidDataStoreException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;


public class BlueprintsPersistenceBackend extends IdGraph<KeyIndexableGraph> implements PersistenceBackend {
	
	private Index<Vertex> metaclassIndex;

	protected class NeoEdge extends IdEdge {
		public NeoEdge(Edge edge) {
			super(edge, BlueprintsPersistenceBackend.this);
		}
		
		/**
		 * {@inheritDoc} <br>
		 * If the {@link Edge} references a {@link Vertex} with no more incoming
		 * {@link Edge}, the referenced {@link Vertex} is removed as well
		 */
		@Override
		public void remove() {
			Vertex referencedVertex = this.getVertex(Direction.IN);
			super.remove();
			if (!referencedVertex.getEdges(Direction.IN).iterator().hasNext()) {
				// If the Vertex has no more incoming edges remove it from the DB
				referencedVertex.remove();
			}
		}
	}
	
	protected static final String ECLASS__NAME = EcorePackage.eINSTANCE.getENamedElement_Name().getName();
	protected static final String EPACKAGE__NSURI = EcorePackage.eINSTANCE.getEPackage_NsURI().getName();

	protected static final String INSTANCE_OF = "kyanosInstanceOf";

	/**
	 * This {@link Map}&lt;objectID, {@link EObject}> is necessary to maintain a
	 * registry of the already loaded {@link Vertex}es, to avoid duplicated
	 * {@link EObject}s in memory.
	 * 
	 * We use a {@link WeakValueHashMap} for saving memory. When the value
	 * {@link EObject} is no longer referenced and can be garbage collected it
	 * is removed from the {@link Map}.
	 */
	protected Map<Object, InternalPersistentEObject> loadedEObjects = new SoftValueHashMap<Object, InternalPersistentEObject>();
	protected List<EClass> indexedEClasses = new ArrayList<EClass>();
	
	public BlueprintsPersistenceBackend(KeyIndexableGraph baseGraph) {
		super(baseGraph);
		metaclassIndex = getIndex("metaclasses", Vertex.class);
		if(metaclassIndex == null) {
			metaclassIndex = createIndex("metaclasses",Vertex.class);
		}
	}
	
	@Override
	public void start(Map<?, ?> options) throws InvalidDataStoreException {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public boolean isStarted() {
		return true;
	}
	
	@Override
	public void stop() {
		this.shutdown();
		
	}
	
	@Override
	public void save() {
	    if(this.getFeatures().supportsTransactions) {
	        this.commit();
	    } else {
	        this.shutdown();
	    }
	}
	
	public void initMetaClassesIndex(List<EClass> eClassList) {
	    for(EClass eClass : eClassList) {
	        assert !metaclassIndex.get("name", eClass.getName()).iterator().hasNext() : "Index is not consistent";
	        metaclassIndex.put("name", eClass.getName(), getVertex(eClass));
	    }
	}
	
	/**
	 * Create a new vertex, add it to the graph, and return the newly created
	 * vertex. The issued {@link EObject} is used to calculate the
	 * {@link Vertex} <code>id</code>.
	 * 
	 * @param eObject
	 *            The corresponding {@link EObject}
	 * @return the newly created vertex
	 */
	protected Vertex addVertex(EObject eObject) {
		PersistentEObject neoEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, PersistentEObject.class);
		Vertex v = addVertex(neoEObject.id().toString());
		return v;
	}

	/**
	 * Create a new vertex, add it to the graph, and return the newly created
	 * vertex. The issued {@link EClass} is used to calculate the
	 * {@link Vertex} <code>id</code>.
	 * 
	 * @param eClass
	 *            The corresponding {@link EClass}
	 * @return the newly created vertex
	 */
	protected Vertex addVertex(EClass eClass) {
		Vertex vertex = addVertex(buildEClassId(eClass));
		vertex.setProperty(ECLASS__NAME, eClass.getName());
		vertex.setProperty(EPACKAGE__NSURI, eClass.getEPackage().getNsURI());
		return vertex;
	}

	/**
	 * Return the vertex corresponding to the provided {@link EObject}. If no
	 * vertex corresponds to that {@link EObject}, then return null.
	 * 
	 * @param id
	 * @return the vertex referenced by the provided {@link EObject} or null
	 *         when no such vertex exists
	 */
	public Vertex getVertex(EObject eObject) {
		PersistentEObject neoEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, PersistentEObject.class);
		return getVertex(neoEObject.id().toString());
	}

	/**
	 * Return the vertex corresponding to the provided {@link EObject}. If no
	 * vertex corresponds to that {@link EObject}, then the corresponding
	 * {@link Vertex} together with its <code>INSTANCE_OF</code> relationship is
	 * created.
	 * 
	 * @param id
	 * @return the vertex referenced by the provided {@link EObject} or null
	 *         when no such vertex exists
	 */
	public Vertex getOrCreateVertex(EObject eObject) {
		InternalPersistentEObject neoEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, InternalPersistentEObject.class);
		Vertex vertex = getVertex(neoEObject.id().toString());
		if (vertex == null) {
			vertex = addVertex(neoEObject);
			EClass eClass = neoEObject.eClass();
			Iterator<Vertex> metaclassIndexHits = metaclassIndex.get("name", eClass.getName()).iterator();
			Vertex eClassVertex = null;
			if(metaclassIndexHits.hasNext()) {
			    eClassVertex = metaclassIndexHits.next();
			}
			else {
				eClassVertex = addVertex(eClass);
				metaclassIndex.put("name", eClass.getName(), eClassVertex);
				indexedEClasses.add(eClass);
			}
			vertex.addEdge(INSTANCE_OF, eClassVertex);
			loadedEObjects.put(neoEObject.id().toString(), neoEObject);
		}
		return vertex;
	}
	
	/**
	 * Returns the vertex corresponding to the provided {@link EClass}. If no
	 * vertex corresponds to that {@link EClass}, then return null.
	 * 
	 * @param id
	 * @return the vertex referenced by the provided {@link EClass} or null when
	 *         no such vertex exists
	 */
	protected Vertex getVertex(EClass eClass) {
		return getVertex(buildEClassId(eClass));
	}

	

    @Override
	public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
        return new NeoEdge(getBaseGraph().addEdge(id, ((IdVertex) outVertex).getBaseVertex(), ((IdVertex) inVertex).getBaseVertex(), label));
    }

    @Override
	public Edge getEdge(final Object id) {
        final Edge edge = getBaseGraph().getEdge(id);
        if (null == edge)
            return null;
        else
            return new NeoEdge(edge);
    }
    
    

	public EClass resolveInstanceOf(Vertex vertex) {
		Iterator<Vertex> iterator = vertex.getVertices(Direction.OUT, INSTANCE_OF).iterator();
		if (iterator.hasNext()) {
			Vertex eClassVertex = iterator.next();
			String name = eClassVertex.getProperty(ECLASS__NAME);
			String nsUri = eClassVertex.getProperty(EPACKAGE__NSURI);
			EClass eClass = (EClass) Registry.INSTANCE.getEPackage(nsUri).getEClassifier(name);
			return eClass;
		}
		return null;
	}

	
	public InternalPersistentEObject reifyVertex(Vertex vertex, EClass eClass) {
		Object id = vertex.getId();
		InternalPersistentEObject neoEObject = null;
//		synchronized(loadedEObjects) {
			neoEObject = loadedEObjects.get(id);
//		}
		if (neoEObject == null) {
			if (eClass != null) {
			    EObject eObject = null;
			    if(eClass.getEPackage().getClass().equals(EPackageImpl.class)) {
			        // Dynamic EMF
			        eObject = PersistenceFactory.eINSTANCE.create(eClass);
			    } else {
			        // EObject eObject = EcoreUtil.create(eClass);
			        eObject = EcoreUtil.create(eClass);
			    }
				if (eObject instanceof InternalPersistentEObject) {
					neoEObject = (InternalPersistentEObject) eObject;
				} else {
					neoEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, InternalPersistentEObject.class);
				}
				neoEObject.id(new StringId(id.toString()));
			} else {
				NeoLogger.log(NeoLogger.SEVERITY_ERROR, 
						MessageFormat.format("Vertex {0} does not have an associated EClass Vertex", id));
			}
			synchronized(loadedEObjects) {
				loadedEObjects.put(id, neoEObject);
			}
		}
		return neoEObject;
	}
	
	/**
	 * Reifies the given {@link Vertex} as an {@link EObject}. The method
	 * guarantees that the same {@link EObject} is returned for a given
	 * {@link Vertex} in subsequent calls, unless the {@link EObject} returned
	 * in previous calls has been already garbage collected.
	 * 
	 * @param vertex
	 * @return
	 */
	public InternalPersistentEObject reifyVertex(Vertex vertex) {
		Object id = vertex.getId();
		InternalPersistentEObject neoEObject = null;
//		synchronized(loadedEObjects) {
			neoEObject = loadedEObjects.get(id);
//		}
		if (neoEObject == null) {
			EClass eClass = resolveInstanceOf(vertex);
			if (eClass != null) {
			    EObject eObject = null;
			    if(eClass.getEPackage().getClass().equals(EPackageImpl.class)) {
			        // Dynamic EMF
			        eObject = PersistenceFactory.eINSTANCE.create(eClass);
			    } else {
			        // EObject eObject = EcoreUtil.create(eClass);
			        eObject = EcoreUtil.create(eClass);
			    }
				if (eObject instanceof InternalPersistentEObject) {
					neoEObject = (InternalPersistentEObject) eObject;
				} else {
					neoEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, InternalPersistentEObject.class);
				}
				neoEObject.id(new StringId(id.toString()));
			} else {
				NeoLogger.log(NeoLogger.SEVERITY_ERROR, 
						MessageFormat.format("Vertex {0} does not have an associated EClass Vertex", id));
			}
			synchronized(loadedEObjects) {
				loadedEObjects.put(id, neoEObject);
			}
		}
		return neoEObject;
	}
	
	/**
	 * Builds the <code>id</code> used to identify {@link EClass} {@link Vertex}
	 * es.
	 * 
	 * @param eClass
	 * @return
	 */
	protected static String buildEClassId(EClass eClass) {
		if (eClass != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(eClass.getName());
			builder.append("@");
			builder.append(eClass.getEPackage().getNsURI());
			return builder.toString();
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @return the list of EClasses that have been indexed.
	 * This list is needed to support index copy in {@link BlueprintsPersistenceBackendFactory#copyBackend(PersistenceBackend, PersistenceBackend)}
	 */
	public List<EClass> getIndexedEClasses() {
	    return indexedEClasses;
	}
	
	@Override
	public Map<EClass,Iterator<Vertex>> getAllInstances(EClass eClass, boolean strict) {
		Map<EClass,Iterator<Vertex>> indexHits = new HashMap<EClass,Iterator<Vertex>>();
		Set<EClass> eClassToFind = new HashSet<EClass>();
		if(eClass.isAbstract() && strict) {
		    // There is no strict instance of an abstract class
		    return Collections.emptyMap();
		}
		eClassToFind.add(eClass);
		if(!strict) {
		    // Find all the concrete subclasses of the given EClass
            // (the metaclass index only stores the concrete EClasses)
            EPackage ePackage = eClass.getEPackage();
            for(EClassifier eClassifier : ePackage.getEClassifiers()) {
                if(eClassifier instanceof EClass) {
                    EClass packageEClass = (EClass)eClassifier;
                    if(eClass.isSuperTypeOf(packageEClass) && !packageEClass.isAbstract()) {
                        eClassToFind.add(packageEClass);
                    }
                }
            } 
		}
		// Get all the vertices that are indexed with one of the EClass
		for(EClass ec : eClassToFind) {
			Iterator<Vertex> metaClassVertexIterator = metaclassIndex.get("name", ec.getName()).iterator();
			if(metaClassVertexIterator.hasNext()) {
				Vertex metaClassVertex = metaClassVertexIterator.next();
				Iterator<Vertex> instanceVertexIterator = metaClassVertex.getVertices(Direction.IN, INSTANCE_OF).iterator();
				indexHits.put(ec, instanceVertexIterator);
			}
			else {
				NeoLogger.log(NeoLogger.SEVERITY_WARNING, "MetaClass '" + ec.getName() + "'not found in index");
			}
			
		}	
		return indexHits;
	}
}
