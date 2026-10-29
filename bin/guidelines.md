# cvc5 relational solver

The cvc5 relational solver translates Alloy commands into SMT-LIB for the theory of finite
sets and relations of the SMT solver [cvc5](https://cvc5.github.io) (translator in `alloy2smt`).
Without the scope of commands, it can prove that a model is unsatisfiable for every scope.

## Installing cvc5

The Alloy jar of the cvc5 branch bundles the cvc5 1.4.2 binaries for Linux, macOS and
Windows on x86_64 and arm64, from the [cvc5 releases](https://github.com/cvc5/cvc5/releases)
(the static builds without GPL code). The binary for the current platform is extracted into
the temporary directory the first time cvc5 runs. To build such a jar, run
`./gradlew :alloy2smt:fetchCvc5` before building.

Alloy looks for the binary in this order: the Java system property `cvc5.path`, the
environment variable `CVC5`, the bundled binary, the `PATH`, and `bin/cvc5` in the working
directory. Another cvc5 (version 1.2 or later) can be downloaded from
[here](https://cvc5.github.io/downloads.html).

## Options

The solver is chosen in the Options menu with **Relational solver** (Kodkod or cvc5).

- **cvc5 timeout**: the time limit of each command (default 1 minute).
- **cvc5 include scope**: whether signatures are bounded by the scope of commands, as with
  Kodkod (default no). Without the scope (unbounded), cvc5 may find instances larger than the
  scope, and `unsat` means that there is no instance of any size.
  The scope can also be written as cardinality constraints: `run {#A = 3}`.
- **cvc5 quantifiers**: the translation of Alloy quantifiers: the bounded set quantifiers
  `set.all`/`set.some` of cvc5 (default, the best without finite model finding), `hybrid`
  (first-order `forall`/`exists`, and `set.filter` for comprehensions and the quantifiers `one`
  and `lone`; the best with finite model finding), or `forall/exists`.
- **cvc5 finite model finding**: whether cvc5 searches models of increasing size for the
  atoms (`--finite-model-find`, default no). See `CVC5_RESULTS.md` for its effect.

## Generated files

When cvc5 executes a command, the message panel lists the files it generates, as links:
the SMT-LIB script of all the commands and the mapping from signatures and fields to SMT
symbols (`.mapping.json`) open in an editor tab, and the instance file (`.smt.xml`) opens in
the visualizer.

## Translation from the command line

```
java -cp <alloy.jar> edu.uiowa.alloy2smt.Main -i model.als -d out -s -q set
```
writes `out/model_<i>.smt2` for each command: `-s` includes the scope (default unbounded),
`-f` enables finite model finding (default disabled), and `-q` chooses the translation of
quantifiers: `set` (default), `hybrid` or `fo`.

# Integer signatures
## Semantics of functions plus, minus, mul, div, rem
 The cvc5 relational solver interprets integer signatures as sets of mathematical integers. It uses different semantics for functions plus, minus, mul, div, and rem as follows:

- plus[A, B] = {z | ∃ x ∈ A, y ∈ B. x + y = z}
- minus[A, B] = {z | ∃ x ∈ A, y ∈ B. x - y = z}
- mul[A, B] = {z | ∃ x ∈ A, y ∈ B. x * y = z}
- div[A, B] = {z | ∃ x ∈ A, y ∈ B. x / y = z}
- rem[A, B] = {z | ∃ x ∈ A, y ∈ B. x mod y = z}

 **Example**
 ```cmd
 sig A, B, C in Int {}
 fact {
     A = 1 + 2
     B = 4 + 5
     C = plus[A, B]
 }
run {} for 5 Int, 12 seq
```
The result returned from the cvc5 relational solver is
```cmd
this/A={1, 2}
this/B={4, 5}
this/C={5, 6, 7}
```
In this example `C = plus[A, B]` acts like there exists a relation `plus={1->4->5, 1->5->6, 2->4->6, 2->5->7}` where `C = B.(A.plus)`.
Compare this with the result returned from Kodkod solver which interprets an integer signature as the sum of its elements.
```cmd
this/A={1, 2}
this/B={4, 5}
this/C={12}
```
When the operands are singletons, the semantics of the cvc5 relational solver is similar to the Kodkod solver (modulo bitwidth).
To avoid performance issues, it is recommended to use only singletons for integer signatures by restricting their cardinality to be 1.

## Comparisons
Semantics for comparison operators: <, =<, >, >=  is based on singletons as follows:
- A =< B ≡ ∃ x, y ∈ Z. A = {x} and B = {y} and x <= y
- A < B ≡ ∃ x, y ∈ Z. A = {x} and B = {y} and x < y
- A >= B ≡ ∃ x, y ∈ Z. A = {x} and B = {y} and x >= y
- A > B ≡ ∃ x, y ∈ Z. A = {x} and B = {y} and x > y

This is different than Kodkod semantics which compares between the sum of the two operands.

# Unsupported features
Currently the following Alloy features are not supported by the cvc5 solver:
- Cardinality in integer expressions. `#e` is supported when it is compared with a constant
  (`#e = 3`, `#e > 2`, `4 >= #e`), and the comparison `#A + k op #B + m` of two unary sets
  is supported where its existential is skolemized (e.g. in facts, not under `all`).
- univ, iden do not include integers, and there is no subtyping between normal signatures and integer signatures.
- Recursive functions, sequences, strings, temporal operators, sum, and the bit shift operators.
- Quantifiers and comprehensions inside predicates and functions whose arguments are not singletons
  are only translated exactly with set quantifiers.
