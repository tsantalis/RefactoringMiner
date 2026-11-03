package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Block extends BaseCodeElement {
    private final AbstractStatement composite;
    private final VariableDeclarationContainer operation;

    private Block(AbstractStatement composite, VariableDeclarationContainer operation, String identifierIgnoringVersion, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.composite = composite;
        this.operation = operation;
    }

    public BaseCodeElement of(Version version) {
    	return of(this.composite, this.operation, version);
    }

    public AbstractStatement getComposite() {
        return composite;
    }

    public VariableDeclarationContainer getOperation() {
        return operation;
    }

    public static Block of(CompositeStatementObject composite, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(composite, method);
    }

    public static Block of(CompositeStatementObject composite, Method method) {
        LocationInfo compositeLocationInfo = composite.getLocationInfo();
        String statementType = compositeLocationInfo.getCodeElementType().getName() != null ? compositeLocationInfo.getCodeElementType().getName() : composite.toString();
        String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(composite.getAllStatements().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                composite.getSignature()
        );
        return new Block(composite, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
    }

    public static Block of(CompositeStatementObject composite, Attribute attribute) {
        LocationInfo compositeLocationInfo = composite.getLocationInfo();
        String statementType = compositeLocationInfo.getCodeElementType().getName() != null ? compositeLocationInfo.getCodeElementType().getName() : composite.toString();
        String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(composite.getAllStatements().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                attribute.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                composite.getSignature()
        );
        return new Block(composite, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
    }

    public static Block of(StatementObject statement, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(statement, method);
    }

    public static Block of(StatementObject statement, Method method) {
        LocationInfo compositeLocationInfo = statement.getLocationInfo();
        List<AbstractCall> streamAPICalls = streamAPICalls(statement);
        if(streamAPICalls.size() > 0) {
            String statementType = streamAPICalls.get(0).getName();
            String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
            String sha512 = Util.getSHA512(statement.toString());
            String identifierExcludeVersion = String.format(
                    "%s$%s:{%s,%s}",
                    method.getIdentifierIgnoringVersion(),
                    statementType,
                    sha512,
                    signature(statement, statementType)
            );
            return new Block(statement, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
        }
    	String statementType = statement.getLocationInfo().getCodeElementType().name();
        String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(statement.toString());
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                signature(statement, statementType)
        );
        return new Block(statement, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
    }

    public static Block of(StatementObject statement, Attribute attribute) {
        LocationInfo compositeLocationInfo = statement.getLocationInfo();
        List<AbstractCall> streamAPICalls = streamAPICalls(statement);
        if(streamAPICalls.size() > 0) {
            String statementType = streamAPICalls.get(0).getName();
            String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
            String sha512 = Util.getSHA512(statement.toString());
            String identifierExcludeVersion = String.format(
                    "%s$%s:{%s,%s}",
                    attribute.getIdentifierIgnoringVersion(),
                    statementType,
                    sha512,
                    signature(statement, statementType)
            );
            return new Block(statement, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
        }
    	String statementType = statement.getLocationInfo().getCodeElementType().name();
        String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(statement.toString());
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                attribute.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                signature(statement, statementType)
        );
        return new Block(statement, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
    }

    private static String signature(StatementObject statement, String statementType) {
        CompositeStatementObject parent = statement.getParent();
        if (parent == null) {
            return statementType;
        }
        List<AbstractStatement> sameTypeSibling = parent.getStatements().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().getName())).collect(Collectors.toList());
        int typeIndex = 1;
        for (AbstractStatement abstractStatement : sameTypeSibling) {
            if (abstractStatement.getIndex() == statement.getIndex()) {
                break;
            }
            typeIndex++;
        }
        return String.format("%s:%s%d", parent.getSignature(), statementType, typeIndex);
    }

    private static List<AbstractCall> streamAPICalls(AbstractCodeFragment statement) {
        List<AbstractCall> streamAPICalls = new ArrayList<>();
        AbstractCall invocation = statement.invocationCoveringEntireFragment();
        if(invocation == null) {
            invocation = statement.assignmentInvocationCoveringEntireStatement();
        }
        if(invocation != null && (invocation.actualString().contains(" -> ") ||
                invocation.actualString().contains("::"))) {
            for(AbstractCall inv : statement.getMethodInvocations()) {
                if(streamAPIName(inv.getName())) {
                    streamAPICalls.add(inv);
                }
            }
        }
        return streamAPICalls;
    }

    private static boolean streamAPIName(String name) {
        return name.equals("stream") || name.equals("filter") || name.equals("forEach") || name.equals("collect") || name.equals("map") || name.equals("removeIf");
    }

    public static Block of(AbstractStatement statement, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        if (statement instanceof StatementObject)
            return of((StatementObject) statement, method);
        else
            return of((CompositeStatementObject) statement, method);
    }

    public static Block of(AbstractStatement statement, Method method) {
        if (statement instanceof StatementObject)
            return of((StatementObject) statement, method);
        else
            return of((CompositeStatementObject) statement, method);
    }

    public boolean differInFormatting(Block other) {
    	String thisSignature = composite.getActualSignature();
		String otherSignature = other.composite.getActualSignature();
		if (thisSignature != null && otherSignature != null) {
			String[] leftLineArray = thisSignature.split("\\r?\\n");
			String[] rightLineArray = otherSignature.split("\\r?\\n");
			if (leftLineArray.length == rightLineArray.length) {
				//check if lines start and end with the same character
				int linesWithDifferences = 0;
				for (int i=0; i<leftLineArray.length; i++) {
					String leftLine = leftLineArray[i];
					String rightLine = rightLineArray[i];
					if(leftLine.length() > 1 && rightLine.length() > 1 &&
							(leftLine.charAt(0) != rightLine.charAt(0) || leftLine.charAt(leftLine.length()-1) != rightLine.charAt(rightLine.length()-1))) {
						linesWithDifferences++;
					}
				}
				if(linesWithDifferences > 0) {
					return !thisSignature.equals(otherSignature) && thisSignature.replaceAll("\\s+","").equals(otherSignature.replaceAll("\\s+",""));
				}
			}
    		return !thisSignature.equals(otherSignature) && leftLineArray.length != rightLineArray.length && thisSignature.replaceAll("\\s+","").equals(otherSignature.replaceAll("\\s+",""));
    	}
    	return false;
    }

	public int signatureStartLine() {
		return composite.getLocationInfo().getStartLine();
	}

    public boolean isMultiLine() {
    	if (composite.getActualSignature() != null) {
    		return composite.getActualSignature().contains("\n");
    	}
    	return false;
    }

    public void checkClosingBracketOfAnonymousClassDeclaration(int lineNumber) {
    	if (getComposite() instanceof StatementObject && getComposite().getAnonymousClassDeclarations().size() > 0 && getLocation().getEndLine() == lineNumber) {
    		setAnonymousClosingBracket(true);
    	}
    }

    public void checkClosingBracketOfLambda(int lineNumber) {
    	if (getComposite() instanceof StatementObject && getComposite().getLambdas().size() > 0) {
    		for (LambdaExpressionObject lambda : getComposite().getLambdas()) {
    			if (lambda.getBody() != null && getLocation().getEndLine() == lineNumber) {
    				setLambdaClosingBracket(true);
    				break;
    			}
    		}
    	}
    }

    public void checkDoWhileConditional(int lineNumber) {
    	if (getComposite() instanceof CompositeStatementObject && getLocation().getCodeElementType().equals(CodeElementType.DO_STATEMENT) && getLocation().getEndLine() == lineNumber) {
    		setDoWhileConditional(true);
    	}
    }

	public void checkClosingBracket(int lineNumber) {
		if (getComposite() instanceof TryStatementObject) {
			TryStatementObject tryStatement = (TryStatementObject)getComposite();
			if (tryStatement.getCatchClauses().size() > 0) {
				CompositeStatementObject catchClause = tryStatement.getCatchClauses().get(0);
				if (catchClause.getLocationInfo().getStartLine() == lineNumber || catchClause.getLocationInfo().getStartLine() == lineNumber + 1) {
					setClosingCurlyBracket(true);
				}
			}
			else if (tryStatement.getFinallyClause() != null) {
				CompositeStatementObject finnalyClause = tryStatement.getFinallyClause();
				if (finnalyClause.getLocationInfo().getStartLine() == lineNumber || finnalyClause.getLocationInfo().getStartLine() == lineNumber + 1) {
					setClosingCurlyBracket(true);
				}
			}
		}
		if (getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)getComposite();
			if (ifComp.getStatements().size() == 2) {
				// if statement has an else branch, line number is the closing bracket before else branch
				AbstractStatement ifBranch = ifComp.getStatements().get(0);
				AbstractStatement elseBranch = ifComp.getStatements().get(1);
				if(ifBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						ifBranch.getLocationInfo().getEndLine() == lineNumber &&
						elseBranch.getLocationInfo().getStartLine() == lineNumber + 1) {
					setClosingCurlyBracket(true);
				}
			}
		}
		if (getLocation().getEndLine() == lineNumber && getComposite() instanceof CompositeStatementObject && !getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
			setClosingCurlyBracket(true);
		}
	}

	public void checkElseBlockStart(int lineNumber) {
		// in case of if-else-if chain, this method should return true only for the first if in the chain
		// we assume the first if in the chain, introduced the else
		if (getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)getComposite();
			CompositeStatementObject parent = ifComp.getParent();
			if (parent != null && !parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				// find the last else-if in the chain
				while (ifComp.getStatements().size() == 2 && ifComp.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					ifComp = (CompositeStatementObject) ifComp.getStatements().get(1);
				}
				if (ifComp.getStatements().size() == 2) {
					// lineNumber is else branch
					AbstractStatement ifBranch = ifComp.getStatements().get(0);
					AbstractStatement elseBranch = ifComp.getStatements().get(1);
					if (ifBranch.getLocationInfo().getEndLine() == lineNumber &&
							elseBranch.getLocationInfo().getStartLine() == lineNumber &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						setElseBlockStart(true);
					}
					if (ifBranch.getLocationInfo().getEndLine() == lineNumber - 1 &&
							(elseBranch.getLocationInfo().getStartLine() == lineNumber || elseBranch.getLocationInfo().getStartLine() == lineNumber + 1) &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						setElseBlockStart(true);
					}
				}
			}
		}
	}

	public void checkElseBlockEnd(int lineNumber) {
		// in case of if-else-if chain, this method should return true only for the first if in the chain
		// we assume the first if in the chain, introduced the else
		if (getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)getComposite();
			CompositeStatementObject parent = ifComp.getParent();
			if (parent != null && !parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				// find the last else-if in the chain
				while (ifComp.getStatements().size() == 2 && ifComp.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					ifComp = (CompositeStatementObject) ifComp.getStatements().get(1);
				}
				if (ifComp.getStatements().size() == 2) {
					// lineNumber is else branch
					AbstractStatement elseBranch = ifComp.getStatements().get(1);
					if (elseBranch.getLocationInfo().getEndLine() == lineNumber &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						setElseBlockEnd(true);
					}
				}
			}
		}
	}

    @Override
    public LocationInfo getLocation() {
        return composite.getLocationInfo();
    }
}
