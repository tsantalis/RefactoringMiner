package gr.uom.java.xmi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationInfo {

	private Map<String, MethodInfo> map = new HashMap<String, MethodInfo>();
	private int internalInvocations = 0;
	private int externalInvocations = 0;
	private int unresolvedInvocations = 0;
	
	public static class MethodInfo {
		private final String bindingKey;
		private boolean external = true;
		private int count = 0;
		public MethodInfo(String bindingKey) {
			this.bindingKey = bindingKey;
		}
		public String getBindingKey() {
			return bindingKey;
		}
		public boolean isExternal() {
			return external;
		}
		public int getCount() {
			return count;
		}
	}
	
	public void handleMethodDeclaration(MethodDeclaration declaration) {
		IMethodBinding binding = declaration.resolveBinding();
		if (binding.isConstructor()) {
			return;
		}
		String bindingKey = binding.getKey();
		if (!map.containsKey(bindingKey)) {
			MethodInfo methodInfo = new MethodInfo(bindingKey);
			methodInfo.external = false;
			map.put(bindingKey, methodInfo);
		} else {
			MethodInfo info = map.get(bindingKey);
			this.externalInvocations -= info.count;
			this.internalInvocations += info.count;
		}
	}

	public void handleMethodInvocation(MethodDeclaration invoker, MethodInvocation invocation) {
		IMethodBinding invoked = invocation.resolveMethodBinding();
		if (invoked != null) {
			String bindingKey = invoked.getKey();
			if (!map.containsKey(bindingKey)) {
				MethodInfo methodInfo = new MethodInfo(bindingKey);
				methodInfo.count = 1;
				map.put(bindingKey, methodInfo);
				externalInvocations++;
			} else {
				MethodInfo methodInfo = map.get(bindingKey);
				methodInfo.count += 1;
				if (methodInfo.external) {
					externalInvocations++;
				} else {
					internalInvocations++;
				}
			}
//			System.out.println("invocation binding " + invoked.getKey());
		} else {
			unresolvedInvocations++;
//			System.out.println("invocation with NO BINDING " + methodInvocation.getName());
		}
	}

	public int getInternalInvocations() {
		return this.internalInvocations;
	}

	public int getExternalInvocations() {
		return this.externalInvocations + this.unresolvedInvocations;
	}

	public Collection<MethodInfo> getMethodInfoCollection() {
		return this.map.values();
	}
}
