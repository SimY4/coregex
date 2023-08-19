/*
 * Copyright 2021-2023 Alex Simkin
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

package com.github.simy4.coregex.jqwik;

import java.util.Random;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.testing.ShrinkingSupport;
import net.jqwik.testing.TestingSupport;
import org.junit.jupiter.api.Assertions;

class JqwikTest extends Assertions {
  @Property
  void arbitraryTest(@ForAll("regex") Pattern regex, @ForAll Random random) {
    CoregexArbitrary coregex = new CoregexArbitrary(regex);
    TestingSupport.assertAllGenerated(
        coregex, random, generated -> assertTrue(regex.matcher(generated).matches()));
  }

  @Property
  void shrinkingTest(@ForAll("regex") Pattern regex, @ForAll Random random) {
    CoregexArbitrary coregex = new CoregexArbitrary(regex);
    String shrink = ShrinkingSupport.falsifyThenShrink(coregex, random);
    assertTrue(regex.matcher(shrink).matches());
  }

  @Provide("regex")
  Arbitrary<Pattern> arbitraryRegex() {
    return Arbitraries.of(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
            "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
            "((?i)[a-z]+(?-i)-[A-Z]){3,6}")
        .map(Pattern::compile);
  }
}
