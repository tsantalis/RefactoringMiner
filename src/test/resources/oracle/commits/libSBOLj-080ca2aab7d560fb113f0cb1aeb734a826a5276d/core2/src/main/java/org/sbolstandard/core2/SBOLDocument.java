package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;

/**
 * @author Zhen Zhang
 *
 */
/**
 * @author zhangz
 *
 */
public class SBOLDocument {
	
	private	Collection<Context> context;
	private Collection<GenericComponent> genericComponent;
	private Collection<SequenceComponent> sequenceComponent;
	private Collection<Model> model;
	private Collection<Module> module;
	private Collection<Sequence> sequence;
	private Collection<SBOLCollection> SBOLCollection;
	
	
	public Collection<Context> getContext() {
		return context;
	}

	public Collection<GenericComponent> getGenericComponent() {
		return genericComponent;
	}

	public Collection<SequenceComponent> getSequenceComponent() {
		return sequenceComponent;
	}

	public Collection<Model> getModel() {
		return model;
	}

	public Collection<Module> getModule() {
		return module;
	}

	public Collection<Sequence> getSequence() {
		return sequence;
	}

	public Collection<SBOLCollection> getSBOLCollection() {
		return SBOLCollection;
	}
	
	/**
	 * Create a new {@link Context} instance.
	 * @param identity
	 * @param displayId
	 * @param type
	 * @return
	 */
	public Context createContext(URI identity, String displayId, URI type) {		
		Context newContext = new Context(identity, displayId, type);
		context.add(newContext);
		return newContext;
	}
	
	/**
	 * Create a new {@link GenericComponent} instance.
	 * @param identity
	 * @param displayId
	 * @param type
	 * @return
	 */
	public GenericComponent createGenericComponent(URI identity, String displayId, URI type) {		
		GenericComponent newGenerGenericComponent = new GenericComponent(identity, displayId, type);
		genericComponent.add(newGenerGenericComponent);
		return newGenerGenericComponent;
	}
	
	/**
	 * Create a new {@link SequenceComponent} instance.
	 * @param identity
	 * @param displayId
	 * @param type
	 * @return
	 */
	public SequenceComponent createSequenceComponent(URI identity, String displayId, URI type) {
		SequenceComponent newSequenceComponent = new SequenceComponent(identity, displayId, type);
		sequenceComponent.add(newSequenceComponent);
		return newSequenceComponent;
	}

	/**
	 * Create a new {@link Model} instance.
	 * @param identity
	 * @param displayId
	 * @param source
	 * @param language
	 * @param framework
	 * @param role
	 * @return
	 */
	public Model createModel(URI identity, String displayId, URI source, URI language, URI framework, URI role) {
		Model newModel = new Model(identity, displayId, source, language, framework, role);
		model.add(newModel);
		return newModel;
	}
	
	/**
	 * Create a new {@link Module} instance.
	 * @param identity
	 * @param displayId
	 * @return
	 */
	public Module createModule(URI identity, String displayId) {
		Module newModule = new Module(identity, displayId);
		module.add(newModule);
		return newModule;
	}
	
	/**
	 * Create a new {@link Sequence} instance.
	 * @param identity
	 * @param elements
	 * @return
	 */
	public Sequence createSequence(URI identity, String elements) {
		Sequence newSequence = new Sequence(identity, elements);
		sequence.add(newSequence);
		return newSequence;
	}
	
	/**
	 * Create a new {@link SBOLCollection} instance.
	 * @param identity
	 * @param displayId
	 * @return
	 */
	public SBOLCollection createSBOLCollection(URI identity, String displayId) {
		SBOLCollection newSBOLCollection = new SBOLCollection(identity, displayId);
		SBOLCollection.add(newSBOLCollection);
		return newSBOLCollection;
	}
	
	
	
	
}
