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

import static io.kotest.property.PropertyTest1Kt.checkAll;
import static io.kotest.property.arbitrary.MapKt.map;
import static kotlinx.coroutines.BuildersKt.runBlocking;

import io.kotest.property.Arb;
import io.kotest.property.RTree;
import io.kotest.property.Sample;
import io.kotest.property.arbitrary.CollectionsKt;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.regex.Pattern;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KotestTest extends Assertions {
  @Test
  void shrinkingTest() throws InterruptedException {
    runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (__, c1) ->
            checkAll(
                regexes(),
                (context, regex, c2) -> {
                  Arb<String> coregexArbitrary = new CoregexArbitrary(regex);
                  Sample<String> sample = coregexArbitrary.sample(context.randomSource());
                  Queue<RTree<String>> shrinks = Collections.asLifoQueue(new ArrayDeque<>());
                  shrinks.add(sample.getShrinks());
                  while (!shrinks.isEmpty()) {
                    RTree<String> shrink = shrinks.remove();
                    String shrinkValue = shrink.getValue().invoke();
                    assertTrue(regex.matcher(shrinkValue).matches());
                    shrinks.addAll(shrink.getChildren().getValue());
                  }
                  return Unit.INSTANCE;
                },
                c1));
  }

  Arb<Pattern> regexes() {
    return map(
        CollectionsKt.of(
            Arb.Companion,
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
            "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
            "((?i)[a-z]+(?-i)-[A-Z]){3,6}"),
        Pattern::compile);
  }
}
