/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.ast.*;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.Environment;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;

import java.util.*;
import java.util.function.BiFunction;

public class ExprTranslator
{
    final Alloy2SmtTranslator translator;

    final ExprUnaryTranslator exprUnaryTranslator;

    final ExprBinaryTranslator exprBinaryTranslator;

    final ExprQtTranslator exprQtTranslator;

    public ExprTranslator(Alloy2SmtTranslator translator)
    {
        this.translator = translator;
        this.exprUnaryTranslator = new ExprUnaryTranslator(this);
        this.exprBinaryTranslator = new ExprBinaryTranslator(this);
        this.exprQtTranslator = new ExprQtTranslator(this);
    }

    Expression translateExpr(Expr expr)
    {
        return translateExpr(expr, new Environment());
    }

    Expression translateExpr(Expr expr, Environment environment)
    {
        if (expr instanceof Sig)
        {
            return exprUnaryTranslator.translateExprUnary((ExprUnary) ExprUnary.Op.NOOP.make(null, expr), environment);
        }
        if (expr instanceof Sig.Field || expr instanceof ExprVar)
        {
            return exprUnaryTranslator.translateExprUnary((ExprUnary) ExprUnary.Op.NOOP.make(null, expr), environment);
        }
        if (expr instanceof ExprUnary)
        {
            return this.exprUnaryTranslator.translateExprUnary((ExprUnary) expr, environment);
        }
        else if (expr instanceof ExprBinary)
        {
            return this.exprBinaryTranslator.translateExprBinary((ExprBinary) expr, environment);
        }
        else if (expr instanceof ExprQt)
        {
            return exprQtTranslator.translateExprQt((ExprQt) expr, environment);
        }
        else if (expr instanceof ExprConstant)
        {
            return translateExprConstant((ExprConstant) expr, environment);
        }
        else if (expr instanceof ExprList)
        {
            return translateExprList((ExprList) expr, environment);
        }
        else if (expr instanceof ExprCall)
        {
            return translateExprCall((ExprCall) expr, environment);
        }
        else if (expr instanceof ExprITE)
        {
            return translateExprITE((ExprITE) expr, environment);
        }
        else if (expr instanceof ExprLet)
        {
            return translateExprLet((ExprLet) expr, environment);
        }

        throw new UnsupportedOperationException(expr.toString());
    }

    public Expression translateExprITE(ExprITE expr, Environment environment)
    {
        Expression condExpr = translateExpr(expr.cond, environment.withPolarity(Environment.Polarity.BOTH));
        Expression thenExpr = translateExpr(expr.left, environment);
        Expression elseExpr = translateExpr(expr.right, environment);
        return new ITEExpression(condExpr, thenExpr, elseExpr);
    }

    public Expression translateExprConstant(ExprConstant expr, Environment environment)
    {
        switch (expr.op)
        {
            // alloy only supports integers
            case NUMBER:
            {
                Expression intConstant = IntConstant.getSingletonTuple(expr.num);
                return translator.handleIntConstant(intConstant);
            }
            case IDEN:
                return translator.atomIdentity.getVariable();
            case TRUE:
                return BoolConstant.True;
            case FALSE:
                return BoolConstant.False;
            default:
                throw new UnsupportedOperationException(expr.op.name());
        }
    }

    Expression translateExprList(ExprList exprList, Environment environment)
    {
        switch (exprList.op)
        {
            case AND:
                return translateExprListToBinaryExpressions(BinaryExpression.Op.AND, exprList, environment);
            case OR:
                return translateExprListToBinaryExpressions(BinaryExpression.Op.OR, exprList, environment);
            case DISJOINT:
                return translateExprListToDisjBinaryExpressions(exprList, environment);
            case TOTALORDER:
                throw new UnsupportedOperationException();// total order should be handled before coming here
            default:
                throw new UnsupportedOperationException();
        }
    }

