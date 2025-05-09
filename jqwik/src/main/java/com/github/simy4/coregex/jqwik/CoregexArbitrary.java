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

package com.github.simy4.coregex.jqwik;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.arbitraries.ArbitraryDecorator;

public class CoregexArbitrary extends ArbitraryDecorator<String> {
  public static CoregexArbitrary of(String regex) {
    return of(regex, 0);
  }

  public static CoregexArbitrary of(String regex, int flags) {
    return new CoregexArbitrary(Pattern.compile(regex, flags));
  }

  private final Pattern pattern;

  public CoregexArbitrary(Pattern pattern) {
    this.pattern = requireNonNull(pattern, "pattern");
  }

  @Override
  protected Arbitrary<String> arbitrary() {
    return Arbitraries.fromGenerator(new CoregexGenerator(pattern));
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
    return pattern.flags() == that.pattern.flags()
        && pattern.pattern().equals(that.pattern.pattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern.flags(), pattern.pattern());
  }
}
