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

import io.kotest.property.Arb;
import io.kotest.property.RandomSource;
import io.kotest.property.arbitrary.CollectionsKt;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import kotlin.Unit;
import kotlin.sequences.SequencesKt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoregexArbitraryTest extends Assertions {
  @Test
  void shouldGenerateMatchingUUIDString() {
    var randomSource = RandomSource.Companion.seeded(ThreadLocalRandom.current().nextLong());
    assertTrue(
        SequencesKt.all(
            SequencesKt.take(
                CoregexArbitrary.of(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")
                    .samples(randomSource),
                100),
            uuid -> uuid.getValue().equals(UUID.fromString(uuid.getValue()).toString())));
  }

  @Test
  void shouldGenerateMatchingIPv4String() {
    var randomSource = RandomSource.Companion.seeded(ThreadLocalRandom.current().nextLong());
    SequencesKt.forEach(
        SequencesKt.take(
            CoregexArbitrary.of(
                    "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])")
                .samples(randomSource),
            100),
        ipv4 -> {
          String[] expected = ipv4.getValue().split("\\.");
          String[] actual =
              assertDoesNotThrow(() -> InetAddress.getByName(ipv4.getValue()))
                  .getHostAddress()
                  .split("\\.");
          assertEquals(expected.length, actual.length);
          for (int i = 0; i < expected.length; i++) {
            assertEquals(Integer.parseInt(expected[i]), Integer.parseInt(actual[i]));
          }
          return Unit.INSTANCE;
        });
  }

  @Test
  void shouldGenerateMatchingIsoDateString() {
    var formatter = DateTimeFormatter.ISO_INSTANT;
    var randomSource = RandomSource.Companion.seeded(ThreadLocalRandom.current().nextLong());
    assertTrue(
        SequencesKt.all(
            SequencesKt.take(
                CoregexArbitrary.of(
                        "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z")
                    .samples(randomSource),
                100),
            iso8601Date ->
                iso8601Date
                    .getValue()
                    .equals(formatter.format(formatter.parse(iso8601Date.getValue())))));
  }

  @Test
  void shouldGenerateUniqueStrings() {
    var randomSource = RandomSource.Companion.seeded(ThreadLocalRandom.current().nextLong());
    assertTrue(
        SequencesKt.all(
            SequencesKt.take(
                CollectionsKt.list(Arb.Companion, CoregexArbitrary.of("[a-zA-Z0-9]{32,}"))
                    .samples(randomSource),
                100),
            strings ->
                strings.getValue().stream()
                    .allMatch(
                        s -> s.length() >= 32 && s.chars().allMatch(Character::isLetterOrDigit))));
  }
}
