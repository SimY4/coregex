package com.github.simy4.coregex.jqwik;

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.CoregexParser;
import com.github.simy4.coregex.core.RNG;
import com.github.simy4.coregex.core.rng.RandomRNG;
import net.jqwik.api.RandomGenerator;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.ShrinkingDistance;

import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CoregexGenerator implements RandomGenerator<String> {
  private final Coregex coregex;
  private final int size;

  public CoregexGenerator(Pattern regex) {
    this(regex, Integer.MAX_VALUE);
  }

  public CoregexGenerator(Pattern regex, int size) {
    this.coregex = CoregexParser.getInstance().parse(regex);
    this.size = size;
  }

  @Override
  public Shrinkable<String> next(Random random) {
    return new ShrinkableString(coregex, coregex.sized(size).generate(new RandomRNG(random.nextLong())));
  }
}

final class ShrinkableString implements Shrinkable<String> {
  private final Coregex coregex;
  private final String value;

  ShrinkableString(Coregex coregex, String value) {
    this.coregex = coregex;
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public Stream<Shrinkable<String>> shrink() {
    Stream.Builder<Shrinkable<String>> shrinks = Stream.builder();
    RNG rng = new RandomRNG();
    for (int remainder = coregex.minLength();
         remainder < value.length();
         remainder = (remainder * 2) + 1) {
      shrinks.add(new ShrinkableString(coregex, coregex.sized(remainder).generate(rng)));
    }
    return shrinks.build();
  }

  @Override
  public ShrinkingDistance distance() {
    return ShrinkingDistance.of(value.length() - coregex.minLength());
  }
}
