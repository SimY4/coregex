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

package com.github.simy4.coregex.junit.quickcheck;

import com.github.simy4.coregex.core.Coregex;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class CoregexGenerator extends Generator<String> {

  private static final Pattern ANY = Pattern.compile(".*");

  private Pattern regex;
  private Coregex coregex;

  public CoregexGenerator() {
    this(ANY);
  }

  public CoregexGenerator(Pattern regex) {
    super(String.class);
    this.regex = regex;
    this.coregex = Coregex.from(regex);
  }

  public void configure(Regex regex) {
    this.regex = Pattern.compile(regex.value(), regex.flags());
    this.coregex = Coregex.from(this.regex);
  }

  @Override
  public String generate(SourceOfRandomness random, GenerationStatus status) {
    return coregex.generate(random.nextLong());
  }

  @Override
  public boolean canShrink(Object larger) {
    return regex.matcher(narrow(larger)).matches();
  }

  @Override
  public List<String> doShrink(SourceOfRandomness random, String larger) {
    List<String> shrinks = new ArrayList<>();
    for (Optional<Coregex> coregex = this.coregex.shrink();
        coregex.isPresent();
        coregex = coregex.flatMap(Coregex::shrink)) {
      shrinks.add(coregex.get().generate(random.nextLong()));
    }
    return shrinks;
  }

  @Override
  public BigDecimal magnitude(Object value) {
    return BigDecimal.valueOf(narrow(value).length());
  }
}
