package com.github.simy4.coregex.junit.quickcheck;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class SourceOfRandomnessRNGTest {

  @Property
  public void shouldGenerateIntInRange(int i1, int i2, long seed) {
    int start = Math.min(i1, i2);
    int end = Math.max(i1, i2);
    SourceOfRandomness sourceOfRandomness = new SourceOfRandomness(new Random(seed));
    int generated = new SourceOfRandomnessRNG(sourceOfRandomness).genInteger(start, end).getValue();
    assertTrue(start + " <= " + generated + " <= " + end, start <= generated && generated <= end);
  }

  @Property
  public void shouldGenerateIntWhenStartAndEndAreTheSame(int startAndEnd, long seed) {
    SourceOfRandomness sourceOfRandomness = new SourceOfRandomness(new Random(seed));
    int generated = new SourceOfRandomnessRNG(sourceOfRandomness).genInteger(startAndEnd, startAndEnd).getValue();
    assertEquals(startAndEnd, generated);
  }

}