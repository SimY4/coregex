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

package com.github.simy4.coregex.jetCheck;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jetbrains.jetCheck.Generator;
import org.jetbrains.jetCheck.PropertyChecker;
import org.junit.jupiter.api.Test;

class CoregexGeneratorTest {
  @Test
  void shouldGenerateMatchingUUIDString() {
    PropertyChecker.forAll(
        CoregexGenerator.of(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"),
        uuid -> uuid.equals(UUID.fromString(uuid).toString()));
  }

  @Test
  void shouldGenerateMatchingIPv4String() {
    PropertyChecker.forAll(
        CoregexGenerator.of(
            "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"),
        ipv4 -> {
          try {
            String[] expected = ipv4.split("\\.");
            String[] actual = InetAddress.getByName(ipv4).getHostAddress().split("\\.");
            return expected.length == actual.length
                && IntStream.range(0, expected.length)
                    .allMatch(i -> Integer.parseInt(expected[i]) == Integer.parseInt(actual[i]));
          } catch (UnknownHostException ex) {
            throw new UncheckedIOException(ex);
          }
        });
  }

  @Test
  void shouldGenerateMatchingIsoDateString() {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    PropertyChecker.forAll(
        CoregexGenerator.of(
            "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z"),
        iso8601Date -> iso8601Date.equals(formatter.format(formatter.parse(iso8601Date))));
  }

  @Test
  void shouldGenerateUniqueStrings() {
    PropertyChecker.forAll(
        Generator.listsOf(CoregexGenerator.of("[a-zA-Z0-9]{32,}")),
        strings ->
            strings.stream()
                .allMatch(s -> s.length() >= 32 && s.chars().allMatch(Character::isLetterOrDigit)));
  }
}
