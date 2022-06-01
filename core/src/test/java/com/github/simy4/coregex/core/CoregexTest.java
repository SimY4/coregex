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
  public static class ConcatTest {
    @Property
    public void generatedShouldBeInConcat(
        @From(CoregexGenerator.class) Coregex first,
        List<@From(CoregexGenerator.class) Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex concat = Coregex.concat(first, rest.toArray(new Coregex[0]));
      assertTrue(
          "0 <= " + concat.minLength() + " <= " + concat.maxLength(),
          0 <= concat.minLength() && concat.minLength() <= concat.maxLength());
      int length = (concat.maxLength() + concat.minLength()) / 2;
      String generated = concat.generate(rng, length);
      assertTrue(
          concat + ".minLength(" + concat.minLength() + ") <= " + generated + ".length(" + generated.length() + ") <= " + length,
          concat.minLength() <= generated.length() && generated.length() <= length);
      Coregex chunk = first;
      int i = 0;
      do {
        String chunkGenerated = chunk.generate(rng, length);
      } while (i < rest.size() && (chunk = rest.get(i++)) != null);

//      assertTrue(generated + ".startsWith(" + chunkGenerated + ")", generated.startsWith(chunkGenerated));
//      generated = generated.substring(chunkGenerated.length());
//      for (Coregex coregex : rest) {
//        chunkGenerated = coregex.generate(rng, length);
//        assertTrue(generated + ".contains(" + chunkGenerated + ")", generated.contains(chunkGenerated));
//        generated = generated.substring(chunkGenerated.length());
//      }
//      assertTrue(generated + ".isEmpty()", generated.isEmpty());
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Concat.class) Coregex concat,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      Coregex quantified = concat.quantify(start, end);
      assertTrue(
          "0 <= " + quantified.minLength() + " <= " + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      int length = (quantified.maxLength() + quantified.minLength()) / 2;
      String generated = quantified.generate(rng, length);
      assertTrue(
          quantified + ".minLength(" + quantified.minLength() + ") <= " + generated + ".length(" + generated.length() + ") <= " + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class EmptyTest {
    @Property
    public void generatedShouldBeEmpty(
        @From(CoregexGenerator.Empty.class) Coregex empty,
        @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 0) int length) {
      assertTrue("empty", empty.generate(rng, length).isEmpty());
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Empty.class) Coregex empty,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 0) int length) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      Coregex quantified = empty.quantify(start, end);
      assertTrue(
          "0 == " + quantified.minLength() + " == " + quantified.maxLength(),
          0 == quantified.minLength() && quantified.minLength() == quantified.maxLength());
      assertEquals("", quantified.generate(rng, length));
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class SetTest {
    @Property
    public void generatedShouldBeInSet(
        @From(SetItemGenerator.class) SetItem setItem, @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 1) int length) {
      final Coregex set = Coregex.set(setItem);
      String generated = set.generate(rng, length);
      assertTrue(generated + " all match " + set, generated.chars().allMatch(setItem));
      assertTrue(
          generated + ".length(" + generated.length() + ") <= " + length,
          generated.length() <= length);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Set.class) Coregex set,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 1) int length) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      Coregex quantified = set.quantify(start, end);
      assertTrue(
          "0 <= quantified.minLength(" + quantified.minLength() + ") <= " + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      assertTrue(
          start + " == quantified.minLength(" + quantified.minLength() + ") <= " + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      String generated = quantified.generate(rng, length);
      assertTrue(
          quantified + ".minLength(" + quantified.minLength() + ") <= " + generated + ".length(" + generated.length() + ") <= " + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class UnionTest {
    @Property
    public void generatedShouldBeInUnion(
        @From(CoregexGenerator.class) Coregex first,
        List<@From(CoregexGenerator.class) Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex union = Coregex.union(first, rest.toArray(new Coregex[0]));
      assertTrue(
          "0 <= " + union.minLength() + " <= " + union.maxLength(),
          0 <= union.minLength() && union.minLength() <= union.maxLength());
      int length = (union.maxLength() + union.minLength()) / 2;
      String generated = union.generate(rng, length);
      assertTrue(
          union + ".minLength(" + union.minLength() + ") <= " + generated + ".length(" + generated.length() + ") <= " + length,
          union.minLength() <= generated.length() && generated.length() <= length);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Union.class) Coregex union,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      Coregex quantified = union.quantify(start, end);
      assertTrue(
          "0 <= " + quantified.minLength() + " <= " + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      int length = (quantified.maxLength() + quantified.minLength()) / 2;
      String generated = quantified.generate(rng, length);
      assertTrue(
          quantified + ".minLength(" + quantified.minLength() + ") <= " + generated + ".length(" + generated.length() + ") <= " + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }
}
