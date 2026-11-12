package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxElement;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxEmptyExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxExprContainer;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxFragment;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxSpreadChild;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstMemberExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstSpreadElement;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdentName;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstJsxText;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstJsxAttrOrSpread;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstJsxElementChild;
import com.caoccao.javet.swc4j.ast.miscs.Swc4jAstJsxAttr;
import com.caoccao.javet.swc4j.ast.miscs.Swc4jAstJsxOpeningElement;


import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.StringDistance;

/**
 * A JSX element, i.e., {@code <Draggable axis="x" onStop={handleStop}><div className="handle" /></Draggable>},
 * which is a call creating a component: its tag is the name of the call, its attributes are named arguments
 * (in any order), and its children are nested elements, expressions in braces, or text.
 * A fragment {@code <>...</>} is an element without a tag.
 */
public class JsxElement extends AbstractCall {
	private String tag;
	private List<JsxAttribute> attributes = new ArrayList<>();
	private List<JsxChild> children = new ArrayList<>();
	//the functions passed by their name as attribute values, i.e., handleStop in onStop={handleStop}
	private List<MethodReference> functionReferences = new ArrayList<>();
	private volatile int hashCode = 0;

	//an attribute name=value, a boolean attribute without value (value == null), or a spread attribute {...props} (spread == true, name == the spread expression),
	//compared without its location
	public record JsxAttribute(String name, String value, boolean spread, LocationInfo location) {
		public boolean equals(Object o) {
			return o instanceof JsxAttribute a && name.equals(a.name) && java.util.Objects.equals(value, a.value) && spread == a.spread;
		}
		public int hashCode() {
			return java.util.Objects.hash(name, value, spread);
		}
		public String toString() {
			if(spread)
				return "{..." + name + "}";
			return value == null ? name : name + "=" + value;
		}
	}

	//a nested element (element != null), an expression in braces {x}, a spread child {...x}, or text, printed as in the source, compared without its location
	public record JsxChild(String string, JsxElement element, LocationInfo location) {
		public boolean isElement() {
			return element != null;
		}
		public boolean equals(Object o) {
			return o instanceof JsxChild c && string.equals(c.string) && java.util.Objects.equals(element, c.element);
		}
		public int hashCode() {
			return java.util.Objects.hash(string, element);
		}
		public String toString() {
			return string;
		}
	}

	public JsxElement(String sourceFolder, String filePath, Swc4jAstJsxElement element, VariableDeclarationContainer container, String fileContent) {
		super(sourceFolder, filePath, element, CodeElementType.JSX_ELEMENT, container, fileContent);
		Swc4jAstJsxOpeningElement opening = element.getOpening();
		this.tag = TypeScriptVisitor.text(fileContent, opening.getName());
		for(ISwc4jAstJsxAttrOrSpread attribute : opening.getAttrs()) {
			if(attribute instanceof Swc4jAstSpreadElement spread) {
				this.attributes.add(new JsxAttribute(TypeScriptVisitor.text(fileContent, spread.getExpr()), null, true, new LocationInfo(sourceFolder, filePath, spread, CodeElementType.JSX_ELEMENT, fileContent)));
			}
			else if(attribute instanceof Swc4jAstJsxAttr attr) {
				String value = attr.getValue().isPresent() ? TypeScriptVisitor.text(fileContent, attr.getValue().get()) : null;
				this.attributes.add(new JsxAttribute(TypeScriptVisitor.text(fileContent, attr.getName()), value, false, new LocationInfo(sourceFolder, filePath, attr, CodeElementType.JSX_ELEMENT, fileContent)));
				if(attr.getValue().isPresent() && attr.getValue().get() instanceof Swc4jAstJsxExprContainer valueContainer) {
					if(valueContainer.getExpr() instanceof Swc4jAstIdent ident) {
						this.functionReferences.add(new MethodReference(sourceFolder, filePath, ident, ident.getSym(), null, container, fileContent));
					}
					else if(valueContainer.getExpr() instanceof Swc4jAstMemberExpr member && member.getProp() instanceof Swc4jAstIdentName prop) {
						this.functionReferences.add(new MethodReference(sourceFolder, filePath, member, prop.getSym(), TypeScriptVisitor.text(fileContent, member.getObj()), container, fileContent));
					}
				}
			}
		}
		addChildren(sourceFolder, filePath, element.getChildren(), container, fileContent);
		initializeArguments();
	}

