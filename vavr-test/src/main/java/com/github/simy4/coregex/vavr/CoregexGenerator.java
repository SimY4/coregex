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

package com.github.simy4.coregex.vavr;

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.rng.RandomRNG;
import io.vavr.test.Gen;

import java.util.Random;
import java.util.regex.Pattern;

public class CoregexGenerator implements Gen<String> {
  private final Coregex coregex;
  private final int size;

  public CoregexGenerator(Pattern regex, int size) {
    this.coregex = Coregex.from(regex);
    this.size = Math.max(size, coregex.minLength());
  }

  @Override
  public String apply(Random random) {
    return coregex.sized(size).generate(new RandomRNG(random.nextLong()));
  }
}
