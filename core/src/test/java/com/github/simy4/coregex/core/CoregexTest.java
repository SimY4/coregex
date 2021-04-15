package com.github.simy4.coregex.core;

import com.github.simy4.coregex.core.generators.RNGGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class CoregexTest {
  @RunWith(JUnitQuickcheck.class)
  public static class Empty {
    @Property
    public void generatedShouldBeEmpty(@From(Gen.class) Coregex empty, @From(RNGGenerator.class) RNG rng) {
      assertTrue("empty", empty.generate(rng).isEmpty());
    }

    @Property
    public void quantify(@From(Gen.class) Coregex empty,
                         @InRange(minInt = 0, maxInt = 20) int i1,
                         @InRange(minInt = 0, maxInt = 20) int i2,
                         @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      assertTrue("empty", empty.quantify(start, end).generate(rng).isEmpty());
    }

    public static class Gen extends Generator<Coregex> {
      public Gen() {
        super(Coregex.class);
      }

      @Override
      public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
        return Coregex.empty();
      }
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Set {
    @Property
    public void generatedShouldBeInSet(
        @From(SetItemTest.Range.Gen.class) @From(SetItemTest.Set.Gen.class) @From(SetItemTest.Union.Gen.class) SetItem setItem,
        @From(RNGGenerator.class) RNG rng) {
      final Coregex set = Coregex.set(setItem);
      String generated = set.generate(rng);
      assertTrue(generated + " all match " + set, generated.chars().allMatch(setItem));
    }

    @Property
    public void quantify(@From(Gen.class) Coregex set,
                         @InRange(minInt = 0, maxInt = 20) int i1,
                         @InRange(minInt = 0, maxInt = 20) int i2,
                         @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      String quantified = set.quantify(start, end).generate(rng);
      assertTrue(start + " <= \"" + quantified + "\".length(" + quantified.length() + ") <= " + end,
          start <= quantified.length() && quantified.length() <= end);
    }

    public static class Gen extends Generator<Coregex> {
      public Gen() {
        super(Coregex.class);
      }

      @Override
      public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
        return Coregex.set(gen().oneOf(
            gen().make(SetItemTest.Range.Gen.class),
            gen().make(SetItemTest.Set.Gen.class),
            gen().make(SetItemTest.Union.Gen.class)
        ).generate(random, status));
      }
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Union {
    @Property
    public void generatedShouldBeInUnion(
        @From(Empty.Gen.class) @From(Set.Gen.class) @From(Gen.class) Coregex first,
        List<@From(Empty.Gen.class) @From(Set.Gen.class) @From(Gen.class) Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex union = Coregex.union(first, rest.toArray(new Coregex[0]));
      String generated = union.generate(rng);
      assertTrue(generated + " in (" + union + ')', generated.chars().allMatch(ch -> {
        String regex = String.valueOf(ch);
        RNG nextRng = rng.genLong().getKey();
        return regex.equals(first.generate(nextRng)) || rest.stream().map(coregex -> coregex.generate(nextRng)).anyMatch(regex::equals);
      }));
    }

    @Property
    public void quantify(@From(Gen.class) Coregex set,
                         @InRange(minInt = 0, maxInt = 20) int i1,
                         @InRange(minInt = 0, maxInt = 20) int i2,
                         @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      String quantified = set.quantify(start, end).generate(rng);
      assertTrue(start + " <= \"" + quantified + "\".length(" + quantified.length() + ") <= " + end,
          start <= quantified.length() && quantified.length() <= end);
    }

    public static class Gen extends Generator<Coregex> {
      public Gen() {
        super(Coregex.class);
      }

      @Override
      public Coregex generate(SourceOfRandomness random, GenerationStatus status) {
        Empty.Gen emptyGen = gen().make(Empty.Gen.class);
        Set.Gen setGen = gen().make(Set.Gen.class);

        int depth = random.nextInt(0, 2);
        return unionGen(coregexGen(emptyGen, setGen, depth)).generate(random, status);
      }

      private com.pholser.junit.quickcheck.generator.Gen<Coregex> coregexGen(Empty.Gen emptyGen, Set.Gen setGen, int depth) {
        if (depth > 0) {
          return com.pholser.junit.quickcheck.generator.Gen.frequency(
              com.pholser.junit.quickcheck.generator.Gen.freq(3, emptyGen),
              com.pholser.junit.quickcheck.generator.Gen.freq(3, setGen),
              com.pholser.junit.quickcheck.generator.Gen.freq(1, (random, status) ->
                  unionGen(coregexGen(emptyGen, setGen, depth - 1)).generate(random, status))
          );
        } else {
          return com.pholser.junit.quickcheck.generator.Gen.oneOf(emptyGen, setGen);
        }
      }

      private com.pholser.junit.quickcheck.generator.Gen<Coregex> unionGen(com.pholser.junit.quickcheck.generator.Gen<Coregex> coregexGen) {
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

}