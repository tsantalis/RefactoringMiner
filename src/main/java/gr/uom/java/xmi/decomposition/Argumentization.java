package gr.uom.java.xmi.decomposition;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The code fragments of a mapper after replacing parameters with arguments.
 * Each mapper owns its argumentization, so mappers never overwrite each other's argumentized strings.
 * A child mapper (e.g., for an extracted method or a lambda) falls back to the argumentization of its parent mapper.
 */
public class Argumentization {
	private final Argumentization parent;
	private final Map<AbstractCodeFragment, String> argumentizedStrings = new IdentityHashMap<AbstractCodeFragment, String>();

	public Argumentization(Argumentization parent) {
		this.parent = parent;
	}

	public String get(AbstractCodeFragment fragment) {
		String argumentizedString = argumentizedStrings.get(fragment);
		if(argumentizedString != null) {
			return argumentizedString;
		}
		return parent != null ? parent.get(fragment) : fragment.getString();
	}

	public void argumentize(AbstractCodeFragment fragment, Map<String, String> parameterToArgumentMap) {
		argumentizedStrings.put(fragment, parameterToArgumentMap.isEmpty() ? fragment.getString() : fragment.replaceParametersWithArguments(parameterToArgumentMap));
	}

	public void argumentize(List<? extends AbstractCodeFragment> nodes, Map<String, String> parameterToArgumentMap) {
		for(AbstractCodeFragment node : nodes) {
			argumentize(node, parameterToArgumentMap);
			if(node instanceof CompositeStatementObject composite) {
				for(AbstractExpression expression : composite.getExpressions()) {
					argumentizedStrings.put(expression, expression.getString());
				}
			}
		}
	}
}
