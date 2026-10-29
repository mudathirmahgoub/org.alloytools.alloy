/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.Sig;
import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.Environment;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SignatureTranslator
{

    private final Alloy2SmtTranslator translator;
    private final FieldTranslator fieldTranslator;

    private List<Map.Entry<Sig, Expr>> translatedSigFacts = new ArrayList<>();

    public SignatureTranslator(Alloy2SmtTranslator translator)
    {
        this.translator = translator;
        this.fieldTranslator = new FieldTranslator(translator);
    }

    private void translateSignatureHierarchies()
    {
        for (Sig sig : translator.reachableSigs)
        {
            if(sig instanceof Sig.PrimSig)
            {
                Sig.PrimSig primSig = (Sig.PrimSig) sig;

                if (primSig.isAbstract != null)
                {
                    SafeList<Sig.PrimSig> children = primSig.children();
                    if (children.size() > 0)
                    {
                        Expression left = translator.signaturesMap.get(sig).getVariable();
                        Expression union = translator.signaturesMap.get(children.get(0)).getVariable();

                        for (int i = 1; i < children.size(); i++)
                        {
                            Expression expression = translator.signaturesMap.get(children.get(i)).getVariable();
                            union = BinaryExpression.Op.UNION.make(union, expression);
                        }

                        BinaryExpression equality = BinaryExpression.Op.EQ.make(left, union);
                        Assertion isAbstract = new Assertion("abstract " + primSig.toString(), equality);
                        translator.smtProgram.addAssertion(isAbstract);
                    }
                }
            }
        }

        if (translator.topLevelSigs.size() > 0)
        {
            // The union of all top-level sigs equals to the universe
            Expression unionTopSigExprs = translator.signaturesMap.get(translator.topLevelSigs.get(0)).getVariable();

            for (int i = 1; i < translator.topLevelSigs.size(); ++i)
            {
                unionTopSigExprs = BinaryExpression.Op.UNION.make(unionTopSigExprs, translator.signaturesMap.get(translator.topLevelSigs.get(i)).getVariable());
            }
            translator.smtProgram.addAssertion(new Assertion(BinaryExpression.Op.EQ.make(unionTopSigExprs, translator.atomUniverse.getVariable())));

        }
    }

    private void translateDisjointSignatures(List<Sig.PrimSig> signatures)
    {
        for (int i = 0; i < signatures.size(); i++)
        {
            Sig signature = signatures.get(i);
            Expression left = translator.signaturesMap.get(signature).getVariable();

            for (int j = i + 1; j < signatures.size(); j++)
            {
                Expression right = translator.signaturesMap.get(signatures.get(j)).getVariable();
                BinaryExpression disjoint = BinaryExpression.Op.INTERSECTION.make(left, right);
                BinaryExpression equality = BinaryExpression.Op.EQ.make(disjoint, translator.atomNone.getVariable());

                translator.smtProgram.addAssertion(new Assertion("Disjoint signatures", equality));
            }
            // recursively add disjoint constraints to children
            if(signature instanceof Sig.PrimSig)
            {
                Sig.PrimSig primSig = (Sig.PrimSig) signature;
                List<Sig.PrimSig> children = primSig.children().makeCopy()
                        .stream().collect(Collectors.toList());
                translateDisjointSignatures(children);
            }
        }
    }

    private void collectReachableSigs()
    {
        for (Sig sig : translator.alloyModel.getAllReachableSigs())
        {
            if (sig.builtin)
            {
                continue;
            }
            if (sig.isTopLevel())
            {
                translator.topLevelSigs.add(sig);
            }
            translator.reachableSigs.add(sig);
        }
    }

    private void translateSignatures()
    {
        for (Sig sig : translator.reachableSigs)
        {
            if (sig.type().is_int())
            {
                FunctionDeclaration functionDeclaration = declareUnaryIntFunction(sig.toString());
                translator.signaturesMap.put(sig, functionDeclaration);
            }
            else
            {
                FunctionDeclaration functionDeclaration = declareUnaryAtomFunction(sig.toString());
                translator.signaturesMap.put(sig, functionDeclaration);
            }
        }

        for (Sig sig : translator.reachableSigs)
        {
            FunctionDeclaration functionDeclaration = translator.signaturesMap.get(sig);

            // if sig extends another signature
            if (!sig.isTopLevel())
            {
                translateSigSubsetParent(sig, functionDeclaration);
            }

            if (sig.isOne != null)
            {
                translateSignatureOneMultiplicity(sig);
            }
            else if (sig.isLone != null)
            {
                translateSignatureLoneMultiplicity(sig);
            }
            else if (sig.isSome != null)
            {
                translateSignatureSomeMultiplicity(sig);
            }

            for (Expr expr : sig.getFacts())
            {
                translator.sigFacts.add(new java.util.AbstractMap.SimpleEntry<>(sig, expr));
            }
        }

        List<Sig.PrimSig> primSigs = translator.topLevelSigs
                .stream().map(s -> (Sig.PrimSig) s).collect(Collectors.toList());
        translateDisjointSignatures(primSigs);
    }

    private void translateSignatureOneMultiplicity(Sig sig)
    {
        Expression expr;
        ConstantDeclaration constDecl;
        Boolean isInt = sig.type().is_int();
        String name = TranslatorUtils.getNewName();
        FunctionDeclaration signature = translator.signaturesMap.get(sig);

        if (isInt)
        {
            constDecl = new ConstantDeclaration(name, translator.uninterpretedInt);
        }
        else
        {
            constDecl = new ConstantDeclaration(name, translator.atomSort);
        }
        expr = AlloyUtils.mkSingletonOutOfTuple(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, constDecl.getVariable()));
        translator.smtProgram.addConstantDeclaration(constDecl);

        BinaryExpression subset = BinaryExpression.Op.EQ.make(signature.getVariable(), expr);
        translator.smtProgram.addAssertion(new Assertion(subset));
    }

    private void translateSignatureLoneMultiplicity(Sig sig)
    {
        Expression expr;
        ConstantDeclaration constDecl;
        Boolean isInt = sig.type().is_int();
        String name = TranslatorUtils.getNewName();
        FunctionDeclaration signature = translator.signaturesMap.get(sig);

        if (isInt)
        {
            constDecl = new ConstantDeclaration(name, translator.uninterpretedInt);
            expr = AlloyUtils.mkSingletonOutOfTuple(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, constDecl.getVariable()));
        }
        else
        {
            constDecl = new ConstantDeclaration(name, translator.atomSort);
            expr = AlloyUtils.mkSingletonOutOfTuple(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, constDecl.getVariable()));
        }
        translator.smtProgram.addConstantDeclaration(constDecl);

        BinaryExpression subset = BinaryExpression.Op.SUBSET.make(signature.getVariable(), expr);
        translator.smtProgram.addAssertion(new Assertion(subset));
    }

    private void translateSignatureSomeMultiplicity(Sig sig)
    {
        Expression expr;
        ConstantDeclaration constDecl;
        Boolean isInt = sig.type().is_int();
        String name = TranslatorUtils.getNewName();
        FunctionDeclaration signature = translator.signaturesMap.get(sig);

        if (isInt)
        {
            constDecl = new ConstantDeclaration(name, translator.uninterpretedInt);
            expr = AlloyUtils.mkSingletonOutOfTuple(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, constDecl.getVariable()));
        }
        else
        {
            constDecl = new ConstantDeclaration(name, translator.atomSort);
            expr = AlloyUtils.mkSingletonOutOfTuple(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, constDecl.getVariable()));
        }
        translator.smtProgram.addConstantDeclaration(constDecl);

        BinaryExpression subset = BinaryExpression.Op.SUBSET.make(expr, signature.getVariable());
        translator.smtProgram.addAssertion(new Assertion(subset));
    }


    private void translateSigSubsetParent(Sig sig, FunctionDeclaration functionDeclaration)
    {
        if (sig instanceof Sig.PrimSig)
        {
            Sig parent = ((Sig.PrimSig) sig).parent;
            FunctionDeclaration parentDeclaration = translator.signaturesMap.get(parent);
            BinaryExpression subsetExpression = BinaryExpression.Op.SUBSET.make(functionDeclaration.getVariable(), parentDeclaration.getVariable());
            translator.smtProgram.addAssertion(new Assertion(subsetExpression));
        }
        else
        {
            List<Sig> parents = ((Sig.SubsetSig) sig).parents;

            // sig A in B + C or sig A = B + C
            BinaryExpression.Op op = ((Sig.SubsetSig) sig).exact ? BinaryExpression.Op.EQ : BinaryExpression.Op.SUBSET;
            Expression union = translator.signaturesMap.get(parents.get(0)).getVariable();
            for (int i = 1; i < parents.size(); i++)
            {
                Expression expression = translator.signaturesMap.get(parents.get(i)).getVariable();
                union = BinaryExpression.Op.UNION.make(union, expression);
            }
            translator.smtProgram.addAssertion(new Assertion(op.make(functionDeclaration.getVariable(), union)));
        }
    }

    private void declareIntSig()
    {
        translator.signaturesMap.put(Sig.SIGINT, translator.intUniv);
        BinaryExpression eqExpr = BinaryExpression.Op.EQ.make(translator.intUnivExpr, translator.intUniv.getVariable());
        translator.smtProgram.addFunction(translator.intUniv);
        translator.smtProgram.addAssertion(new Assertion(eqExpr));
    }


    private FunctionDeclaration declareUnaryAtomFunction(String varName)
    {
        FunctionDeclaration declaration = null;
        if (varName != null)
        {
            declaration = new FunctionDeclaration(varName, translator.setOfUnaryAtomSort);
            translator.smtProgram.addFunction(declaration);
        }
        return declaration;
    }

    private FunctionDeclaration declareUnaryIntFunction(String varName)
    {
        FunctionDeclaration declaration = null;
        if (varName != null)
        {
            declaration = new FunctionDeclaration(varName, translator.setOfUninterpretedIntTuple);
            translator.smtProgram.addFunction(declaration);
        }
        return declaration;
    }

    public void translateSigs()
    {
        collectReachableSigs();
        translateSignatures();
        translateSignatureHierarchies();
        this.fieldTranslator.translateFields();
    }

    void translateSpecialSigFacts()
    {
        // Translate facts on signatures
        for (Map.Entry<Sig, Expr> sigFact : translator.sigFacts)
        {
            Environment environment = new Environment();
            // the ordering sig Ord is a one sig, this denotes the sig itself
            environment.put("this", translator.signaturesMap.get(sigFact.getKey()).getVariable());
            Expr expr = sigFact.getValue();

            // handle total order operator differently
            if (expr instanceof ExprUnary &&
                    ((ExprUnary) expr).sub instanceof ExprList &&
                    ((ExprList) ((ExprUnary) expr).sub).op == ExprList.Op.TOTALORDER)
            {
                translateTotalOrder(sigFact.getKey(), ((ExprList) ((ExprUnary) expr).sub), environment);
                this.translatedSigFacts.add(sigFact);
            }
        }
    }


    void translateSigFacts()
    {
        // Translate facts on signatures
        for (Map.Entry<Sig, Expr> sigFact : translator.sigFacts)
        {
            // ignore already translated signature facts
            if (this.translatedSigFacts.contains(sigFact))
            {
                continue;
            }
            // sig S {...}{F} means all this: S | F
            Sig sig = sigFact.getKey();
            Expr all = ExprQt.Op.ALL.make(null, null, Collections.singletonList(sig.decl), sigFact.getValue());
            Expression expression = translator.exprTranslator.translateExpr(all);
            translator.smtProgram.addAssertion(new Assertion("Fact for sig: " + sig, expression));
        }
    }

    /**
     * util/ordering: the ordered set is mapped injectively into the integers by the
     * mapping (prefix)IntMapping, and Ord.Next relates each element to the
     * element with the next larger integer.
     */
    private void translateTotalOrder(Sig ordSig, ExprList exprList, Environment environment)
    {
        if (exprList.args.size() != 3)
        {
            throw new UnsupportedOperationException();
        }

        Expr set = exprList.args.get(0);
        Expr first = exprList.args.get(1);

        // define a new uninterpreted one-to-one mapping from the signature to integers
        String label = ordSig.label;
        // convert ordA/Ord to ordA/
        String prefix = label.replaceFirst("/Ord$", "/");
        String mappingName = prefix + "IntMapping";

        Expression setExpression = translator.exprTranslator.translateExpr(set, environment);
        Expression firstExpression = translator.exprTranslator.translateExpr(first, environment);

        FunctionDeclaration mapping = defineInjectiveMapping(mappingName, setExpression);

        // ordering/first
        Expression firstSet = defineFirstElement(prefix, firstExpression, mapping, setExpression);

        // ordering/last
        ConstantDeclaration lastAtom = defineLastElement(prefix, mapping, setExpression);

        // Next relation
        FunctionDeclaration ordNext = translator.options.useSetQuantifiers() ?
                addOrdNextWithFilter(prefix, setExpression, mapping, ordSig) :
                addOrdNext(prefix, setExpression, mapping, ordSig, firstSet, lastAtom);

        // prev relation
        FunctionDeclaration ordPrev = addOrdPrev(prefix, ordNext);

        Expr ordered = set;
        while (ordered instanceof ExprUnary && ((ExprUnary) ordered).op == ExprUnary.Op.NOOP)
        {
            ordered = ((ExprUnary) ordered).sub;
        }
        if (ordered instanceof Sig.PrimSig)
        {
            ConstantDeclaration firstAtom = translator.smtProgram.getConstantDeclarations().stream()
                    .filter(c -> c.getName().equals(prefix + "FirstAtom")).findFirst().get();
            translator.orderings.add(new Alloy2SmtTranslator.Ordering((Sig) ordered, mapping, ordNext, firstAtom, lastAtom));
        }

        VariableDeclaration set1 = new VariableDeclaration("s1", AbstractTranslator.setOfUnaryAtomSort, null);
        VariableDeclaration set2 = new VariableDeclaration("s2", AbstractTranslator.setOfUnaryAtomSort, null);

        // ordering/prevs
        FunctionDefinition prevs = translator.options.useSetQuantifiers() ?
                getPrevsNextsDefinitionWithFilter(prefix, "prevs", set1, setExpression, mapping, BinaryExpression.Op.LT) :
                getPrevsNextsDefinition(prefix, set1, ordPrev, "prevs");
        translator.addFunction(prevs);

        // ordering/nexts
        FunctionDefinition nexts = translator.options.useSetQuantifiers() ?
                getPrevsNextsDefinitionWithFilter(prefix, "nexts", set1, setExpression, mapping, BinaryExpression.Op.GT) :
                getPrevsNextsDefinition(prefix, set1, ordNext, "nexts");
        translator.addFunction(nexts);

        // ordering/lt: e1 in prevs[e2]
        FunctionDefinition lt = getComparisonDefinition(prefix, "lt", mapping, set1, set2, BinaryExpression.Op.LT);
        translator.addFunction(lt);

        // ordering/gt: e1 in nexts[e2]
        FunctionDefinition gt = getComparisonDefinition(prefix, "gt", mapping, set1, set2, BinaryExpression.Op.GT);
        translator.addFunction(gt);

        // ordering/lte: e1 = e2 or lt[e1, e2]
        FunctionDefinition lte = getComparisonOrEqualDefinition(prefix, "lte", set1, set2, lt);
        translator.addFunction(lte);

        // ordering/gte: e1 = e2 or gt[e1, e2]
        FunctionDefinition gte = getComparisonOrEqualDefinition(prefix, "gte", set1, set2, gt);
        translator.addFunction(gte);

        //ordering/larger: lt[e1,e2] => e2 else e1
        FunctionDefinition larger = getLargerSmallerDefinition(prefix, "larger", set1, set2, lt, set2, set1);
        translator.addFunction(larger);

        //ordering/smaller: lt[e1,e2] => e1 else e2
        FunctionDefinition smaller = getLargerSmallerDefinition(prefix, "smaller", set1, set2, lt, set1, set2);
        translator.addFunction(smaller);

        //ordering/max
        FunctionDefinition max = translator.options.useSetQuantifiers() ?
                getMaxMinDefinitionWithFilter(prefix, "max", set1, mapping, BinaryExpression.Op.LTE) :
                getMaxMinDefinition(prefix, "max", set1, ordPrev);
        translator.addFunction(max);

        //ordering/min
        FunctionDefinition min = translator.options.useSetQuantifiers() ?
                getMaxMinDefinitionWithFilter(prefix, "min", set1, mapping, BinaryExpression.Op.GTE) :
                getMaxMinDefinition(prefix, "min", set1, ordNext);
        translator.addFunction(min);
    }

    private static Expression element(Expression tuple)
    {
        return BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), tuple);
    }

    private static Expression map(FunctionDeclaration mapping, Expression atom)
    {
        return new FunctionCallExpression(mapping, atom);
    }

    private FunctionDeclaration defineInjectiveMapping(String mappingName, Expression setExpression)
    {
        FunctionDeclaration mapping = new FunctionDeclaration(mappingName, AbstractTranslator.atomSort, AbstractTranslator.intSort);
        translator.addFunction(mapping);

        // the mapping is one-to-one (injective) on the ordered set
        // for all x, y in set: x != y implies f(x) != f(y)
        Expression pairs = BinaryExpression.Op.PRODUCT.make(setExpression, setExpression);
        Expression injective = translator.exprTranslator.forAllIn(pairs, (pair, env) ->
        {
            Expression x = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), pair);
            Expression y = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(1), pair);
            Expression notXEqualsY = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(x, y));
            Expression not = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(map(mapping, x), map(mapping, y)));
            return BinaryExpression.Op.IMPLIES.make(notXEqualsY, not);
        }, new Environment());

        translator.smtProgram.addAssertion(new Assertion(mappingName + " is injective", injective));

        return mapping;
    }

    private Expression defineFirstElement(String prefix, Expression firstExpression, FunctionDeclaration mapping, Expression setExpression)
    {
        final String suffix = "first";
        ConstantDeclaration firstAtom = new ConstantDeclaration(prefix + "FirstAtom", AbstractTranslator.atomSort);
        FunctionDeclaration first = new FunctionDeclaration(prefix + suffix, AbstractTranslator.setOfUnaryAtomSort);

        Expression firstSet = UnaryExpression.Op.SINGLETON.make(
                new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, firstAtom.getVariable()));

        translator.smtProgram.addConstantDeclaration(firstAtom);
        translator.addFunction(first);

        // there is only one first element
        // ordering/first = (set.singleton (tuple firstAtom))
        Expression firstSingleton = BinaryExpression.Op.EQ.make(first.getVariable(), firstSet);
        translator.smtProgram.addAssertion(new Assertion(prefix + suffix + " = (set.singleton (tuple firstAtom))", firstSingleton));

        // ordering/first = ordering/Ord.First
        Expression ordFirstSingleton = BinaryExpression.Op.EQ.make(first.getVariable(), firstExpression);
        translator.smtProgram.addAssertion(new Assertion(prefix + suffix + " = " + prefix + "Ord.First", ordFirstSingleton));

        // each element is greater than or equal to the first element
        Expression all = translator.exprTranslator.forAllIn(setExpression, (x, env) ->
                BinaryExpression.Op.GTE.make(map(mapping, element(x)), map(mapping, firstAtom.getVariable())), new Environment());
        translator.smtProgram.addAssertion(new Assertion(
                "each element is greater than or equal to the first element", all));

        return firstSet;
    }

    private ConstantDeclaration defineLastElement(String prefix, FunctionDeclaration mapping, Expression setExpression)
    {
        final String suffix = "last";
        ConstantDeclaration lastAtom = new ConstantDeclaration(prefix + "LastAtom", AbstractTranslator.atomSort);
        FunctionDeclaration last = new FunctionDeclaration(prefix + suffix, AbstractTranslator.setOfUnaryAtomSort);

        Expression lastSet = UnaryExpression.Op.SINGLETON.make(
                new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, lastAtom.getVariable()));

        translator.smtProgram.addConstantDeclaration(lastAtom);
        translator.addFunction(last);


        // last element is a member of the set
        Expression lastMember = BinaryExpression.Op.MEMBER.make(new MultiArityExpression(MultiArityExpression.Op.MKTUPLE, lastAtom.getVariable()), setExpression);
        translator.smtProgram.addAssertion(new Assertion("last element is a member", lastMember));

        // there is only one last element
        // ordering/last = (set.singleton (tuple lastAtom))
        Expression lastSingleton = BinaryExpression.Op.EQ.make(last.getVariable(), lastSet);
        translator.smtProgram.addAssertion(new Assertion(prefix + suffix + " = (set.singleton (tuple lastAtom))", lastSingleton));

        // each element is less than or equal to the last element
        Expression all = translator.exprTranslator.forAllIn(setExpression, (x, env) ->
                BinaryExpression.Op.LTE.make(map(mapping, element(x)), map(mapping, lastAtom.getVariable())), new Environment());
        translator.smtProgram.addAssertion(new Assertion(
                "each element is less than or equal to the last element", all));

        return lastAtom;
    }

    // max[es] = es - es.^prev, min[es] = es - es.^next
    private FunctionDefinition getMaxMinDefinition(String prefix, String suffix, VariableDeclaration set, FunctionDeclaration declaration)
    {
        Expression tClosure = UnaryExpression.Op.TCLOSURE.make(declaration.getVariable());

        Expression join = BinaryExpression.Op.JOIN.make(set.getVariable(), tClosure);

        Expression difference = BinaryExpression.Op.SETMINUS.make(set.getVariable(), join);

        return new FunctionDefinition(prefix + suffix, AbstractTranslator.setOfUnaryAtomSort,
                difference, set);
    }

    // prevs[e] = {x: set | some y: e | mapping(x) < mapping(y)}, nexts[e] with >
    private FunctionDefinition getPrevsNextsDefinitionWithFilter(String prefix, String suffix, VariableDeclaration set1, Expression setExpression,
                                                                 FunctionDeclaration mapping, BinaryExpression.Op operator)
    {
        Environment environment = new Environment().withPolarity(Environment.Polarity.BOTH);
        Expression filter = translator.exprTranslator.filter(setExpression, (x, env1) ->
                translator.exprTranslator.someIn(set1.getVariable(), (y, env2) ->
                        operator.make(map(mapping, element(x)), map(mapping, element(y))), env1), environment);
        return new FunctionDefinition(prefix + suffix, AbstractTranslator.setOfUnaryAtomSort, filter, set1);
    }

    // max[es] = {x: es | all y: es | mapping(y) <= mapping(x)}, min[es] with >=
    private FunctionDefinition getMaxMinDefinitionWithFilter(String prefix, String suffix, VariableDeclaration set,
                                                             FunctionDeclaration mapping, BinaryExpression.Op operator)
    {
        Environment environment = new Environment().withPolarity(Environment.Polarity.BOTH);
        Expression filter = translator.exprTranslator.filter(set.getVariable(), (x, env1) ->
                translator.exprTranslator.forAllIn(set.getVariable(), (y, env2) ->
                        operator.make(map(mapping, element(y)), map(mapping, element(x))), env1), environment);
        return new FunctionDefinition(prefix + suffix, AbstractTranslator.setOfUnaryAtomSort, filter, set);
    }

    /**
     * next = {(x, y): set x set | mapping(x) < mapping(y) and no z: set | mapping(x) < mapping(z) < mapping(y)}
     * This definition creates no new terms, unlike the next mapping which, in the body of
     * set.all, makes the solver generate the terms nextMapping(nextMapping(...)).
     */
    private FunctionDeclaration addOrdNextWithFilter(String prefix, Expression setExpression, FunctionDeclaration intMapping, Sig ordSig)
    {
        Expression pairs = BinaryExpression.Op.PRODUCT.make(setExpression, setExpression);
        Expression filter = translator.exprTranslator.filter(pairs, (pair, env) ->
        {
            Expression x = map(intMapping, BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), pair));
            Expression y = map(intMapping, BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(1), pair));
            Expression noneBetween = translator.exprTranslator.forAllIn(setExpression, (z, env2) ->
            {
                Expression between = BinaryExpression.Op.AND.make(BinaryExpression.Op.LT.make(x, map(intMapping, element(z))),
                        BinaryExpression.Op.LT.make(map(intMapping, element(z)), y));
                return UnaryExpression.Op.NOT.make(between);
            }, env);
            return BinaryExpression.Op.AND.make(BinaryExpression.Op.LT.make(x, y), noneBetween);
        }, new Environment());

        FunctionDeclaration ordNext = new FunctionDeclaration(prefix + "next", AbstractTranslator.setOfBinaryAtomSort);
        translator.addFunction(ordNext);
        translator.smtProgram.addAssertion(new Assertion(prefix + "next", BinaryExpression.Op.EQ.make(ordNext.getVariable(), filter)));

        Expression ordSigExpression = translator.signaturesMap.get(ordSig).getVariable();
        Sig.Field nextField = StreamSupport
                .stream(ordSig.getFields().spliterator(), false)
                .filter(field -> field.label.contains("Next"))
                .findFirst().get();
        Expression nextFieldExpression = translator.fieldsMap.get(nextField).getVariable();
        Expression join = BinaryExpression.Op.JOIN.make(ordSigExpression, nextFieldExpression);
        // next = ordSig.Next
        translator.smtProgram.addAssertion(
                new Assertion(ordNext.getName() + " = " + ordSig.label + "." + nextField.label, BinaryExpression.Op.EQ.make(ordNext.getVariable(), join)));
        return ordNext;
    }

    private FunctionDefinition getPrevsNextsDefinition(String prefix, VariableDeclaration set1, FunctionDeclaration ord, String suffix)
    {
        UnaryExpression tClosure = UnaryExpression.Op.TCLOSURE.make(ord.getVariable());
        BinaryExpression join = BinaryExpression.Op.JOIN.make(set1.getVariable(), tClosure);
        String name = prefix + suffix;
        return new FunctionDefinition(name, AbstractTranslator.setOfUnaryAtomSort, join, set1);
    }

    private FunctionDefinition getLargerSmallerDefinition(String prefix, String suffix, VariableDeclaration set1, VariableDeclaration set2,
                                                          FunctionDefinition lt, VariableDeclaration thenSet, VariableDeclaration elseSet)
    {
        FunctionCallExpression call = new FunctionCallExpression(lt, set1.getVariable(), set2.getVariable());
        ITEExpression ite = new ITEExpression(call, thenSet.getVariable(), elseSet.getVariable());
        return new FunctionDefinition(prefix + suffix,
                AbstractTranslator.setOfUnaryAtomSort,
                ite,
                set1, set2
        );
    }

    private FunctionDeclaration addOrdNext(String prefix, Expression setExpression, FunctionDeclaration intMapping, Sig ordSig, Expression firstSet, ConstantDeclaration lastAtom)
    {
        String nextMapping = prefix + "nextMapping";

        FunctionDeclaration mapping = new FunctionDeclaration(nextMapping, AbstractTranslator.atomSort, AbstractTranslator.atomSort);
        translator.addFunction(mapping);

        // for all x : x is a member implies intMapping(x) < intMapping (nextMapping(x)) and
        //                                    x != lastAtom implies nextMapping(x) is a member
        Expression all = translator.exprTranslator.forAllIn(setExpression, (tuple, env) ->
        {
            Expression x = element(tuple);
            Expression next = new FunctionCallExpression(mapping, x);
            Expression xLessThanNext = BinaryExpression.Op.LT.make(map(intMapping, x), map(intMapping, next));
            Expression notXEqualsLast = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(x, lastAtom.getVariable()));
            Expression nextIsMember = BinaryExpression.Op.MEMBER.make(TranslatorUtils.getTuple(next), setExpression);
            return BinaryExpression.Op.AND.make(xLessThanNext, BinaryExpression.Op.IMPLIES.make(notXEqualsLast, nextIsMember));
        }, new Environment());
        translator.smtProgram.addAssertion(
                new Assertion("For all x : x is a member implies (intMapping(x) < intMapping (nextMapping(x)) and " +
                        "(x != lastAtom implies nextMapping(x) is a member))", all));

        // the next mapping is one-to-one (injective) members
        // for all x, y members: x != y implies nextMapping(x) != nextMapping(y)
        Expression pairs = BinaryExpression.Op.PRODUCT.make(setExpression, setExpression);
        Expression injective = translator.exprTranslator.forAllIn(pairs, (pair, env) ->
        {
            Expression x = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), pair);
            Expression y = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(1), pair);
            Expression notXEqualsY = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(x, y));
            Expression notEqual = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(
                    new FunctionCallExpression(mapping, x), new FunctionCallExpression(mapping, y)));
            return BinaryExpression.Op.IMPLIES.make(notXEqualsY, notEqual);
        }, new Environment());
        translator.smtProgram.addAssertion(new Assertion(nextMapping + " is injective for members", injective));

        FunctionDeclaration ordNext = new FunctionDeclaration(prefix + "next", AbstractTranslator.setOfBinaryAtomSort);
        translator.addFunction(ordNext);

        // next = {(x, y) | x, y are members and nextMapping(x) = y}
        // i.e. next is a subset of set x set, and each member except the last is related to its next element
        Expression nextSubset = BinaryExpression.Op.SUBSET.make(ordNext.getVariable(), pairs);
        Expression nextPairs = translator.exprTranslator.forAllIn(ordNext.getVariable(), (pair, env) ->
        {
            Expression x = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(0), pair);
            Expression y = BinaryExpression.Op.TUPSEL.make(IntConstant.getInstance(1), pair);
            return BinaryExpression.Op.EQ.make(new FunctionCallExpression(mapping, x), y);
        }, new Environment());
        Expression nextTotal = translator.exprTranslator.forAllIn(setExpression, (tuple, env) ->
        {
            Expression x = element(tuple);
            Expression notXEqualsLast = UnaryExpression.Op.NOT.make(BinaryExpression.Op.EQ.make(x, lastAtom.getVariable()));
            Expression pair = TranslatorUtils.getTuple(x, new FunctionCallExpression(mapping, x));
            return BinaryExpression.Op.IMPLIES.make(notXEqualsLast, BinaryExpression.Op.MEMBER.make(pair, ordNext.getVariable()));
        }, new Environment());
        translator.smtProgram.addAssertion(new Assertion(prefix + "next", BinaryExpression.Op.AND.make(nextSubset, BinaryExpression.Op.AND.make(nextPairs, nextTotal))));

        Expression ordSigExpression = translator.signaturesMap.get(ordSig).getVariable();

        Sig.Field nextField = StreamSupport
                .stream(ordSig.getFields().spliterator(), false)
                .filter(field -> field.label.contains("Next"))
                .findFirst().get();

        Expression nextFieldExpression = translator.fieldsMap.get(nextField).getVariable();

        Expression join = BinaryExpression.Op.JOIN.make(ordSigExpression, nextFieldExpression);

        Expression equality = BinaryExpression.Op.EQ.make(ordNext.getVariable(), join);

        // next = ordSig.Next
        translator.smtProgram.addAssertion(
                new Assertion(ordNext.getName() + " = " + ordSig.label + "." + nextField.label, equality));
        return ordNext;
    }

    private FunctionDeclaration addOrdPrev(String prefix, FunctionDeclaration ordNext)
    {
        FunctionDeclaration ordPrev = new FunctionDeclaration(prefix + "prev", AbstractTranslator.setOfBinaryAtomSort);

        UnaryExpression transpose = UnaryExpression.Op.TRANSPOSE.make(ordNext.getVariable());

        BinaryExpression equality = BinaryExpression.Op.EQ.make(ordPrev.getVariable(), transpose);

        translator.addFunction(ordPrev);

        translator.smtProgram.addAssertion(new Assertion(prefix + "PrevAuxiliary", equality));

        return ordPrev;
    }

    // lt[e1, e2] = e1 in prevs[e2] = all x: e1 | some y: e2 | mapping(x) < mapping(y)
    private FunctionDefinition getComparisonDefinition(String prefix, String suffix, FunctionDeclaration mapping, VariableDeclaration set1, VariableDeclaration set2, BinaryExpression.Op operator)
    {
        // the body of a definition may be used in any context
        Environment environment = new Environment().withPolarity(Environment.Polarity.BOTH);
        Expression comparison = translator.exprTranslator.forAllIn(set1.getVariable(), (x, env1) ->
                translator.exprTranslator.someIn(set2.getVariable(), (y, env2) ->
                        operator.make(map(mapping, element(x)), map(mapping, element(y))), env1), environment);

        return new FunctionDefinition(prefix + suffix,
                BoolSort.getInstance(),
                comparison,
                set1, set2
        );
    }

    private FunctionDefinition getComparisonOrEqualDefinition(String prefix, String suffix, VariableDeclaration set1, VariableDeclaration set2, FunctionDefinition comparison)
    {
        Expression equal = BinaryExpression.Op.EQ.make(set1.getVariable(), set2.getVariable());
        Expression call = new FunctionCallExpression(comparison, set1.getVariable(), set2.getVariable());
        return new FunctionDefinition(prefix + suffix, BoolSort.getInstance(), BinaryExpression.Op.OR.make(equal, call), set1, set2);
    }
}
