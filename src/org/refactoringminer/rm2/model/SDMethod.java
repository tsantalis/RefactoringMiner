package org.refactoringminer.rm2.model;

import java.util.ArrayList;
import java.util.List;


public class SDMethod extends SDEntity {

    private String identifier;
	private String signature;
	private final boolean constructor;
	private boolean testAnnotation;
	private boolean deprecatedAnnotation;
	private boolean isAbstract;
	private int numberOfStatements;
	private Multiset<SDMethod> callers;
	private Multiset<SDMethod> origins;
	private Multiset<SDMethod> inlinedTo;
	private SourceRepresentation body;
	private Visibility visibility;
	private List<Parameter> parameters;
	private String returnType;

	public SDMethod(SDModel.Snapshot snapshot, int id, String signature, SDContainerEntity container, boolean constructor) {
		super(snapshot, id, container.fullName() + "#" + signature, new EntityKey(container.key() + "#" + signature), container);
		this.signature = signature;
		this.identifier = signature.substring(0, signature.indexOf('('));
		this.callers = new Multiset<SDMethod>();
		this.origins = new Multiset<SDMethod>();
		this.inlinedTo = new Multiset<SDMethod>();
		this.parameters = new ArrayList<Parameter>();
		this.constructor = constructor;
	}

	public Multiset<SDMethod> callers() {
		return this.callers;
	}
	
	@Override
	public String simpleName() {
		return signature;
	}

	public String identifier() {
	    return identifier;
	}

	public Multiset<SDMethod> origins() {
		return this.origins;
	}

	public Multiset<SDMethod> inlinedTo() {
	    return this.inlinedTo;
	}

	public boolean delegatesTo(SDMethod other) {
		return other.callers.contains(this) && numberOfStatements <= 3;
	}

	public boolean isDeprecated() {
		return deprecatedAnnotation;
	}

	@Override
	public boolean isTestCode() {
		return testAnnotation || container.isTestCode();
	}

	public boolean isOverriden() {
		SDType parent = (SDType) this.container;
		for (SDType subtype : parent.subtypes()) {
		  EntityKey overridenMethodKey = new EntityKey(subtype.key() + "#" + signature);
			if (snapshot.find(SDMethod.class, overridenMethodKey) != null) {
				return true;
			}
		}
		return false;
	}

	public SDType container() {
        return (SDType) this.container;
    }
	
	public boolean isRecursive() {
		return this.callers().contains(this);
	}

	public SourceRepresentation sourceCode() {
		return this.body;
	}
	
	public Visibility visibility() {
	    return this.visibility;
	}
	
	@Override
	protected final String getNameSeparator() {
		return "#";
	}

	public int invocationsCount(SDMethod caller) {
		return callers.getMultiplicity(caller);
	}
	
	public boolean isConstructor() {
		return constructor;
	}

	public boolean isAbstract() {
	    return isAbstract;
	}

	// Builder methods
	
	@Override
	public void addReference(SDEntity entity) {
	    entity.addReferencedBy(this);
	}
	
	@Override
	public void addReferencedBy(SDMethod method) {
	    this.callers.add(method);
	}
	
	public void addOrigin(SDMethod method, int multiplicity) {
		this.origins.add(method, multiplicity);
	}

	public void addInlinedTo(SDMethod method, int multiplicity) {
	    this.inlinedTo.add(method, multiplicity);
	}

	public void setTestAnnotation(boolean testAnnotation) {
		this.testAnnotation = testAnnotation;
	}

	public void setDeprecatedAnnotation(boolean deprecatedAnnotation) {
		this.deprecatedAnnotation = deprecatedAnnotation;
	}

	public void setNumberOfStatements(int numberOfStatements) {
		this.numberOfStatements = numberOfStatements;
	}

	public void setSourceCode(SourceRepresentation body) {
		this.body = body;
	}

	public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

	public void setVisibility(Visibility visibility) {
	    this.visibility = visibility;
	}

	public void addParameter(String name, String type) {
        this.parameters.add(new Parameter(name, type));
    }
	
	public void setReturnType(String returnType) {
	    this.returnType = returnType;
	}
	
	public static class Parameter {
	    private final String name;
	    private final String type;
        
	    public Parameter(String name, String type) {
            super();
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
	    
	}

	public String getVerboseSimpleName() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.visibility);
        sb.append(' ');
        if (isConstructor()) {
            sb.append(container.simpleName());
        } else {
            sb.append(identifier);
        }
        sb.append('(');
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Parameter param = parameters.get(i);
            sb.append(param.getName());
            sb.append(' ');
            sb.append(param.getType());
        }
        sb.append(')');
        if (!isConstructor()) {
            sb.append(" : ");
            sb.append(returnType);
        }
        return sb.toString();
    }

	@Override
	public boolean isAnonymous() {
	    return container().isAnonymous();
	}

    public boolean isSetter() {
        return simpleName().startsWith("set") && numberOfStatements == 1 && parameters.size() == 1;
    }

    public boolean isGetter() {
        return simpleName().startsWith("get") && numberOfStatements == 1 && parameters.size() == 0;
    }

    @Override
    public String toString() {
        if (isConstructor()) {
            String result = super.toString();
            return result.replace("#(", "#" + container.simpleName() + "(");
        }
        return super.toString();
    }
}
