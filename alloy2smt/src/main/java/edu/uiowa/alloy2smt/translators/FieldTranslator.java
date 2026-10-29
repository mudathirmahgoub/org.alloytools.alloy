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
import edu.uiowa.smt.smtAst.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FieldTranslator
{

    private final Alloy2SmtTranslator translator;

    public FieldTranslator(Alloy2SmtTranslator translator)
    {
        this.translator = translator;
    }

    void translateFields()
    {
        for (Sig sig : translator.reachableSigs)
        {
            List<Sig.Field> fields = sig.getFields().makeCopy();
            for (Sig.Field f : fields)
            {
                declare(f);
            }
        }
        // field constraints may refer to fields of other signatures
        for (Sig sig : translator.reachableSigs)
        {
            List<Sig.Field> fields = sig.getFields().makeCopy();
            for (Sig.Field f : fields)
            {
                translateConstraints(f);
            }

            translateDisjointFields(sig, fields);

            translateDisjoint2FieldValues(sig, fields);
        }
    }

    private void translateDisjointFields(Sig sig, List<Sig.Field> fields)
    {
        // translate disjoint fields
        for (Decl decl: sig.getFieldDecls())
        {
            // disjoint fields
            if (decl.disjoint != null && decl.names.size() > 1)
            {
                for (int i = 0; i < decl.names.size() - 1; i++)
                {
                    Expression fieldI = getFieldExpression(fields, decl.names.get(i).label);
                    for (int j = i + 1; j < decl.names.size(); j++)
                    {
                        Expression fieldJ = getFieldExpression(fields, decl.names.get(j).label);
                        Expression intersect = BinaryExpression.Op.INTERSECTION.make(fieldI, fieldJ);
                        Expression emptySet = UnaryExpression.Op.EMPTYSET.make(fieldI.getSort());
                        Expression equal = BinaryExpression.Op.EQ.make(intersect, emptySet);
                        Assertion disjoint = new Assertion(String.format("disj %1$s, %2$s", decl.names.get(i), decl.names.get(j)), equal);
                        translator.smtProgram.addAssertion(disjoint);
                    }
                }
            }
        }
    }

    private void translateDisjoint2FieldValues(Sig sig, List<Sig.Field> fields)
    {
        // sig S {f: disj e}
        // all disj a, b: S | no a.f & b.f
        for (Decl decl: sig.getFieldDecls())
        {
            if (decl.disjoint2 == null)
            {
                continue;
            }
            for (ExprHasName name: decl.names)
            {
                Expression field = getFieldExpression(fields, name.label);
                Expression signature = translator.signaturesMap.get(sig).getVariable();
                Expression pairs = BinaryExpression.Op.PRODUCT.make(signature, signature);
                Expression all = translator.exprTranslator.forAllIn(pairs, (pair, env) ->
                {
                    Expression a = MultiArityExpression.Op.MKTUPLE.make(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), pair));
                    Expression b = MultiArityExpression.Op.MKTUPLE.make(BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(1), pair));
                    Expression notEqual = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(a, b));
                    Expression intersect = BinaryExpression.Op.INTERSECTION.make(
                            ExprTranslator.joinTupleLeft(a, field), ExprTranslator.joinTupleLeft(b, field));
                    return BinaryExpression.Op.IMPLIES.make(notEqual, ExprTranslator.mkEmpty(intersect));
                }, new edu.uiowa.smt.Environment());
                translator.smtProgram.addAssertion(new Assertion(sig.label + "." + name.label + " disjoint values", all));
            }
        }
    }

    private Expression getFieldExpression(List<Sig.Field> fields, String label)
    {
        Optional<Sig.Field> field =  fields.stream()
            .filter(f -> f.label.equals(label))
            .findFirst();
        if(!field.isPresent())
        {
            throw new RuntimeException("Can not find field " + label);
        }
        Expression expression = translator.fieldsMap.get(field.get()).getVariable();
        return expression;
    }

    void declare(Sig.Field field)
    {

        String      fieldName   = field.sig.label + "/" + field.label;
        List<Sort>  fieldSorts  = new ArrayList<>();

        for (Sig sig : field.type().fold().get(0))
        {
            if(sig.type().is_int())
            {
                fieldSorts.add(AbstractTranslator.uninterpretedInt);
            }
            else
            {
                fieldSorts.add(AbstractTranslator.atomSort);
            }
        }

        FunctionDeclaration fieldDeclaration = new FunctionDeclaration(fieldName, new SetSort(new TupleSort(fieldSorts)));
        // declare a variable for the field
        translator.smtProgram.addFunction(fieldDeclaration);
        translator.fieldsMap.put(field, fieldDeclaration);
    }

    /**
     * sig S {f: e} is translated into
     * f in S -> univ -> ... -> univ and
     * all this: S | this.f in e
     * where e may refer to 'this' and contain multiplicities
     */
    private void translateConstraints(Sig.Field field)
    {
        FunctionDeclaration declaration = translator.fieldsMap.get(field);
        List<Sort> sorts = ((TupleSort) ((SetSort) declaration.getSort()).elementSort).elementSorts;
        Expression signature = translator.signaturesMap.get(field.sig).getVariable();
        Expression product = BinaryExpression.Op.PRODUCT.make(signature, ExprTranslator.universe(sorts.subList(1, sorts.size())));
        Expression subset = BinaryExpression.Op.SUBSET.make(declaration.getVariable(), product);
        translator.smtProgram.addAssertion(new Assertion(field.toString() + " subset", subset));

        Expr bound = field.decl().expr;
        if (isTrivialBound(bound))
        {
            return;
        }
        ExprVar thisVar = (ExprVar) field.sig.decl.get();
        Expr join = ExprBinary.Op.JOIN.make(null, null, thisVar, field);
        Expr in = ExprBinary.Op.IN.make(null, null, join, bound);
        Expr all = ExprQt.Op.ALL.make(null, null, Collections.singletonList(field.sig.decl), in);
        Expression multiplicity = translator.exprTranslator.translateExpr(all);
        translator.smtProgram.addAssertion(new Assertion(field.toString() + " multiplicity", multiplicity));
    }

    // f: set univ, f: univ -> univ, ... add nothing to f in S -> univ -> ... -> univ
    private static boolean isTrivialBound(Expr bound)
    {
        while (bound instanceof ExprUnary && (((ExprUnary) bound).op == ExprUnary.Op.NOOP || ((ExprUnary) bound).op == ExprUnary.Op.SETOF))
        {
            bound = ((ExprUnary) bound).sub;
        }
        if (bound instanceof ExprBinary && ((ExprBinary) bound).op == ExprBinary.Op.ARROW)
        {
            return isTrivialBound(((ExprBinary) bound).left) && isTrivialBound(((ExprBinary) bound).right);
        }
        return bound == Sig.UNIV;
    }
}
