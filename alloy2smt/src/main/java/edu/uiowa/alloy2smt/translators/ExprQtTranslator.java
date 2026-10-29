package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.ast.*;
import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.smt.Environment;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;

import java.util.*;
import java.util.stream.Collectors;

public class ExprQtTranslator
{
    final ExprTranslator exprTranslator;
    final ExprUnaryTranslator exprUnaryTranslator;
    final ExprBinaryTranslator exprBinaryTranslator;
    final Alloy2SmtTranslator translator;

    public ExprQtTranslator(ExprTranslator exprTranslator)
    {
        this.exprTranslator = exprTranslator;
        this.exprUnaryTranslator = exprTranslator.exprUnaryTranslator;
        this.exprBinaryTranslator = exprTranslator.exprBinaryTranslator;
        this.translator = exprTranslator.translator;
    }

    // whether the solver skolemizes the quantifier: an existential in a positive position or a universal in a negative position
    private static boolean isSkolemized(ExprQt exprQt, Environment environment)
    {
        switch (exprQt.op)
        {
            case SOME: return environment.existsIsSkolemized();
            case ALL:
            case NO: return environment.forallIsSkolemized();
            default: return false;
        }
    }

    Expression translateExprQt(ExprQt exprQt, Environment environment)
    {
        // a skolemized quantifier is cheaper than a set quantifier: the solver replaces it with constants
        if (translator.options.useSetQuantifiers() && hasOnlyOneOfDeclarations(exprQt) && !isSkolemized(exprQt, environment))
        {
            List<List<Decl>> groups = groupIndependentDeclarations(exprQt);
            switch (exprQt.op)
            {
                case ALL: return translateSetQuantifier(true, exprQt, groups, 0, environment);
                case SOME: return translateSetQuantifier(false, exprQt, groups, 0, environment);
                // no x | f is all x | not f: (not (set.some ...)) is much harder for the solver
                case NO: return exprTranslator.translateExpr(ExprQt.Op.ALL.make(exprQt.pos, null, exprQt.decls, exprQt.sub.not()), environment);
                case ONE:
                case LONE:
                case COMPREHENSION:
                {
                    if (groups.size() == 1)
                    {
                        Expression filter = translateFilter(exprQt, groups.get(0), environment);
                        switch (exprQt.op)
                        {
                            case ONE: return exprTranslator.mkOne(filter, environment);
                            case LONE: return exprTranslator.mkLone(filter, environment);
                            default: return filter;
                        }
                    }
                }
                break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        if (translator.options.useFilter() && hasOnlyOneOfDeclarations(exprQt) &&
            (exprQt.op == ExprQt.Op.ONE || exprQt.op == ExprQt.Op.LONE || exprQt.op == ExprQt.Op.COMPREHENSION))
        {
            List<List<Decl>> groups = groupIndependentDeclarations(exprQt);
            if (groups.size() == 1)
            {
                Expression filter = translateFilter(exprQt, groups.get(0), environment);
                switch (exprQt.op)
                {
                    case ONE: return exprTranslator.mkOne(filter, environment);
                    case LONE: return exprTranslator.mkLone(filter, environment);
                    default: return filter;
                }
            }
        }
        return translateFirstOrderQuantifier(exprQt, environment);
    }

    /*
     * Translation with the bounded set quantifiers set.all, set.some and set.filter.
     */

    // x: e where x ranges over the tuples of e
    private static boolean isOneOf(Expr expr)
    {
        if (expr instanceof Sig)
        {
            return true;
        }
        if (expr instanceof ExprUnary)
        {
            ExprUnary.Op op = ((ExprUnary) expr).op;
            return op == ExprUnary.Op.ONEOF || (op == ExprUnary.Op.NOOP && ((ExprUnary) expr).sub.type().arity() == 1);
        }
        return false;
    }

    private static boolean hasOnlyOneOfDeclarations(ExprQt exprQt)
    {
        return exprQt.decls.stream().allMatch(decl -> isOneOf(decl.expr));
    }

    /**
     * Groups consecutive declarations whose ranges do not mention the variables of the group.
     * Each group is quantified at once over the product of its ranges.
     */
    private static List<List<Decl>> groupIndependentDeclarations(ExprQt exprQt)
    {
        List<List<Decl>> groups = new ArrayList<>();
        List<Decl> group = new ArrayList<>();
        for (Decl decl : exprQt.decls)
        {
            boolean dependent = group.stream()
                                     .flatMap(d -> d.names.stream())
                                     .anyMatch(name -> decl.expr.hasVar((ExprVar) name));
            if (dependent)
            {
                groups.add(group);
                group = new ArrayList<>();
            }
            group.add(decl);
        }
        groups.add(group);
        return groups;
    }

    private Expression translateSetQuantifier(boolean all, ExprQt exprQt, List<List<Decl>> groups, int index, Environment environment)
    {
        if (index == groups.size())
        {
            return exprTranslator.translateExpr(exprQt.sub, environment);
        }
        List<Decl> group = groups.get(index);
        Expression range = groupRange(group, environment);
        return all ?
                exprTranslator.forAllIn(range, (tuple, env) ->
                        bindGroup(group, tuple, env, newEnv -> ExprTranslator.implies(
                                getDisjointConstraints(group, newEnv),
                                translateSetQuantifier(true, exprQt, groups, index + 1, newEnv))), environment) :
                exprTranslator.someIn(range, (tuple, env) ->
                        bindGroup(group, tuple, env, newEnv -> ExprTranslator.and(
                                getDisjointConstraints(group, newEnv),
                                translateSetQuantifier(false, exprQt, groups, index + 1, newEnv))), environment);
    }

    private Expression translateFilter(ExprQt exprQt, List<Decl> group, Environment environment)
    {
        Expression range = groupRange(group, environment);
        return exprTranslator.filter(range, (tuple, env) ->
                bindGroup(group, tuple, env, newEnv -> ExprTranslator.and(
                        getDisjointConstraints(group, newEnv),
                        exprTranslator.translateExpr(exprQt.sub, newEnv))), environment);
    }

    // the product of the ranges of the variables in the group
    private Expression groupRange(List<Decl> group, Environment environment)
    {
        Expression product = null;
        for (Decl decl : group)
        {
            Expression range = ExprTranslator.toSet(exprTranslator.translateExpr(decl.expr, environment));
            for (int i = 0; i < decl.names.size(); i++)
            {
                product = product == null ? range : BinaryExpression.Op.PRODUCT.make(product, range);
            }
        }
        return product;
    }

    /**
     * Binds each variable of the group to its components of the tuple and translates the body.
     */
    private Expression bindGroup(List<Decl> group, Expression tuple, Environment environment,
                                 java.util.function.Function<Environment, Expression> body)
    {
        Environment newEnvironment = new Environment(environment);
        List<String> names = group.stream().flatMap(d -> d.names.stream()).map(n -> n.label).collect(Collectors.toList());
        if (names.size() == 1)
        {
            newEnvironment.put(names.get(0), tuple);
            return body.apply(newEnvironment);
        }
        List<Integer> arities = group.stream()
                                     .flatMap(d -> d.names.stream().map(n -> d.expr.type().arity()))
                                     .collect(Collectors.toList());
        Map<String, Expression> bindings = new LinkedHashMap<>();
        int offset = 0;
        for (int i = 0; i < names.size(); i++)
        {
            List<Expression> components = new ArrayList<>();
            for (int j = 0; j < arities.get(i); j++)
            {
                components.add(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(offset++), tuple));
            }
            Expression value = MultiArityExpression.Op.MKTUPLE.make(components);
            VariableDeclaration variable = new VariableDeclaration(names.get(i), value.getSort(), null);
            bindings.put(names.get(i), value);
            newEnvironment.put(names.get(i), variable.getVariable());
        }
        return new LetExpression(bindings, body.apply(newEnvironment));
    }

    private Expression getDisjointConstraints(List<Decl> group, Environment environment)
    {
        Expression constraints = BoolConstant.True;
        for (Decl decl : group)
        {
            if (decl.disjoint != null && decl.names.size() > 1)
            {
                for (int i = 0; i < decl.names.size(); i++)
                {
                    for (int j = i + 1; j < decl.names.size(); j++)
                    {
                        Expression x = environment.get(decl.names.get(i).label);
                        Expression y = environment.get(decl.names.get(j).label);
                        Expression notEqual = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(x, y));
                        constraints = ExprTranslator.and(constraints, notEqual);
                    }
                }
            }
        }
        return constraints;
    }

