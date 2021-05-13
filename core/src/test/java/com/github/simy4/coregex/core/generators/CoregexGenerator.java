package com.github.simy4.coregex.core.generators;

import com.github.simy4.coregex.core.Coregex;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class CoregexGenerator extends Generator<Coregex> {
  public CoregexGenerator() {
    super(Coregex.class);
  }

  @Override
  public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
    return gen().oneOf(
        gen().make(Empty.class),
        gen().make(Set.class),
        gen().make(Union.class)
    )
        .flatMap(coregex -> Gen.oneOf(
            Gen.pure(coregex),
            (rng, sts) -> {
              int i1 = rng.nextInt(0, 20);
              int i2 = rng.nextInt(0, 20);
              return coregex.quantify(Math.min(i1, i2), Math.max(i1, i2));
            }
        ))
        .generate(random, status);
  }

  public static class Empty extends Generator<Coregex> {
    public Empty() {
      super(Coregex.class);
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      return Coregex.empty();
    }
  }

  public static class Set extends Generator<Coregex> {
    public Set() {
      super(Coregex.class);
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      return gen().make(SetItemGenerator.class).map(Coregex::set).generate(random, status);
    }
  }

  public static class Union extends Generator<Coregex> {
    public Union() {
      super(Coregex.class);
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      Set setGen = gen().make(Set.class);

      int depth = random.nextInt(0, 2);
      return unionGen(coregexGen(setGen, depth)).generate(random, status);
    }

    private Gen<Coregex> coregexGen(Set setGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(3, setGen),
            Gen.freq(1, (random, status) ->
                unionGen(coregexGen(setGen, depth - 1)).generate(random, status))
        );
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
        return Coregex.union(first, rest);
      };
    }
  }
}
