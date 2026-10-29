/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.translators;

public class TranslatorOptions
{
    public enum Quantifiers
    {
        /**
         * Alloy quantifiers are translated into first-order forall/exists over atoms
         * and solved with finite model finding.
         */
        FIRST_ORDER,
        /**
         * Alloy quantifiers whose variables range over sets are translated into the
         * bounded set quantifiers set.all / set.some and comprehensions into
         * set.filter. The solver instantiates their predicates only with elements
         * of the bounding sets, so no first-order quantifier is needed for them.
         */
        SET,
        /**
         * First-order forall/exists for the quantifiers all, some and no, and
         * set.filter for comprehensions and the quantifiers one and lone.
         */
        HYBRID
    }

    // the best translation on the Alloy example models without finite model finding (the default);
    // with finite model finding, HYBRID is the best
    public Quantifiers quantifiers = Quantifiers.SET;

    /**
     * Whether cvc5 uses finite model finding (--finite-model-find). It searches models of
     * increasing size for the uninterpreted sorts, which most satisfiable relational
     * problems need to terminate.
     */
    public boolean finiteModelFinding = false;

    public TranslatorOptions()
    {
    }

    public TranslatorOptions(Quantifiers quantifiers)
    {
        this.quantifiers = quantifiers;
    }

    public boolean useSetQuantifiers()
    {
        return quantifiers == Quantifiers.SET;
    }

    /**
     * @return whether comprehensions and the quantifiers one and lone are translated with set.filter
     */
    public boolean useFilter()
    {
        return quantifiers == Quantifiers.SET || quantifiers == Quantifiers.HYBRID;
    }
}
