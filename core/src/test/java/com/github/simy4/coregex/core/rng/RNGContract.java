package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;
import com.pholser.junit.quickcheck.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public interface RNGContract {
  RNG rng(long seed);

  @Property
  default void shouldGenerateIntInRange(int i1, int i2, long seed) {
    int start = Math.min(i1, i2);
    int end = Math.max(i1, i2);
    int generated = rng(seed).genInteger(start, end).getValue();
    assertTrue(start + " <= " + generated + " <= " + end, start <= generated && generated <= end);
  }

  @Property
  default void shouldGenerateIntWhenStartAndEndAreTheSame(int startAndEnd, long seed) {
    int generated = rng(seed).genInteger(startAndEnd, startAndEnd).getValue();
    assertEquals(startAndEnd, generated);
  }
}
