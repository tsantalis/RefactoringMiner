package gr.uom.java.xmi.decomposition;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TagProperty;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

public class ASTFlattener extends NaiveASTFlattener {
	@Override
	public boolean visit(InfixExpression node) {
		node.getLeftOperand().accept(this);
		this.buffer.append(' ');  // for cases like x= i - -1; or x= i++ + ++i;
		this.buffer.append(node.getOperator().toString());
		this.buffer.append(' ');
		node.getRightOperand().accept(this);
		final List extendedOperands = node.extendedOperands();
		if (extendedOperands.size() != 0) {
			for (Iterator it = extendedOperands.iterator(); it.hasNext(); ) {
				this.buffer.append(' ');
				this.buffer.append(node.getOperator().toString()).append(' ');
				Expression e = (Expression) it.next();
				e.accept(this);
			}
		}
		return false;
	}

	@Override
	public boolean visit(TagElement node) {
		if (node.isNested()) {
			// nested tags are always enclosed in braces
			this.buffer.append("{");//$NON-NLS-1$
		} else {
			// top-level tags always begin on a new line
			this.buffer.append("\n * ");//$NON-NLS-1$
		}
		boolean previousRequiresWhiteSpace = false;
		if (node.getTagName() != null) {
			this.buffer.append(node.getTagName());
			previousRequiresWhiteSpace = true;
		}
		boolean previousRequiresNewLine = false;
		for (Object element : node.fragments()) {
			ASTNode e = (ASTNode) element;
			// Name, MemberRef, MethodRef, and nested TagElement do not include white space.
			// TextElements don't always include whitespace, see <https://bugs.eclipse.org/206518>.
			if (previousRequiresNewLine) {
				this.buffer.append("\n * ");//$NON-NLS-1$
			}
			previousRequiresNewLine = true;
			// add space if required to separate
			if (previousRequiresWhiteSpace) {
				this.buffer.append(" "); //$NON-NLS-1$
			}
			e.accept(this);
		}
		if (DOMASTUtil.isJavaDocCodeSnippetSupported(node.getAST().apiLevel())) {
			for (Object element : node.tagProperties()) {
				TagProperty tagProperty = (TagProperty) element;
				tagProperty.accept(this);
			}

		}
		if (node.isNested()) {
			this.buffer.append("}");//$NON-NLS-1$
		}
		return false;
	}
}
