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

package com.github.simy4.coregex.vavr;

import io.vavr.collection.Array;
import io.vavr.test.Arbitrary;
import io.vavr.test.Property;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoregexArbitraryTest {
  @Test
  void shouldGenerateMatchingUUIDString() {
    Property.def("should generate matching UUID string")
        .forAll(
            CoregexArbitrary.of(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"))
        .suchThat(uuid -> uuid.equals(UUID.fromString(uuid).toString()))
        .check();
  }

  @Test
  void shouldGenerateMatchingIPv4String() {
    Property.def("should generate matching IPv4 string")
        .forAll(
            CoregexArbitrary.of(
                "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"))
        .suchThat(
            ipv4 -> {
              String[] expected = ipv4.split("\\.");
              String[] actual = InetAddress.getByName(ipv4).getHostAddress().split("\\.");
              return expected.length == actual.length
                  && Array.of(expected)
                      .zip(Array.of(actual))
                      .forAll(
                          pair ->
                              pair.apply((ex, ac) -> Integer.parseInt(ex) == Integer.parseInt(ac)));
            })
        .check();
  }

  @Test
  void shouldGenerateMatchingIsoDateString() {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    Property.def("should generate matching ISO date string")
        .forAll(
            CoregexArbitrary.of(
                "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z"))
        .suchThat(iso8601Date -> iso8601Date.equals(formatter.format(formatter.parse(iso8601Date))))
        .check();
  }

  @Test
  void shouldGenerateUniqueStrings() {
    Property.def("should generate unique strings")
        .forAll(Arbitrary.list(CoregexArbitrary.of("[a-zA-Z0-9]{32,}")))
        .suchThat(
            strings ->
                strings.forAll(
                    s -> s.length() >= 32 && s.chars().allMatch(Character::isLetterOrDigit)))
        .check();
  }
}
