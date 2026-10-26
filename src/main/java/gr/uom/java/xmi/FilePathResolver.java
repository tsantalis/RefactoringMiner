package gr.uom.java.xmi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.util.PathFileUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gr.uom.java.xmi.decomposition.LeafExpression;

/**
 * Best-effort resolution of the repository files that a file uses code from, so that their models can be attached
 * to the model of the changed files. Only direct clues that map to specific files are followed (import and include paths,
 * qualified names, alias configuration); cases whose clues do not lead to specific files are left unresolved.
 * The import specifier of {@link #resolvePaths(String, String)} is read per language of the importing file:
 * <ul>
 * <li>Java/Kotlin: a qualified name, e.g. {@code a.b.C}, {@code a.b.C.member}, {@code a.b.C.*}</li>
 * <li>Python: a dotted module path, optionally relative, e.g. {@code a.b.c}, {@code ..a.b}, {@code .}</li>
 * <li>TypeScript/JavaScript: the module source, e.g. {@code ./foo}, {@code @/components/X}, {@code @scope/pkg}</li>
 * <li>C/C++: the include path, e.g. {@code foo/bar.h}, {@code "foo.h"}, {@code <foo.h>}</li>
 * </ul>
 */
public class FilePathResolver {
	// TODO RefactoringMiner does not carry enough information for these cases:
	//  - Kotlin: import aliases ("import a.b.C as D") are not recorded in UMLImport, so names used through an alias are not resolved.
	//  - Java/Kotlin: inferred types ("var x = f()", "val x = f()") are recorded as "var"/InferredType, so the used type is only
	//    reachable by resolving the return type of the called declaration (resolveType with the declaring class).
	//  - Python: the relative level of from-imports (LangImportStatement.getRelativeLevel()) is not recorded,
	//    so relative imports are only resolved within the importing file's own package.
	//  - Python: "import a.b" and "from a import b" are both recorded as "a.b", so names are matched against both bindings.
	//  - Python: importlib.import_module(...) / __import__(...) do not create a UMLImport.
	//  - TypeScript: named imports record the local identifier, not the imported one ("import { a as b }" records b),
	//    so "export { a } from" re-exports are missed.
	//  - TypeScript: side-effect imports ("import './x'"), "import x = require('./x')" (only the identifier is recorded),
	//    require(...), and dynamic import(...) do not provide a module source.
	// TODO missing resolution cases with direct clues:
	//  - Python: named re-exports in regular modules (only __init__ files are followed).
	//  - TypeScript: tsconfig "extends" of a package or an array, project "references", and bundler aliases
	//    (Vite/Webpack resolve.alias, Babel module-resolver).
	//  - TypeScript: two-step re-exports ("import { X } from './x'; export { X };") and module.exports = require(...).
	//  - C/C++: include directories from the build configuration (CMake, compile_commands.json).
	// Not resolved by design, as the clues do not lead to specific files or need many files to be parsed to confirm them:
	//  - imports of whole packages or namespaces: Java/Kotlin "a.b.*" (names are resolved through them by resolveName),
	//    Java module imports, Python namespace packages, C/C++ using-directives/declarations and namespace aliases;
	//  - declarations not named after their file: Kotlin top-level functions/properties, C/C++ forward declarations;
	//  - names not reachable through imports or the package: inherited members, TypeScript global scripts,
	//    "declare global" and ambient modules, window/globalThis properties;
	//  - re-export chains through "export * from" and Python "from x import *";
	//  - C/C++ transitively included headers, and definitions outside the header's implementation file.
	private static final String[] TS_EXTENSIONS = {".ts", ".tsx", ".d.ts", ".js", ".jsx", ".mjs", ".cjs", ".mts", ".cts"};
	private static final Map<String, String[]> TS_EMITTED_EXTENSIONS = Map.of(
			".js", new String[] {".ts", ".tsx"},
			".jsx", new String[] {".tsx"},
			".mjs", new String[] {".mts"},
			".cjs", new String[] {".cts"});
	private static final String[] PACKAGE_CONDITIONS = {"types", "import", "module", "default", "require", "node"};
	private static final String[] CPP_HEADER_EXTENSIONS = {".h", ".hpp", ".hh", ".hxx"};
	private static final String[] CPP_SOURCE_EXTENSIONS = {".cpp", ".cc", ".cxx", ".c"};
	private static final Set<String> BUILT_IN_TYPES = Set.of(
			"void", "boolean", "byte", "char", "short", "int", "long", "float", "double", "var", "Inferred",
			"string", "number", "bigint", "symbol", "any", "unknown", "never", "object", "undefined", "null", "this",
			"bool", "signed", "unsigned", "auto", "None", "str", "bytes");
	private static final Pattern TS_RE_EXPORT = Pattern.compile("\\bexport\\s+(?:type\\s+)?(?:\\*\\s+as\\s+([\\w$]+)|\\{([^}]*)\\})\\s*from\\s*['\"]([^'\"]+)['\"]");
	private static final Pattern TS_REFERENCE = Pattern.compile("<reference\\s+(path|types)\\s*=\\s*['\"]([^'\"]+)['\"]");
	private static final Pattern PYTHON_FROM_IMPORT = Pattern.compile("(?m)^[ \\t]*from[ \\t]+(\\.*[\\w.]*)[ \\t]+import[ \\t]+(\\([^)]*\\)|[^\\n#]*)");
	private static final Pattern CPP_INCLUDE = Pattern.compile("#\\s*include\\s*([<\"][^>\"]+[>\"])");

	private final RepositoryStateFiles files;
	private final Map<String, Optional<JsonObject>> jsonCache = new HashMap<>();
	private final Map<String, List<UMLClass>> classesByFile = new HashMap<>();
	private final Map<String, List<UMLAbstractClass>> supertypesByClass = new HashMap<>();

	private record TsConfig(String baseUrl, String pathsBase, JsonObject paths) {}

