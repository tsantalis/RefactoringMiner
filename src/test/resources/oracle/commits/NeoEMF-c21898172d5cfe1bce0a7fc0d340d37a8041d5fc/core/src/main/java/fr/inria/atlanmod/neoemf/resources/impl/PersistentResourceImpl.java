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
package fr.inria.atlanmod.neoemf.resources.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.InternalEObject.EStore;
import org.eclipse.emf.ecore.impl.EClassifierImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.impl.EStoreEObjectImpl;
import org.eclipse.emf.ecore.impl.EStoreEObjectImpl.EStoreEList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.core.impl.NeoEObjectAdapterFactoryImpl;
import fr.inria.atlanmod.neoemf.core.impl.PersistentEObjectImpl;
import fr.inria.atlanmod.neoemf.core.impl.StringId;
import fr.inria.atlanmod.neoemf.datastore.InternalPersistentEObject;
import fr.inria.atlanmod.neoemf.datastore.InvalidOptionsException;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackend;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;
import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.util.NeoURI;

public class PersistentResourceImpl extends ResourceImpl implements PersistentResource {

	/**
	 * Fake {@link EStructuralFeature} that represents the
	 * {@link Resource#getContents()} feature.
	 * 
	 */
	protected static class ResourceContentsEStructuralFeature extends EReferenceImpl {
		protected static final String RESOURCE__CONTENTS__FEATURE_NAME = "eContents";

		public ResourceContentsEStructuralFeature() {
			this.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
			this.setLowerBound(0);
			this.setName(RESOURCE__CONTENTS__FEATURE_NAME);
			this.setEType(new EClassifierImpl() {
			});
			this.setFeatureID(RESOURCE__CONTENTS);
		}
	}

	/**
	 * Dummy {@link EObject} that represents the root entry point for this
	 * {@link Resource}
	 * 
	 */
	protected final class DummyRootEObject extends PersistentEObjectImpl {
		protected static final String ROOT_EOBJECT_ID = "ROOT";

		public DummyRootEObject(Resource.Internal resource) {
			super();
			this.id = new StringId(ROOT_EOBJECT_ID);
			eSetDirectResource(resource);
		}
	}

	protected static final ResourceContentsEStructuralFeature ROOT_CONTENTS_ESTRUCTURALFEATURE = new ResourceContentsEStructuralFeature();

	protected final DummyRootEObject DUMMY_ROOT_EOBJECT = new DummyRootEObject(this);

	protected Map<?, ?> options;

	protected SearcheableResourceEStore eStore;

	/**
	 * The underlying {@link PersistenceBackend} that stores the data
	 */
	protected PersistenceBackend persistenceBackend;
	
	protected boolean isPersistent = false;

