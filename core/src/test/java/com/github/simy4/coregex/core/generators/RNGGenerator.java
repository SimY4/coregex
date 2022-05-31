package com.github.simy4.coregex.core.generators;

import com.github.simy4.coregex.core.RNG;
import com.github.simy4.coregex.core.rng.RandomRNG;
import com.github.simy4.coregex.core.rng.SimpleRNG;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class RNGGenerator extends Generator<RNG> {
  public RNGGenerator() {
    super(RNG.class);
  }

  @Override
  public RNG generate(SourceOfRandomness random, GenerationStatus status) {
    return Gen.oneOf(new SimpleRNG(random.nextLong()), new RandomRNG(random.nextLong())).generate(random, status);
  }
}