	public FilePathResolver(RepositoryStateFiles files) {
		this.files = files;
	}

	public Set<String> resolvePaths(UMLImport umlImport) throws Exception {
		String currentFilePath = umlImport.getLocationInfo().getFilePath();
		String name = umlImport.getName();
		Set<String> paths = new LinkedHashSet<>();
		switch (umlImport.getLANG()) {
			case JAVA:
			case KOTLIN:
				paths.addAll(resolvePaths(currentFilePath, umlImport.isOnDemand() ? name + ".*" : name));
				break;
			case PYTHON:
				// "from . import x" is recorded as "null.x" and "from . import *" as null
				if (name == null)
					paths.addAll(resolvePaths(currentFilePath, "."));
				else
					paths.addAll(resolvePaths(currentFilePath, name.startsWith("null.") ? name.substring("null".length()) : name));
				break;
			case TYPESCRIPT:
				// recorded as "<module source>.<local identifier>"; named imports are recorded as static
				int dot = name.lastIndexOf('.');
				if (dot >= 0)
					paths.addAll(resolveTypeScript(currentFilePath, name.substring(0, dot), umlImport.isStatic() ? name.substring(dot + 1) : null, new HashSet<>()));
				break;
			case C:
			case CPP:
				// recorded imports are using-directives/declarations and namespace aliases, which name namespaces rather than files
				break;
		}
		return paths;
	}

	/**
	 * TypeScript "export { a, b as c } from '...'".
	 */
	public Set<String> resolvePaths(UMLNamedExport namedExport) throws Exception {
		Set<String> paths = new LinkedHashSet<>();
		if (namedExport.getSource() == null)
			return paths;
		String currentFilePath = namedExport.getLocationInfo().getFilePath();
		String source = unquote(namedExport.getSource().getString());
		if (namedExport.getSpecifiers().isEmpty())
			paths.addAll(resolveTypeScript(currentFilePath, source, null, new HashSet<>()));
		for (LeafExpression specifier : namedExport.getSpecifiers())
			paths.addAll(resolveTypeScript(currentFilePath, source, unquote(specifier.getString()), new HashSet<>()));
		return paths;
	}

	/**
	 * C/C++ #include: the included header and its implementation file.
	 */
	public Set<String> resolvePaths(UMLPreprocessorStatement statement) throws Exception {
		Optional<String> include = includePath(statement);
		if (include.isEmpty())
			return new LinkedHashSet<>();
		return resolveCppInclude(statement.getLocationInfo().getFilePath(), include.get());
	}

	/**
	 * TypeScript triple-slash directives ("/// <reference path='...'/>", "/// <reference types='...'/>").
	 */
	public Set<String> resolvePaths(UMLComment comment) throws Exception {
		Set<String> paths = new LinkedHashSet<>();
		String currentFilePath = comment.getLocationInfo().getFilePath();
		Matcher reference = TS_REFERENCE.matcher(comment.getText());
		if (!PathFileUtils.isTypeScriptFile(currentFilePath) || !reference.find())
			return paths;
		if (reference.group(1).equals("path"))
			paths.addAll(resolveTsModule(normalize(join(parent(currentFilePath), reference.group(2)))));
		else
			paths.addAll(resolveTypeScript(currentFilePath, reference.group(2), null, new HashSet<>()));
		return paths;
	}

	public Set<String> resolvePaths(String currentFilePath, String importSpec) throws Exception {
		return switch (PathFileUtils.getLang(currentFilePath)) {
			case JAVA -> resolveJvm(currentFilePath, importSpec, ".java");
			case KOTLIN -> resolveJvm(currentFilePath, importSpec, ".kt", ".java");
			case PYTHON -> resolvePython(currentFilePath, importSpec, new HashSet<>());
			case TYPESCRIPT -> resolveTypeScript(currentFilePath, importSpec, null, new HashSet<>());
			case C, CPP -> resolveCppInclude(currentFilePath, importSpec);
		};
	}

	/**
	 * The files declaring the supertypes of the class, transitively, whose members are used without imports.
	 */
	public Set<String> resolveSupertypes(UMLAbstractClass context) throws Exception {
		Set<String> paths = new LinkedHashSet<>();
		for (UMLAbstractClass supertype : supertypeClasses(context))
			paths.add(supertype.getSourceFile());
		paths.remove(context.getSourceFile());
		return paths;
	}

	/**
	 * The files declaring the types referenced by a type (including type arguments) written in the scope of the class,
	 * e.g., an attribute, parameter, or variable type, a supertype, or the return type of a declaration used through a call.
	 */
	public Set<String> resolveType(UMLType type, UMLAbstractClass context) throws Exception {
		Set<String> names = new LinkedHashSet<>();
		collectTypeNames(type, names);
		Set<String> paths = new LinkedHashSet<>();
		for (String name : names) {
			if (!BUILT_IN_TYPES.contains(name))
				paths.addAll(resolveName(name, context));
		}
		return paths;
	}

	/**
	 * The files declaring a simple or qualified name written in the scope of the class
	 * (e.g., a type name, a call receiver from {@code AbstractCall.getExpression()}, or a called function name),
	 * found through the imports of the file and, for Java/Kotlin, its package.
	 * Declarations of the class's own file are not reported.
	 */
	public Set<String> resolveName(String name, UMLAbstractClass context) throws Exception {
		Set<String> paths = resolveNameInScope(name, context);
		paths.remove(context.getSourceFile());
		return paths;
	}

	/**
	 * Java: the qualified names that a simple type name written in the file of the class may stand for: the single-type
	 * import binding it, or else the packages imported on demand and java.lang.
	 */
	public List<String> qualifiedNameCandidates(String simpleName, UMLAbstractClass context) throws Exception {
		List<String> candidates = new ArrayList<>();
		for (UMLImport umlImport : fileImports(context)) {
			String imported = umlImport.getName();
			if (imported != null && !umlImport.isStatic() && !umlImport.isOnDemand()
					&& (imported.equals(simpleName) || imported.endsWith("." + simpleName)))
				return List.of(imported);
		}
		for (UMLImport umlImport : fileImports(context)) {
			if (umlImport.getName() != null && !umlImport.isStatic() && umlImport.isOnDemand())
				candidates.add(umlImport.getName() + "." + simpleName);
		}
		candidates.add("java.lang." + simpleName);
		return candidates;
	}

