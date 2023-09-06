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

package com.github.simy4.coregex.kotest;

import static io.kotest.property.PropertyTest1Kt.forAll;

import io.kotest.property.Arb;
import io.kotest.property.arbitrary.CollectionsKt;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.sequences.SequencesKt;
import kotlinx.coroutines.BuildersKt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoregexArbitraryTest extends Assertions {
  @Test
  void shouldGenerateMatchingUUIDString() throws InterruptedException {
    BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, c1) ->
            forAll(
                CoregexArbitrary.of(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"),
                (context, uuid, c2) -> uuid.equals(UUID.fromString(uuid).toString()),
                c1));
  }

  @Test
  void shouldGenerateMatchingIPv4String() throws InterruptedException {
    BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, c1) ->
            forAll(
                CoregexArbitrary.of(
                    "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"),
                (context, ipv4, c2) -> {
                  var expected = ipv4.split("\\.");
                  var actual =
                      assertDoesNotThrow(() -> InetAddress.getByName(ipv4))
                          .getHostAddress()
                          .split("\\.");
                  return expected.length == actual.length
                      && SequencesKt.all(
                          SequencesKt.zip(
                              SequencesKt.sequenceOf(expected), SequencesKt.sequenceOf(actual)),
                          pair ->
                              Integer.parseInt(pair.getFirst())
                                  == Integer.parseInt(pair.getSecond()));
                },
                c1));
  }

  @Test
  void shouldGenerateMatchingIsoDateString() throws InterruptedException {
    var formatter = DateTimeFormatter.ISO_INSTANT;
    BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, c1) ->
            forAll(
                CoregexArbitrary.of(
                    "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z"),
                (context, iso8601Date, c2) ->
                    iso8601Date.equals(formatter.format(formatter.parse(iso8601Date))),
                c1));
  }

  @Test
  void shouldGenerateUniqueStrings() throws InterruptedException {
    BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, c1) ->
            forAll(
                CollectionsKt.list(Arb.Companion, CoregexArbitrary.of("[a-zA-Z0-9]{32,}")),
                (context, strings, c2) ->
                    strings.stream()
                        .allMatch(
                            s ->
                                s.length() >= 32 && s.chars().allMatch(Character::isLetterOrDigit)),
                c1));
  }
}
