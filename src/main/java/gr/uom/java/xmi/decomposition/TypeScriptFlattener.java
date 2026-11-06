package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.caoccao.javet.swc4j.ast.clazz.*;
import com.caoccao.javet.swc4j.ast.enums.Swc4jAstTruePlusMinus;
import com.caoccao.javet.swc4j.ast.expr.*;
import com.caoccao.javet.swc4j.ast.expr.lit.*;
import com.caoccao.javet.swc4j.ast.interfaces.*;
import com.caoccao.javet.swc4j.ast.miscs.*;
import com.caoccao.javet.swc4j.ast.module.*;
import com.caoccao.javet.swc4j.ast.pat.*;
import com.caoccao.javet.swc4j.ast.stmt.*;
import com.caoccao.javet.swc4j.ast.ts.*;

/**
 * Prints a TypeScript/JavaScript AST node in a canonical form, like ASTFlattener for Java and PyASTFlattener for Python,
 * so that the strings of statements and expressions do not depend on how the source was formatted:
 * <ul>
 * <li>the spacing of the TypeScript constants (Constants.TYPESCRIPT), i.e., {@code foo(a, b)}, {@code (x) => y}, {@code a ? b : c}, {@code x = y}</li>
 * <li>every statement ends with {@code ;} and a line break, and a block, i.e., the body of a function, prints its statements
 * one per line without indentation, so that the text of a node is the same wherever it is printed</li>
 * <li>an object literal prints one property per line only when it holds the body of a function, like the header of a Java anonymous class</li>
 * <li>literals keep their source text, comments are dropped, and JSX text is normalized like React does</li>
 * </ul>
 * A node type that is not supported, i.e., added by a later version of swc4j, is printed as its source text.
 */
public class TypeScriptFlattener {
	private final StringBuilder sb = new StringBuilder();
	private final String fileContent;

	private TypeScriptFlattener(String fileContent) {
		this.fileContent = fileContent;
	}

	public static String flatten(ISwc4jAst node, String fileContent) {
		TypeScriptFlattener flattener = new TypeScriptFlattener(fileContent);
		flattener.print(node);
		return flattener.sb.toString();
	}

	private void print(ISwc4jAst node) {
		if(node == null) {
			return;
		}
		if(node instanceof ISwc4jAstStmt || node instanceof ISwc4jAstModuleDecl) {
			statement(node);
		}
		else {
			expression(node);
		}
	}

	// ---------------------------------------------------------------- statements

