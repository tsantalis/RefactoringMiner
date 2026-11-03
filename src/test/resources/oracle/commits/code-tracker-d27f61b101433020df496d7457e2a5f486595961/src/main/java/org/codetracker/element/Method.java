package org.codetracker.element;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Method extends BaseCodeElement {
    private final VariableDeclarationContainer umlOperation;
    private final String documentsHashCode;
    private final String identifierIgnoringVersionAndDocumentationAndBody;
    private final String identifierIgnoringVersionAndAnnotation;

    private Method(VariableDeclarationContainer umlOperation, String identifierIgnoringVersion, String identifierIgnoringVersionAndDocumentationAndBody, String identifierIgnoringVersionAndAnnotation, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.umlOperation = umlOperation;
        this.documentsHashCode = getDocumentsSha512(umlOperation);
        this.identifierIgnoringVersionAndDocumentationAndBody = identifierIgnoringVersionAndDocumentationAndBody;
        this.identifierIgnoringVersionAndAnnotation = identifierIgnoringVersionAndAnnotation;
    }

    public BaseCodeElement of(Version version) {
    	return of(this.umlOperation, version);
    }

    public static Method of(VariableDeclarationContainer umlOperation, Version version) {
        String sourceFolder = umlOperation.getLocationInfo().getSourceFolder();
        String identifierIgnoringVersion = null;
        String identifierIgnoringVersionAndDocumentationAndBody = null;
        String identifierIgnoringVersionAndAnnotation = null;
        String name = null;
        if (umlOperation instanceof UMLOperation) {
            identifierIgnoringVersion = getIdentifierExcludeVersion((UMLOperation) umlOperation, true, true, true);
            identifierIgnoringVersionAndDocumentationAndBody = getIdentifierExcludeVersion((UMLOperation) umlOperation, false, false, true);
            identifierIgnoringVersionAndAnnotation = getIdentifierExcludeVersion((UMLOperation) umlOperation, true, true, false);
            name = String.format("%s%s", sourceFolder, ((UMLOperation) umlOperation).getKey());
        }
        else if (umlOperation instanceof UMLInitializer) {
            identifierIgnoringVersion = getIdentifierExcludeVersion((UMLInitializer) umlOperation, true, true);
            identifierIgnoringVersionAndDocumentationAndBody = getIdentifierExcludeVersion((UMLInitializer) umlOperation, false, false);
            identifierIgnoringVersionAndAnnotation = identifierIgnoringVersion;
            name = String.format("%s%s", sourceFolder, umlOperation.getName());
        }
        return new Method(umlOperation, identifierIgnoringVersion, identifierIgnoringVersionAndDocumentationAndBody, identifierIgnoringVersionAndAnnotation, name, umlOperation.getLocationInfo().getFilePath(), version);
    }

    public boolean differInFormatting(Method other) {
    	if (umlOperation instanceof UMLOperation && other.umlOperation instanceof UMLOperation) {
	    	String thisSignature = ((UMLOperation) umlOperation).getActualSignature();
			String otherSignature = ((UMLOperation) other.umlOperation).getActualSignature();
			if (thisSignature != null && otherSignature != null) {
	    		return !thisSignature.equals(otherSignature) && thisSignature.replaceAll("\\s+","").equals(otherSignature.replaceAll("\\s+",""));
	    	}
    	}
    	return false;
    }

	public int signatureStartLine() {
		int methodSignatureStartLine = -1;
		if (umlOperation instanceof UMLOperation) {
			UMLOperation method = (UMLOperation) umlOperation;
			if (method.getModifiers().size() > 0)
				methodSignatureStartLine = method.getModifiers().get(0).getLocationInfo().getStartLine();
			else if (method.getReturnParameter() != null)
				methodSignatureStartLine = method.getReturnParameter().getType().getLocationInfo().getStartLine();
			else if (method.getParameterTypeList().size() > 0)
				methodSignatureStartLine = method.getParameterTypeList().get(0).getLocationInfo().getStartLine();
			else if (method.getThrownExceptionTypes().size() > 0)
				methodSignatureStartLine = method.getThrownExceptionTypes().get(0).getLocationInfo().getStartLine();
		}
		return methodSignatureStartLine;
	}

    public boolean isMultiLine() {
    	if (umlOperation.getBody() != null && umlOperation instanceof UMLOperation) {
    		int bodyStartLine = umlOperation.getBody().getCompositeStatement().getLocationInfo().getStartLine();
    		int methodSignatureStartLine = signatureStartLine();
    		if (methodSignatureStartLine != -1)
    			return bodyStartLine > methodSignatureStartLine;
    	}
    	return false;
    }

    public Variable findVariable(Predicate<Variable> equalOperator) {
        for (VariableDeclaration variableDeclaration : umlOperation.getAllVariableDeclarations()) {
            Variable variable = Variable.of(variableDeclaration, this);
            if (equalOperator.test(variable)) {
                return variable;
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (VariableDeclaration variableDeclaration : operation.getAllVariableDeclarations()) {
                    Variable variable = Variable.of(variableDeclaration, this);
                    if (equalOperator.test(variable)) {
                        return variable;
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            for (VariableDeclaration parameter : lambda.getParameters()) {
                Variable variable = Variable.of(parameter, this);
                if (equalOperator.test(variable)) {
                    return variable;
                }
            }
        }
        return null;
    }

    public Block findBlock(Predicate<Block> equalOperator) {
        if (umlOperation.getBody() != null) {
            for (AbstractStatement composite : umlOperation.getBody().getCompositeStatement().getAllStatements()) {
                Block block = Block.of(composite, this);
                if (block != null && equalOperator.test(block)) {
                    return block;
                }
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                if (operation.getBody() != null) {
                    for (AbstractStatement composite : operation.getBody().getCompositeStatement().getAllStatements()) {
                        Block block = Block.of(composite, this);
                        if (block != null && equalOperator.test(block)) {
                            return block;
                        }
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            if (lambda.getBody() != null) {
                for (AbstractStatement composite : lambda.getBody().getCompositeStatement().getAllStatements()) {
                    Block block = Block.of(composite, this);
                    if (block != null && equalOperator.test(block)) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    public Block findBlockWithoutName(Predicate<Block> equalOperator) {
        if (umlOperation.getBody() != null) {
        	//first process leaves then composites
            for (AbstractCodeFragment leaf : umlOperation.getBody().getCompositeStatement().getLeaves()) {
            	if (leaf instanceof StatementObject) {
	                Block block = Block.of((StatementObject)leaf, this);
	                if (block != null && equalOperator.test(block)) {
	                	/*if (leaf.getLambdas().size() > 0) {
	                		for (LambdaExpressionObject lambda : leaf.getLambdas()) {
	                			if(lambda.getBody() != null) {
	                				for (AbstractCodeFragment lambdaLeaf : lambda.getBody().getCompositeStatement().getLeaves()) {
	                					if (lambdaLeaf instanceof StatementObject) {
	                						Block lambdaBlock = Block.of((StatementObject)lambdaLeaf, this);
	                						if (lambdaBlock != null && equalOperator.test(lambdaBlock)) {
	                							return lambdaBlock;
	                						}
	                					}
	                				}
	                				for (CompositeStatementObject lambdaComposite : lambda.getBody().getCompositeStatement().getInnerNodes()) {
	                					Block lambdaBlock = Block.of(lambdaComposite, this);
	                					if (lambdaBlock != null && equalOperator.test(lambdaBlock)) {
	                						return lambdaBlock;
	                					}
	                				}
	                			}
	                		}
	                	}*/
	                    return block;
	                }
            	}
            }
            Map<CodeElementType, Block> matches = new LinkedHashMap<CodeElementType, Block>();
            for (CompositeStatementObject composite : umlOperation.getBody().getCompositeStatement().getInnerNodes()) {
                Block block = Block.of(composite, this);
                if (block != null && equalOperator.test(block)) {
                    matches.put(block.getLocation().getCodeElementType(), block);
                }
            }
            Block block = promotionStrategy(matches);
            if (block != null) {
            	return block;
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                if (operation.getBody() != null) {
                	//first process leaves then composites
                	for (AbstractCodeFragment leaf : operation.getBody().getCompositeStatement().getLeaves()) {
                    	if (leaf instanceof StatementObject) {
        	                Block block = Block.of((StatementObject)leaf, this);
        	                if (block != null && equalOperator.test(block)) {
        	                    return block;
        	                }
                    	}
                    }
                	Map<CodeElementType, Block> matches = new LinkedHashMap<CodeElementType, Block>();
                    for (CompositeStatementObject composite : operation.getBody().getCompositeStatement().getInnerNodes()) {
                        Block block = Block.of(composite, this);
                        if (block != null && equalOperator.test(block)) {
                        	matches.put(block.getLocation().getCodeElementType(), block);
                        }
                    }
                    Block block = promotionStrategy(matches);
                    if (block != null) {
                    	return block;
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            if (lambda.getBody() != null) {
            	//first process leaves then composites
            	for (AbstractCodeFragment leaf : lambda.getBody().getCompositeStatement().getLeaves()) {
                	if (leaf instanceof StatementObject) {
    	                Block block = Block.of((StatementObject)leaf, this);
    	                if (block != null && equalOperator.test(block)) {
    	                    return block;
    	                }
                	}
                }
            	Map<CodeElementType, Block> matches = new LinkedHashMap<CodeElementType, Block>();
                for (CompositeStatementObject composite : lambda.getBody().getCompositeStatement().getInnerNodes()) {
                    Block block = Block.of(composite, this);
                    if (block != null && equalOperator.test(block)) {
                    	matches.put(block.getLocation().getCodeElementType(), block);
                    }
                }
                Block block = promotionStrategy(matches);
                if (block != null) {
                	return block;
                }
            }
        }
        return null;
    }

	private Block promotionStrategy(Map<CodeElementType, Block> matches) {
		//promote catch over try
		if (matches.containsKey(CodeElementType.CATCH_CLAUSE)) {
			return matches.get(CodeElementType.CATCH_CLAUSE);
		}
		if (matches.containsKey(CodeElementType.FINALLY_BLOCK)) {
			return matches.get(CodeElementType.FINALLY_BLOCK);
		}
		for (CodeElementType key : matches.keySet()) {
			if (!key.equals(CodeElementType.BLOCK)) {
				return matches.get(key);
			}
			else if(matches.size() == 1 && matches.get(key).getComposite().getParent() != null &&
					!matches.get(key).getComposite().getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				return matches.get(key);
			}
		}
		return null;
	}

    public Comment findComment(Predicate<Comment> equalOperator) {
        for (UMLComment umlComment : umlOperation.getComments()) {
            Comment comment = Comment.of(umlComment, this);
            if (comment != null && equalOperator.test(comment)) {
                return comment;
            }
        }
        if (umlOperation instanceof UMLOperation) {
        	UMLJavadoc javadoc = ((UMLOperation) umlOperation).getJavadoc();
        	if (javadoc != null) {
        		Comment comment = Comment.of(javadoc, this);
        		if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
        	}
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
        	for (UMLComment umlComment : anonymousClass.getComments()) {
        		Comment comment = Comment.of(umlComment, this);
                if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
        	}
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (UMLComment umlComment : operation.getComments()) {
                    Comment comment = Comment.of(umlComment, this);
                    if (comment != null && equalOperator.test(comment)) {
                        return comment;
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            for (UMLComment umlComment : lambda.getComments()) {
                Comment comment = Comment.of(umlComment, this);
                if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
            }
        }
        return null;
    }

    public Annotation findAnnotation(Predicate<Annotation> equalOperator) {
        for (UMLAnnotation umlAnnotation : umlOperation.getAnnotations()) {
        	Annotation annotation = Annotation.of(umlAnnotation, this);
            if (annotation != null && equalOperator.test(annotation)) {
                return annotation;
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (UMLAnnotation umlAnnotation : operation.getAnnotations()) {
                	Annotation annotation = Annotation.of(umlAnnotation, this);
                    if (annotation != null && equalOperator.test(annotation)) {
                        return annotation;
                    }
                }
            }
        }
        return null;
    }

    public VariableDeclarationContainer getUmlOperation() {
        return umlOperation;
    }

    public boolean equalIdentifierIgnoringVersionAndDocumentAndBody(Method method) {
        return this.identifierIgnoringVersionAndDocumentationAndBody.equals(method.identifierIgnoringVersionAndDocumentationAndBody);
    }

    public boolean equalIdentifierIgnoringVersionAndAnnotation(Method method) {
        return this.identifierIgnoringVersionAndAnnotation.equals(method.identifierIgnoringVersionAndAnnotation);
    }

    public boolean equalDocuments(Method method) {
        if (this.documentsHashCode == null && method.documentsHashCode == null) return true;

        if (this.documentsHashCode == null || method.documentsHashCode == null) {
            return false;
        }
        return this.documentsHashCode.equals(method.documentsHashCode);
    }

    public boolean equalBody(Method method) {
        if (this.umlOperation.getBody() == null && method.umlOperation.getBody() == null) return true;

        if (this.umlOperation.getBody() == null || method.umlOperation.getBody() == null) {
            return false;
        }
        return this.umlOperation.getBody().getBodyHashCode() == method.umlOperation.getBody().getBodyHashCode();
    }

    public static String getIdentifierExcludeVersion(UMLOperation info, boolean containsBody, boolean containsDocumentation, boolean containsAnnotations) {
        StringBuilder sb = new StringBuilder();
        sb.append(info.getLocationInfo().getSourceFolder());
        sb.append(info.getClassName());
        sb.append(String.format("#(%s)", info.getVisibility().toString()));

        List<String> modifiers = new ArrayList<>();
        if (info.isStatic())
            modifiers.add("static");
        if (info.isAbstract())
            modifiers.add("abstract");
        if (info.isFinal())
            modifiers.add("final");

        if (info.isSynchronized())
            modifiers.add("synchronized");

        if (!modifiers.isEmpty()) {
            modifiers.sort(String::compareTo);
            sb.append(String.format("(%s)", String.join(",", modifiers)));
        }

        sb.append(info.getName());
        sb.append("(");
        sb.append(info.getParametersWithoutReturnType().stream().map(Method.MethodParameter::new).map(Objects::toString).collect(Collectors.joining(",")));
        sb.append(")");
        if (info.getReturnParameter() != null) {
            sb.append(":");
            sb.append(info.getReturnParameter());
        }
        if (!info.getThrownExceptionTypes().isEmpty()) {
            sb.append("[");
            sb.append(info.getThrownExceptionTypes().stream().map(Object::toString).collect(Collectors.joining(",")));
            sb.append("]");
        }
        if (containsBody && info.getBody() != null) {
            sb.append("{");
            sb.append(info.getBody().getBodyHashCode());
            sb.append("}");
        }
        if (containsDocumentation && !info.getComments().isEmpty()) {
            sb.append("{");
            sb.append(getDocumentsSha512(info));
            sb.append("}");
        }
        if (containsAnnotations) {
            sb.append(Util.annotationsToString(info.getAnnotations()));
        }
        return sb.toString();
    }

    public static String getIdentifierExcludeVersion(UMLInitializer info, boolean containsBody, boolean containsDocumentation) {
        StringBuilder sb = new StringBuilder();
        sb.append(info.getLocationInfo().getSourceFolder());
        sb.append(info.getClassName());
        sb.append("#");

        List<String> modifiers = new ArrayList<>();
        if (info.isStatic())
            modifiers.add("static");

        if (!modifiers.isEmpty()) {
            modifiers.sort(String::compareTo);
            sb.append(String.format("(%s)", String.join(",", modifiers)));
        }

        sb.append(info.getName());

        if (containsBody && info.getBody() != null) {
            sb.append("{");
            sb.append(info.getBody().getBodyHashCode());
            sb.append("}");
        }
        if (containsDocumentation && !info.getComments().isEmpty()) {
            sb.append("{");
            sb.append(getDocumentsSha512(info));
            sb.append("}");
        }
        return sb.toString();
    }

    public static String getDocumentsSha512(VariableDeclarationContainer info) {
        if (info.getComments().isEmpty())
            return null;
        return Util.getSHA512(info.getComments().stream().map(UMLComment::getFullText).collect(Collectors.joining(";")));
    }

	public void checkClosingBracket(int lineNumber) {
		if (getLocation().getEndLine() == lineNumber) {
			setClosingCurlyBracket(true);
		}
	}

    @Override
    public LocationInfo getLocation() {
        return umlOperation.getLocationInfo();
    }

    public static class MethodParameter {
        private final UMLParameter info;
        private final Set<UMLAnnotation> annotations;

        public MethodParameter(UMLParameter info) {
            this.info = info;
            annotations = new HashSet<>(info.getAnnotations());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodParameter that = (MethodParameter) o;
            return Objects.equals(this.info, that.info) &&
                    Objects.equals(this.info.getVariableDeclaration().isFinal(), that.info.getVariableDeclaration().isFinal()) &&
                    Objects.equals(this.info.getName(), that.info.getName()) &&
                    Objects.equals(this.annotations, that.annotations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(info, info.getName(), annotations, info.getVariableDeclaration().isFinal());
        }

        @Override
        public String toString() {
            return (this.info.getVariableDeclaration().isFinal() ? "(final)" : "") + info.toString().replace(" ", ":") + Util.annotationsToString(info.getAnnotations());
        }

    }
}
