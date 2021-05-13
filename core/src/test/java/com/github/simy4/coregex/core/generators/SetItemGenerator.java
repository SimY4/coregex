package com.github.simy4.coregex.core.generators;

import com.github.simy4.coregex.core.SetItem;
import com.pholser.junit.quickcheck.generator.Gen;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class SetItemGenerator extends Generator<SetItem> {
  public SetItemGenerator() {
    super(SetItem.class);
  }

  @Override
  public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
    return gen().oneOf(
        gen().make(Range.class),
        gen().make(Set.class),
        gen().make(Union.class)
    )
//        .flatMap(setItem -> Gen.oneOf(
//            setItem,
//            setItem.negate()
//        ))
        .generate(random, status);
  }

  public static class Range extends Generator<SetItem> {
    public Range() {
      super(SetItem.class);
    }

    @Override
    public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
      Generator<Character> charGen = gen().type(char.class);
      char ch1 = charGen.generate(random, status);
      char ch2 = charGen.generate(random, status);
      char start = (char) Math.min(ch1, ch2);
      char end = (char) Math.max(ch1, ch2);
      end = start == end ? (char) (end + 1) : end;
      return SetItem.range(start, end);
    }
  }

  public static class Set extends Generator<SetItem> {
    public Set() {
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

  public static class Union extends Generator<SetItem> {
    public Union() {
      super(SetItem.class);
    }

    @Override
    public SetItem generate(SourceOfRandomness random, GenerationStatus status) {
      Range rangeGen = gen().make(Range.class);
      Set setGen = gen().make(Set.class);

      int depth = random.nextInt(0, 2);
      return unionGen(setItemGen(rangeGen, setGen, depth)).generate(random, status);
    }

    private Gen<SetItem> setItemGen(Range rangeGen, Set setGen, int depth) {
      if (depth > 0) {
        return Gen.frequency(
            Gen.freq(3, rangeGen),
            Gen.freq(3, setGen),
            Gen.freq(1, (random, status) ->
                unionGen(setItemGen(rangeGen, setGen, depth - 1)).generate(random, status))
        );
      } else {
        return Gen.oneOf(rangeGen, setGen);
      }
    }

    private Gen<SetItem> unionGen(Gen<SetItem> setItemGen) {
      return (random, status) -> {
        SetItem first = setItemGen.generate(random, status);
        SetItem[] rest = new SetItem[random.nextInt(0, 10)];
        for (int i = 0; i < rest.length; i++) {
          rest[i] = setItemGen.generate(random, status);
        }
        return SetItem.union(first, rest);
      };
    }
  }
}
