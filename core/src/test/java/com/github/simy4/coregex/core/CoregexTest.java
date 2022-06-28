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

package com.github.simy4.coregex.core;

import com.github.simy4.coregex.core.generators.CoregexGenerator;
import com.github.simy4.coregex.core.generators.RNGGenerator;
import com.github.simy4.coregex.core.generators.SetGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class CoregexTest {
  @RunWith(JUnitQuickcheck.class)
  public static class ConcatTest {
    @Property
    public void generatedShouldBeInConcat(
        @From(CoregexGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Coregex first,
        List<@From(CoregexGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex concat = new Coregex.Concat(first, rest.toArray(new Coregex[0]));
      assertTrue(
          "0 <= " + concat.minLength() + " <= " + concat.maxLength(),
          0 <= concat.minLength() && concat.minLength() <= concat.maxLength());
      int length = (concat.maxLength() + concat.minLength()) / 2;
      String generated = concat.generate(rng, length);
      assertTrue(
          concat
              + ".minLength("
              + concat.minLength()
              + ") <= "
              + generated
              + ".length("
              + generated.length()
              + ") <= "
              + length,
          concat.minLength() <= generated.length() && generated.length() <= length);
      Coregex chunk = first;
      int i = 0;
      do {
        String chunkGenerated = chunk.generate(rng, length);
      } while (i < rest.size() && (chunk = rest.get(i++)) != null);

      //      assertTrue(generated + ".startsWith(" + chunkGenerated + ")",
      // generated.startsWith(chunkGenerated));
      //      generated = generated.substring(chunkGenerated.length());
      //      for (Coregex coregex : rest) {
      //        chunkGenerated = coregex.generate(rng, length);
      //        assertTrue(generated + ".contains(" + chunkGenerated + ")",
      // generated.contains(chunkGenerated));
      //        generated = generated.substring(chunkGenerated.length());
      //      }
      //      assertTrue(generated + ".isEmpty()", generated.isEmpty());
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Concat.class) @InRange(minChar = 'a', maxChar = 'z') Coregex concat,
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
          quantified
              + ".minLength("
              + quantified.minLength()
              + ") <= "
              + generated
              + ".length("
              + generated.length()
              + ") <= "
              + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class LiteralTest {
    @Property
    public void generatedShouldBeLiteral(String literal, @From(RNGGenerator.class) RNG rng) {
      assertEquals(literal, new Coregex.Literal(literal).generate(rng, literal.length()));
    }

    @Property
    public void quantify(
        String literal,
        @InRange(minInt = 0, maxInt = 20) int i1,
        @InRange(minInt = 0, maxInt = 20) int i2,
        @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 0) int length) {
      int start = Math.min(i1, i2);
      int end = Math.max(i1, i2);
      Coregex coregex = new Coregex.Literal(literal);
      Coregex quantified = coregex.quantify(start, end);
      assertTrue(
          "0 <= quantified.minLength(" + quantified.minLength() + ") <= " + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      assertTrue(quantified.generate(rng, length).matches("(" + Pattern.quote(literal) + ")*"));
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class SetTest {
    @Property
    public void generatedShouldBeInSet(
        @From(SetGenerator.class) @InRange(minChar = 'a', maxChar = 'z')
            com.github.simy4.coregex.core.Set charSet,
        @From(RNGGenerator.class) RNG rng,
        @InRange(minInt = 1) int length) {
      Coregex set = new Coregex.Set(charSet);
      String generated = set.generate(rng, length);
      assertTrue(
          generated + " all match " + set,
          generated.chars().allMatch(ch -> charSet.stream().anyMatch(i -> ch == i)));
      assertTrue(
          generated + ".length(" + generated.length() + ") <= " + length,
          generated.length() <= length);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Set.class) @InRange(minChar = 'a', maxChar = 'z') Coregex set,
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
          start
              + " == quantified.minLength("
              + quantified.minLength()
              + ") <= "
              + quantified.maxLength(),
          0 <= quantified.minLength() && quantified.minLength() <= quantified.maxLength());
      String generated = quantified.generate(rng, length);
      assertTrue(
          quantified
              + ".minLength("
              + quantified.minLength()
              + ") <= "
              + generated
              + ".length("
              + generated.length()
              + ") <= "
              + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class UnionTest {
    @Property
    public void generatedShouldBeInUnion(
        @From(CoregexGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Coregex first,
        List<@From(CoregexGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Coregex> rest,
        @From(RNGGenerator.class) RNG rng) {
      Coregex union = new Coregex.Union(first, rest.toArray(new Coregex[0]));
      assertTrue(
          "0 <= " + union.minLength() + " <= " + union.maxLength(),
          0 <= union.minLength() && union.minLength() <= union.maxLength());
      int length = (union.maxLength() + union.minLength()) / 2;
      String generated = union.generate(rng, length);
      assertTrue(
          union
              + ".minLength("
              + union.minLength()
              + ") <= "
              + generated
              + ".length("
              + generated.length()
              + ") <= "
              + length,
          union.minLength() <= generated.length() && generated.length() <= length);
    }

    @Property
    public void quantify(
        @From(CoregexGenerator.Union.class) @InRange(minChar = 'a', maxChar = 'z') Coregex union,
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
          quantified
              + ".minLength("
              + quantified.minLength()
              + ") <= "
              + generated
              + ".length("
              + generated.length()
              + ") <= "
              + length,
          quantified.minLength() <= generated.length() && generated.length() <= length);
    }
  }
}
