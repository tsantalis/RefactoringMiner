package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleItem;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstStmt;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstModule;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstScript;
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
			System.out.println(output.getProgram().toDebugString());
			if (astDiff) {
				ByteArrayInputStream is = new ByteArrayInputStream(fileContent.getBytes());
				TreeContext treeContext = new TypeScriptTreeSitterNgTreeGenerator().generateFrom().stream(is);
				this.umlModel.getTreeContextMap().put(filePath, treeContext);
			}
			if(output.getProgram() instanceof Swc4jAstModule module) {
				List<ISwc4jAstModuleItem> list = module.getBody();
				String sourceFolder = UMLAdapterUtil.extractSourceFolder(filePath);
				String moduleName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length() - 3) : "";
				LocationInfo location = new LocationInfo(sourceFolder, filePath, module.getSpan(), CodeElementType.MODULE_DECLARATION);
				ModuleContainer moduleContainer = new ModuleContainer(location, moduleName);
				OperationBody opBody = new OperationBody(
						module,
						sourceFolder,
						filePath,
						list,
						moduleContainer,
						fileContent
						);
				moduleContainer.addStatements(opBody.getCompositeStatement().getStatements());
			}
			else if(output.getProgram() instanceof Swc4jAstScript script) {
				List<ISwc4jAstStmt> list = script.getBody();
			}
		}
		catch(IOException | Swc4jCoreException e) {
			
		}
	}
}
