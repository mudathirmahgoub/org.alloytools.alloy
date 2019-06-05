package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Map;

public class ExprBinaryTranslator
{
    final ExprTranslator exprTranslator;
    final Alloy2SmtTranslator translator;

    public ExprBinaryTranslator(ExprTranslator exprTranslator)
    {
        this.exprTranslator = exprTranslator;
        translator = exprTranslator.translator;
    }

    Expression translateExprBinary(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        switch (expr.op)
        {
            case ARROW              : return translateArrow(expr, variablesScope);
            case ANY_ARROW_SOME     : throw new UnsupportedOperationException();
            case ANY_ARROW_ONE      : return translateAnyArrowOne(expr, variablesScope);
            case ANY_ARROW_LONE     : return translateAnyArrowLone(expr, variablesScope);
            case SOME_ARROW_ANY     : throw new UnsupportedOperationException();
            case SOME_ARROW_SOME    : return translateSomeArrowSome(expr, variablesScope);
            case SOME_ARROW_ONE     : return translateSomeArrowOne(expr, variablesScope);
            case SOME_ARROW_LONE    : return translateSomeArrowLone(expr, variablesScope);
            case ONE_ARROW_ANY      : return translateOneArrowAny(expr, variablesScope);
            case ONE_ARROW_SOME     : return translateOneArrowSome(expr, variablesScope);
            case ONE_ARROW_ONE      : return translateOneArrowOne(expr, variablesScope);
            case ONE_ARROW_LONE     : return translateOneArrowLone(expr, variablesScope);
            case LONE_ARROW_ANY     : return translateLoneArrowAny(expr, variablesScope);
            case LONE_ARROW_SOME    : return translateLoneArrowSome(expr, variablesScope);
            case LONE_ARROW_ONE     : return translateLoneArrowOne(expr, variablesScope);
            case LONE_ARROW_LONE    : return translateLoneArrowLone(expr, variablesScope);
            case ISSEQ_ARROW_LONE   : throw new UnsupportedOperationException();
            
            // Relational operators
            case JOIN               : return translateJoin(expr, variablesScope);
            case DOMAIN             : return translateDomainRestriction(expr, variablesScope);
            case RANGE              : return translateRangeRestriction(expr, variablesScope);
            case INTERSECT          : return translateSetOperation(expr, BinaryExpression.Op.INTERSECTION, variablesScope);
            case PLUSPLUS           : return translatePlusPlus(expr, variablesScope);
            case EQUALS             : return translateEqComparison(expr, BinaryExpression.Op.EQ, variablesScope);
            case NOT_EQUALS         : return new UnaryExpression(UnaryExpression.Op.NOT, translateEqComparison(expr, BinaryExpression.Op.EQ, variablesScope));

            // Set op
            case PLUS               : return translateSetOperation(expr, BinaryExpression.Op.UNION, variablesScope);
            case MINUS              : return translateSetOperation(expr, BinaryExpression.Op.SETMINUS, variablesScope);
            
            // Arithmetic operators            
            case IPLUS              : return translateArithmetic(expr, BinaryExpression.Op.PLUS, variablesScope);
            case IMINUS             : return translateArithmetic(expr, BinaryExpression.Op.MINUS, variablesScope);
            case MUL                : return translateArithmetic(expr, BinaryExpression.Op.MULTIPLY, variablesScope);
            case DIV                : return translateArithmetic(expr, BinaryExpression.Op.DIVIDE, variablesScope);
            case REM                : return translateArithmetic(expr, BinaryExpression.Op.MOD, variablesScope);
            // Comparison operators
            case LT                 : return translateComparison(expr, BinaryExpression.Op.LT, variablesScope);
            case LTE                : return translateComparison(expr, BinaryExpression.Op.LTE, variablesScope);
            case GT                 : return translateComparison(expr, BinaryExpression.Op.GT, variablesScope);
            case GTE                : return translateComparison(expr, BinaryExpression.Op.GTE, variablesScope);
            case IN                 : return translateSubsetOperation(expr, BinaryExpression.Op.SUBSET, variablesScope);
            case NOT_IN             : return translateSubsetOperation(expr, null, variablesScope);
            case IMPLIES            : return translateImplies(expr, variablesScope);            
            case AND                : return translateAnd(expr, variablesScope);
            case OR                 : return translateOr(expr, variablesScope);
            case IFF                : return translateEqComparison(expr, BinaryExpression.Op.EQ, variablesScope);
            case NOT_LT             : return translateComparison(expr, BinaryExpression.Op.GTE, variablesScope);
            case NOT_LTE            : return translateComparison(expr, BinaryExpression.Op.GT, variablesScope);
            case NOT_GT             : return translateComparison(expr, BinaryExpression.Op.LTE, variablesScope);
            case NOT_GTE            : return translateComparison(expr, BinaryExpression.Op.LT, variablesScope);
            case SHL                : throw new UnsupportedOperationException();
            case SHA                : throw new UnsupportedOperationException();
            case SHR                : throw new UnsupportedOperationException();            
            default                 : throw new UnsupportedOperationException();
        }
    }

