package org.refactoringminer.rm2.model;



public class SDAttribute extends SDEntity {

	private final String name; 
	private Visibility visibility;
    private String type;
    private boolean isStatic;
    private Multiset<SDMethod> referencedBy;
    private SourceRepresentation assignment;
    private SourceRepresentation clientCode;
	
	public SDAttribute(SDModel.Snapshot snapshot, int id, String name, SDContainerEntity container) {
		super(snapshot, id, container.fullName() + "#" + name, new EntityKey(container.key() + "#" + name), container);
		this.name = name;
		this.referencedBy = new Multiset<SDMethod>();
		this.clientCode = new SourceRepresentation(new long[0]);
	}

	@Override
	public String simpleName() {
		return name;
	}
	
	@Override
	public boolean isTestCode() {
		return container.isTestCode();
	}

	@Override
	protected final String getNameSeparator() {
		return "#";
	}

    public Visibility visibility() {
        return visibility;
    }

    public String type() {
        return type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public SDType container() {
        return (SDType) this.container;
    }
    
    public SourceRepresentation clientCode() {
        return clientCode;
    }

    public Multiset<SDMethod> referencedBy() {
        return referencedBy;
    }

    @Override
    public void addReferencedBy(SDMethod method) {
        this.referencedBy.add(method);
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public void setClientCode(SourceRepresentation clientCode) {
        this.clientCode = clientCode;
    }

    public SourceRepresentation assignment() {
        return assignment;
    }

    public void setAssignment(SourceRepresentation assignment) {
        this.assignment = assignment;
    }

    public String getVerboseSimpleName() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.visibility);
        sb.append(' ');
        sb.append(name);
        sb.append(" : ");
        sb.append(type);
        return sb.toString();
    }

    @Override
    public boolean isAnonymous() {
        return container().isAnonymous();
    }
}
