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

import com.github.simy4.coregex.core.Pair;
import com.github.simy4.coregex.core.RNG;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomRNG implements RNG, Serializable {
  private static final long serialVersionUID = 1L;

  private final long seed;

  public RandomRNG() {
    this(ThreadLocalRandom.current().nextLong());
  }

  public RandomRNG(long seed) {
    this.seed = seed;
  }

  @Override
  public Pair<RNG, Boolean> genBoolean() {
    Random rng = new Random(seed);
    return new Pair<>(new RandomRNG(rng.nextLong()), rng.nextBoolean());
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public Pair<RNG, Integer> genInteger(int startInc, int endInc) {
    if (startInc > endInc) {
      throw new IllegalArgumentException(
          "startInc: " + startInc + " should be <= than endInc: " + endInc);
    } else if (startInc == endInc) {
      return new Pair<>(this, startInc);
    }
    Random rng = new Random(seed);
    return new Pair<>(
        new RandomRNG(rng.nextLong()), rng.ints(1, startInc, endInc + 1).findFirst().getAsInt());
  }

  @Override
  public Pair<RNG, Long> genLong() {
    Random rng = new Random(seed);
    return new Pair<>(new RandomRNG(rng.nextLong()), rng.nextLong());
  }
}
