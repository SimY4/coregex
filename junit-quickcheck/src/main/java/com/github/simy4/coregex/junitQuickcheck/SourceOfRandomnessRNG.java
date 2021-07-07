package com.github.simy4.coregex.junitQuickcheck;

import com.github.simy4.coregex.core.RNG;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.AbstractMap;
import java.util.Map;

final class SourceOfRandomnessRNG implements RNG {
  private final SourceOfRandomness sourceOfRandomness;

  public SourceOfRandomnessRNG(SourceOfRandomness sourceOfRandomness) {
    this.sourceOfRandomness = sourceOfRandomness;
  }

  @Override
  public Map.Entry<RNG, Integer> genInteger(int startInc, int endInc) {
    return new AbstractMap.SimpleEntry<>(this, sourceOfRandomness.nextInt(startInc, endInc));
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    return new AbstractMap.SimpleEntry<>(this, sourceOfRandomness.nextLong());
  }
}