	public JsxElement(String sourceFolder, String filePath, Swc4jAstJsxFragment fragment, VariableDeclarationContainer container, String fileContent) {
		super(sourceFolder, filePath, fragment, CodeElementType.JSX_ELEMENT, container, fileContent);
		this.tag = "";
		addChildren(sourceFolder, filePath, fragment.getChildren(), container, fileContent);
		initializeArguments();
	}

	private JsxElement(LocationInfo locationInfo) {
		super(locationInfo);
	}

	private void addChildren(String sourceFolder, String filePath, List<ISwc4jAstJsxElementChild> children, VariableDeclarationContainer container, String fileContent) {
		for(ISwc4jAstJsxElementChild child : children) {
			if(child instanceof Swc4jAstJsxText text) {
				//the whitespace between the elements is formatting, and the text is compared without its indentation
				String value = text.getValue().strip().replaceAll("\\s+", " ");
				if(!value.isEmpty())
					this.children.add(new JsxChild(value, null, new LocationInfo(sourceFolder, filePath, text, CodeElementType.JSX_ELEMENT, fileContent)));
			}
			else if(child instanceof Swc4jAstJsxExprContainer container1 && container1.getExpr() instanceof Swc4jAstJsxEmptyExpr) {
				//an empty expression, i.e., a comment {/* ... */}
			}
			else if(child instanceof Swc4jAstJsxElement element) {
				JsxElement nested = new JsxElement(sourceFolder, filePath, element, container, fileContent);
				this.children.add(new JsxChild(nested.getString(), nested, nested.getLocationInfo()));
			}
			else if(child instanceof Swc4jAstJsxFragment fragment) {
				JsxElement nested = new JsxElement(sourceFolder, filePath, fragment, container, fileContent);
				this.children.add(new JsxChild(nested.getString(), nested, nested.getLocationInfo()));
			}
			else if(child instanceof Swc4jAstJsxSpreadChild || child instanceof Swc4jAstJsxExprContainer) {
				this.children.add(new JsxChild(TypeScriptVisitor.text(fileContent, (ISwc4jAst)child), null, new LocationInfo(sourceFolder, filePath, (ISwc4jAst)child, CodeElementType.JSX_ELEMENT, fileContent)));
			}
		}
	}

	//the attributes are the arguments of the call, like the props object of React.createElement(tag, props, ...children)
	private void initializeArguments() {
		this.arguments = new ArrayList<>();
		for(JsxAttribute attribute : attributes) {
			this.arguments.add(attribute.toString());
		}
		this.numberOfArguments = this.arguments.size();
	}

	public String getTag() {
		return tag;
	}

	public boolean isFragment() {
		return tag.isEmpty();
	}

	public List<JsxAttribute> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}

	public List<MethodReference> getFunctionReferences() {
		return Collections.unmodifiableList(functionReferences);
	}

	public List<JsxChild> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public String getName() {
		return tag;
	}

	@Override
	public boolean identicalName(AbstractCall call) {
		return call instanceof JsxElement element && tag.equals(element.tag);
	}

	@Override
	public double normalizedNameDistance(AbstractCall call) {
		String s1 = tag.toLowerCase();
		String s2 = call.getName().toLowerCase();
		int max = Math.max(s1.length(), s2.length());
		if(max == 0)
			return 0;
		return (double)StringDistance.editDistance(s1, s2)/(double)max;
	}

	@Override
	public JsxElement update(String oldExpression, String newExpression) {
		JsxElement newElement = new JsxElement(this.locationInfo);
		newElement.tag = this.tag;
		newElement.attributes = this.attributes;
		newElement.children = this.children;
		newElement.functionReferences = this.functionReferences;
		update(newElement, oldExpression, newExpression);
		return newElement;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof JsxElement element) {
			return tag.equals(element.tag) && attributes.equals(element.attributes) && children.equals(element.children);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + tag.hashCode();
			result = 37*result + attributes.hashCode();
			result = 37*result + children.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return getString();
	}
}