    // disj[e1, ..., en] holds iff the expressions are pairwise disjoint
    Expression translateExprListToDisjBinaryExpressions(ExprList exprList, Environment environment)
    {
        List<Expression> exprs = new ArrayList<>();

        for (Expr e : exprList.args)
        {
            exprs.add(translateExpr(e, environment));
        }
        Expression finalExpr = BoolConstant.True;
        for (int i = 0; i < exprs.size(); ++i)
        {
            for (int j = i + 1; j < exprs.size(); ++j)
            {
                Expression intersection = BinaryExpression.Op.INTERSECTION.make(exprs.get(i), exprs.get(j));
                finalExpr = and(finalExpr, mkEmpty(intersection));
            }
        }
        return finalExpr;
    }

    Expression translateExprLet(ExprLet exprLet, Environment environment)
    {
        Expression varExpr = translateExpr(exprLet.expr, environment);
        Map<String, Expression> varToExprMap = new HashMap<>();
        String label = exprLet.var.label;
        Variable varDeclExpr = new ConstantDeclaration(label, varExpr.getSort()).getVariable();

        varToExprMap.put(label, varExpr);
        // make a new environment
        Environment newEnvironment = new Environment(environment);
        newEnvironment.put(exprLet.var.label, varDeclExpr);

        Expression letBodyExpr = translateExpr(exprLet.sub, newEnvironment);
        return new LetExpression(varToExprMap, letBodyExpr);
    }

    Expression translateExprCall(ExprCall exprCall, Environment environment)
    {
        return translateExprCall(exprCall, environment, false);
    }

    /**
     * @param negated whether to translate the negation of the predicate call
     */
    Expression translateExprCall(ExprCall exprCall, Environment environment, boolean negated)
    {
        String funcName = exprCall.fun.label;
        List<Expression> argExprs = new ArrayList<>();

        for (Expr e : exprCall.args)
        {
            argExprs.add(translateExpr(e, environment));
        }

        if (funcName.equals("integer/plus") || funcName.equals("integer/add"))
        {
            return translateArithmetic(argExprs.get(0), argExprs.get(1), BinaryExpression.Op.PLUS, environment);
        }
        else if (funcName.equals("integer/minus") || funcName.equals("integer/sub"))
        {
            return translateArithmetic(argExprs.get(0), argExprs.get(1), BinaryExpression.Op.MINUS, environment);
        }
        else if (funcName.equals("integer/mul"))
        {
            return translateArithmetic(argExprs.get(0), argExprs.get(1), BinaryExpression.Op.MULTIPLY, environment);
        }
        else if (funcName.equals("integer/div"))
        {
            return translateArithmetic(argExprs.get(0), argExprs.get(1), BinaryExpression.Op.DIVIDE, environment);
        }
        else if (funcName.equals("integer/rem"))
        {
            return translateArithmetic(argExprs.get(0), argExprs.get(1), BinaryExpression.Op.MOD, environment);
        }
        else if (translator.functionsMap.containsKey(funcName))
        {
            // functions of util/ordering are defined in SignatureTranslator
            FunctionDeclaration function = translator.getFunction(funcName);
            return new FunctionCallExpression(function, argExprs);
        }
        Func func = translator.nameToFuncMap.get(funcName);
        if (func == null)
        {
            throw new UnsupportedOperationException("Function " + funcName + " is not supported");
        }
        return inline(func, argExprs, environment, negated);
    }

    // functions being inlined, to detect recursion
    private final Set<Func> inlining = new HashSet<>();

