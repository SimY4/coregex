package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;

import java.util.AbstractMap;
import java.util.Map;
import java.util.SplittableRandom;

public class RandomRNG implements RNG {
  private final SplittableRandom random;

  public RandomRNG() {
    this(new SplittableRandom());
  }

  public RandomRNG(long seed) {
    this(new SplittableRandom(seed));
  }

  public RandomRNG(SplittableRandom random) {
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
    SplittableRandom rng = random.split();
    return new AbstractMap.SimpleEntry<>(
        new RandomRNG(rng), rng.ints(1, startInc, endInc + 1).findFirst().getAsInt());
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    SplittableRandom rng = random.split();
    return new AbstractMap.SimpleEntry<>(new RandomRNG(rng), rng.nextLong());
  }
}
