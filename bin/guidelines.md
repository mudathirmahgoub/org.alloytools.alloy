# cvc5 relational solver

The cvc5 relational solver translates Alloy commands into SMT-LIB for the theory of finite
sets and relations of the SMT solver [cvc5](https://cvc5.github.io) (translator in `alloy2smt`).
Without the scope of commands, it can prove that a model is unsatisfiable for every scope.

## Installing cvc5

Download cvc5 (version 1.2 or later) from [here](https://cvc5.github.io/downloads.html).
Alloy looks for the binary in this order: the Java system property `cvc5.path`, the environment
variable `CVC5`, the `PATH`, and `bin/cvc5` in the working directory.

## Options

The solver is chosen in the Options menu with **Relational solver** (Kodkod or cvc5).

- **cvc5 timeout**: the time limit of each command (default 0.5 minutes).
- **cvc5 include scope**: whether signatures are bounded by the scope of commands, as with
  Kodkod (default yes). Without the scope, cvc5 may find instances larger than the scope, and
  `unsat` means that there is no instance of any size.
  The scope can also be written as cardinality constraints: `run {#A = 3}`.
- **cvc5 quantifiers**: the translation of Alloy quantifiers: `hybrid` (default: first-order
  `forall`/`exists`, and `set.filter` for comprehensions and the quantifiers `one` and `lone`),
  `forall/exists`, or the bounded set quantifiers `set.all`/`set.some` of cvc5.

## Translation from the command line

```
java -cp <alloy.jar> edu.uiowa.alloy2smt.Main -i model.als -d out -s -q hybrid
```
writes `out/model_<i>.smt2` for each command: `-s` includes the scope, `-q` chooses the
translation of quantifiers: `hybrid` (default), `fo` or `set`.

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
