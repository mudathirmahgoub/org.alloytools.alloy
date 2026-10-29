# Alloy on cvc5 sets: review and benchmark results

Review of the `cvc5` branch against `master`, the fixes made, and how the cvc5
translation (`alloy2smt`) performs against Kodkod. Measured with cvc5 1.4.1 and
Kodkod with native MiniSat, October 2026.

The branch did not build, and its translator produced unsound SMT in several
common cases. After the fixes, no cvc5 answer contradicts Kodkod on the 196
commands of the Alloy example and book models. Bounded, cvc5 solves at most 94
of them within 60 seconds, against 191 for Kodkod; unbounded, it solves 98 and
proves 40 checks for every scope. Translating Alloy quantifiers with
`set.all`/`set.some` everywhere was the slowest of the three encodings. Integer
arithmetic keeps the CVC4 semantics, which differ from Alloy's.

## Setup

- **Models**: the 79 non-temporal models in
  `org.alloytools.alloy.extra/extra/models` (book chapters and appendices,
  examples), 196 commands.
- **Kodkod**: `TranslateAlloyToKodkod` with native MiniSat, default options
  (integer overflow wraps around). Time covers translation and SAT solving
  inside the JVM, not parsing.
- **cvc5**: each command translated into a standalone SMT-LIB script
  (`edu.uiowa.alloy2smt.Main -i model.als -d out [-s] -q <encoding>`), solved by
  one cvc5 process with `--finite-model-find`; wall-clock time of the process.
- **Timeout**: 60 seconds per command. 14 runs in parallel on an Apple M-series
  machine with 18 cores.
- **Encodings of Alloy quantifiers** (`-q`):
  - `hybrid` (now the default): `forall`/`exists`, and `set.filter` for
    comprehensions and for the quantifiers `one` and `lone`.
  - `fo`: `forall`/`exists` only.
  - `set`: the bounded set quantifiers `set.all`/`set.some`, and `set.filter`.

### Bounded and unbounded

- **Bounded** (`-s`, GUI option *cvc5 include scope*, now on by default): every
  signature is constrained by the scope of the command, computed by Alloy's
  `ScopeComputer` (through the new public class `SigScopes`): `S = {x1, …, xn}`
  for an exact scope n, `S ⊆ {x1, …, xn}` otherwise. Each problem is then
  equivalent to the one Kodkod solves, apart from integers (see below), so the
  answers can be compared one to one.
- **Unbounded** (no `-s`): no scope constraints. `unsat` means there is no
  instance of any size; `sat` may be an instance larger than the scope.

The old encoding supported both. Its GUI option *CVC4 include scope* was off by
default, so commands in the editor ran **unbounded**; its test suite
(`BookExamplesTests` and others) ran **bounded**. The integer bitwidth
(`for 4 Int`) is ignored in both modes: integers are unbounded.

## Results: bounded and unbounded

Commands solved out of 196 within 60 s.

| Solver | Encoding | Bounded: solved | sat / unsat | timeout | unsupported | Unbounded: solved | sat / unsat | timeout | unsupported |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Kodkod | | **191** | 86 / 105 | 0 | 5 (model type errors) | n/a | | | |
| cvc5 | hybrid | **94** | 45 / 49 | 78 | 24 | **98** | 54 / 44 | 74 | 24 |
| cvc5 | forall/exists | **90** | 43 / 47 | 82 | 24 | **95** | 54 / 41 | 77 | 24 |
| cvc5 | set.all/set.some | **69** | 31 / 38 | 103 | 24 | **62** | 33 / 29 | 110 | 24 |

Timeouts include `unknown` answers. Kodkod's 5 failures are type errors in the
models themselves (`s_ringlead.als`, `ins.als`).

Bounded runs, solved within a given time:

| Solver | ≤ 0.1 s | ≤ 1 s | ≤ 10 s | ≤ 60 s |
|---|---:|---:|---:|---:|
| Kodkod | 24 | 180 | 188 | 191 |
| cvc5 hybrid | 50 | 77 | 92 | 94 |
| cvc5 forall/exists | 50 | 75 | 90 | 90 |
| cvc5 set.all/set.some | 39 | 55 | 68 | 69 |

### Agreement with Kodkod

- **Bounded**: 0 disagreements. Every `sat`/`unsat` of every cvc5 encoding
  matches Kodkod.
- **Unbounded**: answers may differ from Kodkod's, but only in one direction.
  An unbounded `unsat` must be `unsat` in the scope, and a Kodkod instance is an
  unbounded instance. Both hold for every command and every encoding.

Unbounded cvc5 answers against Kodkod's answers in the scope:

