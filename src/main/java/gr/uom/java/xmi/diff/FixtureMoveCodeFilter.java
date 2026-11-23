package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;

/**
 * Validates the Move Code refactorings whose target is a test fixture (Extract Fixture, and Move Code to an existing
 * set-up or tear-down method) against the code before and after the commit. The move code mappers pair the unmatched
 * statements of two operations by similarity; a pair is evidence of a move only when
 * <ul>
 * <li>the statement left the source: the source operation after the commit has fewer copies of it (else it was copied),</li>
 * <li>the statement is new in the target: the target fixture before the commit had fewer copies of it,</li>
 * <li>it is not a counterpart: the two statements do not differ only by literals (true/false, another file name), nor by
 * an operation and its opposite (open/close, start/stop),</li>
 * <li>it is not an edit in place: neither the source kept an edited version of it, nor the target lost a version of it
 * that is closer than the source statement (the same edit made in sibling classes is not a move between them).</li>
 * </ul>
 * A refactoring without a valid pair is dropped. Code moved out of a fixture into a new fixture is a Split Fixture (or a
 * fixture renamed or migrated), not an Extract Fixture, and is dropped as well.
 */
public class FixtureMoveCodeFilter {
	private static final Pattern TOKEN = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|\\d[\\w.]*|\\w+|\\S");
	private static final Pattern LITERAL = Pattern.compile("^(\"|'|\\d|true$|false$|null$)");
	private static final Pattern WORD = Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?![a-z])");
	private static final String[][] OPPOSITES = {{"open", "close"}, {"start", "stop"}, {"create", "delete"}, {"create", "drop"},
			{"create", "destroy"}, {"create", "revoke"}, {"add", "remove"}, {"add", "delete"}, {"insert", "delete"},
			{"connect", "disconnect"}, {"begin", "end"}, {"lock", "unlock"}, {"register", "unregister"}, {"enable", "disable"},
			{"true", "false"}, {"push", "pop"}, {"acquire", "release"}, {"init", "destroy"}, {"setup", "teardown"},
			{"start", "shutdown"}, {"subscribe", "unsubscribe"}, {"mount", "unmount"}, {"load", "unload"}, {"set", "reset"},
			{"before", "after"}, {"up", "down"}};
	private final UMLModel parentModel;
	private final UMLModel childModel;
	private final Map<String, UMLClass> parentClasses = new HashMap<>();
	private final Map<String, UMLClass> childClasses = new HashMap<>();
	//classes and operations renamed or moved by the commit, both ways, to find an operation on the other side
	private final Map<String, String> nextClassName = new HashMap<>();
	private final Map<String, String> previousClassName = new HashMap<>();
	private final Map<VariableDeclarationContainer, VariableDeclarationContainer> nextOperation = new HashMap<>();
	private final Map<VariableDeclarationContainer, VariableDeclarationContainer> previousOperation = new HashMap<>();

	public FixtureMoveCodeFilter(UMLModel parentModel, UMLModel childModel) {
		this.parentModel = parentModel;
		this.childModel = childModel;
		if(parentModel != null)
			for(UMLClass c : parentModel.getClassList())
				parentClasses.put(c.getName(), c);
		if(childModel != null)
			for(UMLClass c : childModel.getClassList())
				childClasses.put(c.getName(), c);
	}

	public static boolean targetsFixture(Refactoring r) {
		if(r instanceof MoveCodeRefactoring move) {
			VariableDeclarationContainer target = move.getTargetContainer();
			return target.isSetUpMethod() || target.isTearDownMethod();
		}
		return false;
	}

	private void indexRenamesAndMoves(Set<Refactoring> refactorings) {
		for(Refactoring r : refactorings) {
			String before = null, after = null;
			if(r instanceof RenameClassRefactoring rename) {
				before = rename.getOriginalClassName(); after = rename.getRenamedClassName();
			}
			else if(r instanceof MoveClassRefactoring move) {
				before = move.getOriginalClassName(); after = move.getMovedClassName();
			}
			else if(r instanceof MoveAndRenameClassRefactoring move) {
				before = move.getOriginalClassName(); after = move.getRenamedClassName();
			}
			else if(r instanceof MoveOperationRefactoring move) {
				nextOperation.put(move.getOriginalOperation(), move.getMovedOperation());
				previousOperation.put(move.getMovedOperation(), move.getOriginalOperation());
			}
			else if(r instanceof RenameOperationRefactoring rename) {
				nextOperation.put(rename.getOriginalOperation(), rename.getRenamedOperation());
				previousOperation.put(rename.getRenamedOperation(), rename.getOriginalOperation());
			}
			if(before != null && after != null) {
				nextClassName.put(before, after);
				previousClassName.put(after, before);
			}
		}
	}

	public Set<Refactoring> filter(Set<Refactoring> refactorings) {
		indexRenamesAndMoves(refactorings);
		Set<Refactoring> kept = new LinkedHashSet<>();
		for(Refactoring r : refactorings) {
			if(!targetsFixture(r)) {
				kept.add(r);
				continue;
			}
			MoveCodeRefactoring move = (MoveCodeRefactoring)r;
			if(!valid(move)) {
				continue;
			}
			//whether the fixture is new is decided by the code before the commit, not by the operation it was matched with:
			//code moved into a new fixture is an Extract Fixture, into a fixture that existed a Move Code
			boolean targetExisted = existedAsFixture(move.getTargetContainer());
			boolean extract = move.getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE);
			if(targetExisted && extract) {
				move = retype(move, MoveCodeRefactoring.Type.MOVE_BETWEEN_EXISTING);
			}
			else if(!targetExisted && !extract) {
				move = retype(move, MoveCodeRefactoring.Type.MOVE_TO_ADDED);
			}
			kept.add(move);
		}
		return kept;
	}

	private static MoveCodeRefactoring retype(MoveCodeRefactoring move, MoveCodeRefactoring.Type type) {
		MoveCodeRefactoring retyped = new MoveCodeRefactoring(move.getSourceContainer(), move.getTargetContainer(), move.getBodyMapper(), type);
		if(move.getLambdaMapper().isPresent()) {
			retyped.setLambdaMapper(move.getLambdaMapper().get());
		}
		return retyped;
	}

	private boolean valid(MoveCodeRefactoring move) {
		VariableDeclarationContainer source = move.getSourceContainer();
		VariableDeclarationContainer target = move.getTargetContainer();
		if(source instanceof UMLAttribute) {
			return true; //a field initializer that became an assignment in the fixture, paired by the field's name
		}
		boolean sourceIsFixture = source.isSetUpMethod() || source.isTearDownMethod();
		if(sourceIsFixture && !existedAsFixture(target)) {
			return false; //code of a fixture in a new fixture: Split Fixture, or a fixture renamed, migrated or re-scoped
		}
		List<String> sourceBefore = statements(source);
		VariableDeclarationContainer sourceNext = counterpart(source, childClasses);
		VariableDeclarationContainer targetPrevious = counterpart(target, parentClasses);
		//a method re-annotated as a fixture: its statements stay, in the fixture
		boolean reannotated = sourceNext != null && sourceNext.getClassName().equals(target.getClassName()) && sourceNext.getName().equals(target.getName());
		List<String> sourceAfter = reannotated ? null : statements(sourceNext);
		List<String> targetAfter = statements(target);
		List<String> targetBefore = reannotated ? null : statements(targetPrevious);
		List<String> sourceAdded = difference(sourceAfter, sourceBefore);
		List<String> targetRemoved = difference(targetBefore, targetAfter);
		for(AbstractCodeMapping mapping : move.getMappings()) {
			if(!evidence(mapping.getFragment1()) || !evidence(mapping.getFragment2())) {
				continue;
			}
			String s1 = normalize(mapping.getFragment1().getString());
			String s2 = normalize(mapping.getFragment2().getString());
			if(sourceAfter != null && count(sourceAfter, s1) >= count(sourceBefore, s1)) {
				debug(move, s1, s2, "still in the source");
				continue; //still in the source: copied
			}
			if(targetBefore != null && count(targetAfter, s2) <= count(targetBefore, s2)) {
				debug(move, s1, s2, "already in the target");
				continue; //already in the target
			}
			if(!s1.equals(s2) && counterpart(s1, s2)) {
				debug(move, s1, s2, "counterpart");
				continue;
			}
			double similarity = similarity(s1, s2);
			if(!s1.equals(s2) && similarity < 0.75) {
				debug(move, s1, s2, "similarity " + similarity);
				continue; //too different to be the same statement, edited on the way
			}
			if(!s1.equals(s2) && (closer(sourceAdded, s1, similarity) || closer(targetRemoved, s2, similarity))) {
				debug(move, s1, s2, "edited in place");
				continue; //edited in place, in the source or in the target
			}
			return true;
		}
		return false;
	}

	private static final boolean DEBUG = System.getProperty("fixtureFilterDebug") != null;

	private static void debug(MoveCodeRefactoring move, String s1, String s2, String reason) {
		if(DEBUG)
			System.err.println("[fixture filter] " + move.getSourceContainer().getName() + " -> " + move.getTargetContainer().getName() + ": " + reason + ": " + s1 + " -> " + s2);
	}

	private boolean existedAsFixture(VariableDeclarationContainer target) {
		VariableDeclarationContainer previous = counterpart(target, parentClasses);
		return previous != null && (previous.isSetUpMethod() || previous.isTearDownMethod());
	}

	//a statement, not a block or a bare expression
	private static boolean evidence(AbstractCodeFragment fragment) {
		String s = fragment.getString().strip();
		if(fragment instanceof CompositeStatementObject composite) {
			return composite.getExpressions().size() > 0 && s.length() > 3;
		}
		return s.length() > 1;
	}

	//the operation on the other side of the commit: renamed or moved (as RefactoringMiner found), or of the same (possibly
	//renamed or moved) class with the same name and parameter types
	private VariableDeclarationContainer counterpart(VariableDeclarationContainer container, Map<String, UMLClass> classes) {
		boolean toNext = classes == childClasses;
		VariableDeclarationContainer moved = (toNext ? nextOperation : previousOperation).get(container);
		if(moved != null) {
			return moved;
		}
		String className = container.getClassName();
		className = (toNext ? nextClassName : previousClassName).getOrDefault(className, className);
		UMLClass umlClass = classes.get(className);
		if(umlClass == null) {
			return null;
		}
		VariableDeclarationContainer sameName = null;
		for(UMLOperation operation : umlClass.getOperations()) {
			if(operation.getName().equals(container.getName())) {
				if(operation.getParameterTypeList().equals(container.getParameterTypeList())) {
					return operation;
				}
				sameName = operation;
			}
		}
		return sameName;
	}

	private static List<String> statements(VariableDeclarationContainer container) {
		if(container == null) {
			return null;
		}
		OperationBody body = container.getBody();
		List<String> list = new ArrayList<>();
		if(body != null) {
			for(AbstractCodeFragment leaf : body.getCompositeStatement().getLeaves())
				list.add(normalize(leaf.getString()));
			for(CompositeStatementObject node : body.getCompositeStatement().getInnerNodes())
				list.add(normalize(node.getString()));
		}
		return list;
	}

	private static String normalize(String s) {
		s = s.strip();
		if(s.endsWith(";"))
			s = s.substring(0, s.length() - 1);
		return s.replace("this.", "").replaceAll("\\s+", "");
	}

	private static int count(List<String> list, String s) {
		int n = 0;
		for(String x : list)
			if(x.equals(s))
				n++;
		return n;
	}

	private static List<String> difference(List<String> a, List<String> b) {
		List<String> result = new ArrayList<>();
		if(a == null)
			return result;
		List<String> rest = b == null ? new ArrayList<>() : new ArrayList<>(b);
		for(String x : a)
			if(!rest.remove(x))
				result.add(x);
		return result;
	}

	//2 * (longest common subsequence) / (total length): the share of the two statements they have in common, in order
	private static double similarity(String a, String b) {
		if(a.isEmpty() && b.isEmpty())
			return 1.0;
		int[] previous = new int[b.length() + 1], current = new int[b.length() + 1];
		for(int i = 1; i <= a.length(); i++) {
			for(int j = 1; j <= b.length(); j++) {
				current[j] = a.charAt(i - 1) == b.charAt(j - 1) ? previous[j - 1] + 1 : Math.max(previous[j], current[j - 1]);
			}
			int[] swap = previous; previous = current; current = swap;
		}
		return 2.0 * previous[b.length()] / (a.length() + b.length());
	}

	//a statement of the list at least as close to s as the paired statement, and similar enough to be its edited version
	private static boolean closer(List<String> list, String s, double pairSimilarity) {
		for(String x : list) {
			double sim = similarity(x, s);
			if(sim >= 0.6 && sim >= pairSimilarity)
				return true;
		}
		return false;
	}

	private static List<String> tokens(String s) {
		List<String> list = new ArrayList<>();
		Matcher m = TOKEN.matcher(s);
		while(m.find())
			list.add(m.group());
		return list;
	}

	private static Set<String> words(String token) {
		Set<String> set = new LinkedHashSet<>();
		Matcher m = WORD.matcher(token);
		while(m.find())
			set.add(m.group().toLowerCase());
		set.add(token.toLowerCase());
		return set;
	}

	//the tokens that differ are only literals, or one of them is the opposite of the other
	static boolean counterpart(String a, String b) {
		List<String> ta = tokens(a), tb = tokens(b);
		int prefix = 0;
		while(prefix < ta.size() && prefix < tb.size() && ta.get(prefix).equals(tb.get(prefix)))
			prefix++;
		int suffix = 0;
		while(suffix < ta.size() - prefix && suffix < tb.size() - prefix &&
				ta.get(ta.size() - 1 - suffix).equals(tb.get(tb.size() - 1 - suffix)))
			suffix++;
		List<String> da = ta.subList(prefix, ta.size() - suffix), db = tb.subList(prefix, tb.size() - suffix);
		if(da.isEmpty() || db.isEmpty())
			return false;
		boolean literals = true;
		for(String t : da)
			literals &= LITERAL.matcher(t).find();
		for(String t : db)
			literals &= LITERAL.matcher(t).find();
		if(literals)
			return true;
		for(String x : da)
			for(String y : db)
				for(String u : words(x))
					for(String v : words(y))
						for(String[] pair : OPPOSITES)
							if((u.equals(pair[0]) && v.equals(pair[1])) || (u.equals(pair[1]) && v.equals(pair[0])))
								return true;
		return false;
	}
}