	//a statement with its terminating line break
	private void statement(ISwc4jAst node) {
		if(node instanceof Swc4jAstExprStmt s) {
			expression(s.getExpr());
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstVarDecl s) {
			varDecl(s);
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstUsingDecl s) {
			sb.append(s.isAwait() ? "await using " : "using ");
			list(s.getDecls(), ", ", this::expression);
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstReturnStmt s) {
			sb.append("return");
			s.getArg().ifPresent(a -> { sb.append(' '); expression(a); });
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstThrowStmt s) {
			sb.append("throw ");
			expression(s.getArg());
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstBreakStmt s) {
			sb.append("break");
			s.getLabel().ifPresent(l -> sb.append(' ').append(l.getSym()));
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstContinueStmt s) {
			sb.append("continue");
			s.getLabel().ifPresent(l -> sb.append(' ').append(l.getSym()));
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstBlockStmt s) {
			block(s);
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstIfStmt s) {
			sb.append("if (");
			expression(s.getTest());
			sb.append(") ");
			body(s.getCons());
			s.getAlt().ifPresent(a -> { sb.append(" else "); body(a); });
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstForStmt s) {
			sb.append("for (");
			s.getInit().ifPresent(i -> { if(i instanceof Swc4jAstVarDecl v) varDecl(v); else expression(i); });
			sb.append(';');
			s.getTest().ifPresent(t -> { sb.append(' '); expression(t); });
			sb.append(';');
			s.getUpdate().ifPresent(u -> { sb.append(' '); expression(u); });
			sb.append(") ");
			body(s.getBody());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstForInStmt s) {
			sb.append("for (");
			forHead(s.getLeft());
			sb.append(" in ");
			expression(s.getRight());
			sb.append(") ");
			body(s.getBody());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstForOfStmt s) {
			sb.append(s.isAwait() ? "for await (" : "for (");
			forHead(s.getLeft());
			sb.append(" of ");
			expression(s.getRight());
			sb.append(") ");
			body(s.getBody());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstWhileStmt s) {
			sb.append("while (");
			expression(s.getTest());
			sb.append(") ");
			body(s.getBody());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstDoWhileStmt s) {
			sb.append("do ");
			body(s.getBody());
			sb.append(" while (");
			expression(s.getTest());
			sb.append(");\n");
		}
		else if(node instanceof Swc4jAstSwitchStmt s) {
			sb.append("switch (");
			expression(s.getDiscriminant());
			sb.append(") {\n");
			for(Swc4jAstSwitchCase c : s.getCases()) {
				if(c.getTest().isPresent()) {
					sb.append("case ");
					expression(c.getTest().get());
					sb.append(":\n");
				}
				else {
					sb.append("default:\n");
				}
				for(ISwc4jAstStmt cons : c.getCons()) {
					statement(cons);
				}
			}
			sb.append("}\n");
		}
		else if(node instanceof Swc4jAstTryStmt s) {
			sb.append("try ");
			block(s.getBlock());
			s.getHandler().ifPresent(h -> {
				sb.append(" catch ");
				h.getParam().ifPresent(p -> { sb.append('('); expression(p); sb.append(") "); });
				block(h.getBody());
			});
			s.getFinalizer().ifPresent(f -> { sb.append(" finally "); block(f); });
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstLabeledStmt s) {
			sb.append(s.getLabel().getSym()).append(": ");
			statement(s.getBody());
		}
		else if(node instanceof Swc4jAstWithStmt s) {
			sb.append("with (");
			expression(s.getObj());
			sb.append(") ");
			body(s.getBody());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstEmptyStmt) {
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstDebuggerStmt) {
			sb.append("debugger;\n");
		}
		else if(node instanceof Swc4jAstFnDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			function(s.getFunction(), "function", s.getIdent().getSym());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstClassDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			clazz(s.getClazz(), s.getIdent().getSym());
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstTsInterfaceDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			sb.append("interface ").append(s.getId().getSym());
			s.getTypeParams().ifPresent(this::typeParams);
			if(!s.getExtends().isEmpty()) {
				sb.append(" extends ");
				list(s.getExtends(), ", ", this::expression);
			}
			sb.append(' ');
			typeMembers(s.getBody().getBody(), true);
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstTsTypeAliasDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			sb.append("type ").append(s.getId().getSym());
			s.getTypeParams().ifPresent(this::typeParams);
			sb.append(" = ");
			type(s.getTypeAnn());
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstTsEnumDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			if(s.isConst()) sb.append("const ");
			sb.append("enum ").append(s.getId().getSym()).append(" {\n");
			for(Swc4jAstTsEnumMember m : s.getMembers()) {
				expression(m.getId());
				m.getInit().ifPresent(i -> { sb.append(" = "); expression(i); });
				sb.append(",\n");
			}
			sb.append("}\n");
		}
		else if(node instanceof Swc4jAstTsModuleDecl s) {
			if(s.isDeclare()) sb.append("declare ");
			sb.append(s.isGlobal() ? "global" : s.isNamespace() ? "namespace " : "module ");
			if(!s.isGlobal()) expression(s.getId());
			s.getBody().ifPresent(b -> { sb.append(' '); namespaceBody(b); });
			sb.append('\n');
		}
		else if(node instanceof Swc4jAstImportDecl s) {
			importDecl(s);
		}
		else if(node instanceof Swc4jAstExportDecl s) {
			sb.append("export ");
			statement(s.getDecl());
		}
		else if(node instanceof Swc4jAstExportDefaultDecl s) {
			sb.append("export default ");
			ISwc4jAstDefaultDecl decl = s.getDecl();
			if(decl instanceof Swc4jAstFnExpr f) {
				function(f.getFunction(), "function", f.getIdent().map(Swc4jAstIdent::getSym).orElse(null));
				sb.append('\n');
			}
			else if(decl instanceof Swc4jAstClassExpr c) {
				clazz(c.getClazz(), c.getIdent().map(Swc4jAstIdent::getSym).orElse(null));
				sb.append('\n');
			}
			else {
				statement(decl);
			}
		}
		else if(node instanceof Swc4jAstExportDefaultExpr s) {
			sb.append("export default ");
			expression(s.getExpr());
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstNamedExport s) {
			sb.append(s.isTypeOnly() ? "export type { " : "export { ");
			list(s.getSpecifiers(), ", ", this::expression);
			sb.append(" }");
			s.getSrc().ifPresent(src -> { sb.append(" from "); expression(src); });
			s.getWith().ifPresent(w -> { sb.append(" with "); expression(w); });
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstExportAll s) {
			sb.append(s.isTypeOnly() ? "export type * from " : "export * from ");
			expression(s.getSrc());
			s.getWith().ifPresent(w -> { sb.append(" with "); expression(w); });
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstTsExportAssignment s) {
			sb.append("export = ");
			expression(s.getExpr());
			sb.append(";\n");
		}
		else if(node instanceof Swc4jAstTsNamespaceExportDecl s) {
			sb.append("export as namespace ").append(s.getId().getSym()).append(";\n");
		}
		else if(node instanceof Swc4jAstTsImportEqualsDecl s) {
			if(s.isExport()) sb.append("export ");
			sb.append(s.isTypeOnly() ? "import type " : "import ").append(s.getId().getSym()).append(" = ");
			expression(s.getModuleRef());
			sb.append(";\n");
		}
		else {
			source(node);
			sb.append('\n');
		}
	}

	//the body of a control statement: a block without its line break, or a single statement without its line break
	private void body(ISwc4jAstStmt node) {
		if(node instanceof Swc4jAstBlockStmt b) {
			block(b);
		}
		else {
			int start = sb.length();
			statement(node);
			if(sb.length() > start && sb.charAt(sb.length() - 1) == '\n') {
				sb.setLength(sb.length() - 1);
			}
		}
	}

	private void block(Swc4jAstBlockStmt block) {
		if(block.getStmts().isEmpty()) {
			sb.append("{\n}");
			return;
		}
		sb.append("{\n");
		for(ISwc4jAstStmt s : block.getStmts()) {
			statement(s);
		}
		sb.append('}');
	}

	private void varDecl(Swc4jAstVarDecl decl) {
		if(decl.isDeclare()) sb.append("declare ");
		sb.append(decl.getKind().getName()).append(' ');
		list(decl.getDecls(), ", ", this::expression);
	}

	private void forHead(ISwc4jAstForHead head) {
		if(head instanceof Swc4jAstVarDecl v) {
			varDecl(v);
		}
		else if(head instanceof Swc4jAstUsingDecl u) {
			sb.append(u.isAwait() ? "await using " : "using ");
			list(u.getDecls(), ", ", this::expression);
		}
		else {
			expression(head);
		}
	}

	private void namespaceBody(ISwc4jAstTsNamespaceBody body) {
		if(body instanceof Swc4jAstTsModuleBlock b) {
			sb.append("{\n");
			for(ISwc4jAstModuleItem item : b.getBody()) {
				statement(item);
			}
			sb.append('}');
		}
		else if(body instanceof Swc4jAstTsNamespaceDecl n) {
			sb.append('.').append(n.getId().getSym()).append(' ');
			namespaceBody(n.getBody());
		}
	}

