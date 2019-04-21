/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.smt.smtAst;

import edu.uiowa.alloy2smt.smt.printers.SmtAstVisitor;

public abstract class SmtAst
{
    public abstract void accept(SmtAstVisitor visitor);
}