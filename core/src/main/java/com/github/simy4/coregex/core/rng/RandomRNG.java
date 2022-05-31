package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Random;

public class RandomRNG implements RNG, Serializable {
  private static final long serialVersionUID = 1L;

  private final Random random;

  public RandomRNG() {
    this(new Random());
  }

  public RandomRNG(long seed) {
    this(new Random(seed));
  }

  public RandomRNG(Random random) {
    this.random = random;
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public Map.Entry<RNG, Integer> genInteger(int startInc, int endInc) {
    if (startInc > endInc) {
      throw new IllegalArgumentException(
          "startInc: " + startInc + " should be <= than endInc: " + endInc);
    } else if (startInc == endInc) {
      return new AbstractMap.SimpleEntry<>(this, startInc);
    }
    return new AbstractMap.SimpleEntry<>(
        this, random.ints(1, startInc, endInc + 1).findFirst().getAsInt());
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    return new AbstractMap.SimpleEntry<>(this, random.nextLong());
  }
}
