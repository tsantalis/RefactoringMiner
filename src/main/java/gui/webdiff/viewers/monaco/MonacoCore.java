package gui.webdiff.viewers.monaco;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.ParameterizeTestRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.rendersnake.HtmlCanvas;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-05*/
public class MonacoCore {
    private boolean showFilenames;
    private final Diff diff;
    private final int id;
    private final boolean isMoved;
    private String srcFileName;
    private String dstFileName;

    private final String srcFileContent;
    private final String dstFileContent;
    private final List<Refactoring> refactorings;

    public MonacoCore(Diff diff, int id, boolean isMovedDiff, String srcFileContent, String dstFileContent, List<Refactoring> refactorings) {
        this.srcFileContent = srcFileContent;
        this.dstFileContent = dstFileContent;
        this.showFilenames = true;
        this.diff = diff;
        this.id = id;
        this.isMoved = isMovedDiff;
        this.refactorings = refactorings;
        if (diff instanceof ASTDiff){
            this.srcFileName = ((ASTDiff) diff).getSrcPath();
            this.dstFileName = ((ASTDiff) diff).getDstPath();
        }
    }

    public void setShowFilenames(boolean showFilenames) {
        this.showFilenames = showFilenames;
    }

    public void addDiffContainers(HtmlCanvas html) throws IOException {
        int offset = (showFilenames) ? 80 : 0;
        html.div(class_("row h-100"));

        html.div(class_("col-6 h-100"));
        if (showFilenames) {
            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
                    .content(srcFileName);
        }
        String heightFormula = "";
        if (showFilenames)
            heightFormula = "height: calc(100% - " + offset + "px);";
        html.div(class_("edc").id(getLeftContainerId()).style(heightFormula + "border: 1px solid grey; overflow: auto;"))._div()
                ._div();

        html.div(class_("col-6"));
        if (showFilenames) {
            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
                    .content(dstFileName);
        }

        html.div(class_("edc").id(getRightContainerId()).style(heightFormula + "border: 1px solid grey; overflow: auto;"))._div()
                ._div();
        String code = "mymonaco(" + makeDiffConfig() + ");";
        html.macros().script(code); // Pass the config to the main function


//
//        html.div(class_("row h-100"))
//                .div(class_("col-6 h-100"));
//        int offset = (showFilenames) ? 80 : 0;
//        if (showFilenames) {
//            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
//                    .content(srcFileName);
//        }
//
//        String heightFormula = "height: calc(100% - " + offset + "px);";
//        html.div(id(getLeftContainerId()).style(heightFormula + "border:1px solid grey;"))._div()
//                ._div()
//                .div(class_("col-6 h-100"));
//
//        if (showFilenames) {
//            html.h6(style("word-break: break-all; white-space: normal; overflow-wrap: break-word;"))
//                    .content(dstFileName);
//        }
//
//
//        html.div(id(getRightContainerId()).style(heightFormula + "border:1px solid grey;"))._div()
//                ._div();
//        String code = "monaco(" + makeDiffConfig() + ");";
//        html.macros().script(code); // Pass the config to the main function
    }

