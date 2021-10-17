/*
 * Copyright 2021 Alex Simkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      throw new IllegalArgumentException(
          "startInc: " + startInc + " should be <= than endInc: " + endInc);
    } else if (startInc == endInc) {
      return new AbstractMap.SimpleEntry<>(this, startInc);
    }
    Map.Entry<RNG, Long> rngAndLong = genLong();
    int nextInt = (int) (startInc + rngAndLong.getValue() % (endInc - startInc + 1));
    return new AbstractMap.SimpleEntry<>(rngAndLong.getKey(), nextInt);
  }

  @Override
  public Map.Entry<RNG, Long> genLong() {
    long newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
    RNG nextRng = new SimpleRNG(newSeed);
    return new AbstractMap.SimpleEntry<>(nextRng, newSeed);
  }
}
