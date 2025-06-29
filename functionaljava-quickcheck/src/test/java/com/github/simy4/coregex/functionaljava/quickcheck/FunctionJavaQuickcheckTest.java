package com.github.simy4.coregex.functionaljava.quickcheck;

import static fj.test.Property.prop;
import static fj.test.Property.property;

import fj.data.List;
import fj.test.Arbitrary;
import fj.test.Gen;
import fj.test.Property;
import fj.test.Rand;
import fj.test.runner.PropertyTestRunner;
import java.util.regex.Pattern;
import org.junit.runner.RunWith;

@RunWith(PropertyTestRunner.class)
public class FunctionJavaQuickcheckTest {

  public Property shrinkingTest() {
    return property(
        arbitraryRegex(),
        Arbitrary.arbInteger,
        Arbitrary.arbLong,
        (regex, size, seed) -> {
          String sample = CoregexArbitrary.gen(regex).gen(size, Rand.standard.reseed(seed));
          return CoregexArbitrary.shrink(regex)
              .shrink(sample)
              .map(shrink -> prop(regex.matcher(shrink).matches()))
              .foldLeft(Property::and, prop(true));
        });
  }

  Gen<Pattern> arbitraryRegex() {
    return Gen.pickOne(
        List.list(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
                "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
                "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
                "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
                "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
                "((?i)[a-z]+(?-i)-[A-Z]){3,6}")
            .map(Pattern::compile));
  }
}