	private void importDecl(Swc4jAstImportDecl s) {
		sb.append(s.isTypeOnly() ? "import type " : "import ");
		List<ISwc4jAstImportSpecifier> specifiers = s.getSpecifiers();
		boolean named = false;
		boolean first = true;
		for(ISwc4jAstImportSpecifier specifier : specifiers) {
			if(specifier instanceof Swc4jAstImportNamedSpecifier n) {
				if(!named) {
					if(!first) sb.append(", ");
					sb.append("{ ");
					named = true;
				}
				else {
					sb.append(", ");
				}
				if(n.isTypeOnly()) sb.append("type ");
				n.getImported().ifPresent(i -> { expression(i); sb.append(" as "); });
				sb.append(n.getLocal().getSym());
			}
			else {
				if(!first) sb.append(", ");
				if(specifier instanceof Swc4jAstImportStarAsSpecifier star) {
					sb.append("* as ").append(star.getLocal().getSym());
				}
				else if(specifier instanceof Swc4jAstImportDefaultSpecifier d) {
					sb.append(d.getLocal().getSym());
				}
			}
			first = false;
		}
		if(named) sb.append(" }");
		if(!specifiers.isEmpty()) sb.append(" from ");
		expression(s.getSrc());
		s.getWith().ifPresent(w -> { sb.append(" with "); expression(w); });
		sb.append(";\n");
	}

	// ---------------------------------------------------------------- functions and classes

	//keyword is "function" for declarations and expressions, or null for methods
	private void function(Swc4jAstFunction f, String keyword, String name) {
		for(Swc4jAstDecorator d : f.getDecorators()) {
			expression(d);
			sb.append(' ');
		}
		if(f.isAsync()) sb.append("async ");
		if(keyword != null) {
			sb.append(keyword);
			if(f.isGenerator()) sb.append('*');
			if(name != null) sb.append(' ').append(name);
		}
		else {
			if(f.isGenerator()) sb.append('*');
			if(name != null) sb.append(name);
		}
		f.getTypeParams().ifPresent(this::typeParams);
		sb.append('(');
		list(f.getParams(), ", ", this::expression);
		sb.append(')');
		f.getReturnType().ifPresent(this::expression);
		if(f.getBody().isPresent()) {
			sb.append(' ');
			block(f.getBody().get());
		}
		else {
			sb.append(';');
		}
	}

	private void clazz(Swc4jAstClass c, String name) {
		for(Swc4jAstDecorator d : c.getDecorators()) {
			expression(d);
			sb.append(' ');
		}
		if(c.isAbstract()) sb.append("abstract ");
		sb.append("class");
		if(name != null) sb.append(' ').append(name);
		c.getTypeParams().ifPresent(this::typeParams);
		c.getSuperClass().ifPresent(s -> {
			sb.append(" extends ");
			expression(s);
			c.getSuperTypeParams().ifPresent(this::expression);
		});
		if(!c.getImplements().isEmpty()) {
			sb.append(" implements ");
			list(c.getImplements(), ", ", this::expression);
		}
		sb.append(" {\n");
		for(ISwc4jAstClassMember member : c.getBody()) {
			classMember(member);
		}
		sb.append('}');
	}

	private void classMember(ISwc4jAstClassMember member) {
		if(member instanceof Swc4jAstClassMethod m) {
			modifiers(m.getAccessibility(), m.isStatic(), m.isAbstract(), m.isOverride(), false);
			method(m.getFunction(), m.getKind().name(), m.getKey(), m.isOptional());
			sb.append('\n');
		}
		else if(member instanceof Swc4jAstPrivateMethod m) {
			modifiers(m.getAccessibility(), m.isStatic(), m.isAbstract(), m.isOverride(), false);
			method(m.getFunction(), m.getKind().name(), m.getKey(), m.isOptional());
			sb.append('\n');
		}
		else if(member instanceof Swc4jAstConstructor c) {
			c.getAccessibility().ifPresent(a -> sb.append(a.getName()).append(' '));
			sb.append("constructor(");
			list(c.getParams(), ", ", this::expression);
			sb.append(')');
			if(c.getBody().isPresent()) {
				sb.append(' ');
				block(c.getBody().get());
			}
			else {
				sb.append(';');
			}
			sb.append('\n');
		}
		else if(member instanceof Swc4jAstClassProp p) {
			decorators(p.getDecorators());
			if(p.isDeclare()) sb.append("declare ");
			modifiers(p.getAccessibility(), p.isStatic(), p.isAbstract(), p.isOverride(), p.isReadonly());
			propertyName(p.getKey());
			if(p.isOptional()) sb.append('?');
			if(p.isDefinite()) sb.append('!');
			p.getTypeAnn().ifPresent(this::expression);
			p.getValue().ifPresent(v -> { sb.append(" = "); expression(v); });
			sb.append(";\n");
		}
		else if(member instanceof Swc4jAstPrivateProp p) {
			decorators(p.getDecorators());
			modifiers(p.getAccessibility(), p.isStatic(), false, p.isOverride(), p.isReadonly());
			expression(p.getKey());
			if(p.isOptional()) sb.append('?');
			if(p.isDefinite()) sb.append('!');
			p.getTypeAnn().ifPresent(this::expression);
			p.getValue().ifPresent(v -> { sb.append(" = "); expression(v); });
			sb.append(";\n");
		}
		else if(member instanceof Swc4jAstAutoAccessor a) {
			decorators(a.getDecorators());
			modifiers(a.getAccessibility(), a.isStatic(), a.isAbstract(), a.isOverride(), false);
			sb.append("accessor ");
			expression(a.getKey());
			if(a.isDefinite()) sb.append('!');
			a.getTypeAnn().ifPresent(this::expression);
			a.getValue().ifPresent(v -> { sb.append(" = "); expression(v); });
			sb.append(";\n");
		}
		else if(member instanceof Swc4jAstStaticBlock b) {
			sb.append("static ");
			block(b.getBody());
			sb.append('\n');
		}
		else if(member instanceof Swc4jAstTsIndexSignature s) {
			typeMember(s);
			sb.append(";\n");
		}
		else if(member instanceof Swc4jAstEmptyStmt) {
			//a stray semicolon between members
		}
		else {
			source(member);
			sb.append('\n');
		}
	}

