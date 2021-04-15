package com.github.simy4.coregex.core.rng;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class SimpleRNGTest {

  @Property
  public void shouldGenerateIntInRange(int i1, int i2, long seed) {
    int start = Math.min(i1, i2);
    int end = Math.max(i1, i2);
    int generated = new SimpleRNG(seed).genInteger(start, end).getValue();
    assertTrue(start + " <= " + generated + " <= " + end, start <= generated && generated <= end);
  }

  @Property
  public void shouldGenerateIntWhenStartAndEndAreTheSame(int startAndEnd, long seed) {
    int generated = new SimpleRNG(seed).genInteger(startAndEnd, startAndEnd).getValue();
    assertEquals(startAndEnd, generated);
  }

}