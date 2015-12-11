package br.ufmg.dcc.labsoft.refdetector.model;



public class SDAttribute extends SDEntity {

	private final String name; 
	private Visibility visibility;
    private String type;
    private boolean isStatic;
	
	public SDAttribute(SDModel.Snapshot snapshot, int id, String name, SDContainerEntity container) {
		super(snapshot, id, container.fullName() + "#" + name, container);
		this.name = name;
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
    
    public void setType(String type) {
        this.type = type;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
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

}
