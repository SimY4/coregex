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

import static kotlin.LazyKt.lazy;

import com.github.simy4.coregex.core.Coregex;
import io.kotest.property.Arb;
import io.kotest.property.Classifier;
import io.kotest.property.RTree;
import io.kotest.property.RandomSource;
import io.kotest.property.Sample;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import kotlin.jvm.functions.Function0;
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
    return null;
  }

  @NotNull
  @Override
  public Sample<String> sample(@NotNull RandomSource randomSource) {
    long seed = randomSource.getRandom().nextLong();
    String sample = coregex.generate(seed);
    return new Sample<>(
        sample, new RTree<>(() -> sample, lazy(new CoregexShrinker(coregex, seed))));
  }
}

final class CoregexShrinker implements Function0<List<RTree<String>>> {
  private final Coregex coregex;
  private final long seed;

  public CoregexShrinker(Coregex coregex, long seed) {
    this.coregex = coregex;
    this.seed = seed;
  }

  @Override
  public List<RTree<String>> invoke() {
    return coregex
        .shrink()
        .map(
            coregex -> {
              String shrink = coregex.generate(seed);
              return Collections.singletonList(
                  new RTree<>(() -> shrink, lazy(new CoregexShrinker(coregex, seed))));
            })
        .orElse(Collections.emptyList());
  }
}