	private void decorators(List<Swc4jAstDecorator> decorators) {
		for(Swc4jAstDecorator d : decorators) {
			expression(d);
			sb.append(' ');
		}
	}

	private void modifiers(Optional<com.caoccao.javet.swc4j.ast.enums.Swc4jAstAccessibility> accessibility, boolean isStatic, boolean isAbstract, boolean isOverride, boolean isReadonly) {
		accessibility.ifPresent(a -> sb.append(a.getName()).append(' '));
		if(isStatic) sb.append("static ");
		if(isAbstract) sb.append("abstract ");
		if(isOverride) sb.append("override ");
		if(isReadonly) sb.append("readonly ");
	}

	//a method of a class or an object literal: kind is Method, Getter or Setter
	private void method(Swc4jAstFunction f, String kind, ISwc4jAst key, boolean optional) {
		decorators(f.getDecorators());
		if(f.isAsync()) sb.append("async ");
		if(kind.equals("Getter")) sb.append("get ");
		else if(kind.equals("Setter")) sb.append("set ");
		if(f.isGenerator()) sb.append('*');
		propertyName(key);
		if(optional) sb.append('?');
		f.getTypeParams().ifPresent(this::typeParams);
		sb.append('(');
		list(f.getParams(), ", ", this::expression);
		sb.append(')');
		f.getReturnType().ifPresent(this::expression);
		if(f.getBody().isPresent()) {
			sb.append(' ');
			block(f.getBody().get());
		}
		else {
			sb.append(';');
		}
	}

	private void propertyName(ISwc4jAst key) {
		if(key instanceof Swc4jAstComputedPropName c) {
			sb.append('[');
			expression(c.getExpr());
			sb.append(']');
		}
		else {
			expression(key);
		}
	}

	// ---------------------------------------------------------------- expressions

