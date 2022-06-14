package edu.uiowa.alloy2smt.translators;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;

import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.alloy2smt.utils.CommandResult;

import java.util.List;

public class ExprITETranslatorTests
{
  @Test
  public void ite() throws Exception
  {
    String alloy = "sig A {}\n" +
        "sig A0, A1 in A {}\n" +
        "fact{A = {(A0 in A1) => A1 else A0}}\n" +
        "run {#A0 = 2 and #A1 = 1}\n" +
        "run {#A0 = 1 and #A1 = 2}";

    List<CommandResult> commandResults = AlloyUtils.runAlloyString(alloy, false);

    assertEquals("sat", commandResults.get(0).satResult);
    assertEquals("sat", commandResults.get(1).satResult);
  }
}
