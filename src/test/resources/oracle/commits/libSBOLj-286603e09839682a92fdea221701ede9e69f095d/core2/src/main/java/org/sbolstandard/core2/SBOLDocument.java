package org.sbolstandard.core2;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sbolstandard.core2.abstract_classes.Identified;
import org.sbolstandard.core2.abstract_classes.SBOLVisitable;

/**
 * @author Zhen Zhang
 * @version 2.0
 *
 */
public class SBOLDocument extends SBOLVisitable {
	
//	private	Collection<Context> context;
//	private Collection<GenericComponent> genericComponent;
//	private Collection<SequenceComponent> sequenceComponent;
//	private Collection<Model> model;
//	private Collection<Module> module;
//	private Collection<Sequence> sequence;
//	private Collection<SBOLCollection> SBOLCollection;
//	
//	
//	public Collection<Context> getContext() {
//		return context;
//	}
//
//	public Collection<GenericComponent> getGenericComponent() {
//		return genericComponent;
//	}
//
//	public Collection<SequenceComponent> getSequenceComponent() {
//		return sequenceComponent;
//	}
//
//	public Collection<Model> getModel() {
//		return model;
//	}
//
//	public Collection<Module> getModule() {
//		return module;
//	}
//
//	public Collection<Sequence> getSequence() {
//		return sequence;
//	}
//
//	public Collection<SBOLCollection> getSBOLCollection() {
//		return SBOLCollection;
//	}
//	
//	/**
//	 * Create a new {@link Context} instance.
//	 * @param identity
//	 * @param displayId
//	 * @param type
//	 * @return
//	 */
//	public Context createContext(URI identity, String displayId, URI type) {		
//		Context newContext = new Context(identity, displayId, type);
//		context.add(newContext);
//		return newContext;
//	}
//	
//	/**
//	 * Create a new {@link GenericComponent} instance.
//	 * @param identity
//	 * @param displayId
//	 * @param type
//	 * @return
//	 */
//	public GenericComponent createGenericComponent(URI identity, String displayId, URI type) {		
//		GenericComponent newGenerGenericComponent = new GenericComponent(identity, displayId, type);
//		genericComponent.add(newGenerGenericComponent);
//		return newGenerGenericComponent;
//	}
//	
//	/**
//	 * Create a new {@link SequenceComponent} instance.
//	 * @param identity
//	 * @param displayId
//	 * @param type
//	 * @return
//	 */
//	public SequenceComponent createSequenceComponent(URI identity, String displayId, URI type) {
//		SequenceComponent newSequenceComponent = new SequenceComponent(identity, displayId, type);
//		sequenceComponent.add(newSequenceComponent);
//		return newSequenceComponent;
//	}
//
//	/**
//	 * Create a new {@link Model} instance.
//	 * @param identity
//	 * @param displayId
//	 * @param source
//	 * @param language
//	 * @param framework
//	 * @param role
//	 * @return
//	 */
//	public Model createModel(URI identity, String displayId, URI source, URI language, URI framework, URI role) {
//		Model newModel = new Model(identity, displayId, source, language, framework, role);
//		model.add(newModel);
//		return newModel;
//	}
//	
//	/**
//	 * Create a new {@link Module} instance.
//	 * @param identity
//	 * @param displayId
//	 * @return
//	 */
//	public Module createModule(URI identity, String displayId) {
//		Module newModule = new Module(identity, displayId);
//		module.add(newModule);
//		return newModule;
//	}
//	
//	/**
//	 * Create a new {@link Sequence} instance.
//	 * @param identity
//	 * @param elements
//	 * @return
//	 */
//	public Sequence createSequence(URI identity, String elements) {
//		Sequence newSequence = new Sequence(identity, elements);
//		sequence.add(newSequence);
//		return newSequence;
//	}
//	
//	/**
//	 * Create a new {@link SBOLCollection} instance.
//	 * @param identity
//	 * @param displayId
//	 * @return
//	 */
//	public SBOLCollection createSBOLCollection(URI identity, String displayId) {
//		SBOLCollection newSBOLCollection = new SBOLCollection(identity, displayId);
//		SBOLCollection.add(newSBOLCollection);
//		return newSBOLCollection;
//	}
//	
//	/**
//	 * Returns the top-level objects ({@link #SequenceComponent}, {@link #Sequence}, and {@link SBOLCollection}) contained in this document. 
//	 * Top level objects may contain other SBOL objects. For
//	 * example, a {@link #SequenceComponent} may contain a {@link #Sequence} and multiple {@link #SequenceAnnotation}s which
//	 * will not be included in the results of this function. 
//	 * @deprecated
//	 */
//	public List<Identified> getContents() {
//		ArrayList<Identified> identifiedList = new ArrayList<Identified>();
//		identifiedList.addAll(sequenceComponent);
//		identifiedList.addAll(sequence);
//		identifiedList.addAll(SBOLCollection);
//		return identifiedList;
//	}
//	
//	/**
//	 * 
//	 * @param obj
//	 * @deprecated
//	 */
//	public void addContent(Identified obj) {
//		if (obj == null) {
//			throw new NullPointerException();
//		}
//		else if (obj instanceof SequenceComponent) {
//			sequenceComponent.add((SequenceComponent) obj);
//		}
//		else if (obj instanceof Sequence) {
//			sequence.add((Sequence) obj);
//		}
//		else if (obj instanceof SBOLCollection) {
//			SBOLCollection.add((SBOLCollection) obj);
//		}
//		else {			
//			throw new IllegalArgumentException("Identified conent to be added must be a SequenceComponent ...");
//		}
//	}
//
//	/**
//	 * Removes a top level object from the document.
//	 * @param obj
//	 * @deprecated
//	 */
//	public void removeContent(Identified obj) {	
//		if (obj == null) {
//			throw new NullPointerException();
//		}
//		else if (obj instanceof SequenceComponent) {
//			sequenceComponent.remove((SequenceComponent) obj);
//		}
//		else if (obj instanceof Sequence) {
//			sequence.remove((Sequence) obj);
//		}
//		else if (obj instanceof SBOLCollection) {
//			SBOLCollection.remove((SBOLCollection) obj);
//		}
//		else {
//			throw new IllegalArgumentException("Identified conent to be added must be a SequenceComponent ...");			
//		}
//
//	}
//	
//    public UriResolver<Collection> getCollectionUriResolver() {
//        return new UriResolver<Collection>() {
//            @Override
//            public Collection resolve(final URI uri) {
//                final List<Collection> found = new ArrayList<Collection>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(Collection coll) {
//                        if(coll.getURI().equals(uri))
//                            found.add(coll);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
//    @Override
//    @XmlTransient
//    public UriResolver<DnaComponent> getComponentUriResolver() {
//        return new UriResolver<DnaComponent>() {
//            @Override
//            public DnaComponent resolve(final URI uri) {
//                final List<DnaComponent> found = new ArrayList<DnaComponent>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(DnaComponent dc) {
//                        if(dc.getURI().equals(uri))
//                            found.add(dc);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
//    @Override
//    @XmlTransient
//    public UriResolver<DnaSequence> getSequenceUriResolver() {
//        return new UriResolver<DnaSequence>() {
//            @Override
//            public DnaSequence resolve(final URI uri) {
//                final List<DnaSequence> found = new ArrayList<DnaSequence>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(DnaSequence ds) {
//                        if(ds.getURI().equals(uri))
//                            found.add(ds);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
//    @Override
//    @XmlTransient
//    public UriResolver<StructuralAnnotation> getAnnotationUriResolver() {
//        return new UriResolver<StructuralAnnotation>() {
//            @Override
//            public StructuralAnnotation resolve(final URI uri) {
//                final List<StructuralAnnotation> found = new ArrayList<StructuralAnnotation>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(StructuralAnnotation annotation) {
//                        if(annotation.getURI().equals(uri))
//                            found.add(annotation);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
//    @Override
//    @XmlTransient
//    public DisplayIdResolver<Collection> getCollectionDisplayIdResolver() {
//        return new DisplayIdResolver<Collection>() {
//            @Override
//            public Collection resolve(final String displayId) {
//                final List<Collection> found = new ArrayList<Collection>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(Collection coll) {
//                        if(coll.getDisplayId().equals(displayId))
//                            found.add(coll);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
//    @Override
//    @XmlTransient
//    public DisplayIdResolver<DnaComponent> getComponentDisplayIdResolver() {
//        return new DisplayIdResolver<DnaComponent>() {
//            @Override
//            public DnaComponent resolve(final String displayId) {
//                final List<DnaComponent> found = new ArrayList<DnaComponent>();
//
//                accept(new SBOLBaseVisitor<RuntimeException>() {
//                    @Override
//                    public void visit(DnaComponent component) {
//                        if(component.getDisplayId().equals(displayId))
//                            found.add(component);
//                    }
//                });
//
//                // fixme: should merge these
//                return found.isEmpty() ? null : found.get(0);
//            }
//        };
//    }
//
	@Override
	public <T extends Throwable> void accept(SBOLVisitor<T> visitor) throws T {
		// TODO Auto-generated method stub
		
	}

	
	
	
}