	private void expression(ISwc4jAst node) {
		if(node == null) {
			return;
		}
		if(node instanceof Swc4jAstIdent n) {
			sb.append(n.getSym());
			if(n.isOptional()) sb.append('?');
		}
		else if(node instanceof Swc4jAstIdentName n) {
			sb.append(n.getSym());
		}
		else if(node instanceof Swc4jAstPrivateName n) {
			sb.append('#').append(n.getName());
		}
		else if(node instanceof Swc4jAstThisExpr) {
			sb.append("this");
		}
		else if(node instanceof Swc4jAstSuper) {
			sb.append("super");
		}
		else if(node instanceof Swc4jAstImport i) {
			sb.append("import");
			String phase = i.getPhase().getName();
			if(!phase.isEmpty() && !phase.equalsIgnoreCase("evaluation")) sb.append('.').append(phase.toLowerCase());
		}
		else if(node instanceof Swc4jAstStr n) {
			sb.append(n.getRaw().orElseGet(() -> "\"" + n.getValue().replace("\\", "\\\\").replace("\"", "\\\"") + "\""));
		}
		else if(node instanceof Swc4jAstNumber n) {
			sb.append(n.getRaw().orElseGet(() -> sourceText(n)));
		}
		else if(node instanceof Swc4jAstBigInt n) {
			sb.append(n.getRaw().orElseGet(() -> n.getValue() + "n"));
		}
		else if(node instanceof Swc4jAstBool n) {
			sb.append(n.isValue());
		}
		else if(node instanceof Swc4jAstNull) {
			sb.append("null");
		}
		else if(node instanceof Swc4jAstRegex n) {
			sb.append('/').append(n.getExp()).append('/').append(n.getFlags());
		}
		else if(node instanceof Swc4jAstTpl n) {
			template(n.getQuasis(), n.getExprs(), this::expression);
		}
		else if(node instanceof Swc4jAstTaggedTpl n) {
			expression(n.getTag());
			n.getTypeParams().ifPresent(this::expression);
			expression(n.getTpl());
		}
		else if(node instanceof Swc4jAstArrayLit n) {
			sb.append('[');
			List<Optional<Swc4jAstExprOrSpread>> elems = n.getElems();
			for(int i = 0; i < elems.size(); i++) {
				if(i > 0) sb.append(", ");
				elems.get(i).ifPresent(this::expression);
			}
			//a trailing hole needs its own comma: [a, ,]
			if(!elems.isEmpty() && elems.get(elems.size() - 1).isEmpty()) sb.append(',');
			sb.append(']');
		}
		else if(node instanceof Swc4jAstObjectLit n) {
			objectLiteral(n);
		}
		else if(node instanceof Swc4jAstKeyValueProp n) {
			propertyName(n.getKey());
			sb.append(": ");
			expression(n.getValue());
		}
		else if(node instanceof Swc4jAstAssignProp n) {
			sb.append(n.getKey().getSym()).append(" = ");
			expression(n.getValue());
		}
		else if(node instanceof Swc4jAstMethodProp n) {
			method(n.getFunction(), "Method", n.getKey(), false);
		}
		else if(node instanceof Swc4jAstGetterProp n) {
			sb.append("get ");
			propertyName(n.getKey());
			sb.append("()");
			n.getTypeAnn().ifPresent(this::expression);
			n.getBody().ifPresent(b -> { sb.append(' '); block(b); });
		}
		else if(node instanceof Swc4jAstSetterProp n) {
			sb.append("set ");
			propertyName(n.getKey());
			sb.append('(');
			n.getThisParam().ifPresent(t -> { expression(t); sb.append(", "); });
			expression(n.getParam());
			sb.append(')');
			n.getBody().ifPresent(b -> { sb.append(' '); block(b); });
		}
		else if(node instanceof Swc4jAstSpreadElement n) {
			sb.append("...");
			expression(n.getExpr());
		}
		else if(node instanceof Swc4jAstExprOrSpread n) {
			if(n.getSpread().isPresent()) sb.append("...");
			expression(n.getExpr());
		}
		else if(node instanceof Swc4jAstComputedPropName n) {
			propertyName(n);
		}
		else if(node instanceof Swc4jAstFnExpr n) {
			function(n.getFunction(), "function", n.getIdent().map(Swc4jAstIdent::getSym).orElse(null));
		}
		else if(node instanceof Swc4jAstArrowExpr n) {
			if(n.isAsync()) sb.append("async ");
			n.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(n.getParams(), ", ", this::expression);
			sb.append(')');
			n.getReturnType().ifPresent(this::expression);
			sb.append(" => ");
			ISwc4jAstBlockStmtOrExpr body = n.getBody();
			if(body instanceof Swc4jAstBlockStmt b) {
				block(b);
			}
			else {
				expression(body);
			}
		}
		else if(node instanceof Swc4jAstClassExpr n) {
			clazz(n.getClazz(), n.getIdent().map(Swc4jAstIdent::getSym).orElse(null));
		}
		else if(node instanceof Swc4jAstUnaryExpr n) {
			String op = n.getOp().getName();
			sb.append(op);
			if(Character.isLetter(op.charAt(0))) sb.append(' ');
			expression(n.getArg());
		}
		else if(node instanceof Swc4jAstUpdateExpr n) {
			if(n.isPrefix()) {
				sb.append(n.getOp().getName());
				expression(n.getArg());
			}
			else {
				expression(n.getArg());
				sb.append(n.getOp().getName());
			}
		}
		else if(node instanceof Swc4jAstBinExpr n) {
			expression(n.getLeft());
			sb.append(' ').append(n.getOp().getName()).append(' ');
			expression(n.getRight());
		}
		else if(node instanceof Swc4jAstAssignExpr n) {
			expression(n.getLeft());
			sb.append(' ').append(n.getOp().getName()).append(' ');
			expression(n.getRight());
		}
		else if(node instanceof Swc4jAstCondExpr n) {
			expression(n.getTest());
			sb.append(" ? ");
			expression(n.getCons());
			sb.append(" : ");
			expression(n.getAlt());
		}
		else if(node instanceof Swc4jAstSeqExpr n) {
			list(n.getExprs(), ", ", this::expression);
		}
		else if(node instanceof Swc4jAstParenExpr n) {
			sb.append('(');
			expression(n.getExpr());
			sb.append(')');
		}
		else if(node instanceof Swc4jAstMemberExpr n) {
			expression(n.getObj());
			memberProperty(n.getProp(), false);
		}
		else if(node instanceof Swc4jAstSuperPropExpr n) {
			sb.append("super");
			memberProperty(n.getProp(), false);
		}
		else if(node instanceof Swc4jAstOptChainExpr n) {
			ISwc4jAstOptChainBase base = n.getBase();
			if(base instanceof Swc4jAstMemberExpr m) {
				expression(m.getObj());
				memberProperty(m.getProp(), n.isOptional());
			}
			else if(base instanceof Swc4jAstOptCall c) {
				expression(c.getCallee());
				if(n.isOptional()) sb.append("?.");
				c.getTypeArgs().ifPresent(this::expression);
				arguments(c.getArgs());
			}
			else {
				expression(base);
			}
		}
		else if(node instanceof Swc4jAstCallExpr n) {
			expression(n.getCallee());
			n.getTypeArgs().ifPresent(this::expression);
			arguments(n.getArgs());
		}
		else if(node instanceof Swc4jAstNewExpr n) {
			sb.append("new ");
			expression(n.getCallee());
			n.getTypeArgs().ifPresent(this::expression);
			n.getArgs().ifPresent(this::arguments);
		}
		else if(node instanceof Swc4jAstAwaitExpr n) {
			sb.append("await ");
			expression(n.getArg());
		}
		else if(node instanceof Swc4jAstYieldExpr n) {
			sb.append(n.isDelegate() ? "yield*" : "yield");
			n.getArg().ifPresent(a -> { sb.append(' '); expression(a); });
		}
		else if(node instanceof Swc4jAstMetaPropExpr n) {
			sb.append(n.getKind().getName());
		}
		else if(node instanceof Swc4jAstTsAsExpr n) {
			expression(n.getExpr());
			sb.append(" as ");
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsSatisfiesExpr n) {
			expression(n.getExpr());
			sb.append(" satisfies ");
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsConstAssertion n) {
			expression(n.getExpr());
			sb.append(" as const");
		}
		else if(node instanceof Swc4jAstTsNonNullExpr n) {
			expression(n.getExpr());
			sb.append('!');
		}
		else if(node instanceof Swc4jAstTsTypeAssertion n) {
			sb.append('<');
			type(n.getTypeAnn());
			sb.append('>');
			expression(n.getExpr());
		}
		else if(node instanceof Swc4jAstTsInstantiation n) {
			expression(n.getExpr());
			expression(n.getTypeArgs());
		}
		else if(node instanceof Swc4jAstDecorator n) {
			sb.append('@');
			expression(n.getExpr());
		}
		else if(node instanceof Swc4jAstVarDeclarator n) {
			expression(n.getName());
			if(n.isDefinite()) sb.append('!');
			n.getInit().ifPresent(i -> { sb.append(" = "); expression(i); });
		}
		else if(isPattern(node)) {
			pattern(node);
		}
		else if(isJsx(node)) {
			jsx(node);
		}
		else if(node instanceof ISwc4jAstTsType t) {
			type(t);
		}
		else if(node instanceof Swc4jAstTsTypeAnn n) {
			sb.append(": ");
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsTypeParamInstantiation n) {
			sb.append('<');
			list(n.getParams(), ", ", this::type);
			sb.append('>');
		}
		else if(node instanceof Swc4jAstTsTypeParamDecl n) {
			typeParams(n);
		}
		else if(node instanceof Swc4jAstTsExprWithTypeArgs n) {
			expression(n.getExpr());
			n.getTypeArgs().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstTsQualifiedName n) {
			expression(n.getLeft());
			sb.append('.').append(n.getRight().getSym());
		}
		else if(node instanceof Swc4jAstTsExternalModuleRef n) {
			sb.append("require(");
			expression(n.getExpr());
			sb.append(')');
		}
		else if(node instanceof Swc4jAstExportNamedSpecifier n) {
			if(n.isTypeOnly()) sb.append("type ");
			expression(n.getOrig());
			n.getExported().ifPresent(e -> { sb.append(" as "); expression(e); });
		}
		else if(node instanceof Swc4jAstExportNamespaceSpecifier n) {
			sb.append("* as ");
			expression(n.getName());
		}
		else if(node instanceof Swc4jAstExportDefaultSpecifier n) {
			sb.append(n.getExported().getSym());
		}
		else if(node instanceof ISwc4jAstStmt || node instanceof ISwc4jAstModuleDecl) {
			statement(node);
		}
		else {
			source(node);
		}
	}

