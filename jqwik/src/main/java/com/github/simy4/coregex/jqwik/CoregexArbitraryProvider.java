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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

public class CoregexArbitraryProvider implements ArbitraryProvider {
  @Override
  public boolean canProvideFor(TypeUsage targetType) {
    return targetType.isAssignableFrom(String.class) && targetType.isAnnotated(Regex.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(TypeUsage targetType, SubtypeProvider subtypeProvider) {
    Optional<Regex> optionalRegex = targetType.findAnnotation(Regex.class);
    return optionalRegex
        .map(regex -> new CoregexArbitrary(Pattern.compile(regex.value(), regex.flags())))
        .map(decorator -> Collections.<Arbitrary<?>>singleton(decorator.arbitrary()))
        .orElse(Collections.emptySet());
  }

  @Override
  public int priority() {
    return 5;
  }
}
