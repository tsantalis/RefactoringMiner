package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public abstract class DependenciesAstVisitor extends ASTVisitor {

    private boolean onlyFromSource = false;
    
	public DependenciesAstVisitor(boolean onlyFromSource) {
        this.onlyFromSource = onlyFromSource;
    }

    @Override
	public final boolean visit(MethodInvocation node) {
		IMethodBinding methodBinding = node.resolveMethodBinding();
		if (methodBinding != null) {
		    handleTypeBinding(node, methodBinding.getDeclaringClass(), false);
		    handleMethodBinding(node, methodBinding);
		}
		return true;
	}

	@Override
	public final boolean visit(FieldAccess node) {
		IVariableBinding fieldBinding = node.resolveFieldBinding();
		if (fieldBinding != null) {
		    handleTypeBinding(node, fieldBinding.getDeclaringClass(), false);
		    handleFieldBinding(node, fieldBinding);
		}
		return true;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
	    IVariableBinding fieldBinding = node.resolveFieldBinding();
	    if (fieldBinding != null) {
    	    handleTypeBinding(node, fieldBinding.getDeclaringClass(), false);
            handleFieldBinding(node, fieldBinding);
	    }
        return true;
	}

	@Override
	public final boolean visit(SimpleName node) {
		return visitNameNode(node);
	}

	@Override
	public final boolean visit(QualifiedName node) {
		return visitNameNode(node);
	}

	private boolean visitNameNode(Name node) {
		IBinding binding = node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			IVariableBinding variableBindig = (IVariableBinding) binding;
			if (variableBindig.isField()) {
				ITypeBinding declaringClass = variableBindig.getDeclaringClass();
				handleTypeBinding(node, declaringClass, false);
				handleFieldBinding(node, variableBindig);
			} else if (!variableBindig.isEnumConstant()) {
				handleVariableBinding(node, variableBindig);
			}
		}
		return true;
	}

	@Override
	public final boolean visit(ClassInstanceCreation node) {
		ITypeBinding typeBinding = node.getType().resolveBinding();
		handleTypeBinding(node, typeBinding, true);
		return true;
	}

	@Override
	public final boolean visit(VariableDeclarationStatement node) {
		ITypeBinding typeBinding = node.getType().resolveBinding();
		handleTypeBinding(node, typeBinding, true);
		//typeBinding.get
		//supertypes
		return true;
	}

	@Override
	public final boolean visit(CatchClause node) {
		ITypeBinding typeBinding = node.getException().getType().resolveBinding();
		handleTypeBinding(node, typeBinding, true);
		return true;
	}

	@Override
	public final boolean visit(MarkerAnnotation node) {
		ITypeBinding typeBinding = node.getTypeName().resolveTypeBinding();
		handleTypeBinding(node, typeBinding, true);
		return true;
	}

	@Override
	public final boolean visit(CastExpression node) {
		Type type = node.getType();
		handleTypeBinding(node, type.resolveBinding(), true);
		return true;
	}

	@Override
	public final boolean visit(TypeLiteral node) {
		Type type = node.getType();
		handleTypeBinding(node, type.resolveBinding(), true);
		return true;
	}

	protected void onTypeAccess(ASTNode node, ITypeBinding binding) {
		// override
	}

	protected void onVariableAccess(ASTNode node, IVariableBinding binding) {
		// override
	}

	protected void onFieldAccess(ASTNode node, IVariableBinding binding) {
	    // override
	}

	protected void onMethodAccess(ASTNode node, IMethodBinding binding) {
		// override
	}

	private void handleTypeBinding(ASTNode node, ITypeBinding typeBinding, boolean includeTypeParameters) {
		if (typeBinding == null) {
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			//System.out.println(locationInParent.getId() + " has no type binding");
		} else {
			List<ITypeBinding> rawTypes = new ArrayList<ITypeBinding>();
			Set<String> dejavu = new HashSet<String>();
			this.appendRawTypes(rawTypes, dejavu, typeBinding, includeTypeParameters);
			for (ITypeBinding rawType : rawTypes) {
				if (!this.ignoreType(rawType)) {
					this.onTypeAccess(node, rawType);
				}
			}
			
		}
	}

	private void appendRawTypes(List<ITypeBinding> rawTypes, Set<String> dejavu, ITypeBinding typeBinding, boolean includeTypeParameters) {
		String key = typeBinding.getKey();
		if (dejavu.contains(key)) {
			return;
		}
		dejavu.add(key);
		ITypeBinding erasure = typeBinding.getErasure();
		rawTypes.add(erasure);
		
		if (!includeTypeParameters) {
			return;
		}
		
		ITypeBinding elementType = typeBinding.getElementType();
		if (elementType != null) {
			this.appendRawTypes(rawTypes, dejavu, elementType, includeTypeParameters);
		}
		
		ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
		if (typeArguments != null) {
			for (ITypeBinding typeArgument : typeArguments) {
				this.appendRawTypes(rawTypes, dejavu, typeArgument, includeTypeParameters);
			}
		}
		
		ITypeBinding[] typeBounds = typeBinding.getTypeBounds();
		if (typeBounds != null) {
			for (ITypeBinding typeBound : typeBounds) {
				this.appendRawTypes(rawTypes, dejavu, typeBound, includeTypeParameters);
			}
		}
    }

	private void handleVariableBinding(ASTNode node, IVariableBinding variableBindig) {
		if (variableBindig == null) {
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			//System.out.println(locationInParent.getId() + " has no variable binding");
		} else {
			this.onVariableAccess(node, variableBindig);
		}
	}

	private void handleFieldBinding(ASTNode node, IVariableBinding variableBindig) {
		if (variableBindig == null) {
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			//System.out.println(locationInParent.getId() + " has no field binding");
		} else {
			ITypeBinding declaringClass = variableBindig.getDeclaringClass();
			if (declaringClass != null && !this.ignoreType(declaringClass)) {
				this.onFieldAccess(node, variableBindig);
			}
		}
	}

	private void handleMethodBinding(ASTNode node, IMethodBinding methodBinding) {
		if (methodBinding == null) {
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			//System.out.println(locationInParent.getId() + " has no method binding");
		} else {
			ITypeBinding declaringClass = methodBinding.getDeclaringClass();
			if (declaringClass != null && !this.ignoreType(declaringClass)) {
				this.onMethodAccess(node, methodBinding);
			}
		}
	}

	protected boolean ignoreType(ITypeBinding typeBinding) {
		return this.onlyFromSource && !typeBinding.isFromSource();
	}

}
