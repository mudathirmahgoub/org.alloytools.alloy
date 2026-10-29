/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.printers.SmtAstVisitor;

import java.util.Map;

/**
 * A unary predicate (lambda ((x T)) body) used as the first argument of the
 * higher-order set operators set.all, set.some and set.filter.
 */
public class LambdaExpression extends Expression
{
    private final VariableDeclaration parameter;
    private final Expression body;

    public LambdaExpression(VariableDeclaration parameter, Expression body)
    {
        this.parameter = parameter;
        this.body = body;
        checkTypes();
    }

    @Override
    protected void checkTypes()
    {
        if (body.getSort() != BoolSort.getInstance())
        {
            throw new RuntimeException(String.format("The body '%1$s' of the lambda expression is not boolean", body));
        }
    }

    public VariableDeclaration getParameter()
    {
        return parameter;
    }

    public Expression getBody()
    {
        return body;
    }

    @Override
    public void accept(SmtAstVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * @return the sort of the parameter. A lambda is only used as a predicate
     * over the elements of a set, so its parameter sort is what type checking needs.
     */
    @Override
    public Sort getSort()
    {
        return parameter.getSort();
    }

    @Override
    public Expression evaluate(Map<String, FunctionDefinition> functions)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this)
        {
            return true;
        }
        if (!(object instanceof LambdaExpression))
        {
            return false;
        }
        LambdaExpression lambda = (LambdaExpression) object;
        return parameter.equals(lambda.parameter) && body.equals(lambda.body);
    }

    @Override
    public Expression substitute(Variable oldVariable, Variable newVariable)
    {
        if (parameter.getVariable().equals(oldVariable))
        {
            return this; // oldVariable is bound here
        }
        return new LambdaExpression(parameter, body.substitute(oldVariable, newVariable));
    }

    @Override
    public Expression replace(Expression oldExpression, Expression newExpression)
    {
        if (oldExpression.equals(this))
        {
            return newExpression;
        }
        if (parameter.getVariable().equals(oldExpression))
        {
            return this; // oldExpression is bound here
        }
        return new LambdaExpression(parameter, body.replace(oldExpression, newExpression));
    }
}
