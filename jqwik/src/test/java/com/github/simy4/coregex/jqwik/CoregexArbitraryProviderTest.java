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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class CoregexArbitraryProviderTest {
  @Property
  void shouldGenerateMatchingUUIDString(
      @ForAll @Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")
          String uuid) {
    assertEquals(uuid, UUID.fromString(uuid).toString());
  }

  @Property
  void shouldGenerateMatchingIPv4String(
      @ForAll @Regex("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])")
          String ipv4)
      throws UnknownHostException {
    String[] expected = ipv4.split("\\.");
    String[] actual = InetAddress.getByName(ipv4).getHostAddress().split("\\.");
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(Integer.parseInt(expected[i]), Integer.parseInt(actual[i]));
    }
  }

  @Property
  void shouldGenerateMatchingIsoDateString(
      @ForAll
          @Regex(
              "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z")
          String iso8601Date) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    assertEquals(iso8601Date, formatter.format(formatter.parse(iso8601Date)));
  }

  @Property
  void shouldGenerateUniqueStrings(@ForAll List<@Regex("[a-zA-Z0-9]{32,}") String> strings) {
    assertTrue(
        strings.stream()
            .allMatch(s -> s.length() >= 32 && s.chars().allMatch(Character::isLetterOrDigit)));
    assertEquals(strings.size(), new HashSet<>(strings).size());
  }

  @Property
  void shouldGenerateAnyString(@ForAll String any) {}
}
