/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.translators;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.ast.*;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.SigScopes;
import edu.uiowa.alloy2smt.mapping.Mapper;
import edu.uiowa.alloy2smt.mapping.MappingField;
import edu.uiowa.alloy2smt.mapping.MappingSignature;
import edu.uiowa.alloy2smt.mapping.MappingType;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;

import java.util.*;
import java.util.stream.Collectors;

public class Alloy2SmtTranslator extends AbstractTranslator
{
    final CompModule                alloyModel;
    final List<Sig>                 reachableSigs;
    final List<Sig>                 topLevelSigs;
    final List<Command>             commands;
    final TranslatorOptions         options;

    final SignatureTranslator       signatureTranslator;
    final ExprTranslator            exprTranslator;

    Set<String> funcNames;
    List<Map.Entry<Sig, Expr>>                      sigFacts;

    Map<Sig, FunctionDeclaration>                   signaturesMap;
    Map<Sig.Field, FunctionDeclaration>             fieldsMap;
    Map<String, Func> nameToFuncMap;
    Map<Expr, Integer>  sigToIdMap;
    // whether the translation contains lambda expressions (set.all, set.some, set.filter)
    boolean usesLambdas;

    // the translation of an instance of util/ordering
    static class Ordering
    {
        final Sig sig;
        final FunctionDeclaration mapping;
        final FunctionDeclaration next;
        final ConstantDeclaration first;
        final ConstantDeclaration last;

        Ordering(Sig sig, FunctionDeclaration mapping, FunctionDeclaration next, ConstantDeclaration first, ConstantDeclaration last)
        {
            this.sig = sig;
            this.mapping = mapping;
            this.next = next;
            this.first = first;
            this.last = last;
        }
    }

    List<Ordering> orderings = new ArrayList<>();

    public Alloy2SmtTranslator(CompModule alloyModel)
    {
        this(alloyModel, new TranslatorOptions());
    }

    public Alloy2SmtTranslator(CompModule alloyModel, TranslatorOptions options)
    {
        TranslatorUtils.reset();

        this.smtProgram             = new SmtProgram();
        this.alloyModel             = alloyModel;
        this.options                = options;
        this.reachableSigs          = new ArrayList<>();
        this.topLevelSigs           = new ArrayList<>();
        this.commands               = alloyModel.getAllCommands();

        this.signatureTranslator    = new SignatureTranslator(this);
        this.comparisonOperations = new HashMap<>();
        this.arithmeticOperations = new HashMap<>();
        this.signaturesMap          = new HashMap<>();
        this.functionsMap           = new HashMap<>();
        this.nameToFuncMap          = new HashMap<>();
        this.fieldsMap              = new HashMap<>();
        this.sigFacts               = new ArrayList<>();
        this.funcNames              = new HashSet<>();
        this.integerConstants       = new HashMap<>();
        this.sigToIdMap             = new HashMap<>();

        this.signaturesMap.put(Sig.UNIV, atomUniverse);
        this.signaturesMap.put(Sig.SIGINT, intUniv);
        this.smtProgram.addSort(atomSort);
        this.smtProgram.addSort(uninterpretedInt);
        this.smtProgram.addFunction(uninterpretedIntValue);

        this.functionsMap.put(uninterpretedIntValue.getName(), uninterpretedIntValue);

        this.exprTranslator                 = new ExprTranslator(this);
    }

    public Alloy2SmtTranslator(Alloy2SmtTranslator translator)
    {
        this.smtProgram             = new SmtProgram(translator.smtProgram);
        this.alloyModel             = translator.alloyModel;
        this.options                = translator.options;
        this.reachableSigs          = new ArrayList<>(translator.reachableSigs);
        this.topLevelSigs           = new ArrayList<>(translator.topLevelSigs);
        this.sigToIdMap             = new HashMap<>(translator.sigToIdMap);

        this.commands               = this.alloyModel.getAllCommands();


        this.signatureTranslator    = new SignatureTranslator(this);
        this.comparisonOperations = new HashMap<>(translator.comparisonOperations);
        this.integerConstants       = new HashMap<>(translator.integerConstants);
        this.arithmeticOperations = new HashMap<>(translator.arithmeticOperations);
        this.signaturesMap          = new HashMap<>(translator.signaturesMap);
        this.functionsMap           = new HashMap<>(translator.functionsMap);
        this.nameToFuncMap          = new HashMap<>(translator.nameToFuncMap);
        this.fieldsMap              = new HashMap<>(translator.fieldsMap);
        this.sigFacts               = new ArrayList<>(translator.sigFacts);
        this.funcNames              = new HashSet<>(translator.funcNames);
        this.usesLambdas            = translator.usesLambdas;
        this.orderings              = new ArrayList<>(translator.orderings);

        this.exprTranslator                 = new ExprTranslator(this);
    }


