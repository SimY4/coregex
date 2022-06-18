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

package com.github.simy4.coregex.core.generators;

import com.github.simy4.coregex.core.Set;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetGenerator extends Generator<Set> {

  private char min = Character.MIN_VALUE;
  private char max = Character.MIN_SURROGATE - 1;

  public SetGenerator() {
    super(Set.class);
  }

  @Override
  public Set generate(SourceOfRandomness random, GenerationStatus status) {
    Gen<Character> charGen = (rng, s) -> rng.nextChar(min, max);
    Gen<String> stringGen =
        charGen
            .times(status.size())
            .map(chs -> chs.stream().map(Object::toString).collect(Collectors.joining()));
    return Gen.oneOf(set(charGen, stringGen), range(charGen), union(charGen, stringGen, 4))
        .flatMap(setItem -> Gen.oneOf(setItem.build(), setItem.negate().build()))
        .generate(random, status);
  }

  @Override
  public List<Set> doShrink(SourceOfRandomness random, Set larger) {
    return Collections.singletonList(
        Set.builder().single(larger.generate(random.nextLong())).build());
  }

  @Override
  public BigDecimal magnitude(Object value) {
    return BigDecimal.valueOf(narrow(value).weight());
  }

  public void configure(InRange range) {
    min = range.minChar();
    max = range.maxChar();
  }

  private static Gen<Set.Builder> set(Gen<Character> charGen, Gen<String> stringGen) {
    return (random, status) ->
        Set.builder()
            .set(
                charGen.generate(random, status), stringGen.generate(random, status).toCharArray());
  }

  private static Gen<Set.Builder> range(Gen<Character> charGen) {
    return (random, status) -> {
      char ch1 = charGen.generate(random, status);
      char ch2 = charGen.generate(random, status);
      char start = (char) Math.min(ch1, ch2);
      char end = (char) Math.max(ch1, ch2);
      end = start == end ? (char) (end + 1) : end;
      return Set.builder().range(start, end);
    };
  }

  private static Gen<Set.Builder> union(Gen<Character> charGen, Gen<String> stringGen, int depth) {
    return depth > 0
        ? Gen.frequency(
            Gen.freq(2, set(charGen, stringGen)),
            Gen.freq(2, range(charGen)),
            Gen.freq(
                1,
                (random, status) -> union(charGen, stringGen, depth - 1).generate(random, status)))
        : Gen.oneOf(set(charGen, stringGen), range(charGen));
  }
}
