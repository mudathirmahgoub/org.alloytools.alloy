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
import edu.uiowa.smt.smtAst.*;

import java.util.ArrayList;
import java.util.List;

public class ExprUnaryTranslator
{
    final ExprTranslator exprTranslator;
    final Alloy2SmtTranslator translator;
    public ExprUnaryTranslator(ExprTranslator exprTranslator)
    {
        this.exprTranslator = exprTranslator;
        this.translator = exprTranslator.translator;
    }

    Expression translateExprUnary(ExprUnary exprUnary, Environment environment)
    {
        switch (exprUnary.op)
        {
            case NOOP       : return translateNoop(exprUnary, environment);
            case NO         : return ExprTranslator.mkEmpty(translateSet(exprUnary.sub, environment));
            case SOME       : return ExprTranslator.mkNonEmpty(translateSet(exprUnary.sub, environment));
            case ONE        : return exprTranslator.mkOne(translateSet(exprUnary.sub, environment), environment);
            case LONE       : return exprTranslator.mkLone(translateSet(exprUnary.sub, environment), environment);
            // multiplicities are only meaningful in declarations and on the right of 'in',
            // where they are handled by ExprBinaryTranslator.isIn. Elsewhere they denote the set.
            case ONEOF      :
            case LONEOF     :
            case SOMEOF     :
            case SETOF      :
            case EXACTLYOF  : return exprTranslator.translateExpr(exprUnary.sub, environment);
            case CARDINALITY: throw new UnsupportedOperationException("Cardinality is only supported when compared with a constant integer");
            case TRANSPOSE  : return UnaryExpression.Op.TRANSPOSE.make(translateSet(exprUnary.sub, environment));
            case CLOSURE    : return UnaryExpression.Op.TCLOSURE.make(translateSet(exprUnary.sub, environment));
            case RCLOSURE   : return translateReflexiveClosure(exprUnary, environment);
            case NOT        : return translateNot(exprUnary, environment);
            case CAST2INT   : return exprTranslator.translateExpr(exprUnary.sub, environment);
            case CAST2SIGINT: return exprTranslator.translateExpr(exprUnary.sub, environment);
            default:
            {
                throw new UnsupportedOperationException("Not supported yet: " + exprUnary.op);
            }
        }
    }

    /**
     * Pushes the negation inside quantifiers, connectives, let, if-then-else and predicate calls
     * (negation normal form). In particular (not (some x | f)) becomes (all x | not f), which
     * is solved much faster with set quantifiers than (not (set.some ...)).
     */
    private Expression translateNot(ExprUnary not, Environment environment)
    {
        Expr sub = not.sub;
        while (sub instanceof ExprUnary && ((ExprUnary) sub).op == ExprUnary.Op.NOOP)
        {
            sub = ((ExprUnary) sub).sub;
        }
        Expr negated = null;
        if (sub instanceof ExprUnary && ((ExprUnary) sub).op == ExprUnary.Op.NOT)
        {
            negated = ((ExprUnary) sub).sub;
        }
        else if (sub instanceof ExprQt)
        {
            ExprQt exprQt = (ExprQt) sub;
            switch (exprQt.op)
            {
                case ALL: negated = ExprQt.Op.SOME.make(exprQt.pos, null, exprQt.decls, exprQt.sub.not()); break;
                case SOME: negated = ExprQt.Op.ALL.make(exprQt.pos, null, exprQt.decls, exprQt.sub.not()); break;
                case NO: negated = ExprQt.Op.SOME.make(exprQt.pos, null, exprQt.decls, exprQt.sub); break;
                default: break;
            }
        }
        else if (sub instanceof ExprList && (((ExprList) sub).op == ExprList.Op.AND || ((ExprList) sub).op == ExprList.Op.OR))
        {
            ExprList list = (ExprList) sub;
            ExprList.Op dual = list.op == ExprList.Op.AND ? ExprList.Op.OR : ExprList.Op.AND;
            List<Expr> arguments = new ArrayList<>();
            for (Expr argument : list.args)
            {
                arguments.add(argument.not());
            }
            negated = ExprList.make(list.pos, null, dual, arguments);
        }
        else if (sub instanceof ExprBinary)
        {
            ExprBinary binary = (ExprBinary) sub;
            switch (binary.op)
            {
                case AND: negated = ExprBinary.Op.OR.make(binary.pos, null, binary.left.not(), binary.right.not()); break;
                case OR: negated = ExprBinary.Op.AND.make(binary.pos, null, binary.left.not(), binary.right.not()); break;
                case IMPLIES: negated = ExprBinary.Op.AND.make(binary.pos, null, binary.left, binary.right.not()); break;
                default: break;
            }
        }
        else if (sub instanceof ExprLet)
        {
            ExprLet let = (ExprLet) sub;
            negated = ExprLet.make(let.pos, let.var, let.expr, let.sub.not());
        }
        else if (sub instanceof ExprITE && sub.type().is_bool)
        {
            ExprITE ite = (ExprITE) sub;
            negated = ExprITE.make(ite.pos, ite.cond, ite.left.not(), ite.right.not());
        }
        else if (sub instanceof ExprCall && ((ExprCall) sub).fun.isPred && !translator.functionsMap.containsKey(((ExprCall) sub).fun.label))
        {
            return exprTranslator.translateExprCall((ExprCall) sub, environment, true);
        }
        if (negated != null)
        {
            return exprTranslator.translateExpr(negated, environment);
        }
        return UnaryExpression.Op.NOT.make(exprTranslator.translateExpr(sub, environment.flipped()));
    }

