package edu.uiowa.alloy2smt.translators;

import edu.uiowa.alloy2smt.Utils;
import edu.uiowa.alloy2smt.smtAst.*;
import edu.uiowa.shared.CommandResult;
import edu.uiowa.shared.Cvc4Task;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import java.util.List;

public class ArithmeticTests
{
    private int getSingletonInt(FunctionDefinition definition)
    {
        UnaryExpression unary =  (UnaryExpression) definition.expression;
        Assertions.assertEquals(UnaryExpression.Op.SINGLETON, unary.getOP());
        MultiArityExpression tuple =  (MultiArityExpression) unary.getExpression();
        Assertions.assertEquals(MultiArityExpression.Op.MKTUPLE, tuple.getOp());
        IntConstant constant = (IntConstant) tuple.getExpressions().get(0);
        return Integer.parseInt(constant.getValue());
    }

    private FunctionDefinition getFunctionDefinition(CommandResult commandResult, String name)
    {
        FunctionDefinition definition = (FunctionDefinition) commandResult.smtModel
                .getFunctions().stream()
                .filter(f -> f.getName().equals(name)).findFirst().get();
        definition = commandResult.smtModel.evaluateUninterpretedInt(definition);
        return definition;
    }

    @Test
    public void simple() throws Exception
    {
        String alloy =
                "sig a, b, c in Int {}\n" +
                "fact {\n" +
                "#a = 1\n" +
                "#b = 1\n" +
                "1 = 1\n" +
                "plus[a, b] = 2\n" +
                "plus[c, 0] = 2\n" +
                "}\n";
        Translation translation = Utils.translate(alloy);
        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertTrue(commandResults.size() == 1);
        Assertions.assertEquals("sat", commandResults.get(0).result);
        FunctionDefinition a = getFunctionDefinition(commandResults.get(0), "this_a");
        FunctionDefinition b = getFunctionDefinition(commandResults.get(0), "this_b");
        FunctionDefinition c = getFunctionDefinition(commandResults.get(0), "this_c");

        int aValue = getSingletonInt(a);
        int bValue = getSingletonInt(b);
        int cValue = getSingletonInt(c);
        Assertions.assertEquals(2, aValue + bValue);
        Assertions.assertEquals(2, cValue);
    }

    @Test
    public void test1() throws Exception
    {
        String alloy =
                "sig a, b, c in Int {} \n" +
                "fact { \n" +
                "#a = 2 \n" +
                "#b = 2\n" +
                "#c = 4 \n" +
                "plus[a, b] = c\n" +
                "}";
        Translation translation = Utils.translate(alloy);
        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertTrue(commandResults.size() == 1);
        Assertions.assertEquals("sat", commandResults.get(0).result);
        FunctionDefinition plus = getFunctionDefinition(commandResults.get(0), Alloy2SmtTranslator.plus);

    }

    @Test
    public void plusMinus() throws Exception
    {
        String alloy =
                "sig a, b, c in Int {} \n" +
                "fact { \n" +
                "plus[a, b] = c \n" +
                "minus[a,b] = c\n" +
                "}";
        Translation translation = Utils.translate(alloy);
        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertTrue(commandResults.size() == 1);
        Assertions.assertEquals("sat", commandResults.get(0).result);
        FunctionDefinition plus = getFunctionDefinition(commandResults.get(0), Alloy2SmtTranslator.plus);
    }

    @Test
    public void remainder() throws Exception
    {
        String alloy =
                "sig a, b, c in Int {} \n" +
                "fact { \n" +
                "#a = 2\n" +
                "8 in a\n" +
                "6 in a\n" +
                "#b = 1\n" +
                "rem[a,b] = c\n" +
                "}";
        Translation translation = Utils.translate(alloy);
        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertTrue(commandResults.size() == 1);
        Assertions.assertEquals("sat", commandResults.get(0).result);
        FunctionDefinition mod = getFunctionDefinition(commandResults.get(0), Alloy2SmtTranslator.mod);
    }

    @Test
    public void union() throws Exception
    {
        String alloy = "sig a in Int {} fact {a = 6 + 8}";
        Translation translation = Utils.translate(alloy);
        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertEquals("sat", commandResults.get(0).result);
        FunctionDefinition a = getFunctionDefinition(commandResults.get(0), "this_a");
    }

    @Test
    public void testPlusMinus() throws Exception
    {
        String alloy =
                "sig a, b, c, d in Int {}\n" +
                "fact add{plus[a,b] = c + d}\n" +
                "fact subtract{minus[a,b] = c - d}\n" +
                "fact notEqual{a != c and b != d}\n" +
                "fact nonzero {a > 0 and b > 0 and c > 0 and d > 0}\n";
        Translation translation = Utils.translate(alloy);

        Cvc4Task task = new Cvc4Task();
        List<CommandResult> commandResults =  task.run(translation);
        Assertions.assertEquals("unsat", commandResults.get(0).result);
    }
}