	/**
	 * Java/Kotlin: the qualified name of the class whose member is bound to the simple name by a single static import
	 * (e.g., {@code a.b.C} of "import static a.b.C.name").
	 */
	public Optional<String> singleStaticImportOwner(String name, UMLAbstractClass context) throws Exception {
		for (UMLImport umlImport : fileImports(context)) {
			String imported = umlImport.getName();
			if (imported != null && umlImport.isStatic() && !umlImport.isOnDemand() && imported.endsWith("." + name))
				return Optional.of(imported.substring(0, imported.length() - name.length() - 1));
		}
		return Optional.empty();
	}

	/**
	 * Java/Kotlin: the qualified names of the classes whose static members are imported on demand
	 * (e.g., {@code a.b.C} of "import static a.b.C.*"), in the order of the imports.
	 */
	public List<String> onDemandStaticImportOwners(UMLAbstractClass context) throws Exception {
		List<String> owners = new ArrayList<>();
		for (UMLImport umlImport : fileImports(context)) {
			if (umlImport.getName() != null && umlImport.isStatic() && umlImport.isOnDemand())
				owners.add(umlImport.getName());
		}
		return owners;
	}

	/**
	 * C/C++: the implementation file of a header declaring a function, method, or variable,
	 * where the linker finds its definition without an include.
	 */
	public Set<String> resolveDefinitions(LocationInfoProvider declaration) throws Exception {
		return cppImplementations(declaration.getLocationInfo().getFilePath());
	}

	private Set<String> resolveJvm(String currentFilePath, String importSpec, String... extensions) throws Exception {
		return resolveJvm(currentFilePath, importSpec, 2, extensions);
	}

	private Set<String> resolveJvm(String currentFilePath, String importSpec, int minimumSegments, String... extensions) throws Exception {
		// on-demand imports of a type (e.g., static or enum entries) resolve to the type; those of a package are not resolved
		String qualifiedName = importSpec.endsWith(".*") ? importSpec.substring(0, importSpec.length() - 2) : importSpec;
		String[] segments = qualifiedName.split("\\.");
		// trailing segments may be members or nested types, so drop them until a file matches
		for (int n = segments.length; n >= Math.min(minimumSegments, segments.length); n--) {
			String relativePath = String.join("/", Arrays.copyOf(segments, n));
			List<String> matches = new ArrayList<>();
			for (String extension : extensions)
				matches.addAll(pathsEndingWith(relativePath + extension));
			if (!matches.isEmpty())
				return byCloseness(currentFilePath, matches);
		}
		return new LinkedHashSet<>();
	}

	private Set<String> resolvePython(String currentFilePath, String moduleName, Set<String> visited) throws Exception {
		int level = 0;
		while (level < moduleName.length() && moduleName.charAt(level) == '.')
			level++;
		String dotted = moduleName.substring(level);
		String[] segments = dotted.isEmpty() ? new String[0] : dotted.split("\\.");
		if (level == 0 && segments.length == 0)
			return new LinkedHashSet<>();
		Set<String> roots = new LinkedHashSet<>();
		if (level > 0) {
			String dir = parent(currentFilePath);
			for (int i = 1; i < level; i++) {
				if (dir.isEmpty())
					return new LinkedHashSet<>();
				dir = parent(dir);
			}
			roots.add(dir);
		}
		else {
			// the importing file's folder also covers relative imports whose level is not recorded
			roots.add(parent(currentFilePath));
			roots.add("");
			roots.add("src");
		}
		// only "from . import x" can import a name from the package itself
		int minimum = level > 0 && segments.length <= 1 ? 0 : 1;
		// trailing segments may be names defined inside the module, so drop them until a file matches
		for (int n = segments.length; n >= minimum; n--) {
			String relativePath = String.join("/", Arrays.copyOf(segments, n));
			Set<String> found = new LinkedHashSet<>();
			for (String root : roots)
				found.addAll(pythonModuleFiles(join(root, relativePath), n > 0));
			// a package path of two or more segments under an unknown source root
			if (found.isEmpty() && level == 0 && n >= 2) {
				List<String> candidates = new ArrayList<>();
				for (String suffix : new String[] {".py", ".pyi", "/__init__.py", "/__init__.pyi"})
					candidates.addAll(pathsEndingWith(relativePath + suffix));
				found.addAll(byCloseness(currentFilePath, candidates));
			}
			if (!found.isEmpty()) {
				Set<String> resolved = new LinkedHashSet<>(found);
				for (String path : found) {
					if (n < segments.length && fileName(path).startsWith("__init__."))
						resolved.addAll(pythonReExports(path, segments[n], visited));
					resolved.addAll(pythonParentPackages(path, Arrays.copyOf(segments, n)));
				}
				return resolved;
			}
		}
		return new LinkedHashSet<>();
	}

	private Set<String> pythonModuleFiles(String base, boolean module) throws Exception {
		Set<String> found = new LinkedHashSet<>();
		List<String> candidates = new ArrayList<>();
		if (module) {
			candidates.add(base + ".py");
			candidates.add(base + ".pyi");
		}
		candidates.add(join(base, "__init__.py"));
		candidates.add(join(base, "__init__.pyi"));
		for (String candidate : candidates) {
			if (exists(candidate))
				found.add(candidate);
		}
		return found;
	}

