package simple.models;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;

import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.alloy2smt.utils.CommandResult;


public class BookExamplesTests
{

  @Test
  public void addressBook1a() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1a.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1b() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1b.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1c() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1c.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook1d() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1d.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1e() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1e.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1f() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1f.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1g() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1g.als", true, 0);
    assertEquals("sat", result.satResult);
  }


  @Test
  public void addressBook1h0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1h1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 1);

    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook1h2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook1h3() throws Exception
  {
    CommandResult results = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 3);

    assertEquals("unsat", results.satResult);
  }

  @Test
  public void addressBook1h4() throws Exception
  {
    CommandResult results = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 4);

    assertEquals("unsat", results.satResult);
  }

  @Test
  public void addressBook1h5() throws Exception
  {
    CommandResult results = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1h.als", true, 5);

    assertEquals("unsat", results.satResult);
  }

  @Test
  public void addressBook2a() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2a.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2b() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2b.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2c() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2c.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2d() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2d.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2e0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2e.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook2e1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2e.als", true, 1);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void addressBook2e2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2e.als", true, 2);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2e3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook2e.als", true, 3);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void filesystem0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/filesystem.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void filesystem1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/filesystem.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void filesystem2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/filesystem.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void grandpa1_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa1.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void grandpa1_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa1.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void grandpa1_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa1.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void grandpa2_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa2.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void grandpa2_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa2.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void grandpa3_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa3.als", true, 0);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void grandpa3_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa3.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void grandpa3_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/grandpa3.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void lights() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter4/lights.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter5/addressBook.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter5/addressBook.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void lists() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter5/lists.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void sets1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter5/sets1.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void sets2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter5/sets2.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void abstractMemory_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/abstractMemory.als", true, 0);

    assertEquals("unsat", result.satResult);
  }

  @Test
  public void abstractMemory_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/abstractMemory.als", true, 1);

    assertEquals("unsat", result.satResult);
  }

  @Test
  public void cacheMemory() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/cacheMemory.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkCache_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkCache.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkCache_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkCache.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkCache_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkCache.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkCache_3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkCache.als", true, 3);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkFixedSize_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkFixedSize.als", true, 0);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void checkFixedSize_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkFixedSize.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void checkFixedSize_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/checkFixedSize.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void fixedSizeMemory() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/memory/fixedSizeMemory.als", true, 0);
    assertEquals("sat", result.satResult);
  }
 
  @Test
  public void mediaAssets_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/mediaAssets.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void mediaAssets_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/mediaAssets.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void mediaAssets_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/mediaAssets.als", true, 2);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void mediaAssets_3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/mediaAssets.als", true, 3);
    assertEquals("unsat", result.satResult);
  }
 
  @Test
  public void addressBook1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/addressBook1.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/addressBook2.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void barbers() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/barbers.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void closure() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/closure.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void distribution() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/distribution.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void phones() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/phones.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void prison() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/prison.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void properties_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/properties.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void properties_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/properties.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void ring() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/ring.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void spanning() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/spanning.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void tree() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/tree.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void tube() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/tube.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void undirected() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixA/undirected.als", true, 0);
    assertEquals("sat", result.satResult);
  }
 }
