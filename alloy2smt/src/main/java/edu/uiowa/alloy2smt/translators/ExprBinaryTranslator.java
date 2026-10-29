package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprCall;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.Environment;
import edu.uiowa.smt.smtAst.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExprBinaryTranslator
{
    final ExprTranslator exprTranslator;
    final Alloy2SmtTranslator translator;

    public ExprBinaryTranslator(ExprTranslator exprTranslator)
    {
        this.exprTranslator = exprTranslator;
        translator = exprTranslator.translator;
    }

    Expression translateExprBinary(ExprBinary expr, Environment environment)
    {
        switch (expr.op)
        {
            // multiplicities only matter in declarations and on the right of 'in'
            // (see isIn), elsewhere an arrow denotes the product
            case ARROW              :
            case ANY_ARROW_SOME     :
            case ANY_ARROW_ONE      :
            case ANY_ARROW_LONE     :
            case SOME_ARROW_ANY     :
            case SOME_ARROW_SOME    :
            case SOME_ARROW_ONE     :
            case SOME_ARROW_LONE    :
            case ONE_ARROW_ANY      :
            case ONE_ARROW_SOME     :
            case ONE_ARROW_ONE      :
            case ONE_ARROW_LONE     :
            case LONE_ARROW_ANY     :
            case LONE_ARROW_SOME    :
            case LONE_ARROW_ONE     :
            case LONE_ARROW_LONE    : return BinaryExpression.Op.PRODUCT.make(translateSet(expr.left, environment), translateSet(expr.right, environment));
            case ISSEQ_ARROW_LONE   : throw new UnsupportedOperationException();

            // Relational operators
            case JOIN               : return BinaryExpression.Op.JOIN.make(translateSet(expr.left, environment), translateSet(expr.right, environment));
            case DOMAIN             : return translateDomainRestriction(expr, environment);
            case RANGE              : return translateRangeRestriction(expr, environment);
            case INTERSECT          : return translateSetOperation(expr, BinaryExpression.Op.INTERSECTION, environment);
            case PLUSPLUS           : return translatePlusPlus(expr, environment);
            case EQUALS             : return translateEqComparison(expr, environment);
            case NOT_EQUALS         : return UnaryExpression.Op.NOT.make(translateEqComparison(expr, environment));

            // Set op
            case PLUS               : return translateSetOperation(expr, BinaryExpression.Op.UNION, environment);
            case MINUS              : return translateSetOperation(expr, BinaryExpression.Op.SETMINUS, environment);

            // Arithmetic operators
            case IPLUS              : return translateArithmetic(expr, BinaryExpression.Op.PLUS, environment);
            case IMINUS             : return translateArithmetic(expr, BinaryExpression.Op.MINUS, environment);
            case MUL                : return translateArithmetic(expr, BinaryExpression.Op.MULTIPLY, environment);
            case DIV                : return translateArithmetic(expr, BinaryExpression.Op.DIVIDE, environment);
            case REM                : return translateArithmetic(expr, BinaryExpression.Op.MOD, environment);
            // Comparison operators
            case LT                 : return translateComparison(expr, BinaryExpression.Op.LT, environment);
            case LTE                : return translateComparison(expr, BinaryExpression.Op.LTE, environment);
            case GT                 : return translateComparison(expr, BinaryExpression.Op.GT, environment);
            case GTE                : return translateComparison(expr, BinaryExpression.Op.GTE, environment);
            case IN                 : return isIn(translateSet(expr.left, environment), expr.right, environment);
            case NOT_IN             : return UnaryExpression.Op.NOT.make(isIn(translateSet(expr.left, environment), expr.right, environment.flipped()));
            case IMPLIES            : return BinaryExpression.Op.IMPLIES.make(exprTranslator.translateExpr(expr.left, environment.flipped()), exprTranslator.translateExpr(expr.right, environment));
            case AND                : return BinaryExpression.Op.AND.make(exprTranslator.translateExpr(expr.left, environment), exprTranslator.translateExpr(expr.right, environment));
            case OR                 : return BinaryExpression.Op.OR.make(exprTranslator.translateExpr(expr.left, environment), exprTranslator.translateExpr(expr.right, environment));
            case IFF                : return BinaryExpression.Op.EQ.make(exprTranslator.translateExpr(expr.left, environment.withPolarity(Environment.Polarity.BOTH)),
                                                                         exprTranslator.translateExpr(expr.right, environment.withPolarity(Environment.Polarity.BOTH)));
            case NOT_LT             : return translateComparison(expr, BinaryExpression.Op.GTE, environment);
            case NOT_LTE            : return translateComparison(expr, BinaryExpression.Op.GT, environment);
            case NOT_GT             : return translateComparison(expr, BinaryExpression.Op.LTE, environment);
            case NOT_GTE            : return translateComparison(expr, BinaryExpression.Op.LT, environment);
            case SHL                : throw new UnsupportedOperationException();
            case SHA                : throw new UnsupportedOperationException();
            case SHR                : throw new UnsupportedOperationException();
            default                 : throw new UnsupportedOperationException();
        }
    }

    private Expression translateSet(Expr expr, Environment environment)
    {
        Expression expression = ExprTranslator.toSet(exprTranslator.translateExpr(expr, environment));
        if (expression.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            expression = translator.handleIntConstant(expression);
        }
        return expression;
    }

    private static Expr deNoop(Expr expr)
    {
        while (expr instanceof ExprUnary && ((ExprUnary) expr).op == ExprUnary.Op.NOOP)
        {
            expr = ((ExprUnary) expr).sub;
        }
        return expr;
    }

    /**
     * Translates (left in right), where right may contain multiplicities, following
     * the semantics of TranslateAlloyToKodkod.isIn.
     */
    Expression isIn(Expression left, Expr right, Environment environment)
    {
        right = deNoop(right);
        if (right instanceof ExprUnary)
        {
            ExprUnary unary = (ExprUnary) right;
            switch (unary.op)
            {
                case ONEOF:
                    return BinaryExpression.Op.AND.make(exprTranslator.mkOne(left, environment), isIn(left, unary.sub, environment));
                case LONEOF:
                    return BinaryExpression.Op.AND.make(exprTranslator.mkLone(left, environment), isIn(left, unary.sub, environment));
                case SOMEOF:
                    return BinaryExpression.Op.AND.make(ExprTranslator.mkNonEmpty(left), isIn(left, unary.sub, environment));
                case SETOF:
                    return isIn(left, unary.sub, environment);
                case EXACTLYOF:
                    return BinaryExpression.Op.EQ.make(left, translateSet(unary.sub, environment));
                default:
                    break;
            }
        }
        if (right instanceof ExprBinary && ((ExprBinary) right).op.isArrow && hasMultiplicity(right))
        {
            return isInArrow(left, (ExprBinary) right, environment);
        }
        return ExprTranslator.mkSubset(left, translateSet(right, environment));
    }

    private static boolean hasMultiplicity(Expr expr)
    {
        expr = deNoop(expr);
        if (expr instanceof ExprUnary)
        {
            ExprUnary.Op op = ((ExprUnary) expr).op;
            return op == ExprUnary.Op.ONEOF || op == ExprUnary.Op.LONEOF || op == ExprUnary.Op.SOMEOF || op == ExprUnary.Op.EXACTLYOF;
        }
        if (expr instanceof ExprBinary && ((ExprBinary) expr).op.isArrow)
        {
            ExprBinary binary = (ExprBinary) expr;
            return binary.op != ExprBinary.Op.ARROW || hasMultiplicity(binary.left) || hasMultiplicity(binary.right);
        }
        return false;
    }

    private enum Multiplicity {ANY, SOME, ONE, LONE}

    private static Multiplicity leftMultiplicity(ExprBinary.Op op)
    {
        switch (op)
        {
            case SOME_ARROW_ANY: case SOME_ARROW_SOME: case SOME_ARROW_ONE: case SOME_ARROW_LONE: return Multiplicity.SOME;
            case ONE_ARROW_ANY: case ONE_ARROW_SOME: case ONE_ARROW_ONE: case ONE_ARROW_LONE: return Multiplicity.ONE;
            case LONE_ARROW_ANY: case LONE_ARROW_SOME: case LONE_ARROW_ONE: case LONE_ARROW_LONE: return Multiplicity.LONE;
            default: return Multiplicity.ANY;
        }
    }

    private static Multiplicity rightMultiplicity(ExprBinary.Op op)
    {
        switch (op)
        {
            case ANY_ARROW_SOME: case SOME_ARROW_SOME: case ONE_ARROW_SOME: case LONE_ARROW_SOME: return Multiplicity.SOME;
            case ANY_ARROW_ONE: case SOME_ARROW_ONE: case ONE_ARROW_ONE: case LONE_ARROW_ONE: return Multiplicity.ONE;
            case ANY_ARROW_LONE: case SOME_ARROW_LONE: case ONE_ARROW_LONE: case LONE_ARROW_LONE: return Multiplicity.LONE;
            default: return Multiplicity.ANY;
        }
    }

    private Expression multiplicity(Multiplicity multiplicity, Expression set, Environment environment)
    {
        switch (multiplicity)
        {
            case SOME: return ExprTranslator.mkNonEmpty(set);
            case ONE: return exprTranslator.mkOne(set, environment);
            case LONE: return exprTranslator.mkLone(set, environment);
            default: return BoolConstant.True;
        }
    }

    /**
     * r in A m -> n B is translated into
     * r in A -> B and
     * (all a: A | n a.r and a.r in B) and
     * (all b: B | m r.b and r.b in A)
     * where the inner 'in' handle the multiplicities nested in A and B.
     */
    private Expression isInArrow(Expression relation, ExprBinary arrow, Environment environment)
    {
        Expression A = translateSet(arrow.left, environment);
        Expression B = translateSet(arrow.right, environment);
        Expression result = BinaryExpression.Op.SUBSET.make(relation, BinaryExpression.Op.PRODUCT.make(A, B));

        Multiplicity right = rightMultiplicity(arrow.op);
        if (right != Multiplicity.ANY || hasMultiplicity(arrow.right))
        {
            Expression all = exprTranslator.forAllIn(A, (a, env) ->
            {
                Expression image = ExprTranslator.joinTupleLeft(a, relation);
                return ExprTranslator.and(multiplicity(right, image, env), isInArrowSide(image, arrow.right, env));
            }, environment);
            result = BinaryExpression.Op.AND.make(result, all);
        }

        Multiplicity left = leftMultiplicity(arrow.op);
        if (left != Multiplicity.ANY || hasMultiplicity(arrow.left))
        {
            Expression all = exprTranslator.forAllIn(B, (b, env) ->
            {
                Expression image = ExprTranslator.joinTupleRight(relation, b);
                return ExprTranslator.and(multiplicity(left, image, env), isInArrowSide(image, arrow.left, env));
            }, environment);
            result = BinaryExpression.Op.AND.make(result, all);
        }
        return result;
    }

    // a.r in B is implied by r in A -> B, unless B has multiplicities
    private Expression isInArrowSide(Expression image, Expr side, Environment environment)
    {
        if (hasMultiplicity(side))
        {
            return isIn(image, side, environment);
        }
        return BoolConstant.True;
    }

    // r ++ s = (r - (dom(s) <: r)) + s
    private Expression translatePlusPlus(ExprBinary expr, Environment environment)
    {
        int rightExprArity  =  expr.right.type().arity();
        if( rightExprArity == 1)
        {
            // ++ is like a single + with arity 1 (i.e. is like a union)
            return translateSetOperation(expr, BinaryExpression.Op.UNION, environment);
        }
        Expression left     = translateSet(expr.left, environment);
        Expression right    = translateSet(expr.right, environment);
        List<Sort> sorts    = ExprTranslator.elementSorts(right);

        Expression domain = right;
        for(int i = sorts.size() - 1; i > 0; --i)
        {
            domain = BinaryExpression.Op.JOIN.make(domain, ExprTranslator.universe(sorts.get(i)));
        }
        Expression restriction = BinaryExpression.Op.PRODUCT.make(domain, ExprTranslator.universe(sorts.subList(1, sorts.size())));
        Expression intersection = BinaryExpression.Op.INTERSECTION.make(left, restriction);
        Expression difference   = BinaryExpression.Op.SETMINUS.make(left, intersection);
        return BinaryExpression.Op.UNION.make(difference, right);
    }

    // s <: r = (s x univ x ... x univ) & r
    private Expression translateDomainRestriction(ExprBinary expr, Environment environment)
    {
        Expression left = translateSet(expr.left, environment);
        Expression right = translateSet(expr.right, environment);
        List<Sort> sorts = ExprTranslator.elementSorts(right);
        if (sorts.size() == 1)
        {
            return BinaryExpression.Op.INTERSECTION.make(left, right);
        }
        Expression product = BinaryExpression.Op.PRODUCT.make(left, ExprTranslator.universe(sorts.subList(1, sorts.size())));
        return BinaryExpression.Op.INTERSECTION.make(product, right);
    }

    // r :> s = r & (univ x ... x univ x s)
    private Expression translateRangeRestriction(ExprBinary expr, Environment environment)
    {
        Expression left = translateSet(expr.left, environment);
        Expression right = translateSet(expr.right, environment);
        List<Sort> sorts = ExprTranslator.elementSorts(left);
        if (sorts.size() == 1)
        {
            return BinaryExpression.Op.INTERSECTION.make(left, right);
        }
        Expression product = BinaryExpression.Op.PRODUCT.make(ExprTranslator.universe(sorts.subList(0, sorts.size() - 1)), right);
        return BinaryExpression.Op.INTERSECTION.make(left, product);
    }

    public Expression translateArithmetic(ExprBinary expr, BinaryExpression.Op op, Environment environment)
    {
        Expression leftExpr     = translateSet(expr.left, environment);
        Expression rightExpr    = translateSet(expr.right, environment);
        return exprTranslator.translateArithmetic(leftExpr, rightExpr, op, environment);
    }

    // #set + offset, or the constant offset when set is null
    private static class Count
    {
        final Expr set;
        final int offset;

        Count(Expr set, int offset)
        {
            this.set = set;
            this.offset = offset;
        }
    }

    private static Expr deCast(Expr expr)
    {
        while (expr instanceof ExprUnary && (((ExprUnary) expr).op == ExprUnary.Op.NOOP ||
                ((ExprUnary) expr).op == ExprUnary.Op.CAST2INT || ((ExprUnary) expr).op == ExprUnary.Op.CAST2SIGINT))
        {
            expr = ((ExprUnary) expr).sub;
        }
        return expr;
    }

    /**
     * @return the expression as #set + offset, or null if it is not of that form
     */
    private static Count count(Expr expr)
    {
        expr = deCast(expr);
        if (expr instanceof ExprUnary && ((ExprUnary) expr).op == ExprUnary.Op.CARDINALITY)
        {
            return new Count(((ExprUnary) expr).sub, 0);
        }
        if (expr instanceof ExprConstant && ((ExprConstant) expr).op == ExprConstant.Op.NUMBER)
        {
            return new Count(null, ((ExprConstant) expr).num);
        }
        Expr left = null, right = null;
        boolean plus = true;
        if (expr instanceof ExprBinary && (((ExprBinary) expr).op == ExprBinary.Op.IPLUS || ((ExprBinary) expr).op == ExprBinary.Op.IMINUS))
        {
            left = ((ExprBinary) expr).left;
            right = ((ExprBinary) expr).right;
            plus = ((ExprBinary) expr).op == ExprBinary.Op.IPLUS;
        }
        else if (expr instanceof ExprCall && ((ExprCall) expr).args.size() == 2)
        {
            String name = ((ExprCall) expr).fun.label;
            if (name.equals("integer/plus") || name.equals("integer/add") || name.equals("integer/minus") || name.equals("integer/sub"))
            {
                left = ((ExprCall) expr).args.get(0);
                right = ((ExprCall) expr).args.get(1);
                plus = name.equals("integer/plus") || name.equals("integer/add");
            }
        }
        if (left == null)
        {
            return null;
        }
        Count l = count(left), r = count(right);
        if (l == null || r == null)
        {
            return null;
        }
        if (r.set == null)
        {
            return new Count(l.set, plus ? l.offset + r.offset : l.offset - r.offset);
        }
        if (l.set == null && plus)
        {
            return new Count(r.set, l.offset + r.offset);
        }
        return null;
    }

    // whether the comparison involves the cardinality of a set
    private static boolean isCardinality(ExprBinary expr)
    {
        Count left = count(expr.left), right = count(expr.right);
        return left != null && right != null && (left.set != null || right.set != null);
    }

    private static BinaryExpression.Op flip(BinaryExpression.Op op)
    {
        switch (op)
        {
            case LT: return BinaryExpression.Op.GT;
            case LTE: return BinaryExpression.Op.GTE;
            case GT: return BinaryExpression.Op.LT;
            case GTE: return BinaryExpression.Op.LTE;
            default: return op;
        }
    }

    /**
     * Translates #A + k op #B + m where A or B may be absent (constants).
     */
    private Expression translateCardinality(ExprBinary expr, BinaryExpression.Op op, Environment environment)
    {
        Count left = count(expr.left), right = count(expr.right);
        if (left.set == null)
        {
            // c op #B + m is #B flip(op) c - m
            return translateCardinalityComparison(right.set, flip(op), left.offset - right.offset, environment);
        }
        if (right.set == null)
        {
            return translateCardinalityComparison(left.set, op, right.offset - left.offset, environment);
        }
        // #A op #B + d
        int d = right.offset - left.offset;
        if (environment.existsIsSkolemized())
        {
            return compareCardinalities(left.set, right.set, op, d, environment);
        }
        if (environment.forallIsSkolemized() && op != BinaryExpression.Op.EQ)
        {
            // not (#A op #B + d) is #A negated(op) #B + d, whose existential is skolemized
            BinaryExpression.Op negated;
            switch (op)
            {
                case LT: negated = BinaryExpression.Op.GTE; break;
                case LTE: negated = BinaryExpression.Op.GT; break;
                case GT: negated = BinaryExpression.Op.LTE; break;
                default: negated = BinaryExpression.Op.LT; break;
            }
            return UnaryExpression.Op.NOT.make(compareCardinalities(left.set, right.set, negated, d, environment.flipped()));
        }
        throw new UnsupportedOperationException("The comparison of cardinalities is only supported where it is skolemized: " + expr);
    }

    private Expression translateCardinalityComparison(Expr set, BinaryExpression.Op op, int n, Environment environment)
    {
        return exprTranslator.translateCardinalityComparison(translateSet(set, environment), op, n, environment);
    }

    /**
     * #A op #B + d for unary sets A and B, in a position where existentials are skolemized:
     * a fresh relation f in A -> B is a partial bijection that is total on A or onto B. Then
     * #A - #B = #(A - dom(f)) - #(B - ran(f)), where one of these differences is empty.
     */
    private Expression compareCardinalities(Expr leftSet, Expr rightSet, BinaryExpression.Op op, int d, Environment environment)
    {
        Expression A = translateSet(leftSet, environment);
        Expression B = translateSet(rightSet, environment);
        if (ExprTranslator.elementSorts(A).size() != 1 || ExprTranslator.elementSorts(B).size() != 1)
        {
            throw new UnsupportedOperationException("The comparison of cardinalities is only supported for unary sets");
        }
        SetSort sort = (SetSort) BinaryExpression.Op.PRODUCT.make(A, B).getSort();
        FunctionDeclaration f = new FunctionDeclaration(edu.uiowa.smt.TranslatorUtils.getNewSetName(), sort);
        translator.smtProgram.addFunction(f);
        Expression relation = f.getVariable();
        Expression transpose = UnaryExpression.Op.TRANSPOSE.make(relation);
        Expression subset = BinaryExpression.Op.SUBSET.make(relation, BinaryExpression.Op.PRODUCT.make(A, B));
        // functional: ~f.f in iden, injective: f.~f in iden
        Expression functional = BinaryExpression.Op.SUBSET.make(BinaryExpression.Op.JOIN.make(transpose, relation), UnaryExpression.Op.IDEN.make(B));
        Expression injective = BinaryExpression.Op.SUBSET.make(BinaryExpression.Op.JOIN.make(relation, transpose), UnaryExpression.Op.IDEN.make(A));
        Expression domain = BinaryExpression.Op.JOIN.make(relation, B);
        Expression range = BinaryExpression.Op.JOIN.make(A, relation);
        Expression restA = BinaryExpression.Op.SETMINUS.make(A, domain);
        Expression restB = BinaryExpression.Op.SETMINUS.make(B, range);
        // total on A: #A - #B = -#restB, so #A op #B + d iff #restB flip(op) -d
        Expression totalOnA = BinaryExpression.Op.AND.make(ExprTranslator.mkEmpty(restA),
                exprTranslator.translateCardinalityComparison(restB, flip(op), -d, environment));
        // onto B: #A - #B = #restA, so #A op #B + d iff #restA op d
        Expression ontoB = BinaryExpression.Op.AND.make(ExprTranslator.mkEmpty(restB),
                exprTranslator.translateCardinalityComparison(restA, op, d, environment));
        return BinaryExpression.Op.AND.make(BinaryExpression.Op.AND.make(subset, BinaryExpression.Op.AND.make(functional, injective)),
                BinaryExpression.Op.OR.make(totalOnA, ontoB));
    }

    private Expression translateComparison(ExprBinary expr, BinaryExpression.Op op, Environment environment)
    {
        if (isCardinality(expr))
        {
            return translateCardinality(expr, op, environment);
        }
        Expression leftExpr     = translateSet(expr.left, environment);
        Expression rightExpr    = translateSet(expr.right, environment);
        return getComparison(op, leftExpr, rightExpr);
    }

    // A op B holds iff A = {x} and B = {y} and x op y
    private Expression getComparison(BinaryExpression.Op op, Expression left, Expression right)
    {
        translator.translateIntegerAssertions();
        VariableDeclaration x = new VariableDeclaration("__x__", AbstractTranslator.uninterpretedInt, null);
        VariableDeclaration y = new VariableDeclaration("__y__", AbstractTranslator.uninterpretedInt, null);
        Expression xTuple     = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, x.getVariable());
        Expression yTuple     = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, y.getVariable());
        Expression xSingleton = UnaryExpression.Op.SINGLETON.make(xTuple);
        Expression ySingleton = UnaryExpression.Op.SINGLETON.make(yTuple);
        Expression xValue     = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, x.getVariable());
        Expression yValue     = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, y.getVariable());

        Expression relation1EqualsX = BinaryExpression.Op.EQ.make(xSingleton, left);
        Expression relation2EqualsY = BinaryExpression.Op.EQ.make(ySingleton, right);
        Expression and1 = BinaryExpression.Op.AND.make(relation1EqualsX, relation2EqualsY);

        Expression comparison = op.make(xValue, yValue);
        Expression and2 = BinaryExpression.Op.AND.make(and1, comparison);
        return QuantifiedExpression.Op.EXISTS.make(and2, Arrays.asList(x, y));
    }

    private Expression translateEqComparison(ExprBinary expr, Environment environment)
    {
        if (isCardinality(expr))
        {
            return translateCardinality(expr, BinaryExpression.Op.EQ, environment);
        }
        Environment both    = environment.withPolarity(Environment.Polarity.BOTH);
        Expression left     = exprTranslator.translateExpr(expr.left, both);
        Expression right    = exprTranslator.translateExpr(expr.right, both);
        if (left.getSort() == AbstractTranslator.boolSort)
        {
            return BinaryExpression.Op.EQ.make(left, right);
        }
        left = normalizeSet(left);
        right = normalizeSet(right);
        // {a} = {b} is a = b
        if (left instanceof UnaryExpression && ((UnaryExpression) left).getOP() == UnaryExpression.Op.SINGLETON &&
            right instanceof UnaryExpression && ((UnaryExpression) right).getOP() == UnaryExpression.Op.SINGLETON)
        {
            return BinaryExpression.Op.EQ.make(((UnaryExpression) left).getExpression(), ((UnaryExpression) right).getExpression());
        }
        return BinaryExpression.Op.EQ.make(left, right);
    }

    private Expression normalizeSet(Expression expression)
    {
        expression = ExprTranslator.toSet(expression);
        if (expression.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            expression = translator.handleIntConstant(expression);
        }
        return expression;
    }

    private Expression translateSetOperation(ExprBinary expr, BinaryExpression.Op op, Environment environment)
    {
        Expression left     = translateSet(expr.left, environment);
        Expression right    = translateSet(expr.right, environment);
        return op.make(left, right);
    }

    public Expression mkTupleSelectExpr(Expression tupleExpr, int index)
    {
        return BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(index), tupleExpr);
    }
}
