package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleItem;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstStmt;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstModule;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstScript;
import com.caoccao.javet.swc4j.comments.Swc4jComment;
import com.caoccao.javet.swc4j.comments.Swc4jCommentKind;
import com.caoccao.javet.swc4j.comments.Swc4jComments;
import com.caoccao.javet.swc4j.enums.Swc4jMediaType;
import com.caoccao.javet.swc4j.enums.Swc4jParseMode;
import com.caoccao.javet.swc4j.exceptions.Swc4jCoreException;
import com.caoccao.javet.swc4j.options.Swc4jParseOptions;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import com.github.gumtreediff.gen.treesitterng.TypeScriptTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;

import extension.umladapter.UMLAdapterUtil;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.OperationBody;

public class TypeScriptFileProcessor {
	private UMLModel umlModel;

	public TypeScriptFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	public void processTypeScriptFile(String filePath, String fileContent, boolean astDiff, Swc4j swc4j) {
		try {
			URL specifier = Path.of(filePath).toUri().toURL();
			Swc4jParseOptions options = new Swc4jParseOptions()
					.setSpecifier(specifier)
					.setMediaType(Swc4jMediaType.TypeScript)
					.setCaptureAst(true)
					.setCaptureComments(true)
					.setParseMode(Swc4jParseMode.Module);
			// Parse the code.
			Swc4jParseOutput output = swc4j.parse(fileContent, options);
			Swc4jComments comments = output.getComments();
			//System.out.println(output.getProgram().toDebugString());
			if (astDiff) {
				ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
				TreeContext treeContext = new TypeScriptTreeSitterNgTreeGenerator().generateFrom().stream(is);
				this.umlModel.getTreeContextMap().put(filePath, treeContext);
			}
			if(output.getProgram() instanceof Swc4jAstModule module) {
				List<ISwc4jAstModuleItem> list = module.getBody();
				List<UMLImport> imports = new ArrayList<>();
				String sourceFolder = UMLAdapterUtil.extractSourceFolder(filePath);
				String moduleName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length() - 3) : filePath.substring(0, filePath.length() - 3);
				LocationInfo location = new LocationInfo(sourceFolder, filePath, module.getSpan(), CodeElementType.TYPE_DECLARATION);
				List<UMLComment> commentList = extractComments(comments, sourceFolder, filePath, fileContent);
				UMLClass moduleClass = new UMLClass(moduleName, "__module__", location, true, imports);
				moduleClass.setModule(true);
				ModuleContainer moduleContainer = new ModuleContainer(location, moduleName);
				moduleContainer.addComments(commentList);
				OperationBody opBody = new OperationBody(
						module,
						sourceFolder,
						filePath,
						list,
						moduleContainer,
						fileContent
						);
				moduleContainer.addStatements(opBody.getCompositeStatement().getStatements());
				moduleClass.setContainer(moduleContainer);
				moduleClass.setVisibility(Visibility.PUBLIC);
				moduleClass.getComments().addAll(commentList);
				umlModel.addClass(moduleClass);
			}
			else if(output.getProgram() instanceof Swc4jAstScript script) {
				List<ISwc4jAstStmt> list = script.getBody();
			}
		}
		catch(IOException | Swc4jCoreException e) {
			
		}
	}

	private List<UMLComment> extractComments(Swc4jComments comments, String sourceFolder, String filePath, String fileContent) {
		List<UMLComment> commentList = new ArrayList<>();
		Map<Integer, List<Swc4jComment>> leading = comments.getLeading();
		for(Integer key : leading.keySet()) {
			List<Swc4jComment> list = leading.get(key);
			for(Swc4jComment comment : list) {
				LocationInfo locationInfo = null;
				if(comment.getKind().equals(Swc4jCommentKind.Line)) {
					locationInfo = new LocationInfo(sourceFolder, filePath, comment.getSpan(), CodeElementType.LINE_COMMENT);
				}
				else if(comment.getKind().equals(Swc4jCommentKind.Block)) {
					locationInfo = new LocationInfo(sourceFolder, filePath, comment.getSpan(), CodeElementType.BLOCK_COMMENT);
				}
				if(locationInfo != null) {
					int start = locationInfo.getStartOffset();
					int end = locationInfo.getEndOffset();
					String text = fileContent.substring(start, end);
					UMLComment umlComment = new UMLComment(text, locationInfo);
					commentList.add(umlComment);
				}
			}
		}
		return commentList;
	}
}
