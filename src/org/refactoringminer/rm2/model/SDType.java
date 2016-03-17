package org.refactoringminer.rm2.model;

import java.util.ArrayList;
import java.util.List;

public class SDType extends SDContainerEntity {

	private String simpleName;
	private String sourceFilePath;
	private boolean isInterface;
	private boolean isAnonymous;
	private boolean deprecatedAnnotation;
	private EntitySet<SDType> subtypes = new EntitySet<SDType>();
	private List<SDType> anonymousClasses = new ArrayList<SDType>();
	private SourceRepresentation source;
	private int nestingLevel;
	private Multiset<SDType> origins;
	private Multiset<SDType> referencedBy;
	
	public SDType(SDModel.Snapshot snapshot, int id, String simpleName, SDContainerEntity container, String sourceFilePath) {
		this(snapshot, id, simpleName, container.fullName() + "." + simpleName, container, sourceFilePath, false);
	}

	private SDType(SDModel.Snapshot snapshot, int id, int anonymousId, SDType container, String sourceFilePath) {
	    this(snapshot, id, "" + anonymousId, container.fullName() + "$" + anonymousId, container, sourceFilePath, true);
	}

	private SDType(SDModel.Snapshot snapshot, int id, String simpleName, String fullName, SDContainerEntity container, String sourceFilePath, boolean isAnonymous) {
	    super(snapshot, id, fullName, container);
	    this.simpleName = simpleName;
	    this.sourceFilePath = sourceFilePath;
	    this.isAnonymous = isAnonymous;
	    if (container instanceof SDType) {
	        SDType parentType = (SDType) container;
	        this.nestingLevel = parentType.nestingLevel + 1;
	    } else {
	        this.nestingLevel = 0;
	    }
	    this.origins = new Multiset<SDType>();
	    this.referencedBy = new Multiset<SDType>();
	}
	
	@Override
	protected final String getNameSeparator() {
		return isAnonymous ? "$" : ".";
	}
	
	@Override
	public String simpleName() {
		return simpleName;
	}
	
	public boolean isSubtypeOf(SDType supertype) {
		return supertype.subtypes.contains(this);
	}

	public boolean isInterface() {
		return isInterface;
	}

	public boolean isAnonymous() {
	    return isAnonymous;
	}
	
	@Override
	public boolean isPackage() {
		return false;
	}
	
	public List<SDType> anonymousClasses() {
		return anonymousClasses;
	}
	
	public SourceRepresentation sourceCode() {
        return this.source;
    }
	
	@Override
	public boolean isTestCode() {
		if (simpleName.endsWith("Test") || simpleName.startsWith("Test")) {
			return true;
		}
		
		boolean test = false;
		String[] parts = sourceFilePath.split("/");
		for (String part : parts) {
			test = test || part.equals("test") || part.equals("tests") || part.equals("src-test");
		}
		return test;
	}

	public boolean isDeprecated() {
		return deprecatedAnnotation;
	}

	public EntitySet<SDType> subtypes() {
		return subtypes;
	}
	
	public int nestingLevel() {
        return nestingLevel;
    }

	public Multiset<SDType> origins() {
        return this.origins;
    }
	
	public Multiset<SDType> referencedBy() {
        return referencedBy;
    }

    @Override
    public void addReference(SDEntity entity) {
        entity.addReferencedBy(this);
    }
    
    @Override
    public void addReferencedBy(SDType type) {
        this.referencedBy.add(type);
    }
	
    public void addSubtype(SDType type) {
		subtypes.add(type);
	}

	public SDType addAnonymousClass(int id, int anonId) {
		SDType anon = new SDType(snapshot, id, anonId, this, sourceFilePath);
		anonymousClasses.add(anon);
		return anon;
	}

	public void setDeprecatedAnnotation(boolean deprecatedAnnotation) {
		this.deprecatedAnnotation = deprecatedAnnotation;
	}

	public void setIsInterface(boolean isInterface) {
	    this.isInterface = isInterface;
	}

	public void setSourceCode(SourceRepresentation source) {
        this.source = source;
    }
	
	public void addOrigin(SDType type, int multiplicity) {
        this.origins.add(type, multiplicity);
    }
}