	private List<String> pythonParentPackages(String modulePath, String[] segments) throws Exception {
		// importing a.b.c executes a/__init__.py and a/b/__init__.py
		List<String> packages = new ArrayList<>();
		String stem = fileName(modulePath).replaceAll("\\.pyi?$", "");
		boolean moduleFile = segments.length > 0 && stem.equals(segments[segments.length - 1]);
		String dir = moduleFile ? parent(modulePath) : parent(parent(modulePath));
		for (int i = segments.length - 2; i >= 0 && fileName(dir).equals(segments[i]); i--) {
			for (String init : new String[] {"__init__.py", "__init__.pyi"}) {
				if (exists(join(dir, init)))
					packages.add(join(dir, init));
			}
			dir = parent(dir);
		}
		return packages;
	}

	private Set<String> pythonReExports(String initFile, String importedName, Set<String> visited) throws Exception {
		// follows "from m import name [as alias]" lines of a package exporting the imported name
		Set<String> resolved = new LinkedHashSet<>();
		if (!visited.add(initFile + "#" + importedName))
			return resolved;
		Matcher fromImport = PYTHON_FROM_IMPORT.matcher(files.content(initFile));
		while (fromImport.find()) {
			String module = fromImport.group(1);
			for (String item : fromImport.group(2).replaceAll("[()\\\\]", " ").split(",")) {
				String[] parts = item.trim().split("\\s+as\\s+");
				String original = parts[0].trim();
				String exported = parts.length > 1 ? parts[1].trim() : original;
				if (!original.isEmpty() && !original.equals("*") && exported.equals(importedName)) {
					String spec = module.endsWith(".") ? module + original : module + "." + original;
					resolved.addAll(resolvePython(initFile, spec, visited));
				}
			}
		}
		return resolved;
	}

	private Set<String> resolveTypeScript(String currentFilePath, String source, String importedName, Set<String> visited) throws Exception {
		Set<String> resolved = resolveTypeScriptSource(currentFilePath, source);
		Set<String> withReExports = new LinkedHashSet<>(resolved);
		if (importedName != null) {
			for (String path : resolved)
				withReExports.addAll(tsReExports(path, importedName, visited));
		}
		return withReExports;
	}

	private Set<String> tsReExports(String path, String importedName, Set<String> visited) throws Exception {
		// follows "export { a as importedName } from" and "export * as importedName from" re-exports of the imported name
		Set<String> resolved = new LinkedHashSet<>();
		if (!isTsSource(path) || !visited.add(path + "#" + importedName))
			return resolved;
		Matcher reExport = TS_RE_EXPORT.matcher(files.content(path));
		while (reExport.find()) {
			String namespace = reExport.group(1);
			String names = reExport.group(2);
			String source = reExport.group(3);
			if (names == null) {
				if (importedName.equals(namespace))
					resolved.addAll(resolveTypeScript(path, source, null, visited));
				continue;
			}
			for (String entry : names.split(",")) {
				String[] parts = entry.trim().replaceFirst("^type\\s+", "").split("\\s+as\\s+");
				String original = parts[0].trim();
				String exported = parts.length > 1 ? parts[1].trim() : original;
				if (exported.equals(importedName))
					resolved.addAll(resolveTypeScript(path, source, original, visited));
			}
		}
		return resolved;
	}

	private static boolean isTsSource(String path) {
		for (String extension : TS_EXTENSIONS) {
			if (path.endsWith(extension))
				return true;
		}
		return false;
	}

	private Set<String> resolveTypeScriptSource(String currentFilePath, String source) throws Exception {
		String currentDir = parent(currentFilePath);
		if (source.equals(".") || source.equals("..") || source.startsWith("./") || source.startsWith("../"))
			return resolveTsModule(normalize(join(currentDir, source)));
		if (source.startsWith("/"))
			return resolveTsModule(normalize(source));
		Optional<String> tsConfig = nearest(currentDir, "tsconfig.json", "jsconfig.json");
		if (tsConfig.isPresent()) {
			Set<String> viaTsConfig = resolveViaTsConfig(loadTsConfig(tsConfig.get()), source);
			if (!viaTsConfig.isEmpty())
				return viaTsConfig;
		}
		if (source.startsWith("#")) {
			Optional<String> packageJson = nearest(currentDir, "package.json");
			if (packageJson.isEmpty())
				return new LinkedHashSet<>();
			JsonObject imports = object(json(packageJson.get()), "imports");
			return imports == null ? new LinkedHashSet<>() : resolveViaPackageMap(parent(packageJson.get()), imports, source);
		}
		return resolveWorkspacePackage(source);
	}

	private Set<String> resolveTsModule(String base) throws Exception {
		if (base == null)
			return new LinkedHashSet<>();
		Set<String> resolved = resolveTsFile(base);
		if (!resolved.isEmpty())
			return resolved;
		String packageJson = join(base, "package.json");
		if (exists(packageJson)) {
			JsonObject manifest = json(packageJson);
			if (manifest != null)
				return resolvePackageEntry(base, manifest, "");
		}
		return resolved;
	}

	private Set<String> resolveTsFile(String base) throws Exception {
		Set<String> resolved = new LinkedHashSet<>();
		if (base == null)
			return resolved;
		List<String> candidates = new ArrayList<>();
		candidates.add(base);
		for (String extension : TS_EXTENSIONS)
			candidates.add(base + extension);
		// ESM-style TypeScript sources import the emitted file name
		for (Map.Entry<String, String[]> emitted : TS_EMITTED_EXTENSIONS.entrySet()) {
			if (base.endsWith(emitted.getKey())) {
				String withoutExtension = base.substring(0, base.length() - emitted.getKey().length());
				for (String extension : emitted.getValue())
					candidates.add(withoutExtension + extension);
			}
		}
		for (String candidate : candidates) {
			if (exists(candidate))
				resolved.add(candidate);
		}
		if (resolved.isEmpty()) {
			for (String extension : TS_EXTENSIONS) {
				String index = join(base, "index" + extension);
				if (exists(index))
					resolved.add(index);
			}
		}
		return resolved;
	}

