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

package com.github.simy4.coregex.jqwik;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.EdgeCases;
import net.jqwik.api.RandomGenerator;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.arbitraries.ArbitraryDecorator;

public class CoregexArbitrary extends ArbitraryDecorator<String> {
  public static CoregexArbitrary of(String regex) {
    return of(regex, 0);
  }

  public static CoregexArbitrary of(String regex, int flags) {
    return new CoregexArbitrary(Pattern.compile(regex, flags));
  }

  private final Pattern pattern;
  private final Set<String> edgeCases = new HashSet<>();
  private int sized = -1;

  public CoregexArbitrary(Pattern pattern) {
    this.pattern = requireNonNull(pattern, "pattern");
  }

  @Override
  protected Arbitrary<String> arbitrary() {
    return new SizedArbitrary(pattern, edgeCases, sized);
  }

  public CoregexArbitrary withSize(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Size must be positive");
    }
    sized = size;
    return this;
  }

  /** @deprecated User {@link net.jqwik.api.Arbitrary#edgeCases(Consumer)} instead. For removal. */
  @Deprecated
  public CoregexArbitrary withEdgeCases(String... edgeCases) {
    this.edgeCases.addAll(Arrays.asList(edgeCases));
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CoregexArbitrary that = (CoregexArbitrary) o;
    return sized == that.sized
        && pattern.flags() == that.pattern.flags()
        && pattern.pattern().equals(that.pattern.pattern())
        && edgeCases.equals(that.edgeCases);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern.flags(), pattern.pattern(), edgeCases, sized);
  }
}

final class SizedArbitrary implements Arbitrary<String> {
  private final Pattern pattern;
  private final Set<String> edgeCases;
  private final int size;

  SizedArbitrary(Pattern pattern, Set<String> edgeCases, int size) {
    this.pattern = pattern;
    this.edgeCases = edgeCases;
    this.size = size;
  }

  @Override
  public RandomGenerator<String> generator(int genSize) {
    int size = this.size;
    size = -1 == size ? genSize : size;
    return new CoregexGenerator(pattern, size);
  }

  @Override
  public EdgeCases<String> edgeCases(int maxEdgeCases) {
    return EdgeCases.fromSuppliers(
        edgeCases.stream()
            .<Supplier<Shrinkable<String>>>map(edgeCase -> () -> Shrinkable.unshrinkable(edgeCase))
            .collect(Collectors.toList()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SizedArbitrary that = (SizedArbitrary) o;
    return size == that.size
        && pattern.flags() == that.pattern.flags()
        && pattern.pattern().equals(that.pattern.pattern())
        && edgeCases.equals(that.edgeCases);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern.flags(), pattern.pattern(), edgeCases, size);
  }
}
