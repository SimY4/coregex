package com.github.simy4.coregex.core;

import com.github.simy4.coregex.core.generators.CoregexGenerator;
import com.github.simy4.coregex.core.generators.RNGGenerator;
import com.github.simy4.coregex.core.generators.SetItemGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class CoregexTest {
  @RunWith(JUnitQuickcheck.class)
  public static class Concat {
    @Property
    public void generatedShouldBeInConcat(
        @From(CoregexGenerator.class) Coregex first,
        List<@From(CoregexGenerator.class) Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex concat = Coregex.concat(first, rest.toArray(new Coregex[0]));
      String generated = concat.generate(rng);
      assertTrue(generated + " pass coregex predicate", concat.test(generated));
    }

    @Property
    public void sameSeedSameResult(
        @From(CoregexGenerator.Concat.class) Coregex concat, @From(RNGGenerator.class) RNG rng) {
      String generated1 = concat.generate(rng);
      String generated2 = concat.generate(rng);
      assertEquals(generated1, generated2);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Concat.class) Coregex concat,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      String quantified = concat.quantify(start, end).generate(rng);
      assertTrue(
          concat.min() * start
              + " <= \""
              + quantified
              + "\".length("
              + quantified.length()
              + ") <= "
              + concat.max() * end,
          concat.min() * start <= quantified.length() && quantified.length() <= concat.max() * end);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Empty {
    @Property
    public void generatedShouldBeEmpty(
        @From(CoregexGenerator.Empty.class) Coregex empty, @From(RNGGenerator.class) RNG rng) {
      assertTrue("empty", empty.generate(rng).isEmpty());
    }

    @Property
    public void sameSeedSameResult(
        @From(CoregexGenerator.Empty.class) Coregex concat, @From(RNGGenerator.class) RNG rng) {
      String generated1 = concat.generate(rng);
      String generated2 = concat.generate(rng);
      assertEquals(generated1, generated2);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Empty.class) Coregex empty,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      assertEquals("", empty.quantify(start, end).generate(rng));
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Set {
    @Property
    public void generatedShouldBeInSet(
        @From(SetItemGenerator.class) SetItem setItem, @From(RNGGenerator.class) RNG rng) {
      final Coregex set = Coregex.set(setItem);
      String generated = set.generate(rng);
      assertTrue(generated + " pass coregex predicate", set.test(generated));
      assertTrue(generated + " all match " + set, generated.chars().allMatch(setItem));
    }

    @Property
    public void sameSeedSameResult(
        @From(CoregexGenerator.Set.class) Coregex concat, @From(RNGGenerator.class) RNG rng) {
      String generated1 = concat.generate(rng);
      String generated2 = concat.generate(rng);
      assertEquals(generated1, generated2);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Set.class) Coregex set,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      String quantified = set.quantify(start, end).generate(rng);
      assertTrue(
          start + " <= \"" + quantified + "\".length(" + quantified.length() + ") <= " + end,
          start <= quantified.length() && quantified.length() <= end);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Union {
    @Property
    public void generatedShouldBeInUnion(
        @From(CoregexGenerator.class) Coregex first,
        List<@From(CoregexGenerator.class) Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex union = Coregex.union(first, rest.toArray(new Coregex[0]));
      String generated = union.generate(rng);
      assertTrue(generated + " pass coregex predicate", union.test(generated));
      assertTrue(
          generated + " in " + union,
          first.test(generated) || rest.stream().anyMatch(coregex -> coregex.test(generated)));
    }

    @Property
    public void sameSeedSameResult(
        @From(CoregexGenerator.Union.class) Coregex concat, @From(RNGGenerator.class) RNG rng) {
      String generated1 = concat.generate(rng);
      String generated2 = concat.generate(rng);
      assertEquals(generated1, generated2);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Union.class) Coregex union,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      String quantified = union.quantify(start, end).generate(rng);
      assertTrue(
          union.min() * start
              + " <= \""
              + quantified
              + "\".length("
              + quantified.length()
              + ") <= "
              + union.max() * end,
          union.min() * start <= quantified.length() && quantified.length() <= union.max() * end);
    }
  }
}
