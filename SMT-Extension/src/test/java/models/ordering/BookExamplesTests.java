package ordering.models;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;

import edu.uiowa.alloy2smt.utils.AlloyUtils;
import edu.uiowa.alloy2smt.utils.CommandResult;


public class BookExamplesTests
{ 

  @Test
  public void addressBook3a() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3a.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook3b0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3b.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3b1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3b.als", true, 1);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void addressBook3b2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3b.als", true, 2);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void addressBook3b3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3b.als", true, 3);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook3c0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3c1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3c2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3c3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als", true, 3);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void addressBook3d0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3d.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3d1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3d.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3d2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3d.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3d3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3d.als", true, 3);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void addressBook3d4() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3d.als", true, 4);
    assertEquals("unsat", result.satResult);
  }
 
  @Test
  public void hotel1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel1.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void hotel2_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel2.als", true, 0);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void hotel2_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel2.als", true, 1);
    assertEquals("unsat", result.satResult);
  }


  @Test
  public void hotel2_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel2.als", true, 2);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void hotel3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel3.als", true, 0);

    assertEquals("sat", result.satResult);

  }

  @Test
  public void hotel4() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/hotel4.als", true, 0);

    assertEquals("unsat", result.satResult);
  }  
  
  @Test
  public void ringElection1_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection1.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void ringElection1_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection1.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void ringElection1_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection1.als", true, 2);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void ringElection_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection2.als", true, 0);

    assertEquals("sat", result.satResult);
  }

  @Test
  public void ringElection_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection2.als", true, 1);

    assertEquals("unsat", result.satResult);
  }

  @Test
  public void ringElection_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection2.als", true, 2);

    assertEquals("unsat", result.satResult);
  }


  @Test
  public void ringElection_3() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection2.als", true, 3);

    assertEquals("sat", result.satResult);
  }

  @Test
  public void ringElection_4() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/chapter6/ringElection2.als", true, 4);

    assertEquals("unsat", result.satResult);
  }

  
  @Test
  public void p300_hotel() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p300-hotel.als", true, 0);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void p303_hotel_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p303-hotel.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void p303_hotel_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p303-hotel.als", true, 1);
    assertEquals("sat", result.satResult);
  }

  @Test
  public void p306_hotel_0() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p306-hotel.als", true, 0);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void p306_hotel_1() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p306-hotel.als", true, 1);
    assertEquals("unsat", result.satResult);
  }

  @Test
  public void p306_hotel_2() throws Exception
  {
    CommandResult result = AlloyUtils.runAlloyFile("../org.alloytools.alloy.extra/extra/models/book/appendixE/p306-hotel.als", true, 2);
    assertEquals("unsat", result.satResult);
  }
}
