package edu.uiowa.smt;

import edu.mit.csail.sdg.alloy4.Env;
import edu.uiowa.smt.smtAst.Expression;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Environment
{
    private Map<String, Expression> variablesMap = new LinkedHashMap<>();
    private Environment parent;
    // whether expressions translated in this environment end up in the body of a lambda
    private boolean insideLambda;

    public enum Polarity
    {
        POSITIVE, NEGATIVE, BOTH;

        public Polarity flip()
        {
            switch (this)
            {
                case POSITIVE: return NEGATIVE;
                case NEGATIVE: return POSITIVE;
                default: return BOTH;
            }
        }
    }

    // the polarity of the formula translated in this environment
    private Polarity polarity = Polarity.POSITIVE;
    // whether the formula is not in the scope of a quantifier or lambda that prevents skolemization
    private boolean skolemizable = true;

    // top level environment
    public Environment()
    {
        parent = null;
    }
    public Environment(Environment parent)
    {
        this.parent = parent;
        this.insideLambda = parent.insideLambda;
        this.polarity = parent.polarity;
        this.skolemizable = parent.skolemizable;
    }

    /**
     * @return a child environment for a sub formula with the given polarity
     */
    public Environment withPolarity(Polarity polarity)
    {
        Environment environment = new Environment(this);
        environment.polarity = polarity;
        if (polarity == Polarity.BOTH)
        {
            environment.skolemizable = false;
        }
        return environment;
    }

    public Environment flipped()
    {
        return withPolarity(polarity.flip());
    }

    public Polarity getPolarity()
    {
        return polarity;
    }

    public boolean isSkolemizable()
    {
        return skolemizable;
    }

    public void setSkolemizable(boolean skolemizable)
    {
        this.skolemizable = skolemizable;
    }

    /**
     * @return whether an existential quantifier here is skolemized by the solver
     */
    public boolean existsIsSkolemized()
    {
        return skolemizable && !insideLambda && polarity == Polarity.POSITIVE;
    }

    /**
     * @return whether a universal quantifier here is skolemized by the solver (its negation is)
     */
    public boolean forallIsSkolemized()
    {
        return skolemizable && !insideLambda && polarity == Polarity.NEGATIVE;
    }

    public boolean isInsideLambda()
    {
        return insideLambda;
    }

    public void setInsideLambda(boolean insideLambda)
    {
        this.insideLambda = insideLambda;
        if (insideLambda)
        {
            this.skolemizable = false;
        }
    }
    public void put(String key, Expression value)
    {
        variablesMap.put(key, value);
    }

    public Expression get(String key)
    {
        Environment currentEnvironment = this;
        while(currentEnvironment != null)
        {
            Expression value = currentEnvironment.variablesMap.get(key);
            if(value != null)
            {
                return value;
            }
            currentEnvironment = currentEnvironment.parent;
        }
        throw new RuntimeException(String.format("Can not find the variable %s in the environment", key));
    }

    public boolean containsKey(String key)
    {
        Environment currentEnvironment = this;
        while(currentEnvironment != null)
        {
            if(currentEnvironment.variablesMap.containsKey(key))
            {
                return true;
            }
            currentEnvironment = currentEnvironment.parent;
        }
        return false;
    }

    public Environment getParent()
    {
        return parent;
    }

    public LinkedHashMap<String, Expression> getVariables()
    {
        return getVariablesAuxiliary(this);
    }

    private LinkedHashMap<String, Expression> getVariablesAuxiliary(Environment environment)
    {
        if(environment.parent == null)
        {
            return new LinkedHashMap<>(environment.variablesMap);
        }

        LinkedHashMap<String, Expression> map = getVariablesAuxiliary(environment.parent);
        map.putAll(environment.variablesMap);
        return map;
    }
}
