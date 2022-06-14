package com.github.simy4.coregex.core;

import com.github.simy4.coregex.core.generators.SetGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class SetTest {
  @Property
  public void generatedShouldBeInRange(char ch1, char ch2, long seed) {
    char start = (char) Math.min(ch1, ch2);
    char end = (char) Math.max(ch1, ch2);
    Set range = Set.builder().range(start, end).build();
    char generated = range.generate(seed);
    assertTrue(start + " <= " + generated + " <= " + end, start <= generated && generated <= end);
  }

  @Property
  public void generatedShouldBeInSet(char first, String rest, long seed) {
    Set set = Set.builder().set(first, rest.toCharArray()).build();
    char generated = set.generate(seed);
    assertTrue(
        generated + " in [" + set + ']',
        first == generated
            || IntStream.range(0, rest.length()).anyMatch(i -> rest.charAt(i) == generated));
  }

  @Property
  public void generatedShouldBeInUnion(
      @From(SetGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Set first,
      List<@From(SetGenerator.class) @InRange(minChar = 'a', maxChar = 'z') Set> rest,
      long seed) {
    Set union =
        rest.stream()
            .reduce(
                Set.builder().set(first),
                Set.Builder::set,
                (l, r) -> Set.builder().set(l.build()).set(r.build()))
            .build();
    char generated = union.generate(seed);
    assertTrue(
        generated + " in [" + union + ']',
        IntStream.concat(first.stream(), rest.stream().flatMapToInt(Set::stream))
            .anyMatch(ch -> ch == generated));
  }

  @Property
  public void sameSeedSameResult(@From(SetGenerator.class) Set range, long seed) {
    char generated1 = range.generate(seed);
    char generated2 = range.generate(seed);
    assertEquals(generated1, generated2);
  }

  @Property
  public void negation(@From(SetGenerator.class) Set set, long seed) {
    assertEquals(
        Set.builder().set(set).negate().negate().build().generate(seed), set.generate(seed));
  }
}