	private void memberProperty(ISwc4jAst prop, boolean optional) {
		if(prop instanceof Swc4jAstComputedPropName c) {
			if(optional) sb.append("?.");
			sb.append('[');
			expression(c.getExpr());
			sb.append(']');
		}
		else {
			sb.append(optional ? "?." : ".");
			expression(prop);
		}
	}

	private void arguments(List<Swc4jAstExprOrSpread> args) {
		sb.append('(');
		list(args, ", ", this::expression);
		sb.append(')');
	}

	private <T> void template(List<Swc4jAstTplElement> quasis, List<T> exprs, Consumer<T> printer) {
		sb.append('`');
		for(int i = 0; i < quasis.size(); i++) {
			sb.append(quasis.get(i).getRaw());
			if(i < exprs.size()) {
				sb.append("${");
				printer.accept(exprs.get(i));
				sb.append('}');
			}
		}
		sb.append('`');
	}

	private void objectLiteral(Swc4jAstObjectLit n) {
		List<ISwc4jAstPropOrSpread> props = n.getProps();
		if(props.isEmpty()) {
			sb.append("{}");
		}
		else if(holdsFunctionBody(n)) {
			sb.append("{\n");
			list(props, ",\n", this::expression);
			sb.append("\n}");
		}
		else {
			sb.append("{ ");
			list(props, ", ", this::expression);
			sb.append(" }");
		}
	}