    /**
     * Translates signatures, fields, their facts and the functions and predicates of the model.
     * The facts of the model are part of the formula of each command.
     */
    @Override
    public SmtProgram translate()
    {
        setSolverOptions();
        translateSpecialFunctions();
        this.signatureTranslator.translateSigs();
        this.signatureTranslator.translateSpecialSigFacts();
        translateFunctionsAndPredicates();
        this.signatureTranslator.translateSigFacts();
        translateSpecialAssertions();
        return this.smtProgram;
    }

    private void setSolverOptions()
    {
        smtProgram.addOption("produce-models", "true");
        if (options.finiteModelFinding)
        {
            smtProgram.addOption("finite-model-find", "true");
        }
        // set.universe and set.complement
        smtProgram.addOption("sets-exp", "true");
        if (options.useFilter())
        {
            // lambda expressions of set.all, set.some and set.filter
            smtProgram.setLogic("HO_ALL");
            smtProgram.addOption("uf-lazy-ll", "true");
        }
        else
        {
            smtProgram.setLogic("ALL");
        }
    }

    private void translateSpecialFunctions()
    {
        this.smtProgram.addFunction(this.atomNone);
        this.smtProgram.addFunction(this.atomUniverse);
        this.smtProgram.addFunction(this.atomIdentity);
        this.smtProgram.addFunction(this.intUniv);
    }

    private void translateSpecialAssertions()
    {
        this.smtProgram.addAssertion(new Assertion("Empty unary relation definition for Atom", BinaryExpression.Op.EQ.make(this.atomNone.getVariable(), UnaryExpression.Op.EMPTYSET.make(setOfUnaryAtomSort))));
        this.smtProgram.addAssertion(new Assertion("Universe definition for Atom", BinaryExpression.Op.EQ.make(this.atomUniverse.getVariable(), UnaryExpression.Op.UNIVSET.make(setOfUnaryAtomSort))));
        this.smtProgram.addAssertion(new Assertion("Universe definition for UninterpretedInt", BinaryExpression.Op.EQ.make(intUniv.getVariable(), UnaryExpression.Op.UNIVSET.make(setOfUninterpretedIntTuple))));
        // iden = {(a, a) | a in univ}
        Expression identity = UnaryExpression.Op.IDEN.make(this.atomUniverse.getVariable());
        this.smtProgram.addAssertion(new Assertion("Identity relation definition for Atom", BinaryExpression.Op.EQ.make(this.atomIdentity.getVariable(), identity)));
    }

    /**
     * uninterpretedIntValue is injective: (forall x. inverse(value(x)) = x).
     * Added only when integers are used.
     */
    void translateIntegerAssertions()
    {
        if (functionsMap.containsKey(uninterpretedIntValueName + "Inverse"))
        {
            return;
        }
        FunctionDeclaration inverse = new FunctionDeclaration(uninterpretedIntValueName + "Inverse", intSort, uninterpretedInt);
        addFunction(inverse);
        VariableDeclaration x = new VariableDeclaration("x", uninterpretedInt, null);
        Expression value = new FunctionCallExpression(uninterpretedIntValue, x.getVariable());
        Expression inverseValue = new FunctionCallExpression(inverse, value);
        Expression forAll = QuantifiedExpression.Op.FORALL.make(BinaryExpression.Op.EQ.make(inverseValue, x.getVariable()), x);
        smtProgram.addAssertion(new Assertion(uninterpretedIntValueName + " is injective", forAll));
    }

    @Override
    public ConstantDeclaration getUninterpretedIntConstant(IntConstant intConstant)
    {
        translateIntegerAssertions();
        return super.getUninterpretedIntConstant(intConstant);
    }

    /**
     * Functions and predicates are inlined where they are called (see ExprTranslator.inline),
     * except the functions of util/ordering which are defined in SignatureTranslator.
     */
    private void translateFunctionsAndPredicates()
    {
        for (CompModule module: this.alloyModel.getAllReachableModules())
        {
            for (Func func : module.getAllFunc())
            {
                funcNames.add(func.label);
                this.nameToFuncMap.put(func.label, func);
            }
        }
    }

    /**
     *
     * @return a mapper that maps signatures's names to the corresponding names
     * in the generated smt script
     */
    public Mapper generateMapper()
    {
        Mapper              mapper  = new Mapper();
        // add special signatures

        MappingSignature univSignature = getSignature(Sig.UNIV);
        mapper.signatures.add(univSignature);

        MappingSignature intSignature = getSignature(Sig.SIGINT);
        mapper.signatures.add(intSignature);

        //ToDo: add other special signatures: none, iden, string

        for (Sig sig : topLevelSigs)
        {
            mapper.signatures.addAll(getSignatures(sig));
        }

        // add remaining signatures like int signatures
        for (Sig sig : reachableSigs)
        {
            if(! sigToIdMap.keySet().contains(sig))
            {
                mapper.signatures.addAll(getSignatures(sig));
            }
        }

        for (Sig sig : reachableSigs)
        {
            mapper.fields.addAll(getFields(sig));
        }

        return mapper;
    }

