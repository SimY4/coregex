/*
 * Copyright 2021 Alex Simkin
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

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoregexArbitraryConfiguratorTest {
  @Property
  void shouldGenerateMatchingUUIDString(
      @ForAll @Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")
          String uuid) {
    assertEquals(uuid, UUID.fromString(uuid).toString());
  }

  @Property
  void shouldGenerateAnyString(@ForAll String any) {}
}