	public PersistentResourceImpl(URI uri) {
		super(uri);
		this.persistenceBackend = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createTransientBackend();
		this.eStore = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createTransientEStore(this,persistenceBackend);
		this.isPersistent = false;
		// Stop the backend when the application is terminated
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
			    NeoLogger.log(NeoLogger.SEVERITY_INFO, "Closing backend of resource " + this.toString());
			    if(PersistentResourceImpl.this.persistenceBackend.isStarted()) {
			        PersistentResourceImpl.this.persistenceBackend.stop();
			        NeoLogger.log(NeoLogger.SEVERITY_INFO, "Backend of resource " + this.toString() + " closed");
			    }
			}
		});
		NeoLogger.log(NeoLogger.SEVERITY_INFO, "Persistent Resource Created");
	}
	
	/**
	 * Returns the graph DB file
	 * 
	 * @return
	 */
	protected File getFile() {
		return FileUtils.getFile(NeoURI.createNeoURI(getURI()).toFileString());
	}

	@Override
	public void load(Map<?, ?> options) throws IOException {
		try {
			isLoading = true;
			if (isLoaded) {
				return;
			} else if (!getFile().exists()) {
				throw new FileNotFoundException(uri.toFileString());
			} else {
				this.persistenceBackend = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createPersistentBackend(getFile(), options);
				this.eStore = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createPersistentEStore(this, persistenceBackend, options);
				this.isPersistent = true;
			}
			this.options = options;
			isLoaded = true;
		} finally {
			isLoading = false;
		}
	}

	@Override
	public void save(Map<?, ?> options) throws IOException {

		if (this.options != null) {
			// Check that the save options do not collide with previous load options
			for (Entry<?, ?> entry : options.entrySet()) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (this.options.containsKey(key) && value != null) {
					if (!value.equals(this.options.get(key))) {
						throw new IOException(new InvalidOptionsException(MessageFormat.format("key = {0}; value = {1}", key.toString(), value.toString())));
					}
				}
			}
		}
		if(!isLoaded() || !this.isPersistent) {
			PersistenceBackend newBackend = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createPersistentBackend(getFile(), options);
			PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).copyBackend(this.persistenceBackend, newBackend);
			this.persistenceBackend = newBackend;
			this.eStore = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createPersistentEStore(this,persistenceBackend, options);
			this.isLoaded = true;
			this.isPersistent = true;
		}
		persistenceBackend.save();
	}

	
	@Override
	public EList<EObject> getContents() {
		return new ResourceContentsEStoreEList<EObject>(DUMMY_ROOT_EOBJECT, ROOT_CONTENTS_ESTRUCTURALFEATURE, eStore());
	}

	@Override
	public EObject getEObject(String uriFragment) {
		EObject eObject = eStore.eObject(new StringId(uriFragment));
		if (eObject != null) {
			return eObject;
		} else {
			return super.getEObject(uriFragment);
		}
	}

	@Override
	public String getURIFragment(EObject eObject) {
		if (eObject.eResource() != this) {
			return "/-1";
		} else {
			// Try to adapt as a PersistentEObject and return the ID
			PersistentEObject persistentEObject = NeoEObjectAdapterFactoryImpl.getAdapter(eObject, PersistentEObject.class);
			if (persistentEObject != null) {
				return (persistentEObject.id().toString());
			}
		}
		return super.getURIFragment(eObject);
	}
	
	@Override
	public EList<EObject> getAllInstances(EClass eClass) {
		return this.getAllInstances(eClass, false);
	}
	
	@Override
	public EList<EObject> getAllInstances(EClass eClass, boolean strict) {
	    try {
	        return eStore.getAllInstances(eClass,strict);
	    } catch(UnsupportedOperationException e) {
	        NeoLogger.log(NeoLogger.SEVERITY_WARNING, "Persistence Backend does not support advanced allInstances() computation, using standard EMF API instead");
	        Iterator<EObject> it = getAllContents();
	        EList<EObject> instanceList = new BasicEList<EObject>();
	        while(it.hasNext()) {
	            EObject eObject = it.next();
	            if(eClass.isInstance(eObject)) {
	                if(strict) {
	                    if(eObject.eClass().equals(eClass)) {
	                        instanceList.add(eObject);
	                    }
	                }
	                else {
	                    instanceList.add(eObject);
	                }
                }
	        }
	        return instanceList;
	    }
	}
	

	protected void shutdown() {
		this.persistenceBackend.stop();
		this.persistenceBackend = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createTransientBackend();
		this.eStore = PersistenceBackendFactoryRegistry.getFactoryProvider(uri.scheme()).createTransientEStore(this,persistenceBackend);
		this.isPersistent = false;
		this.isLoaded = false;
	}

	@Override
	protected void doUnload() {
		Iterator<EObject> allContents = getAllProperContents(unloadingContents);
		getErrors().clear();
		getWarnings().clear();
		while (allContents.hasNext()) {
			unloaded((InternalEObject) allContents.next());
		}
		shutdown();
	}

	@Override
	protected void finalize() throws Throwable {
		unload();
		super.finalize();
	}

	@Override
	public InternalEObject.EStore eStore() {
		return eStore;
	}

	/**
	 * A notifying {@link EStoreEList} list implementation for supporting
	 * {@link Resource#getContents}.
	 * 
	 * @author agomez
	 * 
	 */
	protected class ResourceContentsEStoreEList<E> extends EStoreEObjectImpl.EStoreEList<E> {
		protected static final long serialVersionUID = 1L;

		protected ResourceContentsEStoreEList(InternalEObject owner, EStructuralFeature eStructuralFeature, EStore store) {
			super(owner, eStructuralFeature, store);
		}

		@Override
		protected E validate(int index, E object) {
			if (!canContainNull() && object == null) {
				throw new IllegalArgumentException("The 'no null' constraint is violated");
			}
			return object;
		}

		@Override
		public Object getNotifier() {
			return PersistentResourceImpl.this;
		}

		@Override
		public int getFeatureID() {
			return RESOURCE__CONTENTS;
		}

		@Override
		protected boolean isNotificationRequired() {
			return PersistentResourceImpl.this.eNotificationRequired();
		}

		@Override
		protected boolean useEquals() {
			return false;
		}

		@Override
		protected boolean hasInverse() {
			return true;
		}

		@Override
		protected boolean isUnique() {
			return true;
		}

		@Override
		public NotificationChain inverseAdd(E object, NotificationChain notifications) {
			InternalEObject eObject = (InternalEObject) object;
			notifications = eObject.eSetResource(PersistentResourceImpl.this, notifications);
			PersistentResourceImpl.this.attached(eObject);
			return notifications;
		}

		@Override
		public NotificationChain inverseRemove(E object, NotificationChain notifications) {
			InternalEObject eObject = (InternalEObject) object;
			if (PersistentResourceImpl.this.isLoaded || unloadingContents != null) {
				PersistentResourceImpl.this.detached(eObject);
			}
			return eObject.eSetResource(null, notifications);
		}
		
		@Override
		protected void delegateAdd(int index, Object object) {
			// FIXME? Maintain a list of hard links to the elements while moving
			// them to the new resource. If a garbage collection happens while
			// traversing the children elements, some unsaved objects that are
			// referenced from a saved object may be garbage collected before
			// they have been completely stored in the DB
			List<Object> hardLinksList = new ArrayList<>();
			InternalPersistentEObject eObject = NeoEObjectAdapterFactoryImpl.getAdapter(object, InternalPersistentEObject.class);
			// Collect all contents
			hardLinksList.add(object);
			for (Iterator<EObject> it = eObject.eAllContents(); it.hasNext(); hardLinksList.add(it.next()));
			// Iterate using the hard links list instead the getAllContents
			// We ensure that using the hardLinksList it is not taken out by JIT
			// compiler
			for (Object element : hardLinksList) {
				InternalPersistentEObject internalElement = NeoEObjectAdapterFactoryImpl.getAdapter(element, InternalPersistentEObject.class);
				internalElement.resource(PersistentResourceImpl.this);
			}
			super.delegateAdd(index, object);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected E delegateRemove(int index) {
			E object = super.delegateRemove(index);
			List<E> hardLinksList = new ArrayList<>();
			InternalPersistentEObject eObject = NeoEObjectAdapterFactoryImpl.getAdapter(object, InternalPersistentEObject.class);
			// Collect all contents
			hardLinksList.add(object);
			for (Iterator<EObject> it = eObject.eAllContents(); it.hasNext(); hardLinksList.add((E)it.next()));
			// Iterate using the hard links list instead the getAllContents
			// We ensure that using the hardLinksList it is not taken out by JIT
			// compiler
			for (E element : hardLinksList) {
				InternalPersistentEObject internalElement = NeoEObjectAdapterFactoryImpl.getAdapter(element, InternalPersistentEObject.class);
				internalElement.resource(null);
			}
			return object;			
		}
		
		@Override
		protected void didAdd(int index, E object) {
			super.didAdd(index, object);
			if (index == size() - 1) {
				loaded();
			}
			modified();
		}

		@Override
		protected void didRemove(int index, E object) {
			super.didRemove(index, object);
			modified();
		}

		@Override
		protected void didSet(int index, E newObject, E oldObject) {
			super.didSet(index, newObject, oldObject);
			modified();
		}

		@Override
		protected void didClear(int oldSize, Object[] oldData) {
			if (oldSize == 0) {
				loaded();
			} else {
				super.didClear(oldSize, oldData);
			}
		}

		protected void loaded() {
			if (!PersistentResourceImpl.this.isLoaded()) {
				Notification notification = PersistentResourceImpl.this.setLoaded(true);
				if (notification != null) {
					PersistentResourceImpl.this.eNotify(notification);
				}
			}
		}

		protected void modified() {
			if (isTrackingModification()) {
				setModified(true);
			}
		}
	}

	public static void shutdownWithoutUnload(PersistentResourceImpl resource) {
		resource.shutdown();
	}
}
