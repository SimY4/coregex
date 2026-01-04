/*
 * Copyright 2021-2026 Alex Simkin
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

package com.github.simy4.coregex.jetCheck;

import com.github.simy4.coregex.core.Coregex;
import java.util.regex.Pattern;
import org.jetbrains.jetCheck.Generator;

public final class CoregexGenerator {
  public static Generator<String> of(String regex) {
    return of(Pattern.compile(regex));
  }

  public static Generator<String> of(String regex, int flags) {
    return of(Pattern.compile(regex, flags));
  }

  public static Generator<String> of(Pattern pattern) {
    Coregex coregex = Coregex.from(pattern);
    return Generator.from(
        generationEnvironment ->
            coregex.generate(generationEnvironment.generate(Generator.integers())));
  }

  private CoregexGenerator() {}
}
