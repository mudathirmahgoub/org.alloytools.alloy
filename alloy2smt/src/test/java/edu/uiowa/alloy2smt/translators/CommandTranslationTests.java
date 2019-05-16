package edu.uiowa.alloy2smt.translators;

import edu.uiowa.alloy2smt.Utils;
import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.alloy2smt.utils.CommandResult;
import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.FunctionDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTranslationTests
{

    @Test
    void noCommand()
    {
        String alloy = "sig A {}\n";

        Translation translation = Utils.translate(alloy);
        String command = translation.translateCommand(0, false);

        assertEquals(1, translation.getCommands().size());
    }

    @Test
    void runCommand1()
    {
        String alloy =
                "sig A {}\n" +
                "run command1 {#A = 1 or #A = 2} for 10";

        Translation translation = Utils.translate(alloy);

        Assertions.assertFalse(translation.getSmtScript().contains("(check-sat)"));
        Assertions.assertFalse(translation.getSmtScript().contains("(get-model)"));

        String command = translation.translateCommand(0, false);

        assertEquals(
                "; command1\n" +
                "(assert (or (exists ((_a3 Atom)) (and (= this_A (singleton (mkTuple _a3))) true)) (exists ((_a4 Atom)(_a5 Atom)) (and (= this_A (insert (mkTuple _a5) (singleton (mkTuple _a4)))) (distinct _a4 _a5)))))\n",
                command);
    }


    @Test
    void runCommand2()
    {
        String alloy =
                "sig A {}\n" +
                "run command1 {#A = 1 or #A = 2} for 10\n" +
                // multiple commands can share the same label
                "run command1 {no A} for 10\n"
                ;
        Translation translation = Utils.translate(alloy);

        String command = translation.translateCommand(1, true);

        assertEquals(
                "; command1\n" +
                "(assert (= this_A (as emptyset (Set (Tuple Atom)))))\n",
                command);
    }


    @Test
    void runCommandWithNewFunctionDeclaration()
    {
        String alloy =
                "sig A0, A1, A2 in Int{}\n" +
                "run command1 {A0 > A1 + A2}";

        Translation translation = Utils.translate(alloy);
        String command = translation.translateCommand(0, false);

        assertEquals(
                "(declare-fun _GT ((Set (Tuple Int))(Set (Tuple Int))) Bool)\n" +
                        "; command1\n" +
                        "(assert (_GT this_A0 (union this_A1 this_A2)))\n",
                command);
    }


    @Test
    void checkCommand1()
    {
        String alloy =
                "sig A {}\n" +
                "assert command1{some A}\n" +
                "check command1 for 10\n" +
                "assert command2 {some A or no A} \n" +
                "check command2 for 10\n";

        Translation translation = Utils.translate(alloy);

        String command1 = translation.translateCommand(0, true);

        assertEquals(
                "; command1\n" +
                        "(assert (not " +
                                    "(exists ((_x1 Atom)) " +
                                        "(member (mkTuple _x1) this_A))))\n",
                command1);

        String command2 = translation.translateCommand(1, true);

        assertEquals(
                "; command2\n" +
                        "(assert (not " +
                                    "(or " +
                                        "(exists ((_x2 Atom)) " +
                                            "(member (mkTuple _x2) this_A)) " +
                                        "(= this_A " +
                                            "(as emptyset (Set (Tuple Atom)))))))\n",
                command2);
    }

    @Test
    void scope1() throws Exception
    {
        String alloy = "sig A {} run {} for exactly 3 A";
        List<CommandResult> results =  AlloyUtils.runAlloyString(alloy, true);
        assertEquals ("sat", results.get(0).satResult);
        FunctionDefinition A = TranslatorUtils.getFunctionDefinition( results.get(0).smtModel, "this_A");
        Set<String> atoms = TranslatorUtils.getAtomSet(A);
        assertEquals (3, atoms.size());
    }

    @Test
    void scope2() throws Exception
    {
        String alloy = "sig a, b {}\n" +
                "run {} for  exactly 1 a, exactly 2 b\n" +
                "run {} for  exactly 3 a, exactly 4 b";
        List<CommandResult> results =  AlloyUtils.runAlloyString(alloy, true);

        assertEquals ("sat", results.get(0).satResult);

        FunctionDefinition a = TranslatorUtils.getFunctionDefinition( results.get(0).smtModel, "this_a");
        Set<String> atomsA = TranslatorUtils.getAtomSet(a);
        assertEquals (1, atomsA.size());

        FunctionDefinition b = TranslatorUtils.getFunctionDefinition( results.get(0).smtModel, "this_b");
        Set<String> atomsB = TranslatorUtils.getAtomSet(b);
        assertEquals (2, atomsB.size());

        a = TranslatorUtils.getFunctionDefinition( results.get(1).smtModel, "this_a");
        atomsA = TranslatorUtils.getAtomSet(a);
        assertEquals (3, atomsA.size());

        b = TranslatorUtils.getFunctionDefinition( results.get(1).smtModel, "this_b");
        atomsB = TranslatorUtils.getAtomSet(b);
        assertEquals (4, atomsB.size());
    }

    @Test
    void scope3() throws Exception
    {
        String alloy = "sig a, b {}\n" +
                "run {} for 3 but exactly 2 a";

        List<CommandResult> results =  AlloyUtils.runAlloyString(alloy, true);

        assertEquals ("sat", results.get(0).satResult);

        FunctionDefinition a = TranslatorUtils.getFunctionDefinition( results.get(0).smtModel, "this_a");
        Set<String> atomsA = TranslatorUtils.getAtomSet(a);
        assertEquals (2, atomsA.size());

        FunctionDefinition b = TranslatorUtils.getFunctionDefinition( results.get(0).smtModel, "this_b");
        Set<String> atomsB = TranslatorUtils.getAtomSet(b);
        assertEquals (0, atomsB.size());
    }
}