/*
 * Copyright 2021-2024 Alex Simkin
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

import static java.util.Objects.requireNonNull;

import io.vavr.test.Arbitrary;
import io.vavr.test.Gen;
import java.util.regex.Pattern;

public class CoregexArbitrary implements Arbitrary<String> {
  public static CoregexArbitrary of(String regex) {
    return of(regex, 0);
  }

  public static CoregexArbitrary of(String regex, int flags) {
    return new CoregexArbitrary(Pattern.compile(regex, flags));
  }

  private final Pattern pattern;
  private int sized = -1;

  public CoregexArbitrary(Pattern pattern) {
    this.pattern = requireNonNull(pattern, "pattern");
  }

  public CoregexArbitrary withSize(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Size must be positive");
    }
    this.sized = size;
    return this;
  }

  @Override
  public Gen<String> apply(int size) {
    int sized = this.sized;
    sized = -1 == sized ? size : sized;
    return new CoregexGenerator(pattern, sized);
  }
}
