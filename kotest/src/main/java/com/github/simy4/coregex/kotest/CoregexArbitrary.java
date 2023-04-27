/*
 * Copyright 2021-2023 Alex Simkin
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

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.RNG;
import com.github.simy4.coregex.core.rng.RandomRNG;
import io.kotest.property.Arb;
import io.kotest.property.Classifier;
import io.kotest.property.GenKt;
import io.kotest.property.RandomSource;
import io.kotest.property.Sample;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CoregexArbitrary extends Arb<String> {
  public static CoregexArbitrary of(String pattern) {
    return of(pattern, 0);
  }

  public static CoregexArbitrary of(String pattern, int flags) {
    return new CoregexArbitrary(Pattern.compile(pattern, flags));
  }

  private final Coregex coregex;
  private final Set<String> edgeCases = new HashSet<>();
  private int sized = -1;

  public CoregexArbitrary(Pattern pattern) {
    this.coregex = Coregex.from(pattern);
  }

  @NotNull
  @Override
  public Classifier<? extends java.lang.String> getClassifier() {
    return string -> "'" + string + "' matching '" + coregex + '\'';
  }

  @Nullable
  @Override
  public String edgecase(@NotNull RandomSource randomSource) {
    if (edgeCases.isEmpty()) {
      return null;
    }
    int idx = randomSource.getRandom().nextInt(edgeCases.size());
    Iterator<String> iterator = edgeCases.iterator();
    for (int counter = 0; iterator.hasNext() && counter < idx; counter++) {
      iterator.next();
    }
    return iterator.next();
  }

  @NotNull
  @Override
  public Sample<String> sample(@NotNull RandomSource randomSource) {
    RNG rng = new RandomRNG(randomSource.getRandom().nextLong());
    CoregexShrinker shrinker = new CoregexShrinker(coregex, rng);
    String sample = coregex.sized(sized >= 0 ? sized : Integer.MAX_VALUE - 2).generate(rng);
    return GenKt.sampleOf(sample, shrinker);
  }

  public CoregexArbitrary withSize(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Size must be positive");
    }
    sized = size;
    return this;
  }

  public CoregexArbitrary withEdgeCases(String... edgeCases) {
    this.edgeCases.addAll(Arrays.asList(edgeCases));
    return this;
  }
}
