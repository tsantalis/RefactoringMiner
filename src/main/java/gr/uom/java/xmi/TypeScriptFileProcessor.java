package gr.uom.java.xmi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstFunction;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstParam;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleItem;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstPat;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstStmt;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstModule;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstScript;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstBlockStmt;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeAnn;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeParam;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeParamDecl;
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
import gr.uom.java.xmi.decomposition.VariableDeclaration;

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
					.setMediaType(filePath.endsWith(".tsx") ? Swc4jMediaType.Tsx : Swc4jMediaType.TypeScript)
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
				int extensionLength = filePath.endsWith(".tsx") ? 4 : 3;
				String moduleName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length() - extensionLength) : filePath.substring(0, filePath.length() - extensionLength);
				LocationInfo location = new LocationInfo(sourceFolder, filePath, module.getSpan(), CodeElementType.TYPE_DECLARATION, fileContent);
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
				moduleClass.operations.addAll(moduleContainer.getNestedOperations());
				moduleClass.attributes.addAll(moduleContainer.getNestedAttributes());
				umlModel.addClass(moduleClass);
				umlModel.getClassList().addAll(moduleContainer.getNestedClasses());
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
			processCommentList(sourceFolder, filePath, fileContent, commentList, list);
		}
		Map<Integer, List<Swc4jComment>> trailing = comments.getTrailing();
		for(Integer key : trailing.keySet()) {
			List<Swc4jComment> list = trailing.get(key);
			processCommentList(sourceFolder, filePath, fileContent, commentList, list);
		}
		return commentList;
	}

	private void processCommentList(String sourceFolder, String filePath, String fileContent,
			List<UMLComment> commentList, List<Swc4jComment> list) {
		for(Swc4jComment comment : list) {
			LocationInfo locationInfo = null;
			if(comment.getKind().equals(Swc4jCommentKind.Line)) {
				locationInfo = new LocationInfo(sourceFolder, filePath, comment.getSpan(), CodeElementType.LINE_COMMENT, fileContent);
			}
			else if(comment.getKind().equals(Swc4jCommentKind.Block)) {
				locationInfo = new LocationInfo(sourceFolder, filePath, comment.getSpan(), CodeElementType.BLOCK_COMMENT, fileContent);
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

	public static UMLOperation processFunctionDeclaration(String sourceFolder, String filePath, Swc4jAstFnDecl functionDecl, Map<String,Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, functionDecl.getSpan(), CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionDecl.getIdent().getSym(), location, className);
		operation.setVisibility(Visibility.PRIVATE);
		Swc4jAstFunction function = functionDecl.getFunction();
		Optional<Swc4jAstTsTypeAnn> returnType = function.getReturnType();
		if(returnType.isPresent()) {
			UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, returnType.get().getTypeAnn(), 0);
			UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
			operation.addParameter(returnParameter);
		}
		Optional<Swc4jAstTsTypeParamDecl> typeParams = function.getTypeParams();
		if(typeParams.isPresent()) {
			List<Swc4jAstTsTypeParam> list = typeParams.get().getParams();
			for(Swc4jAstTsTypeParam param : list) {
				LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, param.getSpan(), CodeElementType.TYPE_PARAMETER, fileContent);
				UMLTypeParameter umlTypeParameter = new UMLTypeParameter(param.getName().getSym(), locationInfo);
				operation.addTypeParameter(umlTypeParameter);
			}
		}
		for(Swc4jAstParam param : function.getParams()) {
			ISwc4jAstPat pat = param.getPat();
			Swc4jAstTsTypeAnn typeAnnotation = VariableDeclaration.extractTypeAnnotation(pat);
			List<Swc4jAstBindingIdent> identifiers = VariableDeclaration.extractVariables(pat);
			for(Swc4jAstBindingIdent identifier : identifiers) {
				VariableDeclaration parameter = new VariableDeclaration(sourceFolder, filePath, typeAnnotation, identifier, operation, activeVariableDeclarations, fileContent);
				parameter.setParameter(true);
				if(parameter.getType() != null) {
					UMLParameter umlParameter = new UMLParameter(parameter.getVariableName(), parameter.getType(), "in", false);
					umlParameter.setVariableDeclaration(parameter);
					operation.addParameter(umlParameter);
				}
			}
		}
		Optional<Swc4jAstBlockStmt> body = function.getBody();
		if(body.isPresent()) {
			OperationBody operationBody = new OperationBody(sourceFolder, filePath, body.get(), operation, activeVariableDeclarations, fileContent);
			operation.setBody(operationBody);
		}
		return operation;
	}
}