	private TsConfig loadTsConfig(String configPath) throws Exception {
		TsConfig inherited = new TsConfig(null, null, null);
		JsonObject config = json(configPath);
		if (config == null)
			return inherited;
		String configDir = parent(configPath);
		Optional<String> extended = string(config, "extends");
		if (extended.isPresent() && extended.get().startsWith(".")) {
			String extendedPath = normalize(join(configDir, extended.get().endsWith(".json") ? extended.get() : extended.get() + ".json"));
			if (extendedPath != null && exists(extendedPath))
				inherited = loadTsConfig(extendedPath);
		}
		JsonObject options = object(config, "compilerOptions");
		Optional<String> ownBaseUrl = string(options, "baseUrl");
		String baseUrl = ownBaseUrl.isPresent() ? normalize(join(configDir, ownBaseUrl.get())) : inherited.baseUrl();
		JsonObject paths = object(options, "paths");
		if (paths != null)
			return new TsConfig(baseUrl, baseUrl != null ? baseUrl : configDir, paths);
		return new TsConfig(baseUrl, baseUrl != null ? baseUrl : inherited.pathsBase(), inherited.paths());
	}

	private Set<String> resolveViaTsConfig(TsConfig config, String source) throws Exception {
		Set<String> resolved = new LinkedHashSet<>();
		if (config.paths() != null && config.pathsBase() != null) {
			String bestPattern = null;
			String bestCapture = null;
			for (String pattern : config.paths().keySet()) {
				Optional<String> capture = matchWildcard(pattern, source);
				if (capture.isPresent() && (bestPattern == null || wildcardPrefixLength(pattern) > wildcardPrefixLength(bestPattern))) {
					bestPattern = pattern;
					bestCapture = capture.get();
				}
			}
			if (bestPattern != null && config.paths().get(bestPattern).isJsonArray()) {
				for (JsonElement target : config.paths().get(bestPattern).getAsJsonArray()) {
					if (isString(target))
						resolved.addAll(resolveTsModule(normalize(join(config.pathsBase(), target.getAsString().replace("*", bestCapture)))));
				}
			}
		}
		if (resolved.isEmpty() && config.baseUrl() != null)
			resolved.addAll(resolveTsModule(normalize(join(config.baseUrl(), source))));
		return resolved;
	}

	private Set<String> resolveWorkspacePackage(String source) throws Exception {
		String[] parts = source.split("/");
		int nameParts = source.startsWith("@") ? 2 : 1;
		if (parts.length < nameParts)
			return new LinkedHashSet<>();
		String packageName = String.join("/", Arrays.copyOf(parts, nameParts));
		String subpath = String.join("/", Arrays.copyOfRange(parts, nameParts, parts.length));
		// a package of the repository lives in a folder named after the package (without its scope)
		String folderName = parts[nameParts - 1];
		for (String packageJson : files.pathsNamed("package.json")) {
			String packageDir = parent(packageJson);
			if (!packageJson.contains("node_modules/") && fileName(packageDir).equals(folderName)) {
				JsonObject manifest = json(packageJson);
				if (string(manifest, "name").filter(packageName::equals).isPresent())
					return resolvePackageEntry(packageDir, manifest, subpath);
			}
		}
		return new LinkedHashSet<>();
	}

	private Set<String> resolvePackageEntry(String packageDir, JsonObject manifest, String subpath) throws Exception {
		JsonElement exports = manifest.get("exports");
		if (exports != null) {
			JsonObject exportMap;
			if (exports.isJsonObject() && exports.getAsJsonObject().keySet().stream().anyMatch(key -> key.startsWith("."))) {
				exportMap = exports.getAsJsonObject();
			}
			else {
				exportMap = new JsonObject();
				exportMap.add(".", exports);
			}
			Set<String> viaExports = resolveViaPackageMap(packageDir, exportMap, subpath.isEmpty() ? "." : "./" + subpath);
			if (!viaExports.isEmpty())
				return viaExports;
		}
		if (!subpath.isEmpty()) {
			Set<String> direct = resolveTsFile(normalize(join(packageDir, subpath)));
			return !direct.isEmpty() ? direct : resolveTsFile(normalize(join(packageDir, "src/" + subpath)));
		}
		Set<String> resolved = new LinkedHashSet<>();
		for (String field : new String[] {"source", "types", "typings", "module", "main"}) {
			Optional<String> entry = string(manifest, field);
			if (entry.isPresent())
				resolved.addAll(resolveTsFile(normalize(join(packageDir, entry.get()))));
		}
		if (resolved.isEmpty())
			resolved.addAll(resolveTsFile(join(packageDir, "src")));
		if (resolved.isEmpty())
			resolved.addAll(resolveTsFile(packageDir));
		return resolved;
	}

	private Set<String> resolveViaPackageMap(String packageDir, JsonObject map, String key) throws Exception {
		Set<String> resolved = new LinkedHashSet<>();
		String bestPattern = null;
		String bestCapture = null;
		for (String pattern : map.keySet()) {
			Optional<String> capture = matchWildcard(pattern, key);
			if (capture.isPresent() && (bestPattern == null || wildcardPrefixLength(pattern) > wildcardPrefixLength(bestPattern))) {
				bestPattern = pattern;
				bestCapture = capture.get();
			}
		}
		if (bestPattern == null)
			return resolved;
		Set<String> targets = new LinkedHashSet<>();
		conditionalTargets(map.get(bestPattern), targets);
		for (String target : targets)
			resolved.addAll(resolveTsFile(normalize(join(packageDir, target.replace("*", bestCapture)))));
		return resolved;
	}

