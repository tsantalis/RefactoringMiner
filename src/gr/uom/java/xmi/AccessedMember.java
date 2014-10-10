package gr.uom.java.xmi;

import java.io.Serializable;

public abstract class AccessedMember implements Comparable<AccessedMember>, Serializable {

	public int compareTo(AccessedMember o) {
		if(this instanceof MethodCall && o instanceof MethodCall) {
			MethodCall mc1 = (MethodCall)this;
			MethodCall mc2 = (MethodCall)o;
			return mc1.toString().compareTo(mc2.toString());
		}
		if(this instanceof FieldAccess && o instanceof FieldAccess) {
			FieldAccess fa1 = (FieldAccess)this;
			FieldAccess fa2 = (FieldAccess)o;
			return fa1.toString().compareTo(fa2.toString());
		}
		if(this instanceof MethodCall && o instanceof FieldAccess)
			return 1;
		if(this instanceof FieldAccess && o instanceof MethodCall)
			return -1;
		return 0;
	}
}
