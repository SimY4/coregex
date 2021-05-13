package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class RandomRNGTest {
  @Property
  public void generatedIntShouldBeInRange(int in1, int in2, @From(Gen.class) RandomRNG rng) {
    int start = Math.min(in1, in2);
    int end = Math.max(in1, in2);
    Map.Entry<RNG, Integer> generated = rng.genInteger(start, end);
    assertTrue(start + " <= " + generated.getValue() + " <= " + end, start <= generated.getValue() && generated.getValue() <= end);
  }

  public static class Gen extends Generator<RandomRNG> {
    public Gen() {
      super(RandomRNG.class);
    }

    @Override
    public RandomRNG generate(SourceOfRandomness random, GenerationStatus status) {
      return new RandomRNG(new Random(random.seed()));
    }
  }
}