package gr.uom.java.xmi;

import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

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
			ITypeBinding paramType = parameterTypes[i];
			if (paramType.isTypeVariable() || paramType.isWildcardType()) {
				sb.append(paramType.getName());
			} else {
				sb.append(paramType.getErasure().getName());
			}
		}
		sb.append(')');
	    return sb.toString();
    }

	public static String getKey(String packageName, String className, MethodDeclaration declaration) {
		StringBuilder sb = new StringBuilder();
		if (packageName != null && !packageName.isEmpty()) {
			sb.append(packageName);
			sb.append('.');
		}
		sb.append(className);
		sb.append('.');
		sb.append(declaration.getName().getIdentifier());
		sb.append('(');
		List<SingleVariableDeclaration> parameters = (List<SingleVariableDeclaration>) declaration.parameters();
		for (int i = 0; i < parameters.size(); i++) {
			if (i != 0)
				sb.append(',');
			Type paramType = parameters.get(i).getType();
			sb.append(paramType.toString());
		}
		sb.append(')');
	    return sb.toString();
    }

	public static String extractMethodSignature(MethodDeclaration declaration) {
		String dcl = declaration.toString();
		int begin = dcl.indexOf("*/");
		if (begin < 0) {
			begin = 0;
		} else {
			begin = Math.max(begin + 2, dcl.length() - 1);
		}
		int end = dcl.indexOf(")", begin);
		if (end < 0) {
			end = dcl.length();
		} else {
			end = end + 1;
		}
		return dcl.substring(begin, end).trim();
	}

}
