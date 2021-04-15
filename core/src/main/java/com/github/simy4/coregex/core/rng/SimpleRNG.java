package com.github.simy4.coregex.core.rng;

import com.github.simy4.coregex.core.RNG;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;

public final class SimpleRNG implements RNG, Serializable {
  private static final long serialVersionUID = 1L;

  private final long seed;

  public SimpleRNG(long seed) {
    this.seed = seed;
  }

  @Override
  public Map.Entry<RNG, Integer> genInteger(int startInc, int endInc) {
    if (startInc > endInc) {
      throw new IllegalArgumentException("startInc: " + startInc + " should be <= than endInc: " + endInc);
    }
    Map.Entry<RNG, Long> rngAndLong = genLong();
    int nextInt = (int) (startInc + rngAndLong.getValue() % (endInc - startInc + 1L));
    return new AbstractMap.SimpleEntry<>(rngAndLong.getKey(), nextInt);
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    long newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
    RNG nextRng = new SimpleRNG(newSeed);
    return new AbstractMap.SimpleEntry<>(nextRng, newSeed);
  }
}
