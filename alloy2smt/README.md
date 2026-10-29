# Alloy2smt

Alloy2smt translates Alloy models into SMT-LIB for the theory of finite sets and relations of
[cvc5](https://cvc5.github.io). It is a project of the Alloy bnd workspace and is built with the
other projects (`./gradlew build`).

Each command is translated into a standalone script:
```
java -cp <alloy.jar> edu.uiowa.alloy2smt.Main -i examples/test.als -d out -s
```
- `-s`: bound the signatures by the scope of the command, as Kodkod does (default unbounded)
- `-f`: enable the finite model finding of cvc5 (default disabled)
- `-q set|hybrid|fo`: translate Alloy quantifiers with the bounded set quantifiers
  `set.all`/`set.some` (default, the best without `-f`), with `forall`/`exists` and
  `set.filter` for comprehensions and `one`/`lone` quantifiers (the best with `-f`), or with
  first-order `forall`/`exists` only.

`./gradlew :alloy2smt:fetchCvc5` downloads the cvc5 binaries that the jar bundles (see
`bin/guidelines.md`).

The tests (`src/test/java`) run the cvc5 binary (see `bin/guidelines.md`) on the example
models; they are not run by the build.
