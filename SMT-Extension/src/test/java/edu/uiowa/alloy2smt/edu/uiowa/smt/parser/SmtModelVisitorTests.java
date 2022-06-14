package edu.uiowa.alloy2smt.edu.uiowa.smt.parser;

import static org.junit.Assert.*;
import org.junit.Test;

import edu.uiowa.smt.Result;
import edu.uiowa.smt.smtAst.SmtUnsatCore;


import java.util.List;

public class SmtModelVisitorTests
{
  @Test
  public void unsatCore1() throws Exception
  {
    String smt = "(\n" +
        "|{\"filename\":\"file.als\",\"x1\":1,\"y1\":4,\"x2\":13,\"y2\":4,\"symbolIndex\":5}|\n" +
        "|{\"filename\":\"file.als\",\"x1\":1,\"y1\":3,\"x2\":13,\"y2\":3,\"symbolIndex\":4}|\n" +
        ")";
    Result result = new Result();
    SmtUnsatCore smtUnsatCore = result.parseUnsatCore(smt);
    List<String> core = smtUnsatCore.getCore();
    assertEquals("{\"filename\":\"file.als\",\"x1\":1,\"y1\":4,\"x2\":13,\"y2\":4,\"symbolIndex\":5}", core.get(0));
    assertEquals("{\"filename\":\"file.als\",\"x1\":1,\"y1\":3,\"x2\":13,\"y2\":3,\"symbolIndex\":4}", core.get(1));
  }
}
