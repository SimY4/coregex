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

import com.github.simy4.coregex.core.Coregex;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.Optional;

@InRange(maxChar = Character.MIN_SURROGATE - 1)
public class CoregexGenerator extends Generator<Coregex> {
  private static final GenerationStatus.Key<Integer> DEPTH =
      new GenerationStatus.Key<>("depth", Integer.class);

  private InRange range;

  public CoregexGenerator() {
    super(Coregex.class);
    configure(getClass());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
    Concat concatGen = gen().make(Concat.class);
    Literal literalGen = gen().make(Literal.class);
    Set setGen = gen().make(Set.class);
    Union unionGen = gen().make(Union.class);
    concatGen.configure(range);
    literalGen.configure(range);
    setGen.configure(range);
    unionGen.configure(range);
    return gen()
        .oneOf(concatGen, literalGen, setGen, unionGen)
        .flatMap(
            coregex ->
                Gen.frequency(
                    Gen.freq(3, Gen.pure(coregex)),
                    Gen.freq(
                        1,
                        (rng, sts) -> {
                          int i1 = rng.nextInt(0, 20);
                          int i2 = rng.nextInt(0, 20);
                          return coregex.quantify(Math.min(i1, i2), Math.max(i1, i2));
                        })))
        .generate(random, status);
  }

  public void configure(InRange range) {
    this.range = range;
  }

  @InRange(maxChar = Character.MIN_SURROGATE - 1)
  public static class Concat extends Generator<Coregex> {
    private InRange range;

    public Concat() {
      super(Coregex.class);
      configure(getClass());
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      Literal literalGen = gen().make(Literal.class);
      Set setGen = gen().make(Set.class);
      Union unionGen = gen().make(Union.class);
      literalGen.configure(range);
      setGen.configure(range);
      unionGen.configure(range);

      int depth;
      Optional<Integer> maybeDepth = status.valueOf(DEPTH);
      if (maybeDepth.isPresent()) {
        depth = maybeDepth.get();
        status.setValue(DEPTH, depth - 1);
      } else {
        depth = random.nextInt(0, 2);
        status.setValue(DEPTH, depth);
      }
      return concatGen(coregexGen(literalGen, setGen, unionGen, depth)).generate(random, status);
    }

    private Gen<Coregex> coregexGen(Literal literalGen, Set setGen, Union unionGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(1, this), Gen.freq(1, literalGen), Gen.freq(3, setGen), Gen.freq(1, unionGen));
      } else {
        return Gen.oneOf(setGen);
      }
    }

    private Gen<Coregex> concatGen(Gen<Coregex> coregexGen) {
      return (random, status) -> {
        Coregex first = coregexGen.generate(random, status);
        Coregex[] rest = new Coregex[random.nextInt(0, 10)];
        for (int i = 0; i < rest.length; i++) {
          rest[i] = coregexGen.generate(random, status);
        }
        return new Coregex.Union(first, rest);
      };
    }

    public void configure(InRange range) {
      this.range = range;
    }
  }

  @InRange(maxChar = Character.MIN_SURROGATE - 1)
  public static class Literal extends Generator<Coregex> {
    private char min;
    private char max;

    public Literal() {
      super(Coregex.class);
      configure(getClass());
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      return Gen.oneOf(
              Coregex.empty(), new Coregex.Literal(String.valueOf(random.nextChar(min, max))))
          .generate(random, status);
    }

    public void configure(InRange range) {
      this.min = range.minChar();
      this.max = range.maxChar();
    }
  }

  @InRange(maxChar = Character.MIN_SURROGATE - 1)
  public static class Set extends Generator<Coregex> {
    private InRange range;

    public Set() {
      super(Coregex.class);
      configure(getClass());
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      SetGenerator setGenerator = gen().make(SetGenerator.class);
      setGenerator.configure(range);
      return setGenerator.map(Coregex.Set::new).generate(random, status);
    }

    public void configure(InRange range) {
      this.range = range;
    }
  }

  @InRange(maxChar = Character.MIN_SURROGATE - 1)
  public static class Union extends Generator<Coregex> {
    private InRange range;

    public Union() {
      super(Coregex.class);
      configure(getClass());
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      Concat concatGen = gen().make(Concat.class);
      Literal literalGen = gen().make(Literal.class);
      Set setGen = gen().make(Set.class);
      concatGen.configure(range);
      literalGen.configure(range);
      setGen.configure(range);

      int depth;
      Optional<Integer> maybeDepth = status.valueOf(DEPTH);
      if (maybeDepth.isPresent()) {
        depth = maybeDepth.get();
        status.setValue(DEPTH, depth - 1);
      } else {
        depth = random.nextInt(0, 2);
        status.setValue(DEPTH, depth);
      }
      return unionGen(coregexGen(concatGen, literalGen, setGen, depth)).generate(random, status);
    }

    private Gen<Coregex> coregexGen(Concat concatGen, Literal literalGen, Set setGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(1, concatGen),
            Gen.freq(2, literalGen),
            Gen.freq(2, setGen),
            Gen.freq(1, this));
      } else {
        return Gen.oneOf(setGen);
      }
    }

    private Gen<Coregex> unionGen(Gen<Coregex> coregexGen) {
      return (random, status) -> {
        Coregex first = coregexGen.generate(random, status);
        Coregex[] rest = new Coregex[random.nextInt(0, 10)];
        for (int i = 0; i < rest.length; i++) {
          rest[i] = coregexGen.generate(random, status);
        }
        return new Coregex.Union(first, rest);
      };
    }

    public void configure(InRange range) {
      this.range = range;
    }
  }
}
