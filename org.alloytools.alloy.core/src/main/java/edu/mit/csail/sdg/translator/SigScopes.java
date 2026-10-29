package edu.mit.csail.sdg.translator;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;

/**
 * The scope of each signature of a command, computed with the same rules as the
 * Kodkod translation. This is for solvers that encode the scope themselves.
 */
public final class SigScopes {

    private final ScopeComputer scopeComputer;

    private SigScopes(ScopeComputer scopeComputer) {
        this.scopeComputer = scopeComputer;
    }

    public static SigScopes compute(A4Reporter rep, Iterable<Sig> sigs, Command cmd) throws Err {
        return new SigScopes(ScopeComputer.compute(rep == null ? A4Reporter.NOP : rep, new A4Options(), sigs, cmd).b);
    }

    /** Returns the scope of the sig, or -1 if the sig has no scope of its own. */
    public int scope(Sig sig) {
        return scopeComputer.sig2scope(sig);
    }

    /** Returns whether the scope of the sig is exact. */
    public boolean isExact(Sig sig) {
        return scopeComputer.isExact(sig);
    }
}