| Encoding | Kodkod sat, unbounded sat | Kodkod unsat, unbounded unsat (valid for every scope) | Kodkod unsat, unbounded sat (instance larger than the scope) | Kodkod sat, unbounded unsat (would be a bug) | No answer unbounded |
|---|---:|---:|---:|---:|---:|
| hybrid | 49 | 44 | 5 | 0 | 98 |
| forall/exists | 49 | 41 | 5 | 0 | 101 |
| set.all/set.some | 32 | 29 | 1 | 0 | 134 |

- **Unbounded solves slightly more than bounded** with `forall`/`exists`
  (95 against 90) and hybrid (98 against 94). With hybrid, 86 commands are
  answered in both modes, 12 only unbounded (among them `firewire`,
  `ringElection1` and `2`, `com` Theorems 2 and 4a, `tube`, `opt_spantree`) and
  8 only bounded. With `set.all`/`set.some`, unbounded is worse (62 against 69).
- **Checks proved for every scope**, which Kodkod cannot do: 40 with hybrid,
  37 with `forall`/`exists`, 25 with `set.all`/`set.some`. Examples:
  `delUndoesAdd` and `addIdempotent` of the address books, `NoSelfFather` of
  the grandpa models, the memory checks of chapter 6, and Theorems 1 to 4b of
  `com.als`.
- **Instances larger than the scope** behave as the models intend.
  `CeilingsAndFloors.als` has `check BelowToo" for 2 expect 0` followed by
  `check BelowToo" for 3 expect 1`, and unbounded cvc5 finds that
  counterexample. The others are `ringElection2` (`AtLeastOneElected for 3
  Process, 7 Time` and `looplessPath for 3 Process, 13 Time`) and two
  `NoRepeats` runs of `firewire`.

### By model family (bounded)

| Family | Commands | Kodkod | hybrid | forall/exists | set.all/some | hybrid unbounded |
|---|---:|---:|---:|---:|---:|---:|
| book/appendixA | 14 | 14 | 13 | 13 | 13 | 14 |
| book/appendixE (hotel) | 6 | 6 | 0 | 0 | 0 | 0 |
| book/chapter2 | 35 | 35 | 31 | 31 | 28 | 28 |
| book/chapter4 | 12 | 12 | 12 | 12 | 10 | 12 |
| book/chapter5 | 5 | 5 | 3 | 3 | 4 | 2 |
| book/chapter6 | 30 | 30 | 16 | 14 | 9 | 20 |
| examples/algorithms | 32 | 28 | 2 | 2 | 0 | 3 |
| examples/case_studies | 37 | 36 | 9 | 7 | 0 | 13 |
| examples/puzzles | 4 | 4 | 0 | 0 | 0 | 0 |
| examples/systems | 12 | 12 | 3 | 3 | 2 | 2 |
| examples/toys | 7 | 7 | 5 | 5 | 3 | 4 |
| examples/tutorial | 2 | 2 | 0 | 0 | 0 | 0 |
| **Total** | **196** | **191** | **94** | **90** | **69** | **98** |

The gap to Kodkod is mostly timeouts on trace models: `util/ordering` over time
with 3- and 4-ary relations indexed by time and overrides (`++`). The hotel
models of chapter 6 and appendix E, the algorithms and the case studies are of
this kind. The 24 unsupported commands compare two cardinalities under a
quantifier (`handshake`, `lists`, `ringlead`), use Alloy 4.1-style integer
arithmetic (`peterson`, `stable_ringlead`), or are the one unsupported command
of `ins`.

cvc5 is faster than Kodkod on 55 commands (bounded, best encoding), almost all
small ones where Kodkod's fixed translation cost of about 70 ms dominates.

## Integer arithmetic

The translation keeps the **CVC4 semantics, which differ from Alloy's**. Only
bugs were fixed; the semantics are the ones documented in `bin/guidelines.md`.

| | Alloy / Kodkod | cvc5 translation (as in CVC4) |
|---|---|---|
| Integers | two's complement with `bitwidth` bits, overflow wraps around | mathematical integers, unbounded |
| Representation | `Int` atoms -2^(b-1) … 2^(b-1)-1 | an uninterpreted sort `UninterpretedInt` with `value: UninterpretedInt → Int`, injective; a literal `n` is a constant `c` with `value(c) = n` |
| `plus[A, B]` (also `minus`, `mul`, `div`, `rem`) | `{sum(A) + sum(B)}` | `{x + y \| x ∈ A, y ∈ B}`, a fresh set with two ∀∃ axioms |
| `A < B` (also `=<`, `>`, `>=`) | `sum(A) < sum(B)` | `A = {x}`, `B = {y}` and `x < y` |
| `#e` | a bitwidth integer, wraps around | counted exactly; only compared with a constant, or two cardinalities `#A + k op #B + m` where the comparison is skolemized |
| `int e`, `Int[e]` | the sum of `e` | `e` itself |
| `univ`, `iden` | include the integer atoms | contain no integers |
| `sum`, `<<`, `>>`, `>>>` | supported | not supported |