	private static void conditionalTargets(JsonElement element, Set<String> targets) {
		if (isString(element)) {
			targets.add(element.getAsString());
		}
		else if (element.isJsonArray()) {
			for (JsonElement alternative : element.getAsJsonArray())
				conditionalTargets(alternative, targets);
		}
		else if (element.isJsonObject()) {
			JsonObject conditions = element.getAsJsonObject();
			for (String condition : PACKAGE_CONDITIONS) {
				if (conditions.has(condition))
					conditionalTargets(conditions.get(condition), targets);
			}
			for (String condition : conditions.keySet())
				conditionalTargets(conditions.get(condition), targets);
		}
	}

	private Set<String> resolveCppInclude(String currentFilePath, String includePath) throws Exception {
		String include = includePath.trim();
		if (include.length() >= 2 && ((include.startsWith("\"") && include.endsWith("\"")) || (include.startsWith("<") && include.endsWith(">"))))
			include = include.substring(1, include.length() - 1);
		Set<String> headers = new LinkedHashSet<>();
		String local = normalize(join(parent(currentFilePath), include));
		if (local != null && exists(local)) {
			headers.add(local);
		}
		else {
			// include directories are unknown without the build configuration, so the include path is matched as a path suffix
			String normalized = normalize(include);
			if (normalized != null)
				headers.addAll(byCloseness(currentFilePath, pathsEndingWith(normalized)));
		}
		Set<String> resolved = new LinkedHashSet<>(headers);
		for (String header : headers)
			resolved.addAll(cppImplementations(header));
		return resolved;
	}

	private Optional<String> includePath(UMLPreprocessorStatement statement) throws Exception {
		if (statement.getType() != UMLPreprocessorStatement.Directive.INCLUDE)
			return Optional.empty();
		if (statement.getName().isPresent() && !statement.getName().get().isBlank())
			return statement.getName();
		Matcher include = CPP_INCLUDE.matcher(sourceText(statement.getLocationInfo()));
		return include.find() ? Optional.of(include.group(1)) : Optional.empty();
	}

	private Set<String> cppImplementations(String header) throws Exception {
		Set<String> implementations = new LinkedHashSet<>();
		String stem = null;
		for (String extension : CPP_HEADER_EXTENSIONS) {
			if (header.endsWith(extension))
				stem = fileName(header).substring(0, fileName(header).length() - extension.length());
		}
		if (stem == null)
			return implementations;
		String dir = parent(header);
		List<String> dirs = new ArrayList<>();
		dirs.add(dir);
		// include/<sub>/foo.h is commonly implemented in src/<sub>/foo.cpp or src/foo.cpp
		List<String> segments = dir.isEmpty() ? List.of() : Arrays.asList(dir.split("/"));
		int include = segments.lastIndexOf("include");
		if (include >= 0) {
			String prefix = String.join("/", segments.subList(0, include));
			String rest = String.join("/", segments.subList(include + 1, segments.size()));
			dirs.add(join(join(prefix, "src"), rest));
			dirs.add(join(prefix, "src"));
		}
		for (String candidateDir : dirs) {
			for (String extension : CPP_SOURCE_EXTENSIONS) {
				String candidate = join(candidateDir, stem + extension);
				if (!candidate.equals(header) && exists(candidate))
					implementations.add(candidate);
			}
		}
		return implementations;
	}

	private Set<String> resolveNameInScope(String name, UMLAbstractClass context) throws Exception {
		return switch (PathFileUtils.getLang(context.getSourceFile())) {
			case JAVA, KOTLIN -> resolveJvmName(name, context);
			case PYTHON -> resolvePythonName(name, context);
			case TYPESCRIPT -> resolveTypeScriptName(name, context);
			// names reach their files through the includes of the file
			case C, CPP -> new LinkedHashSet<>();
		};
	}

	private Set<String> resolveJvmName(String name, UMLAbstractClass context) throws Exception {
		String currentFilePath = context.getSourceFile();
		String first = name.split("\\.")[0];
		String rest = name.substring(first.length());
		List<UMLImport> imports = fileImports(context);
		Set<String> paths = new LinkedHashSet<>();
		// single-type imports shadow the same package, which shadows on-demand imports;
		// a name bound by an import of a type outside the repository resolves to no file
		boolean imported = false;
		for (UMLImport umlImport : imports) {
			String importedName = umlImport.getName();
			if (!umlImport.isOnDemand() && (importedName.equals(first) || importedName.endsWith("." + first))) {
				imported = true;
				paths.addAll(resolvePaths(currentFilePath, importedName + rest));
			}
		}
		if (imported)
			return paths;
		String packageName = filePackage(currentFilePath);
		if (packageName.isEmpty()) {
			for (String extension : new String[] {".java", ".kt"}) {
				String sibling = join(parent(currentFilePath), first + extension);
				if (exists(sibling))
					paths.add(sibling);
			}
		}
		else {
			// the first segment of the name is a type of the package
			paths.addAll(resolveJvmName(currentFilePath, packageName + "." + name, packageName.split("\\.").length + 1));
		}
		if (!paths.isEmpty())
			return paths;
		for (UMLImport umlImport : imports) {
			if (umlImport.isOnDemand())
				// the first segment of the name is a type of an imported package, or a member of a type imported statically
				paths.addAll(resolveJvmName(currentFilePath, umlImport.getName() + "." + name,
						umlImport.getName().split("\\.").length + (umlImport.isStatic() ? 0 : 1)));
		}
		if (paths.isEmpty() && name.contains("."))
			paths.addAll(resolvePaths(currentFilePath, name));
		return paths;
	}

	private Set<String> resolveJvmName(String currentFilePath, String qualifiedName, int minimumSegments) throws Exception {
		if (PathFileUtils.isKotlinFile(currentFilePath))
			return resolveJvm(currentFilePath, qualifiedName, minimumSegments, ".kt", ".java");
		return resolveJvm(currentFilePath, qualifiedName, minimumSegments, ".java");
	}

