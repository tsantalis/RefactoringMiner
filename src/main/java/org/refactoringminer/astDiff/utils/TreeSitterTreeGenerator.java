package org.refactoringminer.astDiff.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TSTreeCursor;
import org.treesitter.TreeSitterCpp;
import org.yaml.snakeyaml.Yaml;

import com.github.gumtreediff.gen.treesitterng.AbstractTreeSitterNgGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Generates the trees of the gumtree treesitter-ng generators (AbstractTreeSitterNgGenerator) with the same rules.yml,
 * but in a single tree cursor traversal: the ancestor types are kept in a stack instead of calling ts_node_parent (which searches from the root) for each rule set of each node.
 * The offsets are the UTF-8 byte offsets of tree-sitter converted to UTF-16 offsets, i.e., the offsets of Java strings,
 * instead of the line starts in characters plus the UTF-8 byte columns, which shifted the nodes after non-ASCII characters, and the labels are the source code of the nodes.
 */
public class TreeSitterTreeGenerator {
	public enum Language {
		CPP("cpp", new TreeSitterCpp());

		private final String name;
		private final TSLanguage tsLanguage;

		Language(String name, TSLanguage tsLanguage) {
			this.name = name;
			this.tsLanguage = tsLanguage;
		}
	}

	private static final Map<String, Map<String, Object>> RULES =
			new Yaml().load(AbstractTreeSitterNgGenerator.class.getClassLoader().getResourceAsStream("rules.yml"));
	private static final Map<Language, Rules> LANGUAGE_RULES = new HashMap<>();

	public static TreeContext generate(Language language, String content) {
		TreeContext context = new TreeContext();
		if(content.isEmpty()) {
			Tree root = context.createTree(TypeSet.type("empty_root"));
			root.setPos(0);
			root.setLength(0);
			context.setRoot(root);
			return context;
		}
		TSParser parser = new TSParser();
		parser.setLanguage(language.tsLanguage);
		TSTree tsTree = parser.parseString(null, content);
		int[] utf16Offsets = utf16Offsets(content);
		Rules rules = rules(language);
		List<String> ancestorTypes = new ArrayList<>();
		Deque<Tree> ancestors = new ArrayDeque<>();
		TSTreeCursor cursor = new TSTreeCursor(tsTree.getRootNode());
		while(true) {
			TSNode node = cursor.currentNode();
			String nodeType = node.getType();
			boolean visitChildren = false;
			Tree tree = null;
			if(!rules.ignored.matches(nodeType, ancestorTypes)) {
				boolean ignoreLabel = rules.labelIgnored.matches(nodeType, ancestorTypes);
				String type = nodeType;
				String aliasedTypes = rules.aliased.match(nodeType, ancestorTypes);
				if(aliasedTypes != null) {
					type = rules.aliases.get(aliasedTypes);
				}
				boolean flatten = rules.flattened.matches(nodeType, ancestorTypes);
				int start = utf16Offset(utf16Offsets, node.getStartByte());
				int end = utf16Offset(utf16Offsets, node.getEndByte());
				int childCount = node.getChildCount();
				//attach label for non ignore-label leafs or flattened nodes
				if((childCount == 0 && !ignoreLabel) || flatten) {
					tree = context.createTree(TypeSet.type(type), content.substring(start, end));
				}
				else {
					tree = context.createTree(TypeSet.type(type));
				}
				tree.setPos(start);
				tree.setLength(end - start);
				if(ancestors.isEmpty()) {
					context.setRoot(tree);
				}
				else {
					ancestors.peek().addChild(tree);
				}
				//if node was flattened, ignore children
				visitChildren = !flatten && childCount > 0;
			}
			else if(ancestors.isEmpty()) {
				return context;
			}
			if(visitChildren && cursor.gotoFirstChild()) {
				ancestors.push(tree);
				ancestorTypes.add(nodeType);
				continue;
			}
			while(!cursor.gotoNextSibling()) {
				if(ancestors.isEmpty() || !cursor.gotoParent()) {
					return context;
				}
				ancestors.pop();
				ancestorTypes.remove(ancestorTypes.size() - 1);
				if(ancestors.isEmpty()) {
					return context;
				}
			}
		}
	}

	private static synchronized Rules rules(Language language) {
		return LANGUAGE_RULES.computeIfAbsent(language, l -> new Rules(RULES.getOrDefault(l.name, new HashMap<>())));
	}

	@SuppressWarnings("unchecked")
	private static class Rules {
		private final RuleSet ignored;
		private final RuleSet labelIgnored;
		private final RuleSet flattened;
		private final RuleSet aliased;
		private final Map<String, String> aliases;

		private Rules(Map<String, Object> rule) {
			this.ignored = new RuleSet((List<String>) rule.get("ignored"));
			this.labelIgnored = new RuleSet((List<String>) rule.get("label_ignored"));
			this.flattened = new RuleSet((List<String>) rule.get("flattened"));
			this.aliases = (Map<String, String>) rule.getOrDefault("aliased", Collections.emptyMap());
			this.aliased = new RuleSet(aliases.keySet());
		}
	}

	//the rules are node types or space-separated ancestor types ending with the node type, i.e., "call_expression identifier"
	private static class RuleSet {
		private final Set<String> rules;
		private final Set<String> nodeTypeRules = new HashSet<>();
		private int maxDepth = 1;

		private RuleSet(Collection<String> rules) {
			this.rules = rules == null ? Collections.emptySet() : new HashSet<>(rules);
			for(String rule : this.rules) {
				int depth = rule.split(" ").length;
				maxDepth = Math.max(maxDepth, depth);
				if(depth == 1) {
					nodeTypeRules.add(rule);
				}
			}
		}

		private boolean matches(String nodeType, List<String> ancestorTypes) {
			return match(nodeType, ancestorTypes) != null;
		}

		//same as AbstractTreeSitterNgGenerator.matchNodeOrAncestorTypes, the last ancestor type is the parent type
		private String match(String nodeType, List<String> ancestorTypes) {
			if(rules.isEmpty()) {
				return null;
			}
			if(nodeTypeRules.contains(nodeType)) {
				return nodeType;
			}
			StringBuilder types = new StringBuilder(nodeType);
			for(int depth = 1, i = ancestorTypes.size() - 1; i >= 0 && depth <= maxDepth; depth++, i--) {
				types.insert(0, ' ');
				types.insert(0, ancestorTypes.get(i));
				String typesString = types.toString();
				if(rules.contains(typesString)) {
					return typesString;
				}
			}
			return null;
		}
	}

	//the UTF-16 offset of each UTF-8 byte offset of tree-sitter, or null for ASCII content, whose offsets are the same
	private static int[] utf16Offsets(String content) {
		boolean ascii = true;
		for(int i = 0; i < content.length() && ascii; i++) {
			ascii = content.charAt(i) < 0x80;
		}
		if(ascii) {
			return null;
		}
		int[] offsets = new int[content.getBytes(StandardCharsets.UTF_8).length + 1];
		int bytes = 0;
		for(int i = 0; i < content.length(); ) {
			int codePoint = content.codePointAt(i);
			int length = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
			for(int k = 0; k < length; k++) {
				offsets[bytes + k] = i;
			}
			bytes += length;
			i += Character.charCount(codePoint);
		}
		offsets[bytes] = content.length();
		return offsets;
	}

	private static int utf16Offset(int[] utf16Offsets, int byteOffset) {
		return utf16Offsets == null ? byteOffset : utf16Offsets[Math.min(byteOffset, utf16Offsets.length - 1)];
	}
}