On singletons without overflow the two agree. Examples (`for 4 Int` or
`for 5 Int`, bounded):

| Model | Kodkod | cvc5 | Why |
|---|---|---|---|
| `sig A, B, C in Int {}`<br>`fact { A = 1 + 2 and B = 4 + 5 and C = plus[A, B] }`<br>`run { 7 in C }` | unsat | sat | Kodkod: `C = {3 + 9} = {12}`; cvc5: `C = {5, 6, 7}` |
| same, `run { C = 12 }` | sat | unsat | same reason |
| `sig A, B in Int {}`<br>`fact { plus[A, B] = 6 and minus[A, B] = 2 and #A = 1 and #B = 1 }`<br>`run { A = 4 and B = 2 }` | sat | sat | singletons: same semantics |
| same, `run { not (A = 4 and B = 2) }` | sat | unsat | Kodkod also has `A = -4, B = -6` by wraparound (`-10 ≡ 6`) |
| `run { some x: Int \| x > 7 } for 4 Int` | unsat | sat | 7 is the largest 4-bit integer |
| `run { plus[7, 1] < 0 } for 4 Int` | sat | unsat | `7 + 1` wraps to -8 in Kodkod |
| `sig A, B in Int {}`<br>`fact { #A = 2 }`<br>`run { A > B }` | sat | unsat | Kodkod compares sums; cvc5 requires singletons |
| `sig A {}`<br>`run { #A > 7 } for 8 A, 4 Int` | unsat | sat | `#A = 8` wraps to -8 in Kodkod |

The integer fixes:
- One path of the arithmetic translation built an axiom with `set.member`
  where `and` was meant, which cvc5 rejects as ill-sorted. That path also left
  the variables of enclosing quantifiers free.
- `lone` and `some` integer signatures, and `one` on integer sets, produced
  ill-sorted SMT.
- Injectivity is now `∀x. inverse(value(x)) = x`, a single-variable
  quantifier, instead of `∀x, y. x ≠ y ⇒ value(x) ≠ value(y)`. It is asserted
  only when the model uses integers; the old encoding added it to every
  problem.

Few benchmark models use integer arithmetic, and those that do heavily are
unsupported, so these differences did not show up in the results.
Matching Alloy exactly is possible: `sum` can be expressed with cvc5's
`set.fold`, and wraparound with bit-vectors of the bitwidth.

## Correctness issues, with examples

The old translator cannot be run as it is: it calls
`CompModule.getModelName()`, which does not exist in the branch's core. Each
example is a small Alloy model, Kodkod's answer, the new translation's answer,
and the old encoding's answer. The old encoding is reproduced in cvc5 syntax,
by hand or by editing the new output the way the old code behaved. All answers
come from actual runs.

| # | Issue | Model | Kodkod | Old encoding | New |
|---|---|---|---|---|---|
| 1 | multiplicity arrows were global sets | `one sig S { r: A -> B }`<br>`fact { S.r in A -> one B }`<br>`assert f { S.r in A -> one B }`<br>`check f` | unsat | **sat** | unsat |
| 2 | ordering `lt` compared an element with itself | `open util/ordering[S]`<br>`check { all disj x, y: S \| lt[x, y] or lt[y, x] }` | unsat | **sat** | unsat |
| 3 | constant on the left of `#` | `check { 2 > #A implies lone A } for 5` | unsat | **sat** | unsat |
| 4 | assertions created by a command were not printed | `sig A { r: set A }`<br>`fact { no r }`<br>`run { some {a: A \| some a.r} }` | unsat | **sat** | unsat |
| 5 | scope 0 ignored | `run { some A } for 3 but 0 A` | unsat | **sat** | unsat |
| 6 | `sig A = B + C` translated as subset | `sig B, C in X {}`<br>`sig A = B + C {}`<br>`run { some B and no A }` | unsat | **sat** | unsat |
| 7 | `this` in a field bound replaced by the signature | `sig Node { succ: lone Node - this }`<br>`run { some succ }` | sat | **unsat** | sat |

Examples 1 to 6 are spurious counterexamples or wrong instances. Example 7
misses an existing instance; in a `check` this would report a false assertion
as valid.

