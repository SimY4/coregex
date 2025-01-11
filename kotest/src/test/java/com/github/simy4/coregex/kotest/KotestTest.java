/*
 * Copyright 2021-2025 Alex Simkin
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

package com.github.simy4.coregex.kotest;

import io.kotest.property.Arb;
import io.kotest.property.RTree;
import io.kotest.property.RandomSource;
import io.kotest.property.arbitrary.CollectionsKt;
import io.kotest.property.arbitrary.MapKt;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.sequences.SequencesKt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KotestTest extends Assertions {
  @Test
  void shrinkingTest() {
    RandomSource randomSource =
        RandomSource.Companion.seeded(ThreadLocalRandom.current().nextLong());
    SequencesKt.forEach(
        SequencesKt.take(regexes().samples(randomSource), 100),
        samplePair -> {
          Pattern regex = samplePair.getValue().component1();
          String value = samplePair.getValue().getSecond();
          RTree<Pair<Pattern, String>> shrinks = samplePair.getShrinks();
          String shrink = shrinks.getValue().invoke().component2();
          assertTrue(shrink.length() <= value.length());
          assertTrue(regex.matcher(shrink).matches());
          Queue<List<RTree<Pair<Pattern, String>>>> childrenStack =
              Collections.asLifoQueue(new ArrayDeque<>());
          childrenStack.add(shrinks.getChildren().getValue());
          while (!childrenStack.isEmpty()) {
            for (RTree<Pair<Pattern, String>> child : childrenStack.remove()) {
              shrink = child.getValue().invoke().component2();
              assertTrue(shrink.length() <= value.length());
              assertTrue(regex.matcher(shrink).matches());
              childrenStack.add(child.getChildren().getValue());
            }
          }
          return Unit.INSTANCE;
        });
  }

  Arb<Pair<Pattern, String>> regexes() {
    return MapKt.flatMap(
        CollectionsKt.of(
            Arb.Companion,
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
            "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
            "((?i)[a-z]+(?-i)-[A-Z]){3,6}"),
        regexStr -> {
          Pattern regex = Pattern.compile(regexStr);
          return MapKt.map(
              new CoregexArbitrary(regex).withSize(1000), coregex -> new Pair<>(regex, coregex));
        });
  }
}
