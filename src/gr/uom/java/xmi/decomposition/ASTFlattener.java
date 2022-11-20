package gr.uom.java.xmi.decomposition;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;

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
}