    private List<MappingSignature> getSignatures(Sig sig)
    {
        List<MappingSignature> signatures = new ArrayList<>();
        MappingSignature signature = getSignature(sig);
        signatures.add(signature);

        for (Sig childSig : children(sig))
        {
            signatures.addAll(getSignatures(childSig));
        }

        return signatures;
    }

    private List<MappingField> getFields(Sig sig)
    {
        List<MappingField> fields = new ArrayList<>();

        for (Sig.Field field : sig.getFields())
        {
            fields.add(getField(field));
        }

        return fields;
    }

    private List<Sig> children(Sig sig)
    {
        if (sig == Sig.NONE)
        {
            return new ArrayList<>();
        }
        if (sig instanceof Sig.PrimSig)
        {
            List<Sig> sigs = new ArrayList<>();
            ((Sig.PrimSig) sig).children().forEach(sigs::add);
            return sigs;
        }
        return new ArrayList<>();
    }


    private MappingSignature getSignature(Sig sig)
    {
        MappingSignature signature = new MappingSignature();

        signature.label         = sig.label;
        signature.functionName  = TranslatorUtils.getOriginalName(signaturesMap.get(sig).getName());

        signature.id            = getSigId(sig);

        // find the ids of the parents
        if (sig instanceof Sig.PrimSig && sig != Sig.UNIV)
        {
            signature.parents.add(getSigId(((Sig.PrimSig) sig).parent));
        }
        else if (sig instanceof Sig.SubsetSig)
        {
            signature.isSubset = true;
            for (Sig parent :  ((Sig.SubsetSig) sig).parents)
            {
                signature.parents.add(getSigId(parent));
            }
        }

        signature.builtIn       = sig.builtin;
        signature.isAbstract    = sig.isAbstract != null;
        signature.isOne         = sig.isOne != null;
        signature.isLone        = sig.isLone != null;
        signature.isSome        = sig.isSome != null;
        signature.isPrivate     = sig.isPrivate != null;
        signature.isMeta        = sig.isMeta != null;

        if(sig instanceof Sig.SubsetSig)
        {
            signature.isExact   = ((Sig.SubsetSig) sig).exact;
        }

        signature.isEnum        = sig.isEnum != null;

        return signature;
    }

    private MappingField getField(Sig.Field field)
    {
        MappingField mappingField   = new MappingField();

        mappingField.label          = field.label;
        mappingField.functionName   = TranslatorUtils.getOriginalName(fieldsMap.get(field).getName());
        mappingField.id             = getSigId(field);
        mappingField.parentId       = getSigId(field.sig);
        mappingField.isPrivate      = field.isPrivate != null;
        mappingField.isMeta         = field.isMeta != null;
        mappingField.types          = getFieldTypes(field);

        return mappingField;
    }

    private List<List<MappingType>> getFieldTypes(Sig.Field field)
    {
        List<List<MappingType>> types = new ArrayList<>();
        for(List<Sig.PrimSig> sigs: field.type().fold())
        {
            List<MappingType> type = new ArrayList<>();
            for (Sig sig: sigs)
            {
                MappingType mappingType = new MappingType();
                mappingType.id = getSigId(sig);
                type.add(mappingType);
            }
            types.add(type);
        }
        return types;
    }

    // Sig.univ usually has id = 2 (1 ++)
    private int mappingSignatureId = 1;

    private int getUniqueId()
    {
        mappingSignatureId ++;
        return mappingSignatureId;
    }

    /**
     *
     * @param expr can be Sig, Field, or Skolem
     * @return the unique id of the expr it exists in the idMap, or generate  a new id
     */
    public int getSigId(Expr expr)
    {
        Integer id = sigToIdMap.get(expr);

        if(id == null)
        {
            id = getUniqueId();
            sigToIdMap.put(expr, id);
        }
        return id;
    }

    /**
     * @param commandIndex the index of the run command
     * @param includeScope whether to bound the signatures by the scope of the command
     * @return an assertion that represents the translation
     * of the command, which includes the facts of the model
     */
    public Assertion translateCommand(int commandIndex, boolean includeScope)
    {
        Command command = this.commands.get(commandIndex);

        Expression expression = this.exprTranslator.translateExpr(command.formula);

        if(includeScope)
        {
            SigScopes scopes = SigScopes.compute(A4Reporter.NOP, alloyModel.getAllReachableSigs(), command);
            expression = ExprTranslator.and(expression, translateScope(scopes));
            expression = ExprTranslator.and(expression, breakOrderingSymmetries(scopes));
        }

        return new Assertion(command.toString(), expression);
    }

