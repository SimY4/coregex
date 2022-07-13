package com.github.simy4.coregex.jqwik;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.configurators.ArbitraryConfiguratorBase;

import java.util.regex.Pattern;

public class CoregexArbitraryConfigurator extends ArbitraryConfiguratorBase {
  public Arbitrary<String> configure(Arbitrary<String> arbitrary, Regex regex) {
    return Arbitraries.fromGenerator(new CoregexGenerator(Pattern.compile(regex.value())));
  }
}
