# Alloy2smt

Alloy2smt translates Alloy models into SMT-LIB for the theory of finite sets and relations of
[cvc5](https://cvc5.github.io). It is a project of the Alloy bnd workspace and is built with the
other projects (`./gradlew build`).

Each command is translated into a standalone script:
```
java -cp <alloy.jar> edu.uiowa.alloy2smt.Main -i examples/test.als -d out -s
```
- `-s`: bound the signatures by the scope of the command, as Kodkod does
- `-q hybrid|fo|set`: translate Alloy quantifiers with `forall`/`exists` and `set.filter`
  for comprehensions and `one`/`lone` quantifiers (default), with first-order
  `forall`/`exists` only, or with the bounded set quantifiers `set.all`/`set.some`.

The tests (`src/test/java`) run the cvc5 binary (see `bin/guidelines.md`) on the example
models; they are not run by the build.
