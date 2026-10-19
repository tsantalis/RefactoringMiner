package gr.uom.java.xmi;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.refactoringminer.astDiff.utils.TypeScriptTreeGenerator;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassMethod;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstConstructor;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstFunction;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstParam;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstPrivateMethod;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleItem;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstParamOrTsParamProp;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstPat;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstStmt;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstTsFnParam;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstArrayPat;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstObjectPat;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstRestPat;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstModule;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstScript;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstBlockStmt;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsMethodSignature;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsParamProp;
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
import com.github.gumtreediff.tree.TreeContext;

import extension.umladapter.UMLAdapterUtil;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.TypeScriptOperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class TypeScriptFileProcessor {
	private UMLModel umlModel;

	public TypeScriptFileProcessor(UMLModel umlModel) {
		this.umlModel = umlModel;
	}

	/**
	 * swc4j gives the offsets in code points, so the source is parsed with each UTF-16 unit of a surrogate pair, i.e., an emoji, replaced by a private use character,
	 * to get UTF-16 offsets, i.e., the offsets of Java strings. The mapping is one-to-one, so different emoji remain different, and the source code text is taken from the original content.
	 */
	public static String utf16Source(String content) {
		char[] chars = null;
		for(int i = 0; i < content.length(); i++) {
			char character = content.charAt(i);
			if(Character.isSurrogate(character)) {
				if(chars == null) {
					chars = content.toCharArray();
				}
				//U+D800-U+DFFF to U+E000-U+E7FF
				chars[i] = (char) (character + 0x800);
			}
		}
		return chars == null ? content : new String(chars);
	}

	//JSX does not conflict with JavaScript, because an expression cannot start with <, while it conflicts with the TypeScript type assertions <T>x
	public static Swc4jMediaType mediaType(String filePath) {
		if(filePath.endsWith(".tsx"))
			return Swc4jMediaType.Tsx;
		if(filePath.endsWith(".ts"))
			return Swc4jMediaType.TypeScript;
		return Swc4jMediaType.Jsx;
	}

	public void processTypeScriptFile(String filePath, String fileContent, boolean astDiff, Swc4j swc4j) {
		//skip minified JavaScript files
		if(filePath.endsWith(".min.js")) {
			return;
		}
		try {
			Swc4jMediaType mediaType = mediaType(filePath);
			URL specifier = Path.of(filePath).toUri().toURL();
			Swc4jParseOptions options = new Swc4jParseOptions()
					.setSpecifier(specifier)
					.setMediaType(mediaType)
					.setCaptureAst(true)
					.setCaptureComments(true)
					.setParseMode(Swc4jParseMode.Module);
			// Parse the code.
			Swc4jParseOutput output = swc4j.parse(utf16Source(fileContent), options);
			Swc4jComments comments = output.getComments();
			//System.out.println(output.getProgram().toDebugString());
			if (astDiff) {
				TreeContext treeContext = TypeScriptTreeGenerator.generate(filePath, fileContent, output.getProgram(), comments);
				this.umlModel.getTreeContextMap().put(filePath, treeContext);
			}
			if(output.getProgram() instanceof Swc4jAstModule module) {
				List<ISwc4jAstModuleItem> list = module.getBody();
				List<UMLImport> imports = new ArrayList<>();
				String sourceFolder = UMLAdapterUtil.extractSourceFolder(filePath);
				int extensionLength = filePath.endsWith(".tsx") || filePath.endsWith(".jsx") ? 4 : 3;
				String moduleName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length() - extensionLength) : filePath.substring(0, filePath.length() - extensionLength);
				LocationInfo location = new LocationInfo(sourceFolder, filePath, module, CodeElementType.TYPE_DECLARATION, fileContent);
				List<UMLComment> commentList = extractComments(comments, sourceFolder, filePath, fileContent);
				UMLClass moduleClass = new UMLClass(moduleName, "__module__", location, true, imports);
				moduleClass.setModule(true);
				ModuleContainer moduleContainer = new ModuleContainer(location, moduleName);
				moduleContainer.addComments(commentList);
				OperationBody opBody = new TypeScriptOperationBody(
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
				moduleClass.getImportedTypes().addAll(moduleContainer.getNestedImports());
				umlModel.getClassList().addAll(moduleContainer.getNestedClasses());
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
			processCommentList(sourceFolder, filePath, fileContent, commentList, list);
		}
		Map<Integer, List<Swc4jComment>> trailing = comments.getTrailing();
		for(Integer key : trailing.keySet()) {
			List<Swc4jComment> list = trailing.get(key);
			processCommentList(sourceFolder, filePath, fileContent, commentList, list);
		}
		//like in the other languages, the comments are in the order they appear in the file
		commentList.sort(Comparator.comparingInt(comment -> comment.getLocationInfo().getStartOffset()));
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

	public static UMLOperation processFunctionDeclaration(String sourceFolder, String filePath, Swc4jAstFnDecl functionDecl, Map<String,Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className, List<UMLComment> comments) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, functionDecl, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionDecl.getIdent().getSym(), location, className);
		operation.setVisibility(Visibility.PRIVATE);
		Swc4jAstFunction function = functionDecl.getFunction();
		return processFunction(sourceFolder, filePath, function, activeVariableDeclarations, fileContent, operation, comments);
	}

	public static UMLOperation processFunctionDeclaration(String sourceFolder, String filePath, Swc4jAstClassMethod functionDecl, Map<String,Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className, List<UMLComment> comments) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, functionDecl, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionDecl.getKey().toString(), location, className);
		operation.setVisibility(Visibility.PRIVATE);
		Swc4jAstFunction function = functionDecl.getFunction();
		return processFunction(sourceFolder, filePath, function, activeVariableDeclarations, fileContent, operation, comments);
	}

	public static UMLOperation processFunctionDeclaration(String sourceFolder, String filePath, Swc4jAstPrivateMethod functionDecl, Map<String,Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className, List<UMLComment> comments) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, functionDecl, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionDecl.getKey().getName(), location, className);
		operation.setVisibility(Visibility.PRIVATE);
		Swc4jAstFunction function = functionDecl.getFunction();
		return processFunction(sourceFolder, filePath, function, activeVariableDeclarations, fileContent, operation, comments);
	}

	public static UMLOperation processFunction(String sourceFolder, String filePath, Swc4jAstFunction function,
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent,
			UMLOperation operation, List<UMLComment> comments) {
		return processFunction(sourceFolder, filePath, function, activeVariableDeclarations, fileContent, operation, comments, true);
	}

	//leadingComment: whether the function takes the comment in the line before it
	public static UMLOperation processFunction(String sourceFolder, String filePath, Swc4jAstFunction function,
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent,
			UMLOperation operation, List<UMLComment> comments, boolean leadingComment) {
		TypeScriptOperationBody.processComments(comments, operation, leadingComment);
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
				LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, param, CodeElementType.TYPE_PARAMETER, fileContent);
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
				parameter.setParameterEntry(identifier, sourceFolder, filePath, operation, activeVariableDeclarations, fileContent);
				if(parameter.getType() != null) {
					UMLParameter umlParameter = new UMLParameter(parameter.getVariableName(), parameter.getType(), "in", false);
					umlParameter.setVariableDeclaration(parameter);
					operation.addParameter(umlParameter);
				}
			}
		}
		Optional<Swc4jAstBlockStmt> body = function.getBody();
		if(body.isPresent()) {
			OperationBody operationBody = new TypeScriptOperationBody(sourceFolder, filePath, body.get(), operation, activeVariableDeclarations, fileContent);
			operation.setBody(operationBody);
		}
		int startSignatureOffset = function.getSpan().getStart();
		int endSignatureOffset = body.isPresent() ?
				body.get().getSpan().getStart() + 1 :
				function.getSpan().getEnd();
		String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
		operation.setActualSignature(text);
		return operation;
	}

	public static UMLOperation processConstructor(String sourceFolder, String filePath, Swc4jAstConstructor functionDecl,
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className, List<UMLComment> comments) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, functionDecl, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(functionDecl.getKey().toString(), location, className);
		TypeScriptOperationBody.processComments(comments, operation);
		operation.setVisibility(Visibility.PRIVATE);
		for(ISwc4jAstParamOrTsParamProp param : functionDecl.getParams()) {
			ISwc4jAstPat pat = null;
			if(param instanceof Swc4jAstParam p) {
				pat = p.getPat();
			}
			else if(param instanceof Swc4jAstTsParamProp paramProp && paramProp.getParam() instanceof ISwc4jAstPat paramPropPat) {
				//the parameter property, i.e., constructor(private source: string)
				pat = paramPropPat;
			}
			if(pat != null) {
				Swc4jAstTsTypeAnn typeAnnotation = VariableDeclaration.extractTypeAnnotation(pat);
				List<Swc4jAstBindingIdent> identifiers = VariableDeclaration.extractVariables(pat);
				for(Swc4jAstBindingIdent identifier : identifiers) {
					VariableDeclaration parameter = new VariableDeclaration(sourceFolder, filePath, typeAnnotation, identifier, operation, activeVariableDeclarations, fileContent);
					parameter.setParameter(true);
					parameter.setParameterEntry(identifier, sourceFolder, filePath, operation, activeVariableDeclarations, fileContent);
					if(parameter.getType() != null) {
						UMLParameter umlParameter = new UMLParameter(parameter.getVariableName(), parameter.getType(), "in", false);
						umlParameter.setVariableDeclaration(parameter);
						operation.addParameter(umlParameter);
					}
				}
			}
		}
		Optional<Swc4jAstBlockStmt> body = functionDecl.getBody();
		if(body.isPresent()) {
			OperationBody operationBody = new TypeScriptOperationBody(sourceFolder, filePath, body.get(), operation, activeVariableDeclarations, fileContent);
			operation.setBody(operationBody);
		}
		int startSignatureOffset = functionDecl.getSpan().getStart();
		int endSignatureOffset = body.isPresent() ?
				body.get().getSpan().getStart() + 1 :
				functionDecl.getSpan().getEnd();
		String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
		operation.setActualSignature(text);
		return operation;
	}

	public static UMLOperation processFunctionSignature(String sourceFolder, String filePath, Swc4jAstTsMethodSignature methodSignature,
			Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, String className) {
		LocationInfo location = new LocationInfo(sourceFolder, filePath, methodSignature, CodeElementType.METHOD_DECLARATION, fileContent);
		UMLOperation operation = new UMLOperation(methodSignature.getKey().toString(), location, className);
		operation.setVisibility(Visibility.PUBLIC);
		Optional<Swc4jAstTsTypeAnn> returnType = methodSignature.getTypeAnn();
		if(returnType.isPresent()) {
			UMLType type = UMLType.extractTypeObject(sourceFolder, filePath, fileContent, returnType.get().getTypeAnn(), 0);
			UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
			operation.addParameter(returnParameter);
		}
		Optional<Swc4jAstTsTypeParamDecl> typeParams = methodSignature.getTypeParams();
		if(typeParams.isPresent()) {
			List<Swc4jAstTsTypeParam> list = typeParams.get().getParams();
			for(Swc4jAstTsTypeParam param : list) {
				LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, param, CodeElementType.TYPE_PARAMETER, fileContent);
				UMLTypeParameter umlTypeParameter = new UMLTypeParameter(param.getName().getSym(), locationInfo);
				operation.addTypeParameter(umlTypeParameter);
			}
		}
		for(ISwc4jAstTsFnParam param : methodSignature.getParams()) {
			ISwc4jAstPat pat = null;
			if(param instanceof Swc4jAstArrayPat arrayPat) {
				pat = arrayPat;
			}
			else if(param instanceof Swc4jAstObjectPat objectPat) {
				pat = objectPat;
			}
			else if(param instanceof Swc4jAstRestPat restPat) {
				pat = restPat;
			}
			else if(param instanceof Swc4jAstBindingIdent identifier) {
				Swc4jAstTsTypeAnn typeAnnotation = identifier.getTypeAnn().isPresent() ? identifier.getTypeAnn().get() : null;
				VariableDeclaration parameter = new VariableDeclaration(sourceFolder, filePath, typeAnnotation, identifier, operation, activeVariableDeclarations, fileContent);
				parameter.setParameter(true);
				parameter.setParameterEntry(identifier, sourceFolder, filePath, operation, activeVariableDeclarations, fileContent);
				if(parameter.getType() != null) {
					UMLParameter umlParameter = new UMLParameter(parameter.getVariableName(), parameter.getType(), "in", false);
					umlParameter.setVariableDeclaration(parameter);
					operation.addParameter(umlParameter);
				}
			}
			if(pat != null) {
				Swc4jAstTsTypeAnn typeAnnotation = VariableDeclaration.extractTypeAnnotation(pat);
				List<Swc4jAstBindingIdent> identifiers = VariableDeclaration.extractVariables(pat);
				for(Swc4jAstBindingIdent identifier : identifiers) {
					VariableDeclaration parameter = new VariableDeclaration(sourceFolder, filePath, typeAnnotation, identifier, operation, activeVariableDeclarations, fileContent);
					parameter.setParameter(true);
					parameter.setParameterEntry(identifier, sourceFolder, filePath, operation, activeVariableDeclarations, fileContent);
					if(parameter.getType() != null) {
						UMLParameter umlParameter = new UMLParameter(parameter.getVariableName(), parameter.getType(), "in", false);
						umlParameter.setVariableDeclaration(parameter);
						operation.addParameter(umlParameter);
					}
				}
			}
		}
		int startSignatureOffset = methodSignature.getSpan().getStart();
		int endSignatureOffset = methodSignature.getSpan().getEnd();
		String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
		operation.setActualSignature(text);
		return operation;
	}
}
