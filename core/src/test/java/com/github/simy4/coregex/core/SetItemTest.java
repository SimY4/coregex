package com.github.simy4.coregex.core;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class SetItemTest {
  @RunWith(JUnitQuickcheck.class)
  public static class Range {
    @Property
    public void generatedShouldBeInRange(char ch1, char ch2, long seed) {
      char start = (char) Math.min(ch1, ch2);
      char end = (char) Math.max(ch1, ch2);
      SetItem range = SetItem.range(start, end);
      char generated = range.generate(seed);
      assertTrue(start <= generated && generated <= end);
    }

    @Property
    public void sameSeedSameResult(@From(Gen.class) SetItem range, long seed) {
      char generated1 = range.generate(seed);
      char generated2 = range.generate(seed);
      assertEquals(generated1, generated2);
    }

    @Property
    public void acceptOnlyCharactersWithinRange(char ch1, char ch2, char check) {
      char start = (char) Math.min(ch1, ch2);
      char end = (char) Math.max(ch1, ch2);
      SetItem range = SetItem.range(start, end);
      assertEquals(start <= check && check <= end, range.test(check));
    }

    @Property
    public void negation(@From(Gen.class) SetItem range, char check) {
      assertNotEquals(range.negate().test(check), range.test(check));
      assertEquals(range.negate().negate().test(check), range.test(check));
    }

    public static class Gen extends Generator<SetItem> {
      public Gen() {
        super(SetItem.class);
      }

      @Override
      public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
        Generator<Character> charGen = gen().type(char.class);
        char ch1 = charGen.generate(random, status);
        char ch2 = charGen.generate(random, status);
        char start = (char) Math.min(ch1, ch2);
        char end = (char) Math.max(ch1, ch2);
        return SetItem.range(start, end);
      }
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Set {
    @Property
    public void generatedShouldBeInSet(char first, String rest, long seed) {
      SetItem set = SetItem.set(first, rest.toCharArray());
      char generated = set.generate(seed);
      assertTrue(first == generated || IntStream.range(0, rest.length()).anyMatch(i -> rest.charAt(i) == generated));
    }

    @Property
    public void sameSeedSameResult(@From(Gen.class) SetItem set, long seed) {
      char generated1 = set.generate(seed);
      char generated2 = set.generate(seed);
      assertEquals(generated1, generated2);
    }

    @Property
    public void acceptOnlyCharactersInSet(char first, String rest, char check) {
      SetItem set = SetItem.set(first, rest.toCharArray());
      assertEquals(first == check || rest.contains(String.valueOf(check)), set.test(check));
    }

    @Property
    public void negation(@From(Gen.class) SetItem set, char check) {
      assertNotEquals(set.negate().test(check), set.test(check));
      assertEquals(set.negate().negate().test(check), set.test(check));
    }

    public static class Gen extends Generator<SetItem> {
      public Gen() {
        super(SetItem.class);
      }

      @Override
      public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
        Generator<Character> charGen = gen().type(char.class);
        Generator<String> stringGen = gen().type(String.class);
        char first = charGen.generate(random, status);
        String rest = stringGen.generate(random, status);
        return SetItem.set(first, rest.toCharArray());
      }
    }
  }

  @RunWith(JUnitQuickcheck.class)
  public static class Union {
    @Property
    public void generatedShouldBeInUnion(@From(Gen.class) SetItem first, List<@From(Gen.class) SetItem> rest, long seed) {
      SetItem union = SetItem.union(first, rest.toArray(new SetItem[0]));
      char generated = union.generate(seed);
      assertTrue(first.generate(seed) == generated || rest.stream().anyMatch(si -> si.generate(seed) == generated));
    }

    @Property
    public void sameSeedSameResult(@From(Gen.class) SetItem first, List<@From(Gen.class) SetItem> rest, long seed) {
      SetItem union = SetItem.union(first, rest.toArray(new SetItem[0]));
      char generated1 = union.generate(seed);
      char generated2 = union.generate(seed);
      assertEquals(generated1, generated2);
    }

    @Property
    public void acceptOnlyCharactersInUnion(@From(Gen.class) SetItem first, List<@From(Gen.class) SetItem> rest, char check) {
      SetItem union = SetItem.union(first, rest.toArray(new SetItem[0]));
      assertEquals(first.test(check) || rest.stream().anyMatch(setItem -> setItem.test(check)), union.test(check));
    }

    @Property
    public void negation(@From(Gen.class) SetItem first, List<@From(Gen.class) SetItem> rest, char check) {
      SetItem union = SetItem.union(first, rest.toArray(new SetItem[0]));
      assertNotEquals(union.negate().test(check), union.test(check));
      assertEquals(union.negate().negate().test(check), union.test(check));
    }

    public static class Gen extends Generator<SetItem> {
      public Gen() {
        super(SetItem.class);
      }

      @Override
      public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
        Range.Gen rangeGen = gen().make(Range.Gen.class);
        Set.Gen setGen = gen().make(Set.Gen.class);
        return setItemGen(rangeGen, setGen).generate(random, status);
      }

      private com.pholser.junit.quickcheck.generator.Gen<SetItem> setItemGen(Range.Gen rangeGen, Set.Gen setGen) {
        return com.pholser.junit.quickcheck.generator.Gen.frequency(
            com.pholser.junit.quickcheck.generator.Gen.freq(5, rangeGen),
            com.pholser.junit.quickcheck.generator.Gen.freq(5, setGen),
            com.pholser.junit.quickcheck.generator.Gen.freq(1, (random, status) ->
                setItemGen(rangeGen, setGen).generate(random, status))
        );
      }
    }
  }
}