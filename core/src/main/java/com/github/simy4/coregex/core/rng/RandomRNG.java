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
  public Map.Entry<RNG, Boolean> genBoolean() {
    SplittableRandom rng = random.split();
    return new AbstractMap.SimpleEntry<>(new RandomRNG(rng), rng.nextBoolean());
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