    private Expression translateOneArrowOne(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A one -> one B
        // and
        // forall x in A . exists y in B . xy in multiplicitySet and
        //       forall v in B. v != y implies xv not in  multiplicitySet
        // and
        // forall y in B . exists x in A . xy in multiplicitySet and
        //       forall u in A. u != x implies uy not in  multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateOneArrowSome(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);

        // multiplicitySet subset of A one -> some B
        // and
        // forall x in A . exists y in B . xy in multiplicitySet
        // and
        // forall y in B . exists x in A . xy in multiplicitySet and
        //       forall u in A. u != x implies uy not in  multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateOneArrowAny(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);

        // multiplicitySet subset of A one -> set B
        // and
        // forall y in B . exists x in A . xy in multiplicitySet and
        //       forall u in A. u != x implies uy not in  multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateSomeArrowOne(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A some -> one B
        // and
        // forall x in A . exists y in B . xy in multiplicitySet and
        //       forall v in B. v != y implies xv not in  multiplicitySet
        // and
        // forall y in B . exists x in A . xy in multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateAnyArrowOne(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A set -> one B
        // and
        // forall x in A . exists y in B . xy in multiplicitySet and
        //       forall v in B. v != y implies xv not in  multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        return multiplicitySet.getVariable();
    }