1. **Multiplicity arrows.** Each occurrence of `A -> one B` became a fresh
   relation `M` with top-level ∀∃∀ assertions making it a total function, and
   `X in A -> one B` became `X ⊆ M`. These assertions hold positively whatever
   the context, so under a negation (every `check`) the formula only says that
   `X` is not contained in one particular function, which a different function
   satisfies:
   ```smt2
   (assert (isFunction M1))           ; A -> one B in the fact
   (assert (isFunction M2))           ; A -> one B in the assertion
   (assert (set.subset r M1))
   (assert (not (set.subset r M2)))   ; sat: r = M1 = {(a,b1)}, M2 = {(a,b2)}
   ```
   Inside a quantifier these assertions referred to unbound variables. The new
   translation follows Kodkod's `isIn` and is local to the formula:
   ```smt2
   ; S.r in A -> one B
   (and (set.subset (rel.join S r) (rel.product A B))
        (forall ((x Atom)) (=> (set.member (tuple x) A)
           (set.is_singleton (rel.join (set.singleton (tuple x)) (rel.join S r))))))
   ```
   Outside `in`, an arrow with multiplicities denotes the product, as in Kodkod.
   This replaced about 1,000 lines of the old `ExprBinaryTranslator`.
2. **Ordering.** `lt`, `gt`, `lte` and `gte` were defined as
   `∀e1 ∈ s1, e2 ∈ s2. m(e1) < m(e1)`, false whenever both arguments are
   nonempty. The definition also used ∀∀ where `util/ordering` defines
   `lt[e1, e2]` as `e1 in prevs[e2]` (∀x ∈ e1 ∃y ∈ e2).
3. **Cardinality.** `n op #A` was translated as if `#A op n` were written:
   `2 > #A` became `#A ≤ 3`, and `n < #A` became `#A ≤ n - 1`.
4. **Command translation.** `Translation.translateCommand` printed the new
   declarations of a command but not its new assertions. Comprehension and
   arithmetic axioms that appear only in a command were dropped. New
   `define-fun`s were printed as bodiless `declare-fun`s.
5. **Scope 0.** The scope constraint was only added when `scope >= 1`.
6. **Exact subset signatures.** `sig A = B + C` became `A ⊆ B ∪ C`.
7. **`this` in field bounds.** The bound was translated without `this` in
   scope, and the fallback replaced `this` with the whole signature:
   `Node - this` became `Node - Node`. Fields are now translated as Kodkod
   does: `all this: S | this.f in e` and `f ⊆ S × univ × …`.

Other bugs fixed:
- The facts of the root module were asserted twice, once in the shared script
  and once in each command: `Command.formula` already contains all reachable
  facts.
- `disj[a, b, c]` only made consecutive arguments disjoint, and compared
  non-unary relations with the empty unary relation.
- Set comprehensions in function calls used a global variable that attached
  their definitions to whichever `no`, `some` or `one` was translated next.
- `let` expressions threw on substitution, and printed their bindings in a
  random order.
- Every signature got a trivial quantified assertion
  `∀a, b. … ⇒ true` for `disj` field values.
- A function called before its definition was printed twice, which cvc5
  rejects.
- Latent: signature facts were kept in a map, one per signature. Alloy
  currently gives each signature a single appended fact.

Build and GUI:
- The merge deleted `.gradle-wrapper/gradle-wrapper.properties` and
  `org.alloytools.alloy.extra/bnd.bnd`, and added Gradle 5 build files (using
  `compile`, which Gradle 7 rejects). alloy2smt was not a bnd project. Now
  `./gradlew :alloy2smt:build :org.alloytools.alloy.dist:jar` succeeds with JDK
  17, and the distribution jar contains the translator.
- `SimpleGUI` did not compile: `task` was declared again inside nested blocks,
  and never assigned on the Kodkod path.
- JAXB, which Java 11 and later no longer ship, was used for the mapping and
  instance XML. It is replaced with the JDK's DOM API.
- The instance XML had no trace attributes, which Alloy 6's
  `A4SolutionReader` requires, so no cvc5 instance could be visualized.
- "Next instance" sent `(check-sat)` again without excluding the last model, so
  it returned the same instance. It now sends `(block-model :values)` first.
- Scripts with `push`/`pop` did not set `:incremental`, which cvc5 requires.
- The CVC4 binaries and `alloy_cvc4.jar` (65 MB) were removed from `bin/`.
  Alloy finds cvc5 through `-Dcvc5.path`, the `CVC5` environment variable, the
  `PATH`, or `bin/cvc5`.

## What happened with set.all and set.some

