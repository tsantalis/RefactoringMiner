package br.ufmg.dcc.labsoft.refdetector.model.builder;

import org.junit.Test;

public class TestBigram {

  @Test
  public void testSim() {
    System.out.println(DiffUtils.similarity("for(int j = 0; j < 100; j++) {", "for(int j = 0; j < 100; j++) {"));
  }
  
}