    /**
     * Inlines the call of the function like the Kodkod translation does:
     * (let ((x1 arg1) ... (xn argn)) body). The body is translated with the polarity
     * of the call, which allows its quantifiers to be skolemized.
     */
    private Expression inline(Func func, List<Expression> arguments, Environment environment, boolean negated)
    {
        if (!inlining.add(func))
        {
            throw new UnsupportedOperationException("Recursive function " + func.label + " is not supported");
        }
        try
        {
            Environment bodyEnvironment = new Environment(environment);
            Map<String, Expression> bindings = new LinkedHashMap<>();
            int index = 0;
            for (Decl decl : func.decls)
            {
                for (ExprHasName name : decl.names)
                {
                    Expression argument = toSet(arguments.get(index++));
                    if (argument.getSort().equals(AbstractTranslator.setOfIntSortTuple))
                    {
                        argument = translator.handleIntConstant(argument);
                    }
                    String label = TranslatorUtils.getNewName() + "_" + name.label;
                    VariableDeclaration parameter = new VariableDeclaration(label, argument.getSort(), null);
                    bindings.put(label, argument);
                    bodyEnvironment.put(name.label, parameter.getVariable());
                }
            }
            Expression body = translateExpr(negated ? func.getBody().not() : func.getBody(), bodyEnvironment);
            if (!func.isPred)
            {
                body = toSet(body);
            }
            if (bindings.isEmpty())
            {
                return body;
            }
            return new LetExpression(bindings, body);
        }
        finally
        {
            inlining.remove(func);
        }
    }

    public Expression translateArithmetic(Expression A, Expression B, BinaryExpression.Op op, Environment environment)
    {
        translator.translateIntegerAssertions();

        A = convertIntConstantToSet(A);

        B = convertIntConstantToSet(B);

        // for all x, y : uninterpretedInt. x in A and y in B implies
        // exists z :uninterpretedInt. (x, y, z) in operation


        if (A.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            A = translator.handleIntConstant(A);
        }

        if (B.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            B = translator.handleIntConstant(B);
        }

        Expression newA = A;
        Expression newB = B;

        // add variables in the environment as arguments to the result function
        LinkedHashMap<String, Expression> argumentsMap = environment.getVariables();
        List<Sort> argumentSorts = new ArrayList<>();
        List<Expression> arguments = new ArrayList<>();
        List<VariableDeclaration> quantifiedArguments = new ArrayList<>();
        for (Map.Entry<String, Expression> argument : argumentsMap.entrySet())
        {
            arguments.add(argument.getValue());
            Variable variable = (Variable) argument.getValue();
            Sort sort = variable.getSort();
            argumentSorts.add(sort);

            // handle set sorts differently to avoid second order quantification
            if (sort instanceof SetSort)
            {
                Sort elementSort = ((SetSort) sort).elementSort;
                VariableDeclaration tuple = new VariableDeclaration(variable.getName(), elementSort, null);
                quantifiedArguments.add(tuple);
                Expression singleton = UnaryExpression.Op.SINGLETON.make(tuple.getVariable());
                newA = newA.replace(argument.getValue(), singleton);
                newB = newB.replace(argument.getValue(), singleton);
                Expression constraint = variable.getDeclaration() instanceof VariableDeclaration ?
                        ((VariableDeclaration) variable.getDeclaration()).getConstraint() : null;
                if (constraint != null)
                {
                    constraint = constraint.replace(variable, singleton);
                }
                tuple.setConstraint(constraint);
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

        FunctionDeclaration result = new FunctionDeclaration(TranslatorUtils.getNewSetName(), argumentSorts, AbstractTranslator.setOfUninterpretedIntTuple);
        translator.smtProgram.addFunction(result);

        Expression resultExpression;
        Expression quantifiedResult;
        if (result.getInputSorts().size() > 0)
        {
            resultExpression = new FunctionCallExpression(result, arguments);
            List<Expression> quantifiedVariables = new ArrayList<>();
            for (VariableDeclaration declaration : quantifiedArguments)
            {
                quantifiedVariables.add(declaration.getSort().equals(argumentSorts.get(quantifiedVariables.size())) ?
                        declaration.getVariable() : UnaryExpression.Op.SINGLETON.make(declaration.getVariable()));
            }
            quantifiedResult = new FunctionCallExpression(result, quantifiedVariables);
        }
        else
        {
            resultExpression = result.getVariable();
            quantifiedResult = resultExpression;
        }

        // the quantified arguments shadow the free variables with the same names
        for (int i = 0; i < quantifiedArguments.size(); i++)
        {
            Variable free = (Variable) arguments.get(i);
            if (!(free.getSort() instanceof SetSort))
            {
                newA = newA.replace(free, quantifiedArguments.get(i).getVariable());
                newB = newB.replace(free, quantifiedArguments.get(i).getVariable());
            }
        }

        VariableDeclaration x = new VariableDeclaration("__x__", AbstractTranslator.uninterpretedInt, null);
        VariableDeclaration y = new VariableDeclaration("__y__", AbstractTranslator.uninterpretedInt, null);
        VariableDeclaration z = new VariableDeclaration("__z__", AbstractTranslator.uninterpretedInt, null);

        Expression xTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, x.getVariable());
        Expression yTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, y.getVariable());
        Expression zTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, z.getVariable());

