package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.gen.treesitterng.PythonTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import extension.ast.node.LangASTNode;
import extension.base.LangASTUtil;
import extension.base.LangSupportedEnum;
import extension.umladapter.UMLAdapterUtil;
import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class PythonFileProcessor {
	private UMLModel umlModel;

	public PythonFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processPythonFile(String filePath, String fileContent, boolean astDiff) {
		try {
			LangSupportedEnum language = LangSupportedEnum.fromFileName(filePath);
			LangASTNode ast = LangASTUtil.getLangAST(language, fileContent);
			if (astDiff) {
				ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
				TreeContext treeContext = new PythonTreeSitterNgTreeGenerator().generateFrom().stream(is);
				List<Tree> trees = TreeUtilFunctions.findChildrenByTypeRecursively(treeContext.getRoot(), "comment");
				List<UMLComment> comments = new ArrayList<UMLComment>();
				for(Tree t : trees) {
					String sourceFolder = UMLAdapterUtil.extractSourceFolder(filePath);
					LocationInfo location = new LocationInfo(sourceFolder, filePath, t, CodeElementType.LINE_COMMENT, fileContent);
					UMLComment comment = new UMLComment(t.getLabel(), location);
					comments.add(comment);
				}
				this.umlModel.getCommentMap().put(filePath, comments);
				this.umlModel.getTreeContextMap().put(filePath, treeContext);
			}
			UMLModelAdapter.extractUMLEntities(ast, umlModel, filePath, fileContent);
		}
		catch(IOException ioe) {
			
		}
	}
}
