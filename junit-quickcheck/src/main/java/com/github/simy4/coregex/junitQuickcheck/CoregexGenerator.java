package com.github.simy4.coregex.junitQuickcheck;

import com.github.simy4.coregex.core.Coregex;
import com.github.simy4.coregex.core.CoregexParser;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.regex.Pattern;

public class CoregexGenerator extends Generator<String> {
  private Coregex coregex;

  public CoregexGenerator() {
    super(String.class);
  }

  public CoregexGenerator(Pattern regex) {
    this();
    this.coregex = CoregexParser.getInstance().parse(regex);
  }

  public void configure(Regex regex) {
    this.coregex = CoregexParser.getInstance().parse(Pattern.compile(regex.value(), regex.flags()));
  }

  @Override
  public String generate(SourceOfRandomness random, GenerationStatus status) {
    return coregex.generate(new SourceOfRandomnessRNG(random));
  }
}
