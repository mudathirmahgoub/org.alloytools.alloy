package edu.uiowa.alloy2smt.mapping;

import static org.junit.Assert.*;
import org.junit.Test;

import edu.uiowa.alloy2smt.Utils;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.alloy2smt.utils.AlloySettings;

public class MapperTests
{
  int univSignatureId = 2;

  @Test
  public void signature1()
  {
    String alloy = "sig A {} \n fact f {#A = 3}";
    Translation translation = Utils.translate(alloy, AlloySettings.Default);
    Mapper mapper = translation.getMapper();
    assertNotNull(mapper);

    MappingSignature signature = mapper.signatures
        .stream().filter(s -> s.label.equals("this/A"))
        .findFirst().get();


    assertFalse(signature.builtIn);
    assertFalse(signature.isAbstract);
    assertFalse(signature.id < univSignatureId);
    assertEquals((int)univSignatureId, (int) signature.parents.get(0));
    assertEquals("this/A", signature.functionName);
  }


  @Test
  public void field1()
  {
    String alloy =
        "sig A {f: A, g: A -> A} \n" +
            "sig B {f: A, g: B -> A}" +
            " fact f {#A = 3 and #B = 4}";
    Translation translation = Utils.translate(alloy, AlloySettings.Default);
    Mapper mapper = translation.getMapper();
    assertNotNull(mapper);

    MappingSignature signatureA = mapper.signatures
        .stream().filter(s -> s.label.equals("this/A"))
        .findFirst().get();

    MappingSignature signatureB = mapper.signatures
        .stream().filter(s -> s.label.equals("this/B"))
        .findFirst().get();

    assertEquals((int)univSignatureId, (int)signatureA.parents.get(0));
    assertEquals((int)univSignatureId, (int)signatureB.parents.get(0));

    assertEquals("this/A", signatureA.functionName);
    assertEquals("this/B", signatureB.functionName);

    MappingField fieldA_f = mapper.fields.stream()
                                         .filter(f -> f.parentId == signatureA.id && f.functionName.equals("this/A/f"))
                                         .findFirst().get();

    MappingField fieldA_g = mapper.fields.stream()
                                         .filter(f -> f.parentId == signatureA.id && f.functionName.equals("this/A/g"))
                                         .findFirst().get();

    MappingField fieldB_f = mapper.fields.stream()
                                         .filter(f -> f.parentId == signatureB.id && f.functionName.equals("this/B/f"))
                                         .findFirst().get();

    MappingField fieldB_g = mapper.fields.stream()
                                         .filter(f -> f.parentId == signatureB.id && f.functionName.equals("this/B/g"))
                                         .findFirst().get();

    assertEquals("f", fieldA_f.label);
    assertEquals("g", fieldA_g.label);
    assertEquals("f", fieldB_f.label);
    assertEquals("g", fieldB_g.label);

    assertEquals(2, fieldA_f.types.get(0).size());
    assertEquals((int)signatureA.id, fieldA_f.types.get(0).get(0).id);
    assertEquals((int)signatureA.id, fieldA_f.types.get(0).get(1).id);

    assertEquals(3, fieldA_g.types.get(0).size());
    assertEquals((int)signatureA.id, fieldA_g.types.get(0).get(0).id);
    assertEquals((int)signatureA.id, fieldA_g.types.get(0).get(1).id);
    assertEquals((int)signatureA.id, fieldA_g.types.get(0).get(2).id);

    assertEquals(2, fieldB_f.types.get(0).size());
    assertEquals((int)signatureB.id, fieldB_f.types.get(0).get(0).id);
    assertEquals((int)signatureA.id, fieldB_f.types.get(0).get(1).id);


    assertEquals(3, fieldB_g.types.get(0).size());
    assertEquals((int)signatureB.id, fieldB_g.types.get(0).get(0).id);
    assertEquals((int)signatureB.id, fieldB_g.types.get(0).get(1).id);
    assertEquals((int)signatureA.id, fieldB_g.types.get(0).get(2).id);
  }
}