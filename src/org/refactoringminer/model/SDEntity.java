package org.refactoringminer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SDEntity implements Comparable<SDEntity> {

	private int id;
	protected final SDModel.Snapshot snapshot;
	protected final String fullName;
	protected final SDContainerEntity container;
	protected final List<SDEntity> children;
	private MembersRepresentation members = null;
	
	public SDEntity(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		this.snapshot = snapshot;
		this.id = id;
		this.fullName = fullName;
		this.container = container;
		this.children = new ArrayList<SDEntity>();
		if (container != null) {
		    this.container.children.add(this);
		}
	}
	
	public int getId() {
		return id;
	}
	
	int setId(int id) {
		return this.id = id;
	}
	
	public String fullName() {
		return fullName;
	}

	public String fullName(SDEntity parent) {
		return parent.fullName() + getNameSeparator() + simpleName();
	}
	
	protected abstract String getNameSeparator();

	public abstract String simpleName();
	
	public SDContainerEntity container() {
		return container;
	}
	
	public abstract boolean isTestCode();
	
	@Override
	public int hashCode() {
//		return fullName.hashCode();
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SDEntity) {
		    SDEntity e = (SDEntity) obj;
			return id == e.id;
			//return e.fullName.equals(fullName);
		}
		return false;
	}

	@Override
	public String toString() {
		return fullName;
	}

	public Iterable<SDEntity> children() {
		return children;
	}
	
	@Override
	public int compareTo(SDEntity o) {
		return fullName.compareTo(o.fullName);
	}

	public SourceRepresentation sourceCode() {
	    throw new UnsupportedOperationException();
	}

	public long simpleNameHash() {
	    long h = 0;
	    String simpleName = this.simpleName();
        for (int i = 0; i < simpleName.length(); i++) {
            h = 31 * h + simpleName.charAt(i);
        }
        return h;
	}
	
	public MembersRepresentation membersRepresentation() {
	    if (this.members == null) {
	        Map<Long, String> debug = new HashMap<Long, String>();
	        long[] hashes = new long[this.children.size()];
	        for (int i = 0; i < hashes.length; i++) {
	            hashes[i] = this.children.get(i).simpleNameHash();
	            debug.put(hashes[i], this.children.get(i).simpleName());
	        }
	        Arrays.sort(hashes);
	        this.members = new MembersRepresentation(hashes, debug);
	    }
	    return this.members;
	}

	public void addReferencedBy(SDMethod method) {
	    throw new UnsupportedOperationException();
	}

	public void addReferencedBy(SDType type) {
	    throw new UnsupportedOperationException();
	}
	
	public void addReference(SDEntity entity) {
	    throw new UnsupportedOperationException();
	}
	
	public boolean isAnonymous() {
        return false;
    }
}