	//an object literal holding the body of a function prints one property per line, like a Java anonymous class
	private static boolean holdsFunctionBody(ISwc4jAst node) {
		if(node instanceof Swc4jAstFnExpr || node instanceof Swc4jAstMethodProp || node instanceof Swc4jAstGetterProp ||
				node instanceof Swc4jAstSetterProp || node instanceof Swc4jAstClassExpr) {
			return true;
		}
		if(node instanceof Swc4jAstArrowExpr arrow && arrow.getBody() instanceof Swc4jAstBlockStmt) {
			return true;
		}
		for(ISwc4jAst child : node.getChildNodes()) {
			if(child != null && holdsFunctionBody(child)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- patterns

	private static boolean isPattern(ISwc4jAst node) {
		return node instanceof Swc4jAstBindingIdent || node instanceof Swc4jAstArrayPat || node instanceof Swc4jAstObjectPat ||
				node instanceof Swc4jAstAssignPat || node instanceof Swc4jAstRestPat || node instanceof Swc4jAstKeyValuePatProp ||
				node instanceof Swc4jAstAssignPatProp || node instanceof Swc4jAstParam || node instanceof Swc4jAstTsParamProp;
	}

	private void pattern(ISwc4jAst node) {
		if(node instanceof Swc4jAstBindingIdent n) {
			expression(n.getId());
			n.getTypeAnn().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstArrayPat n) {
			sb.append('[');
			List<Optional<ISwc4jAstPat>> elems = n.getElems();
			for(int i = 0; i < elems.size(); i++) {
				if(i > 0) sb.append(", ");
				elems.get(i).ifPresent(this::expression);
			}
			if(!elems.isEmpty() && elems.get(elems.size() - 1).isEmpty()) sb.append(',');
			sb.append(']');
			if(n.isOptional()) sb.append('?');
			n.getTypeAnn().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstObjectPat n) {
			if(n.getProps().isEmpty()) {
				sb.append("{}");
			}
			else {
				sb.append("{ ");
				list(n.getProps(), ", ", this::expression);
				sb.append(" }");
			}
			if(n.isOptional()) sb.append('?');
			n.getTypeAnn().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstAssignPat n) {
			expression(n.getLeft());
			sb.append(" = ");
			expression(n.getRight());
		}
		else if(node instanceof Swc4jAstRestPat n) {
			sb.append("...");
			expression(n.getArg());
			n.getTypeAnn().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstKeyValuePatProp n) {
			propertyName(n.getKey());
			sb.append(": ");
			expression(n.getValue());
		}
		else if(node instanceof Swc4jAstAssignPatProp n) {
			expression(n.getKey());
			n.getValue().ifPresent(v -> { sb.append(" = "); expression(v); });
		}
		else if(node instanceof Swc4jAstParam n) {
			decorators(n.getDecorators());
			expression(n.getPat());
		}
		else if(node instanceof Swc4jAstTsParamProp n) {
			decorators(n.getDecorators());
			modifiers(n.getAccessibility(), false, false, n.isOverride(), n.isReadonly());
			expression(n.getParam());
		}
	}

	// ---------------------------------------------------------------- JSX

	private static boolean isJsx(ISwc4jAst node) {
		return node instanceof Swc4jAstJsxElement || node instanceof Swc4jAstJsxFragment || node instanceof Swc4jAstJsxText ||
				node instanceof Swc4jAstJsxExprContainer || node instanceof Swc4jAstJsxEmptyExpr || node instanceof Swc4jAstJsxSpreadChild ||
				node instanceof Swc4jAstJsxMemberExpr || node instanceof Swc4jAstJsxNamespacedName || node instanceof Swc4jAstJsxAttr ||
				node instanceof Swc4jAstJsxOpeningElement || node instanceof Swc4jAstJsxClosingElement;
	}

	private void jsx(ISwc4jAst node) {
		if(node instanceof Swc4jAstJsxElement n) {
			expression(n.getOpening());
			jsxChildren(n.getChildren());
			n.getClosing().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstJsxFragment n) {
			sb.append("<>");
			jsxChildren(n.getChildren());
			sb.append("</>");
		}
		else if(node instanceof Swc4jAstJsxOpeningElement n) {
			sb.append('<');
			expression(n.getName());
			n.getTypeArgs().ifPresent(this::expression);
			for(ISwc4jAstJsxAttrOrSpread attr : n.getAttrs()) {
				sb.append(' ');
				if(attr instanceof Swc4jAstSpreadElement spread) {
					sb.append("{...");
					expression(spread.getExpr());
					sb.append('}');
				}
				else {
					expression(attr);
				}
			}
			sb.append(n.isSelfClosing() ? " />" : ">");
		}
		else if(node instanceof Swc4jAstJsxClosingElement n) {
			sb.append("</");
			expression(n.getName());
			sb.append('>');
		}
		else if(node instanceof Swc4jAstJsxAttr n) {
			expression(n.getName());
			n.getValue().ifPresent(v -> { sb.append('='); expression(v); });
		}
		else if(node instanceof Swc4jAstJsxExprContainer n) {
			sb.append('{');
			if(!(n.getExpr() instanceof Swc4jAstJsxEmptyExpr)) expression(n.getExpr());
			sb.append('}');
		}
		else if(node instanceof Swc4jAstJsxSpreadChild n) {
			sb.append("{...");
			expression(n.getExpr());
			sb.append('}');
		}
		else if(node instanceof Swc4jAstJsxMemberExpr n) {
			expression(n.getObj());
			sb.append('.').append(n.getProp().getSym());
		}
		else if(node instanceof Swc4jAstJsxNamespacedName n) {
			sb.append(n.getNs().getSym()).append(':').append(n.getName().getSym());
		}
		else if(node instanceof Swc4jAstJsxText n) {
			sb.append(jsxText(n.getRaw()));
		}
	}

	//the children of an element without the formatting text between them and without the comments {/* ... */}
	private void jsxChildren(List<ISwc4jAstJsxElementChild> children) {
		for(ISwc4jAstJsxElementChild child : children) {
			if(child instanceof Swc4jAstJsxText text && jsxText(text.getRaw()).isEmpty()) {
				continue;
			}
			if(child instanceof Swc4jAstJsxExprContainer container && container.getExpr() instanceof Swc4jAstJsxEmptyExpr) {
				continue;
			}
			expression(child);
		}
	}

	//the text of a JSX element as React renders it: the lines are trimmed (the first only at its end, the last only at its start),
	//the empty ones dropped, and the rest joined with a space
	static String jsxText(String raw) {
		if(raw.indexOf('\n') < 0) {
			return raw;
		}
		String[] lines = raw.split("\\r?\\n", -1);
		StringBuilder text = new StringBuilder();
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(i > 0) line = line.stripLeading();
			if(i < lines.length - 1) line = line.stripTrailing();
			if(line.isEmpty()) continue;
			if(text.length() > 0) text.append(' ');
			text.append(line);
		}
		return text.toString();
	}

	// ---------------------------------------------------------------- types

	private void type(ISwc4jAst node) {
		if(node instanceof Swc4jAstTsKeywordType n) {
			sb.append(n.getKind().getName());
		}
		else if(node instanceof Swc4jAstTsTypeRef n) {
			expression(n.getTypeName());
			n.getTypeParams().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstTsArrayType n) {
			type(n.getElemType());
			sb.append("[]");
		}
		else if(node instanceof Swc4jAstTsUnionType n) {
			list(n.getTypes(), " | ", this::type);
		}
		else if(node instanceof Swc4jAstTsIntersectionType n) {
			list(n.getTypes(), " & ", this::type);
		}
		else if(node instanceof Swc4jAstTsParenthesizedType n) {
			sb.append('(');
			type(n.getTypeAnn());
			sb.append(')');
		}
		else if(node instanceof Swc4jAstTsFnType n) {
			n.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(n.getParams(), ", ", this::expression);
			sb.append(") => ");
			type(n.getTypeAnn().getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsConstructorType n) {
			if(n.isAbstract()) sb.append("abstract ");
			sb.append("new ");
			n.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(n.getParams(), ", ", this::expression);
			sb.append(") => ");
			type(n.getTypeAnn().getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsTypeLit n) {
			typeMembers(n.getMembers(), false);
		}
		else if(node instanceof Swc4jAstTsTupleType n) {
			sb.append('[');
			list(n.getElemTypes(), ", ", e -> {
				e.getLabel().ifPresent(l -> { expression(l); sb.append(": "); });
				type(e.getTy());
			});
			sb.append(']');
		}
		else if(node instanceof Swc4jAstTsOptionalType n) {
			type(n.getTypeAnn());
			sb.append('?');
		}
		else if(node instanceof Swc4jAstTsRestType n) {
			sb.append("...");
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsConditionalType n) {
			type(n.getCheckType());
			sb.append(" extends ");
			type(n.getExtendsType());
			sb.append(" ? ");
			type(n.getTrueType());
			sb.append(" : ");
			type(n.getFalseType());
		}
		else if(node instanceof Swc4jAstTsInferType n) {
			sb.append("infer ");
			typeParam(n.getTypeParam());
		}
		else if(node instanceof Swc4jAstTsIndexedAccessType n) {
			if(n.isReadonly()) sb.append("readonly ");
			type(n.getObjType());
			sb.append('[');
			type(n.getIndexType());
			sb.append(']');
		}
		else if(node instanceof Swc4jAstTsTypeOperator n) {
			sb.append(n.getOp().getName()).append(' ');
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsMappedType n) {
			sb.append("{ ");
			n.getReadonly().ifPresent(r -> sb.append(plusMinus(r)).append("readonly "));
			sb.append('[').append(n.getTypeParam().getName().getSym()).append(" in ");
			n.getTypeParam().getConstraint().ifPresent(this::type);
			n.getNameType().ifPresent(t -> { sb.append(" as "); type(t); });
			sb.append(']');
			n.getOptional().ifPresent(o -> sb.append(plusMinus(o)).append('?'));
			n.getTypeAnn().ifPresent(t -> { sb.append(": "); type(t); });
			sb.append(" }");
		}
		else if(node instanceof Swc4jAstTsLitType n) {
			ISwc4jAstTsLit lit = n.getLit();
			if(lit instanceof Swc4jAstTsTplLitType tpl) {
				type(tpl);
			}
			else {
				expression(lit);
			}
		}
		else if(node instanceof Swc4jAstTsTplLitType n) {
			template(n.getQuasis(), n.getTypes(), this::type);
		}
		else if(node instanceof Swc4jAstTsThisType) {
			sb.append("this");
		}
		else if(node instanceof Swc4jAstTsTypeQuery n) {
			sb.append("typeof ");
			expression(n.getExprName());
			n.getTypeArgs().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstTsImportType n) {
			sb.append("import(");
			expression(n.getArg());
			n.getAttributes().ifPresent(a -> { sb.append(", "); expression(a.getWith()); });
			sb.append(')');
			n.getQualifier().ifPresent(q -> { sb.append('.'); expression(q); });
			n.getTypeArgs().ifPresent(this::expression);
		}
		else if(node instanceof Swc4jAstTsTypePredicate n) {
			if(n.isAsserts()) sb.append("asserts ");
			expression(n.getParamName());
			n.getTypeAnn().ifPresent(t -> { sb.append(" is "); type(t.getTypeAnn()); });
		}
		else if(node instanceof Swc4jAstTsTypeAnn n) {
			type(n.getTypeAnn());
		}
		else if(node instanceof Swc4jAstTsThisType || node instanceof Swc4jAstIdent || node instanceof Swc4jAstTsQualifiedName) {
			expression(node);
		}
		else {
			source(node);
		}
	}

	private static String plusMinus(Swc4jAstTruePlusMinus value) {
		return switch(value.name()) {
			case "Plus" -> "+";
			case "Minus" -> "-";
			default -> "";
		};
	}

	//the members of an interface (one per line) or of a type literal (in a single line)
	private void typeMembers(List<ISwc4jAstTsTypeElement> members, boolean interfaceBody) {
		if(members.isEmpty()) {
			sb.append("{}");
			return;
		}
		sb.append(interfaceBody ? "{\n" : "{ ");
		for(int i = 0; i < members.size(); i++) {
			typeMember(members.get(i));
			sb.append(interfaceBody ? ";\n" : (i < members.size() - 1 ? "; " : " }"));
		}
		if(interfaceBody) sb.append('}');
	}

	private void typeMember(ISwc4jAst member) {
		if(member instanceof Swc4jAstTsPropertySignature m) {
			if(m.isReadonly()) sb.append("readonly ");
			typeMemberKey(m.getKey(), m.isComputed());
			if(m.isOptional()) sb.append('?');
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsMethodSignature m) {
			typeMemberKey(m.getKey(), m.isComputed());
			if(m.isOptional()) sb.append('?');
			m.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(m.getParams(), ", ", this::expression);
			sb.append(')');
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsIndexSignature m) {
			if(m.isStatic()) sb.append("static ");
			if(m.isReadonly()) sb.append("readonly ");
			sb.append('[');
			list(m.getParams(), ", ", this::expression);
			sb.append(']');
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsCallSignatureDecl m) {
			m.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(m.getParams(), ", ", this::expression);
			sb.append(')');
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsConstructSignatureDecl m) {
			sb.append("new ");
			m.getTypeParams().ifPresent(this::typeParams);
			sb.append('(');
			list(m.getParams(), ", ", this::expression);
			sb.append(')');
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsGetterSignature m) {
			sb.append("get ");
			typeMemberKey(m.getKey(), m.isComputed());
			sb.append("()");
			m.getTypeAnn().ifPresent(this::expression);
		}
		else if(member instanceof Swc4jAstTsSetterSignature m) {
			sb.append("set ");
			typeMemberKey(m.getKey(), m.isComputed());
			sb.append('(');
			expression(m.getParam());
			sb.append(')');
		}
		else {
			source(member);
		}
	}

	private void typeMemberKey(ISwc4jAstExpr key, boolean computed) {
		if(computed) sb.append('[');
		expression(key);
		if(computed) sb.append(']');
	}

	private void typeParams(Swc4jAstTsTypeParamDecl decl) {
		sb.append('<');
		list(decl.getParams(), ", ", this::typeParam);
		sb.append('>');
	}

	private void typeParam(Swc4jAstTsTypeParam p) {
		if(p.isConst()) sb.append("const ");
		if(p.isIn()) sb.append("in ");
		if(p.isOut()) sb.append("out ");
		sb.append(p.getName().getSym());
		p.getConstraint().ifPresent(c -> { sb.append(" extends "); type(c); });
		p.getDefault().ifPresent(d -> { sb.append(" = "); type(d); });
	}

	// ---------------------------------------------------------------- helpers

	private <T> void list(List<T> items, String separator, Consumer<T> printer) {
		for(int i = 0; i < items.size(); i++) {
			if(i > 0) sb.append(separator);
			printer.accept(items.get(i));
		}
	}

	//every node type of swc4j 2.2.0 has a printer above (no fallback on the 1,212 TypeScript/JavaScript files of the oracle);
	//a node type added by a later version of swc4j keeps its source text, so that the analysis can proceed
	private void source(ISwc4jAst node) {
		sb.append(sourceText(node));
	}

	private String sourceText(ISwc4jAst node) {
		if(fileContent == null || node.getSpan() == null) {
			return "";
		}
		int start = Math.max(0, node.getSpan().getStart());
		int end = Math.min(fileContent.length(), node.getSpan().getEnd());
		return start < end ? fileContent.substring(start, end) : "";
	}
}