	private Set<String> resolvePythonName(String name, UMLAbstractClass context) throws Exception {
		String currentFilePath = context.getSourceFile();
		String first = name.split("\\.")[0];
		String rest = name.substring(first.length());
		Set<String> moduleNames = new LinkedHashSet<>();
		for (UMLImport umlImport : fileImports(context)) {
			String imported = umlImport.getName();
			if (imported == null || umlImport.isOnDemand())
				continue;
			if (umlImport.getAlias().isPresent()) {
				if (umlImport.getAlias().get().equals(first))
					moduleNames.add(imported + rest);
			}
			else {
				// "from a import b" binds b and "import a.b" binds a, but both are recorded as "a.b"
				if (imported.equals(first) || imported.endsWith("." + first))
					moduleNames.add(imported + rest);
				if (imported.startsWith(first + "."))
					moduleNames.add(name);
			}
		}
		// names qualified by the parser, e.g., supertypes
		if (moduleNames.isEmpty() && name.contains("."))
			moduleNames.add(name);
		Set<String> paths = new LinkedHashSet<>();
		for (String moduleName : moduleNames)
			paths.addAll(resolvePython(currentFilePath, moduleName.startsWith("null.") ? moduleName.substring("null".length()) : moduleName, new HashSet<>()));
		return paths;
	}

	private Set<String> resolveTypeScriptName(String name, UMLAbstractClass context) throws Exception {
		String currentFilePath = context.getSourceFile();
		String[] segments = name.split("\\.");
		Set<String> paths = new LinkedHashSet<>();
		for (UMLImport umlImport : fileImports(context)) {
			String imported = umlImport.getName();
			int dot = imported.lastIndexOf('.');
			if (dot < 0 || !imported.substring(dot + 1).equals(segments[0]))
				continue;
			// named imports follow the re-exports of the name, namespace imports those of the accessed member
			String importedName = umlImport.isStatic() ? segments[0] : umlImport.isOnDemand() && segments.length > 1 ? segments[1] : null;
			paths.addAll(resolveTypeScript(currentFilePath, imported.substring(0, dot), importedName, new HashSet<>()));
		}
		return paths;
	}

	private List<UMLAbstractClass> supertypeClasses(UMLAbstractClass umlClass) throws Exception {
		String key = umlClass.getSourceFile() + "#" + umlClass.getName();
		List<UMLAbstractClass> supertypes = supertypesByClass.get(key);
		if (supertypes != null)
			return supertypes;
		supertypes = new ArrayList<>();
		// registered before recursing, so cyclic hierarchies terminate
		supertypesByClass.put(key, supertypes);
		List<UMLType> types = new ArrayList<>();
		if (umlClass.getSuperclass() != null)
			types.add(umlClass.getSuperclass());
		types.addAll(umlClass.getImplementedInterfaces());
		for (UMLType type : types) {
			String typeName = mainTypeName(type);
			if (typeName == null)
				continue;
			String simpleName = lastSegment(typeName);
			// the supertype may be declared in the same file
			Set<String> declaring = new LinkedHashSet<>(resolveNameInScope(typeName, umlClass));
			declaring.add(umlClass.getSourceFile());
			for (Map.Entry<String, List<UMLClass>> entry : classesOf(declaring).entrySet()) {
				for (UMLClass candidate : entry.getValue()) {
					boolean self = entry.getKey().equals(umlClass.getSourceFile()) && candidate.getName().equals(umlClass.getName());
					if (!candidate.isModule() && !self && candidate.getNonQualifiedName().equals(simpleName)) {
						supertypes.add(candidate);
						supertypes.addAll(supertypeClasses(candidate));
					}
				}
			}
		}
		return supertypes;
	}

	/**
	 * The name of the type without type arguments and array dimensions (e.g., Map.Entry of Map.Entry<K, V>[]), or null
	 * for types without a main name (e.g., function types).
	 */
	public static String mainTypeName(UMLType type) {
		if (type instanceof CompositeType composite)
			return compositeName(composite);
		if (type instanceof LeafType leaf)
			return leaf.getClassType();
		return null;
	}

	private static String compositeName(CompositeType composite) {
		UMLType left = composite.getLeftType();
		String leftName = left instanceof CompositeType leftComposite ? compositeName(leftComposite) : left.getClassType();
		return leftName + "." + composite.getRightType().getClassType();
	}

	private static void collectTypeNames(UMLType type, Set<String> names) {
		if (type == null)
			return;
		if (type instanceof ListCompositeType list) {
			for (UMLType member : list.getTypes())
				collectTypeNames(member, names);
		}
		else if (type instanceof FunctionType function) {
			collectTypeNames(function.getReceiver(), names);
			collectTypeNames(function.getReturnType(), names);
			for (UMLType parameter : function.getParameterList())
				collectTypeNames(parameter, names);
		}
		else if (type instanceof IndexedAccessType indexed) {
			collectTypeNames(indexed.getObjectType(), names);
			collectTypeNames(indexed.getIndexType(), names);
		}
		else {
			String name = mainTypeName(type);
			if (name != null)
				names.add(name);
		}
		if (type.getTypeArguments() != null) {
			for (UMLType argument : type.getTypeArguments())
				collectTypeNames(argument, names);
		}
	}

	private List<UMLImport> fileImports(UMLAbstractClass context) throws Exception {
		Set<UMLImport> imports = new LinkedHashSet<>(context.getImportedTypes());
		for (UMLClass umlClass : classesOf(context.getSourceFile()))
			imports.addAll(umlClass.getImportedTypes());
		return new ArrayList<>(imports);
	}

	private String filePackage(String path) throws Exception {
		for (UMLClass umlClass : classesOf(path)) {
			if (umlClass.isTopLevel())
				return umlClass.getPackageName();
		}
		return "";
	}

	/**
	 * The classes declared in a file of the repository, parsed without being attached to any model (cached).
	 */
	public List<UMLClass> classesOf(String path) throws Exception {
		return classesOf(List.of(path)).get(path);
	}