    /**
     * The scope of a command bounds the number of atoms of each signature with
     * the scope Alloy computes for the Kodkod translation:
     * S = {x1, ..., xn} for an exact scope n and S in {x1, ..., xn} otherwise.
     */
    private Expression translateScope(SigScopes scopes)
    {
        Expression constraints = BoolConstant.True;
        for (Sig signature : reachableSigs)
        {
            if (!(signature instanceof Sig.PrimSig) || signature.type().is_int())
            {
                continue;
            }
            int scope = scopes.scope(signature);
            if (scope < 0)
            {
                // bounded by the scope of its parent
                continue;
            }
            Expression variable = signaturesMap.get(signature).getVariable();
            boolean exact = scopes.isExact(signature);
            if (scope == 0)
            {
                constraints = ExprTranslator.and(constraints, ExprTranslator.mkEmpty(variable));
                continue;
            }
            List<VariableDeclaration> declarations = new ArrayList<>();
            List<Expression> atoms = new ArrayList<>();
            for (int i = 0; i < scope; i++)
            {
                VariableDeclaration declaration = new VariableDeclaration(TranslatorUtils.getNewAtomName(), AbstractTranslator.atomSort, null);
                declarations.add(declaration);
                atoms.add(MultiArityExpression.Op.MKTUPLE.make(declaration.getVariable()));
            }
            Expression set = ExprTranslator.mkSetOutOfTuples(atoms);
            Expression constraint = exact ?
                    ExprTranslator.and(TranslatorUtils.makeDistinct(declarations.stream().map(d -> (Expression) d.getVariable()).collect(Collectors.toList())),
                            BinaryExpression.Op.EQ.make(variable, set)) :
                    BinaryExpression.Op.SUBSET.make(variable, set);
            constraints = ExprTranslator.and(constraints, QuantifiedExpression.Op.EXISTS.make(constraint, declarations));
        }
        return constraints;
    }

    /**
     * Like Kodkod, fixes the total order of a signature ordered by util/ordering whose
     * scope n is exact: the signature is {c0, ..., cn-1} for distinct constants, with
     * ci < ci+1. Every instance is isomorphic to one with this order, because the atoms
     * of the signature are interchangeable.
     */
    private Expression breakOrderingSymmetries(SigScopes scopes)
    {
        Expression constraints = BoolConstant.True;
        for (Ordering ordering : orderings)
        {
            int n = scopes.scope(ordering.sig);
            if (n <= 0 || !scopes.isExact(ordering.sig))
            {
                continue;
            }
            List<Expression> atoms = new ArrayList<>();
            for (int i = 0; i < n; i++)
            {
                ConstantDeclaration atom = new ConstantDeclaration(TranslatorUtils.getNewAtomName(), atomSort);
                smtProgram.addConstantDeclaration(atom);
                atoms.add(atom.getVariable());
            }
            List<Expression> elements = new ArrayList<>();
            List<Expression> pairs = new ArrayList<>();
            for (int i = 0; i < n; i++)
            {
                elements.add(MultiArityExpression.Op.MKTUPLE.make(atoms.get(i)));
                Expression mapping = new FunctionCallExpression(ordering.mapping, atoms.get(i));
                constraints = ExprTranslator.and(constraints, BinaryExpression.Op.EQ.make(mapping, IntConstant.getInstance(i)));
                if (i + 1 < n)
                {
                    pairs.add(MultiArityExpression.Op.MKTUPLE.make(atoms.get(i), atoms.get(i + 1)));
                }
            }
            constraints = ExprTranslator.and(constraints, TranslatorUtils.makeDistinct(atoms));
            Expression sig = signaturesMap.get(ordering.sig).getVariable();
            constraints = ExprTranslator.and(constraints, BinaryExpression.Op.EQ.make(sig, ExprTranslator.mkSetOutOfTuples(elements)));
            Expression next = pairs.isEmpty() ? UnaryExpression.Op.EMPTYSET.make(setOfBinaryAtomSort) : ExprTranslator.mkSetOutOfTuples(pairs);
            constraints = ExprTranslator.and(constraints, BinaryExpression.Op.EQ.make(ordering.next.getVariable(), next));
            constraints = ExprTranslator.and(constraints, BinaryExpression.Op.EQ.make(ordering.first.getVariable(), atoms.get(0)));
            constraints = ExprTranslator.and(constraints, BinaryExpression.Op.EQ.make(ordering.last.getVariable(), atoms.get(n - 1)));
        }
        return constraints;
    }
}
