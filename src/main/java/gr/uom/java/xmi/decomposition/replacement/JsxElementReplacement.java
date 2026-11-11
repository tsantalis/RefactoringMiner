package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.diff.JsxElementDiff;

//a JSX element replaced with its counterpart, whose differences are in the element diff, i.e., an attribute removed or changed
public class JsxElementReplacement extends Replacement {
	private final JsxElementDiff diff;

	public JsxElementReplacement(JsxElementDiff diff) {
		super(diff.getElement1().getString(), diff.getElement2().getString(), ReplacementType.JSX_ELEMENT);
		this.diff = diff;
	}

	public JsxElementDiff getDiff() {
		return diff;
	}
}
