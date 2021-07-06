package com.github.simy4.coregex.core.generators;

import com.github.simy4.coregex.core.Coregex;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.Optional;

public class CoregexGenerator extends Generator<Coregex> {
  private static final GenerationStatus.Key<Integer> DEPTH = new GenerationStatus.Key<>("depth", Integer.class);

  public CoregexGenerator() {
    super(Coregex.class);
  }

  @Override
  public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
    return gen().oneOf(
        gen().make(Concat.class),
        gen().make(Empty.class),
        gen().make(Set.class),
        gen().make(Union.class)
    )
        .flatMap(coregex -> Gen.frequency(
            Gen.freq(3, Gen.pure(coregex)),
            Gen.freq(1, (rng, sts) -> {
              int i1 = rng.nextInt(0, 20);
              int i2 = rng.nextInt(0, 20);
              return coregex.quantify(Math.min(i1, i2), Math.max(i1, i2));
            })
        ))
        .generate(random, status);
  }

  public static class Concat extends Generator<Coregex> {
    public Concat() {
      super(Coregex.class);
    }

    @Override
    public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
      Empty emptyGen = gen().make(Empty.class);
      Set setGen = gen().make(Set.class);
      Union unionGen = gen().make(Union.class);

      int depth;
      Optional<Integer> maybeDepth = status.valueOf(DEPTH);
      if (maybeDepth.isPresent()) {
        depth = maybeDepth.get();
        status.setValue(DEPTH, depth - 1);
      } else {
        depth = random.nextInt(0, 2);
        status.setValue(DEPTH, depth);
      }
      return concatGen(coregexGen(emptyGen, setGen, unionGen, depth)).generate(random, status);
    }

    private Gen<Coregex> coregexGen(Empty emptyGen, Set setGen, Union unionGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(1, this),
            Gen.freq(1, emptyGen),
            Gen.freq(3, setGen),
            Gen.freq(1, unionGen)
        );
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
        return Coregex.union(first, rest);
      };
    }
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
      Concat concatGen = gen().make(Concat.class);
      Empty emptyGen = gen().make(Empty.class);
      Set setGen = gen().make(Set.class);

      int depth;
      Optional<Integer> maybeDepth = status.valueOf(DEPTH);
      if (maybeDepth.isPresent()) {
        depth = maybeDepth.get();
        status.setValue(DEPTH, depth - 1);
      } else {
        depth = random.nextInt(0, 2);
        status.setValue(DEPTH, depth);
      }
      return unionGen(coregexGen(concatGen, emptyGen, setGen, depth)).generate(random, status);
    }

    private Gen<Coregex> coregexGen(Concat concatGen, Empty emptyGen, Set setGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(1, concatGen),
            Gen.freq(1, emptyGen),
            Gen.freq(3, setGen),
            Gen.freq(1, this)
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