    private Expression translateSomeArrowSome(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A some -> some B
        // and
        // forall x in A . exists y in B . xy in multiplicitySet
        // and
        // forall y in B . exists x in A . xy in multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression existsYBody = new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember);
        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateOneArrowLone(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A one -> lone B
        // and
        // forall x in A .
        //      (forall y in B. xy not in multiplicitySet)
        //      or
        //      (exists y in B . xy in multiplicitySet and
        //          forall v in B. v != y implies xv not in  multiplicitySet)
        // and
        // forall y in B . exists x in A . xy in multiplicitySet and
        //       forall u in A. u != x implies uy not in  multiplicitySet


        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression lone = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, y),
                BinaryExpression.Op.OR, existsY);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, lone);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateSomeArrowLone(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A some -> lone B
        // and
        // forall x in A .
        //      (forall y in B. xy not in multiplicitySet)
        //      or
        //      (exists y in B . xy in multiplicitySet and
        //          forall v in B. v != y implies xv not in  multiplicitySet)
        // and
        // forall y in B . exists x in A . xy in multiplicitySet

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression lone = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, y),
                BinaryExpression.Op.OR, existsY);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, lone);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, existsX);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateAnyArrowLone(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A set -> lone B
        // and
        // forall x in A .
        //      (forall y in B. xy not in multiplicitySet)
        //      or
        //      (exists y in B . xy in multiplicitySet and
        //          forall v in B. v != y implies xv not in  multiplicitySet)

        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression lone = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, y),
                BinaryExpression.Op.OR, existsY);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, lone);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));
        return multiplicitySet.getVariable();
    }

    private Expression translateLoneArrowLone(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A lone -> lone B
        // and
        // forall x in A .
        //      (forall y in B. xy not in multiplicitySet)
        //      or
        //      (exists y in B . xy in multiplicitySet and
        //          forall v in B. v != y implies xv not in  multiplicitySet)
        // and
        // forall y in B.
        //      (forall x in A. xy not in multiplicitySet)
        //      or
        //      (exists x in A . xy in multiplicitySet and
        //          forall u in A. u != x implies uy not in  multiplicitySet)


        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression loneWest = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, y),
                BinaryExpression.Op.OR, existsY);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, loneWest);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression loneEast = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, x),
                BinaryExpression.Op.OR, existsX);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, loneEast);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateLoneArrowOne(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        VariableDeclaration v = new VariableDeclaration("_v", BSort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression vMemberB = new BinaryExpression(v.getVariable(), BinaryExpression.Op.MEMBER, B);

        // multiplicitySet subset of A lone -> one B
        // and
        // forall x in A .
        //      (exists y in B . xy in multiplicitySet and
        //          forall v in B. v != y implies xv not in  multiplicitySet)
        // and
        // forall y in B.
        //      (forall x in A. xy not in multiplicitySet)
        //      or
        //      (exists x in A . xy in multiplicitySet and
        //          forall u in A. u != x implies uy not in  multiplicitySet)


        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression xvTuple = getTupleConcatenation(ASort, BSort, x, v);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression xvMember = new BinaryExpression(xvTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notXV = new UnaryExpression(UnaryExpression.Op.NOT, xvMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);

        Expression vEqualY = new BinaryExpression(v.getVariable(), BinaryExpression.Op.EQ, y.getVariable());
        Expression notVEqualY = new UnaryExpression(UnaryExpression.Op.NOT, vEqualY);

        Expression vImplies = new BinaryExpression(
                new BinaryExpression(vMemberB, BinaryExpression.Op.AND, notVEqualY),
                BinaryExpression.Op.IMPLIES, notXV);
        Expression forAllV = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, vImplies, v);

        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(
                new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllV);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression loneEast = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, x),
                BinaryExpression.Op.OR, existsX);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, loneEast);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateLoneArrowSome(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);

        // multiplicitySet subset of A lone -> some B
        // and
        // forall x in A .
        //      (exists y in B . xy in multiplicitySet
        // and
        // forall y in B.
        //      (forall x in A. xy not in multiplicitySet)
        //      or
        //      (exists x in A . xy in multiplicitySet and
        //          forall u in A. u != x implies uy not in  multiplicitySet)


        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);


        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);

        Expression existsYBody = new BinaryExpression(yMemberB, BinaryExpression.Op.AND, xyMember);

        Expression existsY = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsYBody, y);
        Expression xImplies = new BinaryExpression(xMemberA, BinaryExpression.Op.IMPLIES, existsY);
        Expression forAllX = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, xImplies, x);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " west", forAllX));

        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression loneEast = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, x),
                BinaryExpression.Op.OR, existsX);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, loneEast);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression translateLoneArrowAny(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        FunctionDeclaration multiplicitySet = translator.multiplicityVariableMap.get(expr);

        if(multiplicitySet != null)
        {
            return multiplicitySet.getVariable();
        }

        SetSort sort = new SetSort(new TupleSort(AlloyUtils.getExprSorts(expr)));
        multiplicitySet = new FunctionDeclaration(TranslatorUtils.getNewSetName(), sort);
        translator.multiplicityVariableMap.put(expr, multiplicitySet);
        translator.smtProgram.addFunction(multiplicitySet);

        Expression A = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression B = exprTranslator.translateExpr(expr.right, variablesScope);

        Expression product = new BinaryExpression(A, BinaryExpression.Op.PRODUCT, B);
        Expression subset = new BinaryExpression(multiplicitySet.getVariable(), BinaryExpression.Op.SUBSET, product);

        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " subset constraint", subset));

        SetSort ASort = (SetSort) A.getSort();
        SetSort BSort = (SetSort) B.getSort();

        VariableDeclaration x = new VariableDeclaration("_x", ASort.elementSort);
        VariableDeclaration y = new VariableDeclaration("_y", BSort.elementSort);
        Expression xMemberA = new BinaryExpression(x.getVariable(), BinaryExpression.Op.MEMBER, A);
        Expression yMemberB = new BinaryExpression(y.getVariable(), BinaryExpression.Op.MEMBER, B);

        VariableDeclaration u = new VariableDeclaration("_u", ASort.elementSort);
        Expression uMemberA = new BinaryExpression(u.getVariable(), BinaryExpression.Op.MEMBER, A);

        // multiplicitySet subset of A lone -> set B
        // and
        // forall y in B.
        //      (forall x in A. xy not in multiplicitySet)
        //      or
        //      (exists x in A . xy in multiplicitySet and
        //          forall u in A. u != x implies uy not in  multiplicitySet)


        Expression xyTuple = getTupleConcatenation(ASort, BSort, x, y);
        Expression uyTuple = getTupleConcatenation(ASort, BSort, u, y);

        Expression xyMember = new BinaryExpression(xyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());
        Expression uyMember = new BinaryExpression(uyTuple, BinaryExpression.Op.MEMBER, multiplicitySet.getVariable());

        Expression notXY = new UnaryExpression(UnaryExpression.Op.NOT, xyMember);
        Expression notUY = new UnaryExpression(UnaryExpression.Op.NOT, uyMember);


        Expression uEqualX = new BinaryExpression(u.getVariable(), BinaryExpression.Op.EQ, x.getVariable());
        Expression notUEqualX = new UnaryExpression(UnaryExpression.Op.NOT, uEqualX);

        Expression uImplies = new BinaryExpression(
                new BinaryExpression(uMemberA, BinaryExpression.Op.AND, notUEqualX),
                BinaryExpression.Op.IMPLIES, notUY);
        Expression forAllU = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, uImplies, u);
        Expression existsXBody = new BinaryExpression(
                new BinaryExpression(xMemberA, BinaryExpression.Op.AND, xyMember),
                BinaryExpression.Op.AND, forAllU);

        Expression existsX = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, existsXBody, x);
        Expression loneEast = new BinaryExpression(
                new QuantifiedExpression(QuantifiedExpression.Op.FORALL, notXY, x),
                BinaryExpression.Op.OR, existsX);
        Expression yImplies = new BinaryExpression(yMemberB, BinaryExpression.Op.IMPLIES, loneEast);
        Expression forAllY = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, yImplies, y);
        translator.smtProgram.addAssertion(new Assertion(expr.toString() + " east", forAllY));

        return multiplicitySet.getVariable();
    }

    private Expression getTupleConcatenation(SetSort ASort, SetSort BSort, VariableDeclaration x, VariableDeclaration y)
    {
        List<Expression> tupleElements = new ArrayList<>();
        for(int i = 0; i < ((TupleSort) ASort.elementSort).elementSorts.size(); i++)
        {
            IntConstant index = IntConstant.getInstance(i);
            tupleElements.add(new BinaryExpression(index, BinaryExpression.Op.TUPSEL, x.getVariable()));
        }

        for(int i = 0; i < ((TupleSort) BSort.elementSort).elementSorts.size(); i++)
        {
            IntConstant index = IntConstant.getInstance(i);
            tupleElements.add(new BinaryExpression(index, BinaryExpression.Op.TUPSEL, y.getVariable()));
        }

        return new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, tupleElements);
    }

    private Expression translateImplies(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);
        Expression implExpr  = new BinaryExpression(left, BinaryExpression.Op.IMPLIES, right);

        return implExpr;
    }
    
    private Expression translateAnd(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);
        Expression andExpr  = new BinaryExpression(left, BinaryExpression.Op.AND, right);

        return andExpr;
    }

    private Expression translateOr(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);
        Expression orExpr  = new BinaryExpression(left, BinaryExpression.Op.OR, right);

        return orExpr;
    }    
    
    private Expression translateArrow(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);
        Expression product  = new BinaryExpression(left, BinaryExpression.Op.PRODUCT, right);

        return product;
    }

    private Expression translatePlusPlus(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        int rightExprArity  =  expr.right.type().arity();
        if( rightExprArity == 1)
        {
            // ++ is like a single + with arity 1 (i.e. is like a union)
            return translateSetOperation(expr, BinaryExpression.Op.UNION, variablesScope);
        }
        else 
        {
            Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
            Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);
            Expression join     = right;            
            
            for(int i = 0; i < rightExprArity-1; ++i)
            {
                join = new BinaryExpression(join, BinaryExpression.Op.JOIN, exprTranslator.translator.atomUniverse.getVariable());
            }
            for(int i = 0; i < rightExprArity-1; ++i)
            {
                join = new BinaryExpression(join, BinaryExpression.Op.PRODUCT, exprTranslator.translator.atomUniverse.getVariable());
            }            
            
            Expression intersection         = new BinaryExpression(join, BinaryExpression.Op.INTERSECTION, left);
            Expression difference           = new BinaryExpression(left, BinaryExpression.Op.SETMINUS, intersection);
            Expression union                = new BinaryExpression(difference, BinaryExpression.Op.UNION, right);

            return union;

        }
    }

    private Expression translateDomainRestriction(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        int arity = expr.right.type().arity();

        if(arity <= 1)
        {
            // arity should be greater than one
            throw new UnsupportedOperationException();
        }
        else
        {
            Expression left = exprTranslator.translateExpr(expr.left, variablesScope);
            Expression right = exprTranslator.translateExpr(expr.right, variablesScope);

            for(int i = 0; i < arity - 1; ++i)
            {
                left = new BinaryExpression(left, BinaryExpression.Op.PRODUCT, exprTranslator.translator.atomUniverse.getVariable());
            }
            BinaryExpression    intersection    = new BinaryExpression(left, BinaryExpression.Op.INTERSECTION, right);
            return intersection;
        }
    }

    private Expression translateRangeRestriction(ExprBinary expr, Map<String,Expression> variablesScope)
    {
        int arity = expr.left.type().arity();

        if(arity <= 1)
        {
            // arity should be greater than one
            throw new UnsupportedOperationException();
        }
        else
        {
            Expression left  = exprTranslator.translateExpr(expr.left, variablesScope);
            Expression right = exprTranslator.translateExpr(expr.right, variablesScope);
            
            for(int i = 0; i < arity - 1; ++i)
            {
                right = new BinaryExpression(exprTranslator.translator.atomUniverse.getVariable(), BinaryExpression.Op.PRODUCT, right);
            }            

            BinaryExpression    intersection    = new BinaryExpression(left, BinaryExpression.Op.INTERSECTION, right);

            return intersection;
        }
    }

    public Expression translateArithmetic(ExprBinary expr, BinaryExpression.Op op, Map<String,Expression> variablesScope)
    {
        Expression leftExpr     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression rightExpr    = exprTranslator.translateExpr(expr.right, variablesScope);    
        
        //if(!exprTranslator.translator.arithmeticOperations.containsKey(op))
        // {
            // how about avoiding declaring any arithmetic relation?
            // exprTranslator.declArithmeticOp(op);
        // }

        FunctionDeclaration result = new FunctionDeclaration(TranslatorUtils.getNewSetName(), AbstractTranslator.setOfUninterpretedIntTuple);
        exprTranslator.translator.smtProgram.addFunction(result);

        VariableDeclaration x = new VariableDeclaration("_x", AbstractTranslator.uninterpretedInt);
        VariableDeclaration y = new VariableDeclaration("_y", AbstractTranslator.uninterpretedInt);
        VariableDeclaration z = new VariableDeclaration("_z", AbstractTranslator.uninterpretedInt);

        Expression xTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, x.getVariable());
        Expression yTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, y.getVariable());
        Expression zTuple = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, z.getVariable());

        Expression xValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, x.getVariable());
        Expression yValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, y.getVariable());
        Expression zValue = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, z.getVariable());

        Expression xMember = new BinaryExpression(xTuple, BinaryExpression.Op.MEMBER, leftExpr);
        Expression yMember = new BinaryExpression(yTuple, BinaryExpression.Op.MEMBER, rightExpr);
        Expression zMember = new BinaryExpression(zTuple, BinaryExpression.Op.MEMBER, result.getVariable());

        Expression xyOperation = new BinaryExpression(xValue, op, yValue);
        Expression equal = new BinaryExpression(xyOperation, BinaryExpression.Op.EQ, zValue);

        Expression and1 = new BinaryExpression(xMember, BinaryExpression.Op.AND, yMember);
        Expression and2 = new BinaryExpression(equal, BinaryExpression.Op.AND, and1);
        Expression exists1 = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, and2, x, y);
        Expression implies1 = new BinaryExpression(zMember, BinaryExpression.Op.IMPLIES, exists1);
        Expression forall1 = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, implies1, z);

        Assertion assertion1 = new Assertion(String.format("%1$s %2$s %3$s axiom1", op, leftExpr, rightExpr), forall1);
        exprTranslator.translator.smtProgram.addAssertion(assertion1);

        Expression and3 = new BinaryExpression(equal, BinaryExpression.Op.MEMBER,zMember);
        Expression exists2 = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, and3, z);

        Expression implies2 = new BinaryExpression(and1, BinaryExpression.Op.IMPLIES, exists2);
        Expression forall2 = new QuantifiedExpression(QuantifiedExpression.Op.FORALL, implies2, x, y);

        Assertion assertion2 = new Assertion(String.format("%1$s %2$s %3$s axiom2", op, leftExpr, rightExpr), forall2);
        exprTranslator.translator.smtProgram.addAssertion(assertion2);

        return result.getVariable();
    }
    
    private Expression translateComparison(ExprBinary expr, BinaryExpression.Op op, Map<String,Expression> variablesScope)
    {
        Expression comparisonExpr = null;   
        
        // Right hand side is a expression and right hand side is a constant
        if(((expr.left instanceof ExprUnary) && ((ExprUnary)expr.left).op == ExprUnary.Op.CARDINALITY && 
                (expr.right instanceof ExprConstant)))
        {            
            int n               = ((ExprConstant)expr.right).num;  
            int arity           = ((ExprUnary)expr.left).sub.type().arity();                                    
            Expression leftExpr = exprTranslator.translateExpr(((ExprUnary)expr.left).sub, variablesScope);    
            
            List<Expression>                existentialBdVarExprs   = new ArrayList<>();               
            List<VariableDeclaration>  existentialBdVars       = new ArrayList<>();
            List<Sort> leftExprSorts = AlloyUtils.getExprSorts(((ExprUnary)expr.left).sub);
            
            switch(op)
            {
                case GT:{  
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(leftExprSorts.get(0), n+1);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(leftExprSorts, arity, n+1);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        

                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  rightExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        rightExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        rightExpr = exprTranslator.mkEmptyRelationOfSort(leftExprSorts);
                    }
                    
                    // rightExpr + 1 <= leftExpr
                    comparisonExpr = new BinaryExpression(rightExpr, BinaryExpression.Op.SUBSET, leftExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }                    
                    break;
                }
                case LT:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(leftExprSorts.get(0), n-1);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(leftExprSorts, arity, n-1);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  rightExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        rightExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        rightExpr = exprTranslator.mkEmptyRelationOfSort(leftExprSorts);
                    }
                    
                    // leftExpr <= rightExpr-1
                    comparisonExpr = new BinaryExpression(leftExpr, BinaryExpression.Op.SUBSET, rightExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    } 
                    break;
                }
                case GTE:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(leftExprSorts.get(0), n);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(leftExprSorts, arity, n);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  rightExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        rightExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        rightExpr = exprTranslator.mkEmptyRelationOfSort(leftExprSorts);
                    }
                    
                    // rightExpr <= leftExpr
                    comparisonExpr = new BinaryExpression(rightExpr, BinaryExpression.Op.SUBSET, leftExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }
                    break;
                }
                case LTE:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(leftExprSorts.get(0), n);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(leftExprSorts, arity, n);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  rightExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        rightExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        rightExpr = exprTranslator.mkEmptyRelationOfSort(leftExprSorts);
                    }
                    
                    // rightExpr <= leftExpr
                    comparisonExpr = new BinaryExpression(leftExpr, BinaryExpression.Op.SUBSET, rightExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }
                    break;                    
                }                
                default:break;
            }  
        }
        else if((expr.right instanceof ExprUnary) && ((ExprUnary)expr.right).op == ExprUnary.Op.CARDINALITY && 
                (expr.left instanceof ExprConstant)) 
        {
            int n               = ((ExprConstant)expr.left).num;  
            int arity           = ((ExprUnary)expr.right).sub.type().arity();                                    
            Expression rightExpr = exprTranslator.translateExpr(((ExprUnary)expr.right).sub, variablesScope);    
            
            List<Expression>                existentialBdVarExprs   = new ArrayList<>();               
            List<VariableDeclaration>  existentialBdVars       = new ArrayList<>();
            List<Sort> rightExprSorts = AlloyUtils.getExprSorts(((ExprUnary)expr.right).sub);
            
            switch(op)
            {
                case GT:{  
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(rightExprSorts.get(0), n+1);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(rightExprSorts, arity, n+1);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        

                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  leftExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        leftExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        leftExpr = exprTranslator.mkEmptyRelationOfSort(rightExprSorts);
                    }
                    
                    // rightExpr + 1 <= leftExpr
                    comparisonExpr = new BinaryExpression(rightExpr, BinaryExpression.Op.SUBSET, leftExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }                    
                    break;
                }
                case LT:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(rightExprSorts.get(0), n-1);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(rightExprSorts, arity, n-1);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  leftExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        leftExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        leftExpr = exprTranslator.mkEmptyRelationOfSort(rightExprSorts);
                    }
                    
                    // leftExpr <= rightExpr-1
                    comparisonExpr = new BinaryExpression(rightExpr, BinaryExpression.Op.SUBSET, leftExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    } 
                    break;
                }
                case GTE:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(rightExprSorts.get(0), n);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(rightExprSorts, arity, n);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  leftExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        leftExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        leftExpr = exprTranslator.mkEmptyRelationOfSort(rightExprSorts);
                    }
                    
                    // rightExpr <= leftExpr
                    comparisonExpr = new BinaryExpression(rightExpr, BinaryExpression.Op.SUBSET, leftExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }
                    break;
                }
                case LTE:{
                    if(arity == 1)
                    {
                        existentialBdVars = exprTranslator.getBdVars(rightExprSorts.get(0), n);
                    }
                    else
                    {
                        existentialBdVars = exprTranslator.getBdTupleVars(rightExprSorts, arity, n);
                    }

                    for(VariableDeclaration bdVar : existentialBdVars)
                    {
                        existentialBdVarExprs.add(bdVar.getVariable());
                    }        
                    
                    // (distinct e1 e2 e3 ....)
                    Expression distElementsExpr = TranslatorUtils.makeDistinct(existentialBdVarExprs);

                    exprTranslator.translator.existentialBdVars.addAll(existentialBdVars);        
                    if(exprTranslator.translator.auxExpr != null)
                    {
                        exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distElementsExpr);
                    }
                    else
                    {
                        exprTranslator.translator.auxExpr = distElementsExpr;
                    }
     
                    Expression  leftExpr;

                    if(existentialBdVarExprs.size() > 0)
                    {
                        leftExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(existentialBdVarExprs);
                    }
                    else
                    {
                        leftExpr = exprTranslator.mkEmptyRelationOfSort(rightExprSorts);
                    }
                    
                    // leftExpr <= rightExpr 
                    comparisonExpr = new BinaryExpression(leftExpr, BinaryExpression.Op.SUBSET, rightExpr);
                    comparisonExpr = new BinaryExpression(comparisonExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    
                    if(!exprTranslator.translator.existentialBdVars.isEmpty())
                    {
                        comparisonExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, comparisonExpr);
                    }
                    break;                    
                }                
                default:break;
            }  
        }
        else 
        {
            Expression leftExpr     = exprTranslator.translateExpr(expr.left, variablesScope);
            Expression rightExpr    = exprTranslator.translateExpr(expr.right, variablesScope);


            comparisonExpr =  getComparison(op, leftExpr, rightExpr);
        }

        //ToDo: review the purpose of these 2 lines.
        exprTranslator.translator.auxExpr = null;
        exprTranslator.translator.existentialBdVars.clear();
        return comparisonExpr;     
    }
    
    private Expression getComparison(BinaryExpression.Op op, Expression left, Expression right)
    {
        VariableDeclaration x = new VariableDeclaration("_x", AbstractTranslator.uninterpretedInt);
        VariableDeclaration y = new VariableDeclaration("_y", AbstractTranslator.uninterpretedInt);
        Expression xTuple     = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, x.getVariable());
        Expression yTuple     = new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, y.getVariable());
        Expression xSingleton = new UnaryExpression(UnaryExpression.Op.SINGLETON, xTuple);
        Expression ySingleton = new UnaryExpression(UnaryExpression.Op.SINGLETON, yTuple);
        Expression xValue     = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, x.getVariable());
        Expression yValue     = new FunctionCallExpression(AbstractTranslator.uninterpretedIntValue, y.getVariable());

        Expression relation1EqualsX = new BinaryExpression(xSingleton, BinaryExpression.Op.EQ, left);
        Expression relation2EqualsY = new BinaryExpression(ySingleton, BinaryExpression.Op.EQ, right);
        Expression and1 = new BinaryExpression(relation1EqualsX, BinaryExpression.Op.AND, relation2EqualsY);

        Expression comparison = new BinaryExpression(xValue, op, yValue);
        Expression and2 = new BinaryExpression(and1, BinaryExpression.Op.AND, comparison);
        Expression exists = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, Arrays.asList(x, y), and2);

        //ToDo: remove these 2 lines
