# Eclipse CDT vs. srcML as the C/C++ parser for RefactoringMiner

This document compares Eclipse CDT, the parser RefactoringMiner currently uses for C and C++, with srcML, which has been suggested as an alternative. The question is narrow: **which parser better serves RefactoringMiner's goals, i.e., building a refactoring-aware program model and a fine-grained AST diff of two versions of C/C++ files that are analyzed without their headers or build configuration?**

Everything below was measured, not taken from documentation. Each claim comes either from a small scenario file run through both parsers, or from a corpus of 769 real C/C++ files. The scenarios are reproduced inline, and section 9 describes how to rerun the corpus experiments.

## Contents

1. [Summary](#1-summary)
2. [Setup and method](#2-setup-and-method)
3. [What RefactoringMiner needs from a C/C++ parser](#3-what-refactoringminer-needs-from-a-cc-parser)
4. [Architectural differences](#4-architectural-differences)
5. [Scenarios](#5-scenarios)
6. [Corpus-scale comparison](#6-corpus-scale-comparison)
7. [Performance](#7-performance)
8. [Integration, distribution and licensing](#8-integration-distribution-and-licensing)
9. [Reproducing the experiments](#9-reproducing-the-experiments)
10. [Threats to validity](#10-threats-to-validity)
11. [Conclusion](#11-conclusion)

## 1. Summary

Both tools are mature and actively maintained, and each is clearly better at some things.

**Where srcML is better:**

- **It never fails and its markup is lossless.** With `--src-encoding=UTF-8` on extraction, all 779 corpus files (the 10 non-UTF-8 files included) round-trip byte for byte. It has no notion of a parse error: it always produces markup. (Caveat: 1.2.0's *default* extraction rewrites non-ASCII characters; see section 6.)
- **It marks up every preprocessor branch.** Function variants in sibling `#if`/`#else` branches all get full markup with bodies. CDT parses one configuration. RefactoringMiner recovers the other branches with an extra re-parse (`CppConditionalBodies`, issue #1218).
- **Its heuristics often guess right without headers.** It reads `std::make_unique<Foo>(x)`, `std::vector<Item> v = ...` and `ns::convert<int>(value);` correctly without any declarations. Raw CDT gets the first two wrong. RefactoringMiner's CDT pipeline fixes them, at a cost of about 5× the raw parse time, but `ns::convert<int>(value);` stays a declaration.
- **It handles several odd macro positions that CDT turns into problem nodes.** Examples are `if UNLIKELY (x)`, `"%09" PRId64`, `class EXPORT_MACRO Name {`, and `Q_OBJECT`.
- **It covers more modern C++ syntax.** It handles `operator<=>`, designated initializers in declarations, and `using enum`, all of which are CDT problem nodes. Both parsers have other C++20 gaps.

**Where CDT is better:**

- **It produces real expression trees.** srcML marks up expressions as flat sequences of names and operators: `a + b * c` has no node for `b * c`. In our corpus, 22.6% of srcML expressions that contain an operator have two or more operators at the same level. That leaves about 63,700 intermediate sub-expressions with no node, and those nodes are exactly what RefactoringMiner's statement mappings and GumTree's subtree matching map. srcML also uses the same `<operator>` tag for unary and binary operators (`-a - b`, `*p * q`), and represents C-style casts as plain tokens.
- **It does semantic analysis.** When declarations are visible, CDT uses them to resolve C++'s inherent ambiguities: it correctly reads `a * b;` with `int a, b` as a multiplication, and `T(y);` as an expression. srcML never uses declarations, so it reads `a * b;` as a pointer declaration whatever `a` is.
- **It reports what it couldn't parse.** Problem nodes are the signal RefactoringMiner uses to repair the parse: stubs, forced-include re-parses, and fragment re-parses. srcML misparses silently.
- **It expands real macro definitions.** Stub headers make `TEST(...) {}`, `MATCHER_P(...)` and protobuf's `DO_(...)` into the right constructs, and in-file `#define`s are expanded. srcML can only treat macros as opaque calls, plus coarse token categories.
- **It is more accurate on real code where it matters most for us.** srcML 1.2.0 (and 1.1.0) systematically misparses `auto &x = e;`, `auto *p = e;` and `auto &&x = e;` as expression statements. That pattern occurs on 2,440 lines of the corpus. srcML also fails to recognize about 1.6% of ordinary function definitions, and in some cases the misparse swallows the rest of a class or file.
- **It is faster.** Single-threaded on the corpus, srcML takes 16.2 s, against 1.05 s for raw CDT and 5.1 s for RefactoringMiner's full CDT pipeline. Consuming srcML would also add XML parsing (1.7 s) and process or native-library overhead.
- **It is easier to integrate.** CDT is pure Java on Maven Central under EPL-2.0. srcML is a native C++ executable or library under **GPL-3.0-only**, while RefactoringMiner is MIT-licensed.

**Bottom line.** For AST diff generation and refactoring detection, the decisive requirements are expression-level structure, operator and node types, and declaration-aware disambiguation. CDT provides these and srcML, by design, does not. srcML's strengths (robustness, preprocessor-branch coverage, heuristics without headers) are real, but RefactoringMiner already gets most of their effect on top of CDT: stubs and re-parses cut problem text from 7.0% to 1.6% of the corpus, and inactive branches are re-parsed. Switching to srcML would mean rebuilding the expression trees and semantic disambiguation that CDT already provides. It would also bring back exactly the ambiguity problems srcML leaves to its consumer, and introduce a GPL native dependency. Keeping CDT is the better choice for these goals. Section 11 lists ideas worth borrowing from srcML.

## 2. Setup and method

**Versions.**

| Component | Version |
|---|---|
| srcML | **1.2.0**, the latest release (2026-10-28): the official macOS arm64 binary, plus a source build of the `v1.2.0` tag (Release, `-O3`) that produces byte-identical output on the whole corpus. Key results were cross-checked with the official 1.1.0 binary. |
| Eclipse CDT | `io.joern:eclipse-cdt-core:9.3.200.202607131546+1`, the dependency RefactoringMiner uses (GNU C++ parser) |
| RefactoringMiner | master at `220530700` |
| Machine | Apple Silicon (arm64); all timings single-threaded unless stated |

**Three configurations are compared throughout:**

- **srcML:** `srcml -l C++` with default options (preprocessor markup on, `#else` branches marked up). `--position --tabs 1` was added where positions were needed.
- **Raw CDT:** CDT's GNU C++ parser with no include paths, no stubs and no re-parses, plus `OPTION_PARSE_INACTIVE_CODE`. This is the fair "parser against parser" comparison.
- **RM pipeline:** `CppFileProcessor.parse`, i.e., what RefactoringMiner actually uses. It is CDT plus forced-include stubs (`cpp-stubs/std.h`, `gtest.h`, `absl.h`, `protobuf.h`, `linux.h`), plus re-parses that declare unknown declaration macros, function templates and class templates found in the file. For the AST diff, `CppTreeGenerator` additionally re-parses problem and inactive fragments.

**Corpus.** These are all C/C++ files cached for RefactoringMiner's C/C++ test commits, minus 10 files that are not valid UTF-8 and were excluded so offsets can be compared exactly. That leaves **769 files, 31.1 MB, 851,130 lines**: 115 `.c`, 202 headers, and the rest C++. They come from fallout2-ce (230), tensorflow/XLA (179), Hyprland (118), duckdb (108), llama.cpp (30), RefactoringMiner's own C++ test sources (28), linux (16), rm-action-test (14), flatbuffers (12), drill (12), bitcoin (8), googletest (6), Catch2 (6) and opencv (2). All files were parsed as C++ by both tools, which is what RefactoringMiner does today.

**Alignment.** srcML positions (1-based line:column, counting code points, inclusive end) were converted to UTF-16 offsets, CDT's native unit. Elements were then matched by **exact span**. This compares the same text region in both parsers without having to match names. Section 6 classifies the disagreements by cause.

## 3. What RefactoringMiner needs from a C/C++ parser

RefactoringMiner uses the parser in two places, and both shape the requirements:

1. **The program model** (`CppFileProcessor`, `CppVisitor`, `CppOperationBody`). It builds classes, functions (operations), attributes, and statement trees. Each statement carries its variable declarations, calls with receiver and arguments, object creations, infix expressions with operators, ternaries, casts, lambdas, literals, and so on. Statement mapping, replacement detection (e.g., `a + b` → `a + c`), and refactoring detection (Extract/Inline Variable, Extract Method, Rename, and so on) all work on these typed sub-expressions. The C++ model references `IASTBinaryExpression` more than any other CDT node type (42 references). It also uses name resolution (13 `resolveBinding` calls) to tell type names from other names.
2. **The AST diff** (`CppTreeGenerator` plus GumTree-style matching). This needs a fine-grained tree with exact offsets. Moved or extracted sub-expressions have to exist as nodes to be mapped as units. For example, the initializer of an extracted variable maps to the sub-expression it replaced (scenario S10).

Because RefactoringMiner analyzes commits file by file, **headers and build flags are never available**. Raw robustness without headers therefore matters, and so does the ability to repair a parse when it goes wrong.

## 4. Architectural differences

| Aspect | Eclipse CDT | srcML |
|---|---|---|
| Kind of parser | Full C/C++ front end (DOM AST), with preprocessor, macro expansion, ambiguity nodes resolved by semantic lookup, and bindings | "Island-style" markup parser (ANTLR 2) that wraps the original text in XML tags using heuristics; no preprocessing, no semantics |
| Expressions | Typed, nested trees: `BinaryExpression(op)`, `UnaryExpression(op)`, `FunctionCallExpression`, `FieldReference`, `ArraySubscriptExpression`, `CastExpression`, `NewExpression`, `SimpleTypeConstructorExpression`, `LambdaExpression`, `ConditionalExpression`, and so on | Flat `<expr>` containing `<name>`, `<operator>`, `<literal>`, `<call>`, `<ternary>`, `<lambda>`, `<cast>` (named casts only); no precedence nesting; unary and binary operators share the `<operator>` tag |
| Preprocessor | Expands macros it knows, evaluates `#if` (one configuration), keeps inactive declarations without bodies | Marks up directives and parses **all** branches as if they were one text (except `#if 0` by default) |
| Unknown macros | Usually problem nodes, unless defined, e.g., via forced-include stubs | Heuristics: `NAME(args)` at statement level becomes `<macro>` or `<call>`; some positions (class heads, specifiers) are recognized |
| Errors | Explicit `IASTProblem*` nodes | None; misparses are silent, and unparsable characters stay as untagged text |
| Semantic info | Bindings, scopes, types (complete when declarations are visible) | None |
| Fidelity | Offsets into the original text; tokens between nodes (punctuation, comments) need to be reconstructed by the consumer | Lossless markup: the XML text content *is* the source (extraction in 1.2.0 needs `--src-encoding=UTF-8`, see section 6) |
| Positions | UTF-16 offsets (the same unit as Java strings) | Optional line:column, counting code points and expanding tabs (default tab stop 8) |
| Runtime | Pure Java library | Native C++ executable and library (`libsrcml`); bindings exist for some languages, not for Java |
| License | EPL-2.0 | GPL-3.0-only |

## 5. Scenarios

Each scenario isolates one behavior. The outputs are abridged from the real tool output. "RM pipeline" is shown only where it differs from raw CDT.

### S1. Expressions and operator precedence

```cpp
int x = a + b * c;
bool y = a && b || !c;
return (a + b) * c;
```

srcML:

```
expr `a + b * c`        → name a, operator +, name b, operator *, name c
expr `a && b || !c`     → name a, operator &&, name b, operator ||, operator !, name c
```

CDT:

```
BinaryExpression `a + b * c`
  IdExpression a
  BinaryExpression `b * c`
BinaryExpression `a && b || !c`
  BinaryExpression `a && b`
  UnaryExpression `!c`
```

The same holds for operator kinds:

```
x = -a - b;       srcML: <operator>-</operator><name>a</name> <operator>-</operator> <name>b</name>
y = *p * q;       srcML: <operator>*</operator><name>p</name> <operator>*</operator> <name>q</name>
v = (int)d + e;   srcML: <operator>(</operator><name>int</name><operator>)</operator><name>d</name> ...
o = new Foo(1);   srcML: <operator>new</operator> <call>Foo(1)</call>
```

**Verdict: CDT.** srcML gives no node for `b * c`, no unary/binary distinction, no C-style cast node, and treats an object creation as a `new` token followed by a call. Turning this into what RefactoringMiner needs would mean writing a C++ expression parser over srcML's tokens. That parser would face the same ambiguities srcML avoided, e.g., whether `(T)-x` is a cast or a subtraction, which depends on whether `T` is a type.

### S2. Templates without headers

```cpp
auto p = std::make_unique<Foo>(x);
ns::convert<int>(value);
std::vector<Item> items = load();
absl::StatusOr<Model> model = Build(cfg);
bool lt = a < b && c > d;
bool cmp = a < b > c;
```

| Line | srcML | Raw CDT | RM pipeline |
|---|---|---|---|
| `std::make_unique<Foo>(x)` | ✅ call with template arguments | ❌ `(std::make_unique < Foo) > (x)` | ✅ call |
| `ns::convert<int>(value);` | ✅ call | ❌ declaration of variable `value` | ❌ declaration (see below) |
| `std::vector<Item> items = load();` | ✅ declaration | ❌ comparison chain | ✅ declaration |
| `absl::StatusOr<Model> model = …` | ✅ declaration | ❌ comparison chain | ✅ declaration |
| `a < b && c > d` | ❌ template `a<b && c>` followed by `d` | ✅ `(a<b) && (c>d)` | ✅ |
| `a < b > c` | ❌ template `a<b>` followed by `c` | ✅ `(a<b) > c` | ✅ |
| **Correct** | **4/6** | **2/6** | **5/6** |

**Verdict: srcML beats raw CDT; RM pipeline is best.** srcML's heuristic prefers templates, which is usually right in real code but wrong for genuine comparisons. Raw CDT, lacking declarations, follows the grammar literally. RefactoringMiner fixes this by detecting misparsed names and re-parsing with generated declarations (`template<class...> int f(...);`, class templates, namespaces). That costs about 5× the raw parse time (section 7). `ns::convert<int>(value);` is genuinely ambiguous in C++: if `convert` were a class template, it *would* declare a variable. CDT applies the standard's rule that anything that can be a declaration is one. srcML's guess (a call) is more often what the author meant.

### S3. Declaration/expression ambiguities, without and with declarations

Without declarations:

```cpp
a * b;   T(x);   Foo bar(baz);   Widget w();   x(y);
```

| Statement | srcML | Raw CDT | RM pipeline |
|---|---|---|---|
| `a * b;` | declaration | declaration | declaration |
| `T(x);` | expression | declaration | expression |
| `Foo bar(baz);` | declaration | declaration | declaration |
| `Widget w();` | declaration | declaration | declaration |
| `x(y);` | expression | expression | declaration |

Without declarations, these are undecidable, and both parsers guess. CDT's guess depends on what it knows. Raw CDT reads `T(x);` as declaring `x`, so `x(y);` is then a call. The RM pipeline declares `T` as a function (its misparsed-call repair), so `T(x);` is a call and `x(y);` becomes a declaration.

With the declarations in the same file:

```cpp
struct T { T(int); };  void x(int);  struct Foo { Foo(int); };
template <class U> U convert(int);
void f(int a, int b, int y, int baz) {
  a * b;  T(y);  x(y);  Foo bar(baz);  convert<int>(y);
}
```

| Statement | Correct reading | srcML | CDT (raw and RM) |
|---|---|---|---|
| `a * b;` | expression (`a` is an `int`) | ❌ declaration of pointer `b` | ✅ expression |
| `T(y);` | expression (redeclaring `y` would be ill-formed) | ✅ expression | ✅ expression |
| `x(y);` | call | ✅ | ✅ |
| `Foo bar(baz);` | declaration | ✅ | ✅ |
| `convert<int>(y);` | call | ✅ | ✅ |

**Verdict: CDT, whenever the declarations are visible** (same file, or provided by a stub). srcML's output never depends on declarations, so it can't improve when more context is available.

### S4. Macros

```cpp
class DUCKDB_API Catalog {
  Q_OBJECT
public:
  DUCKDB_API virtual void Drop(const std::string &name);
  int stats_ ABSL_GUARDED_BY(mu_);
};
absl::Status Run(Ctx &ctx) {
  TF_RET_CHECK(ctx.ok()) << "bad context";
  ASSIGN_OR_RETURN(auto plan, Plan(ctx));
  LOG(INFO) << "running " << plan.name();
  FOR_EACH_CHILD(child, plan) { Visit(child); }
  return absl::OkStatus();
}
TEST(CatalogTest, Drops) { EXPECT_EQ(Drop("a"), 1); }
```

| Construct | srcML | Raw CDT | RM pipeline |
|---|---|---|---|
| `class DUCKDB_API Catalog {` | ✅ `<macro>` in class head | ❌ whole class is a problem | ❌ problem* |
| `Q_OBJECT` | ✅ `<macro>` | ❌ (same problem) | ❌* |
| `DUCKDB_API virtual void Drop(...)` | ✅ (macro absorbed into the type) | ❌ | ✅ (unknown-declaration-macro re-parse) |
| `int stats_ ABSL_GUARDED_BY(mu_);` | ❌ function `ABSL_GUARDED_BY` with parameter `mu_` | ❌ | ✅ attribute (absl stub) |
| `TF_RET_CHECK(c) << "msg";` | ✅ call `<<` literal | ✅ | ✅ |
| `ASSIGN_OR_RETURN(auto plan, Plan(ctx));` | ✅ call with arguments `auto plan`, `Plan(ctx)` | ❌ problem | ❌ problem in the model; the AST diff tree re-parses it as the macro name plus an argument list whose first argument is a declaration |
| `FOR_EACH_CHILD(child, plan) { ... }` | ✅ `<macro>` + block | ❌ call + problem `)` + block | ❌ same |
| `TEST(Suite, Name) { ... }` | ❌ `<macro>` followed by a detached block (no function) | ❌ problem | ✅ function `Suite_Name_Test::TestBody` with `@Test` (gtest stub, when the file includes gtest) |

\* No corpus file contains a class-head export macro or `Q_OBJECT`, so this RM gap doesn't occur in our data. It would be fixable with the same mechanism as the member-level macros.

srcML's only configuration hook for macros is `srcml_archive_register_macro(token, type)`. It assigns one of six token categories (`src:macro`, `src:name`, `src:type`, `src:specifier`, `src:label`, `src:case`). In 1.2.0 it is not in the public header and not exposed on the command line. We called it through the exported symbol and saw **no change** in the markup; we did not root-cause whether that was our usage or a defect. Even when it works, it is a token category, not an expansion, so it can't make `TEST(...)` a function or `ASSIGN_OR_RETURN(auto x, e)` a declaration. CDT expands real definitions, which is what the stubs exploit.

**Verdict: mixed.** srcML's generic heuristics handle more unknown macros out of the box. CDT with stubs handles the frequent library macros *semantically* (tests become functions with test annotations, and `DO_` becomes the `if(!(x)) goto failure` it really is), which matters for refactoring detection. Section 6 quantifies this.

### S5. Conditional compilation

```cpp
#ifdef CONFIG_VMAP_STACK
static int alloc_stack(struct task *tsk, int node) { void *stack = vmalloc(node); return stack ? 0 : -1; }
#else
static int alloc_stack(struct task *tsk, int node) { return kmalloc(node) ? 0 : -1; }
#endif

int g(int x) {
#if defined(_WIN32)
  if (x > 0) {
#else
  if (x >= 0) {
#endif
    return 1;
  }
  return 0;
}
```

- **srcML** marks up *both* `alloc_stack` variants completely. For `g`, it parses both branches as one text: the `#else` branch's `if (x >= 0) {` becomes an `if` **nested inside** the first `if`'s block, and the closing brace is attributed accordingly. That structure is wrong.
- **Raw CDT** gives the active `alloc_stack` (`#else`) with its body. The inactive variant is a declaration without a body. In `g`, the active branch is parsed correctly, and the inactive `if (x > 0) {` is not part of the AST.
- **RM pipeline / AST diff tree:**
  - Both `alloc_stack` variants have bodies, because `CppConditionalBodies` re-parses the inactive branch with the directives blanked.
  - In `g`, the active `if` is a proper `if_statement`, and the inactive `if (x > 0) {` is kept as a sequence of token leaves, so it can still be mapped.
  - The model gives each variant a `preprocessorBranch` identity (issue #1218).

**Verdict: srcML for whole-declaration variants; CDT for directives that split a statement.** srcML gets function variants for free. RefactoringMiner had to build this on top of CDT. When a directive splits a statement, srcML's "concatenate all branches" model produces a wrong structure, while CDT's one configuration is correct and the inactive text stays available as tokens.

### S6. Modern C++ (C++20/23)

| Construct | CDT (raw = RM) | srcML |
|---|---|---|
| `import std;` | ❌ variable declaration | ❌ declaration |
| `concept C = requires(T a) {...};` | ❌ problem | ⚠️ declaration of type `concept`, `requires(...)` as `<macro>` |
| `template <Hashable T>` | ✅ parsed (as a typed non-type parameter) | ✅ |
| `auto operator<=>(const P&) const = default;` | ❌ problem | ✅ |
| `co_await sleep(1);` | ❌ declaration | ❌ declaration of type `co_await` |
| `co_return 42;` | ❌ split + problem | ⚠️ expression starting with name `co_return` |
| `auto [k, v] = *m.begin();` | ✅ structured binding | ❌ expression with an index `[k, v]` |
| `if constexpr (...)` | ✅ | ✅ |
| `[](auto... xs) { return (xs + ...); }` | ✅ | ❌ lambda parameters lost, body as argument list |
| `[]<typename U>(U u) { return u; }` | ❌ problem | ❌ flat tokens + empty statement |
| `P p{.x = 1, .y = 2};` | ❌ problem (RM patches designators only in `return {...}`) | ✅ |
| `m \| std::views::keys \| std::views::take(3)` | ✅ | ✅ |
| `using enum Color;` | ❌ problem | ✅ |

**Verdict: srcML slightly ahead, both incomplete.** CDT fails loudly (problem nodes, which RefactoringMiner's tree generator then re-parses as fragments: 4 error nodes remain in this file's tree). srcML fails silently with wrong shapes.

### S7. C and GNU extensions (`.c` file)

```c
int old_style(a, b) int a; char *b; { return a; }
int g(int *p) {
  int new = 0;
  typeof(*p) v = ({ int t = *p; t + 1; });
  int arr[10] = { [0 ... 3] = 1, [5] = 2 };
  __attribute__((unused)) int u;
  asm volatile ("nop" ::: "memory");
  return v + new;
}
```

| Construct | CDT C parser (`GCCLanguage`) | RM today (parses `.c` as C++) | srcML (`-l C`) |
|---|---|---|---|
| K&R definition | ✅ `KnRFunctionDeclarator` | ❌ problems | ✅ |
| `new` as identifier | ✅ | ❌ problem | ✅ |
| `typeof(*p) v = ({...})` | ✅ | ✅ | ❌ split into `<macro>typeof(*p)</macro>` + expression `v = (...)` |
| range designators `[0 ... 3]` | ✅ | ✅ | ✅ |
| `__attribute__` | ✅ | ✅ | ⚠️ separate element before the declaration |
| `asm` | ✅ | ✅ | ✅ |

**Verdict: CDT's C parser.** RefactoringMiner currently sends `.c` files through the C++ parser. That is an integration choice, not a CDT limitation, and switching `.c` files to `GCCLanguage` would fix the two failures.

### S8. Error recovery

```cpp
int before(int a) { return a + 1; }
int broken(int a) {
  if (a > 0 {
    return a;
  }
  return @ a;
}
int after(int b) { return b * 2; }
```

- **srcML** keeps `broken` as a function and the `if` with its block (the condition `(a > 0` is marked up oddly). `@` is left as untagged text, with no error indication.
- **CDT** keeps `before` and `after` intact. `broken` ends early: its `if` is a `ProblemStatement`, `return @ a;` and the stray `}` become problem declarations, and the problems are reported.

**Verdict: srcML degrades more gracefully; CDT tells you where it failed.** RefactoringMiner depends on that signal: it drives the stub, forced-include and fragment re-parses that bring problem text down from 7.0% to 1.6% of the corpus.

### S9. Positions and text fidelity

On a line with a tab, `é` and an emoji (`\tint s = 0; // café 😀`):

- srcML reports `2:9`–`2:18` for the declaration with the default tab stop 8 (`2:2`–`2:11` with `--tabs 1`). The comment ends at column 28, which counts the emoji as **one** column (a code point).
- CDT reports UTF-16 offsets (`[11,21]`, comment `[22,32]`), the unit Java strings and RefactoringMiner use.

Converting srcML positions requires per-line code-point to UTF-16 translation and care with tabs. That is doable but adds a layer. srcML's lossless round-trip is genuinely valuable for tooling that rewrites code, but RefactoringMiner already reconstructs the gap tokens between CDT nodes, so it gains little here.

### S10. What the flat expressions cost the AST diff: Extract Variable

```cpp
// before
return compute(a, b) + offset * scale;
// after
double scaled = offset * scale;
return compute(a, b) + scaled;
```

RefactoringMiner (CDT) reports `Extract Variable scaled : double` and maps the extracted initializer to the sub-expression it replaced, as one node:

```
binary_expression 56-70 (offset * scale, in the old return)  →  binary_expression 49-63 (initializer of scaled)
```

srcML markup of the old return statement:

```xml
<return>return <expr><call>compute(a, b)</call> <operator>+</operator> <name>offset</name> <operator>*</operator> <name>scale</name></expr>;</return>
```

There is **no node** for `offset * scale` on the old side, so a tree diff over srcML can only map the three tokens one by one. A tree-based refactoring detector has no sub-expression to relate to the new initializer. This is the general consequence of the 63,700 missing intermediate nodes counted in section 6.

## 6. Corpus-scale comparison

All numbers are for the 769-file corpus, **RM pipeline against srcML**, aligned by exact UTF-16 span. Raw CDT numbers are given where they differ meaningfully.

### Round-trip (srcML)

- **The markup is lossless.** `srcml -l C++ FILE | srcml -S --src-encoding=UTF-8` reproduced **all 779 original files byte for byte**, the 10 non-UTF-8 files included.
- **srcML 1.2.0's default extraction is not lossless.** Without `--src-encoding=UTF-8`, `srcml -S` changed **42 of 779 files**, every file containing non-ASCII text. Characters in Latin-1 (`é`, `µ`) are written as single Latin-1 bytes, and characters outside it (`’`, emoji) as XML character references (`&#8217;`). The XML itself is correct UTF-8, and srcML 1.1.0 round-trips the same files correctly with default options, so this is a 1.2.0 regression in extraction. It doesn't affect parsing.
- **`--to-dir` can write outside the target folder.** Extracting an archive whose units carry absolute file names (`srcml archive.xml --to-dir DIR`) wrote the files back to their **original absolute paths**, not under `DIR`, with the default encoding above. During this investigation it overwrote 42 cached input files, which we detected through git and restored. All results in this document were recomputed on the restored originals. Tools that drive srcML should pass relative file names (`--filename`) or avoid `--to-dir`.

### Text covered by CDT problem nodes

| | Problem characters | Share of corpus |
|---|---|---|
| Raw CDT | 2,186,714 | **7.0%** |
| RM pipeline (model parse) | 498,130 | **1.6%** |

srcML has no equivalent measure, because it never reports failures.

### Function definitions (bodies matched by span)

| Category | Count |
|---|---|
| Found by both | 32,698 |
| Found only by CDT | 2,108 |
| Found only by srcML | 1,264 |

**CDT-only (2,108):**

| Count | Cause | Who is right |
|---|---|---|
| 1,074 | Test macros (`TEST`, `TEST_F`, `TEST_P`, `MATCHER_P`, ...). CDT expands them through the gtest stub into functions; srcML gives `<macro>` + a detached block | CDT (for refactoring detection a test is a function) |
| 368 | srcML read the definition as an **expression**, e.g., `ggml_tensor * llama_model_base::create_tensor(...) {`, `CLayerSurface::type() const {` after certain preceding code | CDT |
| 157 | srcML read the definition as a **declaration or call** whose `argument_list` is the body, e.g., `absl::Status SetInstructionLayout(...) { ... }` in a class, `absl::StatusOr<std::unique_ptr<PjRtBuffer>> CommonPjRtClient::BufferFromHostBuffer(...) {` | CDT |
| 47 | srcML didn't recognize the header: constructor initializer lists, array-reference parameters `bool (&mask)[N]`, `typename Container::value_type` return types | CDT |
| 125 | Functions generated by macros **defined in the same file** (`DEFINE_PROP(...)` in Hyprland, `TASK_PFA_TEST(...)` in linux), which CDT expands | Representation (CDT expands, srcML keeps the invocation) |
| 335 | Same function in both, with a different body span | Not a real disagreement |
| 2 | Function-`try` blocks | Not a real disagreement |

Some srcML misparses **cascade**. In `collective_pipeliner.cc`, the `argument_list` of a misparsed `CloneBackwardChain` "declaration" swallows the whole following `WhileLoopAnalysis` class. In `layout_assignment.h`, member functions of `LayoutAssignment` become declarations with initializers.

**srcML-only (1,264):**

| Count | Cause | Who is right |
|---|---|---|
| 590 | Bodies in inactive `#if` branches. CDT keeps only the declaration; the RM pipeline re-parses these bodies separately (`CppConditionalBodies`), so the model has them | srcML natively, RM via re-parse |
| 391 | Inside CDT problem nodes, e.g., bitcoin functions with `EXCLUSIVE_LOCKS_REQUIRED(...)` (no stub), flatbuffers `flexbuffers.h` members | srcML |
| 272 | Same function, different span | Not a real disagreement |
| 9 | srcML errors: `/*index=*/{}` inside a call misread as a nested function | CDT |
| 2 | Other (ARM NEON function after a `#if` chain) | Unclear |

Summary: srcML misses about **572 ordinary function definitions (1.6%)** plus every test-macro body. The RM pipeline misses about **391 (1.2%)** in active code where srcML succeeds.

### Statement kinds (statements inside bodies found by both, matched by span)

345,135 statements matched by span. Their kinds agree for **340,647 (98.7%)**. The disagreements:

| Count | CDT says | srcML says | Analysis |
|---|---|---|---|
| 2,313 | declaration | expression | Mostly **srcML misparses**: in a 400-case sample, 380 are `auto &x = e;` / `auto *p = e;` / `auto &&x = e;` and 14 are structured bindings `auto [a, b] = ...` (srcML wrong). The other 6 are CDT errors: `Printer::Print(ToString());` (4) and `AssertLockHeld(cs_main);` (2), which CDT reads as declarations |
| 1,061 | expression | `throw` | Taxonomy only (srcML has a `throw` statement) |
| 541 | `goto` | expression | Taxonomy: the protobuf stub expands `DO_(x)` to `if (!(x)) goto failure` |
| 519 | problem | `if` / expression / declaration / `return` | **srcML right**: `if UNLIKELY (x)`, `"%09" PRId64` string concatenation, `ASSIGN_OR_RETURN(const T* x, ...)`, `new` as an identifier in Linux `.c` files (RM's C++-mode choice) |
| 54 | various | `<macro>` / expression | Mixed |

With **raw CDT**, a further 2,057 statements are expressions in CDT but declarations in srcML. Those are the template-type misparses of S2 (`std::vector<T> x = e;`), where srcML is right. The RM pipeline reduces them to **16**.

The `auto &` misparse is long-standing (identical in srcML 1.1.0). Lines matching `auto[&*&&] name =|{|(` occur **2,440 times** in the corpus. Each such line would lose its variable declaration, which is the basis of Rename, Extract, Inline and Split Variable detection.

### Calls

| Category | Count |
|---|---|
| Matched by span | 214,741 |
| srcML-only | 40,190 |
| CDT-only | 19,886 |

Most one-sided calls are **representation, not error**. 17,433 srcML-only and 17,463 CDT-only calls are the same call with a different start offset, because the two parsers delimit chained and qualified calls differently. The rest:

| Count | Side | Cause |
|---|---|---|
| 3,174 | srcML-only | Constructor member initializers `x(y)` (CDT: `ConstructorChainInitializer`, deliberately not a call) |
| 1,932 | srcML-only | Brace constructions `T{...}` (CDT: typed constructor and initializer nodes) |
| 997 | srcML-only | Operands of `new` (CDT: `NewExpression`) |
| 5,975 | srcML-only | Calls inside CDT problem nodes |
| 2,834 | srcML-only | Calls inside inactive function bodies (RM re-parses these) |
| 497 | srcML-only | Calls inside directives (`#if defined(X)`, macro definitions) |
| about 3,470 | srcML-only | Not covered by any active CDT statement: mostly inactive regions *inside* active functions, e.g., TensorFlow's `#ifdef USE_NEON` intrinsics (classified from samples) |
| about 3,850 | srcML-only | Inside statements CDT parsed, but with a different node or a macro-expansion location, e.g., `MS<Bool>(...)` as type construction, destructor calls, arguments of the expanded `DO_(...)` (classified from samples) |
| 24 | srcML-only | C casts `(int (*)(Program*))` misread by srcML as calls |
| 1,136 | CDT-only | Same call, different end |
| 330 | CDT-only | Calls inside functions srcML misparsed |
| 957 | CDT-only | Mostly calls from in-file macro expansions (e.g., `GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET`) and `a.Swap(&b)` in protobuf `friend` functions |

**Takeaway:** both parsers find essentially the same calls. CDT gives more specific node types (constructor initializer, brace construction, `new`), which RefactoringMiner's model uses (`ObjectCreation` vs `OperationInvocation`). srcML additionally covers code that CDT doesn't parse (inactive regions, problem regions).

### Flat expressions

| Measure | Count |
|---|---|
| srcML `<expr>` elements | 830,733 |
| …containing at least one binary or assignment operator | 177,792 |
| …with **two or more** such operators at the same level | **40,219 (22.6%)** |
| Operators inside those flat expressions | 103,927 |
| Missing intermediate sub-expression nodes | ≈ 63,700 |

## 7. Performance

769 files, 31.1 MB, single-threaded, same machine, load-gated, repeated:

| Configuration | Time | Notes |
|---|---|---|
| Raw CDT parse (warm) | **1.05 s** | 1.03–1.12 s over warm runs in two sessions; 2.3–2.4 s for the first, cold run |
| RM pipeline `CppFileProcessor.parse`, model mode (warm) | **5.1 s** | 5.04–5.43 s; stubs + up to three re-parses |
| srcML 1.2.0 (official binary), `-j 1`, no positions | **16.2 s** | 3 runs at low load: 16.06, 16.30, 16.36 s (16.5–18.0 s in a second session at higher load); source build: 17.2–17.4 s; official 1.1.0: 17.2 s |
| srcML 1.2.0 (official binary), `-j 1`, `--position --tabs 1` | **18.2–18.4 s** | |
| srcML 1.2.0, default `-j 12` | 3.0 s wall | 28.3 s CPU |
| Reading srcML XML in Java (StAX scan only, no tree building) | **+1.7 s** | 377 MB of XML for 31 MB of source (12×). The JDK's default limits (`jdk.xml.maxElementDepth` = 100, `maxGeneralEntitySizeLimit`) had to be lifted: srcML nests deeper than 100 levels |
| srcML as one process per file | about +10 ms per file | 100 files: 3.94 s per-file vs 2.99 s batched |

Single-threaded, srcML is about **15× slower than raw CDT** and **3.2× slower than RefactoringMiner's full CDT pipeline**. CDT can also be parallelized per file. srcML's 12-thread wall time was not compared against a parallel CDT run.

## 8. Integration, distribution and licensing

| | CDT | srcML |
|---|---|---|
| Delivery | Maven artifact (`io.joern:eclipse-cdt-core`, 5.9 MB jar), pure Java | Native executable and shared library per OS and architecture. 1.2.0 ships packages for macOS (arm64, x86_64), Windows (x86_64), Ubuntu, Fedora and openSUSE |
| Use from Java | In-process API | External process (spawn per file or batch, XML over pipes or files) or JNI to `libsrcml`, which has no Java binding |
| Output to consume | Typed AST objects | XML, which must then be parsed and turned into RefactoringMiner's model and GumTree trees |
| License | **EPL-2.0** (weak copyleft; distributing it unmodified alongside MIT code is routine) | **GPL-3.0-only** (`COPYING.txt`, SPDX headers). Linking `libsrcml` would put RefactoringMiner's distribution under GPL terms. Shipping the `srcml` executable next to an MIT tool is usually treated as "aggregation", but the binary's GPL obligations still apply |
| Project activity | Quarterly releases (CDT 12.2 to 12.6, Sep 2025 to Sep 2026); also used by Joern for C/C++ | Active (1.2.0 released 2026-10-28, commits in Nov 2026); releases are infrequent (1.0.0 in 2020, 1.1.0 in 2025, 1.2.0 in 2026) |
| Investment already in RefactoringMiner | about 9,350 lines on CDT (`CppFileProcessor`, `CppPreprocessor`, `CppConditionalBodies`, `CppVisitor`, `CppOperationBody`, `CppTreeGenerator`), 5 stub headers, all C++ test expectations | None |

The last row is a switching cost, not a technical merit, and it is listed separately for that reason. The conclusion in section 11 doesn't depend on it.

## 9. Reproducing the experiments

- **srcML:**
  - Install: official release packages (`srcml-1.2.0-macOS-arm64.tar.gz` and others).
  - Or build: `git clone --branch v1.2.0 https://github.com/srcML/srcML`, then `cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=OFF`, then `ninja`. cmake and ninja can come from `pip install cmake ninja`. On macOS, libxml2, libxslt, libarchive and curl come from the SDK.
  - Markup: `srcml -l C++ [--position --tabs 1] FILE`.
  - Corpus: `srcml --files-from LIST -l C++ -j 1 --position --tabs 1 -o corpus.xml`.
  - Round-trip check: `srcml -l C++ FILE | srcml -S --src-encoding=UTF-8 | cmp - FILE`. Don't use `--to-dir` on archives with absolute file names; it writes to the original paths.
- **Raw CDT:** `GPPLanguage.getDefault().getASTTranslationUnit(FileContent.create(path, chars), new ScannerInfo(new HashMap<>(), new String[0]), IncludeFileContentProvider.getEmptyFilesProvider(), EmptyCIndex.INSTANCE, OPTION_IS_SOURCE_UNIT | OPTION_PARSE_INACTIVE_CODE, new DefaultLogService())`.
- **RM pipeline:** `CppFileProcessor.parse(path, content, false)` for the model, or `true` together with `CppTreeGenerator.generate(content, tu)` for the AST diff tree. Filter declarations from the forced includes with `CppFileProcessor.isPartOfFile`, because CDT marks forced includes as part of the translation unit.
- **Alignment:**
  - Convert srcML `pos:start`/`pos:end` (line:column, code points, inclusive end, `--tabs 1`) to UTF-16 offsets.
  - Match function bodies (`function`/`constructor`/`destructor` > `block` against `IASTFunctionDefinition.getBody()`), statements directly under blocks, and calls (`<call>` against `IASTFunctionCallExpression`) by exact `[start, end)` span.
  - Count statements only inside bodies found by both parsers.
- **Extract Variable demo (S10):** `GitHistoryRefactoringMinerImpl.diffAtDirectories(before, after)` on the two one-file directories.

## 10. Threats to validity

- **Corpus:** 14 sources, dominated by fallout2-ce, TensorFlow/XLA, Hyprland and duckdb. Qt-style or Windows-heavy code (export macros in class heads, `Q_OBJECT`) is absent, and there srcML's macro heuristics would score better than shown.
- **C files** were parsed as C++ by both tools. This matches RefactoringMiner today, but it understates CDT's C parser (S7) and may slightly understate srcML's C mode.
- **Span-based alignment** classifies boundary differences as one-sided items. We bucketed them explicitly ("same start or end") instead of counting them as errors, but some residual buckets ("other") were classified from samples rather than exhaustively.
- **Sampling:** the 400-case sample of declaration/expression disagreements comes from the order of the alignment output, not a random draw. The 2,440 count of the `auto &` pattern is an independent regex count over the files.
- **srcML macro registration** was tested only through the unexported library symbol. Correct usage might change the outcome for some macro categories, though not for the cases that need expansion.
- **CDT version:** this is the Joern-repackaged CDT 9.3.200 core RefactoringMiner already uses. Newer CDT releases may cover more C++20, and newer srcML may fix the `auto &` misparse.
- **Performance** was measured on one machine (Apple Silicon). Relative results are what matter.

## 11. Conclusion

For a tool whose output is a **fine-grained AST diff and refactoring detection**, the parser's job is to give a *structurally and semantically correct tree*. Robustness matters too, but it is secondary, because RefactoringMiner can repair what it can detect. On that criterion:

1. **Expression structure is non-negotiable.** RefactoringMiner's statement mappings, replacements and refactorings work on typed sub-expressions. GumTree maps subtrees. srcML's flat expressions remove about 63,700 such nodes from our corpus, and rebuilding them would mean writing a C++ expression parser over srcML tokens. That parser would face the same cast-vs-expression and template-vs-comparison ambiguities that srcML sidesteps.
2. **Disambiguation needs declarations.** CDT uses them when they are visible: in the file, from stubs, or from the forced-include declarations RefactoringMiner generates. srcML's output can't improve with more context.
3. **Silent misparses are worse than reported ones for this use.** CDT's problem nodes let RefactoringMiner target repairs, which cut problem text from 7.0% to 1.6%. srcML's misparses (e.g., `auto &x = e;` on 2,440 lines, and 572 unrecognized definitions with occasional cascades) would go unnoticed into refactoring results.
4. **Practicalities favor CDT:** it is 3.2–15× faster single-threaded, runs in-process in pure Java, and is EPL-2.0 rather than GPL-3.0-only.

srcML is excellent at what it was designed for: lightweight, robust, language-uniform markup that keeps the source text losslessly and supports querying and transforming source text at scale, including incomplete code and all preprocessor branches. Those strengths point to ideas RefactoringMiner can **borrow** without changing parsers:

- **Statement-level macro heuristics** for CDT problem shapes srcML handles: `if MACRO (cond)`, string-literal macro concatenation (`"%" PRId64`), class-head export macros, `Q_OBJECT`-style member macros, and `ASSIGN_OR_RETURN(<declaration>, expr)` in the model, not only in the tree.
- **The C parser for `.c` files** (`GCCLanguage`) instead of the C++ parser (K&R definitions, `new` as an identifier).
- **Stubs for common attribute macros** that still cause CDT problems, e.g., bitcoin's `EXCLUSIVE_LOCKS_REQUIRED`.
- **Lessons from srcML's misparses** about inputs that need care in any heuristic: templates in comparisons, `auto &`, and macros swallowing headers.

**Recommendation: keep Eclipse CDT** as RefactoringMiner's C/C++ parser, and keep investing in the repair layer (stubs, targeted re-parses, conditional bodies). That layer already brings in srcML's main strengths, robustness and preprocessor-branch coverage, while keeping CDT's structural and semantic precision.
