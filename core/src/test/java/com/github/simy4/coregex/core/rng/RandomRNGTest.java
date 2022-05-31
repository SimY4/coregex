package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class RandomRNGTest implements RNGSpecification {
  @Override
  public RNG rng(long seed) {
    return new RandomRNG(seed);
  }
}