package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Random;

public final class RandomRNG implements RNG, Serializable {

  private final Random random;

  public RandomRNG(Random random) {
    this.random = random;
  }

  @Override
  public Map.Entry<RNG, Integer> genInteger(int startInc, int endInc) {
    long nextInt = startInc + random.nextLong() % (endInc - startInc + 1);
    return new AbstractMap.SimpleEntry<>(this, (int) nextInt);
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    return new AbstractMap.SimpleEntry<>(this, random.nextLong());
  }
}
