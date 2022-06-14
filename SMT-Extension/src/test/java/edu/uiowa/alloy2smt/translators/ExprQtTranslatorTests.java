package edu.uiowa.alloy2smt.translators;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;

import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.alloy2smt.utils.CommandResult;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.FunctionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ExprQtTranslatorTests
{

  @Test
  public void allIntQuantifier() throws Exception
  {
    String alloy = "sig A in Int {}\n" +
        "fact f1{#A = 2}\n" +
        "fact f1{all x : A | x > 5}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<Integer> aAtoms = TranslatorUtils.getIntSet(a);
    assertEquals(2, aAtoms.size());
    assertTrue(new ArrayList<>(aAtoms).get(0) > 5);
    assertTrue(new ArrayList<>(aAtoms).get(1) > 5);
  }

  @Test
  public void allQuantifier() throws Exception
  {
    String alloy = "abstract sig A {}\n" +
        "one sig A0, A1 extends A {}\n" +
        "fact f1{all x : A | x in A0}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void someQuantifier() throws Exception
  {
    String alloy = "abstract sig A {}\n" +
        "one sig A0, A1 extends A {}\n" +
        "fact f1{some x : A | x in A}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
  }

  @Test
  public void allQuantifierMultipleDeclarations() throws Exception
  {
    String alloy = "abstract sig A {}\n" +
        "one sig A0, A1 extends A {}\n" +
        "fact f1{all x : A, y : A - x | A = x + y}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);

    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(2, aAtoms.size());
  }

  @Test
  public void someQuantifierMultipleDeclarations() throws Exception
  {
    String alloy = "abstract sig A {}\n" +
        "one sig A0, A1 extends A {}\n" +
        "fact f1{some x : A, y : A - x | A = x + y}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);

    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(2, aAtoms.size());
  }


  @Test
  public void oneQuantifier() throws Exception
  {
    String alloy = "sig A {} fact {one x : A | x != none}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(1, aAtoms.size());
  }

  @Test
  public void oneQuantifierMultipleDeclarations() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "sig A0, A1 in A {} \n" +
            "fact {one x, y: A | x = A0 and y = A1 and x != y}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(2, aAtoms.size());
  }

  @Test
  public void loneQuantifierMultipleDeclarations() throws Exception
  {
    String alloy =
        "abstract sig A {}\n" +
            "one sig A0, A1 extends A {} \n" +
            "fact f1{lone x, y: A | x = A0 and y = A0 and x != y}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(2, aAtoms.size());
  }

  @Test
  public void secondOrderSomeSet() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "fact f{some x: set A | x = A and x = none}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(0, aAtoms.size());
  }

  @Test
  public void secondOrderSomeSome1() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "fact f{some x: some A | x = none}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void secondOrderAllSome() throws Exception
  {
    String alloy =
        "abstract sig A {}\n" +
            "sig A0, A1 extends A {}\n" +
            "fact f1 {#A0 = 2 and #A1 = 1}\n" +
            "fact f{not all x: some A0, y: some A1 |  x + y != A}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
  }
      
  @Test
  public void setQuantifiersOneLone() throws Exception
  {
    String alloy =
        "abstract sig A {}\n" +
            "sig A0, A1 extends A {}\n" +
            "fact f1 {#A0 = 2 and #A1 = 1}\n" +
            "fact f{not one x: lone A0, y: lone A1 |  x + y = A}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void comprehension() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "sig A0, A1 in A {}\n" +
            "fact f1 {#A0 = 2 and #A1 = 1}\n" +
            "fact f{ A0= {x: A | x not in A1}}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(3, aAtoms.size());

    FunctionDefinition a0 = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A0");
    Set<String> a0Atoms = TranslatorUtils.getAtomSet(a0);
    assertEquals(2, a0Atoms.size());
  }


  @Test
  public void comprehension1() throws Exception
  {
    String alloy =
        "abstract sig A {f: set A, g: set A}\n" +
            "one sig A0, A1 extends A{}\n" +
            "fact f1 {f = A0 -> A0 + A0 -> A1 + A1 -> A0  and g = A1 -> A1}\n" +
            "fact f2{ f= {x: A, y: A | no (x->y & g)}}\n" +
            "run {} for 10";

    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition f = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A/f");
    Set<List<String>> fTuples = TranslatorUtils.getRelation(f);
    assertEquals(3, fTuples.size());

    FunctionDefinition g = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A/g");
    Set<List<String>> gTuples = TranslatorUtils.getRelation(g);
    assertEquals(1, gTuples.size());
  }

  @Test
  public void disjointSingletons() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "assert distinct {all disj x, y : A | x != y}\n" +
            "check distinct";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void disjointSets1() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "assert distinct {all disj x, y : set A | x != y}\n" +
            "check distinct";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition a = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A");
    Set<String> aAtoms = TranslatorUtils.getAtomSet(a);
    assertEquals(0, aAtoms.size());
  }

  @Test
  public void disjointSets2() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "assert distinct {all disj x, y : set A | (some x and some y) => x != y}\n" +
            "check distinct";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  
  @Test
  public void functionComprehension1() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "sig A0, A1 in A {}\n" +
            "fun complement1[x: A]: A {let z = {y : A | not (y in x)} | z}\n" +
            "fun complement2[x: A]: A {A - x}\n" +
            "fact{\n" +
            "#A0 = 2 and #A1 = 2 and\n" +
            "complement1[A0] = A1 and\n" +
            "complement1[A1] = A0 and\n" +
            "A0 = A1\n" +
            "}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  
  @Test
  public void functionComprehension2() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "sig A0, A1 in A {}\n" +
            "fun complement1[x: A]: A {let z = {y : A | not (y in x)} | z}\n" +
            "fact{ #A0 = 2 and #A1 = 2 and A0 = A1}\n" +
            "run {complement1[A0] = A1}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void functionComprehension3() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "fun identity [B: A] : A { let C= {x:A | x in B} | C}\n" +
            "fact {some D:set A | all x: lone identity[D]| x != none}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void setComprehension4() throws Exception
  {
    String alloy =
        "sig B {}\n" +
            "sig A \n" +
            "{\n" +
            "\tf: set B\n" +
            "}\n" +
            "\n" +
            "fact {f = {x: A, y : B | none = none }}\n" +
            "run {#A = 3 and #B = 2} for 3";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
    FunctionDefinition f = AlloyUtils.getFunctionDefinition(commandResults.get(0), "this/A/f");
    Set<List<String>> set = TranslatorUtils.getRelation(f);
    assertEquals(6, set.size());
  }

  @Test
  public void nestedMultiplicities() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "fact {all x: A | some y : x one -> one x | some y}";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
  }
  
  @Test
  public void betterThanAlloy() throws Exception
  {
    String alloy =
        "sig A {}\n" +
            "fact f{all x : lone (A & A) | one x}\n" +
            "run {#A = 3} for 3";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("unsat", commandResults.get(0).satResult);
  }

  @Test
  public void fieldNoop() throws Exception
  {
    String alloy = "sig A {f: A} fact { some x : f | some x }";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
  }

  @Test
  public void fieldSetOf() throws Exception
  {
    String alloy = "sig A {f: A} fact { some x : set f | some x }";
    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);
    assertEquals("sat", commandResults.get(0).satResult);
  }
}