    /*
     * Translation with first-order quantifiers
     */

    private Expression translateFirstOrderQuantifier(ExprQt exprQt, Environment environment)
    {
        // create a new scope for quantified variables
        Environment newEnvironment;
        switch (exprQt.op)
        {
            case ALL:
            case SOME: newEnvironment = new Environment(environment); break;
            case NO: newEnvironment = environment.flipped(); break;
            default: newEnvironment = environment.withPolarity(Environment.Polarity.BOTH); break;
        }
        newEnvironment.setSkolemizable(isSkolemized(exprQt, environment));
        Map<String, Expression> ranges = new LinkedHashMap<>();

        // this variable maintains the multiplicity constraints for declared variables
        // x: [one, lone, some, set] e
        Expression multiplicityConstraints = declareQuantifiedVariables(exprQt, newEnvironment, ranges);

        Expression quantifiersConstraints = multiplicityConstraints;

        Expression disjointConstraints =  getDisjointConstraints(exprQt,  newEnvironment);

        if(!disjointConstraints.equals(BoolConstant.True))
        {
            quantifiersConstraints = BinaryExpression.Op.AND.make(quantifiersConstraints, disjointConstraints);
        }

        // translate the body of the quantified expression
        Expression body = exprTranslator.translateExpr(exprQt.sub, newEnvironment);

        switch (exprQt.op)
        {
            case ALL:
                return translateAllQuantifier(body, ranges, newEnvironment, quantifiersConstraints);
            case NO:
                return translateNoQuantifier(body, ranges, newEnvironment, quantifiersConstraints);
            case SOME:
                return translateSomeQuantifier(body, ranges, newEnvironment, quantifiersConstraints);
            case ONE:
                return translateOneQuantifier(body, ranges, newEnvironment, quantifiersConstraints);
            case LONE:
                return translateLoneQuantifier(body, ranges, newEnvironment, quantifiersConstraints);
            case COMPREHENSION:
                return translateComprehension(exprQt, body, ranges, newEnvironment);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Expression declareQuantifiedVariables(ExprQt exprQt, Environment newEnvironment, Map<String, Expression> ranges)
    {
        Expression multiplicityConstraints = BoolConstant.True;
        for (Decl decl : exprQt.decls)
        {
            Expression range = ExprTranslator.toSet(exprTranslator.translateExpr(decl.expr, newEnvironment));
            for (ExprHasName name : decl.names)
            {
                ranges.put(name.label, range);
                String label = name.label;
                SetSort setSort = (SetSort) range.getSort();
                VariableDeclaration variable;
                variable = getVariableDeclaration(decl.expr, label, setSort, range);
                Expression constraint = getMultiplicityConstraint(decl.expr, variable, setSort, newEnvironment);
                multiplicityConstraints = ExprTranslator.and(multiplicityConstraints, constraint);
                newEnvironment.put(name.label, variable.getVariable());
            }
        }
        return multiplicityConstraints;
    }


    private Expression getDisjointConstraints(ExprQt exprQt, Environment newEnvironment)
    {
        Expression disjointConstraints = BoolConstant.True;

        for (Decl decl : exprQt.decls)
        {
            // disjoint fields
            if (decl.disjoint != null && decl.names.size() > 1)
            {
                for (int i = 0; i < decl.names.size() - 1; i++)
                {
                    for (int j = i + 1; j < decl.names.size(); j++)
                    {
                        Expression variableI = ExprTranslator.toSet(newEnvironment.get(decl.names.get(i).label));
                        Expression variableJ = ExprTranslator.toSet(newEnvironment.get(decl.names.get(j).label));
                        Expression intersect = BinaryExpression.Op.INTERSECTION.make(variableI, variableJ);
                        disjointConstraints = ExprTranslator.and(disjointConstraints, ExprTranslator.mkEmpty(intersect));
                    }
                }
            }
        }
        return disjointConstraints;
    }

    private Expression translateComprehension(ExprQt exprQt, Expression body, Map<String, Expression> ranges, Environment environment)
    {
        // {x: e1, y: e2, ... | f} is translated into
        // declare-fun comprehension(freeVariables): (e1 x e2 x ...)
        // assert forall x, y,... (x in e1 and y in e2 ... and f <=>
        // (x, y, ...) in comprehension(freeVariables))

        List<VariableDeclaration> quantifiedVariables = ranges.entrySet()
                                                              .stream()
                                                              .map(entry -> (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration())
                                                             .collect(Collectors.toList());

        List<Sort> elementSorts = quantifiedVariables.stream()
                .map(v -> ((TupleSort) (v.getSort())).elementSorts.get(0))
                .collect(Collectors.toList());

        Sort returnSort = new SetSort(new TupleSort(elementSorts));

        Expression membership = getMemberOrSubsetExpressions(ranges, environment);

        // add variables in the environment as arguments to the set function
        LinkedHashMap<String, Expression> argumentsMap = environment.getParent().getVariables();
        List<Sort> argumentSorts = new ArrayList<>();
        List<Expression> arguments = new ArrayList<>();
        List<VariableDeclaration> quantifiedArguments = new ArrayList<>();
        for (Map.Entry<String, Expression> argument: argumentsMap.entrySet())
        {
            Variable variable = (Variable) argument.getValue();
            arguments.add(variable);
            Sort sort = variable.getSort();
            argumentSorts.add(sort);

            // handle set sorts differently to avoid second order quantification
            if(sort instanceof SetSort)
            {
                Sort elementSort = ((SetSort) sort).elementSort;
                VariableDeclaration tuple = new VariableDeclaration(variable.getName(), elementSort, null);
                quantifiedArguments.add(tuple);
                Expression singleton = UnaryExpression.Op.SINGLETON.make(tuple.getVariable());
                body = body.replace(variable, singleton);
                membership = membership.replace(argument.getValue(), singleton);
            }
            else if (sort instanceof TupleSort || sort instanceof UninterpretedSort)
            {
                quantifiedArguments.add(new VariableDeclaration(variable.getName(), sort, null));
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }
        FunctionDeclaration setFunction = new FunctionDeclaration(TranslatorUtils.getNewSetName(), argumentSorts, returnSort);
        translator.smtProgram.addFunction(setFunction);

        Expression setFunctionExpression;
        if(argumentSorts.size() == 0)
        {
            setFunctionExpression = setFunction.getVariable();
        }
        else
        {
            // set arguments are quantified with their elements
            List<Expression> expressions = new ArrayList<>();
            for (int i = 0; i < quantifiedArguments.size(); i++)
            {
                Expression variable = quantifiedArguments.get(i).getVariable();
                expressions.add(variable.getSort().equals(argumentSorts.get(i)) ? variable : UnaryExpression.Op.SINGLETON.make(variable));
            }
            setFunctionExpression = new FunctionCallExpression(setFunction, expressions);
        }

        List<Expression> quantifiedExpressions = quantifiedVariables.stream()
                    .map(v -> BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), v.getVariable()))
                    .collect(Collectors.toList());

        Expression tuple = MultiArityExpression.Op.MKTUPLE.make(quantifiedExpressions);

        Expression tupleMember = BinaryExpression.Op.MEMBER.make(tuple, setFunctionExpression);

        Expression and = BinaryExpression.Op.AND.make(membership, body);

        Expression equivalence = BinaryExpression.Op.EQ.make(tupleMember, and);

        // add variables defined in functions, predicates or let expression to the list of quantifiers
        List<VariableDeclaration> quantifiers = new ArrayList<>(quantifiedArguments);
        quantifiers.addAll(quantifiedVariables);
        Expression forAll = QuantifiedExpression.Op.FORALL.make(equivalence, quantifiers);

        Assertion assertion = new Assertion(exprQt.toString(), forAll);
        translator.smtProgram.addAssertion(assertion);

        if(argumentSorts.size() == 0)
        {
            return setFunction.getVariable();
        }
        else
        {
            return new FunctionCallExpression(setFunction, arguments);
        }
    }


    private VariableDeclaration getVariableDeclaration(Expr expr, String variableName, SetSort setSort, Expression range)
    {
        if(expr instanceof Sig)
        {
            return getVariableDeclaration(variableName, setSort, range);
        }
        if(expr instanceof ExprUnary)
        {
            ExprUnary.Op multiplicityOperator = ((ExprUnary) expr).op;
            switch (multiplicityOperator)
            {
                case NOOP: // same as ONEOF
                case ONEOF:
                {
                    return getVariableDeclaration(variableName, setSort, range);
                }
                case SOMEOF: // same as SETOF
                case LONEOF: // same as SETOF
                case SETOF:
                {
                    VariableDeclaration declaration = new VariableDeclaration(variableName, setSort, null);
                    Expression subset = BinaryExpression.Op.SUBSET.make(declaration.getVariable(), range);
                    declaration.setConstraint(subset);
                    return declaration;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
        if(expr instanceof ExprBinary)
        {
            ExprBinary.Op multiplicityOperator = ((ExprBinary) expr).op;
            if (multiplicityOperator.isArrow)
            {
                VariableDeclaration declaration = new VariableDeclaration(variableName, setSort, null);
                Expression subset = BinaryExpression.Op.SUBSET.make(declaration.getVariable(), range);
                declaration.setConstraint(subset);
                return declaration;
            }
            throw new UnsupportedOperationException();
        }
        throw new UnsupportedOperationException();
    }

    private VariableDeclaration getVariableDeclaration(String variableName, SetSort setSort, Expression range)
    {
        VariableDeclaration declaration = new VariableDeclaration(variableName, setSort.elementSort, null);
        Expression member = BinaryExpression.Op.MEMBER.make(declaration.getVariable(), range);
        declaration.setConstraint(member);
        return declaration;
    }

    private Expression getMultiplicityConstraint(Expr expr, VariableDeclaration variable, SetSort setSort, Environment environment)
    {
        if(expr instanceof ExprUnary)
        {
            ExprUnary.Op multiplicityOperator = ((ExprUnary) expr).op;
            switch (multiplicityOperator)
            {
                case NOOP: // same as ONEOF
                case ONEOF:
                case SETOF:
                    return BoolConstant.True;
                case SOMEOF:
                    return ExprTranslator.mkNonEmpty(variable.getVariable());
                case LONEOF:
                    return exprTranslator.mkLone(variable.getVariable(), environment);
                default:
                    throw new UnsupportedOperationException();
            }
        }
        if(expr instanceof ExprBinary)
        {
            // x: A m -> n B
            return exprBinaryTranslator.isIn(variable.getVariable(), expr, environment);
        }
        return BoolConstant.True;
    }

    private Expression translateAllQuantifier(Expression body, Map<String, Expression> ranges,
                                              Environment environment, Expression multiplicityConstraints)
    {
        // all x: e1, y: e2, ... | f is translated into
        // forall x, y,... (x in e1 and y in e2 and ... and multiplicityConstraints implies f)


        List<VariableDeclaration> quantifiedVariables = ranges.entrySet()
                                                              .stream()
                                                              .map(entry -> (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration())
                                                              .collect(Collectors.toList());

        Expression memberOrSubset = getMemberOrSubsetExpressions(ranges, environment);
        Expression and = ExprTranslator.and(memberOrSubset, multiplicityConstraints);
        Expression implies = BinaryExpression.Op.IMPLIES.make(and, body);
        return QuantifiedExpression.Op.FORALL.make(implies, quantifiedVariables);
    }

    private Expression translateNoQuantifier(Expression body, Map<String, Expression> ranges,
                                             Environment environment, Expression multiplicityConstraints)
    {
        Expression notBody = UnaryExpression.Op.NOT.make(body);
        return translateAllQuantifier(notBody, ranges, environment, multiplicityConstraints);
    }

    private Expression translateSomeQuantifier(Expression body, Map<String, Expression> ranges,
                                               Environment environment, Expression multiplicityConstraints)
    {

        // some x: e1, y: e2, ... | f is translated into
        // exists x, y,... (x in e1 and y in e2 and ... and multiplicityConstraints and f)

        List<VariableDeclaration> quantifiedVariables = ranges.entrySet()
                                                              .stream()
                                                              .map(entry -> (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration())
                                                              .collect(Collectors.toList());

        Expression and = getMemberOrSubsetExpressions(ranges, environment);
        and = ExprTranslator.and(and, multiplicityConstraints);
        and = BinaryExpression.Op.AND.make(and, body);
        return QuantifiedExpression.Op.EXISTS.make(and, quantifiedVariables);
    }

    private Expression getMemberOrSubsetExpressions(Map<String, Expression> ranges, Environment environment)
    {
        Expression and = BoolConstant.True;
        for (Map.Entry<String, Expression> entry : ranges.entrySet())
        {
            VariableDeclaration variable = (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration();
            Expression memberOrSubset;
            if (variable.getSort() instanceof TupleSort)
            {
                memberOrSubset = BinaryExpression.Op.MEMBER.make(variable.getVariable(), entry.getValue());
            }
            else if (variable.getSort() instanceof SetSort)
            {
                memberOrSubset = BinaryExpression.Op.SUBSET.make(variable.getVariable(), entry.getValue());
            }
            else
            {
                throw new UnsupportedOperationException(variable.getSort().toString());
            }
            and = ExprTranslator.and(and, memberOrSubset);
        }
        return and;
    }

    private Expression translateOneQuantifier(Expression body, Map<String, Expression> ranges,
                                              Environment environment, Expression multiplicityConstraints)
    {
        // one x: e1, y: e2, ... | f(x, y, ...) is translated into
        // exists x, y, ... ( x in e1 and y in e2 and ... and multiplicityConstraints(x, y, ...) and f(x, y, ...) and
        //                      for all x', y', ... (x in e1 and y in e2 ... and multiplicityConstraints(x', y', ...)
        //                              and not (x' = x and y' = y ...) implies not f(x', y', ...)))

        List<VariableDeclaration> oldVariables = ranges.entrySet()
                                                            .stream()
                                                            .map(entry -> (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration())
                                                            .collect(Collectors.toList());

        Expression oldMemberOrSubset = getMemberOrSubsetExpressions(ranges, environment);

        Expression existsAnd = ExprTranslator.and(oldMemberOrSubset, multiplicityConstraints);

        existsAnd = BinaryExpression.Op.AND.make(existsAnd, body);

        List<VariableDeclaration> newVariables = new ArrayList<>();

        Expression newBody = body;
        Expression oldEqualNew = BoolConstant.True;
        Expression newMemberOrSubset = BoolConstant.True;
        Expression newMultiplicityConstraints = multiplicityConstraints;
        for (Map.Entry<String, Expression> entry : ranges.entrySet())
        {
            VariableDeclaration oldVariable = (VariableDeclaration) ((Variable) environment.get(entry.getKey())).getDeclaration();
            VariableDeclaration newVariable = new VariableDeclaration(TranslatorUtils.getNewAtomName(), oldVariable.getSort(), null);
            if(oldVariable.getConstraint() != null)
            {
                Expression newConstraint = oldVariable.getConstraint().replace(oldVariable.getVariable(), newVariable.getVariable());
                newVariable.setConstraint(newConstraint);
            }

            newVariables.add(newVariable);
            newBody = newBody.replace(oldVariable.getVariable(), newVariable.getVariable());
            newMultiplicityConstraints = newMultiplicityConstraints.replace(oldVariable.getVariable(), newVariable.getVariable());

            oldEqualNew = ExprTranslator.and(oldEqualNew, BinaryExpression.Op.EQ.make(oldVariable.getVariable(), newVariable.getVariable()));
            if (newVariable.getSort() instanceof TupleSort)
            {
                newMemberOrSubset = ExprTranslator.and(newMemberOrSubset, BinaryExpression.Op.MEMBER.make(newVariable.getVariable(), entry.getValue()));
            }
            else if (newVariable.getSort() instanceof SetSort)
            {
                newMemberOrSubset = ExprTranslator.and(newMemberOrSubset, BinaryExpression.Op.SUBSET.make(newVariable.getVariable(), entry.getValue()));
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }


        newBody = UnaryExpression.Op.NOT.make(newBody);
        Expression notOldEqualNew = UnaryExpression.Op.NOT.make(oldEqualNew);

        Expression forAllAnd = ExprTranslator.and(newMemberOrSubset, newMultiplicityConstraints);
        forAllAnd = BinaryExpression.Op.AND.make(forAllAnd, notOldEqualNew);

        Expression implies = BinaryExpression.Op.IMPLIES.make(forAllAnd, newBody);
        Expression forAll = QuantifiedExpression.Op.FORALL.make(implies,  newVariables);
        existsAnd = BinaryExpression.Op.AND.make(existsAnd, forAll);
        return QuantifiedExpression.Op.EXISTS.make(existsAnd, oldVariables);
    }

    private Expression translateLoneQuantifier(Expression body, Map<String, Expression> ranges,
                                                         Environment environment, Expression multiplicityConstraints)
    {
        // lone ... | f is translated into
        // (all ... | not f)  or (one ... | f)

        Expression notBody = UnaryExpression.Op.NOT.make(body);
        Expression allNot = translateAllQuantifier(notBody, ranges, environment, multiplicityConstraints);
        Expression one = translateOneQuantifier(body, ranges, environment, multiplicityConstraints);
        return BinaryExpression.Op.OR.make(allNot, one);
    }
}
