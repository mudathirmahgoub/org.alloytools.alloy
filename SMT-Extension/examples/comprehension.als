sig A {}
sig A0, A1 in A {}
fun complement[x: A]: A {let z = {y : A | not (y in x)} | z}
fact{A0 = A1}
-- unsat
run {#A0 = 2 and #A1 = 2 and complement[A0] = A1}
-- sat
run {#A0 = 0 and #A1 = 0 and complement[A0] = A1}