	private Map<String, List<UMLClass>> classesOf(Collection<String> paths) throws Exception {
		// parsed with RefactoringMiner in one batch, as the Kotlin environment is created per batch
		Map<String, String> unparsed = new LinkedHashMap<>();
		for (String path : paths) {
			if (!classesByFile.containsKey(path) && PathFileUtils.isSupportedFile(path) && exists(path))
				unparsed.put(path, files.content(path));
		}
		if (!unparsed.isEmpty()) {
			UMLModel model = new UMLModelASTReader(unparsed, new LinkedHashSet<>(), false).getUmlModel();
			for (String path : unparsed.keySet())
				classesByFile.put(path, new ArrayList<>());
			for (UMLClass umlClass : model.getClassList())
				classesByFile.computeIfAbsent(umlClass.getSourceFile(), k -> new ArrayList<>()).add(umlClass);
		}
		Map<String, List<UMLClass>> classes = new LinkedHashMap<>();
		for (String path : paths)
			classes.put(path, classesByFile.getOrDefault(path, List.of()));
		return classes;
	}

	private Optional<String> nearest(String dir, String... fileNames) throws Exception {
		for (String current = dir; ; current = parent(current)) {
			for (String fileName : fileNames) {
				String candidate = join(current, fileName);
				if (exists(candidate))
					return Optional.of(candidate);
			}
			if (current.isEmpty())
				return Optional.empty();
		}
	}

	private boolean exists(String path) throws Exception {
		return files.exists(path);
	}

	private String sourceText(LocationInfo locationInfo) throws Exception {
		String content = files.content(locationInfo.getFilePath());
		int start = Math.min(Math.max(locationInfo.getStartOffset(), 0), content.length());
		int end = Math.min(Math.max(locationInfo.getEndOffset(), start), content.length());
		return content.substring(start, end);
	}

	private JsonObject json(String path) throws Exception {
		Optional<JsonObject> cached = jsonCache.get(path);
		if (cached == null) {
			cached = Optional.empty();
			if (exists(path)) {
				try {
					// tsconfig files allow comments (handled by the lenient parser) and trailing commas
					JsonElement parsed = JsonParser.parseString(files.content(path).replaceAll(",(\\s*[}\\]])", "$1"));
					if (parsed.isJsonObject())
						cached = Optional.of(parsed.getAsJsonObject());
				}
				catch (JsonParseException e) {
					// malformed config, treated as absent
				}
			}
			jsonCache.put(path, cached);
		}
		return cached.orElse(null);
	}

	private List<String> pathsEndingWith(String relativePath) throws Exception {
		List<String> matches = new ArrayList<>();
		for (String path : files.pathsNamed(fileName(relativePath))) {
			if (path.equals(relativePath) || path.endsWith("/" + relativePath))
				matches.add(path);
		}
		return matches;
	}

	private static Set<String> byCloseness(String currentFilePath, Collection<String> paths) {
		List<String> sorted = new ArrayList<>(paths);
		sorted.sort(Comparator.comparingInt((String path) -> commonDirSegments(currentFilePath, path)).reversed()
				.thenComparingInt(String::length)
				.thenComparing(Comparator.naturalOrder()));
		return new LinkedHashSet<>(sorted);
	}

	private static int commonDirSegments(String path1, String path2) {
		String[] dirs1 = parent(path1).split("/");
		String[] dirs2 = parent(path2).split("/");
		int common = 0;
		while (common < dirs1.length && common < dirs2.length && dirs1[common].equals(dirs2[common]))
			common++;
		return common;
	}

	private static Optional<String> matchWildcard(String pattern, String value) {
		int star = pattern.indexOf('*');
		if (star < 0)
			return pattern.equals(value) ? Optional.of("") : Optional.empty();
		String prefix = pattern.substring(0, star);
		String suffix = pattern.substring(star + 1);
		if (value.length() >= prefix.length() + suffix.length() && value.startsWith(prefix) && value.endsWith(suffix))
			return Optional.of(value.substring(prefix.length(), value.length() - suffix.length()));
		return Optional.empty();
	}

	private static int wildcardPrefixLength(String pattern) {
		int star = pattern.indexOf('*');
		return star < 0 ? Integer.MAX_VALUE : star;
	}

	private static JsonObject object(JsonObject json, String key) {
		if (json == null || json.get(key) == null || !json.get(key).isJsonObject())
			return null;
		return json.getAsJsonObject(key);
	}

	private static Optional<String> string(JsonObject json, String key) {
		if (json == null || !isString(json.get(key)))
			return Optional.empty();
		return Optional.of(json.get(key).getAsString());
	}

	private static boolean isString(JsonElement element) {
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
	}

	private static String unquote(String text) {
		String trimmed = text.trim();
		if (trimmed.length() >= 2 && "'\"`".indexOf(trimmed.charAt(0)) >= 0 && trimmed.charAt(trimmed.length() - 1) == trimmed.charAt(0))
			return trimmed.substring(1, trimmed.length() - 1);
		return trimmed;
	}

	private static String lastSegment(String name) {
		String[] segments = name.split("::|\\.");
		return segments.length == 0 ? name : segments[segments.length - 1];
	}

	private static String parent(String path) {
		int slash = path.lastIndexOf('/');
		return slash < 0 ? "" : path.substring(0, slash);
	}

	private static String fileName(String path) {
		return path.substring(path.lastIndexOf('/') + 1);
	}

	private static String join(String dir, String relativePath) {
		if (dir.isEmpty())
			return relativePath;
		if (relativePath.isEmpty())
			return dir;
		return dir + "/" + relativePath;
	}

	private static String normalize(String path) {
		Deque<String> segments = new ArrayDeque<>();
		for (String segment : path.split("/")) {
			if (segment.isEmpty() || segment.equals("."))
				continue;
			if (segment.equals("..")) {
				if (segments.isEmpty())
					return null;
				segments.removeLast();
			}
			else {
				segments.addLast(segment);
			}
		}
		return String.join("/", segments);
	}
}