    String getLeftJsConfig() {
        ObjectMapper mapper = new ObjectMapper();
        if (diff instanceof ASTDiff) {
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/left/" + id + "\"").append(",");
            String escapedContent = "";
            try {
                escapedContent = mapper.writeValueAsString(srcFileContent);
            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
            }
            b.append("content:").append(escapedContent).append(",");
            b.append("ranges: [");
            for (Tree t : diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t))
                    appendRange(b, t, "moved", null);
                if (c.getUpdatedSrcs().contains(t))
                    appendRange(b, t, "updated", null);
                if (c.getDeletedSrcs().contains(t))
                    appendRange(b, t, "deleted", null);
                if (c.getSrcMoveOutTreeMap().containsKey(t))
                    appendRange(b, t, "moveOut", c.getSrcMoveOutTreeMap().get(t).toString());
                if (c.getMultiMapSrc().containsKey(t)) {
                    String tag = "mm";
                    boolean _isUpdated = ((MultiMove) (c.getMultiMapSrc().get(t))).isUpdated();
                    if (_isUpdated) {
                        tag += " updOnTop";
                    }
                    appendRange(b, t, tag, null);
                }
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
        else {
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/left/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t: diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t))
                    appendRange(b, t, "moved","");
                if (c.getUpdatedSrcs().contains(t))
                    appendRange(b, t, "updated","");
                if (c.getDeletedSrcs().contains(t))
                    appendRange(b, t, "deleted","");
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
    }

    String getRightJsConfig() {
        ObjectMapper mapper = new ObjectMapper();
        if (diff instanceof ASTDiff) {
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/right/" + id + "\"").append(",");
            String escapedContent = "";
            try {
                escapedContent = mapper.writeValueAsString(dstFileContent);
            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
            }
            b.append("content:").append(escapedContent).append(",");
            b.append("ranges: [");
            for (Tree t : diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t))
                    appendRange(b, t, "moved", null);
                if (c.getUpdatedDsts().contains(t))
                    appendRange(b, t, "updated", null);
                if (c.getInsertedDsts().contains(t))
                    appendRange(b, t, "inserted", null);
                if (c.getDstMoveInTreeMap().containsKey(t))
                    appendRange(b, t, "moveIn", c.getDstMoveInTreeMap().get(t).toString());
                if (c.getMultiMapDst().containsKey(t)) {
                    String tag = "mm";
                    boolean _isUpdated = ((MultiMove) (c.getMultiMapDst().get(t))).isUpdated();
                    if (_isUpdated) {
                        tag += " updOnTop";
                    }
                    appendRange(b, t, tag, null);
                }
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
        else{
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append("url:").append("\"/right/" + id + "\"").append(",");
            b.append("ranges: [");
            for (Tree t: diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t))
                    appendRange(b, t, "moved","");
                if (c.getUpdatedDsts().contains(t))
                    appendRange(b, t, "updated","");
                if (c.getInsertedDsts().contains(t))
                    appendRange(b, t, "inserted","");
            }
            b.append("]").append(",");
            b.append("}");
            return b.toString();
        }
    }

    String getMappingsJsConfig() {
        if (diff instanceof ASTDiff) {
            ASTDiff astDiff = (ASTDiff) diff;
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("[");
            for (Tree t : diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                    Tree d = ((ASTDiff)diff).getAllMappings().getDsts(t).iterator().next();
                    b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
                }
                else {
                    Set<Tree> dsts = astDiff.getAllMappings().getDsts(t);
                    if (dsts == null) continue;
                    for (Tree dst : dsts) {
                        if (dst == null) continue;
                        b.append(String.format("[%s, %s, %s, %s], ",
                                t.getPos(),
                                t.getEndPos(),
                                dst.getPos(),
                                dst.getEndPos()));
                    }
                }
            }
            b.append("]").append(",");
            return b.toString();
        }
        else {
            TreeClassifier c = diff.createRootNodesClassifier();
            StringBuilder b = new StringBuilder();
            b.append("[");
            for (Tree t: diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {

                    Tree d = diff.mappings.getDstForSrc(t);
                    b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
                }
            }
            b.append("]").append(",");
            return b.toString();
        }
    }

    private static boolean isStatement(Tree t) {
    	String type = t.getType().toString();
		return type.endsWith("Statement") || type.equals("Block") || type.endsWith("ConstructorInvocation") || type.equals("SwitchCase");
    }

    private static boolean isExpression(Tree t) {
    	String type = t.getType().toString();
    	return type.endsWith("Expression") || type.endsWith("Literal") || type.endsWith("Reference") || type.endsWith("Invocation") ||
    			type.endsWith("Creation") || type.endsWith("Access") || type.endsWith("Name") || type.endsWith("Annotation") || type.endsWith("Pattern") ||
    			type.equals("Assignment") || type.equals("ArrayInitializer");
    }

    //private Map<Tree, Set<String>> appliedTooltips = new HashMap<>();

    private void appendRange(StringBuilder b, Tree t, String kind, String tip) {
        Set<String> tooltips = tooltip(t);
        if((isStatement(t) || t.getType().toString().endsWith("Declaration") || isExpression(t) ||
        		t.getType().toString().startsWith("LineComment") || t.getType().toString().startsWith("BlockComment")) &&
        		(kind.equals("moved") || kind.startsWith("mm") || kind.equals("moveOut") || kind.equals("moveIn")) &&
        		!tooltips.isEmpty()) {
        	for(String tooltip : tooltips) {
        		//TODO the problem with duplicated tooltips seems to be related with cascading tooltips from parent nodes
        		//when an AST in nested under a parent with tooltips, it inherits all tooltips from its parent
        		//the solution below does not fix the problem
        		/*
        		boolean tipExists = false;
        		if(appliedTooltips.containsKey(t)) {
        			Set<String> tips = appliedTooltips.get(t);
        			if(tips.contains(tooltip)) {
        				tipExists = true;
        			}
        			else {
        				tips.add(tooltip);
        			}
        		}
        		else {
        			Set<String> tips = new HashSet<>();
        			tips.add(tooltip);
        			appliedTooltips.put(t, tooltips);
        		}
        		*/
    			b.append("{")
            	.append("from: ").append(t.getPos())
            	.append(",").append("to: ").append(t.getEndPos()).append(",")
            	.append("index: ").append(t.getMetrics().depth).append(",")
            	.append("kind: ").append("\"" + kind + "\"").append(",")
            	.append("tooltip: ").append("\"" + tooltip + "\"").append(",")
            	.append("}").append(",");
        	}
        }
        else {
        	b.append("{")
        	.append("from: ").append(t.getPos())
        	.append(",").append("to: ").append(t.getEndPos()).append(",")
        	.append("index: ").append(t.getMetrics().depth).append(",")
        	.append("kind: ").append("\"" + kind + "\"").append(",")
        	.append("tooltip: ").append("\"" + "\"").append(",")
        	.append("}").append(",");
        }
    }

    private Set<String> tooltip(Tree t) {
    	ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
    	Set<String> tooltips = new LinkedHashSet<>();
    	for(Refactoring r : refactorings) {
    		Set<String> dstPath = r.getInvolvedClassesAfterRefactoring().stream().map(o -> o.left).collect(Collectors.toSet());
    		Set<String> srcPath = r.getInvolvedClassesBeforeRefactoring().stream().map(o -> o.left).collect(Collectors.toSet());
    		if(!srcPath.contains(srcFileName) && !dstPath.contains(dstFileName)) {
    			continue;
    		}
    		if(r instanceof InlineOperationRefactoring) {
    			InlineOperationRefactoring inline = (InlineOperationRefactoring)r;
	    		UMLOperationBodyMapper bodyMapper = inline.getBodyMapper();
				String tooltipLeft = "inlined to " + inline.getTargetOperationAfterInline();
				String tooltipRight = "inlined from " + inline.getInlinedOperation();
				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    		else if(r instanceof ExtractOperationRefactoring) {
    			ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
    			UMLOperationBodyMapper bodyMapper = extract.getBodyMapper();
				String tooltipLeft = "extracted to " + extract.getExtractedOperation();
				String tooltipRight = "extracted from " + extract.getSourceOperationBeforeExtraction();
				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    		else if(r instanceof ParameterizeTestRefactoring) {
    			ParameterizeTestRefactoring extract = (ParameterizeTestRefactoring)r;
    			UMLOperationBodyMapper bodyMapper = extract.getBodyMapper();
				String tooltipLeft = "parameterized to " + extract.getParameterizedTestOperation();
				String tooltipRight = "parameterized from " + extract.getRemovedOperation();
				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    		else if(r instanceof MergeOperationRefactoring) {
    			MergeOperationRefactoring merge = (MergeOperationRefactoring)r;
    			if(t.getType().toString().endsWith("Statement") || t.getType().toString().startsWith("LineComment") || t.getType().toString().startsWith("BlockComment")) {
    				for(UMLOperationBodyMapper bodyMapper : merge.getMappers()) {
    					String tooltipLeft = "merged to " + bodyMapper.getContainer2();
        				String tooltipRight = "merged from " + bodyMapper.getContainer1();
        				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
        				if(tooltip != null)
        					tooltips.add(tooltip);
    				}
    			}
    		}
    		else if(r instanceof SplitOperationRefactoring) {
    			SplitOperationRefactoring split = (SplitOperationRefactoring)r;
    			if(t.getType().toString().endsWith("Statement") || t.getType().toString().startsWith("LineComment") || t.getType().toString().startsWith("BlockComment")) {
    				for(UMLOperationBodyMapper bodyMapper : split.getMappers()) {
    					String tooltipLeft = "split to " + bodyMapper.getContainer2();
        				String tooltipRight = "split from " + bodyMapper.getContainer1();
        				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
        				if(tooltip != null)
        					tooltips.add(tooltip);
    				}
    			}
    		}
    		else if(r instanceof MoveCodeRefactoring) {
    			MoveCodeRefactoring moveCode = (MoveCodeRefactoring)r;
    			UMLOperationBodyMapper bodyMapper = moveCode.getBodyMapper();
				String tooltipLeft = "moved to " + moveCode.getTargetContainer();
				String tooltipRight = "moved from " + moveCode.getSourceContainer();
				String tooltip = generateTooltip(t, c, bodyMapper, tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    		else if(r instanceof MoveOperationRefactoring || r instanceof MoveAttributeRefactoring) {
    			if(t.getType().toString().endsWith("Declaration")) {
    				String tooltipLeft = "moved to " + r.getInvolvedClassesAfterRefactoring().iterator().next().left;
    				String tooltipRight = "moved from " + r.getInvolvedClassesBeforeRefactoring().iterator().next().left;
    				String tooltip = generateTooltip(t, c, (UMLOperationBodyMapper)null, tooltipLeft, tooltipRight);
					if(tooltip != null)
						tooltips.add(tooltip);
    			}
    		}
    		else if(r instanceof ExtractVariableRefactoring) {
    			ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
    			String tooltipLeft = "extracted to variable " + extract.getVariableDeclaration().getVariableName();
    			String tooltipRight = "extracted to variable " + extract.getVariableDeclaration().getVariableName();
    			String tooltip = generateTooltip(t, c, extract.getSubExpressionMappings(), tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    		else if(r instanceof InlineVariableRefactoring) {
    			InlineVariableRefactoring inline = (InlineVariableRefactoring)r;
    			String tooltipLeft = "inlined from variable " + inline.getVariableDeclaration().getVariableName();
    			String tooltipRight = "inlined from variable " + inline.getVariableDeclaration().getVariableName();
    			String tooltip = generateTooltip(t, c, inline.getSubExpressionMappings(), tooltipLeft, tooltipRight);
				if(tooltip != null)
					tooltips.add(tooltip);
    		}
    	}
    	return tooltips;
        //return (t.getParent() != null)
        //        ? t.getParent().getType() + "/" + t.getType() + "/" + t.getPos() + "/" +  t.getEndPos() : t.getType().toString() + t.getPos() + t.getEndPos();
    }

    private String generateTooltip(Tree tree, ExtendedTreeClassifier classifier, List<LeafMapping> references,
			String tooltipLeft, String tooltipRight) {
    	for(LeafMapping mapping : references) {
			if(subsumes(mapping.getFragment1().codeRange(),tree) && (classifier.getMovedSrcs().contains(tree) || classifier.getMultiMapSrc().containsKey(tree))) {
				return tooltipLeft;
			}
			else if(subsumes(mapping.getFragment1().codeRange(),tree) && classifier.getSrcMoveOutTreeMap().get(tree) != null) {
				return tooltipLeft + " & " + classifier.getSrcMoveOutTreeMap().get(tree);
			}
			if(subsumes(mapping.getFragment2().codeRange(),tree) && (classifier.getMovedDsts().contains(tree) || classifier.getMultiMapDst().containsKey(tree))) {
				return tooltipRight;
			}
			else if(subsumes(mapping.getFragment2().codeRange(),tree) && classifier.getDstMoveInTreeMap().get(tree) != null) {
				return tooltipRight + " & " + classifier.getDstMoveInTreeMap().get(tree);
			}
		}
    	return null;
    }

	private String generateTooltip(Tree tree, ExtendedTreeClassifier classifier, UMLOperationBodyMapper bodyMapper,
			String tooltipLeft, String tooltipRight) {
		if(tree.getType().toString().endsWith("Declaration")) {
			if(classifier.getSrcMoveOutTreeMap().get(tree) != null) {
				return classifier.getSrcMoveOutTreeMap().get(tree).toString();
			}
			else if(classifier.getMultiMapSrc().containsKey(tree)) {
				return tooltipLeft;
			}
			if(classifier.getDstMoveInTreeMap().get(tree) != null) {
				return classifier.getDstMoveInTreeMap().get(tree).toString();
			}
			else if(classifier.getMultiMapDst().containsKey(tree)) {
				return tooltipRight;
			}
		}
		if(bodyMapper != null) {
			for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
				if(subsumes(mapping.getFragment1().codeRange(),tree) && (classifier.getMovedSrcs().contains(tree) || classifier.getMultiMapSrc().containsKey(tree))) {
					return tooltipLeft;
				}
				else if(subsumes(mapping.getFragment1().codeRange(),tree) && classifier.getSrcMoveOutTreeMap().get(tree) != null) {
					return tooltipLeft + " & " + classifier.getSrcMoveOutTreeMap().get(tree);
				}
				if(subsumes(mapping.getFragment2().codeRange(),tree) && (classifier.getMovedDsts().contains(tree) || classifier.getMultiMapDst().containsKey(tree))) {
					return tooltipRight;
				}
				else if(subsumes(mapping.getFragment2().codeRange(),tree) && classifier.getDstMoveInTreeMap().get(tree) != null) {
					return tooltipRight + " & " + classifier.getDstMoveInTreeMap().get(tree);
				}
			}
			if(bodyMapper.getCommentListDiff() != null && (tree.getType().toString().startsWith("LineComment") || tree.getType().toString().startsWith("BlockComment"))) {
				for(Pair<UMLComment, UMLComment> pair : bodyMapper.getCommentListDiff().getCommonComments()) {
					if(subsumes(pair.getLeft().codeRange(),tree) && (classifier.getMovedSrcs().contains(tree) || classifier.getMultiMapSrc().containsKey(tree))) {
						return tooltipLeft;
					}
					if(subsumes(pair.getRight().codeRange(),tree) && (classifier.getMovedDsts().contains(tree) || classifier.getMultiMapDst().containsKey(tree))) {
						return tooltipRight;
					}
				}
			}
		}
		return null;
	}

    private static boolean subsumes(CodeRange range, Tree t) {
    	if(range.getStartOffset() <= t.getPos() &&
				range.getEndOffset() >= t.getEndPos()) {
    		return true;
    	}
    	return false;
    }

    public String getDiffName() {
        return srcFileName + " -> " + dstFileName;
    }

    public String makeDiffConfig() {
        boolean spv = !showFilenames;
        return "{ "
                + "moved: " + this.isMoved
                + ", id: " + id
                + ", spv: " + spv
                + ", file: \"" + srcFileName + "\""
                + ", left: " + this.getLeftJsConfig()
                + ", lcid: \"" + this.getLeftContainerId() + "\""
                + ", right: " + this.getRightJsConfig()
                + ", rcid: \"" + this.getRightContainerId() + "\""
                + ", mappings: " + this.getMappingsJsConfig() + "}";
    }

    private String getLeftContainerId() {
        return "left-container-" + id;
    }
    private String getRightContainerId() {
        return "right-container-" + id;
    }
}