//        Assertion assertion = new Assertion(left + " " + op + " " + right , exists);
//        exprTranslator.translator.smtProgram.addAssertion(assertion);
        return exists;
    }
    
    private Expression translateEqComparison(ExprBinary expr, BinaryExpression.Op op, Map<String,Expression> variablesScope)
    {

        if(   (expr.left instanceof ExprUnary &&
                ((ExprUnary) expr.left).op == ExprUnary.Op.CARDINALITY) ||
                (expr.right instanceof ExprUnary &&
                ((ExprUnary) expr.right).op == ExprUnary.Op.CARDINALITY)
            )
        {
            return translateCardinality(expr, op, variablesScope);
        }


        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);

        if(left instanceof Variable &&
                (!(((Variable)left).getDeclaration().getSort() instanceof SetSort)))
        {
            left = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) left);
        }
        else if(left instanceof MultiArityExpression &&
                ((MultiArityExpression)left).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            left = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)left);
        }
        if(right instanceof Variable &&
                (!(((Variable)right).getDeclaration().getSort() instanceof SetSort)))
        {
            right = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) right);
        }
        else if(right instanceof MultiArityExpression &&
                ((MultiArityExpression)right).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            right = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)right);
        }

        if(left.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            left = exprTranslator.translator.handleIntConstant(left);
        }

        if(right.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            right = exprTranslator.translator.handleIntConstant(right);
        }



        Expression finalExpr = new BinaryExpression(left, BinaryExpression.Op.EQ, right);


        if(!exprTranslator.translator.existentialBdVars.isEmpty())
        {
            if(exprTranslator.translator.auxExpr != null)
            {
                finalExpr = new BinaryExpression(finalExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                exprTranslator.translator.auxExpr = null;
            }
            finalExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, finalExpr);
        }                
        return finalExpr;        
    }

    private Expression translateCardinality(ExprBinary expr, BinaryExpression.Op op , Map<String, Expression> variablesScope)
    {
        // CVC4 doesn't support comparison  between 2 cardinality expressions
        if
            (   expr.left instanceof ExprUnary &&
                ((ExprUnary) expr.left).op == ExprUnary.Op.CARDINALITY &&
                expr.right instanceof ExprUnary &&
                ((ExprUnary) expr.right).op == ExprUnary.Op.CARDINALITY
            )
        {
            throw new UnsupportedOperationException("CVC4 doesn't support comparision between 2 cardinality expressions.");
        }

        if
            (
                (expr.left instanceof ExprUnary &&
                ((ExprUnary) expr.left).op == ExprUnary.Op.CARDINALITY &&
                (!(expr.right instanceof ExprConstant &&
                        ((ExprConstant) expr.right).op == ExprConstant.Op.NUMBER))) ||
                (expr.right instanceof ExprUnary &&
                ((ExprUnary) expr.right).op == ExprUnary.Op.CARDINALITY &&
                (!(expr.left instanceof ExprConstant &&
                        ((ExprConstant) expr.left).op == ExprConstant.Op.NUMBER)))
            )
        {
            throw new UnsupportedOperationException("CVC4 only supports cardinality with numbers");
        }


        // translate cardinality differently
        if
            (   (expr.left instanceof ExprUnary &&
                ((ExprUnary) expr.left).op == ExprUnary.Op.CARDINALITY)
            )
        {
            int         n           = ((ExprConstant)expr.right).num;
            Expression  equality = translateEqCardComparison((ExprUnary) expr.left, n, op, variablesScope);
            return equality;
        }

        if
            (   (expr.right instanceof ExprUnary &&
                ((ExprUnary) expr.right).op == ExprUnary.Op.CARDINALITY)
            )
        {
            int         n           = ((ExprConstant)expr.left).num;
            Expression  equality = translateEqCardComparison((ExprUnary) expr.right, n, op, variablesScope);
            return equality;
        }

        throw new UnsupportedOperationException();
    }

    private Expression translateEqCardComparison(ExprUnary expr, int n, BinaryExpression.Op op ,Map<String, Expression> variablesScope)
    {
        Expression expression = exprTranslator.translateExpr(expr.sub, variablesScope);
        if(n == 0)
        {
            // the set expression is empty
            Expression empty = new UnaryExpression(UnaryExpression.Op.EMPTYSET, expression.getSort());
            Expression equal = new BinaryExpression(expression, BinaryExpression.Op.EQ, empty);
            return equal;
        }
        int arity = expr.sub.type().arity();
        List<Expression> variables = new ArrayList<>();
        List<VariableDeclaration> declarations;
        List<Sort> exprSorts = AlloyUtils.getExprSorts(expr.sub);
        
        if(arity == 1)
        {
            declarations = exprTranslator.getBdVars(exprSorts.get(0), n);
        }
        else
        {
            declarations = exprTranslator.getBdTupleVars(exprSorts, arity, n);
        }
        
        for(VariableDeclaration declaration : declarations)
        {
            variables.add(declaration.getVariable());
        }

        Expression distinct;

        if(variables.size() == 1)
        {
            // distinct operator needs at least 2 arguments
            distinct = new BoolConstant(true);
        }
        else
        {
            distinct = new MultiArityExpression(MultiArityExpression.Op.DISTINCT, variables);
        }
        
        //ToDo: review the need of existentialBdVars in AlloyTranslator
        exprTranslator.translator.existentialBdVars.addAll(declarations);

        if(exprTranslator.translator.auxExpr != null)
        {
            exprTranslator.translator.auxExpr = new BinaryExpression(exprTranslator.translator.auxExpr, BinaryExpression.Op.AND, distinct);
        }
        else
        {
            exprTranslator.translator.auxExpr = distinct;
        }
        
        Expression  distElementSetExpr = exprTranslator.mkUnaryRelationOutOfAtomsOrTuples(variables);
        Expression  right   = distElementSetExpr;
        
        switch (op)
        {
            case EQ :
            {
                Expression eqExpr = new BinaryExpression(expression, BinaryExpression.Op.EQ, right);
                
                if(exprTranslator.translator.auxExpr != null)
                {
                    eqExpr = new BinaryExpression(eqExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                    exprTranslator.translator.auxExpr = null;
                }
                if(!exprTranslator.translator.existentialBdVars.isEmpty())
                {
                    eqExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, declarations, eqExpr);
                    exprTranslator.translator.existentialBdVars.clear();
                }
                return eqExpr;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Expression translateSetOperation(ExprBinary expr, BinaryExpression.Op op, Map<String, Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);

        if(left instanceof Variable &&
                (!(((Variable)left).getDeclaration().getSort() instanceof SetSort)))
        {
            left = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) left);
        }
        else if(left instanceof MultiArityExpression &&
                ((MultiArityExpression)left).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            left = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)left);
        }
        if(right instanceof Variable &&
                (!(((Variable)right).getDeclaration().getSort() instanceof SetSort)))
        {
            right = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) right);
        }
        else if(right instanceof MultiArityExpression &&
                ((MultiArityExpression)right).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            right = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)right);
        } 

        BinaryExpression operation = new BinaryExpression(left, op, right);
        return operation;
    }
    
    private Expression translateSubsetOperation(ExprBinary expr, BinaryExpression.Op op, Map<String, Expression> variablesScope)
    {
        Expression left     = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression right    = exprTranslator.translateExpr(expr.right, variablesScope);

        if(left instanceof Variable &&
                (!(((Variable)left).getDeclaration().getSort() instanceof SetSort)))
        {
            left = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) left);
        }
        else if(left instanceof MultiArityExpression &&
                ((MultiArityExpression)left).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            left = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)left);
        }
        if(right instanceof Variable &&
                (!(((Variable)right).getDeclaration().getSort() instanceof SetSort)))
        {
            right = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) right);
        }
        else if(right instanceof MultiArityExpression &&
                ((MultiArityExpression)right).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            right = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)right);
        }

        if(left.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            left = exprTranslator.translator.handleIntConstant(left);
        }

        if(right.getSort().equals(AbstractTranslator.setOfIntSortTuple))
        {
            right = exprTranslator.translator.handleIntConstant(right);
        }
                
        Expression finalExpr = new BinaryExpression(left, BinaryExpression.Op.SUBSET, right);
        
        if(op == null)
        {
            finalExpr = new UnaryExpression(UnaryExpression.Op.NOT, finalExpr);
        }
        if(!exprTranslator.translator.existentialBdVars.isEmpty())
        {
            if(exprTranslator.translator.auxExpr != null)
            {
                finalExpr = new BinaryExpression(finalExpr, BinaryExpression.Op.AND, exprTranslator.translator.auxExpr);
                exprTranslator.translator.auxExpr = null;
            }
            finalExpr = new QuantifiedExpression(QuantifiedExpression.Op.EXISTS, exprTranslator.translator.existentialBdVars, finalExpr);
            exprTranslator.translator.existentialBdVars.clear();
        }                
        return finalExpr;                 
    }

    private Expression translateJoin(ExprBinary expr, Map<String, Expression> variablesScope)
    {
        Expression          left    = exprTranslator.translateExpr(expr.left, variablesScope);
        Expression          right   = exprTranslator.translateExpr(expr.right, variablesScope);

        if(left instanceof Variable &&
                (!(((Variable)left).getDeclaration().getSort() instanceof SetSort)))
        {
            left = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) left);
        }
        else if(left instanceof MultiArityExpression &&
                ((MultiArityExpression)left).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            left = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)left);
        }
        if(right instanceof Variable &&
                (!(((Variable)right).getDeclaration().getSort() instanceof SetSort)))
        {
            right = AlloyUtils.mkSingletonOutOfTupleOrAtom((Variable) right);
        }
        else if(right instanceof MultiArityExpression &&
                ((MultiArityExpression)right).getOp() == MultiArityExpression.Op.MKTUPLE)
        {
            right = AlloyUtils.mkSingletonOutOfTuple((MultiArityExpression)right);
        }        
        BinaryExpression    join    = new BinaryExpression(left, BinaryExpression.Op.JOIN, right);
        return join;
    }
    
    public Expression mkTupleSelectExpr(Expression tupleExpr, int index)
    {
        return new BinaryExpression(IntConstant.getInstance(index), BinaryExpression.Op.TUPSEL, tupleExpr);
    }
}
