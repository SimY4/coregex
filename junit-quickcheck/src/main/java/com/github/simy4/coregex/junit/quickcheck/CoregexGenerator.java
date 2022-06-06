/*
 * Copyright 2021 Alex Simkin
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
import com.github.simy4.coregex.core.CoregexParser;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.regex.Pattern;

public class CoregexGenerator extends Generator<String> {
  private Coregex coregex;
  private int size;

  public CoregexGenerator() {
    super(String.class);
  }

  public CoregexGenerator(Pattern regex) {
    this(regex, Integer.MAX_VALUE);
  }

  public CoregexGenerator(Pattern regex, int size) {
    this();
    this.coregex = CoregexParser.getInstance().parse(regex);
    this.size = size;
  }

  public void configure(Regex regex) {
    this.coregex = CoregexParser.getInstance().parse(Pattern.compile(regex.value()));
  }

  public void configure(Size size) {
    this.size = size.max();
  }

  @Override
  public String generate(SourceOfRandomness random, GenerationStatus status) {
    return coregex.generate(new SourceOfRandomnessRNG(random), size);
  }
}
