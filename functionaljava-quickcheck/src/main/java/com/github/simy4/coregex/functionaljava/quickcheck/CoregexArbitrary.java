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

package com.github.simy4.coregex.functionaljava.quickcheck;

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.rng.RandomRNG;
import fj.test.Gen;
import java.util.regex.Pattern;

public final class CoregexArbitrary {
  public static Gen<String> of(String regex) {
    return of(regex, 0);
  }

  public static Gen<String> of(String regex, int flags) {
    Coregex coregex = Coregex.from(Pattern.compile(regex, flags));
    return Gen.gen(
        size -> {
          Coregex sized = coregex.sized(Math.max(size, coregex.minLength()));
          return rand -> sized.generate(new RandomRNG(rand.choose(Long.MIN_VALUE, Long.MAX_VALUE)));
        });
  }

  private CoregexArbitrary() {
    throw new UnsupportedOperationException("new");
  }
}
