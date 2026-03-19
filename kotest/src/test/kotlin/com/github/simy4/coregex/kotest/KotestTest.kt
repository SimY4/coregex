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

package com.github.simy4.coregex.kotest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.RTree
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import java.util.ArrayDeque
import java.util.Collections

public class KotestTest: FunSpec() {
  init {
    test("shrinking test") {
      checkAll(regexes) { regex ->
        var attempts = 1000
        val coregexArbitrary = CoregexArbitrary(regex)
        val sample = coregexArbitrary.sample(randomSource())
        val shrinks = Collections.asLifoQueue(ArrayDeque<RTree<String>>())
        shrinks.add(sample.shrinks)
        while (0 < attempts-- && !shrinks.isEmpty()) {
          var shrink = shrinks.remove()
          var shrinkValue = shrink.value()
          shrinkValue shouldMatch regex
          shrinks += shrink.children.value
        }
      }
    }
  }

  public companion object {
    public val regexes: Arb<Regex> =
      Arb.of(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}",
        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
        "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)",
        "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])",
        "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$",
        "((?i)[a-z]+(?-i)-[A-Z]){3,6}"
      ).map(Regex::fromLiteral)
  }
}
