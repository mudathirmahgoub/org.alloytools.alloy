/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.printers.SmtAstVisitor;

import java.util.LinkedHashMap;
import java.util.Map;

public class LetExpression extends Expression
{
    private final Expression expr;
    private final Map<String, Expression> letVariables;

    public LetExpression(Map<String, Expression> letVars, Expression expr)
    {
        this.letVariables = new LinkedHashMap<>();
        this.expr = expr;
        for (Map.Entry<String, Expression> var : letVars.entrySet())
        {
            this.letVariables.put(var.getKey(), var.getValue());
        }
    }


    public Map<String, Expression> getLetVariables()
    {
        return this.letVariables;
    }

    public Expression getExpression()
    {
        return this.expr;
    }

    @Override
    public void accept(SmtAstVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public Sort getSort()
    {
        return expr.getSort();
    }

    @Override
    public Expression evaluate(Map<String, FunctionDefinition> functions)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object object)
    {
        if(object == this)
        {
            return true;
        }
        if(!(object instanceof LetExpression))
        {
            return false;
        }
        LetExpression let = (LetExpression) object;
        return letVariables.equals(let.letVariables) && expr.equals(let.expr);
    }

    private boolean binds(Expression expression)
    {
        return expression instanceof Variable && letVariables.containsKey(((Variable) expression).getName());
    }

    @Override
    public Expression substitute(Variable oldVariable, Variable newVariable)
    {
        Map<String, Expression> newVariables = new LinkedHashMap<>();
        for (Map.Entry<String, Expression> entry : letVariables.entrySet())
        {
            newVariables.put(entry.getKey(), entry.getValue().substitute(oldVariable, newVariable));
        }
        // the let variables are bound in the body (parallel let)
        Expression body = binds(oldVariable) ? expr : expr.substitute(oldVariable, newVariable);
        return new LetExpression(newVariables, body);
    }

    @Override
    public Expression replace(Expression oldExpression, Expression newExpression)
    {
        if(oldExpression.equals(this))
        {
            return newExpression;
        }
        Map<String, Expression> newVariables = new LinkedHashMap<>();
        for (Map.Entry<String, Expression> entry : letVariables.entrySet())
        {
            newVariables.put(entry.getKey(), entry.getValue().replace(oldExpression, newExpression));
        }
        Expression body = binds(oldExpression) ? expr : expr.replace(oldExpression, newExpression);
        return new LetExpression(newVariables, body);
    }
}