    private Expression translateSet(Expr expr, Environment environment)
    {
        return ExprTranslator.toSet(exprTranslator.translateExpr(expr, environment));
    }

    private Expression translateReflexiveClosure(ExprUnary exprUnary, Environment environment)
    {
        Expression relation = translateSet(exprUnary.sub, environment);
        Expression closure = UnaryExpression.Op.TCLOSURE.make(relation);
        Sort sort = ExprTranslator.elementSorts(relation).get(0);
        Expression identity = sort.equals(AbstractTranslator.atomSort) ?
                AbstractTranslator.atomIdentity.getVariable() :
                UnaryExpression.Op.IDEN.make(ExprTranslator.universe(sort));
        return BinaryExpression.Op.UNION.make(closure, identity);
    }

    private Expression translateNoop(ExprUnary exprUnary, Environment environment)
    {
        if(exprUnary.sub instanceof Sig)
        {
            // alloy built in signatures include: univ, none, iden
            if(((Sig) exprUnary.sub).builtin)
            {
                switch (((Sig) exprUnary.sub).label)
                {
                    case "univ": return AbstractTranslator.atomUniverse.getVariable();
                    case "iden": return AbstractTranslator.atomIdentity.getVariable();
                    case "none": return AbstractTranslator.atomNone.getVariable();
                    case "Int":  return AbstractTranslator.intUniv.getVariable();
                    default:
                        throw new UnsupportedOperationException(((Sig) exprUnary.sub).label);
                }
            }
            else
            {
                return translator.signaturesMap.get(((Sig) exprUnary.sub)).getVariable();
            }
        }

        if(exprUnary.sub instanceof Sig.Field)
        {
            return translator.fieldsMap.get(((Sig.Field) exprUnary.sub)).getVariable();
        }

        if(exprUnary.sub instanceof ExprVar)
        {
            String varName = ((ExprVar)exprUnary.sub).label;

            if(environment.containsKey(varName))
            {
                // variables bound to tuples or atoms denote singleton sets
                return ExprTranslator.toSet(environment.get(varName));
            }
            throw new RuntimeException("Something is wrong: we do not have variable in scope - " + varName);
        }

        return exprTranslator.translateExpr(exprUnary.sub, environment);
    }

    public MultiArityExpression mkTupleExpr(List<VariableDeclaration> bdVarDecls)
    {
        return MultiArityExpression.Op.MKTUPLE.make(bdVarDecls.stream().map(VariableDeclaration::getVariable)
                                                              .map(v -> (Expression) v).collect(java.util.stream.Collectors.toList()));
    }
}
