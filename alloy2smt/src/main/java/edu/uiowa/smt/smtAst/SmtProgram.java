/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import java.util.ArrayList;
import java.util.List;

public class SmtProgram extends SmtModel
{
    private final List<ConstantDeclaration>    constantDeclarations = new ArrayList<>();
    private final List<Assertion>              assertions           = new ArrayList<>();
    private final List<SolverOption>           options              = new ArrayList<>();
    private String                             logic                = "ALL";

    public SmtProgram()
    {
    }

    public SmtProgram(SmtProgram program)
    {
        super(program);
        this.constantDeclarations.addAll(program.constantDeclarations);
        this.assertions.addAll(program.assertions);
        this.options.addAll(program.options);
        this.logic = program.logic;
    }

    public String getLogic()
    {
        return logic;
    }

    public void setLogic(String logic)
    {
        this.logic = logic;
    }

    public List<SolverOption> getOptions()
    {
        return options;
    }

    public void addOption(String name, String value)
    {
        options.removeIf(option -> option.name.equals(name));
        options.add(new SolverOption(name, value));
    }

    public void addConstantDeclaration(ConstantDeclaration constantDeclaration)
    {
        if(constantDeclaration != null)
        {
            this.constantDeclarations.add(constantDeclaration);
        }
    }

    public void addAssertion(Assertion assertion)
    {
        if(assertion != null)
        {
            this.assertions.add(assertion);
        }
    }

    public List<ConstantDeclaration> getConstantDeclarations()
    {
        return this.constantDeclarations;
    }

    public List<Assertion> getAssertions()
    {
        return this.assertions;
    }
}
