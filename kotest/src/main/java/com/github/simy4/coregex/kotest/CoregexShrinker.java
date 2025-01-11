/*
 * Copyright 2021-2025 Alex Simkin
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

package com.github.simy4.coregex.kotest;

import static java.util.Objects.requireNonNull;

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.RNG;
import io.kotest.property.Shrinker;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CoregexShrinker implements Shrinker<String> {
  private final Coregex coregex;
  private final RNG rng;

  public CoregexShrinker(Coregex coregex, RNG rng) {
    this.coregex = requireNonNull(coregex, "coregex");
    this.rng = requireNonNull(rng, "rng");
  }

  @NotNull
  @Override
  public List<String> shrink(String s) {
    List<String> shinks = new ArrayList<String>();
    for (int remainder = coregex.minLength();
        remainder < s.length();
        remainder = (remainder * 2) + 1) {
      shinks.add(coregex.sized(remainder).generate(rng));
    }
    return shinks;
  }
}