The plan was to translate `all x: S | F` into
`(set.all (lambda ((x (Tuple Atom))) F) S)` and `some` into `set.some`, with
independent declarations grouped into one quantifier over the product of their
ranges, and comprehensions into `set.filter`. cvc5 rewrites `set.all p S` into
`(= (set.filter p S) S)` and instantiates `p` only with members of `S`. Used
directly, this was the slowest encoding. Measured cases:

| Construct | Example | Measured | What the translation does now |
|---|---|---|---|
| `(not (set.some p S))` | `addressBook2b`: `no n: Name \| n in n.^addr` | timeout (20 s); as `(set.all (λx. ¬p) S)`: 0.10 s; with `(n, n) ∈ ^r` for `{n} ⊆ {n}.^r`: 0.07 s | pushes negations inward (negation normal form); simplifies membership in joins with singletons |
| `set.is_singleton` in a lambda | `(set.is_singleton A)` and `set.all` over A of `(set.is_singleton x.r)` | timeout (10 s) on a trivially satisfiable problem, with or without `--uf-lazy-ll`; fine outside lambdas and under `forall` | `S ≠ ∅ ∧ S × S ⊆ iden(S)` inside lambdas |
| `exists` inside a lambda | `addressBook1b`: `some b: Book \| #b.addr > 1` | witnesses inside `set.some`: timeout (20 s); forall/exists: 0.09 s; polarity-aware: 0.03 s | tracks polarity: where cvc5 skolemizes (positive `some`, negative `all`, not under a binder) it emits `exists`/`forall`; set quantifiers only in universal positions; predicates are inlined so their bodies get the polarity of the call |
| body creates members of its set | `addressBook3b`: ordering axiom `∀x ∈ S. m(x) < m(nm(x)) ∧ (x ≠ last ⇒ nm(x) ∈ S)` | timeout (10 s): each instantiation adds a member `nm(x)`; that axiom as `forall`: unsat in 2.9 s | `next = (set.filter (λ(x, y). m(x) < m(y) ∧ no z between) (S × S))`, no new terms |
| `set.card` | `(= (set.card A) 3)` with `--finite-model-find` | error: "The cardinality beth[0] of the finite type (Tuple Atom) is not supported yet" | cardinality stays as distinct witnesses; finite model finding is required (even `A ≠ ∅ ∧ A ⊆ r.A` does not terminate without it) |
| trivial `set.all` axioms | `addressBook3b` check 1: `Ord.First ⊆ Book`, `m(x) ≥ m(first)`, `m(x) ≤ m(last)` | timeout (10 s); any one of them as `forall`: unsat in about 0.9 s | hybrid mode uses `forall`/`exists` for these |
| `set.all` over a literal set | `S = {c0, c1, c2}`, then `set.all p S` | not grounded: cvc5 keeps `S` a variable and builds `(= S (set.filter @purify S))` | not used yet (next steps) |

The set operators do help as `set.filter` for comprehensions and for `one` and
`lone`, which otherwise need a global ∀-axiom or nested ∃∀. That is the hybrid
mode, which solves 4 bounded commands no other cvc5 encoding solves
(`checkCache` checks 1 and 3, `chord` commands 8 and 13).

The largest single gain came from symmetry breaking for `util/ordering`, which
Kodkod also does. With the scope included, the ordered signature has an exact
scope n and its atoms are interchangeable, so the order is fixed:
`S = {c0, …, cn-1}`, `next = {(c0, c1), …}`, `m(ci) = i`. On `addressBook3b`
check 2 this took forall/exists from a 30 s timeout to unsat in 0.10 s, and
the set encoding to 2.4 s. A first full run, before this change, the
cardinality comparisons and the hybrid mode, solved 84 bounded commands with
forall/exists and 63 with set.all/set.some.

## Next steps

- Ground quantifiers over the scope atoms, as Kodkod does: with the scope
  included, translate each command as a whole program, define each signature as
  a literal set of constants, and expand quantifiers over those constants.
  cvc5 does not substitute `S = literal` before rewriting, so the translator has
  to do it.
- In cvc5: handle `set.is_singleton`/`set.choose` inside lambda bodies, make
  `(not (set.some p S))` as fast as `(set.all (λx. ¬p) S)`, and support
  `set.card` on tuple sorts with finite model finding, which would make general
  cardinality expressions translatable.
- Integers: optionally match Alloy with `set.fold` for sums and bit-vectors for
  wraparound.
- Trace models: the remaining cost is in 3- and 4-ary relations indexed by time
  and in `++`.
- Not done: the GUI was not used interactively (the process, model parser and
  XML paths were tested directly). The alloy2smt JUnit tests compile but were not
  run: several expect the old unbounded behaviour, and some reference
  `./test/*.als` files that are not in the repository.