        Expression xValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, x.getVariable());
        Expression yValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, y.getVariable());
        Expression zValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, z.getVariable());

        Expression xMember = BinaryExpression.Op.MEMBER.make(xTuple, newA);
        Expression yMember = BinaryExpression.Op.MEMBER.make(yTuple, newB);
        Expression zMember = BinaryExpression.Op.MEMBER.make(zTuple, quantifiedResult);

        Expression xyOperation = op.make(xValue, yValue);
        Expression equal = BinaryExpression.Op.EQ.make(xyOperation, zValue);

        Expression and1 = BinaryExpression.Op.AND.make(xMember, yMember);
        Expression and2 = BinaryExpression.Op.AND.make(equal, and1);
        Expression exists1 = QuantifiedExpression.Op.EXISTS.make(and2, x, y);
        Expression argumentConstraints = BoolConstant.True;
        for (VariableDeclaration declaration : quantifiedArguments)
        {
            if (declaration.getConstraint() != null)
            {
                argumentConstraints = and(argumentConstraints, declaration.getConstraint());
            }
        }
        Expression antecedent1 = and(argumentConstraints, zMember);
        Expression implies1 = BinaryExpression.Op.IMPLIES.make(antecedent1, exists1);
        List<VariableDeclaration> quantifiers1 = new ArrayList<>(quantifiedArguments);
        quantifiers1.add(z);
        Expression forall1 = QuantifiedExpression.Op.FORALL.make(implies1, quantifiers1);

        Assertion assertion1 = new Assertion(String.format("%1$s %2$s %3$s axiom1", op, A, B), forall1);
        translator.smtProgram.addAssertion(assertion1);

        Expression and3 = BinaryExpression.Op.AND.make(equal, zMember);
        Expression exists2 = QuantifiedExpression.Op.EXISTS.make(and3, z);

        Expression antecedent2 = and(argumentConstraints, and1);

        Expression implies2 = BinaryExpression.Op.IMPLIES.make(antecedent2, exists2);
        List<VariableDeclaration> quantifiers2 = new ArrayList<>(quantifiedArguments);
        quantifiers2.add(x);
        quantifiers2.add(y);
        Expression forall2 = QuantifiedExpression.Op.FORALL.make(implies2, quantifiers2);

        Assertion assertion2 = new Assertion(String.format("%1$s %2$s %3$s axiom2", op, A, B), forall2);
        translator.smtProgram.addAssertion(assertion2);

        return resultExpression;
    }

    private Expression convertIntConstantToSet(Expression A)
    {
        if (A instanceof IntConstant)
        {
            ConstantDeclaration uninterpretedInt = translator.getUninterpretedIntConstant((IntConstant) A);
            Expression tuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, uninterpretedInt.getVariable());
            A = UnaryExpression.Op.SINGLETON.make(tuple);
        }
        return A;
    }

    private Expression translateExprListToBinaryExpressions(BinaryExpression.Op op, ExprList exprList, Environment environment)
    {

        if (exprList.args.size() == 0)
        {
            if (op == BinaryExpression.Op.AND)
            {
                return BoolConstant.True;
            }

            if (op == BinaryExpression.Op.OR)
            {
                return BoolConstant.False;
            }
            throw new UnsupportedOperationException();
        }

        Expression left = translateExpr(exprList.args.get(0), environment);

        if (exprList.args.size() == 1)
        {
            return left;
        }

        Expression right = translateExpr(exprList.args.get(1), environment);
        BinaryExpression result = op.make(left, right);


        for (int i = 2; i < exprList.args.size(); i++)
        {
            Expression expr = translateExpr(exprList.args.get(i), environment);
            result = op.make(result, expr);
        }

        return result;
    }

    /*
     * Encoding helpers shared by the expression translators.
     */

    static Expression and(Expression left, Expression right)
    {
        if (left == BoolConstant.True)
        {
            return right;
        }
        if (right == BoolConstant.True)
        {
            return left;
        }
        return BinaryExpression.Op.AND.make(left, right);
    }

    static Expression implies(Expression left, Expression right)
    {
        if (left == BoolConstant.True)
        {
            return right;
        }
        return BinaryExpression.Op.IMPLIES.make(left, right);
    }

    /**
     * @return the expression as a set: tuples and atoms are wrapped in singletons
     */
    static Expression toSet(Expression expression)
    {
        Sort sort = expression.getSort();
        if (sort instanceof TupleSort)
        {
            return UnaryExpression.Op.SINGLETON.make(expression);
        }
        if (sort instanceof UninterpretedSort)
        {
            return UnaryExpression.Op.SINGLETON.make(MultiArityExpression.Op.MKTUPLE.make(expression));
        }
        return expression;
    }

    /**
     * left in right, where (set.singleton t) in right is simplified into t in right
     */
    static Expression mkSubset(Expression left, Expression right)
    {
        if (left instanceof UnaryExpression && ((UnaryExpression) left).getOP() == UnaryExpression.Op.SINGLETON)
        {
            return mkMember(((UnaryExpression) left).getExpression(), right);
        }
        return BinaryExpression.Op.SUBSET.make(left, right);
    }

    /**
     * t in set, where membership in a join with a singleton is simplified:
     * t in {a}.r is (a, t) in r and t in r.{b} is (t, b) in r
     */
    static Expression mkMember(Expression tuple, Expression set)
    {
        if (set instanceof BinaryExpression && ((BinaryExpression) set).getOp() == BinaryExpression.Op.JOIN)
        {
            Expression left = ((BinaryExpression) set).getLhsExpr();
            Expression right = ((BinaryExpression) set).getRhsExpr();
            if (left instanceof UnaryExpression && ((UnaryExpression) left).getOP() == UnaryExpression.Op.SINGLETON)
            {
                return mkMember(concatenate(((UnaryExpression) left).getExpression(), tuple), right);
            }
            if (right instanceof UnaryExpression && ((UnaryExpression) right).getOP() == UnaryExpression.Op.SINGLETON)
            {
                return mkMember(concatenate(tuple, ((UnaryExpression) right).getExpression()), left);
            }
        }
        return BinaryExpression.Op.MEMBER.make(tuple, set);
    }

    private static List<Expression> components(Expression tuple)
    {
        if (tuple instanceof MultiArityExpression && ((MultiArityExpression) tuple).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            return ((MultiArityExpression) tuple).getExpressions();
        }
        List<Expression> components = new ArrayList<>();
        int arity = ((TupleSort) tuple.getSort()).elementSorts.size();
        for (int i = 0; i < arity; i++)
        {
            components.add(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i), tuple));
        }
        return components;
    }

    static Expression concatenate(Expression left, Expression right)
    {
        List<Expression> components = new ArrayList<>(components(left));
        components.addAll(components(right));
        return MultiArityExpression.Op.MKTUPLE.make(components);
    }

    static Expression mkEmpty(Expression set)
    {
        return BinaryExpression.Op.EQ.make(set, UnaryExpression.Op.EMPTYSET.make(set.getSort()));
    }

    static Expression mkNonEmpty(Expression set)
    {
        return UnaryExpression.Op.NOT.make(mkEmpty(set));
    }

    static List<Sort> elementSorts(Expression set)
    {
        return ((TupleSort) ((SetSort) set.getSort()).elementSort).elementSorts;
    }

    /**
     * lone e: every two tuples of e are equal.
     */
    Expression mkLone(Expression set, Environment environment)
    {
        List<Sort> sorts = elementSorts(set);
        if (!environment.isInsideLambda())
        {
            return BinaryExpression.Op.OR.make(mkEmpty(set), UnaryExpression.Op.IS_SINGLETON.make(set));
        }
        // set.is_singleton is not supported in the body of lambda expressions
        if (sorts.size() == 1)
        {
            // e x e in iden(e)
            Expression product = BinaryExpression.Op.PRODUCT.make(set, set);
            return BinaryExpression.Op.SUBSET.make(product, UnaryExpression.Op.IDEN.make(set));
        }
        Expression product = BinaryExpression.Op.PRODUCT.make(set, set);
        return forAllIn(product, (pair, env) ->
        {
            Expression equal = BoolConstant.True;
            for (int i = 0; i < sorts.size(); i++)
            {
                Expression left = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i), pair);
                Expression right = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i + sorts.size()), pair);
                equal = and(equal, BinaryExpression.Op.EQ.make(left, right));
            }
            return equal;
        }, environment);
    }

    Expression mkOne(Expression set, Environment environment)
    {
        if (!environment.isInsideLambda())
        {
            return UnaryExpression.Op.IS_SINGLETON.make(set);
        }
        return BinaryExpression.Op.AND.make(mkNonEmpty(set), mkLone(set, environment));
    }

    /**
     * @return a formula equivalent to (forall t: set | body(t)), where body
     * receives a variable for t (a tuple) and the environment in which the body is translated.
     * With set quantifiers this is (set.all (lambda ((t T)) body) set),
     * otherwise (forall ((t T)) (=> (set.member t set) body)).
     */
    Expression forAllIn(Expression set, BiFunction<Expression, Environment, Expression> body, Environment environment)
    {
        return boundedQuantifier(true, set, body, environment);
    }

    Expression someIn(Expression set, BiFunction<Expression, Environment, Expression> body, Environment environment)
    {
        return boundedQuantifier(false, set, body, environment);
    }

    private Expression boundedQuantifier(boolean all, Expression set, BiFunction<Expression, Environment, Expression> body, Environment environment)
    {
        Sort elementSort = ((SetSort) set.getSort()).elementSort;
        VariableDeclaration tuple = new VariableDeclaration(TranslatorUtils.getNewAtomName(), elementSort, null);
        Environment newEnvironment = new Environment(environment);
        boolean skolemized = all ? environment.forallIsSkolemized() : environment.existsIsSkolemized();
        // a skolemized quantifier is cheaper than a set quantifier: the solver replaces it with constants
        if (translator.options.useSetQuantifiers() && !skolemized)
        {
            translator.usesLambdas = true;
            newEnvironment.setInsideLambda(true);
            Expression lambdaBody = body.apply(tuple.getVariable(), newEnvironment);
            LambdaExpression lambda = new LambdaExpression(tuple, lambdaBody);
            return (all ? BinaryExpression.Op.SET_ALL : BinaryExpression.Op.SET_SOME).make(lambda, set);
        }
        newEnvironment.setSkolemizable(skolemized);
        Expression member = BinaryExpression.Op.MEMBER.make(tuple.getVariable(), set);
        Expression formula = body.apply(tuple.getVariable(), newEnvironment);
        if (all)
        {
            return QuantifiedExpression.Op.FORALL.make(BinaryExpression.Op.IMPLIES.make(member, formula), tuple);
        }
        return QuantifiedExpression.Op.EXISTS.make(BinaryExpression.Op.AND.make(member, formula), tuple);
    }

    /**
     * @return {x : set | body(x)} as (set.filter (lambda ((x T)) body) set). Only used with set quantifiers.
     */
    Expression filter(Expression set, BiFunction<Expression, Environment, Expression> body, Environment environment)
    {
        Sort elementSort = ((SetSort) set.getSort()).elementSort;
        VariableDeclaration tuple = new VariableDeclaration(TranslatorUtils.getNewAtomName(), elementSort, null);
        Environment newEnvironment = environment.withPolarity(Environment.Polarity.BOTH);
        translator.usesLambdas = true;
        newEnvironment.setInsideLambda(true);
        LambdaExpression lambda = new LambdaExpression(tuple, body.apply(tuple.getVariable(), newEnvironment));
        return BinaryExpression.Op.SET_FILTER.make(lambda, set);
    }

    /**
     * Cardinality constraints #set op n for a constant n. Where the solver skolemizes
     * existential quantifiers, witnesses are existentially quantified; elsewhere
     * (with set quantifiers) #set <= m is expressed as: any m + 1 tuples of the set are not distinct.
     */
    Expression translateCardinalityComparison(Expression set, BinaryExpression.Op op, int n, Environment environment)
    {
        switch (op)
        {
            case GT: return translateCardinalityComparison(set, BinaryExpression.Op.GTE, n + 1, environment);
            case LT: return translateCardinalityComparison(set, BinaryExpression.Op.LTE, n - 1, environment);
            case GTE:
            case LTE:
            case EQ: break;
            default: throw new UnsupportedOperationException(op.toString());
        }
        if (!translator.options.useSetQuantifiers() || environment.existsIsSkolemized())
        {
            switch (op)
            {
                case GTE: return atLeast(set, n);
                case LTE: return atMost(set, n);
                default: return exactly(set, n);
            }
        }
        if (environment.forallIsSkolemized())
        {
            // the negation of the formula is skolemized
            switch (op)
            {
                case GTE: return not(atMost(set, n - 1));
                case LTE: return not(atLeast(set, n + 1));
                default: return and(not(atMost(set, n - 1)), not(atLeast(set, n + 1)));
            }
        }
        switch (op)
        {
            case GTE: return not(atMostUniversal(set, n - 1, environment));
            case LTE: return atMostUniversal(set, n, environment);
            default: return and(atMostUniversal(set, n, environment), not(atMostUniversal(set, n - 1, environment)));
        }
    }

    static Expression not(Expression expression)
    {
        if (expression == BoolConstant.True)
        {
            return BoolConstant.False;
        }
        if (expression == BoolConstant.False)
        {
            return BoolConstant.True;
        }
        return UnaryExpression.Op.NOT.make(expression);
    }

    // exists distinct x1, ..., xn in set
    private Expression atLeast(Expression set, int n)
    {
        if (n <= 0)
        {
            return BoolConstant.True;
        }
        return witnesses(set, n, (witnessSet, distinct) -> and(distinct, BinaryExpression.Op.SUBSET.make(witnessSet, set)));
    }

    // exists x1, ..., xn. set in {x1, ..., xn}
    private Expression atMost(Expression set, int n)
    {
        if (n < 0)
        {
            return BoolConstant.False;
        }
        if (n == 0)
        {
            return mkEmpty(set);
        }
        return witnesses(set, n, (witnessSet, distinct) -> BinaryExpression.Op.SUBSET.make(set, witnessSet));
    }

    // exists distinct x1, ..., xn. set = {x1, ..., xn}
    private Expression exactly(Expression set, int n)
    {
        if (n < 0)
        {
            return BoolConstant.False;
        }
        if (n == 0)
        {
            return mkEmpty(set);
        }
        return witnesses(set, n, (witnessSet, distinct) -> and(distinct, BinaryExpression.Op.EQ.make(set, witnessSet)));
    }

    private Expression witnesses(Expression set, int n, BiFunction<Expression, Expression, Expression> body)
    {
        Sort elementSort = ((SetSort) set.getSort()).elementSort;
        List<VariableDeclaration> witnesses = new ArrayList<>();
        List<Expression> variables = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            VariableDeclaration witness = new VariableDeclaration(TranslatorUtils.getNewAtomName(), elementSort, null);
            witnesses.add(witness);
            variables.add(witness.getVariable());
        }
        Expression formula = body.apply(mkSetOutOfTuples(variables), TranslatorUtils.makeDistinct(variables));
        return QuantifiedExpression.Op.EXISTS.make(formula, witnesses);
    }

    // any m + 1 tuples of the set are not pairwise distinct
    private Expression atMostUniversal(Expression set, int m, Environment environment)
    {
        if (m < 0)
        {
            return BoolConstant.False;
        }
        if (m == 0)
        {
            return mkEmpty(set);
        }
        if (m == 1)
        {
            return mkLone(set, environment);
        }
        int arity = elementSorts(set).size();
        Expression product = set;
        for (int i = 0; i < m; i++)
        {
            product = BinaryExpression.Op.PRODUCT.make(product, set);
        }
        return forAllIn(product, (tuple, env) ->
        {
            List<Expression> tuples = new ArrayList<>();
            for (int i = 0; i <= m; i++)
            {
                List<Expression> components = new ArrayList<>();
                for (int j = 0; j < arity; j++)
                {
                    components.add(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i * arity + j), tuple));
                }
                tuples.add(MultiArityExpression.Op.MKTUPLE.make(components));
            }
            return not(TranslatorUtils.makeDistinct(tuples));
        }, environment);
    }

    static Expression mkSetOutOfTuples(List<Expression> tuples)
    {
        Expression set = UnaryExpression.Op.SINGLETON.make(tuples.get(tuples.size() - 1));
        if (tuples.size() == 1)
        {
            return set;
        }
        List<Expression> arguments = new ArrayList<>(tuples.subList(0, tuples.size() - 1));
        arguments.add(set);
        return MultiArityExpression.Op.INSERT.make(arguments);
    }

    /**
     * @return the join of the singleton tuple with the relation (tuple.relation)
     */
    static Expression joinTupleLeft(Expression tuple, Expression relation)
    {
        List<Sort> sorts = ((TupleSort) tuple.getSort()).elementSorts;
        if (sorts.size() == 1)
        {
            return BinaryExpression.Op.JOIN.make(UnaryExpression.Op.SINGLETON.make(tuple), relation);
        }
        Expression result = relation;
        for (int i = 0; i < sorts.size(); i++)
        {
            Expression element = MultiArityExpression.Op.MKTUPLE.make(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i), tuple));
            result = BinaryExpression.Op.JOIN.make(UnaryExpression.Op.SINGLETON.make(element), result);
        }
        return result;
    }

    /**
     * @return the join of the relation with the singleton tuple (relation.tuple)
     */
    static Expression joinTupleRight(Expression relation, Expression tuple)
    {
        List<Sort> sorts = ((TupleSort) tuple.getSort()).elementSorts;
        if (sorts.size() == 1)
        {
            return BinaryExpression.Op.JOIN.make(relation, UnaryExpression.Op.SINGLETON.make(tuple));
        }
        Expression result = relation;
        for (int i = sorts.size() - 1; i >= 0; i--)
        {
            Expression element = MultiArityExpression.Op.MKTUPLE.make(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(i), tuple));
            result = BinaryExpression.Op.JOIN.make(result, UnaryExpression.Op.SINGLETON.make(element));
        }
        return result;
    }

    /**
     * @return the universe of the given sort, the set of all atoms or uninterpreted integers
     */
    static Expression universe(Sort sort)
    {
        if (sort.equals(AbstractTranslator.uninterpretedInt))
        {
            return AbstractTranslator.intUniv.getVariable();
        }
        return AbstractTranslator.atomUniverse.getVariable();
    }

    /**
     * @return the product of the universes of the given sorts
     */
    static Expression universe(List<Sort> sorts)
    {
        Expression product = universe(sorts.get(0));
        for (int i = 1; i < sorts.size(); i++)
        {
            product = BinaryExpression.Op.PRODUCT.make(product, universe(sorts.get(i)));
        }
        return product;
    }
}
