package gr.uom.java.xmi;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class ASTUtils {

	public static String getKey(IMethodBinding binding) {
		StringBuilder sb = new StringBuilder();
		sb.append(binding.getDeclaringClass().getErasure().getQualifiedName());
		sb.append('.');
		sb.append(binding.getName());
		sb.append('(');
		final ITypeBinding[] parameterTypes = binding.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			if (i != 0)
				sb.append(',');
			sb.append(parameterTypes[i].getErasure().getName());
		}
		sb.append(')');
	    return sb.toString();
    }

}
