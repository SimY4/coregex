/*
 * Copyright 2021-2026 Alex Simkin
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

package com.github.simy4.coregex.junit.quickcheck;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Only;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.internal.generator.SimpleGenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class JunitQuickcheckTest extends Assert {
  @Property
  public void generatorTest(
      long seed,
      @Only({
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
            "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
            "((?i)[a-z]+(?-i)-[A-Z]){3,6}"
          })
          String regexStr) {
    Pattern regex = Pattern.compile(regexStr);
    SourceOfRandomness random = new SourceOfRandomness(new Random(seed));
    GenerationStatus status = new SimpleGenerationStatus(new GeometricDistribution(), random, 1000);
    Generator<String> coregexGenerator = new CoregexGenerator(regex);

    assertTrue(
        Stream.generate(() -> coregexGenerator.generate(random, status))
            .limit(100L)
            .allMatch(generated -> regex.matcher(generated).matches()));
  }

  @Property
  public void shrinkingTest(
      long seed,
      @Only({
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
            "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
            "((?i)[a-z]+(?-i)-[A-Z]){3,6}"
          })
          String regexStr) {
    Pattern regex = Pattern.compile(regexStr);
    SourceOfRandomness random = new SourceOfRandomness(new Random(seed));
    GenerationStatus status = new SimpleGenerationStatus(new GeometricDistribution(), random, 1000);
    Generator<String> coregexGenerator = new CoregexGenerator(regex);

    String generated = coregexGenerator.generate(random, status);

    coregexGenerator
        .shrink(random, generated)
        .forEach(shrinked -> assertTrue(regex.matcher(shrinked).matches()));
  }
}
